package com.bbxpc.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.bbxpc.consumer.Worker;

/**
 * 为每个client启动handler
 * @Title: ProcessHandler.java
 * @Package com.bbxpc.main
 * @Description: TODO
 * @author yajie
 * @date 2016-7-15
 */
public class ProcessHandler extends Worker implements Runnable {

	private Socket remote;
	Logger log = Logger.getLogger(ProcessHandler.class);
	public ProcessHandler(Socket remote) {
		 super("");
		this.remote = remote;
	}

	@Override
	public void run() {
		try {
			log.debug(remote.getRemoteSocketAddress().toString()+ "--> Connected in.");
			 InputStream is = remote.getInputStream();
			 OutputStream out=remote.getOutputStream();
			 StringBuilder sbf=new StringBuilder();
	            while(is.available() == 0){
	                Thread.sleep(10); //检测客户端是否发送过来了请求数据
	            }
	            byte[] responsePacket = new byte[1024];
	            int totle = 0;
	            int hasRead = 0;
	            while(is.available() > 0){
	            	totle += hasRead;
	            	hasRead=is.read(responsePacket);
	            	 sbf.append(new String(responsePacket, 0, hasRead, "UTF-8"));
	            }
	          log.debug("header[*]:"+sbf.toString());
	           int start=sbf.indexOf("{");
	           if(start>0){
	        	   String msg=sbf.substring(start);
	        	   log.debug("recive:[*]"+msg);
	        	   doWork(msg);
	   	            sendResponse(out, remote);
	           }else{
	        	   log.debug(remote.getRemoteSocketAddress()+"-->\r\n--->"+sbf);
	        	   remote.close();
	           }
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("Error: " + e);
			return;
		}
	}

	private void sendResponse(OutputStream out, Socket remote) {
		try {
			// Send the HTML page
			StringBuilder buf = new StringBuilder();
			StringBuilder responseHeader=new StringBuilder();
			buf.append("{");
			buf.append("\"status\":0,");
			buf.append("\"msg\":\"sucess\",");
			buf.append("\"time\":");
			buf.append(System.currentTimeMillis());
			buf.append("}");
			responseHeader.append("HTTP/1.1 200 OK\n");
			responseHeader.append("Server: SSS/1.1\n");
			responseHeader.append("Content-Type: application/json;charset=utf-8\n");
			responseHeader.append("Content-Length:");
			responseHeader.append(buf.length());
			responseHeader.append("\r\n\r\n");
			responseHeader.append(buf);
			out.write(responseHeader.toString().getBytes("utf-8"));
			log.debug("[*]send-->" + responseHeader);
			out.flush();
			out.close();
			remote.close();
			remote = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
