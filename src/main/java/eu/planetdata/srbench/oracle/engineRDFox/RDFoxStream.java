package eu.planetdata.srbench.oracle.engineRDFox;

import eu.planetdata.srbench.oracle.configuration.Config;
import eu.planetdata.srbench.oracle.query.WindowDefinition;
import eu.planetdata.srbench.oracle.repository.SRBenchImporterRDFox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;

public class RDFoxStream implements Runnable{

    protected boolean stop = false;
    private static final Logger logger = LoggerFactory.getLogger(RDFoxStream.class);
    private ServerConnection serverConnection;
    public SRBenchImporterRDFox sr;

    public RDFoxStream(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        WindowDefinition win = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getWindowDefinition();
        logger.info("Window: " + win.getSize() + " - " + win.getSlide());
        sr = new SRBenchImporterRDFox(new NamedStream((int) win.getSize(), (int) win.getSlide(), serverConnection));
    }

    // private List<String> subscribers = new ArrayList<String>();


    @Override
    public void run() {
        logger.info("Starting stream");
        try {
            sr.clearRepository();
            logger.debug("importing the data");
            sr.importAllData();
            logger.debug("finished import");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
