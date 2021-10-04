package eu.planetdata.srbench.oracle.engineRDFox;

import eu.planetdata.srbench.oracle.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.QueryAnswerMonitor;

import java.util.HashMap;

public class NamedQuery implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NamedQuery.class);

    public String query;
    public NamedStream stream;
    public int answerFrequencyInMilliSeconds;
    public QueryAnswerMonitor queryAnswerMonitor;
    public boolean stop = false;

    public NamedQuery(NamedStream stream, QueryAnswerMonitor queryAnswerMonitor) {
        this.query = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getBooleanQuery();
        this.stream = stream;
        //this.answerFrequencyInMilliSeconds = (Config.getInstance().getInputStreamInterval()).intValue();
        this.answerFrequencyInMilliSeconds = (int) Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getWindowDefinition().getSlide();
        this.queryAnswerMonitor = queryAnswerMonitor;
    }


    @Override
    public void run() {
        logger.info("QUERY: " + query);
        final long queryStartTime = System.currentTimeMillis();
        long i = 1;
        while (!stop) {
            waitUntil(queryStartTime + i * answerFrequencyInMilliSeconds);
            i++;
            try {
                RDFoxWrapper.getRDFoxWrapper().getServerConnection().newDataStoreConnection("Datastore").evaluateQuery(Prefixes.s_defaultPrefixes, query, new HashMap<>(), queryAnswerMonitor);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void waitUntil(long targetTime) {
        long millis = targetTime - System.currentTimeMillis();
        if (millis <= 0)
            return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        if (!stop) {
            stop = true;
            logger.info("Stopping query");
            stream.stop();
        }
    }
}
