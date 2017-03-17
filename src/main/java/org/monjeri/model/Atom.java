package org.monjeri.model;

import org.monjeri.Json;

import java.util.Objects;

public final class Atom implements Type {

  public final Json.Type type;

  private Atom(Json.Type type) {
    this.type = type;
  }

  @Override
  public Json.Type getJsonType() {
    return type;
  }

  @Override
  public Json asJson() {
    return Json.str(type.name());
  }

  public static Atom number() {
    return new Atom(Json.Type.NUMBER);
  }

  public static Atom string() {
    return new Atom(Json.Type.STRING);
  }

  public static Atom bool() {
    return new Atom(Json.Type.BOOL);
  }

  public static Atom objectId() {
    return new Atom(Json.Type.OBJECT_ID);
  }

  public static Ref ref(String target) {
    return new Ref(target);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Atom atom = (Atom) o;
    return type == atom.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type);
  }

  @Override
  public String toString() {
    return asJson().spaces4();
  }
}
