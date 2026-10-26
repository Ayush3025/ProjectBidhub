
package com.bidhub.auction.service;

import com.bidhub.auction.entity.Auction;
import com.bidhub.auction.repository.AuctionRepository;
import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.repository.BidRepository;
import com.bidhub.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(1L);
        auction.setStatus("ACTIVE");
        auction.setEndTime(LocalDateTime.now().minusMinutes(5));
        auction.setCurrentPrice(1000.0);
    }

    @Test
    void shouldCloseExpiredAuctionAndSelectHighestBidder() {
        User winner = new User();
        winner.setId(2L);

        Bid highestBid = new Bid();
        highestBid.setId(10L);
        highestBid.setBidder(winner);
        highestBid.setAmount(1500.0);

        Bid lowerBid = new Bid();
        lowerBid.setId(11L);
        lowerBid.setAmount(1200.0);

        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));

        when(bidRepository.findByAuction_IdOrderByAmountDesc(1L))
                .thenReturn(List.of(highestBid, lowerBid));

        boolean closed = auctionService.closeAuction(1L);

        assertTrue(closed);
        assertEquals("ENDED", auction.getStatus());
        assertEquals(winner, auction.getWinner());
        assertEquals(1500.0, auction.getCurrentPrice());

        verify(auctionRepository).save(auction);
    }

    @Test
    void shouldCloseExpiredAuctionWithoutBids() {
        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));

        when(bidRepository.findByAuction_IdOrderByAmountDesc(1L))
                .thenReturn(List.of());

        boolean closed = auctionService.closeAuction(1L);

        assertTrue(closed);
        assertEquals("ENDED", auction.getStatus());
        assertNull(auction.getWinner());

        // No bids means the starting/current price remains unchanged.
        assertEquals(1000.0, auction.getCurrentPrice());

        verify(auctionRepository).save(auction);
    }

    @Test
    void shouldNotCloseAuctionThatHasNotExpired() {
        auction.setEndTime(LocalDateTime.now().plusHours(1));

        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));

        boolean closed = auctionService.closeAuction(1L);

        assertFalse(closed);
        assertEquals("ACTIVE", auction.getStatus());

        verify(auctionRepository, never()).save(any(Auction.class));
        verifyNoInteractions(bidRepository);
    }

    @Test
    void shouldNotCloseAuctionThatIsAlreadyEnded() {
        auction.setStatus("ENDED");

        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));

        boolean closed = auctionService.closeAuction(1L);

        assertFalse(closed);
        verify(auctionRepository, never()).save(any(Auction.class));
        verifyNoInteractions(bidRepository);
    }

    @Test
    void shouldReturnFalseWhenAuctionDoesNotExist() {
        when(auctionRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        boolean closed = auctionService.closeAuction(99L);

        assertFalse(closed);
        verify(auctionRepository, never()).save(any(Auction.class));
        verifyNoInteractions(bidRepository);
    }

@Test
void shouldCloseAllExpiredAuctionsAndReturnCount() {
    Auction secondAuction = new Auction();
    secondAuction.setId(2L);
    secondAuction.setStatus("ACTIVE");
    secondAuction.setEndTime(LocalDateTime.now().minusMinutes(10));
    secondAuction.setCurrentPrice(2000.0);

    when(auctionRepository.findByStatusAndEndTimeLessThanEqual(
            eq("ACTIVE"), any(LocalDateTime.class)))
            .thenReturn(List.of(auction, secondAuction));

    when(auctionRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(auction));
    when(auctionRepository.findByIdForUpdate(2L))
            .thenReturn(Optional.of(secondAuction));

    when(bidRepository.findByAuction_IdOrderByAmountDesc(anyLong()))
            .thenReturn(List.of());

    int closedCount = auctionService.closeExpiredAuctions();

    assertEquals(2, closedCount);
    assertEquals("ENDED", auction.getStatus());
    assertEquals("ENDED", secondAuction.getStatus());

    verify(auctionRepository).save(auction);
    verify(auctionRepository).save(secondAuction);
}

@Test
void shouldReturnZeroWhenNoAuctionsHaveExpired() {
    when(auctionRepository.findByStatusAndEndTimeLessThanEqual(
            eq("ACTIVE"), any(LocalDateTime.class)))
            .thenReturn(List.of());

    int closedCount = auctionService.closeExpiredAuctions();

    assertEquals(0, closedCount);

    verify(auctionRepository, never()).findByIdForUpdate(anyLong());
    verify(auctionRepository, never()).save(any(Auction.class));
}

}
