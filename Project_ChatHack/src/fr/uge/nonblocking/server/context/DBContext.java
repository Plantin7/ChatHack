package fr.uge.nonblocking.server.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import fr.uge.nonblocking.client.context.ClientContext;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.complexReader.DBReader;
import fr.uge.nonblocking.server.ServerChatHack;
import fr.uge.protocol.ChatHackProtocol;

public class DBContext {
    static private int BUFFER_SIZE = 10_000;
    static private Logger logger = Logger.getLogger(DBContext.class.getName());

    final private SelectionKey key;
    final private SocketChannel sc;
    final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    final private Queue<ByteBuffer> queue = new LinkedList<>();      // Queue de bytebuffer en read mode
    final private DBReader dbReader = new DBReader();
    final private ServerChatHack server;
    private boolean closed = false;


    public DBContext(ServerChatHack server, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
    }

    /**
     * Process the content of bbin
     * <p>
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
     */
    private void processIn() {
        while (true) {
            Reader.ProcessStatus status = dbReader.process(bbin); // BddResponseReader
            switch (status) {
                case DONE:
                    var responseOfDB = dbReader.get();
                    server.sendToClientResponseOfDB(responseOfDB);
                    dbReader.reset();
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
    public void queueMessage(ByteBuffer msg) {
        queue.add(msg);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bbout from the message queue
     */
    private void processOut() {
        while (!queue.isEmpty()) {
            var bb = queue.peek();
            if (bb.remaining() <= bbout.remaining()) {
                bbout.put(queue.poll());
            } else {
                return;
            }
        }
    }

    /**
     * Update the interestOps of the key looking
     * only at values of the boolean closed and
     * of both ByteBuffers.
     * <p>
     * The convention is that both buffers are in write-mode before the call
     * to updateInterestOps and after the call.
     * Also it is assumed that process has been be called just
     * before updateInterestOps.
     */

    private void updateInterestOps() {
        if (!key.isValid()) {
            return;
        }
        var interestOps = 0;
        if (!closed && bbin.hasRemaining()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (bbout.position() != 0 || !queue.isEmpty()) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interestOps == 0) {
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
     * <p>
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call
     *
     * @throws IOException
     */
    public void doRead() throws IOException {
        if (sc.read(bbin) == -1) {
            logger.info("Input stream closed");
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    /**
     * Performs the write action on sc
     * <p>
     * The convention is that both buffers are in write-mode before the call
     * to doWrite and after the call
     *
     * @throws IOException
     */

    public void doWrite() throws IOException {
        bbout.flip();
        try {
            sc.write(bbout);
        } finally {
            bbout.compact();
        }
        processOut();
        updateInterestOps();
    }
    
    public SelectionKey getKey() {
    	return key;
    }

}
/**
 * - Le server -> authentification d'un client par le server BDD  1 byte + 1 long + login + mdp
 * - Réponse BDD server : (1)1 byte + long ou (0)1 byte + 1 long
 * - Le server -> peut demander si un login existe (2) byte + id
 * - Réponse BDD server : (1)1 byte + long ou (0)1 byte + 1 long
 */

