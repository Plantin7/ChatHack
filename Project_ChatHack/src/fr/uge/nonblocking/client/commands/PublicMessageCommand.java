package fr.uge.nonblocking.client.commands;

import fr.uge.nonblocking.client.commands.Commands;
import fr.uge.nonblocking.client.commands.CommandVisitor;

public class PublicMessageCommand implements Commands {

    private String line;

    public PublicMessageCommand(String line) {
        this.line = line;
    }

    public String getLine() {
        return line;
    }

    @Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }
}
