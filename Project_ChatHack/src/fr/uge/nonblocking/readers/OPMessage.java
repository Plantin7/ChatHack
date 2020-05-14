package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

public class OPMessage {
    private final byte op;
    private final Message message;
    private final static int MESSAGE_SIZE = 1024;

    public OPMessage(byte op, Message message) {
        this.op = op;
        this.message = message;
    }

    public ByteBuffer toByteBuffer() {
        var bb = ByteBuffer.allocate(Byte.BYTES + MESSAGE_SIZE);
        bb.put(op).put(message.toByteBuffer()).flip();
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
