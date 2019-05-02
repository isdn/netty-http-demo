package dev.isdn.demo.netty.server;

public class Config {

    private String host = "localhost";
    private int port = 8000;

    public Config() {
    }

    public Config(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Config setHost(String host) {
        this.host = host;
        return this;
    }

    public Config setPort(int port) {
        this.port = port;
        return this;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

}
