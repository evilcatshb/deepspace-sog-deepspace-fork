package com.deepspace.mixin.factionfeedback;

import com.deepspace.util.FunctionExecutionContext;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Brackets every call to {@link ServerFunctionManager#execute} with the
 * {@link FunctionExecutionContext} begin / end lifecycle so that
 * {@link CommandSourceStackMixin} can detect whether a
 * {@code sendSuccess} call is part of a function-execution summary.
 *
 * <h3>Injection strategy</h3>
 * <ul>
 *   <li>{@code HEAD} — sets the context <em>before</em> any commands in the
 *       function file can emit feedback.</li>
 *   <li>{@code RETURN} — clears the context on <em>every</em> exit path.
 *       Mixin generates one injection site per {@code return} statement,
 *       which also covers paths where an exception is thrown and then caught
 *       internally, as is the case in vanilla's function dispatcher.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The context uses a {@link ThreadLocal}, so concurrent function executions
 * on separate server threads are fully independent.
 *
 * <h3>Method signature note</h3>
 * In Forge 1.20.1 (MCP / Parchment mappings) the target method is:
 * <pre>
 *   public int execute(CommandSourceStack source, ResourceLocation id)
 * </pre>
 * The Mixin framework resolves the obfuscated name at load time via the
 * refmap generated during the Forge build.
 */
@Mixin(ServerFunctionManager.class)
public class ServerFunctionManagerMixin {

    /**
     * Sets the function-execution context flag before any commands in the
     * function file are dispatched.
     */
    @Inject(method = "execute", at = @At("HEAD"))
    private void deepspace$beginFunctionExecution(
            CommandFunction function,
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> cir) {
        FunctionExecutionContext.begin();
    }

    /**
     * Clears the function-execution context flag on every return path,
     * including early returns caused by internal error handling.
     *
     * <p>Because Mixin's {@code RETURN} injection covers all {@code return}
     * instructions in the method body, this provides exception-safe cleanup
     * without needing a try / finally wrapper.</p>
     */
    @Inject(method = "execute", at = @At("RETURN"))
    private void deepspace$endFunctionExecution(
            CommandFunction function,
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> cir) {
        FunctionExecutionContext.end();
    }
}
