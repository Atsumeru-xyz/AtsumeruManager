package xyz.atsumeru.manager.facade;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

public class SSLSocketFactoryFacade extends SSLSocketFactory {
    private static TrustManager[] trustAllCerts;
    private SSLSocketFactory sslsf;

    public SSLSocketFactoryFacade() {
        try {
            SSLContext context = SSLContext.getInstance("SSL", "TLS");
            context.init(null, createTrustyTrustManager(), new SecureRandom());
            sslsf = context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException | NoSuchProviderException e) {
            if (!(e instanceof NoSuchProviderException)) {
                System.err.println(e.getLocalizedMessage());
            }
            try {
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, createTrustyTrustManager(), new SecureRandom());
                sslsf = context.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                System.err.println(ex.getLocalizedMessage());
                sslsf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            }
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslsf.getDefaultCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return super.createSocket();
    }

    @Override
    public Socket createSocket(Socket socket, InputStream inputStream, boolean b) throws IOException {
        return super.createSocket(socket, inputStream, b);
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslsf.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(sslsf.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(sslsf.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(sslsf.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(sslsf.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(sslsf.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if(socket instanceof SSLSocket) {
            ArrayList<String> supportedProtocols =
                    new ArrayList<>(Arrays.asList(((SSLSocket) socket).getSupportedProtocols()));
            supportedProtocols.retainAll(Arrays.asList("TLSv1.2", "TLSv1.1", "TLSv1"));

            ((SSLSocket)socket).setEnabledProtocols(supportedProtocols.toArray(new String[0]));
        }
        return socket;
    }

    public static TrustManager[] createTrustyTrustManager() {
        // Create a trust manager that does not validate certificate chains
        if (trustAllCerts == null) {
            trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                    }};
        }
        return trustAllCerts;
    }
}