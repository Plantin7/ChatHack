package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.frame.*;

public interface FrameVisitor {
	void visit(PublicMessage publicMessage);
	void visit(AuthentificationMessage authentificationMessage);
	void visit(ResponseAuthentification responseAuthentification);
	void visit(StringMessage stringMessage);
	void visit(RequestPrivateConnection requestPrivateConnection);
	void visit(ErrorPrivateConnection errorPrivateConnection);
	void visit(RefusePrivateConnection refusePrivateConnection);
	void visit(AcceptPrivateConnection acceptPrivateConnection);
	void visit(PrivateMessage privateMessage);
}
