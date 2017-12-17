package com.example.mikedice.protocol;

//
//  protocol
//
//  Created by Mike Dice on 12/2/17.
//  Copyright © 2017 Mike Dice. All rights reserved.
//

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/********* Spec ********************

 packet with data of length 5
 [--<newline>
 PKT.V1 HELLO<newline>
 LEN: 5<newline>
 ADSFG

 Packet with no data
 [--<newline>
 PKT.V1 HELLO<newline>
 LEN: 0<newline>

 ***********************************/


public class Protocol {
    // Keep track of the thing we are parsing
    private enum parserState {
        Ready0, // default state
        Ready1, // [
        Ready2, // -
        Ready3, // -
        Ready4, // \r
        Ready5, // \n
        RH1, // P
        RH2, // K
        RH3, // T
        RH4, // .
        RH5, // V
        RH6, // 0-9 1-*
        RH7, // ' '
        RH8, // A-F 1-*
        RH9, // \r,
        RH10,  // \n
        LEN1, // L
        LEN2, // E
        LEN3, // N
        LEN4, // :
        LEN5, // 0-9 1-*
        LEN6, // \r
        LEN7, // \n
        RecvData,
    }

    ;


    private parserState state;
    private Integer CurrDataLen = 0;
    private Integer CurrDataRecvd = 0;
    private byte[] PacketSig = new byte[Constants.MAX_PACKETSIG];
    private byte[] PacketCommand = new byte[Constants.MAX_PACKETCOMMAND];
    private byte[] PacketData = new byte[Constants.MAX_PACKETDATA];
    private byte[] PacketLen = new byte[Constants.MAX_PACKETLEN];

    private Integer PacketSigIdx = 0;
    private Integer PacketCommandIdx = 0;
    private Integer PacketDataIdx = 0;
    private Integer PacketLenIdx = 0;


    // Application must call ResetParser to enable the parser to start consuming
    // next available packet. Typically call at beginning of application and
    // immediately after the application has processed a packet
    public void ResetParser() {
        this.state = parserState.Ready0;
        Arrays.fill(this.PacketSig, (byte) 0);
        Arrays.fill(this.PacketCommand, (byte) 0);
        Arrays.fill(this.PacketData, (byte) 0);
        Arrays.fill(this.PacketLen, (byte) 0);
        this.PacketSigIdx = 0;
        this.PacketCommandIdx = 0;
        this.PacketDataIdx = 0;
        this.PacketLenIdx = 0;
        this.CurrDataRecvd = 0;
        this.CurrDataLen = 0;
    }

    private Packet CreatePacket(){
        Packet packet = new Packet();
        packet.Sig = new String(this.PacketSig, 0, this.PacketSigIdx);
        packet.Command = new String(this.PacketCommand, 0, this.PacketCommandIdx);
        packet.DataLength = this.CurrDataLen;
        packet.Data = Arrays.copyOfRange(this.PacketData, 0, this.PacketDataIdx);
        return packet;
    }


    private void AddPacketSigData(byte data) {
        if (this.PacketSigIdx < Constants.MAX_PACKETSIG) {
            this.PacketSig[this.PacketSigIdx++] = data;
        }
    }

    private void AddPacketCommandData(byte data) {
        if (this.PacketCommandIdx < Constants.MAX_PACKETCOMMAND) {
            this.PacketCommand[this.PacketCommandIdx++] = data;
        }
    }

    private void AddPacketLenData(byte data) {
        if (this.PacketLenIdx < Constants.MAX_PACKETLEN) {
            this.PacketLen[this.PacketLenIdx++] = data;
        }
    }

    private void AddPacketData(byte data) {
        if (this.PacketDataIdx < Constants.MAX_PACKETDATA) {
            this.PacketData[this.PacketDataIdx++] = data;
            this.CurrDataRecvd++;
        }
    }

    private void SetDataLen() {
        String str = new String(PacketLen, 0, PacketLenIdx);
        this.CurrDataLen = Integer.parseInt(str);
        this.CurrDataRecvd = 0;
    }

    private void CompletePacket(IPacketProcessor packetProcessor){
        state = parserState.Ready0;
        Packet newPacket = CreatePacket();
        packetProcessor.ProcessPacket(newPacket);
        ResetParser();
    }

