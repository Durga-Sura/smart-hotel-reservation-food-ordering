package com.tablethyme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class Application {
 public static void main(String[] args) { SpringApplication.run(Application.class,args); }
 @Bean BCryptPasswordEncoder passwords(){ return new BCryptPasswordEncoder(12); }
 @Bean CommandLineRunner initialize(JdbcTemplate db, BCryptPasswordEncoder passwords,
  @Value("${app.owner.email}") String email,@Value("${app.owner.password}") String password){return args->{
   if(email.isBlank()||password.isBlank())return;
   if(password.length()<12)throw new IllegalArgumentException("OWNER_PASSWORD must be at least 12 characters");
   if(db.queryForObject("SELECT COUNT(*) FROM app_users WHERE email=?",Integer.class,email.toLowerCase())>0)return;
   db.update("INSERT INTO app_users(name,email,password_hash,role) VALUES(?,?,?,?)","Hotel Owner",email.toLowerCase(),passwords.encode(password),"OWNER");
   long owner=db.queryForObject("SELECT id FROM app_users WHERE email=?",Long.class,email.toLowerCase());
   db.update("INSERT INTO hotels(owner_id,name,location,description,cuisine,image) VALUES(?,?,?,?,?,?)",owner,"The Fern Residency","Indiranagar, Bengaluru","A little greenery, warm service, and memorable food in the heart of Indiranagar.","Indian · Continental","https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=85");
   long hotel=db.queryForObject("SELECT id FROM hotels WHERE owner_id=?",Long.class,owner);
   for(int i=1;i<=12;i++)db.update("INSERT INTO dining_tables(hotel_id,name,capacity,status) VALUES(?,?,?,?)",hotel,String.format("T%02d",i),i%3==0?6:i%2==0?2:4,"Available");
   String[][] menu={{"Paneer Tikka","Starters","320","Tandoor-grilled cottage cheese, peppers & mint chutney.","photo-1631452180519-c014fe946bc7"},{"Pesto Primavera","Mains","420","Fresh basil pesto, seasonal vegetables & parmesan.","photo-1473093295043-cdd812d0e601"},{"Garden Fresh Salad","Starters","260","Seasonal greens with citrus dressing.","photo-1512621776951-a57141f2eefd"},{"Wood-fired Margherita","Mains","480","Fresh mozzarella, tomatoes and basil.","photo-1513104890138-7c749659a591"}};
   for(String[] m:menu)db.update("INSERT INTO menu_items(hotel_id,name,category,price,description,image) VALUES(?,?,?,?,?,?)",hotel,m[0],m[1],new java.math.BigDecimal(m[2]),m[3],"https://images.unsplash.com/"+m[4]+"?auto=format&fit=crop&w=500&q=85");
  };}
}
