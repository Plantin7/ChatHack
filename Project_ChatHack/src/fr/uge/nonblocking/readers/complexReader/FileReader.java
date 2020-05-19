package fr.uge.nonblocking.readers.complexReader;

import fr.uge.nonblocking.frame.AuthentificationMessage;
import fr.uge.nonblocking.frame.FileMessage;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.LongReader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

import java.nio.ByteBuffer;

public class FileReader implements Reader<FileMessage> {

    private enum State {
        DONE,
        WAITING_LOGIN,
        WAITING_KEY,
        WAITING_NAMEFILE,
        WAITING_NBBLOCS,
        WAITING_CONTENT,
        ERROR
    }

    private State state = State.WAITING_KEY;
    private String login;
    private long keyPrivateConnection;
    private String nameFile;
    private long nbBlocs;
    private String content;
    private final LongReader longReader = new LongReader();
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
                state = State.WAITING_KEY;
                stringReader.reset();
            }
            case WAITING_KEY: {
                var stateKey = getLongPart(bb);
                if (stateKey != ProcessStatus.DONE) {
                    return stateKey;
                }
                keyPrivateConnection = longReader.get();
                state = State.WAITING_NAMEFILE;
                longReader.reset();
            }
            case WAITING_NAMEFILE: {
                var stateNameFile = getStringPart(bb);
                if (stateNameFile != ProcessStatus.DONE) {
                    return stateNameFile;
                }
                nameFile = stringReader.get();
                state = State.WAITING_NBBLOCS;
                stringReader.reset();
            }
            case WAITING_NBBLOCS: {
                var stateNbBlocs = getLongPart(bb);
                if (stateNbBlocs != ProcessStatus.DONE) {
                    return stateNbBlocs;
                }
                nbBlocs = longReader.get();
                state = State.WAITING_CONTENT;
                longReader.reset();
            }
            case WAITING_CONTENT: {
                var stateContent = getStringPart(bb);
                if (stateContent != ProcessStatus.DONE) {
                    return stateContent;
                }
                content = stringReader.get();
                state = State.DONE;
                stringReader.reset();
                return ProcessStatus.DONE;
            }
            default:
                throw new IllegalArgumentException("unexpected value: " + state);

        }

    }

    private ProcessStatus getLongPart(ByteBuffer bb) {
        Reader.ProcessStatus status = longReader.process(bb);
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
    public FileMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new FileMessage(login, keyPrivateConnection, nameFile, nbBlocs, content);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        stringReader.reset();
        nameFile = null;
        content = null;
    }
}
