package model.monster;

import lombok.Data;

import java.util.List;

@Data
public class GuildMine {
    private int id;
    private int guildId;
    private int x;
    private int y;
    private List<Integer> guards;
}
