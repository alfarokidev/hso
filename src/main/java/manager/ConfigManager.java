package manager;

import lombok.Getter;
import lombok.Setter;
import model.config.AttributeConfig;
import model.config.SVConfig;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class ConfigManager {
    private SVConfig svConfig;
    private final Map<Integer, AttributeConfig> attributeConfigMap = new HashMap<>();

    private ConfigManager() {
    }

    private static class Holder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return ConfigManager.Holder.INSTANCE;
    }

    public void addAttributeConfig(AttributeConfig config) {
        attributeConfigMap.put(config.getRole(), config);
    }

    public AttributeConfig getAttributeConfig(int role) {
        return attributeConfigMap.get(role);
    }

}
