package com.bbxpc.http;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;




/**
 * HttpClient 简单的http客户端支持http https
 * @author yajie
 * 
 */
public class HttpsClient {

	
	public static String get(String https_url) {
		URL url;
		String ret="";
		try {

			url = new URL(https_url);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			ret=getContent(con);
 		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String sendGet(String url) {
		  StringBuilder result = new StringBuilder();
	        BufferedReader in = null;
	        try {
	            String urlNameString = url;
	            URL realUrl = new URL(urlNameString);
	            // 打开和URL之间的连接
	            URLConnection connection = realUrl.openConnection();
	            // 设置通用的请求属性
	            connection.setRequestProperty("accept", "*/*");
	            connection.setRequestProperty("connection", "close");
	            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
	            // 建立实际的连接
	            connection.connect();
	            // 定义 BufferedReader输入流来读取URL的响应
	            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));  
	            String line;
	            while ((line = in.readLine()) != null) {
	            	result.append(line);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        // 使用finally块来关闭输入流
	        finally {
	            try {
	                if (in != null) {
	                    in.close();
	                }
	            } catch (Exception e2) {
	                e2.printStackTrace();
	            }
	        }
	        return result.toString();
	}
	private static String getContent(HttpsURLConnection con) { 
		StringBuilder sbf=new StringBuilder();
		if (con != null) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String input;
				while ((input = br.readLine()) != null) {
					sbf.append(input);
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sbf.toString();
	}
	
	public static String sendPost(String url, Map<String, String> parameters) {  
        String result = "";// 返回的结果  
        BufferedReader in = null;// 读取响应输入流  
        PrintWriter out = null;  
        StringBuffer sb = new StringBuffer();// 处理请求参数  
        String params = "";// 编码之后的参数  
        try {  
            // 编码请求参数  
            if (parameters.size() == 1) {  
                for (String name : parameters.keySet()) {  
                    sb.append(name).append("=").append(  
                            java.net.URLEncoder.encode(parameters.get(name),  
                                    "UTF-8"));  
                }  
                params = sb.toString();  
            } else {  
                for (String name : parameters.keySet()) {  
                    sb.append(name).append("=").append(  
                            java.net.URLEncoder.encode(parameters.get(name),  
                                    "UTF-8")).append("&");  
                }  
                String temp_params = sb.toString();  
                params = temp_params.substring(0, temp_params.length() - 1);  
            }  
            // 创建URL对象  
            java.net.URL connURL = new java.net.URL(url);  
            // 打开URL连接  
            java.net.HttpURLConnection httpConn = (java.net.HttpURLConnection) connURL  
                    .openConnection();  
            // 设置通用属性  
            httpConn.setRequestProperty("Accept", "*/*");  
            httpConn.setRequestProperty("Connection", "Keep-Alive");  
            httpConn.setRequestProperty("User-Agent",  
                    "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1)");  
            // 设置POST方式  
            httpConn.setDoInput(true);  
            httpConn.setDoOutput(true);  
            // 获取HttpURLConnection对象对应的输出流  
            out = new PrintWriter(httpConn.getOutputStream());  
            // 发送请求参数  
            out.write(params);  
            // flush输出流的缓冲  
            out.flush();  
            // 定义BufferedReader输入流来读取URL的响应，设置编码方式  
            in = new BufferedReader(new InputStreamReader(httpConn  
                    .getInputStream(), "UTF-8"));  
            String line;  
            // 读取返回的内容  
            while ((line = in.readLine()) != null) {  
                result += line;  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if (out != null) {  
                    out.close();  
                }  
                if (in != null) {  
                    in.close();  
                }  
            } catch (IOException ex) {  
                ex.printStackTrace();  
            }  
        }  
        return result;  
    }  
	public synchronized static String sendPost(String url, String params) {  
		System.out.println("发送请求==>"+url+"参数:"+params);
        StringBuilder result = new StringBuilder();// 返回的结果  
        BufferedReader in = null;// 读取响应输入流  
        OutputStreamWriter out = null;  
        try {  
            // 创建URL对象  
            URL connURL = new URL(url);  
            HttpURLConnection httpConn = (HttpURLConnection) connURL.openConnection();   // 打开URL连接  
            httpConn.setRequestProperty("Accept", "*/*");  // 设置通用属性  
            httpConn.setRequestProperty("Connection","Keep-Alive");  
            httpConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");  
            httpConn.setRequestProperty("Cookie", "this is a inner program to rquest api interface author by yajie.");
            httpConn.setConnectTimeout(5000);
            httpConn.setDoInput(true);// 设置POST方式  
            httpConn.setDoOutput(true);  
          //当存在post的值时，才打开OutputStreamWriter
	        if(params!=null && params.toString().trim().length()>0){
	        	out = new OutputStreamWriter(httpConn.getOutputStream(),"UTF-8");
	        	out.write(params.toString());
	        	out.flush();
	        }
            //如果状态不为200则读取errorstream
            if(httpConn.getResponseCode()==500){
            	in = new BufferedReader(new InputStreamReader(httpConn.getErrorStream(),"UTF-8"));
	        }else{
	        	in = new BufferedReader(new InputStreamReader(httpConn.getInputStream(),"UTF-8"));
	        }
            String line;  
            // 读取返回的内容  
            while ((line = in.readLine()) != null) {  
            	result.append(line);  
            }
           System.out.println("<<<<<<<<<响应代码:"+httpConn.getResponseCode()+":"+httpConn.getResponseMessage()+"\t"+result);
            httpConn.disconnect();
            httpConn=null;
            connURL=null;
           if(null!=out){out.close();}
           if(null!=in){in.close();}
            out=null;
            in=null;
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        return result.toString();  
    }  
	
	 
}