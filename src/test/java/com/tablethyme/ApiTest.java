package com.tablethyme;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","app.owner.email=owner@example.com","app.owner.password=TestOwnerPassword123!"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
 MockHttpSession login(String email,String password)throws Exception{return (MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();}
 @Test @Order(1) void authAndRoles()throws Exception{
  mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"name\":\"Guest\",\"email\":\"guest@example.com\",\"password\":\"GuestPassword123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CUSTOMER"));
  var guest=login("guest@example.com","GuestPassword123");
  mvc.perform(post("/api/owner/tables").session(guest).contentType("application/json").content("{\"name\":\"T99\",\"capacity\":4,\"status\":\"Available\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"guest@example.com\",\"password\":\"wrongpass\"}")).andExpect(status().isUnauthorized());
 }
 @Test @Order(2) void bookingConflictPricingAndCancellation()throws Exception{
  var guest=login("guest@example.com","GuestPassword123");String day=LocalDate.now().plusDays(2).toString();
  String booking="{\"hotelId\":1,\"tableId\":1,\"guest\":\"Guest\",\"email\":\"guest@example.com\",\"guests\":2,\"date\":\""+day+"\",\"time\":\"18:00\",\"total\":1,\"items\":[{\"id\":1,\"qty\":2}]}";
  mvc.perform(post("/api/reservations").session(guest).contentType("application/json").content(booking)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(640));
  mvc.perform(post("/api/reservations").session(guest).contentType("application/json").content(booking.replace("18:00","18:30"))).andExpect(status().isConflict());
  mvc.perform(post("/api/reservations").session(guest).contentType("application/json").content(booking.replace("18:00","19:30"))).andExpect(status().isOk());
  mvc.perform(patch("/api/reservations/1").session(guest).contentType("application/json").content("{\"status\":\"Seated\"}")).andExpect(status().isBadRequest());
  mvc.perform(patch("/api/reservations/1").session(guest).contentType("application/json").content("{\"status\":\"Cancelled\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/reservations").session(guest).contentType("application/json").content(booking)).andExpect(status().isOk());
 }
 @Test @Order(3) void ownerIsolationAndOrderTransitions()throws Exception{
  var owner=login("owner@example.com","TestOwnerPassword123!");
  mvc.perform(patch("/api/owner/orders/2").session(owner).contentType("application/json").content("{\"status\":\"Served\"}")).andExpect(status().isBadRequest());
  mvc.perform(patch("/api/owner/orders/2").session(owner).contentType("application/json").content("{\"status\":\"Preparing\"}")).andExpect(status().isOk());
  var other=(MockHttpSession)mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"name\":\"Other owner\",\"email\":\"other@example.com\",\"password\":\"Password123\",\"role\":\"OWNER\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();
  mvc.perform(put("/api/owner/menu/1").session(other).contentType("application/json").content("{}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/auth/logout").session(owner).header("Origin","https://evil.example")).andExpect(status().isForbidden());
 }
}
