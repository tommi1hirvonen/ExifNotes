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

    public static final String KEY_ID = "_id";
    public static final String KEY_ROLLNAME = "rollname";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DATE = "date";
    public static final String KEY_LENS = "lens";
    public static final String KEY_SHUTTER = "shutter";
    public static final String KEY_APERTURE = "aperture";

//    private static final String[] TABLE_FRAMES_COLUMNS = {KEY_ID, KEY_ROLLNAME, KEY_COUNT,
//            KEY_DATE, KEY_LENS, KEY_SHUTTER, KEY_APERTURE};
//    private static final String[] TABLE_LENSES_COLUMNS = {KEY_ID, KEY_LENS};
//    private static final String[] TABLE_ROLLS_COLUMNS = {KEY_ID, KEY_ROLL};

    private static final String DATABASE_NAME = "filmnotes.db";
    private static final int DATABASE_VERSION = 5;

    private static final String CREATE_FRAME_TABLE = "create table " + TABLE_FRAMES
            + "(" + KEY_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null, "
            + KEY_COUNT + " integer, "
            + KEY_DATE + " text not null, "
            + KEY_LENS + " text not null, "
            + KEY_SHUTTER + " text not null, "
            + KEY_APERTURE + " text not null"
            + ");";
    private static final String CREATE_LENS_TABLE = "create table " + TABLE_LENSES
            + "(" + KEY_ID + " integer primary key autoincrement, "
            + KEY_LENS + " text not null"
            + ");";
    private static final String CREATE_ROLL_TABLE = "create table " + TABLE_ROLLS
            + "(" + KEY_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null"
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

    // ********** CRUD operations for the frames table **********

    public void addFrame(Frame frame) {

        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();

        // Create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ROLLNAME, frame.getRoll());
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

//    public Frame getFrame(int id){
//
//        // Get reference to readable database
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Frame frame = new Frame();
//
//        // Build query
//        Cursor cursor =
//                db.query(TABLE_FRAMES, TABLE_FRAMES_COLUMNS, "_id = ?", new String[] {String.valueOf(id)}, null, null, null, null);
//
//        // If we got results get the first one
//        if (cursor != null) cursor.moveToFirst();
//
//        frame.setId(cursor.getInt(0));
//        frame.setRoll(cursor.getString(1));
//        frame.setCount(cursor.getInt(2));
//        frame.setDate(cursor.getString(3));
//        frame.setLens(cursor.getString(4));
//        frame.setShutter(cursor.getString(5));
//        frame.setAperture(cursor.getString(6));
//
//        cursor.close();
//
//        return frame;
//    }

    public Frame getLastFrame(){
        Frame frame = new Frame();

        String query = "SELECT * FROM frames ORDER BY _id DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if ( cursor != null ) cursor.moveToFirst();

        frame.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
        frame.setRoll(cursor.getString(cursor.getColumnIndex(KEY_ROLLNAME)));
        frame.setCount(cursor.getInt(cursor.getColumnIndex(KEY_COUNT)));
        frame.setDate(cursor.getString(cursor.getColumnIndex(KEY_DATE)));
        frame.setLens(cursor.getString(cursor.getColumnIndex(KEY_LENS)));
        frame.setShutter(cursor.getString(cursor.getColumnIndex(KEY_SHUTTER)));
        frame.setAperture(cursor.getString(cursor.getColumnIndex(KEY_APERTURE)));

        cursor.close();

        return frame;
    }

    public ArrayList<Frame> getAllFramesFromRoll(String rollName){
        ArrayList<Frame> frames = new ArrayList<>();

        // Build the query
        String query = "SELECT * FROM " + TABLE_FRAMES + " WHERE " + KEY_ROLLNAME + " = \"" + rollName + "\"";

        // Get reference to readable database
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        Frame frame;

        // Go over each row, build list
        while ( cursor.moveToNext() ) {
            frame = new Frame();

            frame.setId(cursor.getInt(0));
            frame.setRoll(cursor.getString(1));
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
                + KEY_ROLLNAME + "=\"" + frame.getRoll() + "\", "
                + KEY_COUNT + "=" + frame.getCount() + ", "
                + KEY_DATE + "=\"" + frame.getDate() + "\", "
                + KEY_LENS + "=\"" + frame.getLens() + "\", "
                + KEY_SHUTTER + "=\"" + frame.getShutter() + "\", "
                + KEY_APERTURE + "=\"" + frame.getAperture() + "\"";

        db.execSQL(query);

        db.close();

    }



    public void deleteFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete
        db.delete(TABLE_FRAMES,
                KEY_ID + " = ?",
                new String[]{String.valueOf(frame.getId())});

        // Close
        db.close();
    }

    public void deleteAllFramesFromRoll(String rollName){
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_FRAMES, KEY_ROLLNAME + " = ? ", new String[]{String.valueOf(rollName)});

        db.close();
    }

    public void renameAllFramesFromRoll(String oldName, String newName){
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "UPDATE " + TABLE_FRAMES + " SET " + KEY_ROLLNAME + "=\"" + newName + "\" WHERE " + KEY_ROLLNAME + "=\"" +oldName + "\";";

        db.execSQL(query);

        db.close();
    }

    // ********** CRUD operations for the lenses table **********

    public void addLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LENS, lens.getName());

        db.insert(TABLE_LENSES, null, values);

        db.close();
    }

