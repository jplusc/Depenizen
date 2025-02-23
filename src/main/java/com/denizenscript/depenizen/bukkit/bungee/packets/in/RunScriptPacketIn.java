package com.denizenscript.depenizen.bukkit.bungee.packets.in;

import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.depenizen.bukkit.Depenizen;
import com.denizenscript.depenizen.bukkit.bungee.BungeeBridge;
import com.denizenscript.depenizen.bukkit.bungee.PacketIn;
import io.netty.buffer.ByteBuf;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class RunScriptPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "RunScript";
    }

    @Override
    public void process(ByteBuf data) {
        if (data.readableBytes() < 8 + 8 + 4 + 4) {
            BungeeBridge.instance.handler.fail("Invalid RunScriptPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        String scriptName = readString(data, "scriptName");
        String defs = readString(data, "defs");
        if (scriptName == null || defs == null) {
            return;
        }
        long uuidMost = data.readLong();
        long uuidLeast = data.readLong();
        Bukkit.getScheduler().scheduleSyncDelayedTask(Depenizen.instance, () -> {
            PlayerTag linkedPlayer = null;
            if (uuidMost != 0 || uuidLeast != 0) {
                UUID uuid = new UUID(uuidMost, uuidLeast);
                try {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    if (player != null) {
                        linkedPlayer = new PlayerTag(player);
                    }
                }
                catch (Exception ex) {
                    // Ignore
                }
            }
            ScriptTag script = ScriptTag.valueOf(scriptName, CoreUtilities.basicContext);
            if (script == null) {
                Debug.echoError("Invalid Depenizen bungeerun script '" + scriptName + "': script does not exist.");
                return;
            }
            List<ScriptEntry> entries = script.getContainer().getBaseEntries(new BukkitScriptEntryData(linkedPlayer, null));
            if (entries.isEmpty()) {
                return;
            }
            ScriptQueue queue = new InstantQueue("BUNGEERUN_" + scriptName);
            queue.addEntries(entries);
            int x = 1;
            TagContext context = new BukkitTagContext(linkedPlayer, null, script);
            ListTag definitions = ListTag.valueOf(defs, context);
            String[] definition_names = null;
            try {
                String str = script.getContainer().getString("definitions");
                if (str != null) {
                    definition_names = str.split("\\|");
                }
            }
            catch (Exception e) {
                // Ignored
            }
            for (String definition : definitions) {
                String name = definition_names != null && definition_names.length >= x ?
                        definition_names[x - 1].trim() : String.valueOf(x);
                queue.addDefinition(name, definition);
                Debug.echoDebug(entries.get(0), "Adding definition '" + name + "' as " + definition);
                x++;
            }
            queue.addDefinition("raw_context", defs);
            queue.start();
        });
    }
}
