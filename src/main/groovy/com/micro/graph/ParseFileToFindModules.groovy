package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter
import org.javatuples.Pair

class ParseFileToFindModules extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this)


    @Override
    AbstractActor.Receive createReceive() {
        receiveBuilder()
          .match(Pair, { Pair pair ->
            def it = (File)pair.getValue0()
            def appName = (String)pair.getValue1()
            HashSet<String> serviceClassSet = new HashSet<>()
            if (it.name.toLowerCase().contains('module') && !it.name.toLowerCase().contains("local") && !it.name.toLowerCase().contains("test")) {
                String moduleName = it.getName().replace(".java", "")
                it.eachLine { line ->
                    String trimLine = line.trim();
                    if (trimLine.startsWith("bind")) {
                        String serviceClass = trimLine.substring(trimLine.indexOf("(") + 1, trimLine.indexOf("."))
                        getContext().actorSelection("/user/main-actor").tell(new ServiceClassByAppName(serviceClass: serviceClass, appName: appName), ActorRef.noSender())
                        serviceClassSet.add(serviceClass)
                    }
                }
                getContext().actorSelection("/user/main-actor").tell(new AppNameByServiceClassSet(serviceClassSet: serviceClassSet, appName: appName), ActorRef.noSender())
                getContext().actorSelection("/user/main-actor").tell(new ModuleNameByAppName(moduleName: moduleName, appName: appName), ActorRef.noSender())
            }

        })
                .build()
    }

    public static Props create() {
        return Props.create(ParseFileToFindModules.class)
    }
}
