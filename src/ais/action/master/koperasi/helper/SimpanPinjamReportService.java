package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.common.ConstantValues;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;

/**
 * Sumber data bersama untuk layar ZK dan kontrak native Laporan Simpan Pinjam.
 *
 * <p>Kelas ini sengaja tidak membuat komponen UI dan tidak memformat angka.
 * Setiap pembangun mengembalikan struktur kolom serta nilai mentah yang sama,
 * sehingga ZK, Flutter desktop/Android, dan ekspor Excel tidak mempunyai tiga
 * versi perhitungan yang dapat saling menyimpang.</p>
 */
public final class SimpanPinjamReportService {

    public static final String BUKU_SIMPAN_PINJAM = "buku_simpan_pinjam";
    public static final String JURNAL_KAS_MASUK = "jurnal_kas_masuk";
    public static final String JURNAL_KAS_KELUAR = "jurnal_kas_keluar";
    public static final String BUKU_KAS = "buku_kas";
    public static final String DAFTAR_TUNGGAKAN = "daftar_tunggakan";
    public static final String SIMPANAN_BERJANGKA = "simpanan_berjangka";
    public static final String BUKU_ANGGOTA = "buku_anggota";
    public static final String PROMOSI_EKONOMI = "promosi_ekonomi_anggota";
    public static final String BUNGA_SIMPANAN = "bunga_simpanan";

    private static final String[] KUNCI_UTAMA = {
            BUKU_SIMPAN_PINJAM, JURNAL_KAS_MASUK, JURNAL_KAS_KELUAR,
            BUKU_KAS, DAFTAR_TUNGGAKAN, SIMPANAN_BERJANGKA,
            BUKU_ANGGOTA, PROMOSI_EKONOMI
    };

    private SimpanPinjamReportService() { }

    /** Struktur satu tabel laporan beserta dekorasi angka yang tetap mentah. */
    public static final class Bagian {
        public final String kunci;
        public final String judul;
        public final String deskripsi;
        public final String sheet;
        public final String namaBerkas;
        public final String[] header;
        public final int[] jenisKolom;
        public final List<Object[]> baris;
        public final LinkedHashMap<String, Double> grafik = new LinkedHashMap<String, Double>();
        public final LinkedHashMap<String, Object> ringkasan = new LinkedHashMap<String, Object>();
        public String catatan;

        private Bagian(String kunci, String judul, String deskripsi, String sheet,
                String namaBerkas, String[] header, int[] jenisKolom, List<Object[]> baris) {
            this.kunci = kunci;
            this.judul = judul;
            this.deskripsi = deskripsi;
            this.sheet = sheet;
            this.namaBerkas = namaBerkas;
            this.header = header;
            this.jenisKolom = jenisKolom;
            this.baris = baris == null ? new ArrayList<Object[]>() : baris;
        }
    }

    /** Hasil surat teguran yang sudah dikelompokkan per anggota. */
    public static final class Surat {
        public final int jumlah;
        public final String html;

        private Surat(int jumlah, String html) {
            this.jumlah = jumlah;
            this.html = html == null ? "" : html;
        }
    }

    public static boolean dikenal(String kunci) {
        if (BUNGA_SIMPANAN.equals(kunci)) return true;
        for (String nilai : KUNCI_UTAMA) if (nilai.equals(kunci)) return true;
        return false;
    }

    /** Metadata semua bagian tanpa menyentuh database. */
    public static List<Bagian> katalog() {
        List<Bagian> hasil = new ArrayList<Bagian>();
        hasil.add(definisi(BUKU_SIMPAN_PINJAM));
        hasil.add(definisi(JURNAL_KAS_MASUK));
        hasil.add(definisi(JURNAL_KAS_KELUAR));
        hasil.add(definisi(BUKU_KAS));
        hasil.add(definisi(DAFTAR_TUNGGAKAN));
        hasil.add(definisi(SIMPANAN_BERJANGKA));
        hasil.add(definisi(BUKU_ANGGOTA));
        hasil.add(definisi(PROMOSI_EKONOMI));
        hasil.add(definisi(BUNGA_SIMPANAN));
        return hasil;
    }

    /** Bangun delapan buku utama dalam urutan layar ZK. */
    public static List<Bagian> semua(Session session, Date sekarang) {
        List<Bagian> hasil = new ArrayList<Bagian>();
        for (String kunci : KUNCI_UTAMA) hasil.add(bangun(session, kunci, sekarang));
        return hasil;
    }

