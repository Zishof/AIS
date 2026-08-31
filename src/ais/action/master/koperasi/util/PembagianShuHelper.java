package ais.action.master.koperasi.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembagianShu;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;

/**
 * Perhitungan &amp; penyimpanan pembagian Sisa Hasil Usaha (SHU).
 *
 * <p><b>Mengapa dipisahkan.</b> Logika ini semula berada di dalam composer ZK
 * {@code PembagianShuAction} dan membaca nilainya langsung dari widget
 * {@code Doublebox}. Layar native tidak punya widget, sehingga tanpa pemisahan
 * ini perhitungan SHU harus ditulis dua kali — dan dua salinan pembagian uang
 * ke anggota adalah hal terakhir yang boleh menyimpang. Di sini logikanya
 * dipertahankan apa adanya, hanya sumber nilainya yang diubah dari widget
 * menjadi parameter biasa.</p>
 *
 * <p>Dasar pembagian mengikuti versi ZK: <i>jasa modal</i> dibagi sebanding
 * total simpanan tiap anggota, <i>jasa usaha</i> sebanding margin pinjaman
 * yang masih aktif. Menghitung ulang tahun yang sama bersifat menggantikan:
 * rincian lama dihapus lebih dulu agar tidak menumpuk.</p>
 */
public final class PembagianShuHelper {

    /** Parameter alokasi SHU satu tahun buku, sesuai keputusan RAT. */
    public static final class Parameter {
        public double totalShu;
        public double persenCadangan;
        public double persenJasaModal;
        public double persenJasaUsaha;
        public double persenPendidikan;
        public double persenPengurus;
        public double persenSosial;
    }

    private PembagianShuHelper() { }

    /** Cari pembagian SHU satu tahun (baris terbaru bila kebetulan ada lebih dari satu). */
    public static PembagianShu cari(Session session, int tahun) {
        try {
            return (PembagianShu) session.createQuery("from PembagianShu p where p.tahun = :th order by p.id desc")
                    .setParameter("th", Integer.valueOf(tahun)).setMaxResults(1).uniqueResult();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PembagianShuHelper.cari");
            return null;
        }
    }

    /**
     * Hitung bagian SHU tiap anggota lalu simpan.
     *
     * <p>Membuat atau memperbarui {@link PembagianShu} tahun terpilih, menghapus
     * {@link ShuAnggota} lamanya, lalu menyimpan hasil pembagian yang baru.</p>
     *
     * @return kepala pembagian yang tersimpan
     */
    @SuppressWarnings("unchecked")
    public static PembagianShu hitungDanSimpan(Session session, int tahun, Parameter p) {
        PembagianShu pembagian = cari(session, tahun);
        if (pembagian == null) {
            pembagian = new PembagianShu();
            pembagian.setTahun(Integer.valueOf(tahun));
            try {
                pembagian.setKoperasi(Common.getCurrentKoperasi());
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "PembagianShuHelper.hitungDanSimpan:koperasi");
            }
        }
        pembagian.setTotalShu(p.totalShu);
        pembagian.setPersenCadangan(p.persenCadangan);
        pembagian.setPersenJasaModal(p.persenJasaModal);
        pembagian.setPersenJasaUsaha(p.persenJasaUsaha);
        pembagian.setPersenPendidikan(p.persenPendidikan);
        pembagian.setPersenPengurus(p.persenPengurus);
        pembagian.setPersenSosial(p.persenSosial);
        pembagian.setStatus(PembagianShu.STATUS_DIBAGIKAN);
        Common.refreshSaveOrUpdate(session, pembagian);
        session.flush();

        // Hapus rincian lama agar perhitungan ulang menggantikan, bukan menambah.
        try {
            session.createQuery("delete from ShuAnggota s where s.pembagianShu.id = :id")
                    .setParameter("id", pembagian.getId()).executeUpdate();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PembagianShuHelper.hapusRincianLama");
        }

        Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
        Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;

