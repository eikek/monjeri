package org.monjeri.example.todo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.monjeri.BaseDecode;
import org.monjeri.BaseEncode;
import org.monjeri.Decode;
import org.monjeri.Encode;
import org.monjeri.Id;
import org.monjeri.Json;
import org.monjeri.Json.JDBRef;
import org.monjeri.Json.JObject;
import org.monjeri.List;
import org.monjeri.LockedException;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.Monjeri;
import org.monjeri.Path;
import org.monjeri.lock.Lock;
import org.monjeri.migrate.Change;
import org.monjeri.migrate.Migration;
import org.monjeri.migrate.Result;
import org.monjeri.migrate.SimpleChange;
import org.monjeri.model.Array;
import org.monjeri.model.Atom;
import org.monjeri.model.CollectionModels;
import org.monjeri.model.Ref;

import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.monjeri.Json.JObject.entry;
import static org.monjeri.Json.JObject.ofName;
import static org.monjeri.Json.JObject.ofPath;
import static org.monjeri.Json.array;

/**
 * This class contains everything needed to connect the application to a mongodb database.
 *
 * In real life, this may be distributed across multiple files, of course. I found it
 * useful to have a single point that defines a model, the codec and the set of tasks.
 * Application code usually only ever needs the task collection, though.
 */
public class TodoDB {
  public final Model model;
  public final Codec codec;
  public final Tasks tasks;

  public TodoDB(String database) {
    this.model = new Model(database);
    this.codec = new Codec(model);
    this.tasks = new Tasks(database, codec, model);
  }

  public MonTask1<List<Result>> initialize() {
    Migration migration = new Migration(model);
    List<Change> changes = List.of(
        new SimpleChange("make indexes", "me", model.createIndexes()),
        new SimpleChange("create admin account", "me", tasks.createAdminAccount("admin", "admin"))
    );
    return migration.migrate(changes);
  }

  public static TodoDB getDefault() {
    return new TodoDB("todoapp");
  }

  public String getDatabaseName() {
    return tasks.database;
  }

  public static class Model extends CollectionModels {
    public final UserModel user = add(new UserModel());
    public final TodoListModel todoList = add(new TodoListModel());

    public static class UserModel extends org.monjeri.model.Document {
      public static final String NAME = "users";

      public final Field<Atom> _id = _idField();
      public final Field<Atom> username = addField("username", Atom.string()).unique();
      public final Field<Atom> firstname = addField("firstname", Atom.string());
      public final Field<Atom> lastname = addField("lastname", Atom.string());
      public final Field<Atom> email = addField("email", Atom.string()).unique();
      public final Field<Atom> active = addField("active", Atom.bool());
      public final Field<Atom> created = addField("created", Atom.string());
      public final Field<Atom> admin = addField("admin", Atom.bool());
      public final Field<Atom> password = addField("password", Atom.string());

      public UserModel() {
        super(Path.root);
      }

      public final String name() {
        return NAME;
      }
    }

    public static class TodoModel extends org.monjeri.model.Document {
      public final Field<Atom> title = addField("title", Atom.string());
      public final Field<Atom> checked = addField("checked", Atom.bool());

      public TodoModel(Path basePath) {
        super(basePath);
      }
    }

    public static class TodoListModel extends org.monjeri.model.Document {
      public static final String NAME = "todolists";

      public final Field<Atom> _id = _idField();
      public final Field<Atom> title = addField("title", Atom.string());
      public final Field<Ref> owner = addField("owner", Ref.create(UserModel.NAME));
      public final Array<TodoModel> todos = arrayField("todos", TodoModel::new).type();
      public final Field created = addField("created", Atom.string());

      public TodoListModel() {
        super(Path.root);
        addIndex(Json.obj(
            ofPath(title, Json.num(1)),
            ofPath(owner, Json.num(1))),
            cfg -> cfg.unique(true));
      }

      public final String name() {
        return NAME;
      }
    }

    public Model(String database) {
      super(database);
    }
  }

  public static class Codec implements BaseDecode, BaseEncode {
    private final Model model;

    Codec(Model model) {
      this.model = model;
    }

