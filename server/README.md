# Memory Leak Arena Server

## Сервер — authoritative backend на Ktor WebSocket. Он управляет matchmaking, игровыми комнатами, поступлением ресурсов, deployment карточек, движением юнитов, боевой логикой,

support-логикой, захватом нод и определением победы.

## Ответственности

* Auth через /auth.
* Listen socket через /listen для асинхронных событий.
* Matchmaking через /matchmaking.
* Игровые команды через /game.
* PostgreSQL-backed хранение пользователей.
* Authoritative snapshots состояния игры.

Клиенты отправляют команды; сервер вычисляет реальное состояние матча.

## Требования

* JDK 21.
* PostgreSQL.
* Gradle wrapper из корня проекта.

## Конфигурация БД по умолчанию:

MEMORY_LEAK_DB_URL=jdbc:postgresql://localhost:5432/game
MEMORY_LEAK_DB_USER=gameuser
MEMORY_LEAK_DB_PASSWORD=password

## Запуск PostgreSQL

Из server/:

```./run-postgresql.sh```

Скрипт запускает Docker-контейнер memory-leak-postgres со стандартной БД/пользователем/паролем, ожидаемыми сервером.

Запуск сервера

Из корня проекта:

```./gradlew server:run```

Сервер слушает порт 8080.

## Подробные игровые логи по умолчанию отключены. Чтобы включить их:

```MEMORY_LEAK_DEBUG=true ./gradlew server:run```

## Игровой цикл

После готовности обоих игроков сервер:

1. Создаёт мир из WorldConfig.
2. Отправляет initial state snapshots.
3. Применяет card-команды от клиентов.
4. Обновляет поступление ресурсов каждую секунду.
5. Захватывает Memory/CPU-ноды, если рядом стоят capturer-юниты.
6. Выбирает цели на основе роли юнита.
7. Двигает юниты, применяет боевую и support-логику.
8. Завершает игру, когда Core достигает нуля HP.
