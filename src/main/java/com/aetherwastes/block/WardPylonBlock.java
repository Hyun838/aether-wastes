package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Рунный столб оберега. Повреждается молниями Эфирной бури. */
public class WardPylonBlock extends DeviceBlock {
    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    public WardPylonBlock(Properties properties) {
        super(Devices.WARD, properties);
        registerDefaultState(stateDefinition.any().setValue(DAMAGED, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGED);
    }
}
