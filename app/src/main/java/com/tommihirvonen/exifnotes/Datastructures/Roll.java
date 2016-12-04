package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

/**
 * Roll class holds the information of one roll of film.
 */
public class Roll {

    public long id;
    public String name;
    public String date;
    public String note;
    public long camera_id;

    public Roll(){

    }

    public Roll(long id, String name, String date, String note, long camera_id){
        this.id = id;
        this.name = name;
        this.date = date;
        this.note = note;
        this.camera_id = camera_id;
    }

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
}
