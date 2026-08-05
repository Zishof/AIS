package ais.action.master.sirs.util;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.KepesertaanPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;

/**
 * PenjaminResolver — SATU titik keputusan penjaminan (Blueprint Integrasi SIRS — Fase 2).
 *
 * <p>Tujuan: memberi kode billing/registrasi/klaim yang akan datang sebuah tempat TUNGGAL untuk
 * bertanya "encounter/pasien ini dijamin oleh payer bertipe apa (UMUM / BPJS / ASURANSI_SWASTA /
 * ...)?" tanpa menyebar percabangan ke banyak file dan TANPA menyentuh 6 titik FK {@link Asuransi}
 * yang sudah ada ({@code Pasien.asuransi}, {@code Pendaftaran.asuransi}, {@code BookingRegistrasi.asuransi},
 * {@code TarifKhusus/Diskon/PajakMedis.asuransi}). {@link Asuransi} TETAP master payer tunggal;
 * BPJS hanyalah salah satu {@code jenisPayer}.</p>
 *
 * <p><b>Sifat:</b> kelas ini READ-ONLY dan null-safe. Metode logika murni tidak menyentuh database.
 * Metode yang perlu membaca {@link KepesertaanPasien} menerima {@link Session} milik pemanggil
 * (mis. {@code HibernateUtil.currentSession()}) — kelas ini TIDAK membuka/menutup session sendiri,
 * sesuai aturan sesi proyek. Belum ada pemanggil yang memakainya; menambah kelas ini tidak mengubah
 * perilaku alur mana pun.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 dan Hibernate 3.6.</p>
 */
public final class PenjaminResolver {

	private PenjaminResolver() {
	}

	/**
	 * Asuransi (payer) efektif untuk sebuah encounter: pakai payer pada {@code Pendaftaran} bila ada,
	 * jika tidak jatuh ke payer default pada {@code Pasien}. Bisa mengembalikan {@code null} (berarti UMUM).
	 */
	public static Asuransi asuransiEfektif(Pendaftaran pendaftaran) {
		if (pendaftaran == null) {
			return null;
		}
		Asuransi dariPendaftaran = null;
		try {
			dariPendaftaran = pendaftaran.getAsuransi();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:asuransiEfektif-pendaftaran");
		}
		if (dariPendaftaran != null) {
			return dariPendaftaran;
		}
		try {
			Pasien pasien = pendaftaran.getPasien();
			if (pasien != null) {
				return pasien.getAsuransi();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:asuransiEfektif-pasien");
		}
		return null;
	}

	/** Tipe payer efektif untuk encounter. {@code null} payer diperlakukan sebagai UMUM. */
	public static String jenisPayer(Pendaftaran pendaftaran) {
		return jenisPayer(asuransiEfektif(pendaftaran));
	}

	/** Tipe payer default seorang pasien. */
	public static String jenisPayer(Pasien pasien) {
		if (pasien == null) {
			return Asuransi.PAYER_UMUM;
		}
		try {
			return jenisPayer(pasien.getAsuransi());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:jenisPayer-pasien");
			return Asuransi.PAYER_UMUM;
		}
	}

	/** Tipe payer dari sebuah Asuransi. {@code null} → UMUM. */
	public static String jenisPayer(Asuransi asuransi) {
		if (asuransi == null) {
			return Asuransi.PAYER_UMUM;
		}
		try {
			return asuransi.getJenisPayerEfektif();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:jenisPayer-asuransi");
			return Asuransi.PAYER_UMUM;
		}
	}

	public static boolean isBpjs(Pendaftaran pendaftaran) {
		return Asuransi.PAYER_BPJS.equals(jenisPayer(pendaftaran));
	}

	public static boolean isBpjs(Asuransi asuransi) {
		return Asuransi.PAYER_BPJS.equals(jenisPayer(asuransi));
	}

	public static boolean isUmum(Pendaftaran pendaftaran) {
		return Asuransi.PAYER_UMUM.equals(jenisPayer(pendaftaran));
	}

	/** Label penjamin untuk tampilan: nama payer bila ada, jika tidak "Umum". */
	public static String labelPenjamin(Pendaftaran pendaftaran) {
		Asuransi asuransi = asuransiEfektif(pendaftaran);
		if (asuransi == null) {
			return "Umum";
		}
		try {
			String nama = asuransi.getNama();
			return (nama == null || nama.trim().isEmpty()) ? "Umum" : nama;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:labelPenjamin");
			return "Umum";
		}
	}

	/**
	 * Kepesertaan (Coverage) UTAMA yang aktif untuk seorang pasien pada tanggal tertentu.
	 * Read-only; memakai {@link Session} milik pemanggil. Mengembalikan {@code null} bila tidak ada.
	 * Urutan pilih: {@code urutanPenjamin} menaik (penjamin utama dulu), lalu terbaru.
	 */
	public static KepesertaanPasien cariKepesertaanUtama(Session session, Pasien pasien, Date tanggal) {
		if (session == null || pasien == null || pasien.getId() == null) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			List<KepesertaanPasien> daftar = session.createCriteria(KepesertaanPasien.class)
					.add(Restrictions.eq("pasien", pasien)).add(Restrictions.eq("statusAktif", Boolean.TRUE))
					.addOrder(Order.asc("urutanPenjamin")).addOrder(Order.desc("id")).list();
			if (daftar == null) {
				return null;
			}
			for (int i = 0; i < daftar.size(); i++) {
				KepesertaanPasien k = daftar.get(i);
				if (k != null && k.berlakuPada(tanggal)) {
					return k;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/PenjaminResolver.java:cariKepesertaanUtama");
		}
		return null;
	}
}
