# Yaamp для Android

Android приложение музыкального плеера с интеграцией Яндекс.Музыки, основанное на оригинальном [Yaamp](https://github.com/umnik1/yaamp).

## Возможности

### Основной функционал
- ✅ Интеграция с Яндекс.Музыкой через официальный API
- ✅ Воспроизведение музыки с использованием ExoPlayer
- ✅ Поиск артистов, альбомов и треков
- ✅ "Моя волна" - персонализированная радиостанция
- ✅ "Любимые треки" - доступ к избранному
- ✅ Управление плейлистом
- ✅ Фоновое воспроизведение
- ✅ Управление через уведомления

### UI/UX
- ✅ Современный Material Design 3 интерфейс
- ✅ Dark/Light тема с поддержкой Dynamic Colors
- ✅ Jetpack Compose для UI
- ✅ Плавные анимации и переходы
- ✅ Адаптивный дизайн

### Технические возможности
- ✅ Архитектура MVVM
- ✅ Kotlin Coroutines для асинхронных операций
- ✅ StateFlow для реактивного UI
- ✅ ExoPlayer для аудио воспроизведения
- ✅ Retrofit для сетевых запросов
- ✅ Coil для загрузки изображений

## Технологии

- **Язык**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Архитектура**: MVVM
- **Плеер**: Media3 ExoPlayer
- **Сеть**: Retrofit + OkHttp
- **Изображения**: Coil
- **Асинхронность**: Kotlin Coroutines + Flow

## Требования

- Android 8.0 (API 26) или выше
- Android Studio Hedgehog (2023.1.1) или новее
- Kotlin 1.9.20+
- Gradle 8.2+

## Установка и запуск

### 1. Клонирование проекта

```bash
git clone <repository-url>
cd yaamp-android
```

### 2. Настройка API ключа

Для работы с Яндекс.Музыкой необходим OAuth токен:

1. Получите OAuth токен Яндекс.Музыки (инструкции ниже)
2. В файле `MainActivity.kt` раскомментируйте и установите токен:

```kotlin
viewModel.setAuthToken("YOUR_YANDEX_MUSIC_TOKEN_HERE")
```

### 3. Сборка и запуск

```bash
./gradlew assembleDebug
```

Или откройте проект в Android Studio и запустите на эмуляторе/устройстве.

## Получение OAuth токена Яндекс.Музыки

### Способ 1: Через веб-браузер

1. Откройте [OAuth Яндекс](https://oauth.yandex.ru/)
2. Зарегистрируйте приложение
3. Получите Client ID
4. Используйте URL для получения токена:
   ```
   https://oauth.yandex.ru/authorize?response_type=token&client_id=YOUR_CLIENT_ID
   ```
5. После авторизации токен будет в URL (access_token)

### Способ 2: Используя yandex-music-api

Можно использовать Python библиотеку [yandex-music-api](https://github.com/MarshalX/yandex-music-api):

```python
from yandex_music import Client

# Получить токен через логин/пароль (не рекомендуется)
client = Client.from_credentials('username', 'password')
print(client.token)
```

## Структура проекта

```
app/
├── src/main/
│   ├── kotlin/com/yaamp/android/
│   │   ├── data/
│   │   │   ├── api/           # Retrofit API интерфейсы
│   │   │   ├── model/         # Модели данных
│   │   │   └── repository/    # Репозитории
│   │   ├── player/            # PlayerManager для управления воспроизведением
│   │   ├── service/           # MusicPlaybackService (MediaSession)
│   │   ├── ui/
│   │   │   ├── components/    # Переиспользуемые UI компоненты
│   │   │   ├── screens/       # Экраны приложения
│   │   │   ├── theme/         # Material 3 тема
│   │   │   └── viewmodel/     # ViewModels
│   │   ├── MainActivity.kt
│   │   └── YaampApplication.kt
│   ├── res/                    # Ресурсы (strings, themes, etc.)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Основные компоненты

### MusicRepository
Отвечает за взаимодействие с Yandex Music API:
- Поиск треков, артистов, альбомов
- Получение "Моей волны"
- Загрузка "Любимых треков"
- Получение URL для стриминга

### PlayerManager
Управление воспроизведением:
- Контроль ExoPlayer через MediaController
- Управление плейлистом
- StateFlow для реактивного обновления UI

### MusicPlaybackService
MediaSessionService для фонового воспроизведения:
- Интеграция с системными медиа-контролами
- Уведомления с контролами
- Поддержка MediaSession

### MainViewModel
Центральный ViewModel приложения:
- Управление состоянием UI
- Координация между Repository и PlayerManager
- Обработка действий пользователя

## Известные ограничения

1. **OAuth авторизация**: В текущей версии токен устанавливается вручную. Для production необходимо реализовать полноценный OAuth flow с WebView.

2. **Получение прямых ссылок**: Yandex Music API возвращает XML с информацией о загрузке. Необходимо парсить XML и строить прямую ссылку для ExoPlayer.

3. **Кеширование**: Нет локального кеширования треков. Вся музыка стримится онлайн.

4. **Плейлисты**: Ограниченная работа с плейлистами пользователя.

5. **Эквалайзер**: Пока не реализован (можно добавить через AudioEffect API).

## Roadmap

- [ ] Полноценная OAuth авторизация
- [ ] Корректная обработка download URLs из Yandex Music API
- [ ] Локальное кеширование треков
- [ ] Работа с пользовательскими плейлистами
- [ ] Эквалайзер
- [ ] Визуализация (спектр)
- [ ] Скробблинг в Last.fm
- [ ] HTTP API для удаленного управления
- [ ] Виджет на главном экране
- [ ] Android Auto поддержка
- [ ] Оффлайн режим

## Отличия от оригинального Yaamp

| Функция | Оригинал (Desktop) | Android версия |
|---------|-------------------|----------------|
| Интерфейс | Winamp (Webamp) | Material Design 3 |
| Платформа | Electron (Windows/Mac/Linux) | Android |
| Плеер | Webamp | ExoPlayer |
| Скины | Поддержка .wsz | Нет |
| Milkdrop | Есть | Нет |
| HTTP API | Есть | Планируется |
| Discord RPC | Есть | Нет |

## Лицензия

MIT License (как и оригинальный Yaamp)

## Благодарности

- [umnik1/yaamp](https://github.com/umnik1/yaamp) - оригинальный проект
- [captbaritone/webamp](https://github.com/captbaritone/webamp) - Webamp
- [MarshalX/yandex-music-api](https://github.com/MarshalX/yandex-music-api) - Yandex Music API
- Яндекс.Музыка за API

## Дисклеймер

Это неофициальное приложение. Winamp, Яндекс.Музыка и их логотипы являются собственностью соответствующих компаний.

## Поддержка

Если у вас возникли проблемы или есть предложения, пожалуйста, создайте Issue в репозитории.
