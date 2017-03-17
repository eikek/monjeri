package org.monjeri;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Attempt to run a mongodb operation
 */
public abstract class Attempt<A> {
  private Attempt() {}

  /**
   * Returns the success value if this is success. Note that it
   * may also return {@code Optional#empty()} in case of success,
   * when the success value is {@code null}.
   */
  public abstract Optional<A> asSuccess();

  public abstract Optional<Exception> asFailure();

  public boolean isSuccess() {
    return !isFailure();
  }

  public boolean isFailure() {
    return asFailure().isPresent();
  }

  public abstract A get() throws Exception;

  public A unsafeGet() {
    try {
      return get();
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  public abstract void foreach(Consumer<A> consumer);

  public abstract <B> Attempt<B> map(Function<? super A, ? extends B> f);

  public abstract <B> Attempt<B> flatMap(Function<? super A, Attempt<B>> f);

  public abstract Attempt<A> onError(Function<? super Exception, Attempt<A>> f);

  public Attempt<A> or(Attempt<A> other) {
    return onError(x -> other);
  }

  public A getOrElse(A defaultValue) {
    return or(success(defaultValue)).unsafeGet();
  }

  public abstract <B> B fold(
      Function<? super A, ? extends B> f,
      Function<? super Exception, ? extends B> g);

  public <B, C> Attempt<C> combine(Attempt<B> next, BiFunction<A,B,C> f) {
    return flatMap(a -> next.map(b -> f.apply(a, b)));
  }

  /**
   * Fold the given list of {@link Attempt}s to one attempt that holds the
   * collection of all the results. Note that {@code null} values are discarded
   * while accumulating.
   */
  public static <A> Attempt<List<A>> seq(List<Attempt<A>> attempts) {
    Attempt<List<A>> z = Attempt.success(List.nil());
    return attempts.foldLeft(z, (listAttempt, a) -> listAttempt.combine(a, List::nullSafeCons)).map(List::reverse);
  }

  public static <A> Attempt<A> success(A value) {
    return new Success<>(value);
  }

  public static <A> Attempt<A> fail(Exception e) {
    return new Failure<>(e);
  }

  public static <A> Attempt<A> eval(Callable<A> code) {
    try {
      return success(code.call());
    } catch (Exception e) {
      return fail(e);
    }
  }

  public static final class Success<A> extends Attempt<A> {
    private final A value;

    private Success(A value) {
      this.value = value;
    }

    public A getValue() {
      return value;
    }

    @Override
    public Optional<A> asSuccess() {
      // value maybe still null, in case of Attempt<Void>
      return Optional.ofNullable(value);
    }

    @Override
    public Optional<Exception> asFailure() {
      return Optional.empty();
    }

    @Override
    public A get() throws Exception {
      return value;
    }

    @Override
    public void foreach(Consumer<A> consumer) {
      consumer.accept(value);
    }

    @Override
    public <B> Attempt<B> map(Function<? super A, ? extends B> f) {
      return eval(() -> f.apply(value));
    }

    @Override
    public <B> Attempt<B> flatMap(Function<? super A, Attempt<B>> f) {
      return eval(() -> f.apply(value).get());
    }

    @Override
    public <B> B fold(Function<? super A, ? extends B> f, Function<? super Exception, ? extends B> g) {
      return f.apply(value);
    }

    @Override
    public Attempt<A> onError(Function<? super Exception, Attempt<A>> f) {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Success<?> success = (Success<?>) o;
      return Objects.equals(value, success.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Success{" + value + '}';
    }
  }

  public static final class Failure<A> extends Attempt<A> {
    private final Exception error;

    private Failure(Exception error) {
      this.error = error;
    }

    public Exception getError() {
      return error;
    }

    @Override
    public Optional<A> asSuccess() {
      return Optional.empty();
    }

    @Override
    public Optional<Exception> asFailure() {
      return Optional.of(error);
    }

    @Override
    public A get() throws Exception {
      throw error;
    }

    @Override
    public void foreach(Consumer<A> consumer) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B> Attempt<B> map(Function<? super A, ? extends B> f) {
      return (Attempt<B>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B> Attempt<B> flatMap(Function<? super A, Attempt<B>> f) {
      return (Attempt<B>) this;
    }

    @Override
    public <B> B fold(Function<? super A, ? extends B> f, Function<? super Exception, ? extends B> g) {
      return g.apply(error);
    }

    @Override
    public Attempt<A> onError(Function<? super Exception, Attempt<A>> f) {
      return eval(() -> f.apply(error).get());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Failure<?> failure = (Failure<?>) o;
      return Objects.equals(error, failure.error);
    }

    @Override
    public int hashCode() {
      return Objects.hash(error);
    }

    @Override
    public String toString() {
      return "Failure{" + error + '}';
    }
  }
}
