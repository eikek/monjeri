package org.monjeri;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.security.SecureRandom;

import static org.monjeri.TestUtils.dbPrefix;

public abstract class AbstractMongoTest implements Monjeri {

  private MongoClient client;
  private final TestDatabase testDb = new TestDatabase();

  @BeforeClass
  public void setUp() throws Exception {
    String host = System.getProperty("monjeri.test.mongodb.host");
    if (host == null || host.trim().isEmpty()) {
      host = "localhost";
    }
    String sport = System.getProperty("monjeri.test.mongodb.port");
    int port = 27017;
    if (sport != null) {
      port = Integer.parseInt(sport);
    }
    try {
      MongoClientOptions opts = MongoClientOptions.builder()
          .serverSelectionTimeout(200)
          .writeConcern(WriteConcern.ACKNOWLEDGED)
          .build();

      client = new MongoClient(new ServerAddress(host, port), opts);
      client.listDatabaseNames().first();
    } catch (MongoException e) {
      client.close();
      client = null;
      throw new SkipException("no mongodb setup", e);
    }
  }

  @AfterClass
  public void tearDown() throws Exception {
    if (client != null) {
      List.fromIterable(client.listDatabaseNames())
          .filter(name -> name.startsWith(dbPrefix))
          .foreach(client::dropDatabase);
    }
  }

  protected MongoClient client() {
    return client;
  }

  protected TestDatabase testData() {
    return testDb;
  }

  protected TestDatabase.Model model() {
    return testDb.model;
  }

  protected TestDatabase.Codec codec() {
    return testDb.codec;
  }


  protected MonTask1<MongoDatabase> newDb() {
    return db(TestUtils.randomDbName());
  }

}
