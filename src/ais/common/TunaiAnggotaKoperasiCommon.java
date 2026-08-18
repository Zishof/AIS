package ais.common;

import java.util.Collection;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.zkoss.zul.Rows;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasiDetail;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.MyMessageboxConfig;

public class TunaiAnggotaKoperasiCommon {

    public static PembayaranAnggotaKoperasi onSave(AnggotaKoperasi anggotaKoperasi,
            Collection<TransaksiKoperasiDetail> tag, Double deposit, String validator,
            CaraPembayaranKoperasi caraPembayaranKoperasi, Rows rowsDetailBiaya, Date tanggalTransaski)
            throws Exception {

        Double nilaiTagihan = rowsDetailBiaya == null ? 0.0 : PembayaranAnggotaKoperasi.chekDetail(rowsDetailBiaya);
        double nilaiDeposit = deposit == null ? 0.0 : deposit.doubleValue();
        double totalBayar = (nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue()) + nilaiDeposit;

        if (totalBayar <= 0.1) {
            PesanFormalHelper.tampilkanGagal("pembayaran tunai anggota koperasi",
                    "Belum ada transaksi yang dipilih dan tidak ada nilai tabungan/topup yang diisi, sehingga "
                            + "total yang harus dibayarkan bernilai nol.",
                    new String[] {
                            "Silakan centang minimal satu transaksi pada daftar rincian, atau",
                            "Isi nilai tabungan/topup yang ingin dibayarkan sebelum menyimpan." });
            return null;
        }

        if (anggotaKoperasi == null || anggotaKoperasi.getId() == null) {
            PesanFormalHelper.tampilkanGagal("pembayaran tunai anggota koperasi",
                    "Data Anggota Koperasi belum dipilih, sehingga sistem tidak dapat menentukan pemilik "
                            + "transaksi pembayaran ini.",
                    new String[] {
                            "Silakan pilih terlebih dahulu Anggota Koperasi yang bersangkutan.",
                            "Ulangi proses penyimpanan pembayaran setelah data anggota terisi." });
            return null;
        }

        StringBuilder keterangan = new StringBuilder("angsuran:");
        if (tag != null) {
            for (TransaksiKoperasiDetail transaksiKoperasiDetail : tag) {
                if (transaksiKoperasiDetail != null) {
                    keterangan.append(" ke-").append(transaksiKoperasiDetail.getKe()).append(",");
                }
            }
        }
        if (nilaiDeposit > 0.1) {
            keterangan.append(" topup tabungan ").append(Common.numberFormat.get().format(nilaiDeposit)).append(",");
        }

        Session session = null;
        Transaction tx = null;
        PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            AnggotaKoperasi anggotaManaged = (AnggotaKoperasi) session.get(AnggotaKoperasi.class,
                    anggotaKoperasi.getId());
            if (anggotaManaged == null) {
                throw new IllegalArgumentException("Data anggota koperasi tidak ditemukan atau sudah dihapus.");
            }

            pembayaranAnggotaKoperasi = new PembayaranAnggotaKoperasi();
            pembayaranAnggotaKoperasi.setAnggotaKoperasi(anggotaManaged);
            pembayaranAnggotaKoperasi.setKoperasi(anggotaManaged.getKoperasi());
            pembayaranAnggotaKoperasi.setTanggal(tanggalTransaski == null ? ais.ui.util.WaktuUtil.getDate()
                    : tanggalTransaski);
            pembayaranAnggotaKoperasi.setTanggalBayar(tanggalTransaski == null ? ais.ui.util.WaktuUtil.getDate()
                    : tanggalTransaski);
            pembayaranAnggotaKoperasi.setKeterangan(keterangan.toString());
            pembayaranAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasi);
            pembayaranAnggotaKoperasi.setNominal(totalBayar);
            pembayaranAnggotaKoperasi.setTambahanDeposit(nilaiDeposit);
            pembayaranAnggotaKoperasi.setValidator(validator == null ? "" : validator);
            session.save(pembayaranAnggotaKoperasi);

            if (tag != null) {
                for (TransaksiKoperasiDetail selectedDetail : tag) {
                    if (selectedDetail == null || selectedDetail.getId() == null) {
                        continue;
                    }
                    TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) session.get(
                            TransaksiKoperasiDetail.class, selectedDetail.getId());
                    if (detail == null || detail.getPembayaranAnggotaKoperasiDetail() != null) {
                        continue;
                    }
                    Double nominal = new Double(safeDouble(detail.getMargin()) + safeDouble(detail.getPokok()));
                    PembayaranAnggotaKoperasiDetail pembayaranDetail = new PembayaranAnggotaKoperasiDetail();
                    pembayaranDetail.setNominal(nominal);
                    pembayaranDetail.setNominalManual(nominal);
                    pembayaranDetail.setPembayaranAnggotaKoperasi(pembayaranAnggotaKoperasi);
                    pembayaranDetail.setTransaksiKoperasiDetail(detail);
                    session.save(pembayaranDetail);
                    detail.setPembayaranAnggotaKoperasiDetail(pembayaranDetail);
                    session.update(detail);
                }
            }

            tx.commit();
            return pembayaranAnggotaKoperasi;
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:115");
            }
            throw e;
        } finally {
            if (session != null) {
                try {
                    session.clear();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:122");
                }
                try {
                    session.disconnect();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:126");
                }
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:132");
                }
            }
            try {
                HibernateUtil.closeSession();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/TunaiAnggotaKoperasiCommon.java:137");
            }
        }
    }

    private static double safeDouble(Double value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
