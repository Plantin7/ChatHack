package fr.uge.nonblocking.readers.complexReader;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.Frame;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.protocol.ChatHackProtocol;

public class FrameReader implements Reader<Frame> {

	private enum State { DONE, WAITING_OP, WAITING_FRAME, ERROR }

	private State state = State.WAITING_OP;

	private PublicMessageReader publicMessageReader = new PublicMessageReader();
	private AuthentificationReader authentificationReader = new AuthentificationReader();
	private ResponseAuthentificationReader responseAuthentificationReader = new ResponseAuthentificationReader();
	
	private Reader<? extends Frame> currentFrameReader;
	private Frame frame;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_OP: {
			bb.flip();
			if(!bb.hasRemaining()) {
				bb.compact();
				return ProcessStatus.REFILL;
			}
			var currentOpcode = bb.get(); // Get the opcode
			bb.compact();
			
			switch (currentOpcode) {
			case 10 : {
				currentFrameReader = authentificationReader;
				break;
			}
			case 11 : {
				currentFrameReader = responseAuthentificationReader;
				break;
			}
			case 13 : {
				currentFrameReader = publicMessageReader;
				break;
			}
			case 14 : {
				break;
			}
			default:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
			
			state = State.WAITING_FRAME;
		}
		case WAITING_FRAME: {
			var status = getPart(bb);
			if (status != ProcessStatus.DONE) {
				return status;
			}
			
			frame = currentFrameReader.get();
			state = State.DONE;
			return ProcessStatus.DONE;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}
	}

	private ProcessStatus getPart(ByteBuffer bb) {
		Reader.ProcessStatus status = currentFrameReader.process(bb);
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
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return frame;
	}

	@Override
	public void reset() {
		state = State.WAITING_OP;
		publicMessageReader.reset();
		currentFrameReader.reset();
		frame = null;
	}

}