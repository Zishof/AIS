package ais.action.master.helper.util;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Notifikasi;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.ui.util.WaktuUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;

/**
 * Tipe khusus untuk evaluasi belanja processor. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * TimerTask}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah validasi/perhitungan ({@code
 * cekDanKirimNotifikasiPelanggaran()}); mutasi data ({@code prosesEvaluasiDanKembalikanNotifikasi()}, {@code
 * prosesEvaluasiDanKembalikanNotifikasi()}, {@code simpanNotifikasi()}); operasi domain lain ({@code run()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see TimerTask
 */
public class EvaluasiBelanjaProcessor extends TimerTask {

	@Override
	public void run() {
		// Method run() tetap ada untuk menjaga kompatibilitas dengan TimerTask / Scheduler.
		System.out.println(">>> Memulai Proses Evaluasi Belanja Anggota Koperasi (Via Scheduler)...");
		List<String> warnings = new ArrayList<String>();
		EvaluasiBelanjaProcessor.prosesEvaluasiDanKembalikanNotifikasi(warnings);

		// Cetak warnings ke console jika dijalankan dari background scheduler
		for (String w : warnings) {
			System.out.println(w);
		}
	}

	public static List<Notifikasi> prosesEvaluasiDanKembalikanNotifikasi(List<String> warnings) {
		return prosesEvaluasiDanKembalikanNotifikasi(warnings, false);
	}

	/**
	 * Method baru yang mengeksekusi evaluasi dan mengembalikan daftar Notifikasi
	 * yang terbentuk untuk keperluan download/export. 
	 * * @param warnings List referensi untuk menyimpan log riwayat proses
	 * @param abaikanHariMinggu boolean flag untuk bypass aturan hari Minggu
	 * @return List<Notifikasi> daftar notifikasi pelanggaran yang digenerate
	 */
	public static List<Notifikasi> prosesEvaluasiDanKembalikanNotifikasi(List<String> warnings,
			boolean abaikanHariMinggu) {
		List<Notifikasi> daftarNotifikasi = new ArrayList<Notifikasi>();

		if (!abaikanHariMinggu) {
			// PENGAMAN 1: Evaluasi hanya boleh dilakukan pada hari MINGGU.
			Calendar cal = Calendar.getInstance();
			if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
				warnings.add("SYSTEM: Bukan hari Minggu. Evaluasi dibatalkan agar member bisa berbelanja Senin-Sabtu.");
				return daftarNotifikasi; // Berhenti dan kembalikan list kosong
			}
		}

		warnings.add("SYSTEM: Memulai proses evaluasi pelanggaran belanja...");
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();

			@SuppressWarnings("unchecked")
			List<JenisAnggotaKoperasi> listJenis = ConstantValues
					.simpleList(session.createCriteria(JenisAnggotaKoperasi.class).add(Restrictions.eq("aktif", true))
							.add(Restrictions.eq("wajibBelanjaRutin", true)), JenisAnggotaKoperasi.class);

			if (listJenis.isEmpty()) {
				warnings.add("INFO: Tidak ada Jenis Anggota yang diset wajib belanja rutin (Toggle Off). Evaluasi selesai.");
			}

