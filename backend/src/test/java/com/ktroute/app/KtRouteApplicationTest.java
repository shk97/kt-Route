package com.ktroute.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class KtRouteApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the entire Spring context starts without errors
    }
}
