package ru.espocrm.smoke;

import com.codeborne.selenide.Configuration;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.title;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ВРЕМЕННЫЙ тест проверки окружения.
 * <p>
 * Проверяет, что собранный проект работоспособен: зависимости подтянулись, Selenide поднимает
 * браузер и видит стенд, REST Assured достукивается до API, а аннотация {@link Step} попадает
 * в отчёт Allure (значит, AspectJ-агент подключён).
 * <p>
 * Архитектуры фреймворка здесь намеренно нет - ни BaseTest, ни Page Object. Это твоя часть работы.
 * Когда напишешь свой первый нормальный тест, этот файл можно удалить.
 */
@DisplayName("Проверка окружения: сборка, браузер, API, отчёт")
class EnvironmentSmokeTest {

    private static Properties props;

    @BeforeAll
    static void setUp() throws IOException {
        props = new Properties();
        try (InputStream in = EnvironmentSmokeTest.class.getClassLoader()
                .getResourceAsStream("env/local.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Не найден env/local.properties. Скопируй local.properties.example и заполни значения.");
            }
            props.load(in);
        }
        Configuration.baseUrl = props.getProperty("url");
        RestAssured.baseURI = props.getProperty("url");
    }

    @AfterEach
    void tearDown() {
        closeWebDriver();
    }

    @Test
    @DisplayName("Selenide поднимает браузер и открывает стенд")
    void browserOpensStand() {
        openStand();
        $("body").shouldBe(visible);
        assertThat(title())
                .as("Заголовок страницы стенда")
                .contains("EspoCRM");
    }

    @Test
    @DisplayName("REST Assured достукивается до API стенда")
    void apiIsReachable() {
        Response response = callApiWithoutAuth();
        assertThat(response.statusCode())
                .as("Без авторизации API должен отвечать 401")
                .isEqualTo(401);
    }

    /** Шаг существует, чтобы проверить: @Step действительно попадает в отчёт Allure. */
    @Step("Открыть стенд EspoCRM")
    private void openStand() {
        open("/");
    }

    @Step("Запросить /api/v1/App/user без авторизации")
    private Response callApiWithoutAuth() {
        return given().when().get("/api/v1/App/user").then().extract().response();
    }
}
