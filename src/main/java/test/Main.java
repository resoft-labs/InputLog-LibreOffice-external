package test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnector;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class Main {

    private static final int SOCKET_PORT = 2002;

    private static void dispatch(String command) {
        try {
            // Step 1: Connect to office
            System.out.println("Connecting to Office...");

            // Create a local Component Context
            XComponentContext localContext = Bootstrap.createInitialComponentContext(null);

            // Get a local service manager
            XMultiComponentFactory localFactory = localContext.getServiceManager();

            // Connect to office via a socket
            XConnector connector = UnoRuntime.queryInterface(XConnector.class,
                    localFactory.createInstanceWithContext("com.sun.star.connection.Connector", localContext));
            XConnection connection = connector.connect("socket,host=localhost,port=" + SOCKET_PORT);

            // Create a bridge
            XBridgeFactory bridgeFactory = UnoRuntime.queryInterface(XBridgeFactory.class,
                    localFactory.createInstanceWithContext("com.sun.star.bridge.BridgeFactory", localContext));
            XBridge bridge = bridgeFactory.createBridge("socketBridgeAD", "urp", connection, null);

            // Get a remote service manager
            XMultiComponentFactory serviceManager = UnoRuntime.queryInterface(XMultiComponentFactory.class, bridge.getInstance("StarOffice.ServiceManager"));

            // Retrieve the remote component context as a property
            XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, serviceManager);
            Object defaultContext = props.getPropertyValue("DefaultContext");

            // Retrieve the remote XComponentContext
            XComponentContext remoteCC = UnoRuntime.queryInterface(XComponentContext.class, defaultContext);

            if (remoteCC == null) {
                System.err.println("Remote Component Context could not be created");
                System.exit(1);
            }

            // Retrieve (remote) service Factory
            XMultiComponentFactory remoteFactory = remoteCC.getServiceManager();

            if (remoteFactory == null) {
                System.err.println("Remote Service Manager is unavailable");
                System.exit(1);
            }

            // Retrieve the (remote) desktop
            XDesktop xDesktop = UnoRuntime.queryInterface(XDesktop.class, remoteFactory.createInstanceWithContext("com.sun.star.frame.Desktop", remoteCC));

            if (xDesktop == null) {
                System.err.println("Could not create a desktop service");
                System.exit(1);
            }

            System.out.println("Connected...");

            // Step 2: dispatch the call
            System.out.println("Sending command to InputLog-LO: " + command);

            XDispatchHelper helper = UnoRuntime.queryInterface(XDispatchHelper.class,
                    remoteFactory.createInstanceWithContext("com.sun.star.frame.DispatchHelper", remoteCC));

            if (helper == null) {
                System.err.println("Could not create dispatch helper");
                System.exit(1);
            }

            XDispatchProvider provider = UnoRuntime.queryInterface(XDispatchProvider.class, xDesktop.getCurrentFrame());
            helper.executeDispatch(provider, command, "", 0, null);

            // helper.executeDispatch(provider, ".uno:Close", "", 0, null);

            System.out.println("Done...");

            // TODO: proper cleanup?
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Failed to stop InputLog-LO " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void startOffice() {

        System.out.println("Creating libreoffice process");

        try {
            String[] cmd = new String[] {
                    "libreoffice",
                    "-accept=socket,host=localhost,port=" + SOCKET_PORT + ";urp;" };

            Process p = Runtime.getRuntime().exec(cmd);

            if (p != null) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    // ignored
                }
            }

            System.out.println("Done");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Failed to create headless libreoffice" + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void usage() {
        System.out.println("Usage: [ startoffice | startsession | stopsession ]");
        System.exit(1);
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            usage();
        }

        String arg = args[0];

        if ("startoffice".equals(arg)) {
            startOffice();
        } else if ("startsession".equals(arg)) {
            dispatch("service:com.resoftlabs.Inputlog?actionStart");
        } else if ("stopsession".equals(arg)) {
            dispatch("service:com.resoftlabs.Inputlog?actionStop");
        } else {
            usage();
        }
    }
}
