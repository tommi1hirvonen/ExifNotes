package com.tommihirvonen.filmphotonotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;


// Copyright 2015
// Tommi Hirvonen

public class FilmDbHelper extends SQLiteOpenHelper {

    public static final String TABLE_FRAMES = "frames";
    public static final String TABLE_LENSES = "lenses";
    public static final String TABLE_ROLLS = "rolls";

    public static final String KEY_FRAME_ID = "frame_id";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DATE = "date";
    public static final String KEY_SHUTTER = "shutter";
    public static final String KEY_APERTURE = "aperture";

    public static final String KEY_LENS_ID = "lens_id";
    public static final String KEY_LENS = "lens";

    public static final String KEY_ROLL_ID = "roll_id";
    public static final String KEY_ROLLNAME = "rollname";
    public static final String KEY_ROLL_DATE = "roll_date";
    public static final String KEY_ROLL_NOTE = "roll_note";

    private static final String DATABASE_NAME = "filmnotes.db";
    private static final int DATABASE_VERSION = 7;

    private static final String CREATE_FRAME_TABLE = "create table " + TABLE_FRAMES
            + "(" + KEY_FRAME_ID + " integer primary key autoincrement, "
            + KEY_ROLL_ID + " integer not null, "
            + KEY_COUNT + " integer not null, "
            + KEY_DATE + " text not null, "
            + KEY_LENS + " text not null, "
            + KEY_SHUTTER + " text not null, "
            + KEY_APERTURE + " text not null"
            + ");";
    private static final String CREATE_LENS_TABLE = "create table " + TABLE_LENSES
            + "(" + KEY_LENS_ID + " integer primary key autoincrement, "
            + KEY_LENS + " text not null"
            + ");";
    private static final String CREATE_ROLL_TABLE = "create table " + TABLE_ROLLS
            + "(" + KEY_ROLL_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null, "
            + KEY_ROLL_DATE + " text not null, "
            + KEY_ROLL_NOTE + " text"
            + ");";

