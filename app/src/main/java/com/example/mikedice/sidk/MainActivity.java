package com.example.mikedice.sidk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.mikedice.protocol.IPacketProcessor;
import com.example.mikedice.protocol.Packet;
import com.example.mikedice.protocol.PacketBuffer;
import com.example.mikedice.protocol.Protocol;
import com.example.mikedice.transport.DeviceDisplayInfo;
import com.example.mikedice.transport.Transport;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Transport transport;
    private Protocol protocol;
    private int pingCount;

    private void MarshaledSetStatusLabel(final String message){
        final TextView statusLabel = (TextView) findViewById(R.id.arduinoStatusLabel);
        if (statusLabel != null) {
            statusLabel.post(new Runnable() {
                public void run() {
                    statusLabel.setText(message);
                }
            });
        }
    }

    private void SetTickCount(String message){
        int idx = message.indexOf("Ticks: ");
        String subStr = message.substring(idx + 7);

        MarshaledSetStatusLabel("Message Count from Arduino: " + subStr);
    }

    private void SetAckCount(String message){
        String token = "command count ";
        int idx = message.indexOf(token);
        String subStr = message.substring(idx + token.length());

        MarshaledSetStatusLabel("Commands acknowledged by Arduino: " + subStr);
    }


    View.OnClickListener pingButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String command = "PING";
            String count = String.valueOf(pingCount);

            PacketBuffer packetBuffer = protocol.CreatePacketBuffer(command, count.getBytes(StandardCharsets.UTF_8));
            transport.sendData(packetBuffer.Data);
        }
    };

    private String StringFromPacketData(Packet packet)
    {
        String packetData = null;
        if (packet.Data != null && packet.DataLength > 0)
        {
            packetData = new String(packet.Data, 0, packet.DataLength, StandardCharsets.UTF_8);
        }
        return packetData;
    }

    private void TracePacket(Packet packet)
    {
        String packetData = null;
        if (packet.Data != null && packet.DataLength > 0)
        {
            packetData = new String(packet.Data, 0, packet.DataLength, StandardCharsets.UTF_8);
        }

        Log.d("Main", "================");
        Log.d("Main", packet.Sig);
        Log.d("Main", packet.Command);
        if (packetData != null){
            Log.d("Main", packetData);
        }
        Log.d("Main", "================");
    }

    // The transport produces bytes that are processed by the protocol component
    private UsbSerialInterface.UsbReadCallback transportReadCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] data)
        {
            // as data is received the protocol processor processes data
            // one byte at a time
            for (int i = 0; i<data.length; i++) {
                protocol.ProcessStreamByte(data[i], packetProcessor);
            }
        }
    };

    // The protocol produces packets when they are available from the
    // transport
    IPacketProcessor packetProcessor= new IPacketProcessor() {
        @Override
        public void ProcessPacket(Packet packet) {
            // The application consumes the packets and executes
            // actions based on the commands contained in the packets
            TracePacket(packet);

            if (packet.Command.equals("MEMSTAT")) {
                SetTickCount(StringFromPacketData(packet));
            }
            else if (packet.Command.equals("ACK")){
                SetAckCount(StringFromPacketData(packet));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.protocol = new Protocol();
        this.protocol.ResetParser();

        this.transport = new Transport(transportReadCallback);
        Button pingButton = (Button)findViewById(R.id.pingButton);
        pingButton.setOnClickListener(pingButtonClicked);

        List<DeviceDisplayInfo> devices = transport.StartTransport(this);
        PopulatePicker(devices);
    }

    private void PopulatePicker(List<DeviceDisplayInfo> devices){
        // TODO, eventually populate a nice picker UI. For now just look for an arduino
        for (int i = 0; i<devices.size(); i++){
            if (devices.get(i).ManufacturerName.contains("arduino")){
                this.transport.ConnectDevice(devices.get(i).Key);
                break;
            }
        }
    }
}
