package fr.uge.nonblocking.readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import fr.uge.nonblocking.frame.AcceptPrivateConnection;
import fr.uge.nonblocking.frame.AuthentiticationMessage;
import fr.uge.nonblocking.frame.ErrorPrivateConnection;
import fr.uge.nonblocking.frame.PublicFrame;
import fr.uge.nonblocking.frame.PrivateMessage;
import fr.uge.nonblocking.frame.PublicMessage;
import fr.uge.nonblocking.frame.RefusePrivateConnection;
import fr.uge.nonblocking.frame.RequestPrivateConnection;
import fr.uge.nonblocking.frame.ResponseAuthentification;
import fr.uge.nonblocking.frame.SendPrivateConnection;
import fr.uge.nonblocking.frame.AnonymousAuthenticationMessage;
import fr.uge.nonblocking.readers.sequentialreader.SequentialMessageReader;
import fr.uge.protocol.ChatHackProtocol;

public class FrameReader implements Reader<PublicFrame> {

	private enum State { DONE, WAITING_OP, WAITING_FRAME, ERROR }

	private State state = State.WAITING_OP;
	
	private final StringReader stringReader = new StringReader();
	private final LongReader longReader = new LongReader();
	private final InetSocketAddressReader socketAddressReader = new InetSocketAddressReader();

	// private PublicMessageReader publicMessageReader = new PublicMessageReader();
	// private AuthentificationReader authentificationReader = new AuthentificationReader();
	// private PrivateMessageReader privateMessageReader = new PrivateMessageReader();
	// private ResponseAuthentificationReader responseAuthentificationReader = new ResponseAuthentificationReader();
	// private RequestPrivateConnectionReader requestPrivateConnectionReader = new RequestPrivateConnectionReader();
	// private ErrorPrivateConnectionReader errorReader = new ErrorPrivateConnectionReader();
	// private RefusePrivateConnectionReader refuseConnection = new RefusePrivateConnectionReader();
	// private StringMessageReader stringMessageReader = new StringMessageReader();
	// private AcceptPrivateConnectionReader acceptConnection = new AcceptPrivateConnectionReader();
	
	private Reader<PublicMessage> publicMessageReader = 
			SequentialMessageReader
			.<PublicMessage>create()
			.addPart(stringReader, this::setStringOne)
			.addPart(stringReader, this::setStringTwo)
			.addValueRetriever(this::computePublicMessage)
			.build();
	
	private Reader<AuthentiticationMessage> authentificationReader =
			SequentialMessageReader
			.<AuthentiticationMessage>create()
			.addPart(stringReader, this::setStringOne)
			.addPart(stringReader, this::setStringTwo)
			.addValueRetriever(this::computeAuthentificationMessage)
			.build();
	
	private Reader<RequestPrivateConnection> requestPrivateConnectionReader =
			SequentialMessageReader
			.<RequestPrivateConnection>create()
			.addPart(stringReader, this::setStringOne)
			.addValueRetriever(this::computeRequestPrivateConnectionMessage)
			.build();
	
	private Reader<ErrorPrivateConnection> errorPrivateConnectionReader =
			SequentialMessageReader
			.<ErrorPrivateConnection>create()
			.addPart(stringReader, this::setStringOne)
			.addValueRetriever(this::computeErrorPrivateConnectionMessage)
			.build();
	
	private Reader<RefusePrivateConnection> refusePrivateConnectionReader =
			SequentialMessageReader
			.<RefusePrivateConnection>create()
			.addPart(stringReader, this::setStringOne)
			.addValueRetriever(this::computeRefusePrivateConnectionMessage)
			.build();
	
	private Reader<AcceptPrivateConnection> acceptPrivateConnectionReader =
			SequentialMessageReader
			.<AcceptPrivateConnection>create()
			.addPart(stringReader, this::setStringOne)
			.addPart(socketAddressReader, this::setInetSocketAddress)
			.addPart(longReader, this::setLongOne)
			.addValueRetriever(this::computeAcceptPrivateConnectionMessage)
			.build();
	
