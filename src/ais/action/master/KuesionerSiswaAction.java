package ais.action.master;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;

import ais.action.master.helper.generic.AngketGuruWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk kuesioner siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Siswa siswa}, {@code Tabpanel absensiSp}, {@code Combobox ta}, {@code Combobox smt}, {@code
 * Center rowData}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code load()}); operasi domain lain ({@code onAbsensiSp()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class KuesionerSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220401469178L;
	protected MyWindow addWindow;
	protected MyGrid grid;

	protected Siswa siswa;

	protected Tabpanel absensiSp;

	protected Combobox ta;
	protected Combobox smt;

	public void onAbsensiSp(Event event) {

		if (absensiSp.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("97%");
			window.setWidth("97%");
			window.setParent(absensiSp);
			MyInclude iframe = new MyInclude("/common/checklist_penilaian_umum.zul?siswa=" + siswa.getId());
			iframe.setParent(window);
		}
	}

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
		boolean cek = execution.getParameter("pass") == null;
		if (cek) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getSiswa() == null) {
			alert("Anda harus login sebagai siswa");
			return;
		}
		siswa = tbmuser.getSiswa();

		Common.generateTahunAjaran(ta);

		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		smt.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		smt.appendChild(comboitem);
		if (smt != null) { smt.setCols(2); }

		Common.selectComboItem(smt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (smt != null) { smt.setReadonly(true); }

		onSearchDefault(null);

	}

	private Center rowData;

	public void onSearchDefault(Event event) {
		load();
	}

	@SuppressWarnings("unchecked")
	private void load() {

		String ta = (String) this.ta.getSelectedItem().getValue();
		Integer smt = (Integer) this.smt.getSelectedItem().getValue();

		Common.clear(rowData);

		Session session = HibernateUtil.currentSession();

		String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id=" + siswa.getId()
				+ " and kelas_id is not null and aktif=true group by kelas_id)";
		Criterion criterionKls = Restrictions.sqlRestriction(sql);

		sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
				+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
		Criterion criterionLes = Restrictions.sqlRestriction(sql);

		Criterion criterion = Restrictions.or(criterionKls, criterionLes);

		List<Long> jadwalPelajarans = session.createCriteria(JadwalPelajaran.class).add(criterion)
				.add(Restrictions.eq("tahunAjaran", ta)).setProjection(Projections.property("id"))
				.add(Restrictions.eq("semester", smt)).list();

		AngketGuruWindow angketDosenWindow = new AngketGuruWindow(ta,
				smt.equals(1) ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP, jadwalPelajarans, siswa, addWindow,
				true);
		Row rowUtama = Common.tampilanScroll1(rowData);
		rowUtama.appendChild(angketDosenWindow);

	}

}
