package eu.planetdata.srbench.oracle.engineYASPER;

import eu.planetdata.srbench.oracle.repository.SRBenchImporterCSPARQL2;
import eu.planetdata.srbench.oracle.repository.SRBenchImporterYASPER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stopper implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Stopper.class);
    private YASPERResultObserver yasperResultObserver;
    private SRBenchImporterCSPARQL2 srBenchImporterCSPARQL2;

    public Stopper(YASPERResultObserver yasperResultObserver) {
        this.yasperResultObserver = yasperResultObserver;
    }

    @Override
    public void run() {
        while (true) {
            if (YASPERResultObserver.lastUpdateTime != 0 && System.currentTimeMillis() > (YASPERResultObserver.lastUpdateTime + 3000)) {
                SRBenchImporterYASPER.setStop(true);
                break;
            }
            try {
                //logger.info("Working");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
