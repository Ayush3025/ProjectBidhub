
package com.bidhub.bid.websocket;

import com.bidhub.bid.event.BidPlacedEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BidWebSocketListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BidWebSocketListener listener;

    @Test
    void shouldSendBidEventToCorrectAuctionTopic() {
        BidPlacedEvent event = new BidPlacedEvent(
                1L,
                10L,
                2L,
                1200.0,
                1200.0
        );

        listener.handleBidPlaced(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/auctions/1",
                event
        );

        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void shouldUseDifferentTopicForDifferentAuction() {
        BidPlacedEvent event = new BidPlacedEvent(
                5L,
                20L,
                3L,
                2500.0,
                2500.0
        );

        listener.handleBidPlaced(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/auctions/5",
                event
        );
    }
}
