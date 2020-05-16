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
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.nonblocking.client.context.ClientContext;
import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.server.context.ServerContext;

public class ClientChatHack {

	static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());


	private final SocketChannel sc;
	private final Selector selector;

	private final Thread console;
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private enum Command {AUTH, MESSAGE}
	private final ArrayBlockingQueue<Command> commandQueue = new ArrayBlockingQueue<>(10);
	private ClientContext uniqueContext;

	// params clientChatHack
	private final InetSocketAddress serverAddress;
	private final String login;
	private final String pathRepository;
	private final String password;

	private enum StateConnection {CONNECTED, DISCONNECTED};
	private enum error {INCORRECT_LOGIN, LOGIN_IN_USE, LOGIN_ALREADY_INTO_DB}
	private StateConnection stateConnection;

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
				switch (line) {
				case "AUTH": 
					sendCommand(Command.AUTH);
					break;
				case "MESSAGE" : 
					sendCommand(Command.MESSAGE);
					break;
				default:
					System.out.println("Commande Invalide !");
				}
			}
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	/**
	 * 
                if(stateConnection == StateConnection.DISCONNECTED) { // if client is not connected
                    if (scan.toString().toUpperCase().equals("AUTH")) {
                        sendAuthentification(login, password);
                    }
                } else {
                    switch (scan.next().toUpperCase()) {
                        case "/":
                        case "@":
                            var msg = scan.nextLine();
                            sendCommand(msg);
                            break;
                        case "@login":
                            // TODO : private message
                            break;
                        case "/login file":
                            //TODO : private file
                        default:
                            throw new IllegalArgumentException("Unexpected command: " + scan.next());
                    }
                }
                // stocker la correspondance entre le login et le contexte : map
                // je donne tel id à tel contexte
                // L'objet Context ne sera pas le même
                // Client  1 -> négocier le login avec le serveur
                // selecteur --> le même selecteur qui surveille toute les connexions ()
                // filtrer si on est connecté ou non
                //  traiter le paquet ou pas
	 * 
	 * 
	 * 
	 * 
	 * 
	 * */
	/**
	 * Send a command to the selector via commandQueue and wake it up
	 *
	 * @param
	 * @throws InterruptedException
	 */
	private void sendCommand(Command cmd) throws InterruptedException {
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
				var cmd = commandQueue.poll();
				if(cmd == null) { return; }
				switch (cmd) {
				case AUTH: 
					//uniqueContext.queueMessage(ClientCommands.requestAuthentication(login, password));
					uniqueContext.queueMessage(new AuthentificationMessage(login, password).asByteBuffer());
					break;
				case MESSAGE : 
					uniqueContext.queueMessage(new PublicMessage(login, "Bonjour toi !").asByteBuffer());
					break;
				}
			}
		}
	}
	
	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		uniqueContext = new ClientContext(this, key);
		key.attach(uniqueContext);
		sc.connect(serverAddress);
		
		// CLEINT 
		// Pour eviter la negotitation !
		// write
		// readfully
		// non bloquant 

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
	
	public void displayDialog(PublicMessage publicMessage) {
		System.out.println("-------------------------------");
		System.out.println(publicMessage);
	}

	public void displayAuthentification(AuthentificationMessage authentificationMessage) {
		System.out.println(authentificationMessage);
	}


	
	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 4) {
			usage();
			return;
		}
		new ClientChatHack(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[2], args[3], " ").launch();
	}

	private static void usage() {
		System.out.println("Usage : ClientChatHack hostname port path_file login password(optional)");
	}
}
