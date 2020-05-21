package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

public class PrivateMessageReader implements Reader<PrivateMessage> {

	private enum State {DONE, WAITING_LOGIN, WAITING_MSG, ERROR}

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
			if(stateLogin != ProcessStatus.DONE) {
				return stateLogin;
			}
			login = stringReader.get();
			state = State.WAITING_MSG;
			stringReader.reset();
		}
		case WAITING_MSG: {
			var stateMessage = getStringPart(bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
			}
			message = stringReader.get();
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
	public PrivateMessage get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new PrivateMessage(login, message);
	}

	@Override
	public void reset() {
		state = State.WAITING_LOGIN;
		stringReader.reset();
		login = null;
		message = null;
	}
}
