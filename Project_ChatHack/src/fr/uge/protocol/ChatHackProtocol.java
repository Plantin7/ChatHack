package fr.uge.protocol;

public class ChatHackProtocol {
	// Authentication with password
	public final static byte OPCODE_ASK_AUTH_WITH_PASSWORD = 10;
	public final static byte OPCODE_RESPONSE_AUTH_WITH_PASSWORD = 11;
	
	// Authentication without password
	public final static byte OPCODE_ASK_AUTH_WITHOUT_PASSWORD = 12;
	public final static byte OPCODE_RESPONSE_AUTH_WITHOUT_PASSWORD = 13;
	
	// Public Message
	public final static byte OPCODE_PUBLIC_MESSAGE = 14;
	
	// Private Message
	public final static byte OPCODE_ASK_PRIVATE_CONNECTION = 15;
	public final static byte OPCODE_SEND_PRIVATE_CONNECTION = 16;
	public final static byte OPCODE_ERROR_PRIVATE_CONNECTION = 17;
	public final static byte OPCODE_ACCEPT_PRIVATE_CONNECTION = 18;
	public final static byte OPCODE_REFUSE_PRIVATE_CONNECTION = 19;

	public final static byte OPCODE_VERIF_CONNECT_ID_PRIVATE_CONNECTION = 20;
	public final static byte OPCODE_CONNECT_ID_PRIVATE_CONNECTION_IS_VALID = 21;
	public final static byte OPCODE_SEND_FILE_MESSAGE= 22;
	public final static byte OPCODE_SEND_PRIVATE_MESSAGE = 23;
	
}
