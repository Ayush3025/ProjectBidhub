
package com.bidhub.bid.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidPlacedEventTest {

    @Test
    void shouldCreateEventWithCorrectValues() {
        BidPlacedEvent event = new BidPlacedEvent(
                1L,
                101L,
                2L,
                25000.0,
                25000.0
        );

        assertEquals(1L, event.auctionId());
        assertEquals(101L, event.bidId());
        assertEquals(2L, event.bidderId());
        assertEquals(25000.0, event.amount());
        assertEquals(25000.0, event.currentPrice());
    }

    @Test
    void shouldSupportEqualityForSameValues() {
        BidPlacedEvent event1 = new BidPlacedEvent(
                1L, 101L, 2L, 25000.0, 25000.0
        );

        BidPlacedEvent event2 = new BidPlacedEvent(
                1L, 101L, 2L, 25000.0, 25000.0
        );

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void shouldDetectDifferentAuctionIds() {
        BidPlacedEvent event1 = new BidPlacedEvent(
                1L, 101L, 2L, 25000.0, 25000.0
        );

        BidPlacedEvent event2 = new BidPlacedEvent(
                2L, 101L, 2L, 25000.0, 25000.0
        );

        assertNotEquals(event1, event2);
    }
}
