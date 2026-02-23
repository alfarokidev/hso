package game.pet;

import lombok.Getter;
import model.pet.PetData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public class PetManager {
    private final Map<Integer, PetData> petMap = new ConcurrentHashMap<>();

    private PetManager() {
    }

    private static class Holder {
        private static final PetManager INSTANCE = new PetManager();
    }

    public static PetManager getInstance() {
        return PetManager.Holder.INSTANCE;
    }

    public void addPet(PetData pet) {
        petMap.put(pet.getId(), pet);
    }

    public PetData getPet(int id) {
        return petMap.get(id);
    }

    public Pet createPet(int itemId) {
        PetData petData = getPet(itemId);
        if (petData == null) return null;

        Pet pet = new Pet();
        pet.setId(petData.getId());
        pet.setName(petData.getName());
        pet.setIcon(petData.getIcon());
        pet.setImage(petData.getImage());
        pet.setColor(petData.getColor());
        pet.setFrameCount(petData.getFrameCount());
        pet.setMaxGrow(petData.getMaxGrow());
        pet.setOptions(petData.getOptions());
        pet.setTypeMove(petData.getTypeMove());
        pet.setLevel(1);
        pet.setGrow(0);
        pet.setMaxGrow(300);
        pet.setMaxPoints(10_000);
        pet.setHatchTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(petData.getHatchTime()));
        return pet;
    }

}
