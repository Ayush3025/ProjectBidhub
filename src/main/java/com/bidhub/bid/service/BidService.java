
package com.bidhub.bid.service;

import com.bidhub.auction.entity.Auction;
import com.bidhub.auction.repository.AuctionRepository;
import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.repository.BidRepository;
import com.bidhub.user.entity.User;
import com.bidhub.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    public BidService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Bid placeBid(Long auctionId, Long bidderId, Double amount) {

        // 1. Validate the submitted amount
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Bid amount must be a valid positive number."
            );
        }

        // 2. Lock the auction row for concurrent bidding safety
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Auction not found.")
                );

        // 3. Find the bidder
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Bidder not found.")
                );

        // 4. Validate auction dates
        LocalDateTime now = LocalDateTime.now();

        if (auction.getStartTime() == null
                || auction.getEndTime() == null) {
            throw new IllegalArgumentException(
                    "Auction start and end times must be configured."
            );
        }

        if (now.isBefore(auction.getStartTime())) {
            throw new IllegalArgumentException(
                    "Auction has not started yet."
            );
        }

        if (!now.isBefore(auction.getEndTime())) {
            throw new IllegalArgumentException(
                    "Auction has already ended."
            );
        }

        // 5. Only ACTIVE auctions accept bids
        if (!"ACTIVE".equalsIgnoreCase(auction.getStatus())) {
            throw new IllegalArgumentException(
                    "Auction is not active."
            );
        }

        // 6. Prevent the creator from bidding on their own auction
        if (auction.getSeller() != null
                && auction.getSeller().getId().equals(bidderId)) {
            throw new IllegalArgumentException(
                    "You cannot bid on your own auction."
            );
        }

        // 7. Validate against the latest locked price
        Double currentPrice = auction.getCurrentPrice();

        if (currentPrice == null) {
            throw new IllegalArgumentException(
                    "Auction current price is not configured."
            );
        }

        if (amount <= currentPrice) {
            throw new IllegalArgumentException(
                    "Your bid must be higher than the current price: "
                            + currentPrice
            );
        }

        // 8. Create the accepted bid
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);

        // 9. Update the auction price
        auction.setCurrentPrice(amount);

        // Both changes commit together or roll back together
        auctionRepository.save(auction);

        return bidRepository.save(bid);
    }
}
