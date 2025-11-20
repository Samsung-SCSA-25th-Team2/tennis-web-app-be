package com.example.scsa;

import com.example.scsa.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRedisConfig.class)
class ScsaApplicationTests {

	@Test
	void contextLoads() {
	}

}
