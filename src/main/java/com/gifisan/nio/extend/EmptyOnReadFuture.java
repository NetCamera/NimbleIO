package com.gifisan.nio.extend;

import com.gifisan.nio.component.Session;
import com.gifisan.nio.component.protocol.future.ReadFuture;

public class EmptyOnReadFuture implements OnReadFuture{
	
	public void onResponse(Session session, ReadFuture future) {
		
	}
	
}
