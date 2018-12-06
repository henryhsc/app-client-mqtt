package com.notificacion.client.NotificacionService;

/**
 * Clase con valores constantes necesarios para el manejo de datos del servicio MQTT al BroadCastReceiver
 *
 */

public final class Constants {
    public static final String BROADCAST_ACTION = "MQTT.SERVICE.LOCAL.BROADCAST";   // identificacion para notificacion en BROADCAST
    public static final String EXTENDED_DATA_MESSAGE = "MQTT.SERVICE.DATA.STATUS";  // mensaje recibido en BROADCAST
    public static final String RESTART_SERVICE = "MQTT.SERVICE.RESTART";            // señal de reinicio del servicio

    public static final String BASE_URL_NOTIFICACION = "http://ruta-base/";
    public static final String URL_SERVER = "tcp://ruta-base-broker"; // conexion a servidor broker MQTT

    // para notificaciones
    public static final String PRIMARY_NOTIF_CHANNEL = "default";
    public static final int PRIMARY_FOREGROUND_NOTIF_SERVICE_ID = 1001;
    public static final String mainTopic = "MAIN_TOPIC";
}
