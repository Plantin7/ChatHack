package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.visitors.PrivateFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class RequestConfirmationIsValid implements PrivateFrame {
	private final String login;
	private final Charset UTF8 = StandardCharsets.UTF_8;

	public RequestConfirmationIsValid(String login) {
		this.login = login;
	}
	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit());
		bb.put(ChatHackProtocol.OPCODE_CONNECT_ID_PRIVATE_CONNECTION_IS_VALID).putInt(bbLogin.limit()).put(bbLogin).flip();
		return bb;
	}
	
	public String getLogin() {
		return login;
	}
	
	@Override
	public void accept(PrivateFrameVisitor visitor) {
		visitor.visit(this);
	}

}
