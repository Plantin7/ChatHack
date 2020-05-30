package fr.uge.nonblocking.client.commands;

import fr.uge.nonblocking.client.commands.Commands;
import fr.uge.nonblocking.client.commands.CommandVisitor;

public class PrivateMessageCommand implements Commands {

    private String from;
    private String message;

    public PrivateMessageCommand(String from, String message) {
        this.from = from;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }
}
