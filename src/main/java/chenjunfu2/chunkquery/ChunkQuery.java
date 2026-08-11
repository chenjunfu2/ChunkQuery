package chenjunfu2.chunkquery;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

public class ChunkQuery implements ModInitializer
{
	public static final String MOD_ID = "chunkquery";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize()
	{
		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) ->
			{
        	    dispatcher.register(
        	        literal("chunkQuery")
					.then(CommandManager.argument("chunkPos", ColumnPosArgumentType.columnPos())
        	        	.executes(context -> executeQuery(context, ColumnPosArgumentType.getColumnPos(context, "chunkPos").toChunkPos(), false))
						.then(CommandManager.literal("tick")
							.executes(context -> executeQuery(context, ColumnPosArgumentType.getColumnPos(context, "chunkPos").toChunkPos(), true))
						)
					)
					.then(CommandManager.literal("help")
						.executes(context -> executeHelp(context))
					)
        	    );
        	}
		);
	}
	
	private static int executeHelp(CommandContext<ServerCommandSource> context)
	{
		ServerCommandSource source = context.getSource();
		source.sendFeedback(() -> Text.literal("Format: T[gameTick], C[chunkX,chunkZ] -> L[loadInfo(loadLevel)], E[entityStatus]"), false);
		
        return 1;
	}
	
	private static String getLevelString(ChunkHolder holder)
	{
		if(holder == null)
		{
			return "§4NaN§r";
		}
		else
		{
			int loadLevel = holder.getLevel();
			if(loadLevel <= 31)
			{
				return String.format("§aEntity(%d)§r", loadLevel);//绿
			}
			else if(loadLevel == 32)
			{
				return String.format("§bBlock(%d)§r", loadLevel);//蓝
			}
			else if(loadLevel == 33)
			{
				return String.format("§dFull(%d)§r", loadLevel);//紫
			}
			else if(loadLevel >= 34 && loadLevel <= 45)
			{
				String info;
				if(loadLevel == 34)
				{
					info = "InitLight";
				}
				else if(loadLevel == 35)
				{
					info = "Carvers";
				}
				else if(loadLevel == 36)
				{
					info = "Biomes";
				}
				else if(loadLevel >= 37 && loadLevel <= 44)
				{
					info = "Structure";
				}
				else//45
				{
					info = "Inaccessible";
				}

				return String.format("§7%s(%d)§r", info, loadLevel);//灰
			}
			else
			{
				return String.format("§cUnknown(%d)§r", loadLevel);//红
			}
		}
	}
	
	private static String getEntityStatusString(ServerEntityManager.Status entityStatus)
	{
		return switch (entityStatus)
		{
			case FRESH -> String.format("§7%s§r", entityStatus);//灰
			case PENDING -> String.format("§d%s§r", entityStatus);//紫
			case LOADED -> String.format("§a%s§r", entityStatus);//绿
			default -> String.format("§c%s§r", entityStatus);//红
		};
	}
	
	private static int executeQuery(CommandContext<ServerCommandSource> context, ChunkPos chunkPos, boolean showTick)
	{
        ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		
		ChunkHolder holder = world.getChunkManager().threadedAnvilChunkStorage.getCurrentChunkHolder(chunkPos.toLong());
		String level = getLevelString(holder);
		
		ServerEntityManager.Status entityStatus = world.entityManager.managedStatuses.get(chunkPos.toLong());
		String status = getEntityStatusString(entityStatus);
		
		if(showTick)
		{
			long tick = world.getTime();
			source.sendFeedback(() -> Text.literal(String.format("T[§e%d§r], C[§a%d, %d§r] -> L[%s], E[%s]", tick, chunkPos.x, chunkPos.z, level, status)), false);
		}
		else
		{
			source.sendFeedback(() -> Text.literal(String.format("C[§a%d, %d§r] -> L[%s], E[%s]", chunkPos.x, chunkPos.z, level, status)), false);
		}
		
        return 1;
    }
}
