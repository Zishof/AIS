package ais.action.master.penelitiandanpengabdian.helper;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Spesialisasi {@link PengajuanPenelitianDanPengabdianHelper} untuk layar persetujuan pengajuan
 * PENGABDIAN (pengabdian kepada masyarakat, {@link ConstantValues#PENGABDIAN}), dengan mode
 * "persetujuan" aktif ({@code approve=true} pada superclass). Konstruktor otomatis mengisi filter
 * pengaju dari user yang sedang login: dosen -> filter berdasarkan userId dengan target pengumuman
 * {@link PengumumanAkademis#UNTUK_DOSEN}; mahasiswa -> filter berdasarkan NIM dengan target
 * {@link PengumumanAkademis#UNTUK_MAHASISWA}.
 */
public class PersetujuanPengabdianHelper extends PengajuanPenelitianDanPengabdianHelper {

	/** Membuat helper persetujuan pengabdian, mengisi filter pengaju otomatis dari user yang login (dosen atau mahasiswa). */
	public PersetujuanPengabdianHelper() {
		super(true, ConstantValues.PENGABDIAN);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	@Override
	/** @return label tampilan layar ini, {@code "Persetujuan Pengabdian Dosen"}. */
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Pengabdian Dosen";
	}

}