    /** Bangun satu bagian saja agar API tidak menjalankan delapan kueri sekaligus. */
    public static Bagian bangun(Session session, String kunci, Date sekarang) {
        if (session == null) throw new IllegalArgumentException("Sesi database tidak tersedia.");
        if (!dikenal(kunci)) throw new IllegalArgumentException("Bagian laporan tidak dikenal.");
        Date now = sekarang == null ? new Date() : sekarang;
        Long pinjaman = ConstantValues.PINJAMAN == null ? null : ConstantValues.PINJAMAN.getId();
        Long simpanan = ConstantValues.SIMPANAN == null ? null : ConstantValues.SIMPANAN.getId();
        if (BUKU_SIMPAN_PINJAM.equals(kunci)) return bukuSimpanPinjam(session, pinjaman);
        if (JURNAL_KAS_MASUK.equals(kunci)) return jurnalKasMasuk(session, simpanan, pinjaman);
        if (JURNAL_KAS_KELUAR.equals(kunci)) return jurnalKasKeluar(session, pinjaman);
        if (BUKU_KAS.equals(kunci)) return bukuKas(session, pinjaman);
        if (DAFTAR_TUNGGAKAN.equals(kunci)) return daftarTunggakan(session, pinjaman, now);
        if (SIMPANAN_BERJANGKA.equals(kunci)) return simpananBerjangka(session, simpanan, now);
        if (BUKU_ANGGOTA.equals(kunci)) return bukuAnggota(session);
        if (PROMOSI_EKONOMI.equals(kunci)) return promosiEkonomi(session, simpanan, pinjaman);
        return bungaSimpanan(session, simpanan, now);
    }

    private static Bagian definisi(String kunci) {
        List<Object[]> kosong = new ArrayList<Object[]>();
        if (BUKU_SIMPAN_PINJAM.equals(kunci)) return bagian(kunci, "Buku Simpan Pinjam",
                "Rincian tiap pinjaman anggota beserta jadwal cicilannya: pokok, bunga, jumlah, dan sisa.",
                "Buku Simpan Pinjam", "buku_simpan_pinjam",
                new String[] { "Anggota", "Produk", "Tgl Pinjam", "Besar Pinjaman", "Angsuran Ke",
                        "Tgl Angsuran", "Angsuran Pokok", "Angsuran Bunga", "Jumlah", "Sisa Pinjaman", "Status" },
                new int[] { 0, 0, 3, 2, 1, 3, 2, 2, 2, 2, 0 }, kosong);
        if (JURNAL_KAS_MASUK.equals(kunci)) return bagian(kunci, "Jurnal Kas Masuk",
                "Ringkasan penerimaan kas koperasi per kategori (simpanan, angsuran, jasa).",
                "Jurnal Kas Masuk", "jurnal_kas_masuk",
                new String[] { "Kategori Penerimaan", "Jumlah" }, new int[] { 0, 2 }, kosong);
        if (JURNAL_KAS_KELUAR.equals(kunci)) return bagian(kunci, "Jurnal Kas Keluar",
                "Ringkasan pengeluaran kas koperasi dari kegiatan simpan pinjam.",
                "Jurnal Kas Keluar", "jurnal_kas_keluar",
                new String[] { "Kategori Pengeluaran", "Jumlah" }, new int[] { 0, 2 }, kosong);
        if (BUKU_KAS.equals(kunci)) return bagian(kunci, "Buku Kas Simpan Pinjam",
                "Catatan uang masuk (setoran/angsuran) dan keluar (pencairan pinjaman) beserta sisa saldonya.",
                "Buku Kas", "buku_kas_simpan_pinjam",
                new String[] { "No", "Tanggal", "Uraian", "Pemasukan", "Pengeluaran", "Saldo" },
                new int[] { 1, 3, 0, 2, 2, 2 }, kosong);
        if (DAFTAR_TUNGGAKAN.equals(kunci)) return bagian(kunci, "Daftar Tunggakan (Perlu Pembinaan)",
                "Angsuran yang sudah lewat tanggal jatuh tempo namun belum dibayar, diurutkan dari yang tertua.",
                "Tunggakan", "daftar_tunggakan",
                new String[] { "Anggota", "Angsuran Ke", "Jatuh Tempo", "Jumlah Tertunggak",
                        "Hari Terlambat", "Kolektibilitas" }, new int[] { 0, 1, 3, 2, 1, 0 }, kosong);
        if (SIMPANAN_BERJANGKA.equals(kunci)) return bagian(kunci, "Simpanan Berjangka (Deposito) & Jatuh Tempo",
                "Daftar simpanan berjangka anggota beserta perkiraan tanggal jatuh temponya.",
                "Simpanan Berjangka", "simpanan_berjangka",
                new String[] { "Anggota", "Produk", "Nominal", "Tgl Setor", "Perkiraan Jatuh Tempo", "Status" },
                new int[] { 0, 0, 2, 3, 3, 0 }, kosong);
        if (BUKU_ANGGOTA.equals(kunci)) return bagian(kunci, "Buku Anggota Koperasi",
                "Daftar seluruh anggota: identitas, tanggal masuk, status, serta tanggal & alasan berhenti.",
                "Buku Anggota", "buku_anggota",
                new String[] { "No", "Nama Lengkap", "Alamat", "Jenis Anggota", "Tgl Masuk", "Status",
                        "Tgl Berhenti", "Alasan Berhenti" }, new int[] { 1, 0, 0, 0, 3, 0, 3, 0 }, kosong);
        if (PROMOSI_EKONOMI.equals(kunci)) return bagian(kunci, "Laporan Promosi Ekonomi Anggota",
                "Simpanan, pinjaman, jasa yang dibayar, dan SHU yang diterima tiap anggota.",
                "Promosi Ekonomi Anggota", "promosi_ekonomi_anggota",
                new String[] { "Anggota", "Total Simpanan", "Total Pinjaman", "Jasa Dibayar", "SHU Diterima" },
                new int[] { 0, 2, 2, 2, 2 }, kosong);
        return bagian(kunci, "Rincian Bunga Simpanan per Anggota",
                "Saldo terendah dan rata-rata ditampilkan sebagai bahan telusur perhitungan.",
                "Bunga Simpanan", "bunga_simpanan",
                new String[] { "Anggota", "Produk", "Metode", "Bunga %/th", "Saldo Terendah",
                        "Saldo Rata-rata", "Bunga Bulan Ini" }, new int[] { 0, 0, 0, 1, 2, 2, 2 }, kosong);
    }

