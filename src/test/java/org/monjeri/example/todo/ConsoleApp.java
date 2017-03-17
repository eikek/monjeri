package org.monjeri.example.todo;

import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.monjeri.Id;
import org.monjeri.Json;
import org.monjeri.List;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.Util;
import org.monjeri.migrate.Result;

import java.io.Console;
import java.time.Instant;
import java.util.function.Consumer;

public class ConsoleApp {
  private final Console console;
  private final TodoDB db;

  public ConsoleApp(Console console, TodoDB todoDB) {
    this.console = console;
    this.db = todoDB;
  }

  public ConsoleApp(TodoDB db) {
    this(System.console(), db);
  }

  public void start(String[] args) {
    // find mongo host and port from args
    MongoClient client = createMongoClient(List.fromArray(args));

    // initialise database
    List<Result> run = db.initialize().run(client);
    System.out.println(run);

    // start application
    application().run(null, client);
  }

  public Screen application() {
    Screen main = Screen.titled("Main Menu")
        .withExit()
        .submenu("Todo Lists", Screen.titled("Todo Lists")
            .withExit().withBack()
            .action("Create new list", this::createNewListAction)
            .action("Choose List", this::chooseListAction))
        .adminMenu("User Menu", Screen.titled("Manage User")
            .withExit().withBack()
            .action("Delete user", this::deleteUserAction)
            .action("List user", this::listUserAction)
            .action("New user", this::createNewUserAction)
            .action("Change user", this::changeUserAction));

    return Screen.titled("Welcome…!")
        .withExit()
        .action("login", runMainAuthenticated(main));
  }

  private MonTask<Id<ObjectId, User>, Screen> runMainAuthenticated(Screen main) {
    return (_null, client) -> {
      String name = promptNonEmpty("Login: ");
      String pass = promptPass("Password: ");
      Id<ObjectId, User> user = db.tasks.authenticate(name, pass).run(client);
      main.run(user, client);
      return Screen.current();
    };
  }

  private Screen changeUserAction(Id<ObjectId, User> cur, MongoClient client) {
    User merged = chooseUser()
        .flatMap(user -> userForm(true).map(newUser -> user.update(x -> newUser)))
        .flatMap(db.tasks::changeUser)
        .run(client);
    console.printf("New user: %s", formatUserLine(merged));
    return Screen.current();
  }

  private Screen createNewUserAction(Id<ObjectId, User> cur, MongoClient cl) {
    ObjectId result = userForm(false).flatMap(db.tasks::insertUser).run(cl);
    console.printf("\nInserted with id %s\n", result);
    return Screen.current();
  }

  private Screen listUserAction(Id<ObjectId, User> cur, MongoClient cl) {
    console.printf("\nAll user:\n");
    db.tasks.findUser(Json.JObject.empty())
        .map(s -> s.map(id -> id.value))
        .run(cl)
        .forEach(printUser());
    prompt("Press Enter to continue.");
    return Screen.current();
  }

  private Screen deleteUserAction(Id<ObjectId, User> cur, MongoClient client) {
    Long deletes = chooseUser()
        .map(Id::id)
        .flatMap(db.tasks::deleteUser)
        .run(client);
    console.printf("\nDeleted %d entries (todo lists and user)", deletes);
    return Screen.current();
  }

  Screen chooseListAction(Id<ObjectId, User> user, MongoClient client) {
    Id<ObjectId, TodoList<Json.JDBRef>> chosenList = chooseTodoList(user.id).run(client);

    MonTask1<String> formatItems = db.tasks.getTodoList(chosenList.id)
        .map(TodoList::getTodos)
        .map(todos -> todos.zipWithIndex()
            .map(p2 -> String.format(" %d) %s", p2._2, formatTodo(p2._1)))
            .intersperse("\n")
            .foldLeft("", Util.stringAppend()))
        .map(s -> s.isEmpty() ? "<empty list>" : s);

    return Screen.titled(chosenList.value.getTitle())
        .withExit().withBack()
        .withDescription(formatItems)
        .action("Delete item", removeListItem(chosenList))
        .action("Change priority", changePriority(chosenList))
        .action("Mark item done", checkListItem(chosenList, true))
        .action("Mark item todo", checkListItem(chosenList, false))
        .action("Add item", addListItem(chosenList));
  }

  MonTask<Id<ObjectId, User>, Screen> changePriority(Id<ObjectId, TodoList<Json.JDBRef>> list) {
    return MonTask.lift(db.tasks.getTodoList(list.id)
        .map(TodoList::getTodos)
        .flatMap(todos -> {
          int n = promptN("Item: ", 0, todos.size() - 1);
          int prio = promptN("Prio: ", 0, todos.size() - 1);
          return db.tasks.changePriority(list.id, todos.elementAt(n), prio)
              .map(ign -> Screen.current());
        }));
  }

  MonTask<Id<ObjectId, User>, Screen> checkListItem(Id<ObjectId, TodoList<Json.JDBRef>> list, boolean flag) {
    return MonTask.lift(chooseTodo(list.id, "Item: ")
        .flatMap(todo -> db.tasks.checkTodoItem(list.id, todo, flag))
        .map(ign -> {
          console.printf("Marked item.");
          return Screen.current();
        }));
  }

