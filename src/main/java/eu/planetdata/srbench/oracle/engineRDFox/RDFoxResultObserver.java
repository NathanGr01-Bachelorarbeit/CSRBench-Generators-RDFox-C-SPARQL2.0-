package eu.planetdata.srbench.oracle.engineRDFox;

import com.google.gson.Gson;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.GeneralResult;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Queries125.*;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Query3.*;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Query4.*;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Query6.*;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Query7.*;
import eu.planetdata.srbench.oracle.engineRDFox.Datatypes.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.client.QueryAnswerMonitor;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;
import tech.oxfordsemantic.jrdfox.logic.expression.Resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RDFoxResultObserver implements QueryAnswerMonitor {
    private static final Logger logger = LoggerFactory.getLogger(RDFoxResultObserver.class);
    private GeneralResult generalResult;
    public static long lastUpdateTime = 0;
    private String currentQuery;
    private NamedStream namedStream;

    public RDFoxResultObserver(String queryKey) {
        currentQuery = queryKey;
    }

    @Override
    public void queryAnswersStarted(String[] strings) throws JRDFoxException {
        //logger.info("Hier sind wir angekommen1.");
    }

    @Override
    public synchronized void processQueryAnswer(List<Resource> list, long l) {
        lastUpdateTime = System.currentTimeMillis();
        //logger.info("Results added");
        long timestamp = ((int) (System.currentTimeMillis() - namedStream.systemStartTime) / 1000) * 1000;
        if(currentQuery.equals("srb.query1") || currentQuery.equals("srb.query2") || currentQuery.equals("srb.query5")) {
            if (generalResult == null)
                generalResult = new GeneralResult125();
            (generalResult).relations.add(new Result125(new Head125(new String[]{"sensor", "obs"}), timestamp, new Results125(new Bindings125[]{new Bindings125(timestamp, new Binding125(new TypeValue("uri", list.get(0).toString().substring(1, list.get(0).toString().length() - 1)), new TypeValue("uri", list.get(1).toString().substring(1, list.get(1).toString().length() - 1))))})));
        }
        else if (currentQuery.equals("srb.query3")) {
            if (generalResult == null)
                generalResult = new GeneralResult3();
            (generalResult).relations.add(new Result3(new Head3(new String[]{"sensor", "obs", "value"}), timestamp, new Results3(new Bindings3[]{new Bindings3(timestamp, new Binding3(new TypeValue("uri", list.get(0).toString().substring(1, list.get(0).toString().length() - 1)), new TypeValue("uri", list.get(1).toString().substring(1, list.get(1).toString().length() - 1)), new TripleTypValue("literal", list.get(2).toString().split("\"")[1], "http://www.w3.org/2001/XMLSchema#double")))})));
        }
        else if (currentQuery.equals("srb.query4")) {
            if (generalResult == null)
                generalResult = new GeneralResult4();
            (generalResult).relations.add(new Result4(new Head4(new String[]{"avg"}), timestamp, new Results4(new Bindings4[]{new Bindings4(timestamp, new Binding4(new TripleTypValue("literal", list.get(0).toString().split("\"")[1], "http://www.w3.org/2001/XMLSchema#double")))})));
        }
        else if (currentQuery.equals("srb.query6")) {
            if (generalResult == null)
                generalResult = new GeneralResult6();
            (generalResult).relations.add(new Result6(new Head6(new String[]{"sensor", "ob1", "value1", "obs"}), timestamp, new Results6(new Bindings6[]{new Bindings6(timestamp, new Binding6(new TypeValue("uri", list.get(0).toString().substring(1, list.get(0).toString().length() - 1)), new TypeValue("uri", list.get(3).toString().substring(1, list.get(3).toString().length() - 1)), new TypeValue("uri", list.get(1).toString().substring(1, list.get(1).toString().length() - 1)), new TripleTypValue("literal", list.get(2).toString().split("\"")[1], "http://www.w3.org/2001/XMLSchema#double")))})));
        }
        else {
            if (generalResult == null)
                generalResult = new GeneralResult7();
            (generalResult).relations.add(new Result7(new Head7(new String[]{"sensor", "ob1"}), timestamp, new Results7(new Bindings7[]{new Bindings7(timestamp, new Binding7(new TypeValue("uri", list.get(0).toString().substring(1, list.get(0).toString().length() - 1)), new TypeValue("uri", list.get(1).toString().substring(1, list.get(1).toString().length() - 1))))})));
        }
        try (OutputStream out = new FileOutputStream("rdfox-answer-" + currentQuery.substring(currentQuery.length()-1) + ".json")) {
            Gson gson = new Gson();
            generalResult.relations = generalResult.relations.stream().distinct().sorted(new Comparator<Result>() {
                @Override
                public int compare(Result result, Result t1) {
                    return (int)(result.timestamp - t1.timestamp);
                }
            }).collect(Collectors.toList());
            out.write(gson.toJson(generalResult).getBytes(StandardCharsets.UTF_8));
            //logger.info(gson.toJson(relations));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void queryAnswersFinished() throws JRDFoxException {
        //logger.info("Hier sind wir angekommen3.");
    }

    public void setNamedStream(NamedStream namedStream) {
        this.namedStream = namedStream;
    }
}
