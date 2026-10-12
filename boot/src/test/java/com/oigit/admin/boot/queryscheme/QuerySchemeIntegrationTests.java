package com.oigit.admin.boot.queryscheme;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.queryscheme.app.QuerySchemeAppService;
import com.oigit.admin.queryscheme.dto.req.*;
import com.oigit.admin.queryscheme.dto.rsp.QuerySchemeRspDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class QuerySchemeIntegrationTests {
 static final String SCENE = "mdm.global.dict.item.page";
 static final String CONTENT = "{\"schemaVersion\":1,\"quickConditions\":[],\"advancedCondition\":{\"nodeType\":\"text\",\"field\":\"dictItemName\",\"op\":\"CONTAINS\",\"value\":\"海运\"}}";
 @Autowired QuerySchemeAppService app;
 @Autowired JdbcTemplate jdbc;
 @BeforeEach void setup() { jdbc.update("delete from sys_user_query_scheme"); jdbc.update("delete from sys_user_query_scheme_lock"); OperatorContext.set(901L,"test",null); }
 @AfterEach void clear() { OperatorContext.clear(); }
 QuerySchemeCreateReqDTO request(String name, boolean defaultScheme) {
  var req=new QuerySchemeCreateReqDTO(); req.setSceneCode(SCENE);req.setSchemeName(name);req.setContentJson(CONTENT);req.setDefaultScheme(defaultScheme);return req;
 }
 @Test void isolatesUsersAndRejectsStaleChanges() {
  Long id=app.create(request("方案",true));
  OperatorContext.set(902L,"other",null);
  assertThat(app.list(SCENE)).isEmpty();
  assertThatThrownBy(()->app.delete(id,0)).isInstanceOf(BizException.class);
  OperatorContext.set(901L,"test",null);
  var update=new QuerySchemeUpdateReqDTO();update.setId(id);update.setVersion(0);update.setSchemeName("修改");update.setContentJson(CONTENT);
  app.update(update);
  assertThat(app.list(SCENE).get(0).getVersion()).isEqualTo(1);
  assertThatThrownBy(()->app.update(update)).isInstanceOf(BizException.class);
  assertThatThrownBy(()->app.delete(id,0)).isInstanceOf(BizException.class);
  app.delete(id,1); assertThat(app.list(SCENE)).isEmpty();
 }
 @Test void supportsRepeatedSameNameDeletionAndDefaultCancellation() {
  for(int i=0;i<3;i++){var id=app.create(request("复用",true));app.delete(id,0);}
  app.create(request("复用",true));
  var req=new QuerySchemeDefaultUpdateReqDTO();req.setSceneCode(SCENE);app.updateDefault(req);
  assertThat(app.list(SCENE)).noneMatch(QuerySchemeRspDTO::isDefaultScheme);
 }
 @Test void rejectsMissingIdentityAndDuplicateNames() {
  app.create(request("同名",false));
  assertThatThrownBy(()->app.create(request("同名",false))).isInstanceOf(BizException.class);
  OperatorContext.clear();assertThatThrownBy(()->app.list(SCENE)).isInstanceOf(BizException.class);
 }
 @Test void serializesConcurrentCreationAndDefaultSelection() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  try {
   var a=pool.submit(()->{OperatorContext.set(901L,"test",null);try{start.await();return app.create(request("A",true));}finally{OperatorContext.clear();}});
   var b=pool.submit(()->{OperatorContext.set(901L,"test",null);try{start.await();return app.create(request("B",true));}finally{OperatorContext.clear();}});
   start.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
   assertThat(app.list(SCENE)).hasSize(2).filteredOn(QuerySchemeRspDTO::isDefaultScheme).hasSize(1);
  } finally {pool.shutdownNow();}
 }
 @Test void enforcesPerUserSceneLimit() {
  for(int i=0;i<20;i++)app.create(request("方案"+i,false));
  assertThatThrownBy(()->app.create(request("超限",false))).isInstanceOf(BizException.class);
 }
}
