package ais.action.master.penelitiandanpengabdian.helper;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Spesialisasi {@link PengajuanPenelitianDanPengabdianHelper} untuk jenis pengajuan
 * <b>Pengabdian</b> (kepada masyarakat), dibedakan dari pengajuan Penelitian lewat parameter
 * {@link ConstantValues#PENGABDIAN} yang diteruskan ke konstruktor induk.
 *
 * <p>
 * Saat dibuat, kelas ini langsung menentukan siapa yang mengajukan berdasarkan
 * {@link Common#getCurrentUser() user yang sedang login}: bila user tersebut memiliki data dosen,
 * pengajuan ditandai atas nama dosen ({@code usernamePengajuan} = user id, target
 * {@link PengumumanAkademis#UNTUK_DOSEN}); bila tidak, tetapi memiliki data mahasiswa, pengajuan
 * ditandai atas nama mahasiswa ({@code usernamePengajuan} = NIM, target
 * {@link PengumumanAkademis#UNTUK_MAHASISWA}). Bila user tidak memiliki keduanya, kedua bidang
 * warisan tersebut dibiarkan tidak terisi.
 * </p>
 */
public class PengajuanPengabdianHelper extends PengajuanPenelitianDanPengabdianHelper {

	/**
	 * Membuat helper pengajuan pengabdian untuk user yang sedang login, sekaligus menentukan
	 * peran pengaju (dosen atau mahasiswa) lewat pemeriksaan {@link Common#getCurrentUser()}.
	 */
	public PengajuanPengabdianHelper() {
		super(false, ConstantValues.PENGABDIAN);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	/** @return label tetap {@code "Pengajuan Pengabdian Dosen"} untuk jenis pengajuan ini. */
	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Pengabdian Dosen";
	}

}
