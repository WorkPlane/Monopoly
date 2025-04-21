package com.example.monopoly.model;

import jakarta.persistence.*;

@Entity
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private int price;
    private int rent;
    private boolean isMortgaged;
    private int houses;
    private boolean hasHotel;
    
    @ManyToOne
    private Player owner;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getRent() { return rent; }
    public void setRent(int rent) { this.rent = rent; }
    public boolean isMortgaged() { return isMortgaged; }
    public void setMortgaged(boolean mortgaged) { isMortgaged = mortgaged; }
    public int getHouses() { return houses; }
    public void setHouses(int houses) { this.houses = houses; }
    public boolean isHasHotel() { return hasHotel; }
    public void setHasHotel(boolean hasHotel) { this.hasHotel = hasHotel; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }
}