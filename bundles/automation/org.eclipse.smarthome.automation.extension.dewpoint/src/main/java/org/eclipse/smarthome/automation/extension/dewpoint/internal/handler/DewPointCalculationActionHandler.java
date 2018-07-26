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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
/**
 * Calculates the dew point based on the given input items for temperature and humidity.
 * Both inputs should be of type Quantity and the output will be Quantity<Temperature> with unit Celsius
 *
 * @author Stefan Triller - Initial Contribution
 *
 */
public class DewPointCalculationActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    private static final String TEMPERATURE_ITEM_STATE = "temperatureItemState";
    private static final String HUMIDITY_ITEM_STATE = "humidityItemState";
    private static final String OUTPUT = "dewPoint";

    public DewPointCalculationActionHandler(Action module) {
        super(module);
    }

    private final Logger logger = LoggerFactory.getLogger(DewPointCalculationActionHandler.class);

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> context) {
        Object temperatureState = context.get(TEMPERATURE_ITEM_STATE);
        Object humidityState = context.get(HUMIDITY_ITEM_STATE);

        if (!(temperatureState instanceof QuantityType<?>) || !(humidityState instanceof QuantityType<?>)) {
            return null;
        }
        QuantityType<Temperature> dewPoint = calculateDewPoint((QuantityType<Temperature>) temperatureState,
                (QuantityType<Dimensionless>) humidityState);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT, dewPoint);

        return result;
    }

    private QuantityType<Temperature> calculateDewPoint(QuantityType<Temperature> tempParam,
            QuantityType<Dimensionless> hum) {

        QuantityType<Temperature> temp;

        if (!tempParam.getUnit().equals(SIUnits.CELSIUS)) {
            temp = tempParam.toUnit(SIUnits.CELSIUS);
        } else {
            temp = tempParam;
        }

        BigDecimal temperature = temp.toBigDecimal();

        // constants for temperature -45°C to 60°C from https://de.wikipedia.org/wiki/Taupunkt
        BigDecimal k2 = new BigDecimal(17.62);
        BigDecimal k3 = new BigDecimal(243.12);

        BigDecimal lnHum = new BigDecimal(Math.log(hum.doubleValue() / 100));

        BigDecimal numerator = ((k2.multiply(temperature)).divide(k3.add(temperature), 4, RoundingMode.HALF_UP))
                .add(lnHum);
        BigDecimal denominator = ((k2.multiply(k3)).divide(k3.add(temperature), 4, RoundingMode.HALF_UP))
                .subtract(lnHum);

        BigDecimal fraction = numerator.divide(denominator, 4, RoundingMode.HALF_UP);

        BigDecimal dewPoint = fraction.multiply(k3, new MathContext(4, RoundingMode.HALF_UP));

        return new QuantityType<>(dewPoint, SIUnits.CELSIUS);
    }
}
