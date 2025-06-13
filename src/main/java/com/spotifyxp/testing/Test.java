package com.spotifyxp.testing;

import com.spotifyxp.deps.xyz.gianlu.librespot.core.OAuth;
import com.spotifyxp.events.EventSubscriber;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        OAuth oAuth = new OAuth("65b708073fc0480ea92a077233ca87bd", "http://127.0.0.1:5588/login", new EventSubscriber() {
            @Override
            public void run(Object... data) {
                System.out.println(data);
            }
        });
        oAuth.flow(new OAuth.CallbackURLReceiver() {
            @Override
            public void run(String callbackURL) {
                System.out.println(callbackURL);
            }
        });
    }
}
