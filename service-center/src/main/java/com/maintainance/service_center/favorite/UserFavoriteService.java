package com.maintainance.service_center.favorite;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final MaintenanceCenterRepository centerRepository;

    public long countByUser(User user) {
        return favoriteRepository.countByUserId(user.getId());
    }

    public List<UserFavoriteResponse> findByUser(User user) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserFavoriteResponse findById(Long id, User caller) {
        UserFavorite favorite = getFavorite(id);
        checkAccess(favorite, caller);
        return toResponse(favorite);
    }

    @Transactional
    public UserFavoriteResponse create(UserFavoriteRequest request, User user) {
        if (favoriteRepository.existsByUserIdAndCenterId(user.getId(), request.getCenterId())) {
            throw new IllegalArgumentException("Center already in favorites");
        }

        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));

        UserFavorite favorite = UserFavorite.builder()
                .user(user)
                .center(center)
                .notes(request.getNotes())
                .build();

        favoriteRepository.save(favorite);
        log.info("Created favorite id={} for user id={} and center id={}",
                favorite.getId(), user.getId(), request.getCenterId());
        return toResponse(favorite);
    }

    @Transactional
    public UserFavoriteResponse update(Long id, UserFavoriteRequest request, User caller) {
        UserFavorite favorite = getFavorite(id);
        checkAccess(favorite, caller);

        if (!favorite.getCenter().getId().equals(request.getCenterId()) &&
            favoriteRepository.existsByUserIdAndCenterId(caller.getId(), request.getCenterId())) {
            throw new IllegalArgumentException("Center already in favorites");
        }

        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));

        favorite.setCenter(center);
        favorite.setNotes(request.getNotes());

        favoriteRepository.save(favorite);
        log.info("Updated favorite id={}", id);
        return toResponse(favorite);
    }

    @Transactional
    public void delete(Long id, User caller) {
        UserFavorite favorite = getFavorite(id);
        checkAccess(favorite, caller);

        favoriteRepository.delete(favorite);
        log.info("Deleted favorite id={}", id);
    }

    private UserFavorite getFavorite(Long id) {
        return favoriteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Favorite not found with id: " + id));
    }

    private void checkAccess(UserFavorite favorite, User caller) {
        if (!favorite.getUser().getId().equals(caller.getId())) {
            throw new IllegalArgumentException("You do not have permission to access this favorite");
        }
    }

    private UserFavoriteResponse toResponse(UserFavorite favorite) {
        MaintenanceCenter center = favorite.getCenter();
        return UserFavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .userName(favorite.getUser().fullName())
                .centerId(center.getId())
                .centerName(center.getNameEn())
                .centerPhone(center.getPhone())
                .centerAddress(center.getAddress())
                .centerLatitude(center.getLatitude())
                .centerLongitude(center.getLongitude())
                .notes(favorite.getNotes())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
