package com.notificacion.client.NotificacionService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.notificacion.client.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Al llegar un mensaje push del servidor MQTT el BroadcastReceiver crea la notificacion al usuario
 * y define un Activity donde se pueden tomar acciones respecto al mensaje recibido
 *
 */

public class BroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
            String id, mensaje, jsonString;
            boolean mensajePropio = false;
            id = "";
            mensaje = "";
            int prioridad = 0;  // 0: normal; 1: alta
            jsonString = intent.getStringExtra(Constants.EXTENDED_DATA_MESSAGE);
            try {
                JSONObject object = new JSONObject(jsonString);
                if(!object.isNull("source")) {
                    mensajePropio = true;
                    Log.v("SERVICIO", "mensaje propio: " + object.getString("source"));
                }
                else {
                    mensajePropio = false;
                    mensaje = object.getString("mensaje");
                    Log.v("SERVICIO", "mensaje externo: " + object.getString("mensaje"));
                }
                if(object.has("id")) {
                    id = object.getString("id");
                }
                if(!object.isNull("prioridad")) {
                    prioridad = object.getInt("prioridad");
                }
                if(!mensajePropio) {
                    JSONObject respuesta = new JSONObject();
                    respuesta.put("id", id);
                    respuesta.put("estado", "recibido");
                    respuesta.put("source", "movil");
                    MQTTService.publish("SEDEM_IN", respuesta.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                mensaje = jsonString;
                id = "---";
            }

            if(!mensajePropio) {
                // notificamos al usuario
                notification(context, mensaje, id);

                // esperamos un tiempo hasta lanzar el activity pop-up ya que no trabaja bien de forma simultánea con la notificación
                try {
                    Thread.sleep(1000);     // 1 segundos
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // mensaje POP UP, por tipo de prioridad
                if(prioridad > 0) {
                    Intent popup = new Intent(context, MensajePushActivity.class);
                    popup.putExtra("message", mensaje);
                    popup.putExtra("id", id);
                    popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(popup);
                }
            }
    }

    //@SuppressWarnings("deprecation")
    public void notification(Context context, String message, String id) {
        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(context, MensajePushActivity.class);
        // Send data to NotificationView Class
        intent.putExtra("id", id);
        intent.putExtra("message", message);
        intent.putExtra("show", false); // no mostramos el mensaje al pulsar en la barra de notificaciones
        // Open NotificationView.java Activity
        Random random = new Random();
        PendingIntent pIntent = PendingIntent.getActivity(context, random.nextInt(1000000), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);  // PendingIntent.FLAG_IMMUTABLE

        // Create Notification using NotificationCompat.Builder
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_icon);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context)
                .setLargeIcon(bitmap)
                .setSmallIcon(R.drawable.play_icon)
                .setContentTitle("Mensaje del servicio de notificación")
                .setContentText(message)
                .addAction(R.drawable.play_icon, "ver detalles", pIntent)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                //.setDefaults(NotificationCompat.FLAG_AUTO_CANCEL);
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Create Notification Manager
        NotificationManager notificationmanager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(Constants.PRIMARY_NOTIF_CHANNEL, "default", NotificationManager.IMPORTANCE_HIGH);
            canal.setLightColor(Color.TRANSPARENT);
            canal.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationmanager.createNotificationChannel(canal);

            NotificationCompat.Builder notification =  new NotificationCompat.Builder(context, Constants.PRIMARY_NOTIF_CHANNEL)
                    .setSmallIcon(R.drawable.play_icon)
                    .setContentTitle("Nuevo mensaje")
                    .setContentText(message)
                    .setChannelId(Constants.PRIMARY_NOTIF_CHANNEL); //.setPriority(NotificationCompat.PRIORITY_MAX)*/
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntent(intent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(random.nextInt(10000), PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setContentIntent(pendingIntent);
            notificationmanager.notify(Constants.PRIMARY_FOREGROUND_NOTIF_SERVICE_ID, notification.build());
        }
        else {
            // Build Notification with Notification Manager
            notificationmanager.notify(random.nextInt(1000000), builder.build());
        }

    }
}
