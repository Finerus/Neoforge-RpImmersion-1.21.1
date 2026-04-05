package net.rp.rpimmersion.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.rp.rpimmersion.RpImmersion;

/**
 * Enregistrement client-only.
 * La classe n'est instanciée que si FMLEnvironment.dist == CLIENT (vérifié dans RpImmersion).
 */
public class ClientSetup {

    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(ClientEventHandler.class);
    }
}