package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;

/**
 * Helper riwayat harga DPP barang/jasa.
 *
 * DPP dipakai sebagai harga pembanding utama dari Tagihan Vendor, PO, dan BAST karena belum ditambah PPN dan belum
 * dikurangi PPH. Nilai ini lebih stabil untuk membantu petugas menentukan harga
 * wajar saat membuat PR/PO baru.
 */
public final class AssetHargaDppHistoryUtil {

    private static final int MAX_RIWAYAT_PER_SUMBER = 25;

    private AssetHargaDppHistoryUtil() {
    }

    /**
     * Tipe implementasi bersarang {@link HargaDppInfo} milik {@link AssetHargaDppHistoryUtil}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * AssetHargaDppHistoryUtil}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long masterAssetId}, {@code String
     * namaBarang}, {@code String sumber}, {@code String nomorDokumen}, {@code String penyedia}, {@code Date
     * tanggal}, {@code Double hargaDppTotal}, {@code Double hargaDppSatuan}; operasi lokal: {@code
     * getMasterAssetId()}, {@code setMasterAssetId()}, {@code getNamaBarang()}, {@code setNamaBarang()}, {@code
     * getSumber()}, {@code setSumber()}, {@code getNomorDokumen()}, {@code setNomorDokumen()}, {@code
     * getPenyedia()}, {@code setPenyedia}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see AssetHargaDppHistoryUtil
     */
    public static class HargaDppInfo {
        private Long masterAssetId;
        private String namaBarang;
        private String sumber;
        private String nomorDokumen;
        private String penyedia;
        private Date tanggal;
        private Double hargaDppTotal;
        private Double hargaDppSatuan;
        private Double rataRataDppSatuan;
        private Integer jumlahRiwayat;

        public Long getMasterAssetId() {
            return masterAssetId;
        }

        public void setMasterAssetId(Long masterAssetId) {
            this.masterAssetId = masterAssetId;
        }

        public String getNamaBarang() {
            return namaBarang;
        }

        public void setNamaBarang(String namaBarang) {
            this.namaBarang = namaBarang;
        }

        public String getSumber() {
            return sumber;
        }

        public void setSumber(String sumber) {
            this.sumber = sumber;
        }

        public String getNomorDokumen() {
            return nomorDokumen;
        }

        public void setNomorDokumen(String nomorDokumen) {
            this.nomorDokumen = nomorDokumen;
        }

        public String getPenyedia() {
            return penyedia;
        }

        public void setPenyedia(String penyedia) {
            this.penyedia = penyedia;
        }

        public Date getTanggal() {
            return tanggal;
        }

        public void setTanggal(Date tanggal) {
            this.tanggal = tanggal;
        }

        public Double getHargaDppTotal() {
            return hargaDppTotal;
        }

        public void setHargaDppTotal(Double hargaDppTotal) {
            this.hargaDppTotal = hargaDppTotal;
        }

        public Double getHargaDppSatuan() {
            return hargaDppSatuan;
        }

        public void setHargaDppSatuan(Double hargaDppSatuan) {
            this.hargaDppSatuan = hargaDppSatuan;
        }

        public Double getRataRataDppSatuan() {
            return rataRataDppSatuan;
        }

        public void setRataRataDppSatuan(Double rataRataDppSatuan) {
            this.rataRataDppSatuan = rataRataDppSatuan;
        }

        public Integer getJumlahRiwayat() {
            return jumlahRiwayat;
        }

        public void setJumlahRiwayat(Integer jumlahRiwayat) {
            this.jumlahRiwayat = jumlahRiwayat;
        }

        public boolean hasHarga() {
            return hargaDppSatuan != null && hargaDppSatuan.doubleValue() > 0.0d;
        }
    }

