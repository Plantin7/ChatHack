package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.DB;

public class DBReader implements Reader<DB>{
	private enum State {DONE, WAITING_OP, WAITING_ID, ERROR}

	private State state = State.WAITING_OP;
	private byte opcode;
	private long id;
	
	private final LongReader longReader = new LongReader();
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_OP: {
			bb.flip();
			try {
				if(!bb.hasRemaining()) {
					return ProcessStatus.REFILL;
				}
				opcode = bb.get();
			}
			finally {
				bb.compact();
			}
			state = State.WAITING_ID;
		}
		case WAITING_ID: {
			var stateMessage = getLongPart(bb);
			if(stateMessage != ProcessStatus.DONE) {
				return stateMessage;
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
	public DB get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new DB(opcode, id);
	}

	@Override
	public void reset() {
		state = State.WAITING_OP;
		longReader.reset();
	}

}
