package com.notificacion.client.NotificacionService;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.notificacion.client.MainActivity;
import com.notificacion.client.NotificacionService.DB.DataBaseAccess;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public class MQTTService extends Service {
    public static MqttAndroidClient clientMqtt;
    private MqttConnectOptions options;
    private static String topic = "";
    private boolean isMainActivity = false;
    private String clientID = "clienteAndroidPush";
    private final String user = "mqtt-test";
    private final String _LOG = "SERVICIO";
    private DataBaseAccess dataBaseAccess = null;
    public static final int _JOB_ID = 1231;
    private IBinder mBinder = new MyBinder();

    // Last will
    private String lastWillTopic = "LASTWILL";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, new Notification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dataBaseAccess = new DataBaseAccess(this);
        JSONObject datos = dataBaseAccess.getData();

        try {
            if(intent.hasExtra("canal")) {
                topic = intent.getStringExtra("canal");
                isMainActivity = true;
            }
            else {
                topic = "appmovil";
            }
        } catch (NullPointerException e) {
            // e.printStackTrace();
            topic = "appmovil";
        }
        if(!datos.isNull("documento_identidad")) {
            try {
                topic = datos.getString("documento_identidad") + datos.getString("numero");
                clientID = topic + datos.getString("imei");
            } catch (JSONException e) {
                // e.printStackTrace();
                topic = "appmovil";
            }
        }
        Log.v(_LOG, "topic en inicio de servicio: " + topic);
        if(isMainActivity) {
            setText("topic en inicio de servicio: " + topic);
        }
        clientMqtt = new MqttAndroidClient(this, Constants.URL_SERVER, clientID);
        clientMqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                if(b) {
                    Log.v(_LOG, "Reconectado a: " + Constants.URL_SERVER);
                    if(isMainActivity) {
                        setText("Reconectado a: " + Constants.URL_SERVER);
                    }
                    subscribe();
                }
                else {
                    Log.v(_LOG, "conectado a: " + Constants.URL_SERVER);
                    if(isMainActivity) {
                        setText("conectado a: " + Constants.URL_SERVER);
                    }
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.v(_LOG, "conexion perdida");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String mensajeRecibido = new String(mqttMessage.getPayload());
                Log.v(_LOG, "mensaje recibido: " + mensajeRecibido);
                Log.v(_LOG, "Canal emisor: " + s);
                    if(isMainActivity) {
                        setText("mensaje recibido: " + mensajeRecibido);
                    }

                    // NOTIFICAMOS QUE SE RECIBIO UN MENSAJE
                    Intent localIntent = new Intent(MQTTService.this, BroadcastReceiver.class);
                    localIntent.putExtra(Constants.EXTENDED_DATA_MESSAGE, mensajeRecibido);
                    sendBroadcast(localIntent);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        inicioConexion();
        return START_STICKY;
    }


    private void inicioConexion() {
        // opciones de configuracion
        options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        String newUser = "prueba";
        options.setUserName(newUser);
        options.setPassword(newUser.toCharArray());
        options.setMaxInflight(50); // default 10
        options.setKeepAliveInterval(0);    // default 60

        // iniciamos la conexion a broker
        Log.v(_LOG, "iniciando conexion a " + Constants.URL_SERVER);
        if(isMainActivity) {
            setText("iniciando conexion a " + Constants.URL_SERVER);
        }
            try {
                if(clientMqtt.isConnected())
                    clientMqtt.disconnect();

                clientMqtt.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        //clientMqtt.setBufferOpts(disconnectedBufferOptions);
                        Log.v(_LOG, "conectado a: " + Constants.URL_SERVER + "\nMensaje: " + iMqttToken.toString());
                        if(isMainActivity) {
                            setText("conectado a: " + Constants.URL_SERVER);
                        }
                        subscribe();
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.w(_LOG, "imprimiendo stacktrace");
                        throwable.printStackTrace();
                        Log.d(_LOG, "error en conexion, mensaje servidor: " + throwable.getMessage());
                        if(isMainActivity) {
                            setText("error en conexion, mensaje servidor: " + throwable.getMessage());
                        }
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
                Log.d(_LOG, "Excepcion en conexion, mensaje servidor: " + e.getMessage().toString());
            }
    }

    /**
     * SUBSCRIPCION
     * QoS:
     *  At most once (0)
     *  At least once (1)
     *  Exactly once (2).
     */
    public void subscribe() {
        int qos = 1;

        try {
            Log.v(_LOG, "suscribiendose a : " + topic + ", " + Constants.mainTopic);
            String[] topics = new String[]{topic, Constants.mainTopic};
            int[] qosArray = new int[]{qos, qos};
            clientMqtt.subscribe(topics, qosArray, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.v(_LOG, "suscripcion exitosa");
                    if(isMainActivity) {
                        setText("suscripcion exitosa");
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.v(_LOG, "error en la suscripcion");
                    if(isMainActivity) {
                        setText("error en la suscripcion");
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * PUBLICACION
     */
    public static void publish(String topic, final String payload) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes("UTF-8"));
            // clientMqtt.publish(Constants.mainTopic, message);
            clientMqtt.publish(topic, message);
            Log.v("SERVICIO", "PUBLISH topic: " + topic + "; mensaje: " + payload);

            if(!clientMqtt.isConnected()) {
                Log.v("SERVICIO", "mensajes en buffer: " + clientMqtt.getBufferedMessageCount());
            }

        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        if( clientMqtt != null && clientMqtt.isConnected() ){
            try {
                clientMqtt.unsubscribe(topic);
                clientMqtt.disconnect();
                clientMqtt = null;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDestroy() {
        Log.w(_LOG, "terminando servicio");

        super.onDestroy();
    }

    final Handler mHandler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(_LOG, "onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(_LOG, "onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(_LOG, "onUnbind");
        return true;
    }

    public class MyBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    private void setText(String text) {
        MainActivity.tvEstado.setText(MainActivity.tvEstado.getText().toString() + "\n" + text);
    }
}
