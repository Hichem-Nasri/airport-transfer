package com.airporttransfer.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findAllByOrderByDisplayOrderAscIdAsc();

    List<Zone> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<Zone> findByIdAndActiveTrue(Long id);
}
