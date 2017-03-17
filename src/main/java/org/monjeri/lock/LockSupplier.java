package org.monjeri.lock;

import org.monjeri.model.Atom;
import org.monjeri.model.CollectionModels;
import org.monjeri.model.Document;

public final class LockSupplier {

  private final LockModel model;

  public LockSupplier(String database, String collectionName) {
    this.model = new LockModel(database, collectionName);
  }
  public LockSupplier(String database) {
    this(database, "monjeri_locks");
  }

  public Lock get(LockName name) {
    return new Lock(model, name);
  }

  static final class LockModel extends CollectionModels {

    public final Locks locks;

    public LockModel(String database, String collectionName) {
      super(database, null);
      this.locks = new Locks(collectionName);
    }

    static class Locks extends Document {
      public final Field<Atom> _id = addField("_id", Atom.string());
      public final Field<Atom> locked = addField("locked", Atom.string());

      public Locks(String name) {
        super(name);
      }
    }
  }
}
