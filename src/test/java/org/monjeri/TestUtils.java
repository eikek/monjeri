package org.monjeri;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;

public class TestUtils {
  static final String dbPrefix = "monjeri-test-";


  public static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String exceptionToString(Throwable error) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    error.printStackTrace(new PrintStream(out));
    return new String(out.toByteArray());
  }

  public static String randomString(CharSequence chars, int len) {
    SecureRandom random = new SecureRandom();
    char[] result = new char[len];
    for (int i=0; i<len; i++) {
      int index = random.nextInt(chars.length());
      result[i] = chars.charAt(index);
    }
    return new String(result);
  }

  public static String randomAlphaNum(int len) {
    String alpha = "abcdefghijklmnopqrstuvwxyz";
    String num = "0123456789";
    return randomString(alpha + alpha.toUpperCase() + num, len);
  }

  public static String randomDbName() {
    return dbPrefix + randomAlphaNum(8);
  }

}
