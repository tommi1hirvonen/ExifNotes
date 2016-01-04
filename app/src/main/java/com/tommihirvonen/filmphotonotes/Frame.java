package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

public class Frame {

    public int id;
    public int roll;
    public int count;
    public String date;
    public String lens;
    public String shutter;
    public String aperture;

    public Frame(){
        // Empty constructor
    }

    public Frame(int roll, int count, String date, String lens, String shutter, String aperture) {
        this.roll = roll;
        this.count = count;
        this.date = date;
        this.lens = lens;
        this.shutter = shutter;
        this.aperture = aperture;
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

    public void setLens(String input){
        this.lens = input;
    }

    public void setShutter(String input) {
        this.shutter = input;
    }

    public void setAperture(String input) {
        this.aperture = input;
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

    public String getLens(){
        return this.lens;
    }

    public String getShutter(){
        return this.shutter;
    }

    public String getAperture(){
        return this.aperture;
    }

}
