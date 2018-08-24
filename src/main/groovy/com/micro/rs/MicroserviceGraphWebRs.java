package com.micro.rs;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.ConnectHttpImpl;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;
import akka.http.scaladsl.UseHttp2;
import akka.io.Tcp;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.micro.graph.AppDetail;
import com.micro.graph.ParseRepoToDs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;

public class MicroserviceGraphWebRs extends AllDirectives {

    static LoggingAdapter log;

    static Map<String, Map<String, Map<String, List<AppDetail>>>> graph = new HashMap<>();

    public static void main(String[] args) throws Exception {


        int i = 0;
        List<String> filePaths = new ArrayList<>();
        String gsonFileLocation = "";
        String host = "127.0.0.1";
        int port = 8080;
        int index;
        for (index = 0; index < args.length; ++index) {
            System.out.println("args[" + index + "]: " + args[index]);
        }
        for (String arg : args) {
            if(i+1 != args.length) {
                if(i==0) {
                    host = arg;
                } else if (i==1) {
                    port = Integer.valueOf(arg);
                } else {
                    filePaths.add(arg);
                }
            } else {
                gsonFileLocation = arg;
            }
            i++;
        }
        ParseRepoToDs parseRepoToDs = new ParseRepoToDs();
        graph = parseRepoToDs.run(filePaths, gsonFileLocation);

        // boot up server using the route as defined below
        ActorSystem system = ActorSystem.create("routes");

        log =  Logging.getLogger(system, new MicroserviceGraphWebRs());
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        MicroserviceGraphWebRs app = new MicroserviceGraphWebRs();


        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);

        //enable remote access
        //ConnectHttp connectHttp = new ConnectHttpImpl("0.0.0.0", 8080, new UseHttp2.Negotiated$());
        //final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, connectHttp, materializer);

        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host, port), materializer);

        System.out.println("Server online at http://"+host+":"+port + "/");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    // (fake) async database query api
    private CompletionStage<Optional<Item>> fetchItem(String name, boolean showMore) {
        Item item = new Item();
        if(showMore) {
            item.setMicroMapWithMoreInfo(graph.get(name));
        } else {
            Map<String, Set<String>> lessInfo = new HashMap<>();
            if(graph.containsKey(name)) {
                graph.get(name)
                        .keySet()
                        .stream()
                        .forEach(type -> {
                            lessInfo.put(type, graph.get(name).get(type).keySet());
                        });
                item.setMicroMapWithLessInfo(lessInfo);
            }
        }
        return CompletableFuture.completedFuture(Optional.of(item));
    }

    private Route createRoute() {

        return route(
                path("dependencies", () ->
                        route(
                                get(() ->
                                        parameter("name", name ->
                                                parameterOptional(StringUnmarshallers.BOOLEAN, "more", showMore -> {
                                                    boolean show = showMore.isPresent() && showMore.get();
                                                    log.info("incoming request: name: {} , more: {} ", name, show);

                                                    final CompletionStage<Optional<Item>> futureMaybeItem = fetchItem(name, show);
                                                    return onSuccess(futureMaybeItem, maybeItem ->
                                                            maybeItem.map(item -> completeOK(item, Jackson.marshaller()))
                                                                    .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Not Found"))
                                                    );
                                                })
                                        ))
                        )));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Item {

        public Item() {
        }

        Map<String, Map<String, List<AppDetail>>> microMapWithMoreInfo = null;
        Map<String, Set<String>> microMapWithLessInfo = null;

        public Map<String, Map<String, List<AppDetail>>> getMicroMapWithMoreInfo() {
            return microMapWithMoreInfo;
        }

        public void setMicroMapWithMoreInfo(Map<String, Map<String, List<AppDetail>>> microMapWithMoreInfo) {
            this.microMapWithMoreInfo = microMapWithMoreInfo;
        }

        public Map<String, Set<String>> getMicroMapWithLessInfo() {
            return microMapWithLessInfo;
        }

        public void setMicroMapWithLessInfo(Map<String, Set<String>> microMapWithLessInfo) {
            this.microMapWithLessInfo = microMapWithLessInfo;
        }
    }
}
