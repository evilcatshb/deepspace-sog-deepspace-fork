package com.deepspace.mixin.factionfeedback;

import com.deepspace.util.FunctionExecutionContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla function-execution summary message:
 * <pre>
 *   Executed X command(s) from function 'namespace:path'
 * </pre>
 * with surgical precision — every other {@code sendSuccess} call is
 * passed through unchanged.
 *
 * <h3>Why this message is sensitive</h3>
 * In the Deep Space faction system, function paths such as
 * {@code factions:raid/cleanup_pirate} encode gameplay-critical metadata.
 * Broadcasting these paths to players would expose raid types, faction
 * identifiers, and scripted event names, breaking both immersion and
 * competitive balance.
 *
 * <h3>Filtering criteria — ALL must be true to suppress</h3>
 * <ol>
 *   <li>{@link FunctionExecutionContext#isActive()} returns {@code true} —
 *       the current thread is inside a {@code ServerFunctionManager#execute}
 *       call.</li>
 *   <li>The message string starts with {@code "Executed"} — matches the
 *       vanilla translation key {@code commands.function.success}.</li>
 *   <li>The message string contains {@code "command"} — disambiguates from
 *       any other message that could start with "Executed".</li>
 *   <li>The message string contains {@code "from function"} — the unique
 *       substring that identifies this specific feedback type.</li>
 * </ol>
 *
 * <p>Any message that does not meet every one of these conditions — including
 * {@code /help}, {@code /data}, {@code /tag}, {@code /trigger}, {@code /say},
 * and all command-block output — is passed through to the player normally.</p>
 *
 * <h3>Thread safety</h3>
 * The context check reads from a {@link ThreadLocal}, so suppression on one
 * server thread never bleeds into a different player's command pipeline.
 */
@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin {

    /**
     * Intercepts {@link CommandSourceStack#sendSuccess} before any output is
     * emitted and cancels the callback if — and only if — the message matches
     * the function-execution summary pattern while a function is active on
     * this thread.
     *
     * @param message         the feedback component to be sent
     * @param broadcastToOps  whether operators should also receive the message
     * @param ci              mixin callback info; {@link CallbackInfo#cancel()}
     *                        prevents the message from being sent
     */
    @Inject(
            method = "sendSuccess(Ljava/util/function/Supplier;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void deepspace$suppressFunctionSummary(
            java.util.function.Supplier<Component> messageSupplier,
            boolean broadcastToOps,
            CallbackInfo ci) {

        // Fast path: not inside a function execution — never suppress.
        if (!FunctionExecutionContext.isActive()) return;

        // Resolve the component only when context is active, keeping the
        // hot path (normal commands) free of any string allocation.
        String text = messageSupplier.get().getString();

        // All three substrings must be present to match the vanilla message:
        //   "Executed 14 command(s) from function 'factions:raid/cleanup_pirate'"
        if (text.startsWith("Executed")
                && text.contains("command")
                && text.contains("from function")) {
            ci.cancel();
        }
    }
}
