package imbuy.bid.controller;

import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "Place a bid on a lot", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
    public Mono<BidDto> placeBid(
            @PathVariable Long lotId,
            @RequestParam Long currentUserId,
            @Valid @RequestBody CreateBidDto createBidDto) {

        return bidService.placeBid(lotId, createBidDto, currentUserId);
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