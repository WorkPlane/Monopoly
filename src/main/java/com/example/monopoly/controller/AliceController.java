package com.example.monopoly.controller;

import com.example.monopoly.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AliceController {
    
    private final GameService gameService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AliceController(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/alice")
public Map<String, Object> handleAliceRequest(@RequestBody Map<String, Object> request) {
    Map<String, Object> response = new HashMap<>();
    
    try {
        Map<String, Object> session = (Map<String, Object>) request.get("session");
        Map<String, Object> requestBody = (Map<String, Object>) request.get("request");
        String command = Optional.ofNullable((String) requestBody.get("command")).orElse("");
        String sessionId = (String) session.get("session_id");
        
        // Обрабатываем команду игрока
        String responseText = processCommand(command, sessionId);
        
        // Если это был ход игрока - выполняем ход компьютера
        if (command.contains("бросить") || command.contains("пропустить")) {
            responseText += "\n" + gameService.computerTurn(sessionId);
        }
        
        // Формируем ответ
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("text", responseText);
        responseBody.put("end_session", false);
        
        response.put("version", "1.0");
        response.put("session", session);
        response.put("response", responseBody);
        
    } catch (Exception e) {
        response.put("error", "Произошла ошибка при обработке запроса");
    }
    
    return response;
}

    private String processCommand(String command, String sessionId) {
        command = command.toLowerCase().trim();
        
        if (command.isEmpty()) {
            return "Привет! Это игра Монополия. Скажите 'начать игру' и ваше имя, например: 'начать игру, меня зовут Иван'";
        }
        
        if (command.contains("начать игру")) {
            String playerName = extractName(command);
            if (playerName.isEmpty()) {
                return "Пожалуйста, назовите ваше имя, например: 'начать игру, меня зовут Иван'";
            }
            return gameService.startNewGame(sessionId, playerName);
        }
        
        if (command.contains("бросить кубики") || command.contains("бросить")) {
            return gameService.rollDice(sessionId);
        }
        
        if (command.contains("купить")) {
            return gameService.buyProperty(sessionId);
        }
        
        if (command.contains("пропустить")) {
            return gameService.endTurn(sessionId);
        }
        
        if (command.contains("статус") || command.contains("как дела")) {
            return gameService.getGameStatus(sessionId);
        }
        
        return "Я не поняла команду. Доступные команды: 'Начать игру', 'Бросить кубики', 'Купить', 'Пропустить', 'Статус'";
    }
    
    private String extractName(String command) {
        String[] parts = command.split("зовут|имя");
        if (parts.length > 1) {
            return parts[1].trim().replaceAll("[^а-яА-Яa-zA-Z ]", "");
        }
        return "";
    }
}