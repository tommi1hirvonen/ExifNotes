package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

/**
 * Lens class holds the information of one lens.
 */
public class Lens {

    public long id;
    public String make;
    public String model;

    public Lens(){

    }

    public Lens(long id, String make, String model){
        this.id = id;
        this.make = make;
        this.model = model;
    }

    public void setId(long input){
        this.id = input;
    }

    public void setMake(String input){
        this.make = input;
    }

    public void setModel(String input){
        this.model = input;
    }

    public long getId(){
        return this.id;
    }

    public String getMake(){
        return this.make;
    }

    public String getModel(){
        return this.model;
    }

}
