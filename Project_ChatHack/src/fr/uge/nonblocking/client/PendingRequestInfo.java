package fr.uge.nonblocking.client;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class PendingRequestInfo {
	
	enum State {REQUEST_REFUSE, REQUEST_PENDING, REQUEST_DONE}
	final Queue<ByteBuffer> pendingMessage = new LinkedList<>();
	
	State state = State.REQUEST_PENDING;
	ClientPrivateContext privateContext;
	long connect_id;
	
	public PendingRequestInfo() {}
	
//	public PendingRequestInfo(ClientPrivateContext privateContext, long connect_id) {
//		this.connect_id = connect_id;
//		this.privateContext = privateContext;
//	}
	
	public void add(ByteBuffer message) {
		pendingMessage.add(message);
	}
	
	public void clearPendingMessage() {
		pendingMessage.clear();
	}
}
