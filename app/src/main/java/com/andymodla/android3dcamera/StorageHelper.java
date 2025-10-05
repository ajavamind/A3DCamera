package com.andymodla.android3dcamera;


import android.content.Context;
import android.content.SharedPreferences;

public class StorageHelper {

    /**
     * Reads a String value from private storage.
     *
     * @param context     The application context.
     * @param storageName The name of the SharedPreferences file.
     * @param key         The key to retrieve.
     * @return The String value associated with the key, or an empty string if the key is not found or storage is unavailable.
     */
    public String readStringFromPrivateStorage(Context context, String storageName, String key) {
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
            return sharedPreferences.getString(key, ""); // Return empty string if key not found
        } catch (Exception e) {
            // Handle potential exceptions (e.g., storage unavailable)
            e.printStackTrace(); // Log the error for debugging
            return ""; // Return empty string in case of error
        } finally {
            // Ensure SharedPreferences is closed (though it's usually handled automatically)
            if (sharedPreferences != null) {
                //SharedPreferences will be closed automatically when it goes out of scope.
                //No need to explicitly close it.
            }
        }
    }

    /**
     * Writes a String value to private storage.
     *
     * @param context     The application context.
     * @param storageName The name of the SharedPreferences file.
     * @param key         The key to store the value under.
     * @param value       The String value to store.
     */
    public void writeStringToPrivateStorage(Context context, String storageName, String key, String value) {
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
            // Handle potential exceptions (e.g., storage unavailable)
            e.printStackTrace(); // Log the error for debugging
        } finally {
            // Ensure SharedPreferences is closed (though it's usually handled automatically)
            if (sharedPreferences != null) {
                //SharedPreferences will be closed automatically when it goes out of scope.
                //No need to explicitly close it.
            }
        }
    }
}
