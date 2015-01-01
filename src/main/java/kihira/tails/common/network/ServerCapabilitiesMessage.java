package kihira.tails.common.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import kihira.tails.common.Tails;

public class ServerCapabilitiesMessage implements IMessage {

    private boolean library;
    /**
     * Whether they can edit other entities
     */
    private boolean editEntities;

    public ServerCapabilitiesMessage() {}
    public ServerCapabilitiesMessage(boolean library, boolean editEntities) {
        this.library = library;
        this.editEntities = editEntities;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        library = buf.readBoolean();
        editEntities = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(library);
        buf.writeBoolean(editEntities);
    }

    public static class Handler implements IMessageHandler<ServerCapabilitiesMessage, IMessage> {

        @Override
        public IMessage onMessage(ServerCapabilitiesMessage message, MessageContext ctx) {
            Tails.libraryEnabled = message.library;
            Tails.editEntities = message.editEntities;
            return null;
        }
    }
}
