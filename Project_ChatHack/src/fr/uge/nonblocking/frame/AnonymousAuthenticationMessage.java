package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class AnonymousAuthenticationMessage implements PublicFrame {
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private final String login;
	
	public AnonymousAuthenticationMessage(String login) {
		this.login = login;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
        var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit());
        bb.put(ChatHackProtocol.OPCODE_ASK_AUTH_WITHOUT_PASSWORD).putInt(bbLogin.limit()).put(bbLogin).flip();
		return bb;
	}

	@Override
	public void accept(PublicFrameVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String toString() {
		return login;
	}
	
	public String getLogin() {
		return login;
	}

}
