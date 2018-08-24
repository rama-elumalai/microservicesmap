package com.micro.graph;

public class AppDetail {

    private final String appName;
    private final String methodName;
    private final String serviceClassName;
    private final String path;
    private final String moduleName;

    public AppDetail(String appName, String methodName, String serviceClassName, String path, String moduleName) {
        this.appName = appName;
        this.methodName = methodName;
        this.serviceClassName = serviceClassName;
        this.path = path;
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getAppName() {
        return appName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "AppDetail{" +
                "appName='" + appName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", serviceClassName='" + serviceClassName + '\'' +
                ", path='" + path + '\'' +
                ", moduleName='" + moduleName + '\'' +
                '}';
    }
}
