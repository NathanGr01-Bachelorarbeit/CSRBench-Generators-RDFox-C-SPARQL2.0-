package eu.planetdata.srbench.oracle.engineRDFox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stopper implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Stopper.class);
    private NamedStream stream;
    private NamedQuery query;

    public Stopper(NamedStream stream, NamedQuery query) {
        this.stream = stream;
        this.query = query;
    }

    @Override
    public void run() {
        while (true) {
            if (RDFoxResultObserver.lastUpdateTime != 0 && System.currentTimeMillis() > (RDFoxResultObserver.lastUpdateTime + 10000)) {
                stream.stop();
                query.stop();
                break;
            }
            try {
                logger.info("Working");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
