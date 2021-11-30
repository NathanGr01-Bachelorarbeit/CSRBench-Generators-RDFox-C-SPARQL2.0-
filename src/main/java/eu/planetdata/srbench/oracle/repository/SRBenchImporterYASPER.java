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
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.riot.RDFDataMgr;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.streamreasoning.rsp4j.api.RDFUtils;
import org.streamreasoning.rsp4j.yasper.examples.RDFStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SRBenchImporterYASPER extends RDFStream implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(SRBenchImporterYASPER.class);
	private int max=0;
	private long interval=0;
	private Set<String> exclude=null;
	private static boolean stop = false;
	public static Map<String, Long> obsTimestamps = new HashMap<>();

	public SRBenchImporterYASPER(String uri){
		super(uri);
		max=Config.getInstance().getInputStreamMaxTime();
		interval=Config.getInstance().getInputStreamInterval();
		exclude=Config.getInstance().getInputStreamHoles();
	}
	
	public void addTimestampedData(File f, long timestamp) throws RDFParseException, IOException, RepositoryException {
		long currentTimestamp = timestamp + System.currentTimeMillis();
		org.apache.commons.rdf.api.RDF instance = RDFUtils.getInstance();
		Graph graph = instance.createGraph();
		URI graphURI = BenchmarkVocab.getGraphURI(currentTimestamp);
		System.out.println("GraphURI: " + graphURI);
		//logger.info("Strings: " + graphURI.toString() + ", " + BenchmarkVocab.hasTimestamp.toString());
		IRI xsdDouble = instance.createIRI("http://www.w3.org/2001/XMLSchema#long");
		graph.add(instance.createTriple(instance.createIRI(graphURI.stringValue()), instance.createIRI(BenchmarkVocab.hasTimestamp.toString()), instance.createLiteral(Long.toString(System.currentTimeMillis()), xsdDouble)));
		this.put(graph, System.currentTimeMillis());
		graph = new JenaRDF().asGraph(RDFDataMgr.loadModel(f.getAbsolutePath()));
		logger.info("Number of Triples: " + graph.size());
		Iterator<Triple> iterator = graph.iterate().iterator();
		while (iterator.hasNext()) {
			Triple tr = iterator.next();
			obsTimestamps.put(tr.getSubject().toString().substring(1, tr.getSubject().toString().length() - 1), currentTimestamp);
			//System.out.println("Triple Sub: " + tr.getSubject().toString().substring(1, tr.getSubject().toString().length() - 1));
		}
		this.put(graph, System.currentTimeMillis());
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


	public static void setStop(boolean stop) {
		SRBenchImporterYASPER.stop = stop;
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
}
