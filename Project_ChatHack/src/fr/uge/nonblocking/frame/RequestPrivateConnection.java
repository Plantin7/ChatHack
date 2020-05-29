package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RequestPrivateConnection implements PublicFrame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final String message;

    public RequestPrivateConnection(String login, String message) {
		this.login = login;
        this.message = message;
    }
    
	@Override
    public ByteBuffer asByteBuffer() {
    	var bbLogin = UTF8.encode(login);
    	var bbMessage = UTF8.encode(message);
        var bb = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + bbLogin.limit() + bbMessage.limit());
        bb.put(ChatHackProtocol.OPCODE_ASK_PRIVATE_CONNECTION).putInt(bbLogin.limit()).put(bbLogin).putInt(bbMessage.limit()).put(bbMessage);
        return bb.flip();
    }

    public String getLogin() {
        return login;
    }
    
    public String getMessage() {
    	return message;
    }

    @Override
    public String toString() {
        return login + " : " + message;
    }

    @Override
    public void accept(PublicFrameVisitor visitor) {
        visitor.visit(this);
    }
}
