package com.gifisan.nio.component;

import java.net.SocketException;

import com.gifisan.nio.Attachment;
import com.gifisan.nio.common.MessageFormatter;
import com.gifisan.nio.common.UUIDGenerator;
import com.gifisan.nio.component.protocol.tcp.ProtocolEncoder;
import com.gifisan.nio.server.NIOContext;

public abstract class AbstractSession extends AttributesImpl implements Session {

	private Attachment					attachment			= null;
	private long						creationTime			= System.currentTimeMillis();
	private byte						logicSessionID			= 0;
	private boolean					closed				= false;
	private SessionEventListenerWrapper	lastListener			= null;
	private SessionEventListenerWrapper	listenerStub			= null;
	protected TCPEndPoint				endPoint				= null;
	protected OutputStreamAcceptor		outputStreamAcceptor	= null;
	protected ProtocolEncoder			encoder				= null;
	protected EndPointWriter			endPointWriter			= null;
	protected String 					sessionID				= null;

	public AbstractSession(TCPEndPoint endPoint, byte logicSessionID) {
		NIOContext context = endPoint.getContext();
		this.endPointWriter = endPoint.getEndPointWriter();
		this.encoder = context.getProtocolEncoder();
		this.outputStreamAcceptor = context.getOutputStreamAcceptor();
		this.logicSessionID = logicSessionID;
		this.endPoint = endPoint;
	}

	public void addEventListener(SessionEventListener listener) {
		if (this.listenerStub == null) {
			this.listenerStub = new SessionEventListenerWrapper(listener);
			this.lastListener = this.listenerStub;
		} else {
			this.lastListener.setNext(new SessionEventListenerWrapper(listener));
		}
	}

	public void attach(Attachment attachment) {
		this.attachment = attachment;
	}

	public Attachment attachment() {
		return this.attachment;
	}

	public long getCreationTime() {
		return this.creationTime;
	}

	public String getLocalAddr() {
		return endPoint.getLocalAddr();
	}

	public String getLocalHost() {
		return endPoint.getLocalHost();
	}

	public int getLocalPort() {
		return endPoint.getLocalPort();
	}

	public int getMaxIdleTime() throws SocketException {
		return endPoint.getMaxIdleTime();
	}

	public String getRemoteAddr() {
		return endPoint.getRemoteAddr();
	}

	public String getRemoteHost() {
		return endPoint.getRemoteHost();
	}

	public int getRemotePort() {
		return endPoint.getRemotePort();
	}

	public boolean isBlocking() {
		return endPoint.isBlocking();
	}

	public boolean isOpened() {
		return endPoint.isOpened();
	}

	public byte getLogicSessionID() {
		return logicSessionID;
	}

	public void destroyImmediately() {

		this.closed = true;

		SessionEventListenerWrapper listenerWrapper = this.listenerStub;

		for (; listenerWrapper != null;) {
			listenerWrapper.onDestroy(this);
			listenerWrapper = listenerWrapper.nextListener();
		}
	}

	protected TCPEndPoint getEndPoint() {
		return endPoint;
	}

	public OutputStreamAcceptor getOutputStreamAcceptor() {
		return outputStreamAcceptor;
	}

	public String toString() {
		return MessageFormatter.format("session-{},edp-{}", logicSessionID, endPoint.toString());
	}

	public boolean closed() {
		return closed;
	}

	public String getSessionID() {
		if (sessionID == null) {
			sessionID = UUIDGenerator.random();
		}
		return sessionID;
	}
	
	
}
