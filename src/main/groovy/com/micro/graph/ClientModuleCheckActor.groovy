package com.micro.graph

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter

class ClientModuleCheckActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);



    AbstractActor.Receive createReceive() {
        receiveBuilder()
            .match(FileRef.class, { FileRef s ->
                File eachDir = s.file
                Boolean  clientFound = false
                if(eachDir.getName().endsWith("middle") || eachDir.getName().equalsIgnoreCase("amsnotification")) {
                    new File(eachDir.getPath()).eachDir() {
                        if (it.getName().toLowerCase().equalsIgnoreCase("client")) {
                            clientFound = true
                        }
                    }
                }
            FileRefWithBoolean fileRefWithBoolean = new FileRefWithBoolean()
            fileRefWithBoolean.hasClient = clientFound
            fileRefWithBoolean.file = eachDir
            getSender().tell(fileRefWithBoolean, getSelf())
            }).build()
    }

    public static Props create() {
        return Props.create(ClientModuleCheckActor.class);
    }
}
