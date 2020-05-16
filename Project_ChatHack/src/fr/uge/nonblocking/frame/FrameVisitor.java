package fr.uge.nonblocking.frame;

public interface FrameVisitor {
	void visit(PublicMessage publicMessage);
	void visit(AuthentificationMessage authentificationMessage);
	void visit(ResponseAuthentification responseAuthentification);
}
