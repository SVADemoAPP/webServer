/**   
 * @Title: AmqpThread.java 
 * @Package com.sva.service 
 * @Description: 执行java版amqp对接线程
 * @author labelCS   
 * @date 2016年8月31日 下午4:46:31 
 * @version V1.0   
 */
package com.sva.service;

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.qpid.QpidException;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.AMQConnection;
import org.apache.qpid.url.URLSyntaxException;
import com.sva.common.ConvertUtil;
import com.sva.common.MyLog;
import com.sva.dao.AmqpDao;
import com.sva.model.LocationModel;
import com.sva.model.SvaModel;
import com.sva.web.controllers.AccountController;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/** 
 * @ClassName: AmqpThread 
 * @Description: 执行java版amqp对接线程 
 * @author labelCS 
 * @date 2016年8月31日 下午4:46:31 
 *  
 */
public class AmqpThread extends Thread {

    /** 
     * @Fields log : 日志处理类 
     */ 
    private static final Logger LOG = Logger.getLogger(AmqpThread.class);
    
    private static final MyLog mylog = AccountController.mylog;
    /** 
     * @Fields sva : sva信息 
     */ 
    private SvaModel sva;
    /** 
     * @Fields dao : 数据库处理句柄 
     */ 
    private AmqpDao dao;
    /** 
     * @Fields queueId : 队列id
     */ 
    private String queueId;
    /** 
     * @Fields isStop : 是否停止线程标志 
     */ 
    private boolean isStop = false;
    private AMQConnection conn;
    
    /** 
     * <p>Title: </p> 
     * <p>Description: 构造函数</p> 
     * @param sm
     * @param dao
     * @param queue 
     */
    public AmqpThread(SvaModel sm, AmqpDao dao, String queue){
        this.sva = sm;
        this.dao = dao;
        this.queueId = queue;
    }
    
    /** 
     * @Title: stopThread 
     * @Description: 通过改变标志位，停止线程
     */
    public void stopThread()
    {
        this.isStop = true;
    }
    
