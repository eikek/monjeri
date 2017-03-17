package org.monjeri.model;

import org.monjeri.Json;

import java.util.Objects;

public final class Ref implements Type {
  private final String  collection;

  public Ref(String collection) {
    this.collection = collection;
  }

  public static Ref create(String target) {
    return new Ref(target);
  }

  @Override
  public Json.Type getJsonType() {
    return Json.Type.DBREF;
  }

  @Override
  public Json asJson() {
    return Json.obj("target", Json.str(collection))
        .put("type", Json.str(getJsonType().name()));
  }

  public String target() {
    return collection;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ref ref = (Ref) o;
    return Objects.equals(collection, ref.collection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(collection);
  }

  @Override
  public String toString() {
    return asJson().spaces4();
  }
}
