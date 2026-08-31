package ais.action.master.koperasi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;

/**
 * Mesin perhitungan diskon &amp; cashback belanja kantin.
 *
 * <p><b>Mengapa kelas ini ada.</b> Aturan diskon semula tertanam di dalam
 * composer ZK {@code BerandaAnggotaKantinAction}: aturan dimuat, dievaluasi,
 * lalu hasilnya dikirim ke {@code KantinHelper.bayar} sebagai bagian payload.
 * Ketika layar belanja dilayani juga secara native, menyalin logika ini ke
 * controller kedua berarti ada DUA implementasi hitungan uang yang pasti akan
 * menyimpang seiring waktu — pelanggan yang sama bisa memperoleh potongan
 * berbeda hanya karena memakai layar yang berbeda. Karena itu logikanya
 * dipindahkan ke sini apa adanya dan kedua pemanggil memakai kelas ini.</p>
 *
 * <p><b>Mengapa evaluasi wajib di server.</b> Pada ZK, diskon dihitung di
 * composer yang juga berjalan di server, sehingga nilainya tepercaya. Kontrak
 * native menerima permintaan dari klien yang tidak tepercaya; bila klien boleh
 * mengirimkan angka diskon/cashback-nya sendiri, siapa pun dapat membeli
 * barang dengan potongan yang dikarangnya. Controller native karena itu HANYA
 * menerima (produk, jumlah) dari klien dan memanggil mesin ini untuk
 * menentukan potongannya.</p>
 *
 * <p>Perilaku dipertahankan persis seperti versi ZK: urutan prioritas,
 * penggabungan aturan, grup eksklusif, dasar perhitungan, batas potongan, dan
 * pembatasan cashback terhadap nilai baris.</p>
 */
public final class KantinDiskonEngine {

    /** Satu baris keranjang yang akan dinilai. Field hasil diisi oleh {@link #evaluasi}. */
    public static final class Baris {
        public final Long produkId;
        public final Long tokoId;
        public final double harga;
        public final int jumlah;

        /** Hasil evaluasi: potongan langsung untuk baris ini. */
        public double diskon;
        /** Hasil evaluasi: cashback untuk baris ini. */
        public double cashback;
        /** Hasil evaluasi: aturan pertama yang dipakai (null bila hanya dari grup). */
        public Long aturanDiskonId;

        public Baris(Long produkId, Long tokoId, double harga, int jumlah) {
            this.produkId = produkId;
            this.tokoId = tokoId;
            this.harga = harga;
            this.jumlah = jumlah;
        }
    }

    /** Satu aturan diskon, baik dari {@code aturan_diskon} maupun {@code grup_aturan_diskon}. */
    public static final class Aturan {
        public Long aturanId, produkId, tokoId, jenisId, tipeId;
        public boolean berlakuSemua, potonganLangsung, khususMember, sumberGrup, dapatDigabung, aktivasiManual;
        public double persen, maxPot, nominal, cashbackTetap;
        public int prioritas = 100;
        public String dasarPerhitungan = "SETELAH_DISKON", grupEksklusif, jenisMemberJson, tipeMemberJson, hariAktif;
        public Date tglMulai, tglSelesai;
    }

    private KantinDiskonEngine() { }

    // ------------------------------------------------------------------ muat

