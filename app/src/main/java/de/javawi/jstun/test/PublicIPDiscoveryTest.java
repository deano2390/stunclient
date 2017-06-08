package de.javawi.jstun.test;

import com.kodholken.stunclient.Logger;
import com.kodholken.stunclient.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.UtilityException;

/**
 * Created by deanwild on 08/06/2017.
 */

public class PublicIPDiscoveryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryTest.class);
    InetAddress iaddress;
    String stunServer;
    int port;
    int timeoutInitValue = 300; //ms
    MappedAddress ma = null;
    ChangedAddress ca = null;
    boolean nodeNatted = true;
    DatagramSocket socketTest1 = null;
    DiscoveryInfo di = null;

    public PublicIPDiscoveryTest(InetAddress iaddress, String stunServer, int port) {
        super();
        this.iaddress = iaddress;
        this.stunServer = stunServer;
        this.port = port;
    }

    public DiscoveryInfo test() throws UtilityException, SocketException, UnknownHostException, IOException, MessageAttributeParsingException, MessageAttributeException, MessageHeaderParsingException {
        ma = null;
        ca = null;
        nodeNatted = true;
        socketTest1 = null;
        di = new DiscoveryInfo(iaddress);

        test1();

        socketTest1.close();

        return di;
    }

    private boolean test1() throws UtilityException, SocketException, UnknownHostException, IOException, MessageAttributeParsingException, MessageHeaderParsingException {

        int timeSinceFirstTransmission = 0;

        int timeout = timeoutInitValue;

        while (true) {
            try {
                // Test 1 including response
                socketTest1 = new DatagramSocket(new InetSocketAddress(iaddress, 0));
                socketTest1.setReuseAddress(true);
                socketTest1.connect(InetAddress.getByName(stunServer), port);
                socketTest1.setSoTimeout(timeout);

                if (socketTest1.getLocalSocketAddress() instanceof InetSocketAddress) {
                    di.setLocalIP(((InetSocketAddress) socketTest1.getLocalSocketAddress()).getAddress());
                }
                System.out.println("!!!!! SocketAddress: " + socketTest1.getLocalSocketAddress());

                MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
                sendMH.generateTransactionID();

                ChangeRequest changeRequest = new ChangeRequest();
                sendMH.addMessageAttribute(changeRequest);

                byte[] data = sendMH.getBytes();
                DatagramPacket send = new DatagramPacket(data, data.length);
                socketTest1.send(send);
                LOGGER.debug("Test 1: Binding Request sent.");

                MessageHeader receiveMH = new MessageHeader();
                while (!(receiveMH.equalTransactionID(sendMH))) {
                    DatagramPacket receive = new DatagramPacket(new byte[200], 200);
                    socketTest1.receive(receive);
                    receiveMH = MessageHeader.parseHeader(receive.getData());
                    receiveMH.parseAttributes(receive.getData());
                }

                ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);
                ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
                if (ec != null) {
                    di.setError(ec.getResponseCode(), ec.getReason());
                    LOGGER.debug("Message header contains an Errorcode message attribute.");
                    return false;
                }
                if ((ma == null) || (ca == null)) {
                    di.setError(700, "The server is sending an incomplete response (Mapped Address and Changed Address message attributes are missing). The client should not retry.");
                    LOGGER.debug("Response does not contain a Mapped Address or Changed Address message attribute.");
                    return false;
                } else {
                    di.setPublicIP(ma.getAddress().getInetAddress());
                    if ((ma.getPort() == socketTest1.getLocalPort()) && (ma.getAddress().getInetAddress().equals(socketTest1.getLocalAddress()))) {
                        LOGGER.debug("Node is not natted.");
                        nodeNatted = false;
                    } else {
                        LOGGER.debug("Node is natted.");
                    }
                    return true;
                }
            } catch (SocketTimeoutException ste) {
                if (timeSinceFirstTransmission < 7900) {
                    LOGGER.debug("Test 1: Socket timeout while receiving the response.");
                    timeSinceFirstTransmission += timeout;
                    int timeoutAddValue = (timeSinceFirstTransmission * 2);
                    if (timeoutAddValue > 1600) timeoutAddValue = 1600;
                    timeout = timeoutAddValue;
                } else {
                    // node is not capable of udp communication
                    LOGGER.debug("Test 1: Socket timeout while receiving the response. Maximum retry limit exceed. Give up.");
                    di.setBlockedUDP();
                    LOGGER.debug("Node is not capable of UDP communication.");
                    return false;
                }
            }
        }
    }
}
