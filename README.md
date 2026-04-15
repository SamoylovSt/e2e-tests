e2e-tests
===============================================================

### Основная информация
Репозиторий представляет собой централизованное хранилище для e2e тестов между сервисами

### Используемый стек
- Spring Boot 3
- Spring Data JDBC
- Spring Kafka

### Секреты

Перед запуском тестов добавьте секреты в `~/.gradle/gradle.properties`:

```properties
telegramBotToken=<токен телеграм-бота>
googleApplicationCredentialsJson=<JSON сервисного аккаунта Google>
serviceImageTag=dev
```

Gradle передаёт эти значения как переменные окружения в тестовый процесс.

### Запуск тестов

```bash
./gradlew test
```

### Локальный запуск и тестирование
- Через консоль
```bash
    ./gradlew bootRun --args='--spring.profiles.active=ide'
```

- В IntelliJ IDEA
    * Run -> Edit Configurations....
    * В поле Active profiles введите имя профиля: `ide`

### Ссылки на репозиторий документации
- [Системная аналитика e2e-tests]
