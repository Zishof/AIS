package ais.action.master.koperasi.helper;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.inventory.SesiKasKasir;
import ais.database.model.inventory.Toko;

/**
 * <h2>SesiKasUtil — mesin pusat Sesi Kas Kasir (Buka/Tutup Kas).</h2>
 *
 * <p>
 * Kumpulan metode statik yang menampung SELURUH logika buka/tutup kas dan perhitungan penjualan
 * tunai/non-tunai per sesi, sehingga versi <b>JSP</b> ({@code kantin/kas/*}) dan versi <b>ZKoss</b>
 * ({@code KasKasirZkAction}) memakai kode yang sama — tidak ada duplikasi aturan, dan pemeliharaan
 * cukup di satu tempat. Perhitungan penjualan mencocokkan transaksi POS ke kasir melalui kolom
 * {@code oleh} (bisa nama atau id pengguna) pada {@code koperasi.pembelian_anggota_koperasi} dalam
 * rentang waktu buka..tutup, sehingga tidak perlu menambah relasi baru pada tabel penjualan.
 * </p>
 *
 * <p>
 * <b>Sesi:</b> semua metode menerima {@link Session} milik pemanggil (framework/ZK atau JSP) dan
 * menyimpan lewat {@link Common#refreshSaveOrUpdate(Session, ais.database.model.GeneralValueObject)};
 * util ini TIDAK membuka/menutup sesi sendiri. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul kas kasir)
 * @see SesiKasKasir
 */
public final class SesiKasUtil {

	private SesiKasUtil() {
	}

	private static String joinCaraPembayaran() {
		return " from koperasi.pembelian_anggota_koperasi h"
				+ " left join koperasi.cara_pembayaran_koperasi c1 on c1.id=h.cara_pembayaran_koperasi"
				+ " left join koperasi.cara_pembayaran_koperasi c2 on c2.id=h.cara_pembayaran_koperasi_2"
				+ " left join koperasi.cara_pembayaran_koperasi c3 on c3.id=h.cara_pembayaran_koperasi_3"
				+ " left join koperasi.cara_pembayaran_koperasi c4 on c4.id=h.cara_pembayaran_koperasi_4"
				+ " left join koperasi.cara_pembayaran_koperasi c5 on c5.id=h.cara_pembayaran_koperasi_5";
	}

	private static String nilaiPembayaran(boolean tunai) {
		String cocok = tunai ? "" : "not ";
		String n1 = "greatest(0,coalesce(h.total_biaya,0)-coalesce(h.nominal_bayar_2,0)"
				+ "-coalesce(h.nominal_bayar_3,0)-coalesce(h.nominal_bayar_4,0)-coalesce(h.nominal_bayar_5,0))";
		StringBuilder jumlah = new StringBuilder();
		for (int slot = 1; slot <= 5; slot++) {
			if (slot > 1) jumlah.append("+");
			String nominal = slot == 1 ? n1 : "coalesce(h.nominal_bayar_" + slot + ",0)";
			jumlah.append("case when not coalesce(c").append(slot).append(".masuk_sebagai_hutang,false) and ")
					.append(cocok).append("coalesce(c").append(slot)
					.append(".ada_kembalian,c").append(slot).append(".nama ilike '%tunai%') then ")
					.append(nominal).append(" else 0 end");
		}
		String tersimpan = tunai ? "h.bayar_tunai" : "h.bayar_non_tunai";
		return "case when coalesce(h.bayar_tunai,0)=0 and coalesce(h.bayar_non_tunai,0)=0"
				+ " then (" + jumlah + ") else coalesce(" + tersimpan + ",0) end";
	}

