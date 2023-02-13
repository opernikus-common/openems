package io.openems.edge.heater.chp.dachs;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.OperationModeRequest;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Methods to access the "Dachs" and prepare/parse in/outgoing data.
 */
public class DachsDevice {

    private static final int NORMAL_OPERATION_RPM_THRESHOLD = 1800;

    private static final String COMMON_QUERY_GLT_B_ACTIVE = "Stromf_Ew.Anforderung_GLT.bAktiv=";
    private static final String COMMON_QUERY_GLT_B_NUMBER_OF_MODULES = "Stromf_Ew.Anforderung_GLT.bAnzahlModule=";

    private final DachsGltImpl parent;
    private final HttpTools httptools;

    private boolean parseReadResultWarning = false;

    protected DachsDevice(DachsGltImpl parent) {
        this.parent = parent;
        this.httptools = new HttpTools(this.parent.getConfig().url(), this.parent.getConfig().username(),
                this.parent.getConfig().password());
    }

    protected void readValuesAndMapToChannel() throws OpenemsException {
		/* Important Note: we can -NOT- request all values in one request, because DACHS
		 in "nachtabsenkung"(lower performance in the night)
		 will fail on this request (even if "nachtabsenkung" is
		 disabled again)
		 */

        this.parseReadResultWarning = false;
        try {
            // tbh idk if collect is necessary -> i think filter().forEach() should be ok enoug -> needs to be debugged
            Arrays.stream(DachsGlt.ChannelId.values()).filter(channelId -> channelId.getKey() != null)
                    .collect(Collectors.toList()).forEach(channelId -> {
                var key = channelId.getKey();
                String res;
                try {
                    res = this.httptools.readKey(key);
                } catch (OpenemsException e) {
                    throw new RuntimeException(e);
                }
                res = extractValue(res);
                //internal method to cast values
                //if null/empty String sets to null automatically
                this.parent.channel(channelId).setNextValue(channelId.applyConverter(res));
                if (this.parent.getConfig().verbose()) {
                    this.parent.logInfo("Request: " + key);
                    this.parent.logInfo("Response: " + res);
                }
                try {
                    Thread.sleep(60);
                } catch (InterruptedException ignored) {
                }
            });
        } catch (RuntimeException e) {
            throw new OpenemsException(e);
        }

        this.parent.channel(DachsGlt.ChannelId.WARNING).setNextValue(this.parent.getStateWarning().getValue() > 0);
        this.parent.channel(DachsGlt.ChannelId.ERROR).setNextValue(this.parent.getStateError().getValue() > 0);

        this.updateGeneralState();

        this.parent._setReadResultWarning(this.parseReadResultWarning);
    }

    private String extractValue(String response) {
        String res = response.replaceAll(".*=", "").trim();
        return res.substring(0, res.indexOf("/n"));
    }

    private void updateGeneralState() {
        if (this.parseReadResultWarning) {
            this.parent._setHeaterState(HeaterState.UNDEFINED);
        } else if (this.parent.getRpm().orElse(0) > NORMAL_OPERATION_RPM_THRESHOLD) {
            this.parent._setHeaterState(HeaterState.RUNNING);
        } else if (this.parent.getHeatRunRequested().orElse(0) > 0) {
            this.parent._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT);
        } else if (this.parent.getRunClearance().orElse(0) > 0) {
            this.parent._setHeaterState(HeaterState.STANDBY);
        } else {
            this.parent._setHeaterState(HeaterState.BLOCKED_OR_ERROR);
        }
    }

    /**
     * @return true, if the heater can be or is activated.
     */
    protected boolean available() {
        HeaterState hs = this.parent.getHeaterState();
        return (hs == HeaterState.RUNNING || hs == HeaterState.STANDBY || hs == HeaterState.STARTING_UP_OR_PREHEAT);
    }

    private void activateDachs() throws OpenemsException {
        String result = this.httptools
                .writeKeys(COMMON_QUERY_GLT_B_ACTIVE + "1&" + COMMON_QUERY_GLT_B_NUMBER_OF_MODULES + "1");

        if (this.parent.getConfig().verbose()) {
            this.parent.logInfo(result);
        }
        // TODO result auswerten: use HttpTools.extractValueFromMessage
    }

    private void deactivateDachs() throws OpenemsException {
        String result = this.httptools.writeKeys(COMMON_QUERY_GLT_B_ACTIVE + "0");

        if (this.parent.getConfig().verbose()) {
            this.parent.logInfo(result);
        }
        // TODO result auswerten: use HttpTools.extractValueFromMessage
    }

    /*
     * This is the on-off switch. There are some things to watch out for: - This is
     * not a hard command, especially the ’off’ command. The Dachs has a list of
     * reasons to be running (see ’Dachs-Lauf-Anforderungen’), the ’external
     * requirement’ (this on/off switch) being one of many. If any one of those
     * reasons is true, it is running. Only if all of them are false, it will shut
     * down. Bottom line, deactivateDachs() only does something if nothing else
     * tells the Dachs to run. And activateDachs() might be ignored because of a
     * limitation. - Timing: need to send ’on’ command at least every 10 minutes for
     * the Dachs to keep running. ’interval’ is capped at 9 minutes, so this should
     * be taken care of. - Also: You cannot switch a CHP on/off as you want. Number
     * of starts should be minimized, and because of that there is a limit/hour on
     * how often you can start. If the limit is reached, the chp won't start.
     * Currently, the code does not enforce any restrictions to not hit that limit!
     */
    public void handleActivationState() throws OpenemsException {
        if (this.parent.getOperationModeRequestChannel().getNextWriteValue().orElse(OperationModeRequest.OFF).equals(OperationModeRequest.ON)) {
            this.activateDachs();
        } else {
            deactivateDachs();
        }
    }
}