    // Bytes are passed to the protocol processor one at a time by the application.
    // THe application is responsible for obtaining those byte from a transport stream
    // that it maintains, such as serial port, tcp channel or bluetooth stream.
    // If function returns FALSE then application should call ResetParser because
    // a byte not understood by the protocol was received.
    public boolean ProcessStreamByte(byte dataByte, IPacketProcessor packetProcessor) {
        boolean result = false;
        // receiving ready sequence
        if (state == parserState.Ready0 && dataByte == '[') {
            state = parserState.Ready1;
        } else if (state == parserState.Ready1 && dataByte == '-') {
            state = parserState.Ready2;
        } else if (state == parserState.Ready2 && dataByte == '-') {
            state = parserState.Ready3;
        } else if (state == parserState.Ready3 && dataByte == '\r') {
            state = parserState.Ready4;
        } else if (state == parserState.Ready4 && dataByte == '\n') {
            state = parserState.Ready5;
        }


        // receiving header
        else if (state == parserState.Ready5 && dataByte == 'P') {
            state = parserState.RH1;
            AddPacketSigData(dataByte);
        } else if (state == parserState.RH1 && dataByte == 'K') {
            state = parserState.RH2;
            AddPacketSigData(dataByte);
        } else if (state == parserState.RH2 && dataByte == 'T') {
            state = parserState.RH3;
            AddPacketSigData(dataByte);
        } else if (state == parserState.RH3 && dataByte == '.') {
            state = parserState.RH4;
            AddPacketSigData(dataByte);
        } else if (state == parserState.RH4 && dataByte == 'V') {
            state = parserState.RH5;
            AddPacketSigData(dataByte);
        }

        // receiving version of protocol
        else if ((state == parserState.RH5 || state == parserState.RH6) && dataByte >= '0' && dataByte <= '9') {
            state = parserState.RH6;
            AddPacketSigData(dataByte);
        }

        // receving packet command
        else if (state == parserState.RH6 && (dataByte == ' ' || dataByte == '\t')) {
            state = parserState.RH7;
        } else if ((state == parserState.RH7 || state == parserState.RH8) && dataByte >= 'A' && dataByte <= 'Z') {
            state = parserState.RH8;
            AddPacketCommandData(dataByte);
        } else if (state == parserState.RH8 && dataByte == '\r') {
            state = parserState.RH9;
        } else if (state == parserState.RH9 && dataByte == '\n') {
            state = parserState.RH10;
        }

        // receiving data length
        else if (state == parserState.RH10 && dataByte == 'L') {
            state = parserState.LEN1;
        } else if (state == parserState.LEN1 && dataByte == 'E') {
            state = parserState.LEN2;
        } else if (state == parserState.LEN2 && dataByte == 'N') {
            state = parserState.LEN3;
        } else if (state == parserState.LEN3 && dataByte == ':') {
            state = parserState.LEN4;
        } else if (state == parserState.LEN4 && (dataByte == ' ' || dataByte == '\t')) {
            state = state;
        } else if ((state == parserState.LEN4 || state == parserState.LEN5) && dataByte >= '0' && dataByte <= '9') {
            state = parserState.LEN5;
            AddPacketLenData(dataByte);
        } else if (state == parserState.LEN5 && dataByte == '\r') {
            state = parserState.LEN6;
        } else if (state == parserState.LEN6 && dataByte == '\n') {
            state = parserState.RecvData;
            SetDataLen();
            if (CurrDataLen == 0){
                CompletePacket(packetProcessor);
            }
        }

        // receiving data
        else if (state == parserState.RecvData && CurrDataRecvd < CurrDataLen) {
            AddPacketData(dataByte);
            if (CurrDataRecvd == CurrDataLen) {
                CompletePacket(packetProcessor);
            }
        }
        else if (state == parserState.RecvData && CurrDataRecvd == CurrDataLen) {
            CompletePacket(packetProcessor);
        }

        // White space is insignificant
        else if (dataByte == ' ' || dataByte == '\t') {
            state = state; // state doesn't change
        }

        // unexpected dataByte in the stream. application should stop processing
        else {
            result = false;
        }

        return result;
    }

    // When application is done with a packet call this function to remove the Packet
    // data structure from memory
    public void DeletePacket(Packet packet) {

    }

    // To send a new packet, start by calling CreatePacket to allocate a new packet.
    // This will create a packet properly formatted per protocol definition. If no data
    // is to be passed then pass NULL for the data parameter and 0 for dataLength
    public PacketBuffer CreatePacketBuffer(String command, byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append("[--\r\n");
        builder.append("PKT.V1 "); builder.append(command); builder.append("\r\n");
        builder.append("LEN: ");
        if (data != null && data.length > 0){
            builder.append(data.length);
            builder.append("\r\n");
        }
        else
        {
            builder.append("0\r\n");
        }

        byte[] header = builder.toString().getBytes(StandardCharsets.UTF_8);
        if (data != null && data.length > 0){
            byte[] result = new byte[header.length + data.length];
            int idx = 0;
            for (int i = 0; i<header.length; i++){
                result[idx++] = header[i];
            }
            for (int i = 0; i<data.length; i++){
                result[idx++] = data[i];
            }
            PacketBuffer packetBuffer = new PacketBuffer();
            packetBuffer.Data = result;
            return packetBuffer;
        }
        else{
            PacketBuffer packetBuffer = new PacketBuffer();
            packetBuffer.Data = header;
            return packetBuffer;
        }
    }

    // Application must delete packet buffers after they are returned
    public void DeletePacketBuffer(PacketBuffer packetBuffer) {

    }
}

