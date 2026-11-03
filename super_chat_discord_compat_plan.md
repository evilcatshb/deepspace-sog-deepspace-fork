# Super Chat + BFCR + Discord Integration Compatibility Plan

## Revision status

Revision: exhaustive code-level architecture revision
Verification date: 2026-10-09
Target Minecraft: 1.20.1
Target Forge: 47.4.3
Target Java: 17
DeepSpace source: C:\Users\henry\Desktop\minecraft_mods_source\deepspace
Server data: C:\Users\henry\Desktop\deepspace_1.0.0\data

This document is the implementation contract. It is not permission to implement the code yet.

The plan was re-verified against the actual installed JARs, Forge EventBus 6.0.5, Mixin 0.8.5, the current DeepSpace source tree, build.gradle, deepspace.mixins.json, and the live BFCR config.

IMPORTANT CORRECTION FROM THE PREVIOUS PLAN:
Super Chat does call PlayerList's broadcastSystemMessage-equivalent method, but the actual installed bytecode invocation is the SRG method:
net.minecraft.server.players.PlayerList.m_240416_(Component, boolean)
The earlier prose saying only that the invocation was a named broadcastSystemMessage call was conceptually correct but insufficiently precise for implementation. The exact runtime target above is now locked.

A second architectural correction is required:
same-priority Super Chat/BFCR ordering must NOT be relied upon. DeepSpace must deterministically run the verified Super Chat handler first at a higher Forge EventPriority and let its normal-priority registered handler be skipped after cancellation.

No Minecraft client or server is to be started or stopped while implementing this plan unless separately authorized.

## Current installed-state fact

The server currently contains:
super_chat-3.1.0-forge-1.20.1.disabled

Therefore Super Chat is currently disabled on the server. This plan does NOT enable it or modify the server mods directory.

BFCR is currently enabled:
bfcrmod-1.20.1-3.0.0.jar

Discord Integration is currently enabled:
dcintegration-forge-3.0.7.1-1.20.1.jar

The implementation/runtime test phase must explicitly account for the fact that Super Chat must eventually be enabled for the full feature to be exercised. Do not enable it as part of plan preparation.

## Locked deliverables

1. BFCR remains installed and enabled.
2. BFCR remains authoritative for BFCR configuration.
3. BFCR config must not be disabled, rewritten, or bypassed.
4. Super Chat remains responsible for the complete Minecraft rich chat Component.
5. Super Chat's clickable username must remain intact.
6. /msg click behavior must remain intact.
7. /tpa click behavior must remain intact.
8. Mentions must remain intact.
9. Hover actions must remain intact.
10. LuckPerms prefix and suffix must remain intact.
11. Super Chat filtering must remain intact.
12. Super Chat logging must remain intact.
13. Super Chat cancellation must remain intact.
14. Exactly one Minecraft chat broadcast must occur.
15. Discord Integration must receive the logical chat through its existing chat path.
16. DeepSpace must not implement Discord formatting or sending.
17. DeepSpace must not hard-depend on BFCR, Super Chat, or Discord Integration.
18. Missing optional mods must not cause classloading failures.
19. Unsupported BFCR presentation formats must fail closed.
20. No third-party JAR may be modified.
21. Existing DeepSpace files must receive .BAK backups before modification.
22. Every class, method, field, descriptor, annotation, target, mod ID, and API used by the implementation must be verified from the actual installed artifact or the exact Forge/Mixin dependency used by this project.
23. No guessed syntax is permitted.

## Required final Minecraft result

With the current BFCR configuration:

chatMessageFormat = "$time | $name: $msg"
chatMessageColor = "WHITE"
timestampFormat = "HH:mm"
enableTimestamp = true

and Super Chat producing the rich component equivalent to:

[HV] evilcats: test

the single Minecraft broadcast must display:

01:14 | [HV] evilcats: test

The [HV] evilcats: test component tree must remain the original Super Chat component tree. DeepSpace may add a parent/wrapper component around it, but must not reconstruct it from plain text.

## Verified Super Chat artifact

Exact installed artifact:
C:\Users\henry\Desktop\deepspace_1.0.0\data\mods\super_chat-3.1.0-forge-1.20.1.disabled

The JAR itself was inspected despite its .disabled filename.

mods.toml verified:
modId = "super_chat"
version = "3.1.0"
loaderVersion = "[47,)"
minecraft dependency is mandatory and ordered AFTER.

The JAR contains:
net/mcreator/superchat/command/ChatInteractionHandler.class

Exact handler:
public static void onServerChat(ServerChatEvent)

Exact descriptor:
(Lnet/minecraftforge/event/ServerChatEvent;)V

The class has @Mod.EventBusSubscriber.
The handler has @SubscribeEvent with no explicit priority or receiveCanceled attributes.

Therefore the handler's effective defaults are:
priority = NORMAL
receiveCanceled = false

These defaults were verified against the actual Forge EventBus 6.0.5 SubscribeEvent annotation, not assumed.

## Verified Super Chat handler bytecode

The handler sequence is:

