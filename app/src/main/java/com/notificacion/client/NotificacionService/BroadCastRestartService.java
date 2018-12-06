package com.notificacion.client.NotificacionService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class BroadCastRestartService extends BroadcastReceiver {
    MQTTService mqttService;
    boolean mServiceBound = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("SERVICIO", "verificando acceso al servidor de notificaciones");
        final Context serviceContext = context;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceContext.startForegroundService(new Intent(serviceContext, MQTTService.class));
            serviceContext.bindService(new Intent(serviceContext, MQTTService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
            serviceContext.startService(new Intent(serviceContext, MQTTService.class));
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
}