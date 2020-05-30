package fr.uge.nonblocking.client.commands;

public interface Commands {
    void accept(CommandVisitor visitor);
}
