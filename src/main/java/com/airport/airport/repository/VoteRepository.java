package com.airport.airport.repository;

import com.airport.airport.domain.Route;
import com.airport.airport.domain.User;
import com.airport.airport.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote,Long> {

    @Query("SELECT SUM(v.positive) FROM Vote v WHERE v.route.routeId =:routeId")
    Optional<Integer> findPositiveByRoute_id(@Param("routeId")String routeId);

    @Query("SELECT SUM(v.negative) FROM Vote v WHERE v.route.routeId =:routeId")
    Optional<Integer>  findNegativeByRoute_id(@Param("routeId")String routeId);

    Optional<Vote> findByUserAndRoute(User user, Route route);


}
