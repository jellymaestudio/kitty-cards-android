package kittycards.kittycardsandroid.uiAusprobieren;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kittycards.kittycardsandroid.R;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private TextView statusTextView;
    private TextView dataLogTextView;
    private TextView errorTextView;
    @Inject
    INetworkManager networkManager;


    // Listen für die Bluetooth-Geräte im Umkreis
    private final List<NetworkDevice> discoveredDevices = new ArrayList<>();
    private NetworkDevice targetDevice = null;
    private void showError(String source, String message) {
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText("[" + source + "] " + message);
        dataLogTextView.append("\n❌ [FEHLER][" + source + "] " + message);
    }
    @SuppressLint("MissingPermission") // Schaltet die Warnung/den Fehler für diese Methode aus
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_ausprobieren);

        // Da minSdk = 31, fragen wir einfach direkt und ohne Weichen nach den neuen Rechten
        requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
        }, 101);

        // Ab hier folgt dein restlicher UI- und NetworkManager-Code...

        statusTextView = findViewById(R.id.tv_status);
        dataLogTextView = findViewById(R.id.tv_data_log);
        errorTextView = findViewById(R.id.tv_error);

        Button btnHost = findViewById(R.id.btn_host);
        Button btnJoin = findViewById(R.id.btn_join);
        Button btnConfirm = findViewById(R.id.btn_confirm);
        Button btnSend = findViewById(R.id.btn_send);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);

        // Zentrales Event-Logging
        networkManager.setNetworkEventListener(event -> runOnUiThread(() -> {
            String prefix = "[" + event.source() + "] ";
            switch (event.type()) {
                case INFO -> dataLogTextView.append("\nℹ️ " + prefix + event.message());
                case WARNING -> dataLogTextView.append("\n⚠️ " + prefix + event.message());
                case ERROR -> showError(event.source(), event.message());
            }
        }));

        // 1. MATCH HOSTEN
        btnHost.setOnClickListener(v -> {
            errorTextView.setVisibility(View.GONE);
            String deviceName = BluetoothAdapter.getDefaultAdapter().getName();
//            statusTextView.setText("Rolle: HOST (Warte auf Gäste...)");
            //           dataLogTextView.append("\n[System] Starte Hosting als: " + deviceName);
            btnConfirm.setEnabled(false); // Erstmal deaktivieren, bis jemand kommt

            networkManager.hostMatch(guests -> runOnUiThread(() -> {
                discoveredDevices.clear();
                discoveredDevices.addAll(guests);
                if (!guests.isEmpty()) {
                    targetDevice = guests.get(0);
                    //                   dataLogTextView.append("\n[Scanner] Gast gefunden: " + targetDevice.deviceAddress());
//                    statusTextView.setText("Gast verfügbar! Bitte 'Bestätigen' klicken.");
                    btnConfirm.setEnabled(true); // <-- HIER AKTIVIERT: Gast gefunden!
                }
            }));
        });

        // 2. NACH MATCHES SUCHEN (GUEST)
        btnJoin.setOnClickListener(v -> {
            errorTextView.setVisibility(View.GONE);
            //           statusTextView.setText("Rolle: GUEST (Suche nach Räumen...)");
            //           dataLogTextView.append("\n[System] Starte BLE-Scan nach Räumen...");
            btnConfirm.setEnabled(false); // Erstmal deaktivieren, bis gescannt wurde

            networkManager.joinMatch(rooms -> runOnUiThread(() -> {
                discoveredDevices.clear();
                discoveredDevices.addAll(rooms);
                if (!rooms.isEmpty()) {
                    targetDevice = rooms.get(0);
                    //               dataLogTextView.append("\n[Scanner] Raum gefunden: " + targetDevice.deviceAddress());
                    //                statusTextView.setText("Raum gefunden! Bitte 'Bestätigen' klicken.");
                    btnConfirm.setEnabled(true); // <-- HIER AKTIVIERT: Raum gefunden!
                }
            }));
        });

        // 3. VERBINDUNG BESTÄTIGEN ODER GAST AUSWÄHLEN
        btnConfirm.setOnClickListener(v -> {
            if (targetDevice == null) {
                //            statusTextView.setText("Fehler: Kein Gerät in Reichweite!");
                return;
            }

            switch (networkManager.getRole()) {
                case GUEST -> {
                    //                 statusTextView.setText("Verbinde mit: " + targetDevice.deviceAddress());
                    //dataLogTextView.append("\n[System] Sende Verbindungsanfrage an " + targetDevice.deviceAddress());
                    networkManager.confirmRoom(targetDevice);
                }
                case HOST -> {
                    //                 statusTextView.setText("Akzeptiere Gast: " + targetDevice.deviceAddress());
                    //dataLogTextView.append("\n[System] Wähle Gast aus und schließe Lobby für andere: " + targetDevice.deviceAddress());
                    networkManager.selectGuest(targetDevice);
                }
                case NOT_CONNECTED -> {
                    //                  statusTextView.setText("Fehler: Keine Rolle zugewiesen!");
                }
            }
        });

        // 4. TEST-DATEN SENDEN
        btnSend.setOnClickListener(v -> {
            try {
                GameAction dummyAction = new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        null, null, -1, -1, 1
                );
                networkManager.sendGameChange(dummyAction);
                //dataLogTextView.append("\n📤 [Gesendet] Typ: " + dummyAction.type() + " | ContextInt: " + dummyAction.contextSensitiveInt());
            } catch (Exception e) {
                showError("MainActivity", "Sende-Fehler: " + e.getMessage());
            }
        });

        // 5. TRENNEN
        btnDisconnect.setOnClickListener(v -> {
            networkManager.disconnect();
            targetDevice = null;
            discoveredDevices.clear();
            errorTextView.setVisibility(View.GONE);
            //       statusTextView.setText("Status: Getrennt");
            //dataLogTextView.append("\n🔌 [System] Verbindung manuell getrennt.");
            btnConfirm.setEnabled(false); // <-- DEAKTIVIERT: Nach Trennung zurücksetzen
        });

        startIncomingDataListener();
    }

    private void startIncomingDataListener() {
        new Thread(() -> {
            while (!isFinishing()) {
                try {
                    // Blockiert, bis Daten über BLE eingehen
                    GameAction action = networkManager.fetchNextAction();
                    if (action != null) {
                                     runOnUiThread(() -> dataLogTextView.append(
                                                    "\n📥 [Empfangen] Typ: " + action.type() + " | ContextInt: " + action.contextSensitiveInt()
                                       ));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    //           runOnUiThread(() -> dataLogTextView.append("\n❌ [Fehler beim Lesen]: " + e.getMessage()));
                }
            }
        }).start();
    }
}