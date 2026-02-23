package model.account;

import language.LanguageType;
import lombok.Data;

@Data
public class Account {
    private int id;
    private String user;
    private String pass;
    private String ipAddress;
    private int role;
    private LanguageType language;


}
