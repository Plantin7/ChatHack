package fr.uge.nonblocking.readers.basicReader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.Reader.ProcessStatus;

public class InetSocketAddressReader implements Reader<InetSocketAddress>{

	private static Logger logger = Logger.getLogger(InetSocketAddressReader.class.getName());
	private enum State {DONE, WAITING_HOSTNAME, WAITING_PORT, ERROR}

	private State state = State.WAITING_HOSTNAME;

	private IntReader intReader = new IntReader();
	private StringReader stringReader = new StringReader();

	private int port;
	private String hostName;

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_HOSTNAME: {
			var stringStatus = getStringPart(stringReader, bb);
			if(stringStatus != ProcessStatus.DONE) {
				return stringStatus;
			}
			hostName = stringReader.get();
			System.out.println(hostName);
			state = State.WAITING_PORT;
		}
		case WAITING_PORT: {
			var integerStatus = getStringPart(intReader, bb);
			if(integerStatus != ProcessStatus.DONE) {
				return integerStatus;
			}
			port = intReader.get();
			if(port < 1024 || port > 65535 ) { logger.info("Bad Port !"); state = State.ERROR; return ProcessStatus.ERROR; }

			state = State.DONE;
			return ProcessStatus.DONE;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + state);
		}
	}

	private ProcessStatus getStringPart(Reader<?> reader, ByteBuffer bb) {
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
	public InetSocketAddress get() {
		if (state!= State.DONE) {
			throw new IllegalStateException();
		}
		return new InetSocketAddress(hostName, port);
	}

	@Override
	public void reset() {
		state = State.WAITING_HOSTNAME;
		intReader.reset();
		stringReader.reset();
		hostName = null;
	}

}
