
package com.bidhub.bid.websocket;

import com.bidhub.bid.event.BidPlacedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BidWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    public BidWebSocketListener(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBidPlaced(BidPlacedEvent event) {

        messagingTemplate.convertAndSend(
                "/topic/auctions/" + event.auctionId(),
                event
        );
    }
}
