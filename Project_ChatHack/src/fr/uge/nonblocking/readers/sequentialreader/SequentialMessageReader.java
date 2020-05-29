package fr.uge.nonblocking.readers.sequentialreader;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fr.uge.nonblocking.readers.Reader;
import fr.uge.nonblocking.readers.Reader.ProcessStatus;


public class SequentialMessageReader<T> implements EmptyBuilder<T>, PartBuilder<T>, FinishBuidler<T> {

	private class Pair<V> {
		final Reader<V> reader;
		final Consumer<V> consumer;
		
		public Pair(Reader<V> reader, Consumer<V> consumer) {
			this.reader = reader;
			this.consumer = consumer;
		}
		
		Reader.ProcessStatus process(ByteBuffer bb) {
			var status = reader.process(bb);
			if(status != ProcessStatus.DONE) {
				return status;
			}
			consumer.accept(reader.get());
			reader.reset();
			return ProcessStatus.DONE;
		}
	}
	
	private final List<Pair<?>> pairList = new ArrayList<>();
	private SequentialMessageReader() {}
	private Supplier<T> supplier;
	
	public static <T> EmptyBuilder<T> create() {
		return new SequentialMessageReader<>();
	}
	@Override
	public <V> PartBuilder<T> addPart(Reader<V> reader, Consumer<V> consumer) {
		pairList.add(new Pair<>(reader, consumer));
		return this;
	}
	
	@Override
	public FinishBuidler<T> addValueRetriever(Supplier<T> supplier) {
		this.supplier = supplier;
		return this;
	}

	private enum State {DONE, BUILDERING, ERROR}
	@Override
	public Reader<T> build() {
		return new Reader<T>() {
			private State state = State.BUILDERING;
			private List<Pair<?>> copyPairList = new ArrayList<>(pairList);
			private int currentReaderPos = 0; // index of the current reader we need to treat !

			@Override
			public ProcessStatus process(ByteBuffer bb) {
				
				if(state == State.ERROR || currentReaderPos >= copyPairList.size()) {
					throw new IllegalStateException(); 
				}
				while(currentReaderPos < copyPairList.size()) {
					var status = copyPairList.get(currentReaderPos).process(bb);
					switch (status) {
					case DONE: {
						break;
					}
					case REFILL: {
						return status;
					}
					case ERROR: {
						state = State.ERROR;
						return status;
					}
					default:
						throw new IllegalArgumentException("Unexpected value: " + status);
					}
					currentReaderPos++;
				}
				state = State.DONE;
				return ProcessStatus.DONE;
			}

			@Override
			public T get() {
				if(state != State.DONE) {
					throw new IllegalStateException();
				}
				return supplier.get();
			}

			@Override
			public void reset() {
				state = State.BUILDERING;
				currentReaderPos = 0;
			}
		};
	}
}
