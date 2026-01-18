package com.jiralite.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
