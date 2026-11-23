package ru.espocrm.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Форма создания и редактирования компании.
 *
 * <p>Поля формы находятся по {@code data-name} — это собственный атрибут EspoCRM,
 * которым помечено каждое поле. Такой локатор читается как название поля в интерфейсе
 * и не ломается при смене вёрстки и стилей.
 *
 * <p>Отдельная история — выпадающие списки. EspoCRM рисует их библиотекой Selectize:
 * настоящий {@code <select>} скрыт ({@code display: none}), а пользователь видит
 * набор div-ов. Обычный {@code selectOption()} по скрытому select не сработает,
 * поэтому с таким списком работают как человек: кликают по видимой части и выбирают
 * пункт из выпавшего меню. Метод {@link #selectType(String)} показывает, как именно.
 */
public class AccountFormPage {

    private final SelenideElement nameInput = $("div.field[data-name='name'] input[data-name='name']");
    private final SelenideElement websiteInput = $("div.field[data-name='website'] input[data-name='website']");
    private final SelenideElement descriptionInput = $("div.field[data-name='description'] textarea");
    private final SelenideElement saveButton = $("button[data-action='save']");
    private final SelenideElement cancelButton = $("button[data-action='cancel']");

    public AccountFormPage typeName(String name) {
        nameInput.shouldBe(visible).setValue(name);
        return this;
    }

    public AccountFormPage typeWebsite(String website) {
        websiteInput.setValue(website);
        return this;
    }

    public AccountFormPage typeDescription(String description) {
        descriptionInput.setValue(description);
        return this;
    }

    /**
     * Выбрать значение в выпадающем списке «Type».
     *
     * <p>Три шага, как у пользователя: кликнуть по видимой части списка, дождаться
     * выпадающего меню, кликнуть нужный пункт. Меню Selectize рисуется не внутри поля,
     * а отдельным блоком в конце страницы, поэтому пункт ищется глобально, а не внутри поля.
     */
    public AccountFormPage selectType(String type) {
        $("div.field[data-name='type'] .selectize-input").click();
        $$(".selectize-dropdown [data-value='" + type + "']")
                .first()
                .shouldBe(visible)
                .click();
        return this;
    }

    /** Поле «Название» — тестам нужно для проверки отображения формы и валидации */
    public SelenideElement nameInput() {
        return nameInput;
    }

    /**
     * Ячейка поля «Название» целиком.
     *
     * <p>При неуспешной валидации EspoCRM добавляет ей класс {@code has-error}.
     * Проверять нужно именно ячейку, а не сам input: класс вешается на обёртку.
     */
    public SelenideElement nameFieldCell() {
        return $("div.cell[data-name='name']");
    }

    /** Кнопка «Сохранить» */
    public SelenideElement saveButton() {
        return saveButton;
    }

    /** Сохранить и перейти на карточку созданной компании */
    public AccountDetailPage clickSave() {
        saveButton.click();
        return new AccountDetailPage();
    }

    public void clickCancel() {
        cancelButton.click();
    }
}
