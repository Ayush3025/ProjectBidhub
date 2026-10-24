
package com.bidhub.auction.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import com.bidhub.auction.entity.Auction;
import com.bidhub.auction.repository.AuctionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionRepository auctionRepository;

    public AuctionController(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }
    
    @GetMapping
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuctionDetails(
            @PathVariable Long auctionId) {

        return auctionRepository.findById(auctionId)
                .map(auction -> {
                    Map<String, Object> response =
                            new LinkedHashMap<>();

                    response.put("id", auction.getId());
                    response.put("title", auction.getTitle());
                    response.put("description",
                            auction.getDescription());
                    response.put("basePrice",
                            auction.getBasePrice());
                    response.put("currentPrice",
                            auction.getCurrentPrice());
                    response.put("status", auction.getStatus());
                    response.put("startTime",
                            auction.getStartTime());
                    response.put("endTime",
                            auction.getEndTime());
                    response.put("imageUrl",
                            auction.getImageUrl());

                    if (auction.getSeller() != null) {
                        response.put("sellerId",
                                auction.getSeller().getId());
                        response.put("sellerName",
                                auction.getSeller().getName());
                    }

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
