package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The camera class holds the information of a camera.
 */
public class Camera extends Gear {

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
     * Integer defining whether the exposure compensation values can be changed in
     * third or half stop increments.
     *
     * 0 = third stop (default)
     * 1 = half stop
     */
    private int exposureCompIncrements = 0;

    /**
     * empty constructor
     */
    public Camera(){
        super();
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
     * @param input exposure compensation value change increments
     *              0 = third stop (default)
     *              1 = half stop
     */
    public void setExposureCompIncrements(int input) {
        if (input <= 1 && input >= 0) {
            this.exposureCompIncrements = input;
        }
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

    /**
     *
     * @return exposure compensation value change increments
     *          0 = third stop (default)
     *          1 = half stop
     */
    public int getExposureCompIncrements() {
        return this.exposureCompIncrements;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Camera's information
     */
    private Camera(Parcel pc){
        super(pc);
        this.serialNumber = pc.readString();
        this.minShutter = pc.readString();
        this.maxShutter = pc.readString();
        this.shutterIncrements = pc.readInt();
        this.exposureCompIncrements = pc.readInt();
    }

    /**
     * Writes this object's members to a Parcel given as argument
     *
     * @param parcel Parcel which should be written with this object's members
     * @param i not used
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(serialNumber);
        parcel.writeString(minShutter);
        parcel.writeString(maxShutter);
        parcel.writeInt(shutterIncrements);
        parcel.writeInt(exposureCompIncrements);
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

}
