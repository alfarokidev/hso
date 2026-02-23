package game.friend;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Data
@Slf4j
public class FriendList {
    private int id;
    private List<Integer> friends = new ArrayList<>();

    public void add(int paramId) {
        friends.add(paramId);
    }

    public boolean remove(int paramId) {
        return friends.removeIf(id -> id == paramId);
    }

    public boolean isFriend(int paramId) {
        return friends.stream().anyMatch(id -> id == paramId);
    }


}
