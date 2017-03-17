package org.monjeri.migrate;

import com.mongodb.client.MongoDatabase;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;

public class SimpleChange implements Change {
  private final String id;
  private final String author;
  private final MonTask<MongoDatabase, Void> task;

  public SimpleChange(String id, String author, MonTask<MongoDatabase, ?> task) {
    this.id = id;
    this.author = author;
    this.task = task.drain();
  }

  public SimpleChange(String id, String author, MonTask1<?> task) {
    this(id, author, MonTask.lift(task));
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public String author() {
    return author;
  }

  @Override
  public MonTask<MongoDatabase, Void> task() {
    return task;
  }
}
