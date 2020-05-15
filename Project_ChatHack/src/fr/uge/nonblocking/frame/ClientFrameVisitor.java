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

    public void visit(PublicMessage publicMessage) {
    	//ctx.queueMessage(publicMessage.asByteBuffer());
    	//System.out.println(publicMessage);
    	clientChatHack.displayDialog(publicMessage);
    }
}
