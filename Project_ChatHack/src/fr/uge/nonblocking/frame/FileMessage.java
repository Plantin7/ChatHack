package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.visitors.PrivateFrameVisitor;
import fr.uge.protocol.ChatHackProtocol;

public class FileMessage implements PrivateFrame {

    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final String fileName;
    private final ByteBuffer bbFileData;

    public FileMessage(String login, String fileName, ByteBuffer bbFileData) {
        this.login = login;
        this.fileName = fileName;
        this.bbFileData = bbFileData;
        bbFileData.flip();
    }
    
    @Override
    public ByteBuffer asByteBuffer() {
        var bbLogin = UTF8.encode(login);
        var bbFileName = UTF8.encode(fileName);
        var bb = ByteBuffer.allocate(Byte.BYTES + 3 * Integer.BYTES + bbLogin.limit() + bbFileName.limit() + bbFileData.limit());
        bb.put(ChatHackProtocol.OPCODE_SEND_FILE_MESSAGE)
        .putInt(bbLogin.limit()).put(bbLogin)
        .putInt(bbFileName.limit()).put(bbFileName)
        .putInt(bbFileData.limit()).put(bbFileData)
        .flip();
        return bb;
    }
    
    public String getFileName() {
		return fileName;
	}
    
    public ByteBuffer getBbFileData() {
		return bbFileData;
	}

    @Override
    public void accept(PrivateFrameVisitor visitor) {
    	visitor.visit(this);
    }
    
    @Override
    public String toString() {
    	return login + ":" + fileName + bbFileData.toString();
    }
}
