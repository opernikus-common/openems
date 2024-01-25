package io.openems.edge.evcs.hardybarth.ecb1;

public class Consts {
	enum IntConst{
		PHASES_NUM_MAX(3),
		;

	    private final int val;

	    IntConst(final int argVal) {
	        this.val = argVal;
	    }

	    public int val() {
	        return this.val;
	    }
	}

	enum Endpoint{
		AMPERE("ampere"),
		CHARGE_CONTROL("chargecontrol"),
		CHARGE_CONTROLS("chargecontrols"),
		MAIN_SUPPLY("mainsupply"),
		MANUAL("manual"),
		METERS("meters"),
		MAX_CURRENT("maxcurrent"),
		MIN_CURRENT("mincurrent"),
		MODE("mode"),
		PVMODE("pvmode"),
		START("start"),
		STOP("stop"),
		;

	    private final String ep;

	    /**
	     * @param text
	     */
	    Endpoint(final String argEp) {
	        this.ep = argEp;
	    }

	    public String toUri() {
	        return "/" + this.ep;
	    }
	    
	    /* (non-Javadoc)
	     * @see java.lang.Enum#toString()
	     */
	    @Override
	    public String toString() {
	        return this.ep;
	    }
	}

	enum Key{
		BUS_ID("busid"),
		CHARGE_CONTROLS("chargecontrols"),
		ID("id"),
		MANUAL_MODE_AMP("manualmodeamp"),
		MAX_CURRENT("maxcurrent"),
		MIN_CURRENT("mincurrent"),
		MODE("mode"),
		NAME("name"),
		SERIAL("serial"),
		;

	    private final String key;

	    /**
	     * @param text
	     */
	    Key(final String argMode) {
	        this.key = argMode;
	    }

	    /* (non-Javadoc)
	     * @see java.lang.Enum#toString()
	     */
	    @Override
	    public String toString() {
	        return this.key;
	    }
	}
	
	enum ChargingMode{
		MANUAL("manual")
		;

	    private final String mode;

	    /**
	     * @param text
	     */
	    ChargingMode(final String argMode) {
	        this.mode = argMode;
	    }

	    public String toString() {
	        return this.mode;
	    }
	    
	    public final boolean compare(String other) {
	        return this.toString()==other;
	    }
	}
	
	public static String endpointChargeControlMode(int ccindex) {
		return Consts.Endpoint.CHARGE_CONTROLS.toUri() + 
				"/" + ccindex +
				Consts.Endpoint.MODE.toUri(); 
	}
	
	
}
