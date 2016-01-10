package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

import java.util.ArrayList;

public class Camera {

    public int id;
    public String name;
    public ArrayList<Lens> mountableLenses;

    public Camera(){

    }

    public Camera(int id, String name, ArrayList<Lens> mountableLenses){
        this.id = id;
        this.name = name;
        this.mountableLenses = mountableLenses;
    }

    public void setId(int input){
        this.id = input;
    }

    public void setName(String input){
        this.name = input;
    }

    public void setMountableLenses(ArrayList<Lens> input) {
        this.mountableLenses = input;
    }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public ArrayList<Lens> getMountableLenses() {
        return this.mountableLenses;
    }
}
