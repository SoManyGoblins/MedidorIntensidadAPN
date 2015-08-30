package com.fing.medidorintensidadapn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView listview;
    private EditText txtCantidad;
    private EditText txtX;
    private EditText txtY;
    private Button btnSave;
    private Button btnScan;
    private Button btnClear;
    private RadioGroup radioGroup;
    private WifiManager wifiManager;
    private MedidaAdapter adapter;
    private ArrayList<Data> medidas = new ArrayList<Data>();
    private int numeroMedida = 0;
    private static final String filePath = "MEDIDAS-APN.txt";
    private BroadcastReceiver receiver_aps;
    private Button btnNuevo;

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiver_aps, filter);

        // for the system's orientation sensor registered listeners
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
//                SensorManager.SENSOR_DELAY_GAME);

        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver_aps);
// to stop the listener and save battery
//        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Trabajando, no se ponga nervioso...");

        listview = (ListView) findViewById(R.id.listview);
        txtCantidad = (EditText) findViewById(R.id.txtCantidad);
        txtX = (EditText) findViewById(R.id.txtX);
        txtY = (EditText) findViewById(R.id.txtY);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnScan = (Button) findViewById(R.id.btnScan);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnNuevo = (Button) findViewById(R.id.btnNuevo);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

//        image = (ImageView) findViewById(R.id.imageViewCompass);


        // initialize your android device sensor capabilities
