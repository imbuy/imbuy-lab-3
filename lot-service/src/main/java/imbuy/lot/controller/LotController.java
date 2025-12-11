package imbuy.lot.controller;

import imbuy.lot.dto.*;
import imbuy.lot.enums.LotStatus;
import imbuy.lot.service.LotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lots")
@RequiredArgsConstructor
@Tag(name = "Lots", description = "Lot management APIs")
public class LotController {

    private final LotService lotService;

    @GetMapping
    @Operation(summary = "Get all lots with pagination and filtering")
    public ResponseEntity<List<LotDto>> getLots(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LotStatus lotStatus = null;
        if (status != null) {
            try {
                lotStatus = LotStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        LotFilterDto filter = new LotFilterDto(
                title,
                lotStatus,
                categoryId,
                ownerId,
                activeOnly != null ? activeOnly : false
        );

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        List<LotDto> lots = lotService.getLots(filter, pageable);
        return ResponseEntity.ok(lots);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lot by ID")
    public ResponseEntity<LotDto> getLotById(@PathVariable Long id) {
        LotDto lot = lotService.getLotById(id);
        return ResponseEntity.ok(lot);
    }

    @PostMapping
    @Operation(summary = "Create a new lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LotDto> createLot(@Valid @RequestBody CreateLotDto createLotDto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        LotDto lot = lotService.createLot(createLotDto, userId);
        return new ResponseEntity<>(lot, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LotDto> approveLot(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        LotDto approvedLot = lotService.approveLot(id, userId);
        return ResponseEntity.ok(approvedLot);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LotDto> cancelLot(@PathVariable Long id,
                                            @RequestParam(required = false) String reason,
                                            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        LotDto cancelledLot = lotService.cancelLot(id, userId, reason);
        return ResponseEntity.ok(cancelledLot);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LotDto> updateLot(@PathVariable Long id,
                                            @Valid @RequestBody UpdateLotDto updateLotDto,
                                            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        LotDto lot = lotService.updateLot(id, updateLotDto, userId);
        return ResponseEntity.ok(lot);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete lot", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteLot(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        lotService.deleteLot(id, userId);
        return ResponseEntity.noContent().build();
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
}