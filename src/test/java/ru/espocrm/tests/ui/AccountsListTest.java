package ru.espocrm.tests.ui;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.espocrm.api.EspoApi;
import ru.espocrm.common.BaseUiTest;
import ru.espocrm.pages.AccountsListPage;
import ru.espocrm.steps.AccountSteps;
import ru.espocrm.steps.LoginSteps;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attributeMatching;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

/**
 * Список компаний: состав колонок, количество записей, поиск.
 */
@Tag("ui")
@Feature("Список компаний")
@DisplayName("Список компаний")
class AccountsListTest extends BaseUiTest {

    private final LoginSteps loginSteps = new LoginSteps();
    private final AccountSteps accountSteps = new AccountSteps();

    @BeforeEach
    void login() {
        loginSteps.loginAsAdminAndWait();
    }

    @Test
    @Tag("smoke")
    @DisplayName("В списке отображаются все колонки")
    void listShowsAllColumns() {
        AccountsListPage list = accountSteps.openList();

        list.columnHeader("name").shouldBe(visible).shouldHave(text("Name"));
        list.columnHeader("website").shouldBe(visible).shouldHave(text("Website"));
        list.columnHeader("type").shouldBe(visible).shouldHave(text("Type"));
        list.columnHeader("billingAddressCountry").shouldBe(visible).shouldHave(text("Country"));
    }

    /**
     * Счётчик в интерфейсе сверяется с данными из API.
     *
     * <p>Так тест не ломается при изменении набора данных: он не знает заранее, что компаний
     * пятнадцать, а спрашивает систему. Жёстко вписанное в тест число — частая причина
     * «красных» прогонов после того, как кто-то добавил запись на стенде.
     *
     * <p>Приём называется сверкой независимых источников: интерфейс показывает одно,
     * API отдаёт другое, и расхождение между ними — настоящий дефект.
     */
    @Test
    @DisplayName("Счётчик записей совпадает с данными API")
    void counterMatchesApi() {
        int totalFromApi = EspoApi.accountsTotal();
        AccountsListPage list = accountSteps.openList();

        // Проверяем существование, а не видимость: когда все записи помещаются
        // на одну страницу, EspoCRM прячет блок пагинации, но счётчик в атрибуте
        // title остаётся корректным. Требовать visible здесь — ложное падение.
        list.counter()
                .should(exist)
                .shouldHave(attributeMatching("title", "Total: " + totalFromApi));

        // И вторая, независимая проверка: сколько строк реально отрисовано.
        // Она честна, пока все записи влезают на одну страницу (размер страницы 20).
        if (totalFromApi <= 20) {
            list.rows().shouldHave(size(totalFromApi));
        }
    }

    /**
     * Параметризованный тест: один код, несколько наборов данных.
     *
     * <p>Вместо трёх почти одинаковых тестов — один с аннотацией {@link ParameterizedTest}.
     * JUnit выполнит его по разу на каждое значение из {@link ValueSource} и покажет
     * в отчёте тремя отдельными строками. Упадёт одно значение — остальные всё равно
     * выполнятся, и будет видно, какое именно сломалось.
     */
    @ParameterizedTest(name = "Поиск находит компанию: {0}")
    @ValueSource(strings = {"ООО «УралПрокат»", "АО «Метиз-Трейд»", "ООО «ЭнергоСвар»"})
    @DisplayName("Поиск по названию находит компанию")
    void searchFindsAccount(String companyName) {
        accountSteps.openList();
        AccountsListPage list = accountSteps.search(companyName);

        list.rows().shouldHave(size(1), Duration.ofSeconds(20));
        list.accountNames().first().shouldHave(text(companyName));
    }
}
