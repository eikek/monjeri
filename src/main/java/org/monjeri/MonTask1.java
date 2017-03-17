package org.monjeri;

import com.mongodb.MongoClient;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface MonTask1<A> {

  A run(MongoClient client);

  default Attempt<A> attemptRun(MongoClient client) {
    return Attempt.eval(() -> run(client));
  }

  default <B> MonTask1<B> map(MonTask<A, B> task) {
    Function<A, MonTask1<B>> ff = a -> client -> task.run(a, client);
    return flatMap(ff);
  }

  default <B> MonTask1<B> map(Function<A, B> f) {
    return flatMap(f.andThen(MonTask1::unit));
  }

  @SuppressWarnings("unchecked")
  default <B> MonTask1<B> flatMap(Function<A, MonTask1<B>> f) {
    return new StackedMonTask1<>((MonTask1) this, List.of((Function) f));
  }

  default MonTask1<Void> drain() {
    return map(x -> null);
  }

  default MonTask1<A> thenDo(Consumer<A> effect) {
    return map(a -> {
      effect.accept(a);
      return a;
    });
  }

  default MonTask1<A> thenRun(MonTask1<Void> effect) {
    return flatMap(a -> effect.map(x -> a));
  }

  @SuppressWarnings("ConstantConditions")
  default MonTask1<A> alwaysRun(MonTask1<Void> effect) {
    return thenRun(effect).onError(ex -> effect.map(ignore -> {
      throw ex;
    }));
  }

  default <B> MonTask1<B> transform(Function<MonTask1<A>, MonTask1<B>> f) {
    return f.apply(this);
  }

  default MonTask1<A> onError(Function<RuntimeException, MonTask1<A>> f) {
    return client -> {
      try {
        return run(client);
      } catch (RuntimeException e) {
        return f.apply(e).run(client);
      }
    };
  }

  static <A> MonTask1<A> unit(A value) {
    return c -> value;
  }

  static <A> MonTask1<A> defer(Supplier<A> value) {
    return c -> value.get();
  }

  static <A> MonTask1<A> fail(RuntimeException e) {
    return c -> {
      throw e;
    };
  }

  /**
   * Create a single task from the given list of tasks. Note that {@code null}
   * values are not collected. That means that a list of {@code MonTask1<Void>}
   * results in an empty list, which is (probably in most cases) a good thing.
   * If you need to know if a task in the list returns with {@code null}, wrap
   * the outputs in for example an {@link java.util.Optional}.
   */
  static <A> MonTask1<List<A>> seq(List<MonTask1<A>> tasks) {
    MonTask1<List<A>> zero = unit(List.nil());
    return tasks.foldLeft(zero, (listTask, t) -> listTask.flatMap(l -> t.map(l::nullSafeCons)));
  }

  static MonTask1<Void> runAll(List<MonTask1<?>> tasks) {
    return client -> {
      tasks.foreach(t -> t.run(client));
      return null;
    };
  }
}
