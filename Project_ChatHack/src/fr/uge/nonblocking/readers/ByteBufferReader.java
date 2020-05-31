package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

public class ByteBufferReader implements Reader<ByteBuffer> {
	private enum State {DONE, WAITING_SIZE, WAITING_BYTEBUFFER,ERROR};

	private final IntReader intReader = new IntReader();

	private State state = State.WAITING_SIZE;
	private ByteBuffer internalbb;
	private ByteBuffer bbValue;
	private int bbSize;

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state== State.DONE || state== State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_SIZE: {
			var stringStatus = getPart(intReader, bb);
			if(stringStatus != ProcessStatus.DONE) {
				return stringStatus;
			}
			bbSize = intReader.get();
			internalbb = ByteBuffer.allocate(bbSize);
			if(bbSize < 0) { state = State.ERROR ; return ProcessStatus.ERROR;}
			state = State.WAITING_BYTEBUFFER;
		}
		case WAITING_BYTEBUFFER: {
			var missing = bbSize - internalbb.position();
			bb.flip();
			try {
				if (bb.remaining() <= missing){
					internalbb.put(bb);
				} else {
					var oldLimit = bb.limit();
					bb.limit(internalbb.remaining());
					internalbb.put(bb);
					bb.limit(oldLimit);
				}
			} finally {
				bb.compact();
			}
			if (internalbb.position() < bbSize){
				return ProcessStatus.REFILL;
			}
			state=State.DONE;
			internalbb.flip();
			bbValue = ByteBuffer.allocate(bbSize);
			bbValue.put(internalbb);
			bbValue.flip();
			return ProcessStatus.DONE;
		}
		default:{
			throw new IllegalArgumentException("Unexpected value: " + state);
		}
		}
	}


	private ProcessStatus getPart(Reader<?> reader, ByteBuffer bb) {
		Reader.ProcessStatus status = reader.process(bb);
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
	public ByteBuffer get() {
		if (state!= State.DONE) {
			throw new IllegalStateException();
		}
		return bbValue;
	}

	@Override
	public void reset() {
		state= State.WAITING_SIZE;
		intReader.reset();
		internalbb.clear();
		bbValue = null;
	}

}
