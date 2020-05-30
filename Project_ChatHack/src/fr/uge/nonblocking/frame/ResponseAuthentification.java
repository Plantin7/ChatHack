package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class ResponseAuthentification {
	private final String message;
	private final static Charset UTF8 = Charset.forName("UTF-8");
	
	public ResponseAuthentification(String message) {
		this.message = message;
	}

	
	public ByteBuffer asByteBuffer() {
		var bbMessage = UTF8.encode(message);
		var bb = ByteBuffer.allocate(Integer.BYTES + bbMessage.limit());
		bb.putInt(bbMessage.limit()).put(bbMessage).flip();
		return bb;
	}
	
	@Override
	public String toString() {
		return message;
	}
}
