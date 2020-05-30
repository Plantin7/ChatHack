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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.nonblocking.client.PendingRequestInfo.State;
import fr.uge.nonblocking.client.commands.*;
import fr.uge.nonblocking.client.commands.PublicMessageCommand;
import fr.uge.nonblocking.frame.*;
import fr.uge.nonblocking.client.commands.CommandVisitor;

public class ClientChatHack implements CommandVisitor {

	// ---------------- SERVER ----------------------
	private final ServerSocketChannel serverSocketChannel;

	static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());
	private static Charset UTF8 = StandardCharsets.UTF_8;

	private final SocketChannel sc;
	private final Selector selector;

	private final Thread console;

	private final ArrayBlockingQueue<Commands> commandArrayBlockingQueue = new ArrayBlockingQueue<>(10);
	private ClientContext uniqueContext;
	final HashMap<String, PendingRequestInfo> myPendingRequest = new HashMap<>(); // Mes Demandes
	private final ArrayList<String> clientAwaitingResponse = new ArrayList<>(); // Les demandes des autres

	// params clientChatHack
	private final InetSocketAddress serverAddress;
	final String login;
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
				if (!line.isEmpty()) {
					var caractere = line.charAt(0);
					switch (caractere) {
					case '@':
						var privateRequest = line.substring(1).split(" ", 2);
						var lineLogin = privateRequest[0];
						if (!login.equals(lineLogin)) {
							var privateMessageCommand = new PrivateMessageCommand(lineLogin, line.substring(1));
							sendCommand(privateMessageCommand);
						} else {
							System.out.println("Vous essayé de vous parler à vous meme, bizzare");
						}

						break;
					case '/':
						var accept = line.startsWith("/accept");
						var refuse = line.startsWith("/refuse");
						var login = line.split(" ", 2)[1]; // bug si pas de message !
						if (accept && checkValidRequest(login)) {
							sendCommand(new AcceptPrivateConnectionCommand(login));
						} else if (refuse && checkValidRequest(login)) {
							sendCommand(new RefusePrivateConnectionCommand(login));
						} else {
							//System.out.println("TODO Implement private file");
						}
						break;
					case '!': {
						// DebugMode
						System.out.println("Mes demandes en attente : " + myPendingRequest);
						System.out.println("Les clients en attente d'une réponse : " + clientAwaitingResponse);
						break;
					}
					default:
						sendCommand(new PublicMessageCommand(line));
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
		if (clientAwaitingResponse.contains(login)) {
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
	private void sendCommand(Commands cmd) throws InterruptedException {
		/*
		synchronized (commandQueue) {
			commandQueue.put(cmd);
			selector.wakeup();
		}
		 */
		synchronized (commandArrayBlockingQueue) {
			commandArrayBlockingQueue.put(cmd);
			selector.wakeup();
		}

	}

	/**
	 * Processes the command from commandQueue
	 */

	private void processCommands() {
		while (true) {
			synchronized (commandArrayBlockingQueue) {
				var cmd = commandArrayBlockingQueue.poll();
				if (cmd == null) {
					return;
				}
				cmd.accept(this);
			}
		}
	}

	private long generateConnectId() {
		return new Random().nextLong();
	}

	public void launch() throws IOException {
		sc.connect(serverAddress); // Blocking Mode
		// Waiting the response of authentication
		if (!password.isEmpty()) {
			getAuthenticationResponse(new AuthentiticationMessage(login, password), serverAddress);
		} else {
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
				if (uniqueContext.isClosed()) {
					selector.close();
					return;
				}
				selector.select(this::treatKey);
				processCommands();
				for(var sKey : selector.keys()) {
					if(!sKey.isValid()) {
						myPendingRequest.entrySet()
										.stream()
										.filter(k -> k.getValue().privateContext.getKey().equals(sKey))
										.findFirst()
										.ifPresent(pending -> myPendingRequest.remove(pending.getKey()));
					}
				}
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
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isConnectable()) {
				if (key.attachment() instanceof ClientContext) {
					uniqueContext.doConnect();
				}
				if (key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doConnect();
				}
			}
			if (key.isValid() && key.isWritable()) {
				if (key.attachment() instanceof ClientContext) {
					uniqueContext.doWrite();
				}
				if (key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doWrite();
				}
			}
			if (key.isValid() && key.isReadable()) {
				if (key.attachment() instanceof ClientContext) {
					uniqueContext.doRead();
				}
				if (key.attachment() instanceof ClientPrivateContext) {
					((ClientPrivateContext) key.attachment()).doRead();
				}
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
			silentlyClose(key);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		var sc = serverSocketChannel.accept();
		if (sc == null) {
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
		while (bb.hasRemaining()) {
			if (sc.read(bb) == -1) {
				return false;
			}
		}
		return true;
	}

	private void getAuthenticationResponse(PublicFrame request, SocketAddress server) throws IOException {
		var bbToSend = request.asByteBuffer();
		sc.write(bbToSend);

		var bbResponseSize = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbResponseSize)) {
			return;
		}
		var bbResponse = ByteBuffer.allocate(bbResponseSize.flip().getInt());
		if (!readFully(sc, bbResponse)) {
			return;
		}
		bbResponse.flip();
		System.out.println(UTF8.decode(bbResponse));
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length == 4) {
			new ClientChatHack(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2], args[3], "").launch();
		} else if (args.length == 5) {
			new ClientChatHack(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2], args[3], args[4]).launch();
		} else {
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

	// Je fais une demade
	public void manageRequestPrivateConnection(SendPrivateConnection sendPrivateConnection) {
		displayFrameDialog(sendPrivateConnection);
		clientAwaitingResponse.add(sendPrivateConnection.getLogin());
	}

	// Je refuse une demande
	public void manageRefusePrivateConnection(RefusePrivateConnection refusePrivateConnection) {
		displayFrameDialog(refusePrivateConnection);
		myPendingRequest.remove(refusePrivateConnection.getLogin());
	}

	// J'accepte la demande
	public void manageAcceptPrivateConnection(AcceptPrivateConnection acceptPrivateConnection) {
		displayFrameDialog(acceptPrivateConnection);

		try {
			var sc = SocketChannel.open();
			sc.configureBlocking(false);
			var clientKey = sc.register(selector, SelectionKey.OP_CONNECT);
			var ctx = new ClientPrivateContext(this, clientKey);
			clientKey.attach(ctx);
			ctx.privateLogin = acceptPrivateConnection.getLogin();
			var pendingInfo = myPendingRequest.get(acceptPrivateConnection.getLogin());
			pendingInfo.connect_id = acceptPrivateConnection.getConnectId();
			pendingInfo.privateContext = ctx;
			sc.connect(acceptPrivateConnection.getSocketAddress());
		} catch (IOException e) {
			//
		}
	}

	public void checkConnectId(ConfirmationPrivateConnection confirmationPrivateConnection, ClientPrivateContext ctx) {
		var connectIdFromClient = confirmationPrivateConnection.getConnect_id();
		var pendingInfo = myPendingRequest.get(confirmationPrivateConnection.getLogin());
		var connectIdFromMap = pendingInfo.connect_id;
		pendingInfo.privateContext = ctx;
		if(connectIdFromClient == connectIdFromMap) {
			pendingInfo.state = State.REQUEST_DONE;
			pendingInfo.privateContext.queueMessage(new RequestConfirmationIsValid(login).asByteBuffer());
			System.out.println("VERIFICATION --> OK");
		}
		else {
			pendingInfo.privateContext.silentlyClose();
			myPendingRequest.remove(confirmationPrivateConnection.getLogin());
			System.out.println("VERIFICATION --> Erreur");
		}
	}

	public void receivedValidationOfConfirmation(RequestConfirmationIsValid requestConfirmationIsValid, ClientPrivateContext ctx) {
		var pendingInfo = myPendingRequest.get(requestConfirmationIsValid.getLogin());
		pendingInfo.state = State.REQUEST_DONE;
		pendingInfo.privateContext = ctx;
		pendingInfo.pendingMessage.forEach(message -> {
			pendingInfo.privateContext.queueMessage(message);
		});
		pendingInfo.clearPendingMessage();
	}


	//***************************************** MANAGE CLIENT COMMANDS VISITOR  *********************************************//

	@Override
	public void visit(PublicMessageCommand publicMessageCommand) {
		uniqueContext.queueMessage(new PublicMessage(login, publicMessageCommand.getLine()).asByteBuffer());
	}

	@Override
	public void visit(PrivateMessageCommand privateMessageCommand) {
		var privateRequest = privateMessageCommand.getMessage().split(" ", 2);
		var requestLogin = privateRequest[0];
		if (!myPendingRequest.containsKey(requestLogin)) {
			var message = privateRequest[1];

			var requestInfo = new PendingRequestInfo();
			requestInfo.add(new PrivateMessage(this.login, message).asByteBuffer());
			myPendingRequest.put(requestLogin, requestInfo);

			uniqueContext.queueMessage(new RequestPrivateConnection(requestLogin, message).asByteBuffer());
		} else {
			checkPendingState(myPendingRequest.get(requestLogin).state, requestLogin, privateRequest[1]);
		}
	}

	private void checkPendingState(State pendingState, String requestLogin, String message) {
		switch (pendingState) {
		case REQUEST_DONE: {
			var privateContext = myPendingRequest.get(requestLogin).privateContext;
			privateContext.queueMessage(new PrivateMessage(login, message).asByteBuffer());
			break;
		}
		case REQUEST_PENDING : {
			var info = myPendingRequest.get(requestLogin);
			info.add(new PrivateMessage(this.login, message).asByteBuffer());
			System.out.println("Le client " + "\"" + requestLogin + "\"" + " n'a pas encore répondu à votre demande !");
			break;
		}
		case REQUEST_REFUSE: {
			myPendingRequest.remove(requestLogin);
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + pendingState);
		}

	}

	@Override
	public void visit(FileMessageCommand fileMessage) {
		System.out.println("visit FileMessageCommand");
	}

	@Override
	public void visit(AcceptPrivateConnectionCommand acceptPrivateConnectionCommand) {
		var port = serverSocketChannel.socket().getLocalPort();
		var hostName = serverSocketChannel.socket().getInetAddress().getHostName();
		var socketAddress = new InetSocketAddress(hostName, port);
		var connectId = generateConnectId();

		var pendingInfo = new PendingRequestInfo();
		pendingInfo.connect_id = connectId;

		myPendingRequest.put(acceptPrivateConnectionCommand.getLogin(), pendingInfo);
		uniqueContext.queueMessage(new AcceptPrivateConnection(acceptPrivateConnectionCommand.getLogin(), socketAddress, connectId).asByteBuffer());
		clientAwaitingResponse.remove(acceptPrivateConnectionCommand.getLogin());
	}

	@Override
	public void visit(RefusePrivateConnectionCommand refusePrivateConnectionCommand) {
		uniqueContext.queueMessage(new RefusePrivateConnection(refusePrivateConnectionCommand.getLogin()).asByteBuffer());
		clientAwaitingResponse.remove(refusePrivateConnectionCommand.getLogin());
	}

}
