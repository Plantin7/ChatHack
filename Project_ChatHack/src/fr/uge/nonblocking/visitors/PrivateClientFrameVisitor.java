package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.client.ClientChatHack;
import fr.uge.nonblocking.client.ClientPrivateContext;
import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.frame.AuthentiticationMessage;
import fr.uge.nonblocking.frame.ConfirmationPrivateConnection;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.FileMessage;
import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.frame.RequestConfirmationIsValid;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.frame.SendPrivateConnection;
import fr.uge.nonblocking.frame.AnonymousAuthenticationMessage;

public class PrivateClientFrameVisitor implements PrivateFrameVisitor {
    private final ClientPrivateContext ctx;
    private final ClientChatHack clientChatHack;

    public PrivateClientFrameVisitor(ClientPrivateContext ctx, ClientChatHack clientChatHack){
        this.ctx = ctx;
        this.clientChatHack = clientChatHack;
    }
	@Override
	public void visit(PrivateMessage privateMessage) {
		clientChatHack.displayFrameDialog(privateMessage);
	}
	@Override
	public void visit(ConfirmationPrivateConnection confirmationPrivateConnection) {
		clientChatHack.checkConnectId(confirmationPrivateConnection, ctx);
	}
	@Override
	public void visit(RequestConfirmationIsValid requestConfirmationIsValid) {
		clientChatHack.receivedValidationOfConfirmation(requestConfirmationIsValid, ctx);
	}
	@Override
	public void visit(FileMessage fileMessage) {
		System.out.println("File Received !");
		clientChatHack.receiveFile(fileMessage);
	}
}
