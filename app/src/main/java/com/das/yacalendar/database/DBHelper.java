package com.das.yacalendar.database;

/**
 * Created by yaturner on 3/20/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.das.yacalendar.Constants;
import com.das.yacalendar.notes.Note;
import com.das.yacalendar.yacalendar;
import com.google.common.collect.ArrayListMultimap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Helper class for DB creation/updating
public class DBHelper extends SQLiteOpenHelper
{
    // Your local name for the schema
    private static final String DB_NAME = "das_calendar";
    private static DBHelper sInstance;
    private SQLiteDatabase database;
    private int databaseOpenRefCount = 0;
    private final static String[] DELETE_STATEMENTS =
            {
                    "DROP TABLE IF EXISTS " + CalendarContract.NOTE_TABLE_NAME,
                    "DROP TABLE IF EXISTS " + CalendarContract.INFO_TABLE_NAME
            };

    public DBHelper(Context context)
    {
        super(context, DB_NAME, null, Constants.CURRENT_DB_VERSION);
    }

    /**
     * Creates any of our tables.
     * This function is only run once or after every Clear Data
     */
    @Override
    public void onCreate(SQLiteDatabase database)
    {
        try
        {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + CalendarContract.INFO_TABLE_NAME + "(" +
                            CalendarContract.CalendarInfoEntry._ID + " integer primary key autoincrement, " +
                            CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_NPO_NAME + " TEXT, " +
                            CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_CALENDAR_VERSION + " INTEGER, " +
                            CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_START_DATE + " TEXT, " +
                            CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_END_DATE + " TEXT " +
                            ");");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS "+ CalendarContract.NOTE_TABLE_NAME + "(" +
                            CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID + " INTEGER primary key autoincrement, " +
                            CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY + " INTEGER, " +
                            CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT + " TEXT, " +
                            CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE + " TEXT, " +
                            CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE + " INTEGER " +
                    ");");

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public synchronized SQLiteDatabase openDatabase()
    {
        if (databaseOpenRefCount == 0)
        {
            databaseOpenRefCount++;
            database = this.getWritableDatabase();
        }
        return database;
    }

    public synchronized void closeDatabase()
    {
//        databaseOpenRefCount--;
//        if (databaseOpenRefCount == 0)
//        {
//            database.close();
//        }
    }

    public void clearDatabase(final String which_table)
    {
        SQLiteDatabase db = openDatabase();
        int index = 0;

        database.execSQL(DELETE_STATEMENTS[index]);
    }

       /**
     * addInfo
     *
     * @param version
     * @param startDate
     * @param endDate
     * @return
     */
    public long addInfo(final int version, final String startDate, final String endDate)
    {
        SQLiteDatabase db = this.openDatabase();
        long info_id = -1L;
        ContentValues values = new ContentValues();

        //force replace
        values.put(CalendarContract.CalendarInfoEntry._ID, 0);
        values.put(CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_NPO_NAME, "das");
        values.put( CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_CALENDAR_VERSION, version);
        values.put(CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_START_DATE, startDate);
        values.put(CalendarContract.CalendarInfoEntry.COLUMN_NAME_INFO_END_DATE, endDate);

        info_id = db.insertWithOnConflict(CalendarContract.INFO_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        closeDatabase();

        return info_id;
    }

    /**
     * addNote
     *
     * @param note users can only add notes to the local database
     */
    public long addNote(final Note note)
    {
        SQLiteDatabase db = this.openDatabase();
        long note_id = -1L;

        if (note != null)
        {
            String dateString = yacalendar.formatDate(note.getDate(), Constants.DATABASE_SHORT_DATE_FORMAT);

            //escape single quotes in text
            String text = note.getText();
            text = text.replace("'", "''");

            //does it already exist in the database
            Cursor c =  db.query(CalendarContract.NOTE_TABLE_NAME, null,
                    CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY + " = '"+ note.getPriority() + "' and " +
                    CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT + " = '"+ text + "' and " +
                    CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE + " = '"+ dateString + "' and " +
                    CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE + " = '"+ (note.isEditable()?1:0) + "'",
                    null, null, null, null);

            if(c.getCount() > 0)
            {
                c.moveToFirst();
                note_id = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID));
                c.close();
                return note_id;
            }

            ContentValues values = new ContentValues();

            values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY, note.getPriority());
            values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT, text);
            values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE, dateString);
            values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE, (note.isEditable()?1:0));


            note_id = db.insertWithOnConflict(CalendarContract.NOTE_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            closeDatabase();

            return note_id;
        }
        else
        {
            return -1L;
        }
    }

    /**
     * updateNotes
     *
     * @param notes
     */
    public void updateNotes(List<Note> notes)
    {
        if(notes != null && notes.size() > 0)
        {
            for(Note note : notes)
            {
                updateNote(note);
            }
        }
    }

    public void updateNote(Note note)
    {
        SQLiteDatabase db = this.openDatabase();

        String dateString = yacalendar.formatDate(note.getDate(), Constants.DATABASE_SHORT_DATE_FORMAT);

        //escape single quotes in text
        String text = note.getText();
        text = text.replace("'", "''");

        ContentValues values = new ContentValues();

        values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID, note.getId());
        values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY, note.getPriority());
        values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT, text);
        values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE, dateString);
        values.put(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE, (note.isEditable()?1:0));


        db.replace(CalendarContract.NOTE_TABLE_NAME, null, values);

        closeDatabase();
    }

    public boolean removeNote(final Note note)
    {
        SQLiteDatabase db = this.openDatabase();
        String whereClause = CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID + "=?";
        String[] whereArgs = new String[] { String.valueOf(note.getId()) };
        return db.delete(CalendarContract.NOTE_TABLE_NAME, whereClause, whereArgs) > 0;
    }

    /**
     * getNote
     *
     * @param id
     * @return
     */
    public Note getNote(final int id)
    {
        SQLiteDatabase db = this.openDatabase();

        Cursor c =  db.query(CalendarContract.NOTE_TABLE_NAME, null, "_id=" + id, null, null, null, null);

        if(c.getCount() == 0)
        {
            return null;
        }

        Calendar date = Calendar.getInstance();

        c.moveToFirst();
        int _id = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID));
        int priority = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY));
        String text = c.getString(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT));
        String dateString = c.getString(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE));
        date.setTime(yacalendar.parseDate(dateString, Constants.DATABASE_SHORT_DATE_FORMAT));
        int editable = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE));
        Note note = new Note(_id, date, priority, text, (editable==1)?true:false);

        c.close();

        closeDatabase();

        return note;
    }

    public ArrayListMultimap<Integer, Note> getNotesForMonth(final int month, final int year)
    {
        SQLiteDatabase db = this.openDatabase();
        String month2 = (month<10)?"0"+month:""+month;
        ArrayListMultimap<Integer, Note> notes = ArrayListMultimap.create();

        Cursor c =  db.query(CalendarContract.NOTE_TABLE_NAME, null,
                "date between '" + year+month2+"01' and '"+year+month2+"31'", null, null, null, null);

        if(c.getCount() > 0) {
            Calendar date = Calendar.getInstance();

            c.moveToFirst();
            do {
                int _id = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_ID));
                int priority = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_PRIORITY));
                String text = c.getString(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_TEXT));
                String dateString = c.getString(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_DATE));
                date.setTime(yacalendar.parseDate(dateString, Constants.DATABASE_SHORT_DATE_FORMAT));
                int editable = c.getInt(c.getColumnIndex(CalendarContract.CalendarNoteEntry.COLUMN_NAME_NOTE_EDITABLE));

                Note note = new Note(_id, date, priority, text, (editable==1)?true:false);
                notes.put(date.get(Calendar.DAY_OF_MONTH), note);
            } while (c.moveToNext());
        }
        c.close();

        closeDatabase();

        return notes;
    }

    /**
     * Handle all database version upgrades
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        database.beginTransaction();
        boolean okay = true;
    }

    /**
     * Check if a table exists. Useful for new table creation in onUpgrade
     */
    private boolean checkTable(SQLiteDatabase pDatabase, String pTable)
    {
        try
        {
            Cursor c = pDatabase.query(pTable, null, null, null, null, null, null);
            if (c == null)
            {
                return false;
            }
            c.close();
        } catch (Exception e)
        {
            return false;
        }
        return true;
    }

    /**
     * Return current date as a string
     */
    public static String getDate()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date date = new Date();
        return sdf.format(date);
    }

    public String buildSQLStatementString(String start, String tableName, String[] fields)
    {
        StringBuilder sql = new StringBuilder(start + " INTO " + tableName + " (");

        int fieldsCount = fields.length;
        for (int i = 0; i < fieldsCount; i++)
        {
            sql.append(fields[i]);
            if (i < fieldsCount - 1)
            {
                sql.append(",");
            }
        }
        sql.append(") VALUES (");
        for (int i = 0; i < fieldsCount; i++)
        {
            sql.append("?");
            if (i < fieldsCount - 1)
            {
                sql.append(", ");
            }
        }
        sql.append(");");

        return sql.toString();
    }

}