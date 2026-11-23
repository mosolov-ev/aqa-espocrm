package ru.espocrm.pages;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import static com.codeborne.selenide.Selenide.$;

/**
 * Карточка компании — страница просмотра одной записи.
 *
 * <p>EspoCRM использует одну и ту же разметку для формы и для карточки: поля помечены
 * тем же {@code data-name}, только в режиме просмотра вместо {@code <input>} рисуется текст.
 * Поэтому локаторы здесь похожи на локаторы формы, но указывают на отображаемое значение.
 */
public class AccountDetailPage {

    /** Значение поля «Название» в режиме просмотра */
    private final SelenideElement nameValue = $("div.field[data-name='name']");

    /** Значение поля «Сайт» */
    private final SelenideElement websiteValue = $("div.field[data-name='website']");

    /** Значение поля «Тип» */
    private final SelenideElement typeValue = $("div.field[data-name='type']");

    public SelenideElement name() {
        return nameValue;
    }

    public SelenideElement website() {
        return websiteValue;
    }

    public SelenideElement type() {
        return typeValue;
    }

    /**
     * Идентификатор записи, вытащенный из адресной строки.
     *
     * <p>После сохранения EspoCRM переходит на адрес вида
     * {@code http://localhost:8080/#Account/view/6ad89fd6835ca3d8c}.
     * Этот идентификатор нужен, чтобы потом удалить созданную запись через API —
     * тест обязан убирать за собой, иначе после сотни прогонов в стенде будет
     * сотня мусорных компаний, и проверки количества записей начнут падать.
     *
     * @return идентификатор записи
     * @throws IllegalStateException если адрес не похож на карточку записи
     */
    public String recordId() {
        String url = WebDriverRunner.url();
        int marker = url.indexOf("#Account/view/");
        if (marker < 0) {
            throw new IllegalStateException(
                    "Ожидался адрес карточки компании вида #Account/view/<id>, а получен: " + url);
        }
        return url.substring(marker + "#Account/view/".length());
    }
}
