package ais.action.master;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.ProgramDataMahasiswaDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.ProgramMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk program mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchMahasiswa}, {@code Textbox
 * nama}, {@code Combobox program}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onManajemenString()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class ProgramMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchMahasiswa;

	private Textbox nama;
	private Combobox program;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ProgramMahasiswa programMahasiswa;
	private MyToolbarbuttonConfig add;
	private Decimalbox smtMulai;
	private Decimalbox smtSampai;
	private Combobox program2;
	private Decimalbox smtMulai2;
	private Decimalbox smtSampai2;
	private Combobox program3;
	private Decimalbox smtMulai3;
	private Decimalbox smtSampai3;

	private Tabpanel manajemenString;

	public void onManajemenString(Event event) {
		if (manajemenString.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenString);
			MyInclude iframe = new MyInclude("/pages/master/status_awal_mahasiswa.zul");
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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "program", "smtMulai", "smtSampai"

				, "program2", "smtMulai2", "smtSampai2", "program3", "smtMulai3", "smtSampai3"

				, "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProgramMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ProgramMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ProgramMahasiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ProgramMahasiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ProgramMahasiswaAction
	 */
	class ProgramMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProgramMahasiswa programMahasiswa = (ProgramMahasiswa) arg1;

			(new ProgramDataMahasiswaDetailAction(programMahasiswa)).setParent(arg0);

			RevisiHelper.createNewRevisi(ProgramMahasiswa.class, programMahasiswa, programMahasiswa.getNama())
					.setParent(arg0);
			new Label(programMahasiswa.getProgram() == null ? "" : programMahasiswa.getProgram()).setParent(arg0);
			new Label(programMahasiswa.getSmtMulai() + " sd " + programMahasiswa.getSmtSampai()).setParent(arg0);

			new Label(programMahasiswa.getProgram2() == null ? "" : programMahasiswa.getProgram2()).setParent(arg0);
			new Label(programMahasiswa.getSmtMulai2() + " sd " + programMahasiswa.getSmtSampai2()).setParent(arg0);

			new Label(programMahasiswa.getProgram3() == null ? "" : programMahasiswa.getProgram3()).setParent(arg0);
			new Label(programMahasiswa.getSmtMulai3() + " sd " + programMahasiswa.getSmtSampai3()).setParent(arg0);

			new Label(programMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, programMahasiswa, ProgramMahasiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ProgramMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		programMahasiswa = (ProgramMahasiswa) obj;
		init(programMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ProgramMahasiswa programMahasiswa) {
		this.programMahasiswa = programMahasiswa;
		addWindow.setTitle(programMahasiswa.getId() == null ? "Tambah Program Mahasiswa" : "Ubah Program Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Program *"));
		row.appendChild(nama = new Textbox(programMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program (I) *"));
		program = new Combobox();
		Common.initPrograms(program);
		Common.selectComboItem(program, programMahasiswa.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (I)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai = new Decimalbox(new BigDecimal(programMahasiswa.getSmtMulai())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai = new Decimalbox(new BigDecimal(programMahasiswa.getSmtSampai())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program (II) *"));
		program2 = new Combobox();
		Common.initPrograms(program2);
		Common.selectComboItem(program2, programMahasiswa.getProgram2());
		row.appendChild(program2);
		program2.setWidth("90%");
		program2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (II)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai2 = new Decimalbox(new BigDecimal(programMahasiswa.getSmtMulai2())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai2 = new Decimalbox(new BigDecimal(programMahasiswa.getSmtSampai2())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program (III) *"));
		program3 = new Combobox();
		Common.initPrograms(program3);
		Common.selectComboItem(program3, programMahasiswa.getProgram3());
		row.appendChild(program3);
		program3.setWidth("90%");
		program3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (III)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai3 = new Decimalbox(new BigDecimal(programMahasiswa.getSmtMulai3())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai3 = new Decimalbox(new BigDecimal(programMahasiswa.getSmtSampai3())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(programMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program Mahasiswa",
					"Kolom Nama Program Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Program Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program (I)",
					"Kolom Program (I) belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Program (I).",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (programMahasiswa.getId() != null) {
			programMahasiswa = (ProgramMahasiswa) session.load(ProgramMahasiswa.class, programMahasiswa.getId());

		}
		programMahasiswa.setNama(nama.getValue());

		programMahasiswa
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));

		programMahasiswa.setSmtMulai(smtMulai.getValue() == null ? null : smtMulai.getValue().intValue());
		programMahasiswa.setSmtSampai(smtSampai.getValue() == null ? null : smtSampai.getValue().intValue());

		programMahasiswa.setProgram2(
				(String) (program2.getSelectedItem() == null ? null : program2.getSelectedItem().getValue()));

		programMahasiswa.setSmtMulai2(smtMulai2.getValue() == null ? null : smtMulai2.getValue().intValue());
		programMahasiswa.setSmtSampai2(smtSampai2.getValue() == null ? null : smtSampai2.getValue().intValue());

		programMahasiswa.setProgram3(
				(String) (program3.getSelectedItem() == null ? null : program3.getSelectedItem().getValue()));

		programMahasiswa.setSmtMulai3(smtMulai3.getValue() == null ? null : smtMulai3.getValue().intValue());
		programMahasiswa.setSmtSampai3(smtSampai3.getValue() == null ? null : smtSampai3.getValue().intValue());

		programMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, programMahasiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		List<Long> ids = new ArrayList<Long>();

		Session session = HibernateUtil.currentSession();

		if (!searchMahasiswa.getValue().trim().isEmpty()) {
			ids = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(
							Restrictions.ilike("nim", searchMahasiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("nama", searchMahasiswa.getValue().trim(), MatchMode.ANYWHERE)))
					.add(Restrictions.isNotNull("programMahasiswa"))
					.setProjection(Projections.groupProperty("programMahasiswa.id")).list();
		}

		Criteria criteria = session.createCriteria(ProgramMahasiswa.class);

		if (!ids.isEmpty()) {
			criteria.add(Restrictions.in("id", ids));
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ProgramMahasiswa> programMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(programMahasiswa);
		grid.setRowRenderer(new ProgramMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
