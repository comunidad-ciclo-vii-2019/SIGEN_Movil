package com.example.crud_encuesta.Componentes_DC.WebServices;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.crud_encuesta.Componentes_AP.DAO.DAOUsuario;
import com.example.crud_encuesta.Componentes_AP.Models.Usuario;
import com.example.crud_encuesta.DatabaseAccess;
import com.example.crud_encuesta.Dominio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;

public class Descargar {

    private Context context;
    private SQLiteDatabase cx;
    private DatabaseAccess dba;
    private ProgressDialog progressDialog;
    private DAOUsuario daoUsuario;
    private int clave_id;
    private Dominio dominio;

    public Descargar(Context context){
        this.context = context;
        dba = DatabaseAccess.getInstance(context);
        dominio = Dominio.getInstance(context);
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Descargando...");
        daoUsuario = new DAOUsuario(context);
    }

    public void descargar_turno(int turno_id, int estudiante_id){
        //Se procede a verificar que el usuario tenga acceso a Internet
        //En caso de no tener acceso, se cancela la operación de descarga
        /*if (! isInternetAvailable()){
            Toast.makeText(context, "No hay acceso a Internet.", Toast.LENGTH_LONG).show();
            return;
        }*/


        String str_dominio = dominio.getDominio();
        if (str_dominio == null){
            Toast.makeText(context, "No hay conexión con el servidor.", Toast.LENGTH_LONG).show();
            return;
        }

        RequestQueue request;
        JsonObjectRequest jsonObjectRequest;

        progressDialog.show();
        cx = dba.open();

        request = Volley.newRequestQueue(context);
        String url =  str_dominio + "/api/evaluacion/turno/" + turno_id + "/obtener/" +estudiante_id;
        System.out.println(url);
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response.toString());
                try {
                    //Se procede a verificar si posee turno

                    if (response.getInt("resultado") == 0) {
                        Toast.makeText(context, "No se pudo descargar. Intentos disponibles: "+response.getString("resultado"), Toast.LENGTH_LONG).show();
                    } else {
                    //Se procede a obtener la clave y almacenarla
                    JSONObject clave = response.getJSONObject("clave");

                    verificacion(clave.getInt("id"));

                    ContentValues contenedor_clave = new ContentValues();
                    contenedor_clave.put("ID_CLAVE", clave.getInt("id"));
                    contenedor_clave.put("ID_TURNO", clave.getInt("turno_id"));
                    contenedor_clave.put("NUMERO_CLAVE", clave.getInt("numero_clave"));

                    cx.insert("CLAVE", null, contenedor_clave);

                    //Se procede a obtener el intento y almacenarlo
                    JSONObject intento = response.getJSONObject("intento");

                    ContentValues contenedor_intento = new ContentValues();
                    contenedor_intento.put("ID_INTENTO", intento.getInt("id"));
                    contenedor_intento.put("ID_EST", intento.getInt("estudiante_id"));
                    contenedor_intento.put("ID_CLAVE", intento.getInt("clave_id"));
                    contenedor_intento.put("FECHA_INICIO_INTENTO", intento.getString("fecha_inicio_intento"));
                    contenedor_intento.put("NUMERO_INTENTO", intento.getString("numero_intento"));

                    cx.insert("INTENTO", null, contenedor_intento);

                    //Se procede a obtener las clave_areas, recorerlas y almacenarlas
                    JSONArray clave_areas = response.getJSONArray("clave_areas");
                    for (int i = 0; i < clave_areas.length(); i++) {

                        JSONObject clave_area = clave_areas.getJSONObject(i);

                        ContentValues contenedor_clave_area = new ContentValues();
                        contenedor_clave_area.put("ID_CLAVE_AREA", clave_area.getInt("id"));
                        contenedor_clave_area.put("ID_AREA", clave_area.getInt("area_id"));
                        contenedor_clave_area.put("ID_CLAVE", clave_area.getInt("clave_id"));
                        contenedor_clave_area.put("NUMERO_PREGUNTAS", clave_area.getInt("numero_preguntas"));
                        contenedor_clave_area.put("ALEATORIO", clave_area.getInt("aleatorio"));
                        contenedor_clave_area.put("PESO", clave_area.getInt("peso"));

                        cx.insert("CLAVE_AREA", null, contenedor_clave_area);
                    }

                    //Se procede a obtener las areas, recorerlas y almacenarlas
                    JSONArray areas = response.getJSONArray("areas");
                    for (int i = 0; i < areas.length(); i++) {

                        JSONObject area = areas.getJSONObject(i);

                        ContentValues contenedor_area = new ContentValues();
                        contenedor_area.put("ID_AREA", area.getInt("id"));
                        contenedor_area.put("ID_CAT_MAT", area.getInt("id_cat_mat"));
//                        contenedor_area.put("ID_PDG_DCN", area.getInt("id_pdg_dcn"));
                        contenedor_area.put("ID_TIPO_ITEM", area.getInt("tipo_item_id"));
                        contenedor_area.put("TITULO", area.getString("titulo"));

                        cx.insert("AREA", null, contenedor_area);
                    }

                    //Se procede a obtener las clave_area_preguntas, recorerlas y almacenarlas
                    JSONArray clave_area_preguntas = response.getJSONArray("clave_area_preguntas");
                    for (int i = 0; i < clave_area_preguntas.length(); i++) {

                        JSONObject clave_area_pregunta = clave_area_preguntas.getJSONObject(i);

                        ContentValues contenedor_clave_area_pregunta = new ContentValues();
                        contenedor_clave_area_pregunta.put("ID_CLAVE_AREA_PRE", clave_area_pregunta.getInt("id"));
                        contenedor_clave_area_pregunta.put("ID_PREGUNTA", clave_area_pregunta.getInt("pregunta_id"));
                        contenedor_clave_area_pregunta.put("ID_CLAVE_AREA", clave_area_pregunta.getInt("clave_area_id"));

                        cx.insert("CLAVE_AREA_PREGUNTA", null, contenedor_clave_area_pregunta);

                    }

                    //Se procede a obtener los grupos de emparejamiento, recorerlos y almacenarlos
                    JSONArray grupos_emp = response.getJSONArray("grupos_emp");
                    for (int i = 0; i < grupos_emp.length(); i++) {

                        JSONObject grupo_emp = grupos_emp.getJSONObject(i);

                        ContentValues contenedor_grupo_emp = new ContentValues();
                        contenedor_grupo_emp.put("ID_GRUPO_EMP", grupo_emp.getInt("id"));
                        contenedor_grupo_emp.put("ID_AREA", grupo_emp.getInt("area_id"));
                        contenedor_grupo_emp.put("DESCRIPCION_GRUPO_EMP", grupo_emp.getString("descripcion_grupo_emp"));

                        cx.insert("GRUPO_EMPAREJAMIENTO", null, contenedor_grupo_emp);
                    }

                    //Se procede a obtener las preguntas, recorerlas y almacenarlas
                    JSONArray preguntas = response.getJSONArray("preguntas");
                    for (int i = 0; i < preguntas.length(); i++) {

                        JSONObject pregunta = preguntas.getJSONObject(i);

                        ContentValues contenedor_pregunta = new ContentValues();
                        contenedor_pregunta.put("ID_PREGUNTA", pregunta.getInt("id"));
                        contenedor_pregunta.put("ID_GRUPO_EMP", pregunta.getInt("grupo_emparejamiento_id"));
                        contenedor_pregunta.put("PREGUNTA", pregunta.getString("pregunta"));

                        cx.insert("PREGUNTA", null, contenedor_pregunta);
                    }

                    //Se procede a obtener las opciones, recorerlas y almacenarlas
                    JSONArray opciones = response.getJSONArray("opciones");
                    for (int i = 0; i < opciones.length(); i++) {

                        JSONObject opcion = opciones.getJSONObject(i);

                        ContentValues contenedor_opcion = new ContentValues();
                        contenedor_opcion.put("ID_OPCION", opcion.getInt("id"));
                        contenedor_opcion.put("ID_PREGUNTA", opcion.getInt("pregunta_id"));
                        contenedor_opcion.put("OPCION", opcion.getString("opcion"));
                        contenedor_opcion.put("CORRECTA", opcion.getInt("correcta"));

                        cx.insert("OPCION", null, contenedor_opcion);

                    }
                    dba.close();

                    Toast.makeText(context, "¡Exito: Se almaceno de manera correcta la Evaluación!", Toast.LENGTH_SHORT).show();
                }
                    progressDialog.hide();
                }catch (JSONException e){
                    e.printStackTrace();
                    dba.close();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.hide();
                Toast.makeText(context, "Hubo un error en el proceso de descarga, intentelo de nuevo más tarde.", Toast.LENGTH_LONG).show();
                dba.close();
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.add(jsonObjectRequest);
    }

