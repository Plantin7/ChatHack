package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class ErrorPrivateConnection implements PublicFrame {

	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private final String login;

	public ErrorPrivateConnection(String login) {
		this.login = login;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit());
		bb.put(ChatHackProtocol.OPCODE_ERROR_PRIVATE_CONNECTION).putInt(bbLogin.limit()).put(bbLogin);
		return bb.flip();
	}

	public String getLogin() {
		return login;
	}

	@Override
	public String toString() {
		return "(INFO) The client " + "\"" + login + "\"" + " is not connected !";
	}

	@Override
	public void accept(PublicFrameVisitor visitor) {
		visitor.visit(this);
	}
}
