package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class AuthentificationMessage implements Frame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final String password;

    public AuthentificationMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        var bbLogin = UTF8.encode(login);
        ByteBuffer bb;
        if(!password.isEmpty()) {
            var bbPassword = UTF8.encode(password);
            bb = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + bbLogin.limit() + bbPassword.limit());
            bb.put(ChatHackProtocol.OPCODE_ASK_AUTH_WITH_PASSWORD).putInt(bbLogin.limit()).put(bbLogin).putInt(bbPassword.limit()).put(bbPassword);
        }
        else {
            bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit());
            bb.put(ChatHackProtocol.OPCODE_ASK_AUTH_WITHOUT_PASSWORD).putInt(bbLogin.limit()).put(bbLogin);
        }
        return bb.flip();
    }
    
    public String getLogin() {
    	return login;
    }
    
    public String getPassword() {
    	return password;
    }

    @Override
    public String toString() {
        return "AuthentificationMessage{" +
                "OP=" + ChatHackProtocol.OPCODE_ASK_AUTH_WITH_PASSWORD +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visit(this);
    }
}
