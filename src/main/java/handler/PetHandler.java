package handler;

import game.entity.player.PlayerEntity;
import game.inventory.InventorySlot;
import game.pet.Pet;
import game.pet.PetManager;
import lombok.extern.slf4j.Slf4j;
import manager.ItemManager;
import model.item.*;
import network.Message;
import network.Session;
import service.NetworkService;

import java.io.IOException;
import java.util.Optional;

import static handler.Command.PET_EAT;
import static handler.Command.UPDATE_PET_CONTAINER;

@Slf4j
public class PetHandler {

    public static void onMessage(Session s, Message m) throws IOException {
        switch (m.command) {
            case UPDATE_PET_CONTAINER -> onUpdatePetContainer(s, m);
            case PET_EAT -> onFeedPet(s, m);
        }
    }

    private static void onFeedPet(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        short petId = m.in().readShort();
        short itemId = m.in().readShort();
        byte category = m.in().readByte();
        byte type = m.in().readByte();

        if (category == 7) {
            return;
        }

        log.debug("PET_ID {} ITEM_ID {} CATEGORY {} TYPE {}", petId, itemId, category, type);
        int index = 0;
        InventorySlot slot = switch (category) {
            case 3 -> {
                InventorySlot sl = p.getInventoryManager().getInventory().slot(itemId);
                if (sl.isEmpty()) yield null;

                EquipmentItem eq = (EquipmentItem) sl.getItem();
                index = switch (eq.getType()) {
                    case 8, 9 -> 0;
                    case 4, 5 -> 1;
                    case 0, 1, 2, 3, 6 -> 2;
                    case 10, 11 -> 3;
                    default -> -1;
                };
                yield index == -1 ? null : sl;
            }

            case 4 -> {
                Optional<InventorySlot> opt = p.getInventoryManager().getInventory().findSlot(itemId, ItemCategory.POTION);
                if (opt.isEmpty()) yield null;

                InventorySlot sl = opt.get();
                if (sl.isEmpty()) yield null;

                index = switch (itemId) {
                    case 51 -> 0;
                    case 48 -> 1;
                    case 50 -> 2;
                    case 49 -> 3;
                    default -> -1;
                };
                yield index == -1 ? null : sl;
            }
            default -> null;
        };

        if (slot == null) {
            p.sendMessageDialog("Makanan pet tidak cocok");
            return;
        }

        Pet pet;
        if (type == 1) {
            pet = p.getPlayerPet().getActivePet();
        } else {
            pet = p.getPlayerPet().getPet(petId);
        }

        if (pet == null) {
            p.sendMessageDialog("Target pet tidak ditemukan");
            return;
        }

        if (pet.canEvolution()) {
            p.sendMessageDialog("Pet membutuhkan growth potion");
            return;
        }
        int point = category == 4 ? 50 : 15;
        if (!pet.addPoint(index, point)) {
            p.sendMessageDialog("Point sudah maksimal");
            return;
        }

        double multiply = category == 4 ? 0.03 : 0.050;
        pet.addExperience(Math.round(pet.getRequiredExp() * multiply));
        slot.decrease();
        p.getInventoryManager().updateInventory();
        p.recalculateStats();
        if (type == 1) {
            p.getInventoryManager().broadcastWearing();
            NetworkService.gI().sendMainCharInfo(p);
        } else {
            NetworkService.gI().sendAddItemPetContainer(p, 9, pet);
        }

        String text = switch (index) {
            case 0 -> String.format("+%d Pet STR", point);
            case 1 -> String.format("+%d Pet DEX", point);
            case 2 ->String.format("+%d Pet VIT", point);
            case 3 -> String.format("+%d Pet INT", point);
            default -> "";
        };

        NetworkService.gI().sendToast(p, text);

    }

    private static void onUpdatePetContainer(Session s, Message ms) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;


        byte type = ms.in().readByte();
        short bagSlot = ms.in().readShort();

        log.debug("type {} id {}", type, bagSlot);
        switch (type) {
            case 0 -> {
                Pet activePet = p.getPet();
                if (activePet != null) {
                    activePet.setFollow(false);
                }

                Pet pet = p.getPlayerPet().getPet(bagSlot);
                if (pet == null) {
                    NetworkService.gI().sendToast(p, "Pet tidak ditemukan");
                    return;
                }

                pet.setFollow(true);
                NetworkService.gI().removePetContainer(p, bagSlot);
                if (activePet != null) {
                    NetworkService.gI().sendAddItemPetContainer(p, 9, activePet);
                }
                p.getInventoryManager().broadcastWearing();
            }
            case 1 -> {
                InventorySlot slot = p.getInventoryManager().getSlot(bagSlot);
                if (slot == null || slot.getItem() == null) {
                    NetworkService.gI().sendToast(p, "Item tidak ditemukan");
                    return;
                }

                EquipmentItem item = (EquipmentItem) slot.getItem();

                if (p.getPlayerPet().getPet(item.getId()) != null) {
                    p.sendMessageDialog("Kamu sudah memiliki pet jenis ini");
                    return;
                }

                Pet pet;
                if ((pet = PetManager.getInstance().createPet(item.getId())) == null) {
                    NetworkService.gI().sendToast(p, "Telur ini tidak dapat dierami");
                    return;
                }

                NetworkService.gI().sendAddItemPetContainer(p, p.getSession().getAccount().getRole() == 1 ? 9 : 3, pet);
                slot.clear();
                p.getPlayerPet().addPet(pet);

                NetworkService.gI().sendMainCharInfo(p);
                p.getInventoryManager().updateInventory();

            }
        }

    }

}
