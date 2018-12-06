package com.notificacion.client.NotificacionService.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class DataBaseAccess  {
    SQLiteOpenHelper openHelper = null;
    SQLiteDatabase database;
    Context context;

    public DataBaseAccess(Context c){
        context = c;
    }

    private void abrir(){
        openHelper = new DBManager(context, "REGISTRO", null, 6);
        database = openHelper.getWritableDatabase();
    }
    private void cerrar(){
        database.close();
    }

    public JSONObject getData() {
        JSONObject datos = new JSONObject();
        String sql = "SELECT * FROM account LIMIT 1";
        abrir();
        Cursor cursor = database.rawQuery(sql, null);
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                datos.put("documento_identidad", cursor.getString(cursor.getColumnIndex("documento_identidad")));
                datos.put("numero", cursor.getString(cursor.getColumnIndex("numero")));
                datos.put("imei", cursor.getString(cursor.getColumnIndex("imei")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            cerrar();
        }

        return datos;
    }

    public String setData(String documento_identidad, String numero, String imei){
        abrir();
        String sql =
        "INSERT INTO account(documento_identidad, numero, imei) VALUES(?, ?, ?)";
        String[] parametros = new String[]{documento_identidad, numero, imei};
        database.execSQL(sql, parametros);

        cerrar();

        return "done";
    }

    public void deleteData(){
        abrir();
        String sql = "DELETE FROM account";
        database.execSQL(sql);
        cerrar();
    }
}
