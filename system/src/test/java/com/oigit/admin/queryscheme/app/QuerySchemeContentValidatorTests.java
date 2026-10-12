package com.oigit.admin.queryscheme.app;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.scene.*;
import com.oigit.admin.core.query.ast.QueryOperator;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class QuerySchemeContentValidatorTests {
 private final QuerySchemeContentValidator validator = new QuerySchemeContentValidator(new ObjectMapper(), List.of(new SceneQueryDefinition<Row>() {
  public String sceneCode() { return "test.scene"; }
  public Map<String, com.baomidou.mybatisplus.core.toolkit.support.SFunction<Row, String>> textFields() { return Map.of("name", Row::name); }
  public Map<String, com.baomidou.mybatisplus.core.toolkit.support.SFunction<Row, java.time.LocalDateTime>> dateTimeFields() { return Map.of("created", Row::created); }
  public Map<String, com.baomidou.mybatisplus.core.toolkit.support.SFunction<Row, ?>> enumFields() { return Map.of(); }
  public Map<String, com.baomidou.mybatisplus.core.toolkit.support.SFunction<Row, ? extends Number>> numberFields() { return Map.of("amount", Row::amount); }
  public Map<String, com.baomidou.mybatisplus.core.toolkit.support.SFunction<Row, ?>> sortFields() { return Map.of("id", Row::amount); }
  public Set<QueryOperator> allowedOperators(String key) { return "name".equals(key) ? Set.of(QueryOperator.EQ, QueryOperator.IN) : SceneQueryDefinition.super.allowedOperators(key); }
 }));
 record Row(String name, Integer amount, java.time.LocalDateTime created) { }
 private String content(String condition) { return "{\"schemaVersion\":1,\"quickConditions\":[],\"advancedCondition\":" + condition + "}"; }
 @Test void acceptsTypedNestedConditions() {
  assertThatCode(() -> validator.validate("test.scene", content("""
   {"nodeType":"compose","logic":"OR","children":[
    {"nodeType":"text","field":"name","op":"EQ","value":"A"},
    {"nodeType":"number","field":"amount","op":"BETWEEN","start":1,"end":9}]}
   """))).doesNotThrowAnyException();
 }
 @Test void rejectsUnknownSortOnlyAndRestrictedFields() {
  for (String field : List.of("missing", "id", "amount", "name"))
   assertThatThrownBy(() -> validator.validate("test.scene", content("{\"nodeType\":\"text\",\"field\":\""+field+"\",\"op\":\"CONTAINS\",\"value\":\"A\"}"))).isInstanceOf(BizException.class);
 }
 @Test void rejectsMalformedAndUnsupportedContent() {
  for (String value : List.of("null", "", "{}", "{\"schemaVersion\":2,\"quickConditions\":[]}"))
   assertThatThrownBy(() -> validator.validate("test.scene", value)).isInstanceOf(BizException.class);
 }
 @Test void rejectsInvalidRangesAndDates() {
  for (String node : List.of(
   "{\"nodeType\":\"number\",\"field\":\"amount\",\"op\":\"BETWEEN\",\"start\":9,\"end\":1}",
   "{\"nodeType\":\"dateTime\",\"field\":\"created\",\"op\":\"EQ\",\"value\":\"invalid\"}"))
   assertThatThrownBy(() -> validator.validate("test.scene", content(node))).isInstanceOf(BizException.class);
 }
 @Test void countsTheCombinedRootAndStopsDeepRecursion() {
  String leaf = "{\"nodeType\":\"text\",\"field\":\"name\",\"op\":\"EQ\",\"value\":\"A\"}";
  String nested = leaf;
  for (int i=0;i<4;i++) nested = "{\"nodeType\":\"compose\",\"logic\":\"AND\",\"children\":["+nested+"]}";
  String deep = content(nested);
  assertThatThrownBy(() -> validator.validate("test.scene", deep)).isInstanceOf(BizException.class);
  String quick = "{\"columnId\":\"name\",\"condition\":"+leaf+"}";
  String many = "{\"schemaVersion\":1,\"quickConditions\":["+String.join(",", Collections.nCopies(20,quick))+"]}";
  assertThatThrownBy(() -> validator.validate("test.scene", many)).isInstanceOf(BizException.class);
 }
}
