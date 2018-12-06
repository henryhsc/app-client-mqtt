package com.notificacion.client.NotificacionService;

import android.content.Context;
import android.util.Log;

import com.notificacion.client.R;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SslUtility {
    private static SslUtility mInstance = null;
    private static Context mContext = null;

    private static final String _TAG = "KEYSTORE";
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
    private static KeyStore keyStore;
    private static SSLContext sslContext = null;
    private static String commonName = "common-name";

    public SslUtility(Context context) {
        mContext = context;
    }

    public static SslUtility getInstance( ) {
        if ( null == mInstance ) {
            throw new RuntimeException("Primero llamar a SslUtility.newInstance(Context) ");
        }
        return mInstance;
    }

    public static SslUtility newInstance( Context context ) {
        if ( null == mInstance ) {
            mInstance = new SslUtility( context );
        }
        return mInstance;
    }

    public SSLSocketFactory getCertificate(){
        Security.addProvider(new BouncyCastleProvider());
        SSLContext context = null;
        try {
            //CertificateFactory cf = CertificateFactory.getInstance("X509");     // instancia generadora de certificados
            CertificateFactory cf = CertificateFactory.getInstance("X.509");


            // LEYENDO CERTIFICADO CA
            PemReader caReader = new PemReader( new InputStreamReader( mContext.getResources().openRawResource(R.raw.cacert) ) );   // cacert para servidor produccion
            PemObject caCertObject = caReader.readPemObject();
            Log.i("CERTIFICATE", "CERTIFICADO: cacert.pem - TIPO DE CERTIFICADO: " + caCertObject.getType());
            X509Certificate caCertX = (X509Certificate) cf.generateCertificate( new ByteArrayInputStream(caCertObject.getContent()) );
            caReader.close();


            // LEYENDO CERTIFICADO CERT
            PemReader certReader = new PemReader( new InputStreamReader( mContext.getResources().openRawResource(R.raw.cert) ) );   // cert para servidor produccion
            PemObject certObject = certReader.readPemObject();
            X509Certificate certX = (X509Certificate) cf.generateCertificate( new ByteArrayInputStream(certObject.getContent()) );
            Log.i("CERTIFICATE", "CERTIFICADO: cert.pem - TIPO DE CERTIFICADO: " + certObject.getType());
            certReader.close();

            // LEYENDO CERTIFICADO KEY
            PEMParser keyParser = new PEMParser( new InputStreamReader( mContext.getResources().openRawResource(R.raw.key) ) );     // key para servidor produccion
            JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
            PEMKeyPair pemKeyPair = (PEMKeyPair) keyParser.readObject();
            KeyPair keyPair = keyConverter.getKeyPair(pemKeyPair);

            // CARGANDO CERTIFICADO CA
            KeyStore caStore = KeyStore.getInstance("BKS"); //KeyStore.getInstance(KeyStore.getDefaultType());
            caStore.load(null, null);
            caStore.setCertificateEntry("ca-certificate", caCertX);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caStore);

            // CARGANDO CERTIFICADO Y KEY
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("certificate", certX);
            keyStore.setKeyEntry("private-key", keyPair.getPrivate(), new String("password").toCharArray(), new Certificate[]{certX});
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
            keyManagerFactory.init(keyStore, new String("password").toCharArray());

            context = SSLContext.getInstance("TLS");
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        } catch (IOException e) {
            e.printStackTrace();
            Log.w("CERTIFICATE", "error leyendo certificado CA");
        } catch (CertificateException e) {
            e.printStackTrace();
            Log.w("CERTIFICATE", "error en certificado CA");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Log.w("CERTIFICATE", "error en KeyStore");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            Log.w("CERTIFICATE", "error en key, clave no recuperable");
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return context.getSocketFactory();
    }

}
