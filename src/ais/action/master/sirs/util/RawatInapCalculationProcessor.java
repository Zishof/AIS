package ais.action.master.sirs.util;

import java.util.Date;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DetailTransaksiLayanan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.KunjunganDokter;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Tindakan;

/**
 * Prosesor terkait tarif rawat inap modul SIRS. Saat ini hanya satu jalur yang benar-benar aktif:
 * {@link #checkKunjunganDokter} — menyusun/memperbarui {@link DetailTransaksiLayanan} untuk biaya
 * kunjungan dokter berdasarkan {@link Tindakan} dan {@link KelasPerawatan} pasien.
 *
 * <p>
 * <b>Catatan:</b> {@link #doProcess()} (dipanggil dari {@link #run()} sebagai {@link TimerTask})
 * dan {@link #checkPendaftaran(Pendaftaran, Session)} (perhitungan tarif tempat tidur per hari
 * rawat inap) seluruh isinya DIKOMENTARI — kedua method ini efektif TIDAK melakukan apa pun saat
 * ini. Perilaku ini dipertahankan apa adanya sesuai cakupan dokumentasi ini, bukan diaktifkan
 * kembali di sini.
 * </p>
 */
public class RawatInapCalculationProcessor extends TimerTask {

	/** Titik masuk {@link TimerTask}; mendelegasikan ke {@link #doProcess()} (saat ini tidak melakukan apa pun, lihat catatan javadoc kelas). */
	@Override
	public void run() {
		doProcess();
	}

	/** Dimaksudkan untuk memproses pendaftaran rawat inap yang belum keluar secara berkala; seluruh isi method saat ini dikomentari sehingga tidak melakukan apa pun. */
	private void doProcess() {
		// Session session = HibernateUtil.getSessionFactory().openSession();
		//
		// List<Pendaftaran> pendaftarans = session
		// .createCriteria(Pendaftaran.class)
		// .add(Restrictions.isNull("dataPasienKeluar"))
		// .add(Restrictions.eq("jenis", Pendaftaran.RAWAT_INAP)).list();
		//
		// System.out
		// .println("======================================================== checking
		// for pendaftaran =>"
		// + pendaftarans.size()
		// +
		// " ==================================================================");
		//
		// for (Pendaftaran pendaftaran : pendaftarans) {
		// session.getTransaction().begin();
		// checkPendaftaran(pendaftaran, session);
		// session.getTransaction().commit();
		// }
		// // session.disconnect();
		// if (session.isOpen()) {session.disconnect();session.close();}
	}

	/** Varian ringkas {@link #checkKunjunganDokter(KunjunganDokter, Session)} memakai sesi Hibernate thread-local saat ini. */
	public static void checkKunjunganDokter(KunjunganDokter kunjunganDokter) {
		checkKunjunganDokter(kunjunganDokter, HibernateUtil.currentSession());
	}

