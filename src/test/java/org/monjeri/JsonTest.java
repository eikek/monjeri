package org.monjeri;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.monjeri.Json.JObject.entry;

public class JsonTest {

  @Test
  public void testSerialize() throws Exception {
    ObjectId oid = new ObjectId("58c4002bd93a0043e0fd01a4");
    Json.JObject json = Json.obj(
        entry("id", Json.Null()),
        entry("_id", Json.id(oid)),
        entry("regex", Json.regex("^abc")),
        entry("male", Json.True()),
        entry("lastname", Json.str("Schmal")),
        entry("firstname", Json.str("Willi")),
        entry("age", Json.num(22)),
        entry("aliases", Json.array(Json.str("will"), Json.str("ack"), Json.str("ackli"))),
        entry("money", Json.obj(
            entry("2004", Json.num("1401.15")),
            entry("2005", Json.num("12123.15"))
        )),
        entry("score", Json.num(3.12)),
        entry("aref", Json.dbref("othercollection", oid))
    );

    Assert.assertEquals(json.noSpaces(),
        "{\"_id\": {\"$oid\": \"58c4002bd93a0043e0fd01a4\"},\"regex\": {\"$regex\": \"^abc\"},\"male\": true,\"lastname\": \"Schmal\",\"firstname\": \"Willi\",\"age\": 22,\"aliases\": [\"will\",\"ack\",\"ackli\"],\"money\": {\"2004\": 1401.15,\"2005\": 12123.15},\"score\": 3.120000000000000106581410364015028,\"aref\": {\"$ref\": \"othercollection\",\"$id\": \"58c4002bd93a0043e0fd01a4\"}}");

  }
}
