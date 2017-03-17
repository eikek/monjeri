package org.monjeri;

import com.mongodb.DBRef;
import jdk.nashorn.internal.scripts.JO;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.monjeri.model.PathAccess;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.monjeri.Json.JObject.entry;

/**
 * Methods for constructing json data literals.
 */
public abstract class Json {

  private Json() {}

  public enum Type {
    NULL, BOOL, NUMBER, STRING, ARRAY, OBJECT, OBJECT_ID, REGEX, DBREF
  }

  public static Json tryOf(Object obj) {
    if (obj == null) {
      return Null();
    }
    if (obj instanceof Json) {
      return (Json) obj;
    }
    if (obj instanceof ObjectId) {
      return id((ObjectId) obj);
    }
    if (obj instanceof DBRef) {
      return dbref((DBRef) obj);
    }
    if (obj instanceof Boolean){
      return of((Boolean) obj);
    }
    if (obj instanceof BigDecimal) {
      return num((BigDecimal) obj);
    }
    if (obj instanceof Integer) {
      return num((int) obj);
    }
    if (obj instanceof Long) {
      return num((long) obj);
    }
    if (obj instanceof String) {
      return str((String) obj);
    }
    throw new IllegalArgumentException("Cannot create json value from: " + obj);
  }


  public static JNull Null() {
    return JNull.INSTANCE;
  }

  public static JBool of(boolean value) {
    return value ? JBool.TRUE : JBool.FALSE;
  }
  public static JBool True() {
    return JBool.TRUE;
  }

  public static JBool False() {
    return JBool.FALSE;
  }

  public static JNumber num(BigDecimal num) {
    return new JNumber(Objects.requireNonNull(num));
  }
  public static JNumber num(int num) {
    return new JNumber(new BigDecimal(num));
  }
  public static JNumber num(double num) {
    return new JNumber(new BigDecimal(num, MathContext.DECIMAL128));
  }
  public static JNumber num(String num) {
    return new JNumber(new BigDecimal(num));
  }

  public static JString str(String value) {
    return new JString(Objects.requireNonNull(value));
  }

  public static JArray array(List<? extends Json> values) {
    return new JArray(values);
  }
  public static JArray array(Json... values) {
    return new JArray(List.of(values));
  }

  public static JObject obj(JObject.Entry... entries) {
    return JObject.of(entries);
  }
  public static JObject obj(String name, Json value) {
    return JObject.of(entry(name, value));
  }

  public static JObjectId id(ObjectId id) {
    return new JObjectId(id);
  }

  public static JRegex regex(String regex) {
    return new JRegex(regex);
  }
  public static JRegex regex(String regex, String options) {
    return new JRegex(regex, options);
  }

  public static JRegex regex(Pattern pattern) {
    return new JRegex(pattern);
  }

  public static JDBRef dbref(DBRef ref) {
    return new JDBRef(ref);
  }
  public static JDBRef dbref(String collection, Object id) {
    return new JDBRef(new DBRef(collection, id));
  }

  public static JDBRef dbref(org.monjeri.model.Document collection, Object id) {
    return dbref(collection.name(), id);
  }

  public  abstract Type type();

  public boolean isNull() {
    return false;
  }
  public Optional<Boolean> asBool() {
    return Optional.empty();
  }
  public Optional<BigDecimal> asNumber() {
    return Optional.empty();
  }
  public Optional<String> asString() {
    return Optional.empty();
  }
  public Optional<List<Json>> asArray() {
    return Optional.empty();
  }
  public Optional<JObject> asObject() {
    return Optional.empty();
  }
  public Optional<ObjectId> asObjectId() {
    return Optional.empty();
  }
  public Optional<String> asRegex() {
    return Optional.empty();
  }
  public Optional<DBRef> asDBRef() {
    return Optional.empty();
  }

  public String noSpaces() {
    return JsonPrinter.noSpaces(this);
  }

  public String spaces2() {
    return JsonPrinter.spaces2(this);
  }

