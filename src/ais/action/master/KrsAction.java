package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.KrsMahasiswaDataRenderer;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk krs. Tipe ini merupakan titik masuk UI yang menghubungkan event layar
 * dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Label searchnim}, {@code Label searchjurusan}, {@code Label searchnama}, {@code Label
 * searchfakultas}, {@code Label searchangkatan}, {@code Label searchprogram}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefaultKeDatabase()},
 * {@code onSearchDefault()}); operasi domain lain ({@code onKrsSp()}, {@code onMkRemedial()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KrsAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyWindow addWindow;
	protected MyGrid grid;
	protected Label searchnim;
	protected Label searchjurusan;
	protected Label searchnama;
	protected Label searchfakultas;
	protected Label searchangkatan;
	protected Label searchprogram;
	protected Checkbox searchtampilin;

	protected Mahasiswa mahasiswa;

	protected MyColumnConfig colSemester;
	protected MyColumnConfig colTahapan;

	protected MyColumnConfig colIpk;
	protected MyColumnConfig colSks;
	protected MyColumnConfig colKet;
	protected MyColumnConfig colKomen;
	protected MyColumnConfig colCatatan;

	protected Integer semesterPendek;
	protected boolean remedial = false;

	protected Tabpanel krsSp;

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;

	public void onKrsSp(Event event) {

		if (krsSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(krsSp);
			include.setSrc("/pages/master/krs_sp.zul");
		}
	}

	protected Tabpanel mkRemedial;

	public void onMkRemedial(Event event) {

		if (mkRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(mkRemedial);
			include.setSrc("/pages/master/krs_remedial.zul");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
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
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}

		mahasiswa = tbmuser.getMahasiswa();
		if (searchnim != null) { searchnim.setValue(mahasiswa.getNim()); }
		if (searchjurusan != null) { searchjurusan.setValue(mahasiswa.getJurusan().getNama()); }
		if (searchnama != null) { searchnama.setValue(mahasiswa.getNama()); }
		if (searchfakultas != null) { searchfakultas.setValue(mahasiswa.getJurusan().getFakultas().getNama()); }
		if (searchangkatan != null) { searchangkatan.setValue(mahasiswa.getTahunangkatan() + " (" + mahasiswa.getSemesterMulai() + ")"); }
		if (searchprogram != null) { searchprogram.setValue(mahasiswa.getProgram() == null ? "" : mahasiswa.getProgram()); }

		if (ConstantValues.jumlahTahapan.isEmpty()) {
			ConstantValues.initJumlahTahapan();
		}
		if (ConstantValues.aktifkanTahapan
				&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2) {
			colTahapan.setWidth("10%");
		}

		for (Integer i = 1; i <= (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus()>0? mahasiswa.getSemesterLulus() : 40); i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterMulai.appendChild(comboitem);
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterSampai.appendChild(comboitem);
		}

		int defaultPemilihanSemesterMulai = Common
				.getKonfigurasi("default_pemilihan_semester_mulai", mahasiswa.currentSemester() + "").niliaInteger();
		int defaultPemilihanSemesterSampai = Common
				.getKonfigurasi("default_pemilihan_semester_sampai", mahasiswa.currentSemester() + "").niliaInteger();
		
		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterMulai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterMulai = mahasiswa.getSemesterLulus();
		}
		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterSampai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterSampai = mahasiswa.getSemesterLulus();
		}


		if (defaultPemilihanSemesterMulai != defaultPemilihanSemesterSampai) {
			Common.selectComboItem(semesterMulai, defaultPemilihanSemesterMulai);
			Common.selectComboItem(semesterSampai, defaultPemilihanSemesterSampai);
		} else {
			Common.selectComboItem(semesterMulai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());
			Common.selectComboItem(semesterSampai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());
		}

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	public void onSearchDefaultKeDatabase(Event event) {

		if (searchtampilin != null && !searchtampilin.isChecked()) {
			searchtampilin.setChecked(true);
		}
		mahasiswa.reInit();
		colIpk.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colSks.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKet.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKomen.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colCatatan.setVisible(searchtampilin != null && searchtampilin.isChecked());

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new KrsMahasiswaDataRenderer(true, mahasiswa, semesterPendek, remedial,
				searchtampilin != null && searchtampilin.isChecked()));
		grid.setModelCheckMobile(strset);
	}

	public void onSearchDefault(Event event) {

		colIpk.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colSks.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKet.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKomen.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colCatatan.setVisible(searchtampilin != null && searchtampilin.isChecked());

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new KrsMahasiswaDataRenderer(false, mahasiswa, semesterPendek, remedial,
				searchtampilin != null && searchtampilin.isChecked()));
		grid.setModelCheckMobile(strset);

	}

}
