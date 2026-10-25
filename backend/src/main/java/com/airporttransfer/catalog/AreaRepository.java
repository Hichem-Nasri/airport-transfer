package com.airporttransfer.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findAllByOrderByNameFrAscIdAsc();

    List<Area> findAllByActiveTrueOrderByNameFrAscIdAsc();
}
