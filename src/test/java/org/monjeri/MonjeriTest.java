package org.monjeri;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.monjeri.Json.JObject;
import org.monjeri.TestDatabase.Person;
import org.monjeri.migrate.Migration;
import org.monjeri.migrate.SimpleChange;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.monjeri.TestUtils.await;
import static org.monjeri.TestUtils.exceptionToString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class MonjeriTest extends AbstractMongoTest {

  @Test
  public void testInsert() throws Exception {
    MonTask1<MongoDatabase> db = newDb();
    Person person = new Person("john", 33, true, new BigDecimal("12111.02"));
    ObjectId id = db
        .map(model().person)
        .map(insert(person.asJson()))
        .map(castToObjectId())
        .run(client());

    List<Person> persons = db.map(model().person)
        .map(query(JObject.byId(id)))
        .map(decode(codec().decodePerson()))
        .map(s -> s.collect(List.collector()))
        .run(client());

    assertEquals(persons.size(), 1);
    assertEquals(persons.elementAt(0), person);
  }


  @Test
  public void testRetry() throws Exception {
    MonTask1<MongoDatabase> database = newDb();
    Person person = new Person("john", 33, true, new BigDecimal("12111.02"));
    RuntimeException error = new RuntimeException("error");
    AtomicInteger c = new AtomicInteger(0);
    MonTask1<Object> insert = database
        .map(model().person)
        .map(insert(person.asJson()))
        .transform(task -> client -> {
          if (c.incrementAndGet() < 5) {
            throw error;
          } else {
            return task.run(client);
          }
        });

    c.set(0);
    Attempt<Object> fail = insert.transform(retry1(3, RuntimeException.class)).attemptRun(client());
    assertTrue(fail.isFailure(), fail.toString());
    assertEquals(fail.asFailure(), Optional.of(error));

    c.set(0);
    Attempt<Object> ok = insert.transform(retry1(8, RuntimeException.class)).attemptRun(client());
    assertTrue(ok.isSuccess(), ok.toString());
  }

  @Test(invocationCount = 5)
  public void testMigrationOnTwoThreads() throws Throwable {
    Migration migration = new Migration(TestUtils.randomDbName());
    CountDownLatch start = new CountDownLatch(1);
    Future<Void> f1 = ForkJoinPool.commonPool().submit(() -> {
      start.await();
      migration.migrate(List.of(new SimpleChange("test", "test",
          MonTask.lift(collection("x")).map(insert(Json.obj("y", Json.num(2)))))))
          .run(client());
      return null;
    });
    Future<Void> f2 = ForkJoinPool.commonPool().submit(() -> {
      start.await();
      migration.migrate(List.of(new SimpleChange("test", "test",
          MonTask.lift(collection("x")).map(insert(Json.obj("y", Json.num(4)))))))
          .run(client());
      return null;
    });
    start.countDown();
    Attempt<Void> a1 = Attempt.eval(f1::get);
    Attempt<Void> a2 = Attempt.eval(f2::get);
    assertTrue(a1.or(a2).isSuccess());
    assertTrue(a1.flatMap(x -> a2).isFailure());
    a1.flatMap(x -> a2).asFailure().ifPresent(ex ->
        assertTrue(ex.getCause() instanceof LockedException, "not a locked exception: " + exceptionToString(ex.getCause())));

    // check if one thread succeeded
    db(migration.getDatabaseName())
        .map(collection("x"))
        .map(query(Json.obj()))
        .map(expectOne())
        .run(client());
  }
}
