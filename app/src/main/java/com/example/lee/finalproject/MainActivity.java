package com.example.lee.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * MainActivity of program entrance
 */
public class MainActivity extends AppCompatActivity{



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


    }

    public boolean onCreateOptionsMenu(Menu m){
        getMenuInflater().inflate(R.menu.main_menu, m );
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem mi){
        switch (mi.getItemId()){
            case R.id.action_one:
                Log.d("Toolbar", "Start option selected");
                Intent intent = new Intent(MainActivity.this,FilmInfoActivity.class);
                startActivity(intent);
                break;
            case R.id.help:
                Log.d("Toolbar", "Help option selected");
                Intent helpItent = new Intent(MainActivity.this,HelpInfo.class);
                startActivity(helpItent);
                break;
        }
        return true;
    }
}
