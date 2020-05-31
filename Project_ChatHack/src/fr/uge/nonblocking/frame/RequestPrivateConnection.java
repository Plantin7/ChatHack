package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RequestPrivateConnection implements PublicFrame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public RequestPrivateConnection(String login) {
		this.login = login;
    }
    
	@Override
    public ByteBuffer asByteBuffer() {
    	var bbLogin = UTF8.encode(login);
        var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit());
        bb.put(ChatHackProtocol.OPCODE_ASK_PRIVATE_CONNECTION).putInt(bbLogin.limit()).put(bbLogin);
        return bb.flip();
    }

    public String getLogin() {
        return login;
    }

    @Override
    public String toString() {
        return login;
    }

    @Override
    public void accept(PublicFrameVisitor visitor) {
        visitor.visit(this);
    }
}
