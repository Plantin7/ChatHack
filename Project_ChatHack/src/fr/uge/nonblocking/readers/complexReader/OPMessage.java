package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.PublicMessage;

public class OPMessage {
    private final byte op;
    private final PublicMessage message;
    private final static int MESSAGE_SIZE = 1024;

    public OPMessage(byte op, PublicMessage message) {
        this.op = op;
        this.message = message;
    }

    public ByteBuffer toByteBuffer() {
    	var sizeMessage = message.asByteBuffer().remaining();
        var bb = ByteBuffer.allocate(Byte.BYTES + sizeMessage);
        bb.put(op).put(message.asByteBuffer()).flip();
        return bb;
    }

    @Override
    public String toString() {
        return "OPMessage{" +
                "op=" + op +
                ", message=" + message +
                '}';
    }
}
