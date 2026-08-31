package ais.action.master.penelitiandanpengabdian.helper;

import ais.action.master.helper.DetailArtikelHelper;
import ais.common.Common;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Spesialisasi {@link DetailArtikelHelper} untuk alur "Persetujuan Pengajuan Artikel" penelitian
 * dan pengabdian. Konstruktor otomatis menentukan konteks pengaju (dosen atau mahasiswa) dari user
 * yang sedang login, sehingga daftar/ form yang ditampilkan oleh kelas induk terfilter sesuai
 * identitas pengaju yang bersangkutan.
 */
public class PersetujuanArtikelHelper extends DetailArtikelHelper {

	/**
	 * Menentukan konteks pengajuan dari user yang sedang login: bila user terkait dosen,
	 * {@code usernamePengajuan} diisi userId dan diperuntukkan {@link PengumumanAkademis#UNTUK_DOSEN};
	 * bila terkait mahasiswa, diisi NIM dan diperuntukkan {@link PengumumanAkademis#UNTUK_MAHASISWA}.
	 */
	public PersetujuanArtikelHelper() {
		super(null, true);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	/** @return label istilah tampilan untuk alur ini: {@code "Persetujuan Pengajuan Artikel"}. */
	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Pengajuan Artikel";
	}

}
