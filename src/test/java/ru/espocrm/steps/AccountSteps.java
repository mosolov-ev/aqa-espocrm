package ru.espocrm.steps;

import io.qameta.allure.Step;
import ru.espocrm.pages.AccountDetailPage;
import ru.espocrm.pages.AccountFormPage;
import ru.espocrm.pages.AccountsListPage;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

/**
 * Шаги работы с компаниями (раздел Accounts).
 */
public class AccountSteps {

    private final AccountsListPage listPage = new AccountsListPage();

    /** Открыть список компаний и дождаться, пока таблица наполнится */
    @Step("Открыть список компаний")
    public AccountsListPage openList() {
        listPage.openPage();
        // Ждём не «страница открылась», а «данные приехали»: в одностраничном приложении
        // разметка появляется мгновенно, а строки подгружаются запросом к серверу.
        listPage.rows().shouldBe(sizeGreaterThan(0), Duration.ofSeconds(20));
        return listPage;
    }

    /** Найти компанию через строку поиска */
    @Step("Найти компанию по запросу «{0}»")
    public AccountsListPage search(String query) {
        listPage.search(query);
        return listPage;
    }

    /**
     * Создать компанию через интерфейс.
     *
     * @return идентификатор созданной записи — он понадобится, чтобы убрать её после теста
     */
    @Step("Создать компанию «{0}» через интерфейс")
    public String createAccount(String name, String website, String type) {
        AccountFormPage form = openList().clickCreate();
        form.nameInput().shouldBe(visible, Duration.ofSeconds(20));
        form.typeName(name);
        form.typeWebsite(website);
        form.selectType(type);

        AccountDetailPage detail = form.clickSave();
        // После сохранения EspoCRM переходит на карточку записи.
        // Дожидаемся названия — это признак, что переход состоялся и карточка отрисована.
        detail.name().shouldHave(text(name), Duration.ofSeconds(20));
        return detail.recordId();
    }
}
