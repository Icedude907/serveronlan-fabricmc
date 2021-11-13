package net.icedude907.serveronlan.mixin;

import java.io.IOException;
import java.net.Proxy;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.icedude907.serveronlan.LanServerPinger;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;

@Environment(EnvType.SERVER)
@Mixin(MinecraftDedicatedServer.class)
public abstract class MinecraftDedicatedServerMixin
    extends MinecraftServer
    implements DedicatedServer{
    
    @Shadow @Final
    static Logger LOGGER;

    private int lanPort = -1;
    private LanServerPinger lanPinger;

    // Can i get rid of this somehow?
    // Maybe just a bunch of null
    public MinecraftDedicatedServerMixin(
        Thread serverThread, DynamicRegistryManager.Impl registryManager, LevelStorage.Session session, 
        SaveProperties saveProperties, ResourcePackManager dataPackManager, Proxy proxy, DataFixer dataFixer, 
        ServerResourceManager serverResourceManager, @Nullable MinecraftSessionService sessionService, 
        @Nullable GameProfileRepository gameProfileRepo, @Nullable UserCache userCache, 
        WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory){
        super(serverThread, registryManager, session, saveProperties, dataPackManager, proxy, dataFixer, 
            serverResourceManager, sessionService, gameProfileRepo, userCache, worldGenerationProgressListenerFactory);
    }

    // TODO: FIX INJECT
    @Inject(method = "setupServer", 
        /*at = @At(value="INVOKE_ASSIGN", 
        target="Lnet/minecraft/server/MinecraftServer;getNetworkIo()Lnet/minecraft/server/ServerNetworkIo;)"*/
        at = @At("TAIL")
    )
    public void setupLAN(CallbackInfoReturnable<Boolean> ci){
        lanPort = this.getPort();
        boolean result = openToLan(lanPort);
        TranslatableText text = result ? new TranslatableText("commands.publish.started", lanPort) : new TranslatableText("commands.publish.failed");
        LOGGER.log(Level.INFO, text.getString());
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onShutdown(CallbackInfo ci){
        if (this.lanPinger != null) {
            this.lanPinger.interrupt();
            this.lanPinger = null;
        }
    }

    public boolean openToLan(int port) {
        try {
            lanPort = port;
            lanPinger = new LanServerPinger(this.getServerMotd(), "" + port);
            lanPinger.start();
            return true;
        }
        catch (IOException iOException) {
            return false;
        }
    }
}
