package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Filter class holds the information of a photographic filter.
 */
public class Filter implements Parcelable {

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
     * empty constructor
     */
    public Filter(){

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

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Filter's information
     */
    private Filter(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
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
    }

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {
        public Filter createFromParcel(Parcel pc) {
            return new Filter(pc);
        }
        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };

    /**
     * Custom equals to compare two Filters
     *
     * @param obj Filter object
     * @return true if obj is Filter and all its members equal to this object's members
     */
    @Override
    public boolean equals(Object obj) {
        Filter filter;
        if (obj instanceof Filter) filter = (Filter) obj;
        else return false;
        return filter.getId() == id && filter.getMake().equals(make) &&
                filter.getModel().equals(model);
    }

}
