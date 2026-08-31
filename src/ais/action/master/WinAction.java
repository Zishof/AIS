package ais.action.master;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Window;

import ais.common.ConstantValues;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;

/**
 * Controller/action ZK untuk win. Tipe ini merupakan titik masuk UI yang menghubungkan event layar
 * dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class WinAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4914656251286619189L;

	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		if (execution.getParameter("username") != null) {
			Tbmuser users = null;
			Mahasiswa mahasiswa = ConstantValues.ambilByNim(execution.getParameter("username"));
			if (mahasiswa != null) {
				users = new Tbmuser();
				users.setMahasiswa(mahasiswa);
				users.setUserNama(mahasiswa.getNim());
				users.setUserId(mahasiswa.getNim());
				users.setUserRole(ConstantValues.tbmroleMahasiswa);
				users.setFakultas(mahasiswa.getJurusan().getFakultas());
				users.setJurusan(mahasiswa.getJurusan());
				users.setBahasa(mahasiswa.getBahasa());
			} else {
				Siswa siswa = ConstantValues.ambilByNis(execution.getParameter("username"));
				if (siswa != null) {
					users = new Tbmuser();
					users.setDosen(null);
					users.setSiswa(siswa);
					users.setUserNama(siswa.getNomorInduk());
					users.setUserId(siswa.getNomorInduk());
					users.setUserRole(ConstantValues.tbmroleSiswa);
					users.setYayasan(siswa.getSekolah().getYayasan());
					users.setSekolah(siswa.getSekolah());
					users.setBahasa(siswa.getBahasa());
				} else {
					users = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), execution.getParameter("username"));
				}
			}

			if (users != null) {
				Sessions.getCurrent().setAttribute("user", users);
				Sessions.getCurrent().setAttribute("usersTemp", users);
				Sessions.getCurrent(true).setAttribute("current_lang",
						users == null ? Tbmuser.INDONESIA : users.getBahasa());
				HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				((HttpServletRequest) request).getSession(true).setAttribute("mytbmuser", users);
			}
		}
		String winClazz = execution.getParameter("win");
		if (winClazz == null || winClazz.trim().length() == 0) {
			return;
		}
		Window window = (Window) Class.forName(winClazz).newInstance();
		if (window != null) { window.setParent(page.getFirstRoot()); }
		if (window != null) { window.setWidth("100%"); }
		if (window != null) { window.setHeight("100%"); }
		window.onModal();
	}

}
