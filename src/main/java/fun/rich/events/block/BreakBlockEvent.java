package fun.Fear.events.block;

import net.minecraft.util.math.BlockPos;
import fun.Fear.utils.client.managers.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {}
