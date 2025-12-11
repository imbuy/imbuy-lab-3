package imbuy.lot.service;

import imbuy.lot.client.UserClient;
import imbuy.lot.domain.Lot;
import imbuy.lot.dto.*;
import imbuy.lot.enums.LotStatus;
import imbuy.lot.repository.LotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotService {

    private final UserClient userClient;
    private final LotRepository lotRepository;

    public List<LotDto> getLots(LotFilterDto filter, Pageable pageable) {
        Page<Lot> lots = findLotsByFilter(filter, pageable);
        return lots.map(this::mapToDtoWithUserInfo).getContent();
    }

    public LotDto getLotById(Long id) {
        Lot lot = findLotById(id);
        return mapToDtoWithUserInfo(lot);
    }

    @Transactional
    public LotDto createLot(CreateLotDto createLotDto, Long ownerId) {
        validateCreateLotRequest(createLotDto);

        validateUserExists(ownerId);

        Lot lot = buildLotFromRequest(createLotDto, ownerId);
        Lot savedLot = lotRepository.save(lot);

        return mapToDtoWithUserInfo(savedLot);
    }

    private void validateUserExists(Long userId) {
        UserDto user = userClient.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    @Transactional
    public LotDto approveLot(Long lotId, Long currentUserId) {
        Lot lot = findLotById(lotId);
        validateApprovalPermissions(lot, currentUserId);
        validateLotStatus(lot, LotStatus.PENDING_APPROVAL, "Lot is not awaiting approval");

        Lot approvedLot = updateLotStatus(lot, LotStatus.ACTIVE);
        return mapToDtoWithUserInfo(approvedLot);
    }

    @Transactional
    public LotDto cancelLot(Long lotId, Long currentUserId, String reason) {
        Lot lot = findLotById(lotId);
        validateCancelPermissions(lot, currentUserId);
        validateLotStatus(lot, LotStatus.PENDING_APPROVAL, "Lot cannot be rejected");

        Lot cancelledLot = updateLotStatus(lot, LotStatus.CANCELLED);
        log.info("Lot {} cancelled by user {}. Reason: {}", lotId, currentUserId, reason);
        return mapToDtoWithUserInfo(cancelledLot);
    }

    @Transactional
    public LotDto updateLot(Long id, UpdateLotDto updateLotDto, Long currentUserId) {
        Lot lot = findLotById(id);
        validateOwnership(lot, currentUserId);
        validateUpdatableStatus(lot);

        Lot updatedLot = updateLotFromRequest(lot, updateLotDto);
        return mapToDtoWithUserInfo(updatedLot);
    }

    @Transactional
    public void deleteLot(Long id, Long currentUserId) {
        Lot lot = findLotById(id);
        validateOwnership(lot, currentUserId);
        validateDeletableStatus(lot);

        lotRepository.delete(lot);
        log.info("Lot {} deleted by user {}", id, currentUserId);
    }

    private LotDto mapToDtoWithUserInfo(Lot lot) {
        UserDto owner = userClient.getUserById(lot.getOwnerId());
        String ownerName = owner != null ? owner.username() : "Unknown";

        String winnerName = null;
        if (lot.getWinnerId() != null) {
            UserDto winner = userClient.getUserById(lot.getWinnerId());
            winnerName = winner != null ? winner.username() : "Unknown";
        }

        return createLotDto(lot, ownerName, winnerName);
    }

    private LotDto createLotDto(Lot lot, String ownerName, String winnerName) {
        return new LotDto(
                lot.getId(),
                lot.getTitle(),
                lot.getDescription(),
                lot.getStartPrice(),
                lot.getCurrentPrice(),
                lot.getBidStep(),
                lot.getOwnerId(),
                ownerName,
                lot.getCategoryId(),
                lot.getCategoryId() != null ? "Category " + lot.getCategoryId() : null,
                lot.getStatus(),
                lot.getStartDate(),
                lot.getEndDate(),
                lot.getWinnerId(),
                winnerName
        );
    }

    private Lot findLotById(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot not found"));
    }

    private Page<Lot> findLotsByFilter(LotFilterDto filter, Pageable pageable) {
        if (filter != null && hasFilters(filter)) {
            return lotRepository.findByFilters(
                    filter.title(),
                    filter.status(),
                    filter.category_id(),
                    filter.owner_id(),
                    pageable
            );
        } else if (filter != null && Boolean.TRUE.equals(filter.active_only())) {
            return lotRepository.findByStatus(LotStatus.ACTIVE, pageable);
        } else {
            return lotRepository.findAll(pageable);
        }
    }

    private Lot buildLotFromRequest(CreateLotDto createLotDto, Long ownerId) {
        return Lot.builder()
                .title(createLotDto.title())
                .description(createLotDto.description())
                .startPrice(createLotDto.start_price())
                .currentPrice(createLotDto.start_price())
                .bidStep(createLotDto.bid_step())
                .ownerId(ownerId)
                .categoryId(createLotDto.category_id())
                .status(LotStatus.PENDING_APPROVAL)
                .startDate(createLotDto.start_date() != null ? createLotDto.start_date() : LocalDateTime.now())
                .endDate(createLotDto.end_date())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Lot updateLotFromRequest(Lot lot, UpdateLotDto updateLotDto) {
        Lot.LotBuilder lotBuilder = lot.toBuilder();
        if (updateLotDto.title() != null) lotBuilder.title(updateLotDto.title());
        if (updateLotDto.description() != null) lotBuilder.description(updateLotDto.description());
        if (updateLotDto.bid_step() != null) lotBuilder.bidStep(updateLotDto.bid_step());
        if (updateLotDto.end_date() != null) lotBuilder.endDate(updateLotDto.end_date());
        if (updateLotDto.category_id() != null) lotBuilder.categoryId(updateLotDto.category_id());

        return lotRepository.save(lotBuilder.build());
    }

    private Lot updateLotStatus(Lot lot, LotStatus newStatus) {
        Lot updatedLot = lot.toBuilder()
                .status(newStatus)
                .build();
        return lotRepository.save(updatedLot);
    }

    private void validateCreateLotRequest(CreateLotDto createLotDto) {
        if (createLotDto.end_date() != null && createLotDto.end_date().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be in the past");
        }
    }

    private void validateOwnership(Lot lot, Long userId) {
        if (!lot.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own lots");
        }
    }

    private void validateLotStatus(Lot lot, LotStatus expectedStatus, String errorMessage) {
        if (lot.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private void validateUpdatableStatus(Lot lot) {
        if (lot.getStatus() != LotStatus.DRAFT && lot.getStatus() != LotStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update lot in current status");
        }
    }

    private void validateDeletableStatus(Lot lot) {
        if (lot.getStatus() == LotStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete active lot");
        }
    }

    private void validateApprovalPermissions(Lot lot, Long currentUserId) {
        if (lot.getOwnerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner cannot approve their own lot");
        }
        try {
            UserDto approver = userClient.getUserById(currentUserId);
            if (approver == null || approver.role() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot validate approver permissions");
            }
            String role = approver.role();
            if (!"SUPERVISOR".equalsIgnoreCase(role) && !"MODERATOR".equalsIgnoreCase(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only supervisors or moderators can approve lots");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot validate approver permissions");
        }
    }

    private void validateCancelPermissions(Lot lot, Long currentUserId) {
        try {
            UserDto user = userClient.getUserById(currentUserId);
            if (user == null || user.role() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot validate user permissions");
            }
            String role = user.role();

            if ("MODERATOR".equalsIgnoreCase(role)) {
                return;
            }

            if (lot.getOwnerId().equals(currentUserId)) {
                return;
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own lots or be a moderator");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot validate user permissions");
        }
    }

    private boolean hasFilters(LotFilterDto filter) {
        return filter.title() != null || filter.status() != null ||
                filter.category_id() != null || filter.owner_id() != null;
    }
}