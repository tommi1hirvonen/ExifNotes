package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

/**
 * The camera class holds the information of a camera.
 */
public class Camera {

    private long id;
    private String make;
    private String model;
    private String serialNumber;
    private String minShutter;
    private String maxShutter;

    public Camera(){

    }

    public Camera(long id, String make, String model, String serialNumber, String minShutter, String maxShutter){
        this.id = id;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.minShutter = minShutter;
        this.maxShutter = maxShutter;
    }

    public void setId(long input){
        this.id = input;
    }

    public void setMake(String input){
        this.make = input;
    }

    public void setModel(String input){
        this.model = input;
    }

    public void setSerialNumber(String input){
        this.serialNumber = input;
    }

    public void setMinShutter(String input){
        this.minShutter = input;
    }

    public void setMaxShutter(String input){
        this.maxShutter = input;
    }


    public long getId(){
        return this.id;
    }

    public String getMake(){
        return this.make;
    }

    public String getModel(){
        return this.model;
    }

    public String getSerialNumber(){
        return this.serialNumber;
    }

    public String getMinShutter(){
        return this.minShutter;
    }

    public String getMaxShutter(){
        return this.maxShutter;
    }

}
