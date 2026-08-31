package ais.action.maintenance;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;

import ais.action.master.recruitment.CalonPegawaiAction;
import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * Controller/action ZK untuk calon peg. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code tbmuser}, {@code mysouth}, {@code
 * centerUtama}; operasi lokal: {@code doAfterCompose}(). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class CalonPegAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2446397351568124278L;
	private Tbmuser tbmuser;

	private South mysouth;
	private Center centerUtama;

	@SuppressWarnings({})
	public void doAfterCompose(Component comp) throws Exception {
		tbmuser = Common.getCurrentFromSpringUser();
		System.out.println("tbmuser => " + tbmuser);
		if (tbmuser == null && tbmuser.getPenyediaAsset() == null) {
			Common.goLogoff();
			return;
		}
		super.doAfterCompose(comp);
		CalonPegawaiAction.onAddExternal(centerUtama, mysouth, tbmuser.getCalonPegawai());

	}

}
