package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;

public class DB {
	private final byte opcode;
	private final long id;
	
	public DB(byte opcode, long id) {
		this.opcode = opcode;
		this.id = id;
	}

	public ByteBuffer asByteBuffer() {
		var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES);
		bb.put(opcode).putLong(id).flip();
		return bb;
	}
	
	public long getId() {
		return id;
	}
	
	public byte getOpcode() {
		return opcode;
	}
	
	@Override
	public String toString() {
		return "OPCODE " + opcode + "ID " + id ;
	}
}
