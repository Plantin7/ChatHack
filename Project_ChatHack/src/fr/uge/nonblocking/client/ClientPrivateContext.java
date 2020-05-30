package fr.uge.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import fr.uge.nonblocking.frame.PublicFrame;
import fr.uge.nonblocking.frame.ConfirmationPrivateConnection;
import fr.uge.nonblocking.frame.PrivateFrame;
import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.readers.FrameReader;
import fr.uge.nonblocking.readers.PrivateFrameReader;
import fr.uge.nonblocking.visitors.PrivateClientFrameVisitor;

public class ClientPrivateContext {
	static private int BUFFER_SIZE = 10_000;

	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
	final private PrivateFrameReader frameReader = new PrivateFrameReader();
	private final PrivateClientFrameVisitor frameVisitor;
	private final ClientChatHack client;
	String privateLogin;

	private boolean closed = false;

	public ClientPrivateContext(ClientChatHack client, SelectionKey key) {
		this.client = client;
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.frameVisitor = new PrivateClientFrameVisitor(this, client);
	}

	/**
	 * Process the content of bbin
	 * <p>
	 * The convention is that bbin is in write-mode before the call
	 * to process and after the call
	 */
	private void processIn() {
		while (true) {
			var status = frameReader.process(bbin);
			switch (status) {
			case DONE:
				PrivateFrame frame = frameReader.get();
				frameReader.reset();
				treatFrame(frame);
				break;
			case REFILL:
				return;
			case ERROR:
				silentlyClose();
				return;
			}
		}
	}

	private void treatFrame(PrivateFrame frame) {
		frame.accept(frameVisitor);
	}

	/**
	 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
	 *
	 * @param bb
	 */
	public void queueMessage(ByteBuffer bb) {
		queue.add(bb);
		processOut();
		updateInterestOps();
	}

	/**
	 * Try to fill bbout from the message queue
	 */
	private void processOut() {
		while (!queue.isEmpty()) {
			var bb = queue.peek();
			if (bb.remaining() <= bbout.remaining()) {
				queue.remove();
				bbout.put(bb);
			} else {
				return;
			}
		}
	}

	/**
	 * Update the interestOps of the key looking
	 * only at values of the boolean closed and
	 * of both ByteBuffers.
	 * <p>
	 * The convention is that both buffers are in write-mode before the call
	 * to updateInterestOps and after the call.
	 * Also it is assumed that process has been be called just
	 * before updateInterestOps.
	 */

	private void updateInterestOps() {
		var interesOps = 0;
		if (!closed && bbin.hasRemaining()) {
			interesOps = interesOps | SelectionKey.OP_READ;
		}
		if (bbout.position() != 0) {
			interesOps |= SelectionKey.OP_WRITE;
		}
		if (interesOps == 0) {
			silentlyClose();
			return;
		}
		key.interestOps(interesOps);
	}

	public void silentlyClose() {
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/**
	 * Performs the read action on sc
	 * <p>
	 * The convention is that both buffers are in write-mode before the call
	 * to doRead and after the call
	 *
	 * @throws IOException
	 */
	public void doRead() throws IOException {
		if (sc.read(bbin) == -1) {
			closed = true;
		}
		processIn();
		updateInterestOps();
	}

	/**
	 * Performs the write action on sc
	 * <p>
	 * The convention is that both buffers are in write-mode before the call
	 * to doWrite and after the call
	 *
	 * @throws IOException
	 */

	public void doWrite() throws IOException {
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
	}

	public void doConnect() throws IOException {
		if (!sc.finishConnect()) {
			return;
		}
		var pendingInfo = client.myPendingRequest.get(privateLogin);
		queueMessage(new ConfirmationPrivateConnection(client.login, pendingInfo.connect_id).asByteBuffer());
		updateInterestOps();
	}
}

