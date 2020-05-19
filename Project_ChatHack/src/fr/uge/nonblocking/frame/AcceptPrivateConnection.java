package fr.uge.nonblocking.frame;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class AcceptPrivateConnection implements Frame {
	
	//private final InetSocketAddress socketAddress;
	//private final long connectId;
	private final String login;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	public AcceptPrivateConnection(String login) {
		this.login = login;
		//this.socketAddress = socketAddress;
		//this.connectId = connectId;
	}
	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		//var bbSocketAddress = encodeSocketAddress();
		var bbLoginSize = bbLogin.limit();
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLoginSize);
		bb.put(ChatHackProtocol.OPCODE_ACCEPT_PRIVATE_CONNECTION).putInt(bbLoginSize).put(bbLogin);
		return bb.flip();
	}
	
//	private ByteBuffer encodeSocketAddress() {
//		var bbHostName = UTF8.encode(socketAddress.getHostName());
//		var port = socketAddress.getPort();
//		var bbSocketAddress = ByteBuffer.allocate(2 * Integer.BYTES + bbHostName.limit());
//		bbSocketAddress.putInt(port).put(bbHostName);
//		return bbSocketAddress;
//	}
	
	public String getLogin() {
		return login;
	}
	
	@Override
	public String toString() {
		return "The client " + "\"" + login + "\"" + " has accept your request :) ";
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}
}
