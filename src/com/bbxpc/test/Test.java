package com.bbxpc.test;

import java.text.DecimalFormat;

import org.sword.lang.HttpUtils;

public class Test {

	/**  
	 * @Title: Test.java
	 * @Package org.sword.wechat4j.user
	 * @Description: TODO
	 * @author yajie
	 * @date 2016-7-5
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i=0;i<1;i++){
			String ret=HttpUtils.post("http://127.0.0.1:3838/","{\"中文\":"+i+"}");
			System.out.println(i+"--->"+ret);
		}
	}

}
