package model.item;

import game.entity.player.PlayerEntity;
import game.pet.Pet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import service.NetworkService;

import java.io.IOException;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Slf4j
public class PotionItem extends BaseItem {

    private String description;
    private int price;
    private int value;
    private int canTrade;
    private int sell;
    private int potionType;
    private int priceType;


    @Override
    public ItemCategory getCategory() {
        return ItemCategory.POTION;
    }

    @Override
    public void onUse(PlayerEntity player) throws IOException {
        switch (id) {
            case 0, 1, 2, 11, 12, 13, 25, 26 -> player.restoreHp(value);
            case 3, 4, 5, 14, 15, 16 -> player.restoreMp(value);
            case 6 -> {
                player.resetPotentialPoints();
                NetworkService.gI().sendMainCharInfo(player);
            }
            case 7 -> {
                player.resetSkill();
                NetworkService.gI().sendMainCharInfo(player);
            }
            case 69 -> {
                Pet pet = player.getPlayerPet().getActivePet();
                if (pet == null) {
                    player.sendMessageDialog("Kamu tidak sedang membawa pet");
                    return;
                }

                if (!pet.canEvolution()) {
                    player.sendMessageDialog("Level pet belum cukup");
                    return;
                }

                pet.evolve();

                player.getInventoryManager().broadcastWearing();
                NetworkService.gI().sendMainCharInfo(player);
            }
            default -> {
                player.sendMessageDialog("Belum ada fungsi");
                return;
            }
        }

        player.getInventoryManager().getInventory().removeByIdAndCategory(id, 1, ItemCategory.POTION);
        player.getInventoryManager().updateInventory();
    }


}

