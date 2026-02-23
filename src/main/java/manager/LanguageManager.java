package manager;

import database.SQL;
import language.Key;
import language.Language;
import language.LanguageType;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public final class LanguageManager {

    private volatile Map<String, EnumMap<LanguageType, String>> data = new HashMap<>();

    private LanguageManager() {
    }

    private static final class Holder {
        private static final LanguageManager INSTANCE = new LanguageManager();
    }

    public static LanguageManager getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void loadLanguages() {
        try {
            final List<Language> rows = SQL.from(Language.class).get();
            final Map<String, EnumMap<LanguageType, String>> fresh = new HashMap<>(rows.size());

            for (Language entry : rows) {
                final EnumMap<LanguageType, String> texts = new EnumMap<>(LanguageType.class);

                for (LanguageType lang : LanguageType.values()) {
                    final String text = entry.getText(lang);
                    if (text != null && !text.isEmpty()) {
                        texts.put(lang, text);
                    }
                }

                fresh.put(entry.getName(), texts);
            }

            data = fresh;
            log.info("LanguageManager loaded {} keys.", fresh.size());

        } catch (SQLException e) {
            log.error("LanguageManager failed to load language data.", e);
        }
    }


    public String get(Key key, LanguageType lang) {
        final EnumMap<LanguageType, String> texts = data.get(key.name());
        if (texts == null) return "[MISSING:" + key + "]";
        return texts.getOrDefault(lang,
                texts.getOrDefault(LanguageType.ENGLISH, "[NO_TEXT:" + key + "]"));
    }

    public String get(Key key, LanguageType lang, Object... args) {
        String text = get(key, lang);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }


    public void register(String key, LanguageType lang, String text) {
        data.computeIfAbsent(key, k -> new EnumMap<>(LanguageType.class)).put(lang, text);
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }
}