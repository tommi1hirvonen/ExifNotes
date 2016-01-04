package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

public class Roll {

    public int id;
    public String name;

    public Roll(){

    }

    public Roll(int id, String name){
        this.id = id;
        this.name = name;
    }

    public void setId(int input) {
        this.id = input;
    }

    public void setName(String input){
        this.name = input;
    }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

}
