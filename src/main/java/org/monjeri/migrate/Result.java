package org.monjeri.migrate;

import java.util.Objects;

public final class Result {
  private final ChangeRun changeRun;
  private final Outcome outcome;

  public Result(ChangeRun changeRun, Outcome outcome) {
    this.changeRun = changeRun;
    this.outcome = outcome;
  }

  public static Result skipped(ChangeRun run) {
    return new Result(run, Outcome.SKIPPED);
  }

  public static Result executed(ChangeRun run) {
    return new Result(run, Outcome.EXECUTED);
  }

  public enum Outcome {
    SKIPPED, EXECUTED
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Result result = (Result) o;
    return Objects.equals(changeRun, result.changeRun) &&
        outcome == result.outcome;
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeRun, outcome);
  }

  @Override
  public String toString() {
    return "Result{" +
        "changeRun=" + changeRun +
        ", outcome=" + outcome +
        '}';
  }
}
