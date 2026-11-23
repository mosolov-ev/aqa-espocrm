package ru.espocrm.steps;

import io.qameta.allure.Step;
import ru.espocrm.config.TestConfig;
import ru.espocrm.pages.LoginPage;

import java.time.Duration;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Шаги работы с авторизацией.
 *
 * <p>Зачем нужен отдельный слой между тестом и Page Object. Page Object говорит на языке
 * интерфейса: «введи текст в поле», «нажми кнопку». Тест должен говорить на языке задачи:
 * «войди администратором». Слой Steps переводит одно в другое.
 *
 * <p>Практическая польза: когда форму входа переделают и шагов станет четыре вместо двух,
 * поменяется один метод здесь, а не двадцать тестов. И отчёт Allure будет читаться
 * как сценарий из тест-кейса, а не как протокол кликов.
 *
 * <p>Аннотация {@link Step} попадает в отчёт отдельной строкой. Текст в фигурных скобках —
 * подстановка аргумента метода: {0} — первый аргумент.
 */
public class LoginSteps {

    private final LoginPage loginPage = new LoginPage();

    /**
     * Войти под администратором.
     *
     * <p>Учётные данные берутся из настроек, а не пишутся в тесте. Логин в коде теста —
     * это и дубль при смене пароля, и утечка в репозиторий.
     */
    @Step("Войти под администратором")
    public void loginAsAdmin() {
        loginAs(TestConfig.adminLogin(), TestConfig.adminPassword());
    }

    /** Войти под произвольным пользователем — нужно негативным тестам */
    @Step("Войти под пользователем {0}")
    public void loginAs(String user, String password) {
        loginPage.openPage();
        loginPage.userName().shouldBe(visible, Duration.ofSeconds(20));
        loginPage.loginAs(user, password);
    }

    /**
     * Дождаться, что приложение загрузилось после входа.
     *
     * <p>Признак входа — появление верхней панели навигации. Проверять по исчезновению
     * кнопки входа ненадёжно: она исчезает раньше, чем приложение становится готово
     * к работе, и следующий шаг теста может начать кликать по ещё не отрисованной странице.
     */
    @Step("Дождаться загрузки приложения")
    public void waitForApplication() {
        $("#navbar").shouldBe(visible, Duration.ofSeconds(30));
    }

    /** Войти администратором и дождаться готовности приложения — самый частый случай */
    @Step("Войти под администратором и дождаться загрузки приложения")
    public void loginAsAdminAndWait() {
        loginAsAdmin();
        waitForApplication();
    }
}
