package eu.planetdata.srbench.oracle.engineRDFox;

import eu.planetdata.srbench.oracle.configuration.Config;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.client.UpdateType;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

import java.io.*;
import java.util.*;

public class RDFoxWrapper {
    private static final Logger logger = LoggerFactory.getLogger(RDFoxWrapper.class);

    private static RDFoxWrapper rdfoxWrapper;

    private ServerConnection serverConnection;

    public long t0;

    public static void main(String[] args) {
        RDFoxStream stream = new RDFoxStream(getRDFoxWrapper().serverConnection);
        RDFoxResultObserver resultObserver = new RDFoxResultObserver(Config.getInstance().getQuerySet()[0]);
        NamedQuery query = new NamedQuery(stream.sr.stream, resultObserver);
        new Thread(query).start();
        new Thread(stream).start();
        new Thread(stream.sr.stream).start();
        resultObserver.setNamedStream(stream.sr.stream);
        Stopper stopper = new Stopper(stream.sr.stream, query);
        new Thread(stopper).start();

    }

    public static RDFoxWrapper getRDFoxWrapper() {
        if (rdfoxWrapper == null) {
            try {
                rdfoxWrapper = new RDFoxWrapper();
            } catch (JRDFoxException e) {
                e.printStackTrace();
            }
        }
        return rdfoxWrapper;
    }

    private RDFoxWrapper() throws JRDFoxException {
        final String serverURL = "rdfox:local";
        final String roleName = "nathan";
        final String password = "password";

        //RDFox Connection
        Map<String, String> parametersServer = new HashMap<String, String>();
        parametersServer.put("license-file", "/home/nathangruber2001/NathansStudium/Semester6/Bachelorarbeit/Sonstiges/RDFox-linux-x86_64-5.0.0/RDFox.lic");
        logger.debug(Arrays.toString(ConnectionFactory.startLocalServer(parametersServer)));
        ConnectionFactory.createFirstLocalServerRole(roleName, password);
        serverConnection = ConnectionFactory.newServerConnection(serverURL, roleName, password);
        //Datastore
        initializeDatastore();
    }

    public void initializeDatastore() throws JRDFoxException {
        Map<String, String> parametersDatastore = new HashMap<>();
        serverConnection.createDataStore("Datastore", "par-complex-nn", parametersDatastore);
    }

    public static List<Statement> maintainStreamDatastore(Map<Statement, Long> currentWindowStatements, long goOutTime, ServerConnection serverConnection, Prefixes prefixes) throws JRDFoxException {
        List<Statement> toBeDeleted = new ArrayList<>();
        Iterator<Statement> iterator = currentWindowStatements.keySet().iterator();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            if(currentWindowStatements.get(statement) < goOutTime) {
                toBeDeleted.add(statement);
                iterator.remove();
            }
        }
        logger.info("Size toBeDel: " + toBeDeleted.size() + ", size currentWin: " + currentWindowStatements.size());
        DataStoreConnection dataStoreConnection = serverConnection.newDataStoreConnection("Datastore");
        //dataStoreConnection.importData(UpdateType.DELETION, prefixes, reifiedStatementsToTurtleString(toBeDeleted));
        dataStoreConnection.importData(UpdateType.DELETION, prefixes, statementsToTurtleString(toBeDeleted));
        dataStoreConnection.close();
        //logger.debug("Number of changed facts: " + result.getNumberOfChangedFacts());
        //logger.info("WICHTIG1: " + Arrays.toString(currentWindowTripels.toArray()));
        DataStoreConnection dataStoreConnection2 = serverConnection.newDataStoreConnection("Datastore");
        //dataStoreConnection2.importData(UpdateType.ADDITION, prefixes, reifiedStatementsToTurtleString(currentWindowStatements));
        dataStoreConnection2.importData(UpdateType.ADDITION, prefixes, statementsToTurtleString(currentWindowStatements));
        dataStoreConnection2.close();
        //logger.debug("Number of changed facts: " + result.getNumberOfChangedFacts());
        return toBeDeleted;
    }


    public static String statementsToTurtleString(Collection<Statement> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        OutputStream out = new ByteArrayOutputStream();
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        try {
            writer.startRDF();
            for (Statement st: input) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //logger.info(out.toString());
        return out.toString();
    }

    public static String statementsToTurtleString(Map<Statement, Long> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        OutputStream out = new ByteArrayOutputStream();
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        try {
            writer.startRDF();
            for (Statement st: input.keySet()) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //logger.info(out.toString());
        return out.toString();
    }

    public void startStreamUpdating(NamedStream stream) {
        new Thread(stream).start();
    }

    public static URI reifyStatement(Statement statement) {
        return new URIImpl("http://reified.de/" + statement.getSubject().toString().split("/")[statement.getSubject().toString().split("/").length - 1]
                + "/" + statement.getPredicate().toString().split("/")[statement.getPredicate().toString().split("/").length - 1] + "/"
                + statement.getObject().toString().split("/")[statement.getObject().toString().split("/").length - 1]);
    }


    public ServerConnection getServerConnection() {
        return serverConnection;
    }
}

