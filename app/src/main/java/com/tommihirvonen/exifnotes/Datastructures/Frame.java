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

    private int focal_length;
    private String exposure_comp;
    private int no_of_exposures;
    private int flash_used;
    private String flash_power;
    private String flash_comp;
    private long filter_id;
    private int metering_mode;

    public Frame(){
        // Empty constructor
    }

//    public Frame(long roll,
//                 int count,
//                 String date,
//                 long lens_id,
//                 String shutter,
//                 String aperture,
//                 String note,
//                 String location,
//                 int focal_length,
//                 String exposure_comp,
//                 int no_of_exposures,
//                 int flash_used,
//                 String flash_power,
//                 String flash_comp,
//                 long filter_id,
//                 String metering_mode
//    ) {
//        this.roll_id = roll;
//        this.count = count;
//        this.date = date;
//        this.lens_id = lens_id;
//        this.shutter = shutter;
//        this.aperture = aperture;
//        this.note = note;
//        this.location = location;
//        this.focal_length = focal_length;
//        this.exposure_comp = exposure_comp;
//        this.no_of_exposures = no_of_exposures;
//        this.flash_used = flash_used;
//        this.flash_power = flash_power;
//        this.flash_comp = flash_comp;
//        this.filter_id = filter_id;
//        this.metering_mode = metering_mode;
//    }


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

    public void setFocalLength(int input) {
        this.focal_length = input;
    }

    public void setExposureComp(String input) {
        this.exposure_comp = input;
    }

    public void setNoOfExposures(int input){
        this.no_of_exposures = input;
    }

    public void setFlashUsed(int input){
        this.flash_used = input;
    }

    public void setFlashPower(String input){
        this.flash_power = input;
    }

    public void setFlashComp(String input){
        this.flash_comp = input;
    }

    public void setFilterId(long input){
        this.filter_id = input;
    }

    public void setMeteringMode(int input){
        this.metering_mode = input;
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

    public int getFocalLength(){
        return this.focal_length;
    }

    public String getExposureComp(){
        return this.exposure_comp;
    }

    public int getNoOfExposures(){
        return this.no_of_exposures;
    }

    public int getFlashUsed(){
        return this.flash_used;
    }

    public String getFlashPower(){
        return this.flash_power;
    }

    public String getFlashComp(){
        return this.flash_comp;
    }

    public long getFilterId(){
        return this.filter_id;
    }

    public int getMeteringMode(){
        return this.metering_mode;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    private Frame(Parcel pc){
        this.id = pc.readLong();
        this.roll_id = pc.readLong();
        this.count = pc.readInt();
        this.date = pc.readString();
        this.lens_id = pc.readLong();
        this.shutter = pc.readString();
        this.aperture = pc.readString();
        this.note = pc.readString();
        this.location = pc.readString();
        this.focal_length = pc.readInt();
        this.exposure_comp = pc.readString();
        this.no_of_exposures = pc.readInt();
        this.flash_used = pc.readInt();
        this.flash_power = pc.readString();
        this.flash_comp = pc.readString();
        this.filter_id = pc.readLong();
        this.metering_mode = pc.readInt();
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
        parcel.writeInt(focal_length);
        parcel.writeString(exposure_comp);
        parcel.writeInt(no_of_exposures);
        parcel.writeInt(flash_used);
        parcel.writeString(flash_power);
        parcel.writeString(flash_comp);
        parcel.writeLong(filter_id);
        parcel.writeInt(metering_mode);
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
