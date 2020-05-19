package fr.uge.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.nonblocking.client.context.ClientContext;
import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.Frame;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.server.context.ServerContext;

public class ClientChatHack {

	static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());


	private final SocketChannel sc;
	private final Selector selector;

	private final Thread console;
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private enum Command {SEND_PRIVATE_MESSAGE, SEND_PRIVATE_FILE, SEND_PUBLIC_MESSAGE}
	private final ArrayBlockingQueue<Map<Command, String>> commandQueue = new ArrayBlockingQueue<>(10);
	private ClientContext uniqueContext;
	private final HashMap<String, String> pendingRequest = new HashMap<>();

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
	}

	private void consoleRun() {
		try (var scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				var line = scan.nextLine();
				if(!line.isEmpty()) {
					var caractere = line.charAt(0);
					switch (caractere) {
					case '@': 
						var privateMessage = Collections.singletonMap(Command.SEND_PRIVATE_MESSAGE, line.substring(1));
						sendCommand(privateMessage);
						break;
					case '/' : 
						System.out.println("TODO Implement private file message");
						//var privateFile = Collections.singletonMap(Command.SEND_PRIVATE_FILE, line.substring(1));
						//sendCommand(privateFile);
						break;
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
					if(!pendingRequest.containsKey(login)) {
						var message = privateRequest[1];
						pendingRequest.put(login, message);
						uniqueContext.queueMessage(new RequestPrivateConnection(login, message).asByteBuffer());
					}
					else {
						System.out.println("Le client " + "\"" + login + "\"" + " n'a pas encore répondu à votre demande !");
					}
					break;
				}
				case SEND_PUBLIC_MESSAGE : 
					uniqueContext.queueMessage(new PublicMessage(login, line).asByteBuffer());
					break;
				}
			}
		}
	}

	public void launch() throws IOException {
		/*sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		uniqueContext = new ClientContext(this, key);
		key.attach(uniqueContext);
		sc.connect(serverAddress);*/
		
		sc.connect(serverAddress);
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_READ);
		uniqueContext = new ClientContext(this, key);
		key.attach(uniqueContext);
		uniqueContext.queueMessage(new AuthentificationMessage(login, password).asByteBuffer());

		console.setDaemon(true);
		console.start();

		while (!Thread.interrupted()) {
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				uniqueContext.doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				uniqueContext.doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				uniqueContext.doRead();
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
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
	
	public void displayFrameDialog(Frame frame) {
		System.out.println(frame);
	}
	
	public void errorPendingPrivateConnectionRequest(ErrorPrivateConnection errorPrivateConnection) {
		displayFrameDialog(errorPrivateConnection);
		pendingRequest.remove(errorPrivateConnection.getLogin());
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
}
