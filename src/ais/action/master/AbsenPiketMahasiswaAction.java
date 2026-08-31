package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
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
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.DetailAbsenPiketMahasiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AbsenPiketMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AbsenPiketPeserta;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk absen piket mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchketerangan}, {@code Textbox searchsiswa}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchta}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAbsen()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class AbsenPiketMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	protected Textbox searchketerangan;
	protected Textbox searchsiswa;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchta;
	private Combobox searchsmt;

	private AmbilDataPegawaiBanbox searchpegawai;

	private Combobox jurusan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AbsenPiketMahasiswa absenPiketMahasiswa;
	private MyToolbarbuttonConfig add;
	private Combobox fakultas;
	private Combobox tahunAjaran;
	private Combobox semester;

	private Tbmuser tbmuser;
	private MyDatebox tanggal;

	private MyDatebox start;
	private MyDatebox end;

	protected Tabpanel absenPanel;

	private PerguruanTinggi perguruanTinggi;
	private AmbilDataPegawaiBanbox pegawai;

	public void onAbsen(Event event) {

		if (absenPanel.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(absenPanel);
			MyInclude iframe = new MyInclude("/welsis.zul");
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
		Common.generateTahunAjaran(searchta);

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		tbmuser = Common.getCurrentUser();

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

		String[] contents1 = new String[] { "id", "tahunAjaran", "semester", "tanggal", "kelas", "pegawai", "jurusan",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents1);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AbsenPiketMahasiswa.class, contents1);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	class AbsenPiketMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailAbsenPiketMahasiswaHelper detailAbsenPiketMahasiswaHelper = new DetailAbsenPiketMahasiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AbsenPiketMahasiswa absenPiketMahasiswa = (AbsenPiketMahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						detailAbsenPiketMahasiswaHelper.displayDetailPA(absenPiketMahasiswa, detail, addWindow);

					}

				}

			});

			new Label(absenPiketMahasiswa.getTahunAjaran() + "/" + absenPiketMahasiswa.getSemester()).setParent(arg0);
			RevisiHelper.createNewRevisi(AbsenPiketMahasiswa.class, absenPiketMahasiswa,
					Common.dateFormat5.get().format(absenPiketMahasiswa.getTanggal())).setParent(arg0);
			new Label(absenPiketMahasiswa.getJurusan() == null ? "" : absenPiketMahasiswa.getJurusan().getNama())
					.setParent(arg0);
			new Label(absenPiketMahasiswa.getFakultas() == null ? "" : absenPiketMahasiswa.getFakultas().getNama())
					.setParent(arg0);

			new Label(absenPiketMahasiswa.getPegawai() == null ? "" : absenPiketMahasiswa.getPegawai().getNama())
					.setParent(arg0);
			new Label(absenPiketMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, absenPiketMahasiswa, AbsenPiketMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AbsenPiketMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		absenPiketMahasiswa = (AbsenPiketMahasiswa) obj;
		init(absenPiketMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final AbsenPiketMahasiswa absenPiketMahasiswa) throws Exception {
		this.absenPiketMahasiswa = absenPiketMahasiswa;
		addWindow.setTitle(absenPiketMahasiswa.getId() == null ? "Tambah Presensi / Kehadiran" : "Ubah Presensi / Kehadiran");
		addWindow.setWidth("550px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				absenPiketMahasiswa.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, absenPiketMahasiswa.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		fakultas = new Combobox();
		jurusan = new Combobox();

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, absenPiketMahasiswa.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		row.appendChild(jurusan);
		Common.pilihJurusan(jurusan, absenPiketMahasiswa.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		if (absenPiketMahasiswa.getId() == null && tbmuser.getPegawai() != null) {
			absenPiketMahasiswa.setPegawai(tbmuser.getPegawai());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Petugas"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());

		pegawai.setAttribute("pegawai", absenPiketMahasiswa.getPegawai());
		pegawai.setAttribute("myValue", absenPiketMahasiswa.getPegawai());
		pegawai.setValue(absenPiketMahasiswa.getPegawai() == null ? "" : absenPiketMahasiswa.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Absen *"));
		row.appendChild(tanggal = new MyDatebox(absenPiketMahasiswa.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(absenPiketMahasiswa.getKeterangan()));
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

		Session session = HibernateUtil.currentSession();
		try {
			if (absenPiketMahasiswa.getId() != null) {
				absenPiketMahasiswa = (AbsenPiketMahasiswa) session.load(AbsenPiketMahasiswa.class,
						absenPiketMahasiswa.getId());

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/AbsenPiketMahasiswaAction.java:396");
			// TODO: handle exception
		}
		absenPiketMahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
		absenPiketMahasiswa.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));

		absenPiketMahasiswa.setKeterangan(keterangan.getValue());
		absenPiketMahasiswa.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		absenPiketMahasiswa.setSemester((Integer) semester.getSelectedItem().getValue());
		absenPiketMahasiswa.setTanggal(tanggal.getValue());
		absenPiketMahasiswa.setPerguruanTinggi(perguruanTinggi);
		absenPiketMahasiswa.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));

		Common.refreshSaveOrUpdate(session, absenPiketMahasiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AbsenPiketMahasiswa.class)

				.add(Restrictions.or(Restrictions.isNull("perguruanTinggi"),
						Restrictions.eq("perguruanTinggi", perguruanTinggi)))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

		;

		if (!searchsiswa.getValue().trim().isEmpty()) {
			List<Long> kelas = session.createCriteria(AbsenPiketPeserta.class)
					.setProjection(Projections.property("absenPiketMahasiswa.id"))
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

					.add(

							Restrictions.or(
									Restrictions.ilike("mahasiswa.nim", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),

									Restrictions.or(
											Restrictions.ilike("mahasiswa.nama", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("siswa.nomorInduk",
																	searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("siswa.nomorIndukNasional",
																	searchsiswa.getValue().trim(),
																	MatchMode.ANYWHERE))))))
					.list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("id", kelas));
			}
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai")),
								Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai")))))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

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
		Common.initPaging(initCriteria(false), paging);

		List<AbsenPiketMahasiswa> absenPiketMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(absenPiketMahasiswa);
		grid.setRowRenderer(new AbsenPiketMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
