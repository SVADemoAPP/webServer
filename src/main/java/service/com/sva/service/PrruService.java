/**   
 * @Title: PrruService.java 
 * @Package com.sva.service 
 * @Description: Prru服务类 
 * @author labelCS   
 * @date 2016年9月27日 上午11:29:33 
 * @version V1.0   
 */
package com.sva.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.sva.common.ConvertUtil;
import com.sva.common.MyLog;
import com.sva.common.conf.GlobalConf;
import com.sva.dao.PrruSignalDao;
import com.sva.model.LocationModel;
import com.sva.model.PrruFeatureDetailModel;
import com.sva.model.PrruFeatureModel;
import com.sva.model.PrruSignalModel;
import com.sva.web.models.PhoneSignalModel;
import com.sva.web.models.PrruFeatureApiModel;
import com.sva.web.controllers.AccountController;

/** 
 * @ClassName: PrruService 
 * @Description: Prru服务类
 * @author labelCS 
 * @date 2016年9月27日 上午11:29:33 
 *  
 */
@Service("prruService")
public class PrruService {
    /**
     * @Fields log 输出日志
     */
    private static final Logger LOG = Logger.getLogger(PrruService.class);
    
    private static final MyLog mylog = AccountController.mylog;
    
    /** 
     * @Fields DEFAULT_VALUE : 计算特征值时的默认值
     */ 
    private static final BigDecimal DEFAULT_VALUE = new BigDecimal(-1400);
    
    /** 
     * @Fields THRESHOLD : 特征值的阈值
     */ 
    private static final BigDecimal THRESHOLD = new BigDecimal(-1300);
    
    /** 
     * @Fields MULTIPLE10 : 倍数10
     */ 
    private static final BigDecimal MULTIPLE10 = new BigDecimal(10);
    
    /** 
     * @Fields LTE_MIX_3_1 : 0.2
     */ 
    private static final BigDecimal LTE_MIX_3_1 = new BigDecimal(0.2);
    /** 
     * @Fields WIFI_MIX_3_1 : 0.4
     */ 
    private static final BigDecimal WIFI_MIX_3_1 = new BigDecimal(0.4);
    /** 
     * @Fields BLUE_MIX_3_1 : 0.4
     */ 
    private static final BigDecimal BLUE_MIX_3_1 = new BigDecimal(0.4);
    /** 
     * @Fields LTE_WIFI_2_1 : 0.3
     */ 
    private static final BigDecimal LTE_WIFI_2_1 = new BigDecimal(0.3);
    /** 
     * @Fields WIFI_LTE_2_1 : 0.7
     */ 
    private static final BigDecimal WIFI_LTE_2_1 = new BigDecimal(0.7);
    /** 
     * @Fields LTE_BLUE_2_1 : 0.3
     */ 
    private static final BigDecimal LTE_BLUE_2_1 = new BigDecimal(0.3);
    /** 
     * @Fields BLUE_LTE_2_1 : 0.7
     */ 
    private static final BigDecimal BLUE_LTE_2_1 = new BigDecimal(0.7);
    
    /** 
     * @Fields WIFI_BLUE_2_1 : 0.5
     */ 
    private static final BigDecimal WIFI_BLUE_2_1 = new BigDecimal(0.5);
    /** 
     * @Fields BLUE_WIFI_2_1 : 0.5
     */ 
    private static final BigDecimal BLUE_WIFI_2_1 = new BigDecimal(0.5);
    
    /** 
     * @Fields LTEDelay : 5s
     */ 
    private static final long LteDelay = 10L;
    
    /** 
     * @Fields LTERatio : 0.7
     */ 
    private static final double LteRatio = 0.7;
    /** 
     * @Fields WifiRatio : 0.5
     */ 
    private static final double WifiRatio = 0.5;
    /** 
     * @Fields BlueRatio : 0.5
     */ 
    private static final double BlueRatio = 0.5;
    
    /** 
     * @Fields TOP_K : 3
     */ 
    private static final int TOP_K = 3;
    
    /** 
     * @Fields TOP_N : 20
     */ 
    private static final int TOP_N = 20;
    
    /** 
     * @Fields prruSignalDao : prruSignal数据库操作DAO
     */ 
    @Autowired
    private PrruSignalDao prruSignalDao;
    
    @Value("${sva.prruRatio}")
    private String ratio;
    
    @Value("${sva.correction}")
    private String correction;
    
    @Value("${sva.prruDistance}")
    private String prruDistance;
    
    @Value("${prru.disArray}")
    private String disArrayStr;
    
    @Value("${prru.ratioArray}")
    private String ratioArrayStr;
    
    @Value("${prru.gppCount}")
    private String gppCount;
    
    @Value("${blue.count}")
    private String blueCount;
    
    /** 
     * @Title: collectFeatureValue 
     * @Description: 采集特征库
     * @param model
     * @return 
     */
    public Map<String, Object> collectFeatureValue(PrruFeatureApiModel model){
        // 返回值
        Map<String, Object> result = new HashMap<String, Object>();
        // 检查该点是否已采集过有效的特征库
        PrruFeatureModel feature = null;
        if(model.getType()==1){
        	feature = prruSignalDao.getFeatureByPosition(model.getX(), model.getY(), model.getFloorNo());
        }
        //BigDecimal radius = feature.getFeatureRadius();
        if(feature != null && feature.getFeatureRadius() != null){
            // 采集完成，且结果有效，禁止重复采集
            result.put("error", "该位置已有特征库，请勿重复采集！");
        }else{
            CollectThread thread = new CollectThread(model, prruSignalDao);
            thread.start();
            result.put("data", "采集进行中，请稍后");
        }
        return result;
    }
    
    /** 
     * @Title: savePhoneSignal 
     * @Description: 存储手机测量信号
     * @param jsonStr: {
						"userid":String,
						"signal":[{"gpp":String,"rsrp":String,"type":String},...],
						}
     * @return 
     */
    public Map<String, Object> savePhoneSignal(PhoneSignalModel jsonStr){
        // 返回值
        Map<String, Object> result = new HashMap<String, Object>();
        String userid = jsonStr.getUserId();
        long localTimes = System.currentTimeMillis();
    	String signalJson = jsonStr.getSignalWIFI();
    	String signalBlueJson = jsonStr.getSignalBlue();
    	if(!StringUtils.isEmpty(signalJson)){
    		JSONArray signalArray = JSONArray.fromObject(signalJson);
            for(int i = 0; i<signalArray.size(); i++){
            	JSONObject phoneSignal = signalArray.getJSONObject(i);
                String gpp = phoneSignal.getString("gpp");
                String rsrp = phoneSignal.getString("rsrp");
                String type = phoneSignal.getString("type");
                //mylog.prru("prru data: enbid:"+type+" userid:"+userid+" gpp"+gpp+" rsrp:"+rsrp);
                prruSignalDao.savePhoneSignal(gpp,userid,gpp,rsrp,"*",localTimes,type);
            }
    	}

        if(!StringUtils.isEmpty(signalBlueJson)){
        	JSONArray signalArray = JSONArray.fromObject(signalBlueJson);
            for(int i = 0; i<signalArray.size(); i++){
            	JSONObject phoneSignal = signalArray.getJSONObject(i);
                String gpp = phoneSignal.getString("gpp");
                String rsrp = phoneSignal.getString("rsrp");
                String type = phoneSignal.getString("type");
                //mylog.prru("prru data: enbid:"+type+" userid:"+userid+" gpp"+gpp+" rsrp:"+rsrp);
                prruSignalDao.savePhoneSignal(gpp,userid,gpp,rsrp,"*",localTimes,type);
            }
        }
        
        result.put("data", "上传成功");
        return result;
    }
    
