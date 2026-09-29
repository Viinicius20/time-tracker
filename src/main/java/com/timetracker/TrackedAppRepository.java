package com.timetracker;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedAppRepository extends JpaRepository<TrackedApp, Long> {
    Optional<TrackedApp> findFirstByExeIgnoreCase(String exe);
}
