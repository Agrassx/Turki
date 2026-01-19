# GitHub Actions Workflows

## build-and-publish.yml

Автоматически собирает и публикует Docker образы при:
- Push в ветку `main` (автоматически)
- Ручной запуск через `workflow_dispatch` (кнопка в GitHub UI)

### Версионирование

Образы версионируются по формату: `{branch-name}-{short-commit-hash}`

Примеры:
- `main-abc1234` - образ из ветки main
- `feature-xyz-5678` - образ из feature ветки

Для ветки `main` также создается тег `latest`.

### Публикуемые образы

1. **Bot**: `ghcr.io/{repository}/turki:{version}`
2. **Admin**: `ghcr.io/{repository}/turki-admin:{version}` (desktop app, не запускается в Docker)

## cleanup-old-images.yml

Автоматически удаляет старые образы:
- Запускается ежедневно в 2:00 UTC
- Удаляет образы старше 24 часов для всех веток кроме `main`
- Сохраняет минимум 10 последних версий
- Образы из `main` и тег `latest` не удаляются

## Использование

### Автоматическая публикация

При каждом merge в `main` автоматически:
1. Собирается образ с версией `main-{commit-hash}`
2. Создается тег `latest`
3. Образ публикуется в GitHub Container Registry

### Ручная публикация

1. Перейдите в раздел "Actions" в GitHub
2. Выберите workflow "Build and Publish"
3. Нажмите "Run workflow"
4. Выберите ветку и нажмите "Run workflow"

### Использование опубликованных образов

В `docker-compose.yml` укажите:

```yaml
services:
  bot:
    image: ghcr.io/your-username/turki:main-abc1234
    # или
    image: ghcr.io/your-username/turki:latest
```

Для использования нужно:
1. Установить переменную `GITHUB_REPOSITORY` в `.env`:
   ```env
   GITHUB_REPOSITORY=your-username/turki
   VERSION=main-abc1234
   ```

2. Войти в GitHub Container Registry:
   ```bash
   echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
   ```
