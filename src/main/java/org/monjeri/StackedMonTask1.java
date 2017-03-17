package org.monjeri;

import com.mongodb.MongoClient;

import java.util.function.Function;

/**
 * A stack friendly MonTask1.
 *
 * @see StackedMonTask class documenation, its the same here
 */
@SuppressWarnings("unchecked")
final class StackedMonTask1<A> implements MonTask1<A> {

  private final MonTask1<Object> start;
  private final List<Function<Object, MonTask1<Object>>> mappings;

  public StackedMonTask1(MonTask1<Object> start, List<Function<Object, MonTask1<Object>>> mappings) {
    this.start = start;
    this.mappings = mappings;
  }

  @Override
  public A run(MongoClient client) {
    Object a = start.run(client);
    return (A) mappings.foldRight(a, (f, r) -> f.apply(r).run(client));
  }

  @Override
  public <B> MonTask1<B> flatMap(Function<A, MonTask1<B>> f) {
    Function<Object, MonTask1<Object>> ff = (Function) f;
    return new StackedMonTask1<>(start, mappings.cons(ff));
  }
}
