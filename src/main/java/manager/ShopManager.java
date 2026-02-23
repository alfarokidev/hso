package manager;

import lombok.extern.slf4j.Slf4j;
import model.shop.Shop;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ShopManager {

    private ShopManager() {
    }

    private final Map<String, Shop> shops = new HashMap<>();

    private static class Holder {
        private static final ShopManager INSTANCE = new ShopManager();
    }

    public static ShopManager getInstance() {
        return ShopManager.Holder.INSTANCE;
    }

    public void addShop(Shop shop) {
        shops.putIfAbsent(shop.getName(), shop);
        log.debug("Add Shop {} Size {}", shop.getName(), shop.getItems().size());
    }

    public Shop getShop(String name) {
        return shops.get(name);
    }

    public void clear() {
        shops.clear();
    }


}
