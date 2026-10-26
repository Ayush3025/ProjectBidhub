
package com.bidhub.bid.service;

import com.bidhub.auction.entity.Auction;
import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.repository.BidRepository;
import com.bidhub.auction.repository.AuctionRepository;
import com.bidhub.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BidConcurrencyIntegrationTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    @Test
    void onlyOneCompetingBidShouldSucceed() throws Exception {

        String uniqueId = UUID.randomUUID().toString();

        Long sellerId = null;
        Long bidderAId = null;
        Long bidderBId = null;
        Long auctionId = null;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Arrange: create test users using JDBC.
            // Production User.java remains unchanged.
            sellerId = createUser("Test Seller", "seller-" + uniqueId);
            bidderAId = createUser("Bidder A", "bidder-a-" + uniqueId);
            bidderBId = createUser("Bidder B", "bidder-b-" + uniqueId);

            // Create the auction through JDBC because the database has
            // a required starting_price column not mapped by Auction.java.
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
            LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);

            jdbcTemplate.update("""
                    INSERT INTO auctions
                        (title, description, starting_price, base_price,
                         current_price, status, start_time, end_time,
                         seller_id, image_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "Concurrency Test Auction",
                    "Temporary test auction",
                    20000.00,
                    20000.00,
                    20000.00,
                    "ACTIVE",
                    startTime,
                    endTime,
                    sellerId,
                    ""
            );

            auctionId = jdbcTemplate.queryForObject(
                    """
                    SELECT id
                    FROM auctions
                    WHERE title = ? AND seller_id = ?
                    ORDER BY id DESC
                    LIMIT 1
                    """,
                    Long.class,
                    "Concurrency Test Auction",
                    sellerId
            );

            assertNotNull(auctionId, "Auction ID should be generated");

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            AtomicInteger successfulBids = new AtomicInteger();
            AtomicInteger rejectedBids = new AtomicInteger();

            Long finalAuctionId = auctionId;
            Long finalBidderAId = bidderAId;
            Long finalBidderBId = bidderBId;

            // Submit both requests to separate threads.
            Future<?> requestA = executor.submit(() ->
                    submitBid(
                            finalAuctionId,
                            finalBidderAId,
                            21000.0,
                            ready,
                            start,
                            successfulBids,
                            rejectedBids
                    )
            );

            Future<?> requestB = executor.submit(() ->
                    submitBid(
                            finalAuctionId,
                            finalBidderBId,
                            21000.0,
                            ready,
                            start,
                            successfulBids,
                            rejectedBids
                    )
            );

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both requests should be ready"
            );

            // Release both requests at nearly the same time.
            start.countDown();

            requestA.get(15, TimeUnit.SECONDS);
            requestB.get(15, TimeUnit.SECONDS);

            // Exactly one bid should be accepted.
            assertEquals(
                    1,
                    successfulBids.get(),
                    "Exactly one competing bid should succeed"
            );

            assertEquals(
                    1,
                    rejectedBids.get(),
                    "The other bid should be rejected"
            );

            // Verify the persisted auction price.
            Auction updatedAuction = auctionRepository
                    .findById(auctionId)
                    .orElseThrow();

            assertEquals(
                    21000.0,
                    updatedAuction.getCurrentPrice(),
                    "Current price should reflect the accepted bid"
            );

            // Verify only one bid was persisted.
            List<Bid> savedBids =
                    bidRepository.findByAuction_IdOrderByAmountDesc(auctionId);

            assertEquals(
                    1,
                    savedBids.size(),
                    "Only one bid should be stored"
            );

            assertEquals(
                    21000.0,
                    savedBids.get(0).getAmount(),
                    "The accepted bid should be 21000"
            );

        } finally {
            // Release waiting threads and wait for them to finish
            // before deleting records used by the test.
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Remove only the records created by this test.
            if (auctionId != null) {
                bidRepository.deleteAll(
                        bidRepository.findByAuction_IdOrderByAmountDesc(auctionId)
                );

                auctionRepository.deleteById(auctionId);
            }

            if (bidderAId != null) {
                userRepository.deleteById(bidderAId);
            }

            if (bidderBId != null) {
                userRepository.deleteById(bidderBId);
            }

            if (sellerId != null) {
                userRepository.deleteById(sellerId);
            }
        }
    }

    private void submitBid(
            Long auctionId,
            Long bidderId,
            Double amount,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successfulBids,
            AtomicInteger rejectedBids) {

        ready.countDown();

        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for simultaneous start"
                );
            }

            bidService.placeBid(auctionId, bidderId, amount);
            successfulBids.incrementAndGet();

        } catch (IllegalArgumentException ex) {
            // Expected when the other identical bid has already
            // increased the current auction price.
            rejectedBids.incrementAndGet();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private Long createUser(String name, String uniqueSuffix) {

        String username = uniqueSuffix;
        String email = uniqueSuffix + "@test.com";

        jdbcTemplate.update("""
                INSERT INTO users
                    (username, email, password_hash, display_name,
                     account_status, password, name, phone, role)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                username,
                email,
                "test-hash",
                name,
                "ACTIVE",
                "test-password",
                name,
                "9999999999",
                "USER"
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username
        );
    }
}
