
package com.bidhub.auction.scheduler;

import com.bidhub.auction.service.AuctionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionService auctionService;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // Check for expired auctions every 5 seconds.
    @Scheduled(fixedDelay = 5000)
    public void closeExpiredAuctions() {

        try {
            int closedCount =
                    auctionService.closeExpiredAuctions();

            if (closedCount > 0) {
                logger.info(
                        "Auction scheduler processed {} expired auction(s).",
                        closedCount
                );
            }
        } catch (Exception exception) {
            logger.error(
                    "Failed to process expired auctions.",
                    exception
            );
        }
    }
}
