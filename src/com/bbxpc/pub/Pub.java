/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbxpc.pub;

/**
 *
 * @author yajie
 */
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Pub {

  private static final String TASK_QUEUE_NAME = "msg_queue";
 private static ConnectionFactory factory;
 private static  Connection connection;
 private static  Channel channel;
 
  static{
        try {
            factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("exit.............");
                    channel.close();
                    connection.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (TimeoutException ex) {
                    ex.printStackTrace();
                }
            }
            });

        } catch (IOException ex) {
           ex.printStackTrace();
        } catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
          
  }
  public static void queue(String msg){
      if(null==msg || msg.length()<=0){
          return;
      }
      try {
          
          channel.basicPublish( "", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,msg.getBytes());
         System.out.println(">>>>>>>:"+msg+"\t"+System.currentTimeMillis());
      } catch (IOException ex) {
         ex.printStackTrace();
      }
      
  }
  public static void main(String[] argv) throws Exception {


//          ConnectionFactory factory = new ConnectionFactory();
//          factory.setHost("localhost");
//          Connection connection = factory.newConnection();
//          Channel channel = connection.createChannel();
//          
//          channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
//        
//          
//          channel.basicPublish( "", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes());
//          System.out.println(" [x] Sent '" + message + "'");
//          
//          channel.close();
//          connection.close();
     
  }      
 
}