package fr.uge.nonblocking.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.database.RequestAnonymousAuthentication;
import fr.uge.nonblocking.database.RequestAuthenticationWithPassword;
import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.frame.AnonymousAuthenticationMessage;
import fr.uge.nonblocking.frame.AuthentiticationMessage;
import fr.uge.nonblocking.frame.DB;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.frame.SendPrivateConnection;
import fr.uge.nonblocking.server.ServerContext.ConnectionTypes;

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

	//***************************************** MANAGE SERVER VISITOR METHODS *********************************************//

	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	public void broadcast(ByteBuffer msg, ServerContext ctx) {
		if(!ctx.isConnected()) {
			ctx.queueMessage(new ResponseAuthentification("You are not connected !").asByteBuffer());
			ctx.silentlyClose();
			return;
		}
		for (var key : selector.keys()) { 
			if(key.isValid() && key != serverKey && key != dbContext.getKey()) { // On ne veut pas du server et du server db, on veut uniquement les clients
				var context = (ServerContext)key.attachment();
				context.queueMessage(msg.duplicate());
			}
		}
	}

	public void sendAuthentificationToDB(AuthentiticationMessage auth, ServerContext context) {
		if(!map.containsKey(auth.getLogin())) {
			map.put(auth.getLogin(), context);
			context.setConnectionTypeValidated();
			dbContext.queueMessage(new RequestAuthenticationWithPassword(context.getId(), auth.getLogin(), auth.getPassword()).asByteBuffer());			
		}
		else {
			context.queueMessage(new ResponseAuthentification("Login already in use").asByteBuffer());
			context.silentlyInputClose();
		}
	}

	public void sendAnonymousAuthentificationToDB(AnonymousAuthenticationMessage auth, ServerContext context) {
		if(!map.containsKey(auth.getLogin())) {
			map.put(auth.getLogin(), context);
			context.setConnectionTypeAnonymous();
			dbContext.queueMessage(new RequestAnonymousAuthentication(context.getId(), auth.getLogin()).asByteBuffer());			
		}
		else {
			context.queueMessage(new ResponseAuthentification("Login already in use").asByteBuffer());
			context.silentlyInputClose();
		}
	}

	public void sendToClientResponseOfDB(DB dbResponse) {
		var clientContext = searchContextFromID(dbResponse.getId()).get();
		var connectionType = clientContext.getConnectionTypes();
		ResponseAuthentification response;
		if(connectionType == ConnectionTypes.CONNECTION_VALIDATED) {
			if(dbResponse.getOpcode() == 1) {
				response = new ResponseAuthentification("Connected");
				clientContext.setStatusConnection(true);
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
				clientContext.setStatusConnection(true);
			}
			else {
				throw new IllegalStateException("The DB Server doesn't respect the protocol");
			}
		}
		else {
			throw new IllegalStateException("The DB Server doesn't respect the protocol");
		}

		clientContext.queueMessage(response.asByteBuffer()); //response.asByteBuffer()

	}

	public void sendPrivateConnectionRequestToClient(RequestPrivateConnection requestPrivateConnection, ServerContext ctx) {
		if(!ctx.isConnected()) {
			ctx.queueMessage(new ResponseAuthentification("You are not connected !").asByteBuffer());
			ctx.silentlyClose();
			return;
		}
		var loginDest = requestPrivateConnection.getLogin();
		var contextDest = map.get(loginDest);
		if(contextDest != null) {
			var expeditor = getLoginFromId(ctx.getId()).get();
			contextDest.queueMessage(new SendPrivateConnection(expeditor).asByteBuffer());
		}
		else {
			ctx.queueMessage(new ErrorPrivateConnection(loginDest).asByteBuffer());
		}
	}

	public void sendRefuseRequestConnectionToClient(RefusePrivateConnection refusePrivateConnection, ServerContext ctx) {
		if(!ctx.isConnected()) {
			ctx.queueMessage(new ResponseAuthentification("You are not connected !").asByteBuffer());
			ctx.silentlyClose();
			return;
		}
		var loginDest = refusePrivateConnection.getLogin();
		var contextDest = map.get(loginDest);
		if(contextDest != null) {
			var login = getLoginFromId(ctx.getId()).get();
			contextDest.queueMessage(new RefusePrivateConnection(login).asByteBuffer());
		}
		else {
			System.out.println("RefuseConnection : ON NE DOIT PAS RENTRER ICI" );
		}
	}

	public void sendAcceptRequestConnectionToClient(AcceptPrivateConnection acceptPrivateConnection, ServerContext ctx) {
		if(!ctx.isConnected()) {
			ctx.queueMessage(new ResponseAuthentification("You are not connected !").asByteBuffer());
			ctx.silentlyClose();
			return;
		}
		var loginDest = acceptPrivateConnection.getLogin();
		var contextDest = map.get(loginDest);
		if(contextDest != null) {
			var expeditor = getLoginFromId(ctx.getId()).get();
			var socketAddress = acceptPrivateConnection.getSocketAddress();
			var connectId = acceptPrivateConnection.getConnectId();
			contextDest.queueMessage(new AcceptPrivateConnection(expeditor, socketAddress, connectId).asByteBuffer());
		}
		else {
			System.out.println("RefuseConnection : ON NE DOIT PAS RENTRER ICI" );
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

}
