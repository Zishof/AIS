package ais.action.master.koperasi.helper;

import java.util.Date;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

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

	private static String cakupanTransaksi(SesiKasKasir sesi) {
		String sql = "(h.sesi_kas_kasir=? or (h.sesi_kas_kasir is null and "
				+ "(h.kasir_login_nama=? or h.kasir_login_nama=?) "
				+ "and h.tanggal_pembayaran>=? and h.tanggal_pembayaran<=?))";
		if (sesi.getToko() != null) sql += " and h.toko=?";
		return sql;
	}

	private static int ikatCakupan(PreparedStatement ps, int p, SesiKasKasir sesi, Date sampai) throws Exception {
		ps.setLong(p++, sesi.getId().longValue());
		ps.setString(p++, sesi.getKasirNama());
		ps.setString(p++, sesi.getKasirUserId());
		ps.setTimestamp(p++, new Timestamp(sesi.getWaktuBuka().getTime()));
		ps.setTimestamp(p++, new Timestamp(sampai.getTime()));
		if (sesi.getToko() != null) ps.setLong(p++, sesi.getToko().getId().longValue());
		return p;
	}

	private static String teks(String s) { return s == null ? "" : s; }

	/** Membentuk laporan shift lengkap dari satu sumber data transaksi yang sama dengan tutup kas. */
	public static JSONObject laporanTutupKas(Session session, SesiKasKasir sesi, Date sampai,
			double uangFisik) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			JSONObject laporan = new JSONObject();
			laporan.put("idSesi", sesi.getId());
			laporan.put("kodeSesi", teks(sesi.getKode()));
			laporan.put("namaKasir", teks(sesi.getKasirNama()));
			laporan.put("userIdKasir", teks(sesi.getKasirUserId()));
			laporan.put("namaToko", sesi.getToko() == null ? "" : teks(sesi.getToko().getNama()));
			laporan.put("namaPerangkat", teks(sesi.getNamaPerangkat()));
			laporan.put("waktuBuka", Common.dateFormatInput.get().format(sesi.getWaktuBuka()));
			laporan.put("waktuTutup", Common.dateFormatInput.get().format(sampai));
			laporan.put("modalAwal", sesi.getModalAwal());

			String scope = cakupanTransaksi(sesi);
			ps = session.connection().prepareStatement("select count(*),coalesce(sum(h.total_biaya),0) "
					+ "from koperasi.pembelian_anggota_koperasi h where " + scope);
			ikatCakupan(ps, 1, sesi, sampai);
			rs = ps.executeQuery(); rs.next();
			int jumlahTransaksi = rs.getInt(1);
			double totalTransaksi = rs.getDouble(2);
			rs.close(); ps.close(); rs = null; ps = null;

			String n1 = "greatest(0,coalesce(h.total_biaya,0)-coalesce(h.nominal_bayar_2,0)-coalesce(h.nominal_bayar_3,0)-coalesce(h.nominal_bayar_4,0)-coalesce(h.nominal_bayar_5,0))";
			StringBuilder union = new StringBuilder();
			for (int slot = 1; slot <= 5; slot++) {
				if (slot > 1) union.append(" union all ");
				String fk = slot == 1 ? "h.cara_pembayaran_koperasi" : "h.cara_pembayaran_koperasi_" + slot;
				String nominal = slot == 1 ? n1 : "coalesce(h.nominal_bayar_" + slot + ",0)";
				union.append("select h.id transaksi_id,coalesce(c.nama,'Tanpa Metode') metode,")
						.append("coalesce(c.masuk_sebagai_hutang,false) hutang,")
						.append("coalesce(c.ada_kembalian,c.nama ilike '%tunai%') tunai,")
						.append(nominal).append(" nominal from koperasi.pembelian_anggota_koperasi h ")
						.append("left join koperasi.cara_pembayaran_koperasi c on c.id=").append(fk)
						.append(" where ").append(scope).append(" and ").append(fk)
						.append(" is not null and ").append(nominal).append(">0");
			}
			ps = session.connection().prepareStatement("select metode,hutang,tunai,count(distinct transaksi_id),sum(nominal) "
					+ "from (" + union + ") x group by metode,hutang,tunai order by metode");
			int p = 1;
			for (int slot = 1; slot <= 5; slot++) p = ikatCakupan(ps, p, sesi, sampai);
			rs = ps.executeQuery();
			JSONArray metode = new JSONArray();
			double tunai = 0, nonTunai = 0, piutang = 0;
			java.util.HashSet<String> metodeTerpakai = new java.util.HashSet<String>();
			while (rs.next()) {
				JSONObject m = new JSONObject();
				boolean hutang = rs.getBoolean(2), metodeTunai = rs.getBoolean(3);
				double nilai = rs.getDouble(5);
				m.put("nama", rs.getString(1)); m.put("jumlahTransaksi", rs.getInt(4));
				m.put("penerimaan", nilai); m.put("retur", 0); m.put("total", nilai);
				m.put("piutang", hutang); m.put("tunai", metodeTunai);
				metode.put(m);
				metodeTerpakai.add(rs.getString(1) == null ? "" : rs.getString(1).trim().toLowerCase());
				if (hutang) piutang += nilai; else if (metodeTunai) tunai += nilai; else nonTunai += nilai;
			}
			rs.close(); ps.close(); rs = null; ps = null;
			// Struk shift menampilkan seluruh metode yang aktif, termasuk yang nihil pada shift ini,
			// agar kasir dapat melakukan pemeriksaan satu per satu seperti laporan kas retail baku.
			ps = session.connection().prepareStatement("select nama,coalesce(masuk_sebagai_hutang,false),"
					+ "coalesce(ada_kembalian,nama ilike '%tunai%') from koperasi.cara_pembayaran_koperasi "
					+ "where coalesce(aktif,true)=true order by nama");
			rs = ps.executeQuery();
			while (rs.next()) {
				String nama = rs.getString(1) == null ? "Tanpa Metode" : rs.getString(1);
				if (metodeTerpakai.contains(nama.trim().toLowerCase())) continue;
				JSONObject m = new JSONObject(); m.put("nama", nama); m.put("jumlahTransaksi", 0);
				m.put("penerimaan", 0); m.put("retur", 0); m.put("total", 0);
				m.put("piutang", rs.getBoolean(2)); m.put("tunai", rs.getBoolean(3)); metode.put(m);
			}
			rs.close(); ps.close(); rs = null; ps = null;

			// Jumlah transaksi piutang dihitung terpisah agar satu transaksi multi-metode tidak dobel.
			ps = session.connection().prepareStatement("select count(distinct h.id) " + joinCaraPembayaran()
					+ " where " + scope + " and (coalesce(c1.masuk_sebagai_hutang,false) or coalesce(c2.masuk_sebagai_hutang,false)"
					+ " or coalesce(c3.masuk_sebagai_hutang,false) or coalesce(c4.masuk_sebagai_hutang,false) or coalesce(c5.masuk_sebagai_hutang,false))");
			ikatCakupan(ps, 1, sesi, sampai); rs = ps.executeQuery(); rs.next();
			int jumlahPiutang = rs.getInt(1); rs.close(); ps.close(); rs = null; ps = null;

			JSONArray metodeRetur = new JSONArray();
			double totalRetur = 0, returTunai = 0;
			String returSql = "select coalesce(nullif(trim(metodepengembalian),''),'Tanpa Pengembalian'),count(*),coalesce(sum(totalnilai),0)"
					+ " from koperasi.retur_penjualan where waktu>=? and waktu<=? and (oleh=? or oleh=?)";
			if (sesi.getToko() != null) returSql += " and toko=?";
			returSql += " group by 1 order by 1";
			ps = session.connection().prepareStatement(returSql);
			p = 1; ps.setTimestamp(p++, new Timestamp(sesi.getWaktuBuka().getTime()));
			ps.setTimestamp(p++, new Timestamp(sampai.getTime())); ps.setString(p++, sesi.getKasirNama());
			ps.setString(p++, sesi.getKasirUserId()); if (sesi.getToko()!=null) ps.setLong(p++, sesi.getToko().getId());
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject r = new JSONObject(); String nama = rs.getString(1); double nilai = rs.getDouble(3);
				r.put("nama", nama); r.put("jumlah", rs.getInt(2)); r.put("nilai", nilai); metodeRetur.put(r);
				totalRetur += nilai; if (nama != null && nama.toLowerCase().contains("tunai")) returTunai += nilai;
				for (int i=0;i<metode.length();i++) { JSONObject m=metode.getJSONObject(i);
					if (nama != null && nama.equalsIgnoreCase(m.optString("nama"))) { m.put("retur", nilai); m.put("total", m.optDouble("penerimaan")-nilai); }
				}
			}
			if (rs != null) rs.close(); if (ps != null) ps.close(); rs = null; ps = null;

			double biaya = 0; // Belum ada ledger biaya kas yang memiliki relasi sesi; tetap ditampilkan eksplisit.
			double kasSeharusnya = sesi.getModalAwal().doubleValue() + tunai - returTunai - biaya;
			double selisih = uangFisik - kasSeharusnya;
			laporan.put("jumlahKasTunai", uangFisik); laporan.put("uangFisik", uangFisik);
			laporan.put("penjualanTunai", tunai); laporan.put("penjualanNonTunai", nonTunai);
			laporan.put("kasSeharusnya", kasSeharusnya); laporan.put("selisih", selisih);
			laporan.put("returPenjualan", totalRetur); laporan.put("returTunai", returTunai);
			laporan.put("biaya", biaya); laporan.put("jumlahBiaya", 0);
			laporan.put("piutang", piutang); laporan.put("jumlahTransaksiPiutang", jumlahPiutang);
			laporan.put("jumlahTransaksi", jumlahTransaksi); laporan.put("totalTransaksi", totalTransaksi);
			laporan.put("metodePembayaran", metode); laporan.put("metodeRetur", metodeRetur);
			return laporan;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "gagal membuat laporan tutup kas id=" + sesi.getId());
			throw new IllegalStateException("Laporan tutup kas belum dapat dibuat. Sesi belum ditutup dan data transaksi tidak diubah.", e);
		} finally {
			try { if (rs != null) rs.close(); } catch (Exception ignored) {}
			try { if (ps != null) ps.close(); } catch (Exception ignored) {}
		}
	}

	public static JSONObject laporanTersimpanAtauHitung(Session session, SesiKasKasir sesi) {
		try {
			if (sesi.getLaporanTutupJson() != null && sesi.getLaporanTutupJson().trim().length() > 0)
				return new JSONObject(sesi.getLaporanTutupJson());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "snapshot laporan tutup kas rusak id=" + sesi.getId()); }
		Date sampai = sesi.getWaktuTutup() == null ? new Date() : sesi.getWaktuTutup();
		return laporanTutupKas(session, sesi, sampai, sesi.getUangFisik());
	}

	/** Versi teks monospasi untuk layar ZK lama dan printer teks/thermal. */
	public static String laporanTeks(JSONObject l) {
		StringBuilder s = new StringBuilder();
		s.append("LAPORAN TUTUP KAS\n").append(l.optString("namaToko", "")).append("\n")
				.append("--------------------------------\n")
				.append("Nama Kasir : ").append(l.optString("namaKasir", "-")).append("\n")
				.append("Buka       : ").append(l.optString("waktuBuka", "-")).append("\n")
				.append("Tutup      : ").append(l.optString("waktuTutup", "-")).append("\n")
				.append("--------------------------------\n")
				.append("Modal Awal          Rp ").append(Math.round(l.optDouble("modalAwal"))).append("\n")
				.append("Penjualan Tunai     Rp ").append(Math.round(l.optDouble("penjualanTunai"))).append("\n")
				.append("Kas Seharusnya      Rp ").append(Math.round(l.optDouble("kasSeharusnya"))).append("\n")
				.append("Jumlah Kas Tunai    Rp ").append(Math.round(l.optDouble("jumlahKasTunai"))).append("\n")
				.append("Selisih             Rp ").append(Math.round(l.optDouble("selisih"))).append("\n")
				.append("--------------------------------\n")
				.append("Retur Penjualan     Rp ").append(Math.round(l.optDouble("returPenjualan"))).append("\n")
				.append("Biaya               Rp ").append(Math.round(l.optDouble("biaya"))).append("\n")
				.append("Piutang ").append(l.optInt("jumlahTransaksiPiutang")).append("x         Rp ")
				.append(Math.round(l.optDouble("piutang"))).append("\n--------------------------------\n");
		JSONArray metode = l.optJSONArray("metodePembayaran");
		if (metode != null) for (int i=0;i<metode.length();i++) {
			JSONObject m=metode.optJSONObject(i); if (m==null) continue;
			s.append(m.optString("nama", "-")).append("\n  ").append(m.optInt("jumlahTransaksi"))
					.append("x Penerimaan  Rp ").append(Math.round(m.optDouble("penerimaan")))
					.append("\n  Retur          Rp ").append(Math.round(m.optDouble("retur")))
					.append("\n  Total          Rp ").append(Math.round(m.optDouble("total"))).append("\n");
		}
		s.append("--------------------------------\nJumlah Transaksi : ").append(l.optInt("jumlahTransaksi"))
				.append("\nTotal Transaksi  : Rp ").append(Math.round(l.optDouble("totalTransaksi")));
		return s.toString();
	}

	/**
	 * Memuat sesi terbuka milik akun, toko, DAN perangkat yang sama. Jalur API POS baru wajib
	 * memakai method ini; overload lama dipertahankan hanya untuk kompatibilitas layar ZK/JSP lama.
	 */
	public static SesiKasKasir sesiTerbukaPerangkat(Session session, String kasirNama, String kasirUserId,
			Long tokoId, String idPerangkat) {
		String perangkat = normalisasiIdPerangkat(idPerangkat);
		if (perangkat == null) return null;
		// Dahulukan kecocokan perangkat yang PERSIS. Query lama menyatukan kondisi
		// `(idPerangkat = ? OR idPerangkat IS NULL)` lalu mengambil id terbaru. Jika
		// akun masih mempunyai sesi warisan tanpa perangkat yang id-nya lebih baru,
		// sesi warisan itu terpilih walaupun perangkat ini SUDAH memiliki sesi aktif.
		// Saat sesi warisan kemudian diikat, unique index perangkat aktif menolaknya.
		Criteria tepat = session.createCriteria(SesiKasKasir.class)
				.add(Restrictions.or(Restrictions.eq("kasirNama", kasirNama), Restrictions.eq("kasirUserId", kasirUserId)))
				.add(Restrictions.or(Restrictions.eq("status", SesiKasKasir.STATUS_BUKA), Restrictions.isNull("status")))
				.add(Restrictions.eq("idPerangkat", perangkat))
				.addOrder(Order.desc("id"))
				.setMaxResults(1);
		if (tokoId != null) tepat.add(Restrictions.eq("toko", session.load(Toko.class, tokoId)));
		SesiKasKasir hasil = (SesiKasKasir) tepat.uniqueResult();
		if (hasil != null) return hasil;

		// Bila perangkat sudah dipakai sesi akun lain, jangan pernah mengklaim sesi
		// warisan akun ini. Pemanggil akan menampilkan bahwa kas berada di perangkat
		// lain, tanpa menyebabkan status HTTP/API gagal karena constraint database.
		if (sesiTerbukaPadaPerangkat(session, tokoId, perangkat) != null) return null;

		// Hanya ketika perangkat benar-benar bebas, satu sesi warisan milik akun ini
		// boleh diklaim untuk kompatibilitas data sebelum kolom perangkat tersedia.
		Criteria lama = session.createCriteria(SesiKasKasir.class)
				.add(Restrictions.or(Restrictions.eq("kasirNama", kasirNama), Restrictions.eq("kasirUserId", kasirUserId)))
				.add(Restrictions.or(Restrictions.eq("status", SesiKasKasir.STATUS_BUKA), Restrictions.isNull("status")))
				.add(Restrictions.isNull("idPerangkat"))
				.addOrder(Order.desc("id"))
				.setMaxResults(1);
		if (tokoId != null) lama.add(Restrictions.eq("toko", session.load(Toko.class, tokoId)));
		return (SesiKasKasir) lama.uniqueResult();
	}

	/** Sesi terbuka apa pun pada perangkat/toko, untuk mencegah satu mesin dipakai dua kasir. */
	public static SesiKasKasir sesiTerbukaPadaPerangkat(Session session, Long tokoId, String idPerangkat) {
		String perangkat = normalisasiIdPerangkat(idPerangkat);
		if (perangkat == null) return null;
		Criteria c = session.createCriteria(SesiKasKasir.class)
				.add(Restrictions.eq("idPerangkat", perangkat))
				.add(Restrictions.or(Restrictions.eq("status", SesiKasKasir.STATUS_BUKA), Restrictions.isNull("status")))
				.addOrder(Order.desc("id"))
				.setMaxResults(1);
		if (tokoId != null) c.add(Restrictions.eq("toko", session.load(Toko.class, tokoId)));
		return (SesiKasKasir) c.uniqueResult();
	}

	public static String normalisasiIdPerangkat(String nilai) {
		if (nilai == null) return null;
		String hasil = nilai.trim();
		if (hasil.length() == 0) return null;
		return hasil.length() > 128 ? hasil.substring(0, 128) : hasil;
	}

	/** Mengikat satu sesi warisan ke perangkat pertama yang dibuka oleh akun pemiliknya. */
	public static boolean ikatPerangkatJikaLama(SesiKasKasir sesi, String idPerangkat, String namaPerangkat) {
		if (sesi == null || sesi.getIdPerangkat() != null) return false;
		sesi.setIdPerangkat(normalisasiIdPerangkat(idPerangkat));
		String nama = namaPerangkat == null ? null : namaPerangkat.trim();
		if (nama != null && nama.length() > 150) nama = nama.substring(0, 150);
		sesi.setNamaPerangkat(nama == null || nama.length() == 0 ? null : nama);
		return sesi.getIdPerangkat() != null;
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
	public static double[] hitungPenjualan(Session session, String kasirNama, String kasirUserId, Long tokoId, Date dari, Date sampai) {
		try {
			StringBuilder sb = new StringBuilder("select coalesce(sum(")
					.append(nilaiPembayaran(true)).append("),0),coalesce(sum(")
					.append(nilaiPembayaran(false)).append("),0)").append(joinCaraPembayaran())
					.append(" where (h.kasir_login_nama=:o or h.kasir_login_nama=:i)")
					.append(" and h.tanggal_pembayaran>=:dari and h.tanggal_pembayaran<=:sampai ");
			if (tokoId != null) {
				sb.append(" and h.toko=:t ");
			}
			SQLQuery q = session.createSQLQuery(sb.toString());
			q.setParameter("o", kasirNama);
			q.setParameter("i", kasirUserId);
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
					.append(" and (h.kasir_login_nama=:o or h.kasir_login_nama=:i)")
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
		return buka(session, toko, kasirNama, kasirUserId, modalAwal, keterangan, null, null, null, null);
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
		return buka(session, toko, kasirNama, kasirUserId, modalAwal, keterangan, kode, waktuBukaKlien, null, null);
	}

	/** Membuka sesi yang terikat pada instalasi POS tertentu. */
	public static SesiKasKasir buka(Session session, Toko toko, String kasirNama, String kasirUserId, double modalAwal,
			String keterangan, String kode, Date waktuBukaKlien, String idPerangkat, String namaPerangkat) {
		SesiKasKasir o = new SesiKasKasir();
		o.setToko(toko);
		o.setKasirNama(kasirNama);
		o.setKasirUserId(kasirUserId);
		o.setWaktuBuka(waktuBukaKlien != null ? waktuBukaKlien : new Date());
		o.setModalAwal(Double.valueOf(modalAwal));
		o.setStatus(SesiKasKasir.STATUS_BUKA);
		o.setKeterangan(keterangan);
		o.setKode(kode);
		o.setIdPerangkat(normalisasiIdPerangkat(idPerangkat));
		String nama = namaPerangkat == null ? null : namaPerangkat.trim();
		if (nama != null && nama.length() > 150) nama = nama.substring(0, 150);
		o.setNamaPerangkat(nama == null || nama.length() == 0 ? null : nama);
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
		return tutup(session, sesi, uangFisik, keterangan, waktuTutupKlien, null);
	}

	/** Menutup sesi dengan penyesuaian penjualan tunai supervisor yang tetap tersimpan di snapshot audit. */
	public static double tutup(Session session, SesiKasKasir sesi, double uangFisik, String keterangan,
			Date waktuTutupKlien, Double penjualanTunaiKoreksi) {
		Date sampai = waktuTutupKlien != null ? waktuTutupKlien : new Date();
		JSONObject laporan = laporanTutupKas(session, sesi, sampai, uangFisik);
		if (penjualanTunaiKoreksi != null) {
			double tunaiAsli = laporan.optDouble("penjualanTunai", 0);
			double kasSeharusnya = sesi.getModalAwal().doubleValue() + penjualanTunaiKoreksi.doubleValue()
					- laporan.optDouble("returTunai", 0) - laporan.optDouble("biaya", 0);
			try {
				laporan.put("penjualanTunaiSebelumKoreksi", tunaiAsli);
				laporan.put("penjualanTunai", penjualanTunaiKoreksi.doubleValue());
				laporan.put("koreksiPenjualanTunai", penjualanTunaiKoreksi.doubleValue() - tunaiAsli);
				laporan.put("kasSeharusnya", kasSeharusnya);
				laporan.put("selisih", uangFisik - kasSeharusnya);
			} catch (org.json.JSONException e) {
				throw new IllegalStateException("Snapshot koreksi penjualan tunai tidak dapat dibentuk.", e);
			}
		}
		double selisih = laporan.optDouble("selisih", 0);
		sesi.setTotalTunai(Double.valueOf(laporan.optDouble("penjualanTunai", 0)));
		sesi.setTotalNonTunai(Double.valueOf(laporan.optDouble("penjualanNonTunai", 0)));
		sesi.setUangFisik(Double.valueOf(uangFisik));
		sesi.setSelisih(Double.valueOf(selisih));
		sesi.setWaktuTutup(sampai);
		sesi.setStatus(SesiKasKasir.STATUS_TUTUP);
		sesi.setLaporanTutupJson(laporan.toString());
		if (keterangan != null && keterangan.trim().length() > 0) {
			sesi.setKeterangan(keterangan);
		}
		// Sesi yang dimuat melalui session.get()/criteria sudah berada dalam persistence context.
		// Memanggil update() lagi melalui helper generik dapat memicu recovery/rollback internal dan
		// membuat pemanggil mengirim respons sukses walaupun perubahan status tidak pernah ter-commit.
		// Entity managed cukup diubah lewat setter; flush/commit dilakukan oleh pemilik transaksi.
		if (!session.contains(sesi)) {
			Common.refreshSaveOrUpdate(session, sesi);
		}
		return selisih;
	}
}
