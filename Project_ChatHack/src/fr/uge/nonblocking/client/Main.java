package fr.uge.nonblocking.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.nonblocking.frame.Frame;
import fr.uge.nonblocking.readers.InetSocketAddressReader;
import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.Reader.ProcessStatus;
import fr.uge.protocol.ChatHackProtocol;

public class Main {
	//private final static InetSocketAddressReader reader = new InetSocketAddressReader();
	private final static Charset UTF8 = StandardCharsets.UTF_8;

	public static void main(String[] args) {
		var bbHostname = UTF8.encode("192.168.0.1");
		int port = 9000;
		var bb = ByteBuffer.allocate(bbHostname.limit() + 2 * Integer.BYTES);
		bb.putInt(bbHostname.limit()).put(bbHostname).putInt(port).flip();
		var reader = new InetSocketAddressReader();
		//ProcessStatus status = ProcessStatus.REFILL;
		
		System.out.println(bb);
		while (true) {
			var status = reader.process(bb);
			switch (status) {
			case DONE:
				var frame = reader.get();
				reader.reset();
				System.out.println("YO " +frame);
				//treatFrame(frame);
				return;
			}
		}
	}
}
