package org.btuk.teachingtutorials.utils.plugins;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Handles Luckperm operations
 */
public class Luckperms
{
    public static Track getTrack(String szTrack)
    {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider !=null)
        {
            LuckPerms api = provider.getProvider();
            Track track = api.getTrackManager().getTrack(szTrack);
            return track;
        }
        else
            return null;
    }

    public static Group getGroup(String szGroup)
    {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider !=null)
        {
            LuckPerms api = provider.getProvider();
            Group group = api.getGroupManager().getGroup(szGroup);
            return group;
        }
        else
            return null;
    }

    /**
     * Gets a luckperms user from the UUID of the player
     * @param uuid The uuid of the player
     * @return
     */
    public static User getUser(UUID uuid)
    {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider !=null)
        {
            LuckPerms api = provider.getProvider();
            User lpUser = api.getUserManager().getUser(uuid);
            return lpUser;
        }
        else
            return null;
    }
}