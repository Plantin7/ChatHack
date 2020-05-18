package fr.uge.nonblocking.readers.complexReader;

import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

import java.nio.ByteBuffer;

public class AuthentificationReader implements Reader<AuthentificationMessage> {

    private enum State {DONE, WAITING_LOGIN, WAITING_PASSWORD, ERROR}

    private State state = State.WAITING_LOGIN;
    private String login;
    private String password;
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
                state = State.WAITING_PASSWORD;
                stringReader.reset();
            }
            case WAITING_PASSWORD: {
                var statePassword = getStringPart(bb);
                if (statePassword != ProcessStatus.DONE) {
                    return statePassword;
                }
                password = stringReader.get();
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
    public AuthentificationMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new AuthentificationMessage(login, password);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        stringReader.reset();
        login = null;
        password= null;
    }
}