    public void descargar_encuesta(int encuesta_id){
        //Se procede a verificar que el usuario tenga acceso a Internet
        //En caso de no tener acceso, se cancela la operación de descarga
        /*if (! isInternetAvailable()){
            Toast.makeText(context, "No hay acceso a Internet.", Toast.LENGTH_LONG).show();
            return;
        }*/


        String str_dominio = dominio.getDominio();
        if (str_dominio == null){
            Toast.makeText(context, "No hay conexión con el servidor.", Toast.LENGTH_LONG).show();
            return;
        }

        //Obtener usuario loggeado
        Usuario usuario = daoUsuario.getUsuarioLogueado();

        RequestQueue request;
        JsonObjectRequest jsonObjectRequest;

        progressDialog.show();
        cx = dba.open();

        clave_id = 0;

        Cursor cursor_clave = cx.rawQuery("SELECT * FROM CLAVE WHERE ID_ENCUESTA = " + encuesta_id, null);

        if(cursor_clave.moveToFirst())
            clave_id = cursor_clave.getInt(cursor_clave.getColumnIndex("ID_CLAVE"));

        request = Volley.newRequestQueue(context);
        String url = str_dominio + "/api/encuesta/" + encuesta_id + "/" + usuario.getIDUSUARIO() + "/" + clave_id;


        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {

                    if(clave_id == 0){
                        //Se procede a obtener la clave y almacenarla
                        JSONObject encuesta = response.getJSONObject("encuesta");

                        ContentValues contenedor_encuesta = new ContentValues();
                        contenedor_encuesta.put("ID_ENCUESTA", encuesta.getInt("id"));
                        contenedor_encuesta.put("ID_PDG_DCN", encuesta.getInt("id_docente"));
                        contenedor_encuesta.put("TITULO_ENCUESTA", encuesta.getString("titulo_encuesta"));
                        contenedor_encuesta.put("DESCRIPCION_ENCUESTA", encuesta.getString("descripcion_encuesta"));
                        contenedor_encuesta.put("FECHA_INICIO_ENCUESTA", encuesta.getString("fecha_inicio_encuesta"));
                        contenedor_encuesta.put("FECHA_FINAL_ENCUESTA", encuesta.getString("fecha_final_encuesta"));

                        cx.insert("ENCUESTA",null,contenedor_encuesta);

                        //Se procede a obtener la clave y almacenarla
                        JSONObject clave = response.getJSONObject("clave");

                        ContentValues contenedor_clave = new ContentValues();
                        contenedor_clave.put("ID_CLAVE", clave.getInt("id"));
                        contenedor_clave.put("ID_ENCUESTA", clave.getInt("encuesta_id"));
                        contenedor_clave.put("NUMERO_CLAVE", clave.getInt("numero_clave"));

                        cx.insert("CLAVE",null,contenedor_clave);

                        //Se procede a obtener el encuestado y almacenarlo
                        /*JSONObject encuestado = response.getJSONObject("encuestado");

                        //Primero vamos a verificar que el Encuestado no exista, antes de intentar ingresarlo
                        Cursor c = cx.rawQuery("SELECT * FROM ENCUESTADO WHERE ID_ENCUESTADO="+encuestado.getInt("id"), null, null);

                        if(!c.moveToFirst()){

                            ContentValues contenedor_encuestado = new ContentValues();
                            contenedor_encuestado.put("ID_ENCUESTADO", encuestado.getInt("id"));
                            contenedor_encuestado.put("MAC", encuestado.getString("MAC"));

                            cx.insert("ENCUESTADO",null,contenedor_encuestado);

                        }*/

                        //Se procede a obtener el intento y almacenarlo
                        JSONObject intento = response.getJSONObject("intento");

                        ContentValues contenedor_intento = new ContentValues();
                        contenedor_intento.put("ID_INTENTO", intento.getInt("id"));
                        contenedor_intento.put("ID_USUARIO", intento.getInt("user_id"));
                        contenedor_intento.put("ID_CLAVE", intento.getInt("clave_id"));
                        contenedor_intento.put("FECHA_INICIO_INTENTO", intento.getString("fecha_inicio_intento"));

                        cx.insert("INTENTO",null,contenedor_intento);

                        //Se procede a obtener las clave_areas, recorerlas y almacenarlas
                        JSONArray clave_areas = response.getJSONArray("clave_areas");
                        for (int i = 0; i < clave_areas.length(); i++) {

                            JSONObject clave_area = clave_areas.getJSONObject(i);

                            ContentValues contenedor_clave_area = new ContentValues();
                            contenedor_clave_area.put("ID_CLAVE_AREA", clave_area.getInt("id"));
                            contenedor_clave_area.put("ID_AREA", clave_area.getInt("area_id"));
                            contenedor_clave_area.put("ID_CLAVE", clave_area.getInt("clave_id"));
                            contenedor_clave_area.put("NUMERO_PREGUNTAS", clave_area.getInt("numero_preguntas"));
                            contenedor_clave_area.put("ALEATORIO", clave_area.getInt("aleatorio"));
                            contenedor_clave_area.put("PESO", clave_area.getInt("peso"));

                            cx.insert("CLAVE_AREA",null,contenedor_clave_area);
                        }

                        //Se procede a obtener las areas, recorerlas y almacenarlas
                        JSONArray areas = response.getJSONArray("areas");
                        for (int i = 0; i < areas.length(); i++) {

                            JSONObject area = areas.getJSONObject(i);

                            ContentValues contenedor_area = new ContentValues();
                            contenedor_area.put("ID_AREA", area.getInt("id"));
    //                        contenedor_area.put("ID_CAT_MAT", area.getInt("id_cat_mat"));
                            contenedor_area.put("ID_PDG_DCN", area.getInt("id_pdg_dcn"));
                            contenedor_area.put("ID_TIPO_ITEM", area.getInt("tipo_item_id"));
                            contenedor_area.put("TITULO", area.getString("titulo"));

                            cx.insert("AREA",null,contenedor_area);
                        }

                        //Se procede a obtener las clave_area_preguntas, recorerlas y almacenarlas
                        JSONArray clave_area_preguntas = response.getJSONArray("clave_area_preguntas");
                        for (int i = 0; i < clave_area_preguntas.length(); i++) {

                            JSONObject clave_area_pregunta = clave_area_preguntas.getJSONObject(i);

                            ContentValues contenedor_clave_area_pregunta = new ContentValues();
                            contenedor_clave_area_pregunta.put("ID_CLAVE_AREA_PRE", clave_area_pregunta.getInt("id"));
                            contenedor_clave_area_pregunta.put("ID_PREGUNTA", clave_area_pregunta.getInt("pregunta_id"));
                            contenedor_clave_area_pregunta.put("ID_CLAVE_AREA", clave_area_pregunta.getInt("clave_area_id"));

                            cx.insert("CLAVE_AREA_PREGUNTA",null,contenedor_clave_area_pregunta);

                        }

                        //Se procede a obtener los grupos de emparejamiento, recorerlos y almacenarlos
                        JSONArray grupos_emp = response.getJSONArray("grupos_emp");
                        for (int i = 0; i < grupos_emp.length(); i++) {

                            JSONObject grupo_emp = grupos_emp.getJSONObject(i);

                            ContentValues contenedor_grupo_emp = new ContentValues();
                            contenedor_grupo_emp.put("ID_GRUPO_EMP", grupo_emp.getInt("id"));
                            contenedor_grupo_emp.put("ID_AREA", grupo_emp.getInt("area_id"));
                            contenedor_grupo_emp.put("DESCRIPCION_GRUPO_EMP", grupo_emp.getString("descripcion_grupo_emp"));

                            cx.insert("GRUPO_EMPAREJAMIENTO",null,contenedor_grupo_emp);
                        }

                        //Se procede a obtener las preguntas, recorerlas y almacenarlas
                        JSONArray preguntas = response.getJSONArray("preguntas");
                        for (int i = 0; i < preguntas.length(); i++) {

                            JSONObject pregunta = preguntas.getJSONObject(i);

                            ContentValues contenedor_pregunta = new ContentValues();
                            contenedor_pregunta.put("ID_PREGUNTA", pregunta.getInt("id"));
                            contenedor_pregunta.put("ID_GRUPO_EMP", pregunta.getInt("grupo_emparejamiento_id"));
                            contenedor_pregunta.put("PREGUNTA", pregunta.getString("pregunta"));

                            cx.insert("PREGUNTA",null,contenedor_pregunta);
                        }

                        //Se procede a obtener las opciones, recorerlas y almacenarlas
                        JSONArray opciones = response.getJSONArray("opciones");
                        for (int i = 0; i < opciones.length(); i++) {

                            JSONObject opcion = opciones.getJSONObject(i);

                            ContentValues contenedor_opcion = new ContentValues();
                            contenedor_opcion.put("ID_OPCION", opcion.getInt("id"));
                            contenedor_opcion.put("ID_PREGUNTA", opcion.getInt("pregunta_id"));
                            contenedor_opcion.put("OPCION", opcion.getString("opcion"));
                            contenedor_opcion.put("CORRECTA", opcion.getInt("correcta"));

                            cx.insert("OPCION",null,contenedor_opcion);

                        }
                    }else{
                        JSONObject intento = response.getJSONObject("intento");
                        int intento_id = intento.getInt("id");
                        //Cursor cursor_intento = cx.rawQuery("SELECT * FROM INTENTO WHERE ID_INTENTO = " + intento_id, null);
                        cx.delete("INTENTO","ID_CLAVE = " + clave_id, null);
                        //if(!cursor_intento.moveToFirst()){
                            ContentValues contenedor_intento = new ContentValues();
                            contenedor_intento.put("ID_INTENTO", intento_id);
                            contenedor_intento.put("ID_USUARIO", intento.getInt("user_id"));
                            contenedor_intento.put("ID_CLAVE", intento.getInt("clave_id"));
                            contenedor_intento.put("FECHA_INICIO_INTENTO", intento.getString("fecha_inicio_intento"));

                            cx.insert("INTENTO",null,contenedor_intento);
                        //}

                    }
                    dba.close();
                    progressDialog.hide();
                    Toast.makeText(context, "¡Exito: Se almaceno de manera correcta la Encuesta!", Toast.LENGTH_SHORT).show();
                }catch (JSONException e){
                    e.printStackTrace();
                    dba.close();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.hide();
                Toast.makeText(context, "Hubo un error en el proceso de descarga, intentelo de nuevo más tarde.", Toast.LENGTH_LONG).show();
                dba.close();
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.add(jsonObjectRequest);
    }

    /*public boolean isInternetAvailable() {
        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
            return p.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }*/

    public void verificacion(int clave_id){

        Cursor c;

        c = cx.rawQuery("SELECT * FROM CLAVE WHERE ID_CLAVE = " + clave_id, null);

        if (c.moveToFirst()){
            Cursor c2 = cx.rawQuery("SELECT * FROM CLAVE_AREA WHERE ID_CLAVE = " + c.getInt(c.getColumnIndex("ID_CLAVE")), null);
            if(c2.moveToFirst()){
                do{
                    Cursor c3 = cx.rawQuery("SELECT * FROM CLAVE_AREA_PREGUNTA WHERE ID_CLAVE_AREA = " + c2.getInt(c2.getColumnIndex("ID_CLAVE_AREA")), null);
                    if(c3.moveToFirst()){
                        do{
                            Cursor c4 = cx.rawQuery("SELECT * FROM PREGUNTA WHERE ID_PREGUNTA = " + c3.getInt(c3.getColumnIndex("ID_PREGUNTA")), null);
                            if(c4.moveToNext()){
                                do {
                                    Cursor c5 = cx.rawQuery("SELECT * FROM OPCION WHERE ID_PREGUNTA = " + c4.getInt(c4.getColumnIndex("ID_PREGUNTA")), null);
                                    if(c5.moveToFirst()){
                                        do{
                                            Cursor c6 = cx.rawQuery("SELECT * FROM RESPUESTA WHERE ID_OPCION = " + c5.getInt(c5.getColumnIndex("ID_OPCION")), null);
                                            if(c6.moveToFirst()){
                                                do{
                                                    cx.delete("RESPUESTA","ID_RESPUESTA = " + c6.getInt(c6.getColumnIndex("ID_RESPUESTA")), null);
                                                }while (c6.moveToNext());
                                            }
                                            cx.delete("OPCION","ID_OPCION = " + c5.getInt(c5.getColumnIndex("ID_OPCION")), null);
                                        }while (c5.moveToNext());
                                    }
                                    cx.delete("PREGUNTA","ID_PREGUNTA = " + c4.getInt(c4.getColumnIndex("ID_PREGUNTA")), null);
                                    //Eliminar GPO_EMP
                                    cx.delete("GRUPO_EMPAREJAMIENTO","ID_GRUPO_EMP = " + c4.getInt(c4.getColumnIndex("ID_GRUPO_EMP")), null);

                                }while (c4.moveToNext());
                            }
                            cx.delete("CLAVE_AREA_PREGUNTA","ID_CLAVE_AREA_PRE = " + c3.getInt(c3.getColumnIndex("ID_CLAVE_AREA_PRE")), null);
                        }while(c3.moveToNext());
                    }
                    cx.delete("CLAVE_AREA","ID_CLAVE_AREA = " + c2.getInt(c2.getColumnIndex("ID_CLAVE_AREA")), null);
                    cx.delete("AREA","ID_AREA = " + c2.getInt(c2.getColumnIndex("ID_AREA")), null);
                }while (c2.moveToNext());
            }
            cx.delete("CLAVE","ID_CLAVE = " + c.getInt(c.getColumnIndex("ID_CLAVE")), null);
            Cursor c7 = cx.rawQuery("SELECT * FROM INTENTO WHERE ID_CLAVE = " + c.getInt(c.getColumnIndex("ID_CLAVE")), null);

            if(c7.moveToFirst())
                cx.delete("INTENTO","ID_INTENTO = " + c7.getInt(c7.getColumnIndex("ID_INTENTO")), null);
        }

        /*cx.delete("RESPUESTA",null, null);
        cx.delete("CLAVE_AREA_PREGUNTA",null, null);
        cx.delete("OPCION",null, null);
        cx.delete("PREGUNTA",null, null);
        cx.delete("GRUPO_EMPAREJAMIENTO",null, null);
        cx.delete("CLAVE_AREA",null, null);
        cx.delete("INTENTO",null, null);
        cx.delete("CLAVE",null, null);
        cx.delete("AREA", null,null);
        Log.d("LOG","DELETES");*/
    }

    public boolean usuarioPoseeIntento(long id_encuesta){
        boolean existe = false;
        cx = dba.open();
        Cursor clave = cx.rawQuery("SELECT * FROM CLAVE WHERE ID_ENCUESTA = " + id_encuesta, null);

        if(clave.moveToFirst()){

            Usuario usuario = daoUsuario.getUsuarioLogueado();
            Cursor intento = cx.rawQuery("SELECT * FROM INTENTO WHERE ID_CLAVE = " + clave.getInt(clave.getColumnIndex("ID_CLAVE")) + " AND ID_USUARIO = " + usuario.getIDUSUARIO(), null);

            if(intento.moveToFirst()){
                existe = true;
            }

        }
        dba.open();
        return existe;
    }

}
