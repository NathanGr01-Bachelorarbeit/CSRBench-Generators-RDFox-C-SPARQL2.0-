package eu.planetdata.srbench.oracle.engineRDFox;

import org.apache.jena.base.Sys;
import org.openrdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NamedStream implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NamedStream.class);
    private boolean stop = false;
    public int windowSizeInMilliSeconds;
    public int stepSizeInMilliSeconds;
    public long systemStartTime;
    public ServerConnection serverConnection;

    public static Map<String, Long> obsTimestamps = new HashMap<>();
    public Map<Statement, Long> statementsInWindow = new ConcurrentHashMap<>();

    //public ConcurrentLinkedDeque<RdfQuadruple> rdfQuadruples = new ConcurrentLinkedDeque<>();

    public NamedStream(int windowSize, int stepSize, ServerConnection serverConnection) {
        this.windowSizeInMilliSeconds = windowSize;
        this.stepSizeInMilliSeconds = stepSize;
        this.serverConnection = serverConnection;
    }

    @Override
    public void run() {
        long i = 0;

        while (!stop) {

            i++;
            if (i == 1) {
                systemStartTime = System.currentTimeMillis();
            }

            while (systemStartTime + i * stepSizeInMilliSeconds > System.currentTimeMillis()) {
                waitUntil(systemStartTime + i * stepSizeInMilliSeconds);
            }
            try {
                List<Statement> tobeDeleted = RDFoxWrapper.maintainStreamDatastore(new HashMap<>(statementsInWindow),systemStartTime + i * stepSizeInMilliSeconds - windowSizeInMilliSeconds, serverConnection, Prefixes.s_emptyPrefixes);
                for(Statement statement : tobeDeleted)
                    statementsInWindow.remove(statement);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try (DataStoreConnection dataStoreConnection = serverConnection.newDataStoreConnection("Datastore")){
                dataStoreConnection.compact();
            }
            catch (JRDFoxException e) {
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

    public void put(Statement statement) {
        statementsInWindow.put(statement, System.currentTimeMillis());
    }

    public void stop() {
        if (!stop) {
            stop = true;
            logger.info("Stopping namedStream");
        }
    }
}
