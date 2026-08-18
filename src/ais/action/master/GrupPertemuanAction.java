package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
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
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AktifitasGrupPertemuanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupPertemuan;
import ais.database.model.JenisLayananKepadaMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Ruang;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class GrupPertemuanAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchtahunakademik;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Textbox searchtahun;
	private Combobox searchprogram;
	private Combobox searchjenis;
	private Textbox searchkelas;

	private AmbilDataDosenBanbox searchdosen;

	private Textbox nama;
	private MyDatebox tanggal;
	private Timebox waktuMulai;
	private Timebox waktuSelesai;
	private Combobox fakultas;
	private Combobox jurusan;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataRuangBanbox ruang;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox kelas;
	private Textbox keterangan;

	private Integer semesterPendek = null;

	private GrupPertemuan grupPertemuan;
	private Textbox tahunAngkatan;

	private Tabpanel jenisLayananMahasiswa;
	private Tabpanel laporanGrupPertemuan;
	private Combobox program;
	private MyCombobox jenis;
	private MyCombobox jenisLayananKepadaMahasiswa;

	private Rows rowsMahasiswa = null;

	private String selectedJenis = null;

	protected Tabpanel sejarahKrs;

	private MyToolbarbuttonConfig add;

	public void onSejarah(Event event) {

		if (sejarahKrs.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(sejarahKrs);
			include.setSrc("/pages/master/krs_mahasiswa.zul");
		}
	}

	public void onTampilJenisLayanan(Event event) {
		if (jenisLayananMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisLayananMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/jenis_layanan_kepada_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onTampilGrupPertemuan(Event event) {
		if (laporanGrupPertemuan.getChildren().size() == 0) {
			// LaporanRekapitulasiGrupPertemuan laporanRekapitulasiGrupPertemuan
			// = new LaporanRekapitulasiGrupPertemuan();
			// laporanRekapitulasiGrupPertemuan.setHeight("100%");
			// laporanRekapitulasiGrupPertemuan.setWidth("100%");
			// laporanRekapitulasiGrupPertemuan.setParent(laporanGrupPertemuan);
		}
	}

	protected Tabpanel bimbinganSkripsi;

	public void onTugasAkhir(Event event) {

		if (bimbinganSkripsi.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(bimbinganSkripsi);
			include.setSrc("/pages/master/mahasiswa_request_tugas_akhir.zul");
		}
	}

	private Tabpanel sidangSkripsi;

	public void onSidang(Event event) {
		if (sidangSkripsi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sidangSkripsi);
			MyInclude iframe = new MyInclude("/pages/master/skripsi.zul");
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

		if (execution.getParameter("jenis") != null) {
			selectedJenis = execution.getParameter("jenis");
		}

		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		Common.initPrograms(searchprogram);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyComboitemConfig comboitem = new MyComboitemConfig(GrupPertemuan.KRS_MAHASISWA);
		if (comboitem != null) { comboitem.setValue(GrupPertemuan.KRS_MAHASISWA); }
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.BIMBINGAN);
		if (comboitem != null) { comboitem.setValue(GrupPertemuan.BIMBINGAN); }
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.SIDANG);
		if (comboitem != null) { comboitem.setValue(GrupPertemuan.SIDANG); }
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.LAINNYA);
		if (comboitem != null) { comboitem.setValue(GrupPertemuan.LAINNYA); }
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchjenis.appendChild(comboitem);

		if (searchjenis != null) { searchjenis.setSelectedItem(comboitem); }
		if (searchjenis != null) { searchjenis.setReadonly(true); }

		if (selectedJenis != null) {
			Common.selectComboItem(searchjenis, selectedJenis);
			searchjenis.setDisabled(true);

			sejarahKrs.setVisible(selectedJenis.equals(GrupPertemuan.KRS_MAHASISWA));
			sejarahKrs.getLinkedTab().setVisible(selectedJenis.equals(GrupPertemuan.KRS_MAHASISWA));

			sidangSkripsi.setVisible(selectedJenis.equals(GrupPertemuan.SIDANG));
			sidangSkripsi.getLinkedTab().setVisible(selectedJenis.equals(GrupPertemuan.SIDANG));
			bimbinganSkripsi.setVisible(selectedJenis.equals(GrupPertemuan.BIMBINGAN));
			bimbinganSkripsi.getLinkedTab().setVisible(selectedJenis.equals(GrupPertemuan.BIMBINGAN));
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (jenisLayananMahasiswa != null) {
			jenisLayananMahasiswa.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			jenisLayananMahasiswa.getLinkedTab()
					.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}

		onSearchDefault(null);

		String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "pertemuan.catatan",
				"pertemuan.bukuRujukan1", "pertemuan.bukuRujukan2", "grupPertemuan.nama",
				"grupPertemuan.jenisLayananKepadaMahasiswa.nama", "grupPertemuan.dosen.nama",
				"grupPertemuan.tahunAkademik", "grupPertemuan.jenisSemester", "grupPertemuan.tanggal",
				"grupPertemuan.waktuMulai", "grupPertemuan.waktuSelesai", "grupPertemuan.keterangan",
				"grupPertemuan.catatan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PertemuanPunyaGrupPertemuan.class,
				new DataCriteria() {

					@SuppressWarnings("unchecked")
					@Override
					public Object initCriteria(boolean order) {
						List<Long> grupPertemuan = GrupPertemuanAction.this.initCriteria(true)
								.setProjection(Projections.property("id")).list();
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(PertemuanPunyaGrupPertemuan.class)
								.add(grupPertemuan.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("grupPertemuan.id", grupPertemuan));
					}
				}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	class GrupPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GrupPertemuan grupPertemuan = (GrupPertemuan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						AktifitasGrupPertemuanHelper aktifitasGrupPertemuanHelper = new AktifitasGrupPertemuanHelper();
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasGrupPertemuanHelper.initDetail(grupPertemuan, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(grupPertemuan.getDosen()).setParent(vbox);
			vbox.appendChild(new Label(grupPertemuan.getDosen().getNama()));

			Vbox a = RevisiHelper.createNewRevisi(GrupPertemuan.class, grupPertemuan, grupPertemuan.getNama());
			a.setParent(arg0);
			a.appendChild(new Label(grupPertemuan.getJenis()));
			a.appendChild(new Label(grupPertemuan.getJenisLayananKepadaMahasiswa() == null ? ""
					: grupPertemuan.getJenisLayananKepadaMahasiswa().getNama()));
			a.appendChild(new Label(grupPertemuan.getTahunAkademik()
					+ (grupPertemuan.getJenisSemester() == null ? "" : " / " + grupPertemuan.getJenisSemester())));

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(Common.dateFormat4.get().format(grupPertemuan.getTanggal())).setParent(vbox);
			new Label(grupPertemuan.getWaktuMulai() + " s.d " + grupPertemuan.getWaktuSelesai()).setParent(vbox);
			new Label(grupPertemuan.getRuang() == null ? "" : "Ruang : " + grupPertemuan.getRuang().getNama())
					.setParent(vbox);
			new Label(grupPertemuan.getKelas().trim().isEmpty() ? "" : "Kelas : " + grupPertemuan.getKelas())
					.setParent(vbox);

			new Label(grupPertemuan.getFakultas() == null ? "Semua" : grupPertemuan.getFakultas().getNama())
					.setParent(arg0);
			new Label(grupPertemuan.getJurusan() == null ? "Semua" : grupPertemuan.getJurusan().getNama())
					.setParent(arg0);
			new Label(grupPertemuan.getTahunAngkatan() == null ? "Semua" : grupPertemuan.getTahunAngkatan() + "")
					.setParent(arg0);
			new Label(grupPertemuan.getProgram() == null ? "Semua" : grupPertemuan.getProgram()).setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(grupPertemuan.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupPertemuan.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(grupPertemuan);
				}
			});

			new Label(grupPertemuan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(grupPertemuan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(grupPertemuan);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GrupPertemuan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Set<Mahasiswa> selectedMahasiswas;

	private void init(final GrupPertemuan grupPertemuan) throws Exception {
		this.grupPertemuan = grupPertemuan;
		selectedMahasiswas = new HashSet<Mahasiswa>();
		addWindow.setTitle(grupPertemuan.getId() == null ? "Tambah Konsultasi" : "Ubah Konsultasi");
		addWindow.setWidth("590px");
		addWindow.setHeight("99%");

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

		final East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		addWindow.setWidth("99%");
		east.setWidth("60%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			grupPertemuan.setDosen(tbmuser.ambilDosen());
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen *"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setAttribute("dosen", grupPertemuan.getDosen());
		dosen.setAttribute("myValue", grupPertemuan.getDosen());
		dosen.setValue(grupPertemuan.getDosen() == null ? "" : grupPertemuan.getDosen().getNama());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Konsultasi *"));
		row.appendChild(nama = new Textbox(grupPertemuan.getNama() == null ? "" : grupPertemuan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Konsultasi *"));
		row.appendChild(jenis = new MyCombobox());
		jenis.setWidth("90%");

		MyComboitemConfig comboitem = new MyComboitemConfig(GrupPertemuan.KRS_MAHASISWA);
		comboitem.setValue(GrupPertemuan.KRS_MAHASISWA);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.BIMBINGAN);
		comboitem.setValue(GrupPertemuan.BIMBINGAN);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.SIDANG);
		comboitem.setValue(GrupPertemuan.SIDANG);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupPertemuan.LAINNYA);
		comboitem.setValue(GrupPertemuan.LAINNYA);
		jenis.appendChild(comboitem);

		// comboitem = new MyComboitemConfig(GrupPertemuan.LAINNYA);
		// comboitem.setValue(GrupPertemuan.LAINNYA);
		// jenis.appendChild(comboitem);

		Common.selectComboItem(jenis, grupPertemuan.getJenis());
		jenis.setReadonly(true);

		if (selectedJenis != null && jenis.getSelectedItem() == null) {
			Common.selectComboItem(jenis, selectedJenis);
			jenis.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Layanan *"));
		row.appendChild(jenisLayananKepadaMahasiswa = new MyCombobox());
		jenisLayananKepadaMahasiswa.setWidth("90%");
		jenisLayananKepadaMahasiswa.setReadonly(true);

		EventListener jenisEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(jenisLayananKepadaMahasiswa);
				String j = (String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue());
				if (j != null) {
					if (j.equals(GrupPertemuan.KRS_MAHASISWA)) {
						Common.insertCombo(jenisLayananKepadaMahasiswa, "nama", "keterangan",
								JenisLayananKepadaMahasiswa.class, Restrictions.and(Restrictions.eq("pa", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					} else if (j.equals(GrupPertemuan.BIMBINGAN)) {
						Common.insertCombo(jenisLayananKepadaMahasiswa, "nama", "keterangan",
								JenisLayananKepadaMahasiswa.class, Restrictions.and(Restrictions.eq("bimbingan", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					} else if (j.equals(GrupPertemuan.SIDANG)) {
						Common.insertCombo(jenisLayananKepadaMahasiswa, "nama", "keterangan",
								JenisLayananKepadaMahasiswa.class, Restrictions.and(Restrictions.eq("revisi", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					} else {
						Common.insertCombo(jenisLayananKepadaMahasiswa, "nama", "keterangan",
								JenisLayananKepadaMahasiswa.class, Restrictions.and(Restrictions.eq("umum", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					}
				}

				jenisLayananKepadaMahasiswa.getParent()
						.setVisible(!jenisLayananKepadaMahasiswa.getChildren().isEmpty());
				jenisLayananKepadaMahasiswa.setVisible(!jenisLayananKepadaMahasiswa.getChildren().isEmpty());

				if (jenisLayananKepadaMahasiswa.getChildren().size() == 1) {
					jenisLayananKepadaMahasiswa.setSelectedIndex(0);
				} else {
					Common.selectComboItem(jenisLayananKepadaMahasiswa, grupPertemuan.getJenisLayananKepadaMahasiswa());
				}

			}
		};

		jenisEventListener.onEvent(null);
		jenis.addEventListener("onChange", jenisEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal *"));
		row.appendChild(tanggal = new MyDatebox(grupPertemuan.getTanggal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu *"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(waktuMulai = new ais.ui.util.MyTimebox());
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuMulai.setValue(
					grupPertemuan.getWaktuMulai() == null || grupPertemuan.getWaktuMulai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(grupPertemuan.getWaktuMulai()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuSelesai.setValue(
					grupPertemuan.getWaktuSelesai() == null || grupPertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(grupPertemuan.getWaktuSelesai()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				grupPertemuan.getFakultas() == null ? tbmuser.ambilFakultas() : grupPertemuan.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Fakultas")
				+ " jika konsultasi ini berlaku untuk semua " + Common.getBahasaConfig("Fakultas") + ")");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				grupPertemuan.getJurusan() == null ? tbmuser.ambilJurusan() : grupPertemuan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Jurusan")
				+ " jika konsultasi ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan") + ")");

		program = Common.initPrograms(program);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		Common.selectComboItem(program, grupPertemuan.getProgram());
		program.setWidth("90%");
		program.setReadonly(true);

		Common.initKeterangan(rows, "(Kosongkan program jika konsultasi ini berlaku untuk semua program)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunAngkatan = new Textbox(grupPertemuan.getTahunAngkatan()));
		tahunAngkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"(Kosongkan tahun angkatan jika konsultasi ini berlaku untuk semua tahun angkatan, jika terdapat banyak tahun angkatan, masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox(grupPertemuan.getKelas()));
		kelas.setWidth("90%");
		Common.initKeterangan(rows,
				"(Kosongkan kelas jika konsultasi ini berlaku untuk semua kelas, jika terdapat banyak kelas, masukkan kelas yang dipisahkan koma, contoh A,B,C");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setAttribute("ruang", grupPertemuan.getRuang());
		ruang.setValue(grupPertemuan.getRuang() == null ? "" : grupPertemuan.getRuang().getNama());
		ruang.setReadonly(true);
		ruang.setWidth("90%");

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, grupPertemuan.getTahunAkademik());

		jenisSemester = new Combobox();
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		jenisSemester.appendChild(comboitem);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, grupPertemuan.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(grupPertemuan.getKeterangan() == null ? "" : grupPertemuan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		grid = new MyGrid();
		grid.setParent(east);
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		final MyCheckboxConfig configAll = new MyCheckboxConfig();
		column.appendChild(configAll);

		column = new MyColumnConfig();
		column.setParent(columns);

		Hbox hb = new Hbox();
		hb.setParent(column);
		final MyTextbox cari = new MyTextbox();
		cari.setCols(6);
		hb.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs:")));
		hb.appendChild(cari);
		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		hb.appendChild(toolbarbutton);

		column = new MyColumnConfig("Jurusan");
		column.setParent(columns);

		column = new MyColumnConfig("Program");
		column.setParent(columns);

		column = new MyColumnConfig("Tahun Angkatan");
		column.setParent(columns);

		rowsMahasiswa = new Rows();
		rowsMahasiswa.setParent(grid);

		configAll.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = rowsMahasiswa.getChildren();
				for (Row r : rows) {
					MyCheckboxConfig config = (MyCheckboxConfig) r.getAttribute("config");
					config.setChecked(configAll.isChecked());
				}

			}
		});

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsMahasiswa);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>(selectedMahasiswas);

						for (Mahasiswa m : loadMahasiswa(cari.getValue().trim())) {
							if (!mahasiswas.contains(m)) {
								mahasiswas.add(m);
							}
						}

						if (!mahasiswas.isEmpty()) {

							Session session = HibernateUtil.currentSession();
							for (final Mahasiswa mahasiswa : mahasiswas) {
								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rowsMahasiswa);
								final MyCheckboxConfig config = new MyCheckboxConfig();

								if (grupPertemuan != null && grupPertemuan.getId() != null) {
									PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) (session
											.createCriteria(PertemuanPunyaGrupPertemuan.class)
											.add(Restrictions.eq("grupPertemuan", grupPertemuan))
											.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
											.uniqueResult());
									config.setChecked(pertemuanPunyaGrupPertemuan != null);
									row.setValign("top");
									row.setAttribute("pertemuanPunyaGrupPertemuan", pertemuanPunyaGrupPertemuan);
								}

								if (selectedMahasiswas.contains(mahasiswa)) {
									config.setChecked(true);
								}

								row.appendChild(config);
								row.setValign("top");
								row.setAttribute("config", config);
								row.setValign("top");
								row.setAttribute("mahasiswa", mahasiswa);
								Hbox vb = new Hbox();
								vb.setParent(row);
								vb.appendChild(CommonMedia.tampilkanGambarKecil(mahasiswa));
								Vbox a = RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim());
								a.appendChild(new Label(mahasiswa.getNama()));
								vb.appendChild(a);

								row.appendChild(new Label(mahasiswa.getJurusan().getNama()));
								row.appendChild(new Label(mahasiswa.getProgram()));
								row.appendChild(new Label(mahasiswa.getTahunangkatan() + ""));

								config.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (config.isChecked()) {
											selectedMahasiswas.add(mahasiswa);
										} else {
											selectedMahasiswas.remove(mahasiswa);
										}
									}
								});
							}

						}

					}
				});

			}
		};

		cari.addEventListener("onOK", eventListener);
		toolbarbutton.addEventListener("onClick", eventListener);

		dosen.setEventListener(eventListener);
		fakultas.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		tahunAngkatan.addEventListener("onChange", eventListener);
		kelas.addEventListener("onChange", eventListener);
		jenis.addEventListener("onChange", eventListener);
		tahunAkademik.addEventListener("onChange", eventListener);
		jenisSemester.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

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
		if (dosen.getAttribute("dosen") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen",
					"Kolom Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Konsultasi",
					"Kolom Nama Konsultasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Konsultasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenis.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Konsultasi",
					"Kolom Jenis Konsultasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Konsultasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisLayananKepadaMahasiswa.isVisible() && jenisLayananKepadaMahasiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis layanan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							jenisLayananKepadaMahasiswa.focus();
						}
					});
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tanggal.focus();
						}
					});
			return false;
		}
		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Waktu mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							waktuMulai.focus();
						}
					});
			return false;
		}

		if (waktuSelesai.getValue() == null) {
			MyMessageboxConfig.show("Waktu selesai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							waktuSelesai.focus();
						}
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (grupPertemuan.getId() != null) {
			grupPertemuan = (GrupPertemuan) session.load(GrupPertemuan.class, grupPertemuan.getId());

		}

		grupPertemuan.setDosen((Dosen) dosen.getAttribute("dosen"));
		grupPertemuan.setNama(nama.getValue());
		grupPertemuan.setJenis((String) jenis.getSelectedItem().getValue());
		grupPertemuan.setKeterangan(keterangan.getValue());
		grupPertemuan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		grupPertemuan.setTahunAngkatan(tahunAngkatan.getValue().trim());
		grupPertemuan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		grupPertemuan.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		grupPertemuan.setSemesterPendek(semesterPendek);
		grupPertemuan.setRuang((Ruang) ruang.getAttribute("ruang"));
		grupPertemuan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		grupPertemuan.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());
		grupPertemuan
				.setWaktuMulai(waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		grupPertemuan.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));
		grupPertemuan.setTanggal(tanggal.getValue());
		grupPertemuan.setKelas(kelas.getValue());
		grupPertemuan.setJenisLayananKepadaMahasiswa(
				(JenisLayananKepadaMahasiswa) (jenisLayananKepadaMahasiswa.getSelectedItem() == null ? null
						: jenisLayananKepadaMahasiswa.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, grupPertemuan);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				saveDetail(grupPertemuan);
			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Mahasiswa> loadMahasiswa(String cari) {

		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();

		String jenis = (String) (this.jenis.getSelectedItem() == null ? null : this.jenis.getSelectedItem().getValue());
		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");
		if (dosen == null || jenis == null) {
			return mahasiswas;
		}

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		String tahunAngkatan = this.tahunAngkatan.getValue().trim();
		List<Integer> ta = new ArrayList<Integer>();
		for (String t : tahunAngkatan.split(",")) {
			if (Common.isNumber(t)) {
				try {
					ta.add(Integer.parseInt(t.trim()));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		String kelas = this.kelas.getValue().trim();
		String inKel = "";
		for (String t : kelas.split(",")) {
			if (!t.trim().isEmpty()) {
				inKel += inKel.trim().isEmpty() ? "('" + t + "'" : ",'" + t + "'";
			}
		}
		String sqlKelas = "";
		if (!inKel.trim().isEmpty()) {
			sqlKelas = "mahasiswa in (select aa.id from mahasiswa aa where aa.kelas in " + inKel + "))";
		}

		String tahunAkademik = (String) this.tahunAkademik.getSelectedItem().getValue();
		String jenisSemester = (String) (this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		if (jenis.equals(GrupPertemuan.BIMBINGAN)) {

			Criterion c = Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

			c = Restrictions.or(c, Restrictions.eq("dosen3", dosen));
			c = Restrictions.or(c, Restrictions.eq("dosen4", dosen));
			c = Restrictions.or(c, Restrictions.eq("dosen5", dosen));

			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(MahasiswaRequestTugasAkhir.class)
							.add(sqlKelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction(sqlKelas))
							.add(Restrictions.ne("status", MahasiswaRequestTugasAkhir.GAGAL_STATUS))
							.add(Restrictions.eq("tahunAkademik", tahunAkademik))
							.add(jenisSemester == null ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction("this_.semester % 2 = "
											+ (jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 0)))
							.setProjection(Projections.groupProperty("mahasiswa")).add(c)
							.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

							.add(cari.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("mahasiswa.nim", cari.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("mahasiswa.nama", cari.trim(), MatchMode.ANYWHERE)))

							.add(fakultas == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.fakultas", fakultas))
							.add(jurusan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.jurusan", jurusan))
							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))
							.add(ta.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.in("mahasiswa.tahunangkatan", ta))
							.setProjection(Projections.property("mahasiswa.id")).setMaxResults(Common.MAX_RESULT_500),
					Mahasiswa.class, false);
		} else if (jenis.equals(GrupPertemuan.SIDANG)) {

			Criterion c = Restrictions.or(Restrictions.eq("pembimbing", dosen), Restrictions.eq("ketuaSidang", dosen));

			c = Restrictions.or(c, Restrictions.eq("penguji1", dosen));
			c = Restrictions.or(c, Restrictions.eq("penguji2", dosen));
			c = Restrictions.or(c, Restrictions.eq("penguji3", dosen));
			c = Restrictions.or(c, Restrictions.eq("penguji4", dosen));

			c = Restrictions.or(c, Restrictions.eq("pembimbing3", dosen));

			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(Skripsi.class)
							.add(sqlKelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction(sqlKelas))
							.add(Restrictions.eq("tahunAkademik", tahunAkademik))
							.add(jenisSemester == null ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction("this_.semester % 2 = "
											+ (jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 0)))
							.setProjection(Projections.groupProperty("mahasiswa")).add(c)
							.createAlias("mahasiswa", "mahasiswa")
							.add(cari.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("mahasiswa.nim", cari.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("mahasiswa.nama", cari.trim(), MatchMode.ANYWHERE)))
							.createAlias("mahasiswa.jurusan", "jurusan")
							.add(fakultas == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.fakultas", fakultas))
							.add(jurusan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.jurusan", jurusan))
							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))
							.add(ta.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.in("mahasiswa.tahunangkatan", ta))
							.setProjection(Projections.property("mahasiswa.id")).setMaxResults(Common.MAX_RESULT_500),
					Mahasiswa.class, false);
		} else if (jenis.equals(GrupPertemuan.KRS_MAHASISWA)) {

			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(Mahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(sqlKelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction(sqlKelas))
							.add(Restrictions.eq("dosen", dosen.getId()))
							.add(cari.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("nim", cari.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", cari.trim(), MatchMode.ANYWHERE)))
							.createAlias("jurusan", "jurusan")
							.add(fakultas == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.fakultas", fakultas))
							.add(jurusan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan", jurusan))
							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))
							.add(ta.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.in("tahunangkatan", ta))
							.setMaxResults(Common.MAX_RESULT_500),
					Mahasiswa.class);
		} else {

			sqlKelas = "";
			if (!inKel.trim().isEmpty()) {
				sqlKelas = "this_.id in (select aa.id from mahasiswa aa where aa.kelas in " + inKel + "))";
			}

			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(Mahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(cari.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("nim", cari.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", cari.trim(), MatchMode.ANYWHERE)))
							.add(sqlKelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction(sqlKelas))
							.createAlias("jurusan", "jurusan")
							.add(fakultas == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.fakultas", fakultas))
							.add(jurusan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan", jurusan))
							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))
							.add(ta.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.in("tahunangkatan", ta))
							.setMaxResults(Common.MAX_RESULT_500),
					Mahasiswa.class);
		}
		return mahasiswas;
	}

	@SuppressWarnings("unchecked")
	public void saveDetail(GrupPertemuan grupPertemuan) {

		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		List<Row> rows = rowsMahasiswa.getChildren();
		for (Row r : rows) {
			MyCheckboxConfig config = (MyCheckboxConfig) r.getAttribute("config");
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) r
					.getAttribute("pertemuanPunyaGrupPertemuan");
			if (config.isChecked()) {
				mahasiswas.add((Mahasiswa) r.getAttribute("mahasiswa"));
			} else if (pertemuanPunyaGrupPertemuan != null) {
				Common.refreshDelete(pertemuanPunyaGrupPertemuan);
			}
		}

		Session session = HibernateUtil.currentSession();

		for (Mahasiswa mahasiswa : mahasiswas) {

			Pertemuan pertemuan = (Pertemuan) session.createCriteria(PertemuanPunyaGrupPertemuan.class)
					.setProjection(Projections.property("pertemuan")).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("grupPertemuan", grupPertemuan)).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();
			KrsMahasiswa krsMahasiswa = null;
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = null;
			Skripsi skripsi = null;
			if (grupPertemuan.getJenis().equals(GrupPertemuan.SIDANG)) {
				skripsi = (Skripsi) session.createCriteria(Skripsi.class)
						.add(Restrictions.eq("tahunAkademik", grupPertemuan.getTahunAkademik()))
						.add(Restrictions.sqlRestriction("this_.semester % 2 = "
								+ (grupPertemuan.getJenisSemester().equals(Perkuliahan.GANJIL) ? 1 : 0)))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
						: session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.createAlias("skripsi", "skripsi").add(Restrictions.eq("skripsi.mahasiswa", mahasiswa))
								.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal())).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult());
			}

			else if (grupPertemuan.getJenis().equals(GrupPertemuan.BIMBINGAN)) {
				mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) session
						.createCriteria(MahasiswaRequestTugasAkhir.class)
						.add(Restrictions.eq("tahunAkademik", grupPertemuan.getTahunAkademik()))
						.add(Restrictions.sqlRestriction("this_.semester % 2 = "
								+ (grupPertemuan.getJenisSemester().equals(Perkuliahan.GANJIL) ? 1 : 0)))
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.ne("status", MahasiswaRequestTugasAkhir.GAGAL_STATUS))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
						: session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.createAlias("mahasiswaRequestTugasAkhir", "mahasiswaRequestTugasAkhir")
								.add(Restrictions.eq("mahasiswaRequestTugasAkhir.mahasiswa", mahasiswa))
								.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal())).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult());
			}

			else if (grupPertemuan.getJenis().equals(GrupPertemuan.KRS_MAHASISWA)) {

				Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), grupPertemuan.getTahunAkademik(),
						grupPertemuan.getJenisSemester(), mahasiswa.getPindahKeKampusIniMasukSemester(),
						mahasiswa.getSemesterMulai());
				krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);

				pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
						: session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("krsMahasiswa", krsMahasiswa))
								.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal())).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult());

			}

			if (pertemuan == null) {
				pertemuan = new Pertemuan();
				pertemuan.setMulai(grupPertemuan.getTanggal());
				pertemuan.setSelesai(grupPertemuan.getTanggal());
				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);

				try {
					Date d = Common.timeFormat2.get().parse(grupPertemuan.getWaktuMulai());
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(d);

					Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
					calendar1.setTime(grupPertemuan.getTanggal());
					calendar1.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
					calendar1.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
					pertemuan.setMulai(calendar1.getTime());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				try {
					Date d = Common.timeFormat2.get().parse(grupPertemuan.getWaktuSelesai());
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(d);

					Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
					calendar1.setTime(grupPertemuan.getTanggal());
					calendar1.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
					calendar1.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
					pertemuan.setSelesai(calendar1.getTime());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
			pertemuan.setSkripsi(skripsi);
			pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
			pertemuan.setKrsMahasiswa(krsMahasiswa);
			pertemuan.setTanggal(grupPertemuan.getTanggal());
			pertemuan.setRuang(grupPertemuan.getRuang());
			pertemuan.setWaktuMulai(grupPertemuan.getWaktuMulai());
			pertemuan.setWaktuSelesai(grupPertemuan.getWaktuSelesai());
			pertemuan.setFakultasId(mahasiswa.getJurusan().getFakultas().getId());
			pertemuan.setJurusanId(mahasiswa.getJurusan().getId());
			pertemuan.setProgram(mahasiswa.getProgram());
			session.saveOrUpdate(pertemuan);

			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = ((PertemuanPunyaGrupPertemuan) session
					.createCriteria(PertemuanPunyaGrupPertemuan.class).add(Restrictions.eq("pertemuan", pertemuan))
					.add(Restrictions.eq("grupPertemuan", grupPertemuan)).uniqueResult());
			if (pertemuanPunyaGrupPertemuan == null) {
				pertemuanPunyaGrupPertemuan = new PertemuanPunyaGrupPertemuan();
			}

			pertemuanPunyaGrupPertemuan.setMahasiswa(mahasiswa);
			pertemuanPunyaGrupPertemuan.setPertemuan(pertemuan);
			pertemuanPunyaGrupPertemuan.setGrupPertemuan(grupPertemuan);
			pertemuan.setPertemuanPunyaGrupPertemuan(pertemuanPunyaGrupPertemuan);
			session.saveOrUpdate(pertemuanPunyaGrupPertemuan);
			session.saveOrUpdate(pertemuan);
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupPertemuan.class);

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("dosen") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosen.getAttribute("dosen"))))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()))

				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchtahun.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("tahunAngkatan", searchtahun.getValue(), MatchMode.ANYWHERE))

				.add(searchkelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kelas", searchkelas.getValue(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupPertemuan> grupPertemuan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupPertemuan);
		grid.setRowRenderer(new GrupPertemuanRenderer());
		grid.setModelCheckMobile(strset);
//		grid.setOddRowSclass("non-odd");
	}

	public static Hbox tampilkanInfoMahasiswa(PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan)
			throws Exception {
		AuditListener.prosesUntukElearning(pertemuanPunyaGrupPertemuan, "", pertemuanPunyaGrupPertemuan.getId());
		Hbox hbox = new Hbox();
		Mahasiswa mahasiswa = pertemuanPunyaGrupPertemuan.getMahasiswa();
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vbox = new Vbox();

		vbox.setParent(hbox);

		new Label(pertemuanPunyaGrupPertemuan.getMahasiswa().getNim()).setParent(vbox);
		new Label(pertemuanPunyaGrupPertemuan.getMahasiswa().getNama()).setParent(vbox);

		new MyLabelKecil("Keterangan : " + pertemuanPunyaGrupPertemuan.getKeterangan()).setParent(vbox);

		return hbox;
	}

}
