# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Создаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR файл в контейнер
COPY target/monopoly-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт, на котором работает приложение
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]