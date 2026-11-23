package ru.espocrm.tests.ui;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.espocrm.api.EspoApi;
import ru.espocrm.common.BaseUiTest;
import ru.espocrm.pages.AccountFormPage;
import ru.espocrm.steps.AccountSteps;
import ru.espocrm.steps.LoginSteps;

import java.time.Duration;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Валидация формы создания компании.
 *
 * <p>Проверяет, что система не даёт сохранить запись без обязательного поля.
 * Такие тесты ценны вдвойне: они ловят и сломанную валидацию на клиенте,
 * и ситуацию, когда клиент пропустил пустое значение, а сервер его принял.
 */
@Tag("ui")
@Feature("Создание компании")
@DisplayName("Валидация формы компании")
class AccountValidationTest extends BaseUiTest {

    private final LoginSteps loginSteps = new LoginSteps();
    private final AccountSteps accountSteps = new AccountSteps();

    @BeforeEach
    void login() {
        loginSteps.loginAsAdminAndWait();
    }

    @Test
    @DisplayName("Компания без названия не сохраняется")
    void accountWithoutNameIsNotSaved() {
        int totalBefore = EspoApi.accountsTotal();

        AccountFormPage form = accountSteps.openList().clickCreate();
        form.nameInput().shouldBe(visible, Duration.ofSeconds(20));

        // Сохраняем, не заполнив обязательное поле
        form.saveButton().click();

        // Поле подсвечено как ошибочное
        form.nameFieldCell().shouldHave(cssClass("has-error"), Duration.ofSeconds(10));

        // И главное — запись действительно не создалась.
        // Проверять только подсветку недостаточно: клиент мог показать ошибку,
        // но всё равно отправить запрос на сервер.
        assertThat(EspoApi.accountsTotal())
                .as("Количество компаний не должно измениться")
                .isEqualTo(totalBefore);
    }
}
