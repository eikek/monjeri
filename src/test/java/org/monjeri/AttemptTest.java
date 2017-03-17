package org.monjeri;

import org.testng.annotations.Test;

import static org.monjeri.Attempt.success;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AttemptTest {
  private Exception exception = new Exception("error");
  private Attempt<Integer> err = Attempt.fail(exception);
  private Attempt<Integer> succ = success(45);


  @Test
  public void testisSuccessOrFailure() throws Exception {
    assertEquals(err.isFailure(), true);
    assertEquals(err.isSuccess(), false);
    assertEquals(succ.isFailure(), false);
    assertEquals(succ.isSuccess(), true);

    Attempt<Void> a = Attempt.success(null);
    assertTrue(a.isSuccess());
    assertFalse(a.isFailure());
  }

  @Test
  public void testGet() throws Exception {
    try {
      err.get();
      fail("should not be successful");
    } catch (Exception e) {
      assertEquals(e, exception);
    }
  }

  @Test
  public void testEqual() throws Exception {
    assertEquals(succ, success(45));
    assertEquals(err, Attempt.fail(exception));
  }

  @Test
  public void testMapOnFailure() throws Exception {
    assertSame(err.map(n -> n + 1), err);
    assertSame(err.map(n -> {
      throw new RuntimeException();
    }), err);
    assertSame(err.map(a -> a), err);
  }

  @Test
  public void testMapOnSuccess() throws Exception {
    assertEquals(succ.map(n -> n + 1), success(46));
    assertEquals(succ.map(a -> a), succ);
  }

  @Test
  public void testMapWithError() throws Exception {
    Attempt<Integer> fail = succ.map(n -> {
      throw new RuntimeException("boom");
    });
    assertTrue(fail.isFailure());
  }

  @Test
  public void testFlatMapOnFailure() throws Exception {
    assertSame(err.flatMap(n -> success(100)), err);
    assertSame(err.flatMap(n -> {
      throw new RuntimeException("");
    }), err);
  }

  @Test
  public void testFlatMapOnEvalFailure() throws Exception {
    Attempt<Object> fail = succ.flatMap(n -> {
      throw new RuntimeException();
    });
    assertTrue(fail.isFailure());
    assertEquals(succ.flatMap(n -> succ), succ);
  }

  @Test
  public void testFlatMapOnSuccess() throws Exception {
    assertEquals(succ.flatMap(n -> err), err);
  }

  @Test
  public void testOnError() throws Exception {
    assertSame(succ.onError(x -> success(15)), succ);
    assertEquals(err.onError(x -> success(15)), success(15));
    Attempt<Integer> fail = err.onError(x -> {
      throw new RuntimeException();
    });
    assertTrue(fail.isFailure());

    assertSame(succ.or(success(15)), succ);
    assertEquals(err.or(succ), succ);
  }

  @Test
  public void testFold() throws Exception {
    assertEquals((int) succ.<Integer>fold(n -> n, x -> 0), 45);
    assertEquals((int) err.<Integer>fold(n -> n, x -> 0), 0);

  }

  @SuppressWarnings({"AssertEqualsBetweenInconvertibleTypesTestNG", "unchecked"})
  @Test
  public void testSeqStopOnFirstError() throws Exception {
    assertEquals(
        Attempt.seq(List.of(succ, succ, err, succ, succ)),
        err);

    assertEquals(
        Attempt.seq(List.of(succ, success(46), success(5))).get(),
        List.of(45, 46, 5));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSeq() throws Exception {
    assertTrue(Attempt.seq(List.of(err)).isFailure());
    assertTrue(Attempt.seq(List.of(succ, err)).isFailure());
    assertTrue(Attempt.seq(List.of(err, succ)).isFailure());
    assertFalse(Attempt.seq(List.of(succ)).isFailure());

    Attempt<Void> z = Attempt.success(null);
    assertTrue(Attempt.seq(List.of(z, z, z)).isSuccess());
    assertTrue(Attempt.seq(List.of(z, z, z)).asSuccess().get().isEmpty());
  }
}
