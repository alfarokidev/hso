package language;

import lombok.Data;

@Data
public class Language {
    private String name;
    private String english;
    private String indonesian;

    public String getText(LanguageType lang) {
        return switch (lang) {
            case ENGLISH -> english;
            case INDONESIAN -> indonesian;
        };
    }
}