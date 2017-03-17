package org.monjeri.migrate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class ChangeRun {
  public final String changeId;
  public final String author;
  public final Instant started;
  public final Duration duration;

  public ChangeRun(String changeId, String author, Instant started, Duration duration) {
    this.changeId = changeId;
    this.author = author;
    this.started = started;
    this.duration = duration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChangeRun changeRun = (ChangeRun) o;
    return Objects.equals(changeId, changeRun.changeId) &&
        Objects.equals(author, changeRun.author) &&
        Objects.equals(started, changeRun.started) &&
        Objects.equals(duration, changeRun.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeId, author, started, duration);
  }

  @Override
  public String toString() {
    return "ChangeRun{" +
        "changeId='" + changeId + '\'' +
        ", author='" + author + '\'' +
        ", started=" + started +
        ", duration=" + duration +
        '}';
  }
}
