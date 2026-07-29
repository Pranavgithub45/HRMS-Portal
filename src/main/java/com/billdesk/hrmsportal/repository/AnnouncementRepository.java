package com.billdesk.hrmsportal.repository;

import com.billdesk.hrmsportal.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Active announcements only (expiry_date >= today)
    List<Announcement> findByExpiryDateGreaterThanEqualOrderByCreatedDateDesc(LocalDate today);
}