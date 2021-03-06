package com.example.robotica.equilibrista_bluetooth;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
public class MainActivity extends AppCompatActivity {
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    UUID uuid;
    ConnectedThread connectedThread;
    Handler mHandler;
    StringBuilder dadosBluetooth;
    private static final int SOLICITA_ATIVACAO_BLUETOOTH = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int MESSAGE_READ = 3;
    private static String MAC;
    Button buttonConexao;
    ImageView buttonFrente, buttonTraz, buttonEsquerda, buttonDireita, buttonLed;
    boolean conexao;
    int delayRepeticao;
    @SuppressLint({"ClickableViewAccessibility", "HandlerLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        conexao = false;
        delayRepeticao = 20;
        dadosBluetooth = new StringBuilder();
        uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        buttonConexao = (Button) findViewById(R.id.buttonConexao);
        buttonFrente = (ImageView) findViewById(R.id.imageViewCima);
        buttonEsquerda = (ImageView) findViewById(R.id.imageViewEsquerda);
        buttonDireita = (ImageView) findViewById(R.id.imageViewDireita);
        buttonTraz = (ImageView) findViewById(R.id.imageViewBaixo);
        buttonLed = (ImageView) findViewById(R.id.imageViewCentro);
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui bluetooth", Toast.LENGTH_SHORT).show();
        }
        else if (!bluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO_BLUETOOTH);
        }
        buttonConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao) {
                    //desconectar
                    try {
                        bluetoothSocket.close();
                        conexao = false;
                        buttonConexao.setText("Conectar");
                        Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_SHORT).show();
                    }catch (IOException erro) {
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro:" + erro, Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    //conectar
                    Intent abreLista = new Intent(getApplicationContext(), ListaDispositivos.class);
                    startActivityForResult(abreLista, SOLICITA_CONEXAO);
                }
            }
        });

        buttonLed.setOnTouchListener(new View.OnTouchListener() {
            private Rect rect;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (conexao) {
                            connectedThread.enviar("L");
                            Log.d("Comandos", "LED: Acender/Apagar");
                        } else {
                            Toast.makeText(getApplicationContext(), "Bluetooth não conectado", Toast.LENGTH_SHORT).show();
                        }
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                        buttonLed.setColorFilter(Color.argb(50, 0, 0, 0));
                        break;
                    case MotionEvent.ACTION_UP:
                        buttonLed.clearColorFilter();
                    case MotionEvent.ACTION_MOVE:
                        if(!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {
                            buttonLed.clearColorFilter();
                        }
                }
                return true;
            }
        });


        buttonFrente.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonFrente.setColorFilter(Color.argb(50, 0, 0, 0));
                        rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(action, 200);
                        break;
                    case MotionEvent.ACTION_UP:
                        buttonFrente.clearColorFilter();
                        if (handler == null) return true;
                        handler.removeCallbacks(action);
                        handler = null;
                        connectedThread.enviar("C");
                        Log.d("Comandos", "Encerrar Movimentação");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            buttonFrente.clearColorFilter();
                            if (handler == null) return true;
                            handler.removeCallbacks(action);
                            handler = null;
                            connectedThread.enviar("C");
                            Log.d("Comandos", "Encerrar Movimentação");
                        }
                        break;
                }
                return true;
            }
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    if (conexao) {
                        connectedThread.enviar("F");
                        Log.d("Comandos", "Frente");
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Bluetooth não conectado", Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(this, delayRepeticao);
                }
            };
        });

        buttonTraz.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonTraz.setColorFilter(Color.argb(50, 0, 0, 0));
                        rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(action, 200);
                        break;
                    case MotionEvent.ACTION_UP:
                        buttonTraz.clearColorFilter();
                        if (handler == null) return true;
                        handler.removeCallbacks(action);
                        handler = null;
                        connectedThread.enviar("C");
                        Log.d("Comandos", "Encerrar Movimentação");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            buttonTraz.clearColorFilter();
                            if (handler == null) return true;
                            handler.removeCallbacks(action);
                            handler = null;
                            connectedThread.enviar("C");
                            Log.d("Comandos", "Encerrar Movimentação");
                        }
                        break;
                }
                return true;
            }
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    if (conexao) {
                        connectedThread.enviar("T");
                        Log.d("Comandos", "Traz");
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Bluetooth não conectado", Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(this, delayRepeticao);
                }
            };
        });

        buttonDireita.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonDireita.setColorFilter(Color.argb(50, 0, 0, 0));
                        rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(action, 200);
                        break;
                    case MotionEvent.ACTION_UP:
                        buttonDireita.clearColorFilter();
                        if (handler == null) return true;
                        handler.removeCallbacks(action);
                        handler = null;
                        connectedThread.enviar("C");
                        Log.d("Comandos", "Encerrar Movimentação");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            buttonDireita.clearColorFilter();
                            if (handler == null) return true;
                            handler.removeCallbacks(action);
                            handler = null;
                            connectedThread.enviar("C");
                            Log.d("Comandos", "Encerrar Movimentação");
                        }
                        break;
                }
                return true;
            }
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    if (conexao) {
                        connectedThread.enviar("D");
                        Log.d("Comandos", "Direita");
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Bluetooth não conectado", Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(this, delayRepeticao);
                }
            };
        });

        buttonEsquerda.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonEsquerda.setColorFilter(Color.argb(50, 0, 0, 0));
                        rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(action, delayRepeticao);
                        break;
                    case MotionEvent.ACTION_UP:
                        buttonEsquerda.clearColorFilter();
                        if (handler == null) return true;
                        handler.removeCallbacks(action);
                        handler = null;
                        connectedThread.enviar("C");
                        Log.d("Comandos", "Encerrar Movimentação");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            buttonEsquerda.clearColorFilter();
                            if (handler == null) return true;
                            handler.removeCallbacks(action);
                            handler = null;
                            connectedThread.enviar("C");
                            Log.d("Comandos", "Encerrar Movimentação");
                        }
                        break;
                }
                return true;
            }
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    if (conexao) {
                        connectedThread.enviar("E");
                        Log.d("Comandos", "Esquerda");
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Bluetooth não conectado", Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(this, delayRepeticao);
                }
            };
        });
        //Metódo para receber informações do arduino, ainda não implementada!
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String recebidos = (String) msg.obj;
                    dadosBluetooth.append(recebidos);
                    int fimInformacao = dadosBluetooth.indexOf("}");
                    if (fimInformacao > 0) {
                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);
                        int tamanhoInformacao = dadosCompletos.length();
                        if (dadosBluetooth.charAt(0) == '{') {
                            String dadosFinais = dadosBluetooth.substring(1, tamanhoInformacao);
                            Log.d("Recebidos", dadosFinais);
                        }
                        dadosBluetooth.delete(0, dadosBluetooth.length());
                    }
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SOLICITA_ATIVACAO_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "O bluetooth foi ativado", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "O bluetooth não foi ativado, o app será encerrado", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if (resultCode == Activity.RESULT_OK) {
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(getApplicationContext(), "MAC Final: " + MAC, Toast.LENGTH_SHORT).show();
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(MAC);
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                        conexao = true;
                        connectedThread = new ConnectedThread(bluetoothSocket);
                        connectedThread.start();
                        buttonConexao.setText("Desconectar");
                        Toast.makeText(getApplicationContext(), "Conectado ao MAC:" + MAC, Toast.LENGTH_SHORT).show();
                    }catch (IOException erro) {
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro:" + erro, Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Falha ao obter o MAC", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String dadosBluetooth = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBluetooth).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        /* Call this from the main activity to send data to the remote device */
        public void enviar(String dadosEnviar) {
            byte[] mensagemBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(mensagemBuffer);
            } catch (IOException e) { }
        }
    }
}






