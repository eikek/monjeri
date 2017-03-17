package org.monjeri.model;

import org.monjeri.Json;
import org.monjeri.Path;

import java.util.Objects;
import java.util.function.Function;

public final class Array<E extends Type> implements Type, PathAccess {

  private final Function<Path, E> elementSupplier;
  private final Path base;
  private final E element;

  public Array(Function<Path, E> element, Path base) {
    this.elementSupplier = element;
    this.base = base;
    this.element = elementSupplier.apply(base);
  }

  public E element() {
    return element;
  }

  public E at(int index) {
    return elementSupplier.apply(base.snoc(String.format("%d", index)));
  }

  public E each() {
    return elementSupplier.apply(base.snoc("$"));
  }

  @Override
  public Json.Type getJsonType() {
    return Json.Type.ARRAY;
  }

  @Override
  public Json asJson() {
    return Json.array(element.asJson());
  }

  public Path path() {
    return base;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Array<?> array = (Array<?>) o;
    return Objects.equals(base, array.base) &&
        Objects.equals(element, array.element);
  }

  @Override
  public int hashCode() {
    return Objects.hash(base, element);
  }

  @Override
  public String toString() {
    return asJson().spaces4();
  }
}
