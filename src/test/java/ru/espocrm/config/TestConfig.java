package ru.espocrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Единая точка доступа к настройкам стенда: адрес, учётные данные, параметры базы.
 *
 * <p>Зачем отдельный класс, а не чтение файла прямо в тестах:
 * <ul>
 *   <li>настройки читаются один раз, а не при каждом обращении;</li>
 *   <li>тесты не знают, откуда взялось значение — из файла, из системного свойства
 *       или из переменной окружения. Завтра источник поменяется, тесты останутся прежними;</li>
 *   <li>если настройка отсутствует, падение происходит сразу и с понятным сообщением,
 *       а не где-то в глубине теста с невнятным NullPointerException.</li>
 * </ul>
 *
 * <p>Приоритет источников: системное свойство перебивает файл. Это позволяет запускать
 * с другими параметрами, ничего не редактируя:
 * <pre>gradlew test "-Durl=http://localhost:9090"</pre>
 *
 * <p>Класс сделан final с приватным конструктором: создавать его экземпляры незачем,
 * у него только статические методы.
 */
public final class TestConfig {

    /** Путь к файлу настроек внутри src/test/resources. */
    private static final String FILE = "env/local.properties";

    /**
     * Настройки читаются один раз при первом обращении к классу и дальше живут в памяти.
     * static final + загрузка в статическом методе — это и есть ленивая инициализация
     * средствами самой Java, без синхронизации и двойных проверок.
     */
    private static final Properties PROPS = load();

    private TestConfig() {
        // утилитный класс, экземпляры не нужны
    }

    /** Адрес стенда, например http://localhost:8080 */
    public static String url() {
        return get("url");
    }

    /** Логин администратора стенда */
    public static String adminLogin() {
        return get("adminLogin");
    }

    /** Пароль администратора стенда */
    public static String adminPassword() {
        return get("adminPass");
    }

    /** JDBC-строка подключения к базе стенда */
    public static String dbUrl() {
        return get("dbUrl");
    }

    /** Пользователь базы данных */
    public static String dbUser() {
        return get("dbUser");
    }

    /** Пароль пользователя базы данных */
    public static String dbPassword() {
        return get("dbPass");
    }

    /**
     * Возвращает значение настройки.
     * Сначала смотрит системные свойства (их задают ключом -D при запуске),
     * потом файл. Если нигде нет — падает с объяснением, что делать.
     */
    private static String get(String key) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "В файле " + FILE + " не задан параметр '" + key + "'. "
                            + "Скопируй env/local.properties.example в env/local.properties и заполни значения.");
        }
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        // getResourceAsStream читает из classpath, а не из файловой системы.
        // Благодаря этому путь не зависит от того, откуда запущены тесты:
        // из IntelliJ, из Gradle или из CI.
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Не найден файл настроек " + FILE + ". "
                                + "Скопируй env/local.properties.example в env/local.properties и заполни значения.");
            }
            properties.load(in);
        } catch (IOException e) {
            // IOException здесь — редкий сбой чтения. Оборачиваем в unchecked,
            // чтобы не заставлять каждый тест объявлять throws.
            throw new UncheckedIOException("Не удалось прочитать " + FILE, e);
        }
        return properties;
    }
}
