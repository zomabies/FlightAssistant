package ru.octol1ttle.flightassistant.computers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;
import ru.octol1ttle.flightassistant.FlightAssistant;
import ru.octol1ttle.flightassistant.HudRenderer;
import ru.octol1ttle.flightassistant.computers.autoflight.AutoFlightComputer;
import ru.octol1ttle.flightassistant.computers.autoflight.FireworkController;
import ru.octol1ttle.flightassistant.computers.autoflight.PitchController;
import ru.octol1ttle.flightassistant.computers.autoflight.YawController;
import ru.octol1ttle.flightassistant.computers.navigation.FlightPlanner;
import ru.octol1ttle.flightassistant.computers.safety.AlertController;
import ru.octol1ttle.flightassistant.computers.safety.ElytraStateController;
import ru.octol1ttle.flightassistant.computers.safety.GPWSComputer;
import ru.octol1ttle.flightassistant.computers.safety.StallComputer;
import ru.octol1ttle.flightassistant.computers.safety.VoidLevelComputer;

public class ComputerHost {
    public final AirDataComputer data;
    public final StallComputer stall;
    public final GPWSComputer gpws;
    public final VoidLevelComputer voidLevel;
    public final FireworkController firework;
    public final AutoFlightComputer autoflight;
    public final AlertController alert;
    public final TimeComputer time;
    public final PitchController pitch;
    public final YawController yaw;
    public final FlightPlanner plan;
    public final ElytraStateController elytra;
    public final List<IComputer> faulted;
    private final List<ITickableComputer> tickables;

    public ComputerHost(@NotNull MinecraftClient mc, HudRenderer renderer) {
        assert mc.player != null;
        ClientPlayerEntity player = mc.player;

        this.data = new AirDataComputer(mc, player);
        this.time = new TimeComputer();
        this.firework = new FireworkController(time, data, mc.interactionManager);
        this.stall = new StallComputer(firework, data);
        this.voidLevel = new VoidLevelComputer(data, firework, stall);
        this.gpws = new GPWSComputer(data);
        this.elytra = new ElytraStateController(data);

        this.yaw = new YawController(time, data);
        this.pitch = new PitchController(data, stall, time, voidLevel, gpws);

        this.plan = new FlightPlanner(data);
        this.autoflight = new AutoFlightComputer(data, gpws, plan, firework, pitch, yaw);

        this.alert = new AlertController(this, mc.getSoundManager(), renderer);

        // computers are sorted in the order they should be ticked to avoid errors
        this.tickables = new ArrayList<>(List.of(
                data, time, stall, gpws, voidLevel, elytra, plan, autoflight, firework, alert, pitch, yaw
        ));
        Collections.reverse(this.tickables); // we tick computers in reverse, so reverse the collections so that the order is correct

        this.faulted = new ArrayList<>(tickables.size());
    }

    public void tick() {
        for (int i = tickables.size() - 1; i >= 0; i--) {
            ITickableComputer computer = tickables.get(i);
            try {
                computer.tick();
            } catch (Exception e) {
                FlightAssistant.LOGGER.error("Exception ticking computer", e);
                computer.reset();
                faulted.add(computer);
                tickables.remove(computer);
            }
        }
    }

    public void resetComputers(boolean resetWorking) {
        if (resetWorking) {
            for (ITickableComputer tickable : tickables) {
                tickable.reset();
            }
        }

        for (int i = faulted.size() - 1; i >= 0; i--) {
            IComputer computer = faulted.get(i);
            faulted.remove(computer);

            computer.reset();

            if (computer instanceof ITickableComputer tickable) {
                tickables.add(tickable);
            }
        }
    }
}
