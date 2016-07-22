package com.bbxpc.main;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class WebServer {
	Logger log = Logger.getRootLogger();
	static ExecutorService executorService = Executors.newCachedThreadPool();
	int port;
	public WebServer(int port){
		this.port=port;
	}
  /**
   * WebServer constructor.
   */
  protected void start() {
    ServerSocket s;
    log.debug("Super Smart Server is startup on"+port);
    log.debug("It is reciving msg and process it");
    log.debug(executorService.toString());
    log.debug("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(port);
    } catch (Exception e) {
      log.debug(e);
      return;
    }
    log.debug("Waiting for connection");
    for (;;) {
      try {
        Socket remote = s.accept();
        executorService.execute(new ProcessHandler(remote));
      } catch (Exception e) {
        log.debug("Error: " + e);
      }
    }
  }

/**
   * Start the application.
   * 
   * @param args
   *            Command line parameters are not used.
 * @throws InterruptedException 
   */
  public static void main(String args[]) throws InterruptedException {
    WebServer ws = new WebServer(3838);
    ws.start();
    Runtime.getRuntime().addShutdownHook(new Thread(){
    	public void run() {
    		executorService.shutdown();
    	}
    });
  }
  
}
