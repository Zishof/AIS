package ais.action.master.sirs;

import java.util.ArrayList;
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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
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
import ais.action.master.sirs.helper.AmbilDataDokterBanyak;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatInapBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.RawatInapCalculationProcessor;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Instalasi;
import ais.database.model.sirs.KunjunganDokter;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk diagnosa penyakit rawat inap. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel tambahData}, {@code Grid
 * grid}, {@code Paging paging}, {@code MyTextbox searchkode}, {@code AmbilDataPasienBanbox searchpasien}, {@code
 * MyTextbox searchtelp}, {@code MyTextbox searchalamat}, {@code MyTextbox searchnama}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); penghapusan/pembatalan ({@code onProcessDelete()}, {@code
 * onDelete()}); operasi domain lain ({@code onAdd()}, {@code createCatatan()}, {@code createMain()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class DiagnosaPenyakitRawatInapAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	// private Window addWindow;

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
	private AmbilDataPendaftaranRawatInapBanbox pendaftaran;
	private AmbilDataPasienBanbox pasien;

	private Combobox instalasi;
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

	private KunjunganDokterAction kunjunganDokterAction = new KunjunganDokterAction();

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

		myLokasi = Common.getCurrentLokasi();
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

		comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.RAWAT_INAP); }
		searchrajalranap.appendChild(comboitem);
		if (searchrajalranap != null) { searchrajalranap.setSelectedItem(comboitem); }
		if (searchrajalranap != null) { searchrajalranap.setDisabled(true); }

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
				Common.initPaging(paging, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});

			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DiagnosaPenyakitRawatInapAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DiagnosaPenyakitRawatInapAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DiagnosaPenyakitRawatInapAction
	 */
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

			new Label(diagnosaPenyakit.getStatusPulang() == null ? "" : diagnosaPenyakit.getStatusPulang().getNama())
					.setParent(arg0);
			new Label(diagnosaPenyakit.getTanggalPulang() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggalPulang())).setParent(arg0);
			new Label(diagnosaPenyakit.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setVisible(pasien.getAktif() != null && pasien.getAktif());
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (diagnosaPenyakit.getLunas() == null || !diagnosaPenyakit.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(diagnosaPenyakit);
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

			// if (pendaftaran.getPoly() != null) {
			// Common.selectComboItem(poly, pendaftaran.getPoly());
			// subPolyEventListener.onEvent(null);
			// Common.selectComboItem(subpoly, pendaftaran.getSubpoly());
			// } else {
			// poly.setDisabled(false);
			// }
			//
			// if (pendaftaran.getDokter() != null) {
			// dokter.setAttribute("dokter", pendaftaran.getDokter());
			// dokter.setValue(pendaftaran.getDokter().getNama());
			// // dokter.setDisabled(true);
			// } else {
			// dokter.setDisabled(false);
			// }

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

			Pasien pasien = (Pasien) DiagnosaPenyakitRawatInapAction.this.pasien.getAttribute("pasien");
			Pendaftaran pendaftaran = null;
			if (pasien == null) {
				pendaftaran = (Pendaftaran) DiagnosaPenyakitRawatInapAction.this.pendaftaran
						.getAttribute("pendaftaran");

				if (pendaftaran == null || pendaftaran.getPasien() == null) {
					return;
				}

				DiagnosaPenyakitRawatInapAction.this.pasien.setAttribute("pasien", pendaftaran.getPasien());
				DiagnosaPenyakitRawatInapAction.this.pasien.setValue(pendaftaran.getPasien().getKode());
				DiagnosaPenyakitRawatInapAction.this.pasien.setDisabled(true);

			} else {
				DiagnosaPenyakitRawatInapAction.this.pendaftaran.setDisabled(true);
				pendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.eq("jenis", Pendaftaran.RAWAT_INAP)).add(Restrictions.eq("pasien", pasien))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				if (pendaftaran == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, data pendaftaran rawat inap untuk pasien ini tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien telah melakukan pendaftaran rawat inap; (2) periksa kembali data pasien yang dipilih; (3) apabila diperlukan, lakukan pendaftaran rawat inap terlebih dahulu sebelum melanjutkan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				DiagnosaPenyakitRawatInapAction.this.pendaftaran.setAttribute("pendaftaran", pendaftaran);
				DiagnosaPenyakitRawatInapAction.this.pendaftaran.setValue(pendaftaran.getKode());

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

	protected Shift myShift;

	private South southRiwayatPenyakitPasien;

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

	private Borderlayout createMain(DiagnosaPenyakit diagnosaPenyakit) throws Exception {
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil dari Pendaftaran Rawat Inap")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranRawatInapBanbox(false));
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
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				diagnosaPenyakit.getPasien() == null ? "" : diagnosaPenyakit.getPasien().getJenisKelamin()));

		row = new Row();
		row.setVisible(false);
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

		southRiwayatPenyakitPasien = new South();
		southRiwayatPenyakitPasien.setHeight("200px");
		southRiwayatPenyakitPasien.setParent(borderlayout);
		CommonPendaftaranUtil.riwayatPenyakitPasien(southRiwayatPenyakitPasien, diagnosaPenyakit.getPasien());

		return borderlayout;
	}

	// private class TindakanAction {
	//
	// private Grid gridTindakan;
	//
	// private DiagnosaPenyakit diagnosaPenyakit;
	//
	// public Borderlayout init(DiagnosaPenyakit diagnosaPenyakit) {
	// this.diagnosaPenyakit = diagnosaPenyakit;
	// return display();
	// }
	//
	// public Borderlayout display() {
	//
	// Borderlayout borderlayout = new Borderlayout();
	//
	// North north = new North();
	// ais.ui.util.ZkCompat.setFlex(north, true);
	// north.setParent(borderlayout);
	//
	// Toolbar toolbar = new Toolbar();
	// toolbar.setHeight("25px");
	// toolbar.setParent(north);
	// Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data
	// Tindakan",
	// "/img/add_item.png");
	// button.addEventListener("onClick", new EventListener() {
	//
	// @SuppressWarnings("unchecked")
	// @Override
	// public void onEvent(Event event) throws Exception {
	//
	// final Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran
	// .getAttribute("pendaftaran");
	// if (myPendaftaran == null) {
	// Messagebox.show("Masukkan kode pendaftaran",
	// "Peringatan", Messagebox.OK,
	// Messagebox.EXCLAMATION);
	// return;
	// }
	//
	// if (!DiagnosaPenyakitRawatInapAction.this.onSave(event)) {
	// return;
	// }
	//
	// // Session session = HibernateUtil.currentSession();
	// //
	// // List<Tindakan> tindakans = session
	// // .createCriteria(TindakanDiagnosaPenyakit.class)
	// // .setProjection(
	// // Projections.groupProperty("tindakan"))
	// // .add(Restrictions.eq("diagnosaPenyakit",
	// // diagnosaPenyakit)).list();
	//
	// AmbilDataTindakanBanyak ambilDataTindakanBanyak = new
	// AmbilDataTindakanBanyak(
	// new ArrayList<Tindakan>());
	// ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
	// .getFirstRoot()
	// .appendChild(ambilDataTindakanBanyak);
	// ambilDataTindakanBanyak
	// .setEventListener(new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0)
	// throws Exception {
	//
	// List<Tindakan> tindakans = (List<Tindakan>) arg0
	// .getData();
	//
	// save.setDisabled(tindakans.size() == 0);
	//
	// TindakanDiagnosaPenyakitDao tindakanDiagnosaPenyakitDao = DaoFactory
	// .getInstance()
	// .getTindakanDiagnosaPenyakitDao();
	// Session session = tindakanDiagnosaPenyakitDao
	// .getCurrentSession();
	// for (Tindakan tindakan : tindakans) {
	//
	// KelasPerawatan kelasPerawatan = myPendaftaran
	// .getKelasPerawatan() == null ? ConstantValues.kelasNormal
	// : myPendaftaran
	// .getKelasPerawatan();
	//
	// BiayaTindakanPerKelas biayaTindakanPerKelas =
	// Common.getBiayaTindakanPerKelas(tindakan, kelasPerawatan);
	// (BiayaTindakanPerKelas)
	// session
	// .createCriteria(
	// BiayaTindakanPerKelas.class)
	// .add(Restrictions.eq(
	// "tindakan", tindakan))
	// .add(Restrictions.eq(
	// "kelasPerawatan",
	// kelasPerawatan))
	// .setMaxResults(1)
	// .uniqueResult();
	//
	// if (biayaTindakanPerKelas == null) {
	// Messagebox.show(
	// "Biaya untuk layanan "
	// + tindakan
	// .getNama()
	// + " kelas "
	// + kelasPerawatan
	// .getNama()
	// + " belum dimasukkan",
	// "Peringatan",
	// Messagebox.OK,
	// Messagebox.EXCLAMATION);
	// continue;
	// }
	//
	// TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit = new
	// TindakanDiagnosaPenyakit();
	// tindakanDiagnosaPenyakit
	// .setTindakan(tindakan);
	// tindakanDiagnosaPenyakit.setJumlah(1.0);
	// tindakanDiagnosaPenyakit.setBiaya(biayaTindakanPerKelas == null
	// || biayaTindakanPerKelas
	// .getBiaya() == null ? 0.0
	// : biayaTindakanPerKelas
	// .getBiaya());
	//
	// tindakanDiagnosaPenyakit.setFeeDokter(biayaTindakanPerKelas == null
	// || biayaTindakanPerKelas
	// .getFeeDokter() == null ? 0.0
	// : biayaTindakanPerKelas
	// .getFeeDokter());
	//
	// tindakanDiagnosaPenyakit.setFeeMedis(biayaTindakanPerKelas == null
	// || biayaTindakanPerKelas
	// .getFeeMedis() == null ? 0.0
	// : biayaTindakanPerKelas
	// .getFeeMedis());
	//
	// tindakanDiagnosaPenyakit.setFeeRumahsakit(biayaTindakanPerKelas == null
	// || biayaTindakanPerKelas
	// .getFeeRumahsakit() == null ? 0.0
	// : biayaTindakanPerKelas
	// .getFeeRumahsakit());
	//
	// tindakanDiagnosaPenyakit
	// .setKelasPerawatan(myPendaftaran
	// .getKelasPerawatan() == null ? ConstantValues.kelasNormal
	// : myPendaftaran
	// .getKelasPerawatan());
	// tindakanDiagnosaPenyakit
	// .setKeterangan("");
	// tindakanDiagnosaPenyakit
	// .setDiagnosaPenyakit(diagnosaPenyakit);
	// tindakanDiagnosaPenyakitDao
	// .save(tindakanDiagnosaPenyakit);
	//
	// }
	//
	// loadData(null);
	// }
	// });
	// ambilDataTindakanBanyak.setWidth("750px");
	// ambilDataTindakanBanyak.setHeight("97%");
	// ambilDataTindakanBanyak.setVisible(true);
	// ambilDataTindakanBanyak.onModal();
	// }
	//
	// });
	// button.setParent(toolbar);
	//
	// Center center = new Center();
	// center.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(center, true);
	//
	// gridTindakan = new Grid();
	// gridTindakan.setMold("paging");
	// gridTindakan.setPageSize(25);
	// gridTindakan.setParent(center);
	//
	// Columns columns = new Columns();
	//
	// columns.setParent(gridTindakan);
	//
	// Column column = new Column();
	// column.setParent(columns);
	// column.setLabel("Tindakan");
	// column.setWidth("40%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Qty");
	// column.setAlign("right");
	// column.setWidth("10%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Waktu");
	// column.setWidth("15%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Keterangan");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("10%");
	// loadData(null);
	// return borderlayout;
	// }
	//
	// class TindakanDiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {
	//
	// @Override
	// public void render(final Row arg0, Object arg1) throws Exception {
	// // TODO Auto-generated method stub
	// final TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit =
	// (TindakanDiagnosaPenyakit) arg1;
	// final Tindakan tindakan = tindakanDiagnosaPenyakit
	// .getTindakan();
	//
	// new Label(tindakan.getNama()).setParent(arg0);
	//
	// final MyDoublebox jumlah;
	// jumlah = new MyDoublebox(
	// tindakanDiagnosaPenyakit.getJumlah() == null ? 1.0
	// : tindakanDiagnosaPenyakit.getJumlah());
	// jumlah.setParent(arg0);
	// jumlah.setStyle("text-align:right");
	// jumlah.setWidth("90%");
	// jumlah.setWidth("90%");
	// jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil.currentSession();
	// tindakanDiagnosaPenyakit
	// .setJumlah(jumlah.getValue() == null ? 0.0
	// : jumlah.getValue());
	// Common.refreshUpdate(session, (tindakanDiagnosaPenyakit));
	//
	// }
	// });
	//
	// final MyDatebox tanggal;
	// tanggal = new MyDatebox(tindakanDiagnosaPenyakit.getTanggal());
	// tanggal.setParent(arg0);
	// tanggal.setStyle("text-align:right");
	// tanggal.setWidth("90%");
	// tanggal.addEventListener(Events.ON_CHANGE, new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil.currentSession();
	// tindakanDiagnosaPenyakit.setTanggal(tanggal.getValue());
	// Common.refreshUpdate(session, (tindakanDiagnosaPenyakit));
	//
	// }
	// });
	//
	// final MyTextbox keterangan = new MyTextbox(
	// tindakanDiagnosaPenyakit.getKeterangan() == null ? ""
	// : tindakanDiagnosaPenyakit.getKeterangan());
	// keterangan.setWidth("90%");
	// keterangan.setParent(arg0);
	//
	// keterangan.addEventListener(Events.ON_CHANGE,
	// new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil
	// .currentSession();
	// tindakanDiagnosaPenyakit
	// .setKeterangan(keterangan.getValue());
	// session.update(session
	// .merge(tindakanDiagnosaPenyakit));
	// }
	// });
	//
	// Hbox toolbar = new Hbox();
	// Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("",
	// "/img/delete.gif");
	// button.setTooltiptext("Hapus Data");
	// button.setVisible(delete);
	// button.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// Messagebox.show(
	// "Apakah yakin ingin menghapus data ini ?",
	// "Pertanyaan",
	// Messagebox.OK | Messagebox.CANCEL,
	// Messagebox.QUESTION, new EventListener() {
	//
	// @Override
	// public void onEvent(Event event)
	// throws Exception {
	// int i = new Integer(event.getData()
	// .toString());
	// if (i == Messagebox.OK) {
	// try {
	// TindakanDiagnosaPenyakitDao tindakanDiagnosaPenyakitDao = DaoFactory
	// .getInstance()
	// .getTindakanDiagnosaPenyakitDao();
	//
	// tindakanDiagnosaPenyakitDao
	// .delete(tindakanDiagnosaPenyakitDao
	// .merge(tindakanDiagnosaPenyakit));
	// loadData(null);
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/DiagnosaPenyakitRawatInapAction.java:946");
	// ais.common.Common.tampilErrorJikaAdmin(e);
	// Messagebox.show("Data ini tidak dapat dihapus .., karena berelasi dengan data
	// lainnya, error-nya adalah sbagai berikut:"
	// + e.getMessage());
	// }
	//
	// }
	//
	// }
	// });
	//
	// }
	// });
	// button.setParent(toolbar);
	// toolbar.setParent(arg0);
	// }
	//
	// }
	//
	// @SuppressWarnings("unchecked")
	// public void loadData(Event event) {
	// Session session = HibernateUtil.currentSession();
	// List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits =
	// diagnosaPenyakit == null
	// || diagnosaPenyakit.getId() == null ? new
	// ArrayList<TindakanDiagnosaPenyakit>()
	// : session
	// .createCriteria(TindakanDiagnosaPenyakit.class)
	// .addOrder(Order.desc("id"))
	// .add(Restrictions.eq("diagnosaPenyakit",
	// diagnosaPenyakit))
	// .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
	// .setFirstResult(
	// Common.ROWS_COUNT_ON_PAGE
	// * (paging == null ? 0 : paging
	// .getActivePage())).list();
	// ListModel strset = new SimpleListModel(tindakanDiagnosaPenyakits);
	// gridTindakan.setRowRenderer(new TindakanDiagnosaPenyakitRenderer());
	// gridTindakan.setModel(strset);
	// gridTindakan.renderAll();
	//
	// }
	//
	// }

	// private Borderlayout createCatatanHasilLab(DiagnosaPenyakit
	// diagnosaPenyakit)
	// throws Exception {
	// Borderlayout borderlayout = new Borderlayout();
	//
	// South south = new South();
	// ais.ui.util.ZkCompat.setFlex(south, true);
	// south.setParent(borderlayout);
	//
	// Toolbar toolbar = new Toolbar();
	// toolbar.setHeight("30px");
	// toolbar.setParent(south);
	//
	// final Transaksi transaksi = (Transaksi) HibernateUtil
	// .currentSession()
	// .createCriteria(Transaksi.class)
	// .add(Restrictions.eq("pendaftaran",
	// diagnosaPenyakit.getPendaftaran())).setMaxResults(1)
	// .addOrder(Order.desc("id")).uniqueResult();
	//
	// Toolbarbutton cetak = new ais.ui.util.MyToolbarbuttonConfig("Cetak Hasil Uji
	// Lab.",
	// "/img/print.png");
	// cetak.setTooltiptext("Cetak");
	// cetak.setDisabled(transaksi == null || transaksi.getId() == null);
	// cetak.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// final Map<String, Serializable> parameters = new HashMap<String,
	// Serializable>();
	// parameters.put("id", transaksi.getId());
	// Report.generateWindowReport(Report.PDF, parameters,
	// "sirs/hasil_uji_laboratorium",
	// transaksi.getTanggalTransaksi(), application);
	// }
	// });
	// cetak.setParent(toolbar);
	//
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
	// column.setWidth("10%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("90%");
	//
	// Rows rows = new Rows();
	// rows.setParent(grid);
	//
	// Row row = new Row();
	// row.setStyle("border:0px;background: transparent;");
	// row.setParent(rows);
	// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan Hasil Lab")));
	// row.appendChild(keteranganhasillab = new MyTextbox(diagnosaPenyakit
	// .getKeteranganhasillab() == null ? "" : diagnosaPenyakit
	// .getKeteranganhasillab()));
	// keteranganhasillab.setWidth("90%");
	// keteranganhasillab.setRows(10);
	//
	// return borderlayout;
	// }

	// private Borderlayout createCatatanHasilRontgen(
	// DiagnosaPenyakit diagnosaPenyakit) throws Exception {
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
	// column.setWidth("10%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("90%");
	//
	// Rows rows = new Rows();
	// rows.setParent(grid);
	//
	// Row row = new Row();
	// row.setStyle("border:0px;background: transparent;");
	// row.setParent(rows);
	// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan Hasil Rontgen")));
	// row.appendChild(keteranganhasilrontgen = new MyTextbox(diagnosaPenyakit
	// .getKeteranganhasilrontgen() == null ? "" : diagnosaPenyakit
	// .getKeteranganhasilrontgen()));
	// keteranganhasilrontgen.setWidth("90%");
	// keteranganhasilrontgen.setRows(10);
	//
	// return borderlayout;
	// }

	private void init(final DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		this.diagnosaPenyakit = diagnosaPenyakit;

		final South south = new South();

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

		final Tab tabUtama = new Tab("Pasien");
		tabUtama.setParent(tabs);
		tabUtama.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				south.setVisible(true);
			}
		});

		Tab tabDiagnosis = new Tab("Perawatan");
		tabDiagnosis.setParent(tabs);

		Tab tabKunjungan = new Tab("Kunjungan Dokter");
		tabKunjungan.setParent(tabs);
		tabKunjungan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				south.setVisible(true);
			}
		});

		Tab tabCatatan = new Tab("Catatan");
		tabCatatan.setParent(tabs);
		tabCatatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				south.setVisible(true);
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createMain(diagnosaPenyakit));

		final Tabpanel diagnosistabpanel = new ais.ui.util.MyTabpanel();
		diagnosistabpanel.setParent(tabpanels);

		tabDiagnosis.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!onSave(arg0)) {
					tabUtama.setSelected(true);
					return;
				}

				if (diagnosistabpanel.getChildren().isEmpty()) {
					session.setAttribute("diagnosaPenyakitParent",
							DiagnosaPenyakitRawatInapAction.this.diagnosaPenyakit);

					Include include = new MyInclude("/pages/master/sirs/diagnosa_penyakit_detail.zul");
					include.setHeight("100%");
					include.setWidth("100%");
					diagnosistabpanel.appendChild(include);
				}

				south.setVisible(false);
			}
		});

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(kunjunganDokterAction.init(diagnosaPenyakit));

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(createCatatan(diagnosaPenyakit));

		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Rekam Medis", "/img/save.gif");
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
									if (tambahData != null && add != null) {
										Common.freeze(tambahData, true);
										add.setDisabled(false);
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
		// if (jenisPenyakit.getAttribute("jenisPenyakit") == null) {
		// Messagebox.show("Jenis penyakit harus diisi", "Peringatan",
		// Messagebox.OK, Messagebox.EXCLAMATION);
		// return false;
		// }
		// if (apakahMenular.getSelectedItem() == null) {
		// Messagebox.show("Menular / Tidak Menular harus diisi",
		// "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
		// return false;
		// }
		// if (diagnosaAwal1.getAttribute("icd") == null) {
		// Messagebox.show("Diagnosa awal 1 harus diisi", "Peringatan",
		// Messagebox.OK, Messagebox.EXCLAMATION);
		// return false;
		// }
		// if (dokter.getAttribute("dokter") == null) {
		// Messagebox.show("Dokter harus diisi", "Peringatan", Messagebox.OK,
		// Messagebox.EXCLAMATION);
		// return false;
		// }
		// if (poly.getSelectedItem() == null) {
		// Messagebox.show("Poli harus diisi", "Peringatan", Messagebox.OK,
		// Messagebox.EXCLAMATION);
		// return false;
		// }
		// if (subPolyRow.isVisible() && subpoly.getSelectedItem() == null) {
		// Poly mypoly = (Poly) poly.getSelectedItem().getValue();
		// Messagebox.show("Untuk Poli " + mypoly.getNama()
		// + ", sub poli harus diisi", "Peringatan", Messagebox.OK,
		// Messagebox.EXCLAMATION);
		// return false;
		// }

		Session session = HibernateUtil.currentSession();
		if (diagnosaPenyakit.getId() != null) {
			diagnosaPenyakit = (DiagnosaPenyakit) session.load(DiagnosaPenyakit.class, diagnosaPenyakit.getId());

		}

		// diagnosaPenyakit
		// .setSubpoly((Poly) (subpoly.getSelectedItem() == null ? null
		// : subpoly.getSelectedItem().getValue()));
		diagnosaPenyakit.setInstalasi(
				(Instalasi) (instalasi.getSelectedItem() == null ? null : instalasi.getSelectedItem().getValue()));
		// diagnosaPenyakit.setStatusPulang((StatusPulang) (statusPulang
		// .getSelectedItem() == null ? null : statusPulang
		// .getSelectedItem().getValue()));
		// diagnosaPenyakit.setTanggalPulang(tanggalPulang.getValue());
		// diagnosaPenyakit.setDokter((Dokter) dokter.getAttribute("dokter"));
		// diagnosaPenyakit.setTanggal(tanggal.getValue());
		// diagnosaPenyakit.setPoly((Poly) poly.getSelectedItem().getValue());

		// diagnosaPenyakit.setKeluhanDiagnosa(keluhanDiagnosa.getValue().trim());
		// diagnosaPenyakit.setKeluhanPasien(keluhanPasien.getValue().trim());
		// diagnosaPenyakit.setDiagnosaAwal1((Icd) diagnosaAwal1
		// .getAttribute("icd"));
		// diagnosaPenyakit.setDiagnosaAkhir1((Icd) diagnosaAkhir1
		// .getAttribute("icd"));
		//
		// diagnosaPenyakit.setDiagnosaAwal2((Icd) diagnosaAwal2
		// .getAttribute("icd"));
		// diagnosaPenyakit.setDiagnosaAkhir2((Icd) diagnosaAkhir2
		// .getAttribute("icd"));
		//
		// diagnosaPenyakit.setDiagnosaAwal3((Icd) diagnosaAwal3
		// .getAttribute("icd"));
		// diagnosaPenyakit.setDiagnosaAkhir3((Icd) diagnosaAkhir3
		// .getAttribute("icd"));
		// diagnosaPenyakit.setLokasi(myLokasi);
		// diagnosaPenyakit.setApakahMenular((String) apakahMenular
		// .getSelectedItem().getValue());

		// diagnosaPenyakit.setJenisPenyakit((JenisPenyakit) jenisPenyakit
		// .getAttribute("jenisPenyakit"));
		diagnosaPenyakit.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		diagnosaPenyakit.setPasien(diagnosaPenyakit.getPendaftaran().getPasien());
		diagnosaPenyakit.setKeterangan(keterangan.getValue());
		diagnosaPenyakit.setKode(kode.getValue());
		diagnosaPenyakit.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		diagnosaPenyakit.setPasien(diagnosaPenyakit.getPendaftaran().getPasien());
		diagnosaPenyakit.setKeterangan(keterangan.getValue());
		// diagnosaPenyakit.setKeteranganhasillab(keteranganhasillab.getValue());
		// diagnosaPenyakit.setKeteranganhasilrontgen(keteranganhasilrontgen
		// .getValue());

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

		List<DiagnosaPenyakit> diagnosaPenyakit = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(diagnosaPenyakit);
		grid.setRowRenderer(new DiagnosaPenyakitRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	// private class ItemAction {
	//
	// private Grid gridItem;
	//
	// private DiagnosaPenyakit diagnosaPenyakit;
	//
	// public Borderlayout init(DiagnosaPenyakit diagnosaPenyakit) {
	// this.diagnosaPenyakit = diagnosaPenyakit;
	// return display();
	// }
	//
	// public Borderlayout display() {
	//
	// Borderlayout borderlayout = new Borderlayout();
	//
	// North north = new North();
	// ais.ui.util.ZkCompat.setFlex(north, true);
	// north.setParent(borderlayout);
	//
	// Toolbar toolbar = new Toolbar();
	// toolbar.setHeight("25px");
	// toolbar.setParent(north);
	// Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data
	// Item",
	// "/img/add_item.png");
	// button.addEventListener("onClick", new EventListener() {
	//
	// @SuppressWarnings("unchecked")
	// @Override
	// public void onEvent(Event event) throws Exception {
	//
	// final Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran
	// .getAttribute("pendaftaran");
	// if (myPendaftaran == null) {
	// Messagebox.show("Masukkan kode pendaftaran",
	// "Peringatan", Messagebox.OK,
	// Messagebox.EXCLAMATION);
	// return;
	// }
	//
	// if (!DiagnosaPenyakitRawatInapAction.this.onSave(event)) {
	// return;
	// }
	//
	// // Session session = HibernateUtil.currentSession();
	// //
	// // List<Item> items = session
	// // .createCriteria(ItemDiagnosaPenyakit.class)
	// // .setProjection(Projections.groupProperty("item"))
	// // .add(Restrictions.eq("diagnosaPenyakit",
	// // diagnosaPenyakit)).list();
	//
	// AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(
	// new ArrayList<Item>(), new JenisItem(
	// JenisItem.BAHAN_MEDIS));
	// ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
	// .getFirstRoot().appendChild(ambilDataItemBanyak);
	// ambilDataItemBanyak.setEventListener(new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	//
	// List<Item> items = (List<Item>) arg0.getData();
	//
	// save.setDisabled(items.size() == 0);
	//
	// ItemDiagnosaPenyakitDao itemDiagnosaPenyakitDao = DaoFactory
	// .getInstance().getItemDiagnosaPenyakitDao();
	// Session session = itemDiagnosaPenyakitDao
	// .getCurrentSession();
	// for (Item item : items) {
	//
	// KelasPerawatan kelasPerawatan = myPendaftaran
	// .getKelasPerawatan() == null ? ConstantValues.kelasNormal
	// : myPendaftaran.getKelasPerawatan();
	//
	// HargaJualItem hargaJualItem = (HargaJualItem) session
	// .createCriteria(HargaJualItem.class)
	// .add(Restrictions.eq("item", item))
	// .add(Restrictions.eq("kelasPerawatan",
	// kelasPerawatan))
	// .setMaxResults(1).uniqueResult();
	//
	// if (hargaJualItem == null) {
	// Messagebox.show(
	// "Biaya untuk item "
	// + item.getNama()
	// + " kelas "
	// + kelasPerawatan.getNama()
	// + " belum dimasukkan",
	// "Peringatan", Messagebox.OK,
	// Messagebox.EXCLAMATION);
	// continue;
	// }
	//
	// ItemDiagnosaPenyakit itemDiagnosaPenyakit = new ItemDiagnosaPenyakit();
	// itemDiagnosaPenyakit.setItem(item);
	// itemDiagnosaPenyakit.setJumlah(1.0);
	// itemDiagnosaPenyakit.setBiaya(hargaJualItem == null
	// || hargaJualItem.getHargaJual() == null ? 0.0
	// : hargaJualItem.getHargaJual());
	//
	// itemDiagnosaPenyakit.setKelasPerawatan(myPendaftaran
	// .getKelasPerawatan() == null ? ConstantValues.kelasNormal
	// : myPendaftaran.getKelasPerawatan());
	// itemDiagnosaPenyakit.setKeterangan("");
	// itemDiagnosaPenyakit
	// .setDiagnosaPenyakit(diagnosaPenyakit);
	// itemDiagnosaPenyakitDao
	// .save(itemDiagnosaPenyakit);
	//
	// }
	//
	// loadData(null);
	// }
	// });
	// ambilDataItemBanyak.setWidth("750px");
	// ambilDataItemBanyak.setHeight("97%");
	// ambilDataItemBanyak.setVisible(true);
	// ambilDataItemBanyak.onModal();
	// }
	//
	// });
	// button.setParent(toolbar);
	//
	// Center center = new Center();
	// center.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(center, true);
	//
	// gridItem = new Grid();
	// gridItem.setMold("paging");
	// gridItem.setPageSize(25);
	// gridItem.setParent(center);
	//
	// Columns columns = new Columns();
	//
	// columns.setParent(gridItem);
	//
	// Column column = new Column();
	// column.setParent(columns);
	// column.setLabel("Item");
	// column.setWidth("30%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Qty");
	// column.setAlign("right");
	// column.setWidth("10%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Waktu Mulai");
	// column.setWidth("15%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Waktu Sampai");
	// column.setWidth("15%");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("Keterangan");
	//
	// column = new Column();
	// column.setParent(columns);
	// column.setLabel("");
	// column.setWidth("10%");
	// loadData(null);
	// return borderlayout;
	// }
	//
	// class ItemDiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {
	//
	// @Override
	// public void render(final Row arg0, Object arg1) throws Exception {
	// // TODO Auto-generated method stub
	// final ItemDiagnosaPenyakit itemDiagnosaPenyakit = (ItemDiagnosaPenyakit)
	// arg1;
	// final Item item = itemDiagnosaPenyakit.getItem();
	//
	// new Label(item.getNama()).setParent(arg0);
	//
	// final MyDoublebox jumlah;
	// jumlah = new MyDoublebox(
	// itemDiagnosaPenyakit.getJumlah() == null ? 1.0
	// : itemDiagnosaPenyakit.getJumlah());
	// jumlah.setParent(arg0);
	// jumlah.setStyle("text-align:right");
	// jumlah.setWidth("90%");
	// jumlah.setWidth("90%");
	// jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil.currentSession();
	// itemDiagnosaPenyakit
	// .setJumlah(jumlah.getValue() == null ? 0.0
	// : jumlah.getValue());
	// Common.refreshUpdate(session, (itemDiagnosaPenyakit));
	//
	// }
	// });
	//
	// final MyDatebox mulai;
	// mulai = new MyDatebox(itemDiagnosaPenyakit.getMulai());
	// mulai.setParent(arg0);
	// mulai.setStyle("text-align:right");
	// mulai.setWidth("90%");
	// mulai.addEventListener(Events.ON_CHANGE, new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil.currentSession();
	// itemDiagnosaPenyakit.setMulai(mulai.getValue());
	// Common.refreshUpdate(session, (itemDiagnosaPenyakit));
	//
	// }
	// });
	//
	// final MyDatebox sampai;
	// sampai = new MyDatebox(itemDiagnosaPenyakit.getSampai());
	// sampai.setParent(arg0);
	// sampai.setStyle("text-align:right");
	// sampai.setWidth("90%");
	// sampai.addEventListener(Events.ON_CHANGE, new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil.currentSession();
	// itemDiagnosaPenyakit.setSampai(sampai.getValue());
	// Common.refreshUpdate(session, (itemDiagnosaPenyakit));
	//
	// }
	// });
	//
	// final MyTextbox keterangan = new MyTextbox(
	// itemDiagnosaPenyakit.getKeterangan() == null ? ""
	// : itemDiagnosaPenyakit.getKeterangan());
	// keterangan.setWidth("90%");
	// keterangan.setParent(arg0);
	//
	// keterangan.addEventListener(Events.ON_CHANGE,
	// new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Session session = HibernateUtil
	// .currentSession();
	// itemDiagnosaPenyakit.setKeterangan(keterangan
	// .getValue());
	// session.update(session
	// .merge(itemDiagnosaPenyakit));
	// }
	// });
	//
	// Hbox toolbar = new Hbox();
	// Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("",
	// "/img/delete.gif");
	// button.setTooltiptext("Hapus Data");
	// button.setVisible(delete);
	// button.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// Messagebox.show(
	// "Apakah yakin ingin menghapus data ini ?",
	// "Pertanyaan",
	// Messagebox.OK | Messagebox.CANCEL,
	// Messagebox.QUESTION, new EventListener() {
	//
	// @Override
	// public void onEvent(Event event)
	// throws Exception {
	// int i = new Integer(event.getData()
	// .toString());
	// if (i == Messagebox.OK) {
	// try {
	// ItemDiagnosaPenyakitDao itemDiagnosaPenyakitDao = DaoFactory
	// .getInstance()
	// .getItemDiagnosaPenyakitDao();
	//
	// itemDiagnosaPenyakitDao
	// .delete(itemDiagnosaPenyakitDao
	// .merge(itemDiagnosaPenyakit));
	// loadData(null);
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/DiagnosaPenyakitRawatInapAction.java:1750");
	// ais.common.Common.tampilErrorJikaAdmin(e);
	// Messagebox.show("Data ini tidak dapat dihapus .., karena berelasi dengan data
	// lainnya, error-nya adalah sbagai berikut:"
	// + e.getMessage());
	// }
	//
	// }
	//
	// }
	// });
	//
	// }
	// });
	// button.setParent(toolbar);
	// toolbar.setParent(arg0);
	// }
	//
	// }
	//
	// @SuppressWarnings("unchecked")
	// public void loadData(Event event) {
	// Session session = HibernateUtil.currentSession();
	// List<ItemDiagnosaPenyakit> itemDiagnosaPenyakits = diagnosaPenyakit ==
	// null
	// || diagnosaPenyakit.getId() == null ? new
	// ArrayList<ItemDiagnosaPenyakit>()
	// : session
	// .createCriteria(ItemDiagnosaPenyakit.class)
	// .addOrder(Order.desc("id"))
	// .add(Restrictions.eq("diagnosaPenyakit",
	// diagnosaPenyakit))
	// .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
	// .setFirstResult(
	// Common.ROWS_COUNT_ON_PAGE
	// * (paging == null ? 0 : paging
	// .getActivePage())).list();
	// ListModel strset = new SimpleListModel(itemDiagnosaPenyakits);
	// gridItem.setRowRenderer(new ItemDiagnosaPenyakitRenderer());
	// gridItem.setModel(strset);
	// gridItem.renderAll();
	//
	// }
	//
	// }

	/**
	 * Tipe implementasi bersarang {@link KunjunganDokterAction} milik {@link DiagnosaPenyakitRawatInapAction}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DiagnosaPenyakitRawatInapAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid gridDokter}, {@code
	 * DiagnosaPenyakit diagnosaPenyakit}; operasi lokal: {@code init()}, {@code display()}, {@code loadData}().
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DiagnosaPenyakitRawatInapAction
	 */
	private class KunjunganDokterAction {

		private Grid gridDokter;

		private DiagnosaPenyakit diagnosaPenyakit;

		public Borderlayout init(DiagnosaPenyakit diagnosaPenyakit) {
			this.diagnosaPenyakit = diagnosaPenyakit;
			return display();
		}

		public Borderlayout display() {

			Borderlayout borderlayout = new Borderlayout();

			North north = new North();
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(north);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Dokter", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					final Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
					if (myPendaftaran == null) {
						MyMessageboxConfig.show(
								"Mohon maaf, Kode Pendaftaran belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu data pendaftaran pasien sebelum mengambil data dokter.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (!DiagnosaPenyakitRawatInapAction.this.onSave(event)) {
						return;
					}

					List<Dokter> dokters = new ArrayList<Dokter>();

					AmbilDataDokterBanyak ambilDataDokterBanyak = new AmbilDataDokterBanyak(dokters);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataDokterBanyak);
					ambilDataDokterBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<Dokter> dokters = (List<Dokter>) arg0.getData();

							save.setDisabled(dokters.size() == 0);

							Session session = HibernateUtil.currentSession();
							for (Dokter dokter : dokters) {
								KunjunganDokter kunjunganDokter = new KunjunganDokter();
								kunjunganDokter.setDokter(dokter);
								kunjunganDokter.setTindakan(ConstantValues.KUNJUNGAN_RUTIN);
								kunjunganDokter.setWaktu(new Date());
								kunjunganDokter.setKode(Common.generateCode(KunjunganDokter.class, 10));
								kunjunganDokter.setKeterangan("");
								kunjunganDokter.setDiagnosaPenyakit(diagnosaPenyakit);
								session.save(kunjunganDokter);

								RawatInapCalculationProcessor.checkKunjunganDokter(kunjunganDokter);
							}

							loadData(null);
						}
					});
					ambilDataDokterBanyak.setWidth("750px");
					ambilDataDokterBanyak.setHeight("97%");
					ambilDataDokterBanyak.setVisible(true);
					ambilDataDokterBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			gridDokter = new Grid();
			gridDokter.setMold("paging");
			gridDokter.setPageSize(25);
			gridDokter.setParent(center);

			Columns columns = new Columns();

			columns.setParent(gridDokter);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Dokter");
			column.setWidth("30%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Jenis Kunjungan");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Waktu Kunjungan");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Keterangan");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");
			loadData(null);
			return borderlayout;
		}

		/**
		 * Renderer lokal untuk layar/komponen {@link KunjunganDokterAction}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KunjunganDokterAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see KunjunganDokterAction
		 */
		class KunjunganDokterRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				// TODO Auto-generated method stub
				final KunjunganDokter kunjunganDokter = (KunjunganDokter) arg1;
				final Dokter dokter = kunjunganDokter.getDokter();

				new Label(dokter.getNama()).setParent(arg0);

				final Combobox jenisKunjungan = new Combobox();
				jenisKunjungan.setParent(arg0);
				Common.insertCombo(jenisKunjungan, "nama", Tindakan.class,
						Restrictions.and(Restrictions.eq("jenisTindakan", ConstantValues.KUNJUNGAN_DOKTER),
								Restrictions.eq("aktif", true)));
				Common.selectComboItem(jenisKunjungan, kunjunganDokter.getTindakan());
				jenisKunjungan.setWidth("90%");
				jenisKunjungan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (jenisKunjungan.getSelectedItem() == null) {
							MyMessageboxConfig.show(
									"Mohon maaf, Jenis Kunjungan belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu jenis kunjungan dokter yang sesuai.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							Common.selectComboItem(jenisKunjungan, kunjunganDokter.getTindakan());
							return;
						}
						Session session = HibernateUtil.currentSession();
						kunjunganDokter.setTindakan((Tindakan) jenisKunjungan.getSelectedItem().getValue());
						session.update(kunjunganDokter);
						RawatInapCalculationProcessor.checkKunjunganDokter(kunjunganDokter);
					}
				});

				final MyDatebox jumlah;
				jumlah = new MyDatebox(kunjunganDokter.getWaktu());
				jumlah.setParent(arg0);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.setFormat(Common.dateFormat3.get().toPattern());
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						kunjunganDokter.setWaktu(jumlah.getValue());
						session.update(kunjunganDokter);
						RawatInapCalculationProcessor.checkKunjunganDokter(kunjunganDokter);
					}
				});

				final MyTextbox keterangan = new MyTextbox(
						kunjunganDokter.getKeterangan() == null ? "" : kunjunganDokter.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setParent(arg0);

				keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						kunjunganDokter.setKeterangan(keterangan.getValue());
						session.update(kunjunganDokter);
						RawatInapCalculationProcessor.checkKunjunganDokter(kunjunganDokter);
					}
				});

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(kunjunganDokter);
												loadData(null);
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
				});
				button.setParent(toolbar);
				toolbar.setParent(arg0);
			}

		}

		@SuppressWarnings("unchecked")
		public void loadData(Event event) {
			Session session = HibernateUtil.currentSession();
			List<KunjunganDokter> kunjunganDokters = diagnosaPenyakit == null || diagnosaPenyakit.getId() == null
					? new ArrayList<KunjunganDokter>()
					: session.createCriteria(KunjunganDokter.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
			ListModel strset = new SimpleListModel(kunjunganDokters);
			gridDokter.setRowRenderer(new KunjunganDokterRenderer());
			gridDokter.setModel(strset);
			gridDokter.renderAll();

		}

	}

}