    public Decode<User> decodeUser() {
      return doc -> new User(
          string(model.user.username.nameAsPath()).apply(doc),
          stringNullable(model.user.firstname.nameAsPath()).apply(doc),
          stringNullable(model.user.lastname.nameAsPath()).apply(doc),
          stringNullable(model.user.email.nameAsPath()).apply(doc),
          bool(model.user.active.nameAsPath()).apply(doc),
          instantNullable(model.user.created.nameAsPath()).or(Decode.unit(Instant.now())).apply(doc),
          string(model.user.password.nameAsPath()).apply(doc),
          boolNullable(model.user.admin.nameAsPath()).or(Decode.unit(false)).apply(doc));
    }

    public Encode.Object<User> encodeUser() {
      return user -> Json.obj(
          ofName(model.user.username, Json.str(user.getUsername())),
          ofName(model.user.firstname, Json.tryOf(user.getFirstname())),
          ofName(model.user.lastname, Json.tryOf(user.getLastname())),
          ofName(model.user.email, Json.tryOf(user.getEmail())),
          ofName(model.user.active, Json.of(user.isActive())),
          ofName(model.user.created, Json.str(user.getCreated().toString())),
          ofName(model.user.password, Json.str(user.getPassword())),
          ofName(model.user.admin, Json.of(user.isAdmin())));
    }

    public Decode<Todo> decodeTodo() {
      return doc -> new Todo(
          string(model.todoList.todos.element().title.nameAsPath()).apply(doc),
          bool(model.todoList.todos.element().checked.nameAsPath()).apply(doc));
    }

    public Encode.Object<Todo> encodeTodo() {
      return todo -> Json.obj(
          ofName(model.todoList.todos.element().title, Json.str(todo.getName())),
          ofName(model.todoList.todos.element().checked, Json.of(todo.isChecked())));
    }

    public Decode<List<Todo>> decodeSortTodos(Path path) {
      return list(path, Decode.none())
          .map(docs -> docs.map(decodeTodo()));
    }

    public Decode<TodoList<JDBRef>> decodeTodoList() {
      return doc -> new TodoList<>(
          string(model.todoList.title.nameAsPath()).apply(doc),
          dbRef(model.todoList.owner.nameAsPath()).map(Json::dbref).apply(doc),
          decodeSortTodos(model.todoList.todos.nameAsPath()).apply(doc),
          instant(model.todoList.created.nameAsPath()).or(Decode.unit(Instant.now())).apply(doc));
    }

    public Encode.Object<TodoList<JDBRef>> encodeTodoList() {
      return tl -> Json.obj(
          ofName(model.todoList.title, Json.str(tl.getTitle())),
          ofName(model.todoList.owner, dbrefEncode().apply(tl.getOwner().getRef())),
          ofName(model.todoList.todos, listEncode(encodeTodo()).apply(tl.getTodos())),
          ofName(model.todoList.created, instantEncode().apply(tl.getCreated())));
    }
  }

  public static class Tasks implements Monjeri {
    private final String database;
    private final Codec codec;
    private final Model model;

    Tasks(String database, Codec codec, Model model) {
      this.database = database;
      this.codec = codec;
      this.model = model;
    }

    public MonTask1<MongoDatabase> db() {
      return client -> client.getDatabase(database);
    }

    //NotThreadSafe - must use upsert feature
    public MonTask1<ObjectId> createAdminAccount(String username, String password) {
      User admin = new User(username, password, true);
      return db()
          .map(model.user)
          .map(query(Json.obj(model.user.username.name(), Json.str(username))))
          .map(first())
          .flatMap(opt -> opt
              .map(codec.objectId(model.user._id.nameAsPath()))
              .map(MonTask1::unit)
              .orElse(insertUser(admin)));
    }

    public MonTask1<Id<ObjectId, User>> authenticate(String username, String password) {
      return db()
          .map(model.user)
          .map(query(Json.obj(
              ofName(model.user.active, Json.of(true)),
              ofName(model.user.username, Json.str(username)),
              ofName(model.user.password, Json.str(password)))))
          .map(expectOne())
          .map(codec.withObjectId(codec.decodeUser()))
          .onError(e -> MonTask1.fail(new RuntimeException("authentication failed", e)));
    }

    public MonTask1<ObjectId> insertUser(User user) {
      return db()
          .map(model.user)
          .map(insert(codec.encodeUser().apply(user)))
          .map(castToObjectId());
    }