//    public Lens getLens(int id){
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Lens lens = new Lens();
//
//        Cursor cursor = db.query(TABLE_LENSES, TABLE_LENSES_COLUMNS, "_id = ?", new String[] {String.valueOf(id)}, null, null, null, null);
//
//        if ( cursor != null ) cursor.moveToFirst();
//
//        lens.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
//        lens.setName(cursor.getString(cursor.getColumnIndex(KEY_LENS)));
//
//        cursor.close();
//
//        return lens;
//    }
//
//    public int getLensId(String name){
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        String query = "SELECT " + KEY_ID + " FROM " + TABLE_LENSES + " WHERE " + KEY_LENS + "=\"" + name + "\"";
//
//        Cursor cursor = db.rawQuery(query, null);
//
//        if ( cursor != null ) cursor.moveToFirst();
//
//        return cursor.getInt(cursor.getColumnIndex(KEY_ID));
//    }

    public Lens getLastLens(){
        Lens lens = new Lens();
        String query = "SELECT * FROM lenses ORDER BY _id DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if ( cursor != null ) cursor.moveToFirst();

        lens.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
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

//    public int updateLens(Lens lens){
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(KEY_LENS, lens.getName());
//
//        // Updating the row. db.update() returns the number of rows affected.
//        int i = db.update(TABLE_LENSES, values, KEY_ID + " = ?", new String[]{String.valueOf(lens.getId())});
//
//        db.close();
//
//        return i;
//    }

    public void deleteLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_LENSES, KEY_ID + " = ?", new String[]{String.valueOf(lens.getId())});

        db.close();
    }

    // ********** CRUD operations for the rolls table **********

    public void addRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ROLLNAME, roll.getName());
        db.insert(TABLE_ROLLS, null, values);
        db.close();
    }

    public Roll getLastRoll(){
        Roll roll = new Roll();
        String query = "SELECT * FROM " + TABLE_ROLLS
                + " ORDER BY " + KEY_ID
                + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        roll.setId(cursor.getInt(0));
        roll.setName(cursor.getString(1));
        cursor.close();
        return roll;
    }

    public ArrayList<Roll> getAllRolls(){
        ArrayList<Roll> rolls = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_ROLLS + " ORDER BY " + KEY_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Roll roll;
        while ( cursor.moveToNext() ) {
            roll = new Roll();
            roll.setId(cursor.getInt(0));
            roll.setName(cursor.getString(1));
            rolls.add(roll);
        }
        cursor.close();
        return rolls;
    }

    public void deleteRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROLLS, KEY_ID + " = ?", new String[]{String.valueOf(roll.getId())});
        db.close();
    }

    public void updateRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_ROLLS + " SET "
                + KEY_ROLLNAME + "=\"" + roll.getName() + "\"";
        db.execSQL(query);
        db.close();
    }

}
