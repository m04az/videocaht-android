package com.m0aaz.myandroidrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity {



    String userName;
    String pass;
    String _id;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getApplication().getSharedPreferences("mySharedPreferences", 0);

        setContentView(R.layout.activity_login);
        final EditText userIn = (EditText) findViewById(R.id.userin);

        final Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = String.valueOf(userIn.getText());
                prefs.edit().putString("username", userName).commit();
                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
                    }




        });
    }


}
