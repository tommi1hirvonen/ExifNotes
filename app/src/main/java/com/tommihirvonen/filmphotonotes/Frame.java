package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

public class Frame {

    public int count;
    public String date;
    public String lens;


    public Frame(int count, String date, String lens) {
        this.count = count;
        this.date = date;
        this.lens = lens;
    }


    // Methods to set members
    public void setCount(int input){
        this.count = input;
    }

    public void setDate(String input) {
        this.date = input;
    }

    public void setLens(String input){
        this.lens = input;
    }


    //Methods to get members
    public int getCount(){
        return this.count;
    }

    public String getDate(){
        return this.date;
    }

    public String getLens(){
        return this.lens;
    }

}
