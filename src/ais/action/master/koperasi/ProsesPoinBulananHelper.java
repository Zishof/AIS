package ais.action.master.koperasi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Deposit;
import ais.database.model.FormulirKegiatan;
import ais.database.model.KonfigurasiPoinKegiatan;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;

/**
 * Proses bulanan pemberian voucher poin (kehadiran Kajian + Disiplin Kehadiran) -- fitur "Voucher
 * Pegawai". Dipicu MANUAL oleh admin (tombol "Proses Poin Bulan Ini", lihat
 * {@code ProsesPoinBulananAction}), dijalankan admin bersamaan waktu proses gaji bulanan (syarat E
 * spesifikasi) -- SENGAJA terpisah dari action posting gaji itu sendiri (bukan hook otomatis)
 * supaya kalkulasi gaji sama sekali tidak tersentuh (syarat F: poin TIDAK masuk perhitungan gaji).
 *
 * <p><b>Idempoten per (pegawai, kegiatan, periode).</b> Sebelum menerbitkan voucher baru, dicek
 * dulu apakah SUDAH ada baris {@link Deposit} dengan {@code konfigurasiPoinKegiatan} +
 * {@code anggotaKoperasi} yang sama dan {@code waktu} jatuh dalam periode yang sama -- aman
 * dijalankan berkali-kali untuk bulan yang sama tanpa menerbitkan voucher dobel.</p>
 *
 * <p><b>Registrasi AnggotaKoperasi.</b> Pegawai yang baru pertama kali menerima voucher otomatis
 * didaftarkan sebagai {@link AnggotaKoperasi} (dicari lebih dulu lewat relasi {@code pegawai},
 * dibuat baru bila belum ada) -- logic-nya SENGAJA ditulis inline di sini (bukan memanggil
 * {@code Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi}, yang membuka/menutup sesi
 * Hibernate-nya SENDIRI secara internal -- kalau dipanggil di tengah loop yang mengiterasi entity
 * dari sesi lain, entity tsb bisa jadi detached tepat saat sesi ditutup, memicu
 * LazyInitializationException pada iterasi berikutnya) supaya seluruh proses tetap berjalan dalam
 * SATU sesi yang konsisten dari awal sampai akhir.</p>
 */
public final class ProsesPoinBulananHelper {

	private ProsesPoinBulananHelper() {
	}

	public static class HasilProsesPoin {
		public int pegawaiDiproses = 0;
		public int voucherDiterbitkan = 0;
		public int dilewatiSudahAda = 0;
		public double totalNominal = 0;
	}

