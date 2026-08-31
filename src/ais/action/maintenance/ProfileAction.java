package ais.action.maintenance;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.East;

import ais.action.master.helper.profile.ProfileAdminPerguruanTinggi;
import ais.action.master.helper.profile.ProfileAdminSekolah;
import ais.action.master.helper.profile.ProfileDosen;
import ais.action.master.helper.profile.ProfileGabunganPengguna;
import ais.action.master.helper.profile.ProfileGuru;
import ais.action.master.helper.profile.ProfileMahasiswa;
import ais.action.master.helper.profile.ProfileSiswa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.Konfigurasi;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

/**
 * Controller/action ZK untuk profile. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code center}, {@code tbmuser}; operasi lokal:
 * {@code doAfterCompose()}, {@code initProfile()}, {@code initProfile}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ProfileAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1783885254736882086L;

	private Center center;

	private Tbmuser tbmuser;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		try {
			Common.REAL_PATH = session.getWebApp().getRealPath("/");
			Common.initTemp();
			CommonMedia.getMediaDirectory();
			Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
			tbmuser = Common.getCurrentFromSpringUser();
			if (tbmuser == null || tbmuser.getUserId() == null) {
				return;
			}

			ProfileAction.initProfile(tbmuser, center, null);

		} catch (Exception e) {

			Common.tampilErrorJikaAdmin(e);
		}

	}

	public static void initProfile(Tbmuser tbmuser, Component center, PengumumanAkademis pengumumanAkademis)
			throws Exception {
		initProfile(tbmuser, center, pengumumanAkademis, true, false);
	}

	public static void initProfile(Tbmuser tbmuser, Component center, PengumumanAkademis pengumumanAkademis,
			boolean lain, boolean tampilkanChart) throws Exception {
		if (tbmuser == null || center == null) {
			return;
		}

		if (pengumumanAkademis == null || !pengumumanAkademis.getTampilkanProfile()) {

			if (ProfileGabunganPengguna.aktifUntukLoginSaatIni(tbmuser)) {
				new ProfileGabunganPengguna(tbmuser, tampilkanChart).init(center);
				return;
			}

			if (tbmuser.getMahasiswa() != null) {
				new ProfileMahasiswa(tbmuser.getMahasiswa()).init(center, tbmuser.getMahasiswa().currentSemester(),
						null, false);
			} else if (tbmuser.ambilDosen() != null) {
				new ProfileDosen(tbmuser.ambilDosen()).init(center, false);
			} else if (tbmuser.ambilYayasan() != null && tbmuser.getSiswa() != null) {
				new ProfileSiswa(tbmuser.getSiswa()).init(center);
			} else if (tbmuser.ambilYayasan() != null && tbmuser.ambilGuru() != null) {
				new ProfileGuru(tbmuser.ambilGuru()).init(center);
			} else if (tbmuser.ambilSekolah() != null) {
				new ProfileAdminSekolah(tbmuser).init(center);
			} else if (tbmuser.ambilYayasan() != null
					&& Common.bolehKonfigurasi("apakah_aktifkan_modul_pesantren", Konfigurasi.TIDAK_AKTIF)) {
				new ProfileAdminSekolah(tbmuser, lain, tampilkanChart).init(center);
			} else if (Common.bolehKonfigurasi("admin_pt_tampilkan_profile")) {
				new ProfileAdminPerguruanTinggi(tbmuser, lain, tampilkanChart).init(center);
			} else if (center instanceof East) {
				((East) center).setWidth("0px");
				((East) center).setVisible(false);
			}
		}
	}
}
