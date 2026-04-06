package com.maintainance.service_center.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    long countByUser_Id(Integer userId);

    boolean existsByUser_IdAndCenter_Id(Integer userId, Long centerId);

    void deleteByUser_IdAndCenter_Id(Integer userId, Long centerId);
}
