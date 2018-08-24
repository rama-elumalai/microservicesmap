package com.micro.graph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RepoInfo {

    private String appName = "";
    private boolean hasClient = false;
    private List<Path> filePaths = new ArrayList<>();
    private List<String> fileInfos = new ArrayList<>();
    private List<FileInfo> fileInfoList = new ArrayList<>();
    private Set<String> clientModuleServiceClasses = new HashSet<>();
    private Map<String, String> moduleClassToPackage = new HashMap<>();
    private Set<String> clientModuleClasses = new HashSet<>();
    private String correspondingMiddleName;
    private String correspondingClientName;
    private Map<String, Set<String>> serviceClassesByModuleNameMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, Map<String, Set<DependencyInfo>>>> dependencies = new ConcurrentHashMap<>();
    private Map<String, RepoInfo> allInfoMap = new ConcurrentHashMap<>();
    //Map<DependentAppName, Map<ModuleName, Map<ServiceClassName, Set<DependencyInfo>>


    public Map<String, String> getModuleClassToPackage() {
        return moduleClassToPackage;
    }

    public void setModuleClassToPackage(Map<String, String> moduleClassToPackage) {
        this.moduleClassToPackage = moduleClassToPackage;
    }

    public Map<String, RepoInfo> getAllInfoMap() {
        return allInfoMap;
    }

    public void setAllInfoMap(Map<String, RepoInfo> allInfoMap) {
        this.allInfoMap = allInfoMap;
    }

    public Map<String, Set<String>> getServiceClassesByModuleNameMap() {
        return serviceClassesByModuleNameMap;
    }

    public void setServiceClassesByModuleNameMap(Map<String, Set<String>> serviceClassesByModuleNameMap) {
        this.serviceClassesByModuleNameMap = serviceClassesByModuleNameMap;
    }

    public Map<String, Map<String, Map<String, Set<DependencyInfo>>>> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, Map<String, Map<String, Set<DependencyInfo>>>> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getClientModuleClasses() {
        return clientModuleClasses;
    }

    public void setClientModuleClasses(Set<String> clientModuleClasses) {
        this.clientModuleClasses = clientModuleClasses;
    }

    public Set<String> getClientModuleServiceClasses() {
        return clientModuleServiceClasses;
    }

    public void setClientModuleServiceClasses(Set<String> clientModuleServiceClasses) {
        this.clientModuleServiceClasses = clientModuleServiceClasses;
    }

    public String getCorrespondingClientName() {
        return correspondingClientName;
    }

    public void setCorrespondingClientName(String correspondingClientName) {
        this.correspondingClientName = correspondingClientName;
    }

    public String getCorrespondingMiddleName() {
        return correspondingMiddleName;
    }

    public void setCorrespondingMiddleName(String correspondingMiddleName) {
        this.correspondingMiddleName = correspondingMiddleName;
    }

    public RepoInfo() {
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isHasClient() {
        return hasClient;
    }

    public void setHasClient(boolean hasClient) {
        this.hasClient = hasClient;
    }

    public List<Path> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<Path> filePaths) {
        this.filePaths = filePaths;
    }

    public List<String> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(List<String> fileInfos) {
        this.fileInfos = fileInfos;
    }

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }
}
