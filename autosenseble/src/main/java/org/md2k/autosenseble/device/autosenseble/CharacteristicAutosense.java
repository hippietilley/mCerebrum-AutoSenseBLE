package org.md2k.autosenseble.device.autosenseble;
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

import com.polidea.rxandroidble.RxBleConnection;

import org.md2k.autosenseble.Data;
import org.md2k.autosenseble.device.Characteristic;
import org.md2k.autosenseble.device.Sensor;
import org.md2k.autosenseble.device.TranslateAutosense;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.source.datasource.DataSourceType;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import rx.Observable;

public class CharacteristicAutosense extends Characteristic {
    private HashMap<String, Sensor> listSensor;

    CharacteristicAutosense() {
        super("da39c921-1d81-48e2-9c68-d0ae4bbd351f", "CHARACTERISTIC_ACCELEROMETER", 25.0);
        //TODO: fix frequency
    }

    public Observable<Data> getObservable(RxBleConnection rxBleConnection, ArrayList<Sensor> sensors) {
        prepareList(sensors);
        return setNotify(rxBleConnection);
    }

    private void prepareList(ArrayList<Sensor> sensors) {
        listSensor = new HashMap<>();
        for (Sensor sensor : sensors) {
            String t = sensor.getDataSourceType();
            if (sensor.getDataSourceId() != null)
                t += sensor.getDataSourceId();
            listSensor.put(t, sensor);
        }
    }

    private Observable<Data> setNotify(RxBleConnection rxBleConnection) {
        UUID uuid = UUID.fromString(getId());
        return rxBleConnection.setupNotification(uuid)
                .flatMap(notificationObservable -> notificationObservable)
                .map(bytes -> {
                    ArrayList<Data> data = new ArrayList<>();

                    int curSeq = (int) TranslateAutosense.getSequenceNumber(bytes)[0];
                    long curTime = correctTimeStamp(curSeq,65636);


                    if (listSensor.containsKey(DataSourceType.ACCELEROMETER + getName())) {
                        DataType d = new DataTypeDoubleArray(curTime, TranslateAutosense.getAccelerometer(bytes));

                        data.add(new Data(listSensor.get(DataSourceType.ACCELEROMETER + getName()), d));
                    }

                    if (listSensor.containsKey(DataSourceType.RESPIRATION + getName())) {
                       DataType  d = new DataTypeDoubleArray(curTime, TranslateAutosense.getRespirationBase(bytes));
                        data.add(new Data(listSensor.get(DataSourceType.RESPIRATION + getName()), d));
                    }

                    if (listSensor.containsKey(DataSourceType.ECG + getName())) {
                        DataType d = new DataTypeDoubleArray(curTime, TranslateAutosense.getECG(bytes));
                        data.add(new Data(listSensor.get(DataSourceType.ECG + getName()), d));
                    }


                    if (listSensor.containsKey(DataSourceType.SEQUENCE_NUMBER + getName())) {
                        DataType d = new DataTypeDoubleArray(curTime, TranslateAutosense.getSequenceNumber(bytes));
                        data.add(new Data(listSensor.get(DataSourceType.SEQUENCE_NUMBER + getName()), d));
                    }

                    if (listSensor.containsKey(DataSourceType.RAW + getName())) {
                        DataType d = new DataTypeDoubleArray(curTime, TranslateAutosense.getRawData(bytes));
                        data.add(new Data(listSensor.get(DataSourceType.RAW + getName()), d));
                    }
                    lastSequence = curSeq;
                    lastTimestamp = curTime;
                    return data;
                }).flatMap(Observable::from);
    }
}