  MonTask<Id<ObjectId, User>, Screen> removeListItem(Id<ObjectId, TodoList<Json.JDBRef>> list) {
     return MonTask.lift(chooseTodo(list.id, "Item: ")
        .flatMap(todo -> db.tasks.removeTodoItem(list.id, todo))
        .map(ign -> {
          console.printf("Item has been removed.");
          return Screen.current();
        }));
  }

  private MonTask1<Todo> chooseTodo(ObjectId listId, String prompt) {
    return db.tasks.getTodoList(listId)
        .map(TodoList::getTodos)
        .map(todos -> todos.elementAt(promptN(prompt, 0, todos.size() - 1)));
  }

  MonTask<Id<ObjectId, User>, Screen> addListItem(Id<ObjectId, TodoList<Json.JDBRef>> list) {
    return (cur, client) -> {
      Todo todo = new Todo(promptNonEmpty("Name: "), yesOrNo("Done"));
      db.tasks.addTodoItem(list.id, todo, 0).run(client);
      console.printf("New item added.");
      return Screen.current();
    };
  }

  Screen createNewListAction(Id<ObjectId, User> cur, MongoClient client) {
    String title = promptNonEmpty("List title: ");
    ObjectId listId = db.tasks.insertTodoList(
        TodoList.createEmpty(title, cur.asDBRef(db.model.user)))
        .run(client);
    console.printf("\nInserted new list with id " + listId);
    return Screen.current();
  }


  MonTask1<Id<ObjectId, User>> chooseUser() {
    return client -> {
      List<Id<ObjectId, User>> users =
          db.tasks.findUser(Json.JObject.empty())
              .map(s -> s.collect(List.collector()))
              .run(client);
      users.zipWithIndex().foreach(p2 ->
          console.printf("\n• [%d] %s ",
              p2._2, formatUserLine(p2._1.value)));
      int n = promptN("\nWhich: ", 0, users.size() - 1);
      return users.elementAt(n);
    };
  }

  MonTask1<Id<ObjectId, TodoList<Json.JDBRef>>> chooseTodoList(ObjectId owner) {
    return client -> {
      List<Id<ObjectId, TodoList<Json.JDBRef>>> lists =
          db.tasks.findTodoLists(owner)
              .map(s -> s.collect(List.collector()))
              .run(client);
      lists.zipWithIndex().foreach(p2 ->
      console.printf("\n• [%d] %s",
          p2._2, formatTodoList(p2._1.value)));
      int n = promptN("\nWhich: ", 0, lists.size() - 1);
      return lists.elementAt(n);
    };
  }


  String formatTodo(Todo todo) {
    return String.format("[%s] %s", todo.isChecked() ? "X" : " ", todo.getName());
  }

  String formatTodoList(TodoList<?> list) {
    return String.format("%s (%d items) from %s",
        list.getTitle(), list.getTodos().size(), list.getCreated());
  }

  String formatUserLine(User user) {
    return String.format("%s (%s %s), %s %s%s from %s",
        user.getUsername(), user.getFirstname(), user.getLastname(), user.getEmail(),
        user.isAdmin() ? "[admin]" : "", user.isActive() ? "[active]" : "", user.getCreated());
  }

  Consumer<User> printUser() {
    return user -> console.printf("• %s\n", formatUserLine(user));
  }

  MonTask1<User> userForm(boolean optional) {
    return client -> new User(
        optional ? prompt("Username: ") : promptNonEmpty("Username: "),
        prompt("Firstname: "),
        prompt("Lastname: "),
        optional ? prompt("Email: ") : promptNonEmpty("Email: "),
        yesOrNo("Active"),
        Instant.now(),
        optional ? prompt("Password: ") : promptNonEmpty("Password: "),
        yesOrNo("Admin"));
  }

  private String prompt(String prompt) {
    console.printf(prompt);
    String in = console.readLine();
    return in == null || in.trim().isEmpty()
        ? null : in.trim();
  }

  private String promptPass(String prompt) {
    char[] in = console.readPassword(prompt);
    return in.length == 0 ? "" : new String(in);
  }

  private boolean yesOrNo(String msg) {
    String ans = promptNonEmpty(msg + "(y/n)? ");
    return "y".equalsIgnoreCase(ans);
  }

  private String promptNonEmpty(String msg) {
    String in = prompt(msg);
    if (in == null) {
      throw new IllegalArgumentException("A value is required for '" + msg + "'");
    }
    return in.trim();
  }

  private int promptN(String msg, int min, int max) {
    String in = promptNonEmpty(msg);
    int n = Integer.parseInt(in);
    if (n < min || n > max) {
      throw new IllegalArgumentException("Number must be between " + min + " and " + max);
    }
    return n;
  }

  private MongoClient createMongoClient(List<String> argList) {
    String host = argList
        .dropWhile(a -> !a.equals("--host"))
        .tail()
        .headOption()
        .orElse("localhost");

    int port = argList
        .dropWhile(a -> !a.equals("--port"))
        .tail()
        .headOption()
        .map(Integer::parseInt)
        .orElse(27017);
    System.err.println(String.format("Connecting to mongodb at %s:%d", host, port));
    return new MongoClient(host, port);
  }

}
