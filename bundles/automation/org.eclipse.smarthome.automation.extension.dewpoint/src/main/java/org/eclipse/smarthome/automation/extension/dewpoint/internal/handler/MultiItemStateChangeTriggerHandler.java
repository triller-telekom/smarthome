/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.automation.extension.dewpoint.internal.handler;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.GroupItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
/**
 * Trigger handler that reacts on ItemStateChanges for a list of item names and outputs their states and which item was
 * changed (sourceItem)
 *
 * @author Stefan Triller - Initial Contribution
 *
 */
public class MultiItemStateChangeTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber {

    private final Logger logger = LoggerFactory.getLogger(MultiItemStateChangeTriggerHandler.class);

    private static final String CONFIG_ITEM_NAMES = "itemName";

    @SuppressWarnings("rawtypes")
    private @Nullable ServiceRegistration eventSubscriberRegistration;

    private final BundleContext bundleContext;
    private final Set<String> types;
    private List<String> itemNames = Collections.emptyList();

    private final ItemRegistry itemRegistry;

    @SuppressWarnings("unchecked")
    public MultiItemStateChangeTriggerHandler(Trigger module, BundleContext bundleContext, ItemRegistry itemRegistry) {
        super(module);

        this.bundleContext = bundleContext;
        this.itemRegistry = itemRegistry;

        HashSet<String> set = new HashSet<>();
        set.add(ItemStateChangedEvent.TYPE);
        set.add(GroupItemStateChangedEvent.TYPE);
        this.types = Collections.unmodifiableSet(set);

        Configuration config = module.getConfiguration();
        this.itemNames = (List<String>) config.get(CONFIG_ITEM_NAMES);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("event.topics", "smarthome/items/*");
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return this.types;
    }

    @Override
    public void receive(Event event) {
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            Map<String, Object> values = new HashMap<>();
            Map<String, Object> output = new HashMap<String, Object>();

            if (event instanceof ItemStateChangedEvent) {
                ItemStateChangedEvent isce = (ItemStateChangedEvent) event;

                String itemName = isce.getItemName();
                if (itemNames.contains(itemName)) {
                    for (String configItem : itemNames) {
                        try {
                            Item item = itemRegistry.getItem(configItem);
                            values.put(item.getName(), item.getState());
                        } catch (ItemNotFoundException e) {
                            logger.error("Did not find configured item '{}'", configItem);
                        }
                    }
                    output.put("sourceItem", itemName);
                }
            }
            if (!values.isEmpty()) {
                output.put("itemStates", values);
                ((TriggerHandlerCallback) callback).triggered(this.module, output);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            eventSubscriberRegistration = null;
        }
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }
}
