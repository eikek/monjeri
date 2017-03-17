/**
 * <p>This contains a sample multi-user todo application.</p>
 *
 * <p>The domain model are the classes {@code User} and {@code TodoList} (and
 * also {@code Todo} which is part of {@code TodoList}). The classes
 * {@code Main}, {@code Screen} and {@code ConsoleApp} are the user interface (a menu
 * driven console ui), not interesting in this example. Then left is {@code TodoDB}
 * which contains everything to connect the todo-app to mongodb. This contains
 * the interesting code for this example.</p>
 *
 * <p>The user is able to log in and create todo lists. The todo lists can be
 * manipulated by adding, removing and checking items. Admin users can manage
 * the user collection.</p>
 */
package org.monjeri.example.todo;