			for (JenisAnggotaKoperasi jenis : listJenis) {
				int targetBelanja = jenis.getTargetFrekuensiBelanja() != null ? jenis.getTargetFrekuensiBelanja() : 0;
				int maxPelanggaran = jenis.getMaksimalPelanggaran() != null ? jenis.getMaksimalPelanggaran() : 0;

				if (targetBelanja <= 0)
					continue;

				@SuppressWarnings("unchecked")
				List<AnggotaKoperasi> listAnggota = ConstantValues
						.simpleList(session.createCriteria(AnggotaKoperasi.class).add(Restrictions.eq("aktif", true))
								.add(Restrictions.eq("jenisAnggotaKoperasi", jenis)), AnggotaKoperasi.class);

				for (AnggotaKoperasi anggota : listAnggota) {
					// Panggil helper dan tangkap objek Notifikasi-nya
					Notifikasi notif = cekDanKirimNotifikasiPelanggaran(session, anggota, targetBelanja, maxPelanggaran,
							warnings);

					// Jika ada notifikasi, masukkan ke list return
					if (notif != null) {
						daftarNotifikasi.add(notif);
					}
				}
			}
			tx.commit();
			warnings.add("SYSTEM: Proses evaluasi selesai. Total Notifikasi digenerate: " + daftarNotifikasi.size());

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			warnings.add("ERROR: Terjadi kesalahan fatal pada sistem/database. Semua perubahan dibatalkan. Pesan: "
					+ e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/util/EvaluasiBelanjaProcessor.java:104");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/EvaluasiBelanjaProcessor.java:109");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/EvaluasiBelanjaProcessor.java:113");
				}
			}
			HibernateUtil.closeSession();
		}

		return daftarNotifikasi;
	}

	/**
	 * Method Reusable untuk mengecek, menghitung SP, dan mengirim notifikasi.
	 * Mengisi detail pada warnings list. 
	 * PERHATIAN: Ditambahkan 'throws Exception' pada deklarasi agar lolos kompilasi Ant.
	 */
	public static Notifikasi cekDanKirimNotifikasiPelanggaran(Session session, AnggotaKoperasi anggota,
			int targetBelanja, int maxPelanggaran, List<String> warnings) throws Exception {

		Long jumlahBelanja = (Long) session
				.createQuery("SELECT COUNT(p.id) FROM Pembelian p " + "WHERE p.anggotaKoperasi.id = :idAnggota "
						+ "AND date(p.waktu) >= current_date - 7 "
						+ "AND EXTRACT(ISODOW FROM p.waktu) BETWEEN 1 AND 6 ")
				.setParameter("idAnggota", anggota.getId()).uniqueResult();

		long trxCount = jumlahBelanja != null ? jumlahBelanja : 0;
		String identitas = "[" + anggota.getUserid() + "] " + anggota.getNama();

		// Mengambil jumlah SP real-time dari tabel notifikasi (Mencegah desync jika log SP dihapus admin)
		Long totalSpReal = (Long) session
				.createQuery("SELECT COUNT(n.id) FROM Notifikasi n " + "WHERE n.nama = :userId "
						+ "AND n.hasil IN ('WARNING', 'DANGER')")
				.setParameter("userId", anggota.getUserid()).uniqueResult();
		
		int spSekarang = totalSpReal != null ? totalSpReal.intValue() : 0;

		// Evaluasi Pelanggaran
		if (trxCount < targetBelanja) {

			// PENGAMAN ANTI-SPAM
			Long spHariIni = (Long) session
					.createQuery("SELECT COUNT(n.id) FROM Notifikasi n " + "WHERE n.nama = :userId "
							+ "AND date(n.waktu) = current_date " + "AND n.hasil IN ('WARNING', 'DANGER')")
					.setParameter("userId", anggota.getUserid()).uniqueResult();

			// Jika sudah ada SP hari ini, LEWATI agar tidak dobel.
			if (spHariIni != null && spHariIni > 0) {
				// Selaraskan status SP di profil member secara pasif
				if (anggota.getJumlahPeringatan() == null || anggota.getJumlahPeringatan() != spSekarang) {
					anggota.setJumlahPeringatan(spSekarang);
					session.update(anggota);
				}
				warnings.add("SKIP: Member " + identitas
						+ " melanggar target, namun SUDAH menerima notifikasi hari ini. Melewati proses agar tidak spam.");
				return null;
			}

			// Tambah SP untuk peringatan yang baru
			spSekarang += 1;
			anggota.setJumlahPeringatan(spSekarang);

			String pesanNotif;
			String statusNotif = "WARNING";

			// Cek apakah SP sudah mencapai batas maksimal
			if (spSekarang >= maxPelanggaran) {
				anggota.setAktif(false);
				pesanNotif = "Status keanggotaan Anda telah DINONAKTIFKAN karena tidak memenuhi target belanja ("
						+ targetBelanja + "x seminggu) selama " + maxPelanggaran + " minggu berturut-turut.";
				statusNotif = "DANGER";
				warnings.add(
						"ACTION: Member " + identitas + " DINONAKTIFKAN (Mencapai batas " + maxPelanggaran + " SP).");
			} else {
				anggota.setAktif(true); // Memastikan aktif kembali jika SP masih dalam batas aman (misal setelah dihapus admin)
				pesanNotif = "Peringatan SP-" + spSekarang + ": Anda baru berbelanja " + trxCount
						+ "x minggu ini (Syarat: " + targetBelanja
						+ "x). Pastikan penuhi target dari Senin s/d Sabtu agar keanggotaan tidak hangus.";
				warnings.add("ACTION: Member " + identitas + " diberikan SP-" + spSekarang + " (Trx: " + trxCount + "/"
						+ targetBelanja + ").");
			}

			session.update(anggota);

			// Menyimpan notifikasi dengan try-catch lokalan untuk menangkap kegagalan save objek
			try {
				Notifikasi notif = simpanNotifikasi(session, anggota.getUserid(), anggota.getEmail(), pesanNotif, statusNotif);
				warnings.add("SUCCESS: Notifikasi berhasil dibuat dan disimpan untuk " + identitas + ". Pesan : " + pesanNotif);
				return notif;
			} catch (Exception e) {
				warnings.add("ERROR: Gagal menyimpan data notifikasi untuk " + identitas + " - " + e.getMessage());
				throw e; // Melempar ulang agar tertangkap oleh tx.rollback() di atas.
			}

		} else {
			// Jika transaksi memenuhi target dan aman
			anggota.setAktif(true);
			anggota.setJumlahPeringatan(spSekarang); // Selaraskan jumlah peringatan secara real-time
			session.update(anggota);
			
			warnings.add("INFO: Member " + identitas + " AMAN (Trx: " + trxCount + " >= Target: " + targetBelanja + "). Status diselaraskan.");
			return null;
		}
	}

	/**
	 * Method Generic untuk menyimpan Notifikasi.
	 */
	public static Notifikasi simpanNotifikasi(Session session, String userId, String email, String pesan,
			String statusNotif) {
		Notifikasi notif = new Notifikasi();
		notif.setKeterangan(pesan);
		notif.setWaktu(WaktuUtil.getDate());
		notif.setHasil(statusNotif);
		notif.setEmails(email);
		notif.setNama(userId);

		session.save(notif);
		return notif;
	}
}