package ssatr.service;

import java.util.concurrent.BlockingQueue;

/**
 * Preia mesajele de un anumit tip ("telemetry", "alarm" sau "status") din coada
 * inbox, le salvează și actualizează dashboard-ul.
 *
 * Formatul mesajelor (câmpuri separate prin ";"):
 *   telemetry:  deviceId;timestamp;temperatura
 *   alarm:      deviceId;timestamp;temperatura;prag
 *   status:     deviceId;timestamp;text
 */
public class MessageProcessor implements Runnable {

    private final String kind;
    private final BlockingQueue<String> inbox;
    private final CsvStorage storage;
    private final Dashboard dashboard;

    public MessageProcessor(String kind, BlockingQueue<String> inbox, CsvStorage storage, Dashboard dashboard) {
        this.kind = kind;
        this.inbox = inbox;
        this.storage = storage;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        // TODO 4: într-o buclă:
        //   - preluați un mesaj: inbox.take()
        //   - salvați mesajul în fișierul CSV: storage.append(kind, mesaj)
        //     (CsvStorage este gata implementată)
        //   - actualizați dashboard-ul, în funcție de kind
        //   - alarmele și confirmările se afișează imediat în consolă
        //   - un mesaj greșit (format invalid) nu trebuie să oprească thread-ul

        while (true) {
            String message;
            try {
                // take() blochează thread-ul până sosește un mesaj;
                // cât timp coada e goală, thread-ul nu consumă procesor
                message = inbox.take();
            } catch (InterruptedException e) {
                return;  // thread-ul a fost întrerupt: oprim procesarea
            }

            try {
                switch (kind) {
                    case "telemetry":
                        handleTelemetry(message);
                        break;
                    default:
                        // rezolvare parțială: alarm și status nu sunt încă tratate
                        break;
                }
            } catch (IllegalArgumentException e) {
                // NumberFormatException este tot o IllegalArgumentException.
                // Mesajul greșit se raportează și se sare peste el; thread-ul
                // continuă cu următorul mesaj din coadă.
                System.out.println("mesaj " + kind + " invalid, ignorat: \"" + message + "\" (" + e.getMessage() + ")");
            }
        }
    }

    /** telemetry: deviceId;timestamp;temperatura */
    private void handleTelemetry(String message) {
        String[] fields = message.split(";");
        if (fields.length != 3) {
            throw new IllegalArgumentException("se așteaptă 3 câmpuri, sunt " + fields.length);
        }

        String deviceId = fields[0];
        if (deviceId.isEmpty()) {
            throw new IllegalArgumentException("deviceId lipsește");
        }
        // timestamp-ul se verifică doar ca format: dashboard-ul folosește momentul
        // primirii, pe ceasul serviciului, nu ceasul device-ului
        Long.parseLong(fields[1]);
        double temperature = Double.parseDouble(fields[2]);

        // mesajul se validează înainte de salvare, ca în CSV să ajungă doar
        // linii corecte, scrise exact cum au sosit
        storage.append(kind, message);
        dashboard.updateTelemetry(deviceId, temperature);

        // telemetria nu se afișează la fiecare mesaj (ar umple consola);
        // ultima valoare a fiecărui device se vede cu comanda "show"
    }
}
