package com.zeenom.login.plugin;

import android.util.Log;

public class LoginPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value + "from android";
    }
}
