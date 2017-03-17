package org.monjeri;

import org.monjeri.model.Document;

import java.util.function.Function;

public final class Id<I, A> {

  public final I id;
  public final A value;

  public Id(I id, A value) {
    this.id = id;
    this.value = value;
  }

  public Json.JDBRef asDBRef(String collection) {
    return Json.dbref(collection, id);
  }

  public Json.JDBRef asDBRef(Document collection) {
    return asDBRef(collection.name());
  }

  public <B> Id<I, B> update(Function<A, B> f) {
    return new Id<>(id, f.apply(value));
  }

  public I id() {
    return id;
  }

  public A value() {
    return value;
  }

  @Override
  public String toString() {
    return "Id{" +
        "id=" + id +
        ", value=" + value +
        '}';
  }
}
