package com.andymodla.android3dcamera;

import android.content.Context;
import android.graphics.Bitmap;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import com.openai.core.MultipartField;
import com.openai.models.images.ImageEditParams;
import com.openai.models.images.ImageEditParams.Image;
import com.openai.models.images.ImageModel;


import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

import java.io.InputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.*;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.*;
import java.io.*;
import java.util.Base64;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.widget.Toast;

public class AIvision {
    String myToken = "local"; // OpenAI API key
    private static final String TAG = "A3DCamera";
    //token = System.getenv("OPENAI_API_KEY");
    //myToken = "local";
    //String prompt = "Answer only yes or no. Is there a deer in this photo?";


    File selectedFile;
    static String baseUrl = "http://192.168.1.96:8080/v1/";  // local LLM

    //static String model =  "gpt-4o";
    static String model = "gemma3";
    static double temperature = 0.0; // expect no randomness from the model
    static double topP = 1.0;
    int timeout = 600;
    volatile boolean ready = true;

    Executor executor;
    Handler handler;
    Context context;
    MainActivity mainActivity;
    OpenAIClient client;

    public AIvision(MainActivity context) {
        this.context = context;
        this.mainActivity = context;
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        System.out.println("AIvision constructor");
        // Create the OpenAI client with the API key for local LLM server running on private local network
        client = OpenAIOkHttpClient.builder()
                .apiKey(myToken)
                .build();
    }

    public void getInformationFromSurfaceView(SurfaceView surfaceView, String prompt) {
        if (!ready) return;
        ready = false;
        Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
        PixelCopy.request(surfaceView, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
            @Override
            public void onPixelCopyFinished(int copyResult) {
                if (copyResult == PixelCopy.SUCCESS) {
                    // Bitmap now contains the content of the SurfaceView
                    //Toast.makeText(context, "PixelCopy success", Toast.LENGTH_SHORT).show();
                    getInformationFromImage(bitmap, prompt);

                } else {
                    Toast.makeText(context, "PixelCopy failed", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Handler(Objects.requireNonNull(Looper.myLooper()))); // Use a Handler to specify the callback thread

    }

    // Sends an image and prompt to OpenAI and returns the text information about the image
    String getInformationFromImage(Bitmap bitmap, String prompt) {
        final String[] firstContent = new String[1];

        OpenAIClient clientLocal = client.withOptions(optionsBuilder -> {
            optionsBuilder.baseUrl(baseUrl);
            optionsBuilder.maxRetries(2);
        });

        //   avoids android.os.NetworkOnMainThreadException
        executor.execute(() -> {
            sendToAI(clientLocal, bitmap, prompt, firstContent);
        });

        return firstContent[0];
    }

    void sendToAI(OpenAIClient clientLocal, Bitmap bitmap, String prompt, String[] firstContent) {
        System.out.println("sendToAI");
        try {
            // Encode image as base64
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageBytes = stream.toByteArray();
            String imageBase64Url = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);

            ChatCompletionContentPart logoContentPart =
                    ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(imageBase64Url)
                                    .build())
                            .build());
            ChatCompletionContentPart questionContentPart =
                    ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                            .text(prompt)
                            .build());
            ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                    .model(model)  //(ChatModel.GPT_4O_MINI)
                    .maxCompletionTokens(2048)
                    .addUserMessageOfArrayOfContentParts(List.of(questionContentPart, logoContentPart))
                    .build();

//            client.chat().completions().create(createParams).choices().stream()
//                    .flatMap(choice -> choice.message().content().stream())
//                    .forEach(System.out::println);

            // sent it to local LLM and get the response
            firstContent[0] = clientLocal.chat().completions().create(createParams).choices().stream()
                    .flatMap(choice -> choice.message().content().stream())
                    .findFirst() // Get an Optional<String> containing the first element
                    .orElse(""); // Extract the String or return empty

            System.out.println("First content: " + firstContent[0]);
            handler.post(() -> {
                // Update the UI on the main thread with the result
                //Toast.makeText(context, "First content: " + firstContent[0], Toast.LENGTH_SHORT).show();
                if (firstContent[0].toLowerCase().contains("deer")) {
                    //if (firstContent[0].toLowerCase().contains("dog") || firstContent[0].toLowerCase().contains("cat")) {
                    mainActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            mainActivity.captureImages();
                        }
                    });
                }
            });

        } catch (Exception e) {
            handler.post(() -> {
                // Handle any errors on the main thread
                Toast.makeText(context, "sendToAI Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, e.toString());
            });
        }
        //bitmap.recycle();

        ready = true;
    }
}
