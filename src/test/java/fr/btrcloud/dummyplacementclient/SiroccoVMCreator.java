/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.btrcloud.dummyplacementclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.ow2.sirocco.vmm.api.CustomizationSpec;
import org.ow2.sirocco.vmm.api.DiskOperation;
import org.ow2.sirocco.vmm.api.HostMXBean;
import org.ow2.sirocco.vmm.api.ServerPoolMXBean;
import org.ow2.sirocco.vmm.api.StoragePoolMXBean;
import org.ow2.sirocco.vmm.api.VMMException;
import org.ow2.sirocco.vmm.api.VNICSpec;
import org.ow2.sirocco.vmm.api.VNICSpec.MacAddressAssignement;
import org.ow2.sirocco.vmm.api.VirtualDiskSpec;
import org.ow2.sirocco.vmm.api.VirtualMachineConfigSpec;
import org.ow2.sirocco.vmm.api.VirtualMachineMXBean;
import org.ow2.sirocco.vmm.api.Volume;

/**
 *
 * @author Alexandre Garnier
 */
public class SiroccoVMCreator {

    /**
     * @param args the command line arguments
     */
    public static void main(String... args)
            throws MalformedURLException, IOException, NullPointerException,
            MalformedObjectNameException, VMMException {

        // init
        String ip = "localhost";
        int port = 9999;
        String templateDiskName = "vol-100";
        Map<String, List<String>> clusterArch =
                SiroccoVMCreator.asMap(Arrays.asList("host1", "host2", "host3"),
                Arrays.asList(Arrays.asList("vm11", "vm12", "vm13"),
                Arrays.asList("vm21"), new ArrayList<String>()));

        // JMX client connection
        JMXServiceURL jmxsurl =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + ip + ":"
                + port + "/server");
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxsurl, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        // Business
        for (Map.Entry<String, List<String>> hostArch : clusterArch.entrySet()) {
            HostMXBean host = JMX.newMXBeanProxy(mbsc, mbsc.queryNames(
                    new ObjectName("org.ow2.sirocco.vmm.api:type=Host,hostname="
                    + hostArch.getKey() + ",*"), null).
                    toArray(new ObjectName[1])[0], HostMXBean.class, true);
            System.out.println(host.getServerPool());

            Volume templateVolume = null;
            StoragePoolMXBean storagePool = null;
            ServerPoolMXBean serverPool = host.getServerPool();
            for (StoragePoolMXBean pool : serverPool.getStoragePools()) {
                for (Volume vol : pool.getVolumes()) {
                    if (vol.getName().startsWith(templateDiskName)) {
                        templateVolume = vol;
                        storagePool = pool;
                        break;
                    } // if
                } // for
            } // for

            VirtualDiskSpec diskSpec = new VirtualDiskSpec();
            diskSpec.setDiskOp(DiskOperation.CREATE_FROM);
            diskSpec.setCapacityMB(1024);
            diskSpec.setVolume(templateVolume);
            diskSpec.setStoragePool(storagePool);

            for (String vmName : hostArch.getValue()) {
                CustomizationSpec customizationSpec = new CustomizationSpec();
                VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

                VNICSpec nicSpec = new VNICSpec();
                nicSpec.setAddressType(MacAddressAssignement.GENERATED);
                nicSpec.setNetworkName("default");

                vmConfigSpec.setName(vmName);
                vmConfigSpec.setMemoryMB(512);
                vmConfigSpec.setNumVCPUs(1);
                vmConfigSpec.setVnicSpecs(Collections.singletonList(nicSpec));
                vmConfigSpec.setProperties(new HashMap<String, String>());
                vmConfigSpec.getProperties().put("bootDevice", "hd");
                vmConfigSpec.setDiskSpecs(Collections.singletonList(diskSpec));

                VirtualMachineMXBean vm = host.createVirtualMachine(vmConfigSpec, customizationSpec, true, true);

                System.out.println("VM " + vm.getNameLabel() + " state is " + vm.getState());
            }
        } // for

    } // void main(String...)

    private static <K extends Object, V extends Object> Map<K, V> asMap(List<K> keys, List<V> values) {
        if (keys.size() == values.size()) {
            Map<K, V> map = new HashMap<K, V>();
            for (int i = 0; i < keys.size(); ++i) {
                map.put(keys.get(i), values.get(i));
            } // for
            return map;
        } // if
        return null;
    } // Map<K, V> asMap(List<K>, List<V>)
} // class SiroccoVMCreator

