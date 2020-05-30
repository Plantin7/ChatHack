package fr.uge.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.frame.AnonymousAuthenticationMessage;
import fr.uge.nonblocking.frame.AuthentiticationMessage;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.PrivateFrame;
import fr.uge.nonblocking.frame.PublicFrame;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.SendPrivateConnection;

public class ClientChatHack {

	// ---------------- SERVER ----------------------
	private final ServerSocketChannel serverSocketChannel;

	static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());
	private static Charset UTF8 = StandardCharsets.UTF_8;

	private final SocketChannel sc;
	private final Selector selector;

	private final Thread console;
	private enum Command {SEND_PRIVATE_MESSAGE, SEND_PRIVATE_FILE, SEND_PUBLIC_MESSAGE, SEND_ACCEPT_PRIVATE_CONNECTION, SEND_REFUSE_PRIVATE_CONNECTION}
	private final ArrayBlockingQueue<Map<Command, String>> commandQueue = new ArrayBlockingQueue<>(10);
	private ClientContext uniqueContext;
	private final HashMap<String, String> myPendingRequest = new HashMap<>();
	private final ArrayList<String> clientAwaitingResponse = new ArrayList<>();

	// params clientChatHack
	private final InetSocketAddress serverAddress;
	private final String login;
	private final String pathRepository;
	private final String password;

	public ClientChatHack(InetSocketAddress serverAddress, String pathRepository, String login, String password) throws IOException {
		this.serverAddress = serverAddress;
		this.login = login;
		this.pathRepository = pathRepository;
		this.password = password;
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
		this.serverSocketChannel = ServerSocketChannel.open();
	}

	private void consoleRun() {
		try (var scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				var line = scan.nextLine();
				if(!line.isEmpty()) {
					var caractere = line.charAt(0);
					switch (caractere) {
					case '@': 
						var privateRequest = line.substring(1).split(" ", 2);
						var lineLogin = privateRequest[0];
						if(!login.equals(lineLogin)) {
							var privateMessage = Collections.singletonMap(Command.SEND_PRIVATE_MESSAGE, line.substring(1));
							sendCommand(privateMessage);
						}
						else {
							System.out.println("Vous essayé de vous parler à vous meme, bizzare");
						}

						break;
					case '/' :
						var accept = line.startsWith("/accept");
						var refuse = line.startsWith("/refuse");
						var login = line.split(" ", 2)[1]; // bug si pas de message !
						if(accept && checkValidRequest(login)) {
							var mapAccept = Collections.singletonMap(Command.SEND_ACCEPT_PRIVATE_CONNECTION, login);
							sendCommand(mapAccept);
						}
						else if(refuse && checkValidRequest(login)) {
							var mapRefuse = Collections.singletonMap(Command.SEND_REFUSE_PRIVATE_CONNECTION, login);
							sendCommand(mapRefuse);
						}
						else {
							//System.out.println("TODO Implement private file");
						}
						break;
					case '!':{
						// DebugMode
						System.out.println("Mes demandes en attente : " + myPendingRequest);
						System.out.println("Les clients en attente d'une réponse : " + clientAwaitingResponse);
						break;
					} 
					default:
						var publicMessage = Collections.singletonMap(Command.SEND_PUBLIC_MESSAGE, line);
						sendCommand(publicMessage);
					}
				}
			}
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	private boolean checkValidRequest(String login) {
		if(clientAwaitingResponse.contains(login)) {
			return true;
		}
		System.out.println("Vous n'avez pas de demande en cours pour le client : " + login);
		return false;
	}


	/**
	 * Send a command to the selector via commandQueue and wake it up
	 *
	 * @param
	 * @throws InterruptedException
	 */
	private void sendCommand(Map<Command, String> cmd) throws InterruptedException {
		synchronized (commandQueue) {
			commandQueue.put(cmd);
			selector.wakeup();
		}
	}

	/**
	 * Processes the command from commandQueue
	 */

	private void processCommands() {
		while(true) {
			synchronized (commandQueue) {
				var cmdMap = commandQueue.poll();
				if(cmdMap == null) { return;}
				var cmd = cmdMap.keySet().iterator().next();
				var line = cmdMap.get(cmd);

				switch (cmd) {
				case SEND_PRIVATE_MESSAGE: {
					var privateRequest = line.split(" ", 2);
					var login = privateRequest[0];
					// TODO A changer !
					if(!myPendingRequest.containsKey(login)) {
						var message = privateRequest[1];
						myPendingRequest.put(login, message);
						uniqueContext.queueMessage(new RequestPrivateConnection(login, message).asByteBuffer());
					}
					else {
						System.out.println("Le client " + "\"" + login + "\"" + " n'a pas encore répondu à votre demande !");
					}
					break;
				}
				case SEND_ACCEPT_PRIVATE_CONNECTION : {
					var port = serverSocketChannel.socket().getLocalPort();
					var hostName = serverSocketChannel.socket().getInetAddress().getHostName();
					var socketAddress = new InetSocketAddress(hostName, port);
					var connectId = generateConnectId();
					uniqueContext.queueMessage(new AcceptPrivateConnection(line, socketAddress, connectId).asByteBuffer());
					clientAwaitingResponse.remove(line);
					//TODO
					break;
				}
				case SEND_REFUSE_PRIVATE_CONNECTION : {
					uniqueContext.queueMessage(new RefusePrivateConnection(line).asByteBuffer());
					clientAwaitingResponse.remove(line);
					break;
				}
				case SEND_PUBLIC_MESSAGE : 
					uniqueContext.queueMessage(new PublicMessage(login, line).asByteBuffer());
					break;
				}
			}
		}
	}

	private long generateConnectId() {
		return new Random().nextLong();
	} 

	public void launch() throws IOException {
		sc.connect(serverAddress); // Blocking Mode
		// Waiting the response of authentication
		if(!password.isEmpty()) {
			getAuthenticationResponse(new AuthentiticationMessage(login, password), serverAddress);
		}
		else {
			getAuthenticationResponse(new AnonymousAuthenticationMessage(login), serverAddress);
		}
		// configure the client to Non Blocking Mode
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_READ);
		uniqueContext = new ClientContext(this, key);
		key.attach(uniqueContext);

		// ------------ Server ------------------------- //
		serverSocketChannel.bind(new InetSocketAddress(0));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		console.setDaemon(true);
		console.start();

		while (!Thread.interrupted()) {
			try {
				if(uniqueContext.isClosed()) {
					selector.close();
					return;
				}
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}


	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch(IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isConnectable()) {
				if(key.attachment() instanceof ClientContext) {
					uniqueContext.doConnect();
				}
				if(key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doConnect();
				}
			}
			if (key.isValid() && key.isWritable()) {
				if(key.attachment() instanceof ClientContext) {
					uniqueContext.doWrite();
				}
				if(key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doWrite();
				}
			}
			if (key.isValid() && key.isReadable()) {
				if(key.attachment() instanceof ClientContext) {
					uniqueContext.doRead();
				}
				if(key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doRead();
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
		var context = new ClientPrivateContext(this, client);
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
	 * Fill the workspace of the Bytebuffer with bytes read from sc.
	 *
	 * @param sc
	 * @param bb
	 * @return false if read returned -1 at some point and true otherwise
	 * @throws IOException
	 */
	static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while(bb.hasRemaining()) {
			if(sc.read(bb) == -1) {
				return false;
			}
		}
		return true;
	}

	private void getAuthenticationResponse(PublicFrame request, SocketAddress server) throws IOException {
		var bbToSend = request.asByteBuffer();
		sc.write(bbToSend);
		
		var bbResponseSize = ByteBuffer.allocate(Integer.BYTES);
		if(!readFully(sc, bbResponseSize)) {
			return;
		}
		var bbResponse = ByteBuffer.allocate(bbResponseSize.flip().getInt());
		if(!readFully(sc, bbResponse)) {
			return;
		}
		bbResponse.flip();
		System.out.println(UTF8.decode(bbResponse));
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length == 4) {
			new ClientChatHack(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2], args[3], "").launch();
		}
		else if(args.length == 5) {
			new ClientChatHack(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2], args[3], args[4]).launch();
		}
		else {
			usage();
			return;
		}

	}

	private static void usage() {
		System.out.println("Usage : ClientChatHack - hostname - port - path_file - login - (password)");
	}
	
	//***************************************** MANAGE CLIENT VISITOR METHODS *********************************************//
	
	public void displayFrameDialog(PublicFrame frame) {
		System.out.println(frame);
	}

	public void displayFrameDialog(PrivateFrame privateMessage) {
		System.out.println(privateMessage);
	}

	public void errorPendingPrivateConnectionRequest(ErrorPrivateConnection errorPrivateConnection) {
		displayFrameDialog(errorPrivateConnection);
		myPendingRequest.remove(errorPrivateConnection.getLogin());
	}

	public void manageRequestPrivateConnection(SendPrivateConnection sendPrivateConnection) {
		displayFrameDialog(sendPrivateConnection);
		clientAwaitingResponse.add(sendPrivateConnection.getLogin());
	}

	public void manageRefusePrivateConnection(RefusePrivateConnection refusePrivateConnection) {
		displayFrameDialog(refusePrivateConnection);
		myPendingRequest.remove(refusePrivateConnection.getLogin());
	}

	public void manageAcceptPrivateConnection(AcceptPrivateConnection acceptPrivateConnection) {
		displayFrameDialog(acceptPrivateConnection);
		var message = myPendingRequest.remove(acceptPrivateConnection.getLogin());

		try {
			var sc = SocketChannel.open();
			sc.configureBlocking(false);
			var clientKey = sc.register(selector, SelectionKey.OP_CONNECT);
			var ctx = new ClientPrivateContext(this, clientKey);
			clientKey.attach(ctx);
			sc.connect(acceptPrivateConnection.getSocketAddress());
		}
		catch(IOException e) {
			//
		}
	}
	
}
