package ais.action.master.penelitiandanpengabdian;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyWindow;

import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;

/**
 * Controller/action ZK untuk pengajuan penelitian dan pengabdian. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code pengajuanPenelitianDanPengabdianHelper},
 * {@code edit}, {@code delete}; konfigurasi constructor: {@code delete}, {@code edit}, {@code jenis}. Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyWindow
 */
public class PengajuanPenelitianDanPengabdianAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(null);

	@SuppressWarnings("unused")
	private boolean edit = false;
	@SuppressWarnings("unused")
	private boolean delete = false;

	public PengajuanPenelitianDanPengabdianAction() throws Exception {
		super();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		TipePenelitianDanPengabdian jenis = null;
		if (ExecutionsCtrl.getCurrent().getParameter("jenis") != null) {
			jenis = (TipePenelitianDanPengabdian) HibernateUtil.currentSession()
					.createCriteria(TipePenelitianDanPengabdian.class)
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("jenis"))))
					.uniqueResult();
		}

		MyWindow addWindowPengajuan = new MyWindow();
		addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, null, PengumumanAkademis.UNTUK_UMUM, null, this,
				addWindowPengajuan, jenis, "100%");
	}

	public PengajuanPenelitianDanPengabdianAction(String arg0, String arg1, boolean arg2) throws Exception {
		super(arg0, arg1, arg2);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		TipePenelitianDanPengabdian jenis = null;
		if (ExecutionsCtrl.getCurrent().getParameter("jenis") != null) {
			jenis = (TipePenelitianDanPengabdian) HibernateUtil.currentSession()
					.createCriteria(TipePenelitianDanPengabdian.class)
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("jenis"))))
					.uniqueResult();
		}

		MyWindow addWindowPengajuan = new MyWindow();
		addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, null, PengumumanAkademis.UNTUK_UMUM, null, this,
				addWindowPengajuan, jenis, "100%");
	}

}
