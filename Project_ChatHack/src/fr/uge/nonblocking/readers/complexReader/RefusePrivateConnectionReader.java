package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

public class RefusePrivateConnectionReader implements Reader<RefusePrivateConnection> {
	private enum State {DONE, WAITING_REFUSE_MESSAGE, ERROR}
	
	private State state = State.WAITING_REFUSE_MESSAGE;
	private String login;
	
	private final StringReader stringReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_REFUSE_MESSAGE: {
			var stateMessage = getStringPart(bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
			}
			login = stringReader.get();
			state = State.DONE;
			stringReader.reset();
			return ProcessStatus.DONE;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}	
	}
	
	private ProcessStatus getStringPart(ByteBuffer bb) {
		Reader.ProcessStatus status = stringReader.process(bb);
		switch (status) {
		case DONE: return ProcessStatus.DONE;
		case REFILL: return ProcessStatus.REFILL;
		case ERROR: return ProcessStatus.ERROR;
		}
		throw new AssertionError();
	}

	@Override
	public RefusePrivateConnection get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new RefusePrivateConnection(login);
	}

	@Override
	public void reset() {
		state = State.WAITING_REFUSE_MESSAGE;
		stringReader.reset();
		login = null;
	}

}
