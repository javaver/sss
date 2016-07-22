package com.bbxpc.test;

public class AAA {

	/**  
	 * @Title: AAA.java
	 * @Package com.bbxpc.test
	 * @Description: TODO
	 * @author yajie
	 * @date 2016-7-19
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
String str="nContent-Length: 12\r\nContent-Type: text/html; charset=UTF-8\r\nHost: 127.0.0.1:3838\r\nConnection: Keep-Alive\r\nUser-Agent: Apache-HttpClient/4.3.6 (java 1.5)\r\nAccept-Encoding: gzip,deflate\r\n\r\n{\"中文\":0}\"";
System.out.println(str.split("\r\n\r\n")[2]);
	}

}
