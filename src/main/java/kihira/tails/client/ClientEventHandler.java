/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014
 *
 * See LICENSE for full License
 */

package kihira.tails.client;

import kihira.tails.client.gui.GuiEditor;
import kihira.tails.client.model.ModelRendererWrapper;
import kihira.tails.client.texture.TextureHelper;
import kihira.tails.common.PartInfo;
import kihira.tails.common.PartsData;
import kihira.tails.common.Tails;
import kihira.tails.common.network.PlayerDataMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {
    private boolean sentPartInfoToServer = false;
    private boolean clearAllPartInfo = false;

    public static RenderLivingEvent.Pre currentEvent = null;
    public static PartsData currentPartsData = null;
    public static ResourceLocation currentPlayerTexture = null;
    public static float partialTicks;
    boolean flag = false;

    /*
        *** Tails Editor Button ***
     */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onScreenInitPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiIngameMenu) {
            event.buttonList.add(new GuiButton(1234, (event.gui.width / 2) - 35, event.gui.height - 25, 70, 20, I18n.format("gui.button.editor")));
        }
    }

    @SubscribeEvent
    public void onButtonClickPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.gui instanceof GuiIngameMenu) {
            if (event.button.id == 1234) {
                event.gui.mc.displayGuiScreen(new GuiEditor());
                event.setCanceled(true);
            }
        }
    }

    /*
        *** Tails syncing ***
     */
    @SubscribeEvent
    public void onConnectToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        //Add local player texture to map
        if (Tails.localPartsData != null) {
            Tails.proxy.addPartsData(Tails.localPartsData.uuid, Tails.localPartsData);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        Tails.hasRemote = false;
        sentPartInfoToServer = false;
        clearAllPartInfo = true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            partialTicks = event.renderTickTime;
        }
    }

    /*
        *** Rendering and building TailInfo ***
     */
    //TODO this used to be RenderPlayerEvent.Pre but currently borked in forge 1.8 so fix in the future
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerRenderTick(RenderLivingEvent.Pre e) {
        if (e.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) e.entity;
            ModelPlayer model = ((RenderPlayer) e.renderer).func_177136_g();
            UUID uuid = player.getGameProfile().getId();
            if (Tails.proxy.hasPartsData(uuid) && !player.isInvisible()) {
                PartsData data = Tails.proxy.getPartsData(uuid);

                if (!flag) {
                    model.bipedBody.addChild(new ModelRendererWrapper(model, PartsData.PartType.TAIL));
                    model.bipedBody.addChild(new ModelRendererWrapper(model, PartsData.PartType.WINGS));
                    model.bipedHead.addChild(new ModelRendererWrapper(model, PartsData.PartType.EARS));
                    flag = true;
                }

                currentPartsData = data;
                currentPlayerTexture = ((AbstractClientPlayer) player).getLocationSkin();
                currentEvent = e;
            }
        }
    }

    @SubscribeEvent()
    public void onPlayerRenderTickPost(RenderLivingEvent.Post e) {
        //Reset to null after rendering the current tail
        currentPartsData = null;
        currentPlayerTexture = null;
        currentEvent = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLivingSpecialsTick(RenderLivingEvent.Specials.Pre e) {
        //Ignore players here, using the player render currentEvent is better
        if (!(e.entity instanceof EntityPlayer)) {
            UUID uuid = e.entity.getPersistentID();
            //TODO Switch to IExtendedEntityProperties instead? Save the data on the player
            if (Tails.proxy.hasPartsData(uuid) && Tails.proxy.getPartsData(uuid).hasPartInfo(PartsData.PartType.TAIL) && !e.entity.isInvisible()) {
                PartInfo info = Tails.proxy.getPartsData(uuid).getPartInfo(PartsData.PartType.TAIL);
                PartsData.PartType partType = PartsData.PartType.TAIL;

                PartRegistry.getRenderPart(partType, info.typeid).render(e.entity, info, e.x, e.y, e.z, 1);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            if (TextureHelper.needsBuild(e.player) && e.player instanceof AbstractClientPlayer) {
                TextureHelper.buildPlayerInfo((AbstractClientPlayer) e.player);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            if (clearAllPartInfo) {
                Tails.proxy.clearAllPartsData();
                clearAllPartInfo = false;
            }
            //World can't be null if we want to send a packet it seems
            else if (!sentPartInfoToServer && Minecraft.getMinecraft().theWorld != null) {
                Tails.networkWrapper.sendToServer(new PlayerDataMessage(Tails.localPartsData, false));
                sentPartInfoToServer = true;
            }
        }
    }
}
