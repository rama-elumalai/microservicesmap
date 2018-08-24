package com.micro.graph;


import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.file.javadsl.Directory;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseRepoToDs {



    static HashMap<String, String> clientNameVsmiddleName = new HashMap<String, String>() {{
        put("callerid-middle-client","callerid-middle");
        put("cdvr-client","cdvr-middle");
        put("amsnotification","amsnotification-middle");
        put("vodmdms-client","vodmdms-middle");
        put("ppv-ams-client","ppv-middle");
        put("lineup-client","lineup-middle");
        put("stb-lookup-middle-client","stb-lookup-middle");
        put("networksettings-client","networksettings-middle");
        put("instantupgrade-client","instantupgrade-middle");
        put("viewinghistory-client","viewinghistory-middle");
        put("dvr-client","dvr-middle");
        put("tts-client","tts-middle");
        put("lrm-client","lrm-middle");
        put("device-client","device-middle");
        put("savedsearch-client","savedsearch-middle");
        put("ppv-client","ppv-middle");
        put("devicemanager-client","devicemanager-middle");
        put("sports-client","sports-middle");
        put("account-client","account-middle");
    }};


    public static void main (String[] args) throws Exception {
        int i = 0;
        List<String> filePaths = new ArrayList<>();
        String gsonFileLocation = "";
        for (String arg : args) {
            if(i+1 != args.length) {
                filePaths.add(arg);
            } else {
                gsonFileLocation = arg;
            }
            i++;
        }
        ParseRepoToDs parseRepoToDs = new ParseRepoToDs();
        parseRepoToDs.run(filePaths, gsonFileLocation);
        parseRepoToDs.stop();

    }


    public Map<String, Map<String, Map<String, List<AppDetail>>>> run(List<String> filePaths, String gsonFileLocation) throws Exception {

        ParseRepoToDs parseRepoToDs = new ParseRepoToDs();

        /*Map<String, List<Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();
        fileByAppName.putAll(parseRepoToDs.readFiles("/Users/relumalai/Downloads/hackathon18/CHE"));
        fileByAppName.putAll(parseRepoToDs.readFiles("/Users/relumalai/Downloads/hackathon18/SAD"));
        for(String key: fileByAppName.keySet()) {
            System.out.println(key + "   :  " + fileByAppName.get(key).size());
        }*/

        ConcurrentHashMap<String, RepoInfo> repoInfoByAppNameMap = new ConcurrentHashMap<>();
        if(CollectionUtils.isEmpty(filePaths)) {
            repoInfoByAppNameMap.putAll(parseRepoToDs.parseRepos("/Users/relumalai/Downloads/hackathon18/CHE"));
            repoInfoByAppNameMap.putAll(parseRepoToDs.parseRepos("/Users/relumalai/Downloads/hackathon18/SAD"));
        } else {
            for(String path: filePaths) {
                repoInfoByAppNameMap.putAll(parseRepoToDs.parseRepos(path));
            }
        }

        Map<String, String> appNameByModuleNameMap = new HashMap<>();
        for(Map.Entry<String, RepoInfo> entry: repoInfoByAppNameMap.entrySet()) {
            if(clientNameVsmiddleName.containsKey(entry.getKey())) {
                entry.getValue().setCorrespondingMiddleName(clientNameVsmiddleName.get(entry.getKey()));
                if (repoInfoByAppNameMap.containsKey(clientNameVsmiddleName.get(entry.getKey()))) {
                    repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).setCorrespondingClientName(entry.getKey());
                    repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getClientModuleClasses().addAll(entry.getValue().getClientModuleClasses());
                    repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getClientModuleServiceClasses().addAll(entry.getValue().getClientModuleServiceClasses());
                    repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getServiceClassesByModuleNameMap().putAll(entry.getValue().getServiceClassesByModuleNameMap());
                    repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getModuleClassToPackage().putAll(entry.getValue().getModuleClassToPackage());
                    entry.getValue().setClientModuleClasses(new HashSet<>());
                    entry.getValue().setClientModuleServiceClasses(new HashSet<>());
                } else {
                    System.out.println("appName not found in repo: " + clientNameVsmiddleName.get(entry.getKey()));
                }
                //repoInfoByAppNameMap.put(entry.getKey(), entry.getValue());
            }
            if(entry.getValue().getClientModuleClasses() != null && entry.getValue().getClientModuleClasses().size() > 0) {
                for(String moduleClass: entry.getValue().getClientModuleClasses()) {
                    appNameByModuleNameMap.put(moduleClass, entry.getKey());
                }
            }
        }
        for(Map.Entry<String, RepoInfo> entry: repoInfoByAppNameMap.entrySet()) {
            System.out.println(entry.getKey() + " has client: " + entry.getValue().isHasClient() + "service class: " + entry.getValue().getClientModuleServiceClasses() + " module class: " + entry.getValue().getClientModuleClasses() + "module map: " + entry.getValue().getServiceClassesByModuleNameMap());
        }




        for(Map.Entry<String, RepoInfo> entry: repoInfoByAppNameMap.entrySet()) {
            //entry.getValue().getAllInfoMap().putAll(repoInfoByAppNameMap);
            if(clientNameVsmiddleName.containsKey(entry.getKey())) {
                System.out.println(entry.getKey() + " corresponding middle: "+ entry.getValue().getCorrespondingMiddleName());
                if(repoInfoByAppNameMap.containsKey(clientNameVsmiddleName.get(entry.getKey()))) {
                    System.out.println(repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getAppName() + " corresponding client: " + repoInfoByAppNameMap.get(clientNameVsmiddleName.get(entry.getKey())).getCorrespondingClientName());
                }

                //repoInfoByAppNameMap.put(entry.getKey(), entry.getValue());
            }
        }





        List<Pair<String, RepoInfo>> dependencies = Source.from(repoInfoByAppNameMap.entrySet())
                .via(calculateDependencies(repoInfoByAppNameMap))
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .get(200, TimeUnit.SECONDS);


        for(Pair<String, RepoInfo> pair: dependencies) {
            repoInfoByAppNameMap.put(pair.first(), pair.second());
            if(!pair.second().getDependencies().keySet().isEmpty()) {
                System.out.println(pair.first() + " : " + pair.second().getDependencies());
            }
        }

        createGraph(repoInfoByAppNameMap, gsonFileLocation);
        Map<String, Map<String, Map<String, List<AppDetail>>>> dependenciesByAppNameMap = new ConcurrentHashMap<>();

        //parse map to populate ds [which will be used by rest api]
        for(Map.Entry<String, RepoInfo> entry: repoInfoByAppNameMap.entrySet()) {
            if(entry.getValue().getDependencies().size() == 0) {
                continue;
            }
            //
            if(!dependenciesByAppNameMap.containsKey(entry.getKey())) {
                dependenciesByAppNameMap.put(entry.getKey(), new HashMap<>());
            }
            if(!dependenciesByAppNameMap.get(entry.getKey()).containsKey("OUT")) {
                dependenciesByAppNameMap.get(entry.getKey()).put("OUT", new HashMap<>());
            }

            for(Map.Entry<String, Map<String, Map<String, Set<DependencyInfo>>>> depEntry: entry.getValue().getDependencies().entrySet()) {
                if(!dependenciesByAppNameMap.containsKey(depEntry.getKey())) {
                    dependenciesByAppNameMap.put(depEntry.getKey(), new HashMap<>());
                }
                if(!dependenciesByAppNameMap.get(depEntry.getKey()).containsKey("IN")) {
                    dependenciesByAppNameMap.get(depEntry.getKey()).put("IN", new HashMap<>());
                }
                if(!dependenciesByAppNameMap.get(depEntry.getKey()).get("IN").containsKey(entry.getKey())) {
                    dependenciesByAppNameMap.get(depEntry.getKey()).get("IN").put(entry.getKey(), new ArrayList<>());
                }

                if(!dependenciesByAppNameMap.get(entry.getKey()).get("OUT").containsKey(depEntry.getKey())) {
                    dependenciesByAppNameMap.get(entry.getKey()).get("OUT").put(depEntry.getKey(), new ArrayList<>());
                }

                depEntry.getValue().entrySet()
                        .forEach(moduleMapEntry -> {
                            moduleMapEntry.getValue().entrySet()
                                    .forEach(serviceClassMapEntry -> {
                                        serviceClassMapEntry.getValue()
                                                .forEach(dependencyInfo -> {
                                                    AppDetail outAppDetail = new AppDetail(depEntry.getKey(), dependencyInfo.getMethodNameCalled(), serviceClassMapEntry.getKey(), dependencyInfo.getFilePath(), moduleMapEntry.getKey());
                                                    AppDetail inAppDetail = new AppDetail(entry.getKey(), dependencyInfo.getMethodNameCalled(), serviceClassMapEntry.getKey(), dependencyInfo.getFilePath(), moduleMapEntry.getKey());
                                                    dependenciesByAppNameMap.get(entry.getKey()).get("OUT").get(depEntry.getKey()).add(outAppDetail);
                                                    dependenciesByAppNameMap.get(depEntry.getKey()).get("IN").get(entry.getKey()).add(inAppDetail);
                                                });
                                    });
                        });
            }

        }

        System.out.println(dependenciesByAppNameMap);

        return dependenciesByAppNameMap;
    }

    static void createGraph(Map<String, RepoInfo> repoInfoByAppNameMap, String graphFileLocation) {
        Graph graph = TinkerGraph.open();
        Map<String, Vertex> vertexByAppName = new HashMap<>();
        for(String appName : repoInfoByAppNameMap.keySet()) {
            if(repoInfoByAppNameMap.get(appName) != null && !repoInfoByAppNameMap.get(appName).getDependencies().keySet().isEmpty()) {
                if(!vertexByAppName.containsKey(appName)) {
                    Vertex vertex = graph.addVertex(T.label, appName, "name", appName);
                    vertexByAppName.put(appName, vertex);
                }
                for(String depName: repoInfoByAppNameMap.get(appName).getDependencies().keySet()) {
                    if(!vertexByAppName.containsKey(depName)) {
                        Vertex depVertex = graph.addVertex(T.label, depName, "name", depName);
                        vertexByAppName.put(depName, depVertex);
                    }
                }
            }
        }

        for(String appName : repoInfoByAppNameMap.keySet()) {
            if(repoInfoByAppNameMap.get(appName) != null && !repoInfoByAppNameMap.get(appName).getDependencies().keySet().isEmpty()) {
                for(String depName: repoInfoByAppNameMap.get(appName).getDependencies().keySet()) {
                    Vertex fromVertex = vertexByAppName.get(appName);
                    Vertex toVertex = vertexByAppName.get(depName);
                    if(fromVertex != null && toVertex != null) {
                        String edgeDescription = "-";
                        edgeDescription = repoInfoByAppNameMap.get(appName).getDependencies().get(depName).entrySet().stream()
                                .flatMap(entry -> entry.getValue().entrySet().stream())
                                .flatMap(entry -> entry.getValue().stream())
                                .map(dependencyInfo -> dependencyInfo.getVariableName() + ":" + dependencyInfo.getMethodNameCalled())
                                .collect(Collectors.toSet())
                                .toString();
                        fromVertex.addEdge(edgeDescription, toVertex);
                    }
                }
            }
        }

        try {
            GraphSONWriter graphSONWriter = GraphSONWriter.build().create();
            OutputStream fo;
            if(StringUtils.isBlank(graphFileLocation)) {
                fo = new FileOutputStream("output-text.txt", false);
            } else {
                fo = new FileOutputStream(graphFileLocation, false);
            }
            graphSONWriter.writeGraph(fo, graph);
        } catch (Exception e) {
            log.error("Exception occurred while creating Graph ", e.getMessage(), e);
        }

    }

    static private Flow<Map.Entry<String, RepoInfo>, Pair<String, RepoInfo>, NotUsed> getModuleDependencies() {

        return Flow.<Map.Entry<String, RepoInfo>>create()
                .map(entry -> {
                    Long count = entry.getValue().getAllInfoMap().entrySet()
                            .stream()
                            .map(fullMapEntry -> {
                                log.info("peek: " + fullMapEntry.getKey());

                                return new Pair(entry.getKey(), entry.getValue());
                            })
                            .count();
                    log.info("count: {}", count);
                    return new Pair<>(entry.getKey(), entry.getValue());
                });
    }

    static private Flow<Map.Entry<String, RepoInfo>, Pair<String, RepoInfo>, NotUsed> calculateDependencies(ConcurrentHashMap<String, RepoInfo> allAppInfo) {
        log.info("calculateDependencies");
        return Flow.<Map.Entry<String, RepoInfo>>create()
                .map(entry -> {
                    Object entry1 = allAppInfo.entrySet()
                            .stream()
                            .filter(fullMapEntry -> entry.getValue().getClientModuleClasses().size() > 0)
                            //.peek(fullMapEntry -> log.info("peek: " + fullMapEntry.getKey() + " : " + fullMapEntry.getValue()))
                            .filter(fullMapEntry -> !fullMapEntry.getKey().equalsIgnoreCase(entry.getKey())) //take other services to get the usage of the current service [entry]
                            //.peek(other -> log.info("Checking the usage of the  app: " + entry.getKey() +  " with modules " + entry.getValue().getClientModuleClasses() + " to app " + other.getKey()))
                            .map(other -> {
                                other.getValue().getFileInfoList().stream()
                                        .peek(fileInfo -> {
                                            Set<String> moduleNameSet = entry.getValue().getClientModuleClasses().stream()
                                                    .filter(moduleName -> entry.getValue().getModuleClassToPackage().containsKey(moduleName) && fileInfo.getContent().contains(entry.getValue().getModuleClassToPackage().get(moduleName)))
                                                    .filter(moduleName -> fileInfo.getContent().contains(moduleName))
                                                    .map(moduleName -> {
                                                        if (!allAppInfo.get(other.getKey()).getDependencies().containsKey(entry.getKey())) {
                                                            allAppInfo.get(other.getKey()).getDependencies().put(entry.getKey(), new HashMap<>());
                                                        }
                                                        if (!allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).containsKey(moduleName)) {
                                                            allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).put(moduleName, new HashMap<>());
                                                        }
                                                        return moduleName;
                                                    })
                                                    .collect(Collectors.toSet());
                                            if(!moduleNameSet.isEmpty()) {
                                               // log.info("appname: {} Found the moduleName: {} in the app: {} filepath: {} ", entry.getKey(), moduleNameSet, other.getKey(), fileInfo.getPath());
                                            }
                                            //return new Pair<FileInfo, Set<String>>(fileInfo, (CollectionUtils.isEmpty(moduleNameSet) ? new HashSet<>() : moduleNameSet));
                                        })
                                        .map(fileInfo -> {
                                            if(allAppInfo.get(other.getKey()).getDependencies() != null && allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()) != null) {
                                                allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).keySet()
                                                        .stream()
                                                        .map(moduleName -> {
                                                            if (entry.getValue().getServiceClassesByModuleNameMap().containsKey(moduleName)) {
                                                                entry.getValue()
                                                                        .getServiceClassesByModuleNameMap().get(moduleName)
                                                                        .stream()
                                                                        .filter(serviceClass -> fileInfo.getContent().contains(serviceClass))
                                                                        //.peek(serviceClass -> log.info("Looking for service class: {} match in the app:{} in the class: {}  ", serviceClass, other.getKey(), fileInfo.getPath()))
                                                                        .map(serviceClass -> {
                                                                            Map<String, String> serviceClassByVariableNameMap = new HashMap<>();
                                                                            Arrays.stream(fileInfo.getContent().split("\n"))
                                                                                    .filter(line -> line.contains(serviceClass + " "))
                                                                                    //.filter(line -> line.matches("^[a-zA-Z0-9]*$"))
                                                                                    //.peek(line -> log.info("service class matched in the line: {}", line))
                                                                                    .map(line -> {
                                                                                        String trimLine = line.trim();
                                                                                        if ((trimLine.contains("private ") || trimLine.contains("public ")) && trimLine.contains(serviceClass + " ")) {
                                                                                            String serviceVariableName = trimLine.replace("final", "")
                                                                                                    .replace(" ", "").replace("private", "")
                                                                                                    .replace("public", "").replace(serviceClass, "")
                                                                                                    .replace(";", "");
                                                                                            if (serviceVariableName.matches("^[a-zA-Z0-9]*$")) {
                                                                                                //log.info("found serviceVariableName: {} for the serviceClass: {}", serviceVariableName, serviceClass);
                                                                                                if (!allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).get(moduleName).containsKey(serviceClass)) {
                                                                                                    allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).get(moduleName).put(serviceClass, new HashSet<>());
                                                                                                }
                                                                                                //allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).get(moduleName).get(serviceClass).add(serviceVariableName);
                                                                                                serviceClassByVariableNameMap.put(serviceVariableName, serviceClass);
                                                                                            }


                                                                                        }
                                                                                        return null;
                                                                                    }).count();
                                                                            return new Pair<Map<String, String>, FileInfo> (serviceClassByVariableNameMap, fileInfo);
                                                                        })
                                                                        .map(pair -> {

                                                                            pair.first().keySet().stream()
                                                                                    .map(serviceClassVariableName -> {
                                                                                        if (fileInfo.getContent().contains(serviceClassVariableName+".")) {
                                                                                            String methodName1 = fileInfo.getContent().substring(fileInfo.getContent().indexOf(serviceClassVariableName+".") + (serviceClassVariableName +".").length());

                                                                                            String methodName2 = methodName1.substring(0, methodName1.indexOf("("));
                                                                                            if(methodName2!= null && methodName2.length() < 50) {
                                                                                                DependencyInfo dependencyInfo = new DependencyInfo();
                                                                                                dependencyInfo.setFilePath(fileInfo.getPath().toString());
                                                                                                dependencyInfo.setMethodNameCalled(methodName2);
                                                                                                dependencyInfo.setVariableName(serviceClassVariableName);

                                                                                                allAppInfo.get(other.getKey()).getDependencies().get(entry.getKey()).get(moduleName).get(pair.first().get(serviceClassVariableName)).add(dependencyInfo);
                                                                                            }
                                                                                            //println ("methodName: "+methodName2)
                                                                                        }
                                                                                        return null;
                                                                                    }).count();
                                                                            return null;
                                                                        })
                                                                        .count();
                                                                return null;
                                                            }
                                                            return null;
                                                        }).count();
                                            }
                                            return null;
                                        }).count();
                                return entry;
                            }).count();
                    return new Pair<>(entry.getKey(),entry.getValue());
                });
    }

    private Flow<RepoInfo, RepoInfo, NotUsed> findModules(boolean onlyWithClientModules) {
        return Flow.<RepoInfo>create()
                .map(repoInfo -> {
                    Set<String> serviceClassSet =  repoInfo.getFileInfoList()
                            .stream()
                            .filter(fileInfo -> {
                                return (repoInfo.isHasClient() && onlyWithClientModules && fileInfo.isFromClientModule()) || (!onlyWithClientModules && !repoInfo.isHasClient() && repoInfo.getAppName().contains("client"));
                            })
                            .filter(fileInfo -> fileInfo.getPath().getFileName().toString().toLowerCase().contains("module")
                                    && !fileInfo.getPath().getFileName().toString().toLowerCase().contains("local")
                                    && !fileInfo.getPath().getFileName().toString().toLowerCase().contains("test")
                            )
                            .map(fileInfo -> {
                                String moduleName = fileInfo.getPath().getFileName().toString().replace(".java","");
                                List<String> packageList = Arrays.stream(fileInfo.getContent().split("\n"))
                                        .filter(line -> line.startsWith("package"))
                                        .collect(Collectors.toList());
                                String packageName = "";
                                log.info("package found: {}", packageList);
                                if(CollectionUtils.isNotEmpty(packageList)) {
                                    packageName = packageList.get(0);
                                }
                                if(!moduleName.toLowerCase().contains("restmodule") && StringUtils.isNotEmpty(packageName)) {
                                    repoInfo.getClientModuleClasses().add(moduleName);
                                    log.info("moduleName: {} , packageName: {}", moduleName, packageName.replace("package","").trim().replace(";",""));
                                    repoInfo.getModuleClassToPackage().put(moduleName, packageName.replace("package","").replace(";","").trim());
                                }
                                return new Pair<String, FileInfo>(moduleName,fileInfo);
                            })
                            .map(pair -> {
                                Set<String> serviceClasses = Arrays.stream(pair.second().getContent().split("\n"))
                                        .map(line -> {
                                            if (line.trim().startsWith("bind(") && !line.trim().startsWith("bind(n")) {
                                                String serviceClassName = line.trim().substring(line.trim().indexOf("(") + 1, line.trim().indexOf("."));
                                                if(!(serviceClassName.toLowerCase().contains("objectmapper") ||
                                                        serviceClassName.toLowerCase().contains("restclient"))) {
                                                    return serviceClassName;
                                                }
                                            }
                                            return null;
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());
                                if(CollectionUtils.isNotEmpty(serviceClasses)) {
                                    repoInfo.getServiceClassesByModuleNameMap().put(pair.first(), serviceClasses);
                                    return serviceClasses;
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .flatMap(serviceClasses -> serviceClasses.stream())
                            .collect(Collectors.toSet());
                    repoInfo.getClientModuleServiceClasses().addAll(serviceClassSet);
                    return repoInfo;
                });
    }

    public void stop() {
        system.terminate();
    }

    private final Flow<Path, Path, NotUsed> validSourcePaths =
            Flow.<Path>create()
                    .filter(this::isValidSourceFile);


    private final Flow<Path, Pair<Path, String>, NotUsed> pathByApp(String basePath) {
        return Flow.<Path>create()
                .map(p -> new Pair<>(p, convertPathToAppName(p, basePath)));
    }

    private final Flow<Path, ByteString, NotUsed> fileBytes =
            Flow.of(Path.class).flatMapConcat(p -> FileIO.fromPath(p, 2000000));

    private final static ActorSystem system = ActorSystem.create();
    static final LoggingAdapter log = Logging.getLogger(system, new ParseRepoToDs());

    private final static Materializer materializer = ActorMaterializer.create(system);

    private String convertPathToAppName(Path p, String basePath) {
        Pattern pattern = Pattern.compile(basePath + "/(.*?)/");
        Matcher matcher = pattern.matcher(p.toString());
        matcher.find();
        String appName = matcher.group(1);
        //System.out.println(p + " :: " + appName);
        return appName;
    }


    public Map<String, List<Pair<Path, String>>> readFiles(String path) throws Exception {
        Long startTime = System.currentTimeMillis();
        Path basePath = new File(path).toPath();
        final Source<Pair<Path, String>, NotUsed> sourcePairByPath = Directory.walk(basePath).via(validSourcePaths).via(pathByApp(path));
        final Source<ByteString, NotUsed> sourceFileByPath = Directory.walk(basePath).via(validSourcePaths).via(fileBytes);
        final Source<Pair<Pair<Path, String>, ByteString>, NotUsed> combinedSource = sourcePairByPath.zipWith(sourceFileByPath, (sourcePathPair, sourceFile) -> new Pair(sourcePathPair, sourceFile));
        final List<Pair<Pair<Path, String>, ByteString>> combinedResult = combinedSource.runWith(Sink.seq(), materializer).toCompletableFuture().get(200, TimeUnit.SECONDS);
        Map<String, List<Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();
        for(Pair<Pair<Path, String>, ByteString> p: combinedResult) {
            if(!fileByAppName.containsKey(p.first().second())) {
                fileByAppName.put(p.first().second(), new ArrayList<>());
            }
            fileByAppName.get(p.first().second()).add(new Pair<>(p.first().first(), p.second().utf8String()));
        }
        //System.out.println(fileByAppName);
        System.out.println("Total time taken to process all files in the dir " + path + " : " + (System.currentTimeMillis()-startTime)/1000 + " seconds!");
        return fileByAppName;
    }

    private final Flow<Map.Entry<String, List<Pair<Path, String>>>, RepoInfo, NotUsed> convertToRepoInfo(String path) {

        return Flow.<Map.Entry<String, List<Pair<Path, String>>>>create()
                .map(entry -> {
                    RepoInfo repoInfo = new RepoInfo();
                    repoInfo.setAppName(entry.getKey());
                    List<Pair<Path, String>> pairs = entry.getValue();
                    for(Pair<Path, String> pair: pairs) {
                        FileInfo fileInfo = new FileInfo();

                        String filePath = pair.first().toString();
                        String clientPathToCompare = path + (!path.endsWith("/")? "/":"")+entry.getKey() + "/client";
                        if(pair.first().toString().startsWith(clientPathToCompare)) {
                            repoInfo.setHasClient(true);
                            fileInfo.setFromClientModule(true);
                        }
                        repoInfo.getFilePaths().add(pair.first());
                        repoInfo.getFileInfos().add(pair.second());
                        fileInfo.setContent(pair.second());
                        fileInfo.setPath(pair.first());
                        repoInfo.getFileInfoList().add(fileInfo);
                    }
                    return repoInfo;
                });
    }

    public ConcurrentHashMap<String, RepoInfo> parseRepos(String path) throws Exception {
        Long startTime = System.currentTimeMillis();
        Path basePath = new File(path).toPath();
        final Source<Pair<Path, String>, NotUsed> sourcePairByPath = Directory.walk(basePath).via(validSourcePaths).via(pathByApp(path));
        final Source<ByteString, NotUsed> sourceFileByPath = Directory.walk(basePath).via(validSourcePaths).via(fileBytes);
        final Source<Pair<Pair<Path, String>, ByteString>, NotUsed> combinedSource = sourcePairByPath.zipWith(sourceFileByPath, (sourcePathPair, sourceFile) -> new Pair(sourcePathPair, sourceFile));
        final List<Pair<Pair<Path, String>, ByteString>> combinedResult = combinedSource.runWith(Sink.seq(), materializer).toCompletableFuture().get(200, TimeUnit.SECONDS);
        Map<String, List<Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();
        for(Pair<Pair<Path, String>, ByteString> p: combinedResult) {
            if(!fileByAppName.containsKey(p.first().second())) {
                fileByAppName.put(p.first().second(), new ArrayList<>());
            }
            fileByAppName.get(p.first().second()).add(new Pair<>(p.first().first(), p.second().utf8String()));
        }
        ConcurrentHashMap<String, RepoInfo> repoInfoByAppNameMap = new ConcurrentHashMap<>();
        final List<RepoInfo> repoInfos = Source.from(fileByAppName.entrySet())
                .via(convertToRepoInfo(path))
                .via(findModules(true))
                .via(findModules(false))
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);

        /*for(Map.Entry<String, List<Pair<Path, String>>> entry: fileByAppName.entrySet()) {
            RepoInfo repoInfo = new RepoInfo();
            repoInfo.setAppName(entry.getKey());
            List<Pair<Path, String>> pairs = entry.getValue();
            for(Pair<Path, String> pair: pairs) {
                String filePath = pair.first().toString();
                String clientPathToCompare = path + (!path.endsWith("/")? "/":"")+entry.getKey() + "/client";
                if(pair.first().toString().startsWith(clientPathToCompare)) {
                    repoInfo.setHasClient(true);
                }
                repoInfo.getFilePaths().add(pair.first());
                repoInfo.getFileInfos().add(pair.second());
            }
            repoInfoByAppNameMap.put(entry.getKey(), repoInfo);
        }*/
        for(RepoInfo repoInfo: repoInfos) {
            repoInfoByAppNameMap.put(repoInfo.getAppName(), repoInfo);
        }
        //System.out.println(fileByAppName);
        System.out.println("Total time taken to process all files in the dir " + path + " : " + (System.currentTimeMillis()-startTime)/1000 + " seconds!");
        return repoInfoByAppNameMap;
    }

    private boolean isValidSourceFile(Path p) {
        return p.toString().endsWith(".java") && p.toString().contains("main") && !p.toString().contains("deprecated") && !p.toString().contains("depreciated");
    }

}
