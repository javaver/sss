package com.bbxpc.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sword.lang.HttpUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
/**
 * 辅助查询工具
 * @author yajie
 *
 */
public class MyUtil {

	private static String TOPURL		=								"http://115.159.89.35:8080/"; //测试环境
	//private static String TOPURL="http://10.105.48.2:8080/";	//正式环境的接口
	private static String ORDER		=			TOPURL+	"bak/order/detail";		//查询订单详情接口
	private static String USERINFO	=			TOPURL+	"bak/user/getinfo";	//查询用户信息接口
	private static String TOKEN		=								"http://weixin.bbxpc.com/token";
	private static String YD_TOKEN		=								"http://yuedao.bbxhc.com/token";
	private static String SENDURL	=								"https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
	
	private static Logger log=Logger.getLogger(MyUtil.class);
	/**
	 * @param argsString url="http://115.159.89.35:8080/bak/order/detail";
	 */
	public static void main(String[] args) {
		System.out.println(getOrder("563510335229540668","145800641504253619659"));
	}
	/**
	 * 获取订单详情
	 * @param uid
	 * @param orderId
	 * @return
	 */
	public static String getOrder(String uid,String orderId){
		Map<String,String> dataMap=new HashMap<String,String>();
		dataMap.put("passenger_id", uid);
		dataMap.put("order_id", orderId);
		String data=JSON.toJSONString(dataMap);
		String ret=HttpUtils.post(ORDER, data);
		return ret;
	}
	/**
	 * 根据uid orderid获取价格
	 * @param uid
	 * @param orderId
	 * @return
	 */
	public static int getPrice(String uid,String orderId){
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		int order_status=msg.getIntValue("order_status");
		JSONObject price_detail=msg.getJSONObject("price_detail");
		int price=price_detail.getIntValue("actual_price");
		System.out.println("order_status:"+order_status+"你需要支付:"+price/100);
		if (price<=0){
			return 0;
		}else{
			return price/100;
		}
	}
	/**
	 * 根据uid获取openid
	 * @param uid
	 * @return
	 */
	public static String getOpenId(String uid){
		Map<String,String> dataMap=new HashMap<String,String>();
		dataMap.put("uid", uid);
		String data=JSON.toJSONString(dataMap);
		String ret=HttpUtils.post(USERINFO, data);
		if(null==ret || ret.length()<=1){
			return null;
		}
		log.info("获取用户信息:"+ret);
		JSONObject root=JSON.parseObject(ret);
		try{
			JSONObject info=root.getJSONObject("info");
		String openId=info.getString("open_id");
		log.info("获取用户信息:"+ret+"==>openId:"+openId);
		return openId;
		}catch(Exception ex){
			return null;
		}
		
	}
	/*public static void sendMsg(String openId,String msg){
		String token=HttpUtils.get(TOKEN);
		if(null!=token && token.length()>0){
			 Map<String,Object> data=new HashMap<String,Object>();
			 Map<String,String> text=new HashMap<String,String>();
			 data.put("touser", openId);
			 data.put("msgtype", "text");
			 text.put("content", msg);
			 data.put("text", text);
			 String json= JSON.toJSONString(data);
			 String ret=HttpUtils.post(SENDURL+token,json);
			 log.info("===============>发送结果:"+ret);
		}
	}*/
	public static void sendNewsYueDao(String openId,String title,String msg,String link){
		 
		JSONArray jsonArray = new JSONArray();
			JSONObject jsonItem = new JSONObject();
			jsonItem.put("title",title);
			jsonItem.put("description", msg);
			jsonItem.put("url", link);
			jsonItem.put("picurl", "");
			jsonArray.add(jsonItem);
			JSONObject jsonMsg = new JSONObject();
			jsonMsg.put("articles", jsonArray);
		
		JSONObject json = new JSONObject();
		json.put("touser", openId);
		json.put("msgtype","news");
		json.put("news", jsonMsg);
		String jsonStr=json.toJSONString();
		//获取token
		String token=HttpUtils.get(YD_TOKEN);
		log.debug("获取的token 是:"+token);
		if(null!=token && token.length()>0){
			 String ret=HttpUtils.post(SENDURL+token,jsonStr);
			 log.info("===============>发送结果:"+ret);
		}
		 
	}
	public static void sendNews(String openId,String title,String msg,String link){
	 
		JSONArray jsonArray = new JSONArray();
			JSONObject jsonItem = new JSONObject();
			jsonItem.put("title",title);
			jsonItem.put("description", msg);
			jsonItem.put("url", link);
			jsonItem.put("picurl", "");
			jsonArray.add(jsonItem);
			JSONObject jsonMsg = new JSONObject();
			jsonMsg.put("articles", jsonArray);
		
		JSONObject json = new JSONObject();
		json.put("touser", openId);
		json.put("msgtype","news");
		json.put("news", jsonMsg);
		String jsonStr=json.toJSONString();
		//获取token
		String token=HttpUtils.get(TOKEN);
		log.debug("获取的token 是:"+token);
		if(null!=token && token.length()>0){
			 String ret=HttpUtils.post(SENDURL+token,jsonStr);
			 log.info("===============>发送结果:"+ret);
		}
		/*{
		    "touser": "oB4-Tt2CEaXcJ66EAEh370oqD_hw", 
		    "msgtype": "news", 
		    "news": {
		        "articles": [
		            {
		                "title": "Happy Day", 
		                "description": "Is Really A Happy Day", 
		                "url": "http://www.baidu.com/", 
		                "picurl": ""
		            }
		        ]
		    }
		}*/
	}
	/**
	 * 发送订单新增事件
	 * @param openId
	 * @param orderId
	 */
	public static void sendOrderNews(String uid,String openId,String orderId){
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		JSONObject locations=msg.getJSONObject("locations");
		int count=locations.getIntValue("count");//出行人数
		JSONObject start=locations.getJSONObject("start");	//起始地
		JSONObject ends=locations.getJSONObject("end");	//目的地
		String startAddr=start.getString("address");
		String endAddr=ends.getString("address");
		int orderType=msg.getIntValue("order_type");//订单类型
		String appoint_time=msg.getString("appoint_time");	//出发时间
		String title="下单成功通知";
		StringBuilder desc=new StringBuilder();
		String tName=getTypeName(orderType);
		 desc.append("尊敬的客户，您的");
		 desc.append(tName);
		desc.append("出行计划下单成功");
		desc.append("\r\n");
		desc.append("订单号：");desc.append(orderId);
		desc.append("\r\n");
		if(orderType==0){
			desc.append("出行人数：");desc.append(count);
			desc.append("人\r\n");
		}
		desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
		desc.append("上车地点：");desc.append(startAddr);desc.append("\r\n");
		desc.append("下车地点：");desc.append(endAddr);
		desc.append("\r\n");
		//wxtest/
		String s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
		String enURL = null;
		try {
			enURL = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
		
		sendNews(openId,title,desc.toString(),link);
	}
	public static void sendChangePrice(String uid,String openId,String orderId){
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		JSONObject locations=msg.getJSONObject("locations");
		JSONObject start=locations.getJSONObject("start");	//起始地
		JSONObject ends=locations.getJSONObject("end");	//目的地
		String startAddr=start.getString("address");
		String endAddr=ends.getString("address");
		String appoint_time=msg.getString("appoint_time");	//出发时间
		String title="订单改价通知";
		StringBuilder desc=new StringBuilder();
		 desc.append("尊敬的客户，客服对您做了回访，并修改了您的订单价格，请及时登录查看，或致电客服。");
		desc.append("\r\n");
		desc.append("订单号：");desc.append(orderId);
		desc.append("\r\n");
		desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
		desc.append("上车地点：");desc.append(startAddr);desc.append("\r\n");
		desc.append("下车地点：");desc.append(endAddr);
		desc.append("\r\n");
		//wxtest/
		String s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
		String enURL = null;
		try {
			enURL = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=#wechat_redirect";
		
		sendNews(openId,title,desc.toString(),link);
	}
	public static void sendOrderChange(String uid,String openId,String orderId){
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		JSONObject locations=msg.getJSONObject("locations");
		JSONObject start=locations.getJSONObject("start");	//起始地
		JSONObject ends=locations.getJSONObject("end");	//目的地
		String startAddr=start.getString("address");
		String endAddr=ends.getString("address");
		String appoint_time=msg.getString("appoint_time");	//出发时间
		String title="订单改单通知";
		StringBuilder desc=new StringBuilder();
		 desc.append("尊敬的客户，客服对您做了回访，并修改了您的订单信息，请及时登录查看，或致电客服。");
		desc.append("\r\n");
		desc.append("订单号：");desc.append(orderId);
		desc.append("\r\n");
		desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
		desc.append("上车地点：");desc.append(startAddr);desc.append("\r\n");
		desc.append("下车地点：");desc.append(endAddr);
		desc.append("\r\n");
		//wxtest/
		String s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
		String enURL = null;
		try {
			enURL = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=#wechat_redirect";
		
		sendNews(openId,title,desc.toString(),link);
	}
	public static void sendYueDAOOrderNews(String uid,String openId,String orderId){
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		JSONObject locations=msg.getJSONObject("locations");
		int count=locations.getIntValue("count");//出行人数
		JSONObject start=locations.getJSONObject("start");	//起始地
		JSONObject ends=locations.getJSONObject("end");	//目的地
		String startAddr=start.getString("address");
		String endAddr=ends.getString("address");
		int orderType=msg.getIntValue("order_type");//订单类型
		String appoint_time=msg.getString("appoint_time");	//出发时间
		String title="下单成功通知";
		StringBuilder desc=new StringBuilder();
		String tName=getTypeName(orderType);
		 desc.append("尊敬的客户，您的");
		 desc.append(tName);
		desc.append("出行计划下单成功");
		desc.append("\r\n");
		desc.append("订单号：");desc.append(orderId);
		desc.append("\r\n");
		if(orderType==0){
			desc.append("出行人数：");desc.append(count);
			desc.append("人\r\n");
		}
		desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
		desc.append("上车地点：");desc.append(startAddr);desc.append("\r\n");
		desc.append("下车地点：");desc.append(endAddr);
		desc.append("\r\n");
		//wxtest/
		String s="http://yuedao.bbxhc.com/myorder?method=updateorder&orderid="+orderId;
		String enURL = null;
		try {
			enURL = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxbc865a872c0fa24d&redirect_uri=http://yuedao.bbxhc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
		sendNewsYueDao(openId,title,desc.toString(),link);
	}
	/**
	 * 发送高考订单新增事件
	 * @param openId
	 * @param orderId
	 */
	public static void sendGaoKaoOrderNews(String uid,String openId,String orderId){
		String ret=getOrder(uid, orderId);
		log.debug("高考直通车:"+ret);
		JSONObject root=JSON.parseObject(ret);
		JSONObject order=root.getJSONObject("message");
		String order_Msg=order.getString("order_message");//备注信息
		String start=order.getJSONObject("locations").getJSONObject("start").getString("address");	//起始地
		String ends=order.getJSONObject("locations").getJSONObject("end").getString("address");//目的地
		int count=order.getJSONObject("locations").getIntValue("count");//订单类型
		String title="下单成功通知";
		StringBuilder desc=new StringBuilder();
		 desc.append("尊敬的客户，您的爱心直通车预约成功！");
		desc.append("\r\n");
		desc.append("订单号：");desc.append(orderId);
		desc.append("\r\n");
		desc.append("备注信息：");desc.append(order_Msg);
		desc.append("\r\n");
		//desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
		desc.append("家庭地点：");desc.append(start);desc.append("\r\n");
		desc.append("考场地点：");desc.append(ends);
		desc.append("\r\n");
		desc.append("乘车人数：");desc.append(count);
		desc.append("\r\n");
		desc.append("帮邦行祝您高考顺利！\r\n");
		//wxtest/
		String s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
		String enURL = null;
		try {
			enURL = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
		
		sendNews(openId,title,desc.toString(),link);
	}
	/**
	 * 获取订单类型名称 // 0-合乘,
        1-包车,
        2-货,
        3-市内叫车
	 * @param orderType
	 * @return String
	 */
	static String getTypeName(int o){
		String name="";
		switch(o){
		case 0:
			name="城际合乘";
			break;
		case 1:
			name="城际包车";
			break;
		case 2:
			name="小件快递";
			break;
		case 3:
			name="城内打车";
			break;
		}
		return name;
	}
	/**
	 * 获取车牌号
	 * @param uid
	 * @param orderId
	 * @return
	 */
	public static String getCarNo(String uid, String orderId) {
		String ret=getOrder(uid, orderId);
		JSONObject order=JSON.parseObject(ret);
		JSONObject msg=order.getJSONObject("message");
		String car_NO=msg.getString("car_NO");
		return car_NO;
	}
}