        Map<Long, Double> simpananPer = new HashMap<Long, Double>();
        Map<Long, Double> partisipasiPer = new HashMap<Long, Double>();
        Map<Long, AnggotaKoperasi> anggotaMap = new HashMap<Long, AnggotaKoperasi>();

        // Basis jasa modal: total simpanan tiap anggota.
        if (tipeSimpanan != null) {
            List<TransaksiKoperasi> simp = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                            + "where t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", tipeSimpanan).list();
            for (TransaksiKoperasi t : simp) {
                akumulasi(t, t.getNilai(), simpananPer, anggotaMap);
            }
        }
        // Basis jasa usaha: total jasa/bunga (margin) pinjaman yang masih aktif.
        if (tipePinjaman != null) {
            List<TransaksiKoperasi> pinj = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                            + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", tipePinjaman).list();
            for (TransaksiKoperasi t : pinj) {
                try {
                    if (t.getAktif()) {
                        akumulasi(t, t.getMargin(), partisipasiPer, anggotaMap);
                    }
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "PembagianShuHelper.partisipasi");
                }
            }
        }

        double totalSimpAll = jumlah(simpananPer);
        double totalPartAll = jumlah(partisipasiPer);
        double nominalJasaModal = pembagian.getNominalJasaModal();
        double nominalJasaUsaha = pembagian.getNominalJasaUsaha();

        for (Map.Entry<Long, AnggotaKoperasi> e : anggotaMap.entrySet()) {
            try {
                Long aid = e.getKey();
                double simpanan = get0(simpananPer, aid);
                double partisipasi = get0(partisipasiPer, aid);
                double jasaModal = totalSimpAll > 0 ? simpanan / totalSimpAll * nominalJasaModal : 0.0;
                double jasaUsaha = totalPartAll > 0 ? partisipasi / totalPartAll * nominalJasaUsaha : 0.0;

                ShuAnggota sa = new ShuAnggota();
                sa.setPembagianShu(pembagian);
                sa.setAnggota(e.getValue());
                sa.setTotalSimpanan(Double.valueOf(simpanan));
                sa.setTotalTransaksi(Double.valueOf(partisipasi));
                sa.setJasaModal(Double.valueOf(jasaModal));
                sa.setJasaUsaha(Double.valueOf(jasaUsaha));
                sa.setTotalShu(Double.valueOf(jasaModal + jasaUsaha));
                session.save(sa);
            } catch (Exception ex) {
                ais.common.ErrorAuditUtil.record(ex, "PembagianShuHelper.simpanRincian");
            }
        }
        session.flush();
        return pembagian;
    }

    /** Rincian SHU per anggota untuk satu kepala pembagian, terbesar lebih dulu. */
    @SuppressWarnings("unchecked")
    public static List<ShuAnggota> rincian(Session session, PembagianShu pembagian) {
        return session.createQuery(
                "select distinct s from ShuAnggota s left join fetch s.anggota a where s.pembagianShu.id = :id "
                        + "order by s.totalShu desc").setParameter("id", pembagian.getId()).list();
    }

    /** Tambahkan nilai ke akumulator per anggota, sekaligus mencatat objek anggotanya. */
    private static void akumulasi(TransaksiKoperasi t, double nilaiTambah, Map<Long, Double> akum,
            Map<Long, AnggotaKoperasi> anggotaMap) {
        try {
            AnggotaKoperasi a = t.getAnggotaKoperasi();
            if (a == null || a.getId() == null) {
                return;
            }
            Long aid = a.getId();
            Double v = akum.get(aid);
            akum.put(aid, Double.valueOf((v == null ? 0.0 : v.doubleValue()) + nilaiTambah));
            if (!anggotaMap.containsKey(aid)) {
                anggotaMap.put(aid, a);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PembagianShuHelper.akumulasi");
        }
    }

    private static double jumlah(Map<Long, Double> map) {
        double t = 0;
        for (Double v : map.values()) {
            t += v == null ? 0 : v.doubleValue();
        }
        return t;
    }

    private static double get0(Map<Long, Double> map, Long key) {
        Double v = map.get(key);
        return v == null ? 0.0 : v.doubleValue();
    }
}
