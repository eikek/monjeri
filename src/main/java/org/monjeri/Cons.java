package org.monjeri;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

final class Cons<A> implements List<A> {
  final static List<Object> NIL = new List<Object>() {
    @Override
    public Optional<Object> headOption() {
      return Optional.empty();
    }

    @Override
    public List<Object> tail() {
      return this;
    }

    @Override
    public List<Object> dropWhile(Predicate<Object> predicate) {
      return this;
    }

    @Override
    public List<Object> takeWhile(Predicate<Object> predicate) {
      return this;
    }

    @Override
    public String toString() {
      return "[]";
    }

    @Override
    public Iterator<Object> iterator() {
      return Collections.emptyList().iterator();
    }
  };

  private final A head;
  private final List<A> tail;

  private Cons(A head, List<A> tail) {
    this.head = head;
    this.tail = tail;
  }

  public static <A> List<A> of(A value, List<A> tail) {
    return new Cons<>(Objects.requireNonNull(value), Objects.requireNonNull(tail));
  }

  @Override
  public Optional<A> headOption() {
    return Optional.of(head);
  }

  @Override
  public List<A> tail() {
    return tail;
  }

  @Override
  public Iterator<A> iterator() {
    return new Iterator<A>() {
      private List<A> state = Cons.this;

      @Override
      public boolean hasNext() {
        return state.nonEmpty();
      }

      @Override
      public A next() {
        A a = state.head();
        state = state.tail();
        return a;
      }
    };
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    buffer.append(head());
    tail().foreach(a -> buffer.append(",").append(a));
    buffer.append("]");
    return buffer.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Cons<?> cons = (Cons<?>) o;

    if (!head.equals(cons.head)) return false;
    return tail.equals(cons.tail);
  }

  @Override
  public int hashCode() {
    int result = head.hashCode();
    result = 31 * result + tail.hashCode();
    return result;
  }
}