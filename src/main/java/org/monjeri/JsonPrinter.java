package org.monjeri;

import org.monjeri.Json.JObject;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.monjeri.Util.stringAppend;

class JsonPrinter {

  public static String noSpaces(Json json) {
    return render(json, 0, 0);
  }

  public static String spaces2(Json json) {
    return render(json, 2, 0);
  }

  public static String spaces4(Json json) {
    return render(json, 4, 0);
  }

  private static String render(Json json, int ns, int level) {
    final String spc2 = ns > 0 ? "\n" + spaces((level + 1) * ns) : "";
    final String spc1 = ns > 0 ? "\n" + spaces(level * ns) : "";
    return json.fold(
        n -> "null",
        Object::toString,
        BigDecimal::toString,
        s -> "\"" + s + "\"",
        a -> "[" + spc2 + a.filter(e -> !e.isNull())
            .map(j -> render(j, ns, level + 1))
            .intersperse("," + spc2)
            .foldLeft("", stringAppend()) + spc1 + "]",
        o -> "{" + spc2 + o.getValues()
            .filter(e -> !e.value.isNull())
            .map(entryString(j -> render(j, ns, level + 1)))
            .intersperse("," + spc2)
            .foldLeft("", stringAppend()) + spc1 + "}",
        id -> render(id.toStrictMode(), ns, level),
        regex -> render(regex.toStrictMode(), ns, level),
        dbref -> render(dbref.toStrictMode(), ns, level));
  }

  private static Function<JObject.Entry, String> entryString(Function<Json, String> f) {
    return e -> "\"" + e.name + "\": " + f.apply(e.value);
  }

  private static String spaces(int n) {
    switch (n) {
      case 0:
        return "";
      case 2:
        return "  ";
      case 4:
        return "    ";
      case 6:
        return "      ";
      default:
        StringBuilder str = new StringBuilder();
        for (int i=0; i<n; i++) {
          str.append(" ");
        }
        return str.toString();
    }
  }
}
