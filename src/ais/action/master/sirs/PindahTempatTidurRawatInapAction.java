package ais.action.master.sirs;

import java.util.Calendar;
import java.util.Date;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatInapBanbox;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.util.RawatInapCalculationProcessor;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.DataPasienKeluar;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.PindahTempatTidurRawatInap;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PindahTempatTidurRawatInapAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchalamat;

	private boolean edit = false;
	private boolean delete = false;

	private PindahTempatTidurRawatInap pindahTempatTidurRawatInap;
	private Toolbarbutton add;

	private EventListener externalCalled;
	private AmbilDataPasienBanbox pasien;

	private Label nama;
	private Label umur;
	private Label alamat;
	private Label ttl;
	private Label jenisPasien;
	private Label jenisKelamin;

	private Label waktu;
	private Label kelas;
	private Label ruang;
	private Label kamar;
	private Label bed;

	private Combobox kelasPerawatan;
	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;
	private AmbilDataTempatTidurBanbox tempatTidur;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		if (tambahData != null) {
			add = new ais.ui.util.MyToolbarbuttonConfig("Pindah Kelas Baru", "/img/user_male_add.png");
			add.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(new PindahTempatTidurRawatInap());
				}
			});
			init(new PindahTempatTidurRawatInap());
		}

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
	}

	class PindahTempatTidurRawatInapRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PindahTempatTidurRawatInap pindahTempatTidurRawatInap = (PindahTempatTidurRawatInap) arg1;
			Pendaftaran pendaftarandari = pindahTempatTidurRawatInap.getPendaftaranDari();
			Pendaftaran pendaftaranke = pindahTempatTidurRawatInap.getPendaftaranKe();

			Pasien pasien = pendaftarandari.getPasien();

			RevisiHelper.createNewRevisi(PindahTempatTidurRawatInap.class, pindahTempatTidurRawatInap, pasien.getKode())
					.setParent(arg0);

			new Label(pasien.getNama() == null ? "" : pasien.getNama()).setParent(arg0);

			new Label(pasien.getNoTelp() + " / " + pasien.getNoHp()).setParent(arg0);

			new Label(pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama()).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);

			new Label(pindahTempatTidurRawatInap.getWaktu() == null ? ""
					: Common.dateFormat3.get().format(pindahTempatTidurRawatInap.getWaktu())).setParent(arg0);

			String kelas = (pendaftarandari.getKelasPerawatan() == null ? ""
					: pendaftarandari.getKelasPerawatan().getNama()) + " ke "
					+ (pendaftaranke.getKelasPerawatan() == null ? "" : pendaftaranke.getKelasPerawatan().getNama());

			new Label(kelas).setParent(arg0);

			String ruang = (pendaftarandari.getRuangPerawatan() == null ? ""
					: pendaftarandari.getRuangPerawatan().getNama()) + " ke "
					+ (pendaftaranke.getRuangPerawatan() == null ? "" : pendaftaranke.getRuangPerawatan().getNama());

			new Label(ruang).setParent(arg0);

			String kamar = (pendaftarandari.getKamarPerawatan() == null ? ""
					: pendaftarandari.getKamarPerawatan().getNama()) + " ke "
					+ (pendaftaranke.getKamarPerawatan() == null ? "" : pendaftaranke.getKamarPerawatan().getNama());

			new Label(kamar).setParent(arg0);

			String tempatTidur = (pendaftarandari.getTempatTidur() == null ? ""
					: pendaftarandari.getTempatTidur().getNama()) + " ke "
					+ (pendaftaranke.getTempatTidur() == null ? "" : pendaftaranke.getTempatTidur().getNama());

			new Label(tempatTidur).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pindahTempatTidurRawatInap);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(pindahTempatTidurRawatInap);

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final PindahTempatTidurRawatInap pindahTempatTidurRawatInap) throws Exception {
		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data pindah kelas beserta data terkait akan dihapus secara permanen dan tidak dapat dikembalikan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();
								Pendaftaran pendaftaranDari = pindahTempatTidurRawatInap.getPendaftaranDari();
								if (pendaftaranDari != null) {
									pendaftaranDari.setPindahKeKelasPerawatan(null);
									pendaftaranDari.setStatusPendaftaran(Pendaftaran.TERDAFTAR);
									pendaftaranDari.setTanggalKeluar(null);
									Common.refreshUpdate(session, pendaftaranDari);

									TempatTidur tempatTidur = pendaftaranDari.getTempatTidur();
									if (tempatTidur != null) {
										tempatTidur.setTerisi(true);
										Common.refreshUpdate(session, tempatTidur);
									}

									List<DiagnosaPenyakit> diagnosaPenyakits = session
											.createCriteria(DiagnosaPenyakit.class)
											.add(Restrictions.eq("pendaftaran", pendaftaranDari)).list();
									for (DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
										diagnosaPenyakit.setTanggalPulang(null);
										diagnosaPenyakit.setStatusPulang(null);
										Common.refreshUpdate(session, diagnosaPenyakit);
									}
								}

								Pendaftaran pendaftaranKe = pindahTempatTidurRawatInap.getPendaftaranKe();

								Common.refreshDelete(session, pindahTempatTidurRawatInap);

								Common.refreshDelete(session, pendaftaranKe);

								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
										e.getMessage()));
							}

						}

					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new PindahTempatTidurRawatInap());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static Window onExternalAdd(EventListener listener) throws Exception {
		PindahTempatTidurRawatInapAction pindahTempatTidurRawatInapAction = new PindahTempatTidurRawatInapAction();
		pindahTempatTidurRawatInapAction.externalCalled = listener;
		pindahTempatTidurRawatInapAction.addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(pindahTempatTidurRawatInapAction.addWindow);
		pindahTempatTidurRawatInapAction.init(new PindahTempatTidurRawatInap());

		pindahTempatTidurRawatInapAction.addWindow.setWidth("750px");
		pindahTempatTidurRawatInapAction.addWindow.setHeight("95%");
		pindahTempatTidurRawatInapAction.addWindow.setTitle("Tambah Pindah Kelas Baru");
		pindahTempatTidurRawatInapAction.addWindow.setVisible(true);
		pindahTempatTidurRawatInapAction.addWindow.onModal();
		return pindahTempatTidurRawatInapAction.addWindow;
	}

	private void initComponent() {

	}

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			Pasien pasien = (Pasien) PindahTempatTidurRawatInapAction.this.pasien.getAttribute("pasien");
			Pendaftaran pendaftaran = (Pendaftaran) PindahTempatTidurRawatInapAction.this.pendaftaran
					.getAttribute("pendaftaran");

			if (pasien == null && pendaftaran == null) {
				return;
			}

			if (pendaftaran != null) {
				pasien = pendaftaran.getPasien();
			} else {
				pendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.eq("jenis", Pendaftaran.RAWAT_INAP)).add(Restrictions.eq("pasien", pasien))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			}

			if (pendaftaran == null) {
				MyMessageboxConfig.show(
						"Data pendaftaran pasien rawat inap tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien telah memiliki pendaftaran rawat inap yang aktif; (2) periksa kembali data pasien yang dipilih; (3) lakukan pendaftaran rawat inap terlebih dahulu apabila belum ada.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			PindahTempatTidurRawatInapAction.this.pendaftaran.setAttribute("pendaftaran", pendaftaran);
			PindahTempatTidurRawatInapAction.this.pendaftaran.setValue(pendaftaran.getKode());
			PindahTempatTidurRawatInapAction.this.pendaftaran.setDisabled(true);

			PindahTempatTidurRawatInapAction.this.pasien.setAttribute("pasien", pendaftaran.getPasien());
			PindahTempatTidurRawatInapAction.this.pasien.setValue(pendaftaran.getPasien().getKode());
			PindahTempatTidurRawatInapAction.this.pasien.setDisabled(true);

			pasien = pendaftaran.getPasien();
			nama.setValue(pasien == null ? "" : pasien.getNama());

			if (pasien.getTanggalLahir() != null) {
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(pasien.getTanggalLahir());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			} else {
				umur.setValue("");
			}

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? ""
					: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
							+ (pasien.getTanggalLahir() == null ? ""
									: Common.dateFormat2.get().format(pasien.getTanggalLahir())));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			jenisPasien.setValue(
					pasien == null ? "" : pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama());

			waktu.setValue(pendaftaran == null ? ""
					: pendaftaran.getTanggalPendaftaran() == null ? ""
							: Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran()));

			kelas.setValue(pendaftaran == null ? ""
					: pendaftaran.getKelasPerawatan() == null ? "" : pendaftaran.getKelasPerawatan().getNama());

			ruang.setValue(pendaftaran == null ? ""
					: pendaftaran.getRuangPerawatan() == null ? "" : pendaftaran.getRuangPerawatan().getNama());

			kamar.setValue(pendaftaran == null ? ""
					: pendaftaran.getKamarPerawatan() == null ? "" : pendaftaran.getKamarPerawatan().getNama());

			bed.setValue(pendaftaran == null ? ""
					: pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());

			save.setDisabled(false);

		}
	};
	private AmbilDataPendaftaranRawatInapBanbox pendaftaran;
	private Toolbarbutton save;
	private MyDatebox waktuPindah;

	private Panel createRuangPerawatan(final Pendaftaran pendaftaran) throws Exception {

		Panel panel = new Panel();
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Pindah Ke");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class,
				Restrictions.ne("id", ConstantValues.kelasNormalId()));
		Common.selectComboItem(kelasPerawatan, pendaftaran == null ? null : pendaftaran.getKelasPerawatan());
		kelasPerawatan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		Common.selectComboItem(ruangPerawatan, pendaftaran == null ? null : pendaftaran.getRuangPerawatan());
		ruangPerawatan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);
				Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class, Restrictions.and(
						ruangPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ruang", ruangPerawatan.getSelectedItem().getValue()),
						kelasPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelasPerawatan", kelasPerawatan.getSelectedItem().getValue())));
				Common.selectComboItem(kamarPerawatan, pendaftaran == null ? null : pendaftaran.getKamarPerawatan());
			}

		};

		kelasPerawatan.addEventListener("onChange", myEventListener);
		ruangPerawatan.addEventListener("onChange", myEventListener);
		myEventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(tempatTidur = new AmbilDataTempatTidurBanbox());
		tempatTidur.setAttribute("tempatTidur", pendaftaran == null ? null : pendaftaran.getTempatTidur());
		tempatTidur.setValue(pendaftaran == null ? null
				: pendaftaran.getTempatTidur() == null ? ""
						: pendaftaran == null ? null : pendaftaran.getTempatTidur().getNama());
		tempatTidur.setWidth("90%");

		kelasPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (kelasPerawatan.getSelectedItem() == null ? null
						: kelasPerawatan.getSelectedItem().getValue());
				if (mykelasPerawatan != null) {
					tempatTidur.setMyKelasPerawatan(mykelasPerawatan);
				}
			}
		});

		ruangPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (ruangPerawatan.getSelectedItem() == null ? null
						: ruangPerawatan.getSelectedItem().getValue());
				if (myRuang != null) {
					tempatTidur.setMyRuang(myRuang);
				}
			}
		});

		kamarPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (kamarPerawatan.getSelectedItem() == null ? null
						: kamarPerawatan.getSelectedItem().getValue());
				if (myKamar != null) {
					tempatTidur.setMyKamar(myKamar);

					Common.selectComboItem(kelasPerawatan, myKamar.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar.getRuang());
				}
			}
		});

		tempatTidur.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TempatTidur myTempatTidur = (TempatTidur) (tempatTidur.getAttribute("tempatTidur"));
				if (myTempatTidur != null) {
					Kamar myKamar = myTempatTidur.getKamar();

					Common.selectComboItem(kelasPerawatan, myKamar == null ? null : myKamar.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar == null ? null : myKamar.getRuang());

					Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", myTempatTidur.getRuang()),
									Restrictions.eq("kelasPerawatan", myTempatTidur.getKelasPerawatan())));
					Common.selectComboItem(kamarPerawatan, myTempatTidur.getKamar());
				}
			}
		});

		return panel;
	}

	private Panel createPindahDari(final PindahTempatTidurRawatInap pindahTempatTidurRawatInap) throws Exception {

		Panel panel = new Panel();
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Pindah Dari");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("180px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil data Pendaftaran Pasien Ranap")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranRawatInapBanbox(false));
		pendaftaran.setAttribute("pendaftaran", pindahTempatTidurRawatInap.getPendaftaranDari());
		pendaftaran.setValue(pindahTempatTidurRawatInap.getPendaftaranDari() == null ? ""
				: pindahTempatTidurRawatInap.getPendaftaranDari().getKode() + " - "
						+ (pindahTempatTidurRawatInap.getPendaftaranDari().getPasien() == null ? ""
								: pindahTempatTidurRawatInap.getPendaftaranDari().getPasien().getNama()));
		pendaftaran.setWidth("90%");
		pendaftaran.setDisabled(pindahTempatTidurRawatInap.getPendaftaranDari() != null);
		pendaftaran.setEventListener(perubahanPasienListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR Pasien")));

		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setValue(pindahTempatTidurRawatInap.getPendaftaranDari() == null
				|| pindahTempatTidurRawatInap.getPendaftaranDari().getPasien() == null ? ""
						: pindahTempatTidurRawatInap.getPendaftaranDari().getPasien().getKode());
		pasien.setAttribute("pasien", pindahTempatTidurRawatInap.getPendaftaranDari() == null ? null
				: pindahTempatTidurRawatInap.getPendaftaranDari().getPasien());
		pasien.setWidth("90%");
		pasien.setEventListener(perubahanPasienListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Pindah")));
		row.appendChild(waktuPindah = new MyDatebox(pindahTempatTidurRawatInap.getWaktu()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Tempat / Tgl. Lahir"));
		row.appendChild(ttl = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Pendaftaran")));
		row.appendChild(waktu = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas")));
		row.appendChild(kelas = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruang = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamar = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(bed = new Label());

		perubahanPasienListener.onEvent(null);

		return panel;
	}

	private void init(final PindahTempatTidurRawatInap pindahTempatTidurRawatInap) throws Exception {
		this.pindahTempatTidurRawatInap = pindahTempatTidurRawatInap;

		initComponent();

		if (addWindow != null) {
			addWindow.setTitle(pindahTempatTidurRawatInap.getId() == null ? "Tambah Pindah Kelas" : "Ubah Pindah Kelas");
			Common.clear(addWindow);
		} else if (tambahData != null) {
			Common.clear(tambahData);
		}
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("50%");

		center.appendChild(createPindahDari(pindahTempatTidurRawatInap));
		east.appendChild(createRuangPerawatan(pindahTempatTidurRawatInap.getPendaftaranKe()));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		if (addWindow != null) {
			Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// if (externalCalled != null) {
					// addWindow.setAttribute("pindahTempatTidurRawatInap",
					// null);
					// Event myEvent = new Event("my_event", addWindow, null);
					// externalCalled.onEvent(myEvent);
					// }
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);
			Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						onSearchDefault(null);
						Common.initPaging(paging, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
							}
						});
						addWindow.setVisible(false);
					}
				}
			});
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);
		} else if (tambahData != null) {
			add.setParent(toolbar);
			save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Pindah Kelas", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						onSearchDefault(null);
						Common.initPaging(paging, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				}
			});
			save.setParent(toolbar);

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Hapus data Pindah Kelas", "/img/delete.gif");
			button.setVisible(delete);
			button.setTooltiptext("Hapus data Pindah Kelas");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (pindahTempatTidurRawatInap != null && pindahTempatTidurRawatInap.getId() != null) {
						onDelete(pindahTempatTidurRawatInap);

					} else {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data pindah kelas ini? Data yang belum tersimpan akan dikosongkan dan formulir akan dikembalikan ke keadaan awal.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											init(new PindahTempatTidurRawatInap());
										}
									}
								});

					}
				}
			});
			button.setParent(toolbar);

			borderlayout.setParent(tambahData);
			tambahData.getLinkedTab().setSelected(true);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (pendaftaran.getAttribute("pendaftaran") == null) {
			MyMessageboxConfig.show(
					"Data pendaftaran asal (pindah dari) harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih data pendaftaran pasien rawat inap pada bagian pindah dari; (2) pastikan pasien yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tempatTidur.getAttribute("tempatTidur") == null) {
			MyMessageboxConfig.show(
					"Data tempat tidur (bed) tujuan harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih tempat tidur (bed) tujuan pada bagian pindah ke; (2) pastikan tempat tidur yang dipilih tersedia; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pindahTempatTidurRawatInap.getId() != null) {
			pindahTempatTidurRawatInap = (PindahTempatTidurRawatInap) session.load(PindahTempatTidurRawatInap.class,
					pindahTempatTidurRawatInap.getId());
		}

		pindahTempatTidurRawatInap.setPendaftaranDari((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		pindahTempatTidurRawatInap.setWaktu(waktuPindah.getValue());

		Pendaftaran pendaftaranKe = pindahTempatTidurRawatInap.getPendaftaranKe();
		if (pendaftaranKe == null) {
			pendaftaranKe = (Pendaftaran) pindahTempatTidurRawatInap.getPendaftaranDari().clone();
			pendaftaranKe.setId(null);
		}

		pendaftaranKe.setTempatTidur((TempatTidur) tempatTidur.getAttribute("tempatTidur"));
		pendaftaranKe.setKelasPerawatan((KelasPerawatan) (kelasPerawatan.getSelectedItem() == null ? null
				: kelasPerawatan.getSelectedItem().getValue()));
		pendaftaranKe.setRuangPerawatan((Ruang) (ruangPerawatan.getSelectedItem() == null ? null
				: ruangPerawatan.getSelectedItem().getValue()));

		pendaftaranKe.setKamarPerawatan((Kamar) (kamarPerawatan.getSelectedItem() == null ? null
				: kamarPerawatan.getSelectedItem().getValue()));

		pindahTempatTidurRawatInap.setPendaftaranKe(pendaftaranKe);

		if (pendaftaranKe.getId() != null) {
			Common.refreshUpdate(session, pendaftaranKe);
		} else {
			pendaftaranKe.setTanggalPendaftaran(new Date());
			session.save(pendaftaranKe);
		}

		if (pindahTempatTidurRawatInap.getId() != null) {
			Common.refreshUpdate(session, pindahTempatTidurRawatInap);
		} else {
			session.save(pindahTempatTidurRawatInap);
		}
		Pendaftaran pendaftaran = pindahTempatTidurRawatInap.getPendaftaranDari();

		session.refresh(pendaftaran);
		pendaftaran.setPindahKeKelasPerawatan(pendaftaranKe.getKelasPerawatan());
		pendaftaran.setStatusPendaftaran(Pendaftaran.PINDAH);
		pendaftaran.setTanggalKeluar(new Date());
		Common.refreshUpdate(session, pendaftaran);

		DataPasienKeluar dataPasienKeluar = (DataPasienKeluar) session.createCriteria(DataPasienKeluar.class)
				.add(Restrictions.eq("pendaftaran", pendaftaran)).addOrder(Order.desc("id")).setMaxResults(1)
				.uniqueResult();
		if (dataPasienKeluar == null) {
			dataPasienKeluar = new DataPasienKeluar();
		}
		dataPasienKeluar.setPendaftaran(pendaftaran);
		dataPasienKeluar.setStatusPulang(ConstantValues.STATUS_PINDAH);
		dataPasienKeluar.setTanggalPulang(waktuPindah.getValue());
		Common.refreshSaveOrUpdate(session, dataPasienKeluar);

		TempatTidur tempatTidur = pendaftaran.getTempatTidur();
		tempatTidur.updateTerisi();
		Common.refreshUpdate(session, tempatTidur);

		tempatTidur = pendaftaranKe.getTempatTidur();
		tempatTidur.updateTerisi();
		Common.refreshUpdate(session, tempatTidur);

		List<DiagnosaPenyakit> diagnosaPenyakits = session.createCriteria(DiagnosaPenyakit.class)
				.add(Restrictions.eq("pendaftaran", pendaftaran)).list();
		for (DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
			diagnosaPenyakit.setTanggalPulang(pindahTempatTidurRawatInap.getWaktu());
			diagnosaPenyakit.setStatusPulang(ConstantValues.STATUS_PINDAH);
			Common.refreshUpdate(session, (diagnosaPenyakit));
		}

		if (externalCalled != null) {
			addWindow.setAttribute("pindahTempatTidurRawatInap", pindahTempatTidurRawatInap);
			Event myEvent = new Event("my_event", addWindow, pindahTempatTidurRawatInap);
			externalCalled.onEvent(myEvent);
		}

		MyMessageboxConfig.show("Data pindah kelas telah berhasil disimpan.", "Informasi", MyMessageboxConfig.OK,
				MyMessageboxConfig.INFORMATION, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (tambahData != null && add != null) {
							Common.freeze(tambahData, true);
							add.setDisabled(false);

							RawatInapCalculationProcessor
									.checkPendaftaran(pindahTempatTidurRawatInap.getPendaftaranDari());

							RawatInapCalculationProcessor
									.checkPendaftaran(pindahTempatTidurRawatInap.getPendaftaranKe());
						}

					}
				});

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PindahTempatTidurRawatInap.class)
				.createAlias("pendaftaranDari", "pendaftaranDari").createAlias("pendaftaranDari.pasien", "pasien")

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchalamat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.alamat", searchalamat.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<PindahTempatTidurRawatInap> pindahTempatTidurRawatInap = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pindahTempatTidurRawatInap);
		grid.setRowRenderer(new PindahTempatTidurRawatInapRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

}