	/**
	 * Menyusun (atau memperbarui bila sudah ada) satu {@link DetailTransaksiLayanan} untuk biaya
	 * kunjungan dokter, dicocokkan lewat kombinasi {@code kunjunganDokter}+{@code tindakan}. Biaya
	 * dihitung berdasarkan {@link KelasPerawatan} pasien (fallback ke {@code ConstantValues.kelasNormal}
	 * bila pendaftaran belum punya kelas perawatan) lewat {@code CommonPendaftaranUtil.setDetailBiaya}.
	 * Tidak melakukan apa pun bila {@code kunjunganDokter} tidak punya {@link Tindakan} terkait.
	 */
	public static void checkKunjunganDokter(KunjunganDokter kunjunganDokter, Session session) {

		Tindakan jenisKunjungan = kunjunganDokter.getTindakan();
		if (jenisKunjungan == null) {
			return;
		}

		DetailTransaksiLayanan detailTransaksiLayanan = (DetailTransaksiLayanan) session
				.createCriteria(DetailTransaksiLayanan.class).add(Restrictions.eq("kunjunganDokter", kunjunganDokter))
				.add(Restrictions.eq("tindakan", jenisKunjungan)).setMaxResults(1).uniqueResult();
		if (detailTransaksiLayanan == null) {
			detailTransaksiLayanan = new DetailTransaksiLayanan();
			detailTransaksiLayanan.setDiskon(0.0);
			detailTransaksiLayanan.setKeterangan(jenisKunjungan.getNama());
			detailTransaksiLayanan.setLokasi(kunjunganDokter.getDiagnosaPenyakit().getLokasi());
			detailTransaksiLayanan.setPajak(0.0);
			detailTransaksiLayanan.setPasien(kunjunganDokter.getDiagnosaPenyakit().getPasien());
			detailTransaksiLayanan.setQty(0.0);
			detailTransaksiLayanan.setQtyBonus(0.0);
			detailTransaksiLayanan.setTanggal(new Date());
			detailTransaksiLayanan.setTindakan(jenisKunjungan);
			detailTransaksiLayanan.setKunjunganDokter(kunjunganDokter);
		}

		detailTransaksiLayanan.setPendaftaran(kunjunganDokter.getDiagnosaPenyakit().getPendaftaran());
		detailTransaksiLayanan.setKodeTransaksi(ConstantValues.kunjunganDokter);
		detailTransaksiLayanan.setTanggal(kunjunganDokter.getWaktu());
		detailTransaksiLayanan.setKeterangan(
				"Kunjungan dokter " + jenisKunjungan.toString() + ", Ket: " + kunjunganDokter.getKeterangan());
		detailTransaksiLayanan.setQty(1.0);
		KelasPerawatan kelasPerawatan = kunjunganDokter.getDiagnosaPenyakit().getPendaftaran()
				.getKelasPerawatan() == null ? ConstantValues.kelasNormal
						: kunjunganDokter.getDiagnosaPenyakit().getPendaftaran().getKelasPerawatan();
		CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, kelasPerawatan, session);

	}

	/** Varian ringkas {@link #checkPendaftaran(Pendaftaran, Session)} memakai sesi Hibernate thread-local saat ini. */
	public static void checkPendaftaran(Pendaftaran pendaftaran) {
		checkPendaftaran(pendaftaran, HibernateUtil.currentSession());
	}

	/**
	 * Dimaksudkan untuk menghitung tarif tempat tidur per hari rawat inap (membuat
	 * {@code AlatMedis} tarif BED otomatis bila belum ada, lalu menyusun
	 * {@link DetailTransaksiLayanan} berdasarkan selisih hari sejak tanggal pendaftaran).
	 *
	 * <p>
	 * <b>Catatan:</b> seluruh isi method ini saat ini DIKOMENTARI sehingga tidak melakukan apa pun
	 * — lihat catatan pada javadoc kelas.
	 * </p>
	 */
	public static void checkPendaftaran(Pendaftaran pendaftaran, Session session) {
		// if (pendaftaran == null || pendaftaran.getJenis() == null
		// || !pendaftaran.getJenis().equals(Pendaftaran.RAWAT_INAP)) {
		// return;
		// }
		// TempatTidur tempatTidur = pendaftaran.getTempatTidur();
		// System.out.println("tempatTidur = " + tempatTidur);
		// if (tempatTidur == null) {
		// return;
		// }
		//
		// JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) session
		// .createCriteria(JenisAlatMedis.class)
		// .add(Restrictions.eq("nama", JenisAlatMedis.JENIS_TARIF_BED))
		// .setMaxResults(1).uniqueResult();
		// if (jenisAlatMedis == null) {
		// jenisAlatMedis = new JenisAlatMedis();
		// jenisAlatMedis.setNama(JenisAlatMedis.JENIS_TARIF_BED);
		// session.save(jenisAlatMedis);
		// }
		//
		// AlatMedis tarifKamarDanRuangan = (AlatMedis) session
		// .createCriteria(AlatMedis.class).addOrder(Order.desc("id"))
		// .setMaxResults(1)
		// .add(Restrictions.eq("jenisAlatMedis", jenisAlatMedis))
		// .add(Restrictions.eq("tempatTidur", tempatTidur))
		// .uniqueResult();
		// if (tarifKamarDanRuangan == null) {
		//
		// String namaTarif = "Tarif BED "
		// + (tempatTidur == null ? "-" : tempatTidur.getNama())
		// + " di kamar "
		// + tempatTidur.getKamar()
		// + " ruang "
		// + (tempatTidur.getKamar() == null ? "" : tempatTidur
		// .getKamar().getRuang());
		//
		// tarifKamarDanRuangan = new AlatMedis();
		// tarifKamarDanRuangan.setKeterangan(namaTarif);
		// tarifKamarDanRuangan.setJenisAlatMedis(jenisAlatMedis);
		// tarifKamarDanRuangan.setKode("AUTO-GEN");
		// tarifKamarDanRuangan.setNama(namaTarif);
		// tarifKamarDanRuangan.setSemuahargasama(false);
		// tarifKamarDanRuangan.setRuang(tempatTidur.getKamar() == null ? null
		// : tempatTidur.getKamar().getRuang());
		// tarifKamarDanRuangan.setKamar(tempatTidur.getKamar());
		// tarifKamarDanRuangan.setTempatTidur(tempatTidur);
		// tarifKamarDanRuangan.setPer(AlatMedis.PER_HARI);
		// session.save(tarifKamarDanRuangan);
		//
		// }
		//
		// DetailTransaksiLayanan detailTransaksiLayanan =
		// (DetailTransaksiLayanan) session
		// .createCriteria(DetailTransaksiLayanan.class)
		// .add(Restrictions.eq("pendaftaran", pendaftaran))
		// .add(Restrictions.eq("alatMedis", tarifKamarDanRuangan))
		// .setMaxResults(1).uniqueResult();
		// if (detailTransaksiLayanan == null) {
		// detailTransaksiLayanan = new DetailTransaksiLayanan();
		// detailTransaksiLayanan.setDiskon(0.0);
		// detailTransaksiLayanan
		// .setKeterangan(tarifKamarDanRuangan.getNama());
		// detailTransaksiLayanan.setLokasi(pendaftaran.getLokasi());
		// detailTransaksiLayanan.setPajak(0.0);
		// detailTransaksiLayanan.setPasien(pendaftaran.getPasien());
		// detailTransaksiLayanan.setQty(0.0);
		// detailTransaksiLayanan.setQtyBonus(0.0);
		// detailTransaksiLayanan.setTanggal(new Date());
		// detailTransaksiLayanan.setAlatMedis(tarifKamarDanRuangan);
		// detailTransaksiLayanan.setPendaftaran(pendaftaran);
		// }
		//
		// detailTransaksiLayanan.setKodeTransaksi(ConstantValues.transaksiBed);
		//
		// Calendar a = Calendar.getInstance();
		// a.setTime(pendaftaran.getTanggalPendaftaran());
		// Calendar b = Calendar.getInstance();
		// Long unitQty =
		// AlatMedis.PER_KALI.equals(tarifKamarDanRuangan.getPer()) ? 1L
		// : DateUtils
		// .getDifference(
		// a,
		// b,
		// AlatMedis.PER_HARI.equals(tarifKamarDanRuangan
		// .getPer()) ? TimeUnit.DAYS
		// : AlatMedis.PER_JAM
		// .equals(tarifKamarDanRuangan
		// .getPer()) ? TimeUnit.HOURS
		// : TimeUnit.DAYS);
		// Long hari = detailTransaksiLayanan.getQty().longValue();
		// if (unitQty > 0L && !hari.equals(unitQty)) {
		// detailTransaksiLayanan.setQty(unitQty.doubleValue());
		// KelasPerawatan kelasPerawatan = pendaftaran.getKelasPerawatan() ==
		// null ? ConstantValues.kelasNormal
		// : pendaftaran.getKelasPerawatan();
		// CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan,
		// kelasPerawatan, session);
		// }

	}
}
