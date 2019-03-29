package javax.jnlp;

import java.util.HashMap;
import java.util.Map;

public final class ServiceManager {

    private static ServiceManagerStub stub = null;

    private static Map<String, Object> lookupTable = new HashMap<String, Object>(); // ensure lookup is idempotent

    private ServiceManager() {
        // says it can't be instantiated
    }

}