    private static Bagian bagian(String kunci, String judul, String deskripsi, String sheet,
            String namaBerkas, String[] header, int[] jenis, List<Object[]> baris) {
        return new Bagian(kunci, judul, deskripsi, sheet, namaBerkas, header, jenis, baris);
    }

    @SuppressWarnings("unchecked")
    private static Bagian bukuSimpanPinjam(Session session, Long tipe) {
        Bagian hasil = definisi(BUKU_SIMPAN_PINJAM);
        if (tipe == null) return hasil;
        List<TransaksiKoperasiDetail> daftar = session.createQuery(
                "select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
                + "left join fetch t.anggotaKoperasi a left join fetch t.produkKoperasi p "
                + "where p.tipeProdukKoperasi.id = :tipe order by t.id, d.ke")
                .setParameter("tipe", tipe).list();
        for (TransaksiKoperasiDetail d : daftar) {
            TransaksiKoperasi t = d.getTransaksiKoperasi();
            if (t == null) continue;
            hasil.baris.add(new Object[] { nama(t.getAnggotaKoperasi()), nama(t.getProdukKoperasi()),
                    t.getTanggalTransaksi(), Double.valueOf(angka(t.getNilai())),
                    Integer.valueOf(d.getKe() == null ? 0 : d.getKe()), d.getTanggal(),
                    Double.valueOf(d.getPokok()), Double.valueOf(d.getMargin()),
                    Double.valueOf(d.getPokok() + d.getMargin()), Double.valueOf(angka(d.getSisa())),
                    d.getPembayaranAnggotaKoperasiDetail() == null ? "Belum" : "Lunas" });
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian jurnalKasMasuk(Session session, Long simpanan, Long pinjaman) {
        Bagian hasil = definisi(JURNAL_KAS_MASUK);
        double pokok = 0, wajib = 0, sukarela = 0, angsuran = 0, jasa = 0;
        if (simpanan != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
                    + "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", simpanan).list();
            for (TransaksiKoperasi t : daftar) {
                String produk = nama(t.getProdukKoperasi()).toLowerCase();
                if (produk.contains("pokok")) pokok += angka(t.getNilai());
                else if (produk.contains("wajib")) wajib += angka(t.getNilai());
                else sukarela += angka(t.getNilai());
            }
        }
        if (pinjaman != null) {
            List<TransaksiKoperasiDetail> daftar = session.createQuery(
                    "select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
                    + "where d.pembayaranAnggotaKoperasiDetail is not null "
                    + "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", pinjaman).list();
            for (TransaksiKoperasiDetail d : daftar) {
                angsuran += d.getPokok();
                jasa += d.getMargin();
            }
        }
        tambahKategori(hasil, "Simpanan Pokok", pokok);
        tambahKategori(hasil, "Simpanan Wajib", wajib);
        tambahKategori(hasil, "Simpanan Sukarela", sukarela);
        tambahKategori(hasil, "Angsuran Pokok", angsuran);
        tambahKategori(hasil, "Jasa/Bunga Pinjaman", jasa);
        hasil.baris.add(new Object[] { "TOTAL KAS MASUK", Double.valueOf(pokok + wajib + sukarela + angsuran + jasa) });
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian jurnalKasKeluar(Session session, Long pinjaman) {
        Bagian hasil = definisi(JURNAL_KAS_KELUAR);
        double penyaluran = 0;
        if (pinjaman != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
                    + "left join fetch t.anggotaKoperasi a where p.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", pinjaman).list();
            for (TransaksiKoperasi t : daftar) {
                if (Boolean.TRUE.equals(t.getAktif()) && TransaksiKoperasi.DISETUJU.equals(t.getStatus())) {
                    penyaluran += angka(t.getNilai());
                }
            }
        }
        tambahKategori(hasil, "Penyaluran Pinjaman", penyaluran);
        hasil.catatan = "Biaya operasional (ATK, RAT, inventaris, transportasi) dicatat di modul akunting dan belum ikut jurnal ini.";
        return hasil;
    }

    private static void tambahKategori(Bagian hasil, String nama, double nilai) {
        hasil.grafik.put(nama, Double.valueOf(nilai));
        hasil.baris.add(new Object[] { nama, Double.valueOf(nilai) });
    }

    @SuppressWarnings("unchecked")
    private static Bagian bukuKas(Session session, Long pinjaman) {
        Bagian hasil = definisi(BUKU_KAS);
        final List<Object[]> kejadian = new ArrayList<Object[]>();
        List<PembayaranAnggotaKoperasi> pembayaran = session.createQuery(
                "select distinct p from PembayaranAnggotaKoperasi p left join fetch p.anggotaKoperasi a").list();
        for (PembayaranAnggotaKoperasi p : pembayaran) {
            kejadian.add(new Object[] { p.getTanggal(), "Setoran/Angsuran - " + nama(p.getAnggotaKoperasi()),
                    Double.valueOf(angka(p.getNominal())), Double.valueOf(0) });
        }
        if (pinjaman != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
                    + "left join fetch t.anggotaKoperasi a where p.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", pinjaman).list();
            for (TransaksiKoperasi t : daftar) {
                if (!Boolean.TRUE.equals(t.getAktif()) || !TransaksiKoperasi.DISETUJU.equals(t.getStatus())) continue;
                kejadian.add(new Object[] { t.getTanggalTransaksi(), "Pencairan Pinjaman - " + nama(t.getAnggotaKoperasi()),
                        Double.valueOf(0), Double.valueOf(angka(t.getNilai())) });
            }
        }
        Collections.sort(kejadian, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                Date x = a[0] instanceof Date ? (Date) a[0] : null;
                Date y = b[0] instanceof Date ? (Date) b[0] : null;
                if (x == null) return y == null ? 0 : -1;
                return y == null ? 1 : x.compareTo(y);
            }
        });
        double saldo = 0;
        int no = 1;
        for (Object[] e : kejadian) {
            double masuk = angka(e[2]);
            double keluar = angka(e[3]);
            saldo += masuk - keluar;
            hasil.baris.add(new Object[] { Integer.valueOf(no++), e[0], e[1], Double.valueOf(masuk),
                    Double.valueOf(keluar), Double.valueOf(saldo) });
        }
        hasil.ringkasan.put("saldoAkhir", Double.valueOf(saldo));
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian daftarTunggakan(Session session, Long pinjaman, Date now) {
        Bagian hasil = definisi(DAFTAR_TUNGGAKAN);
        if (pinjaman == null) return hasil;
        List<TransaksiKoperasiDetail> daftar = session.createQuery(
                "select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
                + "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is null "
                + "and d.tanggal < :now and t.produkKoperasi.tipeProdukKoperasi.id = :tipe order by d.tanggal")
                .setParameter("now", now).setParameter("tipe", pinjaman).list();
        for (TransaksiKoperasiDetail d : daftar) {
            TransaksiKoperasi t = d.getTransaksiKoperasi();
            long hari = d.getTanggal() == null ? 0 : (now.getTime() - d.getTanggal().getTime()) / 86400000L;
            hasil.baris.add(new Object[] { t == null ? "-" : nama(t.getAnggotaKoperasi()),
                    Integer.valueOf(d.getKe() == null ? 0 : d.getKe()), d.getTanggal(),
                    Double.valueOf(d.getPokok() + d.getMargin()), Long.valueOf(Math.max(0, hari)),
                    t == null ? "Lancar" : t.getKolektibilitasLabel() });
        }
        hasil.ringkasan.put("jumlahTunggakan", Integer.valueOf(hasil.baris.size()));
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian simpananBerjangka(Session session, Long simpanan, Date now) {
        Bagian hasil = definisi(SIMPANAN_BERJANGKA);
        if (simpanan == null) return hasil;
        List<TransaksiKoperasi> daftar = session.createQuery(
                "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
                .setParameter("tipe", simpanan).list();
        for (TransaksiKoperasi t : daftar) {
            ProdukKoperasi p = t.getProdukKoperasi();
            String low = nama(p).toLowerCase();
            if (!(low.contains("berjangka") || low.contains("deposito"))) continue;
            Date setor = t.getTanggalTransaksi();
            Date jatuhTempo = null;
            double bulan = p == null ? 0 : angka(p.getJangkaWaktuBulan());
            if (setor != null && bulan > 0) {
                Calendar c = Calendar.getInstance();
                c.setTime(setor);
                c.add(Calendar.MONTH, (int) Math.round(bulan));
                jatuhTempo = c.getTime();
            }
            hasil.baris.add(new Object[] { nama(t.getAnggotaKoperasi()), nama(p), Double.valueOf(angka(t.getNilai())),
                    setor, jatuhTempo, jatuhTempo != null && jatuhTempo.before(now) ? "Jatuh Tempo" : "Berjalan" });
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian bukuAnggota(Session session) {
        Bagian hasil = definisi(BUKU_ANGGOTA);
        List<AnggotaKoperasi> daftar = session.createQuery(
                "select distinct a from AnggotaKoperasi a left join fetch a.jenisAnggotaKoperasi order by a.tanggal").list();
        int no = 1;
        for (AnggotaKoperasi a : daftar) {
            String jenis = a.getJenisAnggotaKoperasi() == null
                    || a.getJenisAnggotaKoperasi().getNama() == null
                    ? "-" : a.getJenisAnggotaKoperasi().getNama();
            boolean berhenti = a.getTanggalBerhenti() != null || !Boolean.TRUE.equals(a.getAktif());
            hasil.baris.add(new Object[] { Integer.valueOf(no++), nama(a), a.getAlamat(), jenis, a.getTanggal(),
                    berhenti ? "Berhenti" : "Aktif", a.getTanggalBerhenti(),
                    a.getAlasanBerhenti() == null ? "" : a.getAlasanBerhenti() });
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static Bagian promosiEkonomi(Session session, Long simpanan, Long pinjaman) {
        Bagian hasil = definisi(PROMOSI_EKONOMI);
        Map<Long, double[]> nilai = new HashMap<Long, double[]>();
        Map<Long, String> nama = new HashMap<Long, String>();
        if (simpanan != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                    + "where t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", simpanan).list();
            for (TransaksiKoperasi t : daftar) tambah(nilai, nama, t.getAnggotaKoperasi(), 0, angka(t.getNilai()));
        }
        if (pinjaman != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                    + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", pinjaman).list();
            for (TransaksiKoperasi t : daftar) if (Boolean.TRUE.equals(t.getAktif())) {
                tambah(nilai, nama, t.getAnggotaKoperasi(), 1, angka(t.getNilai()));
            }
            List<TransaksiKoperasiDetail> bayar = session.createQuery(
                    "select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
                    + "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is not null "
                    + "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe").setParameter("tipe", pinjaman).list();
            for (TransaksiKoperasiDetail d : bayar) if (d.getTransaksiKoperasi() != null) {
                tambah(nilai, nama, d.getTransaksiKoperasi().getAnggotaKoperasi(), 2, d.getMargin());
            }
        }
        List<ShuAnggota> shu = session.createQuery(
                "select distinct s from ShuAnggota s left join fetch s.anggota a").list();
        for (ShuAnggota s : shu) tambah(nilai, nama, s.getAnggota(), 3, s.getTotalShu());

        double totalSimpanan = 0, totalPinjaman = 0, totalJasa = 0, totalShu = 0;
        List<Long> ids = new ArrayList<Long>(nilai.keySet());
        Collections.sort(ids, new Comparator<Long>() {
            public int compare(Long a, Long b) { return a.compareTo(b); }
        });
        for (Long id : ids) {
            double[] v = nilai.get(id);
            hasil.baris.add(new Object[] { nama.get(id), Double.valueOf(v[0]), Double.valueOf(v[1]),
                    Double.valueOf(v[2]), Double.valueOf(v[3]) });
            totalSimpanan += v[0]; totalPinjaman += v[1]; totalJasa += v[2]; totalShu += v[3];
        }
        hasil.ringkasan.put("anggotaTerlayani", Integer.valueOf(nilai.size()));
        hasil.ringkasan.put("totalSimpanan", Double.valueOf(totalSimpanan));
        hasil.ringkasan.put("totalPinjaman", Double.valueOf(totalPinjaman));
        hasil.ringkasan.put("totalJasa", Double.valueOf(totalJasa));
        hasil.ringkasan.put("totalShu", Double.valueOf(totalShu));
        return hasil;
    }

    private static void tambah(Map<Long, double[]> nilai, Map<Long, String> nama,
            AnggotaKoperasi anggota, int posisi, double jumlah) {
        if (anggota == null || anggota.getId() == null) return;
        double[] baris = nilai.get(anggota.getId());
        if (baris == null) {
            baris = new double[4];
            nilai.put(anggota.getId(), baris);
            nama.put(anggota.getId(), nama(anggota));
        }
        baris[posisi] += jumlah;
    }

    @SuppressWarnings("unchecked")
    private static Bagian bungaSimpanan(Session session, Long simpanan, Date now) {
        Bagian hasil = definisi(BUNGA_SIMPANAN);
        Calendar periode = Calendar.getInstance();
        periode.setTime(now);
        int tahun = periode.get(Calendar.YEAR);
        int bulan = periode.get(Calendar.MONTH);
        int jumlahHari = periode.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar awal = Calendar.getInstance();
        awal.clear(); awal.set(tahun, bulan, 1, 0, 0, 0);
        Calendar berikut = Calendar.getInstance();
        berikut.clear(); berikut.set(tahun, bulan, 1, 0, 0, 0); berikut.add(Calendar.MONTH, 1);
        LinkedHashMap<String, Akum> peta = new LinkedHashMap<String, Akum>();
        if (simpanan != null) {
            List<TransaksiKoperasi> daftar = session.createQuery(
                    "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
                    + "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
                    .setParameter("tipe", simpanan).list();
            for (TransaksiKoperasi t : daftar) {
                ProdukKoperasi p = t.getProdukKoperasi();
                AnggotaKoperasi a = t.getAnggotaKoperasi();
                if (p == null || p.getId() == null || a == null || a.getId() == null) continue;
                String low = nama(p).toLowerCase();
                if (low.contains("pokok") || low.contains("wajib")) continue;
                Date tanggal = t.getTanggalTransaksi() == null ? t.getTanggal() : t.getTanggalTransaksi();
                if (tanggal == null || !tanggal.before(berikut.getTime())) continue;
                String key = a.getId() + "#" + p.getId();
                Akum ak = peta.get(key);
                if (ak == null) {
                    ak = new Akum(); ak.anggota = nama(a); ak.produk = nama(p);
                    ak.metode = p.getMetodeBungaSimpanan(); ak.persen = angka(p.getBungaSimpananPersen());
                    peta.put(key, ak);
                }
                if (tanggal.before(awal.getTime())) ak.saldoAwal += angka(t.getNilai());
                else {
                    Calendar hari = Calendar.getInstance(); hari.setTime(tanggal);
                    ak.hari.add(Integer.valueOf(hari.get(Calendar.DAY_OF_MONTH)));
                    ak.nominal.add(Double.valueOf(angka(t.getNilai())));
                }
            }
        }
        double total = 0;
        boolean tanpaBunga = false;
        for (Akum ak : peta.values()) {
            int[] hari = new int[ak.hari.size()];
            double[] nominal = new double[ak.nominal.size()];
            for (int i = 0; i < hari.length; i++) {
                hari[i] = ak.hari.get(i).intValue(); nominal[i] = ak.nominal.get(i).doubleValue();
            }
            double[] saldo = BungaSimpananUtil.saldoHarian(ak.saldoAwal, jumlahHari, hari, nominal);
            double terendah = BungaSimpananUtil.saldoTerendah(saldo);
            double rata = BungaSimpananUtil.saldoRataRata(saldo);
            double bunga = BungaSimpananUtil.hitungBunga(ak.metode, saldo, ak.persen);
            total += bunga;
            if (ak.persen <= 0) tanpaBunga = true;
            hasil.baris.add(new Object[] { ak.anggota, ak.produk, BungaSimpananUtil.label(ak.metode),
                    Double.valueOf(ak.persen), Double.valueOf(terendah), Double.valueOf(rata), Double.valueOf(bunga) });
        }
        Collections.sort(hasil.baris, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return String.valueOf(a[0]).compareToIgnoreCase(String.valueOf(b[0]));
            }
        });
        hasil.ringkasan.put("totalBunga", Double.valueOf(total));
        hasil.ringkasan.put("produkTanpaBunga", Boolean.valueOf(tanpaBunga));
        hasil.ringkasan.put("tahun", Integer.valueOf(tahun));
        hasil.ringkasan.put("bulan", Integer.valueOf(bulan + 1));
        return hasil;
    }

    @SuppressWarnings("unchecked")
    public static Surat suratTeguran(Session session, Date sekarang) {
        Date now = sekarang == null ? new Date() : sekarang;
        Long pinjaman = ConstantValues.PINJAMAN == null ? null : ConstantValues.PINJAMAN.getId();
        if (pinjaman == null) return new Surat(0, "");
        List<TransaksiKoperasiDetail> daftar = session.createQuery(
                "select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
                + "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is null "
                + "and d.tanggal < :now and t.produkKoperasi.tipeProdukKoperasi.id = :tipe "
                + "order by t.anggotaKoperasi.id, d.tanggal")
                .setParameter("now", now).setParameter("tipe", pinjaman).list();
        LinkedHashMap<Long, List<TransaksiKoperasiDetail>> perAnggota =
                new LinkedHashMap<Long, List<TransaksiKoperasiDetail>>();
        Map<Long, AnggotaKoperasi> anggota = new HashMap<Long, AnggotaKoperasi>();
        for (TransaksiKoperasiDetail d : daftar) {
            TransaksiKoperasi t = d.getTransaksiKoperasi();
            AnggotaKoperasi a = t == null ? null : t.getAnggotaKoperasi();
            if (a == null || a.getId() == null) continue;
            List<TransaksiKoperasiDetail> baris = perAnggota.get(a.getId());
            if (baris == null) {
                baris = new ArrayList<TransaksiKoperasiDetail>();
                perAnggota.put(a.getId(), baris); anggota.put(a.getId(), a);
            }
            baris.add(d);
        }
        StringBuilder html = new StringBuilder();
        boolean pertama = true;
        for (Map.Entry<Long, List<TransaksiKoperasiDetail>> e : perAnggota.entrySet()) {
            if (!pertama) html.append(SuratTeguranHelper.pemisahHalaman());
            html.append(SuratTeguranHelper.buildSurat(anggota.get(e.getKey()), e.getValue(), now));
            pertama = false;
        }
        return new Surat(perAnggota.size(), html.toString());
    }

    private static double angka(Object nilai) {
        return nilai instanceof Number ? ((Number) nilai).doubleValue() : 0.0;
    }

    private static String nama(Object entity) {
        if (entity instanceof AnggotaKoperasi) {
            String nama = ((AnggotaKoperasi) entity).getNama();
            return nama == null || nama.trim().length() == 0 ? "-" : nama;
        }
        if (entity instanceof ProdukKoperasi) {
            String nama = ((ProdukKoperasi) entity).getNama();
            return nama == null || nama.trim().length() == 0 ? "-" : nama;
        }
        return "-";
    }

    private static final class Akum {
        String anggota;
        String produk;
        String metode;
        double persen;
        double saldoAwal;
        final List<Integer> hari = new ArrayList<Integer>();
        final List<Double> nominal = new ArrayList<Double>();
    }
}
