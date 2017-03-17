package org.monjeri.migrate;

import org.monjeri.BaseDecode;
import org.monjeri.BaseEncode;
import org.monjeri.Decode;
import org.monjeri.Encode;
import org.monjeri.Json;
import org.monjeri.List;
import org.monjeri.MonTask1;
import org.monjeri.Monjeri;
import org.monjeri.lock.Lock;
import org.monjeri.lock.LockName;
import org.monjeri.model.Atom;
import org.monjeri.model.CollectionModels;
import org.monjeri.model.Document;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.monjeri.Json.JObject.ofName;

public final class Migration {

  private final Model model;
  private final Tasks tasks;

  public Migration(String databaseName, String collectionName) {
    this.model = new Model(databaseName, collectionName);
    Codec codec = new Codec(model);
    this.tasks = new Tasks(model, codec);
  }

  public Migration(String databaseName) {
    this(databaseName, "monjeri_migration");
  }

  public Migration(CollectionModels db) {
    this(db.getDatabaseName());
  }

  public String getDatabaseName() {
    return model.getDatabaseName();
  }

  public MonTask1<List<Result>> migrate(List<Change> changes) {
    Change c0 = new SimpleChange("monjeri-migrate-indexes", "monjeri", model.createIndexes());
    Instant started = Instant.now();
    return tasks.locked(MonTask1.seq(
        changes.cons(c0)
            .zipWithIndex()
            .map(p2 -> tasks.makeChange(p2._1, p2._2, started))));
  }

  public MonTask1<Stream<ChangeRun>> getChanges() {
    return tasks.getChanges();
  }

  private enum Locks implements LockName {
    MONJERI_MIGRATION_LOCK
  }

  static class Model extends CollectionModels {
    public final ChangeRunModel changeRun;
    public final Lock lock;

    static class ChangeRunModel extends Document {
      public final Field<Atom> changeId = addField("changeid", Atom.string()).unique();
      public final Field<Atom> author = addField("author", Atom.string());
      public final Field<Atom> started = addField("started", Atom.string()).indexed();
      public final Field<Atom> duration = addField("duration", Atom.string());
      public final Field<Atom> index = addField("position", Atom.number());

      public ChangeRunModel(String collectionName) {
        super(collectionName);
      }
    }

    public Model(String database, String collectionName) {
      super(database);
      changeRun = add(new ChangeRunModel(collectionName));
      lock = lockFor(Locks.MONJERI_MIGRATION_LOCK);
    }
  }

  static class Codec implements BaseEncode, BaseDecode {
    private final Model model;

    Codec(Model model) {
      this.model = model;
    }

    public Decode<ChangeRun> decodeChangeRun() {
      return doc -> new ChangeRun(
          string(model.changeRun.changeId.nameAsPath()).apply(doc),
          stringNullable(model.changeRun.author.nameAsPath()).apply(doc),
          instant(model.changeRun.started.nameAsPath()).apply(doc),
          duration(model.changeRun.duration.nameAsPath()).apply(doc));
    }

    public Encode.Object<ChangeRun> encodeChangeRun() {
      return run -> Json.obj(
          ofName(model.changeRun.changeId, Json.str(run.changeId)),
          ofName(model.changeRun.author, Json.str(run.author)),
          ofName(model.changeRun.duration, durationEncode().apply(run.duration)),
          ofName(model.changeRun.started, instantEncode().apply(run.started)));
    }
  }

  static class Tasks implements Monjeri {
    private final Model model;
    private final Codec codec;

    public Tasks(Model model, Codec codec) {
      this.model = model;
      this.codec = codec;
    }

    public MonTask1<Stream<ChangeRun>> getChanges() {
      return model.database()
          .map(model.changeRun)
          .map(query(Json.obj()))
          .map(sort(Json.obj(
              ofName(model.changeRun.started, Json.num(1)),
              ofName(model.changeRun.index, Json.num(2))
          )))
          .map(decode(codec.decodeChangeRun()));
    }

    public MonTask1<Result> makeChange(Change change, int index, Instant started) {
      MonTask1<Optional<Result>> find = model.database()
          .map(model.changeRun)
          .map(query(Json.obj(model.changeRun.changeId.name(), Json.str(change.id()))))
          .map(first())
          .map(opt -> opt.map(codec.decodeChangeRun()).map(Result::skipped));

      MonTask1<Result> execute = model.database()
          .map(change.task().transform(timed()))
          .map(d -> new ChangeRun(change.id(), change.author(), started, d._2))
          .flatMap(result -> model.database()
              .map(model.changeRun)
              .map(insert(codec.encodeChangeRun().apply(result).put(model.changeRun.index.name(), Json.num(index))))
              .map(x -> Result.executed(result)));

      return find.flatMap(opt -> opt.isPresent() ? MonTask1.unit(opt.get()) : execute);
    }

    public <A> MonTask1<A> locked(MonTask1<A> intask) {
      return intask.transform(model.lock.withLock1());
    }
  }
}
