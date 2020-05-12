package fr.uge.nonblocking.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.nonblocking.readers.Message;
import fr.uge.nonblocking.readers.MessageReader;
import fr.uge.nonblocking.readers.Reader.ProcessStatus;
import fr.uge.nonblocking.readers.ResponseServer;
import fr.uge.nonblocking.readers.ResponseServerReader;

public class ClientChatHack {

    static private class Context {

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
        // final private MessageReader messageReader = new MessageReader();
        final private ResponseServerReader responseServerReader = new ResponseServerReader();
        private boolean closed = false;

        private Context(SelectionKey key){
            this.key = key;
            this.sc = (SocketChannel) key.channel();
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
        		ProcessStatus status = responseServerReader.process(bbin);
        		switch (status) {
				case DONE:
					//Message message = messageReader.get();
					ResponseServer response = responseServerReader.get();
					System.out.println(response);
					responseServerReader.reset();
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
         * @param bb
         */
        private void queueMessage(ByteBuffer bb) {
            queue.add(bb);
            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bbout from the message queue
         *
         */
        private void processOut() {
            while (!queue.isEmpty()){
                var bb = queue.peek();
                if (bb.remaining()<=bbout.remaining()){
                    queue.remove();
                    bbout.put(bb);
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
            var interesOps=0;
            if (!closed && bbin.hasRemaining()){
                interesOps=interesOps|SelectionKey.OP_READ;
            }
            if (bbout.position()!=0){
                interesOps|=SelectionKey.OP_WRITE;
            }
            if (interesOps==0){
                silentlyClose();
                return;
            }
            key.interestOps(interesOps);
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
            if (sc.read(bbin)==-1) {
                closed=true;
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
            sc.write(bbout);
            bbout.compact();
            processOut();
            updateInterestOps();
        }

        public void doConnect() throws IOException {
        	if(!sc.finishConnect()) {
        		return;
        	}
        	key.interestOps(SelectionKey.OP_READ);
        }
    }

    static private int BUFFER_SIZE = 10_000;
    static private Logger logger = Logger.getLogger(ClientChatHack.class.getName());


    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String login;
    private final Thread console;
	private final static Charset UTF8 = Charset.forName("UTF8");
    private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private Context uniqueContext;

    public ClientChatHack(String login, InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
        this.login = login;
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }

    private void consoleRun() {
        try(var scan = new Scanner(System.in)) {
            while (scan.hasNextLine()) {
                var msg = scan.nextLine();
                sendCommand(msg);
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
	private static final byte OP       = 1;
	
    private void checkAuthentification() throws IOException {
    	var bbLogin = UTF8.encode(login);
    	var bbMDP = UTF8.encode("€€€€€"); // Etienne login
    	var sizeLogin = bbLogin.remaining();
    	var sizeMDP = bbMDP.remaining();
    	var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + 2 * Integer.BYTES + sizeLogin + sizeMDP);
    	buffer.put(OP).putLong(123).putInt(sizeLogin).put(bbLogin).putInt(sizeMDP).put(bbMDP).flip(); // Read mode
    	uniqueContext.queueMessage(buffer);
    }
    
	static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while(bb.hasRemaining()) {
			if(sc.read(bb) == -1) {
				return false;
			}
		}
		return true;
	}
    

    /**
     * Processes the command from commandQueue
     */

    private void processCommands(){
    	synchronized (commandQueue) {
			var msg = commandQueue.poll();
			if(msg != null) {
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
        uniqueContext = new Context(key);
        key.attach(uniqueContext);
        sc.connect(serverAddress);
        
        console.setDaemon(true);
        console.start();

        while(!Thread.interrupted()) {
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
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=3){
            usage();
            return;
        }
        new ClientChatHack(args[0],new InetSocketAddress(args[1],Integer.parseInt(args[2]))).launch();
    }

    private static void usage(){
        System.out.println("Usage : ClientChat login hostname port");
    }
}
