package com.example.genny.weatherapp;
/**
 * Author: Genny Centeno
 * Last updated on: Nov 8/2018
 */
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private final String API_KEY = "b38343d8b690eb3a2ad740fd9bb24968";
    private final String CELSIUS_UNITS = "metric";
    private String cityName;
    private EditText cityEditTxt;
    private Runnable runnable;
    private Resources res;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get resources such as strings
        res = getResources();

        cityEditTxt = findViewById(R.id.cityEditTxt);

        //Set Halifax as default city
        cityName = "Halifax";
        //Search for Halifax and set all values
        getCityResult();


        //On touch listener for search icon on Edit Text View
        cityEditTxt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    final int DRAWABLE_RIGHT = 2;
                    if (event.getRawX() >= (cityEditTxt.getRight() - cityEditTxt.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        //Run search request on a different thread
                        runOnThread();
                        return true;
                    }
                }
                return false;
            }
        });

        //On Editor Action Listener for search on keyboard
        cityEditTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //Run search request on a different thread
                    runOnThread();
                    return true;
                }
                return false;
            }
        });
    }

    public void runOnThread() {
        cityName = cityEditTxt.getText().toString();

        runnable = new Runnable() {
            @Override
            public void run() {
                getCityResult();
            }
        };

        Thread thread = new Thread(null, runnable, "background");
        thread.start();

        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Performs the request to OpenWeatherAPI to get the information about the requested city
     */
    public void getCityResult() {
        String cityNameStr = TextUtils.isEmpty(cityName) ? "Halifax" : cityName;
        final String url = "http://api.openweathermap.org/data/2.5/weather?q=" + cityNameStr + "&appid=" + API_KEY + "&units=" + CELSIUS_UNITS;
        //build the request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {
                    JSONArray weather = response.getJSONArray("weather");
                    JSONObject main = response.getJSONObject("main");
                    JSONObject cloudsJSON = response.getJSONObject("clouds");

                    //Set values on layout
                    setCityNameOnLayout(response);
                    setWeather(weather);
                    setTemperature(main);
                    setMinMaxTemperature(main);
                    setHumidity(main);
                    setClouds(cloudsJSON);
                    setWeatherIcon((weather.getJSONObject(0)).get("icon").toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

                Toast.makeText(getApplicationContext(), "Please, introduce an existing city", Toast.LENGTH_SHORT).show();
            }
        }
        );
        RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }

    /**
     * Sets the image for the result weather response
     */
    public void setWeatherIcon(String iconID) {
        final ImageView weatherImageView = findViewById(R.id.weatherIconImgV);
        int icon = getDrawableByIcon(iconID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            weatherImageView.setImageDrawable(getResources().getDrawable(icon, getApplicationContext().getTheme()));
        } else {
            weatherImageView.setImageDrawable(getResources().getDrawable(icon));
        }
    }

    /**
     * View's setter methods
     **/
    private void setCityNameOnLayout(JSONObject response) throws JSONException {
        //Set City Name Text View
        TextView cityNameTxtView = findViewById(R.id.cityNameTxtView);
        cityNameTxtView.setText(response.get("name").toString());

    }

    private void setWeather(JSONArray weather) throws JSONException {
        //Set main weather mainWeatherTxtView
        TextView mainWeatherTxtView = findViewById(R.id.mainWeatherTxtView);
        mainWeatherTxtView.setText((weather.getJSONObject(0)).get("main").toString());

        // Set mainWeatherDescTxtView
        TextView mainWeatherDescTxtView = findViewById(R.id.mainWeatherDescTxtView);
        mainWeatherDescTxtView.setText(capitalize((weather.getJSONObject(0)).get("description").toString()));
    }

    private void setTemperature(JSONObject main) throws JSONException {
        TextView temperatureTxtView = findViewById(R.id.temperatureTxtView);
        // Convert string to float
        float tempFloat = Float.parseFloat(main.get("temp").toString());
        //Round
        int temperature = Math.round(tempFloat);
        String temperatureText = String.format(res.getString(R.string.temperature), temperature);
        temperatureTxtView.setText(temperatureText);
    }

    private void setMinMaxTemperature(JSONObject main) throws JSONException {
        //Set min/max
        TextView minMaxTxtView = findViewById(R.id.minMaxTxtView);
        // Convert string to float
        float minTempFloat = Float.parseFloat(main.get("temp_min").toString());
        float maxTempFloat = Float.parseFloat(main.get("temp_max").toString());
        //Round to show an integer on layout
        int minTemp = Math.round(minTempFloat);
        int maxTemp = Math.round(maxTempFloat);
        String minMaxTempText = String.format(res.getString(R.string.minMax), minTemp, maxTemp);

        minMaxTxtView.setText(minMaxTempText);
    }

    private void setHumidity(JSONObject main) throws JSONException {
        // Set humidity
        float humidityFloat = Float.parseFloat(main.get("humidity").toString());
        int humidity = Math.round(humidityFloat);

        TextView humidityPercentageTxtView = findViewById(R.id.humidityPercentage);
        String humidityPercentageText = String.format(res.getString(R.string.humidityString), humidity);
        humidityPercentageTxtView.setText(humidityPercentageText);
    }

    private void setClouds(JSONObject cloudsJSON) throws JSONException {
        float cloudsFloat = Float.parseFloat(cloudsJSON.get("all").toString());
        int clouds = Math.round(cloudsFloat);

        TextView cloudsPercentageTxtView = findViewById(R.id.cloudsPercentage);
        String cloudsPercentageText = String.format(res.getString(R.string.cloudsStr), clouds);
        cloudsPercentageTxtView.setText(cloudsPercentageText);
    }


    /**
     * Utility methods
     **/

    //Capitalizes first word of the sentence
    private String capitalize(final String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    //Gets the icon associated with the received response
    private int getDrawableByIcon(String iconID) {
        int icon = 0;

        //Get drawable according to iconID
        // xxd are for day and xxn are for night
        switch (iconID) {
            case "01d":
                icon = R.drawable.ic_01d;
                break;
            case "02d":
                icon = R.drawable.ic_02d;
                break;
            case "03d":
                icon = R.drawable.ic_03d;
                break;
            case "04d":
                icon = R.drawable.ic_04d;
                break;
            case "09d":
                icon = R.drawable.ic_09d;
                break;
            case "10d":
                icon = R.drawable.ic_10d;
                break;
            case "11d":
                icon = R.drawable.ic_11d;
                break;
            case "13d":
                icon = R.drawable.ic_13d;
                break;
            case "50d":
                icon = R.drawable.ic_50d;
                break;
            case "01n":
                icon = R.drawable.ic_01n;
                break;
            case "02n":
                icon = R.drawable.ic_02n;
                break;
            case "03n":
                icon = R.drawable.ic_03n;
                break;
            case "04n":
                icon = R.drawable.ic_04n;
                break;
            case "09n":
                icon = R.drawable.ic_09n;
                break;
            case "10n":
                icon = R.drawable.ic_10n;
                break;
            case "11n":
                icon = R.drawable.ic_11n;
                break;
            case "13n":
                icon = R.drawable.ic_13n;
                break;
            case "50n":
                icon = R.drawable.ic_50n;
                break;

        }
        return icon;
    }

    /**
     * Activity cycle methods
     **/

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
