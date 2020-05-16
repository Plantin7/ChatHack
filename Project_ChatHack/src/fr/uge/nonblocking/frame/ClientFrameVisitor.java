package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.client.context.ClientContext;

public class ClientFrameVisitor implements FrameVisitor{
    private final ClientContext ctx;
    private final ClientChatHack clientChatHack;

    public ClientFrameVisitor(ClientContext ctx, ClientChatHack clientChatHack){
        this.ctx = ctx;
        this.clientChatHack = clientChatHack;
    }

    @Override
    public void visit(PublicMessage publicMessage) {
    	clientChatHack.displayDialog(publicMessage);
    }

    @Override
    public void visit(AuthentificationMessage authentificationMessage) {
    	System.out.println("ClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
    }

	@Override
	public void visit(ResponseAuthentification responseAuthentification) {
		clientChatHack.displayAuthentification(responseAuthentification);
	}
}
