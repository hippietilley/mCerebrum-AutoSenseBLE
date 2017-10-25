package org.md2k.autosenseble.device;
/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import android.util.Log;

class Data {

    static double[] getSequenceNumber(byte[] data) {
        int x=(data[19] & 0x00000000000000ff);
        int y=(data[18] & 0xf);
        int seq=(y<<8)+x;
        return new double[]{seq};
    }
    static double[] getECG(byte[] data) {
        double[] d=new double[4];
        int x=(data[7] & 0x00000000000000ff);
        int y=(data[6] & 0xff);
        int seq=(y<<8)+x;
        d[0]=seq;
        x=(data[9] & 0x00000000000000ff);
        y=(data[8] & 0xff);
        seq=(y<<8)+x;
        d[1]=seq;
        x=(data[11] & 0x00000000000000ff);
        y=(data[10] & 0xff);
        seq=(y<<8)+x;
        d[2]=seq;
        x=(data[16] & 0x00000000000000ff);
        y=(data[15] & 0xff);
        seq=(y<<8)+x;
        d[3]=seq;
        return d;

    }

    static double[] getRespiration(byte[] data) {
        double[] sample = new double[2];
        int x=(data[14] & 0x00000000000000ff);
        int y=(data[13] & 0xf);
        int seq=(y<<8)+x;

        int xo=((data[13] & 0xf0)>>4);
        int yo=(data[12] & 0x00000000000000ff);
        int seqO=(yo<<4)+xo;

        sample[0] = seq;
        sample[1] = seqO;
//        Log.d("abc","s="+sample[0]+" "+sample[1]);
        return sample;
    }

    private static int byteArrayToIntBE(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).getShort();
    }

    private static double convertAccelADCtoSI(double x) {
        return 1.0 * x / 16384;
    }


    static double[] getAccelerometer(byte[] data) {
        double[] sample = new double[3];
        sample[0] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{data[0], data[1]}));
        sample[1] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{data[2], data[3]}));
        sample[2] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{data[4], data[5]}));
        return sample;
    }


    static double[] getRawData(byte[] data){
        double[] sample=new double[data.length];
        for(int i=0;i<data.length;i++)
            sample[i]=data[i];
        return sample;
    }
}