1. event.getPlayer()
2. null-player guard
3. mute check
4. if muted: send mute message, cancel event, return
5. obtain /msg and /tpa command templates
6. read configured click command values when the Super Chat config is loaded
7. obtain player name
8. replace command placeholders
9. event.getRawText()
10. applyWordFilter(rawMessage)
11. determine mentionsEnabled
12. collect mentioned online players
13. get LuckPerms prefix
14. get LuckPerms suffix
15. create header MutableComponent
16. append prefix
17. append nameLabel containing player name and prefix/suffix
18. build body with buildMessageWithMentions(...)
19. construct finalMsg by appending header and body
20. invoke PlayerList.m_240416_(Component, boolean)
21. notifyMentions(...)
22. event.setCanceled(true)
23. ChatLogger.log(...)
24. return

The normal-path broadcast invocation is exactly one invocation.

The mute-path send is ServerPlayer.m_5661_(Component, boolean), not PlayerList.m_240416_.

Therefore the timestamp Mixin MUST target PlayerList.m_240416_ only. It must not touch the mute message path.

## Exact Super Chat broadcast target

Verified bytecode:

Minecraft runtime class:
net.minecraft.server.players.PlayerList

Exact runtime method reference:
m_240416_

Descriptor:
(Lnet/minecraft/network/chat/Component;Z)V

Invocation inside:
net.mcreator.superchat.command.ChatInteractionHandler.onServerChat(ServerChatEvent)

The invocation is:
PlayerList.m_240416_(finalMsg, false)

There is exactly one invocation of this PlayerList method in the normal Super Chat handler.

This makes an ordinal unnecessary. The implementation should use the verified target descriptor without inventing an ordinal.

Because the target is a third-party compiled class and the installed bytecode contains the SRG name, the Mixin target must be verified and written with the exact runtime name and descriptor. The implementation must not substitute a guessed MCP name.

## Exact timestamp Mixin

Recommended class:
com.deepspace.mixin.SuperChatTimestampMixin

Target:
net.mcreator.superchat.command.ChatInteractionHandler

The Mixin should modify only the Component argument at the verified PlayerList.m_240416_ invocation.

Preferred injection:
@ModifyArg

Verified Mixin 0.8.5 API:
org.spongepowered.asm.mixin.injection.ModifyArg

Callback shape:
private static Component <verifiedMethodName>(Component original)

The ModifyArg must target:
method = "onServerChat"

At:
value = "INVOKE"

Target:
Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V

Index:
0

remap:
false for this third-party target, because the installed third-party bytecode target is already the runtime SRG member name.

The callback must return:
- original Component when BFCR is absent;
- original Component when BFCR is disabled for timestamping;
- original Component when BFCR config cannot be read safely;
- original Component when the BFCR presentation is unsupported;
- wrapped Component only when the exact compatibility analyzer proves the wrapper is semantically valid.

The callback must NEVER:
- call m_240416_ itself;
- call broadcastSystemMessage itself;
- call Super Chat's handler;
- flatten the Component;
- call original.getString() to reconstruct chat;
- rebuild the username;
- rebuild the message;
- modify click events;
- modify hover events;
- remove mentions.

## Exact timestamp wrapper requirement

The wrapper is not a second message.

Conceptually:

MutableComponent wrapper = verified BFCR-compatible prefix component;
wrapper.append(originalSuperChatComponent);

The original Super Chat Component is appended as a child unchanged.

The implementation must not mutate the original Component object unless bytecode verification proves that the exact Minecraft append operation itself is safe and does not rewrite the original tree. Prefer creating a fresh parent component.

## Critical BFCR-format semantic rule

The safe current format is:

$time | $name: $msg

The desired output proves that the original Super Chat Component represents the entire BFCR logical $name: $msg portion.

Therefore the compatibility transform for this format is NOT:

$time | [BFCR raw name] : + [Super Chat Component]

because that would duplicate the name.

Instead it is:

[BFCR-formatted $time + literal " | "] + [original Super Chat Component]

The original Super Chat Component already contains:
[HV] evilcats: test

Therefore the resulting tree is:

[01:14 | ] + [original Super Chat tree]

This is the key semantic rule that must be encoded in the compatibility analyzer.

## Why $name cannot be independently rendered

BFCR's own handler obtains:
BetterForgeChatUtilities.getRawPreferredPlayerName(GameProfile)

and substitutes that value into chatMessageFormat.

Super Chat independently constructs a richer name area using:
LuckPerms prefix
player name
LuckPerms suffix
click event
hover event

The two representations are not equivalent.

DeepSpace must therefore never create a BFCR $name Component independently when Super Chat owns the final name.

For the safe current format, $name is represented by the original Super Chat component's existing name/body structure.

## BFCR format compatibility model

Do not promise arbitrary chatMessageFormat strings.

The analyzer must prove that a configured BFCR format can be represented as:

[BFCR-owned compatible outer presentation] + [untouched Super Chat component]

The analyzer must fail closed if the requested format requires moving or inserting BFCR presentation inside the Super Chat component.

Examples that are inherently unsafe unless an exact structural proof exists:
$name [something] $time: $msg
$msg -- $time
$time $msg $name
$name $time $msg
formats that require duplicate $msg handling
formats that require independently styled $name
formats that require modifying the Super Chat prefix/suffix
formats that require applying BFCR formatting to the Super Chat body

