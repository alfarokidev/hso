package language;

import lombok.Getter;

@Getter
public enum LanguageType {
    ENGLISH("en"),
    INDONESIAN("id");

    public final String code;

    LanguageType(String code) {
        this.code = code;
    }
}