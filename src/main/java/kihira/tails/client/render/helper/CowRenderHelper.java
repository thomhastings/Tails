package kihira.tails.client.render.helper;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import kihira.tails.api.IRenderHelper;
import kihira.tails.client.model.ModelRendererWrapper;
import kihira.tails.client.render.RenderPart;
import kihira.tails.common.PartInfo;
import kihira.tails.common.PartsData;
import net.minecraft.client.model.ModelCow;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.passive.EntityCow;
import org.lwjgl.opengl.GL11;

public class CowRenderHelper implements IRenderHelper<EntityCow> {

    @Override
    public void addModelHelpers(RenderLiving renderLiving) {
        ModelCow modelCow = ObfuscationReflectionHelper.getPrivateValue(RenderLiving.class, renderLiving, "mainModel");
        modelCow.body.addChild(new ModelRendererWrapper(modelCow, PartsData.PartType.TAIL));
        modelCow.head.addChild(new ModelRendererWrapper(modelCow, PartsData.PartType.EARS));
    }

    @Override
    public void onPreRenderPart(EntityCow entity, RenderPart tail, PartInfo info, double x, double y, double z) {
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-entity.renderYawOffset, 0F, 1F, 0F);
        GL11.glRotatef(180F, 0, 0, 1);
        GL11.glRotatef(180F, 0, 1, 0);

        switch (info.partType) {
            case TAIL: {
                GL11.glTranslatef(0, -1F, 0.6F);
                GL11.glScalef(0.85F, 0.85F, 0.85F);
                break;
            }
            case EARS: {
                GL11.glTranslatef(0, -2F, 0.6F);
                GL11.glScalef(0.85F, 0.85F, 0.85F);
                break;
            }
        }
    }
}
