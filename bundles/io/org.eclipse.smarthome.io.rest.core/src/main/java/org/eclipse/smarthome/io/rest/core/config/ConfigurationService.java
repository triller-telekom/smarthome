/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.core.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.MultipleInstanceServiceInfo;
import org.eclipse.smarthome.io.rest.core.internal.RESTCoreActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ConfigurationService} manages configurations in the {@link ConfigurationAdmin}. The config id is the
 * equivalent to the {@link Constants#SERVICE_PID}.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
@Component(service = ConfigurationService.class)
public class ConfigurationService {

    private ConfigurationAdmin configurationAdmin;

    private final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    /**
     * Returns a configuration for a config id.
     *
     * @param configId config id
     * @return config or null if no config with the given config id exists
     * @throws IOException if configuration can not be read
     */
    public Configuration get(String configId) throws IOException {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(configId, null);
        Dictionary<String, Object> properties = configuration.getProperties();
        return toConfiguration(properties);
    }

    /**
     * Creates or updates a configuration for a config id.
     *
     * @param configId config id
     * @param newConfiguration the configuration
     * @return old config or null if no old config existed
     * @throws IOException if configuration can not be stored
     */
    public Configuration update(String configId, Configuration newConfiguration) throws IOException {
        return update(configId, newConfiguration, false);
    }

    public String getProperty(String servicePID, String key) {
        try {
            org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(servicePID, null);
            if (configuration != null && configuration.getProperties() != null) {
                return (String) configuration.getProperties().get(key);
            }
        } catch (IOException e) {
            logger.debug("Error while retrieving property {} for PID {}.", key, servicePID);
        }

        return null;
    }

    /**
     * Creates or updates a configuration for a config id.
     *
     * @param configId config id
     * @param newConfiguration the configuration
     * @param override if true, it overrides the old config completely. means it deletes all parameters even if they are
     *            not defined in the given configuration.
     * @return old config or null if no old config existed
     * @throws IOException if configuration can not be stored
     */
    public Configuration update(String configId, Configuration newConfiguration, boolean override) throws IOException {

        org.osgi.service.cm.Configuration configuration = null;
        if (newConfiguration.containsKey(ConfigConstants.SERVICE_CONTEXT)) {

            try {
                configuration = getConfigurationWithContext(configId);
            } catch (InvalidSyntaxException e) {
                logger.error("Failed to lookup config for PID '{}'", configId);
            }
            if (configuration == null) {
                configuration = configurationAdmin.createFactoryConfiguration(configId, null);
                // obtain configdescription from the transportservice

                String configDescriptionURI = getConfigDescriptionURI(configId);

                Dictionary<String, Object> props = getProperties(configuration);
                props.put(ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI, configDescriptionURI);
                configuration.update(props);
            }
        } else {
            configuration = configurationAdmin.getConfiguration(configId, null);
        }

        Configuration oldConfiguration = toConfiguration(configuration.getProperties());
        Dictionary<String, Object> properties = getProperties(configuration);
        Set<Entry<String, Object>> configurationParameters = newConfiguration.getProperties().entrySet();
        if (override) {
            Set<String> keySet = oldConfiguration.keySet();
            for (String key : keySet) {
                properties.remove(key);
            }
        }
        for (Entry<String, Object> configurationParameter : configurationParameters) {
            Object value = configurationParameter.getValue();
            if (value == null) {
                properties.remove(configurationParameter.getKey());
            } else if (value instanceof String || value instanceof Integer || value instanceof Boolean
                    || value instanceof Object[] || value instanceof Collection) {
                properties.put(configurationParameter.getKey(), value);
            } else {
                // the config admin does not support complex object types, so let's store the string representation
                properties.put(configurationParameter.getKey(), value.toString());
            }
        }
        configuration.update(properties);
        return oldConfiguration;
    }

    private String getConfigDescriptionURI(String configId) {
        ServiceReference<?>[] multiServiceReferences;
        try {
            multiServiceReferences = RESTCoreActivator.getBundleContext()
                    .getServiceReferences(MultipleInstanceServiceInfo.class.getName(), null);
            if (multiServiceReferences != null) {
                for (ServiceReference<?> serviceReference : multiServiceReferences) {
                    MultipleInstanceServiceInfo mis = (MultipleInstanceServiceInfo) RESTCoreActivator.getBundleContext()
                            .getService(serviceReference);

                    if (mis.getServicePID().equals(configId)) {
                        return mis.getConfigDescriptionUri();
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Could not retrieve service reference for service id '{}'", configId, e);
        }
        return null;
    }

    private org.osgi.service.cm.Configuration getConfigurationWithContext(String serviceId)
            throws IOException, InvalidSyntaxException {

        org.osgi.service.cm.Configuration[] configs = configurationAdmin
                .listConfigurations("(&(service.pid=" + serviceId + "))");

        if (configs == null) {
            return null;
        }
        if (configs.length > 1) {
            throw new IllegalStateException("More than one configuration with PID " + serviceId + " exists");
        }

        return configs[0];
    }

    /**
     * Deletes a configuration for a config id.
     *
     * @param configId config id
     * @return old config or null if no old config existed
     * @throws IOException if configuration can not be removed
     */
    public Configuration delete(String configId) throws IOException {
        org.osgi.service.cm.Configuration serviceConfiguration = configurationAdmin.getConfiguration(configId, null);
        Configuration oldConfiguration = toConfiguration(serviceConfiguration.getProperties());
        serviceConfiguration.delete();
        return oldConfiguration;
    }

    private Configuration toConfiguration(Dictionary<String, Object> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<String, Object> properties = new HashMap<>(dictionary.size());
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!key.equals(Constants.SERVICE_PID)) {
                properties.put(key, dictionary.get(key));
            }
        }
        return new Configuration(properties);
    }

    private Dictionary<String, Object> getProperties(org.osgi.service.cm.Configuration configuration) {
        Dictionary<String, Object> properties = configuration.getProperties();
        return properties != null ? properties : new Hashtable<String, Object>();
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
