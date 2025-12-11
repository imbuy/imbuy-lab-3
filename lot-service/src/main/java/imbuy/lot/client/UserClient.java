package imbuy.lot.client;

import imbuy.lot.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @CircuitBreaker(name = "userServiceClient", fallbackMethod = "getUserByIdFallback")
    @GetMapping("/users/id/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    default UserDto getUserByIdFallback(Long id, Exception e) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service is temporarily unavailable"
        );
    }
}