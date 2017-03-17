package org.monjeri.example.todo;

import java.util.Objects;

public final class Todo {

  private final String name;
  private final boolean checked;

  public Todo(String name, boolean checked) {
    this.name = name;
    this.checked = checked;
  }

  public Todo(String name) {
    this(name, false);
  }

  public Todo checked(boolean flag) {
    return new Todo(name, flag);
  }

  public Todo withName(String name) {
    return new Todo(name, checked);
  }

  public String getName() {
    return name;
  }

  public boolean isChecked() {
    return checked;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Todo todo = (Todo) o;
    return checked == todo.checked &&
        Objects.equals(name, todo.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, checked);
  }

  @Override
  public String toString() {
    return "Todo{" +
        "name='" + name + '\'' +
        ", checked=" + checked +
        '}';
  }
}
