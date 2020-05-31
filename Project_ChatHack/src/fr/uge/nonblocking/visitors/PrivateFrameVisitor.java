package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.frame.*;

public interface PrivateFrameVisitor {
	void visit(PrivateMessage privateMessage);
	void visit(ConfirmationPrivateConnection confirmationPrivateConnection);
	void visit(RequestConfirmationIsValid requestConfirmationIsValid);
	void visit(FileMessage fileMessage);
}
