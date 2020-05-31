package fr.uge.nonblocking.client.commands;

import fr.uge.nonblocking.client.commands.Commands;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.client.commands.CommandVisitor;

public class FileMessageCommand implements Commands {
	private final String login;
	private final String fileName;
	private final ByteBuffer bbFileData;
	
    public FileMessageCommand(String login, String fileName, ByteBuffer bbFileData) {
		this.login = login;
		this.fileName = fileName;
		this.bbFileData = bbFileData;
	}
    
    public String getLogin() {
		return login;
	}
    
    public String getFileName() {
		return fileName;
	}
    
    public ByteBuffer getBbFileData() {
		return bbFileData;
	}
    
	@Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }
}
