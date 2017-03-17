package org.monjeri;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PathTest {

  @Test
  public void testParse() throws Exception {
    Path p = Path.parse("a.b.c");
    assertEquals(3, p.length());
    assertEquals("a.b.c", p.render());
  }

  @Test
  public void testIsEmpty() throws Exception {
    assertTrue(Path.root.isEmpty());
    assertFalse(Path.single("a").isEmpty());
  }

  @Test
  public void testConsSnoc() throws Exception {
    assertEquals(Path.parse("b.a"), Path.root.cons("a").cons("b"));
    assertEquals(Path.parse("a.b"), Path.root.snoc("a").snoc("b"));
  }

  @Test
  public void testFirstAndLast() throws Exception {
    assertEquals("a", Path.parse("a.b.c").first());
    assertEquals("c", Path.parse("a.b.c").last());
  }

  @Test
  public void testDropFirst() throws Exception {
    assertEquals(Path.parse("b.c"), Path.p("a.b.c").dropFirst());
    assertEquals(Path.single("c"), Path.p("a.b.c").dropFirst(2));
  }

  @Test
  public void testDropLast() throws Exception {
    assertEquals(Path.parse("a.b.c").dropLast(), Path.parse("a.b"));
    assertEquals(Path.parse("a.b.c").dropLast(2), Path.parse("a"));
  }

  @Test
  public void testStartsWith() throws Exception {
    assertTrue(Path.parse("a.b.c").startsWith(Path.single("a")));
    assertTrue(Path.parse("a.b.c").startsWith(Path.parse("a.b")));
    assertTrue(Path.parse("a.b.c").startsWith(Path.parse("a.b.c")));

    assertFalse(Path.parse("a.b.c").startsWith(Path.single("b")));
    assertFalse(Path.parse("a.b.c").startsWith(Path.single("a.b.c.d")));
  }

  @Test
  public void testEndsWith() throws Exception {
    assertTrue(Path.parse("a.b.c").endsWith(Path.single("c")));
    assertTrue(Path.parse("a.b.c").endsWith(Path.parse("b.c")));
    assertTrue(Path.parse("a.b.c").endsWith(Path.parse("a.b.c")));

    assertFalse(Path.parse("a.b.c").endsWith(Path.single("a")));
    assertFalse(Path.parse("a.b.c").endsWith(Path.parse("e.a.b.c")));
  }
}
