package ais.action.master;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataKelasLesSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.DetailKelasLesSiswaHelper;
import ais.action.master.sekolah.helper.DetailKelasSiswaHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPSB;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelasSiswaPSBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Decimalbox kapasitasRuangan;
	private Combobox gelombangPendaftaranPsb;
	private Combobox searchgelombangPendaftaranPsb;
	private MyToolbarbuttonConfig add;
	private KelasSiswaPSB kelasSiswaPSB;
	private AmbilDataKelasSiswaBanbox kelasSiswa;
	private AmbilDataKelasLesSiswaBanbox kelasLesSiswa;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertCombo(gelombangPendaftaranPsb = new Combobox(), "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPsb.getChildren().isEmpty()) {
			searchgelombangPendaftaranPsb.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPsb != null) { searchgelombangPendaftaranPsb.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPsb, gel);
				searchgelombangPendaftaranPsb.setDisabled(true);
			}
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class KelasSiswaPSBRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelasSiswaHelper detailKelasSiswaHelper = new DetailKelasSiswaHelper();
		private DetailKelasLesSiswaHelper detailKelasLesSiswaHelper = new DetailKelasLesSiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswaPSB kelasSiswaPSB = (KelasSiswaPSB) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						if (kelasSiswaPSB.getKelasSiswa() != null) {
							MyTabConfig tabSoal = new MyTabConfig("Kelas Siswa");
							tabSoal.setParent(tabs);
						}

						if (kelasSiswaPSB.getKelasLesSiswa() != null) {
							MyTabConfig tabSoalKursus = new MyTabConfig("Kelas Kursus Siswa");
							tabSoalKursus.setParent(tabs);
						}

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						if (kelasSiswaPSB.getKelasSiswa() != null) {
							int tinggi = ((Number) HibernateUtil.currentSession()
									.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.rowCount())
									.add(Restrictions.eq("kelasSiswa", kelasSiswaPSB.getKelasSiswa())).uniqueResult())
									.intValue();

							Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
							tabpanelUtama.setStyle("min-height: 200px;");
							tabpanelUtama.setHeight((100 + (77 * tinggi)) + "px");
							tabpanelUtama.setParent(tabpanels);

							detailKelasSiswaHelper.displayDetailPA(kelasSiswaPSB.getKelasSiswa(), detail, addWindow);
						}

						if (kelasSiswaPSB.getKelasLesSiswa() != null) {
							int tinggi = ((Number) HibernateUtil.currentSession()
									.createCriteria(KelasLesSiswaPunyaSiswa.class).setProjection(Projections.rowCount())
									.add(Restrictions.eq("kelasLesSiswa", kelasSiswaPSB.getKelasLesSiswa()))
									.uniqueResult()).intValue();

							Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
							tabpanelUtama.setStyle("min-height: 200px;");
							tabpanelUtama.setHeight((100 + (77 * tinggi)) + "px");
							tabpanelUtama.setParent(tabpanels);

							detailKelasLesSiswaHelper.displayDetailPA(kelasSiswaPSB.getKelasLesSiswa(), detail,
									addWindow);
						}
					}

				}

			});

			Integer isi = cekRuanganIsi(kelasSiswaPSB);

			if (kelasSiswaPSB.getPenuh().equals(0) && isi.equals(kelasSiswaPSB.getKapasitasRuangan())) {
				kelasSiswaPSB.setPenuh(1);
				Common.refreshUpdate(kelasSiswaPSB);
			}

			RevisiHelper.createNewRevisi(KelasSiswaPSB.class, kelasSiswaPSB, kelasSiswaPSB.getNama()).setParent(arg0);

			new Label(kelasSiswaPSB.getKapasitasRuangan() == null ? ""
					: kelasSiswaPSB.getKapasitasRuangan().toString() + "/" + isi).setParent(arg0);
			new Label(kelasSiswaPSB.getGelombangPendaftaranPsb() == null ? ""
					: kelasSiswaPSB.getGelombangPendaftaranPsb().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Penuh");
			checkbox.setChecked(kelasSiswaPSB.getPenuh().equals(1));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasSiswaPSB.setPenuh(checkbox.isChecked() ? 1 : 0);
					Common.refreshSaveOrUpdate(kelasSiswaPSB);
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelasSiswaPSB);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE));
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
											Common.refreshDelete(kelasSiswaPSB);
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

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelasSiswaPSB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelasSiswaPSB kelasSiswaPSB) {
		this.kelasSiswaPSB = kelasSiswaPSB;
		addWindow.setTitle(kelasSiswaPSB.getId() == null ? "Tambah Ruang PSB" : "Ubah Ruang PSB");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Les Siswa *"));
		row.appendChild(kelasLesSiswa = new AmbilDataKelasLesSiswaBanbox());
		kelasLesSiswa.setAttribute("kelasLesSiswa", kelasSiswaPSB.getKelasLesSiswa());
		kelasLesSiswa
				.setValue(kelasSiswaPSB.getKelasLesSiswa() == null ? "" : kelasSiswaPSB.getKelasLesSiswa().getNama());
		kelasLesSiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Siswa *"));
		row.appendChild(kelasSiswa = new AmbilDataKelasSiswaBanbox());
		kelasSiswa.setAttribute("kelasSiswa", kelasSiswaPSB.getKelasSiswa());
		kelasSiswa.setValue(kelasSiswaPSB.getKelasSiswa() == null ? "" : kelasSiswaPSB.getKelasSiswa().getNama());
		kelasSiswa.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kelasSiswa.getParent().setVisible(kelasLesSiswa.getAttribute("kelasLesSiswa") == null);
				kelasLesSiswa.getParent().setVisible(kelasSiswa.getAttribute("kelasSiswa") == null);
			}
		};

		kelasLesSiswa.setEventListener(eventListener);

		kelasSiswa.setEventListener(eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Ruangan"));
		row.appendChild(kapasitasRuangan = new Decimalbox(new BigDecimal(kelasSiswaPSB.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
		gelombangPendaftaranPsb.setDisabled(false);
		Common.selectComboItem(gelombangPendaftaranPsb,
				kelasSiswaPSB.getGelombangPendaftaranPsb() == null ? null : kelasSiswaPSB.getGelombangPendaftaranPsb());
		row.appendChild(gelombangPendaftaranPsb);
		gelombangPendaftaranPsb.setWidth("90%");
		gelombangPendaftaranPsb.setReadonly(true);

		if (kelasSiswaPSB.getId() != null) {
			if (cekRuanganIsi(kelasSiswaPSB) > 0) {
				gelombangPendaftaranPsb.setDisabled(true);
			} else {
				gelombangPendaftaranPsb.setDisabled(false);
			}
		}

		if (searchgelombangPendaftaranPsb.getSelectedItem() != null
				&& searchgelombangPendaftaranPsb.getSelectedItem().getValue() != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, searchgelombangPendaftaranPsb.getSelectedItem().getValue());
			gelombangPendaftaranPsb.setDisabled(searchgelombangPendaftaranPsb.isDisabled());

		}

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

		if (kelasSiswa.getAttribute("kelasSiswa") == null && kelasLesSiswa.getAttribute("kelasLesSiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas siswa / kelas les siswa",
					"Kolom Kelas siswa / kelas les siswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelas siswa / kelas les siswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (kapasitasRuangan.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kapasitas Ruangan",
					"Kolom Kapasitas Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kapasitas Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelasSiswaPSB.getId() != null) {
			kelasSiswaPSB = (KelasSiswaPSB) session.load(KelasSiswaPSB.class, kelasSiswaPSB.getId());
		}
		kelasSiswaPSB.setKapasitasRuangan(
				kapasitasRuangan.getValue() == null ? null : Integer.parseInt(kapasitasRuangan.getValue().toString()));
		kelasSiswaPSB.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
						: gelombangPendaftaranPsb.getSelectedItem().getValue()));

		kelasSiswaPSB.setKelasLesSiswa((KelasLesSiswa) kelasLesSiswa.getAttribute("kelasLesSiswa"));
		kelasSiswaPSB.setKelasSiswa((KelasSiswa) kelasSiswa.getAttribute("kelasSiswa"));

		Common.refreshSaveOrUpdate(session, kelasSiswaPSB);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasSiswaPSB.class);
		if (order)
			criteria.addOrder(Order.asc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null
						|| searchgelombangPendaftaranPsb.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("gelombangPendaftaranPsb",
										searchgelombangPendaftaranPsb.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<KelasSiswaPSB> kelasSiswaPSB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kelasSiswaPSB);
		grid.setRowRenderer(new KelasSiswaPSBRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Integer cekRuanganIsi(KelasSiswaPSB kelasSiswaPSB) {
		Integer count = 0;
		Session session = HibernateUtil.currentSession();
		session.refresh(kelasSiswaPSB);

		count += kelasSiswaPSB.getKelasSiswa() == null || kelasSiswaPSB.getKelasSiswa().getId() == null ? 0
				: ((Number) session.createCriteria(KelasSiswaPunyaSiswa.class)
						.add(Restrictions.eq("kelasSiswa.id", kelasSiswaPSB.getKelasSiswa().getId()))
						.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.nomorInduk", ""))
						.add(Restrictions.isNotNull("calonSiswa.nomorInduk")).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

		count += kelasSiswaPSB.getKelasLesSiswa() == null || kelasSiswaPSB.getKelasLesSiswa().getId() == null ? 0
				: ((Number) session.createCriteria(KelasLesSiswaPunyaSiswa.class)
						.add(Restrictions.eq("kelasLesSiswa.id", kelasSiswaPSB.getKelasLesSiswa().getId()))
						.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.nomorInduk", ""))
						.add(Restrictions.isNotNull("calonSiswa.nomorInduk")).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

		System.out.println("Jumlah isi ruang : " + count);
		return count;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAbsensi(KelasSiswaPSB ruang) throws Exception {

		this.kelasSiswaPSB = ruang;
		Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak absensi " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		Report.generatePDFReport(Report.PDF, parameters, "Coverspsbi", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakBau(KelasSiswaPSB ruang) throws Exception {

		this.kelasSiswaPSB = ruang;
		Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak Bau " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		Report.generatePDFReport(Report.PDF, parameters, "BeritaAcaraUjianPSB", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAlbum(KelasSiswaPSB ruang) throws Exception {

		this.kelasSiswaPSB = ruang;
		// final Map<String, Long> parameters = new HashMap<String, Long>();
		Map parameters = ais.common.HashMapGenerator.getRand();
		List<Map<String, Object>> maps = getDataAlbumPSBAdmin(ruang);
		parameters.put("kelas", ruang.getKelasSiswa() == null ? -1L : ruang.getKelasSiswa().getId());
		parameters.put("kelas_les", ruang.getKelasLesSiswa() == null ? -1L : ruang.getKelasLesSiswa().getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("tahunakademik", ruang.getGelombangPendaftaranPsb().getTahunAjaran());
		parameters.put("gelombang_pendaftaran",
				ruang.getGelombangPendaftaranPsb() == null ? "" : ruang.getGelombangPendaftaranPsb().getNama());
		parameters.put("ket_ruang", ruang.getNama());
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		System.out.println("Cetak Album PSB gelombangPendaftaranPsb " + ruang.getGelombangPendaftaranPsb().getNama()
				+ " ruang " + ruang.getNama());

		parameters.put("gelombangPendaftaranPsb", ruang.getGelombangPendaftaranPsb().getNama());
		Report.generatePDFReport("pdf", parameters, "AlbumPSBHari", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getDataAlbumPSBAdmin(KelasSiswaPSB ruang) throws Exception {
		this.kelasSiswaPSB = ruang;
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Session session = HibernateUtil.currentSession();
		if (ruang.getKelasSiswa() != null && ruang.getKelasSiswa().getId() != null) {
			List<KelasSiswaPunyaSiswa> listPendaftaranWisuda = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonSiswa.nomorInduk")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("kelasSiswa.id", ruang.getKelasSiswa().getId())).list();

			Iterator<?> itr = listPendaftaranWisuda.iterator();

			try {

				while (itr.hasNext()) {
					KelasSiswaPunyaSiswa beanPendaftaranWisuda = (KelasSiswaPunyaSiswa) itr.next();
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("nama", beanPendaftaranWisuda.getCalonSiswa().getNama().toUpperCase());
					map.put("no_ujian", beanPendaftaranWisuda.getCalonSiswa().getNomorInduk());
					map.put("ttl", beanPendaftaranWisuda.getCalonSiswa().getTempatLahir().toUpperCase() + " / "
							+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonSiswa().getTanggalLahir()));
					map.put("kelamin", beanPendaftaranWisuda.getCalonSiswa().getJenisKelamin());

					map.put("gelombang_pendaftaran",
							beanPendaftaranWisuda.getCalonSiswa().getGelombangPendaftaranPsb().getNama());
					map.put("alamat", beanPendaftaranWisuda.getCalonSiswa().getAlamatSiswa());
					map.put("prodi_1", beanPendaftaranWisuda.getCalonSiswa().getSekolah().getNama());
					map.put("prodi_2", beanPendaftaranWisuda.getCalonSiswa().getYayasan().getNama());

					beanPendaftaranWisuda.getCalonSiswa().putPhoto(map);

					maps.add(map);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (ruang.getKelasLesSiswa() != null && ruang.getKelasLesSiswa().getId() != null) {
			List<KelasLesSiswaPunyaSiswa> listPendaftaranWisuda = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
					.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.nomorInduk", ""))
					.add(Restrictions.isNotNull("calonSiswa.nomorInduk")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("kelasLesSiswa.id", ruang.getKelasLesSiswa().getId())).list();

			Iterator<?> itr = listPendaftaranWisuda.iterator();

			try {

				while (itr.hasNext()) {
					KelasLesSiswaPunyaSiswa beanPendaftaranWisuda = (KelasLesSiswaPunyaSiswa) itr.next();
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("nama", beanPendaftaranWisuda.getCalonSiswa().getNama().toUpperCase());
					map.put("no_ujian", beanPendaftaranWisuda.getCalonSiswa().getNomorInduk());
					map.put("ttl", beanPendaftaranWisuda.getCalonSiswa().getTempatLahir().toUpperCase() + " / "
							+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonSiswa().getTanggalLahir()));
					map.put("kelamin", beanPendaftaranWisuda.getCalonSiswa().getJenisKelamin());

					map.put("gelombang_pendaftaran",
							beanPendaftaranWisuda.getCalonSiswa().getGelombangPendaftaranPsb().getNama());
					map.put("alamat", beanPendaftaranWisuda.getCalonSiswa().getAlamatSiswa());
					map.put("prodi_1", beanPendaftaranWisuda.getCalonSiswa().getSekolah().getNama());
					map.put("prodi_2", beanPendaftaranWisuda.getCalonSiswa().getYayasan().getNama());

					beanPendaftaranWisuda.getCalonSiswa().putPhoto(map);

					maps.add(map);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return maps;
	}

}
