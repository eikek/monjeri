package org.monjeri;

import org.testng.annotations.Test;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class UtilTest {

  @Test
  public void testPresentOnly() throws Exception {
    Stream<Optional<Integer>> s = IntStream.range(0, 100)
        .boxed().parallel()
        .map(n -> n % 2 == 0 ? Optional.of(n) : Optional.empty());

    java.util.List<Object> int1 = Util.<Integer>presentOnly().apply(s).collect(Collectors.toList());
    java.util.List<Object> int2 = IntStream.range(0, 100)
        .filter(n -> n % 2 == 0)
        .boxed().collect(Collectors.toList());
    assertEquals(int1, int2);
  }

  @Test
  public void testListCollector() throws Exception {
    assertEquals(Stream.of(1, 2, 3).collect(Util.collectList()), List.of(1, 2, 3));
    assertSame(Stream.empty().collect(Util.collectList()), List.nil());
  }
}
