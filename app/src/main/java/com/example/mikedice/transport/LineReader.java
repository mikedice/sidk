package com.example.mikedice.transport;

import android.util.Log;

import java.nio.charset.StandardCharsets;

/**
 * Created by mikedice on 12/14/17.
 */

public class LineReader{
    private byte[] buffer;
    private int currIndex;
    enum State {
        RECV_DATA,
        RECV_CR,
        RECV_LF
    };
    private State state;

    public LineReader(){
        this.state = State.RECV_DATA;
        this.buffer = new byte[1024];
        this.currIndex = 0;
    }

    public void AddBytes(byte[] data, int length)
    {
        for (int i = 0; i<length; i++) {
            if (data[i]<0 || data[i]>255){
                continue;
            }
            else if (data[i] == '\r' && state == State.RECV_DATA) {
                state = State.RECV_CR;
            } else if (data[i] == '\n' && state == State.RECV_CR) {
                state = State.RECV_LF;
                // write string and clear buffer
                String s = new String(buffer, 0, currIndex, StandardCharsets.US_ASCII);
                Log.d("Transport", s);
                currIndex = 0;
                state = State.RECV_DATA;
            } else {
                state = State.RECV_DATA;
                buffer[currIndex++] = data[i];
            }
        }
    }
}

