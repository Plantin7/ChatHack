package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.FrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileMessage implements Frame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private String login;
    private long keyPrivateConnection;
    private String nameFile;
    private long nbBlocs;
    private String content;

    public FileMessage(String login, long keyPrivateConnection, String nameFile, long nbBlocs, String content) {
        this.login = login;
        this.keyPrivateConnection = keyPrivateConnection;
        this.nameFile = nameFile;
        this.nbBlocs = nbBlocs;
        this.content = content;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        var bbLogin = UTF8.encode(login);
        var bbNameFile = UTF8.encode(nameFile);
        var bbContent = UTF8.encode(content);
        var bb = ByteBuffer.allocate(
                Byte.BYTES + 3* Integer.BYTES + bbLogin.limit() +  bbNameFile.limit() + bbContent.limit()
                + 2* Long.BYTES);
        bb.put(ChatHackProtocol.OPCODE_SEND_FILE_MESSAGE)
                .putLong(keyPrivateConnection)
                .putInt(bbLogin.limit())
                .put(bbLogin)
                .putInt(bbNameFile.limit())
                .put(bbNameFile)
                .putLong(nbBlocs)
                .putInt(bbContent.limit())
                .put(bbContent);

        return bb.flip();
    }

    @Override
    public void accept(FrameVisitor visitor) {
        // TODO to avoid merge conflit
    }
}
