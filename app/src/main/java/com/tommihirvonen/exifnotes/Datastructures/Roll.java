package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Roll class holds the information of one roll of film.
 */
public class Roll implements Parcelable {

    private long id;
    private String name;
    private String date;
    private String note;
    private long camera_id;
    private int iso;
    private String push_pull;
    private int format;

    public Roll(){

    }

//    public Roll(long id,
//                String name,
//                String date,
//                String note,
//                long camera_id,
//                int iso,
//                String push_pull,
//                String format
//    ){
//        this.id = id;
//        this.name = name;
//        this.date = date;
//        this.note = note;
//        this.camera_id = camera_id;
//        this.iso = iso;
//        this.push_pull = push_pull;
//        this.format = format;
//    }

    public void setId(long input) {
        this.id = input;
    }

    public void setName(String input){
        this.name = input;
    }

    public void setDate(String input) {
        this.date = input;
    }

    public void setNote(String input) {
        this.note = input;
    }

    public void setCamera_id(long input) {
        this.camera_id = input;
    }

    public void setIso(int input){
        this.iso = input;
    }

    public void setPushPull(String input){
        this.push_pull = input;
    }

    public void setFormat(int input){
        this.format = input;
    }

    public long getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getDate(){
        return this.date;
    }

    public String getNote(){
        return this.note;
    }

    public long getCamera_id(){
        return this.camera_id;
    }

    public int getIso(){
        return this.iso;
    }

    public String getPushPull(){
        return this.push_pull;
    }

    public int getFormat(){
        return this.format;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    private Roll(Parcel pc){
        this.id = pc.readLong();
        this.name = pc.readString();
        this.date = pc.readString();
        this.note = pc.readString();
        this.camera_id = pc.readLong();
        this.iso = pc.readInt();
        this.push_pull = pc.readString();
        this.format = pc.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(name);
        parcel.writeString(date);
        parcel.writeString(note);
        parcel.writeLong(camera_id);
        parcel.writeInt(iso);
        parcel.writeString(push_pull);
        parcel.writeInt(format);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Roll> CREATOR = new Parcelable.Creator<Roll>() {
        public Roll createFromParcel(Parcel pc) {
            return new Roll(pc);
        }
        public Roll[] newArray(int size) {
            return new Roll[size];
        }
    };
}
