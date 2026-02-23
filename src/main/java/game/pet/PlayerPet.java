package game.pet;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayerPet {
    private int playerId;
    private List<Pet> pets = new ArrayList<>();

    public void addPet(Pet pet) {
        pets.add(pet);
    }

    public Pet getPet(int id) {
        for (Pet pet : pets) {
            if (pet.getId() == id) {
                return pet;
            }
        }

        return null;
    }

    public Pet getActivePet() {
        for (Pet pet : pets) {
            if (pet.isFollow()) {
                return pet;
            }
        }
        return null;
    }

    public List<Pet> getAllPet() {
        return pets.stream()
                .filter(Pet::isHatched)
                .filter(pet -> !pet.isFollow())
                .toList();
    }

    public List<Pet> getEggs() {
        return pets.stream()
                .filter(pet -> !pet.isHatched())
                .toList();
    }

    public int getEggCount() {return getEggs().size();}
}
