package org.monjeri;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Util {

  public static <A, B> Function<A, B> nullSafe(Function<A, B> f) {
    return a -> a != null ? f.apply(a) : null;
  }

  public static BiFunction<String, String, String> stringAppend() {
    return (a, b) -> a + b;
  }

  public static <A> Function<Stream<Optional<A>>, Stream<A>> presentOnly() {
    return s -> s.flatMap(opt -> opt.map(Stream::of).orElse(Stream.empty()));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <A> Stream<A> makeStream(Optional<Long> size, Iterable<A> iterable) {
    if (size.isPresent()) {
      return StreamSupport.stream(
          Spliterators.spliterator(iterable.iterator(), size.get(),
              Spliterator.IMMUTABLE & Spliterator.NONNULL & Spliterator.SIZED),
          false);
    } else {
      return StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(iterable.iterator(),
              Spliterator.IMMUTABLE & Spliterator.NONNULL),
          false);
    }
  }

  public static <A> Stream<A> concat(List<Stream<A>> streams) {
    return streams.foldLeft(Stream.empty(), Stream::concat);
  }

  public static <A> Iterable<List<A>> group(Iterable<A> in, int size) {
    return () -> new Iterator<List<A>>() {
      private Iterator<A> as = in.iterator();

      @Override
      public boolean hasNext() {
        return as.hasNext();
      }

      @Override
      public List<A> next() {
        List<A> result = List.<A>nil().cons(as.next());
        for (int i = 1; i < size; i++) {
          if (as.hasNext()) {
            result = result.cons(as.next());
          } else {
            break;
          }
        }
        return result.reverse();
      }
    };
  }

  public static <T> Collector<T, ?, List<T>> collectList() {
    class Acc<A> {
      List<A> result = List.nil();
    }
    return Collector.<T, Acc<T>, List<T>>of(
        Acc::new,
        (list, a) -> list.result = list.result.cons(a),
        (l1, l2) -> {
          Acc<T> newAcc = new Acc<>();
          newAcc.result = l1.result.append(l2.result);
          return newAcc;
        },
        acc -> acc.result.reverse()
    );
  }


  static void checkLockname(String lockname) {
    if (!lockname.matches("[a-zA-Z0-9_]+")) {
      throw new IllegalArgumentException("Lock names must be alphanumeric.");
    }
  }

}
