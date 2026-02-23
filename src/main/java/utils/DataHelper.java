package utils;

import database.DatabaseLoader;
import database.SQL;
import game.entity.Position;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;
import model.map.MapData;
import model.map.Point;
import model.map.TileData;
import model.npc.NpcData;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
@Slf4j
public class DataHelper {

    public static void main(String[] args) throws IOException {


        //copyPart(Map.of((short) 51,220));
//        byte[] data = IconHelper.getIcon(1, 7500);
//        if (data != null) {
//            log.info(String.valueOf(data.length));
//        }

//        DatabaseLoader loader = DatabaseLoader.getInstance();
//        loader.loadMap();
//        WorldManager world = WorldManager.getInstance();
//        world.createGameMaps();
//
//        TileData tileData = world.getGameMap(0).getMapData().getTileData();
//        if (tileData != null) {
//            int blockId = 0;
//
//
//            byte[] data = FileUtils.loadFile(String.format("data/map/tile_map_info_%d", tileData.getImageId()));
//            assert data != null;
//            DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
//            is.read();
//            blockId = is.read();
//
//            log.info("Stand {}", blockId);
//            byte[] tiles = tileData.getData(); // flat array of tiles
//            int mapWidth = tileData.getWidth();  // replace with your map width
//            int mapHeight = tileData.getHeight(); // replace with your map height
//
//            for (int i = 0; i < tiles.length; i++) {
//                byte tile = tiles[i];
//
//                // blocked tile = 1, walkable = 0
//                System.out.print((tile >= blockId || tile == 0 ? "1 " : "0 "));
//
//                // New line at the end of each row
//                if ((i + 1) % mapWidth == 0) {
//                    System.out.println();
//                }
//            }
//
//            log.info("Block ID {}", blockId);
//        }
    }

    public List<Point> readNpc(MapData mapData) {
        List<Point> npcLocation = new ArrayList<>();
        if (mapData == null || mapData.getNpcData() == null || mapData.getNpcData().isEmpty()) return npcLocation;

        for (String fileName : mapData.getNpcData()) {
            byte[] data = FileUtils.loadFile(String.format("data/npc/%s", fileName));
            if (data == null) continue;

            try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(data))) {
                byte size = is.readByte();
                for (int i = 0; i < size; i++) {
                    String name = is.readUTF();
                    String dialogName = is.readUTF();
                    byte id = is.readByte();
                    byte imageId = is.readByte();
                    short x = is.readShort();
                    short y = is.readShort();
                    byte wBlock = is.readByte();
                    byte hBlock = is.readByte();
                    byte nFrame = is.readByte();

                    byte idBigAvatar = is.readByte();
                    String infoObject = is.readUTF();
                    boolean isPerson = is.readByte() == 1;
                    boolean isShowHP = is.readByte() == 1;

                    NpcData npc = new NpcData();
                    npc.setId(id);
                    npc.setName(name);
                    npc.setLocation(new Position(id, x, y));
                    npc.setDialogName(dialogName);
                    npc.setDialogText(infoObject);
                    npc.setImageId(imageId);
                    npc.setBigAvatar(idBigAvatar);
                    npc.setPerson(isPerson);
                    npc.setTotalFrame(nFrame);
                    npc.setWBlock(wBlock);
                    npc.setHBlock(hBlock);
                    npc.setPerson(isShowHP);
                    npcLocation.add(new Point(id, x, y));

                }
            } catch (Exception e) {
                log.error("Error", e);
            }
        }

        return npcLocation;
    }

    private static void copyMountIcon() {
        Map<Short, Integer> data = new HashMap<>();
        data.put((short) 75, 16037); // Kuda Putih
        data.put((short) 76, 16038); // Kuda Perang BerArmor
        data.put((short) 228, 16039);  // Mamut
        data.put((short) 78, 16040);  // Kuda Hitam
        data.put((short) 229, 16041); // Kerbau Hutan
        data.put((short) 74, 16042); // Kuda Coklat
        data.put((short) 77, 16043); // Kuda Merah
        data.put((short) 112, 16044); // Rusa Kutub
        data.put((short) 195, 16045); //Babi Hutan
        data.put((short) 215, 16046); // Qilin
        data.put((short) 131, 16047); // Serigala Abu Abu
        data.put((short) 143, 16048); // Serigala Badai Salju
        data.put((short) 144, 16049); // Serigala Badai Api
        data.put((short) 145, 16050); // Serigala Hantu
        data.put((short) 147, 16051); // Singa
        data.put((short) 245, 16052); // Qilin Api
        data.put((short) 248, 16053); // Phoenix Api
        data.put((short) 233, 16054); // Ghost Rider
        data.put((short) 217, 16055); // Kuda Skeleton
        data.put((short) 218, 16056); // Tikus Salju
        data.put((short) 249, 16057); // Naga Es
        data.put((short) 232, 16058); // Awan Kinton
        copyIcon(data);

    }

    public static void copyIcon(Map<Short, Integer> data) {

        Path baseDir = Path.of(
                System.getProperty("user.home"),
                "Desktop", "VB", "data", "icons"
        );

        for (Map.Entry<Short, Integer> entry : data.entrySet()) {
            for (int i = 0; i < 4; i++) {
                byte[] iconData = IconHelper.getIcon(i + 1, entry.getKey());

                Path filePath = baseDir
                        .resolve("x" + (i + 1))
                        .resolve(entry.getValue() + ".png");

                save(filePath, iconData);
            }

        }
    }

    //Only for type 113 (targetId, destination)
    public static void copyPart(Map<Short, Integer> data) {

        Path baseDir = Path.of(
                System.getProperty("user.home"),
                "Desktop", "VB", "data", "part_char"
        );

        for (Map.Entry<Short, Integer> entry : data.entrySet()) {
            for (int i = 0; i < 4; i++) {
                PartData part = PartDataLoader.getByZoom(i, (byte) 113, entry.getKey());

                Path filePath = baseDir
                        .resolve("x" + (i + 1))
                        .resolve("data")
                        .resolve("113_" + entry.getValue());

                save(filePath, part.imageData);
            }

        }


    }


    private static void save(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());

            Files.write(
                    path,
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            log.info("SAVED {} ", path.toAbsolutePath().toString());

        } catch (IOException e) {
            log.error("Failed to save file: {}", path, e);
        }
    }
}
