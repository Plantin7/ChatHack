package fr.uge.nonblocking.readers.sequentialreader;

import fr.uge.nonblocking.readers.Reader;

public interface FinishBuidler<T> {
	Reader<T> build();
}
