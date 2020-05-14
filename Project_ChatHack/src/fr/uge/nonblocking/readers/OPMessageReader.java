package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

public class OPMessageReader implements Reader<OPMessage> {

    private enum State {DONE, WAITING_OP, WAITING_MSG, ERROR}

    private State state = State.WAITING_OP;
    private byte op;
    private Message message;

    private final ByteReader byteReader = new ByteReader();
    private final MessageReader messageReader = new MessageReader();

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_OP: {
                var stateOp = getPart(byteReader, bb);
                if (stateOp != ProcessStatus.DONE) {
                    return stateOp;
                }
                op = byteReader.get();
                state = State.WAITING_MSG;
            }
            case WAITING_MSG: {
                var stateMsg = getPart(messageReader, bb);
                if (stateMsg != ProcessStatus.DONE) {
                    return stateMsg;
                }
                message = messageReader.get();
                state = State.DONE;
                return ProcessStatus.DONE;
            }
            default:
                throw new IllegalArgumentException("Unexpected value: " + state);
        }
    }

    private ProcessStatus getPart(Reader<?> reader, ByteBuffer bb) {
        var status = reader.process(bb);
        switch (status) {
            case DONE:
                return ProcessStatus.DONE;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                return ProcessStatus.ERROR;
        }
        throw new AssertionError();
    }

    @Override
    public OPMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new OPMessage(op, message);
    }

    @Override
    public void reset() {
        state = State.WAITING_OP;
        byteReader.reset();
        messageReader.reset();
        message = null;
    }
}
