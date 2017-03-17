package org.monjeri;

import com.mongodb.MongoClient;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@FunctionalInterface
public interface MonTask<A, B> {

  B run(A in, MongoClient client);

  default Attempt<B> attemptRun(A in, MongoClient client) {
    return Attempt.eval(() -> run(in, client));
  }

  default <C> MonTask<A, C> map(Function<? super B, ? extends C> f) {
    return flatMap(f.andThen(MonTask::unit));
  }

  default <C> MonTask<A, C> map(MonTask<? super B, ? extends C> f) {
    Function<? super B, ? extends MonTask<A, C>> ff = b -> (a, client) -> f.run(b, client);
    return flatMap(ff);
  }

  default <C> MonTaskS<A, C> mapToStream(Function<? super B, Stream<C>> f) {
    return MonTaskS.of(map(f));
  }

  @SuppressWarnings("unchecked")
  default <C> MonTask<A, C> flatMap(Function<? super B, ? extends MonTask<A, C>> f) {
    return new StackedMonTask<>((MonTask) this, List.of((Function) f));
  }

  default <C> MonTask<A, C> transform(Function<MonTask<A, B>, MonTask<A, C>> f) {
    return f.apply(this);
  }

  default MonTask<A, B> onError(Function<? super RuntimeException, ? extends MonTask<A, B>> f) {
    return (a, client) -> {
      try {
        return run(a, client);
      } catch (RuntimeException e) {
        return f.apply(e).run(a, client);
      }
    };
  }

  default MonTask<A, Void> drain() {
    return map(x -> null);
  }

  default MonTask<A, B> thenDo(BiConsumer<B, MongoClient> effect) {
    return flatMap(b -> (a, client) -> {
      effect.accept(b, client);
      return b;
    });
  }

  default MonTask<A,B> thenRun(MonTask<A, Void> effect) {
    return flatMap(b -> effect.map(x -> b));
  }

  @SuppressWarnings("ConstantConditions")
  default MonTask<A, B> alwaysRun(MonTask<A, Void> effect) {
    return thenRun(effect).onError(ex -> effect.map(ignore -> {
      throw ex;
    }));
  }

  static <X, Z> MonTask<X, Z> unit(Z value) {
    return (a, b) -> value;
  }

  static <X, Z> MonTask<X, Z> defer(Supplier<Z> value) {
    return (a, b) -> value.get();
  }

  static <A, B> MonTask<A, B> lift(Function<A, B> f) {
    return (a, client) -> f.apply(a);
  }

  static <A, B> MonTask<A, B> lift(MonTask1<B> task) {
    return (a, client) -> task.run(client);
  }

  static <A> MonTask<A, Void> lift(Runnable effect) {
    return (a, client) -> {
      effect.run();
      return null;
    };
  }
}
