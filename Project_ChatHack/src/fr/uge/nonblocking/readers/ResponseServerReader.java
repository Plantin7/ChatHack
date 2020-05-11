package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

public class ResponseServerReader implements Reader<ResponseServer> {

	private enum State {DONE, WAITING_OP, WAITING_ID, ERROR}

	private State state = State.WAITING_OP;
	private byte op;
	private long id;

	private final LongReader longReader = new LongReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if(state == State.DONE && state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
		case WAITING_OP: {
			op = bb.get();
			state = State.WAITING_ID;
		}
		case WAITING_ID: {
			var stateId = getLongPart(bb);
			if(stateId != ProcessStatus.DONE) {
				return stateId;
			}
			id = longReader.get();
			state = State.DONE;
			return ProcessStatus.DONE;

		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}	
	}

	private ProcessStatus getLongPart(ByteBuffer bb) {
		Reader.ProcessStatus status = longReader.process(bb);
		switch (status) {
		case DONE: return ProcessStatus.DONE;
		case REFILL: return ProcessStatus.REFILL;
		case ERROR: return ProcessStatus.ERROR;
		}
		throw new AssertionError();
	}

	@Override
	public ResponseServer get() {
		if(state != State.DONE) {
			throw new IllegalStateException();
		}
		return new ResponseServer(op, id);
	}

	@Override
	public void reset() {
		state = State.WAITING_OP;
		longReader.reset();

	}

}
