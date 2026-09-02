package com.demo.teamboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TeamboardApiApplicationTests {

	// The real decoder resolves its signing keys from the Auth0 tenant at startup, which would make
	// this test depend on network access and on application.yml holding real credentials.
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
