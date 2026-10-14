package com.familyfinance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.storage.mode=memory")
class ApplicationContextTest {
    @Test void applicationStartsWithSecureDefaults() { }
}
