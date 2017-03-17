package org.monjeri;

import org.monjeri.model.Atom;
import org.monjeri.model.CollectionModels;
import org.monjeri.model.Document;

import java.math.BigDecimal;
import java.util.Objects;

import static org.monjeri.Json.JObject.ofName;

public class TestDatabase {

  public final Model model = new Model(TestUtils.randomDbName());
  public final Codec codec = new Codec(model);

  public static class Person {
    private final String name;
    private final int age;
    private final boolean active;
    private final BigDecimal wealth;

    public Person(String name, int age, boolean active, BigDecimal wealth) {
      this.name = name;
      this.age = age;
      this.active = active;
      this.wealth = wealth;
    }

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }

    public boolean isActive() {
      return active;
    }

    public BigDecimal getWealth() {
      return wealth;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Person person = (Person) o;
      return age == person.age &&
          active == person.active &&
          Objects.equals(name, person.name) &&
          Objects.equals(wealth, person.wealth);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, age, active, wealth);
    }

    @Override
    public String toString() {
      return "Person: "+ asJson().spaces4();
    }

    public Json.JObject asJson() {
      return new TestDatabase().codec.encodePerson().apply(this);
    }
  }

  public static class Model extends CollectionModels {
    public final PersonCollection person = new PersonCollection();

    // top level document
    public static class PersonCollection extends Document {
      public final Field<Atom> _id = _idField();
      public final Field<Atom> name = addField("name", Atom.string());
      public final Field<Atom> age = addField("age", Atom.number());
      public final Field<Atom> active = addField("active", Atom.bool());
      public final Field<Atom> wealth = addField("wealth", Atom.number());

      public PersonCollection() {
        super(Path.root);
      }

      @Override
      public String name() {
        return "persons";
      }
    }

    public Model(String database) {
      super(database);
    }
  }

  public static class Codec implements BaseDecode, BaseEncode {
    private final Model model;

    public Codec(Model model) {
      this.model = model;
    }

    public Decode<Person> decodePerson() {
      return doc -> new Person(
          string(model.person.name.nameAsPath()).apply(doc),
          integer(model.person.age.nameAsPath()).apply(doc),
          bool(model.person.active.nameAsPath()).apply(doc),
          decimal(model.person.wealth.nameAsPath()).apply(doc)
      );
    }

    public Encode.Object<Person> encodePerson() {
      return person -> Json.obj(
          ofName(model.person.name, Json.str(person.name)),
          ofName(model.person.age, Json.num(person.age)),
          ofName(model.person.active, Json.of(person.active)),
          ofName(model.person.wealth, Json.num(person.wealth))
      );
    }
  }
}
