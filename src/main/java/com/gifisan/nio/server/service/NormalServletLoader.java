package com.gifisan.nio.server.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gifisan.nio.AbstractLifeCycle;
import com.gifisan.nio.Encoding;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.common.PropertiesLoader;
import com.gifisan.nio.common.StringUtil;
import com.gifisan.nio.component.Configuration;
import com.gifisan.nio.component.DynamicClassLoader;
import com.gifisan.nio.server.FilterAcceptor;
import com.gifisan.nio.server.ServerContext;

public class NormalServletLoader extends AbstractLifeCycle implements ServletLoader {

	private ServerContext				context		= null;
	private DynamicClassLoader			classLoader	= null;
	private ReentrantLock				lock			= new ReentrantLock();
	private Logger						logger		= LoggerFactory.getLogger(NormalServletLoader.class);
	private Map<String, GenericServlet>	servlets		= new LinkedHashMap<String, GenericServlet>();

	public NormalServletLoader(ServerContext context, DynamicClassLoader classLoader) {
		this.context = context;
		this.classLoader = classLoader;
	}

	protected void doStart() throws Exception {

		Map<String, GenericServlet> servlets = loadServlets(context, this.classLoader);

		this.initializeServlets(servlets);

		this.servlets = servlets;

	}

	protected void doStop() throws Exception {

		ReentrantLock lock = this.lock;

		lock.lock();

		Set<Entry<String, GenericServlet>> entries = servlets.entrySet();

		for (Entry<String, GenericServlet> entry : entries) {

			GenericServlet servlet = entry.getValue();

			try {

				servlet.destroy(context, servlet.getConfig());

				logger.info("[NIOServer] 卸载完成 [ {} ]", servlet);

			} catch (Throwable e) {

				logger.error(e.getMessage(), e);

			}
		}

		lock.unlock();
	}

	public FilterAcceptor getServlet(String serviceName) {

		return servlets.get(serviceName);

	}

	private void initializeServlets(Map<String, GenericServlet> servlets) throws Exception {

		Set<Entry<String, GenericServlet>> entries = servlets.entrySet();

		for (Entry<String, GenericServlet> entry : entries) {

			GenericServlet servlet = entry.getValue();

			servlet.initialize(context, servlet.getConfig());

			logger.info("[NIOServer] 加载完成 [ {} ]", servlet);

		}
	}

	private Map<String, GenericServlet> loadServlets(JSONArray array, DynamicClassLoader classLoader) throws Exception {

		Map<String, GenericServlet> servlets = new LinkedHashMap<String, GenericServlet>();

		if (array.size() == 0) {

			throw new Error("empty servlet config");

		}

		for (int i = 0; i < array.size(); i++) {

			JSONObject object = array.getJSONObject(i);

			String className = object.getString("class");

			String serviceName = object.getString("serviceName");

			Configuration config = new Configuration(object);

			Class<?> clazz = classLoader.forName(className);

			if (StringUtil.isNullOrBlank(serviceName)) {

				serviceName = clazz.getSimpleName();

			}

			if (servlets.containsKey(serviceName)) {
				throw new IllegalArgumentException("repeat servlet[ " + serviceName + "@" + className + " ]");
			}

			GenericServlet servlet = (GenericServlet) clazz.newInstance();

			servlets.put(serviceName, servlet);

			servlet.setConfig(config);

		}
		return servlets;
	}

	private Map<String, GenericServlet> loadServlets(ServerContext context, DynamicClassLoader classLoader)
			throws Exception {

		String config = PropertiesLoader.loadContent("servlets.config", Encoding.UTF8);
		
		if (!StringUtil.isNullOrBlank(config)) {

			JSONArray array = JSONObject.parseArray(config);

			return loadServlets(array, classLoader);

		} else {

			throw new Error("不存在Servlet配置文件");

		}
	}

	public void prepare(ServerContext context, Configuration config) throws Exception {

		logger.info("[NIOServer] 尝试加载新的Servlet配置......");

		this.servlets = loadServlets(context, classLoader);

		logger.info("[NIOServer] 尝试启动新的Servlet配置......");

		this.prepare(servlets);

	}

	private void prepare(Map<String, GenericServlet> servlets) throws Exception {

		Set<Entry<String, GenericServlet>> entries = servlets.entrySet();

		for (Entry<String, GenericServlet> entry : entries) {

			GenericServlet servlet = entry.getValue();

			servlet.prepare(context, servlet.getConfig());

			logger.info("[NIOServer] 新的Servlet [ {} ] Prepare完成", servlet);

		}
	}

	public void unload(ServerContext context, Configuration config) throws Exception {

		ReentrantLock lock = this.lock;

		lock.lock();

		Set<Entry<String, GenericServlet>> entries = servlets.entrySet();

		for (Entry<String, GenericServlet> entry : entries) {

			GenericServlet servlet = entry.getValue();

			try {

				servlet.unload(context, servlet.getConfig());

				logger.info("[NIOServer] 旧的Servlet [ {} ] Unload完成", servlet);

			} catch (Throwable e) {

				logger.info("[NIOServer] 旧的Servlet [ {} ] Unload失败", servlet);

				logger.error(e.getMessage(), e);

			}
		}
		lock.unlock();

	}

}
