package com.micro.graph;


import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.file.javadsl.Directory;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AkkaStreamTest {

    public static void main (String[] args) throws Exception {

        AkkaStreamTest akkaStreamTest = new AkkaStreamTest();
        akkaStreamTest.readFiles("/Users/relumalai/Downloads/hackathon18/CHE");
        akkaStreamTest.readFiles("/Users/relumalai/Downloads/hackathon18/SAD");

    }

    private final Path dir = new File("/Users/relumalai/Downloads/hackathon18/CHE").toPath();


    private final Flow<Path, Path, NotUsed> validSourcePaths =
            Flow.<Path>create()
                    .filter(this::isValidSourceFile);



    private final Flow<Path, Pair<Path, String>, NotUsed> pathByApp(String basePath) {
        return Flow.<Path>create()
                .map(p -> new Pair<>(p, convertPathToAppName(p, basePath)));
    }

    private final Flow<Path, ByteString, NotUsed> fileBytes =
            Flow.of(Path.class).flatMapConcat(p -> FileIO.fromPath(p, 2000000));


    private final Flow<Path, Pair<String, List<Path>>, NotUsed> mapByApp =
            Flow.<Path>create()
            .map(p -> new Pair<>(convertPathToAppName(p,""), p.toString()))
            .groupBy(Integer.MAX_VALUE, Pair::first)
            .reduce((left,right) -> new Pair<>(left.first(), left.second() + "::"+right.second()))
            .mergeSubstreams()
            .map(p -> new Pair<>(p.first(), convertStringToPath(p.second())))
            ;

    private List<Path> convertStringToPath(String paths) {
        return Arrays.asList(paths.split("::")).stream()
                .map(s -> new File(s).toPath())
                .collect(Collectors.toList());
    }



    private final ActorSystem system = ActorSystem.create();
    private final Materializer materializer = ActorMaterializer.create(system);

    private String convertPathToAppName(Path p, String basePath) {
        Pattern pattern = Pattern.compile(basePath + "/(.*?)/");
        Matcher matcher = pattern.matcher(p.toString());
        /*if(matcher.find()) {
            String appName = matcher.group(1);
            System.out.println(p + " :: " + appName);
            return appName;
        } else {
            System.out.println(p + " :: not appNamne found");

            return null;
        }*/
        matcher.find();
        String appName = matcher.group(1);
        System.out.println(p + " :: " + appName);
        return appName;
    }


    public Map<String, List<Pair<Path, String>>> readFiles(String path) throws Exception {
        Long startTime = System.currentTimeMillis();
        Path basePath = new File(path).toPath();
        final Source<Pair<Path, String>, NotUsed> sourcePairByPath = Directory.walk(basePath).via(validSourcePaths).via(pathByApp(path));
        final Source<ByteString, NotUsed> sourceFileByPath = Directory.walk(basePath).via(validSourcePaths).via(fileBytes);
        final Source<Pair<Pair<Path, String>, ByteString>, NotUsed> combinedSource = sourcePairByPath.zipWith(sourceFileByPath, (sourcePathPair, sourceFile) -> new Pair(sourcePathPair, sourceFile));
        final List<Pair<Pair<Path, String>, ByteString>> combinedResult = combinedSource.runWith(Sink.seq(), materializer).toCompletableFuture().get(10, TimeUnit.SECONDS);
        Map<String, List<Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();
        for(Pair<Pair<Path, String>, ByteString> p: combinedResult) {
            if(!fileByAppName.containsKey(p.first().second())) {
                fileByAppName.put(p.first().second(), new ArrayList<>());
            }
            fileByAppName.get(p.first().second()).add(new Pair<>(p.first().first(), p.second().utf8String()));
        }
        System.out.println(fileByAppName);
        System.out.println("Total time taken to process all files in the dir " + path + " : " + (System.currentTimeMillis()-startTime)/1000 + " seconds!");
        return fileByAppName;
    }

    public void walkthruDirectory() throws Exception {

        Long startTime = System.currentTimeMillis();

        //final Path dir = fs.getPath("/Users","relumalai","Downloads","hackathon18","CHE");


        final Source<Path, NotUsed> sourceByPath = Directory.walk(dir).via(validSourcePaths);
        //final List<Path> sourcePathResult = sourceByPath.runWith(Sink.seq(), materializer).toCompletableFuture().get(1000, TimeUnit.SECONDS);

        //final Source<Pair<Path, String>, NotUsed> sourcePairByPath = Source.from(sourcePathResult).via(pathByApp);
        //final Source<ByteString, NotUsed> sourceFileByPath = Source.from(sourcePathResult).via(fileBytes);

        final Source<Pair<Path, String>, NotUsed> sourcePairByPath = Directory.walk(dir).via(validSourcePaths).via(pathByApp(""));
        final Source<ByteString, NotUsed> sourceFileByPath = Directory.walk(dir).via(validSourcePaths).via(fileBytes);

        final Source<Pair<Pair<Path, String>, ByteString>, NotUsed> combinedSource = sourcePairByPath.zipWith(sourceFileByPath, (sourcePathPair, sourceFile) -> new Pair(sourcePathPair, sourceFile));

        //final List<Pair<Path, String>> resultSourcePairByPath = sourcePairByPath.runWith(Sink.seq(), materializer).toCompletableFuture().get(1000, TimeUnit.SECONDS);
        //final List<ByteString> resultSourceFileByPath = sourceFileByPath.runWith(Sink.seq(), materializer).toCompletableFuture().get(1000, TimeUnit.SECONDS);

        final List<Pair<Pair<Path, String>, ByteString>> combinedResult = combinedSource.runWith(Sink.seq(), materializer).toCompletableFuture().get(10, TimeUnit.SECONDS);

        Map<String, List<Pair<Path, String>>> fileByAppName = new ConcurrentHashMap<>();

        for(Pair<Pair<Path, String>, ByteString> p: combinedResult) {
            //System.out.println(p.first().first() + "---" + p.first().second() + "===" + p.second().utf8String());
            if(!fileByAppName.containsKey(p.first().second())) {
                fileByAppName.put(p.first().second(), new ArrayList<>());
            }
            fileByAppName.get(p.first().second()).add(new Pair<>(p.first().first(), p.second().utf8String()));
        }

        System.out.println(fileByAppName);
        System.out.println("Total time taken to process all files " + (System.currentTimeMillis()-startTime)/1000 + " seconds!");
        system.terminate();

        /*int i = 0;
        for(Pair<Path, String> pair: resultSourcePairByPath) {
            System.out.println(pair.first() + "---" + pair.second() + "===" + resultSourceFileByPath.get(i).utf8String());
            //Thread.sleep(10000);
            i++;
        }*/

        //final Source<Pair<Pair<Path, String>, ByteString>, NotUsed> combinedSource = sourcePairByPath.zipWith(sourceFileByPath, (pp, bs) ->  new Pair(pp, bs));

        //final List<Pair<Pair<Path, String>, ByteString>> result = combinedSource.runWith(Sink.seq(), materializer).toCompletableFuture().get(10, TimeUnit.SECONDS);


        /*final List<Pair<String, List<Path>>> result =
                source.runWith(Sink.seq(), materializer).toCompletableFuture().get(3, TimeUnit.SECONDS);*/


        /*for(Pair<Pair<Path, String>, ByteString> path: result) {
            System.out.println(path.first().first() + "---" + path.first().second() + "===" + path.second().utf8String());
            Thread.sleep(1000);
        }*/
    }

    public void walkthruDirectoryAndReadFiles() throws Exception {
        ActorSystem system = ActorSystem.create();
        Materializer materializer = ActorMaterializer.create(system);

        //final Path dir = fs.getPath("/Users","relumalai","Downloads","hackathon18","CHE");


        final Source<ByteString, NotUsed> source = Directory.walk(dir)
                .via(validSourcePaths)
                .via(fileBytes);


        final List<ByteString> result =
                source.runWith(Sink.seq(), materializer).toCompletableFuture().get(3, TimeUnit.SECONDS);


        for(ByteString path: result) {
            System.out.println(path.utf8String());
        }
    }

    private boolean isValidSourceFile(Path p) {
        return p.toString().endsWith(".java") && p.toString().contains("main");
    }

}
