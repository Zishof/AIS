package ais.action.master.ux;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.action.master.helper.PertemuanHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk ux pertemuan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code index}, {@code window}; operasi lokal: {@code
 * doAfterCompose}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class UxPertemuanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1783885254736882086L;

	private int index = 0;

	private MyWindow window;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		try {
			index = Integer.parseInt(execution.getParameter("index"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ux/UxPertemuanAction.java:30");
			// TODO: handle exception
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, execution.getParameter("id"));

		PertemuanHelper pertemuanHelper = new PertemuanHelper(tbmuser.getMahasiswa(),
				tbmuser.getBiodataCalonMahasiswa());
		pertemuanHelper.window = window;
		pertemuanHelper.tampilSelesai = false;
		pertemuanHelper.display(pertemuan, new DataLoader() {

			@Override
			public void loadData(Object value) {

			}
		}, index);

	}

}
