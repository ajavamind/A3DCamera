package com.andymodla.android3dcamera;

// Clipboard Helper

import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

//  ClipboardHelper clipboardHelper;
//  String clipboardText;
//  List clipboardTexts;
//  boolean requestClipboard = false;
//  boolean DEBUG = true;
//  void initClipboard() {
//    clipboardHelper = new ClipboardHelper(this);
//    clipboardHelper.startMonitoring();
//  }
//
//  String getClipboard() {
//    String clipboardText = clipboardHelper.getClipboardText(this);
////  List clipboardTexts = clipboardHelper.getAllClipboardTexts(this.getContext());
////  if (DEBUG) println("clipboard:\n"+clipboardTexts);
//    return clipboardText;
//  }

  public class ClipboardHelper implements ClipboardManager.OnPrimaryClipChangedListener {
    private Context context;
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener listener;
    private Handler handler;
    private boolean newData = false;

    // Clipboard Helper construcor

    public ClipboardHelper(Context context) {
      this.context = context;
      this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      //this.handler = new Handler(context.getMainLooper()); // Important for UI updates
      this.handler = new Handler(Looper.getMainLooper());
    }

    public boolean getClipboardStatus() {
      if (newData) {
        newData = false;
        return true;
      }
      return false;
    }

    /**
     * Retrieves the text from the Android clipboard.
     *
     * @param context The application's context.  Required to access the ClipboardManager.
     * @return The text currently on the clipboard, or null if the clipboard is empty or an error occurs.
     */
    public String getClipboardText(Context context) {
      if (context == null) {
        // Handle null context appropriately.  Throwing an exception is a common choice.
        throw new IllegalArgumentException("Context cannot be null.");
      }

      ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

      if (clipboardManager == null) {
        // Handle the case where the ClipboardManager is not available.
        // This is unlikely, but good to check for robustness.
        return null; // Or throw an exception, log an error, etc.
      }

      try {
        if (clipboardManager.hasPrimaryClip()) {
          return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
        } else {
          return null; // Clipboard is empty
        }
      } catch (Exception e) {
        // Handle potential exceptions (e.g., security exceptions, null pointer exceptions).
        // Log the error for debugging.  Returning null is a reasonable default.
        e.printStackTrace(); // Log the exception for debugging.  Remove in production if not needed.
        return null;
      }
    }

    // How to use
    // In your Activity or Fragment:
    //Context context = this; // Or getActivity() if you need the application context
    //String clipboardText = ClipboardHelper.getClipboardText(context);

    //if (clipboardText != null) {
    //  // Use the clipboard text
    //  textView.setText(clipboardText); // Example: set the text to a TextView
    //} else {
    //  // Handle the case where the clipboard is empty or an error occurred
    //  Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show();
    //}

    /**
     * Retrieves all text items from the Android clipboard.
     *
     * @param context The application's context.
     * @return A list of strings, each representing a text item on the clipboard.
     * Returns an empty list if the clipboard is empty or an error occurs.
     */
    public List<String> getAllClipboardTexts(Context context) {
      if (context == null) {
        throw new IllegalArgumentException("Context cannot be null.");
      }

      ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

      if (clipboardManager == null) {
        return new ArrayList<>(); // Return empty list if ClipboardManager is unavailable
      }

      try {
        ClipData clipData = clipboardManager.getPrimaryClip();

        if (clipData != null) {
          List<String> textItems = new ArrayList<>();
          int itemCount = clipData.getItemCount();

          for (int i = 0; i < itemCount; i++) {
            ClipData.Item item = clipData.getItemAt(i);
            if (item != null && item.getText() != null) { // Added null checks
              textItems.add(item.getText().toString());
            }
          }
          return textItems;
        } else {
          return new ArrayList<>(); // Clipboard is empty
        }
      } catch (Exception e) {
        e.printStackTrace(); // Log for debugging
        return new ArrayList<>(); // Return empty list on error
      }
    }

    // How to use
    // In your Activity or Fragment:
    //Context context = this;
    //List<String> clipboardTexts = ClipboardHelper.getAllClipboardTexts(context);

    //if (!clipboardTexts.isEmpty()) {
    //  // Display the available text items to the user (e.g., in a dialog or list)
    //  // or let the user choose which one to use.

    //  // Example: Display the first text item
    //  String firstText = clipboardTexts.get(0);
    //  textView.setText(firstText);

    //  // Example: Show a dialog with all the options
    //  // (You'd need to create a custom dialog for this)
    //  /*
    //  AlertDialog.Builder builder = new AlertDialog.Builder(context);
    //   builder.setTitle("Choose a text from the clipboard:");
    //   builder. setItems(clipboardTexts.toArray(new String[0]), new DialogInterface.OnClickListener() {
    //   @Override
    //   public void onClick(DialogInterface dialog, int which) {
    //   textView.setText(clipboardTexts.get(which));
    //   }
    //   });
    //   builder.show();
    //   */
    //} else {
    //  // Handle the case where the clipboard is empty or an error occurred
    //  Toast.makeText(context, "Clipboard is empty or an error occurred", Toast.LENGTH_SHORT).show();
    //}


    public void startMonitoring() {
      if (clipboardManager != null) {
        //  listener = new ClipboardManager.OnPrimaryClipChangedListener() {
        //    @Override
        //      public void onPrimaryClipChanged() {
        //      // This runs on the main thread, so you can update UI directly.
        //      handler.post(() -> {
        //        // Call your getClipboardText or getAllClipboardTexts here
        //        //List<String> clipboardTexts = getAllClipboardTexts(context);

        //        //if (!clipboardTexts.isEmpty()) {
        //        // Do something with the clipboard text (e.g., update UI)
        //        // Example:
        //        // textView.setText(clipboardTexts.get(0));
        //        //}

        //        // save data prompt
        //        //data = getClipboardText(context);
        //        newData = true;
        //        if (DEBUG) println("Clipboard = "+newData);
        //      }
        //      );
        //    }
        //  };
        //  clipboardManager.addOnPrimaryClipChangedListener(listener);
        //}
      }
    }

    public void stopMonitoring() {
      //if (listener != null) {
      //  clipboardManager.removeOnPrimaryClipChangedListener(listener);
      //}
    }

    @Override
    public void onPrimaryClipChanged() {
      // Handle clipboard change here
      System.out.println("Clipboard Clipboard content changed!");
      newData = true;
      // ... (Retrieve and process clipboard content) ...
    }

    //public void startMonitoring() {
    //    if (clipboardManager != null) {
    //        receiver = new BroadcastReceiver() {
    //            @Override
    //            public void onReceive(Context context, Intent intent) {
    //                if (Intent.ACTION_CLIPBOARD_CHANGED.equals(intent.getAction())) {
    //                    handler.post(() -> {
    //                        List<String> clipboardTexts = getAllClipboardTexts(context);

    //                        if (!clipboardTexts.isEmpty()) {
    //                            // Do something with the clipboard text (e.g., update UI)
    //                            // Example:
    //                            // textView.setText(clipboardTexts.get(0));
    //                        }
    //                    });
    //                }
    //            }
    //        };

    //        IntentFilter filter = new IntentFilter(ClipboardManager.ACTION_CLIPBOARD_CHANGED);
    //        context.registerReceiver(receiver, filter);
    //    }
    //}

    //public void stopMonitoring() {
    //    if (receiver != null) {
    //        context.unregisterReceiver(receiver);
    //    }
    //}

}