	@SuppressWarnings("unchecked")
	public static HasilProsesPoin prosesUntukBulan(int tahun, int bulan1sampai12, String prosesOleh)
			throws Exception {
		Calendar calAwal = Calendar.getInstance();
		calAwal.set(tahun, bulan1sampai12 - 1, 1, 0, 0, 0);
		calAwal.set(Calendar.MILLISECOND, 0);
		Date awal = calAwal.getTime();

		Calendar calAkhir = Calendar.getInstance();
		calAkhir.set(tahun, bulan1sampai12 - 1, calAwal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
		calAkhir.set(Calendar.MILLISECOND, 999);
		Date akhir = calAkhir.getTime();

		String labelPeriode = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(awal);

		HasilProsesPoin hasil = new HasilProsesPoin();
		Session session = HibernateUtil.currentSession();

		List<KonfigurasiPoinKegiatan> semuaKonfig = session.createCriteria(KonfigurasiPoinKegiatan.class)
				.add(Restrictions.eq("aktif", true)).list();
		if (semuaKonfig.isEmpty()) {
			return hasil;
		}

		Koperasi koperasi = (Koperasi) session.createCriteria(Koperasi.class).setMaxResults(1).uniqueResult();

		List<Pegawai> semuaPegawai = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		for (Pegawai pegawai : semuaPegawai) {
			boolean pegawaiDiproses = false;
			AnggotaKoperasi anggota = null;

			for (KonfigurasiPoinKegiatan konfig : semuaKonfig) {
				FormulirKegiatan kegiatan = konfig.getFormulirKegiatan();
				if (kegiatan == null) {
					continue;
				}

				Double poin = "Disiplin Kehadiran".equals(kegiatan.getNama())
						? hitungPoinDisiplin(session, pegawai, konfig, awal, akhir)
						: hitungPoinKajian(session, pegawai, kegiatan, konfig, awal, akhir);

				if (poin == null || poin.doubleValue() <= 0) {
					continue;
				}

				if (anggota == null) {
					anggota = resolveAnggotaKoperasi(session, pegawai, koperasi);
				}
				if (anggota == null) {
					continue; // pegawai tanpa kode/NIK -- tidak bisa didaftarkan sbg anggota koperasi
				}

				if (sudahDiterbitkan(session, konfig, anggota, awal, akhir)) {
					hasil.dilewatiSudahAda++;
					continue;
				}

				terbitkanVoucher(session, anggota, konfig, kegiatan, poin, labelPeriode, prosesOleh);
				hasil.voucherDiterbitkan++;
				hasil.totalNominal += poin.doubleValue();
				pegawaiDiproses = true;
			}

			if (pegawaiDiproses) {
				hasil.pegawaiDiproses++;
			}
		}

		return hasil;
	}

	private static Double hitungPoinDisiplin(Session session, Pegawai pegawai, KonfigurasiPoinKegiatan konfig,
			Date awal, Date akhir) {
		boolean lolos = AgregasiDisiplinKehadiranHelper.memenuhiSyaratDisiplin(session, pegawai.getId(), awal, akhir);
		return lolos ? konfig.getPoinOffline() : null;
	}

	@SuppressWarnings("unchecked")
	private static Double hitungPoinKajian(Session session, Pegawai pegawai, FormulirKegiatan kegiatan,
			KonfigurasiPoinKegiatan konfig, Date awal, Date akhir) {
		List<Pertemuan> sesi = session.createCriteria(Pertemuan.class)
				.add(Restrictions.eq("formulirKegiatan", kegiatan))
				.add(Restrictions.ge("tanggal", awal))
				.add(Restrictions.le("tanggal", akhir)).list();

		double total = 0;
		boolean adaHadir = false;
		for (Pertemuan p : sesi) {
			String kode = p.retreiveAbsensiKode(pegawai.getId());
			if ("M".equals(kode)) {
				String mode = p.retreiveAbsensiKeterangan(pegawai.getId());
				total += "ONLINE".equalsIgnoreCase(mode) ? konfig.getPoinOnline() : konfig.getPoinOffline();
				adaHadir = true;
			}
		}
		return adaHadir ? Double.valueOf(total) : null;
	}

	/**
	 * Cari-atau-buat {@link AnggotaKoperasi} untuk pegawai ini -- versi inline dari logic inti
	 * {@code Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi} (lihat JavaDoc kelas ini soal
	 * kenapa TIDAK memanggil method itu langsung). {@code null} bila pegawai tidak punya kode/NIK
	 * ({@code mycode}) -- tidak bisa didaftarkan tanpa identitas.
	 */
	private static AnggotaKoperasi resolveAnggotaKoperasi(Session session, Pegawai pegawai, Koperasi koperasi) {
		String kode = pegawai.getMycode();
		if (kode == null || kode.trim().isEmpty()) {
			return null;
		}

		AnggotaKoperasi anggota = (AnggotaKoperasi) session.createCriteria(AnggotaKoperasi.class)
				.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.eq("pegawai.id", pegawai.getId())).setMaxResults(1).uniqueResult();
		if (anggota != null) {
			return anggota;
		}

		anggota = new AnggotaKoperasi();
		anggota.setAktif(true);
		anggota.setPegawai(pegawai);
		anggota.setAlamat(pegawai.getAlamat());
		anggota.setEmail(pegawai.getEmail());
		anggota.setJenisIdentitas("NIK");
		anggota.setKeterangan("Anggota Koperasi ini mendaftar otomatis (Voucher Pegawai)");
		anggota.setKodeIdentitas(kode);
		anggota.setKode(kode);
		anggota.setNama(pegawai.getNama());
		anggota.setTipeAnggotaKoperasi(ConstantValues.PEGAWAI);
		anggota.setTipe(ConstantValues.PEGAWAI.getNama());
		anggota.setKoperasi(koperasi);
		Common.refreshSaveOrUpdate(session, anggota);
		return anggota;
	}

	private static boolean sudahDiterbitkan(Session session, KonfigurasiPoinKegiatan konfig, AnggotaKoperasi anggota,
			Date awal, Date akhir) {
		Number jumlah = (Number) session.createCriteria(Deposit.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("konfigurasiPoinKegiatan", konfig))
				.add(Restrictions.eq("anggotaKoperasi", anggota))
				.add(Restrictions.ge("waktu", awal))
				.add(Restrictions.le("waktu", akhir))
				.uniqueResult();
		return jumlah != null && jumlah.longValue() > 0;
	}

	private static void terbitkanVoucher(Session session, AnggotaKoperasi anggota, KonfigurasiPoinKegiatan konfig,
			FormulirKegiatan kegiatan, Double poin, String labelPeriode, String prosesOleh) {
		Deposit deposit = new Deposit();
		deposit.setAnggotaKoperasi(anggota);
		deposit.setNominal(poin);
		deposit.setWaktu(new Date());
		deposit.setKeterangan("Voucher " + kegiatan.getNama() + " - " + labelPeriode);
		deposit.setOleh(prosesOleh);
		deposit.setKonfigurasiPoinKegiatan(konfig);
		if (konfig.getDurasiBerlakuHari() != null) {
			Calendar calExp = Calendar.getInstance();
			calExp.add(Calendar.DAY_OF_MONTH, konfig.getDurasiBerlakuHari().intValue());
			deposit.setTanggalExpired(calExp.getTime());
		}
		Common.refreshSaveOrUpdate(session, deposit);
	}

}