    /**
     * Tipe implementasi bersarang {@link RiwayatHargaDpp} milik {@link AssetHargaDppHistoryUtil}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * AssetHargaDppHistoryUtil}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String sumber}, {@code String
     * nomorDokumen}, {@code String penyedia}, {@code Date tanggal}, {@code Long id}, {@code Double dppTotal},
     * {@code Double dppSatuan}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see AssetHargaDppHistoryUtil
     */
    private static class RiwayatHargaDpp {
        private String sumber;
        private String nomorDokumen;
        private String penyedia;
        private Date tanggal;
        private Long id;
        private Double dppTotal;
        private Double dppSatuan;
    }

    public static HargaDppInfo ambilHargaDppTerbaru(Session session, MasterAsset masterAsset,
            Long abaikanPemesananDetailId) {
        if (session == null || masterAsset == null || masterAsset.getId() == null) {
            return null;
        }

        List<RiwayatHargaDpp> riwayat = ambilRiwayatHargaDpp(session, masterAsset, abaikanPemesananDetailId);
        if (riwayat == null || riwayat.isEmpty()) {
            return null;
        }

        Collections.sort(riwayat, new Comparator<RiwayatHargaDpp>() {
            @Override
            public int compare(RiwayatHargaDpp a, RiwayatHargaDpp b) {
                Date da = a == null ? null : a.tanggal;
                Date db = b == null ? null : b.tanggal;
                if (da != null && db != null) {
                    int c = db.compareTo(da);
                    if (c != 0) {
                        return c;
                    }
                } else if (da == null && db != null) {
                    return 1;
                } else if (da != null) {
                    return -1;
                }
                Long ia = a == null ? null : a.id;
                Long ib = b == null ? null : b.id;
                if (ia == null && ib == null) {
                    return 0;
                }
                if (ia == null) {
                    return 1;
                }
                if (ib == null) {
                    return -1;
                }
                return ib.compareTo(ia);
            }
        });

        double totalUnit = 0.0d;
        int count = 0;
        for (RiwayatHargaDpp row : riwayat) {
            if (row != null && row.dppSatuan != null && row.dppSatuan.doubleValue() > 0.0d) {
                totalUnit += row.dppSatuan.doubleValue();
                count++;
            }
        }

        RiwayatHargaDpp latest = riwayat.get(0);
        HargaDppInfo info = new HargaDppInfo();
        info.setMasterAssetId(masterAsset.getId());
        info.setNamaBarang(masterAsset.getNama());
        info.setSumber(latest.sumber);
        info.setNomorDokumen(latest.nomorDokumen);
        info.setPenyedia(latest.penyedia);
        info.setTanggal(latest.tanggal);
        info.setHargaDppTotal(latest.dppTotal);
        info.setHargaDppSatuan(latest.dppSatuan);
        info.setRataRataDppSatuan(count <= 0 ? latest.dppSatuan : Double.valueOf(totalUnit / count));
        info.setJumlahRiwayat(Integer.valueOf(count));
        return info;
    }

