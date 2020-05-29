package fr.uge.nonblocking.builder;

import fr.uge.nonblocking.readers.Reader;

public interface FinishBuidler<T> {
	Reader<T> build();
}
