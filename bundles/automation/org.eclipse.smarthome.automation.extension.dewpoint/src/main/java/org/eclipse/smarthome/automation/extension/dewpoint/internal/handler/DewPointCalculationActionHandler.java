package org.eclipse.smarthome.automation.extension.dewpoint.internal.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.ModuleHandlerCallback;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class DewPointCalculationActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    private static final String TEMPERATURE_ITEM_NAME = "temperatureItemName";
    private static final String HUMIDITY_ITEM_NAME = "humidityItemName";

    private final ItemRegistry itemRegistry;

    public DewPointCalculationActionHandler(Action module, ItemRegistry itemRegistry) {
        super(module);
        this.itemRegistry = itemRegistry;
    }

    private final Logger logger = LoggerFactory.getLogger(DewPointCalculationActionHandler.class);

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        logger.debug("dispose");
    }

    @Override
    public void setCallback(ModuleHandlerCallback callback) {
        // TODO Auto-generated method stub
        logger.debug("set callback");
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> context) {
        logger.debug("execute");

        String tempItemName = (String) context.get(TEMPERATURE_ITEM_NAME);
        String humItemName = (String) context.get(HUMIDITY_ITEM_NAME);

        Item tempItem = itemRegistry.get(tempItemName);
        Item humItem = itemRegistry.get(humItemName);

        return null;
    }

}
