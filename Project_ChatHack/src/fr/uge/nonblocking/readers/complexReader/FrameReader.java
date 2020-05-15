package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.Frame;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.basicReader.ByteReader;

public class FrameReader implements Reader<Frame> {

	private enum Status {
		DONE, WAITING_OP, WAITING_FRAME, ERROR
	}

	private enum OP {
	} // TODO

	private byte op;
	private Frame frame;
	private Status status = Status.WAITING_OP;

	private ByteReader byteReader = new ByteReader();
	private PublicMessageReader publicMessageReader = new PublicMessageReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (status == Status.DONE || status == Status.ERROR) {
			throw new IllegalStateException();
		}
		switch (status) {
		case WAITING_OP: {
			var state = getPart(byteReader, bb);
			if (state != ProcessStatus.DONE) {
				return state;
			}
			op = byteReader.get();
			status = Status.WAITING_FRAME;
		}
		case WAITING_FRAME: {
			switch (op) {
			case 1: {
				// TODO
				status = Status.DONE;
				return ProcessStatus.DONE;
			}
			case 2: {
				// TODO
				status = Status.DONE;
				return ProcessStatus.DONE;
			}
			case 3: {
				// TODO
				status = Status.DONE;
				return ProcessStatus.DONE;
			}
			case 4: {
				// TODO
				status = Status.DONE;
				return ProcessStatus.DONE;
			}
			case 5: {
				var state = getPart(publicMessageReader, bb);
				if (state != ProcessStatus.DONE) {
					return state;
				}
				frame = publicMessageReader.get();
				
				status = Status.DONE;
				return ProcessStatus.DONE;
			}
			}
		}
		default:
			throw new IllegalStateException();
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
	public Frame get() {
		if (status != Status.DONE) {
			throw new IllegalStateException();
		}
		return frame;
	}

	@Override
	public void reset() {
		status = Status.WAITING_OP;
		byteReader.reset();
		publicMessageReader.reset();
		frame = null;
	}

}
