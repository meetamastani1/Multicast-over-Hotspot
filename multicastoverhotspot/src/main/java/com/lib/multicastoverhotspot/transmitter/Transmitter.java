/*
 * Copyright (C) 2013 Sebastian Kaspari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lib.multicastoverhotspot.transmitter;

import android.content.Intent;
import android.util.Log;

import com.lib.multicastoverhotspot.discovery.Discovery;
import com.lib.multicastoverhotspot.internal.AndroidNetworkIntents;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Transmitter class for sending {@link Intent}s through network.
 */
public class Transmitter {
    private String multicastAddress;
    private int port;

    /**
     * Creates a new {@link Transmitter} instance that will sent {@link Intent}s to
     * the default multicast address and port.
     */
    public Transmitter() {
        this(
                AndroidNetworkIntents.DEFAULT_MULTICAST_ADDRESS,
                AndroidNetworkIntents.DEFAULT_PORT
        );
    }

    /**
     * Creates a new {@link Transmitter} instance that will sent {@link Intent}s to
     * the default multicast address and the given port port.
     *
     * @param "The" destination network port.
     */
    public Transmitter(int port) {
        this(
                AndroidNetworkIntents.DEFAULT_MULTICAST_ADDRESS,
                port
        );
    }

    /**
     * Creates a new {@link Transmitter} instance that will sent {@link Intent}s to
     * the given multicast address and port.
     *
     * @param multicastAddress The destination multicast address, e.g. 225.4.5.6.
     * @param port             The destination network port.
     */
    public Transmitter(String multicastAddress, int port) {
        this.multicastAddress = multicastAddress;
        this.port = port;
    }

    public static String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d("TAG", inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("TAG", ex.toString());
        }
        return null;
    }

    /**
     * Sends an {@link Intent} through the network to any listening {@link Discovery}
     * instance.
     *
     * @param intent The intent to send.
     * @throws TransmitterException
     */
    public void transmit(Intent intent) throws TransmitterException {
        MulticastSocket socket = null;

        try {
            socket = createSocket();
            transmit(socket, intent);
        } catch (UnknownHostException exception) {
            throw new TransmitterException("Unknown host", exception);
        } catch (SocketException exception) {
            throw new TransmitterException("Can't create DatagramSocket", exception);
        } catch (IOException exception) {
            throw new TransmitterException("IOException during sending intent", exception);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    protected MulticastSocket createSocket() throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.setInterface(InetAddress.getByName(getWifiApIpAddress()));
        return socket;
    }

    /**
     * Actual (private) implementation that serializes the {@link Intent} and sends
     * it as {@link DatagramPacket}. Used to separate the implementation from the
     * error handling code.
     *
     * @param socket
     * @param intent
     * @throws UnknownHostException
     * @throws IOException
     */
    private void transmit(MulticastSocket socket, Intent intent) throws UnknownHostException, IOException {
        byte[] data = intent.toUri(0).getBytes();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName(multicastAddress),
                port
        );

        socket.send(packet);
    }
}
