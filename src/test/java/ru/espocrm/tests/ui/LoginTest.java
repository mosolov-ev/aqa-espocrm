package ru.espocrm.tests.ui;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.espocrm.common.BaseUiTest;
import ru.espocrm.pages.LoginPage;
import ru.espocrm.steps.LoginSteps;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Авторизация в EspoCRM.
 *
 * <p>Два теста: позитивный и негативный. Негативный не менее важен — он проверяет,
 * что система не пускает кого попало. Тесты, проверяющие только счастливый путь,
 * пропускают целый класс дефектов безопасности.
 *
 * <p>Аннотация {@link Tag} позволяет запускать группы тестов отдельно:
 * только UI, только API. Используется в Run Configurations и в CI.
 */
@Tag("ui")
@Feature("Авторизация")
@DisplayName("Авторизация")
class LoginTest extends BaseUiTest {

    private final LoginSteps loginSteps = new LoginSteps();

    @Test
    @Tag("smoke")
    @DisplayName("Администратор входит в систему с верными данными")
    void adminCanLogIn() {
        loginSteps.loginAsAdminAndWait();

        // Признак успешного входа — панель навигации с разделами.
        // Проверяем не «нет ошибки», а «появилось то, что должно появиться»:
        // отсутствие ошибки ещё не означает, что вход состоялся.
        $("#navbar").shouldBe(visible);
    }

    @Test
    @DisplayName("Система отклоняет вход с неверными данными")
    void loginWithWrongCredentialsIsRejected() {
        loginSteps.loginAs("nosuchuser", "wrongpassword");

        new LoginPage().errorNotification()
                .shouldBe(visible)
                .shouldHave(text("Wrong username/password"));

        // Дополнительно убеждаемся, что в систему не пустили:
        // форма входа осталась на месте.
        new LoginPage().loginButton().shouldBe(visible);
    }
}
