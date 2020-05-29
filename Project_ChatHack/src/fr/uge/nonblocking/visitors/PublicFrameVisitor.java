package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.frame.*;

public interface PublicFrameVisitor {
	void visit(PublicMessage publicMessage);
	void visit(AuthentiticationMessage authentificationMessage);
	void visit(ResponseAuthentification responseAuthentification);
	void visit(AnonymousAuthenticationMessage stringMessage);
	void visit(RequestPrivateConnection requestPrivateConnection);
	void visit(ErrorPrivateConnection errorPrivateConnection);
	void visit(RefusePrivateConnection refusePrivateConnection);
	void visit(AcceptPrivateConnection acceptPrivateConnection);
	void visit(SendPrivateConnection sendPrivateConnection);
}
