package com.micro.graph

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

class TestSampleGraph {

    public static void main(String[] args) {
        Long startTime = System.currentTimeMillis()
        args=['/Users/relumalai/Downloads/hackathon18/CHE','/Users/relumalai/Downloads/hackathon18/SAD']
        Graph graph = TinkerGraph.open();
        Graph graphDetail = TinkerGraph.open()

        List<String> paths = new ArrayList<>()
        for(String arg: args) {
            paths.add(arg)
        }
        RepoParser.processFiles(paths)
        println "appNameAndItsDependencies: " + RepoParser.appNameAndItsDependencies

        println RepoParser.appNameBymethodDependenciesMap


        println RepoParser.listOfDependencyByAppName
        println((System.currentTimeMillis() - startTime)/1000)
        Map<String, Set<String>> listOfDepByApp = RepoParser.listOfDependencyByAppName
        List<String> spectrumServices = RepoParser.spectrumServices

        Set<String> tobeDeletedApp = new HashSet<>()
        for(String appName : listOfDepByApp.keySet()) {
            if(listOfDepByApp.get(appName) == null || listOfDepByApp.get(appName).isEmpty()) {
                tobeDeletedApp.add(appName)
            }
        }
        for(String appName: tobeDeletedApp) {
            println ("no dependencies for the app: "+ appName + " hence removing it from the map")
            listOfDepByApp.remove(appName)
        }

        Map<String, Vertex> vertexByAppName = new HashMap<>()
        for(String appName : listOfDepByApp.keySet()) {
            if(listOfDepByApp.get(appName) != null && listOfDepByApp.get(appName).size() >= 1) {
                if(!vertexByAppName.containsKey(appName)) {
                    Vertex vertex = graph.addVertex(T.label, appName, "name", appName)
                    vertexByAppName.put(appName, vertex)
                }
                for(String depName: listOfDepByApp.get(appName)) {
                    if(!vertexByAppName.containsKey(depName)) {
                        Vertex depVertex = graph.addVertex(T.label, depName, "name", depName)
                        vertexByAppName.put(depName, depVertex)
                    }
                }
            }
        }

        for(String appName : listOfDepByApp.keySet()) {
            if(listOfDepByApp.get(appName) != null && listOfDepByApp.get(appName).size() >= 1 ) {
                for(String depName: listOfDepByApp.get(appName)) {
                    Vertex fromVertex = vertexByAppName.get(appName)
                    Vertex toVertex = vertexByAppName.get(depName)
                    if(fromVertex != null && toVertex != null) {
                        String edgeDescription = "-"
                        if(RepoParser.appNameAndItsDependencies.get(appName) != null && RepoParser.appNameAndItsDependencies.get(appName).get(depName) != null
                            && RepoParser.appNameAndItsDependencies.get(appName).get(depName).size() > 0 ) {
                            edgeDescription = RepoParser.appNameAndItsDependencies.get(appName).get(depName).join(",")
                        }
                        fromVertex.addEdge(edgeDescription, toVertex)
                    } else {
                        println "from Vertex: ${appName} or toVertex: ${depName} is null "
                    }
                }
            }
        }




        //graph.io(IoCore.graphson()).readGraph("output-text.txt");
/*
// add a software vertex with a name property
        Vertex gremlin = graph.addVertex(T.label, "software",
                "name", "gremlin"); //1
// only one vertex should exist
        assert (IteratorUtils.count(graph.vertices()) == 1)
// no edges should exist as none have been created
        assert (IteratorUtils.count(graph.edges()) == 0)
        // add a new property
        gremlin.property("created", 2009) //2
// add a new software vertex to the graph
        Vertex blueprints = graph.addVertex(T.label, "software",
                "name", "blueprints");
        // connect gremlin to blueprints via a dependsOn-edge
        gremlin.addEdge("dependsOn", blueprints);

        // now there are two vertices and one edge
        assert (IteratorUtils.count(graph.vertices()) == 2)
        assert (IteratorUtils.count(graph.edges()) == 1)

        gremlin.addEdge("encapsulates", blueprints)
*/

        /*def g = graph.traversal()
        assert(g.V().has('name', 'gremlin').out('encapsulates').size()==1)
        for(Vertex t: g.V().has('name', 'gremlin').out('encapsulates')) {
            println t.values("name")[0]
        }*/

        GraphSONWriter graphSONWriter = GraphSONWriter.build().create()
        OutputStream fo = new FileOutputStream("output-text.txt", false)
        graphSONWriter.writeGraph(fo, graph)



        //populate graph object with details edges

    }
}