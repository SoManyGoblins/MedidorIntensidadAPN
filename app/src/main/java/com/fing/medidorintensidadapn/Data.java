package com.fing.medidorintensidadapn;

import android.net.wifi.ScanResult;

/**
 * Created by Matias on 24-Aug-14.
 */
public class Data {
    public final ScanResult result;
    public final Double promedio;

    public Data(ScanResult result, Double promedio) {
        this.result = result;
        this.promedio = promedio;
    }
}
