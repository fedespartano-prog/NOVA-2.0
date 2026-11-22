package com.novatwo.assistant.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.novatwo.assistant.config.APIKeyStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import android.util.Base64;
import android.graphics.Color;

public class MediaAnalysisActivity extends Activity {
    private static final int PICK_MEDIA = 8120;
    private TextView status;
    private TextView result;
    private Button choose;
    private APIKeyStore keys;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        keys = new APIKeyStore(this);
        getWindow().setStatusBarColor(Color.rgb(5,7,11));
        getWindow().setNavigationBarColor(Color.rgb(5,7,11));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(5,7,11));
        TextView title = new TextView(this);
        title.setText("NOVA · ANALISI FOTO E VIDEO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Seleziona una foto o un video. Per i video NOVA analizzerà alcuni fotogrammi rappresentativi. Il contenuto sarà inviato a OpenAI solo dopo la tua conferma.");
        info.setTextColor(0xFFB0BEC5);
        info.setPadding(0,16,0,16);
        root.addView(info);
        choose = new Button(this);
        choose.setText("SCEGLI FOTO O VIDEO");
        root.addView(choose);
        status = new TextView(this);
        status.setTextColor(0xFF00FFA4);
        status.setPadding(0,16,0,12);
        root.addView(status);
        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(15);
        result.setPadding(0,12,0,12);
        root.addView(result);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        choose.setOnClickListener(v -> pickMedia());
    }

    private void pickMedia() {
        try {
            String key = keys.getKey("OPENAI");
            if (key == null || key.trim().isEmpty()) {
                status.setText("Prima inserisci la chiave OpenAI nelle impostazioni di NOVA.");
                return;
            }
        } catch (Exception e) {
            status.setText("Non riesco a leggere la chiave OpenAI salvata.");
            return;
        }
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[]{"image/*","video/*"});
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        startActivityForResult(android.content.Intent.createChooser(intent,"Seleziona una foto o un video"),PICK_MEDIA);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_MEDIA || resultCode!=RESULT_OK || data==null || data.getData()==null) return;
        Uri uri=data.getData();
        String mime=getContentResolver().getType(uri);
        boolean video=mime!=null && mime.startsWith("video/");
        new AlertDialog.Builder(this)
                .setTitle(video ? "Analizzare questo video?" : "Analizzare questa foto?")
                .setMessage("Il file selezionato verrà elaborato sul telefono e inviato a OpenAI per l'analisi. L'API può comportare costi secondo il tuo account. Per i video saranno inviati fino a tre fotogrammi compressi. Confermi?")
                .setNegativeButton("ANNULLA",null)
                .setPositiveButton("CONFERMO E INVIA",(d,w)->analyze(uri,video))
                .show();
    }

    private void analyze(Uri uri, boolean video) {
        choose.setEnabled(false);
        status.setText("Preparazione del file sul telefono...");
        result.setText("");
        new Thread(() -> {
            try {
                List<String> images = video ? extractVideoFrames(uri) : extractImage(uri);
                if(images.isEmpty()) throw new Exception("Non sono riuscita a leggere immagini dal file selezionato.");
                runOnUiThread(() -> status.setText("Invio a OpenAI e analisi in corso..."));
                String answer = requestVision(images, video
                        ? "Analizza questi fotogrammi, estratti in ordine temporale da un video. Descrivi la scena, le azioni e i dettagli visibili; segnala ciò che non puoi dedurre con certezza."
                        : "Analizza questa foto in italiano. Descrivi ciò che è visibile e distingui chiaramente osservazioni e ipotesi.");
                runOnUiThread(() -> {
                    status.setText("ANALISI COMPLETATA");
                    result.setText(answer);
                    choose.setEnabled(true);
                });
            } catch(Exception e) {
                String message=e.getMessage()==null ? "Errore durante l'analisi." : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("ANALISI NON RIUSCITA: "+message);
                    choose.setEnabled(true);
                });
            }
        }).start();
    }

    private List<String> extractImage(Uri uri) throws Exception {
        Bitmap bitmap;
        try(InputStream in=getContentResolver().openInputStream(uri)) {
            if(in==null) throw new Exception("File non accessibile.");
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            byte[] bytes=readLimited(in,20*1024*1024);
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
            if(bounds.outWidth<=0 || bounds.outHeight<=0) throw new Exception("Formato immagine non supportato.");
            BitmapFactory.Options opts=new BitmapFactory.Options();
            int sample=1;
            while(bounds.outWidth/sample>1024 || bounds.outHeight/sample>1024) sample*=2;
            opts.inSampleSize=sample;
            bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
        }
        if(bitmap==null) throw new Exception("Impossibile decodificare la foto.");
        List<String> output=new ArrayList<>();
        output.add(encodeBitmap(bitmap));
        bitmap.recycle();
        return output;
    }

    private List<String> extractVideoFrames(Uri uri) throws Exception {
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        List<String> output=new ArrayList<>();
        try {
            retriever.setDataSource(this,uri);
            String durationValue=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration=durationValue==null ? 0 : Long.parseLong(durationValue);
            if(duration<=0) throw new Exception("Durata video non disponibile.");
            long[] times={Math.min(500,duration/4),duration/2,Math.max(0,duration-500)};
            long previous=-1;
            for(long ms:times) {
                if(ms==previous) continue;
                previous=ms;
                Bitmap frame=retriever.getFrameAtTime(ms*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(frame!=null) {
                    output.add(encodeBitmap(frame));
                    frame.recycle();
                }
            }
        } finally {
            retriever.release();
        }
        return output;
    }

    private String encodeBitmap(Bitmap source) throws Exception {
        int width=source.getWidth(),height=source.getHeight();
        float scale=Math.min(1f,768f/Math.max(width,height));
        Bitmap scaled=source;
        if(scale<1f) scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),true);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG,70,out);
        if(scaled!=source) scaled.recycle();
        return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
    }

    private byte[] readLimited(InputStream in,int limit) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192]; int count,total=0;
        while((count=in.read(buffer))!=-1) {
            total+=count;
            if(total>limit) throw new Exception("File troppo grande: limite 20 MB.");
            out.write(buffer,0,count);
        }
        return out.toByteArray();
    }

    private String requestVision(List<String> images,String instruction) throws Exception {
        String key=keys.getKey("OPENAI");
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL("https://api.openai.com/v1/chat/completions").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(90000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization","Bearer "+key);
            connection.setRequestProperty("Content-Type","application/json");
            JSONArray parts=new JSONArray();
            JSONObject textPart=new JSONObject();
            textPart.put("type","text");
            textPart.put("text",instruction);
            parts.put(textPart);
            for(String image:images) {
                JSONObject imageUrl=new JSONObject();
                imageUrl.put("url","data:image/jpeg;base64,"+image);
                imageUrl.put("detail","low");
                JSONObject imagePart=new JSONObject();
                imagePart.put("type","image_url");
                imagePart.put("image_url",imageUrl);
                parts.put(imagePart);
            }
            JSONObject user=new JSONObject();
            user.put("role","user");
            user.put("content",parts);
            JSONArray messages=new JSONArray();
            messages.put(user);
            JSONObject body=new JSONObject();
            body.put("model","gpt-4o-mini");
            body.put("messages",messages);
            body.put("max_tokens",700);
            try(OutputStream out=connection.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code=connection.getResponseCode();
            InputStream stream=code>=200 && code<300?connection.getInputStream():connection.getErrorStream();
            byte[] response=readLimited(stream,2*1024*1024);
            String raw=new String(response,StandardCharsets.UTF_8);
            if(code<200 || code>=300) throw new Exception("OpenAI HTTP "+code+": "+raw);
            JSONObject json=new JSONObject(raw);
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } finally {
            if(connection!=null) connection.disconnect();
        }
    }
}
