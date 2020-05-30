package fr.uge.nonblocking.client.commands;

import fr.uge.nonblocking.client.commands.Commands;
import fr.uge.nonblocking.client.commands.CommandVisitor;

public class AcceptPrivateConnectionCommand  implements Commands {

    private String login;

    public AcceptPrivateConnectionCommand(String login) {
        this.login = login;
    }


    public String getLogin() {
        return login;
    }

    @Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }
}
