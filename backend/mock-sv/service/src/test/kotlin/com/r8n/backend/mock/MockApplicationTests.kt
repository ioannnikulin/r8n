package com.r8n.backend.mock

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class MockApplicationTests {
    @Test
    fun contextLoads() {
    }
}
