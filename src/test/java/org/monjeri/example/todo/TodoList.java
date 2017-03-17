package org.monjeri.example.todo;

import org.monjeri.List;

import java.time.Instant;

public final class TodoList<A> {

  private final String title;
  private final A owner;
  private final List<Todo> todos;
  private final Instant created;

  public TodoList(String title, A owner, List<Todo> todos, Instant created) {
    this.title = title;
    this.owner = owner;
    this.todos = todos;
    this.created = created;
  }

  public TodoList(String title, A owner, List<Todo> todos) {
    this(title, owner, todos, Instant.now());
  }

  public static <A> TodoList<A> createEmpty(String title, A owner) {
    return new TodoList<>(title, owner, List.nil());
  }

  public A getOwner() {
    return owner;
  }

  public List<Todo> getTodos() {
    return todos;
  }

  public Instant getCreated() {
    return created;
  }

  public String getTitle() {
    return title;
  }

  public TodoList<A> add(Todo item) {
    return new TodoList<>(title, owner, todos.cons(item), created);
  }
}
