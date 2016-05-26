package com.tommihirvonen.exifnotes;

// Copyright 2015
// Tommi Hirvonen

/**
 * The frame class holds the information of one frame.
 */
public class Frame {

    public int id;
    public int roll;
    public int count;
    public String date;
    public int lens_id;
    public String shutter;
    public String aperture;
    public String note;
    public String location;

    public Frame(){
        // Empty constructor
    }

    public Frame(int roll, int count, String date, int lens_id, String shutter, String aperture, String note, String location) {
        this.roll = roll;
        this.count = count;
        this.date = date;
        this.lens_id = lens_id;
        this.shutter = shutter;
        this.aperture = aperture;
        this.note = note;
        this.location = location;
    }


    // Methods to set members
    public void setId(int input) {
        this.id = input;
    }

    public void setRoll(int input){
        this.roll = input;
    }

    public void setCount(int input){
        this.count = input;
    }

    public void setDate(String input) {
        this.date = input;
    }

    public void setLensId(int input){
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
    public int getId() {
        return this.id;
    }

    public int getRoll(){
        return this.roll;
    }

    public int getCount(){
        return this.count;
    }

    public String getDate(){
        return this.date;
    }

    public int getLensId(){
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

}
