package com.micro.graph;

import java.nio.file.Path;

public class FileInfo {

    private String content;
    private boolean fromClientModule = false;
    private Path path;


    public FileInfo() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isFromClientModule() {
        return fromClientModule;
    }

    public void setFromClientModule(boolean fromClientModule) {
        this.fromClientModule = fromClientModule;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
