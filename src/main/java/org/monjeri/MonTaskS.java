package org.monjeri;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Specialisation of a {@link MonTask} for {@link Stream} output type.
 */
@FunctionalInterface
public interface MonTaskS<A, B> extends MonTask<A, Stream<B>> {

  default <C> MonTaskS<A, C> flatMapElements(Function<? super B, MonTask<A, C>> f) {
    return of(flatMap(stream -> (a, client) -> stream.map(f).map(t -> t.run(a, client))));
  }

  default <C> MonTaskS<A, C> mapElements(Function<? super B, ? extends C> f) {
    return of(map(stream -> stream.map(f)));
  }

  default MonTask<A, LongStream> toLongStream(ToLongFunction<? super B> f) {
    return map(stream -> stream.mapToLong(f));
  }

  default MonTask<A, IntStream> toIntStream(ToIntFunction<? super B> f) {
    return map(stream -> stream.mapToInt(f));
  }

  default MonTask<A, DoubleStream> toDoubleStream(ToDoubleFunction<? super B> f) {
    return map(stream -> stream.mapToDouble(f));
  }

  static <A,B> MonTaskS<A,B> of(MonTask<A, Stream<B>> task) {
    return task::run;
  }
}
