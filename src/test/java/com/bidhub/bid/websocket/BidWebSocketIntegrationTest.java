
package com.bidhub.bid.websocket;

import com.bidhub.bid.event.BidPlacedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
//import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
//import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BidWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldDeliverBidEventToSubscribedClient() throws Exception {
        WebSocketStompClient stompClient =
                new WebSocketStompClient(new StandardWebSocketClient());

        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        

        ArrayBlockingQueue<BidPlacedEvent> receivedEvents =
                new ArrayBlockingQueue<>(1);

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS);

        try {
            session.subscribe(
                    "/topic/auctions/1",
                    new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return BidPlacedEvent.class;
                        }

                        @Override
                        public void handleFrame(
                                StompHeaders headers,
                                Object payload) {
                            receivedEvents.offer((BidPlacedEvent) payload);
                        }
                    }
            );

            // Give the subscription time to reach the broker.
            Thread.sleep(300);

            BidPlacedEvent event = new BidPlacedEvent(
                    1L, 101L, 2L, 25000.0, 25000.0
            );

            // Publish directly to the application event system.
            // This checks the real listener, broker, and client connection.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                eventPublisher.publishEvent(event);
            });
            BidPlacedEvent received =
                    receivedEvents.poll(5, TimeUnit.SECONDS);

            assertNotNull(received, "Client did not receive the bid event");
            assertEquals(event.auctionId(), received.auctionId());
            assertEquals(event.bidId(), received.bidId());
            assertEquals(event.bidderId(), received.bidderId());
            assertEquals(event.amount(), received.amount());
            assertEquals(event.currentPrice(), received.currentPrice());

        } finally {
            session.disconnect();
            stompClient.stop();
        }
    }
}
