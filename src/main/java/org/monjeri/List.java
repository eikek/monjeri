package org.monjeri;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * <p>SimpleChange immutable singly linked list.</p>
 * <p>All operations are stack-safe. It does not support {@code null} values.</p>
 */
public interface List<A> extends Iterable<A> {

  Optional<A> headOption();

  List<A> tail();

  default A head() {
    Optional<A> h = headOption();
    if (h.isPresent()) {
      return h.get();
    } else {
      throw new IndexOutOfBoundsException("head of empty list");
    }
  }

  /**
   * Return the element at index {@code index}. Throws an {@link IndexOutOfBoundsException}
   * if the index is not valid. This operation must traverse the up to {@code index} element.
   */
  default A elementAt(int index) {
    return drop(index).head();
  }

  default boolean isEmpty() {
    return !nonEmpty();
  }

  default boolean nonEmpty() {
    return headOption().isPresent();
  }

  /**
   * Prepend {@code value} to this list, which must not be {@code null}.
   */
  default List<A> cons(A value) {
    return Cons.of(Objects.requireNonNull(value), this);
  }

  /**
   * Prepend the given value to this list if it is not {@code null}. If it is
   * {@code null}, do not prepend and return this.
   */
  default List<A> nullSafeCons(A value) {
    return value != null ? cons(value) : this;
  }

  default <B> B foldLeft(B z, BiFunction<B, A, B> f) {
    B result = z;
    List<A> rest = this;
    while (rest.nonEmpty()) {
      result = f.apply(result, rest.head());
      rest = rest.tail();
    }
    return result;
  }

  default void foreach(Consumer<? super A> f) {
    for (A a : this) {
      f.accept(a);
    }
  }

  default <B> B foldRight(B z, BiFunction<A, B, B> f) {
    return reverse().foldLeft(z, (b, a) -> f.apply(a, b));
  }

  default <B> List<B> map(Function<? super A, ? extends B> f) {
    return foldRight(nil(), (a, l) -> l.cons(f.apply(a)));
  }

  default List<A> append(List<A> other) {
    return other.isEmpty()
        ? this
        : this.isEmpty()
        ? other : foldRight(other, (a, l) -> l.cons(a));
  }

  default <B> List<B> flatMap(Function<A, List<B>> f) {
    List<List<B>> mapped = map(f);
    List<B> result = List.nil();
    while (mapped.nonEmpty()) {
      result = mapped.head().foldLeft(result, List::cons);
      mapped = mapped.tail();
    }
    return result.reverse();
  }

  default List<A> filter(Predicate<A> p) {
    List<A> result = nil();
    return foldRight(result, (a, r) -> p.test(a) ? r.cons(a) : r);
  }

  default Optional<A> find(Predicate<A> p) {
    List<A> rest = this;
    while (rest.nonEmpty()) {
      if (p.test(rest.head())) {
        return Optional.of(rest.head());
      }
      rest = rest.tail();
    }
    return Optional.empty();
  }

  default List<A> takeWhile(Predicate<A> predicate) {
    List<A> result = List.nil();
    List<A> rest = this;
    while (rest.nonEmpty() && predicate.test(rest.head())) {
      result = result.cons(rest.head());
      rest = rest.tail();
    }
    return result.reverse();
  }

  default List<A> dropWhile(Predicate<A> predicate) {
    List<A> result = this;
    while (result.nonEmpty() && predicate.test(result.head())) {
      result = result.tail();
    }
    return result;
  }

  default List<A> drop(int n) {
    AtomicInteger i = new AtomicInteger(n);
    return dropWhile(x -> i.getAndDecrement() > 0);
  }

  default List<A> take(int n) {
    AtomicInteger i = new AtomicInteger(n);
    return takeWhile(x -> i.getAndDecrement() > 0);
  }

  /**
   * Return {@code true} if the predicate {@code p} holds for all elements. If the list is empty
   * {@code true} is returned.
   */
  default boolean forall(Predicate<A> p) {
    //return isEmpty() || p.test(head()) && tail().forall(p);
    List<A> rest = this;
    while (rest.nonEmpty()) {
      if (!p.test(rest.head())) {
        return false;
      }
      rest = rest.tail();
    }
    return true;
  }

  /**
   * Return {@code true} if the predicate {@code p} holds for any element. If the list is empty
   * {@code false} is returned.
   */
  default boolean exists(Predicate<A> p) {
    return find(p).isPresent();
  }

  default List<A> reverse() {
    return foldLeft(nil(), List::cons);
  }

  @SuppressWarnings("unchecked")
  default List<A> intersperse(A sep) {
    if (isEmpty()) {
      return this;
    } else {
      List<A> result = List.of(head());
      return tail().foldLeft(result, (r, a) -> r.cons(sep).cons(a)).reverse();
    }
  }

  /**
   * Removes duplicate elements by keeping the first value and dropping all
   * subsequent elements that are equal according to {@code eq}.
   */
  default List<A> distinct(Eq<A> eq) {
    List<A> x = foldLeft(nil(), (r, a) -> r.contains(a, eq) ? r : r.cons(a));
    return x.reverse();
  }

