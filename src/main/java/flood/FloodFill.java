package flood;

import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockStateArgumentType.blockState;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FloodFill implements ModInitializer
{
	public static final String MOD_ID = "flood_fill";
	private static final int LIMIT = 32768;
	private static final Direction[] XY_PLANE = {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
	private static final Direction[] YZ_PLANE = {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};
	private static final Direction[] XZ_PLANE = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
	
	@Override
	public void onInitialize()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("flood").requires(src -> src.hasPermissionLevel(2)).then(argument("start", blockPos()).then(argument("state", blockState(registryAccess)).executes(context -> floodFill(context, BlockPosArgumentType.getBlockPos(context, "start"), null, null, BlockStateArgumentType.getBlockState(context, "state").getBlockState(), Direction.values())))));
			dispatcher.register(literal("floodbox").requires(src -> src.hasPermissionLevel(2)).then(argument("start", blockPos()).then(argument("min", blockPos()).then(argument("max", blockPos()).then(argument("state", blockState(registryAccess)).executes(context -> floodFill(context, BlockPosArgumentType.getBlockPos(context, "start"), BlockPosArgumentType.getBlockPos(context, "min"), BlockPosArgumentType.getBlockPos(context, "max"), BlockStateArgumentType.getBlockState(context, "state").getBlockState(), Direction.values())))))));
			dispatcher.register(literal("floodplane")
			.then(literal("xy").requires(src -> src.hasPermissionLevel(2)).then(argument("start", blockPos()).then(argument("state", blockState(registryAccess)).executes(context -> floodFill(context, BlockPosArgumentType.getBlockPos(context, "start"), null, null, BlockStateArgumentType.getBlockState(context, "state").getBlockState(), XY_PLANE)))))
			.then(literal("yz").requires(src -> src.hasPermissionLevel(2)).then(argument("start", blockPos()).then(argument("state", blockState(registryAccess)).executes(context -> floodFill(context, BlockPosArgumentType.getBlockPos(context, "start"), null, null, BlockStateArgumentType.getBlockState(context, "state").getBlockState(), YZ_PLANE)))))
			.then(literal("xz").requires(src -> src.hasPermissionLevel(2)).then(argument("start", blockPos()).then(argument("state", blockState(registryAccess)).executes(context -> floodFill(context, BlockPosArgumentType.getBlockPos(context, "start"), null, null, BlockStateArgumentType.getBlockState(context, "state").getBlockState(), XZ_PLANE))))));
		});
	}
	
	public static int floodFill(CommandContext<ServerCommandSource> context, BlockPos startPos, BlockPos min, BlockPos max, BlockState blockState, Direction ... fillDirections)
	{
		ServerWorld world = context.getSource().getWorld();
		BlockState targetState = world.getBlockState(startPos);
		BlockBox box = (min != null && max != null) ? BlockBox.create(min, max) : null;
		Deque<BlockPos> stack = new ArrayDeque<BlockPos>();
		Set<BlockPos> set = new HashSet<BlockPos>();
		stack.push(startPos);
		
		if(targetState == blockState)
		{
			context.getSource().sendFeedback(() -> Text.translatable("commands.flood.cancel"), false);
			return 0;
		}
		
		while(stack.size() > 0 && set.size() < LIMIT)
		{
			BlockPos blockPos = stack.pop();
			
			if(set.contains(blockPos) || (box != null && !box.contains(blockPos)))
				continue;
			
			if(world.getBlockState(blockPos) == targetState)
			{
				set.add(blockPos);
				
				for(Direction direction : fillDirections)
				{
					BlockPos offset = blockPos.offset(direction);
					stack.push(offset);
				}
			}
		}
		
		if(set.size() < LIMIT)
		{
			for(BlockPos blockPos : set)
				world.setBlockState(blockPos, blockState);
			
			context.getSource().sendFeedback(() -> Text.translatable("commands.flood.filled", set.size(), targetState.getBlock().getName(), blockState.getBlock().getName()), false);
			return set.size();
		}
		else
		{
			context.getSource().sendFeedback(() -> Text.translatable("commands.flood.limit", LIMIT), false);
			return 0;
		}
	}
}