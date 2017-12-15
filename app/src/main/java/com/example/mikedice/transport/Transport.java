package com.example.mikedice.transport;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.deviceids.CH34xIds;
import com.felhr.deviceids.CP210xIds;
import com.felhr.deviceids.CP2130Ids;
import com.felhr.deviceids.FTDISioIds;
import com.felhr.deviceids.PL2303Ids;
import com.felhr.deviceids.XdcVcpIds;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mikedice on 12/14/17.
 */

public class Transport {
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbManager usbManager;
    private ArrayList<DeviceDisplayInfo> deviceDisplayInfos;
    private HashMap<String, UsbDevice> devices;
    private PendingIntent permissionBroadcastReceiver;
    private Context context;
    private LineReader lineReader;

    public List<DeviceDisplayInfo> StartTransport(Context context)
    {
        UsbManager usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        this.usbManager = usbManager;
        this.lineReader = new LineReader();
        this.deviceDisplayInfos = new ArrayList<DeviceDisplayInfo>();

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        this.devices = usbDevices;
        for (HashMap.Entry<String, UsbDevice> entry : usbDevices.entrySet()){

            DeviceDisplayInfo device = new DeviceDisplayInfo();
            device.Key = entry.getKey();
            UsbDevice value = (UsbDevice)entry.getValue();

            device.ManufacturerName = value.getManufacturerName();
            device.ProductName = value.getProductName();
            device.SystemName = value.getDeviceName();
            this.deviceDisplayInfos.add(device);
        }

        // set up the USB permissions handler
        this.permissionBroadcastReceiver = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
        this.context = context;

        return this.deviceDisplayInfos;
    }

    public void ConnectDevice(String key){
        if (this.devices.containsKey(key)){
            this.usbManager.requestPermission(this.devices.get(key), this.permissionBroadcastReceiver);
        }
    }

    public void DevicePermissionGranted(UsbDevice device){
        UsbDeviceConnection usbConnection = usbManager.openDevice(device);

        UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);

        int vendorId = device.getVendorId();
        int deviceClass = device.getDeviceClass();
        int productId = device.getProductId();
        boolean isSupported = CH34xIds.isDeviceSupported(vendorId, productId);
        isSupported = CP210xIds.isDeviceSupported(vendorId, productId);
        isSupported = CP2130Ids.isDeviceSupported(vendorId, productId);
        isSupported = FTDISioIds.isDeviceSupported(vendorId, productId);
        isSupported = PL2303Ids.isDeviceSupported(vendorId, productId);
        isSupported = XdcVcpIds.isDeviceSupported(vendorId, productId);
        isSupported = UsbSerialDevice.isCdcDevice(device);

        serial.open();
        serial.setBaudRate(57600);
        serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serial.setParity(UsbSerialInterface.PARITY_NONE);
        serial.setStopBits(1);

        serial.read(this.mReadCallback);
    }

    private UsbSerialInterface.UsbReadCallback mReadCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] data)
        {
            lineReader.AddBytes(data, data.length);
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication

                            Log.d("Transpport", "permission granted for device " + device);
                            DevicePermissionGranted(device);
                        }
                    }
                    else {
                        Log.d("Transpport", "permission denied for device " + device);
                    }
                }
            }
        }
    };
}
