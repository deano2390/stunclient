/*
 * This file is part of JSTUN. 
 * 
 * Copyright (c) 2005 Thomas King <king@t-king.de> - All rights
 * reserved.
 * 
 * This software is licensed under either the GNU Public License (GPL),
 * or the Apache 2.0 license. Copies of both license agreements are
 * included in this distribution.
 */

package de.javawi.jstun.attribute;

import de.javawi.jstun.util.*;

public class MappedResponseChangedSourceAddressReflectedFrom extends MessageAttribute {

    int port;
    IPV4Address IPV4Address;
    String IPV6Address;

    /*
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |x x x x x x x x|    Family     |           Port                |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                             Address                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public MappedResponseChangedSourceAddressReflectedFrom() {
        super();
        try {
            port = 0;
            IPV4Address = new IPV4Address("0.0.0.0");
        } catch (UtilityException ue) {
            ue.getMessage();
            ue.printStackTrace();
        }
    }

    public MappedResponseChangedSourceAddressReflectedFrom(MessageAttribute.MessageAttributeType type) {
        super(type);
    }

    public int getPort() {
        return port;
    }

    public IPV4Address getIPV4Address() {
        return IPV4Address;
    }

    public void setPort(int port) throws MessageAttributeException {
        if ((port > 65536) || (port < 0)) {
            throw new MessageAttributeException("Port value " + port + " out of range.");
        }
        this.port = port;
    }

    public void setIPV4Address(IPV4Address IPV4Address) {
        this.IPV4Address = IPV4Address;
    }

    public byte[] getBytes() throws UtilityException {
        byte[] result = new byte[12];
        // message attribute header
        // type
        System.arraycopy(Utility.integerToTwoBytes(typeToInteger(type)), 0, result, 0, 2);
        // length
        System.arraycopy(Utility.integerToTwoBytes(8), 0, result, 2, 2);

        // mappedaddress header
        // family
        result[5] = Utility.integerToOneByte(0x01);
        // port
        System.arraycopy(Utility.integerToTwoBytes(port), 0, result, 6, 2);
        // address
        System.arraycopy(IPV4Address.getBytes(), 0, result, 8, 4);
        return result;
    }

    protected static MappedResponseChangedSourceAddressReflectedFrom parse(MappedResponseChangedSourceAddressReflectedFrom ma, byte[] data) throws MessageAttributeParsingException {
        try {

            if (data.length < 8) {
                throw new MessageAttributeParsingException("Data array too short");
            }

            int family = Utility.oneByteToInteger(data[1]);

            if (family == 0x01) {

                /**
                 * family type is IPV4
                 */
                byte[] portArray = new byte[2];
                System.arraycopy(data, 2, portArray, 0, 2);
                ma.setPort(Utility.twoBytesToInteger(portArray));
                int firstOctet = Utility.oneByteToInteger(data[4]);
                int secondOctet = Utility.oneByteToInteger(data[5]);
                int thirdOctet = Utility.oneByteToInteger(data[6]);
                int fourthOctet = Utility.oneByteToInteger(data[7]);
                ma.setIPV4Address(new IPV4Address(firstOctet, secondOctet, thirdOctet, fourthOctet));
                return ma;

            } else if (family == 0x02) {
                /**
                 * family type is IPV6
                 */


                if (data.length < 20) {
                    throw new MessageAttributeParsingException("Data array too short for IPV6");
                }

                byte[] portArray = new byte[2];
                System.arraycopy(data, 2, portArray, 0, 2);
                ma.setPort(Utility.twoBytesToInteger(portArray));


                String ipv6Address = "";
                int hextetsRemaining = 16;
                int offset = 4;

                while (hextetsRemaining > 0) {
                    byte[] addressArray = new byte[2];
                    System.arraycopy(data, offset, addressArray, 0, 2);
                    String group = bytesToHex(addressArray);
                    ipv6Address += group;
                    offset += 2;
                    hextetsRemaining -= 2;

                    if (hextetsRemaining > 0) {
                        ipv6Address += ":";
                    }
                }

                ma.setIPV6Address(ipv6Address);

                return ma;
            } else {
                throw new MessageAttributeParsingException("Family " + family + " is not supported");
            }
        } catch (UtilityException ue) {
            throw new MessageAttributeParsingException("Parsing error");
        } catch (MessageAttributeException mae) {
            throw new MessageAttributeParsingException("Port parsing error");
        }
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String toString() {
        if(IPV4Address != null){
            return "IPV4 Address " + IPV4Address.toString() + ", Port " + port;
        }

        if(IPV6Address != null){
            return "IPV6 Address " + IPV6Address + ", Port " + port;
        }

        return "no address found";
    }

    public String getIPV6Address() {
        return IPV6Address;
    }

    public void setIPV6Address(String IPV6Address) {
        this.IPV6Address = IPV6Address;
    }

    public boolean isIPV4() {
        return IPV4Address != null;
    }
}