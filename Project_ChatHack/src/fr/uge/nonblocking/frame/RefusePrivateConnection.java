package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class RefusePrivateConnection implements Frame {
	
	private final String login;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	public RefusePrivateConnection(String login) {
		this.login = login;
	}
	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bbLoginSize = bbLogin.limit();
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLoginSize);
		bb.put(ChatHackProtocol.OPCODE_REFUSE_PRIVATE_CONNECTION).putInt(bbLoginSize).put(bbLogin);
		return bb.flip();
	}
	
	public String getLogin() {
		return login;
	}
	
	@Override
	public String toString() {
		return "The client " + "\"" + login + "\"" + " has refused your request !";
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}
}
