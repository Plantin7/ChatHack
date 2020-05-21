package fr.uge.nonblocking.readers.complexReader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.InetSocketAddressReader;
import fr.uge.nonblocking.readers.basicReader.LongReader;
import fr.uge.nonblocking.readers.basicReader.StringReader;

public class AcceptPrivateConnectionReader implements Reader<AcceptPrivateConnection> {
	private enum State {DONE, WAITING_LOGIN, WAITING_SOCKETADDRESS, WAITING_CONNECT_ID,  ERROR}
	
	private State state = State.WAITING_LOGIN;
	private String login;
	private InetSocketAddress socketAddress;
	private long connectId;
	
	private final LongReader longReader = new LongReader();
	private final StringReader stringReader = new StringReader();
	private final InetSocketAddressReader socketAddressReader = new InetSocketAddressReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_LOGIN: {
			var stateMessage = getPart(stringReader, bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
			}
			login = stringReader.get();
			state = State.WAITING_SOCKETADDRESS;
		}
		case WAITING_SOCKETADDRESS : {
			var stateMessage = getPart(socketAddressReader, bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
			}
			socketAddress = socketAddressReader.get();
			state = State.WAITING_CONNECT_ID;
		}
		case WAITING_CONNECT_ID : {
			var stateMessage = getPart(longReader, bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
			}
			connectId = longReader.get();
			state = State.WAITING_CONNECT_ID;
			state = State.DONE;
			return ProcessStatus.DONE;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}	
	}
	
	private ProcessStatus getPart(Reader<?> reader, ByteBuffer bb) {
		Reader.ProcessStatus status = reader.process(bb);
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
		return new AcceptPrivateConnection(login, socketAddress, connectId);
	}

	@Override
	public void reset() {
		state = State.WAITING_LOGIN;
		stringReader.reset();
		longReader.reset();
		socketAddressReader.reset();
		login = null;
		socketAddress = null;
	}

}
