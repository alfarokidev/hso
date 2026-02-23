package database.repositories;


import database.SQL;
import model.account.Account;

import java.sql.SQLException;
import java.util.List;

public class AccountRepository {

    public Account findById(int id) throws SQLException {
        return SQL.from(Account.class).where("id", id).first();
    }

    public Account findByUsername(String username) throws SQLException {
        return SQL.from(Account.class).where("user", username).first();
    }

    public Account findByEmail(String email) throws SQLException {
        return SQL.from(Account.class).where("email", email).first();
    }

    public List<Account> findAll() throws SQLException {
        return SQL.from(Account.class).get();
    }

    public void save(Account account) throws SQLException {
        SQL.insert(account).execute();
    }

    public void update(Account account) throws SQLException {
        SQL.update(account).whereId().execute();
    }

    public void delete(int id) throws SQLException {
        SQL.delete(Account.class).where("id", id).execute();
    }

    public boolean existsByUsername(String username) throws SQLException {
        return SQL.from(Account.class).where("user", username).exists();
    }

    public boolean existsByEmail(String email) throws SQLException {
        return SQL.from(Account.class).where("email", email).exists();
    }
}