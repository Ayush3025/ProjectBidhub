
package com.bidhub.bid.event;

public record BidPlacedEvent(
        Long auctionId,
        Long bidId,
        Long bidderId,
        Double amount,
        Double currentPrice
) {
}
