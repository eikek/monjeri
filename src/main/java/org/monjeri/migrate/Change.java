package org.monjeri.migrate;

import com.mongodb.client.MongoDatabase;
import org.monjeri.MonTask;

public interface Change {
  String id();
  String author();
  MonTask<MongoDatabase, Void> task();
}
