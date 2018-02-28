package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The camera class holds the information of a camera.
 */
public class Camera implements Gear, Parcelable {

    /**
     * database id
     */
    private long id;

    /**
     * make/manufacturer
     */
    private String make;

    /**
     * model
     */
    private String model;

    /**
     * camera's serial number (can contain letters)
     */
    private String serialNumber;

    /**
     * camera's minimum shutter speed (shortest possible duration)
     */
    private String minShutter;

    /**
     * camera's maximum shutter speed (longest possible duration)
     */
    private String maxShutter;

    /**
     *  integer defining whether the shutter speed values can be changed in
     *  third, half or full stop increments
     *
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    private int shutterIncrements = 0;


    /**
     * empty constructor
     */
    public Camera(){

    }

    /**
     *
     * @param input database id
     */
    public void setId(long input){
        this.id = input;
    }

    /**
     *
     * @param input make/manufacturer
     */
    public void setMake(String input){
        this.make = input;
    }

    /**
     *
     * @param input model
     */
    public void setModel(String input){
        this.model = input;
    }

    /**
     *
     * @param input serial number (can contain letters)
     */
    public void setSerialNumber(String input){
        this.serialNumber = input;
    }

    /**
     *
     * @param input minimum shutter speed value (shortest possible duration) in format 1/X or Y"
     *              where X and Y are numbers
     */
    public void setMinShutter(String input){
        this.minShutter = input;
    }

    /**
     *
     * @param input maximum shutter speed value (longest possible duration) in format 1/X or Y"
     *              where X and Y are numbers
     */
    public void setMaxShutter(String input){
        this.maxShutter = input;
    }

    /**
     *
     * @param input shutter speed value change increments
     *              0 = third stop (default)
     *              1 = half stop
     *              2 = full stop
     */
    public void setShutterIncrements(int input){
        if (input <= 2 && input >= 0) {
            this.shutterIncrements = input;
        }
    }


    /**
     *
     * @return database id
     */
    public long getId(){
        return this.id;
    }

    /**
     *
     * @return make/manufacturer
     */
    public String getMake(){
        return this.make;
    }

    /**
     *
     * @return model
     */
    public String getModel(){
        return this.model;
    }

    /**
     *
     * @return make + model
     */
    public String getName() {
        return this.make + " " + this.model;
    }

    /**
     *
     * @return camera's serial number
     */
    public String getSerialNumber(){
        return this.serialNumber;
    }

    /**
     *
     * @return minimum shutter speed value (shortest possible duration) in format 1/X or Y"
     * where X and Y are numbers
     */
    public String getMinShutter(){
        return this.minShutter;
    }

    /**
     *
     * @return maximum shutter speed value (longest possible duration) in format 1/X or Y"
     * where X and Y are numbers
     */
    public String getMaxShutter(){
        return this.maxShutter;
    }

    /**
     *
     * @return shutter speed value change increments
     *              0 = third stop (default)
     *              1 = half stop
     *              2 = full stop
     */
    public int getShutterIncrements(){
        return this.shutterIncrements;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Camera's information
     */
    private Camera(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
        this.serialNumber = pc.readString();
        this.minShutter = pc.readString();
        this.maxShutter = pc.readString();
        this.shutterIncrements = pc.readInt();
    }

    /**
     * Not used
     *
     * @return not used
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this object's members to a Parcel given as argument
     *
     * @param parcel Parcel which should be written with this object's members
     * @param i not used
     */
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

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Camera> CREATOR = new Parcelable.Creator<Camera>() {
        public Camera createFromParcel(Parcel pc) {
            return new Camera(pc);
        }
        public Camera[] newArray(int size) {
            return new Camera[size];
        }
    };

    /**
     * Custom equals to compare two Cameras
     *
     * @param obj Camera object
     * @return true if obj is Camera and all its members equal to this object's members
     */
    @Override
    public boolean equals(Object obj) {
        Camera camera;
        if (obj instanceof Camera) camera = (Camera) obj;
        else return false;
        return camera.getId() == id && camera.getMake().equals(make) &&
                camera.getModel().equals(model);
                // && camera.getSerialNumber().equals(serialNumber) &&
                // camera.getMaxShutter().equals(maxShutter) && camera.getMinShutter().equals(minShutter) &&
                // camera.getShutterIncrements() == shutterIncrements;
    }

}
