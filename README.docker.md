# Docker Deployment

## Использование опубликованных образов

### Переменные окружения

Создайте файл `.env`:

```env
# Required
BOT_TOKEN=your_bot_token
DB_PASSWORD=your_secure_db_password
ADMIN_PASSWORD=your_secure_admin_password

# Optional
GITHUB_REPOSITORY=your-username/turki
VERSION=latest
DB_USER=turki
ADMIN_USER=admin
```

**Обязательные переменные:**
- `BOT_TOKEN` - токен Telegram бота от @BotFather
- `DB_PASSWORD` - пароль для PostgreSQL
- `ADMIN_PASSWORD` - пароль для доступа к админ-панели

### Авторизация в GitHub Container Registry

```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_USERNAME --password-stdin
```

### Запуск

```bash
docker-compose up -d
```

## Версионирование

Образы версионируются по формату: `{branch-name}-{short-commit-hash}`

Примеры:
- `main-abc1234` - образ из ветки main с коммитом abc1234
- `feature-xyz-5678` - образ из ветки feature-xyz с коммитом 5678

Для main ветки также создается тег `latest`.

## Lifecycle Policy

- Образы из ветки `main` хранятся неограниченно
- Образы из других веток удаляются через 24 часа
- Минимум 10 последних версий всегда сохраняются

## Админ-панель

Веб-версия админ-панели доступна по адресу: http://localhost:8081

Админ-панель автоматически разворачивается вместе с ботом в docker-compose.

Для запуска desktop версии локально:
```bash
./gradlew :admin:run
```

Или соберите нативное приложение:
```bash
./gradlew :admin:packageDistributionForCurrentOs
```

## Импорт данных через API

Обновить уроки, словарь и домашние задания на сервере можно через API админ-панели.

### Импорт уроков

```bash
curl -u admin:$ADMIN_PASSWORD \
  -X POST \
  -H "Content-Type: text/plain" \
  --data-binary @data/lessons.csv \
  "http://localhost:8081/api/import/lessons"
```

### Импорт словаря

```bash
curl -u admin:$ADMIN_PASSWORD \
  -X POST \
  -H "Content-Type: text/plain" \
  --data-binary @data/vocabulary.csv \
  "http://localhost:8081/api/import/vocabulary"
```

### Импорт домашних заданий

```bash
curl -u admin:$ADMIN_PASSWORD \
  -X POST \
  -H "Content-Type: text/plain" \
  --data-binary @data/homework.csv \
  "http://localhost:8081/api/import/homework"
```

### Полное обновление (с очисткой)

Добавьте `?clear=true&confirm=yes` для удаления существующих данных перед импортом:

```bash
# Полный импорт: сначала уроки, потом словарь и домашка
curl -u admin:$ADMIN_PASSWORD -X POST --data-binary @lessons.csv \
  "http://localhost:8081/api/import/lessons?clear=true&confirm=yes"

curl -u admin:$ADMIN_PASSWORD -X POST --data-binary @vocabulary.csv \
  "http://localhost:8081/api/import/vocabulary?clear=true&confirm=yes"

curl -u admin:$ADMIN_PASSWORD -X POST --data-binary @homework.csv \
  "http://localhost:8081/api/import/homework?clear=true&confirm=yes"
```

**Важно:** Параметр `confirm=yes` обязателен при использовании `clear=true`.

### Формат ответа

```json
{
  "success": true,
  "imported": 10,
  "updated": 3,
  "errors": []
}
```

### Ограничения безопасности

- **Аутентификация**: HTTP Basic Auth (ADMIN_USER/ADMIN_PASSWORD)
- **Rate limiting**: 10 запросов в минуту
- **Размер файла**: максимум 10 MB
- **Очистка данных**: требует подтверждения `confirm=yes`
