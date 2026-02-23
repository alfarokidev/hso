package model.player;

import lombok.Data;
import game.entity.Position;
import model.item.PartSettings;

@Data
public class Player {
    private int id;
    private int uid;
    private String name;
    private int role;
    private long gold;
    private int gems;
    private int level;
    private int strength;
    private int dexterity;
    private int vitality;
    private int intelligence;
    private int potentialPoint;
    private int skillPoint;
    private long experience;
    private short activePoint;
    private short arenaPoint;
    private boolean isOnline;
    private byte[] body = new byte[3];
    private byte[][] rms = new byte[2][0];
    private byte[] skills = new byte[21];
    private Position location = new Position( 1, (short) ((short) 21*24), (short) ((short) 14*24));
    private PartSettings partSettings;




}
