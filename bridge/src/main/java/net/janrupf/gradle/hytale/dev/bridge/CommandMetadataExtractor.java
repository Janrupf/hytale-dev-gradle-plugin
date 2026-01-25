package net.janrupf.gradle.hytale.dev.bridge;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.arguments.system.AbstractOptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.ArgumentInfo;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.CommandInfo;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.CommandRegistryResponse;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

/**
 * Extracts command metadata from Hytale's CommandManager and converts
 * it to protobuf format for IDE consumption.
 */
public class CommandMetadataExtractor {
    private static final CommandMetadataExtractor INSTANCE = new CommandMetadataExtractor();
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static CommandMetadataExtractor getInstance() {
        return INSTANCE;
    }

    private CommandMetadataExtractor() {
    }

    /**
     * Extract full command registry from CommandManager.
     *
     * @return the command registry response containing all registered commands
     */
    public CommandRegistryResponse extractFullRegistry() {
        CommandManager manager = CommandManager.get();
        if (manager == null) {
            LOGGER.at(Level.WARNING).log("CommandManager not available");
            return CommandRegistryResponse.getDefaultInstance();
        }

        Map<String, AbstractCommand> commands = manager.getCommandRegistration();
        CommandRegistryResponse.Builder builder = CommandRegistryResponse.newBuilder();

        for (AbstractCommand cmd : commands.values()) {
            try {
                CommandInfo info = extractCommand(cmd);
                if (info != null) {
                    builder.addCommands(info);
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).withCause(e).log("Failed to extract command: %s", cmd.getName());
            }
        }

        LOGGER.at(Level.FINE).log("Extracted %d commands", builder.getCommandsCount());
        return builder.build();
    }

    /**
     * Extract a single command including subcommands.
     */
    private CommandInfo extractCommand(AbstractCommand cmd) {
        String name = cmd.getName();
        if (name == null || name.isEmpty()) {
            return null; // Skip variant commands without names
        }

        CommandInfo.Builder builder = CommandInfo.newBuilder()
                .setName(name)
                .setDescription(cmd.getDescription() != null ? cmd.getDescription() : "");

        // Aliases
        for (String alias : cmd.getAliases()) {
            builder.addAliases(alias);
        }

        // Permission
        String permission = cmd.getPermission();
        if (permission != null) {
            builder.setPermission(permission);
        }

        // Required arguments
        for (RequiredArg<?> arg : cmd.getRequiredArguments()) {
            builder.addRequiredArgs(extractRequiredArgument(arg));
        }

        // Optional arguments (via reflection since the field is private)
        extractOptionalArguments(cmd, builder);

        // Subcommands (recursive)
        for (AbstractCommand sub : cmd.getSubCommands().values()) {
            CommandInfo subInfo = extractCommand(sub);
            if (subInfo != null) {
                builder.addSubCommands(subInfo);
            }
        }

        return builder.build();
    }

    /**
     * Extract optional arguments using reflection.
     */
    private void extractOptionalArguments(AbstractCommand cmd, CommandInfo.Builder builder) {
        try {
            Field field = AbstractCommand.class.getDeclaredField("optionalArguments");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, AbstractOptionalArg<?, ?>> optArgs =
                    (Map<String, AbstractOptionalArg<?, ?>>) field.get(cmd);

            for (AbstractOptionalArg<?, ?> arg : optArgs.values()) {
                builder.addOptionalArgs(extractOptionalArgument(arg));
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("Failed to extract optional arguments for %s", cmd.getName());
        }
    }

    /**
     * Extract required argument metadata.
     */
    private ArgumentInfo extractRequiredArgument(RequiredArg<?> arg) {
        ArgumentType<?> type = arg.getArgumentType();

        ArgumentInfo.Builder builder = ArgumentInfo.newBuilder()
                .setName(arg.getName())
                .setDescription(arg.getDescription() != null ? arg.getDescription() : "")
                .setTypeName(getTypeName(type))
                .setIsList(type.isListArgument())
                .setIsRequired(true);

        // Add examples from type
        for (String example : type.getExamples()) {
            builder.addExamples(example);
        }

        return builder.build();
    }

    /**
     * Extract optional argument metadata.
     */
    private ArgumentInfo extractOptionalArgument(AbstractOptionalArg<?, ?> arg) {
        ArgumentType<?> type = arg.getArgumentType();

        ArgumentInfo.Builder builder = ArgumentInfo.newBuilder()
                .setName(arg.getName())
                .setDescription(arg.getDescription() != null ? arg.getDescription() : "")
                .setTypeName(getTypeName(type))
                .setIsList(type.isListArgument())
                .setIsRequired(false);

        // Add examples from type
        for (String example : type.getExamples()) {
            builder.addExamples(example);
        }

        return builder.build();
    }

    /**
     * Get a readable type name from ArgumentType.
     */
    private String getTypeName(ArgumentType<?> type) {
        try {
            return type.getName().toString();
        } catch (Exception e) {
            return type.getClass().getSimpleName()
                    .replace("ArgumentType", "")
                    .replace("Argument", "");
        }
    }
}
