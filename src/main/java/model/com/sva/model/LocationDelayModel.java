package com.sva.model;

import java.math.BigDecimal;

/**
 * @author wwx283823
 * @version iSoftStone 2016-3-16
 * @since iSoftStone
 */
public class LocationDelayModel
{

    private int id;

    private double dataDelay;

    private double positionDelay;

    private long updateTime;
    
    private BigDecimal deviation;

    private MapsModel maps;
    
    private StoreModel store;
    
    private EstimateModel estimate;
    
    private String floorNo;
    
    private int placeId;
    
    public String getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(String floorNo) {
        this.floorNo = floorNo;
    }

    public int getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int placeId) {
        this.placeId = placeId;
    }

    public long getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(long updateTime)
    {
        this.updateTime = updateTime;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public double getDataDelay()
    {
        return dataDelay;
    }

    public void setDataDelay(double dataDelay)
    {
        this.dataDelay = dataDelay;
    }

    public double getPositionDelay()
    {
        return positionDelay;
    }

    public void setPositionDelay(double positionDelay)
    {
        this.positionDelay = positionDelay;
    }

    /**
     * @return the maps
     */
    public MapsModel getMaps() {
        return maps;
    }

    /**
     * @param maps the maps to set
     */
    public void setMaps(MapsModel maps) {
        this.maps = maps;
    }

    /**
     * @return the store
     */
    public StoreModel getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(StoreModel store) {
        this.store = store;
    }

    /**
     * @return the estimate
     */
    public EstimateModel getEstimate() {
        return estimate;
    }

    /**
     * @param estimate the estimate to set
     */
    public void setEstimate(EstimateModel estimate) {
        this.estimate = estimate;
    }

    /**
     * @return the deviation
     */
    public BigDecimal getDeviation() {
        return deviation;
    }

    /**
     * @param deviation the deviation to set
     */
    public void setDeviation(BigDecimal deviation) {
        this.deviation = deviation;
    }
    
}
