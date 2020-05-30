package fr.uge.nonblocking.client.commands;

public interface CommandVisitor {
    void visit(PublicMessageCommand publicMessage);
    void visit(PrivateMessageCommand privateMessage);
    void visit(FileMessageCommand fileMessage);
    void visit(AcceptPrivateConnectionCommand acceptPrivateConnectionCommand);
    void visit(RefusePrivateConnectionCommand refusePrivateConnection);
}
