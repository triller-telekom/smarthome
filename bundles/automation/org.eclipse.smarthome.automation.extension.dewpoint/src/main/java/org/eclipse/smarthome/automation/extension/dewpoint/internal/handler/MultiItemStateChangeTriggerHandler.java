package org.eclipse.smarthome.automation.extension.dewpoint.internal.handler;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import org.eclipse.smarthome.core.items.events.GroupItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.eclipse.smarthome.core.types.State;
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

    @SuppressWarnings("rawtypes")
    private @Nullable ServiceRegistration eventSubscriberRegistration;

    private final BundleContext bundleContext;
    private final Set<String> types;

    public MultiItemStateChangeTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);

        this.bundleContext = bundleContext;

        HashSet<String> set = new HashSet<>();
        set.add(ItemStateChangedEvent.TYPE);
        set.add(GroupItemStateChangedEvent.TYPE);
        this.types = Collections.unmodifiableSet(set);

        Configuration config = module.getConfiguration();

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
            if (event instanceof ItemStateChangedEvent) {
                ItemStateChangedEvent isce = (ItemStateChangedEvent) event;
                State state = isce.getItemState();
                values.put(isce.getItemName(), state);
                values.put("sourceItem", isce.getItemName());
            }
            if (!values.isEmpty()) {
                ((TriggerHandlerCallback) callback).triggered(this.module, values);
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
