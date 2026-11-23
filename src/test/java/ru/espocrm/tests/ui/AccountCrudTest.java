package ru.espocrm.tests.ui;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.espocrm.api.EspoApi;
import ru.espocrm.common.BaseUiTest;
import ru.espocrm.pages.AccountsListPage;
import ru.espocrm.steps.AccountSteps;
import ru.espocrm.steps.LoginSteps;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Создание компании через интерфейс.
 *
 * <p>Тест показывает три важных приёма.
 *
 * <p><b>Первый — уникальные данные.</b> К названию добавляется метка времени.
 * Если прогонять тест с одним и тем же названием, второй прогон столкнётся
 * с проверкой дублей или найдёт в списке запись от прошлого раза и ложно позеленеет.
 *
 * <p><b>Второй — уборка за собой.</b> Созданная запись удаляется в {@link AfterEach},
 * то есть даже если тест упал. Иначе после сотни прогонов в стенде будет сотня
 * мусорных компаний, и проверки количества записей начнут падать на ровном месте.
 *
 * <p><b>Третий — проверка в независимом источнике.</b> Мало увидеть запись в интерфейсе:
 * он мог отрисовать её из кэша, не сохранив на сервере. Поэтому результат
 * дополнительно подтверждается запросом к API.
 */
@Tag("ui")
@Feature("Создание компании")
@DisplayName("Создание компании через интерфейс")
class AccountCrudTest extends BaseUiTest {

    private final LoginSteps loginSteps = new LoginSteps();
    private final AccountSteps accountSteps = new AccountSteps();

    /** Идентификатор созданной записи, чтобы удалить её после теста */
    private String createdAccountId;

    @BeforeEach
    void login() {
        loginSteps.loginAsAdminAndWait();
    }

    @AfterEach
    void removeCreatedAccount() {
        EspoApi.deleteAccountQuietly(createdAccountId);
        createdAccountId = null;
    }

    @Test
    @DisplayName("Созданная компания появляется в списке и сохраняется на сервере")
    void createdAccountAppearsInListAndIsSavedOnServer() {
        String name = "Автотест Компания " + System.currentTimeMillis();
        String websiteTyped = "https://autotest.example.ru";

        // Интерфейс EspoCRM нормализует адрес сайта: введённую схему https:// он отбрасывает.
        // Это проверено отдельно — через API схема сохраняется как есть, значит обрезает
        // её именно виджет формы. Поэтому ожидаем нормализованное значение, а не введённое:
        // иначе тест падал бы на корректном поведении системы.
        String websiteStored = "autotest.example.ru";

        createdAccountId = accountSteps.createAccount(name, websiteTyped, "Customer");

        // 1. Запись видна в интерфейсе — ищем её в списке
        AccountsListPage list = accountSteps.openList();
        accountSteps.search(name);
        list.rows().shouldHave(size(1), Duration.ofSeconds(20));
        list.accountNames().first().shouldHave(text(name));

        // 2. Запись действительно сохранена на сервере — спрашиваем API.
        //    assertThat из AssertJ читается как обычная фраза и при падении
        //    показывает и ожидаемое, и фактическое значение.
        var response = EspoApi.getAccount(createdAccountId);
        assertThat(response.statusCode())
                .as("Созданная компания должна читаться через API")
                .isEqualTo(200);
        assertThat(response.jsonPath().getString("name"))
                .as("Название компании на сервере")
                .isEqualTo(name);
        assertThat(response.jsonPath().getString("website"))
                .as("Сайт компании на сервере, в нормализованном виде")
                .isEqualTo(websiteStored);
    }
}