    /**
     * Muat seluruh aturan aktif: aturan tunggal lalu aturan grup, kemudian
     * diurutkan menurut prioritas seperti pada layar ZK.
     *
     * <p>Kegagalan query tidak dilemparkan: bila tabel aturan bermasalah,
     * belanja tetap dapat berjalan tanpa diskon, bukannya gagal total. Itu
     * pilihan yang disengaja karena aturan diskon bersifat tambahan.</p>
     */
    public static List<Aturan> muatAturan(Session session) {
        List<Aturan> list = new ArrayList<Aturan>();
        try {
            for (Object[] r : rows(session, "SELECT id, produk, toko, jenis_anggota, tipe_anggota, berlaku_semua_member, "
                    + "persentase, maksimal_potongan, nominal, potongan_langsung, tanggal_mulai, tanggal_selesai,hari_aktif,COALESCE(aktivasi_manual,false),"
                    + "COALESCE(prioritas,100),COALESCE(dapat_digabung,false),COALESCE(dasar_perhitungan,'SETELAH_DISKON'),COALESCE(grup_eksklusif,'') "
                    + "FROM koperasi.aturan_diskon WHERE aktif = true")) {
                Aturan x = new Aturan();
                x.aturanId = lng(r[0]);
                x.produkId = lng(r[1]);
                x.tokoId = lng(r[2]);
                x.jenisId = lng(r[3]);
                x.tipeId = lng(r[4]);
                x.berlakuSemua = bool(r[5]);
                x.persen = num(r[6]);
                x.maxPot = num(r[7]);
                x.nominal = num(r[8]);
                x.potonganLangsung = bool(r[9]);
                x.tglMulai = date(r[10]);
                x.tglSelesai = date(r[11]);
                x.hariAktif = str(r[12]);
                x.aktivasiManual = bool(r[13]);
                x.prioritas = ((Number) r[14]).intValue();
                x.dapatDigabung = bool(r[15]);
                x.dasarPerhitungan = str(r[16]);
                x.grupEksklusif = str(r[17]);
                list.add(x);
            }
            for (Object[] r : rows(session, "SELECT g.id,d.produk,g.toko,g.jenis_anggota,g.tipe_anggota,"
                    + "COALESCE(g.berlaku_semua_member,NOT COALESCE(g.khusus_member,false)),g.persentase,g.maksimal_potongan,g.nominal,"
                    + "COALESCE(g.potongan_langsung,true),g.tanggal_mulai,g.tanggal_selesai,g.hari_aktif,COALESCE(g.khusus_member,false),"
                    + "COALESCE(g.jenis_member_json,'[]'),COALESCE(g.tipe_member_json,'[]'),COALESCE(g.cashback,0),COALESCE(g.prioritas,100),"
                    + "COALESCE(g.dapat_digabung,false),COALESCE(g.dasar_perhitungan,'SETELAH_DISKON'),COALESCE(g.grup_eksklusif,'') "
                    + "FROM koperasi.grup_aturan_diskon g JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id AND COALESCE(d.aktif,true) WHERE COALESCE(g.aktif,true)")) {
                Aturan x = new Aturan();
                x.aturanId = lng(r[0]);
                x.produkId = lng(r[1]);
                x.tokoId = lng(r[2]);
                x.jenisId = lng(r[3]);
                x.tipeId = lng(r[4]);
                x.berlakuSemua = bool(r[5]);
                x.persen = num(r[6]);
                x.maxPot = num(r[7]);
                x.nominal = num(r[8]);
                x.potonganLangsung = bool(r[9]);
                x.tglMulai = date(r[10]);
                x.tglSelesai = date(r[11]);
                x.hariAktif = str(r[12]);
                x.khususMember = bool(r[13]);
                x.jenisMemberJson = str(r[14]);
                x.tipeMemberJson = str(r[15]);
                x.cashbackTetap = num(r[16]);
                x.prioritas = ((Number) r[17]).intValue();
                x.dapatDigabung = bool(r[18]);
                x.dasarPerhitungan = str(r[19]);
                x.grupEksklusif = str(r[20]);
                x.sumberGrup = true;
                list.add(x);
            }
            Collections.sort(list, new Comparator<Aturan>() {
                public int compare(Aturan a, Aturan b) {
                    if (a.prioritas != b.prioritas) return a.prioritas > b.prioritas ? -1 : 1;
                    if (a.persen != b.persen) return a.persen > b.persen ? -1 : 1;
                    if (a.nominal != b.nominal) return a.nominal > b.nominal ? -1 : 1;
                    return a.aturanId.compareTo(b.aturanId);
                }
            });
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "KantinDiskonEngine.muatAturan");
        }
        return list;
    }

    // -------------------------------------------------------------- evaluasi

    /**
     * Tentukan diskon &amp; cashback satu baris keranjang.
     *
     * @param b       baris yang dinilai; field hasilnya ditimpa
     * @param aturan  hasil {@link #muatAturan}
     * @param jenisId jenis anggota pembeli (boleh null)
     * @param tipeId  tipe anggota pembeli (boleh null)
     * @param saat    waktu acuan berlakunya aturan
     */
    public static void evaluasi(Baris b, List<Aturan> aturan, Long jenisId, Long tipeId, Date saat) {
        b.diskon = 0;
        b.cashback = 0;
        b.aturanDiskonId = null;
        if (aturan == null || aturan.isEmpty()) {
            return;
        }
        Date now = saat == null ? new Date() : saat;
        List<Aturan> eligible = new ArrayList<Aturan>();
        for (Aturan r : aturan) {
            if (r.aktivasiManual) continue;
            if (r.produkId != null && !r.produkId.equals(b.produkId)) continue;
            if (r.tokoId != null && !r.tokoId.equals(b.tokoId)) continue;
            if (r.tglMulai != null && r.tglMulai.after(now)) continue;
            if (r.tglSelesai != null && r.tglSelesai.before(now)) continue;
            if (!ais.common.HariAktifUtil.aktifPadaHari(r.hariAktif, now)) continue;
            if (!jsonIdMemuat(r.jenisMemberJson, jenisId) || !jsonIdMemuat(r.tipeMemberJson, tipeId)) continue;
            if (!r.berlakuSemua || r.khususMember) {
                if (r.jenisId != null && !r.jenisId.equals(jenisId)) continue;
                if (r.tipeId != null && !r.tipeId.equals(tipeId)) continue;
            }
            eligible.add(r);
        }
        if (eligible.isEmpty()) {
            return;
        }
        final double itemTotal = b.harga * b.jumlah;
        final int jumlahItem = b.jumlah;
        Collections.sort(eligible, new Comparator<Aturan>() {
            public int compare(Aturan a, Aturan c) {
                if (a.prioritas != c.prioritas) return a.prioritas > c.prioritas ? -1 : 1;
                double va = nilaiPotensial(a, itemTotal, jumlahItem), vb = nilaiPotensial(c, itemTotal, jumlahItem);
                if (va != vb) return va > vb ? -1 : 1;
                return a.aturanId.compareTo(c.aturanId);
            }
        });
        Aturan pertama = eligible.get(0);
        Set<String> eksklusif = new HashSet<String>();
        for (int ri = 0; ri < eligible.size(); ri++) {
            Aturan applied = eligible.get(ri);
            // Aturan kedua dan seterusnya hanya ikut bila BOTH boleh digabung.
            if (ri > 0 && (!pertama.dapatDigabung || !applied.dapatDigabung)) break;
            String eks = applied.grupEksklusif == null ? "" : applied.grupEksklusif.trim();
            if (eks.length() > 0 && eksklusif.contains(eks)) continue;
            if (eks.length() > 0) eksklusif.add(eks);
            double dasar = "HARGA_AWAL".equals(applied.dasarPerhitungan) ? itemTotal : Math.max(0, itemTotal - b.diskon);
            double disc = 0;
            if (applied.persen > 0) {
                disc = dasar * (applied.persen / 100.0);
            } else if (applied.nominal > 0) {
                disc = applied.nominal * b.jumlah;
                if (disc > dasar) disc = dasar;
            }
            if (applied.maxPot > 0 && disc > applied.maxPot) {
                disc = applied.maxPot;
            }
            if (applied.potonganLangsung) {
                b.diskon += Math.min(Math.max(0, itemTotal - b.diskon), disc);
            } else {
                b.cashback += disc;
            }
            if (applied.cashbackTetap > 0) b.cashback += Math.min(itemTotal, applied.cashbackTetap * b.jumlah);
            if (b.aturanDiskonId == null && !applied.sumberGrup) b.aturanDiskonId = applied.aturanId;
        }
        // Cashback tidak boleh melebihi sisa nilai baris setelah potongan langsung.
        b.cashback = Math.min(Math.max(0, itemTotal - b.diskon), b.cashback);
    }

    /** Perkiraan nilai sebuah aturan; dipakai hanya untuk mengurutkan kandidat. */
    static double nilaiPotensial(Aturan r, double total, int jumlah) {
        double v = r.persen > 0 ? total * (r.persen / 100d) : r.nominal * Math.max(1, jumlah);
        if (r.maxPot > 0 && v > r.maxPot) v = r.maxPot;
        return Math.max(0, v) + Math.max(0, r.cashbackTetap * Math.max(1, jumlah));
    }

    /**
     * true bila daftar id JSON memuat nilai — daftar kosong berarti "tanpa
     * batasan", sehingga aturan berlaku untuk semua.
     */
    static boolean jsonIdMemuat(String json, Long nilai) {
        if (json == null || json.trim().length() == 0 || "[]".equals(json.trim())) return true;
        if (nilai == null) return false;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                if (String.valueOf(nilai).equals(String.valueOf(a.get(i)))) return true;
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "filter member grup diskon toko online");
        }
        return false;
    }

    // ------------------------------------------------------------------ util

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Session session, String sql) {
        SQLQuery q = session.createSQLQuery(sql);
        return q.list();
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : Long.valueOf(((Number) o).longValue());
    }

    private static boolean bool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return ((Boolean) o).booleanValue();
        String s = o.toString().trim();
        return s.equals("t") || s.equalsIgnoreCase("true") || s.equals("1");
    }

    private static Date date(Object o) {
        return o instanceof Date ? (Date) o : null;
    }
}
