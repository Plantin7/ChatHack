package fr.uge.nonblocking.builder;

import java.util.function.Consumer;

import fr.uge.nonblocking.readers.Reader;

public interface EmptyBuilder<T> {
	<V> PartBuilder<T> addPart(Reader<V> reader, Consumer<V> consumer);
}
