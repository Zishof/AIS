package ais.common;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

/**
 * Laporan per-baris untuk proses upload Excel.
 * Catat SUKSES/GAGAL tiap baris, lalu simpan ke file teks untuk auto-download.
 */
public class UploadReportHelper {

    private final StringBuilder sb = new StringBuilder();
    private final String namaProses;
    private int sukses = 0;
    private int gagal = 0;

    public UploadReportHelper(String namaProses) {
        this.namaProses = namaProses;
        String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        sb.append("=================================================================\n");
        sb.append("LAPORAN UPLOAD: ").append(namaProses).append("\n");
        sb.append("Tanggal      : ").append(waktu).append("\n");
        sb.append("=================================================================\n\n");
    }

    public void sukses(int baris, String identifier, String detail) {
        sukses++;
        sb.append("[SUKSES] Baris ").append(baris).append(" | ").append(identifier);
        if (detail != null && !detail.trim().isEmpty()) {
            sb.append(" | ").append(detail);
        }
        sb.append("\n");
    }

    public void gagal(int baris, String identifier, Exception e, String solusi) {
        gagal++;
        sb.append("[GAGAL]  Baris ").append(baris).append(" | ").append(identifier).append("\n");
        String pesanError = (e != null)
                ? e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "")
                : "Error tidak diketahui";
        sb.append("  Error  : ").append(pesanError).append("\n");
        if (solusi != null && !solusi.trim().isEmpty()) {
            sb.append("  Solusi : ").append(solusi).append("\n");
        }
        sb.append("  Tindak : Jika error berlanjut, hubungi admin/tim pengembang dengan melampirkan screenshot halaman ini.\n");
    }

    public void gagal(int baris, String identifier, String pesan, String solusi) {
        gagal++;
        sb.append("[GAGAL]  Baris ").append(baris).append(" | ").append(identifier).append("\n");
        sb.append("  Error  : ").append(pesan != null ? pesan : "Error tidak diketahui").append("\n");
        if (solusi != null && !solusi.trim().isEmpty()) {
            sb.append("  Solusi : ").append(solusi).append("\n");
        }
        sb.append("  Tindak : Jika error berlanjut, hubungi admin/tim pengembang dengan melampirkan screenshot halaman ini.\n");
    }

    public int getSukses() { return sukses; }
    public int getGagal() { return gagal; }

    public String getRingkasan() {
        return "Berhasil: " + sukses + " baris, Gagal: " + gagal + " baris.";
    }

    /** Tulis laporan ke file temp, kembalikan File-nya untuk Filedownload. */
    public File simpanLaporan() throws IOException {
        sb.append("\n=================================================================\n");
        sb.append("RINGKASAN\n");
        sb.append("  Total Berhasil : ").append(sukses).append(" baris\n");
        sb.append("  Total Gagal    : ").append(gagal).append(" baris\n");
        if (gagal > 0) {
            sb.append("\nPerhatian: Ada data yang GAGAL diproses.\n");
            sb.append("Perbaiki data sesuai petunjuk di atas, kemudian ulangi upload.\n");
            sb.append("Jika masalah berlanjut, kirimkan file laporan ini ke admin/tim pengembang.\n");
        } else {
            sb.append("\nSemua data berhasil diproses.\n");
        }
        sb.append("=================================================================\n");

        String fileName = "laporan_upload_" + System.currentTimeMillis() + ".txt";
        File dir = new File(Common.REAL_PATH + File.separator + "tmp");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        FileUtils.writeStringToFile(file, sb.toString(), "UTF-8");
        return file;
    }
}
