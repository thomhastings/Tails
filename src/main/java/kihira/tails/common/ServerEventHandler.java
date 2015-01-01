/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014
 *
 * See LICENSE for full License
 */

package kihira.tails.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import kihira.tails.common.network.PlayerDataMapMessage;
import kihira.tails.common.network.ServerCapabilitiesMessage;
import net.minecraft.entity.player.EntityPlayerMP;

public class ServerEventHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        //Send current known tails to client
        Tails.networkWrapper.sendTo(new PlayerDataMapMessage(Tails.proxy.getPartsData()), (EntityPlayerMP) event.player);
        Tails.networkWrapper.sendTo(new ServerCapabilitiesMessage(Tails.libraryEnabled,FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer() ||
                FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().func_152596_g(event.player.getGameProfile())),
                (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        //Server doesn't save tails so we discard
        Tails.proxy.removePartsData(event.player.getGameProfile().getId());
    }
}
