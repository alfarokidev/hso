package service;

import database.repositories.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import model.account.Account;
import utils.CryptoUtils;

import java.sql.SQLException;
import java.util.List;

@Slf4j

public class AccountService {
    private final AccountRepository repository = new AccountRepository();
    private static final AccountService INSTANCE = new AccountService();

    private AccountService() {
    }

    public static AccountService gI() {
        return INSTANCE;
    }

    /**
     * Login with username & password
     */
    public Account login(String user, String pass) {
        try {

            Account account = repository.findByUsername(user);

            if (account != null && CryptoUtils.verifyMD5(pass, account.getPass())) {
                return account;
            }

        } catch (SQLException e) {
            log.error("login() Failed: {}", e.getMessage());
        }

        return null;
    }

    public boolean exists(String user) {
        try {

            return repository.existsByUsername(user);

        } catch (SQLException e) {
            log.error("login() Failed: {}", e.getMessage());
        }

        return false;
    }


    public void save(Account account) {
        try {
            repository.update(account);
        } catch (SQLException e) {
            log.error("save() account Failed: {}", e.getMessage());
        }
    }

    /**
     * Create new account
     */
    public boolean createAccount(String user, String pass) {
        try {
            Account account = new Account();
            account.setUser(user);
            account.setPass(CryptoUtils.md5(pass));
            repository.save(account);
            return true;
        } catch (SQLException e) {
            log.error("createAccount() Failed: {}", e.getMessage());
            return false;
        }
    }

}