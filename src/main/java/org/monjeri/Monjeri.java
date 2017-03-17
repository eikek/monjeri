package org.monjeri;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.gridfs.GridFS;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.monjeri.Json.JObject;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Monjeri {
  static Monjeri getInstance() {
    return new Monjeri() {};
  }

  default MonTask1<MongoDatabase> db(String name) {
    return mongo -> mongo.getDatabase(name);
  }

  default MonTask<MongoDatabase, Void> dropDatabase() {
    return (db, client) -> {
      db.drop();
      return null;
    };
  }

  @SuppressWarnings("deprecation")
  default MonTask<MongoDatabase, GridFS> gridFs(String bucketName) {
    return (db, client) -> new GridFS(client.getDB(db.getName()), bucketName);
  }

  @SuppressWarnings("deprecation") // unfortunately, getDB() is deprecated without replacement for GridFS
  default MonTask<MongoDatabase, GridFS> gridFs() {
    return (db, client) -> new GridFS(client.getDB(db.getName()), GridFS.DEFAULT_BUCKET);
  }

  default Function<MongoDatabase, MongoCollection<Document>> collection(String name) {
    return db -> db.getCollection(name);
  }

  default Function<MongoDatabase, MongoCollection<Document>> collection(org.monjeri.model.Document coll) {
    return db -> db.getCollection(coll.name());
  }

  default MonTask<MongoCollection<Document>, Void> drop() {
    return (coll, client) -> {
      coll.drop();
      return null;
    };
  }

  default Function<MongoCollection<Document>, Long> count(JObject filter, CountOptions options) {
    return coll -> coll.count(filter.toDocument(), options);
  }

  default Function<MongoCollection<Document>, Long> count(JObject filter) {
    return count(filter, new CountOptions());
  }

  default Function<MongoCollection<Document>, String> createIndex(JObject keys, IndexOptions options) {
    return coll -> coll.createIndex(keys.toDocument(), options);
  }

  default Function<MongoCollection<Document>, String> createIndex(IndexModel model) {
    return coll -> coll.createIndex(model.getKeys(), model.getOptions());
  }

  default Function<MongoCollection<Document>, String> createIndex(JObject keys) {
    return createIndex(keys, new IndexOptions());
  }


  default <A, B> Function<MonTask<A, B>, MonTask<A, List.P2<B, Duration>>> timed() {
    AtomicLong time = new AtomicLong(0);
    MonTask<A, B> start = MonTask.defer(() -> {
      time.set(System.nanoTime());
      return null;
    });
    return task -> start.flatMap(ign -> task)
        .thenDo((ign, cl) -> time.addAndGet(-System.nanoTime()))
        .map(b -> List.P2.of(b, Duration.ofNanos(-time.get())));
  }

  default <A> Function<MonTask1<A>, MonTask1<List.P2<A, Duration>>> timed1() {
    return task -> {
      MonTask<Object, List.P2<A, Duration>> t2 = this.<Object, A>timed().apply(MonTask.lift(task));
      return client -> t2.run(null, client);
    };
  }


  default Function<MongoCollection<Document>, Sized<Document>> query(JObject query) {
    return a -> {
      long size = a.count(query.toDocument());
      FindIterable<Document> iterable = a.find(query.toDocument());
      return new Sized<>(iterable, size);
    };
  }

  default <A> Function<Sized<A>, Sized<A>> batchSize(int n) {
    return a -> a.set(iter -> iter.batchSize(n));
  }

  default <A> Function<Sized<A>, Sized<A>> project(JObject projection) {
    return a -> a.set(iter -> iter.projection(projection.toDocument()));
  }

  default <A> Function<Sized<A>, Sized<A>> limit(int n) {
    return a -> a.set(iter -> iter.limit(n));
  }

  default <A> Function<Sized<A>, Sized<A>> sort(JObject sort) {
    return a -> a.set(iter -> iter.sort(sort.toDocument()));
  }

  default <A, B> Function<Sized<A>, Stream<List<B>>> grouped(int n, Function<A, B> f) {
    return iter -> Util.makeStream(Optional.empty(), Util.group(iter, n)).map(l -> l.map(f));
  }

  default <A> Function<Sized<A>, Optional<A>> first() {
    return iter -> Optional.ofNullable(iter.getDelegate().first());
  }

  default <A> Function<Sized<A>, A> expectOne() {
    return iter -> {
      Iterator<A> limit = iter.getDelegate().limit(2).iterator();
      if (limit.hasNext()) {
        A fst = limit.next();
        if (limit.hasNext()) {
          throw new IllegalStateException("expected one elmeent but there were more");
        }
        return fst;
      } else {
        throw new IllegalStateException("expected one element but result is empty");
      }
    };
  }

  default <A> Function<Sized<Document>, Stream<A>> decode(Decode<A> decode) {
    return a -> Util.makeStream(a.size(), a.getDelegate().map(decode.mapper()));
  }

  default <A> Function<MongoCollection<Document>, Stream<A>> aggregate(List<JObject> pipeline, Decode<A> decode) {
    return a -> {
      MongoIterable<A> iter = a.aggregate(pipeline.map(JObject::toDocument).toJava())
          .allowDiskUse(false)
          .map(decode.mapper());
      return Util.makeStream(Optional.empty(), iter);
    };
  }

  default Function<MongoCollection<Document>, UpdateResult> updateMany(JObject filter, JObject update, UpdateOptions options) {
    return coll -> coll.updateMany(filter.toDocument(), update.toDocument(), options);
  }

  default Function<MongoCollection<Document>, UpdateResult> updateMany(JObject filter, JObject update) {
    return updateMany(filter, update, new UpdateOptions());
  }

  default Function<MongoCollection<Document>, UpdateResult> updateOne(JObject filter, JObject update, UpdateOptions options) {
    return coll -> coll.updateOne(filter.toDocument(), update.toDocument(), options);
  }

  default Function<MongoCollection<Document>, UpdateResult> updateOne(JObject filter, JObject update) {
    return updateOne(filter, update, new UpdateOptions());
  }

  default <A> Function<MongoCollection<Document>, Optional<A>> findOneAndUpdate(JObject filter, JObject update, FindOneAndUpdateOptions options, Decode<A> decode) {
    return coll -> Optional.ofNullable(coll.findOneAndUpdate(filter.toDocument(), update.toDocument(), options)).map(decode);
  }

  default <A> Function<MongoCollection<Document>, Optional<A>> findOneAndUpdate(JObject filter, JObject update, Decode<A> decode) {
    return findOneAndUpdate(filter, update, new FindOneAndUpdateOptions(), decode);
  }

  default Function<MongoCollection<Document>, Document> findOneAndDelete(JObject filter, FindOneAndDeleteOptions options) {
    return coll -> coll.findOneAndDelete(filter.toDocument(), options);
  }

  default Function<MongoCollection<Document>, Document> findOneAndDelete(JObject filter) {
    return coll -> coll.findOneAndDelete(filter.toDocument(), new FindOneAndDeleteOptions());
  }

  default Function<MongoCollection<Document>, Object> insert(JObject document, InsertOneOptions options) {
    return coll -> {
      Document doc = document.toDocument();
      coll.insertOne(doc, options);
      return doc.get("_id");
    };
  }

  default Function<MongoCollection<Document>, Object> insert(JObject document) {
    return insert(document, new InsertOneOptions());
  }

  default Function<MongoCollection<Document>, List<Object>> insertMany(List<JObject> documents, InsertManyOptions options) {
    return coll -> {
      java.util.List<Document> mutable = documents.map(JObject::toDocument).toJava();
      coll.insertMany(mutable, options);
      return List.fromList(mutable, doc -> doc.get("_id"));
    };
  }

  default Function<MongoCollection<Document>, List<Object>> insertMany(List<JObject> documents) {
    return insertMany(documents, new InsertManyOptions());
  }

  default Function<MongoCollection<Document>, DeleteResult> deleteMany(JObject query, DeleteOptions options) {
    return coll -> coll.deleteMany(query.toDocument(), options);
  }

  default Function<MongoCollection<Document>, DeleteResult> deleteMany(JObject query) {
    return deleteMany(query, new DeleteOptions());
  }

  default Function<MongoCollection<Document>, DeleteResult> deleteOne(JObject query, DeleteOptions options) {
    return coll -> coll.deleteOne(query.toDocument(), options);
  }

  default Function<MongoCollection<Document>, DeleteResult> deleteOne(JObject query) {
    return coll -> coll.deleteOne(query.toDocument(), new DeleteOptions());
  }

  default Function<MongoCollection<Document>, UpdateResult> replaceOne(JObject query, JObject doc, UpdateOptions options) {
    return coll -> coll.replaceOne(query.toDocument(), doc.toDocument(), options);
  }

  default Function<MongoCollection<Document>, UpdateResult> replaceOne(JObject query, JObject doc) {
    return coll -> coll.replaceOne(query.toDocument(), doc.toDocument(), new UpdateOptions());
  }

  default <A, B> Function<MonTask<A, B>, MonTask<A, B>> retry(int max, Class<? extends RuntimeException> when) {
    if (max <= 0) {
      throw new IllegalArgumentException("max must be greater than 0");
    }
    return intask -> (a, client) -> {
      RuntimeException first = null;
      for (int i=0; i<max; i++) {
        try {
          return intask.run(a, client);
        } catch (RuntimeException e) {
          if (when.isInstance(e)) {
            if (first == null) {
              first = e;
            }
          } else {
            throw e;
          }
        }
      }
      if (first != null) {
        throw first;
      } else {
        throw new AssertionError("unreachable code");
      }
    };
  }

  default <A, B> Function<MonTask<A, B>, MonTask<A, B>> retry(int max) {
    return retry(max, MongoException.class);
  }

  default <A> Function<MonTask1<A>, MonTask1<A>> retry1(int max, Class<? extends RuntimeException> when) {
    return intask -> client -> {
      MonTask<MongoClient, A> task = (c1, c2) -> intask.run(c1);
      return this.<MongoClient, A>retry(max, when).apply(task).run(client, client);
    };
  }

  default <A> Function<MonTask1<A>, MonTask1<A>> retry1(int max) {
    return retry1(max, MongoException.class);
  }


  default <A> Function<Json.JDBRef, A> resolve1(MongoDatabase db, Decode<A> decode) {
    throw new UnsupportedOperationException();
  }

  default <A> Function<MongoDatabase, A> resolve2(Json.JDBRef ref, Decode<A> decode) {
    throw new UnsupportedOperationException();
  }

  default Function<Object, ObjectId> castToObjectId() {
    return obj -> (ObjectId) obj;
  }

  default Function<Object, Document> castToDocument() {
    return obj -> (Document) obj;
  }
}
