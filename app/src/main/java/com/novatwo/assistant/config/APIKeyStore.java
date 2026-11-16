
package com.novatwo.assistant.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class APIKeyStore {

    private static final String PREFS = "nova_secure_settings";
    private static final String ALIAS = "NOVA_API_KEYS_AES";
    private static final String SELECTED = "selected_provider";
    private static final String DEFAULT_PROVIDER = "AUTO";

    private final SharedPreferences preferences;

    public APIKeyStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }

    public void saveKey(String provider, String apiKey)
            throws Exception {

        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider non valido.");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Chiave API vuota.");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

        byte[] encrypted = cipher.doFinal(
                apiKey.trim().getBytes(StandardCharsets.UTF_8)
        );

        byte[] iv = cipher.getIV();
        byte[] combined = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(
                encrypted, 0, combined, iv.length, encrypted.length
        );

        preferences.edit().putString(
                providerKey(provider),
                Base64.encodeToString(combined, Base64.NO_WRAP)
        ).apply();
    }

    public String getKey(String provider) throws Exception {

        String stored = preferences.getString(
                providerKey(provider),
                null
        );

        if (stored == null || stored.isEmpty()) {
            return null;
        }

        byte[] combined = Base64.decode(stored, Base64.NO_WRAP);

        if (combined.length <= 12) {
            throw new IllegalStateException("Chiave salvata non valida.");
        }

        byte[] iv = new byte[12];
        byte[] encrypted = new byte[combined.length - 12];

        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(
                combined, 12, encrypted, 0, encrypted.length
        );

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                new GCMParameterSpec(128, iv)
        );

        return new String(
                cipher.doFinal(encrypted),
                StandardCharsets.UTF_8
        );
    }

    public void setSelectedProvider(String provider) {

        if (provider == null || provider.trim().isEmpty()) {
            provider = DEFAULT_PROVIDER;
        }

        preferences.edit()
                .putString(SELECTED, provider.trim().toUpperCase())
                .apply();
    }

    public String getSelectedProvider() {
        return preferences.getString(
                SELECTED,
                DEFAULT_PROVIDER
        );
    }

    public void deleteKey(String provider) {
        preferences.edit()
                .remove(providerKey(provider))
                .apply();
    }

    private String providerKey(String provider) {

        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider non valido.");
        }

        return "api_key_" + provider.trim().toUpperCase();
    }

    private SecretKey getSecretKey() throws Exception {

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (keyStore.containsAlias(ALIAS)) {
            return (SecretKey) keyStore.getKey(ALIAS, null);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
        );

        generator.init(
                new KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        );

        return generator.generateKey();
    }
}
