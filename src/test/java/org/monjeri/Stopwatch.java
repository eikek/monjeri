package org.monjeri;

import java.time.Duration;

public class Stopwatch {
  private long start;
  private long end;

  private Stopwatch(long start, long end) {
    this.start = start;
    this.end = end;
  }

  public static Stopwatch start() {
    return new Stopwatch(System.nanoTime(), -1);
  }

  public Stopwatch stop() {
    return new Stopwatch(start, System.nanoTime());
  }

  public boolean isStopped() {
    return end > start;
  }

  public Stopwatch reset() {
    return start();
  }

  public Duration getDuration() {
    if (isStopped()) {
      return Duration.ofNanos(end - start);
    } else {
      return Duration.ZERO;
    }
  }

  @Override
  public String toString() {
    if (isStopped()) {
      return String.format("%d ms", getDuration().toMillis());
    } else {
      return String.format("Running from %d", start);
    }
  }

  public void printDone() {
    if (!isStopped()) {
      System.out.println(stop());
    } else {
      System.out.println(this);
    }
  }
}
