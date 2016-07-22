package com.bbxpc.consumer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.bbxpc.http.HttpsClient;
import com.bbxpc.http.MyUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;

/**
 * 消息处理
 * @author yajie
 *
 */
public class Worker implements Runnable{

	private static String SUBSCRIBE_URL="http://127.0.0.1/md/ssb";	//关注地址
	private static String ORDERLOG_URL="http://127.0.0.1/md/log";		//订单日志
	//private static String POOLLING_URL="http://weixin.bbxpc.com/pool";	//轮询正式地址
	private static String POOLLING_URL="http://wxtest.bbxpc.com/wxtest/pool";	//轮询测试地址
	static String data="data=";
	
	String jsonStr;
  public Worker(String jsonStr) {
		this.jsonStr = jsonStr;
	}
/**
   * 开始处理json
   * @param jsonStr
 * @throws UnsupportedEncodingException 
   */
  public static void doWork(String jsonStr) {
	  String openIdInJSON=null;
	  try{
    	  JSONObject json=JSON.parseObject(jsonStr);
    	  String dataStr=json.getString("data");
    	  JSONObject obj=JSON.parseObject(dataStr);
    	  String channel_id=obj.getString("channel_id");
    	  if(null!=channel_id && channel_id.contains("yuedao")){
    		  YueDaoWorker.process(jsonStr);
    		  return;
    	  }
          int type=json.getIntValue("type");
          String uid=json.getString("to");
          String openId="";
          System.out.println("--->事件类型type:"+type);
          StringBuilder sb=new StringBuilder();
          switch(type){
          case 1://派车通知
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  System.out.println("uid:"+uid);
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  String orderId=obj.getString("order_id");	//获取订单id
        	  String driver_name=obj.getString("driver_name");
        	  String car_No=obj.getString("car_NO");
        	  String title="派车通知";
        	  sb.append("已指派");
        	  sb.append(driver_name);
        	  sb.append("师傅 ");
        	  sb.append("车牌号");
        	  sb.append(car_No);
        	  sb.append("为您服务，祝您用车愉快");
        	  String s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
      		String enURL = null;
      		try {
      			enURL = URLEncoder.encode(s, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		String link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
        	  MyUtil.sendNews(openId, title, sb.toString(), link);
        	  System.out.println("派车通知:"+sb.toString());
        	  
        	  break;
          case 20://客服取消
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  openIdInJSON=obj.getString("open_id");	//微信号在消息中
        	  String timeStr=json.getString("time");	//时间戳
        	  int order_status=obj.getIntValue("order_status");	//客服取消
        	  String reason=obj.getString("reason");
        	  Object order_origin_obj=obj.get("order_origin");
        	  int order_origin_val=-1;
        	  if(null!=order_origin_obj && order_origin_obj.toString().length()>0){
        		  order_origin_val=Integer.valueOf(order_origin_obj.toString());
        	  }
        	  if(order_status==23){//如果是客服取消的
        		 title="订单取消通知";
        		 if(order_origin_val==99){
        			  sb.append("您已取消行程，帮邦行预祝您高考顺利！"); 
        		 }else{
        			 sb.append("由于 ");
               	  sb.append(reason+"，客服已取消此订单，欢迎下次使用帮邦行");
        		 }
            	  s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
            	  enURL="";
          		try {
          			enURL = URLEncoder.encode(s, "UTF-8");
          		} catch (UnsupportedEncodingException e) {
          			e.printStackTrace();
          		}
          		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
          		
            	  MyUtil.sendNews(openId, title, sb.toString(), link);
            	  System.out.println("客服取消:"+sb.toString());
            	  if(null ==openIdInJSON || openIdInJSON.length()<=4){//如果openid为空或者是null则返回不处理
            		  return;
            	  }
            	  //记录取消事件
            	   StringBuilder sendX=new StringBuilder();
            	  sendX.append(uid);
            	  sendX.append("\t");
            	  sendX.append(openIdInJSON);
            	  sendX.append("\t");
            	  sendX.append(orderId);
            	  sendX.append("\t");
            	  sendX.append(timeStr);
            	  sendX.append("\t");
            	  sendX.append("2");	//取消
              	 System.out.println("取消事件类型type:"+type+"\tuid:"+uid +" orderId:"+orderId+" timeStr:"+new Timestamp(Long.valueOf(timeStr))+"openid:"+openIdInJSON);
                    try {
						HttpsClient.sendPost(ORDERLOG_URL, data+URLEncoder.encode(sendX.toString(),"UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
        	  }
        	  break;
          case 3://司机改派
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  order_status=obj.getIntValue("order_status");	//客服改派
        	  reason=obj.getString("reason");
        	  if(order_status==21){
        		  title="订单改派通知";
        		  String car_NO=MyUtil.getCarNo(uid,orderId);
        		  if("位置不合适当前司机".equalsIgnoreCase(reason)){
        			  
        			  sb.append("很抱歉， ");
        			  sb.append(car_NO);
        			  sb.append("距您较远，为了您的出行更快捷，马上为您改派最近的车辆");
        		  }else if("方位不符".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉， ");
        			  sb.append(car_NO);
        			  sb.append("距您较远，为了您的出行更快捷，马上为您改派最近的车辆");
        		  }else if("拼改包".equalsIgnoreCase(reason)){
        			  sb.append("尊敬的客户，您修改了订单类型，请稍等，小邦马上为您改派车辆。");
        		  }else if("司机设备故障".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于司机设备故障，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("联系不上司机".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("碌中无法接通，小邦马上为您改派车辆，请稍等。");
        		  }else if("司机超时没接上客户".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("因故无法及时赶到，小邦马上为您改派车辆，请稍等。");
        		  }else if("无法联系".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("联系不上您，现为您改派车辆，请保持电话畅通。");
        		  }else if("坐了别人的车".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于计划有变，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("暂时不走".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于计划有变，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("人数不符".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于人数不符，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("车辆故障".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于车辆故障，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }
        		  
            	  s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
            	  enURL="";
          		try {
          			enURL = URLEncoder.encode(s, "UTF-8");
          		} catch (UnsupportedEncodingException e) {
          			e.printStackTrace();
          		}
          		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
            	  MyUtil.sendNews(openId, title, sb.toString(), link);
            	  System.out.println("客服改派:"+sb.toString());
        	  }
        	  break;
          case 24://客服改派
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  order_status=obj.getIntValue("order_status");	//客服改派
        	  reason=obj.getString("reason");
        	  if(order_status==24){
        		  title="订单改派通知";
        		  String car_NO=MyUtil.getCarNo(uid,orderId);
        		  if("位置不合适当前司机".equalsIgnoreCase(reason)){
        			  
        			  sb.append("很抱歉， ");
        			  sb.append(car_NO);
        			  sb.append("距您较远，为了您的出行更快捷，马上为您改派最近的车辆");
        		  }else if("方位不符".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉， ");
        			  sb.append(car_NO);
        			  sb.append("距您较远，为了您的出行更快捷，马上为您改派最近的车辆");
        		  }else if("拼改包".equalsIgnoreCase(reason)){
        			  sb.append("尊敬的客户，您修改了订单类型，请稍等，小邦马上为您改派车辆。");
        		  }else if("司机设备故障".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于司机设备故障，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("联系不上司机".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("碌中无法接通，小邦马上为您改派车辆，请稍等。");
        		  }else if("司机超时没接上客户".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("因故无法及时赶到，小邦马上为您改派车辆，请稍等。");
        		  }else if("无法联系".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，");
        			  sb.append(car_NO);
        			  sb.append("联系不上您，现为您改派车辆，请保持电话畅通。");
        		  }else if("坐了别人的车".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于计划有变，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("暂时不走".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于计划有变，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("人数不符".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于人数不符，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }else if("车辆故障".equalsIgnoreCase(reason)){
        			  sb.append("很抱歉，由于车辆故障，");
        			  sb.append(car_NO);
        			  sb.append("无法去接您，小邦马上为您改派车辆。");
        		  }
        		  
            	  s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
            	  enURL="";
          		try {
          			enURL = URLEncoder.encode(s, "UTF-8");
          		} catch (UnsupportedEncodingException e) {
          			e.printStackTrace();
          		}
          		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
            	  MyUtil.sendNews(openId, title, sb.toString(), link);
            	  System.out.println("客服改派:"+sb.toString());
        	  }
        	  break;
          case 30://后台新增
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  String from=json.getString("from");
        	  if(!"php".equalsIgnoreCase(from)){	//判断是否是后台 下单
        		  return;
        	  }
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  
        	  dataStr=json.getString("data");
        	  timeStr=json.getString("time");	//时间戳
        	  obj=JSON.parseObject(dataStr);
        	  openIdInJSON=obj.getString("open_id");	//微信号在消息中
        	  orderId=obj.getString("order_id");	//获取订单id
        	  //高考直通车判断
        	  order_origin_obj=obj.get("order_origin");
        	  order_origin_val=-1;
        	  if(null!=order_origin_obj){
        		  order_origin_val=Integer.valueOf(order_origin_obj.toString());
        	  }
        	  if(order_origin_val==99){
    			  MyUtil.sendGaoKaoOrderNews(uid, openId, orderId);
    		 }else{
    			 MyUtil.sendOrderNews(uid, openId, orderId);
    		 }
        	  
        	  //记录新增订单
        	  if(null !=openIdInJSON && openIdInJSON.length()>4){//如果openid不为空或者是null则 处理
        		  StringBuilder sendX=new StringBuilder();
            	  sendX.append(uid);
            	  sendX.append("\t");
            	  sendX.append(openId);
            	  sendX.append("\t");
            	  sendX.append(orderId);
            	  sendX.append("\t");
            	  sendX.append(timeStr);
            	  sendX.append("\t");
            	  sendX.append("1");		//新增事件
              	 System.out.println("新增事件类型type:"+type+"\tuid:"+uid+" from:"+from+" orderId:"+orderId+" timeStr:"+new Timestamp(Long.valueOf(timeStr))+"openid:"+openId);
    			try {
    				HttpsClient.sendPost(ORDERLOG_URL, data+URLEncoder.encode(sendX.toString(),"UTF-8"));
    			} catch (UnsupportedEncodingException e) {
    				e.printStackTrace();
    			}
    			System.out.println(sendX.toString());
        	  } 
        	  break;
          case 40:	//新增优惠券事件
        	  sb.setLength(0);
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  System.out.println("----------------->uid:"+uid+"=========>openId:"+openId);
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  System.out.println("优惠券:"+json.toJSONString());
        	  JSONArray array=obj.getJSONArray("list");	//遍历优惠券
        	  if(null==array || array.size()<=0){
        		  return;
        	  }
        	  System.out.println(openId+"接收到新增优惠券消息："+array.toString());
        	  title="优惠券入账通知";
        	  sb.append(array.size()+"张优惠券已放入钱包，快去查收吧~ \r\n");
        	  sb.append("用最优惠的价格，享受最优质的服务\r\nO(∩_∩)O");
        	  s="http://weixin.bbxpc.com/mycoupon.jsp?a=1";
        	  enURL="";
      		try {
      			enURL = URLEncoder.encode(s, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
        	  MyUtil.sendNews(openId, title, sb.toString(), link);
        	  try {
  				HttpsClient.sendPost(POOLLING_URL, data+URLEncoder.encode(array.toString(),"UTF-8")+"&openid="+openId);
  			} catch (UnsupportedEncodingException e) {
  				e.printStackTrace();
  			}
        	  break;
          case 60:	//优惠券超时提醒
          //{"cmd":1,"data":"{\"cnt\":\"3\"}","expire":"2592000000","from":"tcapi","id":"133636","sn":"d628ee37-3bc3446c-bdaed3a5-91856e9b","time":"1464256120000","to":"4549618892100697045","type":50}
        	  sb.setLength(0);
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  System.out.println("优惠券:"+json.toJSONString());
        	  int cnt=obj.getIntValue("cnt");	//遍历优惠券
        	 
        	  System.out.println(openId+"接收到新增优惠券消息："+cnt);
        	  title="优惠券即将过期";
        	  sb.append("您有"+cnt+"张优惠券将于2天后过期，请尽快使用 \r\n");
        	  s="http://weixin.bbxpc.com/mycoupon.jsp?a=1";
        	  enURL="";
      		try {
      			enURL = URLEncoder.encode(s, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
        	  MyUtil.sendNews(openId, title, sb.toString(), link);
        	  break;
          /*case 66://高考直通车
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  from=json.getString("from");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  openIdInJSON=openId;
        	  dataStr=json.getString("data");
        	  timeStr=json.getString("time");									//时间戳
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");								//获取订单id
        	  sendGaoKaoOrderNews();
      		String order_Msg=obj.getString("order_message");	//备注信息
      		String start=obj.getString("start_address");				//起始地
      		String ends=obj.getString("end_address");				//目的地
      		int count=obj.getIntValue("count");							//订单类型
      		title="下单成功通知";
      		StringBuilder desc=new StringBuilder();
      		 desc.append("尊敬的客户，您的爱心直通车预约成功！");
      		desc.append("\r\n");
      		desc.append("订单号：");desc.append(orderId);
      		desc.append("\r\n");
      		desc.append("备注信息：");desc.append(order_Msg);
      		desc.append("人\r\n");
      		//desc.append("出发时间：");desc.append(appoint_time);desc.append("\r\n");
      		desc.append("家庭地点：");desc.append(start);desc.append("\r\n");
      		desc.append("考场地点：");desc.append(ends);
      		desc.append("\r\n");
      		desc.append("乘车人数：");desc.append(count);
      		desc.append("\r\n");
      		//wxtest/
      		String ss="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
      		enURL = null;
      		try {
      			enURL = URLEncoder.encode(ss, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
      		
      		MyUtil.sendNews(openId,title,desc.toString(),link);
        	  //记录新增订单
        	  if(null !=openIdInJSON && openIdInJSON.length()>4){//如果openid不为空或者是null则 处理
        		  StringBuilder sendX=new StringBuilder();
            	  sendX.append(uid);
            	  sendX.append("\t");
            	  sendX.append(openId);
            	  sendX.append("\t");
            	  sendX.append(orderId);
            	  sendX.append("\t");
            	  sendX.append(timeStr);
            	  sendX.append("\t");
            	  sendX.append("1");		//新增事件
              	 System.out.println("新增事件类型type:"+type+"\tuid:"+uid+" from:"+from+" orderId:"+orderId+" timeStr:"+new Timestamp(Long.valueOf(timeStr))+"openid:"+openId);
    			try {
    				HttpsClient.sendPost(ORDERLOG_URL, data+URLEncoder.encode(sendX.toString(),"UTF-8"));
    			} catch (UnsupportedEncodingException e) {
    				e.printStackTrace();
    			}
    			System.out.println(sendX.toString());
        	  } 
        	  break;*/
          /*case 5:	//上车通知
        	  sb.setLength(0);
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  order_status=obj.getIntValue("order_status");	//上车
        	  if(order_status==4){
        		  title="上车通知";
            	  sb.append("行程开始，祝您出行愉快");
            	  link="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
            	  MyUtil.sendNews(openId, title, sb.toString(), link);
            	  System.out.println("上车通知:"+sb.toString());
        	  }
        	  break;
          case 6://	已下车
        	  sb.setLength(0);
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  orderId=obj.getString("order_id");
        	  order_status=obj.getIntValue("order_status");	//已下车
        	  if(order_status==10){
        		  title="待支付通知";
            	  sb.append("您的行程已结束，还需支付");
            	  sb.append(MyUtil.getPrice(uid, orderId));
            	  sb.append("元，请支付费用");
            	  link="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
            	  MyUtil.sendNews(openId, title, sb.toString(), link);
            	  System.out.println("已下车:"+sb.toString());
        	  }
        	  break;*/
          case 84://改价通知
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  timeStr=json.getString("time");	//时间戳
        	  MyUtil.sendChangePrice(uid, openId, orderId);
        	  break;
          case 83://改单通知
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  return;
        	  }
        	  timeStr=json.getString("time");	//时间戳
        	  MyUtil.sendOrderChange(uid, openId, orderId);
        	  break;
          case 25:	//订单超时通知
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  break;
        	  }
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	//  String text=obj.getString("text");
        	  title="订单超时通知";
        	  sb.append("您的订单"+orderId+"已超时，请点击查看");
        	  s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
        	  enURL="";
      		try {
      			enURL = URLEncoder.encode(s, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=#wechat_redirect";
        	  MyUtil.sendNews(openId, title, sb.toString(), link);
        	  System.out.println("订单超时:"+sb.toString());
        	  break;
          case 8:	//订单超时
        	  sb.setLength(0);
        	  uid=json.getString("to");
        	  openId=MyUtil.getOpenId(uid);	//判断是否是微信用户
        	  if(null==openId || openId.length()<=0){
        		  break;
        	  }
        	  dataStr=json.getString("data");
        	  obj=JSON.parseObject(dataStr);
        	  orderId=obj.getString("order_id");	//获取订单id
        	  String text=obj.getString("text");
        	  title="订单超时通知";
        	  sb.append(text);
        	  s="http://weixin.bbxpc.com/myorder?method=updateorder&orderid="+orderId;
        	  enURL="";
      		try {
      			enURL = URLEncoder.encode(s, "UTF-8");
      		} catch (UnsupportedEncodingException e) {
      			e.printStackTrace();
      		}
      		link="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxd884bd4965e1eb2f&redirect_uri=http://weixin.bbxpc.com/code?page="+enURL+"&response_type=code&scope=snsapi_base&state=#wechat_redirect";
        	  MyUtil.sendNews(openId, title, sb.toString(), link);
        	  System.out.println("订单超时:"+sb.toString());
        	  break;
          }
         
      }catch(JSONException e){
    	  e.printStackTrace();
      }
  }
public void run() {
	doWork(jsonStr);
	
}
}