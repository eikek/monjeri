package org.monjeri.model;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import org.monjeri.Json;
import org.monjeri.List;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.Monjeri;
import org.monjeri.Path;
import org.monjeri.lock.LockName;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.monjeri.Json.JObject.ofPath;

public class Document implements Type, PathAccess, Function<MongoDatabase, MongoCollection<org.bson.Document>> {

  private final Path basePath;
  private final String name;
  private List<Field<Type>> fields = List.nil();
  private List<IndexModel> indexes = List.nil();

  private Document(Path basePath, String name) {
    this.basePath = basePath;
    this.name = name;
  }

  /**
   * Create an embedded document with a path.
   */
  public Document(Path basePath) {
    this(basePath, null);
  }

  /**
   * Create a top-level document that has a name.
   */
  public Document(String name) {
    this(Path.root, name);
  }

  @SuppressWarnings("unchecked")
  private <T extends Type> Field<T> addField(Field<T> field) {
    this.fields = fields.cons((Field) field);
    return field;
  }

  protected Field<Atom> _idField() {
    return addField(new Field<>(this, basePath.snoc("_id"), Atom.objectId()));
  }

  protected <T extends Type> Field<T> addField(String name, T type) {
    return addField(new Field<>(this, basePath.snoc(name), type));
  }

  protected <T extends Document> Field<T> embed(String name, Function<Path, T> creator) {
    Path base = basePath.snoc(name);
    return addField(new Field<>(this, base, creator.apply(base)));
  }

  protected <T extends Type> Field<Array<T>> arrayField(String name, Function<Path, T> creator) {
    Path base = basePath.snoc(name);
    return addField(new Field<>(this, base, new Array<>(creator, base)));
  }

  protected void addIndex(Json.JObject keys, Consumer<IndexOptions> opts) {
    IndexOptions options = new IndexOptions();
    opts.accept(options);
    this.indexes = indexes.cons(new IndexModel(keys.toDocument(), options));
  }

  protected void addIndex(Json.JObject keys) {
    addIndex(keys, opts -> {});
  }

  public int fieldCount() {
    return fields.size();
  }

  public List<IndexModel> getIndexes() {
    return indexes;
  }

  public MonTask<MongoDatabase, List<String>> createIndexes() {
    Monjeri tasks = Monjeri.getInstance();
    return MonTask.lift(tasks.collection(this))
        .map(coll -> indexes.isEmpty() ? Collections.<String>emptyList() : coll.createIndexes(indexes.toJava()))
        .map((Function<java.util.List<String>, List<String>>) List::fromList);
  }

  public Optional<Field<Type>> findField(Path path) {
    if (path.startsWith(basePath)) {
      path = path.dropFirst(basePath.length());
    }
    return findField0(path, this, null);
  }

  private Optional<Field<Type>> findField0(Path path, Type doc, Field<Type> field) {
    if (path.isEmpty()) {
      return Optional.ofNullable(field);
    } else {
      if (doc.getJsonType() == Json.Type.OBJECT) {
        String name = path.first();
        Field<Type> next = ((Document) doc).fields.find(f -> f.path().endsWith(name)).orElse(null);
        return findField0(path.dropFirst(), next.type(), next);
      } else {
        return Optional.empty();
      }
    }
  }

  public LockName lockName(Object id) {
    return () -> name() + ":" + Objects.requireNonNull(id).toString();
  }

  @Override
  public MongoCollection<org.bson.Document> apply(MongoDatabase mongoDatabase) {
    return mongoDatabase.getCollection(name());
  }

  public Path path() {
    return basePath;
  }

  @Override
  public String name() {
    return this.name != null ? this.name : PathAccess.super.name();
  }

  @Override
  public Json.Type getJsonType() {
    return Json.Type.OBJECT;
  }

  @Override
  public Json asJson() {
    return fields.foldLeft(Json.obj(), (json, field) -> json.put(field.path().last(), field.type().asJson()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Document document = (Document) o;
    return Objects.equals(basePath, document.basePath) &&
        Objects.equals(fields, document.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(basePath, fields);
  }

  @Override
  public String toString() {
    return asJson().spaces4();
  }

  public static class Field<T extends Type> implements PathAccess {
    private final Document parent;
    private final Path path;
    private final T type;

    Field(Document parent, Path path, T type) {
      this.parent = parent;
      this.path = path;
      this.type = type;
    }

    public Field<T> mapPath(Function<Path, Path> f) {
      return new Field<>(parent, f.apply(path), type);
    }

    public Path path() {
      return path;
    }

    public T type() {
      return type;
    }

    public Field<T> indexed(Consumer<IndexOptions> config) {
      parent.addIndex(Json.obj(ofPath(path(), Json.num(1))), config);
      return this;
    }

    public Field<T> indexed() {
      return indexed(c -> {});
    }

    public Field<T> unique() {
      return indexed(cfg -> cfg.unique(true));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Field<?> field = (Field<?>) o;
      return Objects.equals(path, field.path) &&
          Objects.equals(type, field.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, type);
    }

    @Override
    public String toString() {
      return "Field{" +
          "path=" + path +
          ", type=" + type +
          '}';
    }
  }
}
