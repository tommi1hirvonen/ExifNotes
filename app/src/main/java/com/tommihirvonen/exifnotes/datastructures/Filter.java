package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Filter class holds the information of a photographic filter.
 */
public class Filter extends Gear implements Parcelable {

    /**
     * empty constructor
     */
    public Filter(){
        super();
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Filter's information
     */
    private Filter(Parcel pc){
        super(pc);
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

}
