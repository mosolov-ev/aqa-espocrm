# Автотесты EspoCRM

[![tests](https://github.com/mosolov-ev/aqa-espocrm/actions/workflows/tests.yml/badge.svg)](https://github.com/mosolov-ev/aqa-espocrm/actions/workflows/tests.yml)

UI + API + БД автотесты на Java против локального стенда EspoCRM, который поднимается в Docker вместе с проектом.

**Стек:** Java 21 · Gradle · JUnit 5 · Selenide · REST Assured · AssertJ · Allure · MySQL Connector/J

---

## Быстрый старт

```powershell
cd stand
.\start-stand.ps1
```

```powershell
cd ..
.\gradlew.bat test
```

Первая команда поднимает приложение на http://localhost:8080, вторая прогоняет тесты.

Все команды выполняются **из папки проекта** — `.\gradlew.bat` означает «файл в текущей папке».

---

## Что покрыто

| Группа | Тестов | Что проверяется |
|---|---|---|
| UI | 9 | авторизация, колонки и счётчик списка, поиск, создание компании, валидация формы |
| API | 5 | CRUD, мягкое удаление, отказы 401 / 400 / 409 |
| БД | 4 | сверка таблиц с ответами API, признак мягкого удаления |
| Окружение | 2 | браузер, API, сборка отчёта |

Полный прогон — около 30 секунд.

---

## Запуск

| Команда | Что делает |
|---|---|
| `.\gradlew.bat test` | все тесты |
| `.\gradlew.bat test "-Dtag=ui"` | только интерфейс |
| `.\gradlew.bat test "-Dtag=api"` | только API |
| `.\gradlew.bat test "-Dtag=db"` | только базу |
| `.\gradlew.bat test "-Dtag=smoke"` | самые важные |
| `.\gradlew.bat test "-Dselenide.headless=true"` | без окна браузера |
| `.\gradlew.bat allureReport` | собрать отчёт Allure |

В IntelliJ IDEA то же самое доступно готовыми конфигурациями из папки `.run` — выбрать из списка и нажать Run.

---

## Обучение

Подробная документация — в папке [`docs/`](docs/):

| Документ | О чём |
|---|---|
| [Карта](docs/00-карта.md) | с чего начать, что где лежит |
| [Стенд](docs/01-стенд.md) | запуск приложения, тестовые данные, подключение к базе |
| [Разбор кода](docs/02-разбор-кода.md) | устройство фреймворка слой за слоем |
| [Запуск в IntelliJ](docs/03-запуск-в-intellij.md) | открыть проект, запустить, разобрать падение |
| [Работа с Git](docs/04-работа-с-git.md) | коммиты, ветки, push, что не попадает в репозиторий |
| [Развёртывание с нуля](docs/05-развёртывание-с-нуля.md) | поднять всё на новой машине |
| [Упражнения](docs/06-упражнения.md) | что писать дальше самому |

---

## Структура

```
docs/                 обучение
stand/                локальный стенд EspoCRM (Docker)
.run/                 конфигурации запуска для IntelliJ
src/test/java/ru/espocrm/
├── config/           чтение настроек
├── common/           базовый класс UI-тестов
├── pages/            Page Objects
├── steps/            бизнес-действия для отчёта
├── api/              клиент REST API
├── db/               доступ к базе через JDBC
└── tests/            тесты: ui, api, db
```

---

## Настройка

Паролей в репозитории нет. Перед первым запуском создай два файла из шаблонов:

| Шаблон | Копия |
|---|---|
| `stand/.env.example` | `stand/.env` |
| `src/test/resources/env/local.properties.example` | `src/test/resources/env/local.properties` |

Пароли в обоих файлах должны совпадать. Подробно — в [инструкции по развёртыванию](docs/05-развёртывание-с-нуля.md).

---

## Лицензия

MIT — см. [LICENSE](LICENSE).
