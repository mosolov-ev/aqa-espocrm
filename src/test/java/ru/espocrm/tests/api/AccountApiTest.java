package ru.espocrm.tests.api;

import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.espocrm.api.EspoApi;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API компаний: создание, чтение, удаление и негативные сценарии.
 *
 * <p>Класс не наследуется от BaseUiTest: браузер здесь не нужен, а поднимать его
 * ради каждого теста — минус десять секунд на тест без всякой пользы.
 *
 * <p>Почему негативных тестов больше, чем позитивных. Позитивный путь обычно
 * проверяют и разработчики, и ручное тестирование. А вот что система делает
 * с неверным запросом — забывают проверить чаще всего, и именно там живут дефекты
 * безопасности и невнятные ответы, на которые потом жалуются интеграторы.
 */
@Tag("api")
@Feature("API компаний")
@DisplayName("API компаний")
class AccountApiTest {

    private String createdAccountId;

    @AfterEach
    void cleanUp() {
        EspoApi.deleteAccountQuietly(createdAccountId);
        createdAccountId = null;
    }

    @Test
    @Tag("smoke")
    @DisplayName("Компания создаётся и читается через API")
    void accountCanBeCreatedAndRead() {
        String name = "API Компания " + System.currentTimeMillis();

        createdAccountId = EspoApi.createAccount(name);
        assertThat(createdAccountId)
                .as("Сервер должен вернуть идентификатор созданной записи")
                .isNotBlank();

        Response response = EspoApi.getAccount(createdAccountId);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("name")).isEqualTo(name);
        // deleted — признак "мягкого" удаления. У живой записи он false.
        assertThat(response.jsonPath().getBoolean("deleted")).isFalse();
    }

    /**
     * Удаление в EspoCRM — «мягкое» (soft delete).
     *
     * <p>Это важно понимать, иначе тесты пишутся с неверными ожиданиями. Изначально здесь
     * проверялся код 404 после удаления — и тест падал, хотя система работала правильно.
     * Разбор показал настоящий контракт:
     * <ul>
     *   <li>запись физически остаётся в базе и по идентификатору читается с кодом 200;</li>
     *   <li>у неё выставляется признак {@code deleted: true};</li>
     *   <li>из списков она исчезает — общее количество записей уменьшается.</li>
     * </ul>
     *
     * <p>Отсюда практическое следствие: любой SQL-запрос, сверяющий данные с интерфейсом,
     * обязан содержать {@code WHERE deleted = 0}, иначе счётчики не сойдутся.
     */
    @Test
    @DisplayName("Удалённая компания помечается удалённой и пропадает из списка")
    void deletedAccountIsMarkedAndHiddenFromList() {
        int totalBefore = EspoApi.accountsTotal();
        String id = EspoApi.createAccount("API Удаляемая " + System.currentTimeMillis());
        assertThat(EspoApi.accountsTotal())
                .as("После создания количество компаний увеличивается на одну")
                .isEqualTo(totalBefore + 1);

        RestAssured.given().spec(EspoApi.authorized())
                .when().delete("/Account/" + id)
                .then().statusCode(200);

        Response afterDelete = EspoApi.getAccount(id);
        assertThat(afterDelete.statusCode())
                .as("Запись остаётся доступной по идентификатору — удаление мягкое")
                .isEqualTo(200);
        assertThat(afterDelete.jsonPath().getBoolean("deleted"))
                .as("Признак удаления у записи")
                .isTrue();

        assertThat(EspoApi.accountsTotal())
                .as("Из списка удалённая запись исчезает")
                .isEqualTo(totalBefore);
    }

    @Test
    @DisplayName("Без авторизации API отвечает 401")
    void requestWithoutAuthorizationIsRejected() {
        Response response = RestAssured.given()
                .spec(EspoApi.anonymous())
                .when()
                .get("/Account");

        assertThat(response.statusCode())
                .as("Анонимный запрос к данным должен отклоняться")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Создание без обязательного поля отклоняется с 400")
    void createWithoutRequiredFieldIsRejected() {
        Response response = createAccountWithBody(Map.of("website", "https://no-name.example.ru"));

        assertThat(response.statusCode())
                .as("Компания без названия не должна создаваться")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("Создание дубля отклоняется с 409")
    void createDuplicateIsRejected() {
        String name = "API Дубль " + System.currentTimeMillis();
        createdAccountId = EspoApi.createAccount(name);

        // Тот же запрос, но без заголовка X-Skip-Duplicate-Check:
        // проверка дублей включена, и система обязана возразить.
        Response response = RestAssured.given()
                .spec(EspoApi.authorizedWithDuplicateCheck())
                .body(Map.of("name", name))
                .when()
                .post("/Account");

        assertThat(response.statusCode())
                .as("Повторное создание записи с тем же названием")
                .isEqualTo(409);
        assertThat(response.getHeader("X-Status-Reason"))
                .as("Причина отказа в заголовке ответа")
                .isEqualTo("duplicate");
    }

    @Step("Создать компанию с телом запроса {0}")
    private Response createAccountWithBody(Map<String, Object> body) {
        return RestAssured.given()
                .spec(EspoApi.authorized())
                .body(body)
                .when()
                .post("/Account");
    }
}
