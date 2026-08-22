package ais.action.master.sirs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
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

import ais.action.master.sirs.detail.AlatMedisHelper;
import ais.action.master.sirs.detail.PemeriksaanHelper;
import ais.action.master.sirs.detail.ResepHelper;
import ais.action.master.sirs.detail.TindakanHelper;
import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataIcdBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Icd;
import ais.database.model.sirs.Pemeriksaan;
import ais.database.model.sirs.Shift;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class DiagnosaPenyakitDetailAction extends GenericAutowireComposer implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	// private Window addWindow;

	private Tabpanel tambahData;

	private Grid grid;
	private Paging paging;

	// private MyTextbox kode;
	// private AmbilDataPendaftaranRawatJalanBanbox pendaftaran;
	// private AmbilDataPasienBanbox pasien;
	private Combobox apakahMenular;
	// private Combobox poly = new Combobox();
	// private Combobox subpoly = new Combobox();
	// private AmbilDataJenisPenyakitBanbox jenisPenyakit;

	private AmbilDataIcdBanbox diagnosaAwal1;
	private AmbilDataIcdBanbox diagnosaAwal2;
	private AmbilDataIcdBanbox diagnosaAwal3;
	private AmbilDataIcdBanbox diagnosaAkhir1;
	private AmbilDataIcdBanbox diagnosaAkhir2;
	private AmbilDataIcdBanbox diagnosaAkhir3;
	private MyDatebox tanggal;
	private AmbilDataDokterBanbox dokter;
	// private Combobox statusPulang;
	// private Combobox instalasi;
	// private MyDatebox tanggalPulang;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DiagnosaPenyakit diagnosaPenyakitParent;

	private DiagnosaPenyakit diagnosaPenyakit;
	private Toolbarbutton add;

	private PemeriksaanHelper keluhan = new PemeriksaanHelper(Pemeriksaan.JENIS_KELUHAN);
	private PemeriksaanHelper riwayat = new PemeriksaanHelper(Pemeriksaan.JENIS_RIWAYAT);
	private PemeriksaanHelper periksa = new PemeriksaanHelper(Pemeriksaan.JENIS_PERIKSA);
	private AlatMedisHelper alatMedisHelper;

	private Lokasi myLokasi;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		diagnosaPenyakitParent = (DiagnosaPenyakit) session.getAttribute("diagnosaPenyakitParent");
		if (diagnosaPenyakitParent == null) {
			return;
		}
		session.removeAttribute("diagnosaPenyakitParent");

		myLokasi = Common.getCurrentLokasi();
		add = new ais.ui.util.MyToolbarbuttonConfig("Buat Perawatan Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new DiagnosaPenyakit());
			}
		});

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

	}

	class DiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final DiagnosaPenyakit diagnosaPenyakit = (DiagnosaPenyakit) arg1;
			new Label(diagnosaPenyakit.getKode()).setParent(arg0);

			new Label(diagnosaPenyakit.getDokter() == null ? "" : diagnosaPenyakit.getDokter().getNama())
					.setParent(arg0);

			new Label(diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal())).setParent(arg0);
			new Label(diagnosaPenyakit.getKeluhanDiagnosa()).setParent(arg0);
			new Label(diagnosaPenyakit.getKeluhanPasien()).setParent(arg0);

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
					CommonSirs.onCetakHasilDiagnosaPasienRawatInap(diagnosaPenyakit);
				}

			});
			button.setParent(toolbar);

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
					tambahData.getLinkedTab().setSelected(true);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {

		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin membatalkan perawatan ini? Perlu diketahui, seluruh data yang terkait dengan perawatan ini akan ikut dibatalkan dan tidak dapat dikembalikan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {

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

								session.createSQLQuery(
										"delete from sirs.resep where diagnosa_penyakit = " + diagnosaPenyakit.getId())
										.executeUpdate();
								session.createSQLQuery(
										"delete from sirs.tindakan_diagnosa_penyakit where diagnosa_penyakit = "
												+ diagnosaPenyakit.getId())
										.executeUpdate();

								Common.refreshDelete(session, diagnosaPenyakit);
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

	private Toolbarbutton save;

	private TindakanHelper tindakanHelper;

	private ResepHelper resepHelper;

	private East eastTimDokterDanBidan;

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

	private Set<Dokter> setDokters;

	private South southRiwayatPenyakitPasien;

	protected Shift myShift;

	private Borderlayout createPoliDanDokter(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		// Common.insertCombo(poly, "nama", "jenis", Poly.class,
		// Restrictions.isNull("polyDari"));
		dokter = new AmbilDataDokterBanbox();
		// Ambil tim dokter/bidan terpilih dengan AMAN. Bila entity berasal dari daftar (detached),
		// koleksi lazy getDokters() melempar "failed to lazily initialize a collection ... no session
		// or session was closed". Jalur normal dicoba dulu (entity attached → perilaku sama persis);
		// bila gagal, tim dokter dimuat ulang lewat tabel relasi sirs.pandaftaran_has_dokter.
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

		Center parentcenter = new Center();
		parentcenter.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(parentcenter, true);

		Borderlayout subborderlayout = new Borderlayout();
		subborderlayout.setParent(parentcenter);
		Center center = new Center();
		center.setParent(subborderlayout);
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter")));
		row.appendChild(dokter);
		dokter.setAttribute("dokter", diagnosaPenyakit.getDokter());
		dokter.setValue(diagnosaPenyakit.getDokter() == null ? ""
				: diagnosaPenyakit.getDokter().getKode() + " - " + diagnosaPenyakit.getDokter().getNama());
		dokter.setWidth("90%");

		// row = new Row();
		// row.setStyle("border:0px;background: transparent;");
		// row.setParent(rows);
		// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		// row.appendChild(poly);
		// Common.selectComboItem(poly, diagnosaPenyakit.getPoly());
		// poly.setWidth("90%");

		// subPolyRow = new Row();
		// subPolyRow.setStyle("border:0px;background: transparent;");
		// subPolyRow.setParent(rows);
		// subPolyRow.appendChild(new Label(ais.common.Common.getBahasaConfig("Sub Poli")));
		// subPolyRow.appendChild(subpoly);
		// subpoly.setWidth("90%");

		// poly.addEventListener("onChange", subPolyEventListener);
		// subPolyEventListener.onEvent(null);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Perikasa")));
		row.appendChild(tanggal = new MyDatebox(diagnosaPenyakit.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setWidth("90%");

		CommonSirs.initLokasiDanShift(diagnosaPenyakit.getLokasi() == null ? myLokasi : diagnosaPenyakit.getLokasi(),
				diagnosaPenyakit.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
					}
				});

		southRiwayatPenyakitPasien = new South();
		southRiwayatPenyakitPasien.setHeight("250px");
		southRiwayatPenyakitPasien.setParent(subborderlayout);
		CommonPendaftaranUtil.riwayatPenyakitPasien(southRiwayatPenyakitPasien, diagnosaPenyakit.getPasien());

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

	private void init(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		diagnosaPenyakit.setDiagnosaPenyakitInduk(diagnosaPenyakitParent);
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
		// tabbox.setOrient("vertical");
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		Tab tab = new Tab("Dokter Pemeriksa");
		tab.setParent(tabs);

		Tab tabKeluhan = new Tab("Keluhan Pasien");
		tabKeluhan.setParent(tabs);

		Tab tabRiwayat = new Tab("Riwayat Kesehatan");
		tabRiwayat.setParent(tabs);

		Tab tabPeriksa = new Tab("Proses Pemeriksaan");
		tabPeriksa.setParent(tabs);

		tab = new Tab("Hasil Diagnosis");
		tab.setParent(tabs);

		Tab tabResep = new Tab("Obat-obatan atau Resep");
		tabResep.setParent(tabs);

		Tab tabPerawatan = new Tab("Tindakan atau Perawatan");
		tabPerawatan.setParent(tabs);

		Tab tabMedis = new Tab("Alat Medis atau Kesehatan");
		tabMedis.setParent(tabs);

		tab = new Tab("Catatan Tambahan");
		tab.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
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

		resepHelper = new ResepHelper(DiagnosaPenyakitDetailAction.this, save);

		final Tabpanel tabpanelResep = new ais.ui.util.MyTabpanel();
		tabpanelResep.setParent(tabpanels);
		tabResep.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelResep.getChildren().isEmpty()) {
					tabpanelResep.appendChild(resepHelper.display(diagnosaPenyakit));

				}
			}
		});

		tindakanHelper = new TindakanHelper(DiagnosaPenyakitDetailAction.this, save);

		final Tabpanel tabpanelPerawatan = new ais.ui.util.MyTabpanel();
		tabpanelPerawatan.setParent(tabpanels);
		tabPerawatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPerawatan.getChildren().isEmpty()) {
					tabpanelPerawatan.appendChild(tindakanHelper.init(diagnosaPenyakit));

				}

			}
		});

		alatMedisHelper = new AlatMedisHelper(DiagnosaPenyakitDetailAction.this, save);

		final Tabpanel tabpanelMedis = new ais.ui.util.MyTabpanel();
		tabpanelMedis.setParent(tabpanels);
		tabMedis.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMedis.getChildren().isEmpty()) {
					tabpanelMedis.appendChild(alatMedisHelper.init(diagnosaPenyakit));

				}

			}
		});

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createCatatan(diagnosaPenyakit));

		South north = new South();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		add.setParent(toolbar);
		save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Diagnosis", "/img/save.gif");
		save.setTooltiptext("Simpan");
		// save.setDisabled(diagnosaPenyakit.getId() == null);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					MyMessageboxConfig.show("Data Diagnosis telah berhasil disimpan. Terima kasih.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tambahData != null && add != null) {
										Common.freeze(tambahData, true);
										add.setDisabled(false);
										CommonSirs.onCetakHasilDiagnosaPasienRawatInap(diagnosaPenyakit);
									}

								}
							});

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

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Diagnosis", "/img/delete.gif");
		button.setTooltiptext("Batalkan Diagnosis");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (diagnosaPenyakit != null && diagnosaPenyakit.getId() != null) {
					onDelete(diagnosaPenyakit);

				} else {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin membatalkan pengisian Diagnosis ini? Data yang belum tersimpan akan hilang.",
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

		Session session = HibernateUtil.currentSession();
		if (diagnosaPenyakit.getId() != null) {
			diagnosaPenyakit = (DiagnosaPenyakit) session.load(DiagnosaPenyakit.class, diagnosaPenyakit.getId());

		}

		// diagnosaPenyakit
		// .setSubpoly((Poly) (subpoly.getSelectedItem() == null ? null
		// : subpoly.getSelectedItem().getValue()));
		diagnosaPenyakit.setInstalasi(diagnosaPenyakitParent.getInstalasi());
		// diagnosaPenyakit.setStatusPulang((StatusPulang) (statusPulang
		// .getSelectedItem() == null ? null : statusPulang
		// .getSelectedItem().getValue()));
		// diagnosaPenyakit.setTanggalPulang(tanggalPulang.getValue());
		diagnosaPenyakit.setDokter((Dokter) dokter.getAttribute("dokter"));
		diagnosaPenyakit.setTanggal(tanggal.getValue());
		// diagnosaPenyakit.setPoly((Poly) poly.getSelectedItem().getValue());

		diagnosaPenyakit.setDiagnosaAwal1((Icd) diagnosaAwal1.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir1((Icd) diagnosaAkhir1.getAttribute("icd"));

		diagnosaPenyakit.setDiagnosaAwal2((Icd) diagnosaAwal2.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir2((Icd) diagnosaAkhir2.getAttribute("icd"));

		diagnosaPenyakit.setDiagnosaAwal3((Icd) diagnosaAwal3.getAttribute("icd"));
		diagnosaPenyakit.setDiagnosaAkhir3((Icd) diagnosaAkhir3.getAttribute("icd"));
		diagnosaPenyakit.setLokasi(myLokasi);
		diagnosaPenyakit.setApakahMenular((String) apakahMenular.getSelectedItem().getValue());
		// diagnosaPenyakit.setJenisPenyakit((JenisPenyakit) jenisPenyakit
		// .getAttribute("jenisPenyakit"));
		diagnosaPenyakit.setPendaftaran(diagnosaPenyakitParent.getPendaftaran());
		diagnosaPenyakit.setPasien(diagnosaPenyakit.getPendaftaran().getPasien());
		diagnosaPenyakit.setKeterangan(keterangan.getValue());
		diagnosaPenyakit.setDiagnosaPenyakitInduk(diagnosaPenyakitParent);

		diagnosaPenyakit.setDokters(new HashSet<Dokter>());
		diagnosaPenyakit.getDokters().addAll(setDokters);

		diagnosaPenyakit.setLokasi(myLokasi);
		diagnosaPenyakit.setShift(myShift);

		if (diagnosaPenyakit.getId() != null) {
			Common.refreshUpdate(session, diagnosaPenyakit);
		} else {
			diagnosaPenyakit.setIndex(Common.generateMaxByLokasi(DiagnosaPenyakit.class, myLokasi) + 1);
			String mykode = Common.generateCode(DiagnosaPenyakit.class, 8, "DGS", myLokasi);
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
		Criteria criteria = session.createCriteria(DiagnosaPenyakit.class)
				.add(Restrictions.eq("diagnosaPenyakitInduk", this.diagnosaPenyakitParent));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<DiagnosaPenyakit> diagnosaPenyakit = initCriteria(true).list();
		ListModel strset = new SimpleListModel(diagnosaPenyakit);
		grid.setRowRenderer(new DiagnosaPenyakitRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

}
