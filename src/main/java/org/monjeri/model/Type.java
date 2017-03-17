package org.monjeri.model;

import org.monjeri.Json;

public interface Type {

  Json.Type getJsonType();

  Json asJson();
}
