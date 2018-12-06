package com.notificacion.client.NotificacionService;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.notificacion.client.R;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;

/**
 * Muestra al usuario el mensaje push recibido desde el servidor MQTT para decidir que accion tomar
 *
 */

public class MensajePushActivity extends AppCompatActivity {
    private boolean response;
    private JSONObject dataMovil;
    private JSONObject data;
    private JSONObject issuing;
    private SecretKey secretKey;
    private String id;    // codigo recibido en mensaje push (para devolver)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_mensaje_push);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Bundle extra = getIntent().getExtras();
        String mensaje = extra.getString("message");
        id = extra.getString("id");

            NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Dismiss Notification
            notificationmanager.cancel(0);

        ((TextView)findViewById(R.id.tvMensajePush)).setText(mensaje);
    }

    /**
     * Se invoca al pulsar el boton 'aceptar' en el layout
     * @param view
     */
    public void accept(View view) {
        Log.i("BROADCAST", "boton aceptar");
        JSONObject object = new JSONObject();

        try {
            object.put("id", id);
            object.put("estado", "leido");
            object.put("source", "movil");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MQTTService.publish("SEDEM_IN", object.toString());
        finish();
    }

    /**
     * Se invoca al pulsar el boton 'cancelar' en el layout
     * @param view
     */
    public void decline(View view) {
        Log.i("BROADCAST", "rechazando");
        // MQTTService.publish("mensaje de rechazo");
        finish();
    }
}
