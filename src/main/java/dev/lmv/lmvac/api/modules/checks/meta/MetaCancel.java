package dev.lmv.lmvac.api.modules.checks.meta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.comphenix.protocol.PacketType.Play.Server.*;

@SettingCheck(
        value = "MetaCancel",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.RELEASE,
        description = "Cancels packages that are unnecessary for legitimate clients to hide them from illegal clients."
)
public class MetaCancel extends Check implements PacketCheck, BukkitCheck, Configurable {

    private static final ListeningWhitelist SENDING_WHITELIST = ListeningWhitelist.newBuilder()
            .types(ENTITY_METADATA, ENTITY_EFFECT, ENTITY_EQUIPMENT)
            .build();

    private final Map<Integer, Player> playerCache = new ConcurrentHashMap<>();

    private final List<PotionEffectType> effectsBypass = Arrays.asList(
            PotionEffectType.INVISIBILITY,
            PotionEffectType.GLOWING
    );

    private boolean hideHealth = true;
    private boolean hideEffects = true;
    private boolean hidePotionEffects = true;
    private boolean hideItemEnchants = true;
    private boolean hideItemAttributes = true;
    private boolean hideItemName = true;
    private boolean hideItemLore = true;
    private boolean healthOnlyInvisible = true;

    public MetaCancel(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    @Override
    public void reloadConfiguration(Plugin plugin) {
        try {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.visual.checks.a");
            if (section != null) {
                ConfigurationSection hide = section.getConfigurationSection("hide");
                if (hide != null) {
                    ConfigurationSection item = hide.getConfigurationSection("item");

                    hideHealth = hide.getBoolean("health", true);
                    hideEffects = hide.getBoolean("effects", true);
                    hidePotionEffects = hide.getBoolean("potion.effects", true);

                    if (item != null) {
                        hideItemEnchants = item.getBoolean("enchants", true);
                        hideItemAttributes = item.getBoolean("attributes", true);
                        hideItemName = item.getBoolean("name", true);
                        hideItemLore = item.getBoolean("lore", true);
                    }
                }
                healthOnlyInvisible = section.getBoolean("health-only-invisible", true);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        try {
            if (event == null || event.getPlayer() == null || event.isCancelled()) return;

            Player player = event.getPlayer();
            if (Geyser.isBedrockPlayer(player.getUniqueId())) return;

            PacketContainer packet = event.getPacket();
            if (packet == null || packet.getIntegers().getValues().isEmpty()) return;

            int entityId = packet.getIntegers().readSafely(0);
            if (entityId == player.getEntityId()) return;

            PacketType type = event.getPacketType();

            if (type == ENTITY_METADATA) {
                handleEntityMetadata(packet);
            } else if (type == ENTITY_EFFECT) {
                handleEntityEffect(packet, event);
            } else if (type == ENTITY_EQUIPMENT) {
                handleEntityEquipment(packet);
            }

        } catch (Exception e) {
            logWarning("MetaCancel error", e);
        }
    }

    private void handleEntityMetadata(PacketContainer packet) {
        try {
            if (packet.getWatchableCollectionModifier().size() == 0) return;

            List<?> raw = packet.getWatchableCollectionModifier().readSafely(0);
            if (raw == null || raw.isEmpty()) return;

            List<WrappedWatchableObject> filtered = new ArrayList<>(raw.size());

            for (Object o : raw) {
                if (!(o instanceof WrappedWatchableObject)) continue;
                WrappedWatchableObject w = (WrappedWatchableObject) o;

                if (w.getIndex() == 6 && hideHealth) continue;
                filtered.add(w);
            }

            packet.getWatchableCollectionModifier().write(0, filtered);

        } catch (Exception ignored) {
        }
    }

    private void handleEntityEffect(PacketContainer packet, PacketEvent event) {
        try {
            if (!hideEffects) return;
            if (packet.getIntegers().size() < 2) return;

            int effectId = packet.getIntegers().readSafely(1);
            PotionEffectType effectType = PotionEffectType.getById(effectId);
            if (effectType == null) return;

            if (!effectsBypass.contains(effectType)) {
                event.setCancelled(true);
            }

        } catch (Exception e) {
            logWarning("Error in handleEntityEffect", e);
        }
    }

    private void handleEntityEquipment(PacketContainer packet) {
        try {
            List<?> pairs = packet.getSlotStackPairLists().readSafely(0);
            if (pairs == null || pairs.isEmpty()) return;

            for (Object o : pairs) {
                if (!(o instanceof com.comphenix.protocol.wrappers.Pair)) continue;

                com.comphenix.protocol.wrappers.Pair<?, ?> pair =
                        (com.comphenix.protocol.wrappers.Pair<?, ?>) o;

                Object value = pair.getSecond();
                if (!(value instanceof ItemStack)) continue;

                ItemStack item = (ItemStack) value;
                ItemMeta meta = item.getItemMeta();
                if (meta == null) continue;

                if (meta instanceof PotionMeta) {
                    handlePotion((PotionMeta) meta);
                }

                processItemMeta(meta);
                item.setItemMeta(meta);
            }

        } catch (Exception e) {
            logWarning("Error in handleEntityEquipment", e);
        }
    }

    private void processItemMeta(ItemMeta meta) {
        try {
            if (hideItemEnchants && meta.hasEnchants()) {
                for (Enchantment ench : meta.getEnchants().keySet()) {
                    meta.removeEnchant(ench);
                }
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            }

            if (hideItemAttributes && meta.hasAttributeModifiers()) {
                for (org.bukkit.attribute.Attribute attr : meta.getAttributeModifiers().keySet()) {
                    meta.removeAttributeModifier(attr);
                }
            }

            if (hideItemName) meta.setDisplayName(null);
            if (hideItemLore) meta.setLore(null);

        } catch (Exception ignored) {
        }
    }

    private void handlePotion(PotionMeta meta) {
        try {
            if (hidePotionEffects && !meta.getCustomEffects().isEmpty()) {
                meta.clearCustomEffects();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1), true);
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        playerCache.entrySet().removeIf(e -> e.getValue().equals(p));
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return SENDING_WHITELIST;
    }

    private void logWarning(String msg, Throwable e) {
        LmvAC.getInstance().getLogger().log(Level.WARNING, msg, e);
    }
}
