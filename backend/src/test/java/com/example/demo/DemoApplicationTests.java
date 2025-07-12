package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;


import static org.mockito.Mockito.mock;

@SpringBootTest
class DemoApplicationTests {

  @Configuration
  static class TestConfig {
    @Bean
    UserDetailsService userDetailsService() {
      // returns a mock object (can also use your real UserDetailsServiceImpl)
      return mock(UserDetailsService.class);
    }
  }

  @Test
  void contextLoads() {
    // starts Spring context
  }
}
