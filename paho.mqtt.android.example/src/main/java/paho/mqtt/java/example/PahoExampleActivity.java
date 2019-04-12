/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package paho.mqtt.java.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class PahoExampleActivity extends AppCompatActivity{
    private HistoryAdapter mAdapter;

    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "ssl://10.10.101.80:5222";

    String clientId = "ab01-68e1fdadfcfaf178";
    final String subscriptionTopic = "hello";
    final String publishTopic = "hello";
    final String publishMessage = "Hello World!";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage();
            }
        });


        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);


        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName("u01-68e1fdadfcfaf178");
        mqttConnectOptions.setPassword("088533413c35bbc6".toCharArray());
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setKeepAliveInterval(60);
//        mqttConnectOptions.setSocketFactory(getSocketFactory(mqttAndroidClient,getFromRaw(),"anhang"));
        mqttConnectOptions.setSocketFactory(setCertificates(getFromAssets("ca.crt")));
//        Properties properties = new Properties();
//        properties.setProperty(SSLSocketFactoryFactory.CLIENTAUTH,"true");
//        mqttConnectOptions.setSSLProperties(properties);



        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
//                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri+";exception:"+exception.getMessage()+exception.getCause());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }

    }

    private void addToHistory(String mainText){
        Log.i("MQTT_"+this.getClass().getName(),"LOG: " + mainText);
        mAdapter.add(mainText);
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    Log.i("MQTT_"+this.getClass().getName(),"Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published");
            if(!mqttAndroidClient.isConnected()){
                int bufferedMessageCount = 0;
                try {
                    bufferedMessageCount = mqttAndroidClient.getBufferedMessageCount();
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                addToHistory( bufferedMessageCount+ " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public InputStream getFromAssets(String fileName) {
        try {
            InputStream inputStream = getResources().getAssets().open(fileName);
            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getFromRaw() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.ca);
            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private SSLSocketFactory getSocketFactory(MqttAndroidClient client, InputStream keyStore, String password) {
        try {
            SSLSocketFactory sslSocketFactory = client.getSSLSocketFactory(keyStore, password);
            return sslSocketFactory;
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * @param rootCert    根证书
     * @param middileCert 中间证书
     * @author zhangchen
     * @date 2018/7/5 下午3:45
     * @Description crt, cer格式证书添加支持
     */
    public SSLSocketFactory setCertificates(java.io.InputStream rootCert, @Nullable java.io.InputStream middileCert) {
        try {

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            //KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null);

            if (middileCert != null) {
                keyStore.setCertificateEntry("middile", certificateFactory.generateCertificate(middileCert));
            }

            keyStore.setCertificateEntry("root", certificateFactory.generateCertificate(rootCert));


            SSLContext sslContext = SSLContext.getInstance("TLS");

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance("X509");

            trustManagerFactory.init(keyStore);
            sslContext.init(
                    null,
                    trustManagerFactory.getTrustManagers(),
                    null);
            return sslContext.getSocketFactory();
//            connectionConfig.setSocketFactory(sslContext.getSocketFactory());
//            connectionConfig.setCustomSSLContext(sslContext);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (CertificateException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        } catch (KeyManagementException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    public SSLSocketFactory setCertificates(InputStream... certificates) {
        try {
//            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            keyStore.load(null);
//            int index = 0;
//            for (InputStream certificate : certificates) {
//                String certificateAlias = Integer.toString(index++);
//                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
//
//                try {
//                    if (certificate != null)
//                        certificate.close();
//                } catch (IOException e) {
//                }
//            }
//
            SSLContext sslContext = SSLContext.getInstance("TLS");
//
//            TrustManagerFactory trustManagerFactory =
//                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//
//            trustManagerFactory.init(keyStore);
            TrustManager[] trustAllCerts = new TrustManager[1];
            TrustManager tm = new UMTrustManager(certificates[0]);
            trustAllCerts[0] = tm;
            sslContext.init
                    (
                            null,
                            trustAllCerts,
                            new SecureRandom()
                    );
            return sslContext.getSocketFactory();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static class UMTrustManager implements TrustManager, X509TrustManager {
        private InputStream inputStream;

        public UMTrustManager(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public X509Certificate[] getAcceptedIssuers() {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate=certificateFactory.generateCertificate(inputStream);
                X509Certificate[] x509Certificate = new X509Certificate[]{(X509Certificate)certificate};
                return x509Certificate;
            } catch (CertificateException e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }
    }
}
