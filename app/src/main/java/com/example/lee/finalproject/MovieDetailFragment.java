package com.example.lee.finalproject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class MovieDetailFragment extends Fragment {

    View fragmentView;
    TextView movieId;
    TextView movieTitle;
    TextView movieActors;
    TextView movieLength;
    TextView movieDescription;
    RatingBar movieRatingBar;
    Spinner movieGenreSpinner;
    TextView movieURL;
    ImageView posterImage;

    Button buttonDelete;
    Button buttonModifyContent;
    Bundle bundle;
    SQLiteDatabase sqLiteDatabase;
    FilmDatabaseHelper filmDatabaseHelper;
    Cursor cursor;

    public String[] movieArray;
    List<String> movieArrayList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bundle = getArguments();
        filmDatabaseHelper = new FilmDatabaseHelper(getActivity());
        sqLiteDatabase = filmDatabaseHelper.getReadableDatabase();
        movieArray = getResources().getStringArray(R.array.spingarr);
        movieArrayList = Arrays.asList(movieArray);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final boolean isTablet = bundle.getBoolean("isTablet");
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_movie_detail, container, false);
        movieTitle = (TextView) fragmentView.findViewById(R.id.edit_movie_title);
        movieActors = (TextView) fragmentView.findViewById(R.id.edit_movie_actors);
        movieLength = (TextView) fragmentView.findViewById(R.id.edit_movie_length);
        movieDescription = (TextView) fragmentView.findViewById(R.id.edit_movie_description);
        movieRatingBar = (RatingBar)fragmentView.findViewById(R.id.movie_ratingBar);
        movieGenreSpinner = (Spinner) fragmentView.findViewById(R.id.genre_spinner);
        movieURL = (TextView)fragmentView.findViewById(R.id.edit_movie_URL);
        posterImage = (ImageView)fragmentView.findViewById(R.id.poster_image);

        buttonDelete = (Button)fragmentView.findViewById(R.id.button_delete);
        buttonModifyContent = (Button)fragmentView.findViewById(R.id.modify_content);

        final long id = bundle.getLong("ID");
        final long requestCode = bundle.getInt("RequestCode");
        if(requestCode == 1){ //delete and edit
            cursor = sqLiteDatabase.query( false,FilmDatabaseHelper.TABLE_NAME, new String[]{FilmDatabaseHelper.KEY_ID, FilmDatabaseHelper.KEY_TITLE, FilmDatabaseHelper.KEY_GENRE,
                    FilmDatabaseHelper.KEY_ACTORS, FilmDatabaseHelper.KEY_LENGTH, FilmDatabaseHelper.KEY_RATING, FilmDatabaseHelper.KEY_DESCRIPTION,  FilmDatabaseHelper.KEY_URL}, "ID=?",new String[]{String.valueOf(id)},null,null,null,null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()){
                movieTitle.setText(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_TITLE)));
                movieActors.setText(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_ACTORS)));
                movieLength.setText(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_LENGTH)));
                movieDescription.setText(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_DESCRIPTION)));
                movieRatingBar.setRating(cursor.getFloat(cursor.getColumnIndex(FilmDatabaseHelper.KEY_RATING)));
                movieGenreSpinner.setSelection(movieArrayList.indexOf(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_GENRE))));
                movieURL.setText(cursor.getString(cursor.getColumnIndex(FilmDatabaseHelper.KEY_URL)));
                cursor.moveToNext();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    new DownloadImageFromUrlTask().execute(movieURL.getText().toString().trim());
                }
            }).start();


        }else if(requestCode == 2){ // add
            buttonDelete.setVisibility(View.INVISIBLE); //delete Button set to invisible
            buttonModifyContent.setText(R.string.movie_add);
        }

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isTablet){
                    FilmInfoActivity fia = (FilmInfoActivity)getActivity();
                    fia.deleteARowOnId(id);
                    getFragmentManager().beginTransaction().remove(MovieDetailFragment.this).commit();
                }else{
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putLong("ID",id);
                    intent.putExtras(bundle);
                    getActivity().setResult(70,intent);
                    getActivity().finish();
                }
            }
        });
        buttonModifyContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(!fieldValidated()){
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put(FilmDatabaseHelper.KEY_TITLE,movieTitle.getText().toString());
            cv.put(FilmDatabaseHelper.KEY_ACTORS,movieActors.getText().toString());
            cv.put(FilmDatabaseHelper.KEY_DESCRIPTION,movieDescription.getText().toString());
            cv.put(FilmDatabaseHelper.KEY_GENRE, (String)movieGenreSpinner.getSelectedItem());
            cv.put(FilmDatabaseHelper.KEY_LENGTH, Integer.parseInt(movieLength.getText().toString()));
            cv.put(FilmDatabaseHelper.KEY_RATING,movieRatingBar.getRating());
            cv.put(FilmDatabaseHelper.KEY_URL,movieURL.getText().toString());

                if(requestCode == 2){ // add
                    if(isTablet){
                        sqLiteDatabase.insert(FilmDatabaseHelper.TABLE_NAME, null, cv);
                        FilmInfoActivity fia = (FilmInfoActivity)getActivity();
                        fia.refreshFilmList();
                        getFragmentManager().beginTransaction().remove(MovieDetailFragment.this).commit();
                        Toast.makeText(getActivity(),"New movie is added", Toast.LENGTH_SHORT).show();
                    }else{
                        sqLiteDatabase.insert(FilmDatabaseHelper.TABLE_NAME, null, cv);
                        getActivity().setResult(71);
                        getActivity().finish();
                        Toast.makeText(getActivity(),"New movie is added", Toast.LENGTH_SHORT).show();
                    }
                }else if (requestCode == 1){ //edit
                    sqLiteDatabase.update(FilmDatabaseHelper.TABLE_NAME, cv,FilmDatabaseHelper.KEY_ID+"=?",new String[]{String.valueOf(id)});
                    getActivity().setResult(72);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String fileName = getOutputFileName();
                            File file = getActivity().getBaseContext().getFileStreamPath(fileName);
                            if(file.exists()){
                                file.delete();
                            }
                            new DownloadImageFromUrlTask().execute(movieURL.getText().toString().trim());
                        }
                    }).start();
                    Toast.makeText(getActivity(),"Modification has been saved. Refreshing", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return  fragmentView;

    }

    public boolean fileExistance(String fname){
        File file = getActivity().getBaseContext().getFileStreamPath(fname);
        if(file.exists()){
            Log.i("File Path:",file.getAbsolutePath());
        }
        return file.exists();
    }

    class DownloadImageFromUrlTask extends AsyncTask<String,Integer,Bitmap> {

        @Override
        protected Bitmap doInBackground(String... urls) {

            String fileName = getOutputFileName();
            Bitmap poster = null;
            /*
             * If the ImageFile doesn't exist, then download it from the URL. If it exist, then load it.
             */
            if(!fileExistance(fileName)){
                Log.i("File doesn't exist",fileName);
                poster = getImageBitmap(urls[0]);
                if(poster!=null){
                    SavaImage(poster, fileName);
                }
            }else{
                Log.i("File exist", fileName);
                FileInputStream fis = null;
                try{
                    fis = getActivity().openFileInput(fileName);
                    poster = BitmapFactory.decodeStream(fis);
                } catch (FileNotFoundException e) {
                    Log.e("FileNotFound",fileName,e);
                }
            }

            /*
             * If the file is downLoaded successfully, then return it. Otherwise, load the default.
             */
            if(poster==null){
                poster = BitmapFactory.decodeResource(getContext().getResources() ,R.drawable.movie_tape);
            }
            return poster;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if(bitmap!=null){
                posterImage.setImageBitmap(bitmap);
            }
        }
    }
    public Bitmap getImageBitmap(String url){
        URL imgUrl = null;
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            imgUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imgUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (MalformedURLException e) {
            Log.i("MalformedURLException",url,e);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Invalid URL, please check", Toast.LENGTH_LONG).show();
                }
            });
        } catch(FileNotFoundException e){
            Log.i("Image not found in URL", url, e);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "No image found in URL, please check", Toast.LENGTH_LONG).show();
                }
            });
        } catch (IOException e) {
            Log.e("IO Exception",url,e);

        }
        finally {
            if(is!= null){
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e("No image in URL",url,e);
                }
            }
        }
        return bitmap;
    }


    public boolean fieldValidated(){
        CharSequence text = "";
        boolean valid = true;
        if(movieTitle.getText()==null||"".equals(movieTitle.getText().toString().trim())){
            text = "Please enter movie title";
            valid = false;
        }else if(movieActors.getText()==null||"".equals(movieActors.getText().toString().trim())){
            text = "Please enter Movie Actors";
            valid = false;
        }else if(movieLength.getText()==null||"".equals(movieLength.getText().toString().trim())){
            text = "Please enter Movie Length";
            valid = false;
        }else if(movieDescription.getText()==null||"".equals(movieDescription.getText().toString().trim())){
            text = "Please enter Movie Description";
            valid = false;
        }else if(movieURL.getText()==null||"".equals(movieURL.getText().toString().trim())){
            text = "Please enter Movie URL";
            valid = false;
        }
        if (!valid){
            Toast toast = Toast.makeText(getActivity(),text,Toast.LENGTH_SHORT);
            toast.show();
        }
        return valid;
    }


    public void SavaImage(Bitmap bitmap, String fileName){
        FileOutputStream fileOutputStream=null;
        try {
            fileOutputStream = getContext().openFileOutput(fileName,getContext().MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100,fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException",fileName,e);
        } catch (IOException e) {
            Log.e("IO Exception",fileName,e);
        }
    }


    public String getOutputFileName(){
        String filename = movieTitle.getText().toString()+movieRatingBar.getRating()+".png";
        String newName = filename.replaceAll("\\s+","");
        return newName;
    }


}
