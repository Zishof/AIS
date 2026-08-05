package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PenugasanGuruMengajarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchguru;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;

	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;
	private PenugasanGuruMengajar penugasanGuruMengajar;
	private MyWindow addWindow;
	private Textbox kode;
	private AmbilDataGuruBanbox guru;
	private Textbox keterangan;
	private Combobox tahunAjaran;
	private Combobox semester;
	private MyDatebox tanggalSuratTugas;
	private MyDatebox tmtSuratTugas;

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
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.generateTahunAjaran(searchTahunAjaran);

		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(JadwalPelajaran.GANJIL); }
		if (comboitem != null) { comboitem.setValue(1); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(JadwalPelajaran.GENAP); }
		if (comboitem != null) { comboitem.setValue(2); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? 1 : 2);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "guru", "sekolah", "program", "tahunAkademik", "semester", "kode",
				"tanggalSuratTugas", "tmtSuratTugas", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenugasanGuruMengajar.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenugasanGuruMengajar.class, contents);
		if (upload != null) { upload.setVisible(edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Generate No. SK Berdasarkan Jadwal",
				"/img/svg/check2.svg");
		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {

						Session session = HibernateUtil.currentNativeSession();
						// Thread latar TIDAK lewat FilterJSP -> WAJIB tutup native session sendiri (cegah bocor c3p0).
						try {
							List<JadwalPelajaran> longs = ConstantValues.simpleList(
									session.createCriteria(JadwalPelajaran.class)
											.add(Restrictions.eq("tahunAjaran",
													searchTahunAjaran.getSelectedItem().getValue()))

											.add(Restrictions.eq("semester",
													searchJenisSemester.getSelectedItem().getValue())),
									JadwalPelajaran.class);

							int size = longs.size();
							int index = 0;
							for (JadwalPelajaran jadwalPelajaran : longs) {
								index++;
								try {

									label.setValue("Singkronkan penugasan " + jadwalPelajaran.infoSimple() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
									for (Guru guru : gurus) {

										Common.getPenugasanGuruMengajar(jadwalPelajaran.getSekolah().getId(),
												jadwalPelajaran.getProgram(), jadwalPelajaran.getTahunAjaran(),
												jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL
														: Perkuliahan.GENAP,
												guru);

									}

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

							}
							label.setValue("");
						} finally {
							try { session.clear(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/sekolah/PenugasanGuruMengajarAction.java:205");}
							try { session.disconnect(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/sekolah/PenugasanGuruMengajarAction.java:206");}
							try { session.close(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/sekolah/PenugasanGuruMengajarAction.java:207");}
						}
					}
				}).start();

			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class PenugasanGuruMengajarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenugasanGuruMengajar penugasanGuruMengajar = (PenugasanGuruMengajar) arg1;
			new Label(penugasanGuruMengajar.getGuru() == null ? "" : penugasanGuruMengajar.getGuru().getNama())
					.setParent(arg0);
			RevisiHelper.createNewRevisi(PenugasanGuruMengajar.class, penugasanGuruMengajar,
					penugasanGuruMengajar.getNama()).setParent(arg0);
			new Label(penugasanGuruMengajar.getSekolah() == null ? "" : penugasanGuruMengajar.getSekolah().getNama())
					.setParent(arg0);

			final MyTextbox kode = new MyTextbox(penugasanGuruMengajar.getKode());
			final MyDatebox tanggalSuratTugas = new MyDatebox(penugasanGuruMengajar.getTanggalSuratTugas());
			final MyDatebox tmtSuratTugas = new MyDatebox(penugasanGuruMengajar.getTmtSuratTugas());
			final MyTextbox keterangan = new MyTextbox(penugasanGuruMengajar.getKeterangan());

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penugasanGuruMengajar.setKode(kode.getValue());
					penugasanGuruMengajar.setTmtSuratTugas(tmtSuratTugas.getValue());
					penugasanGuruMengajar.setTanggalSuratTugas(tanggalSuratTugas.getValue());
					penugasanGuruMengajar.setKeterangan(keterangan.getValue());

					Common.refreshUpdate(penugasanGuruMengajar);
				}
			};

			kode.setWidth("95%");
			kode.setParent(arg0);

			tanggalSuratTugas.setWidth("95%");
			tanggalSuratTugas.setParent(arg0);

			tmtSuratTugas.setWidth("95%");
			tmtSuratTugas.setParent(arg0);

			keterangan.setWidth("95%");
			keterangan.setParent(arg0);

			kode.addEventListener("onChange", eventListener);
			tanggalSuratTugas.addEventListener("onChange", eventListener);
			tmtSuratTugas.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);

			Common.copyEditDeleteButtons(edit, delete, penugasanGuruMengajar, PenugasanGuruMengajarAction.this)
					.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenugasanGuruMengajar.class).createAlias("guru", "guru");

		if (order)
			criteria.addOrder(Order.desc("guru.nama")).addOrder(Order.desc("tahunAkademik"))
					.addOrder(Order.asc("semester"));
		criteria

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester",
										searchJenisSemester.getSelectedItem().getValue().equals(1) ? Perkuliahan.GANJIL
												: Perkuliahan.GENAP))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchguru.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("guru.nama", searchguru.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("guru.kode", searchguru.getValue().trim(), MatchMode.ANYWHERE))

				)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenugasanGuruMengajar> penugasanGuruMengajar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penugasanGuruMengajar);
		grid.setRowRenderer(new PenugasanGuruMengajarRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void onAdd(Event event) throws Exception {
		init(new PenugasanGuruMengajar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		penugasanGuruMengajar = (PenugasanGuruMengajar) obj;
		init(penugasanGuruMengajar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenugasanGuruMengajar penugasanGuruMengajar) {
		this.penugasanGuruMengajar = penugasanGuruMengajar;
		addWindow.setTitle(penugasanGuruMengajar.getId() == null ? "Tambah Penugasan Guru" : "Ubah Penugasan Guru");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru *"));
		row.appendChild(guru = new AmbilDataGuruBanbox(false));
		guru.setAttribute("guru", penugasanGuruMengajar.getGuru());
		guru.setValue(penugasanGuruMengajar.getGuru() == null ? "" : penugasanGuruMengajar.getGuru().getNama());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK"));
		row.appendChild(kode = new Textbox(penugasanGuruMengajar.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tanggalSuratTugas = new MyDatebox(penugasanGuruMengajar.getTanggalSuratTugas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TMT SK"));
		row.appendChild(tmtSuratTugas = new MyDatebox(penugasanGuruMengajar.getTmtSuratTugas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				penugasanGuruMengajar.getTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, penugasanGuruMengajar.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(penugasanGuruMengajar.getKeterangan()));
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
		if (guru.getAttribute("guru") == null) {
			MyMessageboxConfig.show("Guru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penugasanGuruMengajar.getId() != null) {
			penugasanGuruMengajar = (PenugasanGuruMengajar) session.load(PenugasanGuruMengajar.class,
					penugasanGuruMengajar.getId());

		}

		penugasanGuruMengajar.setKode(kode.getValue());
		penugasanGuruMengajar.setGuru((Guru) guru.getAttribute("guru"));
		penugasanGuruMengajar.setTmtSuratTugas(tmtSuratTugas.getValue());
		penugasanGuruMengajar.setTanggalSuratTugas(tanggalSuratTugas.getValue());
		penugasanGuruMengajar.setKeterangan(keterangan.getValue());
		penugasanGuruMengajar.setTahunAkademik((String) tahunAjaran.getSelectedItem().getValue());
		penugasanGuruMengajar.setSemester((String) semester.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, penugasanGuruMengajar);

		return true;
	}

}
