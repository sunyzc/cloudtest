package com.unibeta.cloudtest.config.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Param;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Plugin;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Recorder;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * Manage all plugin config files's data, provide all configuration data
 * accessing.
 * 
 * @author jordan.xue
 */
public class PluginConfigProxy {

    private static PluginConfig pluginConfig = null;
    private static long lastModified = 0;

    private static Map<String, CloudTestPlugin> cloudTestPluginInstancesMap = new HashMap<String, CloudTestPlugin>();
    private static Map<String, String> paramValueMap = new HashMap<String, String>();

    /**
     * Get plugin instance by id.
     * 
     * @param id
     * @param clazz
     * @return
     * @throws Exception
     *             if the geiven class is not assignable from return instance.
     */
    public static CloudTestPlugin getCloudTestPluginInstance(String id)
            throws Exception {

        CloudTestPlugin cloudTestPlugin = null;

        if (CommonUtils.isNullOrEmpty(id)) {
            return null;
        } else {
            init();
        }

        cloudTestPlugin = cloudTestPluginInstancesMap.get(id);

        if (null == cloudTestPlugin) {
            throw new Exception("plguin id '" + id + "' was not found.");
        }

        return cloudTestPlugin;
    }

    /**
     * Get the parameter value by the given name, which is defined in
     * PluginConfig.xml file.
     * 
     * @param paramName
     * @return
     * @throws Exception
     */
    public static String getParamValueByName(String paramName) throws Exception {

        if (null == paramName) {
            return null;
        }

        init();

        return paramValueMap.get(paramName.toUpperCase());
    }

    /**
     * Get the slave server info defined in PluginConfig.xml file.
     * 
     * @return
     * @throws Exception
     */
    public static Map<String, PluginConfig.SlaveServer> getSlaveServerMap() {

        Map<String, PluginConfig.SlaveServer> map = new HashMap<String, PluginConfig.SlaveServer>();
        List<PluginConfig.SlaveServer> list;
        try {
            list = loadGlobalPluginConfig().server;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return map;
        }

        for (PluginConfig.SlaveServer s : list) {
            map.put(s.id, s);

        }
        return map;
    }

    /**
     * Get the case recorder info defined in PluginConfig.xml file.
     * 
     * @return
     * @throws Exception
     */
    public static Map<String, Recorder> getCaseRecordersMap() {

        Map<String, Recorder> map = new HashMap<String, Recorder>();
        List<PluginConfig.Recorder> list;
        try {
            list = loadGlobalPluginConfig().recorder;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return map;
        }

        return map;
    }

    /**
     * Get the case recorder info defined in PluginConfig.xml file.
     * 
     * @return
     * @throws Exception
     */
    public static Recorder getSinglePowerOnCaseRecorder() {

        List<PluginConfig.Recorder> list;
        try {
            list = loadGlobalPluginConfig().recorder;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }

        for (Recorder r : list) {
            if (!"true".equalsIgnoreCase(r.poweroff)) {
                return r;
            }
        }

        return null;
    }

    private static void initParamValueMap() throws Exception {

        paramValueMap.clear();
        List<Param> list = loadGlobalPluginConfig().param;

        for (Param e : list) {
            if (!CommonUtils.isNullOrEmpty(e.name)) {
                paramValueMap.put(e.name.toUpperCase(), e.value.trim());
            }
        }

    }

    private static void init() throws Exception {

        File CONFIG_FILE = new File(
                ConfigurationProxy.getConfigurationFilePath());

        if (CONFIG_FILE.exists()&& lastModified != CONFIG_FILE.lastModified()) {
            initCloudTestPluginInstancesMap();
            initParamValueMap();

            lastModified = CONFIG_FILE.lastModified();
        }

    }

    public static void refresh() throws Exception {

        initCloudTestPluginInstancesMap();
        initParamValueMap();
    }

    private static void initCloudTestPluginInstancesMap() throws Exception {

        List<Plugin> list = loadGlobalPluginConfig().plugin;
        cloudTestPluginInstancesMap.clear();

        for (Plugin e : list) {
            if (!CommonUtils.isNullOrEmpty(e.className)
                    && !CommonUtils.isNullOrEmpty(e.id)) {
                try {
                    Class implementation = Class.forName(e.className.trim());
                    Class defination = Class.forName(e.id.trim());

                    if (!defination.isAssignableFrom(implementation)) {
                        throw new Exception("given plguin '" + e.id
                                + "' is not assignable from '" + e.className
                                + "'");
                    } else {

                        Object newInstance = implementation.newInstance();
                        if (newInstance instanceof CloudTestPlugin) {
                            cloudTestPluginInstancesMap.put((e.id),
                                    (CloudTestPlugin) newInstance);
                        } else {
                            throw new Exception("given "
                                    + implementation.getName()
                                    + " Plugin component have to extends '"
                                    + CloudTestPlugin.class.getName() + "'. ");
                        }
                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * Loads <code>PluginConfig</code> from PluginConfig.xml file.
     * 
     * @return
     * @throws Exception
     */
    public static PluginConfig loadGlobalPluginConfig() throws Exception {

        pluginConfig = (PluginConfig) ObjectSerializer.unmarshalToObject(
                XmlUtils.paserDocumentToString(XmlUtils
                        .getDocumentByFileName(ConfigurationProxy
                                .getConfigurationFilePath())),
                PluginConfig.class);

        return pluginConfig;
    }

    // public static void main(String[] args) {
    // PluginConfig pluginConfig = new PluginConfig();
    //
    // List<PluginElement> peList1 = new ArrayList<PluginElement>();
    // List<ParamElement> peList2 = new ArrayList<ParamElement>();
    //
    // PluginElement element1 = new PluginElement();
    // ParamElement element2 = new ParamElement();
    //
    // peList1.add(element1);
    // peList2.add(element2);
    //
    // pluginConfig.paramElement = peList2;
    // pluginConfig.pluginElement = peList1;
    //
    // try {
    // String str = ObjectSerializer.marshalToXml(pluginConfig);
    // // System.out.println(str);
    // } catch (Exception e) {
    // // TODO Auto-generated catch block
    // }
    //
    // }
}
