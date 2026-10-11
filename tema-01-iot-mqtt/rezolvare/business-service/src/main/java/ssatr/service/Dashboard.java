package ssatr.service;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ultima stare cunoscută a fiecărui device: ultima temperatură, momentul
 * ultimului mesaj, numărul de alarme etc.
 *
 * Este actualizată de thread-urile MessageProcessor și citită de CommandConsole,
 * deci accesul trebuie sincronizat.
 */
public class Dashboard {

    // TODO 5: alegeți structura de date pentru starea device-urilor
    //   (de exemplu un Map<String, ...> cu cheia deviceId)
    //   și adăugați metodele de actualizare apelate din MessageProcessor.

    /** Starea unui device, așa cum reiese din ultimele mesaje primite. */
    private static class DeviceState {
        double temperature;
        long lastMessageTime;  // momentul primirii, în ms, pe ceasul serviciului
    }

    // cheia este deviceId; TreeMap ține device-urile ordonate după nume în tabel.
    // Map-ul nu este thread-safe: orice acces trece prin metodele synchronized de mai jos.
    private final Map<String, DeviceState> devices = new TreeMap<>();

    /**
     * Apelată de MessageProcessor (thread-ul "telemetry") pentru fiecare
     * mesaj de telemetrie valid.
     *
     * synchronized: print() citește aceleași date de pe thread-ul consolei,
     * deci nu poate vedea o stare actualizată pe jumătate.
     */
    public synchronized void updateTelemetry(String deviceId, double temperature) {
        // primul mesaj de la un device nou creează intrarea lui
        DeviceState state = devices.computeIfAbsent(deviceId, id -> new DeviceState());
        state.temperature = temperature;
        state.lastMessageTime = System.currentTimeMillis();
    }

    /** Afișează un tabel cu starea tuturor device-urilor. */
    public synchronized void print() {
        // TODO 5: afișați câte un rând pentru fiecare device, de exemplu:
        //   DEVICE     TEMP    ULTIMUL MESAJ   ALARME
        //   device-1   23.41   acum 1 s        2

        // rezolvare parțială: coloana ALARME se adaugă odată cu tratarea alarmelor
        if (devices.isEmpty()) {
            System.out.println("(niciun device nu a trimis telemetrie)");
            return;
        }

        long now = System.currentTimeMillis();
        System.out.printf("%-12s %7s   %s%n", "DEVICE", "TEMP", "ULTIMUL MESAJ");
        for (Map.Entry<String, DeviceState> entry : devices.entrySet()) {
            DeviceState state = entry.getValue();
            long secondsAgo = (now - state.lastMessageTime) / 1000;
            System.out.printf(Locale.US, "%-12s %7.2f   acum %d s%n", entry.getKey(), state.temperature, secondsAgo);
        }
    }
}
