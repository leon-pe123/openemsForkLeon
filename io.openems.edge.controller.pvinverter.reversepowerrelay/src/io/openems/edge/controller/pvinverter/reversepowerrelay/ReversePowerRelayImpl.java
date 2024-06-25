package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

import io.openems.edge.common.channel.IntegerReadChannel;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.ReversePowerRelay", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ReversePowerRelayImpl extends AbstractOpenemsComponent
		implements ReversePowerRelay, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ReversePowerRelayImpl.class);

	@Reference
	private ComponentManager componentManager;

	private String pvInverterId;
	/** The configured Power Limit. */
	private int powerLimit = 0;

	private int powerLimit30Percent = 0;
	private int powerLimit60Percent = 0;

	ChannelAddress inputChannelAddress0Percent = null;
	ChannelAddress inputChannelAddress30Percent = null;
	ChannelAddress inputChannelAddress60Percent = null;
	ChannelAddress inputChannelAddress100Percent = null;

	private ManagedSymmetricPvInverter pvInverter;

	public ReversePowerRelayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ReversePowerRelay.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.pvInverterId = config.pvInverter_id();

		this.powerLimit30Percent = config.powerLimit30();
		this.powerLimit60Percent = config.powerLimit60();

		try {
			this.inputChannelAddress0Percent = ChannelAddress.fromString(config.inputChannelAddress0Percent());
			this.inputChannelAddress30Percent = ChannelAddress.fromString(config.inputChannelAddress30Percent());

			this.inputChannelAddress60Percent = ChannelAddress.fromString(config.inputChannelAddress60Percent());
			this.inputChannelAddress100Percent = ChannelAddress.fromString(config.inputChannelAddress100Percent());

		} catch (OpenemsNamedException e) {
			log.error("Error parsing channel addresses", e);
		}
	}

	@Deactivate
	protected void deactivate() {
		// Reset limit
		ManagedSymmetricPvInverter pvInverter;
		try {
			pvInverter = this.componentManager.getComponent(this.pvInverterId);
			pvInverter.setActivePowerLimit(null);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
		}
		super.deactivate();
	}

	private void setPvLimit(Integer powerLimit) {

		try {
			pvInverter = this.componentManager.getComponent(this.pvInverterId);
			if (pvInverter != null) {
				pvInverter.setActivePowerLimit(this.powerLimit);
			}
		} catch (OpenemsNamedException e) {
			log.error("Error setting PV limit", e);
		}

	}

	private int getChannelValue(ChannelAddress address) throws OpenemsNamedException {
		IntegerReadChannel channel = this.componentManager.getChannel(address);
		return channel.value().orElse(0);
	}

	@Override
	public void run() throws OpenemsNamedException {

		int value0Percent = getChannelValue(inputChannelAddress0Percent);
		int value30Percent = getChannelValue(inputChannelAddress30Percent);
		int value60Percent = getChannelValue(inputChannelAddress60Percent);
		int value100Percent = getChannelValue(inputChannelAddress100Percent);

		//
		if (value0Percent == 1) {
			setPvLimit(0);
		} else if (value0Percent == 0 && value30Percent == 1 && value60Percent == 0 && value100Percent == 0) {
			setPvLimit(powerLimit30Percent);
		} else if (value0Percent == 0 && value30Percent == 0 && value60Percent == 1 && value100Percent == 0) {
			setPvLimit(powerLimit60Percent);
		} else if (value0Percent == 0 && value30Percent == 0 && value60Percent == 0 && value100Percent == 1) {
			setPvLimit(null);
		} else {
			setPvLimit(0);
		}

	}
}