    /** 
     * @Title: calcFloorNo
     * @Description: 根据用户的gpp计算楼层
     * @param gpp：扫描到的gpp
     * @return 
     */
    public String calcFloorNo(List<String> gpps){      
        Map<String, Integer> floorCountMap = new HashMap<String, Integer>();
        List<Map<String, Object>> singleGppMap = new ArrayList<Map<String, Object>>();
        
        for(int i = 0;i<gpps.size();i++){
            String gpp = gpps.get(i);
            singleGppMap.clear();
            
            singleGppMap = prruSignalDao.getFloorCount(gpp);
            
            /*返回每个gpp在floorNo中出现的次数
             floorNo	floorNoCount
             10004.00	2
             10002.00	8
             10003.00	4
             */
            
        	//遍历楼层数据
        	for(Map<String, Object> singleGpp:singleGppMap) {
        		String floorTemp = singleGpp.get("floorNo").toString();
        		int countTemp = Integer.parseInt(singleGpp.get("floorNoCount").toString());
        		
        		if (floorCountMap.containsKey(floorTemp)) {
        			floorCountMap.put(floorTemp, countTemp + floorCountMap.get(floorTemp));
        		}
        		else {
        			floorCountMap.put(floorTemp, countTemp);
        		}
        	}
        }
        
        //打印每个楼层的gpp总数
        for (String key : floorCountMap.keySet()) {
        	 LOG.debug("floorCountMap key= "+ key + " and value= " + floorCountMap.get(key));
        }
        
        /*选择floorCountMap中floorNoCount最大的为用户所在楼层
        floorNo	floorNoCount
        10004.00	6
        10002.00	235
        10003.00	69
        */
        List<Map.Entry<String,Integer>> entryList = new ArrayList<Map.Entry<String,Integer>>(floorCountMap.entrySet());  
        Collections.sort(entryList, new Comparator<Map.Entry<String,Integer>>() {  
            public int compare(Entry<String,Integer> entry1,  
                    Entry<String,Integer> entry2) {  
                int value1 = entry1.getValue().intValue();  
                int value2 = entry2.getValue().intValue();  
                
                return value2 - value1;  
            }  
        }); 
        
        // 返回最大值
        return entryList.get(0).getKey();
    }
    
