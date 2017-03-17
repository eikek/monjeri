package org.monjeri.model;

import com.mongodb.client.MongoDatabase;
import org.monjeri.List;
import org.monjeri.MonTask1;
import org.monjeri.Monjeri;
import org.monjeri.lock.Lock;
import org.monjeri.lock.LockName;
import org.monjeri.lock.LockSupplier;

public class CollectionModels {

  private List<Document> collections = List.nil();
  private final String database;
  private final Monjeri tasks = Monjeri.getInstance();
  private final LockSupplier lockSupplier;

  protected CollectionModels(String database, LockSupplier lockSupplier) {
    this.database = database;
    this.lockSupplier = lockSupplier;
  }

  public CollectionModels(String database) {
    this(database, new LockSupplier(database));
  }

  protected <T extends Document> T add(T model) {
    this.collections = collections.cons(model);
    return model;
  }

  public String getDatabaseName() {
    return database;
  }

  public List<Document> getCollections() {
    return collections;
  }

  public MonTask1<MongoDatabase> database() {
    return tasks.db(database);
  }

  public Lock lockFor(LockName name) {
    return lockSupplier.get(name);
  }

  /**
   * Create a task that creates all declared indexes and returns its names.
   */
  public MonTask1<List<String>> createIndexes() {
    return MonTask1.seq(collections
        .map(Document::createIndexes)
        .map(task -> database().map(task)))
        .map(ll -> ll.foldLeft(List.nil(), List::append));
  }

}
