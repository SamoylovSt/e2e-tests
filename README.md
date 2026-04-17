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

Тег задаётся через переменную окружения `TESTCONTAINER_DOCKER_IMAGES_TAG`. Если переменная не задана — запуск завершится ошибкой.

```bash
TESTCONTAINER_DOCKER_IMAGES_TAG=dev ./gradlew test
```

Для отдельного сервиса можно переопределить тег через переменную вида `<SERVICE>_DOCKER_IMAGE_TAG`. Если она задана — используется она, иначе берётся `TESTCONTAINER_DOCKER_IMAGES_TAG`.

| Сервис | Переменная |
|---|---|
| gateway | `GATEWAY_DOCKER_IMAGE_TAG` |
| auth-service | `AUTH_SERVICE_DOCKER_IMAGE_TAG` |
| data-importer | `DATA_IMPORTER_DOCKER_IMAGE_TAG` |
| profile-service | `PROFILE_SERVICE_DOCKER_IMAGE_TAG` |
| project-service | `PROJECT_SERVICE_DOCKER_IMAGE_TAG` |
| mentor-service | `MENTOR_SERVICE_DOCKER_IMAGE_TAG` |
| job-market-analytics-service | `JOB_MARKET_ANALYTICS_SERVICE_DOCKER_IMAGE_TAG` |

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
- [Системная аналитика e2e-tests](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/e2e-tests.md)
