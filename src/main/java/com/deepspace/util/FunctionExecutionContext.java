package com.deepspace.util;

/**
 * Thread-safe tracker that marks when a Minecraft function file is actively
 * being dispatched by {@link net.minecraft.server.ServerFunctionManager}.
 *
 * <p>Uses a {@link ThreadLocal} so that concurrent server threads running
 * different function files never interfere with each other's suppression
 * state — a requirement on multiplayer dedicated servers where multiple
 * players may trigger functions simultaneously on different tick threads.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@link #begin()} is called at the entry of
 *       {@code ServerFunctionManager#execute} (via mixin HEAD inject).</li>
 *   <li>{@link #end()} is called on every exit path, including exceptional
 *       ones (via mixin RETURN inject covering all return/throw paths).</li>
 *   <li>{@link #isActive()} is queried inside
 *       {@code CommandSourceStack#sendSuccess} to decide whether to suppress
 *       the "Executed X command(s) from function …" summary message.</li>
 * </ol>
 */
public final class FunctionExecutionContext {

    /** Per-thread active flag; defaults to {@code false}. */
    private static final ThreadLocal<Boolean> ACTIVE =
            ThreadLocal.withInitial(() -> false);

    // Non-instantiable utility class.
    private FunctionExecutionContext() {}

    /**
     * Mark the current thread as being inside a function execution.
     * Called at the HEAD of {@code ServerFunctionManager#execute}.
     */
    public static void begin() {
        ACTIVE.set(true);
    }

    /**
     * Clear the function-execution flag on the current thread.
     * Called at every RETURN point of {@code ServerFunctionManager#execute},
     * ensuring cleanup even when an exception propagates.
     */
    public static void end() {
        ACTIVE.set(false);
    }

    /**
     * @return {@code true} if the current thread is inside a function
     *         execution dispatch; {@code false} otherwise.
     */
    public static boolean isActive() {
        return ACTIVE.get();
    }
}
