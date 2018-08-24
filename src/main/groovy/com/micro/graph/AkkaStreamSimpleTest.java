package com.micro.graph;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import scala.Int;

import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;

public class AkkaStreamSimpleTest {

    final static Sink<String, CompletionStage<IOResult>> fileSink(String filename) {
        return Flow.of(String.class)
                .map(s -> ByteString.fromString(s + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)), Keep.right());
    }

     public static void main(String[] argv) {
         final ActorSystem system = ActorSystem.create("QuickStart");
         final Materializer materializer = ActorMaterializer.create(system);
            // Code here
         final Source<Integer, NotUsed> source = Source.range(1, 100);
         final CompletionStage<Done> done = source.runForeach(i -> System.out.println(i), materializer);

         //done.thenRun(() -> system.terminate());

         final Source<Integer, NotUsed> factorialSource = source.scan(1, (acc, next) -> acc * next);

         final CompletionStage<IOResult> resultCompletionStage = factorialSource.map(num -> ByteString.fromString(num.toString() + "\n"))
                 .runWith(FileIO.toPath(Paths.get("factorials.txt")), materializer);
         //resultCompletionStage.thenRun(() -> system.terminate());

         factorialSource.map(String::valueOf).runWith(fileSink("factorial2.txt"), materializer).thenRun(() -> system.terminate());


        }
}
