package com.micro.graph;

public class DependencyInfo {

    private String filePath;
    private String variableName;
    private String methodNameCalled;

    public DependencyInfo() {
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getMethodNameCalled() {
        return methodNameCalled;
    }

    public void setMethodNameCalled(String methodNameCalled) {
        this.methodNameCalled = methodNameCalled;
    }

    @Override
    public String toString() {
        return "DependencyInfo{" +
                "filePath='" + filePath + '\'' +
                ", variableName='" + variableName + '\'' +
                ", methodNameCalled='" + methodNameCalled + '\'' +
                '}';
    }
}
