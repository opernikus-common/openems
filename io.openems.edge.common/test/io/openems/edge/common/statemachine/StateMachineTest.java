package io.openems.edge.common.statemachine;

import static org.junit.Assert.assertThrows;

import org.junit.Assert;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class StateMachineTest {

	public static final String DUPLICATED_ON_ENTRY_ON_EXIT_CALL = "Duplicated OnEntry or OnExit call!";

	public static final String SHOULD_SWITCH_STILL_ACTIVE = "Should switch is still active";

	public static final String SHOULD_SWITCH_NOT_ACTIVE = "Should switch has not been set";

	public class Context extends AbstractContext<DummyComponent> {
		public Context(DummyComponent parent) {
			super(parent);
		}
	}

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		FIRST(0), //
		SECOND(1), //
		THIRD(2), //
		TEST_EXCEPTION(3), //
		;

		private final int value;

		private State(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public OptionsEnum getUndefined() {
			return THIRD;
		}

		@Override
		public State[] getStates() {
			return State.values();
		}
	}

	public class StateMachine extends AbstractStateMachine<State, Context> {

		public StateMachine() {
			super(State.FIRST);
		}

		@Override
		public StateHandler<State, Context> getStateHandler(State state) {
			return switch (state) {
			case FIRST -> new FirstHandler();
			case SECOND -> new SecondHandler();
			case THIRD -> new ThirdHandler();
			case TEST_EXCEPTION -> new TestExceptionHandler();
			};
		}

	}

	public static void setEntryOrExitTestchannel(Channel<?> channel, State value) throws OpenemsException {
		if (channel.getNextValue().asEnum().equals(value)) {
			throw new OpenemsException(DUPLICATED_ON_ENTRY_ON_EXIT_CALL);
		}
		channel.setNextValue(value);
	}

	private class FirstHandler extends StateHandler<State, Context> {

		@Override
		protected State runAndGetNextState(Context context) {
			if (context.getParent().shouldSwitch()) {
				return State.SECOND;
			} else {
				return State.FIRST;
			}
		}

		@Override
		protected void onEntry(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.FIRST);
		}

		@Override
		protected void onExit(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.FIRST);
			context.getParent().toggle();
		}
	}

	private class SecondHandler extends StateHandler<State, Context> {
		@Override
		protected State runAndGetNextState(Context context) {
			if (context.getParent().shouldSwitch()) {
				return State.THIRD;
			} else {
				return State.SECOND;
			}
		}

		@Override
		protected void onEntry(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.SECOND);
		}

		@Override
		protected void onExit(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.SECOND);
			context.getParent().toggle();
		}
	}

	private class ThirdHandler extends StateHandler<State, Context> {
		@Override
		protected State runAndGetNextState(Context context) {
			if (context.getParent().shouldSwitch()) {
				return State.FIRST;
			} else {
				return State.THIRD;
			}
		}

		@Override
		protected void onEntry(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.THIRD);
		}

		@Override
		protected void onExit(Context context) throws OpenemsException {
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.THIRD);
			context.getParent().toggle();
		}
	}

	private class TestExceptionHandler extends StateHandler<State, Context> {

		@Override
		protected State runAndGetNextState(Context context) throws OpenemsException {
			if (context.getParent().shouldSwitch()) {
				return State.FIRST;
			} else {
				throw new OpenemsException(SHOULD_SWITCH_NOT_ACTIVE);
			}
		}

		@Override
		protected void onEntry(Context context) throws OpenemsException {
			if (!context.getParent().shouldSwitch()) {
				throw new OpenemsException(SHOULD_SWITCH_NOT_ACTIVE);
			}
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.TEST_EXCEPTION);
			context.getParent().toggle();
		}

		@Override
		protected void onExit(Context context) throws OpenemsException {
			if (!context.getParent().shouldSwitch()) {
				throw new OpenemsException(SHOULD_SWITCH_NOT_ACTIVE);
			}
			var channel = context.getParent().channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);
			setEntryOrExitTestchannel(channel, State.TEST_EXCEPTION);
			context.getParent().toggle();
		}
	}

	private class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		private final Context context;
		private final StateMachine stateMachine = new StateMachine();
		private boolean shouldSwitch = false;

		public boolean shouldSwitch() {
			return this.shouldSwitch;
		}

		public void toggle() {
			this.shouldSwitch = !this.shouldSwitch;
		}

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

			ON_ENTRY_CALLED_BY(Doc.of(State.values())),

			ON_EXIT_CALLED_BY(Doc.of(State.values())),; //

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values(), //
					DummyComponent.ChannelId.values() //
			);
			super.activate(null, id, "", true);
			this.context = new Context(this);
		}

	}

	@Test
	public void testCycleThroughStates() throws OpenemsError.OpenemsNamedException {
		var component = new DummyComponent("foo0");

		component.stateMachine.run(component.context);

		Assert.assertFalse(component.shouldSwitch());
		var onEntryChannel = component.channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
		var onExitChannel = component.channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);

		Assert.assertEquals(State.FIRST, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.THIRD, this.getNextEntryValue(onExitChannel)); // Default / Undefined
		Assert.assertEquals(State.FIRST, component.stateMachine.getCurrentState()); // current State

		component.toggle();
		Assert.assertTrue(component.shouldSwitch());

		component.stateMachine.run(component.context);
		Assert.assertEquals(State.FIRST, this.getNextEntryValue(onExitChannel)); // onExit Called
		Assert.assertEquals(State.SECOND, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.SECOND, component.stateMachine.getCurrentState()); // current State
		Assert.assertFalse(component.shouldSwitch());

		component.toggle();
		Assert.assertTrue(component.shouldSwitch());

		component.stateMachine.run(component.context);

		Assert.assertEquals(State.SECOND, this.getNextEntryValue(onExitChannel)); // onExit Called
		Assert.assertEquals(State.THIRD, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.THIRD, component.stateMachine.getCurrentState()); // current State

		Assert.assertFalse(component.shouldSwitch());

		component.toggle();
		Assert.assertTrue(component.shouldSwitch());
		component.stateMachine.run(component.context);

		Assert.assertEquals(State.THIRD, this.getNextEntryValue(onExitChannel)); // onExit Called
		Assert.assertEquals(State.FIRST, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.FIRST, component.stateMachine.getCurrentState()); // current State
		Assert.assertFalse(component.shouldSwitch());
	}

	// create Statemachine -> only onEntry once, on Exit once, run n many times

	@Test
	public void testDoubleOnEntry() {
		Exception testExc = assertThrows(OpenemsException.class, () -> {
			var component = new DummyComponent("foo0");
			component.stateMachine.run(component.context);
			component.stateMachine.getStateHandler(component.stateMachine.getCurrentState()).onEntry(component.context);
		});
		String occurredMessage = testExc.getMessage();
		Assert.assertTrue(occurredMessage.contains(DUPLICATED_ON_ENTRY_ON_EXIT_CALL));
	}

	@Test
	public void testDoubleOnExit() {
		Exception testExc = assertThrows(OpenemsException.class, () -> {
			var component = new DummyComponent("foo0");
			component.stateMachine.getStateHandler(component.stateMachine.getCurrentState()).onExit(component.context);
			component.stateMachine.getStateHandler(component.stateMachine.getCurrentState()).onExit(component.context);
		});
		String occurredMessage = testExc.getMessage();
		Assert.assertTrue(occurredMessage.contains(DUPLICATED_ON_ENTRY_ON_EXIT_CALL));
	}

	@Test
	public void assertOnlyOneSwitch() throws OpenemsError.OpenemsNamedException {
		var component = new DummyComponent("foo0");

		component.stateMachine.run(component.context);

		Assert.assertFalse(component.shouldSwitch());
		var onEntryChannel = component.channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
		var onExitChannel = component.channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);

		Assert.assertEquals(State.FIRST, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.THIRD, this.getNextEntryValue(onExitChannel)); // Default / Undefined
		Assert.assertEquals(State.FIRST, component.stateMachine.getCurrentState()); // current State
		component.stateMachine.run(component.context);
		var currentState = component.stateMachine.getCurrentState();
		component.stateMachine.run(component.context);
		component.stateMachine.run(component.context);
		Assert.assertSame(component.stateMachine.getCurrentState(), currentState);

	}

	@Test
	public void forceNextState() throws OpenemsError.OpenemsNamedException {
		var component = new DummyComponent("foo0");

		component.stateMachine.run(component.context);

		Assert.assertFalse(component.shouldSwitch());
		var onEntryChannel = component.channel(DummyComponent.ChannelId.ON_ENTRY_CALLED_BY);
		var onExitChannel = component.channel(DummyComponent.ChannelId.ON_EXIT_CALLED_BY);

		Assert.assertEquals(State.FIRST, this.getNextEntryValue(onEntryChannel)); // onEntry Called
		Assert.assertEquals(State.THIRD, this.getNextEntryValue(onExitChannel)); // Default / Undefined
		Assert.assertEquals(State.FIRST, component.stateMachine.getCurrentState()); // current State
		component.stateMachine.run(component.context);
		component.stateMachine.forceNextState(State.SECOND);
		component.stateMachine.run(component.context);
		Assert.assertSame(component.stateMachine.getCurrentState(), State.SECOND);
		component.stateMachine.run(component.context);

	}

	@Test
	public void exceptionThrows() throws OpenemsError.OpenemsNamedException {

		var component = new DummyComponent("foo0");

		Exception testExc = assertThrows(OpenemsException.class, () -> {
			Assert.assertFalse(component.shouldSwitch()); // do not switch
			component.stateMachine.run(component.context); // initial run
			component.stateMachine.forceNextState(State.TEST_EXCEPTION); // force next State
			component.stateMachine.run(component.context); // switch to Test_exception and call on entry
			component.stateMachine.run(component.context); // run and next state
		});
		String occurredMessage = testExc.getMessage();
		Assert.assertTrue(occurredMessage.contains(SHOULD_SWITCH_NOT_ACTIVE));
		Assert.assertSame(component.stateMachine.getCurrentState(), State.FIRST);
		component.stateMachine.run(component.context);

	}

	private State getNextEntryValue(Channel<?> channel) throws OpenemsError.OpenemsNamedException {
		if (channel instanceof EnumReadChannel eChannel) {
			return eChannel.getNextValue().asEnum();
		}
		throw new OpenemsError.OpenemsNamedException(OpenemsError.GENERIC,
				"Cannot convert TestComponent Channel to EnumReadChannel");
	}

}
