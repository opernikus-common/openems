package io.openems.edge.heater.chp.dachs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater CHP Dachs GLT-Interface",
        description = "Implements the Senertec Dachs CHP.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "chp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "URL", description = "IP address of the GLT web interface, e.g. http://127.0.0.1 or http://127.0.0.1:8080")
    String url() default "http://127.0.0.1";

    @AttributeDefinition(name = "Username", description = "Username for the GLT web interface")
    String username() default "glt";

    @AttributeDefinition(name = "Password", description = "Password for the GLT web interface (default '')", type = AttributeType.PASSWORD)
    String password() default "";

    @AttributeDefinition(name = "Polling interval", description = "Number of core cycle times between read requests.")
    int interval() default 10;
    
    @AttributeDefinition(name = "Read only", description = "Only read values from device, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Verbose", description = "Shows debug information on HTTP requests")
	boolean verbose() default false;

    String webconsole_configurationFactory_nameHint() default "Heater CHP Dachs GLT-Interface [{id}]";

}