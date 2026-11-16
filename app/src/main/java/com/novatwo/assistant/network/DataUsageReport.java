
package com.novatwo.assistant.network;

import android.content.Context;

public final class DataUsageReport {

    private DataUsageReport() {
    }

    public static String generate(Context context) {

        long sent = DataUsageMonitor.getSentBytes(context);
        long received = DataUsageMonitor.getReceivedBytes(context);
        long total = sent + received;

        StringBuilder report = new StringBuilder();

        report.append("RAPPORTO CONSUMO NOVA 2.0\n\n");

        report.append("Connessione: ")
                .append(NetworkInfo.getConnectionType(context))
                .append("\n");

        report.append("Richieste AI: ")
                .append(DataUsageMonitor.getRequestCount(context))
                .append("\n");

        report.append("Dati inviati: ")
                .append(DataUsageFormatter.formatBytes(sent))
                .append("\n");

        report.append("Dati ricevuti: ")
                .append(DataUsageFormatter.formatBytes(received))
                .append("\n");

        report.append("Consumo totale: ")
                .append(DataUsageFormatter.formatBytes(total))
                .append("\n");

        if (DataUsageLimit.isLimitEnabled(context)) {
            report.append("Limite impostato: ")
                    .append(DataUsageLimit.getLimitMB(context))
                    .append(" MB\n");

            report.append("Limite raggiunto: ")
                    .append(DataUsageLimit.isLimitReached(context)
                            ? "Sì" : "No");
        } else {
            report.append("Limite consumo: Disattivato");
        }

        return report.toString();
    }
}
