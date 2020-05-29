package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PrivateFrameVisitor;
import fr.uge.nonblocking.visitors.PublicFrameVisitor;

import java.nio.ByteBuffer;

/**
 * 
 * @param visitor
 */
public interface PrivateFrame { 
	ByteBuffer asByteBuffer();
	void accept(PrivateFrameVisitor visitor);
}
