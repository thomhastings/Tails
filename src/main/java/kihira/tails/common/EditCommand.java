package kihira.tails.common;

import kihira.tails.client.ClientEventHandler;
import kihira.tails.client.render.RenderPart;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class EditCommand implements ICommand {

    @Override
    public String getCommandName() {
        return "taileditor";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return null;
    }

    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    public void processCommand(ICommandSender commandSender, String[] args) {
        if (Tails.editEntities) {
            EntityLivingBase editEntity = Minecraft.getMinecraft().thePlayer;
            if (Minecraft.getMinecraft().pointedEntity instanceof EntityLivingBase) {
                editEntity = (EntityLivingBase) Minecraft.getMinecraft().pointedEntity;
            }
            if (editEntity instanceof EntityPlayer || RenderPart.hasRenderHelper(editEntity.getClass())) {
                //TODO open gui or set to editor mode
                ClientEventHandler.openEditor = true;
            }
            else {
                commandSender.addChatMessage(new ChatComponentText("Cannot edit " + editEntity.getCommandSenderName() + " as there is no render helper for the entity type"));
            }
        }
        else {
            commandSender.addChatMessage(new ChatComponentText("You cannot edit other entities"));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender p_71519_1_) {
        return true;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender p_71516_1_, String[] p_71516_2_) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
