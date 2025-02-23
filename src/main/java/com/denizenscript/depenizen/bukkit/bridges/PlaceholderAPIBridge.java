package com.denizenscript.depenizen.bukkit.bridges;

import com.denizenscript.depenizen.bukkit.Bridge;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIBridge extends Bridge {

    // <--[language]
    // @name PlaceholderAPI Bridge
    // @group Depenizen Bridges
    // @plugin Depenizen, PlaceholderAPI
    // @description
    // Using Depenizen with PlaceholderAPI allows you to use <@link tag placeholder> and <@link tag placeholder.player>,
    // and also allows you to read Denizen tags from any PAPI placeholder location, written in the format: %denizen_<tag-here>%
    // For example: %denizen_<player.flag[MyFlag]>%
    //
    // -->

    @Override
    public void init() {
        if (!new PlaceholderHook().register()) {
            Debug.echoError("Failed to register placeholder for identifier 'denizen'!" +
                    " Denizen PlaceholderAPI placeholders will not function.");
        }
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                tagEvent(event);
            }
        }, "placeholder");
    }

    public void tagEvent(ReplaceableTagEvent event) {
        Attribute attribute = event.getAttributes();

        // <--[tag]
        // @attribute <placeholder[<name>]>
        // @returns ElementTag
        // @plugin Depenizen, PlaceholderAPI
        // @description
        // Returns the value of the placeholder.
        // For example, <placeholder[server_name]>
        // Note that you do not need to include the "%" marks on the placeholder.
        // The queue's linked player, if any, will be linked to the placeholder as well for any player-specific placeholder usage.
        // Note: this should generally only be used for reading values from other plugins not already supported by Depenizen.
        // This should not be used for cases where a Denizen tag already exists, or should exist.
        // -->
        String placeholder = attribute.getParam();
        if (CoreUtilities.toLowerCase(placeholder).startsWith("denizen_")) {
            Debug.echoError("Cannot use <placeholder[]> tags with 'denizen' prefix!");
            return;
        }
        attribute = attribute.fulfill(1);
        OfflinePlayer player = ((BukkitTagContext) event.getContext()).player != null ? ((BukkitTagContext) event.getContext()).player.getOfflinePlayer() : null;

        // <--[tag]
        // @attribute <placeholder[<name>].player[<player>]>
        // @returns ElementTag
        // @plugin Depenizen, PlaceholderAPI
        // @description
        // Returns the value of the placeholder for the specified player.
        // -->
        if (attribute.matches("player") && attribute.hasParam() && attribute.getParamObject().canBeType(PlayerTag.class)) {
            player = attribute.paramAsType(PlayerTag.class).getOfflinePlayer();
            attribute.fulfill(1);
        }
        event.setReplacedObject(new ElementTag(PlaceholderAPI.setPlaceholders(player, "%" + placeholder + "%"))
                .getObjectAttribute(attribute));
    }

    private static class PlaceholderHook extends PlaceholderExpansion {

        @Override
        public boolean canRegister() {
            return true;
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String getAuthor() {
            return "The DenizenScript Team";
        }

        @Override
        public String getIdentifier() {
            return "denizen";
        }

        @Override
        public String getVersion() {
            return "2.0.0";
        }

        @Override
        public String onRequest(OfflinePlayer player, String identifier) {
            return TagManager.tag(identifier, new BukkitTagContext(PlayerTag.mirrorBukkitPlayer(player), null, null, false, null));
        }
    }
}
