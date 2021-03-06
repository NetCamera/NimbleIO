package com.gifisan.nio.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.gifisan.nio.TimeoutException;
import com.gifisan.nio.common.CloseUtil;
import com.gifisan.nio.common.LifeCycleUtil;
import com.gifisan.nio.common.MessageFormatter;
import com.gifisan.nio.component.DefaultEndPointWriter;
import com.gifisan.nio.component.NIOContext;
import com.gifisan.nio.component.TCPEndPoint;
import com.gifisan.nio.component.TCPSelectorLoop;
import com.gifisan.nio.component.concurrent.TaskExecutor;
import com.gifisan.nio.component.concurrent.UniqueThread;
import com.gifisan.nio.component.concurrent.Waiter;
import com.gifisan.nio.extend.configuration.ServerConfiguration;

public class TCPConnector extends AbstractIOConnector {

	private TaskExecutor		taskExecutor;
	private TCPSelectorLoop		selectorLoop;
	private DefaultEndPointWriter	endPointWriter;
	private TCPEndPoint			endPoint;
	private UniqueThread		endPointWriterThread	= new UniqueThread();
	private UniqueThread		selectorLoopThread		= new UniqueThread();
	private UniqueThread		taskExecutorThread;
	private long				beatPacket;
	private IOException			connectException;

	public long getBeatPacket() {
		return beatPacket;
	}

	public void setBeatPacket(long beatPacket) {
		this.beatPacket = beatPacket;
	}

	protected UniqueThread getSelectorLoopThread() {
		return selectorLoopThread;
	}

	protected void setIOService(NIOContext context) {
		context.setTCPService(this);
	}

	public String toString() {
		return "TCP:Selector@server:" + serverAddress.toString();
	}

	protected InetSocketAddress getLocalSocketAddress() {
		return endPoint.getLocalSocketAddress();
	}

	protected void connect(InetSocketAddress address) throws IOException {

		SocketChannel channel = SocketChannel.open();

		channel.configureBlocking(false);

		this.selector = Selector.open();

		channel.register(selector, SelectionKey.OP_CONNECT);

		channel.connect(address);

		ServerConfiguration configuration = context.getServerConfiguration();
		
		this.endPointWriter = new DefaultEndPointWriter(configuration.getSERVER_WRITE_QUEUE_SIZE());

		this.selectorLoop = new ClientTCPSelectorLoop(context, selector, this, endPointWriter);
	}

	private Waiter	waiter	= new Waiter();

	protected void startComponent(NIOContext context, Selector selector) throws IOException {

		this.selectorLoopThread.start(selectorLoop, this.toString());

		if (!waiter.await(30000)) {

			CloseUtil.close(this);

			if (connectException == null) {

				throw new TimeoutException("time out");
			}

			throw new TimeoutException(MessageFormatter.format("connect faild,connector:{},nested exception is {}", this, connectException.getMessage())  , connectException);
		}
		
		if (waiter.isSuccess()) {
			return ;
		}
		
		this.connected.compareAndSet(true, false);
		
		throw new TimeoutException(connectException.getMessage(), connectException);
	}

	protected void stopComponent(NIOContext context, Selector selector) {

		LifeCycleUtil.stop(selectorLoopThread);
		LifeCycleUtil.stop(endPointWriterThread);
		LifeCycleUtil.stop(taskExecutorThread);

		CloseUtil.close(endPoint);
	}

	protected int getSERVER_PORT(ServerConfiguration configuration) {
		return configuration.getSERVER_TCP_PORT();
	}

	private void startTouchDistantJob() {
		TouchDistantJob job = new TouchDistantJob(endPointWriter, endPoint);
		this.taskExecutor = new TaskExecutor(job, beatPacket);
		this.taskExecutorThread = new UniqueThread();
		this.taskExecutorThread.start(taskExecutor, "touch-distant-task");
	}

	protected void finishConnect(TCPEndPoint endPoint, IOException exception) {

		if (exception == null) {

			this.endPoint = endPoint;

			this.session = endPoint.getSession();

			this.waiter.setPayload(null);

			if (waiter.isSuccess()) {

				this.endPointWriterThread.start(endPointWriter, endPointWriter.toString());

				if (beatPacket > 0) {
					this.startTouchDistantJob();
				}
			}
			
		} else {

			this.connectException = exception;

			this.waiter.setPayload(null,false);
		}
	}
}
