package fr.uge.nonblocking.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.readers.Message;
import fr.uge.nonblocking.readers.MessageReader;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.utils.FileParser;

public class ServerChatHack {

	static private class Context {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(2*Integer.BYTES + 2*BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(2*Integer.BYTES + 2*BUFFER_SIZE);
		final private Queue<ByteBuffer> queue = new LinkedList<>();      // Queue de bytebuffer en read mode
		final private MessageReader messageReader = new MessageReader();    
		final private ServerChatHack server;
		private boolean closed = false;

		private Context(ServerChatHack server, SelectionKey key){
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
		}

		/**
		 * Process the content of bbin
		 *
		 * The convention is that bbin is in write-mode before the call
		 * to process and after the call
		 *
		 */
		private void processIn() {
			while(true) {
				Reader.ProcessStatus status = messageReader.process(bbin);
				switch (status) {
				case DONE:
					Message message = messageReader.get();
					server.broadcast(message);
					messageReader.reset();
					break;
				case REFILL:
					return;
				case ERROR:
					silentlyClose();
					return;
				}
			}
		}

		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param msg
		 */
		private void queueMessage(ByteBuffer msg) {
			queue.add(msg);
			processOut();
			updateInterestOps();
		}

		/**
		 * Try to fill bbout from the message queue
		 *
		 */
		private void processOut() {
			while(!queue.isEmpty()) {
				var bb = queue.peek();
				if(bb.remaining() <= bbout.remaining()) { // suffisament grand pour contenir un message
					bbout.put(queue.poll()); // the size of msg will not exceed 2056 octets
				}
				else {
					return;
				}
			}
		}

		/**
		 * Update the interestOps of the key looking
		 * only at values of the boolean closed and
		 * of both ByteBuffers.
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to updateInterestOps and after the call.
		 * Also it is assumed that process has been be called just
		 * before updateInterestOps.
		 */

		private void updateInterestOps() {
			if(!key.isValid()) {
				return;
			}
			var interestOps = 0;
			if(!closed && bbin.hasRemaining()) {
				interestOps |= SelectionKey.OP_READ;
			}
			if(bbout.position() != 0 || !queue.isEmpty()) {
				interestOps |= SelectionKey.OP_WRITE;
			}
			if(interestOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(interestOps);
		}

		private void silentlyClose() {
			try {
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to doRead and after the call
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			if(sc.read(bbin) == -1) {
				logger.info("Input stream closed");
				closed = true;
			}
			processIn();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call
		 * to doWrite and after the call
		 *
		 * @throws IOException
		 */

		private void doWrite() throws IOException {
			bbout.flip();
			try {
				sc.write(bbout);
			} finally {
				bbout.compact();
			}
			processOut();
			updateInterestOps();
		}

	}

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ServerChatHack.class.getName());

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private SelectionKey serverKey;

	public ServerChatHack(int port, String filePath) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		FileParser.parsePasswordFile(filePath, "$");
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while(!Thread.interrupted()) {
			printKeys(); // for debug
			System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch(IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO,"Connection closed with client due to IOException",e);
			silentlyClose(key);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		var sc = serverSocketChannel.accept();
		if(sc == null) {
			return;
		}
		sc.configureBlocking(false);
		var client = sc.register(selector, SelectionKey.OP_READ);
		client.attach(new Context(this, client));
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	private void broadcast(Message msg) {
		for (var key : selector.keys()) { 
			if(key.isValid() && key != serverKey) { // On ne veut pas du server
				var context = (Context)key.attachment();
				context.queueMessage(msg.toByteBuffer());
			}
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 2){
			usage();
			return;
		}
		new ServerChatHack(Integer.parseInt(args[0]), args[1]).launch();
	}

	private static void usage(){
		System.out.println("Usage : ServerChatHack port passwordfile");
	}

	/***
	 *  Theses methods are here to help understanding the behavior of the selector
	 ***/

	private String interestOpsToString(SelectionKey key){
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
		if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
		if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
		return String.join("|",list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet){
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e){
			return "???";
		}
	}

	public void printSelectedKey(SelectionKey key) {
		SelectableChannel channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			SocketChannel sc = (SocketChannel) channel;
			System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable()) list.add("ACCEPT");
		if (key.isReadable()) list.add("READ");
		if (key.isWritable()) list.add("WRITE");
		return String.join(" and ",list);
	}
}
