package fr.uge.nonblocking.frame;

import java.nio.ByteBuffer;

/**
 * 
 * @param visitor
 */
public interface Frame { 
	ByteBuffer asByteBuffer();
	void accept(FrameVisitor visitor);
}
