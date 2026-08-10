package com.redox.fintechBookingSystem.shared.json;


import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

//using jackson 3
@JacksonComponent
public class TrimmingJsonDeserializer extends ValueDeserializer<String> {
  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    String value=p.getString();
    return value==null? null:value.strip();
  }
}
