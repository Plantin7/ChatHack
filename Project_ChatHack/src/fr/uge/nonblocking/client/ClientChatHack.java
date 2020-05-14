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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.nonblocking.readers.Reader.ProcessStatus;
import fr.uge.nonblocking.readers.complexReader.MessageReader;
import fr.uge.nonblocking.readers.complexReader.OPMessageReader;
import fr.uge.nonblocking.readers.complexReader.ResponseServer;
import fr.uge.nonblocking.readers.complexReader.ResponseServerReader;
import fr.uge.nonblocking.client.context.ClientContext;

public class ClientChatHack {
	
    static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());


    private final SocketChannel sc;
    private final Selector selector;

    private final Thread console;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private ClientContext uniqueContext;

    // params clientChatHack
    private final InetSocketAddress serverAddress;
    private final String login;
    private final String pathRepository;
    private final String password;

    private enum StateConnection {CONNECTED, DISCONNECTED};
    private enum error {INCORRECT_LOGIN, LOGIN_IN_USE, LOGIN_ALREADY_INTO_DB}
    private StateConnection stateConnection;

    private static final byte OP_CONNECTION_WITH_MDP = 1;
    private static final byte OP_CONNECTION_NO_MDP = 2;

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
     * @param msg
     * @throws InterruptedException
     */
    private void sendCommand(String msg) throws InterruptedException {
        synchronized (commandQueue) {
            commandQueue.put(msg);
            selector.wakeup();
        }
    }

    private void sendAuthentification(String login, String password) {
        var bbLogin = UTF8.encode(login);
        var sizeLogin = bbLogin.remaining();
        ByteBuffer bufferAuth;
        if (!password.isEmpty()) {
            var bbPassword = UTF8.encode(password);
            var sizePassWord = bbPassword.remaining();
            bufferAuth = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + sizeLogin + Integer.BYTES + sizePassWord);
            bufferAuth.put(OP_CONNECTION_WITH_MDP).putInt(sizeLogin).put(bbLogin).putInt(sizePassWord).put(bbPassword);
        } else {
            bufferAuth = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + sizeLogin);
            bufferAuth.put(OP_CONNECTION_NO_MDP).putInt(sizeLogin).put(bbLogin);
        }
        synchronized (this.commandQueue) {
            uniqueContext.queueMessage(bufferAuth);
            this.selector.wakeup();
        }

    }


    private void checkAuthentification() throws IOException {
        var bbLogin = UTF8.encode(login);
        var bbMDP = UTF8.encode("€€€€€"); // Etienne login
        var sizeLogin = bbLogin.remaining();
        var sizeMDP = bbMDP.remaining();
        var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + 2 * Integer.BYTES + sizeLogin + sizeMDP);
        buffer.put(OP_CONNECTION_WITH_MDP).putLong(123).putInt(sizeLogin).put(bbLogin).putInt(sizeMDP).put(bbMDP).flip(); // Read mode
        uniqueContext.queueMessage(buffer);
    }

    /**
     * Processes the command from commandQueue
     */

    private void processCommands() {
        synchronized (commandQueue) {
            var msg = commandQueue.poll();
            if (msg != null) {
//				var bbLogin = UTF8.encode(login);
//				var bbMessage = UTF8.encode(msg);
//				var sizeLogin = bbLogin.limit();
//				var sizeMessage = bbMessage.limit();
//				var buffer = ByteBuffer.allocate(2*Integer.BYTES + sizeLogin + sizeMessage);
//				buffer.putInt(sizeLogin).put(bbLogin).putInt(sizeMessage).put(bbMessage).flip();
//				uniqueContext.queueMessage(buffer);
                try {
                    checkAuthentification();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new ClientContext(key);
        key.attach(uniqueContext);
        sc.connect(serverAddress);

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
