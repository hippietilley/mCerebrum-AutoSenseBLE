package org.md2k.autosenseble.device.autosenseble;

import org.md2k.autosenseble.device.Sensor;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.time.DateTime;

import java.util.ArrayList;

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
public class ECG  {
    private int lastSeq;
    private double[] ecgBuffer = new double[16];
    private long startTime;
    //TODO datasource fix

//    ECG(DataSource dataSource) {
//
//          super(dataSource);
//        for (int i = 0; i < ecgBuffer.length; i++)
//            ecgBuffer[i] = -1;
//    }

    public ArrayList<DataTypeDoubleArray> pushOld(int curSeq) {
        if (curSeq - lastSeq > 4) {
            lastSeq=curSeq-1;
            return insertData();
        } else return null;
    }

    public ArrayList<DataTypeDoubleArray> add(int curSeq, double[] ecgSample) {
        switch (curSeq % 4) {
            case 0:
                ecgBuffer[1 - 1] = ecgSample[0];
                ecgBuffer[5 - 1] = ecgSample[1];
                ecgBuffer[9 - 1] = ecgSample[2];
                ecgBuffer[13 - 1] = ecgSample[3];
                startTime=DateTime.getDateTime();
                break;
            case 1:
                ecgBuffer[2 - 1] = ecgSample[0];
                ecgBuffer[6 - 1] = ecgSample[1];
                ecgBuffer[10 - 1] = ecgSample[2];
                ecgBuffer[14 - 1] = ecgSample[3];
                startTime=DateTime.getDateTime()-40;
                break;
            case 2:
                ecgBuffer[3 - 1] = ecgSample[0];
                ecgBuffer[7 - 1] = ecgSample[1];
                ecgBuffer[11 - 1] = ecgSample[2];
                ecgBuffer[15 - 1] = ecgSample[3];
                startTime=DateTime.getDateTime()-80;
                break;
            case 3:
                ecgBuffer[4 - 1] = ecgSample[0];
                ecgBuffer[8 - 1] = ecgSample[1];
                ecgBuffer[12 - 1] = ecgSample[2];
                ecgBuffer[16 - 1] = ecgSample[3];
                startTime=DateTime.getDateTime()-120;
                break;
        }
        if (curSeq % 4 == 3) {
            lastSeq=curSeq;
            return insertData();
        } else return null;
    }

    private ArrayList<DataTypeDoubleArray> insertData() {
        ArrayList<DataTypeDoubleArray> dataTypeDoubleArrays = new ArrayList<>();
        long curTime = DateTime.getDateTime();
        long endTime = startTime + 160;
        if (endTime > curTime)
            endTime = curTime;
        for (int i = 0; i < ecgBuffer.length; i++) {
            if (ecgBuffer[i] != -1) {
                curTime = startTime + i * (endTime - startTime) / 16;
                DataTypeDoubleArray db = new DataTypeDoubleArray(curTime, new double[]{ecgBuffer[i]});
            //TODO insert ECG
                //    insert(db);
                dataTypeDoubleArrays.add(db);
                ecgBuffer[i] = -1;
            }
        }
        return dataTypeDoubleArrays;
    }
}
