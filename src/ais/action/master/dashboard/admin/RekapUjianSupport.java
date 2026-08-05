package ais.action.master.dashboard.admin;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PertemuanPunyaUjian;

/**
 * Penopang bersama untuk dasbor rekap ujian: memberi nama kolom dan mengambil nilai ujian seorang
 * peserta. Dipisah agar logika yang sama tidak ditulis ulang di tiap dasbor rekap ujian.
 */
public final class RekapUjianSupport {

	private RekapUjianSupport() {
	}

	/** Nama kolom untuk satu komponen ujian, termasuk format nilai / persentase bila ada. */
	public static String judulKolom(PertemuanPunyaUjian ppu) {
		if (ppu == null) {
			return "";
		}
		if (ppu.getFormatNilai() != null) {
			return ppu.getNama() + " (" + ppu.getFormatNilai().getNama() + " "
					+ Common.numberFormat.get().format(ppu.getFormatNilai().getPersen()) + ")";
		}
		if (ppu.getProsentase() != null) {
			return ppu.getNama() + " " + Common.numberFormat.get().format(ppu.getProsentase());
		}
		return ppu.getNama();
	}

	/** Ambil nilai ujian terakhir milik peserta untuk satu komponen; null bila peserta belum ikut. */
	public static Double ambilNilai(Session session, PertemuanPunyaUjian ppu, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa cal) {
		Number nilai = (Number) session.createCriteria(HasilUjianMahasiswa.class)
				.add(Restrictions.isNotNull("keyhasil")).setProjection(Projections.property("nilai"))
				.add(Restrictions.eq("pertemuanPunyaUjian", ppu))
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa)
						: Restrictions.eq("biodataCalonMahasiswa", cal))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		return nilai == null ? null : Double.valueOf(nilai.doubleValue());
	}

	/**
	 * Bentuk satu sel rekap ujian: nilai ditampilkan sebagai angka (0 jika peserta belum ikut), dan
	 * ditandai "sudah ikut" hanya bila benar-benar ada nilai.
	 */
	public static RekapNilaiView.Cell sel(boolean ikut, Double nilai) {
		if (!ikut) {
			return RekapNilaiView.Cell.tidakIkut();
		}
		String display = Common.numberFormat.get().format(nilai == null ? 0.0 : nilai.doubleValue());
		return new RekapNilaiView.Cell(true, nilai != null, nilai, display);
	}
}
