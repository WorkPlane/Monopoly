package com.example.monopoly.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private String name;
    private int balance;
    private int position;
    private boolean inJail;
    private int jailTurns;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Property> properties = new ArrayList<>();

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isInJail() { return inJail; }
    public void setInJail(boolean inJail) { this.inJail = inJail; }
    public int getJailTurns() { return jailTurns; }
    public void setJailTurns(int jailTurns) { this.jailTurns = jailTurns; }
    public List<Property> getProperties() { return properties; }
    public void setProperties(List<Property> properties) { this.properties = properties; }
}