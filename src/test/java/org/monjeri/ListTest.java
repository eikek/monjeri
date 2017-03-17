package org.monjeri;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ListTest {
  @Test
  public void testCons() throws Exception {
    assertSame(List.of(), List.nil());
    assertEquals(List.nil().cons(1), List.of(1));
    assertEquals(List.of(1).cons(2), List.of(2, 1));
    assertEquals(List.nil().cons(3).cons(2).cons(1), List.of(1, 2, 3));
    expectThrows(NullPointerException.class, () -> List.of(1, 2, 3).map(i -> null));
    expectThrows(NullPointerException.class, () -> List.of(null));
  }

  @Test
  public void testEmpty() throws Exception {
    assertEquals(List.nil().isEmpty(), true);
    assertEquals(List.nil().nonEmpty(), false);
    assertEquals(List.of(1).isEmpty(), false);
    assertEquals(List.of(1).nonEmpty(), true);
  }

  @Test
  public void testFoldLeft() throws Exception {
    assertEquals(List.of(1, 2, 3).foldLeft("0", (a, b) -> a + b), "0123");
  }

  @Test
  public void testFoldRight() throws Exception {
    assertEquals(List.of(1, 2, 3).foldRight("4", (a, b) -> b + a), "4321");
  }

  @Test
  public void testForeach() throws Exception {
    AtomicInteger n = new AtomicInteger(0);
    List.of(1, 2, 3).foreach(n::addAndGet);
    assertEquals(n.get(), 6);
  }

  @Test
  public void testMap() throws Exception {
    assertEquals(List.of(1, 2, 3).map(n -> n + 1), List.of(2, 3, 4));
  }

  @Test
  public void testAppend() throws Exception {
    assertEquals(List.of(1, 2, 3).append(List.of(4, 5, 6)), List.of(1, 2, 3, 4, 5, 6));
  }

  @Test
  public void testFlatMap() throws Exception {
    assertSame(List.<Integer>nil().flatMap(n -> List.of(n, n)), List.nil());
    assertEquals(List.of(1, 2, 3).flatMap(n -> List.of(n, n)), List.of(1, 1, 2, 2, 3, 3));
  }

  @Test
  public void testFilter() throws Exception {
    assertSame(List.<Integer>nil().filter(n -> n % 2 == 0), List.nil());
    assertEquals(List.of(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0), List.of(2, 4, 6));
  }

  @Test
  public void testReverse() throws Exception {
    assertSame(List.nil().reverse(), List.nil());
    assertEquals(List.of(1, 2, 3).reverse(), List.of(3, 2, 1));
  }

  @Test
  public void testIntersperse() throws Exception {
    assertSame(List.<Integer>nil().intersperse(5), List.nil());
    assertEquals(List.of("a", "b", "c").intersperse("."), List.of("a", ".", "b", ".", "c"));
  }

  @Test
  public void testDistinct() throws Exception {
    assertEquals(List.of(1, 2, 1, 2, 1, 2).distinct(Eq.objectEq()), List.of(1, 2));

    // check that it keeps the leftmost value
    class Val {
      private int n;
      private int m;

      public Val(int n, int m) {
        this.n = n;
        this.m = m;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Val val = (Val) o;
        return n == val.n;
      }

      @Override
      public int hashCode() {
        return Objects.hash(n);
      }
    }

    Val v1 = new Val(1, 1);
    Val v2 = new Val(1, 2);

    List<Val> result = List.of(v1, v2).distinct(Eq.objectEq());
    assertEquals(result.size(), 1);
    assertSame(result.head(), v1);
  }

  @Test
  public void testToJava() throws Exception {
    assertEquals(List.of(1, 2, 3).toJava(), java.util.Arrays.asList(1, 2, 3));
  }

  @Test
  public void testFromIterator() throws Exception {
    assertEquals(List.fromIterable(java.util.Arrays.asList(1, 2, 3)), List.of(1, 2, 3));
    assertSame(List.fromIterable(Collections.emptyList()), List.nil());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testZip() throws Exception {
    assertEquals(List.of(1, 2, 3).zip(List.of(3, 2, 1)),
        List.of(List.P2.of(1, 3), List.P2.of(2, 2), List.P2.of(3, 1)));

    assertEquals(List.of(1, 2).zip(List.of(7, 6, 5, 4)),
        List.of(List.P2.of(1, 7), List.P2.of(2, 6)));

    assertEquals(List.of(1, 2, 3, 4, 5).zip(List.of(7, 6)),
        List.of(List.P2.of(1, 7), List.P2.of(2, 6)));

    assertSame(List.nil().zip(List.of(1, 2, 3)), List.nil());
    assertSame(List.of(1, 2, 3).zip(List.nil()), List.nil());
  }

  @Test
  public void testSort() throws Exception {
    assertEquals(List.of(4, 3, 2, 5, 1).sortWith(i -> i), List.of(1, 2, 3, 4, 5));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testZipWithIndex() throws Exception {
    assertEquals(List.of("a", "b", "c", "d").zipWithIndex(),
        List.of(List.P2.of("a", 0), List.P2.of("b", 1), List.P2.of("c", 2), List.P2.of("d", 3)));
  }

  @Test
  public void testDropWhile() throws Exception {
    assertEquals(List.of(1, 2, 3, 4, 5).dropWhile(n -> n < 4), List.of(4, 5));
  }

  @Test
  public void testTakeWhile() throws Exception {
    assertEquals(List.of(1, 2, 3, 4, 5).takeWhile(n -> n <= 3), List.of(1, 2, 3));
  }

  @Test
  public void testDrop() throws Exception {
    assertEquals(List.of(1, 2, 3, 4).drop(2), List.of(3, 4));
    assertSame(List.nil().drop(4), List.nil());
  }

  @Test
  public void testTake() throws Exception {
    assertEquals(List.of(1, 2, 3, 4).take(2), List.of(1, 2));
    assertSame(List.nil().take(4), List.nil());
  }

  @Test
  public void testFromArray() throws Exception {
    assertEquals(List.fromArray(new Integer[]{1, 2, 3}), List.of(1, 2, 3));
  }

  @Test
  public void testElementAt() throws Exception {
    assertEquals(1, (int) List.of(1, 2, 3, 4).elementAt(0));
    assertEquals(3, (int) List.of(1, 2, 3, 4).elementAt(2));
    assertEquals(4, (int) List.of(1, 2, 3, 4).elementAt(3));
  }

  @Test
  public void testFill() throws Exception {
    assertEquals(List.fill(3, i -> i), List.of(0, 1, 2));
  }

  @Test
  public void testExists() throws Exception {
    assertTrue(List.of(1, 2, 3).exists(i -> i == 2));
    assertFalse(List.of(1, 2, 3).exists(i -> i > 4));
    assertFalse(List.nil().exists(i -> true));

    // test short circuit
    AtomicInteger count = new AtomicInteger(0);
    boolean result = List.of(1, 2, 3, 4, 5, 6, 7).exists(i -> {
      count.incrementAndGet();
      return i > 3;
    });
    assertTrue(result);
    assertEquals(4, count.get());
  }

  @Test
  public void testForall() throws Exception {
    assertTrue(List.of(1, 2, 3).forall(i -> i < 10));
    assertFalse(List.of(1, 2, 3).forall(i -> i == 2));
    assertTrue(List.nil().forall(i -> false));

    // test short circuit
    AtomicInteger count = new AtomicInteger(0);
    boolean result = List.of(1, 2, 3, 4, 5, 6, 7).forall(i -> {
      count.incrementAndGet();
      return i < 4;
    });
    assertFalse(result);
    assertEquals(4, count.get());
  }

  @Test
  public void testContains() throws Exception {
    assertTrue(List.of(1, 2, 3).contains(2, Eq.objectEq()));
    assertFalse(List.of(1, 2, 3).contains(15, Eq.objectEq()));
  }

  @Test
  public void testRemove() throws Exception {
    assertEquals(List.of(1, 2, 3, 2, 4).remove(2), List.of(1, 3, 4));
    assertEquals(List.of(1, 2).remove(4), List.of(1, 2));
  }

  @Test
  public void testUnfold() throws Exception {
    List<Integer> ints = List.unfold(0, n -> n >= 50 ? Optional.empty() : Optional.of(n + 1));
    assertEquals(ints.size(), 51, ints.toString());
    assertEquals(ints.reverse(), List.fill(51, i -> i));
    assertEquals(ints.reverse(), List.range(0, 51), ints.reverse().toString() + " vs. " + List.range(0, 51));
  }

  @Test(enabled = false)
  public void testBuildLargeList() throws Exception {
    for (int n=0; n<100; n++) {
      System.out.println("---- run " + n + " ----");
      List<String> list = List.nil();
      int max = 100_000;
      Stopwatch w = Stopwatch.start();
      for (int i = 0; i < max; i++) {
        list = list.cons(i + "");
      }
      list = list.reverse();
      System.out.println("Constructed (" + max + ") in: " + w.stop());
      assertNotNull(list);

      w = w.reset();
      String s = list.toString();
      System.out.println("ToString (" + max + " elements) in " + w.stop());
      assertNotNull(list);

      w = w.reset();
      list = list.drop(5000).intersperse("-");
      System.out.println("Intersperse (" + (max - 5000) + ") in " + w.stop());
      assertNotNull(list);

      max = list.size();
      w = w.reset();
      list = list.flatMap(x -> List.of(x, x, x));
      System.out.println("FlatMap (" + max + " elements) in " + w.stop());
      assertNotNull(list);

      list.exists(x -> x.equalsIgnoreCase("189999"));
    }
  }
}
