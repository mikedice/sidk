package com.example.mikedice.protocol;

/**
 * Created by mike on 12/3/17.
 */

// Application callback function that gets called once a packet becomes
// ready for the application to process
public interface IPacketProcessor {
    void ProcessPacket(Packet packet);
}