    /* (非 Javadoc) 
     * <p>Title: run</p> 
     * <p>Description: 实现sva数据订阅</p>  
     * @see java.lang.Thread#run() 
     */
    public void run()
    {        
        String ip = sva.getIp();
        String id = sva.getId();
        String userId = sva.getUsername();
        String port = sva.getBrokerPort();
        LOG.debug("amqp started:"
                + "userId:" + userId 
                + ",queueId:" + queueId
                + ",ip:" + ip
                + ",port:" + port
                + ",id:" + id);
        mylog.location("amqp started:"
                + "userId:" + userId 
                + ",queueId:" + queueId
                + ",ip:" + ip
                + ",port:" + port
                + ",id:" + id);
        // 获取keystore的路径
//        String path = getClass().getResource("/").getPath();
        String path = System.getProperty("user.dir");
        path = path.substring(0, path.indexOf("bin"))+"webapps/SVAProject/WEB-INF";
//        path = path.substring(1, path.indexOf("/classes"));
        path = path + File.separator + "java_keystore" + File.separator;
        LOG.debug("get keystore path:"+path);
        mylog.location("get keystore path:"+path);
        // 设置系统环境jvm参数
        System.setProperty("javax.net.ssl.keyStore", path + "keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "importkey");
        System.setProperty("javax.net.ssl.trustStore", path + "mykeystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "importkey");

        // 地址变量  
        String brokerOpts = "?brokerlist='tcp://"+ip+":"+port+"?ssl='true'&ssl_verify_hostname='false''";
        String connectionString = "amqp://"+userId+":"+"xxxx@xxxx/"+brokerOpts;
        LOG.debug("connection string:" + connectionString);
        mylog.location("connection string:" + connectionString);
        // 建立连接
        try {
            conn = new AMQConnection(connectionString);
            conn.start();
            LOG.debug("connection started!");
            mylog.location("connection started!");
            // 获取session
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            LOG.debug("session created!");
            mylog.location("session created!");
            // 获取队列
            Destination queue = new AMQAnyDestination("ADDR:"+queueId+";{create:sender}");
            MessageConsumer consumer = session.createConsumer(queue);
            LOG.debug("consumer created!");
            mylog.location("consumer created!");
            
            while(!isStop  && !this.isInterrupted())
            {
                Message m = consumer.receive(60000);
                // message为空的情况,
                if(m == null){
                    LOG.debug("Get NULL message, pause for 1 miniute!");
                    mylog.location("Get NULL message, pause for 1 miniute!");
//                    sleep(60000);
                    continue;
                }
                // message格式是否正确
                if(m instanceof BytesMessage){
                    BytesMessage tm = (BytesMessage)m;
                    int length = new Long(tm.getBodyLength()).intValue();
                    if(length > 0){
                        byte[] keyBytes = new byte[length];
                        tm.readBytes(keyBytes);
                        String messages = new String(keyBytes);
                        LOG.debug("SVA Data:"+messages);
                        saveSvaData(messages, Integer.parseInt(sva.getStoreId()), sva.getType(), sva.getIp(), sva.getId());
                        mylog.other("SVA Data:"+messages);
                    }else{
                        LOG.debug("Get zero length message");
                        mylog.location("Get zero length message");
                    }
                }else{
                    LOG.warn("Message is not in Byte format!");
                    mylog.location("Message is not in Byte format!");
                }
            }
        } catch (URLSyntaxException e) {
            LOG.error(e);
        } catch (QpidException e) {
            LOG.error(e);
        } catch (JMSException e) {
            LOG.error(e);
        } catch (URISyntaxException e) {
            LOG.error(e);
        } catch (Exception e){
            LOG.error(e);
        } finally{
            try {
                if(conn != null)
                {
                    conn.close();
                }
            } catch (JMSException e) {
                LOG.error(e);
            }
            mylog.location("[AMQP]No data from SVA,connection closed!");
            LOG.error("[AMQP]No data from SVA,connection closed!");
        }
    }
    
    /**   
     * @Title: saveSvaData   
     * @Description: 将从sva获取的数据解析并保存到数据库   
     * @param jsonStr：待解析的字符串      
     * @param storeId: 商场id
     * @param type: 订阅类型
     * @throws   
     */ 
    private void saveSvaData(String jsonStr, int storeId, int type, String ip, String svaId){
        if(StringUtils.isEmpty(jsonStr)){
            LOG.warn("No data from SVA!");
            mylog.location("No data from SVA!");
        }else{
            JSONObject result = JSONObject.fromObject(jsonStr);
            // 非匿名化订阅
            if(result.containsKey("locationstream")){
                saveLocationstream(result, storeId, type, svaId);
            }
            // 匿名化订阅
            else if(result.containsKey("locationstreamanonymous")){
                saveLocationstreamAnonymous(result, storeId, svaId);
            }
            // 网络信息订阅
            else if(result.containsKey("networkinfo")){
                saveNetworkinfo(result, ip);
            }
            // 电子围栏订阅
            else if(result.containsKey("geofencing")){
                saveGeofencing(result);
            }else{
                LOG.warn("Unknow message from SVA!");
                mylog.location("Unknow message from SVA!");
            }
        }
    }
    
    /** 
     * @Title: svaLocationstream 
     * @Description: 非匿名化位置信息入库逻辑
     * @param result
     * @param storeId
     * @param type 
     * @param svaId
     */
    private void saveLocationstream(JSONObject result, int storeId, int type, String svaId)
    {
        JSONArray list = result.getJSONArray("locationstream");
        for(int i = 0; i<list.size();i++){
            LocationModel lm = new LocationModel();
            JSONObject loc = list.getJSONObject(i);
            if(!parseLocation(loc, storeId, lm, svaId)){
                continue;
            }
            // 全量订阅
            if(type == 0){
                // 获取当天的日期字符串
                String tableName = "location" + ConvertUtil.dateFormat(loc.getLong("Timestamp"), "yyyyMMdd");
                // 执行数据库保存逻辑
                dao.saveAmqpData(lm,tableName);
                
            }
            // 指定用户订阅
            else
            {
                //检查该用户是否已经存在
                int count = dao.checkPhoneIsExisted(lm.getUserID());
                if(count > 0){
                    dao.updatePhoneLocation(lm);
                }else{
                    dao.saveAmqpData(lm, "locationphone");
                }
            }
        }
    }

    /** 
     * @Title: saveLocationstreamAnonymous 
     * @Description: 匿名化订阅位置信息入库
     * @param result
     * @param storeId 
     * @param svaId
     */
    private void saveLocationstreamAnonymous(JSONObject result, int storeId, String svaId)
    {
        JSONArray list = result.getJSONArray("locationstreamanonymous");
        
        for(int i = 0; i<list.size();i++){
            LocationModel lm = new LocationModel();
            JSONObject loc = list.getJSONObject(i);
            if(!parseLocation(loc, storeId, lm, svaId)){
                continue;
            }
            
            // 获取当天的日期字符串
            String tableName = "location" + ConvertUtil.dateFormat(loc.getLong("Timestamp"), "yyyyMMdd");
            // 执行数据库保存逻辑
            dao.saveAmqpData(lm,tableName);
            
        }
    }
    
    /** 
     * @Title: saveNetworkinfo 
     * @Description: 网络信息入库
     * @param result
     * @param ip 
     */
    private void saveNetworkinfo(JSONObject result,String ip)
    {
        JSONObject jsonAll = result.getJSONArray("networkinfo").getJSONObject(0);
        String userid = correctUserId(jsonAll.getString("userid"));
        String enbid = jsonAll.getJSONObject("lampsiteinfo").getString("enbid");
        JSONArray jsonList = jsonAll.getJSONObject("lampsiteinfo").getJSONArray("prrusignal");
        long timestamp = jsonAll.getLong("timestamp");
        long localTimes = System.currentTimeMillis();
        for(int i = 0; i<jsonList.size(); i++){
            JSONObject temp = jsonList.getJSONObject(i);
            String gpp = temp.getString("gpp");
            String rsrp = temp.getString("rsrp");
            mylog.prru("prru data: enbid:"+enbid+" userid:"+userid+" gpp"+gpp+" rsrp:"+rsrp);
            dao.svaPrru(enbid,userid,gpp,rsrp,ip,timestamp);
        }
    }
    
    /** 
     * @Title: saveGeofencing 
     * @Description: 电子围栏信息入库
     * @param result 
     */
    private void saveGeofencing(JSONObject result)
    {
        JSONArray jsonList = result.getJSONArray("geofencing");
        for(int i = 0; i<jsonList.size(); i++){
            JSONObject temp = jsonList.getJSONObject(i);
            // 用户存在多个的情况，目前只取第一个；若用户为空则不作处理
            JSONArray useridList = temp.getJSONArray("userid");
            String userid = "";
            if(useridList.size()>0){
                userid = useridList.getString(0);
            }else{
                continue;
            }
            // 当前时间
            long timeLocal = System.currentTimeMillis();
            
            dao.svaGeofencing(temp.getString("IdType")
                    ,userid
                    ,temp.getString("mapid")
                    ,temp.getString("zoneid")
                    ,temp.getString("zone_event")
                    ,temp.getString("Timestamp")
                    ,timeLocal);
        }
    }
    
    /**   
     * @Title: parseLocation   
     * @Description: 将json数据转换为LocationModel  
     * @param loc
     * @param storeId
     * @param lm
     * @param svaId
     * @return：boolean       
     * @throws   
     */ 
    private boolean parseLocation(JSONObject loc, int storeId, LocationModel lm, String svaId){
        // 当前时间戳
        long timeLocal = System.currentTimeMillis();
        lm.setTimestamp(new BigDecimal(timeLocal));
        // 设置LocationModel
        if(loc.containsKey("location")){
            JSONObject location = loc.getJSONObject("location");
            lm.setX(BigDecimal.valueOf(location.getInt("x")));
            lm.setY(BigDecimal.valueOf(location.getInt("y")));
            int z = location.getInt("z");
            // 楼层号转换
            if(z > 0){
                z += storeId*10000;
            }else{
                z = Math.abs(z) + 5000 + storeId*10000;
            }
            lm.setZ(BigDecimal.valueOf(z));
        }
        JSONArray useridList = loc.getJSONArray("userid");
        // 用户存在多个的情况，目前只取第一个；若用户为空则不作处理
        if(useridList.size()>0){
            lm.setUserID(correctUserId(useridList.getString(0)));
        }else{
            return false;
        }
        lm.setSvaId(svaId);
        lm.setIdType(loc.getString("IdType"));
        lm.setTimestamp(BigDecimal.valueOf(System.currentTimeMillis()));
        lm.setTimestampSva(BigDecimal.valueOf(loc.getLong("Timestamp")));
        lm.setDataType(loc.getString("datatype"));
        
        return true;
    }
    
    /** 
     * @Title: correctUserId 
     * @Description: 临时修正从sva上报的错误userId 
     * @param wrongId
     * @return 
     */
    private String correctUserId(String wrongId){
        String rightId = wrongId;
        if(wrongId.length() == 7){
            rightId = "0" + rightId;
        }
        return rightId;
    }
}
