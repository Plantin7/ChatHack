package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.client.context.ClientContext;
import fr.uge.nonblocking.frame.*;

public class ClientFrameVisitor implements FrameVisitor {
    private final ClientContext ctx;
    private final ClientChatHack clientChatHack;

    public ClientFrameVisitor(ClientContext ctx, ClientChatHack clientChatHack){
        this.ctx = ctx;
        this.clientChatHack = clientChatHack;
    }

    @Override
    public void visit(PublicMessage publicMessage) {
    	clientChatHack.displayFrameDialog(publicMessage);
    }

    @Override
    public void visit(AuthentificationMessage authentificationMessage) {
    	System.out.println("ClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
    }

	@Override
	public void visit(ResponseAuthentification responseAuthentification) {
		clientChatHack.displayFrameDialog(responseAuthentification);
	}

	@Override
	public void visit(StringMessage stringMessage) {
		System.out.println("ServerFrameVisitor : TU NE DOIS PAS RENTRER ICI !");
	}

	@Override
	public void visit(RequestPrivateConnection requestPrivateConnection) {
		System.out.println("ClientFrameVisitor : TU NE DOIS PAS RENTRER ICI !");
	}

	@Override
	public void visit(ErrorPrivateConnection errorPrivateConnection) {
		clientChatHack.errorPendingPrivateConnectionRequest(errorPrivateConnection);
	}

	@Override
	public void visit(RefusePrivateConnection refusePrivateConnection) {
		clientChatHack.manageRefusePrivateConnection(refusePrivateConnection);
	}

	@Override
	public void visit(AcceptPrivateConnection acceptPrivateConnection) {
		clientChatHack.manageAcceptPrivateConnection(acceptPrivateConnection);
	}

	@Override
	public void visit(PrivateMessage privateMessage) {
		System.out.println("ClientFrameVisitor : TU NE DOIS PAS RENTRER ICI !");
	}

	@Override
	public void visit(SendPrivateConnection sendPrivateConnection) {
		clientChatHack.manageRequestPrivateConnection(sendPrivateConnection);
	}
	
}
