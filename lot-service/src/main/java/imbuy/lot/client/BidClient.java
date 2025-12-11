package imbuy.lot.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@FeignClient(name = "bid-service")
public interface BidClient {

    @CircuitBreaker(name = "bidServiceClient", fallbackMethod = "getAuctionWinnerIdFallback")
    @GetMapping("/bids/lots/{lotId}/winning")
    Long getAuctionWinner(@PathVariable("lotId") Long lotId);

    default Long getAuctionWinnerIdFallback(Long id, Exception e) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Bid service is temporarily unavailable"
        );
    }
}