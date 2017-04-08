package com.tommihirvonen.exifnotes.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Roll class holds the information of one roll of film.
 */
public class Roll implements Parcelable {

    /**
     * database id
     */
    private long id;

    /**
     * name/title of this roll
     */
    private String name;

    /**
     * datetime when this roll was loaded in a camera, in format 'YYYY-M-D H:MM'
     */
    private String date;

    /**
     * custom notes
     */
    private String note;

    /**
     * database id of the camera this roll was loaded in
     */
    private long cameraId;

    /**
     * ISO value of the film
     */
    private int iso;

    /**
     * value defining whether this roll was or will be pushed or pulled when processed
     */
    private String pushPull;

    /**
     * the film format
     *
     * corresponding values defined in res/values/array.xml
     */
    private int format;

    /**
     * empty constructor
     */
    public Roll(){

    }

//    public Roll(long id,
//                String name,
//                String date,
//                String note,
//                long cameraId,
//                int iso,
//                String pushPull,
//                String format
//    ){
//        this.id = id;
//        this.name = name;
//        this.date = date;
//        this.note = note;
//        this.cameraId = cameraId;
//        this.iso = iso;
//        this.pushPull = pushPull;
//        this.format = format;
//    }

    /**
     *
     * @param input database id
     */
    public void setId(long input) {
        this.id = input;
    }

    /**
     *
     * @param input name/title of the roll
     */
    public void setName(String input){
        this.name = input;
    }

    /**
     *
     * @param input datetime when the film roll was loaded, for example
     *              in format 'YYYY-M-D H:MM'
     */
    public void setDate(String input) {
        this.date = input;
    }

    /**
     *
     * @param input custom note
     */
    public void setNote(String input) {
        this.note = input;
    }

    /**
     *
     * @param input used camera's database id
     */
    public void setCameraId(long input) {
        this.cameraId = input;
    }

    /**
     *
     * @param input ISO value
     */
    public void setIso(int input){
        this.iso = input;
    }

    /**
     *
     * @param input push or pull in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    public void setPushPull(String input){
        this.pushPull = input;
    }

    /**
     *
     * @param input film format
     *              corresponding values defined in res/values/array.xml
     */
    public void setFormat(int input){
        this.format = input;
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
     * @return name/title of roll
     */
    public String getName(){
        return this.name;
    }

    /**
     *
     * @return datetime when the film was loaded, in format 'YYYY-M-D H:MM'
     */
    public String getDate(){
        return this.date;
    }

    /**
     *
     * @return custom note
     */
    public String getNote(){
        return this.note;
    }

    /**
     *
     * @return used camera's database id
     */
    public long getCameraId(){
        return this.cameraId;
    }

    /**
     *
     * @return film's ISO value
     */
    public int getIso(){
        return this.iso;
    }

    /**
     *
     * @return push or pull in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    public String getPushPull(){
        return this.pushPull;
    }

    /**
     *
     * @return film format
     * corresponding values defined in res/values/array.xml
     */
    public int getFormat(){
        return this.format;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Camera's information
     */
    private Roll(Parcel pc){
        this.id = pc.readLong();
        this.name = pc.readString();
        this.date = pc.readString();
        this.note = pc.readString();
        this.cameraId = pc.readLong();
        this.iso = pc.readInt();
        this.pushPull = pc.readString();
        this.format = pc.readInt();
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
        parcel.writeString(name);
        parcel.writeString(date);
        parcel.writeString(note);
        parcel.writeLong(cameraId);
        parcel.writeInt(iso);
        parcel.writeString(pushPull);
        parcel.writeInt(format);
    }

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Roll> CREATOR = new Parcelable.Creator<Roll>() {
        public Roll createFromParcel(Parcel pc) {
            return new Roll(pc);
        }
        public Roll[] newArray(int size) {
            return new Roll[size];
        }
    };
}