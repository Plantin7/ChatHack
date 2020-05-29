package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.visitors.PrivateFrameVisitor;
import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class PrivateMessage implements PrivateFrame {
	private final String from;
	private final String message;
	private final static Charset UTF8 = Charset.forName("UTF-8");
	
	public PrivateMessage(String from, String message) {
		this.from = from;
		this.message = message;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(from);
		var bbMessage = UTF8.encode(message);
		var bb = ByteBuffer.allocate(Byte.BYTES + 2*Integer.BYTES + bbLogin.limit() + bbMessage.limit());
		bb.put(ChatHackProtocol.OPCODE_SEND_PRIVATE_MESSAGE).putInt(bbLogin.limit()).put(bbLogin).putInt(bbMessage.limit()).put(bbMessage).flip();
		return bb;
	}
	
	@Override
	public String toString() {
		return from + " : " + message;
	}

	@Override
	public void accept(PrivateFrameVisitor visitor) {
		visitor.visit(this);		
	}

}
