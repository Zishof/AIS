package ais.common.newui.laporan;

import java.util.Date;
import java.util.Map;

import org.json.JSONObject;

import ais.common.Common;

/**
 * Penyaji laporan Jasper sebagai PDF ber-base64 di dalam amplop JSON.
 *
 * <p>Tiga kontrak laporan native — laporan generik, laporan kinerja BKD/LKP,
 * dan laporan BKD ringkas/peringkat — sama-sama berakhir pada langkah yang
 * persis sama: render template, pastikan berkasnya jadi, baca isinya, lalu
 * kirim sebagai base64 beserta nama berkas bertanggal. Langkah itu semula
 * disalin di tiap kontrak. Menyalinnya lagi untuk kontrak keempat berarti
 * empat tempat yang harus disunting begitu cara pengirimannya berubah
 * (misalnya bila kelak berkas besar dialirkan alih-alih di-base64), dan tiga
 * di antaranya pasti terlewat.</p>
 */
public final class JasperPdfUtil {

    private JasperPdfUtil() { }

    /**
     * Render template lalu sisipkan hasilnya ke {@code json}.
     *
     * <p>Mengisi {@code namaFile}, {@code varianNama}, dan {@code pdfBase64}.
     * Kegagalan render dilaporkan sebagai {@link IllegalStateException} agar
     * pemanggil memulangkan galat yang jelas, bukan PDF kosong yang tampak
     * seperti laporan tanpa data.</p>
     *
     * @param json       amplop yang akan diisi
     * @param template   nama template Jasper (tanpa akhiran)
     * @param parameters parameter laporan
     * @param kunci      awalan nama berkas unduhan
     * @param judul      nama laporan untuk ditampilkan klien
     */
    @SuppressWarnings("rawtypes")
    public static void tulis(JSONObject json, String template, Map parameters, String kunci, String judul)
            throws Exception {
        java.io.File pdf = ais.action.report.Report.generateFileReportSimple(
                ais.action.report.Report.PDF, parameters, template);
        if (pdf == null || !pdf.exists()) {
            throw new IllegalStateException("PDF laporan gagal dibuat.");
        }
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        json.put("namaFile", kunci + "_" + Common.databaseDateFormat.get().format(new Date()) + ".pdf");
        json.put("varianNama", judul);
        json.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }
}
