package test;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnector;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class Main {

    private static final int SOCKET_PORT = 2002;

    private static XComponent bridgeComponent = null;
    private static XComponentContext xcc = null;
    private static XMultiComponentFactory mcFactory;
    private static XDesktop xDesktop = null;

    public static <T> T qi(Class<T> aType, Object o) {
        return UnoRuntime.queryInterface(aType, o);
    }

    public static <T> T createInstanceMCF(Class<T> aType, String serviceName, Object[] args) {
        /*
         * create an interface object of class aType from the named service and arguments; uses XComponentContext and 'new' XMultiComponentFactory so only a
         * bridge to office is needed
         */
        T interfaceObj = null;
        try {
            Object o = mcFactory.createInstanceWithArgumentsAndContext(serviceName, args, xcc);
            // create service component using the specified args and component context
            interfaceObj = qi(aType, o);
            // uses bridge to obtain proxy to remote interface inside service;
            // implements casting across process boundaries
        } catch (Exception e) {
            System.out.println("Couldn't create interface for \"" + serviceName + "\": " + e);
        }
        return interfaceObj;
    } // end of createInstanceMCF()

    public static <T> T createInstanceMCF(Class<T> aType, String serviceName) {
        /*
         * create an interface object of class aType from the named service; uses XComponentContext and 'new' XMultiComponentFactory so only a bridge to office
         * is needed
         */
        T interfaceObj = null;
        try {
            Object o = mcFactory.createInstanceWithContext(serviceName, xcc);
            // create service component using the specified component context
            interfaceObj = qi(aType, o);
            // uses bridge to obtain proxy to remote interface inside service;
            // implements casting across process boundaries
        } catch (Exception e) {
            System.out.println("Couldn't create interface for \"" + serviceName + "\": " + e);
        }
        return interfaceObj;
    } // end of createInstanceMCF()

    private static XComponentContext socketContext() {
        // use socket connection to Office
        // https://forum.openoffice.org/en/forum/viewtopic.php?f=44&t=1014

        XComponentContext xcc = null; // the remote office component context

        try {
            /*
             * String[] cmdArray = new String[3]; cmdArray[0] = "soffice"; // requires soffice to be in Windows PATH env var. cmdArray[1] = "-headless";
             * cmdArray[2] = "-accept=socket,host=localhost,port=" + SOCKET_PORT + ";urp;";
             * 
             * Process p = Runtime.getRuntime().exec(cmdArray);
             * 
             * if (p != null) { System.out.println("Office process created"); Thread.sleep(5000); } // Wait 5 seconds, until office is in listening mode
             * 
             */

            // Create a local Component Context
            XComponentContext localContext = Bootstrap.createInitialComponentContext(null);

            // Get the local service manager
            XMultiComponentFactory localFactory = localContext.getServiceManager();

            // connect to Office via its socket
            /*
             * Object urlResolver = localFactory.createInstanceWithContext( "com.sun.star.bridge.UnoUrlResolver", localContext); XUnoUrlResolver xUrlResolver =
             * Lo.qi(XUnoUrlResolver.class, urlResolver); Object initObject = xUrlResolver.resolve( "uno:socket,host=localhost,port=" + SOCKET_PORT +
             * ";urp;StarOffice.ServiceManager");
             */
            XConnector connector = qi(XConnector.class, localFactory.createInstanceWithContext("com.sun.star.connection.Connector", localContext));

            XConnection connection = connector.connect(
                    "socket,host=localhost,port=" + SOCKET_PORT);

            // create a bridge to Office via the socket
            XBridgeFactory bridgeFactory = qi(XBridgeFactory.class, localFactory.createInstanceWithContext("com.sun.star.bridge.BridgeFactory", localContext));

            // create a nameless bridge with no instance provider
            XBridge bridge = bridgeFactory.createBridge("socketBridgeAD", "urp", connection, null);

            bridgeComponent = qi(XComponent.class, bridge);

            // get the remote service manager
            XMultiComponentFactory serviceManager = qi(XMultiComponentFactory.class, bridge.getInstance("StarOffice.ServiceManager"));

            // retrieve Office's remote component context as a property
            XPropertySet props = qi(XPropertySet.class, serviceManager);
            // initObject);
            Object defaultContext = props.getPropertyValue("DefaultContext");

            // get the remote interface XComponentContext
            xcc = qi(XComponentContext.class, defaultContext);
        } catch (Exception e) {
            System.out.println("Unable to socket connect to Office");
        }

        return xcc;
    } // end of socketContext()

    public static XComponentLoader connectToOffice() {
        /*
         * Creation sequence: remote component content (xcc) --> remote service manager (mcFactory) --> remote desktop (xDesktop) --> component loader
         * (XComponentLoader) Once we have a component loader, we can load a document. xcc, mcFactory, and xDesktop are stored as static globals.
         */
        System.out.println("Loading Office...");

        xcc = socketContext(); // connects to office via a socket

        if (xcc == null) {
            System.out.println("Office context could not be created");
            System.exit(1);
        }

        // get the remote office service manager
        mcFactory = xcc.getServiceManager();

        if (mcFactory == null) {
            System.out.println("Office Service Manager is unavailable");
            System.exit(1);
        }

        // desktop service handles application windows and documents
        xDesktop = createInstanceMCF(XDesktop.class, "com.sun.star.frame.Desktop");

        if (xDesktop == null) {
            System.out.println("Could not create a desktop service");
            System.exit(1);
        }

        // XComponentLoader provides ability to load components
        return qi(XComponentLoader.class, xDesktop);
    } // end of loadOffice()

    public static boolean dispatchCmd(String cmd) {
        return dispatchCmd(xDesktop.getCurrentFrame(), cmd, null);
    }

    public static boolean dispatchCmd(String cmd, PropertyValue[] props) {
        return dispatchCmd(xDesktop.getCurrentFrame(), cmd, props);
    }

    public static boolean dispatchCmd(XFrame frame, String cmd, PropertyValue[] props) {
        // cmd does not include the ".uno:" substring; e.g. pass "Zoom" not ".uno:Zoom"

        XDispatchHelper helper = createInstanceMCF(XDispatchHelper.class, "com.sun.star.frame.DispatchHelper");
        if (helper == null) {
            System.out.println("Could not create dispatch helper for command " + cmd);
            return false;
        }

        try {
            XDispatchProvider provider = qi(XDispatchProvider.class, frame);

            /*
             * returns failure even when the event works (?), and an illegal value when the dispatch actually does fail
             */
            /*
             * DispatchResultEvent res = (DispatchResultEvent) helper.executeDispatch(provider, (".uno:" + cmd), "", 0, props); if (res.State ==
             * DispatchResultState.FAILURE) System.out.println("Dispatch failed for \"" + cmd + "\""); else if (res.State == DispatchResultState.DONTKNOW)
             * System.out.println("Dispatch result unknown for \"" + cmd + "\"");
             */
            helper.executeDispatch(provider, (cmd), "", 0, props);
            return true;
        } catch (java.lang.Exception e) {
            System.out.println("Could not dispatch \"" + cmd + "\":\n  " + e);
        }
        return false;
    } // end of dispatchCmd()

    public static void main(String[] args) {

        try {
            connectToOffice();

            System.out.println("remote ServiceManager is available");
            String[] services = mcFactory.getAvailableServiceNames();

            /*
             * System.out.println("Available services:");
             * 
             * for (String s : services) {
             * 
             * if (s != null) { System.out.println(" " + s); } }
             */
            dispatchCmd("service:com.resoftlabs.Inputlog?actionStop");

        } catch (Exception e) {
            System.out.println("Failed " + e);
            e.printStackTrace();

        }

        System.out.println("Done");
    }

    /*
     * public static void main(String[] args) {
     * 
     * System.out.println("Starting test");
     * 
     * try { // get the remote office component context XComponentContext context = Bootstrap.bootstrap();
     * 
     * System.out.println("Connected to a running office ...");
     * 
     * XMultiComponentFactory service = context.getServiceManager();
     * 
     * if (service != null) { System.out.println("remote ServiceManager is available");
     * 
     * String[] services = service.getAvailableServiceNames();
     * 
     * System.out.println("Available services:");
     * 
     * for (String s : services) {
     * 
     * if (s != null) { System.out.println(" " + s); } } } else { System.out.println("remote ServiceManager is NOT available"); }
     * 
     * } catch (Exception e) { System.out.println("Failed " + e); e.printStackTrace();
     * 
     * }
     * 
     * System.out.println("Done"); }
     */
}
