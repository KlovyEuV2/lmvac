package dev.lmv.lmvac.api.implement.api.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RandomArmorGenerator {

    private static final Random random = new Random();

    private static final Material[] HELMETS = {
            Material.LEATHER_HELMET, Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET,
            Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.AIR
    };
    private static final Material[] CHESTPLATES = {
            Material.LEATHER_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE,
            Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.AIR
    };
    private static final Material[] LEGGINGS = {
            Material.LEATHER_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
            Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS, Material.AIR
    };
    private static final Material[] BOOTS = {
            Material.LEATHER_BOOTS, Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS,
            Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS, Material.AIR
    };

    public static void sendRandomArmor(Player player, int entityId) {
        try {
            PacketContainer equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
            equipmentPacket.getIntegers().write(0, entityId);

            List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = Arrays.asList(
                    new Pair<>(EnumWrappers.ItemSlot.HEAD, new ItemStack(getRandomMaterial(HELMETS))),
                    new Pair<>(EnumWrappers.ItemSlot.CHEST, new ItemStack(getRandomMaterial(CHESTPLATES))),
                    new Pair<>(EnumWrappers.ItemSlot.LEGS, new ItemStack(getRandomMaterial(LEGGINGS))),
                    new Pair<>(EnumWrappers.ItemSlot.FEET, new ItemStack(getRandomMaterial(BOOTS)))
            );

            equipmentPacket.getSlotStackPairLists().write(0, equipment);

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, equipmentPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Material getRandomMaterial(Material[] materials) {
        return materials[random.nextInt(materials.length)];
    }
}