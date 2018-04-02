package com.coxandkings.travel.bookingengine.db.config;

import java.util.HashMap;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.bson.Document;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.coxandkings.travel.bookingengine.db.exception.BookingDBConfigDataNotFoundException;
import com.coxandkings.travel.bookingengine.db.mongo.MongoProductConfig;
import com.coxandkings.travel.bookingengine.db.postgres.common.JsonPostgreSQLDialect;
import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.coxandkings.travel.bookingengine.db.repository")
public class DBConfig {

  private ComboPooledDataSource comboPoolDataSource;
  private LocalContainerEntityManagerFactoryBean entityManagerFactoryBean;
  private static String url;
  private static String userName;
  private static  String password ;
  private  static String driverClass;
  private static String schemaName;
  
  static {
     Document configDoc = MongoProductConfig.getConfig("BookingDBConfig");
     url = configDoc.getString("url");
     userName =  configDoc.getString("userName");
     password = configDoc.getString("password");
     driverClass = configDoc.getString("driverClass");
     schemaName =  configDoc.getString("schemaName");
  }

  @Bean(name = "dataSource")
  @Primary
  public DataSource dataSource() {
    try {
      // DB URL
     
        if (url == null || url.trim().isEmpty()||userName == null || userName.trim().isEmpty()|| schemaName == null || schemaName.trim().isEmpty()||password == null || password.trim().isEmpty()||driverClass == null || driverClass.trim().isEmpty()) {
          throw new BookingDBConfigDataNotFoundException();
      
        }

      if (comboPoolDataSource == null) {
        comboPoolDataSource = new ComboPooledDataSource();
        comboPoolDataSource.setJdbcUrl(url);
        comboPoolDataSource.setPassword(password);
        comboPoolDataSource.setUser(userName);
        comboPoolDataSource.setDriverClass(driverClass);
        comboPoolDataSource.setTestConnectionOnCheckout(true);
        comboPoolDataSource.setMinPoolSize(10);
        comboPoolDataSource.setMaxPoolSize(20);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return comboPoolDataSource;
  }


  @PersistenceContext(unitName = "primary")
  @Primary
  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder) {
    try {
      if (entityManagerFactoryBean == null) {
        entityManagerFactoryBean = builder.dataSource(dataSource()).properties(getJPAProperties())
            .persistenceUnit("primary").packages("com.coxandkings.travel.bookingengine.db.model").build();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return entityManagerFactoryBean;
  }

 

  private HashMap<String, Object> getJPAProperties() {
    HashMap<String, Object> jpaProperties = new HashMap<>();
    jpaProperties.put("hibernate.show_sql", "true");
    jpaProperties.put("hibernate.connection.autocommit", "true");
    jpaProperties.put("hibernate.hbm2ddl.auto", "create-drop");
    jpaProperties.put("hibernate.dialect", JsonPostgreSQLDialect.class.getCanonicalName());
    
    
    jpaProperties.put("hibernate.default_schema", schemaName);
    return jpaProperties;


  }

}
