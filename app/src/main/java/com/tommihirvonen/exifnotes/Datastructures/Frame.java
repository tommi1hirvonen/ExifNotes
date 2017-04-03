package com.tommihirvonen.exifnotes.Datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The frame class holds the information of one frame.
 */
public class Frame implements Parcelable {

    private long id;
    private long rollId;
    private int count;
    private String date;
    private long lensId;
    private String shutter;
    private String aperture;
    private String note;
    private String location;

    private int focalLength;
    private String exposureComp;
    private int noOfExposures;
    private int flashUsed;
    private String flashPower;
    private String flashComp;
    private long filterId;
    private int meteringMode;

    public Frame(){
        // Empty constructor
    }

//    public Frame(long roll,
//                 int count,
//                 String date,
//                 long lensId,
//                 String shutter,
//                 String aperture,
//                 String note,
//                 String location,
//                 int focalLength,
//                 String exposureComp,
//                 int noOfExposures,
//                 int flashUsed,
//                 String flashPower,
//                 String flashComp,
//                 long filterId,
//                 String meteringMode
//    ) {
//        this.rollId = roll;
//        this.count = count;
//        this.date = date;
//        this.lensId = lensId;
//        this.shutter = shutter;
//        this.aperture = aperture;
//        this.note = note;
//        this.location = location;
//        this.focalLength = focalLength;
//        this.exposureComp = exposureComp;
//        this.noOfExposures = noOfExposures;
//        this.flashUsed = flashUsed;
//        this.flashPower = flashPower;
//        this.flashComp = flashComp;
//        this.filterId = filterId;
//        this.meteringMode = meteringMode;
//    }


    // Methods to set members
    public void setId(long input) {
        this.id = input;
    }

    public void setRollId(long input){
        this.rollId = input;
    }

    public void setCount(int input){
        this.count = input;
    }

    public void setDate(String input) {
        this.date = input;
    }

    public void setLensId(long input){
        this.lensId = input;
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
        this.focalLength = input;
    }

    public void setExposureComp(String input) {
        this.exposureComp = input;
    }

    public void setNoOfExposures(int input){
        this.noOfExposures = input;
    }

    public void setFlashUsed(int input){
        this.flashUsed = input;
    }

    public void setFlashPower(String input){
        this.flashPower = input;
    }

    public void setFlashComp(String input){
        this.flashComp = input;
    }

    public void setFilterId(long input){
        this.filterId = input;
    }

    public void setMeteringMode(int input){
        this.meteringMode = input;
    }

    //Methods to get members
    public long getId() {
        return this.id;
    }

    public long getRollId(){
        return this.rollId;
    }

    public int getCount(){
        return this.count;
    }

    public String getDate(){
        return this.date;
    }

    public long getLensId(){
        return this.lensId;
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
        return this.focalLength;
    }

    public String getExposureComp(){
        return this.exposureComp;
    }

    public int getNoOfExposures(){
        return this.noOfExposures;
    }

    public int getFlashUsed(){
        return this.flashUsed;
    }

    public String getFlashPower(){
        return this.flashPower;
    }

    public String getFlashComp(){
        return this.flashComp;
    }

    public long getFilterId(){
        return this.filterId;
    }

    public int getMeteringMode(){
        return this.meteringMode;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    private Frame(Parcel pc){
        this.id = pc.readLong();
        this.rollId = pc.readLong();
        this.count = pc.readInt();
        this.date = pc.readString();
        this.lensId = pc.readLong();
        this.shutter = pc.readString();
        this.aperture = pc.readString();
        this.note = pc.readString();
        this.location = pc.readString();
        this.focalLength = pc.readInt();
        this.exposureComp = pc.readString();
        this.noOfExposures = pc.readInt();
        this.flashUsed = pc.readInt();
        this.flashPower = pc.readString();
        this.flashComp = pc.readString();
        this.filterId = pc.readLong();
        this.meteringMode = pc.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(rollId);
        parcel.writeInt(count);
        parcel.writeString(date);
        parcel.writeLong(lensId);
        parcel.writeString(shutter);
        parcel.writeString(aperture);
        parcel.writeString(note);
        parcel.writeString(location);
        parcel.writeInt(focalLength);
        parcel.writeString(exposureComp);
        parcel.writeInt(noOfExposures);
        parcel.writeInt(flashUsed);
        parcel.writeString(flashPower);
        parcel.writeString(flashComp);
        parcel.writeLong(filterId);
        parcel.writeInt(meteringMode);
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
