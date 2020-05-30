package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.visitors.PrivateFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class ConfirmationPrivateConnection implements PrivateFrame {
	private final String login;
	private final long connect_id;
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	
	public ConfirmationPrivateConnection(String login, long connect_id) {
		this.login = login;
		this.connect_id = connect_id;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + Integer.BYTES + bbLogin.limit());
		bb.put(ChatHackProtocol.OPCODE_VERIF_CONNECT_ID_PRIVATE_CONNECTION).putInt(bbLogin.limit()).put(bbLogin).putLong(connect_id).flip();
		return bb;
	}

	@Override
	public void accept(PrivateFrameVisitor visitor) {
		visitor.visit(this);
	}
	
	public long getConnect_id() {
		return connect_id;
	}
	
	public String getLogin() {
		return login;
	}

}
