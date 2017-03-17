package org.monjeri.example.todo;

public class Main {

  public static void main(String[] args) {
    ConsoleApp app = new ConsoleApp(TodoDB.getDefault());
    app.start(args);
    System.out.println("Good bye.");
  }

}
