package com.tommihirvonen.exifnotes;

// Copyright 2015
// Tommi Hirvonen

/**
 * Lens class holds the information of one lens.
 */
public class Lens {

    public int id;
    public String make;
    public String model;

    public Lens(){

    }

    public Lens(int id, String make, String model){
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
