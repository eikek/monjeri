package org.monjeri;

import com.mongodb.MongoException;

public class LockedException extends MongoException {

  public LockedException(String msg) {
    super(msg);
  }

  public LockedException(int code, String msg) {
    super(code, msg);
  }

  public LockedException(String msg, Throwable t) {
    super(msg, t);
  }

  public LockedException(int code, String msg, Throwable t) {
    super(code, msg, t);
  }
}
