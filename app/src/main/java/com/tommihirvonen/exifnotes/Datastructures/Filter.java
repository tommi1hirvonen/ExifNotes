package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2016
// Tommi Hirvonen

public class Filter {

    public int id;
    public String make;
    public String model;

    public Filter(){

    }

    public Filter(int id, String make, String model){
        this.id = id;
        this.make = make;
        this.model = model;
    }

    public void setId(int input){
        this.id = input;
    }

    public void setMake(String input){
        this.make = input;
    }

    public void setModel(String input){
        this.model = input;
    }


    public int getId(){
        return this.id;
    }

    public String getMake(){
        return this.make;
    }

    public String getModel(){
        return this.model;
    }
}
