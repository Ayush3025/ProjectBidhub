
package com.bidhub.auction.service;

import com.bidhub.auction.entity.Auction;
import com.bidhub.auction.repository.AuctionRepository;
import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.repository.BidRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Closes all active auctions whose end time has passed.
     * Called periodically by the auction scheduler.
     */
    @Transactional
    public int closeExpiredAuctions() {

        LocalDateTime now = LocalDateTime.now();

        List<Auction> expiredAuctions =
                auctionRepository
                        .findByStatusAndEndTimeLessThanEqual(
                                "ACTIVE", now);

        int closedCount = 0;

        for (Auction expiredAuction : expiredAuctions) {

            closeAuction(expiredAuction.getId());

            closedCount++;
        }

        return closedCount;
    }

    /**
     * Locks and finalizes one auction.
     */
    @Transactional
    public boolean closeAuction(Long auctionId) {

        Auction auction = auctionRepository
                .findByIdForUpdate(auctionId)
                .orElse(null);

        if (auction == null) {
            return false;
        }

        // Recheck status after acquiring the database lock.
        if (!"ACTIVE".equalsIgnoreCase(auction.getStatus())) {
            return false;
        }

        if (auction.getEndTime() == null
                || auction.getEndTime().isAfter(LocalDateTime.now())) {
            return false;
        }

        // Highest bid appears first because bids are sorted descending.
        List<Bid> bids =
                bidRepository.findByAuction_IdOrderByAmountDesc(
                        auctionId);

        if (bids.isEmpty()) {
            // Auction ended without any accepted bids.
            auction.setWinner(null);
        } else {
            // The highest accepted bid wins.
            Bid winningBid = bids.get(0);
            auction.setWinner(winningBid.getBidder());
            auction.setCurrentPrice(winningBid.getAmount());
        }

        auction.setStatus("ENDED");

        auctionRepository.save(auction);

        return true;
    }
}
