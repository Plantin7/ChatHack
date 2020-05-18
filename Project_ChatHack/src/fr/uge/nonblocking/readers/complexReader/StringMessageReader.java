package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.StringMessage;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

public class StringMessageReader implements Reader<StringMessage>{
    private enum State {DONE, WAITING_STRING, ERROR}

    private State state = State.WAITING_STRING;
    private String string;
    private final StringReader stringReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_STRING: {
                var statePassword = getStringPart(bb);
                if (statePassword != ProcessStatus.DONE) {
                    return statePassword;
                }
                string = stringReader.get();
                state = State.DONE;
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
	public StringMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new StringMessage(string);
	}

	@Override
	public void reset() {
        state = State.WAITING_STRING;
        stringReader.reset();		
	}

}
