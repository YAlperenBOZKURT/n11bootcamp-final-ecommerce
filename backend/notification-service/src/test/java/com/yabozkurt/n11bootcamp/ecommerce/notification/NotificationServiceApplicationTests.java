package com.yabozkurt.n11bootcamp.ecommerce.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {}
}
