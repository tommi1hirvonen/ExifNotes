package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

/**
 * Abstract super class for different types of gear.
 * Defines all common member variables and methods
 * as well as mandatory interfaces to implement.
 */
public abstract class Gear implements Parcelable {

    /**
     * Unique database id
     */
    private long id;

    /**
     * The make of the Gear
     */
    private String make;

    /**
     * The model of the Gear
     */
    private String model;

    /**
     * Empty default constructor
     */
    Gear() {

    }

    /**
     *
     * @return unique database id
     */
    public long getId() {
        return this.id;
    }

    /**
     *
     * @return make of the Gear
     */
    @NonNull
    public String getMake() {
        return this.make;
    }

    /**
     *
     * @return model of the Gear
     */
    @NonNull
    public String getModel() {
        return this.model;
    }

    /**
     *
     * @return make and model of the Gear concatenated
     */
    @NonNull
    public String getName() {
        return this.make + " " + this.model;
    }

    /**
     *
     * @param input unique database id
     */
    public void setId(long input) {
        this.id = input;
    }

    /**
     *
     * @param input make of the Gear
     */
    public void setMake(@NonNull String input) {
        this.make = input;
    }

    /**
     *
     * @param input model of the Gear
     */
    public void setModel(@NonNull String input) {
        this.model = input;
    }

    /**
     * Method used to compare two instances of Gear.
     * Used when a collection of Gears is being sorted.
     *
     * @param object object that is an instance of Gear
     * @return true if the two instances are copies of each other
     */
    @Override
    public boolean equals(Object object) {
        Gear gear;
        if (object instanceof Gear) gear = (Gear) object;
        else return  false;
        return gear.id == this.id && gear.make.equals(this.make) && gear.model.equals(this.model);
    }

    /**
     * Constructs Gear from Parcel
     *
     * @param pc Parcel object containing Gear's information
     */
    Gear(Parcel pc) {
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
    }

    /**
     * Required by the Parcelable interface. Not used.
     *
     * @return 0
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

}