BFCR itself documents that using $msg more than once breaks its formatter. DeepSpace must not attempt to improve or reinterpret that behavior.

## Current-format compatibility contract

For the exact current format:

$time | $name: $msg

the analyzer must recognize the structural pattern:
- exactly one $time;
- exactly one $name;
- exactly one $msg;
- $name immediately belongs to the name/message segment that Super Chat already supplies;
- no required content occurs after $msg;
- the only additional outer content before the Super Chat component is the timestamp and separator.

The exact separator is:
" | "

The final name/body punctuation remains owned by Super Chat.

If a later BFCR format differs, the analyzer must independently prove the transformation before enabling it.

If proof fails, return original Component unchanged and emit one warning.

## BFCR artifact

Exact installed artifact:
C:\Users\henry\Desktop\deepspace_1.0.0\data\mods\bfcrmod-1.20.1-3.0.0.jar

mods.toml verified:
modId = "bfcrmod"
artifact metadata version field is "1.0.3" even though the filename is 3.0.0.
Do not treat the filename as the mod metadata version.

The BFCR source class is:
com.jeremiahbl.bfcrmod.events.ChatEventHandler

Exact handler:
public void onServerChat(ServerChatEvent)

Descriptor:
(Lnet/minecraftforge/event/ServerChatEvent;)V

The class has @Mod.EventBusSubscriber.
The handler has @SubscribeEvent with default NORMAL priority and receiveCanceled=false.

## BFCR live config fields

Verified bytecode in ChatEventHandler.reloadConfigOptions() accesses:

ConfigHandler.config.enableTimestamp
ConfigHandler.config.timestampFormat
ConfigHandler.config.enableMarkdown
ConfigHandler.config.chatMessageFormat
ConfigHandler.config.chatMessageColor

Each is a:
net.minecraftforge.common.ForgeConfigSpec.ConfigValue

The getter is:
ConfigValue.get()

The exact containing class is:
com.jeremiahbl.bfcrmod.config.ConfigHandler$ConfigBuilder

The static config holder is:
com.jeremiahbl.bfcrmod.config.ConfigHandler.config

These exact names are now verified against the installed JAR.

The TOML key is different for Markdown:
markdownEnabled

The ConfigBuilder field is:
enableMarkdown

Do not confuse those two names.

## BFCR timestamp semantics

BFCR reloadConfigOptions() does exactly:

loaded = false

if ConfigHandler.config.enableTimestamp.get() is true:
    timestampFormat = new SimpleDateFormat(
        ConfigHandler.config.timestampFormat.get()
    )
else:
    timestampFormat = null

markdownEnabled is read separately.

chatMessageFormat is read from ConfigHandler.config.chatMessageFormat.get()

chatMessageColor is read from ConfigHandler.config.chatMessageColor.get()

loaded is then set true.

Therefore timestamp enablement is represented by whether timestampFormat is null after BFCR reload.

DeepSpace should nevertheless read the live enableTimestamp ConfigValue itself so that compatibility state is explicit and does not depend on a private BFCR implementation field.

## BFCR timestamp formatting

BFCR uses:
new java.text.SimpleDateFormat(configuredPattern)

and:
timestampFormat.format(new java.util.Date())

DeepSpace must use the exact same SimpleDateFormat pattern semantics.

Do not hard-code HH:mm.

Current:
HH:mm

Example:
01:14

If BFCR changes to:
HH:mm:ss

DeepSpace must use:
01:14:37

The bridge must not introduce its own timezone conversion.

## BFCR text formatting

Verified class:
com.jeremiahbl.bfcrmod.TextFormatter

Verified public method:
public static MutableComponent stringToFormattedText(String)

