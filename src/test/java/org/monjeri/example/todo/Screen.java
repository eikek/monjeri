package org.monjeri.example.todo;

import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.monjeri.Id;
import org.monjeri.List;
import org.monjeri.MonTask;
import org.monjeri.MonTask1;
import org.monjeri.Util;

import java.io.Console;

public class Screen {
  private static final Console console = System.console();
  private final String title;
  private final MonTask1<String> description;
  private final List<Action> actions;
  private static final Screen CURRENT = new Screen("current", MonTask1.unit(""), List.nil());
  private static final Screen BACK = new Screen("back", MonTask1.unit(""), List.nil());

  private Screen(String title, MonTask1<String> description, List<Action> actions) {
    this.title = title;
    this.description = description;
    this.actions = actions;
  }

  public static Screen current() {
    return CURRENT;
  }

  public static Screen back() {
    return BACK;
  }

  public static Screen titled(String title) {
    return new Screen(title, MonTask1.unit(""), List.nil());
  }

  private Screen action(Action action) {
    return new Screen(title, description, this.actions.cons(action));
  }

  public Screen withExit() {
    return action("Exit", (a, b) -> null);
  }

  public Screen withBack() {
    return action("Back", (a, b) -> back());
  }

  public Screen withDescription(MonTask1<String> text) {
    return new Screen(title, text, actions);
  }

  public Screen action(String title, MonTask<Id<ObjectId, User>, Screen> task) {
    return action(new Action(title, task));
  }

  public Screen submenu(String title, Screen screen) {
    return action(title, (a, b) -> screen);
  }

  public Screen adminMenu(String title, Screen screen) {
    return action(title + "*", (cur, client) -> {
      if (!cur.value.isAdmin()) {
        console.printf("This menu is only for admins.");
        return Screen.current();
      }
      return screen;
    });
  }

  public String render(Id<ObjectId, User> cur, MongoClient client) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("\n ").append(title).append("\n ");
    List.fill(title.length(), "-").foreach(buffer::append);
    buffer.append("\n\n");
    String text = description.run(client);
    if (!text.isEmpty()) {
      buffer.append(text).append("\n\n");
      List.fill(40, "-").foreach(buffer::append);
      buffer.append("\n");
    }
    actions.zipWithIndex().foreach(p2 ->
        buffer.append(" [").append(p2._2).append("] ")
            .append(p2._1.title()).append("\n"));
    buffer.append("\n Choice: ");
    return buffer.toString();
  }

  public Screen next(Id<ObjectId, User> current, MongoClient client) {
    String in = null;
    while (!"exit".equalsIgnoreCase(in)) {
      System.out.print(render(current, client));
      in = console.readLine();
      try {
        int n = Integer.parseInt(in);
        if (n < 0 || n >= actions.size()) {
          throw new IllegalArgumentException("Wrong input: " + in);
        }
        return actions.zipWithIndex()
            .find(p2 -> p2._2 == n)
            .map(List.P2::__1)
            .orElseThrow(() -> new Exception("Action not found for " + n))
            .run(current, client);
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
      }
    }
    return Screen.current();
  }

  public void run(Id<ObjectId, User> current, MongoClient client) {
    Screen screen = this;
    List<Screen> stack = List.of(this);
    while (screen != null) {
      console.printf("\n–––––––––––––––––––––––––––––– your todos ––––––––––––––––––––––––––––––\n");
      console.printf(" (%s) ", current != null ? current.value.getUsername() : "not logged in");
      if (!stack.tail().isEmpty()) {
        console.printf(stack.reverse()
            .map(s -> s.title)
            .intersperse(" -> ")
            .foldLeft("", Util.stringAppend()));
      }
      console.printf("\n");
      screen = screen.next(current, client);
      if (screen == Screen.current()) {
        screen = stack.head();
      } else if (screen == Screen.back()) {
        stack = stack.tail();
        screen = stack.head();
      } else if (screen != null) {
        stack = stack.cons(screen);
      }
    }
  }

  private final static class Action implements MonTask<Id<ObjectId, User>, Screen> {
    private final String title;
    private final MonTask<Id<ObjectId, User>, Screen> task;
    protected Action(String title, MonTask<Id<ObjectId, User>, Screen> task) {
      this.title = title;
      this.task = task;
    }

    public String title() {
      return title;
    }

    @Override
    public Screen run(Id<ObjectId, User> in, MongoClient client) {
      return task.run(in, client);
    }
  }
}
