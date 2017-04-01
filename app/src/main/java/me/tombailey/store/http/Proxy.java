package me.tombailey.store.http;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by tomba on 25/01/2017.
 */

public class Proxy {

    private static final byte NULL_BYTE = 0;
    private static final byte SOCKS4A_BYTE = 4;
    private static final byte STREAM_CONNECTION_BYTE = 1;
    private static final byte REQUEST_GRANTED_BYTE = 90;


    private String mHost;
    private int mPort;

    public Proxy(String host, int port) {
        mHost = host;
        mPort = port;
    }

    public Socket getSocketFor(String destinationHost, int destintationPort, int timeout) throws IOException {
        Socket proxySocket = new Socket();
        proxySocket.connect(new InetSocketAddress(mHost, mPort), timeout);
        socks4aHandShake(proxySocket, destinationHost, destintationPort);
        return proxySocket;
    }

    private void socks4aHandShake(Socket socket, String destinationHost, int destintationPort) throws IOException {
        socks4aInit(socket, destinationHost, destintationPort);
        socks4aVerify(socket);
    }

    private void socks4aInit(Socket socket, String destinationHost, int destintationPort) throws IOException {
        //handshake according to https://en.wikipedia.org/wiki/SOCKS
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        //send socks version
        outputStream.writeByte(SOCKS4A_BYTE);

        //send connection type (stream vs binding)
        outputStream.writeByte(STREAM_CONNECTION_BYTE);

        //send destination port
        outputStream.writeShort((short) destintationPort);

        //send invalid IP to trigger domain to be resolved by proxy
        outputStream.writeInt(1);

        //send null byte (user id not applicable)
        outputStream.writeByte(NULL_BYTE);

        //send domain name to be resolved
        outputStream.writeBytes(destinationHost);
        outputStream.writeByte(NULL_BYTE);
    }

    private void socks4aVerify(Socket socket) throws IOException {
        //handshake according to https://en.wikipedia.org/wiki/SOCKS
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        //receive response bytes
        byte firstByte = inputStream.readByte();
        byte secondByte = inputStream.readByte();
        if(firstByte == NULL_BYTE && secondByte == REQUEST_GRANTED_BYTE) {
            //read unused proxy bytes so socket produces application data only
            inputStream.readShort();
            inputStream.readInt();
        } else {
            socket.close();
            throw new IOException("SOCKS4a connect failed, got " + firstByte + " - " + secondByte + ", but expected 0x00 - 0x5a");
        }
    }
}
