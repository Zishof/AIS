package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.epsbed.EpsbedFrekuensiKurikulum;
import ais.database.model.epsbed.EpsbedPelaksanaanKurikulum;
import ais.database.model.epsbed.EpsbedStatus;
import ais.database.model.epsbed.EpsbedStatusAkreditasi;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenjangProgramStudiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2201964045553345368L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Combobox searchjurusan;
	private Combobox searchfakultas;

	private Combobox jurusan;
	private Combobox jenjang;
	private MyDatebox tanggalBerdiri;
	private Textbox email;
	private Textbox sksLulus;
	private Textbox status;
	private Textbox dimulaiDariSemester;
	// private Textbox nmKaPS;
	// private Textbox nidnKaPS;
	private Textbox telpKaPS;
	private Textbox telpPS;
	private Textbox faxPS;
	private Textbox namaOperator;
	private Textbox hpOperator;
	private Textbox frekuensiKurikulum;
	private Textbox pelaksanaanKurikulum;
	private Textbox noSKDikti;
	private MyDatebox tglMulaiSKDikti;
	private MyDatebox tglAkhirSKDikti;
	private Textbox noSKAkreditasi;
	private MyDatebox tglMulaiSKAkreditasi;
	// private MyDatebox tglAkhirSKAkreditasi;
	private Textbox statusAkreditasi;

	private Combobox epsbedStatus;
	private Textbox tahunHapus;
	private Combobox epsbedStatusAkreditasi;
	private Combobox epsbedFrekuensiKurikulum;
	private Combobox epsbedPelaksanaanKurikulum;

	private JenjangProgramStudi jenjangProgramStudi;
	// private KapasitasMahasiswaBaru kapasitasProdi;
	// private Jurusan jurusan;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	private Intbox toefl;
	private Intbox toafl;
	private Combobox fakultas;
	private Intbox sksPerSemester;
	private MyDatebox tglMulaiOperasional;
	private Textbox homepagePS;
	private MyTextbox pejabatSkBerdiri;
	private Intbox sksWajibLulus;
	private Intbox sksPilihanLulus;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
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

		Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class JenjangProgramStudiRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) arg1;
			/*
			 * new Label(jenjangProgramStudi.getJurusan().getFakultas().getNama())
			 * .setParent(arg0);
			 */
			new Label(jenjangProgramStudi.getJurusan().getNama()).setParent(arg0);
			new Label(jenjangProgramStudi.getJenjang().getNama()).setParent(arg0);
			new Label(jenjangProgramStudi.getNmKaPS()).setParent(arg0);
			new Label(jenjangProgramStudi.getTelpPS()).setParent(arg0);
			new Label(jenjangProgramStudi.getNoSKDikti()).setParent(arg0);
			new Label(jenjangProgramStudi.getNoSKAkreditasi()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					// TODO Auto-generated method stub
					init(jenjangProgramStudi, true);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					// TODO Auto-generated method stub
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(jenjangProgramStudi);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);

		}
	}

	public void onAdd(Event event) throws Exception {
		init(new JenjangProgramStudi(), true);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public Borderlayout init(JenjangProgramStudi jenjangProgramStudi, Boolean tampilkanControl) {

		this.jenjangProgramStudi = jenjangProgramStudi;

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(epsbedStatus = new Combobox(), "nama", EpsbedStatus.class);
		Common.insertCombo(epsbedStatusAkreditasi = new Combobox(), "nama", EpsbedStatusAkreditasi.class);
		Common.insertCombo(epsbedFrekuensiKurikulum = new Combobox(), "nama", EpsbedFrekuensiKurikulum.class);
		Common.insertCombo(epsbedPelaksanaanKurikulum = new Combobox(), "nama", EpsbedPelaksanaanKurikulum.class);

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");
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

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		Common.selectComboItem(fakultas,
				jenjangProgramStudi.getJurusan() == null ? null : jenjangProgramStudi.getJurusan().getFakultas());
		fakultas.setDisabled(!tampilkanControl);
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Homepage PS"));
		row.appendChild(homepagePS = new Textbox(jenjangProgramStudi.getHomepagePS()));
		homepagePS.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, jenjangProgramStudi.getJurusan());
		jurusan.setDisabled(!tampilkanControl);
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Telp Operator"));
		row.appendChild(hpOperator = new Textbox(
				jenjangProgramStudi.getHpOperator() == null ? "" : jenjangProgramStudi.getHpOperator()));
		hpOperator.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, jenjangProgramStudi.getJenjang());
		jenjang.setDisabled(!tampilkanControl);
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Operator"));
		row.appendChild(namaOperator = new Textbox(
				jenjangProgramStudi.getNamaOperator() == null ? "" : jenjangProgramStudi.getNamaOperator()));
		namaOperator.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Berdiri"));
		row.appendChild(tanggalBerdiri = new MyDatebox(jenjangProgramStudi.getTanggalBerdiri() == null ? ais.ui.util.WaktuUtil.getDate()
				: jenjangProgramStudi.getTanggalBerdiri()));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fax PS"));
		row.appendChild(
				faxPS = new Textbox(jenjangProgramStudi.getFaxPS() == null ? "" : jenjangProgramStudi.getFaxPS()));
		faxPS.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(
				email = new Textbox(jenjangProgramStudi.getEmail() == null ? "" : jenjangProgramStudi.getEmail()));
		email.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Frekuensi Pemutakhiran Kurikulum"));
		row.appendChild(frekuensiKurikulum = new Textbox(jenjangProgramStudi.getFrekuensiKurikulum() == null ? ""
				: jenjangProgramStudi.getFrekuensiKurikulum()));
		frekuensiKurikulum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Lulus"));
		row.appendChild(sksLulus = new Textbox(
				jenjangProgramStudi.getSksLulus() == null ? "" : jenjangProgramStudi.getSksLulus()));
		sksLulus.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Operasional"));
		row.appendChild(tglMulaiOperasional = new MyDatebox(jenjangProgramStudi.getTglMulaiOperasional()));
		tglMulaiOperasional.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rata-rata SKS Per Semester"));
		row.appendChild(sksPerSemester = new Intbox(jenjangProgramStudi.getSksPerSemester()));
		sksPerSemester.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Pelaksanaan Pemutakhiran Kurikulum"));
		row.appendChild(pelaksanaanKurikulum = new Textbox(jenjangProgramStudi.getPelaksanaanKurikulum() == null ? ""
				: jenjangProgramStudi.getPelaksanaanKurikulum()));
		pelaksanaanKurikulum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Mk Wajib Kelulusan"));
		row.appendChild(sksWajibLulus = new Intbox(jenjangProgramStudi.getSksWajibLulus()));
		sksWajibLulus.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Mk Pilihan Kelulusan"));
		row.appendChild(sksPilihanLulus = new Intbox(jenjangProgramStudi.getSksPilihanLulus()));
		sksPilihanLulus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Standard Kelulusan TOEFL"));
		row.appendChild(toefl = new Intbox(
				jenjangProgramStudi.getStandardToefl() == null ? 0 : jenjangProgramStudi.getStandardToefl()));
		toefl.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Standard Kelulusan TOAFL"));
		row.appendChild(toafl = new Intbox(
				jenjangProgramStudi.getStandardToafl() == null ? 0 : jenjangProgramStudi.getStandardToafl()));
		toafl.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(
				status = new Textbox(jenjangProgramStudi.getStatus() == null ? "" : jenjangProgramStudi.getStatus()));
		status.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Dimulai dari semester"));
		row.appendChild(dimulaiDariSemester = new Textbox(jenjangProgramStudi.getDimulaiDariSemester() == null ? ""
				: jenjangProgramStudi.getDimulaiDariSemester()));
		dimulaiDariSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No SK Operasional"));
		row.appendChild(noSKDikti = new Textbox(
				jenjangProgramStudi.getNoSKDikti() == null ? "" : jenjangProgramStudi.getNoSKDikti()));
		noSKDikti.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Operasinal"));
		row.appendChild(tglMulaiSKDikti = new MyDatebox(jenjangProgramStudi.getTglMulaiSKDikti() == null ? ais.ui.util.WaktuUtil.getDate()
				: jenjangProgramStudi.getTglMulaiSKDikti()));

		/*
		 * row = new MyFormRow();		 * row.setParent(rows); row.appendChild(new ais.ui.util.MyLabelConfig(
		 * "Nama Ketua PS")); row.appendChild(nmKaPS = new Textbox(
		 * jenjangProgramStudi.getNmKaPS() == null ? "" :
		 * jenjangProgramStudi.getNmKaPS())); nmKaPS.setWidth("90%");
		 */
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir SK Dikti"));
		row.appendChild(tglAkhirSKDikti = new MyDatebox(jenjangProgramStudi.getTglAkhirSKDikti() == null ? ais.ui.util.WaktuUtil.getDate()
				: jenjangProgramStudi.getTglAkhirSKDikti()));

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("NIP Ketua PS"));
		// row
		// .appendChild(nidnKaPS = new Textbox(jenjangProgramStudi
		// .getNidnKaPS() == null ? "" : jenjangProgramStudi
		// .getNidnKaPS()));
		// nidnKaPS.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("No SK Berdiri"));
		row.appendChild(noSKAkreditasi = new Textbox(
				jenjangProgramStudi.getNoSKAkreditasi() == null ? "" : jenjangProgramStudi.getNoSKAkreditasi()));
		noSKAkreditasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp Ketua PS"));
		row.appendChild(telpKaPS = new Textbox(
				jenjangProgramStudi.getTelpKaPS() == null ? "" : jenjangProgramStudi.getTelpKaPS()));
		telpKaPS.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK Berdiri"));
		row.appendChild(
				tglMulaiSKAkreditasi = new MyDatebox(jenjangProgramStudi.getTglMulaiSKAkreditasi() == null ? ais.ui.util.WaktuUtil.getDate()
						: jenjangProgramStudi.getTglMulaiSKAkreditasi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp PS"));
		row.appendChild(
				telpPS = new Textbox(jenjangProgramStudi.getTelpPS() == null ? "" : jenjangProgramStudi.getTelpPS()));
		telpPS.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Pejabat SK Berdiri"));
		row.appendChild(pejabatSkBerdiri = new MyTextbox(jenjangProgramStudi.getPejabatSkBerdiri()));
		pejabatSkBerdiri.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Akreditasi"));
		row.appendChild(statusAkreditasi = new Textbox(
				jenjangProgramStudi.getStatusAkreditasi() == null ? "" : jenjangProgramStudi.getStatusAkreditasi()));
		statusAkreditasi.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Epsbed"));
		row.appendChild(epsbedStatus);
		Common.selectComboItem(epsbedStatus,
				jenjangProgramStudi.getEpsbedStatus() == null ? null : jenjangProgramStudi.getEpsbedStatus());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Hapus (EPSBED)"));
		row.appendChild(tahunHapus = new Textbox(
				jenjangProgramStudi.getEpsbedTahunHapus() == null ? "" : jenjangProgramStudi.getEpsbedTahunHapus()));
		tahunHapus.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Akreditasi Epsbed"));
		row.appendChild(epsbedStatusAkreditasi);
		Common.selectComboItem(epsbedStatusAkreditasi, jenjangProgramStudi.getEpsbedStatusAkreditasi() == null ? null
				: jenjangProgramStudi.getEpsbedStatusAkreditasi());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Frekuensi Kurikulum Epsbed"));
		row.appendChild(epsbedFrekuensiKurikulum);
		Common.selectComboItem(epsbedFrekuensiKurikulum,
				jenjangProgramStudi.getEpsbedFrekuensiKurikulum() == null ? null
						: jenjangProgramStudi.getEpsbedFrekuensiKurikulum());

		// row = new MyFormRow();
		//		// row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pelaksanaan Kurikulum Epsbed"));
		row.appendChild(epsbedPelaksanaanKurikulum);
		Common.selectComboItem(epsbedPelaksanaanKurikulum,
				jenjangProgramStudi.getEpsbedPelaksanaanKurikulum() == null ? null
						: jenjangProgramStudi.getEpsbedPelaksanaanKurikulum());

		if (tampilkanControl) {
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
					if (onSave(event, null)) {
						onSearchDefault(null);
						addWindow.setVisible(false);
					}
				}
			});
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);
		}

		return borderlayout;
	}

	@SuppressWarnings("unused")
	private boolean checkJenjangProgramStudi(JenjangProgramStudi jenjangProgramStudi) {
		Session session = HibernateUtil.currentSession();
		Integer count = 0;
		if (jenjangProgramStudi.getId() == null) {
			count = ((Number) session.createCriteria(JenjangProgramStudi.class)
					.add(Restrictions.and(Restrictions.eq("jurusan", jenjangProgramStudi.getJurusan()),
							Restrictions.eq("jenjang", jenjangProgramStudi.getJenjang())))
					.setProjection(Projections.count("jurusan")).uniqueResult()).intValue();
		} else {
			count = ((Number) session.createCriteria(JenjangProgramStudi.class)
					.add(Restrictions.and(Restrictions.eq("jurusan", jenjangProgramStudi.getJurusan()),
							Restrictions.eq("jenjang", jenjangProgramStudi.getJenjang())))
					.setProjection(Projections.count("jurusan")).uniqueResult()).intValue();
		}

		return !count.equals(0);
	}

	public boolean onSave(Event event, Jurusan pilihanJurusan) throws Exception {

		if (jenjangProgramStudi == null) {
			return false;
		}

		if (pilihanJurusan == null) {
			if (jurusan != null && jurusan.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu " + Common.getBahasaConfig("Jurusan"), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (jenjang != null && jenjang.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu Jenjang", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Session mySession1 = HibernateUtil.currentNativeSession();

		if (jenjangProgramStudi == null && jurusan != null) {
			jenjangProgramStudi = new JenjangProgramStudi();
			jenjangProgramStudi.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		}

		if (jenjangProgramStudi.getId() != null) {
			System.out.println("Load");
			jenjangProgramStudi = (JenjangProgramStudi) mySession1.load(JenjangProgramStudi.class,
					jenjangProgramStudi.getId());
		}

		if (pilihanJurusan == null) {
			jenjangProgramStudi.setJurusan(
					(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
							: jurusan.getSelectedItem().getValue()));
			jenjangProgramStudi.setJenjang(
					(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		} else {
			jenjangProgramStudi.setJurusan(pilihanJurusan);
			jenjangProgramStudi.setJenjang(pilihanJurusan.getJenjang());
		}
		jenjangProgramStudi.setSksPilihanLulus(sksPilihanLulus.getValue());
		jenjangProgramStudi.setSksWajibLulus(sksWajibLulus.getValue());
		jenjangProgramStudi.setPejabatSkBerdiri(pejabatSkBerdiri.getValue());
		jenjangProgramStudi.setHomepagePS(homepagePS.getValue());
		jenjangProgramStudi.setTglMulaiOperasional(tglMulaiOperasional.getValue());
		jenjangProgramStudi.setSksPerSemester(sksPerSemester.getValue());
		jenjangProgramStudi.setTanggalBerdiri(tanggalBerdiri.getValue());
		jenjangProgramStudi.setEmail(email.getValue());
		jenjangProgramStudi.setSksLulus(sksLulus.getValue());
		jenjangProgramStudi.setStandardToefl(toefl.getValue().intValue());
		jenjangProgramStudi.setStandardToafl(toafl.getValue().intValue());
		jenjangProgramStudi.setStatus(status.getValue());
		jenjangProgramStudi.setDimulaiDariSemester(dimulaiDariSemester.getValue());
		// jenjangProgramStudi.setNmKaPS(nmKaPS.getValue());
		// jenjangProgramStudi.setNidnKaPS(nidnKaPS.getValue());
		jenjangProgramStudi.setTelpKaPS(telpKaPS.getValue());
		jenjangProgramStudi.setTelpPS(telpPS.getValue());
		jenjangProgramStudi.setFaxPS(faxPS.getValue());
		jenjangProgramStudi.setNamaOperator(namaOperator.getValue());
		jenjangProgramStudi.setHpOperator(hpOperator.getValue());
		jenjangProgramStudi.setFrekuensiKurikulum(frekuensiKurikulum.getValue());
		jenjangProgramStudi.setPelaksanaanKurikulum(pelaksanaanKurikulum.getValue());
		jenjangProgramStudi.setNoSKDikti(noSKDikti.getValue());
		jenjangProgramStudi.setTglMulaiSKDikti(tglMulaiSKDikti.getValue());
		jenjangProgramStudi.setTglAkhirSKDikti(tglAkhirSKDikti.getValue());
		jenjangProgramStudi.setNoSKAkreditasi(noSKAkreditasi.getValue());
		jenjangProgramStudi.setTglMulaiSKAkreditasi(tglMulaiSKAkreditasi.getValue());
		// jenjangProgramStudi.setTglAkhirSKAkreditasi(tglAkhirSKAkreditasi.getValue());
		jenjangProgramStudi.setStatusAkreditasi(statusAkreditasi.getValue());
		jenjangProgramStudi.setEpsbedTahunHapus(tahunHapus.getValue());

		jenjangProgramStudi.setEpsbedStatus((EpsbedStatus) (epsbedStatus.getSelectedItem() == null ? null
				: epsbedStatus.getSelectedItem().getValue()));
		jenjangProgramStudi.setEpsbedStatusAkreditasi(
				(EpsbedStatusAkreditasi) (epsbedStatusAkreditasi.getSelectedItem() == null ? null
						: epsbedStatusAkreditasi.getSelectedItem().getValue()));

		jenjangProgramStudi.setEpsbedFrekuensiKurikulum(
				(EpsbedFrekuensiKurikulum) (epsbedFrekuensiKurikulum.getSelectedItem() == null ? null
						: epsbedFrekuensiKurikulum.getSelectedItem().getValue()));
		jenjangProgramStudi.setEpsbedPelaksanaanKurikulum(
				(EpsbedPelaksanaanKurikulum) (epsbedPelaksanaanKurikulum.getSelectedItem() == null ? null
						: epsbedPelaksanaanKurikulum.getSelectedItem().getValue()));

		// jenjangProgramStudiDao.beginTransaction();

		// if (jenjangProgramStudi.getId() != null) {
		// jenjangProgramStudiDao.update(jenjangProgramStudi);
		// MyMessageboxConfig.show("Jenjang program studi berhasil di-update",
		// "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// } else {
		// if (checkJenjangProgramStudi(jenjangProgramStudi)) {
		// MyMessageboxConfig.show("Jenjang Jurusan '" + jurusan.getValue()
		// + "' dan jenjang '" + jenjang.getValue()
		// + "' sudah dimasukkan ke dalam database", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// jenjangProgramStudiDao.save(jenjangProgramStudi);
		// MyMessageboxConfig.show("Jenjang program studi berhasil disimpan",
		// "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// }

		mySession1.getTransaction().begin();
		if (jenjangProgramStudi.getId() != null) {
			mySession1.update(jenjangProgramStudi);
		} else {
			mySession1.save(jenjangProgramStudi);
		}
		mySession1.getTransaction().commit();

		HibernateUtil.closeSession();

		// jenjangProgramStudiDao.commitTransaction();
		return true;

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenjangProgramStudi.class);
		if (order)
			criteria.addOrder(Order.asc("id"));
		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenjangProgramStudi> jenjangProgramStudi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenjangProgramStudi);
		grid.setRowRenderer(new JenjangProgramStudiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
