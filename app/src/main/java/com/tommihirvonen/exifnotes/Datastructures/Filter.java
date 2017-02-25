package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2016
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

public class Filter implements Parcelable {

    public long id;
    public String make;
    public String model;

    public Filter(){

    }

    public Filter(long id, String make, String model){
        this.id = id;
        this.make = make;
        this.model = model;
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


    public long getId(){
        return this.id;
    }

    public String getMake(){
        return this.make;
    }

    public String getModel(){
        return this.model;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    public Filter(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
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
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {
        public Filter createFromParcel(Parcel pc) {
            return new Filter(pc);
        }
        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        Filter filter;
        if (obj instanceof Filter) filter = (Filter) obj;
        else return false;
        return filter.getId() == id && filter.getMake().equals(make) &&
                filter.getModel().equals(model);
    }

}
