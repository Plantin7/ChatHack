package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

public class ResponseServer {
	private final byte op;
	private final long id;
	
	public ResponseServer(byte op, long id) {
		this.op = op;
		this.id = id;
	}
	
	public ByteBuffer toByteBuffer() {
		var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES);
		bb.put(op).putLong(id).flip();
		return bb;
	}
	
	@Override
	public String toString() {
		return op + " " + id;
	}

}
