package org.monjeri;

import com.mongodb.DBRef;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.monjeri.Util.nullSafe;

/**
 * This is a collection of basic, low-level, converters that should be used as a
 * basis for custom ones.
 * <p>
 * The idea is to use java8 default methods on interfaces to be able to mixin
 * multiple converter collections to use in your custom converter impl.
 */
public interface BaseDecode {
  static BaseDecode getInstance() {
    return new BaseDecode() {};
  }

  default Decode.Id doc(Path path) {
    return path.isEmpty()
        ? doc -> doc
        : doc -> doc(path.dropFirst()).apply((doc.get(path.first(), Document.class)));
  }

  default Decode<String> json() {
    return Document::toJson;
  }

  default <B> Decode<B> path(Path p, BiFunction<Document, String, B> conv) {
    if (p.isEmpty()) {
      //return doc -> conv.apply(doc, "");
      throw new IllegalArgumentException("path must not be root");
    }
    return p.dropFirst().isEmpty()
        ? doc -> conv.apply(doc, p.first())
        : doc(p.firstPath()).then(path(p.dropFirst(), conv));
  }

  default Decode<Integer> intNullable(Path path) {
    return path(path, Document::getInteger);
  }

  default Decode<Integer> integer(Path path) {
    return intNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<Integer>> intOpt(Path path) {
    return optional(intNullable(path));
  }

  default Decode<Long> longNullable(Path path) {
    return path(path, Document::getLong);
  }

  default Decode<Optional<Long>> longOpt(Path path) {
    return optional(longNullable(path));
  }

  default Decode<Long> long_(Path path) {
    return longNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Double> doubleNullable(Path path) {
    return path(path, Document::getDouble);
  }

  default Decode<Double> double_(Path path) {
    return doubleNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<Double>> doubleOpt(Path path) {
    return optional(doubleNullable(path));
  }

  default Decode<String> stringNullable(Path path) {
    return path(path, Document::getString);
  }

  default Decode<Optional<String>> stringOpt(Path path) {
    return optional(stringNullable(path));
  }

  default Decode<String> string(Path path) {
    return stringNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Boolean> boolNullable(Path path) {
    return path(path, Document::getBoolean);
  }

  default Decode<Boolean> bool(Path path) {
    return boolNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<Boolean>> boolOpt(Path path) {
    return optional(boolNullable(path));
  }

  default Decode<ObjectId> objectIdNullable(Path path) {
    return path(path, Document::getObjectId);
  }

  default Decode<ObjectId> objectId(Path path) {
    return objectIdNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<ObjectId>> objectIdOpt(Path path) {
    return optional(objectIdNullable(path));
  }

  default Decode<BigDecimal> decimalNullable(Path path) {
    Decode<BigDecimal> fromLong = longNullable(path).map(nullSafe(BigDecimal::new));
    Decode<BigDecimal> fromInt = intNullable(path).map(nullSafe(BigDecimal::new));
    Decode<BigDecimal> fromDouble = doubleNullable(path).map(nullSafe(BigDecimal::new));
    Decode<BigDecimal> fromString = stringNullable(path).map(nullSafe(BigDecimal::new));
    return fromString.onError(fromDouble).onError(fromLong).onError(fromInt);
  }

  default Decode<BigDecimal> decimal(Path path) {
    return decimalNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<BigDecimal>> decimalOpt(Path path) {
    return optional(decimalNullable(path));
  }

  default Decode<Duration> durationNullable(Path path) {
    return stringNullable(path).map(nullSafe(Duration::parse));
  }

  default Decode<Duration> duration(Path path) {
    return string(path).map(Duration::parse);
  }

  default Decode<Optional<Duration>> durationOpt(Path path) {
    return optional(durationNullable(path));
  }

  default Decode<ZonedDateTime> zonedDateTimeNullable(Path path) {
    return stringNullable(path).map(nullSafe(ZonedDateTime::parse));
  }

  default Decode<ZonedDateTime> zonedDateTime(Path path) {
    return zonedDateTimeNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<ZonedDateTime>> zonedDateTimeOpt(Path path) {
    return optional(zonedDateTimeNullable(path));
  }

  default Decode<Instant> instantNullable(Path path) {
    return stringNullable(path).map(nullSafe(Instant::parse));
  }

  default Decode<Instant> instant(Path path) {
    return instantNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<Instant>> instantOpt(Path path) {
    return instantNullable(path).map(Optional::ofNullable);
  }

  default Decode<LocalDate> localDateNullable(Path path) {
    return stringNullable(path).map(nullSafe(LocalDate::parse));
  }

  default Decode<LocalDate> localDate(Path path) {
    return localDateNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<LocalDate>> localDateOpt(Path path) {
    return localDateNullable(path).map(Optional::ofNullable);
  }

  default Decode<DBRef> dbRefNullable(Path path) {
    return path(path, (doc, name) -> (DBRef) doc.get(name));
  }

  default Decode<DBRef> dbRef(Path path) {
    return dbRefNullable(path).map(Objects::requireNonNull);
  }

  default Decode<Optional<DBRef>> dbRefOpt(Path path) {
    return dbRefNullable(path).map(Optional::ofNullable);
  }

  default <A> Decode<Optional<A>> optional(Decode<A> nullableDecoder) {
    return nullableDecoder.map(Optional::ofNullable);
  }

  @SuppressWarnings("unchecked")
  default <A> Decode<List<A>> list(Path p, Function<?, A> decA) {
    return listIndexed(p, i -> decA);
  }

  @SuppressWarnings("unchecked")
  default <A> Decode<List<A>> listIndexed(Path p, Function<Integer, Function<?, A>> decAWithIndex) {
    Function<Integer, Function<Object, A>> f = (Function<Integer, Function<Object, A>>) (Object) decAWithIndex;
    return path(p, (doc, name) -> {
      Iterable<Object> array = (Iterable<Object>) doc.get(name);
      if (array == null) {
        throw new NullPointerException("document " + doc + " has no array at " + name);
      }
      List<A> result = List.nil();
      int index = 0;
      for (Object el : array) {
        if (el == null) {
          throw new NullPointerException("Array element from database is null");
        }
        result = result.cons(f.apply(index).apply(el));
        index ++;
      }
      return result.reverse();
    });
  }

  default Decode<Boolean> exists(Path path) {
    return path.isEmpty()
        ? doc -> true  // todo true, false, exception?
        : path(path, Document::containsKey);
  }

  default <A> Decode<Id<ObjectId, A>> withObjectId(Decode<A> decode) {
    return withId(objectId(Path.single("_id")), decode);
  }

  default <I, A> Decode<Id<I, A>> withId(Decode<I> iddec, Decode<A> adec) {
    return iddec.combine(adec, Id::new);
  }
}