Descriptor:
(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;

The one-argument method delegates to:
stringToFormattedText(String, boolean, boolean)
with both booleans true.

The formatter:
- starts with ChatFormatting.WHITE;
- reads ChatEventHandler.getChatMessageColor();
- resolves the configured color;
- parses & color/style codes;
- constructs MutableComponents;
- uses BitwiseStyling.

Therefore if the BFCR-compatible outer wrapper contains BFCR-formatted text, use the verified TextFormatter method rather than inventing a second color parser.

The wrapper must never pass the original Super Chat Component through TextFormatter.

Only the newly created outer text is BFCR-formatted.

## BFCR color rule

Current:
chatMessageColor = "WHITE"

For current configuration, the outer timestamp prefix should use BFCR's verified TextFormatter semantics.

Do not call TextFormatter on the Super Chat Component.

If reflection into TextFormatter fails, fail closed for the BFCR wrapper rather than silently applying a different styling implementation.

## BFCR Markdown rule

Do not process Markdown in DeepSpace before Super Chat.

Super Chat must receive the raw event first because its own filtering, mentions, click actions, and component construction must remain authoritative.

The compatibility order is:

raw ServerChatEvent
-> Super Chat processing
-> Super Chat rich Component
-> DeepSpace BFCR outer wrapper

not:

raw ServerChatEvent
-> DeepSpace Markdown
-> Super Chat

DeepSpace must not duplicate BFCR's raw-message Markdown transformation.

## BFCR runtime reflection design

DeepSpace cannot compile-time depend on BFCR.

The implementation may use reflection only after:
ModList.isLoaded("bfcrmod") is verified true.

Reflection targets:
com.jeremiahbl.bfcrmod.config.ConfigHandler
com.jeremiahbl.bfcrmod.config.ConfigHandler$ConfigBuilder
net.minecraftforge.common.ForgeConfigSpec$ConfigValue
com.jeremiahbl.bfcrmod.TextFormatter
com.jeremiahbl.bfcrmod.events.ChatEventHandler

All names above have been verified against the exact JAR.

Preferred live access:
ConfigHandler.config
 -> ConfigBuilder field
 -> ConfigValue.get()

Do not parse bfcrmod-common.toml as the authoritative implementation path.

The TOML file is useful for verification and troubleshooting only.

## Current live BFCR config

Exact file:
C:\Users\henry\Desktop\deepspace_1.0.0\data\config\bfcrmod-common.toml

Verified current values:

chatMessageFormat = "$time | $name: $msg"
chatMessageColor = "WHITE"
timestampFormat = "HH:mm"
enableTimestamp = true
markdownEnabled = true
enableDiscordIntegraation = false

The existing BFCR config must not be changed by this compatibility implementation.

## Deterministic event-order solution

This is the most important architecture correction.

Do NOT depend on the registration order of the two existing NORMAL-priority handlers.

Forge EventBus 6.0.5 was inspected directly.

@SubscribeEvent defaults:
priority = NORMAL
receiveCanceled = false

EventPriority order is:
HIGHEST
HIGH
NORMAL
LOW
LOWEST
Listeners are added to the priority list in registration order.

This proves why same-priority ordering cannot be used.

DeepSpace HIGHEST orchestration is therefore mandatory for deterministic behavior.

## No modification to BFCR

Do not:
- Mixin BFCR handler priority;
- Mixin BFCR cancellation;
- alter BFCR @SubscribeEvent annotation;
- suppress BFCR's event handler;
- edit BFCR JAR;
- edit BFCR config.

The deterministic solution belongs entirely in DeepSpace.

## No modification to Super Chat

Do not edit Super Chat JAR.

Do not patch its handler.

Mixin only:
- changes the Component argument at the verified broadcast call;
- invokes Discord Integration at the verified post-broadcast point.

The actual Super Chat handler remains responsible for its own behavior.

## No modification to Discord Integration

Do not edit DI JAR.

Do not patch its chat method.

Reflection invokes its existing public method.

## Required source-level comments

Every new compatibility class should contain comments documenting:
- exact third-party artifact/version;
- exact verified target class;
- exact verified method descriptor;
- why HIGHEST orchestration exists;
- why same-priority ordering is not used;
- why the original Super Chat Component is preserved;
- why BFCR $name is not independently reconstructed;
- why Discord is invoked before Super Chat cancellation.

Comments must describe verified facts, not speculation.

## Required unit-style pure logic tests

Test BFCR format analyzer independently of Minecraft runtime where possible.

At minimum:
1. "$time | $name: $msg" -> SUPPORTED
2. "$time $name: $msg" -> SUPPORTED only if exact transform is implemented and verified
3. "$name: $msg $time" -> UNSUPPORTED
4. "$msg $time" -> UNSUPPORTED
5. "$time | $name: $msg $msg" -> UNSUPPORTED
6. "$time | $msg" -> UNSUPPORTED unless explicitly supported as an exact transform
7. missing $msg -> UNSUPPORTED

Do not mark additional formats supported merely because they look visually plausible.

## Required Component-preservation test

Construct a test Component containing:
- a click event;
- a hover event;
- nested styled children;
- a mention-like styled child.

Wrap it using the same Component wrapper method.

Assert the original subtree remains structurally intact.

The wrapper must only add an outer prefix component.

## Required reflection tests

BFCR:
- class resolves;
- ConfigHandler.config resolves;
- ConfigValue fields resolve;
- get() resolves;
- TextFormatter.stringToFormattedText(String) resolves;
- ChatEventHandler.getChatMessageColor() resolves.

Super Chat:
- handler resolves;
- handler is static;
- descriptor is exact.

DI:
- mod object resolves;
- class name is exact;
- chat method is public instance;
- descriptor is exact.

## Required build assertions

Build must finish with exit code 0.

No new Java compilation errors.

No new Mixin annotation-processor errors.

No unresolved refmap target errors.

No missing optional-class verification failures.

No classloading references that force absent optional mods.

## Required log assertions after authorized runtime test

Server log must show:
- no new Super Chat Mixin target failure;
- no BFCR reflection failure;
- no Discord reflection failure;
- no duplicate broadcast symptoms;
- no new compatibility stack traces.

Existing DI runtime errors must be compared with the pre-change baseline.

## Deployment artifact

The final built artifact is:
build/libs/deepspace-1.0.0.jar

Do not copy it to the server until explicitly authorized.

Before deployment:
- compare SHA-256;
- verify JAR contents;
- preserve a backup of the currently installed deepspace JAR.

## Server backup requirement

Current installed DeepSpace JAR:
C:\Users\henry\Desktop\deepspace_1.0.0\data\mods\deepspace-1.0.0.jar

Before replacing it:
create a timestamped .BAK.

Do not delete or overwrite prior backups.

## Super Chat activation requirement

Current server file:
super_chat-3.1.0-forge-1.20.1.disabled

For a full runtime test, a later authorized deployment step must make it active as:
super_chat-3.1.0-forge-1.20.1.jar

This plan does not perform that activation.

The exact filename must be verified before any deployment.

## No client requirement for Super Chat feature

Super Chat's mods.toml declares BOTH sides for its Minecraft dependency but the chat handler is server-side.

The compatibility feature is server-side.

Do not assume a client-side Super Chat JAR is required for this server chat behavior.

## Current build compatibility

DeepSpace currently uses official 1.20.1 mappings and Forge 47.4.3.

Third-party installed bytecode contains SRG runtime names.

Mixin target strings must match the exact runtime target used by the installed JAR and generated refmap.

Do not substitute names from a different mappings channel/version.

## Exact source naming rule

Use:
com.deepspace

not:
com.deepspace.mixin.compat
unless the mixin package configuration is updated and verified.

Existing source and mixin configuration both prove com.deepspace is the root.

## Implementation pseudocode contract

The HIGHEST event handler is logically:

onServerChat(event):
    if !ModList.get().isLoaded("super_chat"):
        return
    if event.isCanceled():
        return
    method = resolveSuperChatHandler()
    if method == null:
        warnOnce()
        return
    method.invoke(null, event)

No other behavior belongs in this method.

## Timestamp Mixin pseudocode contract

modifyBroadcastComponent(original):
    state = bfcrAdapter.readLiveState()
    if state is not timestamp-enabled:
        return original
    format = state.chatMessageFormat
    compatibility = analyze(format)
    if compatibility is unsupported:
        warnOnce()
        return original
    timestamp = formatWithSimpleDateFormat(state.timestampFormat, new Date())
    outer = BFCR TextFormatter stringToFormattedText(verified outer text)
    outer.append(original)
    return outer

The exact implementation must not invent a placeholder substitution that duplicates Super Chat's name.

## Discord Mixin pseudocode contract

afterSuperChatBroadcast(event):
    if !ModList.get().isLoaded("dcintegration"):
        return
    bridge.invokeChat(event)

No broadcast.
No event cancellation.
No message reconstruction.

## Required current-format expected tree

For:
timestamp = "01:14"
format = "$time | $name: $msg"

Expected outer text:
"01:14 | "

Expected child:
the exact original Super Chat finalMsg Component.

Expected logical string:
"01:14 | [HV] evilcats: test"

The original child must retain:
username click;
username hover;
prefix;
suffix;
body styles;
mentions.

## Unsupported-format example

For:
$name | $time: $msg

Do not automatically output:
[HV] evilcats | 01:14: test

unless a structural mapping can preserve the exact Super Chat tree.

Because this requires the timestamp to be inserted into the name/message relationship, it should be UNSUPPORTED under the conservative current analyzer.

Result:
Super Chat's original output remains unchanged.
One warning is emitted.

## Conservative compatibility philosophy

It is better to preserve Super Chat's exact rich behavior and omit a BFCR presentation modification than to produce an approximate string that destroys interactive chat features.

Unsupported BFCR presentation must never justify flattening Super Chat.

## Final exact event flow

ServerChatEvent
    |
    v
DeepSpace HIGHEST handler
    |
    |-- Super Chat absent -> return
    |
    v
invoke ChatInteractionHandler.onServerChat(event)
    |
    +-- mute handling
    +-- filtering
    +-- mentions
    +-- LuckPerms prefix/suffix
    +-- rich Component construction
    |
    v
ModifyArg at PlayerList.m_240416_(Component, boolean)
    |
    +-- BFCR absent/disabled/unsupported -> original Component
    |
    +-- supported -> BFCR timestamp outer wrapper + original Component
    |
    v
PlayerList.m_240416_(finalComponent, false)
    |
    | EXACTLY ONE MINECRAFT BROADCAST
    v
Discord Mixin AFTER m_240416_
    |
    +-- invoke DiscordIntegrationMod.chat(event)
    |
    v
Super Chat event.setCanceled(true)
    |
    v
Forge skips later NORMAL receiveCanceled=false listeners
    |
    +-- BFCR handler skipped
    +-- Super Chat's registered NORMAL handler skipped

## Definition of 100%-correct implementation

Do not call the implementation complete until all of these are true:

- exact JARs were inspected;
- exact class names were inspected;
- exact method names were inspected;
- exact descriptors were inspected;
- exact runtime m_240416_ target was inspected;
- exact event ordering was proven from EventBus 6.0.5;
- deterministic HIGHEST orchestration is implemented;
- BFCR remains enabled;
- BFCR config remains untouched;
- BFCR remains authoritative;
- live BFCR ConfigValue reads are used;
- current timestamp format is correct;
- current format produces timestamp + original Super Chat Component;
- original Component is not flattened;
- Super Chat interactions remain intact;
- exactly one Minecraft broadcast occurs;
- DI uses its existing chat method;
- DI does not create another Minecraft broadcast;
- optional mods remain optional;
- optional Mixins are conditionally gated;
- unsupported BFCR formats fail closed;
- warnings are emitted at most once per failure category;
- all modified DeepSpace files have backups;
- final JAR contains no bundled third-party classes;
- static dependency checks pass;
- Mixin refmap resolves correctly;
- build exits successfully;
- fresh runtime logs contain no new compatibility errors;
- the existing DI baseline error state is distinguished from new errors;
- no Minecraft client/server was started or stopped without authorization.

## Absolute prohibitions

Never:
- disable BFCR;
- alter BFCR config;
- remove BFCR;
- alter Super Chat JAR;
- alter Discord Integration JAR;
- manually broadcast chat;
- flatten Super Chat Component;
- duplicate username/message rendering;
- invoke Discord's event-hook class as the sender;
- implement Discord formatting in DeepSpace;
- hard-code HH:mm;
- hard-code the current BFCR format as the only possible format without analyzer logic;
- guess Mixin targets;
- guess Forge APIs;
- guess mod IDs;
- depend on same-priority registration order;
- use an unverified ordinal;
- start or stop the game/server during build verification;
- silently approximate unsupported formatting.

## Final architecture statement

Super Chat owns Minecraft chat construction and the single Minecraft broadcast.

BFCR owns BFCR configuration semantics.

DeepSpace owns only the compatibility orchestration and the verified timestamp wrapper.

Discord Integration owns Minecraft-to-Discord delivery.

The deterministic invariant is:

HIGHEST DeepSpace orchestration
-> exact Super Chat handler
-> original rich Component
-> optional verified BFCR timestamp wrapper
-> exactly one PlayerList.m_240416_ broadcast
-> existing DiscordIntegrationMod.chat(event)
-> Super Chat cancellation
-> BFCR skipped because the event is canceled and BFCR does not receive canceled events

No third-party handler is disabled.
No third-party JAR is modified.
No rich Component is flattened.
No duplicate Minecraft message is created.
No guessed API is accepted.
## Final verification addendum — 2026-10-09

This addendum records the last bytecode/API checks performed after the main plan was written.

### PlayerList target cross-check

The exact Forge 47.4.3 mapped Minecraft JAR was inspected.

net.minecraft.server.players.PlayerList contains:

public void broadcastSystemMessage(Component, boolean)

and the installed Super Chat bytecode calls the corresponding runtime SRG member:

m_240416_(Component, boolean)

Therefore:
- source/mapped name = broadcastSystemMessage
- runtime third-party bytecode target = m_240416_
- descriptor = (Lnet/minecraft/network/chat/Component;Z)V

The Mixin target must be the runtime member used by the installed Super Chat class:
Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V

### Discord chat recursion cross-check

The exact DiscordIntegrationMod.chat(ServerChatEvent) method was isolated from the installed JAR and searched for:
- MinecraftForge.EVENT_BUS
- EventBus.post(...)
- PlayerList.broadcastSystemMessage
- ServerChatEvent.setMessage
- DiscordIntegration.sendMessage

Inside chat(...), the only relevant operations are Discord send operations and a late ServerChatEvent.setMessage(...) for Discord mention conversion.

No EVENT_BUS.post(...) occurs inside chat(...).
No PlayerList broadcast occurs inside chat(...).

Therefore direct invocation of:
DiscordIntegrationMod.chat(event)

does not itself create a second Minecraft chat broadcast and does not recursively repost ServerChatEvent.

### Discord registration cross-check

The installed DiscordIntegrationMod constructor bytecode was inspected.

It registers its instance on:
MinecraftForge.EVENT_BUS

Therefore the verified public instance:
DiscordIntegrationMod.chat(ServerChatEvent)

is the correct existing Forge chat path.

The compatibility bridge must not create a second DiscordIntegrationMod instance.

### ModList object cross-check

Forge 47.4.3 fmlcore was inspected.

ModList.getModObjectById(String) first obtains the ModContainer and maps it to the contained mod object.

Therefore:
ModList.get().getModObjectById("dcintegration")

is a verified API for obtaining the actual loaded DiscordIntegrationMod object without guessing a ModContainer field or singleton.

The exact mod ID is:
dcintegration

### BFCR cancellation cross-check

Forge EventBus 6.0.5 ASMEventHandler was inspected.

Its invoke logic is effectively:
if event.isCanceled() && !subscribeEvent.receiveCanceled():
    return
handler.invoke(event)

The installed BFCR ChatEventHandler.onServerChat annotation has no explicit receiveCanceled setting.

Forge's verified annotation default is:
receiveCanceled = false

Therefore Super Chat's successful cancellation prevents BFCR's normal listener from executing.

The same is true for Super Chat's own registered NORMAL listener after DeepSpace has already manually invoked the same method at HIGHEST.

### Super Chat cancellation cross-check

The installed Super Chat bytecode was re-read.

Normal path:
PlayerList.m_240416_(finalMsg, false)
notifyMentions(...)
event.setCanceled(true)
ChatLogger.log(...)

Therefore the HIGHEST orchestration call reaches the exact same cancellation path as normal Super Chat execution.

### Super Chat target count cross-check

The installed ChatInteractionHandler.onServerChat method contains:
- one normal PlayerList.m_240416_(Component, boolean) invocation;
- separate mute-path ServerPlayer.m_5661_(Component, boolean) handling.

Therefore:
- timestamp ModifyArg targets only m_240416_;
- Discord AFTER injection targets only m_240416_;
- no ordinal is required;
- mute messages are not timestamp-wrapped by this compatibility path.

### Current plan state

The architecture is now code-level locked for the pinned artifacts:

Super Chat 3.1.0
BFCR artifact bfcrmod-1.20.1-3.0.0.jar with mod metadata bfcrmod version 1.0.3
Discord Integration 3.0.7.1
Forge 47.4.3
EventBus 6.0.5
Mixin 0.8.5

The only remaining work before implementation is implementation itself plus static/build verification. It is not an unresolved architectural question.

No third-party event priority is guessed.
No same-priority ordering is relied upon.
No third-party handler is disabled.
No third-party config is changed.
No direct Discord sender is reimplemented.

### Implementation gate

An implementation AI may proceed only if it follows this file literally.

Before touching source:
1. re-run the exact JAR assertions;
2. make .BAK copies for existing DeepSpace files that will change;
3. create the new compatibility classes;
4. implement the verified HIGHEST orchestration;
5. implement the verified Mixins;
6. implement the conditional Mixin plugin;
7. update the Mixin config;
8. build headlessly;
9. inspect the generated JAR/refmap;
10. perform the static optional-mod/classloading checks.

If any verification differs from this document, STOP and update this plan before coding.

### Final invariant

One ServerChatEvent
-> one exact Super Chat handler invocation
-> one exact Super Chat Component
-> optional BFCR outer timestamp wrapper
-> one PlayerList.m_240416_ Minecraft broadcast
-> one existing DiscordIntegrationMod.chat(event) invocation when DI is enabled
-> Super Chat cancellation
-> BFCR normal handler skipped

That is the required implementation contract.
## Final Mixin-plugin verification addendum — supersedes earlier plugin-location wording

One additional code-level issue was checked after the preceding revision.

### Mixin 0.8.5 plugin invocation order

The local Mixin 0.8.5 MixinInfo bytecode was inspected.

During Mixin target preparation, Mixin calls:
PluginHandle.shouldApplyMixin(targetName, mixinClassName)

before accepting the declared target classes into the Mixin target list.

The verified sequence is:
- read declared mixin targets;
- call shouldApplyMixin(...);
- only if it returns true add the declared target;
- then resolve/read target classes.

Therefore a companion plugin can veto the optional Super Chat Mixins before an absent Super Chat target is resolved.

This removes the earlier uncertainty about whether conditional gating occurs too late.

### Correct companion-plugin package

The previous suggested location:
com.deepspace.compat.superchat.SuperChatCompatMixinPlugin

is superseded.

The plugin must instead be outside the configured Mixin package:

com.deepspace.compat.superchat.SuperChatCompatMixinPlugin

Reason:
the existing Mixin package is:
com.deepspace.mixin

and the plugin is bootstrap/compatibility infrastructure, not a Mixin class. Keeping it outside the Mixin package avoids mixing the plugin class into the package dedicated to Mixin classes and follows the Mixin package-isolation model.

The plugin JSON entry must therefore be exactly:
"plugin": "com.deepspace.compat.superchat.SuperChatCompatMixinPlugin"

### Correct Mixin class locations

Actual Mixin classes remain under:
com.deepspace.mixin

Therefore:

com.deepspace.mixin.SuperChatTimestampMixin
com.deepspace.mixin.SuperChatDiscordIntegrationMixin

The companion plugin is:
com.deepspace.compat.superchat.SuperChatCompatMixinPlugin

The runtime event/orchestration and adapters are also under:
com.deepspace.compat.superchat

### Correct plugin implementation constraint

The companion plugin must not reference:
net.minecraft.*
net.minecraftforge.*
net.mcreator.superchat.*
com.jeremiahbl.bfcrmod.*
de.erdbeerbaerlp.dcintegration.*

in fields, method signatures, static initializers, or ordinary logic.

Its only target-detection dependency is the already-loaded Mixin service API.

Verified implementation mechanism:
org.spongepowered.asm.service.MixinService.getService()
 -> getClassProvider()
 -> findClass(targetClassName, false)

The boolean false prevents class initialization while testing availability.

### Exact plugin decision

For:
com.deepspace.mixin.SuperChatTimestampMixin
and:
com.deepspace.mixin.SuperChatDiscordIntegrationMixin

return:

MixinService.getService()
    .getClassProvider()
    .findClass(
        "net.mcreator.superchat.command.ChatInteractionHandler",
        false
    )

success -> true
ClassNotFoundException -> false

For every other Mixin in deepspace.mixins.json:
return true.

Do not use ModList in the early Mixin plugin.

### Required plugin static test

Build a small isolated test or use the compiled plugin with the exact Mixin 0.8.5 dependency to verify:
1. Super Chat target class present -> shouldApplyMixin returns true.
2. Super Chat target class absent -> shouldApplyMixin returns false.
3. plugin itself loads without Minecraft/Forge classes.
4. absent Super Chat does not cause target-class resolution.
5. existing non-Super-Chat Mixins remain enabled.

This test must pass before the feature is considered complete.

## Final Mixin JSON requirement

The existing:
src/main/resources/deepspace.mixins.json

must retain:
"required": true

and add:
"plugin": "com.deepspace.compat.superchat.SuperChatCompatMixinPlugin"

The two Super Chat Mixins must be added to the existing universal "mixins" array, not the client array.

Do not lower defaultRequire from 1.

Do not use a guessed second manifest config.

The plugin-based gating is now verified at the MixinInfo target-selection stage.

## Final implementation package map

com.deepspace.compat.superchat.SuperChatDiscordCompat
    HIGHEST ServerChatEvent orchestration

com.deepspace.compat.superchat.SuperChatBfcrAdapter
    live BFCR reflection and format analysis

com.deepspace.compat.superchat.SuperChatDiscordIntegrationBridge
    existing DI chat(ServerChatEvent) reflection

com.deepspace.compat.superchat.SuperChatCompatMixinPlugin
    early optional Super Chat target gating

com.deepspace.mixin.SuperChatTimestampMixin
    exact ModifyArg at PlayerList.m_240416_

com.deepspace.mixin.SuperChatDiscordIntegrationMixin
    exact AFTER injection at PlayerList.m_240416_

## Final code-level target table

| Purpose | Exact class | Exact method/target | Verified |
|---|---|---|---|
| Orchestration | DeepSpace | ServerChatEvent | Yes |
| Super Chat handler | net.mcreator.superchat.command.ChatInteractionHandler | onServerChat(ServerChatEvent) | Yes |
| Minecraft broadcast | net.minecraft.server.players.PlayerList | m_240416_(Component, boolean) | Yes |
| Mapped Minecraft broadcast name | net.minecraft.server.players.PlayerList | broadcastSystemMessage(Component, boolean) | Yes |
| BFCR handler | com.jeremiahbl.bfcrmod.events.ChatEventHandler | onServerChat(ServerChatEvent) | Yes |
| BFCR config | com.jeremiahbl.bfcrmod.config.ConfigHandler$ConfigBuilder | enableTimestamp/timestampFormat/chatMessageFormat/chatMessageColor | Yes |
| BFCR formatting | com.jeremiahbl.bfcrmod.TextFormatter | stringToFormattedText(String) | Yes |
| Discord mod | de.erdbeerbaerlp.dcintegration.forge.DiscordIntegrationMod | chat(ServerChatEvent) | Yes |
| Forge mod lookup | net.minecraftforge.fml.ModList | getModObjectById(String) | Yes |
| Forge loaded check | net.minecraftforge.fml.ModList | isLoaded(String) | Yes |
| Event priority | net.minecraftforge.eventbus.api.EventPriority | HIGHEST | Yes |
| Event subscription defaults | net.minecraftforge.eventbus.api.SubscribeEvent | NORMAL / receiveCanceled=false | Yes |
| Optional Mixin gating | org.spongepowered.asm.service.MixinService | getService().getClassProvider().findClass(..., false) | Yes |
| Mixin conditional API | org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin | shouldApplyMixin(...) | Yes |

## Final status after this addendum

The architecture and exact API targets are now verified.

There is no remaining unresolved event-ordering question.

There is no remaining unresolved Discord-recursion question.

There is no remaining unresolved optional-Mixin gating question.

The implementation phase may begin only after the implementation AI repeats the static assertions listed in this document against the current files/JARs and stops if any artifact has changed.

## Implementation addendum — 2026-10-09

The first implementation compile established one build-system requirement that was not an architectural dependency: Mixin 0.8.5's annotation processor must be able to resolve the optional Super Chat target class while compiling the two string-target Mixins.

Therefore build.gradle adds:
compileOnly(files('../../deepspace_1.0.0/data/mods/super_chat-3.1.0-forge-1.20.1.disabled'))

This is compileOnly only. The Super Chat JAR is not bundled into DeepSpace and remains optional at runtime. The `.disabled` filename is intentionally used as the exact installed artifact path; its contents are a valid JAR and were already verified before implementation.

The first compile failed only because the annotation processor could not locate the optional target class. After adding this compileOnly target, `./gradlew clean compileJava` completed successfully with 61 pre-existing/deprecation warnings and zero errors.

This build-only dependency does not violate the optional-mod requirement: no Super Chat class is referenced by the generated DeepSpace runtime dependency graph outside the optional Mixin target metadata and reflection strings.


## Implementation verification correction — ModList return type — 2026-10-09

During post-build bytecode inspection, Forge 47.4.3's actual signature was re-checked with javap:

ModList.getModObjectById(String) returns java.util.Optional, not the contained object directly.

The compatibility bridge therefore MUST unwrap it before invoking DiscordIntegrationMod.chat(ServerChatEvent):

Object mod = ModList.get().getModObjectById("dcintegration").orElse(null);

The previous prose in this plan saying that getModObjectById directly returns the mod object was imprecise and is superseded by this correction.

The runtime design is unchanged: obtain the actual loaded DiscordIntegrationMod object from Forge's ModList, then invoke its verified public instance chat(ServerChatEvent) method. No second DiscordIntegrationMod instance is created.

The bridge source has been corrected accordingly and a backup was created before the correction.
