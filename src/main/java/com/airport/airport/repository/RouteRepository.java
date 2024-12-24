package com.airport.airport.repository;

import com.airport.airport.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
   // @Query("SELECT r FROM Route r WHERE r.route_id = :route_id")
    Route findByRouteId(String route_id);
    Boolean existsByRouteId(String route_id);
}
