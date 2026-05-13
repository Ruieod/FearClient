package fun.Fear.events.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fun.Fear.utils.client.managers.event.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}
