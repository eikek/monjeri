package org.monjeri.model;

import org.monjeri.Path;

public class Test {

  public static class UserCollection extends Document {
    public static final String NAME = "users";
    public final Field<Atom> _id = _idField();
    public final Field<Atom> firstName = addField("firstname", Atom.string());
    public final Field<Atom> age = addField("age", Atom.number());

    public UserCollection(Path basePath) {
      super(basePath);
    }
  }

  public static class TestCollection extends Document {

    public final Field<Atom> _id = _idField();
    public final UserCollection userEmbedded = embed("userEmbedded", UserCollection::new).type();

    public final Field<Ref> userRef = addField("userRef", Atom.ref(UserCollection.NAME));

    public final Array<UserCollection> userArray = arrayField("userArray", UserCollection::new).type();


    public TestCollection(Path basePath) {
      super(basePath);
    }
  }

  public static void main(String[] args) {

    UserCollection user = new UserCollection(Path.root);
    TestCollection test = new TestCollection(Path.root);
    System.out.println(test.userEmbedded.firstName.path());
    System.out.println(user.firstName.path());
    System.out.println(test.findField(test.userEmbedded.firstName.path()));
    System.out.println(test.userEmbedded.findField(Path.p("firstname")));
    System.out.println("---");
    System.out.println(test.userArray.element().firstName.path());
    System.out.println(test.userArray.at(2).firstName.path());
    System.out.println(test.userArray.each().firstName.path());
    System.out.println(test);
  }
}
