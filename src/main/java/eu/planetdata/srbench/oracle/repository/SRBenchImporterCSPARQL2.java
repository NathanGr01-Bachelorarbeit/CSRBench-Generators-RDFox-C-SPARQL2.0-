/*******************************************************************************
 * Copyright 2013 Politecnico di Milano, Universidad Politécnica de Madrid
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Authors: Daniele Dell'Aglio, Jean-Paul Calbimonte, Marco Balduini,
 * 			Oscar Corcho, Emanuele Della Valle
 ******************************************************************************/
package eu.planetdata.srbench.oracle.repository;


import eu.planetdata.srbench.oracle.configuration.Config;
import eu.planetdata.srbench.oracle.engineRDFox.NamedStream;
import it.polimi.yasper.core.stream.data.DataStreamImpl;
import it.polimi.yasper.core.stream.web.WebStreamImpl;
import org.apache.jena.base.Sys;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SRBenchImporterCSPARQL2 extends WebStreamImpl implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(SRBenchImporterCSPARQL2.class);
	private int max=0;
	private long interval=0;
	private Set<String> exclude=null;
	private DataStreamImpl<Graph> stream;
	private static boolean stop = false;
	public static Map<String, Long> obsTimestamps = new HashMap<>();

	public SRBenchImporterCSPARQL2(DataStreamImpl<Graph> stream, String uri){
		super(uri);
		max=Config.getInstance().getInputStreamMaxTime();
		interval=Config.getInstance().getInputStreamInterval();
		exclude=Config.getInstance().getInputStreamHoles();
		this.stream = stream;
	}
	
	public void addTimestampedData(File f, long timestamp) throws RDFParseException, IOException, RepositoryException {
		long currentTimestamp = timestamp + System.currentTimeMillis();
		URI graph = BenchmarkVocab.getGraphURI(timestamp);
		Model model1 = ModelFactory.createDefaultModel();
		logger.info("Strings: " + graph.toString() + ", " + BenchmarkVocab.hasTimestamp.toString());
		model1.createLiteralStatement(new ResourceImpl(graph.toString()), new PropertyImpl(BenchmarkVocab.hasTimestamp.toString()), System.currentTimeMillis());
		stream.put(model1.getGraph(), timestamp);
		Model model2 = RDFDataMgr.loadModel(f.getAbsolutePath());
		logger.info("Number of Triples: " + model2.size());
		Iterator<Statement> iterator = model2.listStatements();
		while (iterator.hasNext()) {
			Statement st = iterator.next();
			obsTimestamps.put(st.getSubject().toString(), timestamp);
		}
		stream.put(model2.getGraph(), System.currentTimeMillis());
	}

	
	public void importData(int time) throws RDFParseException, IOException, RepositoryException{
		addTimestampedData(new File("data/data_"+(time < 10 ? "0" + time : time)+".ttl"), time*interval);
	}
	
	public void importAllData() throws RDFParseException, RepositoryException, IOException{
		for (int i=0;i<=max;i++){
			if (!exclude.contains(""+i)) {
				logger.info("Import" + i);
				importData(i);
				try {
					Thread.sleep(Config.getInstance().getInputStreamInterval());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setStream(DataStreamImpl<Graph> stream) {
		this.stream = stream;
	}

	public static void setStop(boolean stop) {
		SRBenchImporterCSPARQL2.stop = stop;
	}

	@Override
	public void run() {
		try {
			importAllData();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RDFParseException e) {
			e.printStackTrace();
		}
		while (!stop) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/*public void getGraphs() throws RepositoryException, MalformedQueryException, QueryEvaluationException{
		RepositoryConnection conn=getRepository().getConnection();
		String qg="SELECT ?g " +
				"FROM <"+BenchmarkVocab.graphList+"> " +
				"WHERE{" +
				"?g <" + BenchmarkVocab.hasTimestamp + "> ?timestamp. " +
				//"FILTER(?timestamp >= "+range.getFrom() + " && ?timestamp < "+range.getTo()+") " +
				"}";
		TupleQuery q=conn.prepareTupleQuery(QueryLanguage.SPARQL, qg);
		TupleQueryResult tqr=q.evaluate();		
		while (tqr.hasNext()){
			BindingSet bind=tqr.next();
	    	logger.info(bind.getValue("g").stringValue());
		}
	}*/
	/*
	public Graph read(String file){
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		Graph myGraph = new TreeModel();
		StatementCollector collector = new StatementCollector(myGraph);
		rdfParser.setRDFHandler(collector);
		
		try {
			rdfParser.parse(new FileInputStream(file), "http://example.com/");
		} catch (RDFParseException | RDFHandlerException
				 | IOException e) {
			logger.error("Error while parsing the file "+ file, e);
		}
		logger.debug("Finished parse");
		return myGraph;
	}*/
	/*public static void main(String[] args)   {
		SRBenchImporter sr=new SRBenchImporter();
		logger.debug("clean repository");
		try {
			sr.clearRepository();
		logger.debug("importing the data");	
	        sr.importAllData();
		logger.debug("finished import");
	
			sr.getGraphs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}
	}*/
}
