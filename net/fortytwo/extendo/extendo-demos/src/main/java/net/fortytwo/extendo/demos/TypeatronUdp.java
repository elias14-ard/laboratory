package net.fortytwo.extendo.demos;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import net.fortytwo.extendo.p2p.osc.OscControl;
import net.fortytwo.extendo.p2p.osc.OscSender;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OSC controller for the Monomanual Typeatron
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class TypeatronUdp extends TypeatronControlWrapper {

    private static final Logger logger = Logger.getLogger(TypeatronUdp.class.getName());

    private final int portIn, portOut;

    public TypeatronUdp(final int portIn,
                        final int portOut) throws OscControl.DeviceInitializationException {
        super();

        logger.info("connecting to Typeatron via UDP ports " + portIn + " (in) and " + portOut + " (out)");

        this.portIn = portIn;
        this.portOut = portOut;
    }

    public void run() throws SocketException {
        OSCPortIn pi = new OSCPortIn(portIn);
        pi.addListener("", new OSCListener() {
            @Override
            public void acceptMessage(Date date, OSCMessage oscMessage) {
                typeatron.getReceiver().receive(oscMessage);
            }
        });

        final OSCPortOut po = new OSCPortOut(InetAddress.getLoopbackAddress(), portOut);
        OscSender sender = new OscSender() {
            @Override
            public void send(OSCBundle bundle) {
                try {
                    po.send(bundle);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to send OSC bundle", e);
                }
            }
        };
        typeatron.connect(sender);

        logger.info("listening for /exo messages");
        pi.startListening();
    }

    /*
     * Usage example:
     *   ./typeatron-serial.sh -d /dev/ttyUSB0 -r 115200
     */
    public static void main(final String[] args) throws Exception {
        try {
            Options options = new Options();

            Option portInOpt = new Option("i", "portIn", true, "port for incoming OSC messages (default: 42003)");
            portInOpt.setArgName("PORT_IN");
            portInOpt.setRequired(false);
            options.addOption(portInOpt);

            Option portOutOpt = new Option("o", "portOut", true, "port for outgoing OSC messages (default: 42002)");
            portOutOpt.setArgName("PORT_OUT");
            portOutOpt.setRequired(false);
            options.addOption(portOutOpt);

            CommandLineParser clp = new PosixParser();
            CommandLine cmd = null;

            try {
                cmd = clp.parse(options, args);
            } catch (ParseException e) {
                printUsage(options);
                System.exit(1);
            }

            int portIn = Integer.valueOf(cmd.getOptionValue(portInOpt.getOpt(), "42003"));
            int portOut = Integer.valueOf(cmd.getOptionValue(portOutOpt.getOpt(), "42002"));

            new TypeatronUdp(portIn, portOut).run();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void printUsage(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("typeatron-osc", options);
    }
}
