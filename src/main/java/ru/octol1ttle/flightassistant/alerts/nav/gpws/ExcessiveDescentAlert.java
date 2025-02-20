package ru.octol1ttle.flightassistant.alerts.nav.gpws;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.octol1ttle.flightassistant.HudComponent;
import ru.octol1ttle.flightassistant.alerts.AbstractAlert;
import ru.octol1ttle.flightassistant.alerts.AlertSoundData;
import ru.octol1ttle.flightassistant.computers.AirDataComputer;
import ru.octol1ttle.flightassistant.computers.safety.GPWSComputer;
import ru.octol1ttle.flightassistant.config.FAConfig;



public class ExcessiveDescentAlert extends AbstractAlert {
    private static final float SINK_RATE_THRESHOLD = 7.5f;
    private static final float PULL_UP_THRESHOLD = 5.0f;
    private final AirDataComputer data;
    private final GPWSComputer gpws;

    public ExcessiveDescentAlert(AirDataComputer data, GPWSComputer gpws) {
        this.data = data;
        this.gpws = gpws;
    }

    @Override
    public boolean isTriggered() {
        return data.pitch < 0 && gpws.descentImpactTime >= 0.0f;
    }

    @Override
    public boolean renderCentered(TextRenderer textRenderer, DrawContext context, float x, float y, boolean highlight) {
        if (gpws.descentImpactTime <= PULL_UP_THRESHOLD) {
            HudComponent.drawHighlightedMiddleAlignedText(textRenderer, context, Text.translatable("alerts.flightassistant.pull_up"), x, y,
                    FAConfig.hud().warningColor, highlight);

            return true;
        }

        if (gpws.descentImpactTime <= SINK_RATE_THRESHOLD) {
            HudComponent.drawHighlightedMiddleAlignedText(textRenderer, context, Text.translatable("alerts.flightassistant.sink_rate"), x, y,
                    FAConfig.hud().cautionColor, highlight);

            return true;
        }

        return false;
    }

    @Override
    public @NotNull AlertSoundData getAlertSoundData() {
        if (gpws.descentImpactTime <= PULL_UP_THRESHOLD) {
            return AlertSoundData.PULL_UP;
        }
        if (gpws.descentImpactTime <= SINK_RATE_THRESHOLD) {
            return AlertSoundData.SINK_RATE;
        }

        return AlertSoundData.EMPTY;
    }
}
