package com.sva.model;

import java.math.BigDecimal;

public class MapsModel
{
    private String scale;

    private String xo;

    private String yo;

    private String floor;

    private String coordinate;

    private float angle;

    private String path;

    private String svg;

    private String route;

    private String pathFile;

    private String updateTime;

    private int imgWidth;

    private int imgHeight;

    private BigDecimal floorNo;

    private BigDecimal floorid;

    private String id;

    private int mapId;
    
    private String fMap;
    
    private String zMap;
    
    private String zMapPathfile;
    
    private String zIosMap;
    
    private StoreModel store;
    
    private int mapType;
    
    public int getMapType() {
        return mapType;
    }

    public void setMapType(int mapType) {
        this.mapType = mapType;
    }

    public String getfMap()
    {
        return fMap;
    }

    public void setfMap(String fMap)
    {
        this.fMap = fMap;
    }

    public String getzMap()
    {
        return zMap;
    }

    public void setzMap(String zMap)
    {
        this.zMap = zMap;
    }

    public String getzMapPathfile()
    {
        return zMapPathfile;
    }

    public void setzMapPathfile(String zMapPathfile)
    {
        this.zMapPathfile = zMapPathfile;
    }

    public String getzIosMap()
    {
        return zIosMap;
    }

    public void setzIosMap(String zIosMap)
    {
        this.zIosMap = zIosMap;
    }

    public int getMapId()
    {
        return mapId;
    }

    public void setMapId(int mapid)
    {
        mapId = mapid;
    }

    public String getPathFile()
    {
        return pathFile;
    }

    public void setPathFile(String pathFile)
    {
        this.pathFile = pathFile;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute(String route)
    {
        this.route = route;
    }

    public String getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(String updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getSvg()
    {
        return svg;
    }

    public void setSvg(String svg)
    {
        this.svg = svg;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public BigDecimal getFloorid()
    {
        return floorid;
    }

    public void setFloorid(BigDecimal floorid)
    {
        this.floorid = floorid;
    }

    public String getXo()
    {
        return xo;
    }

    public void setXo(String xo)
    {
        this.xo = xo;
    }

    public String getYo()
    {
        return yo;
    }

    public void setYo(String yo)
    {
        this.yo = yo;
    }

    public BigDecimal getFloorNo()
    {
        return floorNo;
    }

    public void setFloorNo(BigDecimal floorNo)
    {
        this.floorNo = floorNo;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getScale()
    {
        return scale;
    }

    public void setScale(String scale)
    {
        this.scale = scale;
    }

    public String getFloor()
    {
        return floor;
    }

    public void setFloor(String floor)
    {
        this.floor = floor;
    }

    public int getImgWidth()
    {
        return imgWidth;
    }

    public void setImgWidth(int imgWidth)
    {
        this.imgWidth = imgWidth;
    }

    public int getImgHeight()
    {
        return imgHeight;
    }

    public void setImgHeight(int imgHeight)
    {
        this.imgHeight = imgHeight;
    }

    public String getCoordinate()
    {
        return coordinate;
    }

    public void setCoordinate(String coordinate)
    {
        this.coordinate = coordinate;
    }

    public float getAngle()
    {
        return angle;
    }

    public void setAngle(float angle)
    {
        this.angle = angle;
    }

    public StoreModel getStore() {
        return store;
    }

    public void setStore(StoreModel store) {
        this.store = store;
    }

}
