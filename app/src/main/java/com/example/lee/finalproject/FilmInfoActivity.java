package com.example.lee.finalproject;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * FilmInfo activity, the core activity of this program
 */
public class FilmInfoActivity extends Activity {

    ListView filmList;
    ArrayList<String> filmArrayList;
    FilmListAdapter flAdapter;
    FilmDatabaseHelper filmDatabaseHelper;
    SQLiteDatabase sqLiteDatabase;
    Button addFilm;
    Button downloadList;
    Cursor cursor;
    ProgressBar progressBar;
    TextView highest;
    TextView average;
    TextView lowest;
    static Object LOCK = new Object();
    static boolean FLAG = true;
    private String snackbarMsg = "Update movies finished";
    boolean isTablet;
    FrameLayout frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_info);
        filmList = (ListView)findViewById(R.id.film_list);
        addFilm = (Button)findViewById(R.id.add_film);
        downloadList = (Button)findViewById(R.id.download_list);
        filmArrayList = new ArrayList<>();
        flAdapter = new FilmListAdapter(this);
        filmList.setAdapter(flAdapter);
        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        frame = (FrameLayout)findViewById(R.id.frame);
        highest = (TextView)findViewById(R.id.highest);
        average = (TextView)findViewById(R.id.average);
        lowest = (TextView)findViewById(R.id.lowest);

        isTablet = frame!=null;

        filmDatabaseHelper = new FilmDatabaseHelper(this);
        sqLiteDatabase = filmDatabaseHelper.getWritableDatabase();
        refreshFilmList();

        filmList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle bundle = new Bundle();
                bundle.putInt("RequestCode", 1);  //delete and edit
                bundle.putBoolean("isTablet",isTablet);
                bundle.putLong("ID",id);
                if(isTablet){
                    MovieDetailFragment mdf = new MovieDetailFragment();
                    mdf.setArguments(bundle);
                    FragmentTransaction ft = getFragmentManager().beginTransaction().replace(R.id.frame, mdf);
                    ft.commit();
                }else{
                    Intent intent = new Intent(FilmInfoActivity.this, MovieDetail.class);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, 1);
                }
            }
        });

        addFilm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFilm();
            }
        });
        downloadList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                new MoviesUpdate().execute("http://13.59.45.15:8080/finalProject/new_movies.xml");
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                synchronized (LOCK){
                    while(FLAG){
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            Log.e("InterruptedException",null,e);
                        }
                    }
                    FLAG = true;
                }
                refreshFilmList();
                progressBar.setVisibility(View.INVISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Snackbar.make(v, snackbarMsg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == 70){ //delete
            long id = data.getLongExtra("ID", -1);
            deleteARowOnId(id);
        }else if(resultCode == 71){ // add
            refreshFilmList();
        }else if(resultCode == 72){ // edit
            refreshFilmList();
        }
    }


    private void addFilm(){

        if(isTablet){
            Bundle bundle = new Bundle();
            bundle.putInt("RequestCode", 2);  //add
            bundle.putBoolean("isTablet",isTablet);
            MovieDetailFragment mdf = new MovieDetailFragment();
            mdf.setArguments(bundle);
            FragmentTransaction ft = getFragmentManager().beginTransaction().replace(R.id.frame, mdf);
            ft.commit();
        }else{
            Bundle bundle = new Bundle();
            bundle.putInt("RequestCode", 2);  // add
            Intent intent = new Intent(FilmInfoActivity.this, MovieDetail.class);
            intent.putExtras(bundle);
            startActivityForResult(intent,2);
        }

    }

    public void refreshFilmList(){
        filmArrayList.clear();
        double high=0;
        double low=5;
        double sum=0;
        double avg=0;
        double temp=0;
        cursor = sqLiteDatabase.query( false,FilmDatabaseHelper.TABLE_NAME, new String[]{FilmDatabaseHelper.KEY_ID, FilmDatabaseHelper.KEY_TITLE, FilmDatabaseHelper.KEY_GENRE,
                FilmDatabaseHelper.KEY_ACTORS, FilmDatabaseHelper.KEY_LENGTH, FilmDatabaseHelper.KEY_RATING, FilmDatabaseHelper.KEY_DESCRIPTION,  FilmDatabaseHelper.KEY_URL},
                null,null,null,null,null,null);
        cursor.moveToFirst();
        if(cursor.getCount()<=0){
            highest.setText("");
            lowest.setText("");
            average.setText("");
            return;
        }
        while (!cursor.isAfterLast()){
            temp = cursor.getFloat(cursor.getColumnIndex(FilmDatabaseHelper.KEY_RATING));
            high = temp>high?temp:high;
            low = temp<low?temp:low;
            sum = sum+temp;
            filmArrayList.add(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_TITLE))+" "+cursor.getFloat(cursor.getColumnIndex(FilmDatabaseHelper.KEY_RATING)));
            cursor.moveToNext();
        }
        avg = sum/cursor.getCount();
        flAdapter.notifyDataSetChanged();
        BigDecimal bigDAvg = BigDecimal.valueOf(avg).setScale(1,BigDecimal.ROUND_HALF_UP);
        highest.setText("Highest: "+ String.valueOf(high));
        lowest.setText("Lowest: "+String.valueOf(low));
        average.setText("Average: "+bigDAvg.toPlainString());
    }

    void deleteARowOnId(long id){
        sqLiteDatabase.delete(FilmDatabaseHelper.TABLE_NAME, FilmDatabaseHelper.KEY_ID+"=?", new String[]{String.valueOf(id)});
        refreshFilmList();
        flAdapter.notifyDataSetChanged();
    }


    private class FilmListAdapter extends ArrayAdapter<String>{

        private Context ctx;
        protected FilmListAdapter(Context ctx){
            super(ctx,0);
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return filmArrayList.size();
        }

        @Override
        public String getItem(int position) {
            return filmArrayList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = FilmInfoActivity.this.getLayoutInflater();
            View result = null;
            result = inflater.inflate(R.layout.film_brief_layout, null);
            TextView filmBrief = (TextView) result.findViewById(R.id.film_brief);
            ImageView imageView = (ImageView) result.findViewById(R.id.small_poster);
            String newName = getItem(position).toString().replaceAll("\\s+","")+".png";
            Bitmap poster = null;
            if(fileExistance(newName)){
                FileInputStream fis = null;
                try{
                    fis = ctx.openFileInput(newName);
                    poster = BitmapFactory.decodeStream(fis);
                } catch (FileNotFoundException e) {
                    Log.e("FileNotFound",newName,e);
                }
            }else {
                poster = BitmapFactory.decodeResource(getContext().getResources() ,R.drawable.movie_tape);
            }
            imageView.setImageBitmap(poster);
            filmBrief.setText(getItem(position));
            return result;

        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            return cursor.getInt(cursor.getColumnIndex(FilmDatabaseHelper.KEY_ID));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(this.getClass().getSimpleName(),"in onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(this.getClass().getSimpleName(), "in onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(this.getClass().getSimpleName(), "in onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(this.getClass().getSimpleName(), "in onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        sqLiteDatabase.close();
        Log.i(this.getClass().getSimpleName(), "in onDestroy()");
    }

    class MoviesUpdate extends AsyncTask<String, Integer, String>{

        @Override
        protected String doInBackground(String... strings) {
            InputStream in = null;
            String urlString = strings[0];;
            try{
                publishProgress(0);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                //Starts the query
                conn.connect();
                in  = conn.getInputStream();
                XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = xppFactory.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, "UTF-8");
                int eventType = parser.getEventType();
                ContentValues cv = new ContentValues();
                publishProgress(30);
                while (eventType != XmlPullParser.END_DOCUMENT){
                    switch (eventType){
                        case XmlPullParser.START_TAG:
                            String name = parser.getName();
                            if(("title").equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_TITLE, parser.nextText());
                            }else if ("actors".equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_ACTORS, parser.nextText());
                            }else if("length".equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_LENGTH, parser.nextText());
                            }else if("description".equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_DESCRIPTION, parser.nextText());
                            }else if("rating".equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_RATING, parser.nextText());
                            }else if("genre".equalsIgnoreCase(name)){
                                String[] genreArr = getResources().getStringArray(R.array.spingarr);
                                int genreId =  Integer.parseInt(parser.nextText());
                                cv.put(FilmDatabaseHelper.KEY_GENRE, genreArr[genreId]);
                            }else if("url".equalsIgnoreCase(name)){
                                cv.put(FilmDatabaseHelper.KEY_URL, parser.nextText());
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            String endTag = parser.getName();
                            if("movie".equalsIgnoreCase(endTag)){
                                sqLiteDatabase.insertWithOnConflict(FilmDatabaseHelper.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                                cv.clear();
                            }
                            break;
                    }
                    eventType = parser.next();
                }
                publishProgress(60);

            } catch (MalformedURLException e) {
                Log.e("MalformedURL",urlString ,e);
            } catch (IOException e) {
                Log.e("IOException",urlString,e);
            } catch (XmlPullParserException e) {
                Log.e("XmlPullParserException",urlString,e);
            }finally {
                if (in!=null){
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e("IOException",urlString,e);
                    }
                }
            }
            publishProgress(100);
            synchronized (LOCK){
                FLAG = false;
                LOCK.notifyAll();
            }


            return null;

        }

        @Override
        protected void onPostExecute(String s) {


        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }
    }


    public boolean fileExistance(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        if(file.exists()){
            Log.i("File Path:",file.getAbsolutePath());
        }
        return file.exists();
    }

}