    /** 
     * @Title: getLocationPrru 
     * @Description: 根据用户id，匹配特征库，获取定位信息
     * @param userId：用户id
     * @param x 上一次定位点x坐标
     * @param y 上一次定位点y坐标
     * @return 
     */
    public Map<String, Object> getLocationPrru(String userId, String x, String y, String x1, String y1, String floorNo){
        LOG.debug("用户id："+userId);
        // 结果
        Map<String, Object> result = new HashMap<String,Object>();
        if (StringUtils.isEmpty(userId))
        {
            LOG.error("用户id为空!");
            result.put("error", "用户id为空!");
            return result;
        }
        // 取出当前用户的prru信号信息 TODO:新定位算法的场合要用另一个dao方法
        //List<PrruSignalModel> signals = prruSignalDao.getCurrentSignalByUserId(ConvertUtil.convertMacOrIp(userId), "3");
        //List<PrruSignalModel> signals = prruSignalDao.getTwoSignalByUserId(ConvertUtil.convertMacOrIp(userId), "3");
        List<PrruSignalModel> signals = prruSignalDao.getSignalsByUserId(ConvertUtil.convertMacOrIp(userId), blueCount, "3");
        // 修改gpp的值,添加enbId
        //editGpp(signals);
        // 取出信号信息中的柜框槽号，并拼接成字符串
        List<String> gpps = new ArrayList<String>();
        for(PrruSignalModel item : signals){
            gpps.add(item.getGpp());
            LOG.debug("用户["+userId+"]的信号信息："+item.toString());
        }
        LOG.debug("用户柜框槽号信息："+gpps.toString());
        
        // 如果未找到信号数据，返回错误信息
        if(signals.isEmpty()){
            LOG.error("用户信号数据为空");
            result.put("error", "用户信号数据为空");
            return result;
        }
        
        // TODO 新定位算法的场合，去重
        //getAvgPrruSignals(signals, gpps);
        getAveragePrruSignals(signals, gpps);
        LOG.debug("去重后的柜框槽号信息："+gpps.toString());
        // 获取楼层号
        if(floorNo == null || "null".equals(floorNo)){
            try{
                floorNo = prruSignalDao.queryFloorNoByUseId(ConvertUtil.convertMacOrIp(userId));
            }catch(Exception e){
                LOG.error("获取楼层号失败：" + e.getMessage());
                result.put("error", "获取楼层号失败!");
                return result;
            }
        }
        
        //根据用户的gpp信息计算楼层
        String calcFloor = calcFloorNo(gpps);

        if (!calcFloor.substring(0,calcFloor.indexOf(".")).equals((floorNo.substring(0,calcFloor.indexOf("."))))) {
        	LOG.error("计算出来的楼层和前台传入楼层不同");
        }
        LOG.debug("用户所在楼层："+floorNo + " 计算出来的楼层："+ calcFloor);
        
        // 取出与用户信号prru有交集的特征库
        List<PrruFeatureModel> prruFeatures = prruSignalDao.getRelativeFeature(gpps,floorNo);
        // 如果没有匹配的特征库，返回错误信息
        if(prruFeatures.isEmpty()){
            LOG.error("用户信号没有匹配的特征库");
            result.put("error", "用户信号没有匹配的特征库");
            return result;
        }
        
        // 格式转换
        Map<Integer,PrruFeatureModel> formated = formatFeature(prruFeatures);        
        // 计算最接近点
        PrruFeatureModel closest = calcClosestPoint(signals,formated,x,y,x1,y1,floorNo);
        //数据格式转换
        LocationModel data = new LocationModel();   
        data.setX(new BigDecimal(closest.getX()).multiply(MULTIPLE10));
        data.setY(new BigDecimal(closest.getY()).multiply(MULTIPLE10));
        data.setZ(new BigDecimal(closest.getFloorNo()));
        data.setUserID(closest.getUserId());
        data.setTimestamp(new BigDecimal(System.currentTimeMillis()));
        data.setTimestampPrru(new BigDecimal(closest.getTimestamp()));
        // 返回
        result.put("data", data);
        return result;
    }
    /** 
     * @Title: getGppS 
     * @Description: 按gpp去重
     * @param validSignals
     * @param GppS 
     */
    private List<String> getGppS(List<PrruSignalModel> signals){
    	List<String> gpps = new ArrayList<String>();
        for(PrruSignalModel item : signals){
            gpps.add(item.getGpp());
            LOG.debug("用户的信号信息："+item.toString());
        }
		return gpps;
    }
    /** 
     * @Title: preProcessPrruSignals 
     * @Description: 按gpp去重
     * @param validSignals
     * @param validGpp 
     */
    private void preProcessPrruSignals(List<PrruSignalModel> signals){
 
    	Map<Long, BigDecimal> rsrpAvg = new HashMap<Long, BigDecimal>();
        Map<Long, BigDecimal> rsrpCount = new HashMap<Long, BigDecimal>();
        // 遍历
        for(int i = 0;i<signals.size();i++){
            // 时间戳
            long timestamp = signals.get(i).getTimestamp();
            // 不存在就创建
            if(rsrpAvg.get(timestamp) == null){
                rsrpAvg.put(timestamp, signals.get(i).getRsrp());
                rsrpCount.put(timestamp, new BigDecimal(1));
            }else{
                // 存在，则相加
                BigDecimal rsrpTemp = signals.get(i).getRsrp().add(rsrpAvg.get(timestamp));
                rsrpAvg.put(timestamp, rsrpTemp);
                rsrpCount.put(timestamp, rsrpCount.get(timestamp).add(new BigDecimal(1)));
            }
        }
        // 取平均值,计算checkValue
        Iterator<Entry<Long, BigDecimal>> it = rsrpAvg.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<Long, BigDecimal> entity = (Entry<Long, BigDecimal>) it.next();
            Long key = entity.getKey();
            BigDecimal value = entity.getValue();
            entity.setValue(value.divide(rsrpCount.get(key),2));
        }
        // 修正rsrp，得到data值
        for(PrruSignalModel p:signals){
            long timestamp = p.getTimestamp();
            p.setRsrp(p.getRsrp().subtract(rsrpAvg.get(timestamp)));
        }       
    }
    
    /** 
     * @Title: formatFeatureValue 
     * @Description: 将特征库数据转换格式，便于接下来计算 
     * @param datas：源数据
     * @return 
     */
    private void formatFeatureValue(List<PrruFeatureModel> datas, Map<Integer,PrruFeatureModel> result, String type, Boolean check){
        // 转换中间格式
        //Map<Integer,PrruFeatureModel> result = new HashMap<Integer,PrruFeatureModel>();
        // 遍历list，格式化
        for(PrruFeatureModel item:datas){
            // 如果结构里已存在该定位点的特征信息，则将特征值加入
            if(result.get(item.getId())!=null){
            	if(result.get(item.getId()).getFeatureValuesByType(type)!=null){
            		result.get(item.getId()).getFeatureValuesByType(type).put(item.getGpp(),item.getFeatureValue());
            	}
            	else{
            		Map<String, BigDecimal> features = new HashMap<String,BigDecimal>();
            		features.put(item.getGpp(),item.getFeatureValue());
            		result.get(item.getId()).setFeatureValuesByType(features,type);
            	}
            }
            // 否则，初始化该定位点的特征信息
            else{ 
            	if(check)
            	{
            		LOG.debug("该指纹id不存在：!"+item.getId());
            		continue;
            	}
            	Map<String, BigDecimal> features = new HashMap<String,BigDecimal>();
                features.put(item.getGpp(),item.getFeatureValue());
                item.setFeatureValuesByType(features, type);
                result.put(item.getId(), item);
            }
        }
    }
    /** 
     * @Title: getSignalWithProcess 
     * @Description: 将特征库数据转换格式，便于接下来计算 
     * @param datas：源数据
     * @return 
     */
    private List<PrruSignalModel> getSignalWithProcess(Map<Integer,PrruFeatureModel> featureResult,String userId,
    		long timestamp, String type, 
    		String floorNo, Boolean check){
    	List<PrruSignalModel> Signals = prruSignalDao.getCurrentSignalsByUserIdTime(ConvertUtil.convertMacOrIp(userId),timestamp,type); 
		if(!Signals.isEmpty()){
			List<String> Gpps = getGppS(Signals);
			// 取出与用户信号prru有交集的特征库
            List<PrruFeatureModel> features;
            if(floorNo == null || "null".equals(floorNo)){
            	features = prruSignalDao.getRelativeFeatureWithoutFloorNo(Gpps);
            }
            else{
            	features = prruSignalDao.getRelativeFeature(Gpps,floorNo);
            }
	        formatFeatureValue(features, featureResult, type, check);
		}
		return Signals;
    }
    /** 
     * @Title: checkIntersection 
     * @Description: 根据信号类型，交集比例检查交集是否满足
     * @return 
     */
    public boolean checkIntersection(List<PrruSignalModel> signals,
    		PrruFeatureModel featureModel,String type, double ratio){
    	if(featureModel.getFeatureValuesByType(type)==null){
    		LOG.error("未找到對應指紋點!");
    		return false;
    	}
    	double userSignalSize = (double)signals.size();
    	double featureSignalSize = (double)featureModel.getFeatureValuesByType(type).size();
		return (featureSignalSize>=(userSignalSize*ratio));	
    }
    /** 
     * @Title: calLocation3 
     * @Description: 根据用户上报信号，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> calLocation3(List<PrruSignalModel> lteSignals,List<PrruSignalModel> wifiSignals,List<PrruSignalModel> blueSignals,
    		Map<Integer,PrruFeatureModel> featureResult, String floorNo){
    	if(lteSignals.isEmpty()){
    		LOG.error("当前用户无Lte数据上报!");
    		return calLocationWifiBlue(wifiSignals,blueSignals,featureResult,floorNo);
    	}
    	else if(wifiSignals.isEmpty()){
    		LOG.error("当前用户无wifi数据上报!");
    		return calLocationLteBlue(lteSignals,blueSignals,featureResult,floorNo);
    	}
    	else if(blueSignals.isEmpty()){
    		LOG.error("当前用户无蓝牙数据上报!");
    		return calLocationLteWifi(lteSignals,wifiSignals,featureResult,floorNo);
    	}
    	LOG.debug("3种信号混合定位用户所在楼层："+floorNo+"lte信号个数："+lteSignals.size()+"wifi信号个数："+wifiSignals.size()+"蓝牙信号个数："+blueSignals.size());
    
    	Map<BigDecimal, Integer> distanceFeature = new TreeMap<BigDecimal, Integer>();
    	// 遍历map，计算各个特征半径
        Iterator<Entry<Integer, PrruFeatureModel>> it = featureResult.entrySet().iterator();  
        while(it.hasNext()){
            Entry<Integer, PrruFeatureModel> entity = it.next();
            int id = entity.getKey();
            PrruFeatureModel featureModel = entity.getValue();
            if(checkIntersection(lteSignals,featureModel,"1",LteRatio) && checkIntersection(wifiSignals,featureModel,"2",WifiRatio) && checkIntersection(blueSignals,featureModel,"3",BlueRatio)){
            	BigDecimal lteDistance = calSignalDistance(lteSignals,featureModel,"1");
            	BigDecimal wifiDistance = calSignalDistance(wifiSignals,featureModel,"2");
            	BigDecimal blueDistance = calSignalDistance(blueSignals,featureModel,"3");
            	BigDecimal temDistance = lteDistance.multiply(LTE_MIX_3_1).add((wifiDistance.multiply(WIFI_MIX_3_1)));
            	BigDecimal signalDistance = blueDistance.multiply(BLUE_MIX_3_1).add(temDistance);
            	distanceFeature.put(signalDistance, id);
            }
        }
        return KNNMatch(distanceFeature, featureResult);
    }
    /** 
     * @Title: calLocationLteWifi 
     * @Description: 根据用户上报Lte和Wifi信号，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> calLocationLteWifi(List<PrruSignalModel> lteSignals,List<PrruSignalModel> wifiSignals,
    		Map<Integer,PrruFeatureModel> featureResult, String floorNo){
    	if(lteSignals.isEmpty()){
    		LOG.error("当前用户无Lte数据上报!");
    		return calLocation1(wifiSignals,featureResult,floorNo,WifiRatio,"2");
    	}
    	else if(wifiSignals.isEmpty()){
    		LOG.error("当前用户无wifi数据上报!");
    		return calLocation1(lteSignals,featureResult,floorNo,LteRatio,"1");
    	}
    	LOG.debug("Lte和Wifi信号混合定位，用户所在楼层："+floorNo+"lte信号个数："+lteSignals.size()+"wifi信号个数："+wifiSignals.size());
    	Map<BigDecimal, Integer> distanceFeature = new TreeMap<BigDecimal, Integer>();
    	// 遍历map，计算各个特征半径
        Iterator<Entry<Integer, PrruFeatureModel>> it = featureResult.entrySet().iterator(); 
    	while(it.hasNext()){
            Entry<Integer, PrruFeatureModel> entity = it.next();
            int id = entity.getKey();
            PrruFeatureModel featureModel = entity.getValue();
            if(checkIntersection(lteSignals,featureModel,"1",LteRatio) && checkIntersection(wifiSignals,featureModel,"2",WifiRatio)){
            	BigDecimal lteDistance = calSignalDistance(lteSignals,featureModel,"1");
            	BigDecimal wifiDistance = calSignalDistance(wifiSignals,featureModel,"2");
            	BigDecimal signalDistance = lteDistance.multiply(LTE_WIFI_2_1).add((wifiDistance.multiply(WIFI_LTE_2_1)));
            	distanceFeature.put(signalDistance, id);
            }
        }
    	return KNNMatch(distanceFeature, featureResult);
    }
    /** 
     * @Title: calSignalDistance 
     * @Description: 根据用户上报信号值，计算rsrp距离
     * @return 
     */
    public BigDecimal calSignalDistance(List<PrruSignalModel> signals,
    		PrruFeatureModel featureModel, String type){
    	// rsrp距离
        BigDecimal dis = new BigDecimal("0.000");
        int m = 0;
        Map<String, BigDecimal> featureValue = featureModel.getFeatureValuesByType(type);
        for(PrruSignalModel p:signals){
        	String gpp = p.getGpp();
        	if(featureValue.containsKey(gpp)){
        		m++;
        		BigDecimal rsrp = p.getRsrp();
        		BigDecimal featureRsrp = featureValue.get(gpp);
        		dis = dis.add(rsrp.subtract(featureRsrp).pow(2));
        	}
        }
        if(m>0){
        	if(type.equals("1")){
        		m=m*100;
        	}
        	dis = dis.divide(new BigDecimal(m),2,6);
        }
        return dis;
    }
    /** 
     * @Title: calLocationLteBlue 
     * @Description: 根据用户上报Lte和蓝牙信号，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> calLocationLteBlue(List<PrruSignalModel> lteSignals,List<PrruSignalModel> blueSignals,
    		Map<Integer,PrruFeatureModel> featureResult, String floorNo){
    	
    	if(lteSignals.isEmpty()){
    		LOG.error("当前用户无Lte数据上报!");
    		return calLocation1(blueSignals,featureResult,floorNo,BlueRatio,"3");
    	}
    	else if(blueSignals.isEmpty()){
    		LOG.error("当前用户无蓝牙数据上报!");
    		return calLocation1(lteSignals,featureResult,floorNo,LteRatio,"1");
    	}
    	LOG.debug("Lte和蓝牙信号混合定位，用户所在楼层："+floorNo+"lte信号个数："+lteSignals.size()+"蓝牙信号个数："+blueSignals.size());
    	Map<BigDecimal, Integer> distanceFeature = new TreeMap<BigDecimal, Integer>();
    	// 遍历map，计算各个特征半径
        Iterator<Entry<Integer, PrruFeatureModel>> it = featureResult.entrySet().iterator(); 
    	while(it.hasNext()){
            Entry<Integer, PrruFeatureModel> entity = it.next();
            int id = entity.getKey();
            PrruFeatureModel featureModel = entity.getValue();
            if(checkIntersection(lteSignals,featureModel,"1",LteRatio) && checkIntersection(blueSignals,featureModel,"3",BlueRatio)){
            	BigDecimal lteDistance = calSignalDistance(lteSignals,featureModel,"1");
            	BigDecimal blueDistance = calSignalDistance(blueSignals,featureModel,"3");
            	BigDecimal signalDistance = lteDistance.multiply(LTE_BLUE_2_1).add((blueDistance.multiply(BLUE_LTE_2_1)));
            	distanceFeature.put(signalDistance, id);
            }
        }
    	return KNNMatch(distanceFeature, featureResult);
    }
    /** 
     * @Title: KNNMatch 
     * @Description: 根据各个指纹点的匹配度，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> KNNMatch(Map<BigDecimal, Integer> distanceFeature,
    		Map<Integer,PrruFeatureModel> featureResult){
    	Map<String, Object> result = new HashMap<String,Object>();
    	if(featureResult.isEmpty()){
    		LOG.debug("指纹点为空");
            // 返回
            result.put("error", "没有获取到匹配的指纹点");
    		return result;
    	}
    	int k = 0;
    	BigDecimal delta = new BigDecimal(0.0001);
    	BigDecimal sum = new BigDecimal(0.0001);
    	BigDecimal one = new BigDecimal(1.000);
    	String FloorNum = "";
    	//第一次遍历获取TOP_K的加权系数
    	for (Entry<BigDecimal, Integer> entry: distanceFeature.entrySet()) {
    		if(k<TOP_K){
    			int featureId = entry.getValue();
    			String curFloorNum = featureResult.get(featureId).getFloorNo();
    			if(k==0){
    				FloorNum = curFloorNum;
        			BigDecimal postDistance = one.divide((entry.getKey().add(delta)),2,6);
	    			sum = sum.add(postDistance);
	    			k++;
        		}
    			else if(curFloorNum.equals(FloorNum)){
    					BigDecimal postDistance = one.divide((entry.getKey().add(delta)),2,6);
    	    			sum = sum.add(postDistance);
    	    			k++;
    				}
    		}
    		else{
    			break;
    		}
    	}
    	LOG.debug("TOP_K的加权系数獲取完畢："+sum.doubleValue());
    	k=0;
    	BigDecimal pos_x = new BigDecimal(0.000);
    	BigDecimal pos_y = new BigDecimal(0.000);
    	//第二次遍历获取TOP_K的加权坐标
    	for (Entry<BigDecimal, Integer> entry: distanceFeature.entrySet()) {
    		if(k<TOP_K){
    			int featureId = entry.getValue();
    			String curFloorNum = featureResult.get(featureId).getFloorNo();
    			if(!curFloorNum.equals(FloorNum)){
    				continue;
    			}
    			BigDecimal upDistance = one.divide((entry.getKey().add(delta)),2,6);
    			String x = featureResult.get(featureId).getX();
    			String y = featureResult.get(featureId).getY();
    			pos_x = pos_x.add(upDistance.divide(sum,2,6).multiply(new BigDecimal(x)));
    			pos_y = pos_y.add(upDistance.divide(sum,2,6).multiply(new BigDecimal(y)));
    			k++;
    		}
    		else{
    			break;
    		}
    	}
    	LOG.debug("TOP_K的加权坐标完畢！");
    	k=0;
    	int minFeatureId = 0;
    	BigDecimal minDistance = new BigDecimal(0.000);
    	//第三次遍历匹配TOP_N中与加权坐标欧式距离最小的指纹点
    	for (Entry<BigDecimal, Integer> entry: distanceFeature.entrySet()) {
    		if(k<TOP_N){
    			int featureId = entry.getValue();
    			String curFloorNum = featureResult.get(featureId).getFloorNo();
    			if(!curFloorNum.equals(FloorNum)){
    				continue;
    			}
    			String x = featureResult.get(featureId).getX();
    			String y = featureResult.get(featureId).getY();
    			BigDecimal distanceX = pos_x.subtract((new BigDecimal(x))).pow(2);
    			BigDecimal distanceY = pos_y.subtract((new BigDecimal(y))).pow(2);
    			BigDecimal tmpDistance = distanceX.add(distanceY);
    			if(k==0 || tmpDistance.compareTo(minDistance)<0){
    				minDistance = tmpDistance;
        			minFeatureId = featureId;
        			FloorNum = featureResult.get(featureId).getFloorNo();
    			}
    			k++;
    		}
    		else{
    			break;
    		}
    	}
    	LOG.debug("歐式距離最小的指紋點："+minFeatureId);
    	//根据指纹点
    	String x = featureResult.get(minFeatureId).getX();
		String y = featureResult.get(minFeatureId).getY();
		//数据格式转换
        LocationModel data = new LocationModel();   
        data.setX(new BigDecimal(x).multiply(MULTIPLE10));
        data.setY(new BigDecimal(y).multiply(MULTIPLE10));
        data.setZ(new BigDecimal(FloorNum));
        LOG.debug("floorNum："+FloorNum);
        data.setUserID(featureResult.get(minFeatureId).getUserId());
        //LOG.debug("userid："+featureResult.get(minFeatureId).getUserId());
        data.setTimestamp(new BigDecimal(System.currentTimeMillis()));
        data.setTimestampPrru(new BigDecimal(featureResult.get(minFeatureId).getTimestamp()));
        LOG.debug("返回的点：x-"+data.getX()+",y-"+data.getY()+",z-"+FloorNum);
        // 返回
        result.put("data", data);
		return result;
    }
    /** 
     * @Title: calLocationWifiBlue 
     * @Description: 根据用户上报的Wifi和蓝牙信号，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> calLocationWifiBlue(List<PrruSignalModel> wifiSignals,List<PrruSignalModel> blueSignals,
    		Map<Integer,PrruFeatureModel> featureResult, String floorNo){
    	
    	if(wifiSignals.isEmpty()){
    		LOG.error("当前用户无wifi数据上报!");
    		return calLocation1(blueSignals,featureResult,floorNo,BlueRatio,"3");
    	}
    	else if(blueSignals.isEmpty()){
    		LOG.error("当前用户无蓝牙数据上报!");
    		return calLocation1(wifiSignals,featureResult,floorNo,WifiRatio,"2");
    	}
    	LOG.debug("wifi和蓝牙信号混合定位，用户所在楼层："+floorNo+"wifi信号个数："+wifiSignals.size()+"蓝牙信号个数："+blueSignals.size());
    	Map<BigDecimal, Integer> distanceFeature = new TreeMap<BigDecimal, Integer>();
    	// 遍历map，计算各个特征半径
        Iterator<Entry<Integer, PrruFeatureModel>> it = featureResult.entrySet().iterator(); 
    	while(it.hasNext()){
            Entry<Integer, PrruFeatureModel> entity = it.next();
            int id = entity.getKey();
            PrruFeatureModel featureModel = entity.getValue();
            if(checkIntersection(wifiSignals,featureModel,"2",WifiRatio) && checkIntersection(blueSignals,featureModel,"3",BlueRatio)){
            	BigDecimal wifiDistance = calSignalDistance(wifiSignals,featureModel,"2");
            	BigDecimal blueDistance = calSignalDistance(blueSignals,featureModel,"3");
            	BigDecimal signalDistance = wifiDistance.multiply(WIFI_BLUE_2_1).add((blueDistance.multiply(BLUE_WIFI_2_1)));
            	distanceFeature.put(signalDistance, id);            	
            }
        }
    	
		return KNNMatch(distanceFeature, featureResult);
    }
    /** 
     * @Title: calLocationWifiBlue 
     * @Description: 根据用户上报信号，匹配特征库，获取定位信息
     * @return 
     */
    public Map<String, Object> calLocation1(List<PrruSignalModel> signals,
    		Map<Integer,PrruFeatureModel> featureResult, String floorNo, double setRatio, String type){
    	Map<String, Object> result = new HashMap<String,Object>();
    	if(signals.isEmpty()){
    		LOG.error("当前无用户数据上报!");
    		result.put("error", "当前无用户数据上报");
            return result;
    	}
    	LOG.debug("单信号定位，用户所在楼层："+floorNo+"信号个数："+signals.size()+"信号类型："+type);
    	try{
    		Map<BigDecimal, Integer> distanceFeature = new TreeMap<BigDecimal, Integer>();
    		// 遍历map，计算各个特征半径
    		Iterator<Entry<Integer, PrruFeatureModel>> it = featureResult.entrySet().iterator(); 
    		while(it.hasNext()){
    			Entry<Integer, PrruFeatureModel> entity = it.next();
    			int id = entity.getKey();
    			PrruFeatureModel featureModel = entity.getValue();
    			if(checkIntersection(signals,featureModel,type,setRatio)){
    				BigDecimal signalDistance = calSignalDistance(signals,featureModel,type);
    				distanceFeature.put(signalDistance, id);
    			}
    		}
    		LOG.debug("单信号定位，距離集合大小："+distanceFeature.size()+"信号类型："+type);
    		return KNNMatch(distanceFeature, featureResult);
    	}catch(Exception e){
            LOG.error("KNN ERROR:"+e.getMessage());
        }finally{
        	result.put("error", "KNN MATCH ERROR");
        	//return result;
        }
    	return result;
    	
    }
    /** 
     * @Title: getLocationMixPrru 
     * @Description: 根据用户id，匹配特征库，获取定位信息
     * @param userId：用户id
     * @param x 上一次定位点x坐标
     * @param y 上一次定位点y坐标
     * @return 
     */
    public Map<String, Object> getLocationMixPrru(String userId, String switchLTE, String switchWifi, String switchBlue, String floorNo){
        LOG.debug("用户id："+userId);
        // 结果
        Map<String, Object> result = new HashMap<String,Object>();
        if (StringUtils.isEmpty(userId))
        {
            LOG.error("用户id为空!");
            result.put("error", "用户id为空!");
            return result;
        }
        LOG.debug("定位开始，用户所在楼层："+floorNo+"LTE开关："+switchLTE+"Wifi开关："+switchWifi+"Blue开关："+switchBlue);
        long LTETimes = System.currentTimeMillis()-LteDelay*1000;
        boolean check = false;
        Map<Integer,PrruFeatureModel> featureResult = new HashMap<Integer,PrruFeatureModel>();
        if(switchLTE.equals("1")){
        	//LTE打开
        	List<PrruSignalModel> signals = prruSignalDao.getCurrentSignalsByUserIdTime(ConvertUtil.convertMacOrIp(userId),LTETimes,"1");
        	// 修改gpp的值,添加enbId
            editGpp(signals);
            List<String> gpps = getGppS(signals);
            preProcessPrruSignals(signals);
            // 如果未找到信号数据，返回错误信息
            if(signals.isEmpty()){
                LOG.error("用户信号数据为空");
                result.put("error", "用户信号数据为空");
                return result;
            }
            // 取出与用户信号prru有交集的特征库
            List<PrruFeatureModel> prruFeatures;
            if(floorNo == null || "null".equals(floorNo)){
            	prruFeatures = prruSignalDao.getRelativeFeatureWithoutFloorNo(gpps);
            }
            else{
            	prruFeatures = prruSignalDao.getRelativeFeature(gpps,floorNo);
            }
     
            // 如果没有匹配的特征库，返回错误信息
            if(prruFeatures.isEmpty()){
                LOG.error("用户信号没有匹配的特征库");
                result.put("error", "用户信号没有匹配的特征库");
                return result;
            }
            // 格式转换
            formatFeatureValue(prruFeatures, featureResult, "1",check);
            check = !(featureResult.isEmpty());
        	if(switchWifi.equals("1")){
        		List<PrruSignalModel> wifiSignals = getSignalWithProcess(featureResult,userId,LTETimes,"2",floorNo,check);
                if(switchBlue.equals("1"))
        		{
                	check = (featureResult.isEmpty());
        			//LTE,Wifi,蓝牙三种信号混合定位
        			List<PrruSignalModel> blueSignals = getSignalWithProcess(featureResult,userId,LTETimes,"3",floorNo,check);
        			return calLocation3(signals,wifiSignals,blueSignals,featureResult,floorNo);
        		}
        		//LTE,Wifi两种信号混合定位
                return calLocationLteWifi(signals,wifiSignals,featureResult,floorNo);
        	}
        	else if(switchBlue.equals("1")){
        		//LTE，蓝牙两种信号混合定位
        		List<PrruSignalModel> blueSignals = getSignalWithProcess(featureResult,userId,LTETimes,"3",floorNo,check);
        		return calLocationLteBlue(signals,blueSignals,featureResult,floorNo);
        	}
        	return calLocation1(signals,featureResult,floorNo,LteRatio,"1");
        }
        else if(switchWifi.equals("1"))
        {
        	check = false;
        	List<PrruSignalModel> wifiSignals = getSignalWithProcess(featureResult,userId,LTETimes,"2",floorNo,check);
        	if(switchBlue.equals("1"))
        	{
        		check = (!featureResult.isEmpty());
        		//Wifi,蓝牙两种信号混合定位
        		List<PrruSignalModel> blueSignals = getSignalWithProcess(featureResult,userId,LTETimes,"3",floorNo,check);
        		return calLocationWifiBlue(wifiSignals,blueSignals,featureResult,floorNo);
        	}
        	return calLocation1(wifiSignals,featureResult,floorNo,WifiRatio,"2");
        }
        else if(switchBlue.equals("1")){
        	//蓝牙一种信号进行定位
        	List<PrruSignalModel> blueSignals = getSignalWithProcess(featureResult,userId,LTETimes,"3",floorNo,false);
        	return calLocation1(blueSignals,featureResult,floorNo,BlueRatio,"3");
        }
        LOG.debug("用户信号使能开关未打开");
        result.put("error", "用户信号使能开关未打开");
        return result;
    }
    
    /** 
     * @Title: getLocationPrru 
     * @Description: 根据用户id，匹配特征库，获取定位信息
     * @param userId：用户id
     * @param x 上一次定位点x坐标
     * @param y 上一次定位点y坐标
     * @return 
     */
    public Map<String, Object> getLocationPrruBak(String userId, String x, String y, String floorNo){
        LOG.debug("用户id："+userId);
        // 结果
        Map<String, Object> result = new HashMap<String,Object>();
        if (StringUtils.isEmpty(userId))
        {
            LOG.error("用户id为空!");
            result.put("error", "用户id为空!");
            return result;
        }
        // 取出当前用户的prru信号信息 TODO:新定位算法的场合要用另一个dao方法
        //List<PrruSignalModel> signals = prruSignalDao.getCurrentSignalByUserId(ConvertUtil.convertMacOrIp(userId));
        List<PrruSignalModel> signals = prruSignalDao.getTwoSignalByUserId(ConvertUtil.convertMacOrIp(userId), "1");
        // 取出信号信息中的柜框槽号，并拼接成字符串
        List<PrruSignalModel> validSignals = new ArrayList<PrruSignalModel>();
        List<String> validGpp = new ArrayList<String>();
        for(PrruSignalModel item : signals){
            if(item.getRsrp().compareTo(DEFAULT_VALUE) > 0){
                validSignals.add(item);
                validGpp.add(item.getGpp());
            }
        }
        LOG.debug("用户柜框槽号信息："+validGpp.toString());
        
        // 如果未找到信号数据，返回错误信息
        if(validSignals.isEmpty()){
            LOG.error("用户信号数据为空");
            result.put("error", "用户信号数据为空");
            return result;
        }
        
        // TODO 新定位算法的场合，去重
        getAvgPrruSignals(validSignals, validGpp);
        LOG.debug("去重后的柜框槽号信息："+validGpp.toString());
        // 获取楼层号
        if(floorNo == null || "null".equals(floorNo)){
            try{
                floorNo = prruSignalDao.queryFloorNoByUseId(ConvertUtil.convertMacOrIp(userId));
            }catch(Exception e){
                LOG.error("获取楼层号失败：" + e.getMessage());
                result.put("error", "获取楼层号失败!");
                return result;
            }
        }
        LOG.debug("用户所在楼层："+floorNo);
        // 取出与用户信号prru有交集的特征库
        List<PrruFeatureModel> prruFeatures = prruSignalDao.getRelativeFeature(validGpp,floorNo);
        // 如果没有匹配的特征库，返回错误信息
        if(prruFeatures.isEmpty()){
            LOG.error("用户信号没有匹配的特征库");
            result.put("error", "用户信号没有匹配的特征库");
            return result;
        }
        
        // 格式转换
        Map<Integer,PrruFeatureModel> formated = formatFeature(prruFeatures);        
        // 计算最接近点
        PrruFeatureModel closest = calcClosestPoint(validSignals,formated,x,y,"","",floorNo);
        //数据格式转换
        LocationModel data = new LocationModel();   
        data.setX(new BigDecimal(closest.getX()).multiply(MULTIPLE10));
        data.setY(new BigDecimal(closest.getY()).multiply(MULTIPLE10));
        data.setZ(new BigDecimal(closest.getFloorNo()));
        data.setUserID(closest.getUserId());
        data.setTimestamp(new BigDecimal(System.currentTimeMillis()));
        data.setTimestampPrru(new BigDecimal(closest.getTimestamp()));
        // 返回
        result.put("data", data);
        return result;
    }
    
    private String getStateString(int state){
    	if(state==1){
    		return "AE信号无上报";
    	}
    	if(state==2){
    		return "LTE信号上报不连续";
    	}
    	if(state==3){
    		return "WIFI信号上报不连续";
    	}
    	if(state==4){
    		return "LTE与WIFI信号融合失败";
    	}
    	if(state==5){
    		return "定点采集超时";
    	}
    	if(state==6){
    		return "线路采集超时";
    	}
		return "成功";
    	
    }
    
    public Map<String, Object> finishCollectPrru(PrruFeatureApiModel requestModel)
    {
        CollectThread t = (CollectThread) GlobalConf.getPrruThread(requestModel.getUserId());
        int state = 0;
        if(t != null){
        	// 取得当前时间戳，作为任务起始时间
            long finishTimestamp = System.currentTimeMillis();
            state = t.finishLineSampling(finishTimestamp);
            t.stopThread();
        }
        else{
        	state = 1;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("state", state);
        result.put("data", getStateString(state));
        return result;
    }
    /** 
     * @Title: checkIsFinished 
     * @Description: 检查采集是否成功
     * @param model
     * @return 
     */
    public Map<String,Object> checkIsFinished(PrruFeatureApiModel model){
        // 返回值
        Map<String, Object> result = new HashMap<String,Object>();
        //查看该线程是否存在
        Thread  t = GlobalConf.getPrruThread(model.getUserId());
        if (t!=null) {
            result.put("data", "waiting");
            int state = ((CollectThread) t).getRunState();
            result.put("state", state);
            if(state!=0){
            	((CollectThread) t).stopThread();
            }
        }else
        {
        	result.put("state", 0);
            // 查询生成的特征半径
            PrruFeatureModel feature = 
                    prruSignalDao.getFeatureByPosition(model.getX(), model.getY(), model.getFloorNo());
            BigDecimal radius = null;
            if(feature!=null){
            	radius = feature.getFeatureRadius();
            }
            // radius为null说明异常中断，否则采集完成
            if (radius==null) {
            	result.put("state", 1);
                result.put("data", "AE信号无上报"); 
            }else 
            {
                // 无效的 需要删除，否则采集数据有效
                if (radius.compareTo(model.getRadius())>0) {
                    prruSignalDao.deletFeatureById(feature.getId());
                    prruSignalDao.deleteFeatureDetailByFeatureId(feature.getId());
                    result.put("data", "failed");
                }else
                {
                    result.put("data", "success"); 
                }
            }
        }
       
        return result;
    }
    
    /** 
     * @Title: getAllFeaturePosition 
     * @Description: 获取指定楼层所有的特征点
     * @param floorNo
     * @return 
     */
    public Map<String,Object> getAllFeaturePosition(String floorNo){
        // 返回值
        Map<String,Object> result = new HashMap<String,Object>();
        List<PrruFeatureModel> data = prruSignalDao.getAllFeaturePostion(floorNo);
        result.put("data", data);
        return result;
    }
    
    /** 
     * @Title: deleteFeatureById 
     * @Description: 删除指定id的特征库信息
     * @param id
     * @return 
     */
    public Map<String,Object> deleteFeatureById(int id){
     // 返回值
        Map<String,Object> result = new HashMap<String,Object>();
        int res1 = prruSignalDao.deletFeatureById(id);
        int res2 = prruSignalDao.deleteFeatureDetailByFeatureId(id);
        if(res1==1 && res2==1){
            result.put("data", "success");
        }else{
            result.put("error", true);
        }
        return result;
    }
    
    /** 
     * @Title: getCurrentPrruWithRsrp 
     * @Description: 取出用户id对应的prru信息
     * @param userId
     * @return 
     */
    public Map<String,Object> getCurrentPrruWithRsrp(String userId){
        // 返回值
        Map<String,Object> result = new HashMap<String,Object>();
        // 取出用户id对应的prru信息
        List<PrruSignalModel> res1 = prruSignalDao.getCurrentSignalByUserId(ConvertUtil.convertMacOrIp(userId), "1");
        result.put("data", res1);
        return result;
       }
    
    
    /** 
     * @Title: formatFeature 
     * @Description: 将特征库数据转换格式，便于接下来计算 
     * @param datas：源数据
     * @return 
     */
    private Map<Integer,PrruFeatureModel> formatFeature(List<PrruFeatureModel> datas){
        // 转换中间格式
        Map<Integer,PrruFeatureModel> result = new HashMap<Integer,PrruFeatureModel>();
        // 遍历list，格式化
        for(PrruFeatureModel item:datas){
            // 需要整合的特征值
            PrruFeatureDetailModel detail = new PrruFeatureDetailModel();
            detail.setGpp(item.getGpp());
            detail.setFeatureValue(item.getFeatureValue());
            // 如果结构里已存在该定位点的特征信息，则将特征值加入
            if(result.get(item.getId())!=null){
                result.get(item.getId()).getFeatureValues().add(detail);
            }
            // 否则，初始化该定位点的特征信息
            else{                
                List<PrruFeatureDetailModel> features = new ArrayList<PrruFeatureDetailModel>();
                features.add(detail);
                item.setFeatureValues(features);
                result.put(item.getId(), item);
            }
        }
        return result;
    }
    
    /** 
     * @Title: calcClosestPoint 
     * @Description: 计算信号对应的特征库里最接近的点
     * @param signals：信号数据
     * @param datas：特征库数据
     * @return 
     */
    private PrruFeatureModel calcClosestPoint(
            List<PrruSignalModel> signals, 
            Map<Integer,PrruFeatureModel> datas,
            String x,
            String y,
            String x1,
            String y1,
            String floorNo){
        // 距离阈值数组
        String[] disArray = disArrayStr.split(",");
        // 系数数组
        String[] ratioArray = ratioArrayStr.split(",");
        // 特征半径集合
        Map<Integer, BigDecimal> distance = new HashMap<Integer, BigDecimal>();
        
        // 遍历map，计算各个特征半径
        Iterator<Entry<Integer, PrruFeatureModel>> it = datas.entrySet().iterator();  
        while(it.hasNext())  
        {
            Entry<Integer, PrruFeatureModel> entity = it.next();
            int id = entity.getKey();
            PrruFeatureModel featureModel = entity.getValue();
            // 计算该点特征值得最小值
            BigDecimal minusFeature = calcMinus(featureModel);
            // 特征半径
            BigDecimal dis = new BigDecimal(0);
            
            for(PrruSignalModel p:signals){
                String gpp = p.getGpp();
                BigDecimal rsrp = p.getRsrp();
                BigDecimal featureValue = getFeatureValue(gpp,featureModel,minusFeature);
                if(featureValue==null){
                    continue;
                }
                LOG.debug("参与计算的gpp："+gpp);
                dis = dis.add(rsrp.subtract(featureValue).pow(2));
            }
            
            if(datas.size()>0){
            	dis = dis.divide(new BigDecimal(datas.size()),2);
            }
            
            LOG.debug("半径："+dis);
            LOG.debug(featureModel.getX());
            LOG.debug(x);
            LOG.debug(featureModel.getY());
            LOG.debug(y);
            LOG.debug(featureModel.getFloorNo());
            LOG.debug(floorNo);
            LOG.debug("特征库ID："+featureModel.getId());
            if(new BigDecimal(x).compareTo(new BigDecimal(0)) != 0 && new BigDecimal(y).compareTo(new BigDecimal(0)) != 0){
                // 如果特征点是上一个定位点，特征半径要乘以小于1的系数ratio
                if(new BigDecimal(featureModel.getX()).compareTo(new BigDecimal(x)) == 0 
                        && new BigDecimal(featureModel.getY()).compareTo(new BigDecimal(y)) == 0 
                        && new BigDecimal(featureModel.getFloorNo()).compareTo(new BigDecimal(floorNo)) == 0){
                    LOG.debug("找到上一个定位点：x="+x+",y="+y+",z="+floorNo+"。原半径："+dis);
                    dis = dis.multiply(new BigDecimal(ratio)).divide(new BigDecimal(100));
                    LOG.debug("上一个点乘系数后半径："+dis);
                }
                // 如果特征点是上上一个定位点，特征半径要乘以大于1的系数correction
                else if(new BigDecimal(featureModel.getX()).compareTo(new BigDecimal(x1)) == 0 
                        && new BigDecimal(featureModel.getY()).compareTo(new BigDecimal(y1)) == 0 
                        && new BigDecimal(featureModel.getFloorNo()).compareTo(new BigDecimal(floorNo)) == 0){
                    LOG.debug("找到上上一个定位点：x1="+x1+",y1="+y1+",z="+floorNo+"。原半径："+dis);
                    dis = dis.multiply(new BigDecimal(correction));
                    LOG.debug("上上一个点乘系数后半径："+dis);
                }
                // 否则，特征半径要乘以对应的系数
                else if(new BigDecimal(featureModel.getFloorNo()).compareTo(new BigDecimal(floorNo)) == 0){
                    double disReal = Math.sqrt(Math.pow(Double.parseDouble(featureModel.getX()) - Double.parseDouble(x),2D)
                            + Math.pow(Double.parseDouble(featureModel.getY()) - Double.parseDouble(y),2D));                 
                    BigDecimal ratio = findRatio(disArray, ratioArray, new BigDecimal(disReal));
                    LOG.debug("距离："+disReal+"比例系数："+ratio);
                    dis = dis.multiply(ratio);
                    LOG.debug("乘系数后半径："+dis);
                }
            }
            // 将特征半径放入集合
            distance.put(id, dis);
        }
        
        // 获取半径最小的key
        int minId = getKeyOfMinValue(distance);
        
        // 判断新定位点与上次定位点之间的距离是否超过阈值
        PrruFeatureModel currentPoint = datas.get(minId); 
        LOG.debug("本次计算的定位坐标：x="+currentPoint.getX()+",y="+currentPoint.getY());
        if(new BigDecimal(x).compareTo(new BigDecimal(0)) != 0 && new BigDecimal(y).compareTo(new BigDecimal(0)) != 0) {
        	double disReal = Math.pow(Double.parseDouble(currentPoint.getX()) - Double.parseDouble(x),2D) 
                + Math.pow(Double.parseDouble(currentPoint.getY()) - Double.parseDouble(y),2D);
        	disReal = Math.sqrt(disReal);
        
        	LOG.debug("两次定位之间的距离："+disReal);
        	// 如果超过阈值，返回上次的定位点信息
        	try{
        		if(disReal > Double.parseDouble(prruDistance)){
        			LOG.debug("距离超过："+ prruDistance);
        			LOG.debug("数据库查询前：x="+x+",y="+y+",floorNo="+floorNo);
        			
        			currentPoint = prruSignalDao.getFeatureByPosition(
                		new BigDecimal(x), new BigDecimal(y), new BigDecimal(floorNo));
        			if (currentPoint != null) {
        				LOG.debug("数据库返回：x="+currentPoint.getX()+",y="+currentPoint.getY());
        			}
        		}
        	}catch(Exception e){
        		LOG.error(e);
        	}
        }
        
        if(currentPoint == null){
            currentPoint = datas.get(minId);
        }
        
        LOG.debug("返回的点：x="+currentPoint.getX()+",y="+currentPoint.getY()+",z="+currentPoint.getFloorNo());
        return currentPoint;
    }
    
    /** 
     * @Title: calcDataCheckValue 
     * @Description: 计算单条信号数据的dataCheckValue
     * @param datas：信号数据
     * @return 
     */
    private BigDecimal calcDataCheckValue(List<PrruSignalModel> datas){
        // 结果
        BigDecimal result = new BigDecimal(0);
        // 计数器
        int counter = 0;
        // 遍历，符合条件的求和
        for(PrruSignalModel item:datas){
            BigDecimal temp = item.getRsrp();
            if(temp.compareTo(THRESHOLD)>0){
                result = result.add(temp);
                counter++;
            }
        }
        // 返回平均值
        if(counter > 0){
            result = result.divide(new BigDecimal(counter),2);
        }
        return result;
    }
    
    /** 
     * @Title: getFeatureValue 
     * @Description: 从特征库里找出指定prru对应的特征值，没有的话使用默认值
     * @param gpp：柜框槽号
     * @param model：特征库
     * @return 
     */
    private BigDecimal getFeatureValue(String gpp, PrruFeatureModel model, BigDecimal defaultVal){
        // 结果
        BigDecimal result = null;
        // 特征值list
        List<PrruFeatureDetailModel> list = model.getFeatureValues();
        // 遍历list，取出对应gpp的特征值
        for(PrruFeatureDetailModel item:list){
            if(item.getGpp().equals(gpp)){
                result = item.getFeatureValue();
                break;
            }
        }
        return result;
    }
    
    /** 
     * @Title: getKeyOfMinValue 
     * @Description: 取指定类型的map里，value最小的对应的key 
     * @param data：源map
     * @return 
     */
    private int getKeyOfMinValue(Map<Integer, BigDecimal> data){
        // 排序
        List<Map.Entry<Integer, BigDecimal>> entryList = new ArrayList<Map.Entry<Integer, BigDecimal>>(data.entrySet());  
        Collections.sort(entryList, new Comparator<Map.Entry<Integer, BigDecimal>>() {  
            public int compare(Entry<Integer, BigDecimal> entry1,  
                    Entry<Integer, BigDecimal> entry2) {  
                int value1 = entry1.getValue().intValue();  
                int value2 = entry2.getValue().intValue();  
                
                return value1 - value2;  
            }  
        }); 
        
        // 返回最小值
        return entryList.get(0).getKey();
    }
    
    /** 
     * @Title: findRatio 
     * @Description: 找到距离对应的系数
     * @param disArray
     * @param ratioArray
     * @param distance
     * @return 
     */
    private BigDecimal findRatio(String[] disArray, String[] ratioArray, BigDecimal distance){
        BigDecimal result = new BigDecimal(0);
        for(int i = 0; i<disArray.length; i++){
            BigDecimal disTemp = new BigDecimal(disArray[i]);
            if(distance.compareTo(disTemp)> 0){
                result = new BigDecimal(ratioArray[i]);
            }
        }
        return result;
    }
    
    /** 
     * @Title: getAvgPrruSignals 
     * @Description: 按gpp去重
     * @param validSignals
     * @param validGpp 
     */
    private void getAvgPrruSignals(List<PrruSignalModel> signals, List<String> gpps){
        Map<Long, BigDecimal> rsrpAvg = new HashMap<Long, BigDecimal>();
        Map<Long, BigDecimal> rsrpCount = new HashMap<Long, BigDecimal>();
        gpps.clear();
        // 遍历
        for(int i = 0;i<signals.size();i++){
            // 时间戳
            long timestamp = signals.get(i).getTimestamp();
            gpps.add(signals.get(i).getGpp());
            // 不存在就创建
            if(rsrpAvg.get(timestamp) == null){
                rsrpAvg.put(timestamp, signals.get(i).getRsrp());
                rsrpCount.put(timestamp, new BigDecimal(1));
            }else{
                // 存在，则相加
                BigDecimal rsrpTemp = signals.get(i).getRsrp().add(rsrpAvg.get(timestamp));
                rsrpAvg.put(timestamp, rsrpTemp);
                rsrpCount.put(timestamp, rsrpCount.get(timestamp).add(new BigDecimal(1)));
            }
        }
        // 取平均值,计算checkValue
        Iterator<Entry<Long, BigDecimal>> it = rsrpAvg.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<Long, BigDecimal> entity = (Entry<Long, BigDecimal>) it.next();
            Long key = entity.getKey();
            BigDecimal value = entity.getValue();
            entity.setValue(value.divide(rsrpCount.get(key),2));
        }
        // 修正rsrp，得到data值
        for(PrruSignalModel p:signals){
            long timestamp = p.getTimestamp();
            p.setRsrp(p.getRsrp().subtract(rsrpAvg.get(timestamp)));
        }
        
        Map<String, PrruSignalModel> temp = new HashMap<String, PrruSignalModel>();
        
        // 遍历
        for(int i = 0;i<gpps.size();i++){
            String s = gpps.get(i);
            // gpp在map中不存在，则加入
            if(temp.get(s) == null){
                temp.put(s, signals.get(i));
                // 否则，将2个rsrp值求和取平均
            }else{
                BigDecimal secondRsrp = signals.get(i).getRsrp();
                BigDecimal firstRsrp = temp.get(s).getRsrp();
                temp.get(s).setRsrp(firstRsrp.add(secondRsrp).divide(new BigDecimal(2)));
            }
        }
        
        // 对入参重新赋值
        signals.clear();
        gpps.clear();
        //List<PrruSignalModel> tempList = new ArrayList<PrruSignalModel>();
        Iterator<Entry<String, PrruSignalModel>> itModel = temp.entrySet().iterator();  
        while(itModel.hasNext()){
            Entry<String,PrruSignalModel> entity = itModel.next();
            //tempList.add(entity.getValue());
            gpps.add(entity.getKey());
            signals.add(entity.getValue());
        }
        /* 测试算法
        // 按gpp从高到低排序
        Collections.sort(tempList, new Comparator<PrruSignalModel>() {  
            public int compare(PrruSignalModel d1,  
                    PrruSignalModel d2) {  
                BigDecimal value1 = d1.getRsrp();  
                BigDecimal value2 = d2.getRsrp();  
                
                return value2.compareTo(value1);  
            }  
        }); 
        
        // 取排序后的前gppCount个
        for(int i = 0; i < Integer.parseInt(gppCount); i++){
            // 如果tempList的个数不够，则提前结束循环
            if(i == tempList.size()){
                break;
            }

            signals.add(tempList.get(i));
            gpps.add(tempList.get(i).getGpp());
        }
        */
        LOG.debug("final rsrp:"+signals.toString());
    }
    
    private void getAveragePrruSignals(List<PrruSignalModel> signals, List<String> gpps){
        Map<String, Integer> rsrpCount = new HashMap<String, Integer>();
        
        Map<String, PrruSignalModel> temp = new HashMap<String, PrruSignalModel>();
        // 遍历
        for(int i = 0;i<gpps.size();i++){
            String s = gpps.get(i);
            // gpp在map中不存在，则加入
            if(temp.get(s) == null){
                temp.put(s, signals.get(i));
                rsrpCount.put(s, 1);
                // 否则，将n个rsrp值求和
            }else{
                BigDecimal secondRsrp = signals.get(i).getRsrp();
                BigDecimal firstRsrp = temp.get(s).getRsrp();
                temp.get(s).setRsrp(firstRsrp.add(secondRsrp));
                rsrpCount.put(s, rsrpCount.get(s)+1);
            }
        }
        
        // 对入参重新赋值
        signals.clear();
        gpps.clear();
        //List<PrruSignalModel> tempList = new ArrayList<PrruSignalModel>();
        Iterator<Entry<String, PrruSignalModel>> itModel = temp.entrySet().iterator();
        // 取平均
        while(itModel.hasNext()){
            Entry<String,PrruSignalModel> entity = itModel.next();
            //tempList.add(entity.getValue());
            gpps.add(entity.getKey());
            PrruSignalModel psm = entity.getValue();
            psm.setRsrp(psm.getRsrp().divide(new BigDecimal(rsrpCount.get(entity.getKey())), 2));
            signals.add(psm);
        }
        LOG.debug("final rsrp:"+signals.toString());
    }
    
    /** 
     * @Title: getAvgPrruSignals 
     * @Description: 按gpp去重
     * @param validSignals
     * @param validGpp 
     */
    private void getAvgPrruSignalsBak(List<PrruSignalModel> validSignals, List<String> validGpp){
        Map<String, PrruSignalModel> temp = new HashMap<String, PrruSignalModel>();
        // 遍历
        for(int i = 0;i<validGpp.size();i++){
            String s = validGpp.get(i);
            // gpp在map中不存在，则加入
            if(temp.get(s) == null){
                temp.put(s, validSignals.get(i));
                // 否则，将2个rsrp值求和取平均
            }else{
                BigDecimal secondRsrp = validSignals.get(i).getRsrp();
                BigDecimal firstRsrp = temp.get(s).getRsrp();
                temp.get(s).setRsrp(firstRsrp.add(secondRsrp).divide(new BigDecimal(2)));
            }
        }
        
        // 对入参重新赋值
        validSignals.clear();
        validGpp.clear();
        Iterator<Entry<String, PrruSignalModel>> it = temp.entrySet().iterator();  
        while(it.hasNext()){
            Entry<String,PrruSignalModel> entity = it.next();
            validGpp.add(entity.getKey());
            validSignals.add(entity.getValue());
        }
    }
    
    /**   
     * @Title: calcMinus   
     * @Description: 计算特征值的最小值
     * @param feature
     * @return：BigDecimal       
     * @throws   
     */ 
    private BigDecimal calcMinus(PrruFeatureModel feature){
        // 特征值列表
        List<PrruFeatureDetailModel> featureValues = feature.getFeatureValues();        
        // 排序
        Collections.sort(featureValues, new Comparator<PrruFeatureDetailModel>() {  
            public int compare(PrruFeatureDetailModel d1,  
                    PrruFeatureDetailModel d2) {  
                BigDecimal value1 = d1.getFeatureValue();  
                BigDecimal value2 = d2.getFeatureValue();  
                
                return value1.compareTo(value2);  
            }  
        }); 
        
        // 返回最小值
        return featureValues.get(0).getFeatureValue();
    }
    
    /** 
     * @Title: editGpp 
     * @Description: gpp添加enbId标识 
     * @param data 
     */
    private void editGpp(List<PrruSignalModel> data){
        for(PrruSignalModel p:data){
            p.setGpp(p.getEnbid() + "__" + p.getGpp());
        }
    }
}
