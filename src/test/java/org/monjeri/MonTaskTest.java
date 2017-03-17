package org.monjeri;

import org.testng.annotations.Test;

import javax.management.RuntimeMBeanException;
import java.time.Duration;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MonTaskTest {

  @Test
  public void testCombinatorStack() throws Exception {
    MonTask1<Integer> task = MonTask1.unit(5);
    for (int i=0; i<100000; i++) {
      task = task.flatMap(n -> MonTask1.unit(n + 1));
    }
    int r = task.run(null);
    assertEquals(r, 100005);


    MonTask<String, Integer> task2 = MonTask.unit(5);
    for (int i=0; i<100000; i++) {
      task2 = task2.flatMap(n -> MonTask.unit(n + 1));
    }
    r = task2.run("", null);
    assertEquals(r, 100005);
  }

  @Test
  public void testFlatMapLeftIdentity() throws Exception {
    MonTask1<Integer> task = MonTask1.unit(5);
    Function<Integer, MonTask1<Integer>> f = n -> MonTask1.unit(n + 10);
    List.range(-20, 20).foreach(ign ->
      assertEquals(task.flatMap(f).run(null), f.apply(5).run(null))
    );

    MonTask<String, Integer> task2 = MonTask.unit(5);
    Function<Integer, MonTask<String, String>> ff = n -> MonTask.unit((n + 10) + "");
    List.range(-20, 20).foreach(ign ->
      assertEquals(task2.flatMap(ff).run("", null), ff.apply(5).run("", null))
    );
  }

  @Test
  public void testFlatMapRightIdentity() throws Exception {
    MonTask1<Integer> task = MonTask1.unit(5);
    List.range(-20, 20).foreach(ign ->
      assertEquals(task.flatMap(MonTask1::unit).run(null), task.run(null))
    );

    MonTask<String, Integer> task2 = MonTask.unit(5);
    List.range(-20, 20).foreach(ign ->
      assertEquals(task2.flatMap(MonTask::<String, Integer>unit).run("", null), task2.run("", null)));
  }

  @Test
  public void testFlatMapAssociativity() throws Exception {
    MonTask1<Integer> task = MonTask1.unit(5);
    List.range(-20, 20).foreach(ign ->
        assertEquals(
            task.flatMap(n -> MonTask1.unit(n + 2).flatMap(x -> MonTask1.unit(x * 2))).run(null),
            task.flatMap(n -> MonTask1.unit(n + 2)).flatMap(x -> MonTask1.unit(x * 2)).run(null)
        ));


    MonTask<String, Integer> task2 = MonTask.unit(5);
    List.range(-20, 20).foreach(ign ->
        assertEquals(
            task2.flatMap(n -> MonTask.<String, Integer>unit(n + 2).flatMap(x -> MonTask.unit(x * 2))).run("", null),
            task2.flatMap(n -> MonTask.unit(n + 2)).flatMap(x -> MonTask.unit(x * 2)).run("", null)
        ));
  }

  @Test
  public void testTimed() throws Exception {
    MonTask1<Integer> task = client -> {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return 5;
    };

    List.P2<Integer, Duration> result = task.transform(Monjeri.getInstance().timed1()).run(null);
    assertEquals((int) result._1, 5);
    assertTrue(result._2.toMillis() > 299);
  }
}
