package com.bbxpc.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 我的Hello服务器
 * @Title: MyHello.java
 * @Package com.bbxpc.test
 * @Description: TODO
 * @author yajie
 * @date 2016-7-18
 */
public class MyHello {
	
	private Selector selector;
	
	 private Charset charset = Charset.forName("UTF-8");		//字符集
	 private CharsetEncoder encoder = charset.newEncoder();	//编码器
	  
	 int port;
	
	private boolean debug = true; //是否要调试
	 
	public MyHello( int port){
		this.port=port;
	}
	/**  
	 * @Title: MyHello.java
	 * @Package com.bbxpc.test
	 * @Description: TODO
	 * @author yajie
	 * @date 2016-7-18
	 * @param args
	 */
	public static void main(String[] args) {
		MyHello hello=new MyHello( 3838);
		hello.process();
		
	}
	private void process(){
		try {
			this.selector = Selector.open();
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
			System.out.println("Server started on "+port+"...");
			 while (true) {
	                selector.selectNow();
	                Iterator<SelectionKey> i = selector.selectedKeys().iterator();
	                while (i.hasNext()) {
	                    SelectionKey key = i.next();
	                    i.remove();
	                    if (!key.isValid()) {
	                        continue;
	                    }
	                    try {
	                        // get a new connection
	                        if (key.isAcceptable()) {
	                            // accept them
	                            SocketChannel client = serverChannel.accept();
	                            // non blocking please
	                            client.configureBlocking(false);
	                            // show out intentions
	                            client.register(selector, SelectionKey.OP_READ);
	                            // read from the connection
	                        } else if (key.isReadable()) {
	                            //  get the client
	                            SocketChannel client = (SocketChannel) key.channel();
	                            // get the session
	                            HTTPSession session = (HTTPSession) key.attachment();
	                            // create it if it doesnt exist
	                            if (session == null) {
	                                session = new HTTPSession(client);
	                                key.attach(session);
	                            }
	                            // get more data
	                            session.readData();
	                            // decode the message
	                            String line;
	                            while ((line = session.readLine()) != null) {
	                            	System.out.println("-->"+line);
	                                // check if we have got everything
	                                if (line.isEmpty()) {
	                                    HTTPRequest request = new HTTPRequest(session.readLines.toString());
	                                    session.sendResponse(handle(session, request));
	                                    session.close();
	                                }
	                            }
	                        }else if(key.isWritable()){
	                        	
	                        }
	                    } catch (Exception ex) {
	                        System.err.println("Error handling client: " + key.channel());
	                        if (debug) {
	                            ex.printStackTrace();
	                        } else {
	                            System.err.println(ex);
	                            System.err.println("\tat " + ex.getStackTrace()[0]);
	                        }
	                        if (key.attachment() instanceof HTTPSession) {
	                            ((HTTPSession) key.attachment()).close();
	                        }
	                    }
	                }
	            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	 /**
     * Handle a web request.
     *
     * @param session the entire http session
     * @return the handled request
     */
    protected HTTPResponse handle(HTTPSession session, HTTPRequest request) throws IOException {
        HTTPResponse response = new HTTPResponse();
        response.setContent("已收到你的请求".getBytes());
        response.setBody("已收到你的请求");
        return response;
    } 
    
    public final class HTTPSession {
    	 
        private final SocketChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(2048);
        private final StringBuilder readLines = new StringBuilder();
        private int mark = 0;
 
        public HTTPSession(SocketChannel channel) {
            this.channel = channel;
        }
 
        /**
         * Try to read a line.
         */
        public String readLine() throws IOException {
            StringBuilder sb = new StringBuilder();
            int l = -1;
            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                sb.append(c);
                if (c == '\n' && l == '\r') {
                    // mark our position
                    mark = buffer.position();
                    // append to the total
                    readLines.append(sb);
                    // return with no line separators
                    return sb.substring(0, sb.length() - 2);
                }
                l = c;
            }
            return null;
        }
 
        /**
         * Get more data from the stream.
         */
        public void readData() throws IOException {
            buffer.limit(buffer.capacity());
            int read = channel.read(buffer);
            if (read == -1) {
                throw new IOException("End of stream");
            }
            System.out.println(charset.decode(buffer));
            buffer.flip();
            buffer.position(mark);
        }
 
        private void writeLine(String line) throws IOException {
            channel.write(encoder.encode(CharBuffer.wrap(line + "\r\n")));
        }
        private void response(String body) throws IOException {
            channel.write(encoder.encode(CharBuffer.wrap(body)));
        }
        public void sendResponse(HTTPResponse response) {
            response.addDefaultHeaders();
            try {
                writeLine(response.version + " " + response.responseCode + " " + response.responseReason);
                for (Map.Entry<String, String> header : response.headers.entrySet()) {
                    writeLine(header.getKey() + ": " + header.getValue());
                }
                writeLine("");
                response(response.body);
                //channel.write(ByteBuffer.wrap(response.content));
            } catch (IOException ex) {
                // slow silently
            }
        }
 
        public void close() {
            try {
                channel.close();
            } catch (IOException ex) {
            }
        }
    }
 
    public static class HTTPRequest {
 
        private final String raw;
        private String method;
        private String location;
        private String version;
        private Map<String, String> headers = new HashMap<String, String>();
 
        public HTTPRequest(String raw) {
            this.raw = raw;
            parse();
        }
 
        private void parse() {
            // parse the first line
            StringTokenizer tokenizer = new StringTokenizer(raw);
            method = tokenizer.nextToken().toUpperCase();
            location = tokenizer.nextToken();
            version = tokenizer.nextToken();
            // parse the headers
            String[] lines = raw.split("\r\n");
            for (int i = 1; i < lines.length; i++) {
                String[] keyVal = lines[i].split(":", 2);
                headers.put(keyVal[0], keyVal[1]);
            }
        }
 
        public String getMethod() {
            return method;
        }
 
        public String getLocation() {
            return location;
        }
 
        public String getHead(String key) {
            return headers.get(key);
        }
    }
 
    public static class HTTPResponse {
 
        private String version = "HTTP/1.1";
        private int responseCode = 200;
        private String responseReason = "OK";
        private Map<String, String> headers = new LinkedHashMap<String, String>();
        private byte[] content;
        private String body;
 
        private void addDefaultHeaders() {
            headers.put("Date", new Date().toString());
            headers.put("Server", "Java NIO Webserver by md_5");
            headers.put("Content-Type","text/html;charset=UTF-8");
            headers.put("Connection", "close");
            headers.put("Content-Length", Integer.toString(content.length));
        }
 
        public int getResponseCode() {
            return responseCode;
        }
 
        public String getResponseReason() {
            return responseReason;
        }
 
        public String getHeader(String header) {
            return headers.get(header);
        }
 
        public byte[] getContent() {
            return content;
        }
 
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }
 
        public void setResponseReason(String responseReason) {
            this.responseReason = responseReason;
        }
 
        public void setContent(byte[] content) {
            this.content = content;
        }
 
        public void setHeader(String key, String value) {
            headers.put(key, value);
        }

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

    }
}
