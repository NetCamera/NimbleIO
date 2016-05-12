package com.gifisan.nio.component.future;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.gifisan.nio.component.ByteArrayInputStream;
import com.gifisan.nio.component.TCPEndPoint;
import com.gifisan.nio.component.IOEventHandle;
import com.gifisan.nio.component.Session;

//ByteArrayInputStreamResponseWriter
public class ByteArrayWriteFuture extends AbstractWriteFuture {

	public ByteArrayWriteFuture(TCPEndPoint endPoint,Session session,String serviceName, ByteBuffer textBuffer, byte []textCache ,
			ByteArrayInputStream inputStream,IOEventHandle handle) {
		super(endPoint,handle,serviceName, textBuffer, textCache, session);
		this.inputStream = inputStream;
		this.streamBuffer = ByteBuffer.wrap(inputStream.toByteArray());
	}

	private ByteBuffer	streamBuffer	= null;

	public boolean write() throws IOException {
		ByteBuffer buffer = this.textBuffer;

		if (buffer.hasRemaining()) {
			attackNetwork(endPoint.write(buffer));
			if (buffer.hasRemaining()) {
				return false;
			}
		}

		buffer = streamBuffer;

		if (buffer.hasRemaining()) {
			attackNetwork(endPoint.write(buffer));

			return !buffer.hasRemaining();
		}

		return true;
	}

}