    @SuppressWarnings("unchecked")
    private static List<RiwayatHargaDpp> ambilRiwayatHargaDpp(Session session, MasterAsset masterAsset,
            Long abaikanPemesananDetailId) {
        List<RiwayatHargaDpp> data = new ArrayList<RiwayatHargaDpp>();

        // Helper ini HANYA membaca riwayat harga (read-only). Ia dipanggil saat merender form
        // (per baris detail) memakai currentSession yang dipakai bersama proses simpan. Bila
        // session sedang menahan entitas kotor (mis. detail dgn kodeUnik bentrok), query biasa
        // akan memicu AUTO-FLUSH -> flush gagal -> "current transaction is aborted" merembet ke
        // semua query berikutnya. Pakai FlushMode.MANUAL pada tiap kriteria agar pembacaan riwayat
        // TIDAK ikut menulis (flush) apa pun; jadi baca riwayat tak pernah jadi korban/pemicu abort.

        try {
            Criteria criteriaPo = session.createCriteria(PemesananPengadaanMasterAssetDetail.class)
                    .setFlushMode(FlushMode.MANUAL)
                    .createAlias("masterAsset", "ma")
                    .add(Restrictions.eq("ma.id", masterAsset.getId()))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(MAX_RIWAYAT_PER_SUMBER);
            if (abaikanPemesananDetailId != null) {
                criteriaPo.add(Restrictions.ne("id", abaikanPemesananDetailId));
            }
            List<PemesananPengadaanMasterAssetDetail> listPo = criteriaPo.list();
            if (listPo != null) {
                for (PemesananPengadaanMasterAssetDetail detail : listPo) {
                    RiwayatHargaDpp row = fromPemesananDetail(detail);
                    if (row != null && row.dppSatuan != null && row.dppSatuan.doubleValue() > 0.0d) {
                        data.add(row);
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        try {
            Criteria criteriaBast = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
                    .setFlushMode(FlushMode.MANUAL)
                    .createAlias("masterAsset", "ma")
                    .add(Restrictions.eq("ma.id", masterAsset.getId()))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(MAX_RIWAYAT_PER_SUMBER);
            List<PenerimaanPengadaanMasterAssetDetail> listBast = criteriaBast.list();
            if (listBast != null) {
                for (PenerimaanPengadaanMasterAssetDetail detail : listBast) {
                    RiwayatHargaDpp row = fromPenerimaanDetail(detail);
                    if (row != null && row.dppSatuan != null && row.dppSatuan.doubleValue() > 0.0d) {
                        data.add(row);
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }


        try {
            Criteria criteriaTagihanVendor = session.createCriteria(SaldoAwalMasterAssetDetail.class)
                    .setFlushMode(FlushMode.MANUAL)
                    .createAlias("masterAsset", "ma")
                    .add(Restrictions.eq("ma.id", masterAsset.getId()))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(MAX_RIWAYAT_PER_SUMBER);
            List<SaldoAwalMasterAssetDetail> listTagihanVendor = criteriaTagihanVendor.list();
            if (listTagihanVendor != null) {
                for (SaldoAwalMasterAssetDetail detail : listTagihanVendor) {
                    RiwayatHargaDpp row = fromSaldoAwalDetail(detail);
                    if (row != null && row.dppSatuan != null && row.dppSatuan.doubleValue() > 0.0d) {
                        data.add(row);
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        return data;
    }

    private static RiwayatHargaDpp fromPemesananDetail(PemesananPengadaanMasterAssetDetail detail) {
        if (detail == null || detail.getMasterAsset() == null) {
            return null;
        }
        double qty = nvl(detail.getJumlah(), 1.0d);
        double dpp = hitungDpp(qty, detail.getHargaBeli(), detail.getHargaPotongan(),
                detail.getDiskonDalamBentukPersen());
        RiwayatHargaDpp row = new RiwayatHargaDpp();
        row.sumber = "PO";
        row.id = detail.getId();
        row.dppTotal = Double.valueOf(dpp);
        row.dppSatuan = Double.valueOf(qty <= 0.0d ? dpp : dpp / qty);
        PemesananPengadaanMasterAsset master = detail.getPemesananPengadaanMasterAsset();
        if (master != null) {
            row.nomorDokumen = master.getKode();
            row.tanggal = pilihTanggal(master.getTanggalPersetujuan(), master.getTanggalPembuatan());
            row.penyedia = namaPenyedia(master.getPenyedia());
        }
        return row;
    }

    private static RiwayatHargaDpp fromPenerimaanDetail(PenerimaanPengadaanMasterAssetDetail detail) {
        if (detail == null || detail.getMasterAsset() == null) {
            return null;
        }
        double qty = nvl(detail.getDiterima(), nvl(detail.getJumlah(), 1.0d));
        double dpp = hitungDpp(qty, detail.getHargaBeli(), detail.getHargaPotongan(),
                detail.getDiskonDalamBentukPersen());
        RiwayatHargaDpp row = new RiwayatHargaDpp();
        row.sumber = "BAST";
        row.id = detail.getId();
        row.dppTotal = Double.valueOf(dpp);
        row.dppSatuan = Double.valueOf(qty <= 0.0d ? dpp : dpp / qty);
        PenerimaanPengadaanMasterAsset master = detail.getPenerimaanPengadaanMasterAsset();
        if (master != null) {
            row.nomorDokumen = master.getKode();
            row.tanggal = pilihTanggal(master.getTanggalPersetujuan(), master.getTanggalPembuatan());
            row.penyedia = namaPenyedia(master.getPenyedia());
        }
        return row;
    }


    private static RiwayatHargaDpp fromSaldoAwalDetail(SaldoAwalMasterAssetDetail detail) {
        if (detail == null || detail.getMasterAsset() == null) {
            return null;
        }
        double qty = nvl(detail.getJumlah(), 1.0d);
        double dpp = hitungDpp(qty, detail.getHarga(), detail.getHargaPotongan(),
                detail.getDiskonDalamBentukPersen());
        RiwayatHargaDpp row = new RiwayatHargaDpp();
        row.sumber = "TAGIHAN VENDOR";
        row.id = detail.getId();
        row.dppTotal = Double.valueOf(dpp);
        row.dppSatuan = Double.valueOf(qty <= 0.0d ? dpp : dpp / qty);
        SaldoAwalMasterAsset master = detail.getSaldoAwal();
        if (master != null) {
            row.nomorDokumen = master.getKodeTagihan();
            if (row.nomorDokumen == null || row.nomorDokumen.trim().length() == 0) {
                row.nomorDokumen = master.getKode();
            }
            row.tanggal = pilihTanggal(master.getTanggalTagihan(),
                    pilihTanggal(master.getTanggalPersetujuan(), master.getTanggalPembuatan()));
            row.penyedia = namaPenyedia(master.getPenyedia());
        }
        return row;
    }

    public static double hitungDpp(double jumlah, Double hargaBeli, Double hargaPotongan,
            Boolean diskonDalamBentukPersen) {
        double qty = jumlah <= 0.0d ? 1.0d : jumlah;
        double dpp = qty * nvl(hargaBeli, 0.0d);
        double potongan = Boolean.FALSE.equals(diskonDalamBentukPersen)
                ? nvl(hargaPotongan, 0.0d)
                : (nvl(hargaPotongan, 0.0d) / 100.0d) * dpp;
        dpp = dpp - potongan;
        return dpp < 0.0d ? 0.0d : dpp;
    }

    public static String formatMoney(Double value) {
        try {
            return Common.numberFormat.get().format(nvl(value, 0.0d));
        } catch (Exception e) {
            return String.valueOf(nvl(value, 0.0d));
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        try {
            return Common.dateFormat3.get().format(date);
        } catch (Exception e) {
            return "-";
        }
    }

    public static String ringkasan(HargaDppInfo info) {
        if (info == null || !info.hasHarga()) {
            return "Belum ada riwayat DPP";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DPP/satuan Rp ").append(formatMoney(info.getHargaDppSatuan()));
        if (info.getRataRataDppSatuan() != null) {
            sb.append(" | Rata-rata Rp ").append(formatMoney(info.getRataRataDppSatuan()));
        }
        if (info.getSumber() != null) {
            sb.append(" | ").append(info.getSumber());
        }
        if (info.getNomorDokumen() != null) {
            sb.append(" ").append(info.getNomorDokumen());
        }
        return sb.toString();
    }

    private static Date pilihTanggal(Date utama, Date fallback) {
        return utama == null ? fallback : utama;
    }

    private static String namaPenyedia(PenyediaAsset penyedia) {
        try {
            return penyedia == null ? "-" : penyedia.getNama();
        } catch (Exception e) {
            return "-";
        }
    }

    private static double nvl(Double value, double def) {
        return value == null ? def : value.doubleValue();
    }
}
