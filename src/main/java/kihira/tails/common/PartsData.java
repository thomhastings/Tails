/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014
 *
 * See LICENSE for full License
 */

package kihira.tails.common;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.UUID;

public class PartsData {

    @Expose public final UUID uuid;
    //Keeping these as fields vs a map should prove better performance wise
    @Expose private PartInfo tailInfo;
    @Expose private PartInfo earsInfo;
    @Expose private PartInfo wingsInfo;

    public PartsData(UUID uuid) {
        this.uuid = uuid;
    }

    public void setPartInfo(PartType partType, PartInfo partInfo) {
        switch (partType) {
            case EARS: {
                earsInfo = partInfo;
                break;
            }
            case TAIL: {
                tailInfo = partInfo;
                break;
            }
            case WINGS: {
                wingsInfo = partInfo;
                break;
            }
        }
    }

    public PartInfo getPartInfo(PartType partType) {
        switch (partType) {
            case EARS: {
                return earsInfo;
            }
            case TAIL: {
                return tailInfo;
            }
            case WINGS: {
                return wingsInfo;
            }
        }
        return null;
    }

    public boolean hasPartInfo(PartType partType) {
        PartInfo partInfo = null;
        switch (partType) {
            case EARS: {
                partInfo = earsInfo;
                break;
            }
            case TAIL: {
                partInfo = tailInfo;
                break;
            }
            case WINGS: {
                partInfo = wingsInfo;
                break;
            }
        }
        return partInfo != null && partInfo.hasPart;
    }

    public void clearTextures() {
        if (earsInfo != null) earsInfo.setTexture(null);
        if (tailInfo != null) tailInfo.setTexture(null);
        if (wingsInfo != null) wingsInfo.setTexture(null);
    }

    public PartsData deepCopy() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), PartsData.class);
    }

    public enum PartType {
        EARS,
        TAIL,
        WINGS
    }
}
