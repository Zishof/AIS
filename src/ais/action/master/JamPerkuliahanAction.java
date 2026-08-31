package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.JamPerkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JamPerkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk jam perkuliahan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchketerangan}, {@code Combobox
 * searchfakultas}, {@code Combobox searchprogram}, {@code Combobox program}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); penghapusan/pembatalan
 * ({@code onDelete()}); operasi domain lain ({@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JamPerkuliahanAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchfakultas;
	private Combobox searchprogram;
	private Combobox program;
	private Combobox searchjurusan;
	private Checkbox searchaktif;

	private Textbox nama;
	private MyTimebox mulai;
	private MyTimebox sampai;
	private Combobox jurusan;
	private Combobox fakultas;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JamPerkuliahan jamPerkuliahan;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Decimalbox sks;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.initPrograms(searchprogram);
		Common.checkProgramString(searchprogram, true);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "waktuMulai", "waktuSelesai", "sks", "fakultas", "jurusan",
				"program", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JamPerkuliahan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JamPerkuliahan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jamPerkuliahan = (JamPerkuliahan) obj;
		init(jamPerkuliahan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener, JamPerkuliahan jamPerkuliahan)
			throws Exception {
		JamPerkuliahanAction jamPerkuliahanAction = new JamPerkuliahanAction();
		jamPerkuliahanAction.eventListener = eventListener;
		jamPerkuliahanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(jamPerkuliahanAction.addWindow);
		jamPerkuliahanAction.addWindow.setHeight("350px");
		jamPerkuliahanAction.addWindow.setWidth("550px");

		jamPerkuliahanAction.init(jamPerkuliahan);

		jamPerkuliahanAction.addWindow.setVisible(true);
		jamPerkuliahanAction.addWindow.onModal();
	}

	class JamPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JamPerkuliahan jamPerkuliahan = (JamPerkuliahan) arg1;

			RevisiHelper.createNewRevisi(JamPerkuliahan.class, jamPerkuliahan, jamPerkuliahan.getNama())
					.setParent(arg0);

			new Label(jamPerkuliahan.getWaktuMulai() == null ? "Semua" : jamPerkuliahan.getWaktuMulai())
					.setParent(arg0);
			new Label(jamPerkuliahan.getWaktuSelesai() == null ? "Semua" : jamPerkuliahan.getWaktuSelesai())
					.setParent(arg0);
			new Label(jamPerkuliahan.getJurusan() == null ? "Semua" : jamPerkuliahan.getJurusan().getNama())
					.setParent(arg0);
			new Label(jamPerkuliahan.getFakultas() == null ? "Semua" : jamPerkuliahan.getFakultas().getNama())
					.setParent(arg0);
			new Label(jamPerkuliahan.getProgram() == null ? "Semua" : jamPerkuliahan.getProgram()).setParent(arg0);
			new Label(jamPerkuliahan.getSks() == null ? "-" : jamPerkuliahan.getSks().toString()).setParent(arg0);
			new Label(jamPerkuliahan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jamPerkuliahan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jamPerkuliahan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jamPerkuliahan);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jamPerkuliahan, JamPerkuliahanAction.this).setParent(arg0);

		}

	}

	public static void onDelete(JamPerkuliahan jamPerkuliahan) {
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		String sql = "update perkuliahan set jam_perkuliahan = null where jam_perkuliahan = " + jamPerkuliahan.getId()
				+ ";";
		session.createSQLQuery(sql).executeUpdate();
		sql = "update template_perkuliahan_detail set jam_perkuliahan = null where jam_perkuliahan = "
				+ jamPerkuliahan.getId() + ";";
		session.createSQLQuery(sql).executeUpdate();
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		Common.refreshDelete(jamPerkuliahan);
	}

	public void onAdd(Event event) throws Exception {
		init(new JamPerkuliahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JamPerkuliahan jamPerkuliahan) {
		Tbmuser tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		this.jamPerkuliahan = jamPerkuliahan;
		addWindow.setTitle(jamPerkuliahan.getId() == null ? "Tambah Jam Perkuliahan" : "Ubah Jam Perkuliahan");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam Perkuliahan *"));
		row.appendChild(nama = new Textbox(jamPerkuliahan.getNama() == null ? "" : jamPerkuliahan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai *"));
		row.appendChild(mulai = new MyTimebox(jamPerkuliahan.getMulai()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai *"));
		row.appendChild(sampai = new MyTimebox(jamPerkuliahan.getSampai()));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS"));
		row.appendChild(
				sks = new Decimalbox(jamPerkuliahan.getSks() == null ? null : new BigDecimal(jamPerkuliahan.getSks())));
		sks.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas,
				jamPerkuliahan.getJurusan() == null || jamPerkuliahan.getJurusan().getFakultas() == null
						? tbmuser.ambilFakultas()
						: jamPerkuliahan.getJurusan().getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				jamPerkuliahan.getJurusan() == null ? tbmuser.ambilJurusan() : jamPerkuliahan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = new Combobox();
		Common.initPrograms(program);
		Common.checkProgramString(program, true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jamPerkuliahan.getKeterangan() == null ? "" : jamPerkuliahan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, JamPerkuliahanAction.this.jamPerkuliahan));
					}

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jam Perkuliahan",
					"Kolom Jam Perkuliahan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jam Perkuliahan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (mulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jam mulai",
					"Kolom Jam mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jam mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (sampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jam sampai",
					"Kolom Jam sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jam sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		JamPerkuliahanDao jamPerkuliahanDao = DaoFactory.getInstance().getJamPerkuliahanDao();
		if (jamPerkuliahan.getId() != null) {
			jamPerkuliahan = jamPerkuliahanDao.load(jamPerkuliahan.getId());

		}

		jamPerkuliahan.setNama(nama.getValue());
		jamPerkuliahan.setKeterangan(keterangan.getValue());
		jamPerkuliahan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		jamPerkuliahan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		jamPerkuliahan.setMulai(mulai.getValue());
		jamPerkuliahan.setSampai(sampai.getValue());

		jamPerkuliahan.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		jamPerkuliahan.setSks(sks.getValue() == null ? null : sks.getValue().intValue());

		if (jamPerkuliahan.getId() != null) {
			jamPerkuliahanDao.update(jamPerkuliahan);
		} else {
			jamPerkuliahanDao.save(jamPerkuliahan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JamPerkuliahan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("mulai"));
		if (order)
			criteria.addOrder(Order.asc("sampai"));
		criteria.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchketerangan == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<JamPerkuliahan> jamPerkuliahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jamPerkuliahan);
		grid.setRowRenderer(new JamPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}
}