  default boolean contains(A element, Eq<A> eq) {
    return exists(a -> eq.equal(a, element));
  }

  default boolean contains(A element) {
    return contains(element, Eq.objectEq());
  }

  default int size() {
    return foldLeft(0, (n, a) -> n + 1);
  }

  default List<A> remove(A element, Eq<A> eq) {
    return filter(a -> !eq.equal(a, element));
  }

  default List<A> remove(A element) {
    return remove(element, Eq.objectEq());
  }

  default <B> List<P2<A, B>> zip(List<B> other) {
    List<P2<A, B>> result = nil();
    List<A> as = this;
    List<B> bs = other;
    while (as.nonEmpty() && bs.nonEmpty()) {
      result = result.cons(P2.of(as.head(), bs.head()));
      as = as.tail();
      bs = bs.tail();
    }
    return result.reverse();
  }

  default List<P2<A, Integer>> zipWithIndex() {
    AtomicInteger index = new AtomicInteger(0);
    List<P2<A, Integer>> result =
        foldLeft(List.nil(), (list, a) -> list.cons(P2.of(a, index.getAndIncrement())));
    return result.reverse();
  }

  @SuppressWarnings("unchecked")
  default <T> List<A> sortWith(Function<? super A, ? extends Comparable<T>> f) {
    java.util.List<? extends P2<A, ? extends Comparable<T>>> p2s = zip(map(f)).toJava();

    p2s.sort((Comparator<P2<A, ? extends Comparable<T>>>)
        (o1, o2) -> o1._2.compareTo((T) o2._2));

    return fromList(p2s, P2::__1);
  }

  default java.util.List<A> toJava() {
    java.util.List<A> result = new ArrayList<>();
    foreach(result::add);
    return result;
  }

  /**
   * Collect a {@link java.util.stream.Stream} into a list.
   */
  static <A> Collector<A, ?, List<A>> collector() {
    return Util.collectList();
  }

  /**
   * Return the empty list.
   */
  @SuppressWarnings("unchecked")
  static <A> List<A> nil() {
    return (List<A>) Cons.NIL;
  }

  @SuppressWarnings("unchecked")
  static <A> List<A> of(A... more) {
    return fromArray(more);
  }

  static <A, B> List<A> fromIterator(Iterator<B> iter, Function<B, A> f) {
    List<A> result = nil();
    while (iter.hasNext()) {
      result = result.cons(f.apply(iter.next()));
    }
    return result.reverse();
  }

  static <A, B> List<A> fromList(java.util.List<B> list, Function<B, A> f) {
    List<A> result = List.nil();
    for (int i=list.size()-1; i>=0; i--) {
      result = result.cons(f.apply(list.get(i)));
    }
    return result;
  }

  static <A> List<A> fromIterator(Iterator<A> iter) {
    return fromIterator(iter, Function.identity());
  }

  static <A> List<A> fromList(java.util.List<A> list) {
    return fromList(list, Function.identity());
  }

  static <A> List<A> fromIterable(Iterable<A> iter) {
    return fromIterator(iter.iterator());
  }

  static <A> List<A> fromArray(A[] array) {
    return fromIterator(new Iterator<A>() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return i < array.length;
      }

      @Override
      public A next() {
        i = i + 1;
        return array[i - 1];
      }
    });
  }

  /**
   * Create a new list by collecting the result of applying {@code as}
   * to an increasing number up to {@code size}.
   */
  static <A> List<A> fill(int size, Function<Integer, A> as) {
    List<A> result = nil();
    for (int i = size - 1; i >= 0; i--) {
      result = result.cons(as.apply(i));
    }
    return result;
  }

  static <A> List<A> fill(int size, A a) {
    return fill(size, i -> a);
  }

  static <A> List<A> unfold(A start, Function<A, Optional<A>> next) {
    List<A> result = nil();
    while (start != null) {
      result = result.cons(start);
      start = next.apply(start).orElse(null);
    }
    return result;
  }

  static List<Integer> range(int from, int to) {
    return unfold(from, n -> n < to - 1 ? Optional.of(n + 1) : Optional.empty()).reverse();
  }

  /**
   * A tuple class used for the zip results.
   */
  final class P2<A, B> {
    public final A _1;
    public final B _2;

    public P2(A _1, B _2) {
      this._1 = _1;
      this._2 = _2;
    }

    public static <A, B>  P2<A, B> of(A a, B b) {
      return new P2<>(a, b);
    }

    public A __1() {
      return _1;
    }

    public B __2() {
      return _2;
    }

    public boolean isEqual() {
      return Objects.equals(_1, _2);
    }

    public boolean notEqual() {
      return !isEqual();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      P2<?, ?> p2 = (P2<?, ?>) o;
      return Objects.equals(_1, p2._1) &&
          Objects.equals(_2, p2._2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_1, _2);
    }

    @Override
    public String toString() {
      return "P2(" + _1 + ", " + _2 + ')';
    }
  }
}
