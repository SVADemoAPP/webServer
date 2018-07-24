package com.sva.web.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.util.JSONPObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sva.common.ConvertUtil;
import com.sva.common.HttpUtil;
import com.sva.common.TCPDesktopServer;
import com.sva.common.Util;
import com.sva.common.conf.Params;
import com.sva.dao.AccuracyDao;
import com.sva.dao.AreaDao;
import com.sva.dao.BZParamsDao;
import com.sva.dao.CodeDao;
import com.sva.dao.CommonDao;
import com.sva.dao.DynamicAccuracyDao;
import com.sva.dao.EstimateDao;
import com.sva.dao.GeofencingDao;
import com.sva.dao.LocationDao;
import com.sva.dao.LocationDelayDao;
import com.sva.dao.MapsDao;
import com.sva.dao.MessageDao;
import com.sva.dao.MessagePushDao;
import com.sva.dao.ParamDao;
import com.sva.dao.ParkingDao;
import com.sva.dao.PetAttributesDao;
import com.sva.dao.PetLocationDao;
import com.sva.dao.PhoneDao;
import com.sva.dao.PrruDao;
import com.sva.dao.PushMsgDao;
import com.sva.dao.RegisterDao;
import com.sva.dao.StaticAccuracyDao;
import com.sva.dao.SvaDao;
import com.sva.dao.TicketDao;
import com.sva.model.ACRModel;
import com.sva.model.AccuracyModel;
import com.sva.model.AreaModel;
import com.sva.model.CodeModel;
import com.sva.model.DynamicAccuracyModel;
import com.sva.model.LocationDelayModel;
import com.sva.model.LocationModel;
import com.sva.model.MapsModel;
import com.sva.model.MessageModel;
import com.sva.model.MessagePushModel;
import com.sva.model.MyModel;
import com.sva.model.ParamModel;
import com.sva.model.ParkinginformationModel;
import com.sva.model.PetAttributesModel;
import com.sva.model.PhoneModel;
import com.sva.model.PrruModel;
import com.sva.model.RegisterModel;
import com.sva.model.StaticAccuracyModel;
import com.sva.model.StoreModel;
import com.sva.model.SvaModel;
import com.sva.service.SubscriptionService;
import com.sva.model.PetLocationModel;
import com.sva.web.models.AccuracyApiModel;
import com.sva.web.models.ApiRequestModel;
import com.sva.web.models.DynamicAccuracyApiModel;
import com.sva.model.GeofencingModel;
import com.sva.web.models.MapMngModel;
import com.sva.web.models.StaticAccuracyApiModel;

@Controller
@RequestMapping(value = "/api")
public class ApiController
{

    private static final Logger LOG = Logger.getLogger(ApiController.class);

    @Autowired
    private LocationDao dao;

    @Autowired
    private MessagePushDao messagePushDao;

    @Autowired
    private LocationDelayDao delayDao;

    @Autowired
    private MapsDao daoMaps;

    @Autowired
    private MessageDao daoMsg;

    @Autowired
    private CodeDao daoCode;

    @Autowired
    private PhoneDao phoneDao;

    @Autowired
    private RegisterDao registerDao;

    @Autowired
    private AccuracyDao daoAccuracy;

    @Autowired
    private StaticAccuracyDao staticAccuracyDao;

    @Autowired
    private DynamicAccuracyDao dynamicAccuracyDao;

    @Autowired
    private EstimateDao daoEstimate;

    @Autowired
    private SvaDao svaDao;

    @Autowired
    private PrruDao prruDao;

    @Autowired
    private CommonDao comDao;

    @Autowired
    private ParamDao daoParam;

    @Autowired
    private AreaDao daoArea;

    @Autowired
    private BZParamsDao bzDao;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private GeofencingDao geofencingDao;
    
    @Autowired
    private PushMsgDao pushMsgDao;
    
    @Autowired
    private SubscriptionService service;
    
    @Autowired
    private PetAttributesDao petDao;
    
    @Autowired
    private PetLocationDao petLoction;
            
    @Autowired
    private ParkingDao parkDao;
    

    @Autowired
    private TicketDao ticketDao;

