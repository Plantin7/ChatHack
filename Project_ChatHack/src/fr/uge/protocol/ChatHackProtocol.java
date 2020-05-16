package fr.uge.protocol;

public enum ChatHackProtocol {
	OPCODE_ERROR,
	OPCODE_AUTH_WITH_PASSWORD,
	OPCODE_AUTH_WITHOUT_PASSWORD,
	OPCODE_PUBLIC_MESSAGE,
	OPCODE_PRIVATE_MESSAGE;
	//TODO Add all frames
}
