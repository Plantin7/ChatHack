package fr.uge.nonblocking.visitors;

import fr.uge.nonblocking.frame.*;

public interface FrameVisitor {
	void visit(PublicMessage publicMessage);
	void visit(AuthentificationMessage authentificationMessage);
	void visit(ResponseAuthentification responseAuthentification);
	void visit(StringMessage stringMessage);
	void visit(RequestPrivateConnection requestPrivateConnection);
}
