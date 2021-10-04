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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import eu.planetdata.srbench.oracle.engineRDFox.NamedStream;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.planetdata.srbench.oracle.configuration.Config;

public class SRBenchImporterRDFox extends StreamImporter{
	private final static Logger logger = LoggerFactory.getLogger(SRBenchImporterRDFox.class);
	private int max=0;
	private long interval=0;
	private Set<String> exclude=null;
	public NamedStream stream;
	
	public SRBenchImporterRDFox(NamedStream namedStream){
		max=Config.getInstance().getInputStreamMaxTime();
		interval=Config.getInstance().getInputStreamInterval();
		exclude=Config.getInstance().getInputStreamHoles();
		stream = namedStream;
	}
	
	public void addData(File f, long timestamp) throws RDFParseException, IOException, RepositoryException{
		URI graph = BenchmarkVocab.getGraphURI(timestamp);
		//repo.getConnection().begin();
		if(!existsGraph(graph)){
			stream.put(new ContextStatementImpl(graph, BenchmarkVocab.hasTimestamp, new NumericLiteralImpl(timestamp), BenchmarkVocab.graphList));
			//repo.getConnection().add(graph, BenchmarkVocab.hasTimestamp, new NumericLiteralImpl(timestamp), BenchmarkVocab.graphList);
		}
		//repo.getConnection().add(f,"",RDFFormat.TURTLE, graph);
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		Collection statements = new ArrayList();
		StatementCollector collector = new StatementCollector(statements);
		rdfParser.setRDFHandler(collector);
		try {
			rdfParser.parse(new FileInputStream(f), f.getName());
		}
		catch (Exception e) {

		}
		for (Statement statement : collector.getStatements()) {
			stream.put(statement);
		}
		//repo.getConnection().commit();
	}
	
	public void addStaticData(File f) throws RDFParseException, IOException, RepositoryException{
		URI graph = BenchmarkVocab.graphStaticData;
		repo.getConnection().begin();
		repo.getConnection().add(f,"",RDFFormat.TURTLE, graph);
		repo.getConnection().commit();
	}
	
	public void importData(int time) throws RDFParseException, IOException, RepositoryException{
		addData(new File("data/data_"+(time < 10 ? "0" + time : time)+".ttl"), time*interval);
	}
	
	public void importAllData() throws RDFParseException, RepositoryException, IOException{
		for (int i=0;i<=max;i++){
			if (!exclude.contains(""+i)) {
				importData(i);
				try {
					Thread.sleep(Config.getInstance().getInputStreamInterval());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
			
	}
	
	public void  getGraphs() throws RepositoryException, MalformedQueryException, QueryEvaluationException{
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
	}
}
