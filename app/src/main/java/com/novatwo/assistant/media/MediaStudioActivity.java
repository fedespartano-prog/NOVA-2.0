package com.novatwo.assistant.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Toast;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.net.Uri;

import com.novatwo.assistant.config.APIKeyStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

public class MediaStudioActivity extends Activity {
    private EditText prompt;
    private TextView status;
    private ImageView preview;
    private Button generate;
    private Bitmap generated;
    private APIKeyStore keys;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        keys = new APIKeyStore(this);
        getWindow().setStatusBarColor(Color.rgb(5,7,11));
        getWindow().setNavigationBarColor(Color.rgb(5,7,11));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,24,28,24);
        root.setBackgroundColor(Color.rgb(5,7,11));
        TextView title = new TextView(this);
        title.setText("NOVA · STUDIO IMMAGINI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        root.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Descrivi l'immagine da creare. La generazione usa la chiave OpenAI configurata e può comportare costi del provider.");
        hint.setTextColor(0xFFB0BEC5);
        hint.setPadding(0,16,0,12);
        root.addView(hint);
        prompt = new EditText(this);
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(0xFF9AA6B2);
        prompt.setHint("Es. un paesaggio cinematografico al tramonto...");
        prompt.setMinLines(3);
        prompt.setGravity(Gravity.TOP);
        root.addView(prompt, new LinearLayout.LayoutParams(-1,-2));
        generate = new Button(this);
        generate.setText("GENERA IMMAGINE");
        root.addView(generate);
        status = new TextView(this);
        status.setTextColor(0xFF00FFA4);
        status.setPadding(0,12,0,12);
        root.addView(status);
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        Button save = new Button(this);
        save.setText("SALVA NELLA GALLERIA");
        root.addView(save);
        save.setOnClickListener(v -> saveImage());
        generate.setOnClickListener(v -> confirmAndGenerate());
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void confirmAndGenerate() {
        String text = prompt.getText().toString().trim();
        if (text.isEmpty()) { status.setText("Scrivi prima una descrizione."); return; }
        try {
            String key = keys.getKey("OPENAI");
            if (key == null || key.trim().isEmpty()) {
                status.setText("Configura prima una chiave OpenAI nelle impostazioni di NOVA.");
                return;
            }
        } catch (Exception e) { status.setText("Impossibile leggere la chiave OpenAI."); return; }
        new AlertDialog.Builder(this)
            .setTitle("Confermi la generazione?")
            .setMessage("NOVA invierà la descrizione a OpenAI. Il servizio potrebbe addebitare il costo previsto dal tuo account. Nessuna richiesta partirà senza il tuo consenso.")
            .setNegativeButton("ANNULLA", null)
            .setPositiveButton("CONFERMO", (d,w) -> generateImage(text))
            .show();
    }

    private void generateImage(String description) {
        generate.setEnabled(false);
        status.setText("Generazione in corso. Attendi...");
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String key = keys.getKey("OPENAI");
                URL url = new URL("https://api.openai.com/v1/images/generations");
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(20000);
                c.setReadTimeout(120000);
                c.setDoOutput(true);
                c.setRequestProperty("Authorization","Bearer "+key);
                c.setRequestProperty("Content-Type","application/json");
                JSONObject body = new JSONObject();
                body.put("model","gpt-image-2.5-flare");
                body.put("prompt",description);
                body.put("size","1024x1024");
                body.put("quality","low");
                body.put("n",1);
                try(OutputStream out=c.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code=c.getResponseCode();
                InputStream in=code>=200 && code<300 ? c.getInputStream() : c.getErrorStream();
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                byte[] buf=new byte[8192]; int count;
                while((count=in.read(buf))!=-1) bytes.write(buf,0,count);
                String response=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
                if(code<200 || code>=300) throw new Exception("OpenAI HTTP "+code+": "+response);
                JSONObject json=new JSONObject(response);
                String encoded=json.getJSONArray("data").getJSONObject(0).getString("b64_json");
                byte[] image=Base64.decode(encoded,Base64.DEFAULT);
                Bitmap bitmap=BitmapFactory.decodeByteArray(image,0,image.length);
                if(bitmap==null) throw new Exception("Immagine ricevuta ma non leggibile.");
                runOnUiThread(() -> {
                    generated=bitmap;
                    preview.setImageBitmap(bitmap);
                    status.setText("Immagine creata. Puoi salvarla nella galleria.");
                    generate.setEnabled(true);
                });
            } catch(Exception e) {
                String msg=e.getMessage()==null ? "Errore durante la generazione." : e.getMessage();
                runOnUiThread(() -> { status.setText("Generazione non riuscita: "+msg); generate.setEnabled(true); });
            } finally { if(c!=null)c.disconnect(); }
        }).start();
    }

    private void saveImage() {
        if(generated==null) { Toast.makeText(this,"Non c'è ancora un'immagine da salvare.",Toast.LENGTH_SHORT).show(); return; }
        try {
            ContentValues values=new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,"NOVA_"+System.currentTimeMillis()+".png");
            values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/NOVA");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            if(uri==null) throw new Exception("Impossibile creare il file.");
            try(OutputStream out=getContentResolver().openOutputStream(uri)) {
                generated.compress(Bitmap.CompressFormat.PNG,100,out);
            }
            Toast.makeText(this,"Immagine salvata nella galleria.",Toast.LENGTH_LONG).show();
        } catch(Exception e) { Toast.makeText(this,"Salvataggio non riuscito: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
}