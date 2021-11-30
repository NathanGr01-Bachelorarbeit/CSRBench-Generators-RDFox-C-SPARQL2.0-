package eu.planetdata.srbench.oracle.engineCSPARQL2;

import eu.planetdata.srbench.oracle.configuration.Config;
import eu.planetdata.srbench.oracle.query.WindowDefinition;
import eu.planetdata.srbench.oracle.repository.SRBenchImporterCSPARQL2;
import it.polimi.sr.rsp.csparql.engine.CSPARQLEngine;
import it.polimi.yasper.core.engine.config.EngineConfiguration;
import it.polimi.yasper.core.querying.ContinuousQuery;
import it.polimi.yasper.core.querying.ContinuousQueryExecution;
import it.polimi.yasper.core.sds.SDSConfiguration;
import it.polimi.yasper.core.stream.data.DataStreamImpl;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.jena.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

public class CSPARQL2Wrapper {
    private static final Logger logger = LoggerFactory.getLogger(CSPARQL2Wrapper.class);

    private static CSPARQLEngine csparqlEngine;
    private static SDSConfiguration config;
    private static CSPARQL2ResultObserver resultObserver;
    public static long t0;

    public static void main(String[] args) throws ConfigurationException, FileNotFoundException {
        EngineConfiguration ec = EngineConfiguration.loadConfig("/csparqlsetup.properties");
        String path = CSPARQL2Wrapper.class.getResource("/csparqlsetup.properties").getPath();
        config = new SDSConfiguration(path);
        t0 = System.currentTimeMillis();
        csparqlEngine = new CSPARQLEngine(t0, ec);
        startCSPARQL2Streams();
        startCSPARQL2Queries();
        Stopper stopper = new Stopper(resultObserver);
        new Thread(stopper).start();
    }


    private static void startCSPARQL2Streams() {
        WindowDefinition win = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getWindowDefinition();
        logger.info("Window: " + win.getSize() + " - " + win.getSlide());
        SRBenchImporterCSPARQL2 sr = new SRBenchImporterCSPARQL2(null, "http://inputstream.org");
        DataStreamImpl<Graph> register = csparqlEngine.register(sr);
        logger.info("RETURN: " + register.toString() + " " + (register == null));
        sr.setStream(register);
        logger.info("Starting stream");
        try {
            logger.debug("importing the data");
            new Thread(sr).start();
            logger.debug("finished import");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startCSPARQL2Queries () throws FileNotFoundException {
        String queryId = Config.getInstance().getQuerySet()[0];
        String query = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getRspqlQuery();
        logger.info("QUERY: " + query);
        ContinuousQueryExecution cqe = csparqlEngine.register(query, config);
        ContinuousQuery q = cqe.getContinuousQuery();
        CSPARQL2Wrapper.resultObserver = new CSPARQL2ResultObserver(queryId,"TABLE", false);
        cqe.add(resultObserver);
    }
}

