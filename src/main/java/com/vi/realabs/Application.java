package com.vi.realabs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec("go get github.com/googlecodelabs/tools/claat");

        InputStream serviceAccount = new FileInputStream("test-1610421956350-firebase-adminsdk-c4uru-4d03741792.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);


        SpringApplication.run(Application.class, args);
    }

}
