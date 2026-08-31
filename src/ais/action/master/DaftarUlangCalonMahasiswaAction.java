package ais.action.master;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.JenisKegiatan;

/**
 * Controller/action ZK untuk daftar ulang calon mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DaftarUlangMahasiswaBaruAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk
 * variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak
 * bercabang atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(); konfigurasi constructor:
 * {@code jenisKegiatan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see DaftarUlangMahasiswaBaruAction
 */
public class DaftarUlangCalonMahasiswaAction extends DaftarUlangMahasiswaBaruAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1035398565522440476L;

	public DaftarUlangCalonMahasiswaAction() {
		super();
		super.jenisKegiatan = (JenisKegiatan) ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.selectComboItem(semesterPilihan, 0);
		if (semesterPilihan != null) { semesterPilihan.setDisabled(true); }
	}

}
