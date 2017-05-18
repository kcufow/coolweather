package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.coolweather.android.Constant;
import com.coolweather.android.WeatherActivity;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.Httputil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       updateWeather();
       undateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 *60 *60*1000;//8个小时的毫秒数
        long triggerattime = SystemClock.elapsedRealtime()+anHour;
        Intent i = new Intent(this , AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerattime,pi);

        return super.onStartCommand(intent, flags, startId);
    }

    private void undateBingPic() {
        String requestBingPic = Constant.bingPicUrl;
        Httputil.sendOkHttpREquest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString(Constant.BING_PIC,bingPic);
                editor.apply();

            }
        });

    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString(Constant.WEATHER_STRING,null);
        if (weatherString!=null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = Constant.weatherBaseUrl+weatherId+Constant.key;
            Httputil.sendOkHttpREquest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather!=null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString(Constant.WEATHER_STRING,responseText);
                        editor.apply();
                    }

                }
            });
        }

    }
}
