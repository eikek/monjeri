package org.monjeri;

import com.mongodb.MongoClient;

import java.util.function.Function;

/**
 * <p>A stack-friendly flat map operation.</p>
 *
 * <p>Combining many tasks could potentially blow the call stack. For example,
 * with the more straight forward implementation of flatMap:</p>
 *
 * <pre>
 *   return (a, client) -> {
 *     B b = run(a, client);
 *     return f.apply(b).run(a, client);
 *   };
 * </pre>
 *
 * <p>Thus, this implementation collects all the transformations and applies
 * them all at once. This sacrifices type safety, though.</p>
 */
@SuppressWarnings("unchecked")
final class StackedMonTask<A, B> implements MonTask<A, B> {

  private final MonTask<Object, Object> start;
  private final List<Function<Object, MonTask<Object, Object>>> mappings;

  public StackedMonTask(MonTask<Object, Object> start, List<Function<Object, MonTask<Object, Object>>> mappings) {
    this.start = start;
    this.mappings = mappings;
  }

  @Override
  public Object run(Object a, MongoClient client) {
    Object b = start.run(a, client);
    return mappings.foldRight(b, (f, r) -> f.apply(r).run(a, client));
  }

  @Override
  public <C> MonTask<A, C> flatMap(Function<? super B, ? extends MonTask<A, C>> f) {
    Function<Object, MonTask<Object, Object>> ff = (Function) f;
    return (MonTask) new StackedMonTask(start, mappings.cons(ff));
  }
}
