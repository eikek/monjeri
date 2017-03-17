package org.monjeri;

import com.mongodb.client.FindIterable;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Sized<A> implements Iterable<A> {

  private final FindIterable<A> iterable;
  private final long size;

  public Sized(FindIterable<A> iterable, long size) {
    this.iterable = iterable;
    this.size = size;
  }

  @Override
  public Iterator<A> iterator() {
    return iterable.iterator();
  }

  /**
   * Configure the {@link FindIterable} and return {@code this}.
   */
  public Sized<A> set(Consumer<FindIterable<A>> setter) {
    setter.accept(iterable);
    return this;
  }

  public FindIterable<A> getDelegate() {
    return iterable;
  }

  public Optional<Long> size() {
    return Optional.of(size).filter(n -> n >= 0);
  }
}
