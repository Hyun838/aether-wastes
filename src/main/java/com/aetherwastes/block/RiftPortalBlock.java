package com.aetherwastes.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Общие свойства блока Разлома. */
public final class RiftPortalBlock {
    /** true — выход из арены обратно в мир. */
    public static final BooleanProperty EXIT = BooleanProperty.create("exit");

    private RiftPortalBlock() {}
}
