package ru.octol1ttle.flightassistant;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import org.joml.Matrix3f;
import ru.octol1ttle.flightassistant.commands.FlightPlanCommand;
import ru.octol1ttle.flightassistant.commands.MCPCommand;
import ru.octol1ttle.flightassistant.commands.ResetCommand;
import ru.octol1ttle.flightassistant.computers.ComputerHost;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class FACallbacks {
    public static void setup() {
        setupCommandRegistration();
        setupWorldRender();
        setupHudRender();
        setupUseItem();
    }

    private static void setupCommandRegistration() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(FlightAssistant.MODID);
            ResetCommand.register(builder);
            MCPCommand.register(builder);
            FlightPlanCommand.register(builder);

            LiteralCommandNode<FabricClientCommandSource> node = dispatcher.register(builder);
            dispatcher.register(literal("flas").redirect(node));
            dispatcher.register(literal("fhud").redirect(node));
            dispatcher.register(literal("fh").redirect(node));
        });
    }

    private static void setupWorldRender() {
        WorldRenderEvents.END.register(context -> {
            ComputerHost host = HudRenderer.getHost();
            if (host != null && !host.faulted.contains(host.data)) {
                Matrix3f inverseViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();
                host.data.updateRoll(inverseViewRotationMatrix.invert());
            }
        });
    }

    private static void setupHudRender() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return;
            }

            if (HudRenderer.getHost() == null) {
                HudRenderer.INSTANCE = new HudRenderer(client);
            }
            HudRenderer.getHost().tick();

            HudRenderer.INSTANCE.render(drawContext, client);
        });
    }

    private static void setupUseItem() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            ComputerHost host = HudRenderer.getHost();
            if (!world.isClient() || host == null || host.faulted.contains(host.firework)) {
                return TypedActionResult.pass(stack);
            }
            if (!host.data.isFlying || !(stack.getItem() instanceof FireworkRocketItem)) {
                host.firework.unsafeFireworks = false;
                return TypedActionResult.pass(stack);
            }

            host.firework.unsafeFireworks = !host.firework.isFireworkSafe(stack);

            boolean gpwsDanger = !host.faulted.contains(host.gpws) && (host.gpws.isInDanger() || !host.gpws.fireworkUseSafe);
            if (!host.firework.activationInProgress && (host.firework.unsafeFireworks || host.firework.lockManualFireworks || gpwsDanger)) {
                return TypedActionResult.fail(stack);
            }

            if (host.firework.fireworkResponded) {
                if (!host.faulted.contains(host.time) && host.time.prevMillis != null) {
                    host.firework.lastUseTime = host.time.prevMillis;
                }
                host.firework.fireworkResponded = false;
            }

            return TypedActionResult.pass(stack);
        });
    }
}
