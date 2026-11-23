package ru.espocrm.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withText;

/**
 * Страница входа в EspoCRM.
 *
 * <p>Это Page Object — описание одной страницы: где лежат её элементы и что с ними можно делать.
 * Главное правило: <b>в Page Object нет проверок</b>. Он только находит элементы и выполняет
 * действия, а решает, что считать правильным, — тест. Если смешать, один и тот же Page Object
 * нельзя будет переиспользовать в тестах с разными ожиданиями: позитивном и негативном.
 *
 * <p>Локаторы собраны по реальной разметке стенда. Все они опираются на атрибут {@code id}
 * или {@code data-name} — такие локаторы переживают изменение вёрстки и стилей,
 * в отличие от привязки к сгенерированным CSS-классам или к позиции элемента на странице.
 */
public class LoginPage {

    private final SelenideElement userNameInput = $("#field-userName");
    private final SelenideElement passwordInput = $("#field-password");
    private final SelenideElement loginButton = $("#btn-login");

    /** Всплывающее уведомление об ошибке входа */
    private final SelenideElement notification = $("#notification");

    /** Открыть страницу входа. Адрес берётся из Configuration.baseUrl, заданного в BaseUiTest. */
    public LoginPage openPage() {
        open("/");
        return this;
    }

    /**
     * Заполнить форму и нажать «Войти».
     *
     * <p>{@code withText(password).sensitive()} прячет пароль из логов и отчёта Allure —
     * иначе он попал бы в отчёт открытым текстом и уехал бы в CI вместе с артефактами.
     */
    public void loginAs(String user, String password) {
        userNameInput.setValue(user);
        passwordInput.setValue(withText(password).sensitive());
        loginButton.click();
    }

    /** Поле «Имя пользователя» — нужно тестам, чтобы убедиться, что форма отображается */
    public SelenideElement userName() {
        return userNameInput;
    }

    /** Кнопка входа */
    public SelenideElement loginButton() {
        return loginButton;
    }

    /** Уведомление об ошибке — нужно негативному тесту */
    public SelenideElement errorNotification() {
        return notification;
    }
}
