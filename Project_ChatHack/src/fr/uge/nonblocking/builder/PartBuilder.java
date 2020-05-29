package fr.uge.nonblocking.builder;

import java.util.function.Consumer;
import java.util.function.Supplier;

import fr.uge.nonblocking.readers.Reader;

public interface PartBuilder<T> {
	<V> PartBuilder<T> addPart(Reader<V> reader, Consumer<V> consumer);
	FinishBuidler<T> addValueRetriever(Supplier<T> supplier);
}
