package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter
import org.apache.commons.lang3.tuple.Triple
import org.javatuples.Pair

import static groovy.io.FileType.FILES

class ParseRepoActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this)


    @Override
    AbstractActor.Receive createReceive() {
        receiveBuilder()
                .match(FileRefWithAppName.class, { FileRefWithAppName s ->
            new File(s.pathName).eachFileRecurse(FILES) { File it ->
                //log.info("inside file: {}", it.name)
                getContext().actorOf(ParseFileToFindModules.create()).tell(new Pair(it, s.appName), ActorRef.noSender())
            }
        })
                .match(DependencyDsToParseRepo.class, { DependencyDsToParseRepo s ->
            log.info("Inside DependencyDsToParseRepo: appName: {}, file: {}", s.fileRefWithAppName.appName, s.fileRefWithAppName.file)
            Set<String> dependentAppName = new HashSet<>()
            FileRefWithAppName fileRefWithAppName = s.getFileRefWithAppName()
            Map<String, String> moduleNameByAppNameMap = s.getModuleNameByAppNameMap()
            Map<String, String> appNameByServicesMap = s.getAppNameByServicesMap()
            Set<String> serviceClassSet = s.getServiceClassSet()
            Map<String, String> appNameByserviceClassNameMap = new HashMap<>()
            Map<String, String> appNameBymethodNameMap = new HashMap<>()
            Map<String, Set<String>> listOfMethodNamesByAppName = new HashMap<>()
            Set<String> usedServiceSet = new HashSet<>()


            String appName = fileRefWithAppName.getAppName()
            fileRefWithAppName.getFile().eachFileRecurse(FILES) { file ->
                String fileName = file.getPath()

               // log.info('file path : {}', fileName)

                    if (fileName.contains("src") && !fileName.contains("/test/") && fileName.contains(".java")) {
                        String fileText = file.text
                        log.info('file path : {}', file.getPath())
                       // log.info("file text: {}", fileText)
                        moduleNameByAppNameMap.each { key, value ->

                        if (fileText.contains(key)) {
                            if (!dependentAppName.contains(value + "[" + key + "]") && !appName.equalsIgnoreCase(value)) {
                                log.info("Found dependency: " + key + "," + value + " :: found in " + file.getPath())
                                //dependentAppName.add(value+"["+key+"]")
                                dependentAppName.add(value)
                            }
                        }
                        Set<String> serviceFieldName = new HashSet<>()
                        //String fileText = ""
                        fileText.eachLine { line ->
                            //fileText += line
                            appNameByServicesMap.keySet().each { service ->
                                if (serviceClassSet == null || !serviceClassSet.contains(service)) {
                                    String trimLine = line.trim()
                                    if ((trimLine.contains("private ") || trimLine.contains("public ")) && trimLine.contains(" $service ")) {
                                        String serviceVariableName = trimLine.replace("final", "")
                                                .replace(" ", "").replace("private", "")
                                                .replace("public", "").replace(service, "")
                                                .replace(";", "")
                                        serviceFieldName.add(serviceVariableName)
                                        appNameByserviceClassNameMap.put(serviceVariableName, appNameByServicesMap.get(service))
                                        //println("found service: $service used in appName: $appName")
                                        usedServiceSet.add(service)
                                    }
                                }
                            }
                        }
                        /*if(!serviceFieldName.isEmpty()) {
                            log.info("AppName: {} , fileName: {} Calling GetListOfMethodNamesByAppNameActor to get list of services by app : {} ", appName, file.getName(), serviceFieldName)
                            getContext().actorOf(GetListOfMethodNamesByAppNameActor.create()).tell(new GetListOfMethodNamesByAppName(appName: new String(appName), text: new String(fileText), serviceFieldName: new HashSet<String>(serviceFieldName), appNameByserviceClassNameMap: new HashMap<String, String>(appNameByserviceClassNameMap)), ActorRef.noSender())
                        }*/

                        if (!serviceFieldName.isEmpty()) {

                            //file.eachLine { line ->
                            serviceFieldName.each { service ->
                                if (fileText.contains("$service.")) {
                                    def methodName1 = fileText.substring(fileText.indexOf("$service.") + "$service.".length())

                                    def methodName2 = methodName1.substring(0, methodName1.indexOf("("))
                                    if(methodName2!= null && methodName2.length() < 50) {
                                        appNameBymethodNameMap.put(methodName2, appNameByserviceClassNameMap.get(service))
                                        if (!listOfMethodNamesByAppName.containsKey(appNameByserviceClassNameMap.get(service))) {
                                            listOfMethodNamesByAppName.put(appNameByserviceClassNameMap.get(service), new HashSet<String>())
                                        }
                                        listOfMethodNamesByAppName.get(appNameByserviceClassNameMap.get(service)).add(methodName2)
                                    }
                                    //println ("methodName: "+methodName2)
                                }
                            }
                            //}
                        }

                    }
                }
            }

            if (!dependentAppName.isEmpty() || dependentAppName.size() >= 1) {

                log.info('found dependencies for the appName: ' + s.getFileRefWithAppName().getAppName() + " - " + dependentAppName)
                //call mainactor to update a datastructure which hold: listOfDependencyByAppName
                getSender().tell(new ListOfDependencyByAppName(appName: s.getFileRefWithAppName().getAppName(), dependencies: dependentAppName ), ActorRef.noSender())
            } else {
                log.info(s.getFileRefWithAppName().getAppName() + " - no dependencies")
            }

            //getSender().tell(new AppNameBymethodDependenciesMap(appName: s.getFileRefWithAppName().getAppName(), methodDependencies:  appNameBymethodNameMap), ActorRef.noSender())
            //getSender().tell(new AppNameAndItsDependencies(appName: s.getFileRefWithAppName().getAppName(), dependencies:  listOfMethodNamesByAppName), ActorRef.noSender())
            //println "appNameBymethodNameMap: " + appNameBymethodNameMap

            //println "listOfMethodNamesByAppName: " + listOfMethodNamesByAppName

        })
                .build();
    }

    public static Props create() {
        return Props.create(ParseRepoActor.class)
    }
}
