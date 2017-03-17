package org.monjeri;

import org.monjeri.Json.JObject;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Encode some value to json.
 */
public interface Encode<A> extends Function<A, Json> {

  default <B> Encode<B> contramap(Function<B, A> f) {
    return b -> apply(f.apply(b));
  }

  default Encode<A> combine(Encode<A> next, BiFunction<Json, Json, Json> merge) {
    return a -> merge.apply(apply(a), next.apply(a));
  }

  static Encode<Json> none() {
    return d -> d;
  }

  interface Object<A> extends Encode<A> {

    @Override
    JObject apply(A value);

  }

  static <A> Encode<A> unit(Json json) {
    return a -> json;
  }

  static <A> Encode<List<A>> seq(List<Encode<A>> encodes) {
    return lista -> Json.array(lista.zip(encodes).map(p2 -> p2._2.apply(p2._1)));
  }
}
