package fr.uge.nonblocking.database;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.protocol.ServerMDPProtococol;

public class RequestAuthenticationWithPassword {
	private final long id;
	private final String login;
	private final String password;
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	
	
	public RequestAuthenticationWithPassword(long id, String login, String password) {
		this.id = id;
		this.login = login;
		this.password = password;
	}
	
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bbPassword = UTF8.encode(password);
		var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + 2 * Integer.BYTES + bbLogin.limit() + bbPassword.limit());
		bb.put(ServerMDPProtococol.OPCODE_ASK_AUTH_TO_DB_WITH_PASWWORD)
		.putLong(id)
		.putInt(bbLogin.limit()).put(bbLogin)
		.putInt(bbPassword.limit()).put(bbPassword)
		.flip();
		return bb;
	}
	

}
