package fr.uge.nonblocking.database;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.protocol.ServerMDPProtococol;

public class RequestAnonymousAuthentication {
	private final long id;
	private final String login;
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	
	
	public RequestAnonymousAuthentication(long id, String login) {
		this.id = id;
		this.login = login;
	}
	
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + Integer.BYTES + bbLogin.limit());
		bb.put(ServerMDPProtococol.OPCODE_ASK_AUTH_TO_DB_WITHOUT_PASSWORD).putLong(id).putInt(bbLogin.limit()).put(bbLogin).flip();
		return bb;
	}

}
