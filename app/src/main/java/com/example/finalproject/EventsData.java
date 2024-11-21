package com.example.finalproject;
import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.MediaColumns.TITLE;
import static com.example.finalproject.Constants.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EventsData extends SQLiteOpenHelper {
    public EventsData(Context ctx){
        super(ctx, "events.db", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "
                + IMAGE + " TEXT, "
                + MONEY + " DOUBLE NOT NULL, "
                + TYPE + " INTEGER NOT NULL, "
                + CATEGORY + " TEXT NOT NULL, "
                + DESCRIPTION + " TEXT, "
                + DATE + " TEXT NOT NULL, "
                + TIME + " TEXT NOT NULL, "
                + RECEIVER + " TEXT);"  );
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
