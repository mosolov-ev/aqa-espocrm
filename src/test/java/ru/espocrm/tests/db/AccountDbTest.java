package ru.espocrm.tests.db;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.espocrm.api.EspoApi;
import ru.espocrm.db.Database;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сверка данных в базе с тем, что отдаёт API.
 *
 * <p>Это самый глубокий уровень проверки. Интерфейс может нарисовать запись из кэша,
 * API может отдать её из промежуточного слоя — а в базе её не окажется. Поэтому
 * финальная истина проверяется прямо в таблице.
 *
 * <p>Приём называется сверкой независимых источников и в реальной работе ловит дефекты,
 * которых не видно ни глазами, ни через API.
 */
@Tag("db")
@Feature("Данные в базе")
@DisplayName("Сверка данных с базой")
class AccountDbTest {

    private String createdAccountId;

    @AfterEach
    void cleanUp() {
        EspoApi.deleteAccountQuietly(createdAccountId);
        createdAccountId = null;
    }

    @Test
    @Tag("smoke")
    @DisplayName("Созданная через API компания появляется в таблице account")
    void createdAccountIsStoredInDatabase() {
        String name = "БД Компания " + System.currentTimeMillis();

        createdAccountId = EspoApi.createAccount(name);

        assertThat(Database.accountName(createdAccountId))
                .as("Название компании в таблице account")
                .isEqualTo(name);
        assertThat(Database.isAccountDeleted(createdAccountId))
                .as("Только что созданная запись не должна быть помечена удалённой")
                .isFalse();
    }

    /**
     * Доказательство мягкого удаления на уровне базы.
     *
     * <p>После удаления строка остаётся в таблице, но у неё меняется признак deleted.
     * Именно поэтому во всех запросах нужен {@code WHERE deleted = 0}.
     */
    @Test
    @DisplayName("Удаление не стирает строку, а помечает её признаком deleted")
    void deleteOnlyMarksRowAsDeleted() {
        String name = "БД Удаляемая " + System.currentTimeMillis();
        String id = EspoApi.createAccount(name);

        int activeBefore = Database.countActiveAccounts();
        EspoApi.deleteAccountQuietly(id);

        assertThat(Database.accountName(id))
                .as("Строка физически остаётся в таблице")
                .isEqualTo(name);
        assertThat(Database.isAccountDeleted(id))
                .as("Признак удаления после DELETE")
                .isTrue();
        assertThat(Database.countActiveAccounts())
                .as("Количество живых записей уменьшается на одну")
                .isEqualTo(activeBefore - 1);
    }

    @Test
    @DisplayName("Количество компаний в базе совпадает с ответом API")
    void databaseCountMatchesApi() {
        assertThat(Database.countActiveAccounts())
                .as("Количество живых компаний в базе и в ответе API должно совпадать")
                .isEqualTo(EspoApi.accountsTotal());
    }

    @Test
    @DisplayName("Запрос к таблице с зарезервированным именем выполняется")
    void reservedTableNameIsQueryable() {
        // Проверка не столько функциональности, сколько самого приёма:
        // lead — зарезервированное слово MySQL, и без обратных кавычек
        // запрос падает с синтаксической ошибкой.
        assertThat(Database.countActiveLeads())
                .as("Количество лидов в базе")
                .isGreaterThan(0);
    }
}
