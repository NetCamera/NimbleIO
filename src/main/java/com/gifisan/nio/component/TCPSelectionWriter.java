package com.gifisan.nio.component;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class TCPSelectionWriter implements SelectionAcceptor {

	public void accept(SelectionKey selectionKey) throws IOException {

		TCPEndPoint endPoint = (TCPEndPoint) selectionKey.attachment();

		if (endPoint.isEndConnect()) {
			return;
		}

		endPoint.wakeup();
	}

}
