# Docker Deployment

## Использование опубликованных образов

### Переменные окружения

Создайте файл `.env`:

```env
GITHUB_REPOSITORY=your-username/turki
VERSION=main-abc1234
BOT_TOKEN=your_bot_token
```

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
