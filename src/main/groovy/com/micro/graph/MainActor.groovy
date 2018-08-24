package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter
import org.javatuples.Pair
import scala.concurrent.ExecutionContext

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import static groovy.io.FileType.FILES

class MainActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this)

    static ConcurrentHashMap<String, String> clientNameVsmiddleNameMap = ['callerid-middle-client':'callerid-middle',
                                                                   'cdvr-client'           :'cdvr-middle',
                                                                   'amsnotification'       :'amsnotification-middle',
                                                                   'vodmdms-client'        :'vodmdms-middle',
                                                                   'ppv-ams-client'        :'ppv-middle',
                                                                   'lineup-client'         :'lineup-middle',
                                                                   'stb-lookup-middle-client':'stb-lookup-middle',
                                                                   // 'networksettings-client':'networksettings-middle',
                                                                   'instantupgrade-client':'instantupgrade-middle',
                                                                   'viewinghistory-client':'viewinghistory-middle',
                                                                   'dvr-client':'dvr-middle',
                                                                   'tts-client':'tts-middle',
                                                                   'device-client':'device-middle',
                                                                   'savedsearch-client':'savedsearch-middle',
                                                                   'ppv-client':'ppv-middle',
                                                                   'devicemanager-client':'devicemanager-middle',
                                                                   'sports-client':'sports-middle',
                                                                   'lrm-client':'lrm-middle'
    ]

    static ConcurrentHashMap<String, Set<String>> setOfServicesByAppNameMap = new ConcurrentHashMap<>()
    static ConcurrentHashMap<String, String> appNameByServicesMap = new ConcurrentHashMap<>()
    static ConcurrentHashMap<String, String> appNameByModuleNameMap = new ConcurrentHashMap<>()
    static ConcurrentHashMap<String, Set<String>> listOfDependencyByAppName = new ConcurrentHashMap<>()
    static ConcurrentHashMap<String,Map<String, String>> appNameBymethodDependenciesMap = new ConcurrentHashMap<>()
    static ConcurrentHashMap<String, Map<String, Set<String>>> appNameAndItsDependencies = new HashMap<>()

    static ConcurrentHashMap<String, List<akka.japi.Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, RepoInfo> repoInfoByAppName = new ConcurrentHashMap<>();




    AbstractActor.Receive createReceive() {
        receiveBuilder()
        .match(FileRefWithBoolean.class, {FileRefWithBoolean s ->
            ActorRef appNameActor = getContext().actorOf(AppNameActor.create(clientNameVsmiddleNameMap))

            log.info("file path: {} has clientModule: {}, appNameActor: {}", s.file.getPath(), s.hasClient, appNameActor.toString())
            appNameActor.tell(s, self())

        })
        .match(String.class, {String s ->
            ActorRef clientModuleCheckActor = getContext().actorOf(ClientModuleCheckActor.create())

            new File(s).eachDir() {
                    //get client's module from middle's multi-module(client) project
                eachDir ->
                    if (eachDir.getName().endsWith("middle") || eachDir.getName().equalsIgnoreCase("amsnotification") || ((eachDir.getName().endsWith("client")) && !eachDir.getName().equalsIgnoreCase("charter-client"))) {
                        FileRef fileRef = new FileRef(file: eachDir)
                        clientModuleCheckActor.tell(fileRef, self())
                    }
            }
        })
        .match(FileRefWithAppName.class, {FileRefWithAppName s ->
            log.info("client module ref path : {} , appname: {}, file: {}, hasClient: {}", s.pathName, s.appName, s.file, s.hasClient)
            getContext().actorOf(ParseRepoActor.create()).tell(s, ActorRef.noSender())
        })
        .match(ServiceClassByAppName.class, {ServiceClassByAppName s ->
            appNameByServicesMap.put(s.serviceClass, s.appName)
        })
        .match(AppNameByServiceClassSet.class, {AppNameByServiceClassSet s ->
            if(!setOfServicesByAppNameMap.containsKey(s.appName)) {
                setOfServicesByAppNameMap.put(s.appName, new HashSet<String>())
            }
            setOfServicesByAppNameMap.get(s.appName).addAll(s.serviceClassSet)
        })
        .match(ModuleNameByAppName.class, {ModuleNameByAppName s ->
            appNameByModuleNameMap.put(s.moduleName, s.appName)
        })
        .match(Pair.class, {Pair<String, String> s ->
            log.info("setOfServicesByAppNameMap: {}", setOfServicesByAppNameMap)
            new File(s.getValue0()).eachDir() { File eachDir ->
                    String appName = eachDir.getName()
                    if (eachDir.getName().endsWith("cron") || eachDir.getName().endsWith("middle") || eachDir.getName().endsWith("edge") || eachDir.getName().equalsIgnoreCase("amsnotification")) {
                        log.info("Analyzing app: " + appName + " to get the dependencies")
                        Set<String> serviceClassSet = setOfServicesByAppNameMap.get(appName)
                        log.info( appName + " : " + serviceClassSet)
                        //call a new actor to parse thru each files with a Triple
                        getContext().actorOf(ParseRepoActor.create()).tell(new DependencyDsToParseRepo(fileRefWithAppName: new FileRefWithAppName(appName: appName, file: eachDir),
                                moduleNameByAppNameMap: appNameByModuleNameMap,
                                appNameByServicesMap: appNameByServicesMap,
                                serviceClassSet: serviceClassSet
                        ), self())
                    }
            }
        })
        .match(ListOfDependencyByAppName.class, { ListOfDependencyByAppName s ->
            if(listOfDependencyByAppName.get(s.appName) == null || listOfDependencyByAppName.get(s.appName).size() == 0) {
                listOfDependencyByAppName.put(s.appName, new HashSet<String>())
            }
            listOfDependencyByAppName.get(s.appName).addAll(s.dependencies)

        })
        .match(AppNameBymethodDependenciesMap.class, { AppNameBymethodDependenciesMap s ->
            if(appNameBymethodDependenciesMap.get(s.appName) == null || appNameBymethodDependenciesMap.get(s.appName).size() == 0) {
                appNameBymethodDependenciesMap.put(s.appName, new HashMap<String, String>())
            }
            appNameBymethodDependenciesMap.get(s.appName).putAll(s.methodDependencies)
        })
        .match(AppNameAndItsDependencies.class, {AppNameAndItsDependencies s ->
            if(appNameAndItsDependencies.get(s.appName) == null || appNameAndItsDependencies.get(s.appName).size() == 0) {
                appNameAndItsDependencies.put(s.appName, new HashMap<String, Set<String>>())
            }
            appNameAndItsDependencies.get(s.appName).putAll(s.dependencies)
            log.info("appNameAndItsDependencies: {}",appNameAndItsDependencies)
        })
        .matchAny({Object o ->
            log.info("Undefined object passed: {}",o.getClass().getName())
        })
        .build()
    }

    public static Props create() {
        return Props.create(MainActor.class);
    }

    public static void parseRepoToDs() {
        ParseRepoToDs parseRepoToDs = new ParseRepoToDs();
        repoInfoByAppName.putAll(parseRepoToDs.parseRepos("/Users/relumalai/Downloads/hackathon18/CHE"));
        repoInfoByAppName.putAll(parseRepoToDs.parseRepos("/Users/relumalai/Downloads/hackathon18/SAD"));
    }

    public static void main (String[] args) {



        Long startTime = System.currentTimeMillis()
        final ActorSystem system = ActorSystem.create("helloakka");
        final ExecutionContext ex = system.dispatchers().lookup("my-dispatcher");

        ActorRef mainActor = system.actorOf(MainActor.create(), "main-actor")

        parseRepoToDs()

        mainActor.tell('/Users/relumalai/Downloads/hackathon18/CHE', ActorRef.noSender())
        mainActor.tell('/Users/relumalai/Downloads/hackathon18/SAD', ActorRef.noSender())

        Thread.sleep(000)
        appNameByServicesMap.remove("ObjectMapper")
        appNameByServicesMap.remove("RestClient")
        println(appNameByModuleNameMap.toString())
        println(setOfServicesByAppNameMap.toString())
        println(appNameByServicesMap.toString())


        mainActor.tell(new Pair('/Users/relumalai/Downloads/hackathon18/CHE',null), ActorRef.noSender())
        mainActor.tell(new Pair('/Users/relumalai/Downloads/hackathon18/SAD', null), ActorRef.noSender())
        println ((System.currentTimeMillis() - startTime)/1000)



    }

}
