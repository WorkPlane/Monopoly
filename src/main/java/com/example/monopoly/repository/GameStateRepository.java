package com.example.monopoly.repository;

import com.example.monopoly.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameStateRepository extends JpaRepository<GameState, Long> {
    Optional<GameState> findBySessionId(String sessionId);
}