package net.rp.rpimmersion;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.rp.rpimmersion.client.ClientSetup;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.event.FogServerEventHandler;
import net.rp.rpimmersion.network.NetworkHandler;
import org.slf4j.Logger;

@Mod(RpImmersion.MODID)
public class RpImmersion {

    public static final String MODID = "rpimmersion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpImmersion(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, FogConfig.SPEC,
                "rpimmersion/rpimmersion-fog.toml");

        NetworkHandler.register(modEventBus);
        ClientSetup.register(modEventBus);

        NeoForge.EVENT_BUS.register(new FogServerEventHandler());
    }
}
