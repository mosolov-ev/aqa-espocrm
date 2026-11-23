package ru.espocrm.common;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ru.espocrm.config.TestConfig;

import static com.codeborne.selenide.Selenide.closeWebDriver;

/**
 * Базовый класс для UI-тестов: настраивает Selenide, подключает отчёт и закрывает браузер.
 *
 * <p>Любой UI-тест наследуется от него и получает всё это бесплатно, не повторяя настройку.
 * API- и JDBC-тесты от него НЕ наследуются: им браузер не нужен, и поднимать его было бы
 * пустой тратой секунд на каждый тест.
 *
 * <p>Класс абстрактный намеренно: сам по себе он не тест, запускать его нечего.
 */
public abstract class BaseUiTest {

    /**
     * Выполняется один раз на класс, до всех тестов.
     *
     * <p>{@code Configuration.baseUrl} позволяет писать в тестах короткое
     * {@code open("/#Account")} вместо полного адреса. Поменяется адрес стенда —
     * правка будет в одном месте, в local.properties.
     *
     * <p>{@code AllureSelenide} — слушатель: он перехватывает каждое действие Selenide
     * и кладёт его в отчёт отдельным шагом, а при падении автоматически прикладывает
     * скриншот и исходный код страницы. Без него отчёт показывал бы только текст ошибки,
     * и разбирать падения было бы заметно труднее.
     */
    @BeforeAll
    static void setUpSelenide() {
        Configuration.baseUrl = TestConfig.url();
        Configuration.browserSize = "1920x1080";

        // Таймаут ожидания элемента. Selenide сам перепроверяет условие,
        // пока оно не выполнится или не истечёт это время — поэтому
        // Thread.sleep в тестах не нужен и является антипаттерном.
        Configuration.timeout = 10_000;

        // addListener вызывается много раз (по разу на класс тестов),
        // но Selenide хранит слушателей по имени и просто заменяет одноимённый.
        SelenideLogger.addListener("allure", new AllureSelenide()
                .screenshots(true)
                .savePageSource(true));
    }

    /**
     * Выполняется перед каждым тестом.
     *
     * <p>Здесь намеренно пусто кроме комментария: браузер открывается не тут,
     * а в момент первого обращения к странице. Selenide создаёт WebDriver лениво —
     * при первом {@code open(...)} или первом обращении к элементу.
     */
    @BeforeEach
    void beforeEachTest() {
        // точка расширения: сюда можно добавить общую подготовку данных
    }

    /**
     * Выполняется после каждого теста — закрывает браузер.
     *
     * <p>Почему после КАЖДОГО теста, а не один раз на класс: так тесты изолированы.
     * Каждый начинает с чистой сессии, без куков и состояния от предыдущего.
     * Тест, который падает из-за соседа, — худший вид нестабильности: он отнимает
     * часы на поиск несуществующей ошибки.
     *
     * <p>Плата за изоляцию — время на перезапуск браузера и повторный вход.
     * Это осознанный размен надёжности на скорость в пользу надёжности.
     */
    @AfterEach
    void closeBrowser() {
        closeWebDriver();
    }
}
