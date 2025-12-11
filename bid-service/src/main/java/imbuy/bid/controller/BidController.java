package imbuy.bid.controller;

import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Bid management APIs")
public class BidController {

    private final BidService bidService;

    @GetMapping("/lots/{lotId}")
    @Operation(summary = "Get bid history for a lot")
    public Flux<BidDto> getBidsByLotId(
            @PathVariable Long lotId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return bidService.getBidsByLotId(lotId, pageable);
    }

    @PostMapping("/lots/{lotId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a bid on a lot", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<BidDto> placeBid(
            @PathVariable Long lotId,
            @Valid @RequestBody CreateBidDto createBidDto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        return bidService.placeBid(lotId, createBidDto, userId);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "JWT token is required. Please provide Authorization header with Bearer token"
            );
        }
        return (Long) authentication.getDetails();
    }

    @GetMapping("/lots/{lotId}/winning")
    @Operation(summary = "Get winning bid for a lot")
    public Long getAuctionWinner(@PathVariable Long lotId) {
        try {
            return bidService.getAuctionWinnerId(lotId)
                    .doOnSubscribe(s -> System.out.println("MONO SUBSCRIBED"))
                    .doOnNext(r -> System.out.println("MONO RESULT: " + r))
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            return null;
        }
    }
}