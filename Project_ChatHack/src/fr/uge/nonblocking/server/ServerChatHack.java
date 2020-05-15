package fr.uge.nonblocking.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.readers.complexReader.OPMessage;
import fr.uge.nonblocking.server.context.ServerContext;

public class ServerChatHack {
	
	static private Logger logger = Logger.getLogger(ServerChatHack.class.getName());
	/* -------------------------------- CLIENT CHAT HACK ----------------------------------------*/
	private final InetSocketAddress serverDB;
	private final SocketChannel socketChannel;
	private SelectionKey dbKey;
	private final static Charset UTF8 = StandardCharsets.UTF_8;

	/* -------------------------------- SERVER CHAT HACK ----------------------------------------*/
	private final ServerSocketChannel serverSocketChannel;
	private final HashMap<Long, SelectionKey> map = new HashMap<>();
	private final Selector selector;
	private SelectionKey serverKey;

	public ServerChatHack(int port, InetSocketAddress serverDB) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		this.serverDB = serverDB;
		this.socketChannel = SocketChannel.open();
	}
	
	public void launch() throws IOException {
		
		serverSocketChannel.configureBlocking(false);
		serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		socketChannel.connect(serverDB);
		socketChannel.configureBlocking(false);
		dbKey = socketChannel.register(selector, SelectionKey.OP_READ);
		//enregitre mon contexte

		
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
				((ServerContext) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ServerContext) key.attachment()).doRead();
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
		var context = new ServerContext(this, client);
		client.attach(context);
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
	public void broadcast(ByteBuffer msg) {
		for (var key : selector.keys()) { 
			if(key.isValid() && key != serverKey && key != dbKey) { // On ne veut pas du server et du server db, on veut uniquement les clients
				var context = (ServerContext)key.attachment();
				context.queueMessage(msg.duplicate());
			}
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 3){
			usage();
			return;
		}
		new ServerChatHack(Integer.parseInt(args[0]), new InetSocketAddress(args[1], Integer.parseInt(args[2]))).launch();
	}

	private static void usage(){
		System.out.println("Usage : ServerChatHack portClient + hostname port server DB ");
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
