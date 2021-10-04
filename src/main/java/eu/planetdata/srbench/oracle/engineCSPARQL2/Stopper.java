package eu.planetdata.srbench.oracle.engineCSPARQL2;

import eu.planetdata.srbench.oracle.repository.SRBenchImporterCSPARQL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stopper implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Stopper.class);
    private CSPARQL2ResultObserver csparql2ResultObserver;
    private SRBenchImporterCSPARQL2 srBenchImporterCSPARQL2;

    public Stopper(CSPARQL2ResultObserver csparql2ResultObserver) {
        this.csparql2ResultObserver = csparql2ResultObserver;
    }

    @Override
    public void run() {
        while (true) {
            if (CSPARQL2ResultObserver.lastUpdateTime != 0 && System.currentTimeMillis() > (CSPARQL2ResultObserver.lastUpdateTime + 3000)) {
                SRBenchImporterCSPARQL2.setStop(true);
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
