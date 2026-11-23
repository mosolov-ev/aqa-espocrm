package ru.espocrm.db;

import ru.espocrm.config.TestConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Доступ к базе данных стенда через JDBC.
 *
 * <p>Зачем автотестам база, если есть API. Затем, что интерфейс и API могут показывать
 * одно, а в базе лежать другое: запись не сохранилась, сохранилась дважды, сохранилась
 * с обрезанным полем. База — последняя инстанция, и сверка с ней ловит дефекты,
 * невидимые через интерфейс.
 *
 * <p>Два приёма, которые стоит перенять.
 *
 * <p><b>try-with-resources.</b> Соединение, запрос и результат объявлены в круглых скобках
 * после {@code try}. Java закроет их сама, в обратном порядке, даже если выбросится
 * исключение. Забытое соединение — классическая утечка: через сотню тестов база
 * перестанет принимать новые подключения.
 *
 * <p><b>PreparedStatement вместо склейки строк.</b> Значения подставляются через {@code ?},
 * а не приклеиваются к тексту запроса. Это защищает от SQL-инъекции и снимает вопрос
 * экранирования кавычек в данных — а в наших тестовых компаниях кавычки есть.
 */
public final class Database {

    private Database() {
    }

    /** Открыть соединение с базой стенда */
    private static Connection open() throws SQLException {
        return DriverManager.getConnection(
                TestConfig.dbUrl(), TestConfig.dbUser(), TestConfig.dbPassword());
    }

    /**
     * Количество «живых» компаний.
     *
     * <p>Условие {@code deleted = 0} обязательно: EspoCRM удаляет записи мягко,
     * физически они остаются в таблице с признаком {@code deleted = 1}.
     * Без этого условия счётчик не сойдётся с интерфейсом.
     */
    public static int countActiveAccounts() {
        return queryInt("SELECT COUNT(*) FROM account WHERE deleted = 0");
    }

    /** Название компании по идентификатору, или null, если записи нет */
    public static String accountName(String id) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name FROM account WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать компанию " + id + " из базы", e);
        }
    }

    /** Помечена ли компания удалённой */
    public static boolean isAccountDeleted(String id) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT deleted FROM account WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Компании " + id + " нет в базе вообще");
                }
                return rs.getBoolean("deleted");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать признак удаления у " + id, e);
        }
    }

    /**
     * Количество «живых» лидов.
     *
     * <p>Обрати внимание на обратные кавычки вокруг названия таблицы: {@code lead} —
     * зарезервированное слово MySQL, и без кавычек запрос не выполнится.
     */
    public static int countActiveLeads() {
        return queryInt("SELECT COUNT(*) FROM `lead` WHERE deleted = 0");
    }

    private static int queryInt(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось выполнить запрос: " + sql, e);
        }
    }
}
