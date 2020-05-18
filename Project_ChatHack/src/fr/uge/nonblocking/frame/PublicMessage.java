package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class PublicMessage implements Frame {
	private final String from;
	private final String message;
	private final static Charset UTF8 = Charset.forName("UTF-8");
	
	public PublicMessage(String from, String message) {
		this.from = from;
		this.message = message;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(from);
		var bbMessage = UTF8.encode(message);
		var bb = ByteBuffer.allocate(Byte.BYTES + 2*Integer.BYTES + bbLogin.limit() + bbMessage.limit());
		bb.put(ChatHackProtocol.OPCODE_PUBLIC_MESSAGE).putInt(bbLogin.limit()).put(bbLogin).putInt(bbMessage.limit()).put(bbMessage).flip();
		return bb;
	}
	
	@Override
	public String toString() {
		return from + " : " + message;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);		
	}
}
