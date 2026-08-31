package ais.action.master.pmb;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.ui.util.MyButtonTabbox;

/**
 * Controller/action ZK untuk statistik. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code tabHost}, {@code tabbox}, {@code tabAktif};
 * operasi lokal: {@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initTabbox()}, {@code pilihTab()},
 * {@code onPendaftarSpmb()}, {@code onJenisPendidikan()}, {@code onPropinsi()}, {@code onPerHari}(). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class StatistikAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		initTabbox();
	}

	private Div tabHost;
	private MyButtonTabbox tabbox;
	private final int[] tabAktif = new int[] { 1 };

	private void initTabbox() {
		if (tabbox != null || tabHost == null) {
			return;
		}
		tabbox = MyButtonTabbox.buat(tabHost, "100%", tabAktif);
		tabbox.tambahTabZul(1, "Pendaftar SPMB", "/pages/pmb/statistik/rekap_pendaftar_spmb.zul");
		tabbox.tambahTabZul(2, "Jenis Pendidikan", "/pages/pmb/statistik/rekap_pendaftar_spmb_per_jenis_sekolah.zul");
		tabbox.tambahTabZul(3, "Propinsi", "/pages/pmb/statistik/rekap_pendaftar_spmb_propinsi.zul");
		tabbox.tambahTabZul(4, "Per hari", "/pages/pmb/statistik/rekap_pendaftar_spmb_per_hari.zul");
		tabbox.pilih(1);
	}

	private void pilihTab(int index) {
		initTabbox();
		if (tabbox != null) {
			tabbox.pilih(index);
		}
	}

	public void onPendaftarSpmb(Event event) {
		pilihTab(1);
	}

	public void onJenisPendidikan(Event event) {
		pilihTab(2);
	}

	public void onPropinsi(Event event) {
		pilihTab(3);
	}

	public void onPerHari(Event event) {
		pilihTab(4);
	}
	
}
