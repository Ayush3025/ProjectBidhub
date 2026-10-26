
package com.bidhub.bid.service;

import com.bidhub.auction.entity.Auction;
import com.bidhub.auction.repository.AuctionRepository;
import com.bidhub.bid.entity.Bid;
import com.bidhub.bid.repository.BidRepository;
import com.bidhub.user.entity.User;
import com.bidhub.user.repository.UserRepository;
import com.bidhub.bid.event.BidPlacedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BidService bidService;

    private Auction auction;
    private User bidder;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(1L);
        auction.setStatus("ACTIVE");
        auction.setCurrentPrice(1000.0);
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusHours(1));

        bidder = new User();
        bidder.setId(2L);
    }

    @Test
    void shouldRejectNullBidAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 2L, null)
        );

        verifyNoInteractions(
                auctionRepository,
                bidRepository,
                userRepository,
                eventPublisher
        );
    }

    @Test
    void shouldRejectBidWhenAuctionHasEnded() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));

        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(bidder));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 2L, 1200.0)
        );

        assertEquals("Auction has already ended.", exception.getMessage());

        verify(bidRepository, never()).save(any(Bid.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldRejectBidBelowMinimumIncrement() {
        when(auctionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(auction));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(bidder));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 2L, 1050.0)
        );

        assertTrue(exception.getMessage().contains("Your bid must be at least"));

        verify(bidRepository, never()).save(any(Bid.class));
        verifyNoInteractions(eventPublisher);
    }

@Test
void shouldAcceptValidBidAndPublishEvent() {
    when(auctionRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(auction));

    when(userRepository.findById(2L))
            .thenReturn(Optional.of(bidder));

    when(bidRepository.save(any(Bid.class)))
            .thenAnswer(invocation -> {
                Bid savedBid = invocation.getArgument(0);
                savedBid.setId(10L);
                return savedBid;
            });

    Bid result = bidService.placeBid(1L, 2L, 1200.0);

    assertNotNull(result);
    assertEquals(1200.0, result.getAmount());
    assertEquals(1200.0, auction.getCurrentPrice());

    verify(auctionRepository).save(auction);
    verify(bidRepository).save(any(Bid.class));
    verify(eventPublisher).publishEvent(any(BidPlacedEvent.class));
}
@Test
void shouldRejectSellerBiddingOnOwnAuction() {
    User seller = new User();
    seller.setId(1L);
    auction.setSeller(seller);

    when(auctionRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(auction));
    when(userRepository.findById(1L))
            .thenReturn(Optional.of(seller));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> bidService.placeBid(1L, 1L, 1200.0)
    );

    assertEquals(
            "You cannot bid on your own auction.",
            exception.getMessage()
    );

    verify(bidRepository, never()).save(any(Bid.class));
    verifyNoInteractions(eventPublisher);
}
}