    public FilmDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_FRAME_TABLE);
        database.execSQL(CREATE_LENS_TABLE);
        database.execSQL(CREATE_ROLL_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(FilmDbHelper.class.getName(), "Upgrading database from version " + oldVersion
                + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRAMES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROLLS);
        onCreate(db);
    }

    // ******************** CRUD operations for the frames table ********************

    public void addFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        // Create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ROLL_ID, frame.getRoll());
        values.put(KEY_COUNT, frame.getCount());
        values.put(KEY_DATE, frame.getDate());
        values.put(KEY_LENS, frame.getLens());
        values.put(KEY_SHUTTER, frame.getShutter());
        values.put(KEY_APERTURE, frame.getAperture());
        // Insert
        db.insert(TABLE_FRAMES, // table
                null, // nullColumnHack
                values); // key/value -> keys = column names/ value
        // Close
        db.close();
    }

    public Frame getLastFrame(){
        Frame frame = new Frame();
        String query = "SELECT * FROM " + TABLE_FRAMES + " ORDER BY " + KEY_FRAME_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        frame.setId(cursor.getInt(cursor.getColumnIndex(KEY_FRAME_ID)));
        frame.setRoll(cursor.getInt(cursor.getColumnIndex(KEY_ROLL_ID)));
        frame.setCount(cursor.getInt(cursor.getColumnIndex(KEY_COUNT)));
        frame.setDate(cursor.getString(cursor.getColumnIndex(KEY_DATE)));
        frame.setLens(cursor.getString(cursor.getColumnIndex(KEY_LENS)));
        frame.setShutter(cursor.getString(cursor.getColumnIndex(KEY_SHUTTER)));
        frame.setAperture(cursor.getString(cursor.getColumnIndex(KEY_APERTURE)));
        cursor.close();
        return frame;
    }

    public ArrayList<Frame> getAllFramesFromRoll(int roll_id){
        ArrayList<Frame> frames = new ArrayList<>();
        // Build the query
        String query = "SELECT * FROM " + TABLE_FRAMES + " WHERE " + KEY_ROLL_ID + " = " + roll_id;
        // Get reference to readable database
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Frame frame;
        // Go over each row, build list
        while ( cursor.moveToNext() ) {
            frame = new Frame();
            frame.setId(cursor.getInt(0));
            frame.setRoll(cursor.getInt(1));
            frame.setCount(cursor.getInt(2));
            frame.setDate(cursor.getString(3));
            frame.setLens(cursor.getString(4));
            frame.setShutter(cursor.getString(5));
            frame.setAperture(cursor.getString(6));
            frames.add(frame);
        }
        cursor.close();
        return frames;
    }

    public void updateFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_FRAMES + " SET "
                + KEY_ROLL_ID + "=" + frame.getRoll() + ", "
                + KEY_COUNT + "=" + frame.getCount() + ", "
                + KEY_DATE + "=\"" + frame.getDate() + "\", "
                + KEY_LENS + "=\"" + frame.getLens() + "\", "
                + KEY_SHUTTER + "=\"" + frame.getShutter() + "\", "
                + KEY_APERTURE + "=\"" + frame.getAperture() + "\""
                + " WHERE " + KEY_FRAME_ID + "=" + frame.getId();
        db.execSQL(query);
        db.close();
    }

    public void deleteFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete
        db.delete(TABLE_FRAMES,
                KEY_FRAME_ID + " = ?",
                new String[]{String.valueOf(frame.getId())});
        // Close
        db.close();
    }

    public void deleteAllFramesFromRoll(int roll_id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FRAMES, KEY_ROLL_ID + " = ? ", new String[]{String.valueOf(roll_id)});
        db.close();
    }

    // ******************** CRUD operations for the lenses table ********************

    public void addLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LENS, lens.getName());
        db.insert(TABLE_LENSES, null, values);
        db.close();
    }

    public Lens getLastLens(){
        Lens lens = new Lens();
        String query = "SELECT * FROM " + TABLE_LENSES + " ORDER BY " + KEY_LENS_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        lens.setId(cursor.getInt(cursor.getColumnIndex(KEY_LENS_ID)));
        lens.setName(cursor.getString(cursor.getColumnIndex(KEY_LENS)));
        cursor.close();
        return lens;
    }

    public ArrayList<Lens> getAllLenses(){
        ArrayList<Lens> lenses = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_LENSES + " ORDER BY " + KEY_LENS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Lens lens;
        while ( cursor.moveToNext() ) {
            lens = new Lens();
            lens.setId(cursor.getInt(0));
            lens.setName(cursor.getString(1));
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    public void deleteLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LENSES, KEY_LENS_ID + " = ?", new String[]{String.valueOf(lens.getId())});
        db.close();
    }

    // ******************** CRUD operations for the rolls table ********************

    public void addRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ROLLNAME, roll.getName());
        values.put(KEY_ROLL_DATE, roll.getDate());
        values.put(KEY_ROLL_NOTE, roll.getNote());
        db.insert(TABLE_ROLLS, null, values);
        db.close();
    }

    public Roll getLastRoll(){
        Roll roll = new Roll();
        String query = "SELECT * FROM " + TABLE_ROLLS + " ORDER BY " + KEY_ROLL_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        roll.setId(cursor.getInt(0));
        roll.setName(cursor.getString(1));
        roll.setDate(cursor.getString(2));
        roll.setNote(cursor.getString(3));
        cursor.close();
        return roll;
    }

    public ArrayList<Roll> getAllRolls(){
        ArrayList<Roll> rolls = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_ROLLS + " ORDER BY " + KEY_ROLL_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Roll roll;
        while ( cursor.moveToNext() ) {
            roll = new Roll();
            roll.setId(cursor.getInt(0));
            roll.setName(cursor.getString(1));
            roll.setDate(cursor.getString(2));
            roll.setNote(cursor.getString(3));
            rolls.add(roll);
        }
        cursor.close();
        return rolls;
    }

    public Roll getRoll(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Roll roll = new Roll();
        String query = "SELECT * FROM " + TABLE_ROLLS + " WHERE " + KEY_ROLL_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        roll.setId(cursor.getInt(0));
        roll.setName(cursor.getString(1));
        roll.setDate(cursor.getString(2));
        roll.setNote(cursor.getString(3));
        cursor.close();
        return roll;
    }

    public void deleteRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROLLS, KEY_ROLL_ID + " = ?", new String[]{String.valueOf(roll.getId())});
        db.close();
    }

    public void updateRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_ROLLS + " SET "
                + KEY_ROLLNAME + "=\"" + roll.getName() + "\", "
                + KEY_ROLL_DATE + "=\"" + roll.getDate() + "\", "
                + KEY_ROLL_NOTE + "=\"" + roll.getNote() + "\""
                + " WHERE " + KEY_ROLL_ID + "=" + roll.getId();
        db.execSQL(query);
        db.close();
    }
}
