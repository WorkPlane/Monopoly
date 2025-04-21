package com.example.monopoly.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private int currentPlayerTurn;
    private boolean gameStarted;
    private boolean gameFinished;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public int getCurrentPlayerTurn() { return currentPlayerTurn; }
    public void setCurrentPlayerTurn(int currentPlayerTurn) { this.currentPlayerTurn = currentPlayerTurn; }
    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
    public boolean isGameFinished() { return gameFinished; }
    public void setGameFinished(boolean gameFinished) { this.gameFinished = gameFinished; }
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
}