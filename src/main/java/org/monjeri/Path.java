package org.monjeri;

/**
 * An immutable list of mongo key names.
 */
public final class Path {
    public static final Path root = new Path(List.nil());

    private final List<String> segments;

    private Path(List<String> segments) {
        this.segments = segments;
    }

    public static Path parse(String path) {
        if (path == null || path.trim().isEmpty()) {
            return root;
        } else {
            String[] parts = path.split("\\.");
            Path p = root;
            for (int i = parts.length-1; i>=0; i--) {
                p = p.cons(parts[i]);
            }
            return p;
        }
    }

    public static Path p(String path) {
        return parse(path);
    }

    public static Path single(String el) {
        return root.cons(el);
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    private void requireNonEmpty() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
    }

    public Path cons(String segment) {
        if (segment == null || segment.trim().isEmpty()) {
            throw new IllegalArgumentException("Segments must not be null or empty");
        }
        return new Path(segments.cons(segment));
    }

    public Path snoc(String segment) {
        return new Path(segments.reverse().cons(segment).reverse());
    }

    public String first() {
        requireNonEmpty();
        return segments.head();
    }

    public Path firstPath() {
        return Path.single(first());
    }

    public Path lastPath() {
        return Path.single(last());
    }

    public String last() {
        requireNonEmpty();
        return segments.reverse().head();
    }

    public Path dropFirst(int n) {
        return new Path(segments.drop(n));
    }

    public Path dropFirst() {
        return dropFirst(1);
    }

    public Path dropLast(int n) {
        List<String> p = segments.reverse();
        for (int i=0; i<n; i++) {
            p = p.tail();
        }
        return p.isEmpty() ? root : new Path(p.reverse());
    }

    public boolean startsWith(Path prefix) {
        List<List.P2<String, String>> zipped = segments.zip(prefix.segments);
        return length() >= prefix.length() && zipped.forall(List.P2::isEqual);
    }

    public boolean startsWith(String name) {
        return startsWith(Path.single(name));
    }

    public boolean endsWith(Path suffix) {
        List<List.P2<String, String>> zipped = segments.reverse().zip(suffix.segments.reverse());
        return length() >= suffix.length() && zipped.forall(List.P2::isEqual);
    }

    public boolean endsWith(String name) {
        return endsWith(Path.single(name));
    }

    public int length() {
        return segments.size();
    }

    public Path dropLast() {
        return dropLast(1);
    }

    public String render() {
        return segments.intersperse(".").foldLeft("", Util.stringAppend());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Path path = (Path) o;

        return segments != null ? segments.equals(path.segments) : path.segments == null;
    }

    @Override
    public int hashCode() {
        return segments != null ? segments.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Path{" + render() + '}';
    }
}
