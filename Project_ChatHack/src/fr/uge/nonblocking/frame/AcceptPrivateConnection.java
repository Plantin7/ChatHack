package fr.uge.nonblocking.frame;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.visitors.PublicFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class AcceptPrivateConnection implements PublicFrame {
	
	private final InetSocketAddress socketAddress;
	private final long connectId;
	private final String login;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	public AcceptPrivateConnection(String login, InetSocketAddress socketAddress, long connectId) {
		this.login = login;
		this.socketAddress = socketAddress;
		this.connectId = connectId;
	}
	@Override
	public ByteBuffer asByteBuffer() {
		var bbLogin = UTF8.encode(login);
		var bbSocketAddress = encodeSocketAddress();
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbLogin.limit() + bbSocketAddress.limit() + Long.BYTES);
		bb.put(ChatHackProtocol.OPCODE_ACCEPT_PRIVATE_CONNECTION).putInt(bbLogin.limit()).put(bbLogin).put(bbSocketAddress).putLong(connectId);
		return bb.flip();
	}
	
	private ByteBuffer encodeSocketAddress() {
		var bbHostName = UTF8.encode(socketAddress.getHostName());
		var port = socketAddress.getPort();
		var bbSocketAddress = ByteBuffer.allocate(2 * Integer.BYTES + bbHostName.limit());
		bbSocketAddress.putInt(bbHostName.limit()).put(bbHostName).putInt(port);
		return bbSocketAddress.flip();
	}
	
	public String getLogin() {
		return login;
	}
	
	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}
	
	public long getConnectId() {
		return connectId;
	}
	
	@Override
	public String toString() {
		return "The client has accept your request :) | Informations Login :" + login + " InetSocketAddress " + socketAddress.getAddress() + " ConnectId " + connectId;
	}

	@Override
	public void accept(PublicFrameVisitor visitor) {
		visitor.visit(this);
	}
}
