package com.tommihirvonen.exifnotes;

// Copyright 2015
// Tommi Hirvonen

/**
 * The camera class holds the information of a camera.
 */
import java.util.ArrayList;

public class Camera {

    public int id;
    public String make;
    public String model;
    public ArrayList<Lens> mountableLenses;

    public Camera(){

    }

    public Camera(int id, String make, String model, ArrayList<Lens> mountableLenses){
        this.id = id;
        this.make = make;
        this.model = model;
        this.mountableLenses = mountableLenses;
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

    public void setMountableLenses(ArrayList<Lens> input) {
        this.mountableLenses = input;
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

    public ArrayList<Lens> getMountableLenses() {
        return this.mountableLenses;
    }
}
