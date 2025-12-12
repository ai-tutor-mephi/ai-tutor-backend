# AI Tutor Backend

Бэкенд-сервис для приложения AI Tutor. Предоставляет REST API для аутентификации, управления диалогами и обработки файлов, обеспечивающих работу RAG (Retrieval-Augmented Generation) пайплайна. Интегрируется с PostgreSQL для хранения реляционных данных, MinIO для объектного хранилища и внешним Python-сервисом RAG для генерации ответов.

## Архитектура
- **Backend (Spring Boot):** Предоставляет stateless API, защищенные JWT, и оркестрирует потоки диалогов и загрузку файлов.
- **PostgreSQL:** Хранит пользователей, метаданные диалогов и состояние безопасности; транзакционные гарантии защищают идентификационные данные и авторизацию.
- **MinIO:** Хранит загруженные документы (PDF/TXT/DOCX) в S3-совместимом бакете, обеспечивая независимость от облачного провайдера (cloud-agnostic).
- **External RAG service (Python):** Вызывается по HTTP (`RestClient`/`WebClient`) для создания эмбеддингов и генерации ответов на основе загруженного контента.
- **Docker Compose:** Связывает приложение, Postgres и MinIO в единый стек; Testcontainers и WireMock обеспечивают изолированное интеграционное тестирование.

## Технологический стек и обоснование выбора
- **Java 17 & Spring Boot 3:** Современные возможности языка, зрелый DI (Dependency Injection), автоконфигурация и инструменты Actuator для быстрой разработки и поставки.
- **Spring Security & JWT:** Stateless-аутентификация с подписанными токенами упрощает горизонтальное масштабирование и позволяет избежать проблем со "sticky sessions".
- **PostgreSQL:** Строгие гарантии ACID для учетных записей пользователей, refresh-токенов и метаданных диалогов.
- **Spring Data JPA / Hibernate:** Уменьшает количество шаблонного кода (boilerplate) и обеспечивает согласованность схемы данных, сохраняя возможность переопределения запросов.
- **MinIO:** S3-совместимое хранилище, работающее локально; позволяет сменить провайдера S3 без изменения кода.
- **Docker & Docker Compose:** Воспроизводимые среды для приложения и зависимостей; поднятие всего стека одной командой.
- **RestClient / WebClient:** Неблокирующий, конфигурируемый HTTP-клиент для связи с Python RAG-сервисом, поддерживающий таймауты, повторные попытки (retries) и логирование пейлоада.
- **Testing:** JUnit 5 + Mockito для юнит-тестов; Testcontainers для реальных Postgres/MinIO; WireMock для фиксации контрактов RAG-клиента.

## Начало работы (Getting Started)
Требования: Docker, Docker Compose, Java 17+ (для локальной сборки/тестов).

### Запуск через Docker Compose
1) Создайте общую сеть один раз (Compose ожидает её наличие): `docker network create ai_tutor_network`
2) Соберите и запустите сервисы: `docker-compose up --build`
3) API доступно по адресу `http://localhost:8080` (Postgres `localhost:5432`, консоль MinIO `http://localhost:9001`).

### Локальная сборка и тестирование
- Сборка: `./mvnw clean package`
- Запуск тестов (требуется Docker для Testcontainers): `./mvnw test`

## Конфигурация
Ключевые переменные окружения (переопределяют значения в `application.yml`):
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SECURITY_JWT_SECRET`, `SECURITY_JWT_ACCESS_EXPIRATION` (в секундах), `SECURITY_JWT_REFRESH_EXPIRATION` (в секундах), `SECURITY_JWT_ISSUER`
- `APP_MINIO_ENDPOINT`, `APP_MINIO_ACCESS_KEY`, `APP_MINIO_SECRET_KEY`, `APP_MINIO_BUCKET_NAME`, `APP_MINIO_REGION`
- `CLIENTS_RAG_BASE_URL`, `CLIENTS_RAG_CONNECT_TIMEOUT`, `CLIENTS_RAG_RESPONSE_TIMEOUT`, `CLIENTS_RAG_MAX_IN_MEMORY_SIZE`, `CLIENTS_RAG_LOG_PAYLOADS`, `CLIENTS_RAG_RETRY_MAX_ATTEMPTS`, `CLIENTS_RAG_RETRY_BACKOFF`
- `SERVER_PORT`

Задавайте их в `.env` файле или в блоке `environment` в docker-compose; в продакшене секреты должны храниться в Secret Manager.

## Примечания
- Учетные данные MinIO в docker-compose предназначены только для локального использования; в продакшене обязательно ротируйте их.
- Поверхность API документирована через Swagger (`/swagger-ui` / `/v3/api-docs`, если включено). Убедитесь, что RAG-сервис доступен перед отправкой вопросов в диалог.