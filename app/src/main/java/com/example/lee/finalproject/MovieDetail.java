package com.example.lee.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

public class MovieDetail extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        MovieDetailFragment mdf = new MovieDetailFragment();
        mdf.setArguments(bundle);
        getFragmentManager().beginTransaction().add(R.id.movie_detail_layout, mdf).commit();

    }
}
