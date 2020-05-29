package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringMessage implements PublicFrame {
	
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private final String stringMessage;
	
	
	public StringMessage(String stringMessage) {
		this.stringMessage = stringMessage;
	}

	@Override
	public ByteBuffer asByteBuffer() {
		var bbString = UTF8.encode(stringMessage);
		var bb = ByteBuffer.allocate(bbString.limit());
		bb.put(bbString).flip();
		return bb;
	}

	@Override
	public void accept(PublicFrameVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String toString() {
		return stringMessage;
	}
	
	public String getStringMessage() {
		return stringMessage;
	}

}
