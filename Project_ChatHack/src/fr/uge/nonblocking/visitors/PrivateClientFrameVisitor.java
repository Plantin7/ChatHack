package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.client.context.ClientPrivateContext;
import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.frame.StringMessage;

public class PrivateClientFrameVisitor implements FrameVisitor {
    private final ClientPrivateContext ctx;
    private final ClientChatHack clientChatHack;

    public PrivateClientFrameVisitor(ClientPrivateContext ctx, ClientChatHack clientChatHack){
        this.ctx = ctx;
        this.clientChatHack = clientChatHack;
    }

    @Override
    public void visit(PublicMessage publicMessage) {
    	System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
    }

    @Override
    public void visit(AuthentificationMessage authentificationMessage) {
    	System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
    }

	@Override
	public void visit(ResponseAuthentification responseAuthentification) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(StringMessage stringMessage) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(RequestPrivateConnection requestPrivateConnection) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(ErrorPrivateConnection errorPrivateConnection) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(RefusePrivateConnection refusePrivateConnection) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(AcceptPrivateConnection acceptPrivateConnection) {
		System.out.println("PrivateClientFrameVisitor : TU NE DOIS PAS RENTRER ICI");
	}

	@Override
	public void visit(PrivateMessage privateMessage) {
		clientChatHack.displayFrameDialog(privateMessage);
	}

}
