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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.frame.DB;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.frame.StringMessage;
import fr.uge.nonblocking.server.context.DBContext;
import fr.uge.nonblocking.server.context.ServerContext;
import fr.uge.nonblocking.server.context.ServerContext.ConnectionTypes;
import fr.uge.protocol.ChatHackProtocol;

public class ServerChatHack {

	static private Logger logger = Logger.getLogger(ServerChatHack.class.getName());
	/* -------------------------------- CLIENT CHAT HACK ----------------------------------------*/
	private final InetSocketAddress serverDB;
	private final SocketChannel socketChannel;
	private DBContext dbContext;
	private final static Charset UTF8 = StandardCharsets.UTF_8;

	/* -------------------------------- SERVER CHAT HACK ----------------------------------------*/
	private final ServerSocketChannel serverSocketChannel;
	private final HashMap<String, ServerContext> map = new HashMap<>();
	private final Selector selector;
	private SelectionKey serverKey;
	private long id = 0;

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
		var dbKey = socketChannel.register(selector, SelectionKey.OP_READ);
		dbContext = new DBContext(this, dbKey);
		dbKey.attach(dbContext);

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
				if(key == dbContext.getKey()) {
					((DBContext) key.attachment()).doWrite();
				}
				else {
					((ServerContext) key.attachment()).doWrite();
				}
			}
			if (key.isValid() && key.isReadable()) {
				if(key == dbContext.getKey()) {
					((DBContext) key.attachment()).doRead();
				}
				else {
					((ServerContext) key.attachment()).doRead();
				}
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
		var context = new ServerContext(this, client, id);
		client.attach(context);
		id++;
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		var context = (ServerContext)key.attachment();
		try {
			deleteElementFromId(context.getId());
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
			if(key.isValid() && key != serverKey && key != dbContext.getKey()) { // On ne veut pas du server et du server db, on veut uniquement les clients
				var context = (ServerContext)key.attachment();
				context.queueMessage(msg.duplicate());
			}
		}
	}

	public void sendAuthentificationToDB(AuthentificationMessage auth, ServerContext context) {
		if(!map.containsKey(auth.getLogin())) {
			map.put(auth.getLogin(), context);
			var id = context.getId();
			var bbLogin = UTF8.encode(auth.getLogin());
			context.setConnectionTypeValidated();
			var bbPassword = UTF8.encode(auth.getPassword());
			var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + 2 * Integer.BYTES + bbLogin.limit() + bbPassword.limit());
			bb.put(ChatHackProtocol.OPCODE_ASK_AUTH_TO_DB_WITH_PASWWORD).putLong(id).putInt(bbLogin.limit()).put(bbLogin).putInt(bbPassword.limit()).put(bbPassword).flip();
			dbContext.queueMessage(bb);			
		}
		else {
			context.queueMessage(new ResponseAuthentification("Login already in use").asByteBuffer());
			context.silentlyInputClose();
		}
	}

	public void sendAnonymousAuthentificationToDB(StringMessage auth, ServerContext context) {
		if(!map.containsKey(auth.getStringMessage())) {
			map.put(auth.getStringMessage(), context); // String Message = login
			var id = context.getId();
			var bbLogin = UTF8.encode(auth.getStringMessage());
			context.setConnectionTypeAnonymous();
			var bb = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + Integer.BYTES + bbLogin.limit());
			bb.put(ChatHackProtocol.OPCODE_ASK_AUTH_TO_DB_WITHOUT_PASSWORD).putLong(id).putInt(bbLogin.limit()).put(bbLogin).flip();
			dbContext.queueMessage(bb);			
		}
		else {
			context.queueMessage(new ResponseAuthentification("Login already in use").asByteBuffer());
			context.silentlyInputClose();
		}
	}

	public void sendToClientResponseOfDB(DB dbResponse) {
		var clientContext = searchContextFromID(dbResponse.getId()).get();
		var connectionType = clientContext.getConnectionTypes();
		System.out.println("ConnnectionType " + connectionType);
		ResponseAuthentification response;
		if(connectionType == ConnectionTypes.CONNECTION_VALIDATED) {
			if(dbResponse.getOpcode() == 1) {
				response = new ResponseAuthentification("Connected");
			}
			else if (dbResponse.getOpcode() == 0) {
				response = new ResponseAuthentification("Login or password incorrect !");
				clientContext.silentlyInputClose();
			}
			else {
				throw new IllegalStateException("The DB Server doesn't respect the protocol");
			}
		}
		else if(connectionType == ConnectionTypes.CONNECTION_ANONYMOUS) {
			if(dbResponse.getOpcode() == 1) { // Login dans la base de donnée
				response = new ResponseAuthentification("You tried to connect with an already existing account");
				clientContext.silentlyInputClose();
			}
			else if (dbResponse.getOpcode() == 0) {
				response = new ResponseAuthentification("Connected (anonymous)");
			}
			else {
				throw new IllegalStateException("The DB Server doesn't respect the protocol");
			}
		}
		else {
			throw new IllegalStateException("The DB Server doesn't respect the protocol");
		}

		clientContext.queueMessage(response.asByteBuffer());

	}
	// BAD NAME BURK CEST PAS BEAU
	public void sendPrivateConnectionRequestToClient(RequestPrivateConnection requestPrivateConnection, ServerContext ctx) {
		var contextDest = map.get(requestPrivateConnection.getLogin());
		var loginDest = requestPrivateConnection.getLogin();
		if(contextDest != null) {
			var expeditor = getLoginFromId(ctx.getId()).get();
			var response = "The " + "\"" + expeditor + "\"" + " client wants to send you a message. Do you accept ? \n@accept " + expeditor + "\n@refuse " + expeditor;
			contextDest.queueMessage(new ResponseAuthentification(response).asByteBuffer());
		}
		else {
			ctx.queueMessage(new ErrorPrivateConnection(loginDest).asByteBuffer());
		}
	}

	private Optional<ServerContext> searchContextFromID(long id) {
		for(var entry : map.entrySet()){
			var tmpId = map.get(entry.getKey()).getId();
			if(tmpId == id) {
				return Optional.of(map.get(entry.getKey()));
			}
		}
		return Optional.empty();
	}
	
	private Optional<String> getLoginFromId(long id){
		for(var entry : map.entrySet()){
			var tmpId = map.get(entry.getKey()).getId();
			if(tmpId == id) {
				return Optional.of(entry.getKey());
			}
		}
		return Optional.empty();
	}
	
	public void deleteElementFromId(long id){
		var login = getLoginFromId(id);
		if(login.isPresent()) {
			map.remove(login.get());
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
