package org.eclipse.smarthome.automation.extension.dewpoint.internal;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.extension.dewpoint.internal.handler.DewPointCalculationActionHandler;
import org.eclipse.smarthome.automation.extension.dewpoint.internal.handler.MultiItemStateChangeTriggerHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@NonNullByDefault
@Component(immediate = true, service = ModuleHandlerFactory.class)
public class DewPointModuleFactory extends BaseModuleHandlerFactory {

    private static final String MODULE_NAME = "DewPointCalculationAction";
    private static final String TRIGGER_NAME = "MultiItemStateChangeTrigger";

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        System.out.println("activate stefans factory");
        this.bundleContext = bundleContext;
    }

    @Override
    public Collection<String> getTypes() {
        return Arrays.asList(MODULE_NAME, TRIGGER_NAME);
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        if (MODULE_NAME.equals(module.getTypeUID())) {
            return new DewPointCalculationActionHandler((Action) module, itemRegistry);
        } else if (TRIGGER_NAME.equals(module.getTypeUID())) {
            return new MultiItemStateChangeTriggerHandler((Trigger) module, bundleContext);
        }
        return null;
    }

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

}
