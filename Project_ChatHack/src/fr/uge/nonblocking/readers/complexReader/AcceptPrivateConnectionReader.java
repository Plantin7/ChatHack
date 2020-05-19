package fr.uge.nonblocking.readers.complexReader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.InetSocketAddressReader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

public class AcceptPrivateConnectionReader implements Reader<AcceptPrivateConnection> {
	private enum State {DONE, WAITING_ACCEPT_MESSAGE, ERROR}
	
	private State state = State.WAITING_ACCEPT_MESSAGE;
	private String login;
	private InetSocketAddress socketAddress;
	private long connectId;
	
	private final StringReader stringReader = new StringReader();
	private final InetSocketAddressReader scoketAdressReader = new InetSocketAddressReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_ACCEPT_MESSAGE: {
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
	public AcceptPrivateConnection get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new AcceptPrivateConnection(login);
	}

	@Override
	public void reset() {
		state = State.WAITING_ACCEPT_MESSAGE;
		stringReader.reset();
		login = null;
	}

}
