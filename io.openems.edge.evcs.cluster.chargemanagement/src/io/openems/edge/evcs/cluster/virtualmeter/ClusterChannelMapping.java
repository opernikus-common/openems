package io.openems.edge.evcs.cluster.virtualmeter;

import java.util.function.Consumer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.meter.api.ElectricityMeter;

public class ClusterChannelMapping {

	private final ElectricityMeter parent;
	private Channel<Integer> stateChannel;
	private Channel<Integer> chargePowerChannel;
	private Channel<Integer> currentL1Channel;
	private Channel<Integer> currentL2Channel;
	private Channel<Integer> currentL3Channel;
	private Channel<Long> consEnergyChannel;

	private Consumer<Value<Integer>> stateConsumer;
	private Consumer<Value<Integer>> chargePowerConsumer;
	private Consumer<Value<Integer>> currentL1Consumer;
	private Consumer<Value<Integer>> currentL2Consumer;
	private Consumer<Value<Integer>> currentL3Consumer;
	private Consumer<Value<Long>> consEnergyConsumer;

	public ClusterChannelMapping(ElectricityMeter parent) {
		this.parent = parent;
		this.parent._setVoltage(230_000);
		this.parent._setVoltageL1(230_000);
		this.parent._setVoltageL2(230_000);
		this.parent._setVoltageL3(230_000);
		this.parent._setFrequency(50_000);
	}

	/**
	 * Called on Component activate().
	 *
	 * @param evcsCluster the <{@link EvcsClusterChargeMgmt }>
	 */
	public void activate(EvcsClusterChargeMgmt evcsCluster) {

		this.stateChannel = evcsCluster.channel(OpenemsComponent.ChannelId.STATE);
		this.chargePowerChannel = evcsCluster.channel(Evcs.ChannelId.CHARGE_POWER);
		this.currentL1Channel = evcsCluster.channel(Evcs.ChannelId.CURRENT_L1);
		this.currentL2Channel = evcsCluster.channel(Evcs.ChannelId.CURRENT_L2);
		this.currentL3Channel = evcsCluster.channel(Evcs.ChannelId.CURRENT_L3);
		this.consEnergyChannel = evcsCluster.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

		ElectricityMeter.calculateSumCurrentFromPhases(this.parent);

		this.stateConsumer = this.stateChannel.onSetNextValue(newVal -> {
			this.parent.channel(OpenemsComponent.ChannelId.STATE).setNextValue(newVal);
		});
		this.chargePowerConsumer = this.chargePowerChannel.onSetNextValue(newVal -> {
			this.parent.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).setNextValue(newVal);
		});
		this.currentL1Consumer = this.currentL1Channel.onSetNextValue(newVal -> {
			this.parent.channel(ElectricityMeter.ChannelId.CURRENT_L1).setNextValue(newVal);
			this.parent.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1)
					.setNextValue(newVal.isDefined() ? EvcsUtils.currentInMilliampereToPower(newVal.get(), 1) : null);
		});
		this.currentL2Consumer = this.currentL2Channel.onSetNextValue(newVal -> {
			this.parent.channel(ElectricityMeter.ChannelId.CURRENT_L2).setNextValue(newVal);
			this.parent.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2)
					.setNextValue(newVal.isDefined() ? EvcsUtils.currentInMilliampereToPower(newVal.get(), 1) : null);
		});
		this.currentL3Consumer = this.currentL3Channel.onSetNextValue(newVal -> {
			this.parent.channel(ElectricityMeter.ChannelId.CURRENT_L3).setNextValue(newVal);
			this.parent.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3)
					.setNextValue(newVal.isDefined() ? EvcsUtils.currentInMilliampereToPower(newVal.get(), 1) : null);
		});
		this.consEnergyConsumer = this.consEnergyChannel.onSetNextValue(newVal -> {
			this.parent.channel(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY).setNextValue(newVal);

		});
	}

	/**
	 * Deactivates the component.
	 */
	protected void deactivate() {
		if (this.stateChannel == null) {
			return;
		}
		this.stateChannel.removeOnSetNextValueCallback(this.stateConsumer);
		this.chargePowerChannel.removeOnSetNextValueCallback(this.chargePowerConsumer);
		this.currentL1Channel.removeOnSetNextValueCallback(this.currentL1Consumer);
		this.currentL2Channel.removeOnSetNextValueCallback(this.currentL2Consumer);
		this.currentL3Channel.removeOnSetNextValueCallback(this.currentL3Consumer);
		this.consEnergyChannel.removeOnSetNextValueCallback(this.consEnergyConsumer);
		this.stateChannel = null;
	}

}
