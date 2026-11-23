package ru.espocrm.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

/**
 * Список компаний — раздел Accounts.
 *
 * <p>Адрес строится через решётку: {@code #Account}. EspoCRM — одностраничное приложение,
 * и навигация в нём идёт по хэшу, а не перезагрузкой страницы.
 *
 * <p>Обрати внимание на работу со строками. Строки — это коллекция
 * ({@link ElementsCollection}), а не список строк. Разница принципиальная: коллекция
 * Selenide «ленивая», она не обращается к браузеру, пока её не спросят. Поэтому
 * {@code rows().shouldHave(size(15))} будет сам перепроверять количество, пока таблица
 * догружается, а {@code rows().size()} вернёт сколько есть прямо сейчас — и на медленной
 * загрузке даст ноль. Это типичная причина «плавающих» падений.
 */
public class AccountsListPage {

    /** Строки таблицы. У каждой есть data-id — идентификатор записи в системе. */
    private final ElementsCollection rows = $$("tr.list-row");

    /** Поле поиска над таблицей */
    private final SelenideElement searchInput = $("input[data-name='textFilter']");

    /** Кнопка создания новой компании */
    private final SelenideElement createButton = $("a[data-action='create']");

    /**
     * Счётчик записей. Выглядит как «1–15 / 15», а в атрибуте title лежит «Total: 15».
     * Для проверок удобнее title: он не зависит от того, сколько записей влезло на страницу.
     */
    private final SelenideElement paginationCounter = $("a.pagination-btn-middle");

    public AccountsListPage openPage() {
        open("/#Account");
        return this;
    }

    /** Все строки таблицы */
    public ElementsCollection rows() {
        return rows;
    }

    /**
     * Ссылки с названиями компаний. Пригодятся, чтобы проверить содержимое списка,
     * не разбирая каждую строку по отдельности.
     */
    public ElementsCollection accountNames() {
        return $$("tr.list-row td[data-name='name'] a.link");
    }

    /** Строка по названию компании. Ищем именно по ссылке в колонке «Name». */
    public SelenideElement rowByName(String name) {
        return $$("tr.list-row").findBy(text(name));
    }

    /** Ячейка конкретной колонки в строке с заданным названием */
    public SelenideElement cell(String accountName, String columnName) {
        return rowByName(accountName).$("td[data-name='" + columnName + "']");
    }

    /** Поле поиска */
    public SelenideElement searchInput() {
        return searchInput;
    }

    /** Ввести запрос в поиск и выполнить его нажатием Enter */
    public AccountsListPage search(String query) {
        searchInput.setValue(query).pressEnter();
        return this;
    }

    /** Очистить поиск и вернуть полный список */
    public AccountsListPage clearSearch() {
        searchInput.clear();
        searchInput.pressEnter();
        return this;
    }

    /** Счётчик записей — для проверки общего количества */
    public SelenideElement counter() {
        return paginationCounter;
    }

    /** Кнопка «Создать» */
    public AccountFormPage clickCreate() {
        createButton.click();
        return new AccountFormPage();
    }

    /** Заголовок колонки таблицы — для проверки состава колонок */
    public SelenideElement columnHeader(String columnName) {
        return $("th.field-header-cell[data-name='" + columnName + "']");
    }
}
