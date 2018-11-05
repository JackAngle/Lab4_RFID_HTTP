package com.example.lehuy.lab4_rfid_http;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class MyRequest {
    private static MyRequest mInstance = null;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    private MyRequest(Context context){
        mContext = context;
        mRequestQueue = getRequestQueue();

    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        return mRequestQueue;
    }

    public static synchronized MyRequest getInstance(Context context){
        if (mInstance == null){
            mInstance = new MyRequest(context) ;
        }
        return mInstance;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }


}
