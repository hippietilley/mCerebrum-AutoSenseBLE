package org.md2k.autosenseble.device.sensor;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.autosenseble.MyApplication;
import org.md2k.autosenseble.ServiceAutoSense;
import org.md2k.autosenseble.data_quality.ECGQualityCalculation;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.mcerebrum.core.data_format.DATA_QUALITY;

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
public class DataQualityECG extends Sensor{
    private ECGQualityCalculation ecgQualityCalculation;

    private ArrayList<Integer> samples;

    public DataQualityECG(DataSource dataSource) {
        super(dataSource);
        samples = new ArrayList<>();
        ecgQualityCalculation = new ECGQualityCalculation();
    }
    public synchronized int getStatus() {
        try {
            int size = samples.size();
            int samps[] = new int[size];
            for (int i = 0; i < size; i++)
                samps[i] = samples.get(i);
            samples.clear();
            return ecgQualityCalculation.currentQuality(samps);
        } catch (Exception e) {
            return DATA_QUALITY.GOOD;
        }
    }

    public synchronized void add(double sample) {

        samples.add((int) sample);
    }

    public void insert(DataTypeInt dataTypeInt){
        int[] intArray=new int[7];
        for(int i=0;i<7;i++) intArray[i]=0;
        int value=dataTypeInt.getSample();
        intArray[value]=3000;
        try {
            DataKitAPI.getInstance(MyApplication.getContext()).insert(dataSourceClient, dataTypeInt);
            DataKitAPI.getInstance(MyApplication.getContext()).setSummary(dataSourceClient, new DataTypeIntArray(dataTypeInt.getDateTime(), intArray));
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(new Intent(ServiceAutoSense.INTENT_STOP));
        }
    }
}