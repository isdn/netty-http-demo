package dev.isdn.demo.netty;

import dev.isdn.demo.netty.server.Config;
import dev.isdn.demo.netty.server.Server;

public class App {

    public static void main(String[] args) {

        Config cnf = new Config();
        cnf.setPort(8080).setHost("localhost");
        if (args.length > 0) {
            cnf.setPort(Integer.parseInt(args[0]));
        }
        new Server(cnf).run();
    }
}
