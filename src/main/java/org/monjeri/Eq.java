package org.monjeri;

public interface Eq<A> {

  boolean notNullEqual(A a1, A a2);

  default boolean equal(A a1, A a2) {
    return a1 == a2 || !(a1 == null || a2 == null) && notNullEqual(a1, a2);
  }

  static <A> Eq<A> objectEq() {
    return Object::equals;
  }

  static <A> Eq<A> referenceEq() {
    return (a, b) -> a == b;
  }

  static <A> Eq<A> neverEq() {
    return (a, b) -> false;
  }

  static <A, B> Eq<Id<A, B>> idEq() {
    return (id1, id2) -> id1.id().equals(id2.id());
  }
}
