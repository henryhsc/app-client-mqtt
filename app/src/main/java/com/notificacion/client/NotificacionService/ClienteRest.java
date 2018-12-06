package com.notificacion.client.NotificacionService;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;

public class ClienteRest {
    private static AsyncHttpClient cliente = new AsyncHttpClient();
    private static AsyncHttpClient clienteStatus = new AsyncHttpClient();

    public static void get(String url, RequestParams params, JsonHttpResponseHandler peticion){
        // cliente.setSSLSocketFactory(MySSLSocketFactory.getFixedSocketFactory());
        cliente.get(Constants.BASE_URL_NOTIFICACION + url, params, peticion);
    }

    public static void post(String url, RequestParams params, JsonHttpResponseHandler peticion){
        cliente.setSSLSocketFactory(MySSLSocketFactory.getFixedSocketFactory());
        cliente.post(Constants.BASE_URL_NOTIFICACION + url, params, peticion);
    }

    public static void patch(String url, RequestParams params, JsonHttpResponseHandler peticion){
        // cliente.setSSLSocketFactory(MySSLSocketFactory.getFixedSocketFactory());
        cliente.patch(Constants.BASE_URL_NOTIFICACION + url, params, peticion);
    }
}