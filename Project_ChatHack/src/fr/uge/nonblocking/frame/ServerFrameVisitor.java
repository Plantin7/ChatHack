package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.server.ServerChatHack;
import fr.uge.nonblocking.server.context.ServerContext;

public class ServerFrameVisitor implements FrameVisitor {

    private final ServerContext ctx;
    private final ServerChatHack server;

    public ServerFrameVisitor(ServerContext ctx, ServerChatHack server) {
        this.ctx = ctx;
        this.server = server;
    }

    @Override
    public void visit(PublicMessage publicMessage) {
        server.broadcast(publicMessage.asByteBuffer());
    }

    @Override
    public void visit(AuthentificationMessage authentificationMessage) {
        server.broadcast(authentificationMessage.asByteBuffer(), ctx.getKey());
    }

    /*public void visit(PrivateMessage privateMessage) {
        //...
        server.sendTo(privateMessage.dst,privateMessage.asByteBuffer());
    }*/
}
