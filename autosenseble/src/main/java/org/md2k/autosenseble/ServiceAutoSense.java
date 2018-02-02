package org.md2k.autosenseble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;

import org.md2k.autosenseble.configuration.ConfigurationManager;
import org.md2k.autosenseble.data_quality.DataQualityManager;
import org.md2k.autosenseble.datakit.DataKitManager;
import org.md2k.autosenseble.device.DeviceManager;
import org.md2k.autosenseble.device.Sensor;
import org.md2k.autosenseble.error.ErrorNotify;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.mcerebrum.commons.permission.Permission;



import java.util.ArrayList;

import rx.BackpressureOverflow;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.md2k.autosenseble.ActivitySettings.ACTION_LOCATION_CHANGED;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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

public class ServiceAutoSense extends Service {  public static final String INTENT_DATA = "INTENT_DATA";
    private DataKitManager dataKitManager;
    DeviceManager deviceManager;
    Subscription subscription;
    SparseArray<Summary> summary;
    DataQualityManager dataQualityManager;



    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("abc","Service: onCreate()...");
        summary=new SparseArray<>();
        ErrorNotify.removeNotification(ServiceAutoSense.this);
        loadListener();

        subscription = Observable.just(true)
                .map(aBoolean -> {
                    Log.e("abc","permission");
                    boolean res = Permission.hasPermission(ServiceAutoSense.this);
                    if (!res) ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.PERMISSION);
                    return res;
                }).filter(x -> x)
                .map(aBoolean -> {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    boolean res = mBluetoothAdapter.isEnabled();
                    if (!res) ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.BLUETOOTH_OFF);
                    return res;
                }).filter(x -> x)
                .map(aBoolean -> {
                    LocationManager locationManager = (LocationManager) ServiceAutoSense.this.getSystemService(LOCATION_SERVICE);
                    boolean res = (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                    if (!res) ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.GPS_OFF);
                    return res;
                }).filter(x -> x)
                .map(aBoolean -> {
                    ArrayList<DataSource> dataSources = ConfigurationManager.read(ServiceAutoSense.this);
                    if (dataSources == null || dataSources.size() == 0) {
                        ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.NOT_CONFIGURED);
                        return false;
                    }
                    return true;
                }).filter(x -> x)
                .flatMap(aBoolean -> {
                    dataKitManager = new DataKitManager();
                    return dataKitManager.connect(ServiceAutoSense.this).map(res -> {
                        if (!res) ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.DATAKIT_CONNECTION_ERROR);
                        return res;
                    });
                }).doOnUnsubscribe(() -> {
                    if(dataKitManager!=null)
                        dataKitManager.disconnect();
                }).filter(x -> x)
                .map(aBoolean -> {
                    ArrayList<DataSource> dataSources = ConfigurationManager.read(ServiceAutoSense.this);
                    deviceManager = new DeviceManager();
                    dataQualityManager=new DataQualityManager();
                    if (dataSources == null || dataSources.size() == 0) return false;
                    for (int i = 0; i < dataSources.size(); i++) {
                        DataSourceClient dataSourceClient = dataKitManager.register(dataSources.get(i));
                        if (dataSourceClient == null) {
                            ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.DATAKIT_REGISTRATION_ERROR);
                            return false;
                        }

                        Sensor sensor = new Sensor(dataSourceClient,
                                dataSources.get(i).getPlatform().getType(),
                                dataSources.get(i).getPlatform().getMetadata().get(METADATA.DEVICE_ID),
                                dataSources.get(i).getMetadata().get("CHARACTERISTIC_NAME"),
                                dataSources.get(i).getType(),
                                dataSources.get(i).getId());
                        if(dataSources.get(i).getType().equals(DataSourceType.DATA_QUALITY)){
                            dataQualityManager.addSensor(sensor);
                        }else{
                            deviceManager.add(sensor);
                        }
                    }
                    return true;
                }).filter(x -> x)
                .flatMap(new Func1<Boolean, Observable<Data>>() {
                    @Override
                    public Observable<Data> call(Boolean aBoolean) {
                        //TODO: activate
                        return Observable.just(null);
          //              return Observable.merge(deviceManager.connect(ServiceAutoSense.this), dataQualityManager.getObservable());
                    }
                })
                .doOnUnsubscribe(() -> {
                    if(deviceManager!=null)
                        deviceManager.disconnect();
                })
                .onBackpressureBuffer(1024, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST)
                .retryWhen(errors -> errors.flatMap((Func1<Throwable, Observable<?>>) throwable -> {
                    Log.e("abc", "Service: error=" + throwable.toString());
//                    if(throwable instanceof rx.exceptions.MissingBackpressureException || throwable instanceof CompositeException) {
                    if(deviceManager!=null)
                        deviceManager.disconnect();
                    return Observable.just(null);
                    //                  }else return Observable.error(throwable);
                }))
                .observeOn(Schedulers.newThread())
                .subscribe(new Observer<Data>() {
                    @Override
                    public void onCompleted() {
                        Log.e("abc","Service -> onCompleted()");
                        Intent intent = new Intent(ServiceAutoSense.this, ActivityError.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("error", "service: onCompleted");
                        ServiceAutoSense.this.startActivity(intent);
                        unsubscribe();
                        stopSelf();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Intent intent = new Intent(ServiceAutoSense.this, ActivityError.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("error", "service:" +e.toString());
                        ServiceAutoSense.this.startActivity(intent);
                        Log.e("abc","Service onError()... e="+e.toString()+" ");
                        unsubscribe();
                        stopSelf();
                    }

                    @Override
                    public void onNext(Data data) {
                        dataKitManager.insert(data.getSensor().getDataSourceClient(), data.getDataType());
                        if(data.getSensor().getDataSourceType().equals(DataSourceType.DATA_QUALITY))
                            dataKitManager.setSummary(data.getSensor().getDataSourceClient(), dataQualityManager.getSummary(data));
                        else
                            dataQualityManager.addData(data);

                        Intent intent=new Intent(INTENT_DATA);
                        Summary s = summary.get(data.getSensor().getDataSourceClient().getDs_id());
                        if(s==null){
                            s=new Summary();
                            summary.put(data.getSensor().getDataSourceClient().getDs_id(), s);
                        }
                        s.set();
                        intent.putExtra(DataSource.class.getSimpleName(), data.getSensor().getDataSourceClient().getDataSource());
                        intent.putExtra(DataType.class.getSimpleName(), data.getDataType());
                        intent.putExtra(Summary.class.getSimpleName(), s);
                        LocalBroadcastManager.getInstance(ServiceAutoSense.this).sendBroadcast(intent);
                    }
                });
    }

    void loadListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_LOCATION_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unsubscribe();
        try {
            unregisterReceiver(mReceiver);
        }catch (Exception ignored){}
        Log.e("abc","Service: onDestroy()...");
        super.onDestroy();
    }
    void unsubscribe(){
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
        subscription=null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action!=null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.BLUETOOTH_OFF);
                        unsubscribe();
                        stopSelf();
                        break;
                }
            } else if (action!=null && action.equals(ACTION_LOCATION_CHANGED)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
                if (locationManager!=null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    ErrorNotify.handle(ServiceAutoSense.this, ErrorNotify.GPS_OFF);
                    unsubscribe();
                    stopSelf();
                }
            }
        }
    };
}