	/**
	 * Memuat objek sesi kas yang masih TERBUKA milik kasir tertentu (opsional dibatasi toko), atau
	 * {@code null} bila tidak ada.
	 *
	 * <p><b>Kenapa Criteria (object query), BUKAN raw SQL.</b> Sebelumnya method ini (dan {@link
	 * #idSesiTerbuka}) memakai {@code session.createSQLQuery(...)} mentah -- selain rawan salah ketik
	 * nama kolom (mis. insiden {@code waktu_buka} vs {@code waktubuka} saat verifikasi manual lewat
	 * psql), raw SQL di Hibernate TIDAK ikut auto-flush perubahan tertunda pada sesi yang sama sebelum
	 * dieksekusi (beda dari Criteria/HQL yang otomatis flush lebih dulu) -- celah korektnes yang tak
	 * perlu ada utk query sesederhana ini. Criteria langsung memetakan ke properti entitas ({@code
	 * kasirNama}/{@code kasirUserId}/{@code status}/{@code toko}), jadi tetap benar walau nama kolom
	 * fisik berubah di masa depan.</p>
	 *
	 * <p><b>Kenapa {@code kasirNama}/{@code kasirUserId}, BUKAN {@code oleh}/{@code olehId}.</b> Lihat
	 * javadoc {@link SesiKasKasir#getKasirNama()} -- {@code oleh}/{@code olehId} adalah metadata audit
	 * generik yang bisa ditimpa interceptor kapan saja, tidak aman dipakai sbg kunci pencarian data
	 * bisnis.</p>
	 */
	public static SesiKasKasir sesiTerbuka(Session session, String kasirNama, String kasirUserId, Long tokoId) {
		Criteria c = session.createCriteria(SesiKasKasir.class)
				.add(Restrictions.or(Restrictions.eq("kasirNama", kasirNama), Restrictions.eq("kasirUserId", kasirUserId)))
				.add(Restrictions.or(Restrictions.eq("status", SesiKasKasir.STATUS_BUKA), Restrictions.isNull("status")))
				.addOrder(Order.desc("id"))
				.setMaxResults(1);
		if (tokoId != null) {
			c.add(Restrictions.eq("toko", session.load(Toko.class, tokoId)));
		}
		return (SesiKasKasir) c.uniqueResult();
	}

	/**
	 * Mengembalikan id sesi kas yang masih TERBUKA milik kasir tertentu (opsional dibatasi toko),
	 * atau {@code null} bila tidak ada. Tipis di atas {@link #sesiTerbuka} -- SATU query, bukan dua
	 * (dulu method ini query sendiri lalu {@link #sesiTerbuka} query ULANG + {@code session.get}).
	 */
	public static Long idSesiTerbuka(Session session, String kasirNama, String kasirUserId, Long tokoId) {
		SesiKasKasir sesi = sesiTerbuka(session, kasirNama, kasirUserId, tokoId);
		return sesi == null ? null : sesi.getId();
	}

