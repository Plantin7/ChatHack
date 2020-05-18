package fr.uge.protocol;

public class ChatHackProtocol {
	
	//OPCODE USED FOR THE BDD
	public final static byte OPCODE_ASK_AUTH_TO_DB_WITH_PASWWORD = 1;
	public final static byte OPCODE_ASK_AUTH_TO_DB_WITHOUT_PASSWORD = 2;

	public final static byte OPCODE_ASK_PRIVATE_CONNECTION = 3;

	public final static byte OPCODE_ASK_AUTH_WITH_PASSWORD = 10;
	public final static byte OPCODE_RESPONSE_AUTH_WITH_PASSWORD = 11;
	
	public final static byte OPCODE_ASK_AUTH_WITHOUT_PASSWORD = 12;
	public final static byte OPCODE_RESPONSE_AUTH_WITHOUT_PASSWORD = 13;
	
	public final static byte OPCODE_PUBLIC_MESSAGE = 14;
	public final static byte OPCODE_PRIVATE_MESSAGE = 15;

}
