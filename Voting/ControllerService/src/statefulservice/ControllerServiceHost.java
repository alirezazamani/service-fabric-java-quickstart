package statefulservice;

import java.io.File;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.logging.Level;

import microsoft.servicefabric.services.runtime.ServiceRuntime;

public class ControllerServiceHost {

    private static final Logger logger = Logger.getLogger(ControllerServiceHost.class.getName());

    public static void main(String[] args) throws Exception{
        try {
            ServiceRuntime.registerStatefulServiceAsync("ControllerServiceType", (context)-> new ControllerService(context), Duration.ofSeconds(10));
            logger.log(Level.INFO, "Registered stateful service of type DataServiceType");
            File myFile = new File("/tmp/ControllerService.txt");
            myFile.createNewFile();
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception occurred", ex);
            throw ex;
        }
    }
}