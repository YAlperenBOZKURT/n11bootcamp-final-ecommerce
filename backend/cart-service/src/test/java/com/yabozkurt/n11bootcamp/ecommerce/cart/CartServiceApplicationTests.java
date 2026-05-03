package com.yabozkurt.n11bootcamp.ecommerce.cart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.config.import=",
		"eureka.client.enabled=false"
})
@Disabled("Config server import is mandatory in this module bootstrap")
class CartServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
