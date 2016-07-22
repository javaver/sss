package com.bbxpc.sss;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.bbxpc.consumer.Worker;
import com.bbxpc.main.ProcessHandler;

/**
 * Smart Super Server
 * 
 * @Title: SSS.java
 * @Package com.bbxpc.sss
 * @Description: TODO
 * @author yajie
 * @date 2016-7-19
 */
public class SSS {
	private static int BUFFER_SIZE = 1024;
	private static String CHARSET = "utf-8"; // 默认编码
	private CharsetDecoder decoder; // 解码
	
	private int port = 3838;
	private ServerSocketChannel channel;
	private Selector selector;
	private final ByteBuffer buffer;

	private Charset charset = Charset.forName(CHARSET); // 字符集
	private CharsetEncoder encoder = charset.newEncoder(); // 编码器
	
	static Logger log = Logger.getRootLogger();
	static ExecutorService executorService = Executors.newCachedThreadPool();
	
	private SSS(int port) throws IOException {
		super();
		this.port = port;
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.decoder = Charset.forName(CHARSET).newDecoder();
		this.selector = Selector.open(); // 打开选择器
	}

	/**
	 * @Title: SSS.java
	 * @Package com.bbxpc.sss
	 * @Description: TODO
	 * @author yajie
	 * @date 2016-7-19
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	            log.info("正在启动服务...");
	            SSS server = new SSS(3838);
	            server.listen();
	            Runtime.getRuntime().addShutdownHook(new Thread(){
	            	public void run() {
	            		executorService.shutdown();
	            	}
	            });
	}

	private void listen() throws IOException {
		//打开一个服务通道
        this.channel = ServerSocketChannel.open();
        //绑定服务端口
        ServerSocket socket = channel.socket();
        socket.bind(new InetSocketAddress(port));
        //使用非阻塞模式，使用多道io操作
        channel.configureBlocking(false);
        log.info("服务运行在端口:"+port);
        while (true) {
            //非阻塞，没有连接，立即返回null，与socket.accept()方法(阻塞)不同，
            SocketChannel client = channel.accept();
            if (client != null) {
                log.info("客户端口-->" + client.getRemoteAddress().toString());
                registerClient(client);
            }
            service();
        }
	}
	 /**
     * 将客户端channel注册到selector上
     * 四个事件：Connect、Accept、Read、Write
     *
     * @param client
     * @throws IOException
     */
    private void registerClient(SocketChannel client) throws IOException {
        //设置非阻塞
        client.configureBlocking(false);
        //将客户端channel注册到selector上
        client.register(selector, SelectionKey.OP_READ);//OP_READ
    }
    /**
     * 遍历各客户端通道
     * select()阻塞到至少有一个通道在你注册的事件上就绪了
     * select(long timeout) 多设置一个阻塞时间(毫秒)
     * selectNow() 不阻塞，有无都返回。
     */
    private void service() throws IOException {
        if (selector.selectNow() > 0) {
            //客户端channel的键集合
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                SocketChannel client = (SocketChannel) key.channel();
                if(key.isAcceptable()){
                	client.register(this.selector, SelectionKey.OP_READ);
                }else if (key.isReadable()) {
                	 client.register(this.selector, SelectionKey.OP_WRITE);
                    read(key);
                } else if (key.isWritable()) {
                    client.register(this.selector, SelectionKey.OP_READ);
                    write(key);
                }
            }
        }
    }
    //读信息
    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        int c = client.read(buffer);
        if (c > 0) {
            //flip方法将Buffer从写模式切换到读模式
            buffer.flip();
            CharBuffer charBuffer = decoder.decode(buffer);
            //接收请求
            String req=charBuffer.toString();
            String []array=req.split("\r\n\r\n");
            if(array.length<=2){
            	req=array[1];
            }
            log.info("客户端请求参数:"+req);
            if(req.startsWith("{")){
            	//executorService.execute(new Worker(req));
            	Worker.doWork(req);
            }
        } else {
            client.close();
        }
        buffer.clear();
    }
    //响应信息
    private void write(SelectionKey key) throws IOException {
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
    private void response( SocketChannel client,String body) throws IOException {
    	log.debug("返回参数："+body);
    	client.write(encoder.encode(CharBuffer.wrap(body)));
    	 client.close();
    }
}
