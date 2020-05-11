package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class StringReader implements Reader<String>{

	private final static Charset UTF8 = Charset.forName("UTF8");
	private static Logger logger = Logger.getLogger(StringReader.class.getName());
	private enum State {DONE, WAITING_SIZE, WAITING_TEXT, ERROR}
	private final static int BUFFER_SIZE = 1_024;

	private State state = State.WAITING_SIZE;
	private IntReader intReader = new IntReader();
	private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE);

	private int size;
	private String string;

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		
		switch (state) {
		case WAITING_SIZE: {
			var integerStatus = getStringPart(bb);
			if(integerStatus != ProcessStatus.DONE) {
				return integerStatus;
			}
			size = intReader.get();
			if(size < 0 || size > BUFFER_SIZE ) { logger.info("The String size does not respect the protocol"); state = State.ERROR; return ProcessStatus.ERROR; }
			state = State.WAITING_TEXT;
		}
		case WAITING_TEXT: {
			fillByteBuffer(bb);
			if (internalbb.position() < size){
				return ProcessStatus.REFILL;
			}
			
			state = State.DONE;
			internalbb.flip();
			string = UTF8.decode(internalbb).toString();

			return ProcessStatus.DONE;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}
	}

	private ProcessStatus getStringPart(ByteBuffer bb) {
		Reader.ProcessStatus status = intReader.process(bb);
		switch (status) {
		case DONE: return ProcessStatus.DONE;
		case REFILL: return ProcessStatus.REFILL;
		default: throw new AssertionError();
		}
	}

	public void fillByteBuffer(ByteBuffer bb) {
		var missing = size - internalbb.position();
		bb.flip(); // ReadMode
		try {
			if(bb.remaining() <= missing) {
				internalbb.put(bb);
			}
			else {
				var oldLimit = bb.limit();
				bb.limit(missing);
				internalbb.put(bb);
				bb.limit(oldLimit);
			}

		} finally {
			bb.compact();
		}
	}

	@Override
	public String get() {
		if (state!= State.DONE) {
			throw new IllegalStateException();
		}
		return string;
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
		intReader.reset();
		internalbb.clear();
		string = null;
	}

}
