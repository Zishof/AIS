package ais.action.master.koperasi;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;

/**
 * Evaluasi syarat "Disiplin Kehadiran" (poin D spesifikasi Voucher Pegawai): pegawai berhak atas
 * poin bulan itu HANYA bila dalam periode yang dicek TIDAK PERNAH izin/sakit/cuti (apa pun
 * jenisnya, sesuai permintaan client -- semua jenis cuti/izin yang SUDAH DISETUJUI menggugurkan,
 * bukan hanya cuti mendadak) DAN TIDAK PERNAH terlambat datang.
 *
 * <p><b>Sumber data (reuse penuh, tidak ada tabel baru).</b></p>
 * <ul>
 * <li>{@link CutiDanIzin} -- baris {@code setujui=true} yang rentang tanggalnya ({@code
 * mulai}/{@code sampai}) beririsan dengan periode yang dicek dianggap menggugurkan, TANPA memandang
 * {@code jenisCutiDanIzin} (izin/sakit/cuti semua diperlakukan sama, sesuai konfirmasi client).
 * Baris yang belum disetujui ({@code setujui=false}/null) TIDAK menggugurkan -- pengajuan yang
 * masih pending/ditolak bukan bukti ketidakhadiran sungguhan.</li>
 * <li>{@link StatuskehadiranKaryawanHarian} -- kolom {@code jumlahTerlambat} (bukan getter
 * {@code isDatangTerlambat()} yang cuma komputasi in-memory) dipakai LANGSUNG di query SQL/Criteria
 * supaya tidak bergantung pada nilai getter yang mungkin belum pernah dipanggil sebelum baris
 * disimpan -- pelajaran dari bug serupa di {@code StokOpname.selisih} (lihat
 * {@code StokOpnameScanUtil.simpanOpname}). Hanya keterlambatan DATANG yang dicek (sesuai teks
 * spesifikasi "tidak pernah terlambat HADIR") -- keterlambatan PULANG ({@code pulangTerlambat})
 * sengaja TIDAK ikut dievaluasi di sini.</li>
 * </ul>
 */
public final class AgregasiDisiplinKehadiranHelper {

	private AgregasiDisiplinKehadiranHelper() {
	}

	/**
	 * @param session    sesi Hibernate aktif.
	 * @param pegawaiId  id {@link ais.database.model.Pegawai} yang dievaluasi.
	 * @param awalPeriode  tanggal awal periode (inklusif), mis. tanggal 1 bulan berjalan.
	 * @param akhirPeriode tanggal akhir periode (inklusif), mis. tanggal terakhir bulan berjalan.
	 * @return {@code true} bila pegawai memenuhi syarat Disiplin Kehadiran (nol izin/sakit/cuti
	 *         disetujui yang overlap periode, DAN nol hari terlambat datang) -- {@code false} bila
	 *         salah satu syarat tidak terpenuhi, atau {@code pegawaiId} null.
	 */
	public static boolean memenuhiSyaratDisiplin(Session session, Long pegawaiId, Date awalPeriode,
			Date akhirPeriode) {
		if (pegawaiId == null || awalPeriode == null || akhirPeriode == null) {
			return false;
		}

		Number jumlahCutiIzin = (Number) session.createCriteria(CutiDanIzin.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("pegawai.id", pegawaiId))
				.add(Restrictions.eq("setujui", true))
				.add(Restrictions.le("mulai", akhirPeriode))
				.add(Restrictions.ge("sampai", awalPeriode))
				.uniqueResult();
		if (jumlahCutiIzin != null && jumlahCutiIzin.longValue() > 0) {
			return false;
		}

		Number jumlahTerlambat = (Number) session.createCriteria(StatuskehadiranKaryawanHarian.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("pegawai.id", pegawaiId))
				.add(Restrictions.ge("tanggal", awalPeriode))
				.add(Restrictions.le("tanggal", akhirPeriode))
				.add(Restrictions.gt("jumlahTerlambat", 0.001))
				.uniqueResult();
		if (jumlahTerlambat != null && jumlahTerlambat.longValue() > 0) {
			return false;
		}

		return true;
	}

}
