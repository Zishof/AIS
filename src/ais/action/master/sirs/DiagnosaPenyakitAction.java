package ais.action.master.sirs;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.AlatMedisHelper;
import ais.action.master.sirs.detail.PemeriksaanHelper;
import ais.action.master.sirs.detail.ResepHelper;
import ais.action.master.sirs.detail.TindakanHelper;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataIcdBanbox;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatJalanBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Icd;
import ais.database.model.sirs.Instalasi;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pemeriksaan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class DiagnosaPenyakitAction extends GenericAutowireComposer implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tabpanel tambahData;

	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private AmbilDataPasienBanbox searchpasien;
	private MyTextbox searchtelp;
	private MyTextbox searchalamat;
	private MyTextbox searchnama;
	private Combobox searchrajalranap;
	private MyTextbox searchnip;
	private MyTextbox searchpekerjaan;

	private MyTextbox kode;
	private AmbilDataPendaftaranRawatJalanBanbox pendaftaran;
	private AmbilDataPasienBanbox pasien;
	private Combobox apakahMenular;
	private Combobox poly = new Combobox();
	private Combobox subpoly = new Combobox();
	// private AmbilDataJenisPenyakitBanbox jenisPenyakit;

	// private MyTextbox keluhanDiagnosa;

	private AmbilDataIcdBanbox diagnosaAwal1;
	private AmbilDataIcdBanbox diagnosaAwal2;
	private AmbilDataIcdBanbox diagnosaAwal3;
	private AmbilDataIcdBanbox diagnosaAkhir1;
	private AmbilDataIcdBanbox diagnosaAkhir2;
	private AmbilDataIcdBanbox diagnosaAkhir3;
	private MyDatebox tanggal;
	private AmbilDataDokterBanbox dokter;
	// private Combobox statusPulang;
	private Combobox instalasi;
	// private MyDatebox tanggalPulang;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DiagnosaPenyakit diagnosaPenyakit;
	private Toolbarbutton add;

	private Label nama;
	private Label umur;
	private Label alamat;
	private Label ttl;
	private Label jenisPasien;
	private Label jenisKelamin;

	private PemeriksaanHelper keluhan = new PemeriksaanHelper(Pemeriksaan.JENIS_KELUHAN);
	private PemeriksaanHelper riwayat = new PemeriksaanHelper(Pemeriksaan.JENIS_RIWAYAT);
	private PemeriksaanHelper periksa = new PemeriksaanHelper(Pemeriksaan.JENIS_PERIKSA);

	private TindakanHelper tindakanHelper;
	private AlatMedisHelper alatMedisHelper;
	private ResepHelper resepHelper;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

	protected Set<Tindakan> pakets;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		add = new ais.ui.util.MyToolbarbuttonConfig("Buat Rekam Medis Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new DiagnosaPenyakit());
			}
		});

		Comboitem comboitem = new Comboitem(Pendaftaran.RAWAT_JALAN);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.RAWAT_JALAN); }
		searchrajalranap.appendChild(comboitem);
		if (searchrajalranap != null) { searchrajalranap.setSelectedItem(comboitem); }
		if (searchrajalranap != null) { searchrajalranap.setDisabled(true); }

		comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.RAWAT_INAP); }
		searchrajalranap.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		init(new DiagnosaPenyakit());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchpasien.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class DiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) arg1;

			Pasien pasien = diagnosaPenyakit.getPasien();
			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}

			new Label(diagnosaPenyakit.getKode()).setParent(arg0);
			new Label(diagnosaPenyakit.getPoly() == null ? "" : diagnosaPenyakit.getPoly().getNama()).setParent(arg0);
			new Label(diagnosaPenyakit.getDokter() == null ? "" : diagnosaPenyakit.getDokter().getNama())
					.setParent(arg0);
			RevisiHelper
					.createNewRevisi(DiagnosaPenyakit.class, diagnosaPenyakit,
							diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getKode())
					.setParent(arg0);
			new Label(diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getNama())
					.setParent(arg0);
			new Label(diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal())).setParent(arg0);

			String diagnosaAwal = "<font style='font-size: x-small;'><ol>";
			if (diagnosaPenyakit.getDiagnosaAwal1() != null) {
				diagnosaAwal += "<li>" + diagnosaPenyakit.getDiagnosaAwal1().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal1().getNama_english() + "</li>";
			}
			if (diagnosaPenyakit.getDiagnosaAwal2() != null) {
				diagnosaAwal += "<li>" + diagnosaPenyakit.getDiagnosaAwal2().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal2().getNama_english() + "</li>";
			}
			if (diagnosaPenyakit.getDiagnosaAwal3() != null) {
				diagnosaAwal += "<li>" + diagnosaPenyakit.getDiagnosaAwal3().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal3().getNama_english() + "</li>";
			}
			diagnosaAwal += "</ol></font>";
			new Html(diagnosaAwal).setParent(arg0);

			String diagnosaAkhir = "<font style='font-size: x-small;'><ol>";
			if (diagnosaPenyakit.getDiagnosaAkhir1() != null) {
				diagnosaAkhir += "<li>" + diagnosaPenyakit.getDiagnosaAkhir1().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir1().getNama_english() + "</li>";
			}
			if (diagnosaPenyakit.getDiagnosaAkhir2() != null) {
				diagnosaAkhir += "<li>" + diagnosaPenyakit.getDiagnosaAkhir2().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir2().getNama_english() + "</li>";
			}
			if (diagnosaPenyakit.getDiagnosaAkhir3() != null) {
				diagnosaAkhir += "<li>" + diagnosaPenyakit.getDiagnosaAkhir3().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir3().getNama_english() + "</li>";
			}
			diagnosaAkhir += "</ol></font>";
			new Html(diagnosaAkhir).setParent(arg0);

			new Label(diagnosaPenyakit.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Rekam Medis");
			button.setVisible(diagnosaPenyakit.getPendaftaran().getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonSirs.onCetakHasilDiagnosaPasien(diagnosaPenyakit);
				}

			});
			button.setParent(toolbar);

			toolbar.setVisible(pasien.getAktif() != null && pasien.getAktif());
			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (diagnosaPenyakit.getLunas() == null || !diagnosaPenyakit.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					DiagnosaPenyakit myDiagnosaPenyakit = (DiagnosaPenyakit) HibernateUtil.currentSession()
							.createCriteria(DiagnosaPenyakit.class).add(Restrictions.idEq(diagnosaPenyakit.getId()))
							.uniqueResult();
					init(myDiagnosaPenyakit);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (diagnosaPenyakit.getLunas() == null || !diagnosaPenyakit.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(diagnosaPenyakit);
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onProcessDelete(final DiagnosaPenyakit diagnosaPenyakit) {

		Session session = HibernateUtil.currentSession();

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_pasien where item_diagnosa_penyakit in (select id from sirs.item_diagnosa_penyakit where diagnosa_penyakit = "
						+ diagnosaPenyakit.getId() + ");")
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_layanan where kunjungan_dokter in (select id from sirs.kunjungan_dokter where diagnosa_penyakit = "
						+ diagnosaPenyakit.getId() + ");")
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_layanan where tindakan_diagnosa_penyakit in (select id from sirs.tindakan_diagnosa_penyakit where diagnosa_penyakit = "
						+ diagnosaPenyakit.getId() + ");")
				.executeUpdate();

		session.createSQLQuery("delete from sirs.resep where diagnosa_penyakit = " + diagnosaPenyakit.getId())
				.executeUpdate();
		session.createSQLQuery(
				"delete from sirs.tindakan_diagnosa_penyakit where diagnosa_penyakit = " + diagnosaPenyakit.getId())
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.kunjungan_dokter where diagnosa_penyakit = " + diagnosaPenyakit.getId())
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.item_diagnosa_penyakit where diagnosa_penyakit = " + diagnosaPenyakit.getId())
				.executeUpdate();

		Common.refreshDelete(session, diagnosaPenyakit);
	}

	public void onDelete(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {

		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin membatalkan perawatan ini? Perlu diketahui, seluruh data yang terkait dengan perawatan ini akan ikut dibatalkan dan tidak dapat dikembalikan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();
								List<DiagnosaPenyakit> diagnosaPenyakits = session
										.createCriteria(DiagnosaPenyakit.class)
										.add(Restrictions.eq("diagnosaPenyakitInduk", diagnosaPenyakit))
										.addOrder(Order.desc("id")).list();

								for (DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
									onProcessDelete(diagnosaPenyakit);
								}
								onProcessDelete(diagnosaPenyakit);
								onSearchDefault(event);

							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
										e.getMessage()));
							}
						}
					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new DiagnosaPenyakit());
	}

	private EventListener perubahanPasienListener = new EventListener() {

		private void doExecute(Pendaftaran pendaftaran) throws Exception {

			if (pendaftaran.getPoly() != null
					&& pendaftaran.getPoly().getId().equals(ConstantValues.POLI_UGD.getId())) {
				Common.selectComboItem(instalasi, ConstantValues.UGD);
				instalasi.setDisabled(true);
			} else if (pendaftaran.getJenis().equals(Pendaftaran.RAWAT_JALAN)) {
				Common.selectComboItem(instalasi, ConstantValues.RAWAT_JALAN);
				instalasi.setDisabled(true);
			} else if (pendaftaran.getJenis().equals(Pendaftaran.RAWAT_INAP)) {
				Common.selectComboItem(instalasi, ConstantValues.RAWAT_INAP);
				instalasi.setDisabled(true);
			}

			if (pendaftaran.getPoly() != null) {
				Common.selectComboItem(poly, pendaftaran.getPoly());
				subPolyEventListener.onEvent(null);
				Common.selectComboItem(subpoly, pendaftaran.getSubpoly());
			} else {
				poly.setDisabled(false);
			}

			if (pendaftaran.getDokter() != null) {
				dokter.setAttribute("dokter", pendaftaran.getDokter());
				dokter.setValue(pendaftaran.getDokter().getNama());
				// dokter.setDisabled(true);
			} else {
				dokter.setDisabled(false);
			}

			Pasien pasien = pendaftaran.getPasien();
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

			CommonPendaftaranUtil.riwayatPenyakitPasien(southRiwayatPenyakitPasien, pasien);

			save.setDisabled(false);
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			Pasien pasien = (Pasien) DiagnosaPenyakitAction.this.pasien.getAttribute("pasien");
			Pendaftaran pendaftaran = null;
			if (pasien == null) {
				Pendaftaran tempPendaftaran = (Pendaftaran) DiagnosaPenyakitAction.this.pendaftaran
						.getAttribute("pendaftaran");

				if (tempPendaftaran == null || tempPendaftaran.getPasien() == null) {
					return;
				}

				pendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.idEq(tempPendaftaran.getId())).uniqueResult();

				DiagnosaPenyakitAction.this.pasien.setAttribute("pasien", pendaftaran.getPasien());
				DiagnosaPenyakitAction.this.pasien.setValue(pendaftaran.getPasien().getKode());
				DiagnosaPenyakitAction.this.pasien.setDisabled(true);

			} else {
				DiagnosaPenyakitAction.this.pendaftaran.setDisabled(true);
				pendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.eq("jenis", Pendaftaran.RAWAT_JALAN)).add(Restrictions.eq("pasien", pasien))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				if (pendaftaran == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, data pendaftaran untuk pasien ini tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien telah melakukan pendaftaran rawat jalan; (2) periksa kembali data pasien yang dipilih; (3) apabila diperlukan, lakukan pendaftaran terlebih dahulu sebelum melanjutkan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				DiagnosaPenyakitAction.this.pendaftaran.setAttribute("pendaftaran", pendaftaran);
				DiagnosaPenyakitAction.this.pendaftaran.setValue(pendaftaran.getKode());

			}

			pakets = pendaftaran.getPakets();
			if (!pakets.isEmpty()) {
				eastInfoPasien.setVisible(true);
				CommonPendaftaranUtil.displayDetailPaket(eastInfoPasien, pakets);
			} else {
				eastInfoPasien.setVisible(false);
			}

			DiagnosaPenyakit myDiagnosaPenyakit = (DiagnosaPenyakit) HibernateUtil.currentSession()
					.createCriteria(DiagnosaPenyakit.class).add(Restrictions.eq("pendaftaran", pendaftaran))
					.add(Restrictions.isNull("diagnosaPenyakitInduk")).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (myDiagnosaPenyakit != null && diagnosaPenyakit.getId() == null) {
				init(myDiagnosaPenyakit);
			} else {
				doExecute(pendaftaran);
			}

		}
	};

	private Toolbarbutton save;

	private Row subPolyRow;

	private Borderlayout createDiagnosa(DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
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
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		Rows rows = new Rows();
		rows.setParent(grid);

		// Row row = new Row();
		// row.setStyle("border:0px;background: transparent;");
		// row.setParent(rows);
		// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Penyakit")));
		// row.appendChild(jenisPenyakit = new AmbilDataJenisPenyakitBanbox());
		// jenisPenyakit.setAttribute("jenisPenyakit",
		// diagnosaPenyakit.getJenisPenyakit());
		// jenisPenyakit.setValue(diagnosaPenyakit.getJenisPenyakit() == null ?
		// ""
		// : diagnosaPenyakit.getJenisPenyakit().getNama());
		// jenisPenyakit.setWidth("90%");

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Menular/Tidak Menular"));
		row.appendChild(apakahMenular = new Combobox());
		Comboitem comboitem = new Comboitem(DiagnosaPenyakit.TIDAK_MENULAR);
		comboitem.setValue(DiagnosaPenyakit.TIDAK_MENULAR);
		apakahMenular.appendChild(comboitem);
		comboitem = new Comboitem(DiagnosaPenyakit.MENULAR);
		comboitem.setValue(DiagnosaPenyakit.MENULAR);
		apakahMenular.appendChild(comboitem);
		Common.selectComboItem(apakahMenular, diagnosaPenyakit.getApakahMenular());
		apakahMenular.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hasil Diagnosa Awal")));
		row.appendChild(diagnosaAwal1 = new AmbilDataIcdBanbox());
		diagnosaAwal1.setAttribute("icd", diagnosaPenyakit.getDiagnosaAwal1());
		diagnosaAwal1.setValue(diagnosaPenyakit.getDiagnosaAwal1() == null ? ""
				: diagnosaPenyakit.getDiagnosaAwal1().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal1().getNama_english());
		diagnosaAwal1.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hasil Diagnosa Akhir")));
		row.appendChild(diagnosaAkhir1 = new AmbilDataIcdBanbox());
		diagnosaAkhir1.setAttribute("icd", diagnosaPenyakit.getDiagnosaAkhir1());
		diagnosaAkhir1.setValue(diagnosaPenyakit.getDiagnosaAkhir1() == null ? ""
				: diagnosaPenyakit.getDiagnosaAkhir1().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir1().getNama_english());
		diagnosaAkhir1.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komplikasi 1:")));
		row.appendChild(diagnosaAwal2 = new AmbilDataIcdBanbox());
		diagnosaAwal2.setAttribute("icd", diagnosaPenyakit.getDiagnosaAwal2());
		diagnosaAwal2.setValue(diagnosaPenyakit.getDiagnosaAwal2() == null ? ""
				: diagnosaPenyakit.getDiagnosaAwal2().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal2().getNama_english());
		diagnosaAwal2.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komplikasi 2:")));
		row.appendChild(diagnosaAkhir2 = new AmbilDataIcdBanbox());
		diagnosaAkhir2.setAttribute("icd", diagnosaPenyakit.getDiagnosaAkhir2());
		diagnosaAkhir2.setValue(diagnosaPenyakit.getDiagnosaAkhir2() == null ? ""
				: diagnosaPenyakit.getDiagnosaAkhir2().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir2().getNama_english());
		diagnosaAkhir2.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komplikasi 3:")));
		row.appendChild(diagnosaAwal3 = new AmbilDataIcdBanbox());
		diagnosaAwal3.setAttribute("icd", diagnosaPenyakit.getDiagnosaAwal3());
		diagnosaAwal3.setValue(diagnosaPenyakit.getDiagnosaAwal3() == null ? ""
				: diagnosaPenyakit.getDiagnosaAwal3().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAwal3().getNama_english());
		diagnosaAwal3.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komplikasi 4:")));
		row.appendChild(diagnosaAkhir3 = new AmbilDataIcdBanbox());
		diagnosaAkhir3.setAttribute("icd", diagnosaPenyakit.getDiagnosaAkhir3());
		diagnosaAkhir3.setValue(diagnosaPenyakit.getDiagnosaAkhir3() == null ? ""
				: diagnosaPenyakit.getDiagnosaAkhir3().getKode() + " - "
						+ diagnosaPenyakit.getDiagnosaAkhir3().getNama_english());
		diagnosaAkhir3.setWidth("90%");

		return borderlayout;
	}

	// private Borderlayout createPasienKeluar(DiagnosaPenyakit
	// diagnosaPenyakit)
	// throws Exception {
	// Borderlayout borderlayout = new Borderlayout();
	// Center center = new Center();
	// center.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(center, true);
	// Grid grid = new Grid();
	// grid.setParent(center);
	// grid.setWidth("100%");
	// grid.setHeight("100%");
	//
	// Columns columns = new Columns();
	// columns.setParent(grid);
	//
	// Column column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("20%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("80%");
	//
	// Rows rows = new Rows();
	// rows.setParent(grid);
	//
	// Row row = new Row();
	// row.setStyle("border:0px;background: transparent;");
	// row.setParent(rows);
	// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alasan Pasien Keluar (Pulang)")));
	// row.appendChild(statusPulang = new Combobox());
	// Common.insertCombo(statusPulang, "nama", StatusPulang.class);
	// Common.selectComboItem(statusPulang, diagnosaPenyakit.getStatusPulang());
	// statusPulang.setWidth("90%");
	// statusPulang.setDisabled(true);
	//
	// row = new Row();
	// row.setStyle("border:0px;background: transparent;");
	// row.setParent(rows);
	// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Keluar (Pulang)")));
	// row.appendChild(tanggalPulang = new MyDatebox(diagnosaPenyakit
	// .getTanggalPulang()));
	// tanggalPulang.setFormat(Common.dateFormat3.get().toPattern());
	// tanggalPulang.setWidth("90%");
	// tanggalPulang.setDisabled(true);
	//
	// return borderlayout;
	// }

	private EventListener subPolyEventListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Poly.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("polyDari",
							poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue()))
					.uniqueResult()).intValue();
			subPolyRow.setVisible(count > 0);
			Common.clear(subpoly);
			Common.insertCombo(subpoly, "nama", "jenis", Poly.class, Restrictions.eq("polyDari",
					poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue()));
			Common.selectComboItem(subpoly, diagnosaPenyakit.getSubpoly());

		}
	};

	private org.zkoss.zul.Div eastInfoPasien;

	private East eastTimDokterDanBidan;
	private Set<Dokter> setDokters;

	private org.zkoss.zul.Div southRiwayatPenyakitPasien;

	private Borderlayout createPoliDanDokter(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		Common.insertCombo(poly, "nama", "jenis", Poly.class, Restrictions.isNull("polyDari"));
		dokter = new AmbilDataDokterBanbox();
		// Ambil tim dokter/bidan terpilih dengan AMAN. Bila entity berasal dari daftar (detached),
		// koleksi lazy getDokters() melempar "failed to lazily initialize a collection ... no session
		// or session was closed". Jalur normal dicoba dulu (entity attached → perilaku sama persis);
		// bila gagal, tim dokter dimuat ulang lewat tabel relasi sirs.pandaftaran_has_dokter memakai
		// session aktif. setDokters memang hanya "working set" (di-reset & di-addAll saat simpan).
		try {
			setDokters = diagnosaPenyakit.getDokters();
			setDokters.size();
		} catch (Exception lazyEx) {
			setDokters = new HashSet<Dokter>();
			if (diagnosaPenyakit.getId() != null) {
				try {
					List<?> idDokters = HibernateUtil.currentSession()
							.createSQLQuery("select dokter from sirs.pandaftaran_has_dokter where pandaftaran = :diagId")
							.setParameter("diagId", diagnosaPenyakit.getId()).list();
					for (Object o : idDokters) {
						if (o instanceof Number) {
							Dokter d = (Dokter) HibernateUtil.currentSession()
									.get(Dokter.class, Long.valueOf(((Number) o).longValue()));
							if (d != null) {
								setDokters.add(d);
							}
						}
					}
				} catch (Exception q) {
					q.printStackTrace();
				}
			}
		}

		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		eastTimDokterDanBidan = new East();
		ais.ui.util.ZkCompat.setFlex(eastTimDokterDanBidan, true);
		eastTimDokterDanBidan.setParent(borderlayout);
		eastTimDokterDanBidan.setWidth("50%");
		CommonPendaftaranUtil.dokterDanBidanPemeriksa(eastTimDokterDanBidan, setDokters, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				setDokters = (Set<Dokter>) arg0.getData();
			}
		});

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("80%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter")));
		row.appendChild(dokter);
		dokter.setAttribute("dokter", diagnosaPenyakit.getDokter());
		dokter.setValue(diagnosaPenyakit.getDokter() == null ? ""
				: diagnosaPenyakit.getDokter().getKode() + " - " + diagnosaPenyakit.getDokter().getNama());
		dokter.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		row.appendChild(poly);
		Common.selectComboItem(poly, diagnosaPenyakit.getPoly());
		poly.setWidth("90%");

		subPolyRow = new Row();
		subPolyRow.setStyle("border:0px;background: transparent;");
		subPolyRow.setParent(rows);
		subPolyRow.appendChild(new Label(ais.common.Common.getBahasaConfig("Sub Poli")));
		subPolyRow.appendChild(subpoly);
		subpoly.setWidth("90%");

		poly.addEventListener("onChange", subPolyEventListener);
		subPolyEventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu")));
		row.appendChild(tanggal = new MyDatebox(diagnosaPenyakit.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setWidth("90%");

		return borderlayout;
	}

	private Borderlayout createCatatan(DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
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
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("80%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan")));
		row.appendChild(keterangan = new MyTextbox(
				diagnosaPenyakit.getKeterangan() == null ? "" : diagnosaPenyakit.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(10);

		return borderlayout;
	}

	private org.zkoss.zul.Vlayout createMain(DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		// Tata letak VERTIKAL (Vlayout + Grid/Rows/Row) MENGGANTIKAN Borderlayout — isian & riwayat
		// menumpuk pada TINGGI KONTEN sehingga tidak ada celah besar antara isian dan riwayat (frame
		// tidak lagi dipaksa tinggi lalu Center di-flex mengisinya).
		org.zkoss.zul.Vlayout wadah = new org.zkoss.zul.Vlayout();
		wadah.setWidth("100%");

		org.zkoss.zul.Div headerInput = new org.zkoss.zul.Div();
		headerInput.setStyle("background:#233876;color:#fff;font-weight:700;padding:8px 12px;border-radius:4px 4px 0 0;");
		headerInput.appendChild(new Label(ais.common.Common.getBahasaConfig("Input data Pasien")));
		headerInput.setParent(wadah);

		Grid grid = new Grid();
		grid.setParent(wadah);
		grid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("35%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("35%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Rekam Medis")));
		String mykode = diagnosaPenyakit.getKode();
		row.appendChild(kode = new MyTextbox(diagnosaPenyakit.getKode() == null ? mykode : diagnosaPenyakit.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		if (kode.getValue().trim().equals("")) {
			mykode = Common.generateCode(DiagnosaPenyakit.class, 8, "DGS", myLokasi);
			kode.setValue(mykode);
		}

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(
				nama = new Label(diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil dari Pendaftaran ")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranRawatJalanBanbox(false));
		pendaftaran.setAttribute("pendaftaran", diagnosaPenyakit.getPendaftaran());
		pendaftaran.setValue(diagnosaPenyakit.getPendaftaran() == null ? ""
				: diagnosaPenyakit.getPendaftaran().getKode() + " - "
						+ (diagnosaPenyakit.getPendaftaran().getPasien() == null ? ""
								: diagnosaPenyakit.getPendaftaran().getPasien().getNama()));
		pendaftaran.setWidth("90%");
		pendaftaran.setDisabled(diagnosaPenyakit.getPendaftaran() != null);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur = new Label(
				diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getUmur().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR Pasien")));

		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setValue(diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getKode());
		pasien.setAttribute("pasien", diagnosaPenyakit.getPasien());
		pasien.setEventListener(perubahanPasienListener);
		pasien.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(
				diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getAlamatLengkap()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Tempat / Tgl. Lahir"));
		row.appendChild(ttl = new Label(diagnosaPenyakit.getPasien() == null ? ""
				: diagnosaPenyakit.getPasien().getTanggalLahir() + " / "
						+ Common.dateFormat.get().format(diagnosaPenyakit.getPasien().getTanggalLahir())));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Label(
				diagnosaPenyakit.getPendaftaran() == null || diagnosaPenyakit.getPendaftaran().getJenisPasien() == null
						? ""
						: diagnosaPenyakit.getPendaftaran().getJenisPasien().getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getJenisKelamin()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Instalasi")));
		row.appendChild(instalasi = new Combobox());
		Common.insertCombo(instalasi, "nama", Instalasi.class);
		Common.selectComboItem(instalasi, diagnosaPenyakit.getInstalasi());
		instalasi.setWidth("90%");

		pendaftaran.setEventListener(perubahanPasienListener);

		CommonSirs.initLokasiDanShift(diagnosaPenyakit.getLokasi() == null ? myLokasi : diagnosaPenyakit.getLokasi(),
				diagnosaPenyakit.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
					}
				});

		// Panel detail paket (dulu East) — kini Div yang menumpuk di bawah, tampil saat paket dipilih.
		eastInfoPasien = new org.zkoss.zul.Div();
		eastInfoPasien.setWidth("100%");
		eastInfoPasien.setVisible(false);
		eastInfoPasien.setParent(wadah);

		// Riwayat penyakit pasien (dulu South 200px) — kini Div di bawah, tinggi mengikuti konten.
		southRiwayatPenyakitPasien = new org.zkoss.zul.Div();
		southRiwayatPenyakitPasien.setWidth("100%");
		southRiwayatPenyakitPasien.setParent(wadah);
		CommonPendaftaranUtil.riwayatPenyakitPasien(southRiwayatPenyakitPasien, diagnosaPenyakit.getPasien());

		return wadah;
	}

	private void init(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		this.diagnosaPenyakit = diagnosaPenyakit;

		save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Rekam Medis", "/img/save.gif");

		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");

		Center center = new Center();
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		Tab tabPasien = new Tab("Pasien");
		tabPasien.setParent(tabs);

		Tab tabDokter = new Tab("Poli dan Dokter");
		tabDokter.setParent(tabs);

		Tab tabKeluhan = new Tab("Keluhan Pasien");
		tabKeluhan.setParent(tabs);

		Tab tabRiwayat = new Tab("Riwayat Kesehatan");
		tabRiwayat.setParent(tabs);

		Tab tabPeriksa = new Tab("Proses Pemeriksaan");
		tabPeriksa.setParent(tabs);

		final Tab tabDiagnosis = new Tab("Hasil Diagnosis");
		tabDiagnosis.setParent(tabs);

		Tab tabResep = new Tab("Obat-obatan atau Resep");
		tabResep.setParent(tabs);

		Tab tabPerawatan = new Tab("Tindakan atau Perawatan");
		tabPerawatan.setParent(tabs);

		Tab tabMedis = new Tab("Alat Medis atau Kesehatan");
		tabMedis.setParent(tabs);

		Tab tabCatatan = new Tab("Catatan Tambahan");
		tabCatatan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createMain(diagnosaPenyakit));

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createPoliDanDokter(diagnosaPenyakit));

		final Tabpanel tabpanelKeluhan = new ais.ui.util.MyTabpanel();
		tabpanelKeluhan.setParent(tabpanels);
		tabKeluhan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKeluhan.getChildren().isEmpty()) {
					tabpanelKeluhan.appendChild(keluhan.createPemeriksaan(diagnosaPenyakit));
				}
			}
		});

		final Tabpanel tabpanelRiwayat = new ais.ui.util.MyTabpanel();
		tabpanelRiwayat.setParent(tabpanels);
		tabRiwayat.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelRiwayat.getChildren().isEmpty()) {
					tabpanelRiwayat.appendChild(riwayat.createPemeriksaan(diagnosaPenyakit));
				}
			}
		});

		final Tabpanel tabpanelPeriksa = new ais.ui.util.MyTabpanel();
		tabpanelPeriksa.setParent(tabpanels);
		tabPeriksa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPeriksa.getChildren().isEmpty()) {
					tabpanelPeriksa.appendChild(periksa.createPemeriksaan(diagnosaPenyakit));
				}
			}
		});

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createDiagnosa(diagnosaPenyakit));

		resepHelper = new ResepHelper(DiagnosaPenyakitAction.this, save);

		final Tabpanel tabpanelResep = new ais.ui.util.MyTabpanel();
		tabpanelResep.setParent(tabpanels);
		tabResep.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelResep.getChildren().isEmpty()) {
					tabpanelResep.appendChild(resepHelper.display(diagnosaPenyakit));

				}

				if (!resepHelper.setPaket(pakets, diagnosaPenyakit)) {
					tabDiagnosis.setSelected(true);
				}
			}
		});

		tindakanHelper = new TindakanHelper(DiagnosaPenyakitAction.this, save);

		final Tabpanel tabpanelPerawatan = new ais.ui.util.MyTabpanel();
		tabpanelPerawatan.setParent(tabpanels);
		tabPerawatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPerawatan.getChildren().isEmpty()) {
					tabpanelPerawatan.appendChild(tindakanHelper.init(diagnosaPenyakit));

				}

				if (!tindakanHelper.setPaket(pakets, diagnosaPenyakit)) {
					tabDiagnosis.setSelected(true);
				}
			}
		});

		alatMedisHelper = new AlatMedisHelper(DiagnosaPenyakitAction.this, save);

		final Tabpanel tabpanelMedis = new ais.ui.util.MyTabpanel();
		tabpanelMedis.setParent(tabpanels);
		tabMedis.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMedis.getChildren().isEmpty()) {
					tabpanelMedis.appendChild(alatMedisHelper.init(diagnosaPenyakit));

				}
				if (!alatMedisHelper.setPaket(pakets, diagnosaPenyakit)) {
					tabDiagnosis.setSelected(true);
				}
			}
		});

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createCatatan(diagnosaPenyakit));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.setDisabled(diagnosaPenyakit.getId() == null);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (onSave(event)) {

					MyMessageboxConfig.show("Data Rekam Medis telah berhasil disimpan. Terima kasih.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									if (pakets != null && !pakets.isEmpty()) {
										if (!tindakanHelper.setPaket(pakets, diagnosaPenyakit)) {
											return;
										}
										if (!alatMedisHelper.setPaket(pakets, diagnosaPenyakit)) {
											return;
										}
										if (!resepHelper.setPaket(pakets, diagnosaPenyakit)) {
											return;
										}
									}

									if (tambahData != null && add != null) {
										Common.freeze(tambahData, true);
										add.setDisabled(false);
										CommonSirs.onCetakHasilDiagnosaPasien(diagnosaPenyakit);
									}

								}
							});

					onSearchDefault(null);

				}
			}
		});
		save.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Rekam Medis", "/img/delete.gif");
		button.setTooltiptext("Batalkan Rekam Medis");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (diagnosaPenyakit != null && diagnosaPenyakit.getId() != null) {
					onDelete(diagnosaPenyakit);

				} else {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin membatalkan pengisian Rekam Medis ini? Data yang belum tersimpan akan hilang.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new DiagnosaPenyakit());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		perubahanPasienListener.onEvent(null);
	}

	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Lokasi belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu lokasi pelayanan sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Shift belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu shift pelayanan sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pendaftaran.getAttribute("pendaftaran") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Kode Pendaftaran belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu data pendaftaran pasien sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (!keluhan.check()) {
			return false;
		}

		if (!riwayat.check()) {
			return false;
		}

		if (!periksa.check()) {
			return false;
		}

		if (apakahMenular.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, status Menular / Tidak Menular belum diisi. Mohon Bapak/Ibu menentukan terlebih dahulu status penyakit sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (diagnosaAwal1.getAttribute("icd") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Diagnosa Awal 1 belum diisi. Mohon Bapak/Ibu mengisi terlebih dahulu hasil diagnosa awal sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (dokter.getAttribute("dokter") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Dokter belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu dokter pemeriksa sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (poly.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Poli belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu poli tujuan sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (subPolyRow.isVisible() && subpoly.getSelectedItem() == null) {
			Poly mypoly = (Poly) poly.getSelectedItem().getValue();
			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk Poli \"{V1}\" kolom Sub Poli belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu sub poli yang sesuai sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mypoly.getNama());
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (diagnosaPenyakit.getId() != null) {
			diagnosaPenyakit = (DiagnosaPenyakit) session.load(DiagnosaPenyakit.class, diagnosaPenyakit.getId());

		}

		diagnosaPenyakit
				.setSubpoly((Poly) (subpoly.getSelectedItem() == null ? null : subpoly.getSelectedItem().getValue()));
		diagnosaPenyakit.setInstalasi(
				(Instalasi) (instalasi.getSelectedItem() == null ? null : instalasi.getSelectedItem().getValue()));
		// diagnosaPenyakit.setStatusPulang((StatusPulang) (statusPulang
		// .getSelectedItem() == null ? null : statusPulang
		// .getSelectedItem().getValue()));
		// diagnosaPenyakit.setTanggalPulang(tanggalPulang.getValue());
		diagnosaPenyakit.setDokter((Dokter) dokter.getAttribute("dokter"));
		diagnosaPenyakit.setTanggal(tanggal.getValue());
		diagnosaPenyakit.setPoly((Poly) poly.getSelectedItem().getValue());
		// diagnosaPenyakit.setKeluhanDiagnosa(keluhanDiagnosa.getValue().trim());

		diagnosaPenyakit.setDiagnosaAwal1((Icd) diagnosaAwal1.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir1((Icd) diagnosaAkhir1.getAttribute("icd"));

		diagnosaPenyakit.setDiagnosaAwal2((Icd) diagnosaAwal2.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir2((Icd) diagnosaAkhir2.getAttribute("icd"));

		diagnosaPenyakit.setDiagnosaAwal3((Icd) diagnosaAwal3.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir3((Icd) diagnosaAkhir3.getAttribute("icd"));
		diagnosaPenyakit.setLokasi(myLokasi);
		diagnosaPenyakit.setApakahMenular((String) apakahMenular.getSelectedItem().getValue());

		diagnosaPenyakit.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		diagnosaPenyakit.setPasien(diagnosaPenyakit.getPendaftaran().getPasien());
		diagnosaPenyakit.setKeterangan(keterangan.getValue());
		diagnosaPenyakit.setKode(kode.getValue());

		diagnosaPenyakit.setDokters(new HashSet<Dokter>());
		diagnosaPenyakit.getDokters().addAll(setDokters);

		diagnosaPenyakit.setLokasi(myLokasi);
		diagnosaPenyakit.setShift(myShift);

		if (diagnosaPenyakit.getId() != null) {
			Common.refreshUpdate(session, diagnosaPenyakit);
		} else {
			diagnosaPenyakit.setIndex(Common.generateMaxByLokasi(DiagnosaPenyakit.class, myLokasi) + 1);
			String mykode = Common.generateCode(DiagnosaPenyakit.class, 8, "DGS", myLokasi);
			kode.setValue(mykode);
			diagnosaPenyakit.setKode(mykode);
			session.save(diagnosaPenyakit);
		}

		keluhan.simpan(diagnosaPenyakit);
		riwayat.simpan(diagnosaPenyakit);
		periksa.simpan(diagnosaPenyakit);

		return true;
	}

	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		String jenis = (String) (searchrajalranap.getSelectedItem() == null ? null
				: searchrajalranap.getSelectedItem().getValue());

		Pasien pasien = (Pasien) searchpasien.getAttribute("pasien");

		Criteria criteria = session.createCriteria(DiagnosaPenyakit.class)
				.add(Restrictions.isNull("diagnosaPenyakitInduk"))
				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.createAlias("pasien.kelurahan", "kelurahan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("pasien.propinsi", "propinsi", Criteria.LEFT_JOIN)

				.add(jenis == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pendaftaran.jenis", jenis))

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))

				.add((searchnip == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nip", searchnip.getValue(), MatchMode.ANYWHERE)))

				.add((searchpekerjaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.pekerjaan", searchpekerjaan.getValue(), MatchMode.ANYWHERE)))

				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", searchtelp.getValue(), MatchMode.ANYWHERE))))

				.add((searchalamat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("propinsi.nama", searchalamat.getValue(), MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("kota.nama", searchalamat.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("kecamatan.nama", searchalamat.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("kelurahan.nama", searchalamat.getValue(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("pasien.alamat", searchalamat.getValue(),
														MatchMode.ANYWHERE)))))))

				.add(pasien == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pasien", pasien))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pendaftaran.kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DiagnosaPenyakit> diagnosaPenyakit = initCriteria(true)

				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(diagnosaPenyakit);
		grid.setRowRenderer(new DiagnosaPenyakitRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

}
