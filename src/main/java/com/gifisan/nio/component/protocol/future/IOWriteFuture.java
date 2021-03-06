package com.gifisan.nio.component.protocol.future;

import java.io.IOException;

import com.gifisan.nio.component.TCPEndPoint;

public interface IOWriteFuture extends WriteFuture {

	public abstract boolean write() throws IOException;

	public abstract TCPEndPoint getEndPoint();

	public abstract void onException(IOException e);

	public abstract void onSuccess();
}
