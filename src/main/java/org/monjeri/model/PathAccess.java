package org.monjeri.model;

import org.monjeri.Path;

public interface PathAccess {

  Path path();

  default String name() {
    return path().last();
  }

  default Path nameAsPath() {
    return Path.single(name());
  }
}
