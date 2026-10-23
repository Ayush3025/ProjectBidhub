
package com.bidhub.bid.controller;

import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.service.BidService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {

        try {
            Bid bid = bidService.placeBid(
                    request.getAuctionId(),
                    request.getBidderId(),
                    request.getAmount()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Bid placed successfully",
                            "bidId", bid.getId(),
                            "auctionId", bid.getAuction().getId(),
                            "bidderId", bid.getBidder().getId(),
                            "amount", bid.getAmount(),
                            "bidTime", bid.getBidTime() == null
                                    ? "Saved successfully"
                                    : bid.getBidTime().toString()
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", ex.getMessage())
            );
        }
    }

    public static class BidRequest {

        private Long auctionId;
        private Long bidderId;
        private Double amount;

        public BidRequest() {
        }

        public Long getAuctionId() {
            return auctionId;
        }

        public void setAuctionId(Long auctionId) {
            this.auctionId = auctionId;
        }

        public Long getBidderId() {
            return bidderId;
        }

        public void setBidderId(Long bidderId) {
            this.bidderId = bidderId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }
    
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<?> getBidHistory(
            @PathVariable Long auctionId) {

        try {
            List<Bid> bids = bidService.getBidHistory(auctionId);
            return ResponseEntity.ok(bids);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