    @RequestMapping(value = "/getData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getLocationData(@RequestBody ApiRequestModel requestModel)
    {

        LOG.debug("api getData.ip:" + requestModel.getIp());

        if (StringUtils.isEmpty(requestModel.getIp()))
        {
            return null;
        }
        
        long paramUpdate = 0;       
        Collection<ParamModel> paramUpdates = daoParam.doquery();     
        for (ParamModel paramModel : paramUpdates)        
        {     
            paramUpdate = paramModel.getUpdateTime();     
        }
        long petTime = petLoction.getMaxPetTime();
        List<LocationModel> list = new ArrayList<LocationModel>(10);
        Collection<LocationModel> resultList = dao.queryLocationByUseId(ConvertUtil.convertMacOrIp(requestModel.getIp()));
        for (LocationModel l : resultList)
        {
            list.add(l);
        }
        List<MessageModel> messgeLis = null;
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        if (!resultList.isEmpty())
        {
            LocationModel loc = list.get(0);
            messgeLis = daoMsg.queryByLocation1(loc);
            modelMap.put("error", null);
            modelMap.put("data", loc);
            modelMap.put("message", messgeLis);
        }
        else
        {
            modelMap.put("data", null);
        }
        modelMap.put("paramUpdateTime", paramUpdate);
        modelMap.put("petTime", petTime);

        return modelMap;
    }
    //暂时没用
    @RequestMapping(value = "/getTikectData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getTikectData(
            @RequestBody ApiRequestModel requestModel)
    {

        if (StringUtils.isEmpty(requestModel.getIp()))
        {
            return null;
        }
        String userId = ConvertUtil.convertMacOrIp(requestModel.getIp());
        List<LocationModel> list = new ArrayList<LocationModel>(10);
        Collection<LocationModel> resultList = dao.queryLocationByUseId(userId);
        // 查询参数更新的时间
        List<String> ticketList = new ArrayList<String>();
        int len;
        String tikectPath = null;
        for (LocationModel l : resultList)
        {
            list.add(l);
        }

        Map<String, Object> modelMap = new HashMap<String, Object>(3);
        if (!resultList.isEmpty())
        {
            LocationModel loc = list.get(0);
            Collection<MessageModel> msgList1 = daoMsg.queryByLocation3(loc);
            for (MessageModel l : msgList1)
            {
                if (l.getTicketPath().length() > 6)
                {
                    ticketList.add(l.getTicketPath());
                }
            }
            len = ticketList.size();
            if (len > 0)
            {
                Random rand = new Random();
                int randNum = rand.nextInt(len);
                tikectPath = ticketList.get(randNum);

            }
        }
        modelMap.put("tikectPath", tikectPath);
        return modelMap;

    }
 
    @RequestMapping(value = "/getNewTikectData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getNewTikectData(
            @RequestParam("messageId") String messageId
            )
    {

        List<Map<String, Object>> list = ticketDao.getTiketPathByMsgId(messageId);
        Map<String, Object> modelMap = new HashMap<String, Object>(3); 
        String tikePath = null;
        int allSize = 0;
        int suiji = (int)(Math.random()*100+1);
        if (list.isEmpty())
        {
            return modelMap;
        }
        
        for (int i = 0; i < list.size(); i++)
        {
            String ticke = list.get(i).get("ticketPath").toString();
            int chance = Integer.parseInt(list.get(i).get("chances").toString());
            if (i==0)
            {
                allSize = allSize + chance;
                if (suiji<=chance)
                {
                    tikePath = ticke; 
                }
            }
            else
            {
                boolean temp = allSize<suiji&&suiji<=allSize+chance;
                if (temp)
                {
                    tikePath = ticke; 
                }else
                {
                    allSize = allSize + chance; 
                }
            }
        }
            
        modelMap.put("tikectPath",tikePath);
        return modelMap;

    }    

    @RequestMapping(value = "/subscription", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> subscription(
            @RequestParam("storeId") String storeId,
            @RequestParam("ip") String ip)
    {
        Collection<SvaModel> svaList = svaDao.queryByStoreId(storeId);
        boolean result = true;
        for (SvaModel sva : svaList)
        {
            service.subscribeSvaPhone(sva,ip);
        }        

        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", result);
        
        return modelMap;
    }
    
    @RequestMapping(value = "/unsubscribe", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> unsubscribe(
            @RequestParam("storeId") String storeId,
            @RequestParam("ip") String ip)
    {
        Collection<SvaModel> svaList = svaDao.queryByStoreId(storeId);
        boolean result = true;
        for (SvaModel sva : svaList)
        {
            service.unsubscribeSvaPhone(sva,ip);
        }    
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", result);
        return modelMap;
    }

    @RequestMapping(value = "/subscribePrru", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> subscribePrru(
            @RequestParam("storeId") String storeId,
            @RequestParam("ip") String ip)
    {
        Collection<SvaModel> svaList = svaDao.queryByStoreId(storeId);
        String token = null;
        HttpUtil capi = null;
        boolean result = true;
        String jsonStr = null;
        String charset = null;
        String url = null;
        String content = null;
        try
        {
            for (SvaModel sva : svaList)
            {

                url = "https://" + sva.getIp() + ':' + sva.getTokenPort()
                        + "/v3/auth/tokens";
                content = "{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\": {\"user\": {\"domain\": \"Api\",\"name\": \""
                        + sva.getUsername()
                        + "\",\"password\": \""
                        + sva.getPassword() + "\"}}}}}";
                charset = "UTF-8";
                LOG.debug("from ip:" + ip + ",getToken url:" + url);
                capi = new HttpUtil();

                token = capi.httpsPost(url, content, charset);
                url = "https://" + sva.getIp() + ':' + sva.getTokenPort()
                        + "/enabler/catalog/networkinfo/json/v1.0";
                content = "{\"APPID\":\"" + sva.getUsername()
                        + "\",\"infotype\":\"ransignal\",\"useridlist\":[\""
                        + ConvertUtil.convertMacOrIp(ip) + "\"]}";
                LOG.debug("from ip:" + ip + ",subscription url:" + url
                        + " content:" + content);
                jsonStr = capi.subscription(url, content, token, "POST");
                LOG.debug("subscription:" + jsonStr);
            }
        }
        catch (KeyManagementException e)
        {
            result = false;
            LOG.error("KeyManagementException.", e);
        }
        catch (NoSuchAlgorithmException e)
        {
            result = false;
            LOG.error("NoSuchAlgorithmException.", e);
        }
        catch (IOException e)
        {
            result = false;
            LOG.error("IOException.", e);
        }
        catch (Exception e)
        {
            result = false;
            LOG.error("Exception.", e);
        }

        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", result);
        return modelMap;
    }

    @RequestMapping(value = "/getMapDataByIp")
    @ResponseBody
    public Map<String, Object> getMapDataByIp()
    {
        LOG.info("getMapDataByIp");
        Thread desktopServerThread = new Thread(new TCPDesktopServer());
        desktopServerThread.start();

        Collection<MapsModel> resultList = daoMaps.doquery();
        Collection<AreaModel> areaList = daoArea.doquery();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        for (AreaModel area :areaList) 
        {
            area.setFloorNo(area.getMaps().getFloorNo());
        }

        modelMap.put("error", null);
        modelMap.put("data", resultList);
        modelMap.put("areaData", areaList);

        return modelMap;
    }
    
    /** 
     * @Title: getMessages 
     * @Description: 同通过店铺Id获取店铺范围的消息推送以及电子围栏消息
     * @param areaId 店铺Id
     * @return Map<String,Object>       
     * @throws 
     */
    @RequestMapping(value = "/getMessages", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getMessages(@RequestBody Map<String,List<String>> map)
    {
        Collection<MessageModel> messageList = new ArrayList<MessageModel>(10);
        Collection<GeofencingModel> geofencingList = new ArrayList<GeofencingModel>();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        List<String> msglists = new ArrayList<String>();
        try
        {
            List<String> areaList = map.get("areaId");
            if (!areaList.isEmpty())
            {
                for (int i = 0; i < areaList.size(); i++)
                {
                    String areaId = areaList.get(i);
                    messageList.addAll(daoMsg.getAllMessageDataByAreaIdNew(areaId));
                    msglists.addAll(daoMsg.getMsgId(areaId));
                    String zoneId = daoArea.getZoneIdByAreaId(areaId);
                    LOG.debug("getMessage: areaId:"+areaId+" zoneId"+zoneId);
                    geofencingList.addAll(geofencingDao.getGeofencingByZoneId(zoneId)); 
                }
            }
            
        }
        catch (Exception e)
        {
            LOG.error("getMessage error:",e);
        }
        modelMap.put("message", messageList);
        modelMap.put("geoFence", geofencingList);
        return modelMap;
    }    

    @RequestMapping(value = "/getMapData", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> getMapData()
    {
        List<MapMngModel> list = new ArrayList<MapMngModel>(10);
        Collection<MapsModel> resultList = new ArrayList<MapsModel>(10);
        MapMngModel mmm;
        for (MapsModel l : resultList)
        {
            mmm = new MapMngModel();
            mmm.setFloor(l.getFloor());
            mmm.setXo(l.getXo());
            mmm.setYo(l.getYo());
            mmm.setScale(l.getScale());
            mmm.setPath(l.getPath());
            mmm.setSvg(l.getSvg());
            list.add(mmm);
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);

        modelMap.put("error", null);
        modelMap.put("data", list);
        LOG.debug("getMapData end");
        return modelMap;
    }

    @RequestMapping(value = "/checkCode", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> checkCode(@RequestParam("name") String name,
            @RequestParam("password") String password)
    {
        int a = 0;
        Collection<CodeModel> model = daoCode.getData();
        boolean result = false;
        int flag = daoCode.checkIsValid(name, password);
        if (flag > a)
        {
            for (CodeModel m : model)
            {
                if (m.getName().equals(name)
                        && m.getPassword().equals(password))
                {
                    result = true;
                }
            }
        }

        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", result);
        return modelMap;
    }
    //以前接口，现在版本未用
    @RequestMapping(value = "/saveTestData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> saveTestData(@RequestBody AccuracyApiModel aam)
    {

        LOG.debug("api saveTestData.offset:" + aam.getOffset());
        int result = 0;
        String err = null;
        try
        {
            result = daoAccuracy.saveTestInfo(transformToBeanAccuracy(aam));
        }
        catch (Exception e)
        {
            LOG.error(e);
            err = e.toString();
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", err);
        modelMap.put("data", result);
        return modelMap;
    }

    // 静态精度测试接口
    @RequestMapping(value = "/staticSaveTestData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> staticSaveTestData(
            @RequestBody StaticAccuracyApiModel aam)
    {

        LOG.debug("api saveTestData.getAvgeOffset:" + aam.getAvgeOffset());
        int result = 0;
        String err = null;
        try
        {
            result = staticAccuracyDao.staticSaveTestInfo(transformToBeanStatic(aam));
        }
        catch (Exception e)
        {
            LOG.error(e);
            err = e.toString();
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", err);
        modelMap.put("data", result);
        return modelMap;
    }

    // 动态精度测试接口
    @RequestMapping(value = "/dynamicSaveTestData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> dynamicSaveTestData(
            @RequestBody DynamicAccuracyApiModel aam)
    {

        LOG.debug("api saveTestData.getAvgeOffset:" + aam.getAvgeOffset());
        int result = 0;
        String err = null;
        try
        {
            result = dynamicAccuracyDao.dynamicSaveTestInfo(transformToBeanDynamic(aam));
        }
        catch (Exception e)
        {
            LOG.error(e);
            err = e.toString();
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", err);
        modelMap.put("data", result);
        return modelMap;
    }

    @RequestMapping(value = "/pingSVA", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> pingSVA(@RequestParam("ip") String ip,
            @RequestParam("pingnumber") int pingnumber,
            @RequestParam("packtsize") int packtsize,
            @RequestParam("timeout") int timeout)
    {
        // 返回值
        Map<String, Object> result = new HashMap<String, Object>(2);
        LOG.info("ip:" + ip);
        LOG.info("pingnumber" + pingnumber);
        LOG.info("packtsize:" + packtsize);
        LOG.info("timeout:" + timeout);

        result = Util.ping(ip, pingnumber, packtsize, timeout);

        return result;
    }

    @RequestMapping(value = "/getEstimate", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getEstimate(
            @RequestParam("floorNo") String floorNo)
    {
        LOG.info("floorNo:" + floorNo);
        // 返回值
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        BigDecimal result = daoEstimate.getEstimate(floorNo);
        modelMap.put("error", null);
        modelMap.put("data", result);
        return modelMap;
    }

    @RequestMapping(value = "/getPrruInfo", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getPrruInfo(
            @RequestParam("floorNo") String floorNo)
    {
        Collection<PrruModel> resultList = prruDao.getPrruInfoByflooNo(floorNo);
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", resultList);
        return modelMap;
    }
    //暂时未实现
    @RequestMapping(value = "/getPrruSignal",method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getPrruSignal(@RequestParam("ip") String ip)
    {
        if (ip!=null) {
            return null;
        }
        String userId = ConvertUtil.convertMacOrIp(ip);
        long time = System.currentTimeMillis()-60000;
        List<Map<String, Object>> svaList = prruDao.getSignal(userId,time);
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", svaList);

        return modelMap;
    }

    @RequestMapping(value = "/getLineDataByHour", method = {RequestMethod.GET})
    @ResponseBody
    public JSONPObject getLineDataByHour(String callbackparam)
    {
        LOG.info("getLineDataByHour");
        // 返回值
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        // 时间
        String time = ConvertUtil.dateFormat(new Date(), Params.YYYYMMDD2) + "%'";
        List<Map<String, Object>> res = comDao.getDataToday(time);
        modelMap.put("error", null);
        modelMap.put("data", res);
        return new JSONPObject(callbackparam, modelMap);
    }

    @RequestMapping(value = "/getPieData", method = {RequestMethod.GET})
    @ResponseBody
    public JSONPObject getPieData(String callbackparam)
    {
        LOG.info("getPieData");
        // 返回值
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        List<Map<String, Object>> res = comDao.getStatisticTemp();
        modelMap.put("error", null);
        modelMap.put("data", res);
        return new JSONPObject(callbackparam, modelMap);
    }

    @RequestMapping(value = "/getDataParam", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> getTableDataParam()
    {
        LOG.info("ParamController:getTableData");
        Collection<ParamModel> resultList = daoParam.doquery();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        modelMap.put("data", resultList);
        return modelMap;
    }

    @RequestMapping(value = "/savaMessageData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> savaMessageData(
            @RequestBody MessagePushModel messagePush)
    {

        LOG.debug("api savaMessageData:");
        int result = 0;
        String err = null;
        messagePush.setUpdateTime(new Date().getTime());
        try
        {
            result = messagePushDao.savaMessagePush(messagePush);
        }
        catch (Exception e)
        {
            LOG.error(e);
            err = e.toString();
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", err);
        modelMap.put("data", result);
        return modelMap;
    }

    @RequestMapping(value = "/savaLocationDelay", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> savaLocationDelay(
            @RequestBody LocationDelayModel locationDelay)
    {

        LOG.debug("api savaLocationDelay:");
        int result = 0;
        String err = null;
        locationDelay.setUpdateTime(new Date().getTime());
        try
        {
            result = delayDao.savaMessagePush(locationDelay);
        }
        catch (Exception e)
        {
            LOG.error(e);
            err = e.toString();
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", err);
        modelMap.put("data", result);
        return modelMap;
    }

    // 获取所有的消息
    @RequestMapping(value = "/getAllMessageData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getAllMessageData()
    {

        Map<String, Object> modelMap = new HashMap<String, Object>(3);
        Collection<MessageModel> resultList = daoMsg.getAllMessageData();
        modelMap.put("data", resultList);

        return modelMap;
    }

    @RequestMapping(value = "/savePhone", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> savePhone(@RequestBody PhoneModel model)
    {

        LOG.debug("api savePhone:");
        long time = System.currentTimeMillis();
        model.setTimestamp(time);
        try
        {
            phoneDao.savePhone(model);
        }
        catch (Exception e)
        {
            LOG.error(e);
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        return modelMap;
    }

    // 注册
    @RequestMapping(value = "/saveRegister", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> savaRegister(@RequestBody RegisterModel model)
    {

        String phoneNumber = model.getPhoneNumber();
        List<RegisterModel> lis = registerDao.getDataByPhoneNumber(phoneNumber); 
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        if (!lis.isEmpty())
        {
            modelMap.put("error", "0");
            return modelMap;
        }
        LOG.debug("api savaRegister:");
        long time = System.currentTimeMillis();
        model.setStatus(0);
        model.setTimes(time);
        try
        {
            model.setUserId(ConvertUtil.convertMacOrIp(model.getUserId()));
            registerDao.saveRegister(model);
        }
        catch (Exception e)
        {
            LOG.error(e);
        }

        modelMap.put("error", null);
        return modelMap;
    }
    
    @RequestMapping(value = "/refreshRegister", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> refreshRegister(@RequestBody RegisterModel myModel)
    {

        LOG.debug("api refreshRegister:");
        long time = System.currentTimeMillis();
        String userid = ConvertUtil.convertMacOrIp(myModel.getUserId());
        myModel.setUserId(userid);
        myModel.setTimes(time);
        try
        {
            registerDao.refreshRegister(myModel);
        }
        catch (Exception e)
        {
            LOG.error(e);
        }
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        modelMap.put("error", null);
        return modelMap;
    }

    // 登陆验证
    @RequestMapping(value = "/loginCheck", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> loginCheck(@RequestBody RegisterModel model)
    {

        String baseChart = "abcdefghijklmnopqrstuvwxyz0123456789";
        int baseSize = baseChart.length();
        StringBuilder strb = new StringBuilder();
        Random ron = new Random();
        for (int i = 0; i < 10; i++)
        {
            int val = ron.nextInt(baseSize);
            strb.append(baseChart.charAt(val));
        }
        LOG.debug("api loginCheck:");
        int b = registerDao.checkLogin1(model);
        LOG.debug("passWord:" + model.getPassWord() + " phoneNumber:"
                + model.getPhoneNumber() + " b:" + b +" token:"+strb);
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        if (b > 0)
        {
            registerDao.setLoginStatus(strb.toString(), model.getPhoneNumber());
            modelMap.put("loginStatus", strb);
            modelMap.put("error", "1");
            return modelMap;
        }
        else
        {
            modelMap.put("error", "0");
            return modelMap;
        }

    }

    // 请求找人
    @RequestMapping(value = "/seekPeople", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> seekPeople(@RequestBody MyModel model)
    {
        String myPhone = model.getMyPhone();
        String otherPhone = model.getOtherPhone();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("api seekPeople:" + " myPhone:" + myPhone + " otherPhone:"
                + otherPhone);
        List<RegisterModel> lis = registerDao.getDataByPhoneNumber(otherPhone);
        List<RegisterModel> lis1;
        List<RegisterModel> lis2;
        List<RegisterModel> lis3;
        List<RegisterModel> lis4;
        List<RegisterModel> lis5;
        lis3 = registerDao.getDataByStatus(otherPhone, "2");
        if (!lis3.isEmpty())
        {
            modelMap.put("error", "4");
            return modelMap;
        }
        if (lis.isEmpty())
        {
            modelMap.put("error", "0");
            return modelMap;
        }
        
        LOG.debug("gai bian zhuang tai 1");
        // 线程没两秒查询一次是否接受
        for (int i = 0; i < 15; i++)
        {
            try
            {
                lis1 = registerDao.getDataByIsTrue(otherPhone, "1");
                lis2 = registerDao.getDataByIsTrue(otherPhone, "2");
                lis4 = registerDao.getDataByStatus(otherPhone, "2");
                lis5 = registerDao.getDataByStatus(otherPhone, "3");
                if (!lis4.isEmpty())
                {
                    modelMap.put("error", "4");
                    return modelMap;
                }
                if (!lis1.isEmpty())
                {
                    LOG.debug("error1");
                    modelMap.put("error", "1");
                    return modelMap;
                }
                if (!lis2.isEmpty())
                {
                    LOG.debug("error3");
                    modelMap.put("error", "3");
                    return modelMap;
                }
                if (!lis5.isEmpty())
                {
                    modelMap.put("error", "2");
                    return modelMap;
                }
                LOG.debug("seekPeople wait");
                Thread.sleep(1000);

            }
            catch (Exception e)
            {
                LOG.debug("seekPeople error");
            }
        }
        LOG.debug("seekPeople wei xiang ying!");
        modelMap.put("error", "2");
        return modelMap;

    }

    /** 
     * @Title: requestAnyTime 
     * @Description: 服务器推送，是否被找/是否有服务器推送
     * @param model
     * @return Map<String,Object>   
     * @throws 
     */
    @RequestMapping(value = "/requestAnyTime", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> requestAnyTime(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);

        // 服务器推送消息
        List<String> msgList = new ArrayList<String>();
        String userid = ConvertUtil.convertMacOrIp(model.getUserId());
        List<Map<String, Object>> messages = pushMsgDao.getMessageByUserId(userid);
        try{
            for(Map<String, Object> m : messages){
                // 将取过的推送消息删除，避免二次推送
                pushMsgDao.deleteMessageById(m.get("id").toString());
                // 存在多个消息的情况做拼接处理
                msgList.add(m.get("content").toString());
            }
        }catch(Exception e){
            LOG.error(e);
        }        
        modelMap.put("pushMsg", msgList);
        
        // 是否被别人找    
        String myPhone = model.getMyPhone();
        LOG.debug("api seekPeople:" + " myPhone:" + myPhone);
        
        List<RegisterModel> lis = registerDao.getDataByPhoneNumber(myPhone);
        if (!lis.isEmpty())
        {
            RegisterModel userInfo = lis.get(0);
            String loginStatus = userInfo.getLoginStatus();
            int status = userInfo.getStatus();
            switch(status){
            case 1:        // 有人找，返回找人电话号码
                modelMap.put("error", userInfo.getOtherPhone());
                break;
            case 2:        // 对方取消
                modelMap.put("error", "1");
                break;
            default:
                modelMap.put("error", "0");
                break;
            }
            modelMap.put("loginStatus",loginStatus);
        }else{
            modelMap.put("error", "0");
        }
        
        return modelMap;

    }

    @RequestMapping(value = "/updateStatus", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updateStatus(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("updateStatus :");
        String otherPhone = model.getOtherPhone();
        registerDao.updateIsTrue(otherPhone, "0");
        modelMap.put("error", "1");
        return modelMap;

    }

    @RequestMapping(value = "/updateStatus1", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updateStatus1(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("updateStatus1 :");
        String myPhone = model.getMyPhone();
        registerDao.updateStatusByPhoneNumber(myPhone, "0");
        modelMap.put("error", "1");
        return modelMap;

    }
    
    @RequestMapping(value = "/updateStatus4", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updateStatus4(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("updateStatus1 :");
        String otherPhone = model.getOtherPhone();
        registerDao.updateStatusByPhoneNumber(otherPhone, "0");
        modelMap.put("error", "1");
        return modelMap;

    }

    @RequestMapping(value = "/updateStatus2", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updateStatus2(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("updateStatus2 :");
        String myPhone = model.getMyPhone();
        registerDao.updateStatusByPhoneNumber(myPhone, "3");
        modelMap.put("error", "1");
        return modelMap;

    }

    @RequestMapping(value = "/updateStatus3", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updateStatus3(@RequestBody MyModel model)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("updateStatus3 :");
        String myPhone = model.getMyPhone();
        String otherPhone = model.getOtherPhone();
        registerDao.updataStatus(myPhone, otherPhone);
        modelMap.put("error", "1");
        return modelMap;

    }

    // 返回是否接受
    @RequestMapping(value = "/returnResult", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> returnResult(@RequestBody MyModel model)
    {
        String myPhone = model.getMyPhone();
        String result = model.getResult();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("api returnResult:" + " result:" + result + " myPhone:"
                + myPhone);
        List<String> lis = registerDao.getStatusByphoneNumber2(myPhone);
        if (!lis.isEmpty())
        {
            modelMap.put("data", "2");
            registerDao.updateStatusByPhoneNumber(myPhone, "0");
            return modelMap;
        }
        if ("1".equals(result))
        {
            registerDao.updateIsTrue(myPhone, "1");
            registerDao.updateStatusByPhoneNumber(myPhone, "0");
            modelMap.put("data", "1");
            return modelMap;
        }
        if ("0".equals(result))
        {
            registerDao.updateIsTrue(myPhone, "2");
            registerDao.updateStatusByPhoneNumber(myPhone, "0");
            modelMap.put("data", "0");
            return modelMap;
        }
        modelMap.put("data", "0");
        return modelMap;
    }

    // 找到人后返回结果
    @RequestMapping(value = "/cancalFind", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> cancalFind(@RequestBody MyModel model)
    {
        String otherPhone = model.getOtherPhone();
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("api cancalFind:" + " otherPhone:" + otherPhone);
        List<String> lis = registerDao.getStatusByIsTrue(otherPhone, "2");
        List<String> lis1 = registerDao.getStatusByIsTrue(otherPhone, "1");
        if (!lis.isEmpty())
        {
            modelMap.put("error", "0");
            registerDao.updateStatusByPhoneNumber(otherPhone, "0");
            registerDao.updateIsTrue(otherPhone, "0");
            return modelMap;
        }
        if (!lis1.isEmpty())
        {
            modelMap.put("error", "1");
            registerDao.updateStatusByPhoneNumber(otherPhone, "0");
            registerDao.updateIsTrue(otherPhone, "0");
            return modelMap;
        }
        registerDao.updateStatusByPhoneNumber(otherPhone, "2");
        modelMap.put("error", null);
        return modelMap;
    }

    // 两个人实时坐标
    @RequestMapping(value = "/twoPeoPleData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> twoPeoPleData(@RequestBody MyModel model)
    {
        String floorNo = model.getFloorNo();
        String myPhone = model.getMyPhone();
        String otherPhone = model.getOtherPhone();
        LocationModel myresult = null;
        LocationModel otherresult = null;
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        LOG.debug("api twoPeoPleData:" + " myPhone:" + myPhone + " otherPhone"
                + otherPhone);
        List<String> list = registerDao.getIpByUserName(myPhone);
        List<String> list1 = registerDao.getIpByUserName(otherPhone);
        String userID = null;
        String userID1 = null;
        for (int i = 0; i < list.size(); i++)
        {
            userID = list.get(i);
        }
        for (int i = 0; i < list1.size(); i++)
        {
            userID1 = list1.get(i);
        }
        LOG.debug("userID:" + userID + " userID1:" + userID1);
        List<LocationModel> lis = locationDao.doquery1(userID, floorNo);
        List<LocationModel> lis1 = locationDao.doquery1(userID1, floorNo);

        
        if (lis.size()>0) {
            myresult = lis.get(0);
        }
        if (lis1.size()>0) {
            otherresult = lis1.get(0);
        }
        modelMap.put("myDate", myresult);
        modelMap.put("otherDate",otherresult);
        return modelMap;

    }

    @RequestMapping(value = "/getShDate", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getShDate()
    {
        Calendar currentDate = new GregorianCalendar();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        List<Map<String, Object>> bzData = bzDao.getAllFloorNo2("1");
        String floorNo1 = null;
        String floorNo2 = null;
        String floorNo3 = null;
        String periodSel = null;
        double coefficient = 0;
        long bztime = 0;
        String startTime = null;
        if (bzData.size() > 0)
        {
            floorNo1 = bzData.get(0).get("floorNo1").toString();
            floorNo2 = bzData.get(0).get("floorNo2").toString();
            floorNo3 = bzData.get(0).get("floorNo3").toString();
            startTime = bzData.get(0).get("startTime").toString();
            periodSel = bzData.get(0).get("periodSel").toString();
            coefficient = Double.parseDouble(bzData.get(0).get("coefficient")
                    .toString());
            startTime = startTime.split(" ")[1].substring(0, 8);
        }
        if (coefficient == 0)
        {
            coefficient = 1.0;
        }

        List<AreaModel> ResultList1 = daoArea.selectAeareBaShow(floorNo1);
        List<AreaModel> ResultList2 = daoArea.selectAeareBaShow(floorNo2);
        List<AreaModel> ResultList3 = daoArea.selectAeareBaShow(floorNo3);
        long nowTime = System.currentTimeMillis()
                - (Integer.parseInt(periodSel)+1) * 60 * 1000;
        List<Object> areaData = new ArrayList<Object>();
        List<Object> areaData2 = new ArrayList<Object>();
        List<Object> areaData3 = new ArrayList<Object>();
        List<Object> areaData1 = null;
        Map<String, Object> map = null;
        Map<String, Object> allDataMap = new HashMap<String, Object>(2);

        String tableName = Params.LOCATION
                + ConvertUtil
                        .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
        String visitDay = ConvertUtil.dateFormat(currentDate.getTime(),
                "yyyy-MM-dd");
        // 当前时间拼接
        if (startTime != null)
        {
            String startDate = visitDay + " " + startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try
            {
                bztime = sdf.parse(startDate).getTime();
            }
            catch (Exception e)
            {
                LOG.debug("Time zhuanhuan error!");
            }
        }
        Map<String, Object> tquyu = null;
        double allTime1 = 0;
        double allTime2 = 0;
        double allTime3 = 0;
        long allTimes2 = 0; 
        long allTimes3 = 0; 
        for (int i = 0; i < ResultList1.size(); i++)
        {
            Map<String, Object> quyu1 = null;
            quyu1 = getAreaDate(areaData1, ResultList1.get(i),
                    ResultList1.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            allTime1 = allTime1 + Double.parseDouble((quyu1.get("average").toString()));
            if (quyu1.size() != 0)
            {
                areaData.add(quyu1);
            }
        }
        for (int i = 0; i < ResultList2.size(); i++)
        {
            Map<String, Object> quyu2 = null;
            quyu2 = getAreaDate(areaData1, ResultList2.get(i),
                    ResultList2.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            allTime2 = allTime2 +  Double.parseDouble(quyu2.get("average").toString());
            allTimes2 = Long.parseLong(quyu2.get("allTime").toString())+allTimes2;
            if (quyu2.size() != 0)
            {
                areaData2.add(quyu2);
            }
        }
        for (int i = 0; i < ResultList3.size(); i++)
        {
            Map<String, Object> quyu3 = null;
            quyu3 = getAreaDate(areaData1, ResultList3.get(i),
                    ResultList3.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            allTime3 = allTime3 +  Double.parseDouble((quyu3.get("average").toString()));
            allTimes3 = Long.parseLong(quyu3.get("allTime").toString())+allTimes3;
            if (quyu3.size() != 0)
            {
                areaData3.add(quyu3);
            }
        }
    
        allDataMap.put("item", areaData);
        allDataMap.put("item1", areaData2);
        allDataMap.put("item2", areaData3);

        int allUsers1 = 0;
        int allUsers2 = 0;
        int allUsers3 = 0;
 
        int allLeiji1 = 0;
        int allLeiji2 = 0;
        int allLeiji3 = 0;
        
        double allUser2 = 0;
        double allUser3 = 0;
        
        long time = System.currentTimeMillis() - Integer.parseInt(periodSel) * 60 * 1000;
        
        allLeiji1 = dao.queryHeatmap6(floorNo1, tableName).size();
        allLeiji2 = dao.queryHeatmap6(floorNo2, tableName).size();
        allLeiji3 = dao.queryHeatmap6(floorNo3, tableName).size();
        
        allUser2 = Math.ceil(allLeiji2 * coefficient);
        allUser3 = Math.ceil(allLeiji3 * coefficient);
        
        allUsers1 = (dao.queryHeatmap5(floorNo1, time, tableName)).size();
        allUsers2 = (dao.queryHeatmap5(floorNo2, time, tableName)).size();
        allUsers3 = (dao.queryHeatmap5(floorNo3, time, tableName)).size();

        allDataMap.put("coefficient", coefficient);
        allDataMap.put("allTime1", allTime1);
        DecimalFormat    df   = new DecimalFormat("######0.00");   
        String avgAllTime2 = allUser2 == 0 ? "0.00" : df.format(allTimes2/60000.0/allUser2);
        String avgAllTime3 = allUser3 == 0 ? "0.00" : df.format(allTimes3/60000.0/allUser3);
        if (Double.parseDouble(avgAllTime2)>=120)
        {
            avgAllTime2 = "120.23";
        }
        if (Double.parseDouble(avgAllTime3)>=120)
        {
            avgAllTime2 = "120.10";
        }
        allDataMap.put("allTime2", avgAllTime2);
        allDataMap.put("allTime3", avgAllTime3);
        allDataMap.put("User1", Math.ceil(allUsers1 * coefficient));
        allDataMap.put("User2",Math.ceil(allUsers2 * coefficient) );
        allDataMap.put("User3", Math.ceil(allUsers3 * coefficient));
        allDataMap.put("allUser1",Math.ceil(allLeiji1 * coefficient) );
        allDataMap.put("allUser2",allUser2 );
        allDataMap.put("allUser3", allUser3);


        return allDataMap;
    }

    private String getMinute(long time, int size)
    {
        if (size == 0 || time == 0)
        {
            return "0";
        }
        else
        {

            float b = (float) (time / 1000) / 60;
            float averageTime = b / size;
            DecimalFormat df = new DecimalFormat("0.00");
            return df.format(averageTime);
        }

    }

    private Map<String, Object> getAreaDate(List<Object> areaData,
            AreaModel areaMole, String areaName, String visitDay,
            Map<String, Object> quyu, Map<String, Object> map, long nowTime,
            double coefficient)
    {
        //获取表名
        Calendar currentDate = new GregorianCalendar();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        String tableName = Params.LOCATION
                + ConvertUtil
                        .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
        
        areaData = new ArrayList<Object>();
        quyu = new HashMap<String, Object>();
        map = new HashMap<String, Object>();

        int allSize = 0;

        int size = 0;
        long allTimes = 0;
        String times = null;
        quyu = getAverageTimeByAreaId(areaMole.getId(), visitDay);
        if (!quyu.isEmpty())
        {
            allSize = Integer.parseInt(quyu.get("count").toString());
            allTimes = Long.parseLong(quyu.get("timePeriod").toString());
            times = getMinute(
                    (Long.parseLong(quyu.get("timePeriod").toString())),
                    allSize);
        }
        if (Double.parseDouble(times)>120)
        {
            times = "120.21";
        }
        int allSize1 = daoArea.getAllArea(areaMole,tableName,nowTime);
        map = new HashMap<String, Object>();
        size = daoArea.getBaShowVisitUser(areaMole,tableName,String.valueOf(nowTime));
        map.put("name", areaName);
        map.put("current", Math.ceil(size * coefficient));
        map.put("cumulative", Math.ceil(allSize1 * coefficient));
        map.put("average", times);
        map.put("allTime", allTimes);

        return map;
    }
    
    @RequestMapping(value = "/getBaShow", method = { RequestMethod.POST })
    @ResponseBody
    public Map<String, Object> getBaShow() {
        Calendar currentDate = new GregorianCalendar();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        List<Map<String, Object>> bzData = bzDao.getBzAllData("1");
        String floorNo = null;
        String floorNo2 = null;
        String floorNo3 = null;
        String periodSel = null;
        double coefficient = 0;
        long bztime = 0;
        String startTime = null;
        long time = 0;
        if (bzData.size() > 0) {
            floorNo = bzData.get(0).get("floorNo").toString();
            floorNo2 = bzData.get(0).get("floorNo2").toString();
            floorNo3 = bzData.get(0).get("floorNo3").toString();
            startTime = bzData.get(0).get("startTime").toString();
            periodSel = bzData.get(0).get("periodSel").toString();
            coefficient = Double.parseDouble(bzData.get(0).get("coefficient")
                    .toString());
            startTime = startTime.split(" ")[1].substring(0, 8);
            long maxTimestamp = daoArea.getMaxTimestamp();
            if(maxTimestamp > 0)
            {
                time = maxTimestamp - Integer.parseInt(periodSel)
                        * 60 * 1000;
            }
            else
            {
                time = System.currentTimeMillis() - Integer.parseInt(periodSel)
                    * 60 * 1000;
            }
        }
        if (coefficient == 0) {
            coefficient = 1.0;
        }

        List<AreaModel> ResultList = daoArea.selectAeareBaShow(floorNo);
        List<Object> areaData = new ArrayList<Object>();
        Map<String, Object> map = null;
        Map<String, Object> allDataMap = new HashMap<String, Object>(2);
        

        String tableName = Params.LOCATION
                + ConvertUtil
                        .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
        String visitDay = ConvertUtil.dateFormat(currentDate.getTime(),
                "yyyy-MM-dd");
        //当前时间拼接
        if (startTime!=null) {
            String startDate = visitDay+" "+startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                bztime = sdf.parse(startDate).getTime();
            } catch (Exception e) {
                LOG.debug("Time zhuanhuan error!");
            }
        }
        Map<String, Object> quyu = null;
        Map<String, Object> allQuyu = null;
        Map<String, Object> allQuyu2 = null;
        Map<String, Object> allQuyu3 = null;
        
        for (int i = 0; i < ResultList.size(); i++) {
            int allSize = 0;
            int size = 0;
            String areaId = ResultList.get(i).getId();
            String times = null;
            quyu = getAverageTimeByAreaId1(areaId, bztime);
            if (!quyu.isEmpty()) {
                allSize =  Integer.parseInt(quyu.get("count").toString());
                times = getMinute(Long.parseLong( quyu.get("timePeriod").toString()), allSize);
            }
            size = daoArea.getBaShowVisitUser(ResultList.get(i),tableName,String.valueOf(time));
            map = new HashMap<String, Object>();
            map.put("name", ResultList.get(i).getAreaName());
            map.put("current", Math.ceil(size * coefficient));
            map.put("cumulative",Math.ceil(allSize * coefficient));
            map.put("average", times);
            areaData.add(map);
        }
        allDataMap.put("item", areaData);

        String allAverageTime = null;
        int allUsers = 0;
        String areaTime = null;
        String allAverageTime2 = null;
        int allUsers2 = 0;
        String areaTime2 = null;
        String allAverageTime3 = null;
        int allUsers3 = 0;
        String areaTime3 = null;
        allQuyu = getAllAverageTimeByAreaId2(floorNo, bztime, tableName);
        allQuyu2 = getAllAverageTimeByAreaId2(floorNo2, bztime, tableName);
        allQuyu3 = getAllAverageTimeByAreaId2(floorNo3, bztime, tableName);
        if (!allQuyu.isEmpty()) {
            allAverageTime = allQuyu.get("timePeriod").toString();

            allUsers = Integer.parseInt( allQuyu.get("count").toString());
            areaTime = getMinute(Long.valueOf(allAverageTime), allUsers);
        }
        if (!allQuyu2.isEmpty()) {
            allAverageTime2 = allQuyu2.get("timePeriod").toString();

            allUsers2 =  Integer.parseInt(allQuyu2.get("count").toString());
            areaTime2 = getMinute(Long.valueOf(allAverageTime2), allUsers2);
        }
        if (!allQuyu3.isEmpty()) {
            allAverageTime3 = allQuyu3.get("timePeriod").toString();

            allUsers3 = Integer.parseInt( allQuyu3.get("count").toString());
            areaTime3 = getMinute(Long.valueOf(allAverageTime3), allUsers3);
        }
        // 用于隐藏热力图
        allDataMap.put("coefficient", coefficient);
        allDataMap.put("allTime", areaTime);
        allDataMap.put("allUser", Math.ceil(allUsers * coefficient));
        allDataMap.put("allTime2", areaTime2);
        allDataMap.put("allUser2", allUsers2);
        allDataMap.put("allTime3", areaTime3);
        allDataMap.put("allUser3", allUsers3);
        return allDataMap;
    }
    
    
    @RequestMapping(value = "/getBaShowData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getBaShowData()
    {
        Calendar currentDate = new GregorianCalendar();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        List<Map<String, Object>> bzData = bzDao.getAllFloorNo("1");
        String floorNo1 = null;
        String floorNo2 = null;
        String floorNo3 = null;
        String floorNo4 = null;
        String floorNo5 = null;
        String floorNo6 = null;
        String floorNo7 = null;
        String floorNo8 = null;
        String periodSel = null;
        double coefficient = 0;
        long bztime = 0;
        String startTime = null;
        long time = 0;
        if (bzData.size() > 0)
        {
            floorNo1 = bzData.get(0).get("floorNo1").toString();
            floorNo2 = bzData.get(0).get("floorNo2").toString();
            floorNo3 = bzData.get(0).get("floorNo3").toString();
            floorNo4 = bzData.get(0).get("floorNo4").toString();
            floorNo5 = bzData.get(0).get("floorNo5").toString();
            floorNo6 = bzData.get(0).get("floorNo6").toString();
            floorNo7 = bzData.get(0).get("floorNo7").toString();
            floorNo8 = bzData.get(0).get("floorNo8").toString();
            startTime = bzData.get(0).get("startTime").toString();
            periodSel = bzData.get(0).get("periodSel").toString();
            coefficient = Double.parseDouble(bzData.get(0).get("coefficient")
                    .toString());
            startTime = startTime.split(" ")[1].substring(0, 8);
            long maxTimestamp = daoArea.getMaxTimestamp();
            if (maxTimestamp > 0)
            {
                time = maxTimestamp - Integer.parseInt(periodSel) * 60 * 1000;
            }
            else
            {
                time = System.currentTimeMillis() - Integer.parseInt(periodSel)
                        * 60 * 1000;
            }
        }
        if (coefficient == 0)
        {
            coefficient = 1.0;
        }

        List<AreaModel> ResultList1 = daoArea.selectAeareBaShow(floorNo1);
        List<AreaModel> ResultList2 = daoArea.selectAeareBaShow(floorNo2);
        List<AreaModel> ResultList3 = daoArea.selectAeareBaShow(floorNo3);
        List<AreaModel> ResultList4 = daoArea.selectAeareBaShow(floorNo4);
        List<AreaModel> ResultList5 = daoArea.selectAeareBaShow(floorNo5);
        List<AreaModel> ResultList6 = daoArea.selectAeareBaShow(floorNo6);
        List<AreaModel> ResultList7 = daoArea.selectAeareBaShow(floorNo7);
        List<AreaModel> ResultList8 = daoArea.selectAeareBaShow(floorNo8);
        long nowTime = System.currentTimeMillis()
                - (Integer.parseInt(periodSel) + 1) * 60 * 1000;
        List<Object> areaData = new ArrayList<Object>();
        List<Object> areaData1 = null;
        Map<String, Object> map = null;
        Map<String, Object> allDataMap = new HashMap<String, Object>(2);

        String tableName = Params.LOCATION
                + ConvertUtil
                        .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
        String visitDay = ConvertUtil.dateFormat(currentDate.getTime(),
                "yyyy-MM-dd");
        // 当前时间拼接
        if (startTime != null)
        {
            String startDate = visitDay + " " + startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try
            {
                bztime = sdf.parse(startDate).getTime();
            }
            catch (Exception e)
            {
                LOG.debug("Time zhuanhuan error!");
            }
        }
        Map<String, Object> tquyu = null;
        for (int i = 0; i < ResultList1.size(); i++)
        {
            Map<String, Object> quyu1 = null;
            quyu1 = getAreaDate(areaData1, ResultList1.get(i),
                    ResultList1.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu1.size() != 0)
            {
                areaData.add(quyu1);
            }
        }
        for (int i = 0; i < ResultList2.size(); i++)
        {
            Map<String, Object> quyu2 = null;
            quyu2 = getAreaDate(areaData1, ResultList2.get(i),
                    ResultList2.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu2.size() != 0)
            {
                areaData.add(quyu2);
            }
        }
        for (int i = 0; i < ResultList3.size(); i++)
        {
            Map<String, Object> quyu3 = null;
            quyu3 = getAreaDate(areaData1, ResultList3.get(i),
                    ResultList3.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu3.size() != 0)
            {
                areaData.add(quyu3);
            }
        }
        for (int i = 0; i < ResultList4.size(); i++)
        {
            Map<String, Object> quyu4 = null;
            quyu4 = getAreaDate(areaData1, ResultList4.get(i),
                    ResultList4.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu4.size() != 0)
            {
                areaData.add(quyu4);
            }
        }
        for (int i = 0; i < ResultList5.size(); i++)
        {
            Map<String, Object> quyu5 = null;
            quyu5 = getAreaDate(areaData1, ResultList5.get(i),
                    ResultList5.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu5.size() != 0)
            {
                areaData.add(quyu5);
            }
        }
        for (int i = 0; i < ResultList6.size(); i++)
        {
            Map<String, Object> quyu6 = null;
            quyu6 = getAreaDate(areaData1, ResultList6.get(i),
                    ResultList6.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu6.size() != 0)
            {
                areaData.add(quyu6);
            }
        }
        for (int i = 0; i < ResultList7.size(); i++)
        {
            Map<String, Object> quyu7 = null;
            quyu7 = getAreaDate(areaData1, ResultList7.get(i),
                    ResultList7.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu7.size() != 0)
            {
                areaData.add(quyu7);
            }
        }
        for (int i = 0; i < ResultList8.size(); i++)
        {
            Map<String, Object> quyu8 = null;
            quyu8 = getAreaDate(areaData1, ResultList8.get(i),
                    ResultList8.get(i).getAreaName(), visitDay, tquyu, map,
                    nowTime, coefficient);
            if (quyu8.size() != 0)
            {
                areaData.add(quyu8);
            }
        }
        // Map<String, Object> quyu1 = getAreaDate(areaData1, ResultList2,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu2 = getAreaDate(areaData1, ResultList3,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu3 = getAreaDate(areaData1, ResultList4,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu4 = getAreaDate(areaData1, ResultList5,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu5 = getAreaDate(areaData1, ResultList6,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu6 = getAreaDate(areaData1, ResultList7,
        // visitDay, tquyu, map, nowTime, coefficient);
        // Map<String, Object> quyu7 = getAreaDate(areaData1, ResultList8,
        // visitDay, tquyu, map, nowTime, coefficient);

        // if (quyu1.size() != 0)
        // {
        // areaData.add(quyu1);
        // }
        // if (quyu2.size() != 0)
        // {
        // areaData.add(quyu2);
        // }
        // if (quyu3.size() != 0)
        // {
        // areaData.add(quyu3);
        // }
        // if (quyu4.size() != 0)
        // {
        // areaData.add(quyu4);
        // }
        // if (quyu.size() != 0)
        // {
        // areaData.add(quyu5);
        // }
        // if (quyu6.size() != 0)
        // {
        // areaData.add(quyu6);
        // }
        // if (quyu7.size() != 0)
        // {
        // areaData.add(quyu7);
        // }

        allDataMap.put("item", areaData);

        int allUsers1 = 0;
        int allUsers2 = 0;
        int allUsers3 = 0;
        int allUsers4 = 0;
        int allUsers5 = 0;
        int allUsers6 = 0;
        int allUsers7 = 0;
        int allUsers8 = 0;

        allUsers1 = daoArea.getAllPeoples(floorNo1, tableName, bztime);
        allUsers2 = daoArea.getAllPeoples(floorNo2, tableName, bztime);
        allUsers3 = daoArea.getAllPeoples(floorNo3, tableName, bztime);
        allUsers4 = daoArea.getAllPeoples(floorNo4, tableName, bztime);
        allUsers5 = daoArea.getAllPeoples(floorNo5, tableName, bztime);
        allUsers6 = daoArea.getAllPeoples(floorNo6, tableName, bztime);
        allUsers7 = daoArea.getAllPeoples(floorNo7, tableName, bztime);
        allUsers8 = daoArea.getAllPeoples(floorNo8, tableName, bztime);

        // 用于隐藏热力图
        allDataMap.put("coefficient", coefficient);
        allDataMap.put("allUser1", Math.ceil(allUsers1 * coefficient));
        allDataMap.put("allUser2", Math.ceil(allUsers2 * coefficient));
        allDataMap.put("allUser3", Math.ceil(allUsers3 * coefficient));
        allDataMap.put("allUser4", Math.ceil(allUsers4 * coefficient));
        allDataMap.put("allUser5", Math.ceil(allUsers5 * coefficient));
        allDataMap.put("allUser6", Math.ceil(allUsers6 * coefficient));
        allDataMap.put("allUser7", Math.ceil(allUsers7 * coefficient));
        allDataMap.put("allUser8", Math.ceil(allUsers8 * coefficient));

        return allDataMap;
    }
    
    
    /** 
     * @Title: getShDateJing 
     * @Description: 菁蓉整数据获取
     * @return Map<String,Object>       
     * @throws 
     */
    @RequestMapping(value = "/getShDateJing", method = {RequestMethod.POST})
@ResponseBody
public Map<String, Object> getShDateJing()
{
    Calendar currentDate = new GregorianCalendar();
    currentDate.set(Calendar.HOUR_OF_DAY, 0);
    currentDate.set(Calendar.MINUTE, 0);
    currentDate.set(Calendar.SECOND, 0);
    List<Map<String, Object>> bzData = bzDao.getAllFloorNoJing("1");
    String floorNo1 = null;
    String floorNo2 = null;
    String floorNo3 = null;
    String floorNo4 = null;
    String periodSel = null;
    double coefficient = 0;
    long bztime = 0;
    String startTime = null;
    long time = 0;
    if (bzData.size() > 0)
    {
        floorNo1 = bzData.get(0).get("floorNo1").toString();
        floorNo2 = bzData.get(0).get("floorNo2").toString();
        floorNo3 = bzData.get(0).get("floorNo3").toString();
        floorNo4= bzData.get(0).get("floorNo4").toString();
        startTime = bzData.get(0).get("startTime").toString();
        periodSel = bzData.get(0).get("periodSel").toString();
        coefficient = Double.parseDouble(bzData.get(0).get("coefficient")
                .toString());
        startTime = startTime.split(" ")[1].substring(0, 8);
    }
    if (coefficient == 0)
    {
        coefficient = 1.0;
    }

    List<AreaModel> ResultList1 = daoArea.selectAeareBaShow(floorNo1);
    List<AreaModel> ResultList2 = daoArea.selectAeareBaShow(floorNo2);
    List<AreaModel> ResultList3 = daoArea.selectAeareBaShow(floorNo3);
    List<AreaModel> ResultList4 = daoArea.selectAeareBaShow(floorNo4);
    long nowTime = System.currentTimeMillis()
            - (Integer.parseInt(periodSel)+1) * 60 * 1000;
    List<Object> areaData = new ArrayList<Object>();
    List<Object> areaData2 = new ArrayList<Object>();
    List<Object> areaData3 = new ArrayList<Object>();
    List<Object> areaData4 = new ArrayList<Object>();
    List<Object> areaData1 = new ArrayList<Object>();
    Map<String, Object> map = null;
    Map<String, Object> allDataMap = new HashMap<String, Object>(2);

    String tableName = Params.LOCATION
            + ConvertUtil
                    .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
    String visitDay = ConvertUtil.dateFormat(currentDate.getTime(),
            "yyyy-MM-dd");
    // 当前时间拼接
    if (startTime != null)
    {
        String startDate = visitDay + " " + startTime;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            bztime = sdf.parse(startDate).getTime();
        }
        catch (Exception e)
        {
            LOG.debug("Time zhuanhuan error!");
        }
    }
    Map<String, Object> tquyu = null;
    double allTime1 = 0;
    double allTime2 = 0;
    double allTime3 = 0;
    double allTime4 = 0;
    long allTimes1 = 0; 
    long allTimes2 = 0; 
    long allTimes3 = 0; 
    long allTimes4 = 0; 
    for (int i = 0; i < ResultList1.size(); i++)
    {
        Map<String, Object> quyu2 = null;
        quyu2 = getAreaDate(areaData1, ResultList1.get(i),
                ResultList1.get(i).getAreaName(), visitDay, tquyu, map,
                nowTime, coefficient);
        allTime1 = allTime1 +  Double.parseDouble(quyu2.get("average").toString());
        allTimes1 = Long.parseLong(quyu2.get("allTime").toString())+allTimes1;
        if (quyu2.size() != 0)
        {
            areaData.add(quyu2);
        }
    }
    for (int i = 0; i < ResultList2.size(); i++)
    {
        Map<String, Object> quyu2 = null;
        quyu2 = getAreaDate(areaData1, ResultList2.get(i),
                ResultList2.get(i).getAreaName(), visitDay, tquyu, map,
                nowTime, coefficient);
        allTime2 = allTime2 +  Double.parseDouble(quyu2.get("average").toString());
        allTimes2 = Long.parseLong(quyu2.get("allTime").toString())+allTimes2;
        if (quyu2.size() != 0)
        {
            areaData2.add(quyu2);
        }
    }
    for (int i = 0; i < ResultList3.size(); i++)
    {
        Map<String, Object> quyu3 = null;
        quyu3 = getAreaDate(areaData1, ResultList3.get(i),
                ResultList3.get(i).getAreaName(), visitDay, tquyu, map,
                nowTime, coefficient);
        allTime3 = allTime3 +  Double.parseDouble((quyu3.get("average").toString()));
        allTimes3 = Long.parseLong(quyu3.get("allTime").toString())+allTimes3;
        if (quyu3.size() != 0)
        {
            areaData3.add(quyu3);
        }
    }
    for (int i = 0; i < ResultList4.size(); i++)
    {
        Map<String, Object> quyu3 = null;
        quyu3 = getAreaDate(areaData4, ResultList4.get(i),
            ResultList4.get(i).getAreaName(), visitDay, tquyu, map,
            nowTime, coefficient);
        allTime4 = allTime4 +  Double.parseDouble((quyu3.get("average").toString()));
        allTimes4 = Long.parseLong(quyu3.get("allTime").toString())+allTimes4;
        if (quyu3.size() != 0)
        {
            areaData4.add(quyu3);
        }
    }

    allDataMap.put("item", areaData);
    allDataMap.put("item1", areaData2);
    allDataMap.put("item2", areaData3);
    allDataMap.put("item4", areaData4);

    int allUsers1 = 0;
    int allUsers2 = 0;
    int allUsers3 = 0;
    int allUsers4 = 0;

    int allLeiji1 = 0;
    int allLeiji2 = 0;
    int allLeiji3 = 0;
    int allLeiji4 = 0;
    
    double allUser1 = 0;
    double allUser2 = 0;
    double allUser3 = 0;
    double allUser4 = 0;
    
    long timePeriod = System.currentTimeMillis() - Integer.parseInt(periodSel) * 60 * 1000;
    
    allLeiji1 = dao.queryHeatmap6(floorNo1, tableName).size();
    allLeiji2 = dao.queryHeatmap6(floorNo2, tableName).size();
    allLeiji3 = dao.queryHeatmap6(floorNo3, tableName).size();
    allLeiji4 = dao.queryHeatmap6(floorNo4, tableName).size();
    
    allUser1 = Math.ceil(allLeiji1 * coefficient);
    allUser2 = Math.ceil(allLeiji2 * coefficient);
    allUser3 = Math.ceil(allLeiji3 * coefficient);
    allUser4 = Math.ceil(allLeiji4 * coefficient);
    
    allUsers1 = (dao.queryHeatmap5(floorNo1, timePeriod, tableName)).size();
    allUsers2 = (dao.queryHeatmap5(floorNo2, timePeriod, tableName)).size();
    allUsers3 = (dao.queryHeatmap5(floorNo3, timePeriod, tableName)).size();
    allUsers4 = (dao.queryHeatmap5(floorNo4, timePeriod, tableName)).size();

    allDataMap.put("coefficient", coefficient);
    allDataMap.put("allTime1", allTime1);
    DecimalFormat    df   = new DecimalFormat("######0.00");   
    String avgAllTime1 = allUser1 == 0 ? "0.00" : df.format(allTimes1/60000.0/allUser1);
    String avgAllTime2 = allUser2 == 0 ? "0.00" : df.format(allTimes2/60000.0/allUser2);
    String avgAllTime3 = allUser3 == 0 ? "0.00" : df.format(allTimes3/60000.0/allUser3);
    String avgAllTime4 = allUser4 == 0 ? "0.00" : df.format(allTimes4/60000.0/allUser4);
    if (Double.parseDouble(avgAllTime2)>=120)
    {
        avgAllTime2 = "120.23";
    }
    if (Double.parseDouble(avgAllTime3)>=120)
    {
        avgAllTime2 = "120.10";
    }
    allDataMap.put("allTime1", avgAllTime1);
    allDataMap.put("allTime2", avgAllTime2);
    allDataMap.put("allTime3", avgAllTime3);
    allDataMap.put("allTime4", avgAllTime4);
    allDataMap.put("User1", Math.ceil(allUsers1 * coefficient));
    allDataMap.put("User2",Math.ceil(allUsers2 * coefficient) );
    allDataMap.put("User3", Math.ceil(allUsers3 * coefficient));
    allDataMap.put("User4", Math.ceil(allUsers4 * coefficient));
    allDataMap.put("allUser1",allUser1);
    allDataMap.put("allUser2",allUser2 );
    allDataMap.put("allUser3", allUser3);
    allDataMap.put("allUser4", allUser4);


    return allDataMap;
}
    
    //修改宠物状态接口
    @RequestMapping(value = "/updatePetStatus", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> updatePetStatus(@RequestBody PetAttributesModel mymodel)
    {
        Map<String, Object> map = new HashMap<String, Object>(2);
        int id = mymodel.getId();
        int result = 0 ;
        String sql = "update petlocation set status=1 where id = "+id;
        result = comDao.doUpdate(sql);
        long time = System.currentTimeMillis();
        petDao.updatePetTimeByCaputrue(time);
        map.put("error", result);

        return map;
        
        
    }
    
    //获取所有的宠物信息
    @RequestMapping(value = "/getAllPetData", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getAllPetData(@RequestBody PetAttributesModel model)
    {
        Map<String, Object> map = new HashMap<String, Object>(2);
        Collection<PetLocationModel> pet= null;
        String floorNo = model.getFloorNo();

        pet = petLoction.getPetDataByPosition(floorNo);
        map.put("data", pet);

        return map;
        
        
    }
    
    
    
    
    
@RequestMapping(value = "/getDataByFloorNo", method = {RequestMethod.GET})
@ResponseBody
public Map<String, Object> getDataByFloorNo(@RequestParam("floorNo") String floorNo,@RequestParam(value = "time", required = false) String times)
{
    Calendar currentDate = new GregorianCalendar();
    currentDate.set(Calendar.HOUR_OF_DAY, 0);
    currentDate.set(Calendar.MINUTE, 0);
    currentDate.set(Calendar.SECOND, 0);
    String periodSel = null;
    if (times==null) {
        periodSel = "5";
    }else
    {
        periodSel = times;
    }
    String floorNo1 = floorNo;

    double coefficient = 1;
    long bztime = 0;
    String startTime = null;
    long time = 0;


    List<AreaModel> ResultList1 = daoArea.selectAeareBaShow(floorNo1);

    long nowTime = System.currentTimeMillis()
            - (Integer.parseInt(periodSel)+1) * 60 * 1000;
    List<Object> areaData = new ArrayList<Object>();

    List<Object> areaData1 = new ArrayList<Object>();
    Map<String, Object> map = null;
    Map<String, Object> allDataMap = new HashMap<String, Object>(2);

    String visitDay = ConvertUtil.dateFormat(currentDate.getTime(),
            "yyyy-MM-dd");
    // 当前时间拼接
        String startDate = visitDay + " " + "00:00:00";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            bztime = sdf.parse(startDate).getTime();
        }
        catch (Exception e)
        {
            LOG.debug("Time zhuanhuan error!");
        }
    Map<String, Object> tquyu = null;
    double allTime1 = 0;

    long allTimes1 = 0; 

    for (int i = 0; i < ResultList1.size(); i++)
    {
        Map<String, Object> quyu2 = null;
        quyu2 = getAreaDate(areaData1, ResultList1.get(i),
                ResultList1.get(i).getAreaName(), visitDay, tquyu, map,
                nowTime, coefficient);
        allTime1 = allTime1 +  Double.parseDouble(quyu2.get("average").toString());
        allTimes1 = Long.parseLong(quyu2.get("allTime").toString())+allTimes1;
        if (quyu2.size() != 0)
        {
            areaData.add(quyu2);
        }
    }


    allDataMap.put("item", areaData);


    int allUsers1 = 0;


    int allLeiji1 = 0;

    
    double allUser1 = 0;

    
    String tableName = Params.LOCATION
            + ConvertUtil
                    .dateFormat(currentDate.getTime(), Params.YYYYMMDD);
    allLeiji1 = dao.queryHeatmap6(floorNo1,tableName).size();

    
    allUser1 = Math.ceil(allLeiji1 * coefficient);

    
    allUsers1 = (dao.queryHeatmap5(floorNo1, Integer.parseInt(periodSel),tableName)).size();

    allDataMap.put("allTime1", allTime1);
    DecimalFormat    df   = new DecimalFormat("######0.00");   
    String avgAllTime1 = allUser1 == 0 ? "0.00" : df.format(allTimes1/60000.0/allUser1);


    allDataMap.put("average", avgAllTime1);

    allDataMap.put("user", Math.ceil(allUsers1 * coefficient));

    allDataMap.put("allUser",allUser1);

    return allDataMap;
}
    //获取所有的地图信息
    @RequestMapping(value = "/getAllFloorNo", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> getAllFloorNo()
    {
        Map<String, Object> map = new HashMap<String, Object>(2);
        Collection<MapsModel> model= null;
        model = daoMaps.doquery();
        map.put("data", model);
        map.put("error", null);
        return map;
        
    }

    private Map<String, Object> getAverageTimeByAreaId1(String areaId, long time){
        List<Map<String, Object>> res = daoArea.getAverageTimeByAreaId1(areaId,time);
        long timePeriod = 0;
        long tempTime = 0;
        for (int i = 0; i < res.size(); i++) {
            long timeNow = Long.parseLong( res.get(i).get("timestamp").toString());
            long timeBegin = Long.parseLong(res.get(i).get("time_begin").toString());
            if (timeBegin>time) {
                tempTime = timeBegin;
            }else
            {
                tempTime = time;
            }
            /** 添加需求当每个人统计的时长超过2小时则按两小时计算*/
            long timePeriodTemp = timeNow - tempTime;
            if(timePeriodTemp > 7200000 )
            {
                timePeriodTemp = 7200000;
            }
            timePeriod += timePeriodTemp;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("timePeriod", timePeriod);
        result.put("count", res.size());
        return result;
    }
    
    private Map<String, Object> getAllAverageTimeByAreaId2(String floorNo, long time, String tableName)
    {
        List<Map<String, Object>> res = daoArea.getAllAverageTimeByAreaId2(floorNo, time, tableName);
        long timePeriod = 0;
        long tempTime = 0;
        for (int i = 0; i < res.size(); i++)
        {
            long timeNow = Long.parseLong(res.get(i).get("timestamp").toString());
            long timeBegin = Long.parseLong(res.get(i).get("time_begin").toString());
            if (timeBegin > time)
            {
                tempTime = timeBegin;
            }
            else
            {
                tempTime = time;
            }

            /** 添加需求当每个人统计的时长超过2小时则按两小时计算 */
            long timePeriodTemp = timeNow - tempTime;
            if (timePeriodTemp > 7200000)
            {
                timePeriodTemp = 7200000;
            }
            timePeriod += timePeriodTemp;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("timePeriod", timePeriod);
        result.put("count", res.size());
        return result;
    }
    
    private Map<String, Object> getAverageTimeByAreaId(String areaId, String visiday)
    {
        List<Map<String, Object>> res = daoArea.getAverageTimeByAreaId(areaId, visiday);
        long allTime = 0;
        int size = 0;
        for (int i = 0; i < res.size(); i++)
        {
            allTime = Long.parseLong(res.get(i).get("allTime").toString());
            size = Integer.parseInt(res.get(i).get("number").toString());
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("timePeriod", allTime);
        result.put("count", size);
        return result;
    }
    
    /** 
     * @Title: transformToBean 
     * @Description: Accuracy form bean 转 dao bean
     * @param form
     * @return 
     */
    private AccuracyModel transformToBeanAccuracy(AccuracyApiModel form){
        AccuracyModel bean = new AccuracyModel();
        bean.setAverDevi(form.getAverDevi());
        bean.setCount10(form.getCount10());
        bean.setCount10p(form.getCount10p());
        bean.setCount3(form.getCount3());
        bean.setCount5(form.getCount5());
        bean.setDestination(form.getDestination());
        bean.setDetail(form.getDetail());
        bean.setEnddate(ConvertUtil.dateStringFormat(form.getEnddate(), Params.YYYYMMDDHHMMSS));
        bean.setId(form.getId());
        bean.setOffset(form.getOffset());
        bean.setOrigin(form.getOrigin());
        bean.setStartdate(ConvertUtil.dateStringFormat(form.getStartdate(), Params.YYYYMMDDHHMMSS));
        bean.setTriggerIp(form.getTriggerIp());
        bean.setType(form.getType());
        bean.setVariance(form.getVariance());
        MapsModel map = new MapsModel();
        map.setFloorNo(new BigDecimal(form.getFloorNo()));
        bean.setMaps(map);
        StoreModel store = new StoreModel();
        store.setId(form.getPlaceId());
        bean.setStore(store);
        
        return bean;
    }
    
    /** 
     * @Title: transformToBeanStatic 
     * @Description: 静态精度测试 form bean 转 dao bean
     * @param form
     * @return 
     */
    private StaticAccuracyModel transformToBeanStatic(StaticAccuracyApiModel form){
        StaticAccuracyModel bean = new StaticAccuracyModel();
        bean.setAvgeOffset(form.getAvgeOffset());
        bean.setCount10(form.getCount10());
        bean.setCount10p(form.getCount10p());
        bean.setCount3(form.getCount3());
        bean.setCount5(form.getCount5());
        bean.setDestination(form.getDestination());
        bean.setDetail(form.getDetail());
        bean.setEnddate(ConvertUtil.dateStringFormat(form.getEnddate(), Params.YYYYMMDDHHMMSS));
        bean.setId(form.getId());
        bean.setMaxOffset(form.getMaxOffset());
        bean.setOffsetCenter(form.getOffsetCenter());
        bean.setOffsetNumber(form.getOffsetNumber());
        bean.setOrigin(form.getOrigin());
        bean.setStability(form.getStability());
        bean.setStaicAccuracy(form.getStaicAccuracy());
        bean.setStartdate(ConvertUtil.dateStringFormat(form.getStartdate(), Params.YYYYMMDDHHMMSS));
        bean.setTriggerIp(form.getTriggerIp());
        MapsModel map = new MapsModel();
        map.setFloorNo(form.getFloorNo());
        map.setFloor(form.getFloor());
        bean.setMap(map);
        StoreModel store = new StoreModel();
        store.setId(form.getPlaceId());
        store.setName(form.getPlace());
        bean.setStore(store);
        
        return bean;
    }
    
    /** 
     * @Title: transformToBeanStatic 
     * @Description: 动态精度测试 form bean 转 dao bean
     * @param form
     * @return 
     */
    private DynamicAccuracyModel transformToBeanDynamic(DynamicAccuracyApiModel form){
        DynamicAccuracyModel bean = new DynamicAccuracyModel();
        bean.setAvgeOffset(form.getAvgeOffset());
        bean.setCount10(form.getCount10());
        bean.setCount10p(form.getCount10p());
        bean.setCount3(form.getCount3());
        bean.setCount5(form.getCount5());
        bean.setDestination(form.getDestination());
        bean.setDetail(form.getDetail());
        bean.setEndDate(ConvertUtil.dateStringFormat(form.getEnddate(), Params.YYYYMMDDHHMMSS));
        bean.setId(form.getId());
        bean.setMaxOffset(form.getMaxOffset());
        bean.setOffset(form.getOffset());
        bean.setOrigin(form.getOrigin());
        bean.setStartDate(ConvertUtil.dateStringFormat(form.getStartdate(), Params.YYYYMMDDHHMMSS));
        bean.setTriggerIp(form.getTriggerIp());
        MapsModel map = new MapsModel();
        map.setFloorNo(form.getFloorNo());
        map.setFloor(form.getFloor());
        bean.setMaps(map);
        StoreModel store = new StoreModel();
        store.setId(form.getPlaceId());
        store.setName(form.getPlace());
        bean.setStore(store);
        
        return bean;
    }
    
    @RequestMapping(value = "/getCarMap", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getCarMap(@RequestParam("storeId") String storeId)
    {
        //停车场地图
        MapsModel lis = new MapsModel();
        //空车位号
        List<String> parkingNumber = null;
        Map<String, Object> map = new HashMap<String,Object>();
        Map<String, Object> maplist = null;
        List<Object>  list = new ArrayList<>();
        List<Integer> floorNoList = daoMaps.getFloorByPlaceId(storeId);
        try {
            for (int i = 0; i < floorNoList.size(); i++) {
                maplist = new HashMap<String,Object>();
                lis = daoMaps.getMapCarData(floorNoList.get(i));
                parkingNumber = daoMaps.getParkingNumber(floorNoList.get(i));
                if (lis!=null) {
                    maplist.put("map", lis);
                    maplist.put("parkingNumber", parkingNumber);
                    list.add(maplist);
                }

            }
        } catch (Exception e) {
            LOG.error("getCarMap error:"+e);
            map.put("error:",e.getMessage());
        }
        map.put("data", list);
        
        return map;
        
    }
    
    
    @RequestMapping(value = "/getParkingPlace", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getParkingPlace(@RequestParam("userName") String userName)
    {
        //车位号集合
        List<ParkinginformationModel> parkingNumberList = null ;
        String errorResult = null;
        try {
            parkingNumberList = parkDao.getParking(userName);
        } catch (Exception e) {
            LOG.error("getParkingPlace error:"+e);
            errorResult = e.getMessage();
        }
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("result", parkingNumberList);
        map.put("error",errorResult);
        
        return map;
        
    }
    
    @RequestMapping(value = "/clearParkingNumber", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> clearParkingNumber(@RequestBody ParkinginformationModel model1)
    {
        //取车成功
        int result = 0 ;
        String errorResult = null;
        try {
            result = parkDao.clearParkingNumber1(model1);
            
        } catch (Exception e) {
            LOG.error("saveParkingPlace error:"+e);
            errorResult = e.getMessage();
        }
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("result", result);
        map.put("error",errorResult);
        
        return map;
        
    }
    
    
    @RequestMapping(value = "/saveParkingPlace", method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> saveParkingPlace(@RequestBody ParkinginformationModel model1)
    {
        //停车时间
        long time = System.currentTimeMillis();
        model1.setEntryTime(time);
        //保存结果0失败，1成功
        int result = 0 ;
        //错误信息
        String errorResult = null;
        try {
            result = parkDao.saveParking(model1);
            
        } catch (Exception e) {
            LOG.error("saveParkingPlace error:"+e);
            errorResult = e.getMessage();
        }
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("result", result);
        map.put("error",errorResult);
        
        return map;
        
    }
    
    @RequestMapping(value = "/creatData", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> creatData(@RequestParam("floorNo") int floorNo,@RequestParam("startTime")String startTime,@RequestParam("endTime")String endTime)
    {
        
        //根据楼层号删除添加sql
        String sql = "delete from acrdata where floorNo ="+floorNo;
        String insertSql = "insert into acrdata(areaId,floorNo,areaName,userId,timestamp) values ";
        Map<String, Object> map = new HashMap<String,Object>();
        List<AreaModel> lis  = null;
        List<Map<String, Object>> listMap = null;
        String areaName = null;
        String areaId = null;
        String userId = null;
        String timestamp = null;
        String tableName = Params.LOCATION + startTime.substring(0, 8);
        long start = ConvertUtil.dateFormatStringtoLong(startTime, Params.YYYYMMDDHHMMSS1);
        long end = ConvertUtil.dateFormatStringtoLong(endTime, Params.YYYYMMDDHHMMSS1);
        LOG.debug("creatData time:"+start+" "+end);
        //判断楼层号是否为空
        if ("".equals(floorNo)) {
            map.put("error", "floorNo is null!");
            return map;
        }else
        {
            List<String> inputTempList = new ArrayList<String>();
            //根据楼层号获取所以得区域
            lis =daoArea.getAreaByFloorNo(floorNo);
            for (int i = 0; i < lis.size(); i++) {
                AreaModel area = lis.get(i);
                areaName = area.getAreaName();
                areaId = area.getId();
                //获取某个区域人的userId
                listMap = daoArea.getACRData(area, tableName, floorNo,start,end);
                for (int j = 0; j < listMap.size(); j++) {
                    userId = listMap.get(j).get("userId").toString();
                    timestamp = ConvertUtil.dateFormat((Long)(listMap.get(j).get("timestamp")),Params.YYYYMMDDHHMMSS);
                    String temp = "("+areaId+","+floorNo+",'"+areaName+"','"+userId+"','"+timestamp+"')";
                    inputTempList.add(temp);
                }
            
            }
            //拼接字符串
            String acrInsert = insertSql + StringUtils.join(inputTempList, ",");
            int reslut = 0;
            //区域人数不为空执行删除插入操作
            if (inputTempList.size()>0) {
                comDao.doUpdate(sql);
                reslut  = comDao.doUpdate(acrInsert);
            }else
            {
                map.put("error", "no people from area");  
            }
            map.put("result", reslut);
        }
        return map;
        
    }
    
    
}
