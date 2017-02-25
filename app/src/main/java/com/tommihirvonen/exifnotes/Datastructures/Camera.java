package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The camera class holds the information of a camera.
 */
public class Camera implements Parcelable {

    private long id;
    private String make;
    private String model;
    private String serialNumber;
    private String minShutter;
    private String maxShutter;
    private int shutterIncrements = 0;
    // 0 = third stop (default)
    // 1 = half stop
    // 2 = full stop

    public Camera(){

    }

    public Camera(long id, String make, String model, String serialNumber, String minShutter, String maxShutter, int shutterIncrements){
        this.id = id;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.minShutter = minShutter;
        this.maxShutter = maxShutter;
        if (shutterIncrements <= 2 && shutterIncrements >= 0) {
            this.shutterIncrements = shutterIncrements;
        }
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

    public void setShutterIncrements(int input){
        if (input <= 2 && input >= 0) {
            this.shutterIncrements = input;
        }
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

    public int getShutterIncrements(){
        return this.shutterIncrements;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    public Camera(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
        this.serialNumber = pc.readString();
        this.minShutter = pc.readString();
        this.maxShutter = pc.readString();
        this.shutterIncrements = pc.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(make);
        parcel.writeString(model);
        parcel.writeString(serialNumber);
        parcel.writeString(minShutter);
        parcel.writeString(maxShutter);
        parcel.writeInt(shutterIncrements);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Camera> CREATOR = new Parcelable.Creator<Camera>() {
        public Camera createFromParcel(Parcel pc) {
            return new Camera(pc);
        }
        public Camera[] newArray(int size) {
            return new Camera[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        Camera camera;
        if (obj instanceof Camera) camera = (Camera) obj;
        else return false;
        return camera.getId() == id && camera.getMake().equals(make) &&
                camera.getModel().equals(model) && camera.getSerialNumber().equals(serialNumber) &&
                camera.getMaxShutter().equals(maxShutter) && camera.getMinShutter().equals(minShutter) &&
                camera.getShutterIncrements() == shutterIncrements;
    }

}
