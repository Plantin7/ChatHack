package fr.uge.nonblocking.readers;

import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.PrivateFrame;
import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.readers.sequentialreader.SequentialMessageReader;
import fr.uge.protocol.ChatHackProtocol;

public class PrivateFrameReader implements Reader<PrivateFrame> {

	private enum State { DONE, WAITING_OP, WAITING_FRAME, ERROR }

	private State state = State.WAITING_OP;
	
	private final StringReader stringReader = new StringReader();
	private final LongReader longReader = new LongReader();
	private final InetSocketAddressReader socketAddressReader = new InetSocketAddressReader();
	
	private Reader<? extends PrivateFrame> currentFrameReader;
	private String stringOne;
	private String stringTwo;
	private PrivateFrame frame;
	
	private Reader<PrivateMessage> privateMessageReader = 
			SequentialMessageReader
			.<PrivateMessage>create()
			.addPart(stringReader, this::setStringOne)
			.addPart(stringReader, this::setStringTwo)
			.addValueRetriever(this::computePrivateMessage)
			.build();
	
	private void setStringOne(String string) { stringOne = string; }
	private void setStringTwo(String string) { stringTwo = string; }
	private PrivateMessage computePrivateMessage() { return new PrivateMessage(stringOne, stringTwo); } // Login + message
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_OP: {
			bb.flip();
			byte currentOpcode;
			try {
				if(!bb.hasRemaining()) {
					return ProcessStatus.REFILL;
				}
				currentOpcode = bb.get(); // Get the opcode
			}
			finally {
				bb.compact();
			}
			
			switch (currentOpcode) {
			case ChatHackProtocol.OPCODE_SEND_PRIVATE_MESSAGE : {
				currentFrameReader = privateMessageReader;
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
	public PrivateFrame get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return frame;
	}

	@Override
	public void reset() {
		state = State.WAITING_OP;
		currentFrameReader.reset();
		privateMessageReader.reset();
		frame = null;
		
	}

}
