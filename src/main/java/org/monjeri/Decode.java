package org.monjeri;

import org.bson.Document;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This is the base decoder interface modelled as a function from
 * MongoDB's {@code Document} type to some value {@code A}.
 *
 * Additional to the convert method, it provides simple combinator methods
 * to produce higher level converters based on others.
 */
public interface Decode<A> extends Function<Document, A> {

    static <V> Decode<V> from(Function<Document, V> f) {
        return f::apply;
    }

    default <B> Decode<B> map(Function<A, B> f) {
        return from(this.andThen(f));
    }

    default <B> Decode<B> flatMap(Function<A, Decode<B>> f) {
        return o -> f.apply(this.apply(o)).apply(o);
    }

    default Decode<A> or(Decode<A> other) {
        return flatMap(a -> a != null ? unit(a) : other);
    }

    default Decode<A> onError(Decode<A> other) {
        return doc -> {
            try {
                return apply(doc);
            } catch (Exception e) {
                return other.apply(doc);
            }
        };
    }

    default <B, C> Decode<C> combine(Decode<B> cb, BiFunction<A, B, C> f) {
        return n -> f.apply(this.apply(n), cb.apply(n));
    }

    /** Mongo Java driver has its own function type */
    default com.mongodb.Function<Document, A> mapper() {
        return this::apply;
    }

    interface Id extends Decode<Document> {
        default <A> Decode<A> then(Decode<A> decode) {
            return this.map(decode);
        }
    }

    static Id none() {
        return a -> a;
    }

    static <A> Decode<A> unit(A a) {
        return doc -> a;
    }
}
