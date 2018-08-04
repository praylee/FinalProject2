package com.example.lee.finalproject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by LEE on 2018-04-26.
 */

public class FilmDatabaseHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "Movies.db";
    private static final int VERSION_NUM = 1;

    protected static final String TABLE_NAME = "MOVIESTABLE";
    protected static final String KEY_ID = "ID";
    protected static final String KEY_TITLE = "TITLE";
    protected static final String KEY_ACTORS = "ACTORS";
    protected static final String KEY_LENGTH = "LENGTH";
    protected static final String KEY_DESCRIPTION = "DESCRIPTION";
    protected static final String KEY_RATING = "RATING";
    protected static final String KEY_GENRE = "GENRE";
    protected static final String KEY_URL = "URL";



    public FilmDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION_NUM);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(this.getClass().getSimpleName(), "Calling onCreate");
        db.execSQL("CREATE TABLE "
                + TABLE_NAME + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT NOT NULL UNIQUE, "
                + KEY_ACTORS + " TEXT, "
                + KEY_LENGTH + " INTEGER, "
                + KEY_DESCRIPTION + " TEXT, "
                + KEY_RATING + " FLOAT, "
                + KEY_GENRE + " TEXT, "
                + KEY_URL + " TEXT "
                + ")" );    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(this.getClass().getSimpleName(), "Calling onUpgrade, oldVersion=" + oldVersion + "newVersion=" + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