//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        adapter = new MedidaAdapter();

        listview.setAdapter(adapter);

        btnNuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNuevo();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSave();
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScan();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClear();
            }
        });

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        receiver_aps = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                List<ScanResult> scanResults = wifiManager.getScanResults();

                for (int i = 0; i < scanResults.size(); i++) {
                    ScanResult scanResult = scanResults.get(i);

                    Log.d(TAG, "Scan BSSID: " + scanResult.BSSID);
                    Log.d(TAG, "Scan level: " + scanResult.level);

                    Data triple = new Data(scanResult, (double) scanResult.level);

                    medidas.add(triple);
                }

                int maxMedidas = 10;
                try {
                    maxMedidas = Integer.parseInt(txtCantidad.getText().toString());
                } catch (Exception ex) {
                    Log.e(TAG, "maxMedidas no es un numero: " + txtCantidad.getText().toString());
                }

                Log.d(TAG, "numeroMedida: " + numeroMedida);
                Log.d(TAG, "maxMedidas: " + maxMedidas);

                if (++numeroMedida >= maxMedidas) {
                    Log.d(TAG, "Se llego al limite");

                    List<Data> medidasConPromedio = calcularPromedios();

                    adapter.setData(medidasConPromedio);
                    medidas = new ArrayList<Data>();
                    numeroMedida = 0;

                    progressDialog.dismiss();

                } else {
                    Log.d(TAG, "Solicitando otro scan");

                    wifiManager.startScan();
                }
            }
        };
    }

    private void onNuevo() {
        try {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            String s = externalStorageDirectory.getAbsolutePath() + "/lecturas-apns/";
            File file = new File(s + "lecturas.csv");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy-hh:mm:ss");

            String fecha = simpleDateFormat.format(new Date());

            if (file.exists()) {
                file.renameTo(new File(s + "lecturas-" + fecha + ".csv"));
            }
        } catch (Exception ex) {

        } finally {
            Toast.makeText(this, "Éxito", Toast.LENGTH_SHORT).show();
        }
    }

    private ProgressDialog progressDialog;

    private List<Data> calcularPromedios() {
        ArrayList<Data> result = new ArrayList<Data>();

        HashMap<String, List<Data>> map = new HashMap<String, List<Data>>();
        for (Data medida : medidas) {
            List<Data> list = map.get(medida.result.BSSID);
            if (list == null) {
                list = new ArrayList<Data>();
                map.put(medida.result.BSSID, list);
            }

            list.add(medida);
        }

        for (List<Data> datas : map.values()) {


            Double promedio = 0d;
            for (Data data : datas) {
                promedio += data.promedio;

                Log.d(TAG, "BSSID: " + data.result.BSSID);
                Log.d(TAG, "Valor data: " + data.result.level);
                Log.d(TAG, "Suma: " + promedio);
            }

            promedio = promedio / datas.size();
            Log.d(TAG, "#### Promedio: " + promedio);

            Data data = new Data(datas.get(0).result, promedio);
            result.add(data);
        }

        return result;
    }

    private void onClear() {
        txtX.setText("");
        txtY.setText("");
    }

    private void onScan() {
        wifiManager.startScan();
        progressDialog.show();
    }

    private void onSave() {
        final String x = txtX.getText().toString();
        final String y = txtY.getText().toString();

        if (x.isEmpty() || y.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("X o Y están vacíos")
                    .show();
        } else {

            new AsyncTask<Void, Void, Void>() {
                private Exception exception;
                private ProgressDialog progressDialog;
                private int checkedRadioButtonId;

                @Override
                protected void onPreExecute() {
                    progressDialog = ProgressDialog.show(MainActivity.this, "Guardando", "Guardando datos", true);
                    checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        File externalStorageDirectory = Environment.getExternalStorageDirectory();
                        File dir = new File(externalStorageDirectory.getAbsolutePath() + "/lecturas-apns");
                        dir.mkdirs();
                        File file = new File(dir, "lecturas.csv");

                        boolean exists = file.exists();

                        String direccion = "N";
                        switch (checkedRadioButtonId) {
                            case R.id.radioN:
                                direccion = "N";
                                break;
                            case R.id.radioE:
                                direccion = "E";
                                break;
                            case R.id.radioW:
                                direccion = "W";
                                break;
                            case R.id.radioS:
                                direccion = "S";
                                break;
                        }


                        FileOutputStream stream = null;
                        OutputStreamWriter writer = null;
                        try {
                            stream = new FileOutputStream(file, true);
                            writer = new OutputStreamWriter(stream);

                            if (!exists) {
                                writer.write("SSID,MAC,Lectura Promedio,Direccion,X,Y,Fecha\n");
                            }

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy-hh:mm:ss");

                            List<Data> scans = adapter.getData();
                            for (Data data : scans) {
                                String line = data.result.SSID + "," + data.result.BSSID + "," + data.promedio + "," + direccion +
                                        "," + x + "," + y + "," + simpleDateFormat.format(new Date()) + "\n";
                                writer.write(line);
                            }
                        } catch (Exception ex) {

                        } finally {
                            try {
                                if (writer != null) {
                                    writer.close();
                                }
                            } catch (Exception ex) {
                                // que poronga java por favor.
                            }
                        }


                    } catch (Exception ex) {
                        this.exception = ex;
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    progressDialog.dismiss();

                    if (exception != null) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setMessage("Los datos no se pudieron guardar: " + exception.getMessage())
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, "Los datos fueron guardados con éxito", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MedidaAdapter extends BaseAdapter {

        private List<Data> results =
                new ArrayList<Data>();

        public void setData(List<Data> data) {
            results = data;

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Data getItem(int i) {
            return results.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = getLayoutInflater().inflate(R.layout.result_row, listview, false);
                ViewHolder viewHolder = new ViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            ViewHolder viewHolder = (ViewHolder) rowView.getTag();

            Data item = getItem(i);

            viewHolder.txtSSID.setText(item.result.SSID);
            viewHolder.txtMAC.setText(item.result.BSSID);
            viewHolder.txtLevel.setText(item.promedio.toString() + " dBm");

            return rowView;
        }

        public List<Data> getData() {
            return results;
        }

        private class ViewHolder {
            public final TextView txtSSID;
            public final TextView txtMAC;
            public final TextView txtLevel;

            public ViewHolder(View v) {
                txtSSID = (TextView) v.findViewById(R.id.txtSSID);
                txtMAC = (TextView) v.findViewById(R.id.txtMAC);
                txtLevel = (TextView) v.findViewById(R.id.txtLevel);
            }
        }
    }

//    @Override
//    public void onSensorChanged(SensorEvent event) {
//
//        // get the angle around the z-axis rotated
//        float degree = Math.round(event.values[0]);
//
////        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
//
//        // create a rotation animation (reverse turn degree degrees)
//        RotateAnimation ra = new RotateAnimation(
//                currentDegree,
//                -degree,
//                Animation.RELATIVE_TO_SELF, 0.5f,
//                Animation.RELATIVE_TO_SELF,
//                0.5f);
//
//        // how long the animation will take place
//        ra.setDuration(210);
//
//        // set the animation after the end of the reservation status
//        ra.setFillAfter(true);
//
//        // Start the animation
//        image.startAnimation(ra);
//        currentDegree = -degree;
//
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        // not in use
//    }
//
//    // define the display assembly compass picture
//    private ImageView image;
//
//    // record the compass picture angle turned
//    private float currentDegree = 0f;
//
//    // device sensor manager
//    private SensorManager mSensorManager;
}
