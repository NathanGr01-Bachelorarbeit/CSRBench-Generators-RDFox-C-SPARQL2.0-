package eu.planetdata.srbench.oracle.engineYASPER;

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
import org.streamreasoning.rsp4j.api.operators.r2r.Var;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.assigner.Consumer;
import org.streamreasoning.rsp4j.yasper.querying.operators.r2r.BindingImpl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.stream.Collectors;

public class YASPERResultObserver<O> implements Consumer<O> {
    private static final Logger logger = LoggerFactory.getLogger(YASPERResultObserver.class);
    private String uri;
    private GeneralResult generalResult;
    public static long lastUpdateTime = 0;

    public YASPERResultObserver(String queryKey) {
        uri = queryKey;
    }

    @Override
    public void notify(O event, long l) {
        //logger.info("Receiving");
        lastUpdateTime = System.currentTimeMillis();

        if (event instanceof BindingImpl) {
            BindingImpl result = (BindingImpl) event;

            //-200 is necessary because the results are always produced around XX800 or XX900 and this way we ensure
            //that we got "fitting" timestamps
            long timestamp = ((int) (System.currentTimeMillis() - YASPERWrapper.t0 - 200) / 1000) * 1000;

            if (uri.equals("srb.query1") || uri.equals("srb.query2") || uri.equals("srb.query5")) {
                Var sensor = result.variables().stream().filter(c -> c.toString().equals("sensor")).findAny().get();
                Var obs = result.variables().stream().filter(c -> c.toString().equals("obs")).findAny().get();
                if (generalResult == null)
                    generalResult = new GeneralResult125();
                (generalResult).relations.add(new Result125(new Head125(new String[]{"sensor", "obs"}), timestamp, new Results125(new Bindings125[]{new Bindings125(timestamp, new Binding125(new TypeValue("uri", removeAngularBracketsFromUrl(result.value(sensor).toString())), new TypeValue("uri", removeAngularBracketsFromUrl(result.value(obs).toString()))))})));
            } else if (uri.equals("srb.query3")) {
                Var sensor = result.variables().stream().filter(c -> c.toString().equals("sensor")).findAny().get();
                Var obs = result.variables().stream().filter(c -> c.toString().equals("obs")).findAny().get();
                Var value = result.variables().stream().filter(c -> c.toString().equals("value")).findAny().get();
                if (generalResult == null)
                    generalResult = new GeneralResult3();
                (generalResult).relations.add(new Result3(new Head3(new String[]{"sensor", "obs", "value"}), timestamp, new Results3(new Bindings3[]{new Bindings3(timestamp, new Binding3(new TypeValue("uri", removeAngularBracketsFromUrl(result.value(sensor).toString())), new TypeValue("uri", removeAngularBracketsFromUrl(result.value(obs).toString())), new TripleTypValue("literal", result.value(value).toString().split("\"")[1], "http://www.w3.org/2001/XMLSchema#double")))})));
            } else if (uri.equals("srb.query4")) {
                Var avg = result.variables().stream().filter(c -> c.toString().equals("avg")).findAny().get();
                if (generalResult == null)
                    generalResult = new GeneralResult4();
                if (result.value(avg).toString().split("\"")[1].length() >= 2) {
                    (generalResult).relations.add(new Result4(new Head4(new String[]{"avg"}), timestamp, new Results4(new Bindings4[]{new Bindings4(timestamp, new Binding4(new TripleTypValue("literal", result.value(avg).toString().split("\"")[1].substring(0, result.value(avg).toString().split("\"")[1].length() - 2), "http://www.w3.org/2001/XMLSchema#double")))})));
                }
            } else if (uri.equals("srb.query6")) {
                Var sensor = result.variables().stream().filter(c -> c.toString().equals("sensor")).findAny().get();
                Var obs = result.variables().stream().filter(c -> c.toString().equals("obs")).findAny().get();
                Var ob1 = result.variables().stream().filter(c -> c.toString().equals("ob1")).findAny().get();
                Var value1 = result.variables().stream().filter(c -> c.toString().equals("value1")).findAny().get();
                if (generalResult == null)
                    generalResult = new GeneralResult6();
                (generalResult).relations.add(new Result6(new Head6(new String[]{"sensor", "ob1", "value1", "obs"}), timestamp, new Results6(new Bindings6[]{new Bindings6(timestamp, new Binding6(new TypeValue("uri", removeAngularBracketsFromUrl(result.value(sensor).toString())), new TypeValue("uri", removeAngularBracketsFromUrl(result.value(obs).toString())), new TypeValue("uri", removeAngularBracketsFromUrl(result.value(ob1).toString())), new TripleTypValue("literal", result.value(value1).toString().split("\"")[1], "http://www.w3.org/2001/XMLSchema#double")))})));
            } else {
                Var sensor = result.variables().stream().filter(c -> c.toString().equals("sensor")).findAny().get();
                Var ob1 = result.variables().stream().filter(c -> c.toString().equals("ob1")).findAny().get();
                if (generalResult == null)
                    generalResult = new GeneralResult7();
                (generalResult).relations.add(new Result7(new Head7(new String[]{"sensor", "ob1"}), timestamp, new Results7(new Bindings7[]{new Bindings7(timestamp, new Binding7(new TypeValue("uri", removeAngularBracketsFromUrl(result.value(sensor).toString())), new TypeValue("uri", removeAngularBracketsFromUrl(result.value(ob1).toString()))))})));
            }
            try (OutputStream out = new FileOutputStream("./src/main/resources/answers/yasper-answer-" + uri.substring(uri.length() - 1) + ".json")) {
                Gson gson = new Gson();
                generalResult.relations = generalResult.relations.stream().distinct().sorted(new Comparator<Result>() {
                    @Override
                    public int compare(Result result, Result t1) {
                        return (int) (result.timestamp - t1.timestamp);
                    }
                }).collect(Collectors.toList());
                out.write(gson.toJson(generalResult).getBytes(StandardCharsets.UTF_8));
                //logger.info(gson.toJson(relations));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private String removeAngularBracketsFromUrl(String s) {
        return s.substring(1, s.length() - 1);
    }
}