	/**
	 * Menghitung total penjualan POS oleh kasir dalam rentang waktu. Parameter method ini diisi dari
	 * {@link SesiKasKasir#getKasirNama()}/{@link SesiKasKasir#getKasirUserId()} pemanggil (lihat
	 * {@link #tutup}), TIDAK lagi dari {@code oleh}/{@code olehId} milik sesi.
	 *
	 * <p><b>Gap-closure ditemukan &amp; diperbaiki bersamaan (2026-08-12).</b> Query ini SEBELUMNYA
	 * cuma cocokkan {@code oleh}/{@code olehId} milik {@code koperasi.pembelian_anggota_koperasi} --
	 * kolom itu diisi otomatis oleh {@code AuditTimestampInterceptor} (metadata audit generik, BUKAN
	 * data bisnis, sama kelasnya dgn root cause bug "Kas Terbuka tapi checkout ditolak" yg baru
	 * diperbaiki di commit 869f858d). Javadoc {@link
	 * ais.database.model.koperasi.PembelianAnggotaKoperasi#getKasirLoginNama()} sendiri menegaskan
	 * {@code oleh} SELALU jatuh ke fallback {@code "external_update"} utk permintaan lewat
	 * {@code PosApi} (Electron/Flutter, TANPA sesi browser) -- artinya utk SEMUA transaksi POS yg
	 * dibuat lewat {@code KantinHelper.bayar()} (bukan ZK/JSP), query lama ini TIDAK PERNAH cocok,
	 * shg Total Tunai/Non-Tunai saat Tutup Kas SELALU nol utk kasir Electron/Flutter. Ditambahkan
	 * pencocokan lewat {@code kasir_login_nama} (diisi eksplisit &amp; andal di {@code bayar()}, lihat
	 * javadoc di atas) sbg jalur TAMBAHAN -- {@code oleh}/{@code olehId} tetap dipertahankan sbg
	 * fallback utk baris lama/ZK-JSP (browser session, {@code oleh} bisa berisi nama asli), jadi tidak
	 * ada regresi utk jalur yg SUDAH benar.</p>
	 *
	 * @return array {@code [tunai, nonTunai]}.
	 */
	public static double[] hitungPenjualan(Session session, String oleh, String olehId, Long tokoId, Date dari, Date sampai) {
		try {
			StringBuilder sb = new StringBuilder("select coalesce(sum(")
					.append(nilaiPembayaran(true)).append("),0),coalesce(sum(")
					.append(nilaiPembayaran(false)).append("),0)").append(joinCaraPembayaran())
					.append(" where (h.oleh=:o or h.oleh=:i or h.kasir_login_nama=:o or h.kasir_login_nama=:i)")
					.append(" and h.tanggal_pembayaran>=:dari and h.tanggal_pembayaran<=:sampai ");
			if (tokoId != null) {
				sb.append(" and h.toko=:t ");
			}
			SQLQuery q = session.createSQLQuery(sb.toString());
			q.setParameter("o", oleh);
			q.setParameter("i", olehId);
			q.setParameter("dari", dari);
			q.setParameter("sampai", sampai);
			if (tokoId != null) {
				q.setParameter("t", tokoId);
			}
			Object[] r = (Object[]) q.uniqueResult();
			return new double[] { ((Number) r[0]).doubleValue(), ((Number) r[1]).doubleValue() };
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "gagal menghitung penjualan sesi kas (fallback identitas)");
			throw new IllegalStateException("Ringkasan penjualan sesi kas belum dapat dihitung. Data transaksi tidak diubah; coba muat ulang atau hubungi admin dengan Informasi Teknis.", e);
		}
	}

	/** Jalur utama: transaksi baru memakai FK sesi; transaksi lama tetap dihitung lewat identitas. */
	public static double[] hitungPenjualan(Session session, SesiKasKasir sesi, Date sampai) {
		try {
			StringBuilder sb = new StringBuilder("select coalesce(sum(")
					.append(nilaiPembayaran(true)).append("),0),coalesce(sum(")
					.append(nilaiPembayaran(false)).append("),0)").append(joinCaraPembayaran())
					.append(" where (h.sesi_kas_kasir=:s or (h.sesi_kas_kasir is null")
					.append(" and (h.oleh=:o or h.oleh=:i or h.kasir_login_nama=:o or h.kasir_login_nama=:i)")
					.append(" and h.tanggal_pembayaran>=:dari and h.tanggal_pembayaran<=:sampai)) ");
			if (sesi.getToko() != null) sb.append(" and h.toko=:t ");
			SQLQuery q = session.createSQLQuery(sb.toString());
			q.setParameter("s", sesi.getId());
			q.setParameter("o", sesi.getKasirNama());
			q.setParameter("i", sesi.getKasirUserId());
			q.setParameter("dari", sesi.getWaktuBuka());
			q.setParameter("sampai", sampai);
			if (sesi.getToko() != null) q.setParameter("t", sesi.getToko().getId());
			Object[] r = (Object[]) q.uniqueResult();
			return new double[] { ((Number) r[0]).doubleValue(), ((Number) r[1]).doubleValue() };
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "gagal menghitung penjualan sesi kas id=" + sesi.getId());
			throw new IllegalStateException("Ringkasan penjualan sesi kas belum dapat dihitung. Data transaksi tidak diubah; coba muat ulang atau hubungi admin dengan Informasi Teknis.", e);
		}
	}

	/**
	 * Mencari sesi (status apa pun -- BUKA atau TUTUP) lewat {@link SesiKasKasir#getKode()} --
	 * fondasi idempotensi utk fitur "Sesi Kasir offline-first" (lihat javadoc {@link
	 * SesiKasKasir#getKode()}). {@code null}/kosong selalu mengembalikan {@code null} (bukan
	 * mencocokkan baris ber-kode null -- sengaja, supaya pemanggil lama yang tak kirim kode tidak
	 * pernah "menabrak" baris lama secara tak sengaja).
	 */
	public static SesiKasKasir cariByKode(Session session, String kode) {
		if (kode == null || kode.trim().isEmpty()) {
			return null;
		}
		return (SesiKasKasir) session.createCriteria(SesiKasKasir.class)
				.add(Restrictions.eq("kode", kode))
				.uniqueResult();
	}

	/**
	 * Membuka kas: membuat sesi baru berstatus BUKA dengan modal awal. Mengembalikan sesi yang dibuat.
	 * Pemanggil sebaiknya memastikan belum ada sesi terbuka (lihat {@link #idSesiTerbuka}).
	 */
	public static SesiKasKasir buka(Session session, Toko toko, String kasirNama, String kasirUserId, double modalAwal, String keterangan) {
		return buka(session, toko, kasirNama, kasirUserId, modalAwal, keterangan, null, null);
	}

	/**
	 * Sama seperti {@link #buka(Session, Toko, String, String, double, String)}, DITAMBAH dua
	 * parameter utk alur "Sesi Kasir offline-first" (Desktop/Android menyimpan lokal dulu, sinkron
	 * belakangan -- lihat javadoc {@link SesiKasKasir#getKode()}):
	 *
	 * @param kode           idempotensi klien ({@code null} = perilaku lama, selalu baris baru --
	 *                       dipakai pemanggil ZK/JSP yang tak butuh retry-safety). Pemanggil WAJIB
	 *                       memastikan sendiri lewat {@link #cariByKode} belum ada baris dgn kode ini
	 *                       SEBELUM memanggil method ini (method ini TIDAK mengecek ulang) -- dipisah
	 *                       sengaja supaya pemanggil (KantinHelper) bisa membedakan "baris baru dibuat"
	 *                       vs "baris lama ditemukan &amp; dikembalikan apa adanya" utk pesan hasil yang
	 *                       tepat ke klien.
	 * @param waktuBukaKlien waktu buka SEBENARNYA di perangkat klien ({@code null} = pakai waktu
	 *                       server saat ini, perilaku lama) -- penting utk sinkron yang tertunda
	 *                       (mis. baru online lagi 10 menit setelah kas sebenarnya dibuka offline)
	 *                       supaya {@code waktuBuka} yang tercatat tetap AKURAT, bukan waktu sinkron.
	 */
	public static SesiKasKasir buka(Session session, Toko toko, String kasirNama, String kasirUserId, double modalAwal,
			String keterangan, String kode, Date waktuBukaKlien) {
		SesiKasKasir o = new SesiKasKasir();
		o.setToko(toko);
		o.setKasirNama(kasirNama);
		o.setKasirUserId(kasirUserId);
		o.setWaktuBuka(waktuBukaKlien != null ? waktuBukaKlien : new Date());
		o.setModalAwal(Double.valueOf(modalAwal));
		o.setStatus(SesiKasKasir.STATUS_BUKA);
		o.setKeterangan(keterangan);
		o.setKode(kode);
		Common.refreshSaveOrUpdate(session, o);
		return o;
	}

	/**
	 * Menutup kas: menghitung tunai/non-tunai sepanjang sesi, menyimpan uang fisik, dan menghitung
	 * selisih = uangFisik − (modalAwal + tunai). Mengembalikan selisih.
	 */
	public static double tutup(Session session, SesiKasKasir sesi, double uangFisik, String keterangan) {
		return tutup(session, sesi, uangFisik, keterangan, null);
	}

	/**
	 * Sama seperti {@link #tutup(Session, SesiKasKasir, double, String)}, DITAMBAH
	 * {@code waktuTutupKlien} (waktu tutup SEBENARNYA di perangkat klien, {@code null} = waktu server
	 * saat ini) -- utk alur offline-first, dipakai sbg batas atas rentang {@link #hitungPenjualan}
	 * SUPAYA transaksi yang (kebetulan) tersinkron ke server SETELAH momen tutup lokal yg sebenarnya
	 * (tapi SEBELUM permintaan tutup ini akhirnya sempat disinkronkan) tidak ikut terhitung ke sesi
	 * yang sudah ditutup kasir -- tanpa ini, {@code new Date()} (waktu SINKRON, bisa jauh lebih telat
	 * drpd waktu tutup sungguhan) akan keliru menyertakan transaksi-transaksi itu.
	 */
	public static double tutup(Session session, SesiKasKasir sesi, double uangFisik, String keterangan, Date waktuTutupKlien) {
		Date sampai = waktuTutupKlien != null ? waktuTutupKlien : new Date();
		double[] jual = hitungPenjualan(session, sesi, sampai);
		double seharusnya = sesi.getModalAwal().doubleValue() + jual[0];
		double selisih = uangFisik - seharusnya;
		sesi.setTotalTunai(Double.valueOf(jual[0]));
		sesi.setTotalNonTunai(Double.valueOf(jual[1]));
		sesi.setUangFisik(Double.valueOf(uangFisik));
		sesi.setSelisih(Double.valueOf(selisih));
		sesi.setWaktuTutup(sampai);
		sesi.setStatus(SesiKasKasir.STATUS_TUTUP);
		if (keterangan != null && keterangan.trim().length() > 0) {
			sesi.setKeterangan(keterangan);
		}
		Common.refreshSaveOrUpdate(session, sesi);
		return selisih;
	}
}
