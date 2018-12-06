package com.notificacion.client;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.RequestParams;
import com.notificacion.client.NotificacionService.BroadcastReceiver;
import com.notificacion.client.NotificacionService.Constants;
import com.notificacion.client.NotificacionService.DB.DataBaseAccess;
import com.notificacion.client.NotificacionService.MQTTService;
import com.notificacion.client.NotificacionService.MensajePushActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static TextView tvEstado;
    private LinearLayout llDatosConexion;
    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar pbPeticion;
    private Button btSuscribir;
    private Button btCancelar;
    private String imei = "";
    private final String _LOG = "SERVICIO";
    private final String _ID_CANAL = "idCanalSeguro";
    private final int  _REQUEST_PHONE = 1245;
    private NotificationManager notificationManager;
    private DataBaseAccess dataBaseAccess;
    BroadcastReceiver receiver;
    MQTTService mqttService;
    boolean mServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEstado = (TextView) findViewById(R.id.tvEstado);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        pbPeticion = (ProgressBar) findViewById(R.id.pbPeticion);
        pbPeticion.setVisibility(View.GONE);
        btSuscribir = (Button) findViewById(R.id.btSuscribir);
        btCancelar = (Button) findViewById(R.id.btCancel);
        llDatosConexion = (LinearLayout) findViewById(R.id.llDatosConexion);

        btCancelar.setVisibility(View.GONE);

        // registramos el BroadcastReceiver
        receiver = new BroadcastReceiver();
        IntentFilter filter = new IntentFilter(Constants.BROADCAST_ACTION);
        try {
            registerReceiver(receiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // suscripcion al canal MQTT para notificaciones
        dataBaseAccess = new DataBaseAccess(this);
        JSONObject object = dataBaseAccess.getData();
        if(!object.isNull("documento_identidad")) {
            try {
                suscribirCanal(object.getString("documento_identidad") + object.getString("numero"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            llDatosConexion.setVisibility(View.GONE);
            btCancelar.setVisibility(View.VISIBLE);
        }
    }
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MQTTService.MyBinder myBinder = (MQTTService.MyBinder) service;
            mqttService = myBinder.getService();
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    public void subscribe(View view) {
        final String documento_identidad = etEmail.getText().toString();
        final String numero = etPassword.getText().toString();
        TelephonyManager manager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        imei = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        if(documento_identidad.length() > 0 && numero.length() > 0) {
            RequestParams params = new RequestParams();
            params.put("documento_identidad", documento_identidad);
            params.put("descripcion", numero);
            params.setUseJsonStreamer(true);
            Log.w("SISTEMA", params.toString());
            btSuscribir.setVisibility(View.GONE);
            pbPeticion.setVisibility(View.VISIBLE);
            /*ClienteRest.post("v1/auth/registro", params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                        // Toast.makeText(getApplicationContext(), "Token obtenido : " + response.getString("token"), Toast.LENGTH_LONG). show();
                    btSuscribir.setVisibility(View.GONE);
                    pbPeticion.setVisibility(View.GONE);
                    dataBaseAccess.setData(documento_identidad, numero, imei);
                    llDatosConexion.setVisibility(View.GONE);
                    btCancelar.setVisibility(View.VISIBLE);

                    // suscribimo al canal
                    suscribirCanal(documento_identidad+numero);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    tvEstado.setText("error: mensaje de estado:\n");
                    tvEstado.setText(errorResponse.toString());
                    btSuscribir.setVisibility(View.VISIBLE);
                    pbPeticion.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    tvEstado.setText("error string: mensaje de estado:\n");
                    tvEstado.setText(responseString);
                    btSuscribir.setVisibility(View.VISIBLE);
                    pbPeticion.setVisibility(View.GONE);
                }
            });*/

            // saltamos la etapa de registro en backend de pruebas
            btSuscribir.setVisibility(View.GONE);
            pbPeticion.setVisibility(View.GONE);
            dataBaseAccess.setData(documento_identidad, numero, imei);
            llDatosConexion.setVisibility(View.GONE);
            btCancelar.setVisibility(View.VISIBLE);

            // suscribimo al canal
            suscribirCanal(documento_identidad+numero);
        }
        else {
            Toast.makeText(getApplicationContext(), "Debe establecer un canal de suscripción", Toast.LENGTH_LONG). show();
        }
    }

    public void sendMessage(View view) {
        // iniciamos servicio
        Intent service = new Intent(MainActivity.this, MQTTService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {   // para Android Oreo y superiores mostramos una notificacion permanente
            Log.v(_LOG, "SDK: " + Build.VERSION.SDK_INT + " ; Version Codes: " + Build.VERSION_CODES.O + "; iniciando notificacion");
            CharSequence nombre = "MiCanalSeguro";
            String descripcion = "Descripcion de canal";
            NotificationChannel canalNotificacion = new NotificationChannel(_ID_CANAL, nombre, NotificationManager.IMPORTANCE_HIGH);
            canalNotificacion.setDescription(descripcion);
            canalNotificacion.enableLights(true);
            canalNotificacion.enableVibration(true);

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(canalNotificacion);


            notification(this, "1234567", "Esperando mensajes");
            startForegroundService(service);
        }
        else{
            Log.v(_LOG, "iniciando servicio");
            startService(service);
        }
    }

    public boolean checkActiveServices(String serviceName) {
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        try {
            final List<ActivityManager.RunningServiceInfo> servicios = activityManager.getRunningServices(Integer.MAX_VALUE);
            for (ActivityManager.RunningServiceInfo serviceInfo : servicios) {
                //Log.v(_LOG, "servicios activos : " + serviceInfo.service.getClassName());
                if(serviceName.equals(serviceInfo.service.getClassName())) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.v(_LOG, "error revisando servicios activos");
            return false;
        }
    }

    public void notification(Context context, String code, String message) {
        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(context, MensajePushActivity.class);
        // Send data to NotificationView Class
        intent.putExtra("message", message);
        intent.putExtra("code", code);
        intent.putExtra("show", false); // no mostramos el mensaje al pulsar en la barra de notificaciones
        // Open NotificationView.java Activity
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create Notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, _ID_CANAL)
                .setSmallIcon(R.drawable.notify)
                .setBadgeIconType(R.drawable.notify)
                .setChannelId(_ID_CANAL)
                //.setTicker(message)
                .setContentTitle("Mensaje del servicio de notificación")
                .setContentText(message)
                //.setPriority(NotificationCompat.PRIORITY_MAX)
                //.addAction(R.drawable.notify, "ver detalles", pIntent)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                //.setDefaults(NotificationCompat.FLAG_FOREGROUND_SERVICE);
                .setDefaults(NotificationCompat.FLAG_NO_CLEAR);
        //.setPriority(NotificationCompat.PRIORITY_HIGH);

        // Create Notification Manager
        //NotificationManager notificationmanager = (NotificationManager) context
        //  .getSystemService(Context.NOTIFICATION_SERVICE);
        // Build Notification with Notification Manager
        notificationManager.notify(0, builder.build());
    }


    private void suscribirCanal(String canal) {
        Intent service = new Intent(MainActivity.this, MQTTService.class);
        service.putExtra("canal", canal);
        startService(service);
        if(!mServiceBound || mServiceConnection == null)
            bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE); // Context.BIND_AUTO_CREATE
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
            /*if(mServiceConnection != null && mServiceBound) { // TODO: ACTIVAR/DESACTIVAR DE ACUERDO A LA VERSION DE ANDROID OREO
                unbindService(mServiceConnection);
                mServiceBound = false;
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            receiver = null;
        }
    }

    public void cancelarSuscripcion(View view) {
        dataBaseAccess.deleteData();
        MQTTService.disconnect();
        recreate();
    }
}