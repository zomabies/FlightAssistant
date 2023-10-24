package net.torocraft.flighthud.alerts.nav;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.torocraft.flighthud.HudComponent;
import net.torocraft.flighthud.alerts.AbstractAlert;
import net.torocraft.flighthud.alerts.AlertSoundData;
import net.torocraft.flighthud.alerts.ECAMSoundData;
import net.torocraft.flighthud.computers.FlightComputer;
import net.torocraft.flighthud.computers.VoidDamageLevelComputer;
import org.jetbrains.annotations.NotNull;

import static net.torocraft.flighthud.HudComponent.CONFIG;

public class ApproachingVoidDamageLevelAlert extends AbstractAlert {
    private final FlightComputer computer;

    public ApproachingVoidDamageLevelAlert(FlightComputer computer) {
        this.computer = computer;
    }

    @Override
    public boolean isTriggered() {
        return computer.voidDamage.status >= VoidDamageLevelComputer.STATUS_APPROACHING_DAMAGE_LEVEL;
    }

    @Override
    public @NotNull AlertSoundData getAlertSoundData() {
        return ECAMSoundData.MASTER_WARNING;
    }

    @Override
    public int renderECAM(TextRenderer textRenderer, DrawContext context, float x, float y, boolean highlight) {
        Text text = computer.voidDamage.status == VoidDamageLevelComputer.STATUS_REACHED_DAMAGE_LEVEL
                ? Text.translatable("alerts.flighthud.nav.reached_void_damage_level")
                : Text.translatable("alerts.flighthud.nav.approaching_void_damage_level");

        return HudComponent.drawHighlightedFont(textRenderer, context, x, y, text,
                CONFIG.alertColor,
                !dismissed && highlight);
    }
}
