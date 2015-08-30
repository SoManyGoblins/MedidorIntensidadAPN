package com.fing.medidorintensidadapn;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matias on 24-Aug-14.
 */
public class Data {
    public final List<ScanResult> results;
    public final Double promedio;

    public Data(List<ScanResult> results, Double promedio) {
        this.results = results;
        this.promedio = promedio;
    }
}
