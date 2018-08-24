package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter

import java.util.concurrent.ConcurrentHashMap

class AppNameActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this)

    private final ConcurrentHashMap<String,String> clientNameVsmiddleNameMap

    public AppNameActor(ConcurrentHashMap<String,String> clientNameVsmiddleNameMap) {
        this.clientNameVsmiddleNameMap = clientNameVsmiddleNameMap;
    }

    @Override
    AbstractActor.Receive createReceive() {
        receiveBuilder()
                .match(FileRefWithBoolean.class, { FileRefWithBoolean s ->
            boolean processRepo = false;
            String filePath = s.file.getPath()
            String appName = s.file.getName()
            if (s.hasClient) {
                filePath = s.file.getPath() + "/client"
                appName = s.file.getName()
                processRepo = true
            }

            if ((s.file.getName().endsWith("client")) && !s.file.getName().equalsIgnoreCase("charter-client")
                  &&!s.file.getName().contains("-js-")) {
                clientNameVsmiddleNameMap.each { key, value ->
                    if (key.equalsIgnoreCase(s.file.getName())) {
                        appName = value
                    }
                }
                processRepo = true
            }
            if(processRepo) {
                FileRefWithAppName fileRefWithAppName = new FileRefWithAppName(hasClient: s.hasClient, file: s.file, appName: appName, pathName: filePath)
                getSender().tell(fileRefWithAppName, ActorRef.noSender())
            }
        })
        .build()
    }

    public static Props create(ConcurrentHashMap<String,String> clientNameVsmiddleNameMap) {
        return Props.create(AppNameActor.class, clientNameVsmiddleNameMap);
    }
}
