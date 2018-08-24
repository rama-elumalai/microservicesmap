package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter
import org.apache.commons.collections.CollectionUtils

import java.util.concurrent.ConcurrentHashMap

class GetListOfMethodNamesByAppNameActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props create() {
        return Props.create(GetListOfMethodNamesByAppNameActor.class);
    }

    @Override
    AbstractActor.Receive createReceive() {
        receiveBuilder()
                .match(GetListOfMethodNamesByAppName.class, { GetListOfMethodNamesByAppName s ->
            Map<String, String> appNameBymethodNameMap = new HashMap<>()
            Map<String, String> appNameByserviceClassNameMap = s.appNameByserviceClassNameMap
            Map<String, Set<String>> listOfMethodNamesByAppName = new HashMap<>()


            s.serviceFieldName.each { service ->
                String fileText = s.text
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
            if(appNameByserviceClassNameMap != null && appNameByserviceClassNameMap.keySet().size() >=1) {
                getContext().actorSelection("/user/main-actor").tell(new AppNameBymethodDependenciesMap(appName: s.appName, methodDependencies: appNameBymethodNameMap), ActorRef.noSender())
                log.info("appName: $s.appName  appNameBymethodNameMap: " + appNameBymethodNameMap)
            }
            if(listOfMethodNamesByAppName != null && listOfMethodNamesByAppName.keySet().size() >= 1) {
                getContext().actorSelection("/user/main-actor").tell(new AppNameAndItsDependencies(appName: s.appName, dependencies: listOfMethodNamesByAppName), ActorRef.noSender())
                log.info("appName: $s.appName listOfMethodNamesByAppName: " + listOfMethodNamesByAppName)

            }

        }).build()
    }
}
