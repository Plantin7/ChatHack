package fr.uge.nonblocking.client.commands;

import fr.uge.nonblocking.client.commands.Commands;
import fr.uge.nonblocking.client.commands.CommandVisitor;

public class FileMessageCommand implements Commands {


    @Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }
}
