package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Lens class holds the information of one lens.
 */
public class Lens implements Parcelable {

    private long id;
    private String make;
    private String model;
    private String serialNumber;
    private String minAperture;
    private String maxAperture;
    private int minFocalLength;
    private int maxFocalLength;
    private int apertureIncrements = 0;
    // 0 = third stop (default)
    // 1 = half stop
    // 2 = full stop

    /**
     * Empty constructor
     */
    public Lens(){

    }

    /**
     * Constructor which sets all the members
     */
    public Lens(long id, String make, String model, String serialNumber, String minAperture,
                String maxAperture, int minFocalLength, int maxFocalLength, int apertureIncrements){
        this.id = id;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.minAperture = minAperture;
        this.maxAperture = maxAperture;
        this.minFocalLength = minFocalLength;
        this.maxFocalLength = maxFocalLength;
        if (apertureIncrements <= 2 && apertureIncrements >= 0){
            this.apertureIncrements = apertureIncrements;
        }
    }

    // GETTERS AND SETTERS

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

    public void setMinAperture(String input){
        this.minAperture = input;
    }

    public void setMaxAperture(String input){
        this.maxAperture = input;
    }

    public void setMinFocalLength(int input){
        this.minFocalLength = input;
    }

    public void setMaxFocalLength(int input){
        this.maxFocalLength = input;
    }

    public void setApertureIncrements(int input){
        if (input <= 2 && input >= 0){
            this.apertureIncrements = input;
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

    public String getMinAperture(){
        return this.minAperture;
    }

    public String getMaxAperture(){
        return this.maxAperture;
    }

    public int getMinFocalLength(){
        return this.minFocalLength;
    }

    public int getMaxFocalLength(){
        return this.maxFocalLength;
    }

    public int getApertureIncrements(){
        return this.apertureIncrements;
    }


    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    public Lens(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
        this.serialNumber = pc.readString();
        this.minAperture = pc.readString();
        this.maxAperture = pc.readString();
        this.minFocalLength = pc.readInt();
        this.maxFocalLength = pc.readInt();
        this.apertureIncrements = pc.readInt();
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
        parcel.writeString(minAperture);
        parcel.writeString(maxAperture);
        parcel.writeInt(minFocalLength);
        parcel.writeInt(maxFocalLength);
        parcel.writeInt(apertureIncrements);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Lens> CREATOR = new Parcelable.Creator<Lens>() {
        public Lens createFromParcel(Parcel pc) {
            return new Lens(pc);
        }
        public Lens[] newArray(int size) {
            return new Lens[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        Lens lens;
        if (obj instanceof Lens) lens = (Lens) obj;
        else return false;
        return lens.getId() == id && lens.getMake().equals(make) &&
                lens.getModel().equals(model) && lens.getSerialNumber().equals(serialNumber) &&
                lens.getMaxAperture().equals(maxAperture) && lens.getMinAperture().equals(minAperture) &&
                lens.getApertureIncrements() == apertureIncrements &&
                lens.getMinFocalLength() == minFocalLength && lens.getMaxFocalLength() == maxFocalLength;
    }
}
