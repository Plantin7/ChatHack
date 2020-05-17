package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.protocol.ChatHackProtocol;

public class ResponseAuthentification implements Frame{
	private final String message;
	private final static Charset UTF8 = Charset.forName("UTF-8");
	
	public ResponseAuthentification(String message) {
		this.message = message;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbMessage = UTF8.encode(message);
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbMessage.limit());
		bb.put(ChatHackProtocol.OPCODE_RESPONSE_AUTH_WITH_PASSWORD).putInt(bbMessage.limit()).put(bbMessage).flip();
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
