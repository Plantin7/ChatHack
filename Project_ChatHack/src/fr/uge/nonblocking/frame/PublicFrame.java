package fr.uge.nonblocking.frame;

import fr.uge.nonblocking.visitors.PublicFrameVisitor;

import java.nio.ByteBuffer;

/**
 * 
 * @param visitor
 */
public interface PublicFrame { 
	ByteBuffer asByteBuffer();
	void accept(PublicFrameVisitor visitor);
}
