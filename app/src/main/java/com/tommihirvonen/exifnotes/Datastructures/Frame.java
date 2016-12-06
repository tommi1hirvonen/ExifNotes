package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The frame class holds the information of one frame.
 */
public class Frame implements Parcelable {

    private long id;
    private long roll_id;
    private int count;
    private String date;
    private long lens_id;
    private String shutter;
    private String aperture;
    private String note;
    private String location;

    public Frame(){
        // Empty constructor
    }

    public Frame(long roll, int count, String date, long lens_id, String shutter, String aperture, String note, String location) {
        this.roll_id = roll;
        this.count = count;
        this.date = date;
        this.lens_id = lens_id;
        this.shutter = shutter;
        this.aperture = aperture;
        this.note = note;
        this.location = location;
    }


    // Methods to set members
    public void setId(long input) {
        this.id = input;
    }

    public void setRollId(long input){
        this.roll_id = input;
    }

    public void setCount(int input){
        this.count = input;
    }

    public void setDate(String input) {
        this.date = input;
    }

    public void setLensId(long input){
        this.lens_id = input;
    }

    public void setShutter(String input) {
        this.shutter = input;
    }

    public void setAperture(String input) {
        this.aperture = input;
    }

    public void setNote(String input){
        this.note = input;
    }

    public void setLocation(String input) {
        this.location = input;
    }

    //Methods to get members
    public long getId() {
        return this.id;
    }

    public long getRollId(){
        return this.roll_id;
    }

    public int getCount(){
        return this.count;
    }

    public String getDate(){
        return this.date;
    }

    public long getLensId(){
        return this.lens_id;
    }

    public String getShutter(){
        return this.shutter;
    }

    public String getAperture(){
        return this.aperture;
    }

    public String getNote(){
        return this.note;
    }

    public String getLocation(){
        return this.location;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    public Frame(Parcel pc){
        this.id = pc.readLong();
        this.roll_id = pc.readLong();
        this.count = pc.readInt();
        this.date = pc.readString();
        this.lens_id = pc.readLong();
        this.shutter = pc.readString();
        this.aperture = pc.readString();
        this.note = pc.readString();
        this.location = pc.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(roll_id);
        parcel.writeInt(count);
        parcel.writeString(date);
        parcel.writeLong(lens_id);
        parcel.writeString(shutter);
        parcel.writeString(aperture);
        parcel.writeString(note);
        parcel.writeString(location);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Frame> CREATOR = new Parcelable.Creator<Frame>() {
        public Frame createFromParcel(Parcel pc) {
            return new Frame(pc);
        }
        public Frame[] newArray(int size) {
            return new Frame[size];
        }
    };
}
