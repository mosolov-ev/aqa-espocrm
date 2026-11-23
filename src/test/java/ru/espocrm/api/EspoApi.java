package ru.espocrm.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.espocrm.config.TestConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Клиент REST API EspoCRM.
 *
 * <p>Используется в двух ролях:
 * <ul>
 *   <li>как объект тестирования в API-тестах;</li>
 *   <li>как инструмент подготовки и уборки данных для UI-тестов. Создать компанию
 *       через API — одна секунда, через интерфейс — пятнадцать. Если тест проверяет
 *       не создание, а что-то другое, готовить данные нужно самым быстрым способом.</li>
 * </ul>
 *
 * <p>Важное правило: <b>подготовка данных через API, проверка — через то, что тестируем</b>.
 * Нельзя и создавать, и проверять через API, если тест про интерфейс.
 */
public final class EspoApi {

    private EspoApi() {
    }

    /**
     * Общая часть всех авторизованных запросов: адрес, заголовки, авторизация.
     *
     * <p>Спецификация собирается один раз и переиспользуется. Так в каждом тесте
     * не повторяется один и тот же набор заголовков, а при смене способа авторизации
     * правка будет в одном месте.
     *
     * <p>Заголовок {@code X-Skip-Duplicate-Check} отключает встроенную проверку дублей:
     * без него EspoCRM отвечает 409 Conflict, если запись с таким названием уже есть.
     * Для тестовых данных дубли допустимы.
     */
    public static RequestSpecification authorized() {
        String credentials = TestConfig.adminLogin() + ":" + TestConfig.adminPassword();
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.url())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Basic " + basic)
                .addHeader("X-Skip-Duplicate-Check", "true")
                .build();
    }

    /**
     * Спецификация с ВКЛЮЧЁННОЙ проверкой дублей — без заголовка X-Skip-Duplicate-Check.
     * Нужна негативному тесту, который проверяет, что система действительно
     * не даёт создать запись с уже существующим названием и отвечает 409 Conflict.
     */
    public static RequestSpecification authorizedWithDuplicateCheck() {
        String credentials = TestConfig.adminLogin() + ":" + TestConfig.adminPassword();
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.url())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Basic " + basic)
                .build();
    }

    /** Запрос без авторизации — нужен негативным тестам, проверяющим доступ */
    public static RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.url())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .build();
    }

    /**
     * Создать компанию.
     *
     * @return идентификатор созданной записи
     */
    public static String createAccount(String name) {
        return RestAssured.given()
                .spec(authorized())
                .body(Map.of("name", name))
                .when()
                .post("/Account")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    /**
     * Удалить компанию. Используется для уборки за тестом.
     *
     * <p>Статус не проверяется намеренно: уборка не должна ронять тест, который
     * уже сделал свою работу. Если запись не удалилась — это проблема стенда,
     * а не проверяемой функциональности, и валить из-за неё зелёный тест неправильно.
     */
    public static void deleteAccountQuietly(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        RestAssured.given()
                .spec(authorized())
                .when()
                .delete("/Account/" + id);
    }

    /** Получить компанию по идентификатору. Ответ отдаётся целиком, чтобы тест сам решал, что проверять. */
    public static Response getAccount(String id) {
        return RestAssured.given()
                .spec(authorized())
                .when()
                .get("/Account/" + id);
    }

    /** Общее количество компаний в системе */
    public static int accountsTotal() {
        return RestAssured.given()
                .spec(authorized())
                .queryParam("maxSize", 1)
                .when()
                .get("/Account")
                .then()
                .statusCode(200)
                .extract()
                .path("total");
    }
}
