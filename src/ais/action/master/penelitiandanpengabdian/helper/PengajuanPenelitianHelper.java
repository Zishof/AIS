package ais.action.master.penelitiandanpengabdian.helper;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Spesialisasi {@link PengajuanPenelitianDanPengabdianHelper} untuk jenis pengajuan
 * <b>penelitian</b> ({@link ConstantValues#PENELITIAN}). Konstruktor menentukan otomatis siapa
 * yang mengajukan dan peruntukannya (dosen atau mahasiswa) berdasarkan user yang sedang login.
 */
public class PengajuanPenelitianHelper extends PengajuanPenelitianDanPengabdianHelper {

	/** Menyiapkan helper untuk jenis pengajuan penelitian; username dan peruntukan pengajuan ({@link PengumumanAkademis#UNTUK_DOSEN}/{@code UNTUK_MAHASISWA}) diambil dari user yang sedang login. */
	public PengajuanPenelitianHelper() {
		super(false, ConstantValues.PENELITIAN);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	/** Label istilah yang ditampilkan di UI untuk jenis pengajuan ini: {@code "Pengajuan Penelitian Dosen"}. */
	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Penelitian Dosen";
	}

}
