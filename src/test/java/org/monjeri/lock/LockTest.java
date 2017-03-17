package org.monjeri.lock;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.monjeri.AbstractMongoTest;
import org.monjeri.Attempt;
import org.monjeri.Json;
import org.monjeri.Json.JObject;
import org.monjeri.LockedException;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.TestDatabase.Person;
import org.monjeri.TestUtils;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.monjeri.TestUtils.await;
import static org.monjeri.TestUtils.exceptionToString;
import static org.monjeri.TestUtils.randomDbName;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class LockTest extends AbstractMongoTest {

  enum Locks implements LockName {
    ACCOUNT
  }

  @Test
  public void testSimpleAcquire() throws Exception {
    String dbname = randomDbName();
    Lock lock = new LockSupplier(dbname).get(Locks.ACCOUNT);
    lock.tasks.acquire().run(client());
    assertTrue(lock.lockedBy().run(client()).get().contains(Thread.currentThread().getName()));
  }

  @SuppressWarnings("unchecked")
  @Test(invocationCount = 5)
  public void testConcurrentAcquire() throws Exception {
    String dbname = randomDbName();
    Lock lock = new LockSupplier(dbname).get(Locks.ACCOUNT);

    CountDownLatch start = new CountDownLatch(1);
    CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
      await(start);
      lock.tasks.acquire().run(client());
    });
    CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
      await(start);
      lock.tasks.acquire().run(client());
    });
    start.countDown();

    Attempt<Void> a1 = Attempt.eval(f1::get).onError(this::unwrap);
    Attempt<Void> a2 = Attempt.eval(f2::get).onError(this::unwrap);
    //one of them must succeed
    assertTrue(a1.or(a2).isSuccess());
    //one of them not
    assertTrue(a1.flatMap(x -> a2).isFailure());
    //because of locked error
    a1.flatMap(x -> a2)
        .asFailure()
        .ifPresent(ex -> assertTrue(ex instanceof LockedException, exceptionToString(ex)));
    assertTrue(lock.lockedBy().run(client()).isPresent());
  }

  @Test
  public void testRelease() throws Exception {
    String dbname = TestUtils.randomDbName();
    Lock lock = new LockSupplier(dbname).get(Locks.ACCOUNT);

    lock.tasks.acquire().run(client());
    assertTrue(lock.lockedBy().run(client()).isPresent());
    lock.tasks.release().run(client());
    Attempt<Optional<String>> f = lock.lockedBy().attemptRun(client());
    assertTrue(f.isSuccess());
    assertFalse(f.asSuccess().flatMap(x -> x).isPresent());
  }

  @Test
  public void testWithLockFailing() throws Exception {
    MonTask1<MongoDatabase> database = newDb();
    Person person = new Person("john", 33, true, new BigDecimal("12111.02"));
    ObjectId id = database
        .map(collection(model().person))
        .map(insert(person.asJson()))
        .map(castToObjectId())
        .run(client());

    Lock lock = model().lockFor(() -> "person:" + id);
    MonTask<MongoCollection<Document>, UpdateResult> update = MonTask.lift(
        updateOne(JObject.byId(id), Json.obj("$inc",
            Json.obj(model().person.age.name(), Json.num(1)))))
        .transform(lock.withLock());

    CountDownLatch start = new CountDownLatch(1);
    Future<UpdateResult> f1 = CompletableFuture.supplyAsync(() -> {
      await(start);
      return database.map(collection(model().person)).map(update).run(client());
    });
    Future<UpdateResult> f2 = CompletableFuture.supplyAsync(() -> {
      await(start);
      return database.map(collection(model().person)).map(update).run(client());
    });
    start.countDown();

    Attempt<UpdateResult> a1 = Attempt.eval(f1::get).onError(this::unwrap);
    Attempt<UpdateResult> a2 = Attempt.eval(f2::get).onError(this::unwrap);
    //one of them must succeed
    assertTrue(a1.or(a2).isSuccess());
    //one of them not
    assertTrue(a1.flatMap(x -> a2).isFailure());
    //because of locked error
    a1.flatMap(x -> a2)
        .asFailure()
        .ifPresent(ex -> assertTrue(ex instanceof LockedException, exceptionToString(ex)));

    assertFalse(lock.lockedBy().run(client()).isPresent());
  }

  @Test
  public void testWithLockAndRetry() throws Exception {
    MonTask1<MongoDatabase> database = newDb();
    Person person = new Person("john", 33, true, new BigDecimal("12111.02"));
    ObjectId id = database
        .map(collection(model().person))
        .map(insert(person.asJson()))
        .map(castToObjectId())
        .run(client());

    Lock lock = model().lockFor(() -> "person:" + id);
    MonTask<MongoCollection<Document>, UpdateResult> update = MonTask.lift(
        updateOne(JObject.byId(id), Json.obj("$inc",
            Json.obj(model().person.age.name(), Json.num(1)))))
    .transform(lock.withLock())
    .transform(retry(20, LockedException.class));

    CountDownLatch start = new CountDownLatch(1);
    Future<UpdateResult> f1 = CompletableFuture.supplyAsync(() -> {
      await(start);
      return database.map(model().person).map(update).run(client());
    });
    Future<UpdateResult> f2 = CompletableFuture.supplyAsync(() -> {
      await(start);
      return database.map(model().person).map(update).run(client());
    });
    start.countDown();

    Attempt<UpdateResult> a1 = Attempt.eval(f1::get);
    Attempt<UpdateResult> a2 = Attempt.eval(f2::get);
    assertTrue(a1.isSuccess() && a2.isSuccess());

    UpdateResult res1 = f1.get();
    assertEquals(1L, res1.getMatchedCount());
    assertEquals(1L, res1.getModifiedCount());
    UpdateResult res2 = f2.get();
    assertEquals(1L, res2.getMatchedCount());
    assertEquals(1L, res2.getModifiedCount());

    Person upd = database.map(collection(model().person))
        .map(query(JObject.byId(id)))
        .map(expectOne())
        .map(codec().decodePerson())
        .run(client());
    assertEquals(upd.getAge(), 35);
  }

  @Test
  public void testWithLockAndError() throws Exception {
    MonTask1<MongoDatabase> database = newDb();
    Person person = new Person("john", 33, true, new BigDecimal("12111.02"));
    ObjectId id = database
        .map(collection(model().person))
        .map(insert(person.asJson()))
        .map(castToObjectId())
        .run(client());

    RuntimeException error = new RuntimeException("error");
    Lock lock = model().lockFor(() -> "person:" + id);
    MonTask<MongoCollection<Document>, UpdateResult> update =
        (a, cl) -> {
          throw error;
        };
    update = update.transform(lock.withLock());

    Attempt<UpdateResult> a = database
        .map(collection(model().person))
        .map(update)
        .attemptRun(client());
    assertTrue(a.isFailure());
    a.asFailure().ifPresent(e -> assertSame(e, error));

    // check whether lock has been removed
    assertFalse(lock.lockedBy().run(client()).isPresent());
  }


  private <A> Attempt<A> unwrap(Exception error) {
    if (error instanceof ExecutionException) {
      ExecutionException executionException = (ExecutionException) error;
      return Attempt.fail((Exception) executionException.getCause());
    } else {
      return Attempt.fail(error);
    }
  }

}