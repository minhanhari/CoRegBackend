package com.minhanh.coregbackend;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;

import static org.neo4j.driver.Values.parameters;

@RestController
public class ApplicationController implements AutoCloseable{
	private final Driver driver;

	@GetMapping(value = "/graph", produces = "application/json")
	public Map<String, List<? extends Map<String, ?>>> getCoregulation(@RequestParam Map<String,String> allParams) {
		String[] genes_list = allParams.get("genes").split("\\R");
		byte min = Byte.parseByte(allParams.get("coregulation"));

		return coregulatingGraph(genes_list, min);
	}

	public ApplicationController() {
		String uri = "bolt://localhost:7687";
		String username = "neo4j";
		String password = "coregulation";

		driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password)); //1
		driver.verifyConnectivity(); //2
	}
	@Override
	public void close() throws RuntimeException {
		driver.close();
	}

	private Map<String, List<? extends Map<String, ?>>> coregulatingGraph(String[] genes_list, byte min) {
		try (var session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
			return session.executeRead(tx -> {
				LocalTime start = LocalTime.now();
				var query = new Query(
						"MATCH (t1:TransFactor)-[:REGULATES]->(a:Gene)<-[:REGULATES]-(t2:TransFactor) \n" +
								"WHERE a.symbol IN $list AND t1 <> t2 \n" +
								"WITH t1, t2, count(DISTINCT a) AS n \n" +
								"WHERE n >= $min \n" +
								"RETURN t1.symbol AS u, t2.symbol AS v, n", parameters("list", genes_list, "min", min));
				var result = tx.run(query);

				LocalTime stop = LocalTime.now();
				System.out.println("Time to get data from neo4j: " + start.until(stop, ChronoUnit.MILLIS));

				Set<Map<String,String>> nodes_list = new HashSet<>();
				Set<Map<String,Object>> links_list = new HashSet<>();
				while (result.hasNext()) {
					Record record = result.next();
					String u = record.get("u").asString();
					String v = record.get("v").asString();
					Integer n = record.get("n").asInt();
					nodes_list.add(Map.of("id", u));
					nodes_list.add(Map.of("id", v));
					Map<String,Object> map_link_1 = Map.of("source", u, "target", v, "value", n);
					Map<String,Object> map_link_2 = Map.of("source", v, "target", u, "value", n);
					if (!links_list.contains(map_link_1) & !links_list.contains(map_link_2)) links_list.add(map_link_1);
				}
				System.out.println("Time to process data: " + stop.until(LocalTime.now(), ChronoUnit.MILLIS));

				if (nodes_list.isEmpty() || links_list.isEmpty())
					return new HashMap<>();
				else
					return Map.of("nodes", new ArrayList<>(nodes_list), "links", new ArrayList<>(links_list));
			});
		} catch (Exception e) {
			System.out.println("Cannot make co-regulating graph");
			System.out.println(e.getMessage());
			return new HashMap<>();
		}
	}
}
