
package com.bidhub.bid.repository;

import com.bidhub.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuction_IdOrderByAmountDesc(Long auctionId);

    List<Bid> findByBidder_IdOrderByBidTimeDesc(Long bidderId);

}
