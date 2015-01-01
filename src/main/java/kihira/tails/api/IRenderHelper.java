/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014
 *
 * See LICENSE for full License
 */

package kihira.tails.api;

import kihira.tails.client.render.RenderPart;
import kihira.tails.common.PartInfo;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLivingBase;

public interface IRenderHelper<T extends EntityLivingBase> {

    public void addModelHelpers(RenderLiving renderLiving);

    public void onPreRenderPart(T entity, RenderPart tail, PartInfo info, double x, double y, double z);

}
