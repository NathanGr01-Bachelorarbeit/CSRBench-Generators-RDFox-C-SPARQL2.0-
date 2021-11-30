package eu.planetdata.srbench.oracle.engineYASPER;

import eu.planetdata.srbench.oracle.configuration.Config;
import eu.planetdata.srbench.oracle.query.WindowDefinition;
import eu.planetdata.srbench.oracle.repository.SRBenchImporterYASPER;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.rdf.api.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.abstraction.ContinuousProgram;
import org.streamreasoning.rsp4j.abstraction.QueryTaskAbstractionImpl;
import org.streamreasoning.rsp4j.abstraction.TaskAbstractionImpl;
import org.streamreasoning.rsp4j.api.querying.ContinuousQuery;
import org.streamreasoning.rsp4j.yasper.querying.operators.r2r.Binding;
import org.streamreasoning.rsp4j.yasper.querying.operators.r2r.joins.HashJoinAlgorithm;
import org.streamreasoning.rsp4j.yasper.querying.syntax.TPQueryFactory;

public class YASPERWrapper {
    private static final Logger logger = LoggerFactory.getLogger(YASPERWrapper.class);

    private static YASPERResultObserver<Binding> resultObserver;
    public static long t0;
    public static SRBenchImporterYASPER sr;

    public static void main(String[] args) throws ConfigurationException {
        t0 = System.currentTimeMillis();
        prepareYASPERStreams();
        startYASPERQueries();
        startYasperStream();
        Stopper stopper = new Stopper(resultObserver);
        new Thread(stopper).start();
    }


    private static void prepareYASPERStreams() {
        WindowDefinition win = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getWindowDefinition();
        logger.info("Window: " + win.getSize() + " - " + win.getSlide());
        sr = new SRBenchImporterYASPER("http://inputstream.org");
    }

    private static void startYasperStream() {
        logger.info("Starting stream");
        try {
            logger.debug("importing the data");
            new Thread(sr).start();
            logger.debug("finished import");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startYASPERQueries() {
        String queryId = Config.getInstance().getQuerySet()[0];
        String query = Config.getInstance().getQuery(Config.getInstance().getQuerySet()[0]).getRspqlQuery();
        logger.info("QUERY: " + query);

        ContinuousQuery<Graph, Graph, Binding, Binding> q = TPQueryFactory.parse(query);
        TaskAbstractionImpl<Graph, Graph, Binding, Binding> t =
                new QueryTaskAbstractionImpl.QueryTaskBuilder()
                        .fromQuery(q)
                        .build();
        ContinuousProgram<Graph, Graph, Binding, Binding> cp = new ContinuousProgram.ContinuousProgramBuilder()
                .in(sr)
                .addTask(t)
                .addJoinAlgorithm(new HashJoinAlgorithm())
                .out(q.getOutputStream())
                .build();
        YASPERResultObserver<Binding> dummyConsumer = new YASPERResultObserver<>(queryId);

        q.getOutputStream().addConsumer(dummyConsumer);
    }
}

