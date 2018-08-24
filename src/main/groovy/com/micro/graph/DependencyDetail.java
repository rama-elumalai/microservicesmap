package com.micro.graph;

public class DependencyDetail {

    private final String appName;
    private final AppDetail appDetail;

    public DependencyDetail(String appName, AppDetail appDetail) {
        this.appName = appName;
        this.appDetail = appDetail;
    }

    public String getAppName() {
        return appName;
    }

    public AppDetail getAppDetail() {
        return appDetail;
    }
}
