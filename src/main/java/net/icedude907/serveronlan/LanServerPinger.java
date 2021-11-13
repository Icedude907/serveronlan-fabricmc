// Adapted from the fabric mappings
// Tweaking EnvType to allow for server

package net.icedude907.serveronlan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used to send UDP multicasts to notify other clients of a local game on the same network.
 * <p>These multicasts will always be sent to {@code 224.0.2.60:4445} where other clients can listen for local games.
 */
@Environment(EnvType.SERVER)
public class LanServerPinger
extends Thread {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String PING_ADDRESS = "224.0.2.60";
    public static final int PING_PORT = 4445;
    private static final long PING_INTERVAL = 1500L;
    private final String motd;
    private final DatagramSocket socket;
    private boolean running = true;
    private final String addressPort; // The port clients should join

    public LanServerPinger(String motd, String addressPort) throws IOException {
        super("LanServerPinger #" + THREAD_ID.incrementAndGet());
        this.motd = motd;
        this.addressPort = addressPort;
        this.setDaemon(true);
        this.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
        this.socket = new DatagramSocket();
    }

    @Override
    public void run() {
        String string = LanServerPinger.createAnnouncement(this.motd, this.addressPort);
        byte[] bs = string.getBytes(StandardCharsets.UTF_8);
        while (!this.isInterrupted() && this.running) {
            try {
                InetAddress inetAddress = InetAddress.getByName(PING_ADDRESS);
                DatagramPacket datagramPacket = new DatagramPacket(bs, bs.length, inetAddress, 4445);
                this.socket.send(datagramPacket);
            }
            catch (IOException inetAddress) {
                LOGGER.warn("LanServerPinger: {}", (Object)inetAddress.getMessage());
                break;
            }
            try {
                LanServerPinger.sleep(PING_INTERVAL);
            }
            catch (InterruptedException interruptedException) {}
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        this.running = false;
    }

    public static String createAnnouncement(String motd, String addressPort) {
        return "[MOTD]" + motd + "[/MOTD][AD]" + addressPort + "[/AD]";
    }
}

