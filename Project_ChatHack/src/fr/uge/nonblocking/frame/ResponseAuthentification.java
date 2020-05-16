package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ResponseAuthentification implements Frame{
	private final String message;
	private final static Charset UTF8 = Charset.forName("UTF-8");
	private final byte OP = 11;
	
	public ResponseAuthentification(String message) {
		this.message = message;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbMessage = UTF8.encode(message);
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbMessage.limit());
		bb.put(OP).putInt(bbMessage.limit()).put(bbMessage).flip();
		return bb;
	}
	
	@Override
	public String toString() {
		return message;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);		
	}
}
