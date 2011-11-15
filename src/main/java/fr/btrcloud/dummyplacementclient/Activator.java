/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.btrcloud.dummyplacementclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ow2.sirocco.vmm.placementmanager.api.VMPlacementSolver;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.ow2.sirocco.vmm.api.HostMXBean;
import org.ow2.sirocco.vmm.api.ServerPoolMXBean;
import org.ow2.sirocco.vmm.placementmanager.api.PlacementRule;
import org.ow2.sirocco.vmm.placementmanager.api.VirtualMachineConfigSpecWithConstraints;
import org.ow2.sirocco.vmm.placementmanager.api.VmToVmAntiAffinityRule;

/**
 *
 * @author Alexandre Garnier
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        ServiceReference[] refs = context.getServiceReferences(
                VMPlacementSolver.class.getName(), null);

        for (ServiceReference sr : context.getBundle(5L).getRegisteredServices()) {
            System.out.println(sr.getClass());
            System.out.println(sr.isAssignableTo(context.getBundle(5L), VMPlacementSolver.class.getName()));
        }

        if (refs != null) {
            try {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    System.out.println("Press Enter to call placement algorithm on "
                            + "Sirocco instance's cluster (type 'q' to quit).");
                    if (in.readLine().equals("q")) {
                        break;
                    } // if

                    // Business
                    VMPlacementSolver vmps = (VMPlacementSolver) context.getService(refs[0]);

                    // init
                    String ip = "localhost";
                    int port = 9999;
                    String host1 = "host1", host2 = "host2", host3 = "host3";

                    // JMX client connection
                    JMXServiceURL jmxsurl =
                            new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + ip + ":"
                            + port + "/server");
                    JMXConnector jmxc = JMXConnectorFactory.connect(jmxsurl, null);
                    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

                    // Business
                    HostMXBean host = JMX.newMXBeanProxy(mbsc, mbsc.queryNames(
                            new ObjectName("org.ow2.sirocco.vmm.api:type=Host,hostname="
                            + host1 + ",*"), null).
                            toArray(new ObjectName[1])[0], HostMXBean.class, true);
                    ServerPoolMXBean serverPool = host.getServerPool();
                    PlacementRule antiVM1 = new VmToVmAntiAffinityRule();
                    antiVM1.setVirtualMachines(host.getResidentVMs());
                    List<PlacementRule> placementRules = Arrays.asList(antiVM1);
                    System.out.println(vmps.solve(serverPool, placementRules));

                    VirtualMachineConfigSpecWithConstraints vmSpec =
                            new VirtualMachineConfigSpecWithConstraints();
                    vmSpec.setName("newVM");
                    vmSpec.setNumVCPUs(1);
                    vmSpec.setMemoryMB(128L);
                    vmSpec.setAffinityVirtualMachineGroup(
                            JMX.newMXBeanProxy(mbsc, mbsc.queryNames(
                            new ObjectName("org.ow2.sirocco.vmm.api:type=Host,hostname="
                            + host2 + ",*"), null).
                            toArray(new ObjectName[1])[0], HostMXBean.class, true).
                            getResidentVMs());
                    vmSpec.setAffinityHostGroup(Arrays.asList(
                            JMX.newMXBeanProxy(mbsc, mbsc.queryNames(
                            new ObjectName("org.ow2.sirocco.vmm.api:type=Host,hostname="
                            + host3 + ",*"), null).
                            toArray(new ObjectName[1])[0], HostMXBean.class, true)));
                    System.out.println(vmps.placeVirtualMachine(serverPool, vmSpec, placementRules));

                }
            } catch (IOException ex) {
                System.err.println(ex.getLocalizedMessage());
            } // try
        } else {
            System.err.println("Error: " + VMPlacementSolver.class.getName() + " service not found.");
            this.stop(context);
        } // if
    } // void start(BundleContext)

    @Override
    public void stop(BundleContext context) throws Exception {
        //TODO add deactivation code here
    } // void stop(BundleContext)
}
