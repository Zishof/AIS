package ais.action.master.penelitiandanpengabdian.helper;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Spesialisasi {@link PengajuanPenelitianDanPengabdianHelper} khusus untuk alur
 * <b>persetujuan</b> pengajuan penelitian dosen (bukan pengabdian). Konstruktor memanggil
 * superclass dengan {@code isPersetujuan=true} dan kategori {@link ConstantValues#PENELITIAN},
 * lalu mengisi konteks pengguna saat ini (username pengajuan dan peruntukannya) berdasarkan apakah
 * pengguna login berperan sebagai dosen atau mahasiswa, sehingga daftar pengajuan yang perlu
 * disetujui dapat disaring sesuai peran pemohon.
 */
public class PersetujuanPenelitianHelper extends PengajuanPenelitianDanPengabdianHelper {

	/** Menyiapkan helper untuk alur persetujuan penelitian, mengisi {@code usernamePengajuan} dan {@code diperuntukkanPengajuan} dari user yang sedang login (dosen atau mahasiswa). */
	public PersetujuanPenelitianHelper() {
		super(true, ConstantValues.PENELITIAN);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	/** Mengembalikan label istilah tetap {@code "Persetujuan Penelitian Dosen"} yang dipakai pada tampilan/label layar terkait. */
	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Penelitian Dosen";
	}

}
