package com.bbxpc.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.bbxpc.consumer.Worker;
import com.bbxpc.sss.Response;


public class SS {
	static Charset charset = Charset.forName("UTF-8"); // 字符集
	static CharsetEncoder encoder = charset.newEncoder(); // 编码器
	static CharsetDecoder decoder=Charset.forName("UTF-8").newDecoder();
	static Logger log = Logger.getRootLogger();
	static ExecutorService executorService = Executors.newCachedThreadPool();
	/**  
	 * @Title: SS.java
	 * @Package com.bbxpc.test
	 * @Description: TODO
	 * @author yajie
	 * @date 2016-7-20
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args){
		ServerSocketChannel channel;
		try {
			channel = ServerSocketChannel.open();
			Selector selector = Selector.open();
	        channel.configureBlocking(false);
	        ServerSocket socket = channel.socket();
	        socket.bind(new InetSocketAddress(3838));
	        log.info("=======================服务已启动========================");
	        log.info("端口:3838");
			channel.register(selector, SelectionKey.OP_ACCEPT);
			Runtime.getRuntime().addShutdownHook(new Thread(){
	        	public void run() {
	        		log.info("shutdown连接池......");
	        		executorService.shutdown();
	        	}
	        });
			while (selector.select() > 0)
	        {
	            Set<SelectionKey> keys = selector.selectedKeys();
	            Iterator<SelectionKey> iterator = keys.iterator();
	            while (iterator.hasNext())
	            {
	                SelectionKey key = iterator.next();
	                if (key.isAcceptable())
	                {
	                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
	                    SocketChannel tmpChannel = serverChannel.accept();
	                    tmpChannel.configureBlocking(false);
	                    tmpChannel.register(selector, SelectionKey.OP_READ);
	                } else if (key.isReadable())
	                {
	                    log.info("<<<<<<<<<<<<<接收参数开始<<<<<<<<<<<<<<<<<<<<<");
	                    read(selector,key);
	                    log.info("<<<<<<<<<<<<<接收参数结束<<<<<<<<<<<<<<<<<<<");
	                } else if (key.isWritable())
	                {
	                	log.info(">>>>>>>>>>>>>>响应结果开始>>>>>>>>>>>>>>>>>>>");
	                	write(key);
	                	log.info(">>>>>>>>>>>>>>响应结果结束>>>>>>>>>>>>>>>>>>>");
	                }
	                iterator.remove();
	            }
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//读信息
    private static void read(Selector selector,SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        int c;
		try {
			c = client.read(buffer);
			  if (c > 0) {
		            buffer.flip();//flip方法将Buffer从写模式切换到读模式
		            CharBuffer charBuffer = decoder.decode(buffer);
		            //接收请求
		            String req=charBuffer.toString();
		            String []array=req.split("\r\n\r\n");
		            log.info(executorService.toString()+ "\r\n["+client.getRemoteAddress().toString()+"]客户端请求参数:"+req);
		            log.info("array[]大小:"+array.length);
		            if(array.length>=2){
		            	req=array[1];
			            if(req.startsWith("{")){
			            	executorService.execute(new Worker(req));
			            }
		            }
		            
		        } else {
		            client.close();
		        }
		        client.register(selector, SelectionKey.OP_WRITE);
		        buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
      
    }
  //响应信息
    private static void write(SelectionKey key)  {
        SocketChannel client = (SocketChannel) key.channel();
        StringBuilder buf = new StringBuilder();
		buf.append("{");
		buf.append("\"status\":0,");
		buf.append("\"msg\":\"sucess\",");
		buf.append("\"time\":");
		buf.append(System.currentTimeMillis());
		buf.append("}");
        String res = Response.mix(buf.toString());
        response(client,res);
    }
    private static void response( SocketChannel client,String body) {
    	log.info("返回参数："+body);
    	try {
			client.write(encoder.encode(CharBuffer.wrap(body)));
			 client.close();
		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			log.info(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.info(e);
		}
    	
    }
} 