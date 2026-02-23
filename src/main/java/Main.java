import database.DatabaseLoader;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;
import model.ModelMapper;
import model.item.BaseItem;
import model.item.EquipmentItem;
import model.item.MaterialItem;
import model.item.PotionItem;
import network.GameServer;


@Slf4j
public class Main {

    public static void main(String[] args) {
        log.info("╔═══════════════════════════════════════╗");
        log.info("║               VB PROJECT              ║");
        log.info("╚═══════════════════════════════════════╝");

        // Register polymorphic types
        ModelMapper.registerPolymorphicType(BaseItem.class, "EQUIPMENT", EquipmentItem.class);
        ModelMapper.registerPolymorphicType(BaseItem.class, "POTION", PotionItem.class);
        ModelMapper.registerPolymorphicType(BaseItem.class, "MATERIAL", MaterialItem.class);
        ModelMapper.setDiscriminatorField(BaseItem.class, "category");

        // Load game data SYNCHRONOUSLY
        log.info("Loading game data...");
        DatabaseLoader db = DatabaseLoader.getInstance();
        db.loadAll();

        // Create game maps
        WorldManager.getInstance().createGameMaps();
        log.info("Game data loaded successfully - {} maps created",
                WorldManager.getInstance().gameMaps.size());

        // NOW start server loop (AFTER maps are created)
        ServerLoop.getInstance().start();

        // Create and start network server
        GameServer server = GameServer.getInstance(19129);
        server.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            ServerLoop.getInstance().stop();
            server.shutdown();
            log.info("Server shutdown complete");
        }, "ShutdownHook"));

        // Keep main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.error("Main thread interrupted", e);
            ServerLoop.getInstance().stop();
            server.shutdown();
        }
    }
}