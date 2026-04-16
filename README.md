e2e-tests
===============================================================

### Основная информация
Репозиторий представляет собой централизованное хранилище для e2e тестов между сервисами

### Используемый стек
- Spring Boot 3
- Spring Data JDBC
- Spring Kafka
- Testcontainers

### Секреты

Тесты читают секреты через переменные окружения. Перед запуском задайте:

| Переменная | Описание |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Токен Telegram-бота |
| `GOOGLE_APPLICATION_CREDENTIALS_JSON` | JSON сервисного аккаунта Google |

**Локально** — задайте переменные в окружении системы или в конфигурации запуска IDE.

**В CI** — задайте через секреты пайплайна.

### Тег образов сервисов

По умолчанию используется тег `dev`. Чтобы переопределить, добавьте в `~/.gradle/gradle.properties`:

```properties
serviceImageTag=dev
```

Или передайте при запуске:

```bash
./gradlew test -PserviceImageTag=my-tag
```

### Запуск тестов

```bash
./gradlew test
```

### Локальный запуск приложения

- Через консоль:
```bash
./gradlew bootRun --args='--spring.profiles.active=ide'
```

- В IntelliJ IDEA:
    * Run → Edit Configurations...
    * В поле Active profiles введите: `ide`

### Ссылки на репозиторий документации
- [Системная аналитика e2e-tests]
