package org.monjeri;

import com.mongodb.DBRef;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;

public interface BaseEncode {

  default Encode<String> stringEncdoe() {
    return Json::str;
  }

  default Encode<Integer> intEncode() {
    return Json::num;
  }

  default Encode<Long> longEncode() {
    return Json::num;
  }

  default Encode<Double> doubleEncode() {
    return Json::num;
  }

  default Encode<BigDecimal> decimalEncode() {
    return Json::num;
  }

  default Encode<Boolean> boolEncode() {
    return Json::of;
  }

  default Encode<Json> jsonEncode() {
    return a -> a;
  }

  default Encode<ObjectId> idEncode() {
    return Json::id;
  }

  default Encode<DBRef> dbrefEncode() {
    return Json::dbref;
  }

  default Encode<Instant> instantEncode() {
    return i -> Json.str(i.toString());
  }

  default Encode<ZonedDateTime> zonedDateTimeEncode() {
    return i -> Json.str(i.toString());
  }

  default Encode<Duration> durationEncode() {
    return d -> Json.str(d.toString());
  }

  default <A> Encode<List<A>> listEncode(Encode<A> encode) {
    return list -> Json.array(list.map(encode));
  }

  default <A> Encode<List<A>> listWithIndexEncode(Function<Integer, Encode<A>> encode) {
    return list -> Json.array(list.zipWithIndex().map(p2 -> encode.apply(p2._2).apply(p2._1)));
  }

  default <A> Encode<List<A>> listWithIndexEncode(Encode.Object<A> encode, String positionKey) {
    return listWithIndexEncode(index -> a -> encode.apply(a).put(positionKey, Json.num(index)));
  }

  default <A> Encode<Optional<A>> optionalEncode(Encode<A> encode) {
    return opt -> opt.map(encode).orElse(Json.Null());
  }
}
