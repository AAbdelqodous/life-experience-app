package com.maintainance.service_center.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserIdAndCenterId(Integer userId, Long centerId);

    void deleteByUserIdAndCenterId(Integer userId, Long centerId);
}
