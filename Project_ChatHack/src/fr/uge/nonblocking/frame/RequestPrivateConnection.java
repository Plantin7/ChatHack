package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RequestPrivateConnection implements Frame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String nameRecipient;

    public RequestPrivateConnection(String nameRecipient) {
        this.nameRecipient = nameRecipient;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        var bbRecipient = UTF8.encode(nameRecipient);
        ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + bbRecipient.limit());
        bb.put(ChatHackProtocol.OPCODE_ASK_PRIVATE_CONNECTION).putInt(bbRecipient.limit()).put(bbRecipient);
        return bb.flip();
    }

    public String getNameRecipient() {
        return nameRecipient;
    }

    @Override
    public String toString() {
        return "RequestPrivateConnection{" +
                "nameRecipient='" + nameRecipient + '\'' +
                '}';
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visit(this);
    }
}
