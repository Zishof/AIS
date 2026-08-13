package ais.common.newui.menu;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;

/**
 * Versi headless perhitungan {@code LaporanKeuanganKoperasiAction}. Rumus dan
 * klasifikasi sengaja sama dengan Action ZK existing, tetapi hasilnya berupa
 * DTO sehingga dapat dipakai JSP New UI tanpa komponen ZK/ZUL.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiKoperasiFinancialService {
    private NewUiKoperasiFinancialService() { }

    public static Summary load() {
        Summary result = new Summary();
        Session session = HibernateUtil.currentSession();
        Long simpanan = ConstantValues.SIMPANAN == null ? null : ConstantValues.SIMPANAN.getId();
        Long pinjaman = ConstantValues.PINJAMAN == null ? null : ConstantValues.PINJAMAN.getId();
        if (simpanan != null) {
            List values = session.createQuery("select distinct t from TransaksiKoperasi t "
                    + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id=:tipe")
                    .setParameter("tipe", simpanan).list();
            for (int i = 0; i < values.size(); i++) {
                TransaksiKoperasi value = (TransaksiKoperasi) values.get(i);
                String name = value.getProdukKoperasi() == null || value.getProdukKoperasi().getNama() == null
                        ? "" : value.getProdukKoperasi().getNama().toLowerCase();
                double amount = value.getNilai() == null ? 0D : value.getNilai().doubleValue();
                if (name.indexOf("pokok") >= 0) result.simpananPokok += amount;
                else if (name.indexOf("wajib") >= 0) result.simpananWajib += amount;
                else result.simpananSukarela += amount;
            }
        }
        if (pinjaman != null) {
            List loans = session.createQuery("select distinct t from TransaksiKoperasi t "
                    + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id=:tipe")
                    .setParameter("tipe", pinjaman).list();
            for (int i = 0; i < loans.size(); i++) {
                TransaksiKoperasi value = (TransaksiKoperasi) loans.get(i);
                if (Boolean.TRUE.equals(value.getAktif()) && value.getNilai() != null) {
                    result.totalPokokTersalur += value.getNilai().doubleValue();
                }
            }
            List open = session.createQuery("select distinct d from TransaksiKoperasiDetail d "
                    + "left join fetch d.transaksiKoperasi t where d.pembayaranAnggotaKoperasiDetail is null "
                    + "and t.produkKoperasi.tipeProdukKoperasi.id=:tipe")
                    .setParameter("tipe", pinjaman).list();
            for (int i = 0; i < open.size(); i++) {
                TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) open.get(i);
                double principal = detail.getPokok() == null ? 0D : detail.getPokok().doubleValue();
                result.outstandingPokok += principal;
                TransaksiKoperasi transaction = detail.getTransaksiKoperasi();
                String quality = transaction == null ? TransaksiKoperasi.KOL_LANCAR
                        : transaction.getKolektibilitas();
                if (TransaksiKoperasi.KOL_MACET.equals(quality)) result.outMacet += principal;
                else if (TransaksiKoperasi.KOL_RAGU.equals(quality)) result.outRagu += principal;
                else if (TransaksiKoperasi.KOL_KURANG_LANCAR.equals(quality)) result.outKurang += principal;
                else result.outLancar += principal;
            }
            List paid = session.createQuery("select distinct d from TransaksiKoperasiDetail d "
                    + "left join fetch d.transaksiKoperasi t where d.pembayaranAnggotaKoperasiDetail is not null "
                    + "and t.produkKoperasi.tipeProdukKoperasi.id=:tipe")
                    .setParameter("tipe", pinjaman).list();
            for (int i = 0; i < paid.size(); i++) {
                TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) paid.get(i);
                result.jasaDiterima += detail.getMargin() == null ? 0D : detail.getMargin().doubleValue();
                result.angsuranPokokDiterima += detail.getPokok() == null ? 0D : detail.getPokok().doubleValue();
            }
        }
        List capitals = session.createQuery("from ModalPenyertaanKoperasi m where m.status=:status "
                + "and (m.aktif is null or m.aktif=true)")
                .setParameter("status", ModalPenyertaanKoperasi.STATUS_AKTIF).list();
        for (int i = 0; i < capitals.size(); i++) {
            ModalPenyertaanKoperasi value = (ModalPenyertaanKoperasi) capitals.get(i);
            result.modalPenyertaan += value.getNominal() == null ? 0D : value.getNominal().doubleValue();
        }
        result.calculate();
        return result;
    }

    public static void writeWorkbook(Summary value, OutputStream output) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet summary = workbook.createSheet("RINGKASAN");
            row(summary, 0, "Pos", "Nilai");
            row(summary, 1, "Total Aset", value.totalAset);
            row(summary, 2, "Kas dan Setara", value.kas);
            row(summary, 3, "Piutang Pinjaman", value.outstandingPokok);
            row(summary, 4, "Modal Sendiri", value.modalSendiri);
            row(summary, 5, "Simpanan Sukarela", value.simpananSukarela);
            row(summary, 6, "Pendapatan Jasa", value.jasaDiterima);
            row(summary, 7, "Arus Kas Bersih", value.arusKasBersih);
            row(summary, 8, "PPAP", value.totalPpap);
            row(summary, 9, "Modal Penyertaan Aktif", value.modalPenyertaan);
            XSSFSheet ppap = workbook.createSheet("PPAP");
            row(ppap, 0, "Kolektibilitas", "Outstanding", "Persentase", "Cadangan");
            row(ppap, 1, "Lancar", value.outLancar, "0,5%", value.outLancar * .005D);
            row(ppap, 2, "Kurang Lancar", value.outKurang, "10%", value.outKurang * .10D);
            row(ppap, 3, "Ragu-ragu", value.outRagu, "50%", value.outRagu * .50D);
            row(ppap, 4, "Macet", value.outMacet, "100%", value.outMacet);
            XSSFSheet calk = workbook.createSheet("CALK");
            row(calk, 0, "Pos", "Nilai");
            row(calk, 1, "Simpanan Pokok", value.simpananPokok);
            row(calk, 2, "Simpanan Wajib", value.simpananWajib);
            row(calk, 3, "Simpanan Sukarela", value.simpananSukarela);
            row(calk, 4, "Modal Penyertaan", value.modalPenyertaan);
            row(calk, 5, "Piutang Berjalan", value.outstandingPokok);
            workbook.write(output);
        } finally {
            try { workbook.close(); } catch (Exception ignored) { }
        }
    }

    private static void row(XSSFSheet sheet, int index, Object... values) {
        XSSFRow row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number) row.createCell(i).setCellValue(((Number) values[i]).doubleValue());
            else row.createCell(i).setCellValue(values[i] == null ? "" : String.valueOf(values[i]));
        }
    }

    public static final class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        public double simpananPokok, simpananWajib, simpananSukarela, modalPenyertaan;
        public double totalPokokTersalur, outstandingPokok, jasaDiterima, angsuranPokokDiterima;
        public double outLancar, outKurang, outRagu, outMacet;
        public double totalSimpanan, modalSendiri, kas, totalAset, kasMasuk, kasKeluar;
        public double arusKasBersih, totalPpap;

        private void calculate() {
            totalSimpanan = simpananPokok + simpananWajib + simpananSukarela;
            modalSendiri = simpananPokok + simpananWajib + modalPenyertaan;
            kas = Math.max(0D, totalSimpanan - outstandingPokok);
            totalAset = kas + outstandingPokok;
            kasMasuk = totalSimpanan + angsuranPokokDiterima + jasaDiterima;
            kasKeluar = totalPokokTersalur;
            arusKasBersih = kasMasuk - kasKeluar;
            totalPpap = outLancar * .005D + outKurang * .10D + outRagu * .50D + outMacet;
        }
    }
}