    public MonTask1<Stream<Id<ObjectId, User>>> findUser(JObject query) {
      return db()
          .map(model.user)
          .map(query(query))
          .map(decode(codec.withObjectId(codec.decodeUser())));
    }

    @SuppressWarnings("unchecked")
    public MonTask1<Long> deleteUser(ObjectId userId) {
      MonTask1<DeleteResult> deleteLists = db()
          .map(model.todoList)
          .map(deleteMany(Json.obj(
              model.todoList.owner.name(),
              Json.dbref(model.todoList.owner.type().target(), userId))
          ));

      MonTask1<DeleteResult> deleteUser = db()
          .map(model.user)
          .map(deleteOne(Json.obj(model.user._id.name(), Json.id(userId))));

      return MonTask1.seq(List.of(deleteLists, deleteUser))
          .map(l -> l.foldLeft(0L, (a, b) -> a + b.getDeletedCount()));
    }

    public MonTask1<User> changeUser(Id<ObjectId, User> user) {
      JObject q = JObject.byId(user.id);
      MonTask1<MongoCollection<Document>> userColl = db()
          .map(collection(model.user.name()));
      return userColl
          .map(query(q)).map(expectOne())
          .map(codec.decodeUser())
          .map(u -> u.merge(user.value))
          .flatMap(newUser ->
              userColl.map(findOneAndUpdate(q,
                  Json.obj("$set", codec.encodeUser().apply(newUser)),
                  Decode.none()))
                  .map(x -> newUser));
    }

    public MonTask1<ObjectId> insertTodoList(TodoList<JDBRef> list) {
      return db()
          .map(collection(model.todoList.name()))
          .map(insert(codec.encodeTodoList().apply(list)))
          .map(castToObjectId());
    }

    public MonTask1<Stream<Id<ObjectId, TodoList<JDBRef>>>> findTodoLists(ObjectId owner) {
      return db()
          .map(model.todoList)
          .map(query(Json.obj(
              ofName(model.todoList.owner, Json.dbref(model.user, owner)))
          ))
          .map(decode(codec.withObjectId(codec.decodeTodoList())));
    }

    public MonTask1<TodoList<JDBRef>> getTodoList(ObjectId id) {
      return db()
          .map(model.todoList)
          .map(query(JObject.byId(id)))
          .map(expectOne())
          .map(codec.decodeTodoList());
    }

    public MonTask1<UpdateResult> checkTodoItem(ObjectId listId, Todo item, boolean flag) {
      Array<Model.TodoModel> todos = model.todoList.todos;
      return db()
          .map(model.todoList)
          .map(updateOne(
              JObject.byId(listId)
                  .putPath(todos.element().title, Json.str(item.getName()))
                  .putPath(todos.element().checked, Json.of(item.isChecked())),
              Json.obj("$set", Json.obj(
                  ofPath(todos.each().checked, Json.of(flag))))));

    }

    public MonTask1<UpdateResult> removeTodoItem(ObjectId listId, Todo item) {
      Array<Model.TodoModel> todos = model.todoList.todos;
      return db().map(model.todoList).map(updateOne(
          JObject.byId(listId),
          Json.obj("$pull", Json.obj(
              ofName(todos, Json.obj(
                  ofName(todos.element().title, Json.str(item.getName())),
                  ofName(todos.element().checked, Json.of(item.isChecked()))))))));
    }

    public MonTask1<ObjectId> addTodoItem(ObjectId listId, Todo item, int position) {
      return model.database()
          .map(model.todoList)
          .map(updateOne(
              JObject.byId(listId),
              Json.obj("$push", Json.obj(ofPath(model.todoList.todos, Json.obj(
                  entry("$each", array(codec.encodeTodo().apply(item))),
                  entry("$position", Json.num(position))
              ))))))
          .map(x -> listId);
    }

    public MonTask1<ObjectId> changePriority(ObjectId listId, Todo todo, int target) {
      return model.database()
          .map(model.todoList)
          .map(updateOne(
              JObject.byId(listId),
              Json.obj("$pull", Json.obj(ofPath(model.todoList.todos, Json.obj(
                  ofName(model.todoList.todos.element().title, Json.str(todo.getName())),
                  ofName(model.todoList.todos.element().checked, Json.of(todo.isChecked()))
              ))))
          ))
          .flatMap(x -> addTodoItem(listId, todo, target));
    }
  }
}
