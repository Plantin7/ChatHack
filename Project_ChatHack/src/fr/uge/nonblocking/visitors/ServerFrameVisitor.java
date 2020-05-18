package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.frame.*;
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
        server.sendAuthentificationToDB(authentificationMessage, ctx);
    }

	@Override
	public void visit(ResponseAuthentification responseAuthentification) {
		System.out.println("ServerFrameVisitor : TU NE DOIS PAS RENTRER ICI !");
	}

	@Override
	public void visit(StringMessage stringMessage) {
		server.sendAnonymousAuthentificationToDB(stringMessage, ctx);
	}
}
