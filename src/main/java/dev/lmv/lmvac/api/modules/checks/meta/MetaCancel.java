package dev.lmv.lmvac.api.modules.checks.meta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

// Отменяет Мета-пакеты игрока/ заменяет их.
@SettingCheck(
        value = "MetaCancel",
        cooldown = Cooldown.COOLDOWN
)
public class MetaCancel extends Check implements PacketCheck {

    private final boolean hideHealth;
    private final boolean hideEffects;
    private final boolean hideItemEnchants;
    private final boolean hideItemAttributes;
    private final boolean hideItemName;
    private final boolean hideItemLore;
    private final boolean hidePotionEffects;

    public MetaCancel(Plugin plugin) {
        super(plugin);
        this.hideHealth = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.health");
        this.hideEffects = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.effects");
        this.hideItemEnchants = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.item.enchants");
        this.hideItemAttributes = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.item.attributes");
        this.hideItemName = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.item.name");
        this.hideItemLore = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.item.lore");
        this.hidePotionEffects = ConfigManager.getBoolean(LmvAC.getInstance().getConfig(), "checks.visual.checks.a.hide.potion.effects");
    }

    public void onPacketSending(PacketEvent event) {
        try {
            if (event == null || event.getPlayer() == null || event.isCancelled()) {
                return;
            }

            Player player = event.getPlayer();
            if (!Geyser.isBedrockPlayer(player.getUniqueId())) {
                PacketContainer packet = event.getPacket();
                if (packet != null && !packet.getIntegers().getValues().isEmpty()) {
                    int entityId = packet.getIntegers().read(0);
                    if (entityId != player.getEntityId()) {
                        Player targetPlayer = (Player) LmvPlayer.plID.get(entityId);
                        if (targetPlayer != null) {
                            PacketType type = event.getPacketType();
                            if (type == Server.ENTITY_METADATA) {
                                if (targetPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    handleEntityMetadata(packet, player);
                                }
                            } else if (type == Server.ENTITY_EFFECT) {
                                handleEntityEffect(packet, event);
                            } else if (type == Server.ENTITY_EQUIPMENT) {
                                handleEntityEquipmentSafe(packet, player, event);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logWarning("Error in MetaCancel.onPacketSending", e);
        }
    }

    private void handleEntityEffect(PacketContainer packet, PacketEvent event) {
        try {
            if (packet.getIntegers().size() < 2) {
                return;
            }
            int effectId = packet.getIntegers().read(1);
            PotionEffectType effectType = PotionEffectType.getById(effectId);
            if (effectType != null && !effectType.equals(PotionEffectType.GLOWING) && !effectType.equals(PotionEffectType.INVISIBILITY) && hideEffects) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            logWarning("Error in handleEntityEffect", e);
        }
    }

    private void handleEntityEquipmentSafe(PacketContainer packet, Player receiver, PacketEvent event) {
        try {
            handleEntityEquipment(packet);
        } catch (Exception e) {
            logWarning("Error in handleEntityEquipmentSafe", e);
        }
    }

    public void onPacketReceiving(PacketEvent packetEvent) {
    }

    public ListeningWhitelist getSendingWhitelist() {
        try {
            return ListeningWhitelist.newBuilder()
                    .types(Server.ENTITY_METADATA, Server.ENTITY_EFFECT, Server.ENTITY_EQUIPMENT)
                    .build();
        } catch (Exception e) {
            logSevere("Error creating SendingWhitelist in MetaCancel", e);
            return ListeningWhitelist.EMPTY_WHITELIST;
        }
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    public Plugin getPlugin() {
        return LmvAC.getInstance();
    }

    private void handleEntityMetadata(PacketContainer packet, Player player) {
        try {
            if (packet.getWatchableCollectionModifier().size() == 0) return;

            List<WrappedWatchableObject> metadata = packet.getWatchableCollectionModifier().read(0);
            if (metadata == null || metadata.isEmpty()) return;

            int entityId = packet.getIntegers().read(0);
            Player targetPlayer = (Player) LmvPlayer.plID.get(entityId);
            if (targetPlayer == null) return;

            List<WrappedWatchableObject> filtered = new ArrayList<>();
            for (WrappedWatchableObject watchable : metadata) {
                if (watchable != null) {
                    int index = watchable.getIndex();
                    if ((!hideHealth || index != 8) && isSupportedMetadataTypeSafe(watchable.getValue())) {
                        filtered.add(watchable);
                    }
                }
            }

            packet.getWatchableCollectionModifier().write(0, filtered);
        } catch (Exception e) {
            logWarning("Error in handleEntityMetadata", e);
        }
    }

    private boolean isSupportedMetadataTypeSafe(Object value) {
        try {
            if (value == null) {
                return true;
            } else {
                String className = value.getClass().getName();
                return !(className.contains("DataWatcher") || className.contains("syncher"))
                        && (value instanceof Byte || value instanceof Integer || value instanceof Float
                        || value instanceof Boolean || value instanceof String || value instanceof Optional
                        || value instanceof BaseComponent[] || value instanceof ItemStack);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void handleEntityEquipment(PacketContainer packet) {
        try {
            List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().read(0);
            if (items == null) return;

            List<Pair<EnumWrappers.ItemSlot, ItemStack>> modified = new ArrayList<>(items.size());
            for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
                if (pair != null) {
                    ItemStack item = pair.getSecond();
                    if (item != null && item.getType().isItem()) {
                        ItemStack clone = item.clone();
                        ItemMeta meta = clone.getItemMeta();
                        if (meta != null) {
                            if (meta instanceof PotionMeta) {
                                handlePotion((PotionMeta) meta);
                            } else {
                                processItemMeta(meta);
                            }
                            clone.setItemMeta(meta);
                        }
                        modified.add(new Pair<>(pair.getFirst(), clone));
                    } else {
                        modified.add(pair);
                    }
                }
            }
            packet.getSlotStackPairLists().write(0, modified);
        } catch (Exception e) {
            logWarning("Error in handleEntityEquipment", e);
        }
    }

    private void processItemMeta(ItemMeta meta) {
        try {
            if (meta.hasEnchants() && hideItemEnchants) {
                meta.getEnchants().keySet().forEach(enchantment -> meta.removeEnchant(enchantment));
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            }

            if (meta.hasAttributeModifiers() && hideItemAttributes) {
                meta.getAttributeModifiers().keySet().forEach(meta::removeAttributeModifier);
            }

            if (hideItemName) {
                meta.setDisplayName(null);
            }

            if (hideItemLore) {
                meta.setLore(null);
            }
        } catch (Exception e) {
            logWarning("Error processing item meta", e);
        }
    }

    private void handlePotion(PotionMeta potionMeta) {
        try {
            if (!potionMeta.getCustomEffects().isEmpty() && hidePotionEffects) {
                potionMeta.clearCustomEffects();
                potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1), true);
            }

            if (potionMeta.hasEnchants() && hideItemEnchants) {
                potionMeta.getEnchants().keySet().forEach(potionMeta::removeEnchant);
                potionMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            }

            if (potionMeta.hasAttributeModifiers() && hideItemAttributes) {
                potionMeta.getAttributeModifiers().keySet().forEach(potionMeta::removeAttributeModifier);
            }

            if (hideItemName) {
                potionMeta.setDisplayName(null);
            }

            if (hideItemLore) {
                potionMeta.setLore(null);
            }
        } catch (Exception e) {
            logWarning("Error handling potion meta", e);
        }
    }

    private void logWarning(String message, Throwable e) {
        LmvAC.getInstance().getLogger().log(Level.WARNING, message, e);
    }

    private void logSevere(String message, Throwable e) {
        LmvAC.getInstance().getLogger().log(Level.SEVERE, message, e);
    }
}
