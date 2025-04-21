package com.example.monopoly.service;

import com.example.monopoly.model.GameState;
import com.example.monopoly.model.Player;
import com.example.monopoly.model.Property;
import com.example.monopoly.repository.GameStateRepository;
import com.example.monopoly.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class GameService {
    private final GameStateRepository gameStateRepository;
    private final PlayerRepository playerRepository;
    
    @Autowired
    public GameService(GameStateRepository gameStateRepository, PlayerRepository playerRepository) {
        this.gameStateRepository = gameStateRepository;
        this.playerRepository = playerRepository;
    }

    private static final int START_BALANCE = 1500;
    private static final int JAIL_POSITION = 10;
    private static final int GO_POSITION = 0;
    private static final int GO_SALARY = 200;
    private static final int COMPUTER_BUY_PROBABILITY = 70; // 70% chance to buy property
    
    private final List<Property> properties = initializeProperties();
    
    public String startNewGame(String sessionId, String playerName) {
        Optional<GameState> existingGame = gameStateRepository.findBySessionId(sessionId);
        if (existingGame.isPresent()) {
            return "У вас уже есть активная игра. Хотите продолжить?";
        }
        
        GameState gameState = new GameState();
        gameState.setSessionId(sessionId);
        gameState.setGameStarted(true);
        gameState.setGameFinished(false);
        gameState.setCurrentPlayerTurn(0); // Игрок ходит первым
        
        Player player = createPlayer(sessionId, playerName);
        Player computer = createComputerPlayer(sessionId);
        
        gameState.getPlayers().add(player);
        gameState.getPlayers().add(computer);
        
        gameStateRepository.save(gameState);
        playerRepository.save(player);
        playerRepository.save(computer);
        
        return String.format("Игра началась! %s против Компьютера. Ваш баланс: %d. Скажите 'Бросить кубики', чтобы сделать ход.", 
                playerName, START_BALANCE);
    }
    
    private Player createPlayer(String sessionId, String name) {
        Player player = new Player();
        player.setSessionId(sessionId);
        player.setName(name);
        player.setBalance(START_BALANCE);
        player.setPosition(0);
        player.setInJail(false);
        player.setJailTurns(0);
        player.setProperties(new ArrayList<>());
        return player;
    }
    
    private Player createComputerPlayer(String sessionId) {
        Player computer = new Player();
        computer.setSessionId(sessionId + "_computer");
        computer.setName("Компьютер");
        computer.setBalance(START_BALANCE);
        computer.setPosition(0);
        computer.setInJail(false);
        computer.setJailTurns(0);
        computer.setProperties(new ArrayList<>());
        return computer;
    }
    
    public String rollDice(String sessionId) {
        Optional<GameState> gameStateOpt = gameStateRepository.findBySessionId(sessionId);
        if (gameStateOpt.isEmpty()) {
            return "Игра не найдена. Скажите 'Начать игру', чтобы начать новую игру.";
        }
        
        GameState gameState = gameStateOpt.get();
        if (gameState.isGameFinished()) {
            return "Игра уже завершена. Скажите 'Начать игру', чтобы начать новую игру.";
        }
        
        if (gameState.getCurrentPlayerTurn() != 0) {
            return "Сейчас не ваш ход. Дождитесь хода компьютера.";
        }
        
        Player currentPlayer = gameState.getPlayers().get(0);
        
        if (currentPlayer.isInJail()) {
            return handleJailTurn(gameState, currentPlayer);
        }
        
        Random random = new Random();
        int dice1 = random.nextInt(6) + 1;
        int dice2 = random.nextInt(6) + 1;
        int total = dice1 + dice2;
        
        int newPosition = (currentPlayer.getPosition() + total) % properties.size();
        currentPlayer.setPosition(newPosition);
        
        String response = String.format("Вы выбросили %d и %d. Всего %d. Вы перемещаетесь на %s. ", 
                dice1, dice2, total, properties.get(newPosition).getName());
        
        if (currentPlayer.getPosition() + total >= properties.size()) {
            currentPlayer.setBalance(currentPlayer.getBalance() + GO_SALARY);
            response += String.format("Вы прошли через Старт и получаете %d. Ваш баланс: %d. ", 
                    GO_SALARY, currentPlayer.getBalance());
        }
        
        response += handlePropertyLanding(gameState, currentPlayer, newPosition);
        
        gameState.setCurrentPlayerTurn(1);
        gameStateRepository.save(gameState);
        playerRepository.save(currentPlayer);
        
        return response;
    }
    
    public String computerTurn(String sessionId) {
        Optional<GameState> gameStateOpt = gameStateRepository.findBySessionId(sessionId);
        if (gameStateOpt.isEmpty()) {
            return "Ошибка: игра не найдена";
        }

        GameState gameState = gameStateOpt.get();
        if (gameState.getCurrentPlayerTurn() != 1) {
            return "Сейчас не ход компьютера.";
        }
        
        Player computer = gameState.getPlayers().get(1);
        StringBuilder response = new StringBuilder();
        
        if (computer.isInJail()) {
            response.append(handleComputerJailTurn(gameState, computer));
        } else {
            response.append(handleComputerNormalTurn(gameState, computer));
        }
        
        response.append(handleComputerPropertyDecision(gameState, computer));
        
        gameState.setCurrentPlayerTurn(0);
        gameStateRepository.save(gameState);
        playerRepository.save(computer);
        
        return response.toString();
    }
    
    private String handleComputerNormalTurn(GameState gameState, Player computer) {
        Random random = new Random();
        int dice1 = random.nextInt(6) + 1;
        int dice2 = random.nextInt(6) + 1;
        int total = dice1 + dice2;
        
        int newPosition = (computer.getPosition() + total) % properties.size();
        computer.setPosition(newPosition);
        
        String response = String.format("Компьютер бросает кубики: %d и %d. Идет на %s. ", 
                dice1, dice2, properties.get(newPosition).getName());
        
        if (computer.getPosition() + total >= properties.size()) {
            computer.setBalance(computer.getBalance() + GO_SALARY);
            response += String.format("Компьютер получает %d за проход через Старт. ", GO_SALARY);
        }
        
        response += handlePropertyLanding(gameState, computer, newPosition);
        return response;
    }
    
    private String handleComputerJailTurn(GameState gameState, Player computer) {
        computer.setJailTurns(computer.getJailTurns() + 1);
        
        if (computer.getJailTurns() >= 3) {
            computer.setInJail(false);
            computer.setJailTurns(0);
            return "Компьютер выходит из тюрьмы после 3 ходов. ";
        }
        
        return String.format("Компьютер остается в тюрьме (ход %d/3). ", computer.getJailTurns());
    }
    
    private String handleComputerPropertyDecision(GameState gameState, Player computer) {
        Property currentProperty = properties.get(computer.getPosition());
        
        if (currentProperty.getOwner() == null && 
            computer.getBalance() >= currentProperty.getPrice() &&
            new Random().nextInt(100) < COMPUTER_BUY_PROBABILITY) {
            
            computer.setBalance(computer.getBalance() - currentProperty.getPrice());
            currentProperty.setOwner(computer);
            computer.getProperties().add(currentProperty);
            return String.format("Компьютер покупает %s за %d. ", 
                    currentProperty.getName(), currentProperty.getPrice());
        }
        
        return "";
    }
    
    private String handlePropertyLanding(GameState gameState, Player currentPlayer, int position) {
        Property property = properties.get(position);
        
        // Если это специальное поле (старт, тюрьма и т.д.)
        if (property.getPrice() == 0) {
            return handleSpecialProperty(currentPlayer, property);
        }
        
        // Если поле свободно
        if (property.getOwner() == null) {
            return String.format("Эта собственность свободна. Вы можете купить %s за %d. Скажите 'Купить' или 'Пропустить'.", 
                    property.getName(), property.getPrice());
        }
        
        // Если игрок владеет этим полем
        if (property.getOwner().equals(currentPlayer)) {
            return "Вы владеете этой собственностью. Ничего не происходит.";
        }
        
        // Если поле принадлежит другому игроку
        Player owner = property.getOwner();
        int rent = calculateRent(property);
        
        // Проверяем, может ли игрок заплатить аренду
        if (currentPlayer.getBalance() < rent) {
            // Игрок банкрот
            gameState.setGameFinished(true);
            playerRepository.save(currentPlayer);
            playerRepository.save(owner);
            gameStateRepository.save(gameState);
            return String.format("Вы не можете заплатить аренду %d за %s. Вы банкрот! Игра окончена.", 
                    rent, property.getName());
        }
        
        // Списываем аренду
        currentPlayer.setBalance(currentPlayer.getBalance() - rent);
        owner.setBalance(owner.getBalance() + rent);
        
        playerRepository.save(currentPlayer);
        playerRepository.save(owner);
        
        return String.format("Эта собственность принадлежит %s. Вы платите аренду %d. Ваш баланс: %d.", 
                owner.getName(), rent, currentPlayer.getBalance());
    }
    
    private String handleSpecialProperty(Player player, Property property) {
        switch (property.getName()) {
            case "Старт":
                return "Вы на старте. Ничего не происходит.";
            case "Тюрьма":
                if (!player.isInJail()) {
                    return "Вы просто посещаете тюрьму. Ничего не происходит.";
                }
                return "";
            default:
                return "Вы на специальном поле. Ничего не происходит.";
        }
    }
    
    private int calculateRent(Property property) {
        if (property.isMortgaged()) {
            return 0;
        }
        
        int baseRent = property.getRent();
        if (property.getHouses() > 0) {
            return baseRent * (property.getHouses() + 1);
        }
        if (property.isHasHotel()) {
            return baseRent * 5;
        }
        return baseRent;
    }
    
    private String handleJailTurn(GameState gameState, Player player) {
        player.setJailTurns(player.getJailTurns() + 1);
        
        if (player.getJailTurns() >= 3) {
            player.setInJail(false);
            player.setJailTurns(0);
            playerRepository.save(player);
            return "Вы провели 3 хода в тюрьме и теперь свободны. Скажите 'Бросить кубики', чтобы сделать ход.";
        }
        
        return String.format("Вы в тюрьме (ход %d/3). Скажите 'Выйти из тюрьмы', чтобы попытаться выйти, или 'Пропустить ход'.", 
                player.getJailTurns());
    }
    
    public String buyProperty(String sessionId) {
        Optional<GameState> gameStateOpt = gameStateRepository.findBySessionId(sessionId);
        if (gameStateOpt.isEmpty()) {
            return "Игра не найдена.";
        }
        
        GameState gameState = gameStateOpt.get();
        if (gameState.getCurrentPlayerTurn() != 0) {
            return "Сейчас не ваш ход.";
        }
        
        Player currentPlayer = gameState.getPlayers().get(0);
        Property property = properties.get(currentPlayer.getPosition());
        
        if (property.getOwner() != null) {
            return "Эта собственность уже куплена.";
        }
        
        if (currentPlayer.getBalance() < property.getPrice()) {
            return String.format("У вас недостаточно денег для покупки %s. Нужно %d, у вас %d.", 
                    property.getName(), property.getPrice(), currentPlayer.getBalance());
        }
        
        currentPlayer.setBalance(currentPlayer.getBalance() - property.getPrice());
        property.setOwner(currentPlayer);
        currentPlayer.getProperties().add(property);
        
        playerRepository.save(currentPlayer);
        
        return String.format("Вы купили %s за %d. Ваш баланс: %d.", 
                property.getName(), property.getPrice(), currentPlayer.getBalance());
    }
    
    public String endTurn(String sessionId) {
        Optional<GameState> gameStateOpt = gameStateRepository.findBySessionId(sessionId);
        if (gameStateOpt.isEmpty()) {
            return "Игра не найдена.";
        }
        
        GameState gameState = gameStateOpt.get();
        if (gameState.getCurrentPlayerTurn() != 0) {
            return "Сейчас не ваш ход.";
        }
        
        gameState.setCurrentPlayerTurn(1);
        gameStateRepository.save(gameState);
        
        return "Вы пропускаете ход.";
    }
    
    public String getGameStatus(String sessionId) {
        Optional<GameState> gameStateOpt = gameStateRepository.findBySessionId(sessionId);
        if (gameStateOpt.isEmpty()) {
            return "Игра не найдена. Скажите 'Начать игру', чтобы начать новую игру.";
        }
        
        GameState gameState = gameStateOpt.get();
        Player player = gameState.getPlayers().get(0);
        Player computer = gameState.getPlayers().get(1);
        
        return String.format("Статус игры: %s - баланс %d, собственности: %d; Компьютер - баланс %d, собственности: %d. %s ходит следующим.", 
                player.getName(), player.getBalance(), player.getProperties().size(),
                computer.getBalance(), computer.getProperties().size(),
                gameState.getCurrentPlayerTurn() == 0 ? "Вы" : "Компьютер");
    }
    
    private List<Property> initializeProperties() {
        List<Property> properties = new ArrayList<>();
        
        // Старт
        Property start = new Property();
        start.setName("Старт");
        start.setPrice(0);
        start.setRent(0);
        properties.add(start);
        
        // Улицы
        Property tverskaya = new Property();
        tverskaya.setName("Тверская улица");
        tverskaya.setPrice(60);
        tverskaya.setRent(2);
        properties.add(tverskaya);
        
        Property arbat = new Property();
        arbat.setName("Арбатская улица");
        arbat.setPrice(80);
        arbat.setRent(4);
        properties.add(arbat);
        
        Property nevskiy = new Property();
        nevskiy.setName("Невский проспект");
        nevskiy.setPrice(100);
        nevskiy.setRent(6);
        properties.add(nevskiy);
        
        Property leninskiy = new Property();
        leninskiy.setName("Ленинский проспект");
        leninskiy.setPrice(120);
        leninskiy.setRent(8);
        properties.add(leninskiy);
        
        Property kutuzova = new Property();
        kutuzova.setName("Кутузовский проспект");
        kutuzova.setPrice(140);
        kutuzova.setRent(10);
        properties.add(kutuzova);
        
        Property tverskoy = new Property();
        tverskoy.setName("Тверской бульвар");
        tverskoy.setPrice(160);
        tverskoy.setRent(12);
        properties.add(tverskoy);
        
        Property dostoevskogo = new Property();
        dostoevskogo.setName("Улица Достоевского");
        dostoevskogo.setPrice(180);
        dostoevskogo.setRent(14);
        properties.add(dostoevskogo);
        
        Property sibiryakov = new Property();
        sibiryakov.setName("Сибиряковская улица");
        sibiryakov.setPrice(200);
        sibiryakov.setRent(16);
        properties.add(sibiryakov);
        
        Property zavodskaya = new Property();
        zavodskaya.setName("Заводская улица");
        zavodskaya.setPrice(220);
        zavodskaya.setRent(18);
        properties.add(zavodskaya);
        
        Property krasnaya = new Property();
        krasnaya.setName("Красная площадь");
        krasnaya.setPrice(240);
        krasnaya.setRent(20);
        properties.add(krasnaya);
        
        // Тюрьма
        Property jail = new Property();
        jail.setName("Тюрьма");
        jail.setPrice(0);
        jail.setRent(0);
        properties.add(jail);
        
        // Остальные улицы
        Property chistyePrudy = new Property();
        chistyePrudy.setName("Чистые пруды");
        chistyePrudy.setPrice(260);
        chistyePrudy.setRent(22);
        properties.add(chistyePrudy);
        
        Property pushkinskaya = new Property();
        pushkinskaya.setName("Пушкинская площадь");
        pushkinskaya.setPrice(280);
        pushkinskaya.setRent(24);
        properties.add(pushkinskaya);
        
        Property pokrovka = new Property();
        pokrovka.setName("Покровка улица");
        pokrovka.setPrice(300);
        pokrovka.setRent(26);
        properties.add(pokrovka);
        
        Property tsvetnoyBulvar = new Property();
        tsvetnoyBulvar.setName("Цветной бульвар");
        tsvetnoyBulvar.setPrice(320);
        tsvetnoyBulvar.setRent(28);
        properties.add(tsvetnoyBulvar);
        
        Property sukharevskaya = new Property();
        sukharevskaya.setName("Сухаревская площадь");
        sukharevskaya.setPrice(350);
        sukharevskaya.setRent(35);
        properties.add(sukharevskaya);
        
        Property arbatOld = new Property();
        arbatOld.setName("Старый Арбат");
        arbatOld.setPrice(400);
        arbatOld.setRent(50);
        properties.add(arbatOld);
        
        Property lubyanka = new Property();
        lubyanka.setName("Лубянка");
        lubyanka.setPrice(450);
        lubyanka.setRent(55);
        properties.add(lubyanka);
        
        return properties;
    }
}