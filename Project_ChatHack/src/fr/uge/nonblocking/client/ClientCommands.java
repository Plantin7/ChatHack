package fr.uge.nonblocking.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ClientCommands {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
    private static final byte OP_CONNECTION_WITH_MDP = 1;
    private static final byte OP_CONNECTION_NO_MDP = 2;
	
	public static final ByteBuffer requestAuthentication(String login, String password) {
		var bbLogin = UTF8.encode(login);
		var sizeLogin = bbLogin.remaining();
		
        ByteBuffer bufferAuth;
        if (!password.isEmpty()) {
            var bbPassword = UTF8.encode(password);
            var sizePassWord = bbPassword.remaining();
            bufferAuth = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + sizeLogin + Integer.BYTES + sizePassWord);
            bufferAuth.put(OP_CONNECTION_WITH_MDP).putInt(sizeLogin).put(bbLogin).putInt(sizePassWord).put(bbPassword);
        } else {
            bufferAuth = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + sizeLogin);
            bufferAuth.put(OP_CONNECTION_NO_MDP).putInt(sizeLogin).put(bbLogin);
        }
        
        return bufferAuth.flip();
	}

}
