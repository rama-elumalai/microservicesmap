package com.micro.graph

import static groovy.io.FileType.FILES

class RepoParser {
    static Map<String, Set<String>> setOfServicesByAppNameMap = new HashMap<>()
    static Map<String, String> appNameByServicesMap = new HashMap<>()
    static Map<String,Map<String, String>> appNameBymethodDependenciesMap = new HashMap<>()
    static Map<String, Map<String, Set<String>>> appNameAndItsDependencies = new HashMap<>()
    Map<String, String> moduleNameByAppNameMap = new HashMap<>();
    static Map<String, Set<String>> listOfDependencyByAppName = new TreeMap<>()
    Map<String, String> clientNameVsmiddleName = ['callerid-middle-client':'callerid-middle',
                                                  'cdvr-client':'cdvr-middle',
                                                  'amsnotification':'amsnotification-middle',
                                                  'vodmdms-client':'vodmdms-middle',
                                                  'ppv-ams-client':'ppv-middle',
                                                  'lineup-client':'lineup-middle',
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

    static List<String> spectrumServices = ['amsnotification',
                                     'app-edge',
                                     'bookmarks-edge',
                                     'bookmarks-middle',
                                     'cloudscheduler-cron',
                                     'cloudscheduler-middle',
                                     'companiondevices-edge',
                                     'companiondevices-middle',
                                     'device-edge',
                                     'device-middle',
                                     'deviceactivation-edge',
                                     'devicemanager-edge',
                                     'dvr-edge',
                                     'dvr-middle',
                                     'epg-middle',
                                     //'eventlogger-edge',
                                     'favoritechannels-edge',
                                     'instantupgrade-edge',
                                     'instantupgrade-middle',
                                     'lrm-edge',
                                     'lrm-middle',
                                     'networksettings-edge',
                                     'networksettings-middle',
                                     'ppv-middle',
                                     'ppvmetadata-cron',
                                     'reminders-middle',
                                     'rentals-middle',
                                     //'sports-edge',
                                     'stb-lookup-middle',
                                     'stbdevice-edge',
                                     //'tts-edge',
                                     'videocatalog-edge',
                                     'videocatalog-middle',
                                     'videosession-edge',
                                     //'videosession-middle',
                                     'viewinghistory-edge',
                                     'viewinghistory-middle']

    static boolean doesRepoHasClientModule(File eachDir) {
        boolean  clientFound = false
        if(eachDir.getName().endsWith("middle") || eachDir.getName().equalsIgnoreCase("amsnotification")) {
            new File(eachDir.getPath()).eachDir() {
                println(it.getName())
                if (it.getName().toLowerCase().equalsIgnoreCase("client")) {
                    clientFound = true
                }
            }
        }
        return clientFound
    }

    static Map<String, String> parseRepoToFindModules(String filePath, String appName) {
        Map<String, String> moduleNameByAppNameMap = new HashMap<>();

        try {
            Set<String> serviceClassSet = new HashSet<>()
            new File(filePath).eachFileRecurse(FILES) {
                if (it.name.toLowerCase().contains('module') && !it.name.toLowerCase().contains("local") && !it.name.toLowerCase().contains("test")) {
                    String moduleName = it.getName().replace(".java", "")
                    it.eachLine { line ->
                        String trimLine = line.trim();
                        if (trimLine.startsWith("bind")) {
                            String serviceClass = trimLine.substring(trimLine.indexOf("(") + 1, trimLine.indexOf("."))
                            appNameByServicesMap.put(serviceClass, appName)
                            serviceClassSet.add(serviceClass)
                        }
                    }
                    println appName + " - " + moduleName
                    moduleNameByAppNameMap.put(moduleName, appName)
                }
            }
            setOfServicesByAppNameMap.put(appName, serviceClassSet);
        } catch (Exception e) {
            println (e.getStackTrace())
        }
        return moduleNameByAppNameMap;


    }

    void parseRepos(String basePath) {
        new File(basePath).eachDir() {

                //get client's module from middle's multi-module(client) project
            eachDir ->
                String appName = eachDir.getName()
               // if (spectrumServices.contains(appName)) {

                    List<String> moduleNames = new ArrayList<>();
                    if (eachDir.getName().endsWith("middle") || eachDir.getName().equalsIgnoreCase("amsnotification") || ((eachDir.getName().endsWith("client")) && !eachDir.getName().equalsIgnoreCase("charter-client"))) {
                        println(eachDir.getPath())
                        boolean lookIntoDirectory = false;
                        boolean clientModduleFound = doesRepoHasClientModule(eachDir)
                        println("client module " + (clientModduleFound ? "found " : "not found ") + "in " + eachDir.getName())
                        String filePath = ""
                        if (clientModduleFound) {
                            filePath = eachDir.getPath() + "/client"
                            appName = eachDir.getName()
                            lookIntoDirectory = true;
                        }

                        if ((eachDir.getName().endsWith("client")) && !eachDir.getName().equalsIgnoreCase("charter-client")) {
                            clientNameVsmiddleName.each { key, value ->
                                if (key.equalsIgnoreCase(eachDir.getName())) {
                                    appName = value
                                }
                            }
                            filePath = eachDir.getPath()
                            lookIntoDirectory = true;
                        }

                        moduleNameByAppNameMap.putAll(parseRepoToFindModules(filePath, appName))

                        /*if(lookIntoDirectory) {
                        moduleNameByAppNameMap.putAll(parseRepoToFindModules(filePath, appName))
                    }*/
                    }

                    //get client's module from separate client's project
                    /*if ((eachDir.getName().endsWith("client"))&& !eachDir.getName().equalsIgnoreCase("charter-client")) {
                    //println eachDir.getName()
                    clientNameVsmiddleName.each {key,value->
                        if(key.equalsIgnoreCase(eachDir.getName())) {
                            appName = value
                        }
                    }
                    new File(eachDir.getPath()).eachFileRecurse(FILES) {
                        if (it.name.toLowerCase().contains('module') && !it.name.toLowerCase().contains("local") && !it.name.toLowerCase().contains("test")) {
                            String moduleName = it.getName().replace(".java","")
                            it.eachLine {line->
                                if(line.trim().startsWith("bind")) {
                                    println line
                                    //TODO: store
                                }
                            }
                            println appName + " - " + moduleName

                            moduleNames.add(moduleName)
                            moduleNameByAppNameMap.put(moduleName,appName)

                        }
                    }
                }*/
                    //listOfModuleByAppNameMap.put(appName, moduleNames)

                //}
        }
    }

    void getDependencies(String basePath) {

        new File(basePath).eachDir() {
            eachDir ->
                String appName = eachDir.getName()
                //if (spectrumServices.contains(appName)) {
                    Map<String, String> appNameBymethodNameMap = new HashMap<>()
                    Map<String, Set<String>> listOfMethodNamesByAppName = new HashMap<>()
                    Map<String, String> appNameByserviceClassNameMap = new HashMap<>()

                    Set<String> usedServiceSet = new HashSet<>()
                    Set<String> dependentAppName = new HashSet<>()
                    if (eachDir.getName().endsWith("cron") || eachDir.getName().endsWith("middle") || eachDir.getName().endsWith("edge") || eachDir.getName().equalsIgnoreCase("amsnotification")) {
                        println("Analyzing app: " + appName + " to get the dependencies")
                        Set<String> serviceClassSet = setOfServicesByAppNameMap.get(appName)
                        println appName + " : " + serviceClassSet

                        new File(eachDir.getPath()).eachFileRecurse(FILES) { file ->
                            //if(it.name.toLowerCase().contains("java")) {
                            String fileName = "";
                            moduleNameByAppNameMap.each { key, value ->
                                if (!file.getPath().contains("/test/") && file.getPath().contains(".java")) {
                                    fileName = file.getPath()
                                    if (file.text.contains(key)) {
                                        if (!dependentAppName.contains(value + "[" + key + "]") && !appName.equalsIgnoreCase(value)) {
                                            println("Found dependency: " + key + "," + value + " :: found in " + file.getPath())
                                            //dependentAppName.add(value+"["+key+"]")
                                            dependentAppName.add(value)
                                        }
                                    }
                                    appNameByServicesMap.remove("ObjectMapper")
                                    appNameByServicesMap.remove("RestClient")
                                    Set<String> serviceFieldName = new HashSet<>()
                                    String fileText = ""
                                    file.eachLine { line ->
                                        fileText += line
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
                            if (fileName.length() > 0) {
                                //println fileName
                            }
                            //}

                        }
                        if (!dependentAppName.isEmpty() || dependentAppName.size() >= 1) {
                            listOfDependencyByAppName.put(appName, dependentAppName)
                            println(appName + " - " + dependentAppName)
                        } else {
                            println(appName + " - no dependencies")

                        }
                        appNameBymethodDependenciesMap.put(appName, appNameBymethodNameMap)
                        appNameAndItsDependencies.put(appName, listOfMethodNamesByAppName)
                        println "appNameBymethodNameMap: " + appNameBymethodNameMap

                        println "listOfMethodNamesByAppName: " + listOfMethodNamesByAppName
                    }
                //}
        }
    }

    void printDependencies() {
        Integer divIndex = 0;
        listOfDependencyByAppName.eachWithIndex {event ->
            if (event.value != null && event.value.size() > 0) {
                divIndex++
                //println event.key
                println "g = new Dracula.Graph();"

            }
            event.value.each { dep ->
                println("g.addEdge(\"${event.key}\", \"$dep\");")
            }
            if (event.value != null && event.value.size() > 0) {
                println "var layouter${divIndex} = new Dracula.Layout.Spring(g);"
                println "layouter${divIndex}.layout();"
                println "var renderer${divIndex} = new Dracula.Renderer.Raphael('#paper${divIndex}', g, 1000, 600);"
                println "renderer${divIndex}.draw();"
            }

        }
        (1..divIndex).each {
            println("<div id=\"paper${divIndex}\" style=\"float: left;\"></div>")
        }
    }

    void printSpectrumMsDependencies() {
        Integer divIndex = 1;
        println "g = new Dracula.Graph();"

        listOfDependencyByAppName.eachWithIndex {event ->
            if(spectrumServices.contains(event.key)) {
                event.value.each { dep ->
                    println("g.addEdge(\"${event.key}\", \"$dep\", { directed: true });")
                }
            }


        }
        println "var layouter${divIndex} = new Dracula.Layout.Spring(g);"
        println "layouter${divIndex}.layout();"
        println "var renderer${divIndex} = new Dracula.Renderer.Raphael('#paper${divIndex}', g, 1000, 600);"
        println "renderer${divIndex}.draw();"
        (1..divIndex).each {
            println("<div id=\"paper${divIndex}\" style=\"float: left;\"></div>")
        }
    }

    static void processFiles(List<String> paths){
        RepoParser repoParser = new RepoParser()
        for(String path: paths) {
            repoParser.parseRepos(path)
        }
        for(String path: paths) {
            repoParser.getDependencies(path)
        }


        /*repoParser.parseRepos("/Users/relumalai/Downloads/hackathon18/CHE/")
        repoParser.parseRepos("/Users/relumalai/Downloads/hackathon18/SAD/")
        repoParser.getDependencies("/Users/relumalai/Downloads/hackathon18/CHE/")
        repoParser.getDependencies("/Users/relumalai/Downloads/hackathon18/SAD/")
        */

        //repoParser.printDependencies()
        //repoParser.printSpectrumMsDependencies()
    }
}
