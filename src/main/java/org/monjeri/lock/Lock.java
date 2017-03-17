package org.monjeri.lock;

import org.monjeri.BaseDecode;
import org.monjeri.Json;
import org.monjeri.LockedException;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.Monjeri;
import org.monjeri.lock.LockSupplier.LockModel;

import java.util.Optional;
import java.util.function.Function;

import static org.monjeri.Json.JObject.ofName;

public final class Lock {

  private final LockModel model;
  private final String name;
  final Tasks tasks = new Tasks();

  public Lock(LockModel model, LockName name) {
    this.model = model;
    this.name = name.name().toLowerCase();
  }

  public <A> Function<MonTask1<A>, MonTask1<A>> withLock1() {
    return task -> tasks.acquire()
        .flatMap(x -> task)
        .alwaysRun(tasks.release());
  }

  public <A, B> Function<MonTask<A, B>, MonTask<A,B>> withLock() {
    return task -> MonTask.<A, Void>lift(tasks.acquire())
        .flatMap(x -> task)
        .alwaysRun(MonTask.lift(tasks.release()));
  }

  public MonTask1<Optional<String>> lockedBy() {
    return tasks.lockedBy();
  }

  public MonTask1<Boolean> isLocked() {
    return lockedBy().map(Optional::isPresent);
  }

  class Tasks implements Monjeri {
    MonTask1<Optional<String>> lockedBy() {
      return model.database()
          .map(model.locks)
          .map(query(Json.obj(
              ofName(model.locks._id, Json.str(name))
          )))
          .map(first())
          .map(opt -> opt.map(BaseDecode.getInstance().string(model.locks.locked.path())));
    }

    MonTask1<Void> acquire() {
      String thread = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
      return model.database()
          .map(model.locks)
          .map(insert(Json.obj(
              ofName(model.locks._id, Json.str(name)),
              ofName(model.locks.locked, Json.str(thread))))
          ).onError(ex -> lockedBy().map(n -> {
            throw new LockedException("Lock '" + name + "' is locked by: " + n.orElse("<none>"));
          })).drain();
    }

    MonTask1<Void> release() {
      return model.database()
          .map(model.locks)
          .map(findOneAndDelete(
              Json.obj(ofName(model.locks._id, Json.str(name)))
          )).drain();
    }
  }
}
