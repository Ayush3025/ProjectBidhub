
package com.bidhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void shouldLoadWebSocketMessagingConfiguration() {
        assertNotNull(messagingTemplate);
    }
}