  public String spaces4() {
    return JsonPrinter.spaces4(this);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public  <V> V fold(
      Function<JNull, V> nullf,
      Function<Boolean, V> boolf,
      Function<BigDecimal, V> numf,
      Function<String, V> stringf,
      Function<List<Json>, V> arrayf,
      Function<JObject, V> objf,
      Function<JObjectId, V> idf,
      Function<JRegex, V> regexf,
      Function<JDBRef, V> dbreff
  ) {
    switch (this.type()) {
      case NULL:
        return nullf.apply(Null());
      case BOOL:
        return boolf.apply(asBool().get());
      case NUMBER:
        return numf.apply(asNumber().get());
      case STRING:
        return stringf.apply(asString().get());
      case ARRAY:
        return arrayf.apply(asArray().get());
      case OBJECT:
        return objf.apply(asObject().get());
      case OBJECT_ID:
        return idf.apply((JObjectId) this);
      case REGEX:
        return regexf.apply((JRegex) this);
      case DBREF:
        return dbreff.apply((JDBRef) this);
      default:
        throw new AssertionError("Unreachable code");
    }
  }

  public final static class JNull extends Json {
    public static final JNull INSTANCE = new JNull();
    @Override
    public Type type() {
      return Type.NULL;
    }

    @Override
    public boolean isNull() {
      return true;
    }
  }

  public final static class JBool extends Json {
    public static final JBool TRUE = new JBool(true);
    public static final JBool FALSE = new JBool(false);

    private final boolean value;

    private JBool(boolean value) {
      this.value = value;
    }


    @Override
    public Type type() {
      return Type.BOOL;
    }

    @Override
    public Optional<Boolean> asBool() {
      return Optional.of(value);
    }

    public boolean isValue() {
      return value;
    }
  }

  public final static class JNumber extends Json {
    private final BigDecimal value;

    private JNumber(BigDecimal value) {
      this.value = value;
    }

    @Override
    public Type type() {
      return Type.NUMBER;
    }

    @Override
    public Optional<BigDecimal> asNumber() {
      return Optional.of(value);
    }
  }

  public final static class JString extends Json {
    private final String value;

    private JString(String value) {
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public Type type() {
      return Type.STRING;
    }

    public String getValue() {
      return value;
    }

    @Override
    public Optional<String> asString() {
      return Optional.of(value);
    }
  }

  public final static class JArray extends Json implements Iterable<Json> {
    private final List<Json> value;

    @SuppressWarnings("unchecked")
    private JArray(List<? extends Json> value) {
      this.value = (List<Json>) value;
    }

    @Override
    public Type type() {
      return Type.ARRAY;
    }

    @Override
    public Optional<List<Json>> asArray() {
      return Optional.of(value);
    }

    @Override
    public Iterator<Json> iterator() {
      return value.iterator();
    }
  }

  public final static class JObject extends Json implements Iterable<JObject.Entry> {
    private static final JObject EMPTY = new JObject(List.nil());
    private final List<Entry> values;

    private Map<String, Json> map = null;

    private JObject(List<Entry> values) {
      this.values = values;
    }

    @Override
    public Type type() {
      return Type.OBJECT;
    }

    @Override
    public Optional<JObject> asObject() {
      return Optional.of(this);
    }

    @Override
    public Iterator<JObject.Entry> iterator() {
      return values.iterator();
    }


    public static JObject empty() {
      return EMPTY;
    }

    public static JObject of(Entry... entries) {
      List<Entry> es = List.of(entries);
      return new JObject(es.distinct(Eq.objectEq()));
    }

    public static JObject of(String name, Json value) {
      return of(entry(name, value));
    }

    public static JObject byId(Object id) {
      return of("_id", Json.tryOf(id));
    }

    public Optional<Json> get(String name) {
      return Optional.ofNullable(makeMap().get(name));
    }

    public List<Entry> getValues() {
      return values;
    }

    private Map<String, Json> makeMap() {
      if (map == null) {
        map = new LinkedHashMap<>();
        values.foreach(e -> map.put(e.name, e.value));
      }
      return map;
    }

    public JObject put(Entry entry) {
      return new JObject(values
          .filter(e -> e.notName(entry.name))
          .append(List.of(entry)));
    }

    public JObject put(String name, Json value) {
      return put(entry(name, value));
    }

    public JObject putPath(Path path, Json value) {
      return put(ofPath(path, value));
    }

    public JObject putName(Path path, Json value) {
      return put(ofName(path, value));
    }

    public JObject putPath(PathAccess path, Json value) {
      return put(ofPath(path, value));
    }

    public JObject putName(PathAccess path, Json value) {
      return put(ofName(path, value));
    }

    public Document toDocument() {
      return (Document) toObject(this);
    }

    private static Object toObject(Json json) {
      return json.fold(
          n -> null,
          b -> b,
          bd -> {
            if (bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
              long val = bd.longValueExact();
              if (val > Integer.MAX_VALUE) {
                return val;
              } else {
                return (int) val;
              }
            } else {
              return bd.toPlainString();
            }
          },
          str -> str,
          a -> a.map(JObject::toObject).toJava(),
          o -> {
            Document doc = new Document();
            o.values.foreach(e ->
                Optional.ofNullable(toObject(e.value))
                    .ifPresent(v -> doc.put(e.name, v)));
            return doc;
          },
          Json.JObjectId::getId,
          regex -> regex.toStrictMode().toDocument(),
          ref -> ref.toStrictMode().toDocument());
    }

    public static final class Entry {
      public final String name;
      public final Json value;

      public Entry(String name, Json value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
      }

      public boolean isName(String name) {
        return this.name.equals(name);
      }

      public boolean notName(String name) {
        return !isName(name);
      }
    }


    public static Entry entry(String name, Json value) {
      return new Entry(name, value);
    }

    public static Entry ofPath(Path path, Json value) {
      return new Entry(path.render(), value);
    }

    public static Entry ofName(Path path, Json value) {
      return new Entry(path.last(), value);
    }

    public static Entry ofName(PathAccess path, Json value) {
      return entry(path.name(), value);
    }
    public static Entry ofPath(PathAccess path, Json value) {
      return entry(path.path().render(), value);
    }

  }


  // until here it is json, now the mongodb special things come

  public final static class JObjectId extends Json {
    private final ObjectId id;

    public JObjectId(ObjectId id) {
      this.id = id;
    }

    @Override
    public Type type() {
      return Type.OBJECT_ID;
    }

    @Override
    public Optional<ObjectId> asObjectId() {
      return Optional.of(id);
    }

    public ObjectId getId() {
      return id;
    }

    public JObject toStrictMode() {
      return JObject.of("$oid", Json.str(id.toString()));
    }

    public String toShellMode() {
      return String.format("ObjectId(\"%s\")", id);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JObjectId jObjectId = (JObjectId) o;
      return Objects.equals(id, jObjectId.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "JObjectId{" +
          "id=" + id +
          '}';
    }
  }

  public final static class JRegex extends Json {
    private final Pattern pattern;
    private final String options;

    public JRegex(Pattern pattern) {
      this.pattern = pattern;
      String opts = "";
      int flags = pattern.flags();
      if ((flags & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE) {
        opts += "i";
      }
      if ((flags & Pattern.DOTALL) == Pattern.DOTALL) {
        opts += "s";
      }
      if ((flags & Pattern.MULTILINE) == Pattern.MULTILINE) {
        opts += "m";
      }
      this.options = opts;
    }
    public JRegex(String regex) {
      this(regex, "");
    }

    public JRegex(String regex, String options) {
      this.options = options;
      if (options == null || options.isEmpty()) {
        this.pattern = Pattern.compile(regex);
      } else {
        int flags = 0;
        if (options.contains("i")) {
          flags = flags | Pattern.CASE_INSENSITIVE;
        }
        if (options.contains("s")) {
          flags = flags | Pattern.DOTALL;
        }
        if (options.contains("m")) {
          flags = flags | Pattern.MULTILINE;
        }
        this.pattern = Pattern.compile(regex, flags);
      }
    }

    @Override
    public Type type() {
      return Type.REGEX;
    }

    public Pattern getRegex() {
      return pattern;
    }

    @Override
    public Optional<String> asRegex() {
      return Optional.of(pattern.pattern());
    }

    public JObject toStrictMode() {
      JObject o = JObject.of("$regex", Json.str(pattern.pattern()));
      return options.isEmpty() ? o : o.put("$options", Json.str(options));
    }

    public String toShellMode() {
      return String.format("/%s/%s", pattern.pattern(), options);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JRegex jRegex = (JRegex) o;
      return Objects.equals(pattern, jRegex.pattern) &&
          Objects.equals(options, jRegex.options);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pattern, options);
    }

    @Override
    public String toString() {
      return "JRegex{" +
          "regex='" + pattern.pattern() + '\'' +
          ", options='" + options + '\'' +
          '}';
    }
  }

  public final static class JDBRef extends Json {
    private final DBRef ref;

    public JDBRef(DBRef ref) {
      this.ref = ref;
    }

    @Override
    public Type type() {
      return Type.DBREF;
    }

    @Override
    public Optional<DBRef> asDBRef() {
      return Optional.of(ref);
    }

    public DBRef getRef() {
      return ref;
    }

    public Object getId() {
      return ref.getId();
    }

    public String getCollection() {
      return ref.getCollectionName();
    }

    public JObject toStrictMode() {
      return JObject.of(
          entry("$ref", Json.str(ref.getCollectionName())),
          entry("$id", Json.str(ref.getId().toString()))
      );
    }

    public String toShellMode() {
      return String.format("DBRef(\"%s\", \"%s\"", ref.getCollectionName(), ref.getId());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JDBRef jdbRef = (JDBRef) o;
      return Objects.equals(ref, jdbRef.ref);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ref);
    }

    @Override
    public String toString() {
      return "JDBRef{" +
          "ref=" + ref +
          '}';
    }
  }
}