	private Reader<AnonymousAuthenticationMessage> anonymousAuthenticationMessageReader =
			SequentialMessageReader
			.<AnonymousAuthenticationMessage>create()
			.addPart(stringReader, this::setStringOne)
			.addValueRetriever(this::computeAnonymousAuthenticationMessage)
			.build();
	
	private Reader<SendPrivateConnection> sendPrivateConnectionReader =
			SequentialMessageReader
			.<SendPrivateConnection>create()
			.addPart(stringReader, this::setStringOne)
			.addValueRetriever(this::computeSendPrivateConnectionMessage)
			.build();
	
	private Reader<? extends PublicFrame> currentFrameReader;
	private PublicFrame frame;
	private String stringOne;
	private String stringTwo;
	private InetSocketAddress socketAddress;
	private long longOne;
	
	private void setStringOne(String string) { stringOne = string; }
	private void setStringTwo(String string) { stringTwo = string; }
	private void setLongOne(long longOne) { this.longOne = longOne; }
	private void setInetSocketAddress(InetSocketAddress socketAddress) { this.socketAddress = socketAddress ;}
	
	private PublicMessage computePublicMessage() { return new PublicMessage(stringOne, stringTwo); } // Login + message
	private AuthentiticationMessage computeAuthentificationMessage() { return new AuthentiticationMessage(stringOne, stringTwo); } // login + password
	private AnonymousAuthenticationMessage computeAnonymousAuthenticationMessage() {return new AnonymousAuthenticationMessage(stringOne);}
	private RequestPrivateConnection computeRequestPrivateConnectionMessage() { return new RequestPrivateConnection(stringOne); }
	private ErrorPrivateConnection computeErrorPrivateConnectionMessage() { return new ErrorPrivateConnection(stringOne); } 
	private RefusePrivateConnection computeRefusePrivateConnectionMessage() { return new RefusePrivateConnection(stringOne); } 
	private AcceptPrivateConnection computeAcceptPrivateConnectionMessage() { return new AcceptPrivateConnection(stringOne, socketAddress, longOne); } 
	private SendPrivateConnection computeSendPrivateConnectionMessage() {return new SendPrivateConnection(stringOne);}
	
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
			case ChatHackProtocol.OPCODE_ASK_AUTH_WITH_PASSWORD : {
				System.out.println("PM1");
				currentFrameReader = authentificationReader;
				break;
			}
			case ChatHackProtocol.OPCODE_ASK_AUTH_WITHOUT_PASSWORD : {
				currentFrameReader = anonymousAuthenticationMessageReader;
				break;
			}
			case ChatHackProtocol.OPCODE_PUBLIC_MESSAGE: {
				currentFrameReader = publicMessageReader;
				break;
			}
			case ChatHackProtocol.OPCODE_ASK_PRIVATE_CONNECTION: {
				currentFrameReader = requestPrivateConnectionReader;
				break;
			}
			case ChatHackProtocol.OPCODE_SEND_PRIVATE_CONNECTION: {
				currentFrameReader = sendPrivateConnectionReader;
				break;
			}
			case ChatHackProtocol.OPCODE_ERROR_PRIVATE_CONNECTION: {
				currentFrameReader = errorPrivateConnectionReader;
				break;
			}
			case ChatHackProtocol.OPCODE_REFUSE_PRIVATE_CONNECTION: {
				currentFrameReader = refusePrivateConnectionReader;
				break;
			}
			case ChatHackProtocol.OPCODE_ACCEPT_PRIVATE_CONNECTION :{
				currentFrameReader = acceptPrivateConnectionReader;
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
	public PublicFrame get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return frame;
	}

	@Override
	public void reset() {
		state = State.WAITING_OP;
		publicMessageReader.reset();
		authentificationReader.reset();
		requestPrivateConnectionReader.reset();
		errorPrivateConnectionReader.reset();
		acceptPrivateConnectionReader.reset();
		refusePrivateConnectionReader.reset();
		currentFrameReader.reset();
		anonymousAuthenticationMessageReader.reset();
		sendPrivateConnectionReader.reset();
		frame = null;
		stringOne = null;
		stringTwo = null;
		socketAddress = null;
		
	}

}
