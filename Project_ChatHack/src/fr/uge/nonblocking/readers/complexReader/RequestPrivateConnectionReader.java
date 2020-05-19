package fr.uge.nonblocking.readers.complexReader;

import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

import java.nio.ByteBuffer;

public class RequestPrivateConnectionReader implements Reader<RequestPrivateConnection> {

    private enum State {DONE, WAITING_LOGIN, WAITING_MESSAGE, ERROR}

    private State state = State.WAITING_LOGIN;
    private String login;
    private String message;
    private final StringReader stringReader = new StringReader();

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_LOGIN: {
                var stateLogin = getStringPart(bb);
                if (stateLogin != ProcessStatus.DONE) {
                    return stateLogin;
                }
                login = stringReader.get();
                state = State.WAITING_MESSAGE;
                stringReader.reset();
            }
            case WAITING_MESSAGE: {
                var statePassword = getStringPart(bb);
                if (statePassword != ProcessStatus.DONE) {
                    return statePassword;
                }
                message = stringReader.get();
                state = State.DONE;
                stringReader.reset();
                return ProcessStatus.DONE;
            }
            default:
                throw new IllegalArgumentException("unexpected value: " + state);
        }
    }


    private ProcessStatus getStringPart(ByteBuffer bb) {
        Reader.ProcessStatus status = stringReader.process(bb);
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
    public RequestPrivateConnection get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new RequestPrivateConnection(login, message);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        stringReader.reset();
        login = null;
        message= null;
    }
}
