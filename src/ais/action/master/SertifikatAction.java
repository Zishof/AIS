package ais.action.master;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.HashMapGenerator;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.PendukungReport;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Sertifikat;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KknPunyaKomponenPenilaianKkn;
import ais.database.model.kkn.KomponenPenilaianKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.database.model.pkl.PklPunyaKomponenPenilaianPkl;
import ais.database.model.sekolah.KegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk sertifikat. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * ambilGambarPendukungSertifikat()}, {@code tampilkanButton()}, {@code reloadDataGambar()}, {@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaSertifikat()}); mutasi data ({@code onSave()});
 * pelaporan/ekspor ({@code generateReport()}, {@code generateReport()}, {@code generateReport()}, {@code
 * cetakSertifikat()}, {@code cetakSertifikat()}, {@code cetakFormPendafatranKegiatan()}); operasi domain lain
 * ({@code onAdd()}, {@code isiParameterGambarPendukungSertifikat()}, {@code
 * generateParameterFormulirKegiatan()}, {@code mapSertifikat()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class SertifikatAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Sertifikat sertifikat;
	private MyToolbarbuttonConfig add;
	protected LampiranLain lampiran;
	private East east;
	private MyGrid myGridGaleri;
	private Map<Long, LampiranLain> maps = new HashMap<Long, LampiranLain>();

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

	/**
	 * Renderer lokal untuk layar/komponen {@link SertifikatAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link SertifikatAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see SertifikatAction
	 */
	class SertifikatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Sertifikat sertifikat = (Sertifikat) arg1;

			RevisiHelper.createNewRevisi(Sertifikat.class, sertifikat, sertifikat.getNama()).setParent(arg0);
			new Label(sertifikat.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(sertifikat);
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
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Mohon diperhatikan, data yang telah dihapus tidak dapat dikembalikan. Silakan tekan OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(sertifikat);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu seluruh data lain yang terkait dengan data ini; (2) pastikan data tidak sedang digunakan; (3) ulangi kembali proses penghapusan.",
													e.getMessage()));
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
		init(new Sertifikat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	public static void generateReport(Component east, LampiranLain lainMahasiswa, Sertifikat sertifikat) {
		Map<String, Object> parameters = HashMapGenerator.getRandStringObject();
		List<LampiranLain> lampiranLains = new ArrayList<LampiranLain>();

		if (sertifikat != null && sertifikat.getId() != null) {
			lampiranLains = ambilGambarPendukungSertifikat(sertifikat);
			isiParameterGambarPendukungSertifikat(parameters, lampiranLains);
		}

		// KE-FIX (tab "Gambar Pendukung" hilang): overload lama membuang lampiranLains
		// yang sudah di-query di atas lalu memanggil overload Map 3-argumen (yang cuma
		// tahu 2 tab). Teruskan lampiranLains ke overload 4-argumen di bawah supaya tab
		// pratinjau galeri gambar pendukung bisa dibangun.
		generateReport(east, lainMahasiswa, parameters, lampiranLains,
				sertifikat == null ? null : sertifikat.getNama());
	}

	public static void generateReport(Component east, LampiranLain lainMahasiswa,
			final Map<String, Object> parameters) {
		// Dipertahankan untuk pemanggil lama (mis. proses cetak sertifikat ujian) yang
		// tidak punya daftar lampiran galeri -- tab "Gambar Pendukung" dilewati (null).
		generateReport(east, lainMahasiswa, parameters, null, null);
	}

	@SuppressWarnings("unchecked")
	private static List<LampiranLain> ambilGambarPendukungSertifikat(Sertifikat sertifikat) {
		List<LampiranLain> hasil = new ArrayList<LampiranLain>();
		Set<Long> sudahMasuk = new HashSet<Long>();
		if (sertifikat == null || sertifikat.getId() == null) {
			return hasil;
		}

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("ref", sertifikat.getId()))
					.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
			for (LampiranLain lampiranLain : lampiranLains) {
				if (lampiranLain != null && lampiranLain.getId() != null && sudahMasuk.add(lampiranLain.getId())) {
					hasil.add(lampiranLain);
				}
			}

			if (sertifikat.getNama() != null && !sertifikat.getNama().trim().isEmpty()) {
				List<PendukungReport> pendukungReports = streamingSession.createCriteria(PendukungReport.class)
						.addOrder(Order.asc("id"))
						.add(Restrictions.eq("nama", sertifikat.getNama().trim()))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
				for (PendukungReport pendukungReport : pendukungReports) {
					LampiranLain lampiranLain = LampiranLain.ambil(pendukungReport.getId(),
							PendukungReport.class.getName());
					if (lampiranLain != null && lampiranLain.getId() != null && sudahMasuk.add(lampiranLain.getId())) {
						hasil.add(lampiranLain);
					}
				}
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
		return hasil;
	}

	private static void isiParameterGambarPendukungSertifikat(Map<String, Object> parameters,
			List<LampiranLain> lampiranLains) {
		if (parameters == null || lampiranLains == null) {
			return;
		}
		int index = 0;
		for (LampiranLain pendukung : lampiranLains) {
			try {
				parameters.put("pendukung_" + index, pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
						: pendukung.ambilFile().getAbsolutePath());
				index++;
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:ambilGambarPendukungSertifikat");
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void generateReport(Component east, LampiranLain lainMahasiswa,
			final Map<String, Object> parameters, final List<LampiranLain> lampiranLains,
			final String namaSertifikat) {
		Common.clear(east);
		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {

				if (Common.getApakahAdmin()) {

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(east);

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					// KE-FIX (tab "Gambar Pendukung" hilang + responsivitas): Tabbox/Tabpanel
					// native ZK 5 kerap kolaps tinggi 0px di form besar seperti ini (lihat javadoc
					// MyButtonTabbox). Ganti seluruh tab di layar ini ke MyButtonTabbox (Div biasa,
					// tanpa bug kolaps) sekaligus tambahkan kembali tab pratinjau galeri gambar
					// pendukung yang sebelumnya hanya dibangun di sisi kiri form (myGridGaleri),
					// tidak pernah dijadikan tab pratinjau di sini.
					int[] tabAktif = { 0 };
					ais.ui.util.MyButtonTabbox mtabs = ais.ui.util.MyButtonTabbox.buat(center, "100%", tabAktif);

					org.zkoss.zul.Div panelTampilan = mtabs.tambahTab(0, "Tampilan Sertifikat");

					Borderlayout borderlayoutTampilan = new Borderlayout();
					borderlayoutTampilan.setHeight("100%");
					borderlayoutTampilan.setParent(panelTampilan);

					Center centerTampilan = new Center();
					centerTampilan.setParent(borderlayoutTampilan);
					ais.ui.util.ZkCompat.setFlex(centerTampilan, true);

					org.zkoss.zul.North north = new org.zkoss.zul.North();
					north.setParent(borderlayoutTampilan);
					north.appendChild(CommonReport.exportReport(new ParameterListener() {

						@SuppressWarnings({ "unchecked", "rawtypes" })
						@Override
						public Map generateParameters() throws Exception {
							return parameters;
						}
					}, "Gambar_Sertifikat", null, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, false));

					File file = Report.generateCompileFileReport(Report.PDF, parameters,
							lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					CommonReport.tampilkanReportPDF(centerTampilan, file);

					org.zkoss.zul.Div panelParameter = mtabs.tambahTab(1, "Parameter Sertifikat");

					Borderlayout borderlayoutParameter = new Borderlayout();
					borderlayoutParameter.setHeight("100%");
					borderlayoutParameter.setParent(panelParameter);

					Center centerParameter = new Center();
					centerParameter.setParent(borderlayoutParameter);
					ais.ui.util.ZkCompat.setFlex(centerParameter, true);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(centerParameter);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Key");
					column.setParent(columns);
					column.setWidth("40%");

					column = new MyColumnConfig("Nilai");
					column.setParent(columns);

					final Rows rowsCari = new Rows();
					rowsCari.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.setParent(rowsCari);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);

					final Textbox cari = new Textbox("");
					cari.setParent(hbox);
					cari.setCols(20);

					EventListener cariAkun = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.clear(rowsCari, null, 1);
							for (String key : parameters.keySet()) {
								Object val = parameters.get(key);
								if (cari.getValue().trim().isEmpty()
										|| (val != null
												&& val.toString().toLowerCase().contains(cari.getValue().trim()))
										|| key.toLowerCase().contains(cari.getValue().trim())) {
									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rowsCari);
									row.appendChild(new MyLabelKecil(key));
									row.appendChild(new MyLabelKecil(val == null ? "" : val.toString()));
								}
							}
						}
					};

					cari.addEventListener("onOK", cariAkun);

					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", cariAkun);

					try {
						cariAkun.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					org.zkoss.zul.Div panelGambar = mtabs.tambahTab(2, "Gambar Pendukung");
					panelGambar.setStyle("padding:0;overflow:auto;");
					if (namaSertifikat != null && !namaSertifikat.trim().isEmpty()) {
						MyInclude iframe = new MyInclude("/pages/master/pendukung_report.zul?namaStr="
								+ URLEncoder.encode(namaSertifikat.trim(), "UTF-8"));
						iframe.setWidth("100%");
						iframe.setHeight("100%");
						iframe.setParent(panelGambar);
					} else {
						new Label("Isi dan simpan Nama Sertifikat terlebih dahulu sebelum menambahkan gambar pendukung.")
								.setParent(panelGambar);
					}
					mtabs.pulihkanSeleksi(3);

				} else {

					File file = Report.generateCompileFileReport(Report.PDF, parameters,
							lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					CommonReport.tampilkanReportPDF(east, file);
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	@SuppressWarnings({ "deprecation" })
	private void init(final Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
		addWindow.setTitle(sertifikat.getId() == null ? "Tambah Sertifikat" : "Ubah Sertifikat");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		east = new East();
		east.setParent(borderlayout);
		east.setWidth("70%");

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sertifikat"));
		row.appendChild(nama = new Textbox(sertifikat.getNama() == null ? "" : sertifikat.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Sertifikat (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		ais.ui.util.MenuAksiBaris.pasang(hbox);
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, sertifikat.getId(), LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT,
				"File jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
						generateReport(east, lampiran, sertifikat);
					}
				}, null, false, false, false, true);

		Common.initKeterangan(rows, "Tambahkan parameter no_peserta dan nama_peserta pada file jrxml sertifikat");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(sertifikat.getKeterangan() == null ? "" : sertifikat.getKeterangan()));
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

		lampiran = LampiranLain.ambil(sertifikat.getId(), LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

		generateReport(east, lampiran, sertifikat);

	}

	private void tampilkanButton(final Hbox hboxGambar) {
		maps = new HashMap<Long, LampiranLain>();
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, sertifikat.getId(),
				"Galery_Sertifikat_" + Common.getGeneratedBarCode(), "Gambar Pendukung Sertifikat", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);

							}
						});
					}
				});
	}

	private void reloadDataGambar() throws Exception {
		Common.clear(myGridGaleri);

		Rows rows = new Rows();
		myGridGaleri.appendChild(rows);

		for (final LampiranLain lampiranLain : maps.values()) {

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class, false);
			MyFormRow roww = new MyFormRow();
			roww.setParent(rows);

			Vbox vbox = new Vbox();
			vbox.setParent(roww);

			Image image = new Image(link);
			image.setStyle("max-width: 256px !important;min-width: 60px !important;min-height: 300px !important;");
			image.setSclass("gambar_profile");
			image.setWidth("90%");
			image.setParent(vbox);

			A a = new A(link);
			a.setStyle("font-size:10px");
			a.setParent(vbox);
			a.setTarget("_blank");
			a.setHref(link);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(3);
			textbox.setParent(vbox);

			textbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setDeskripsi(textbox.getValue());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Mohon diperhatikan, data yang telah dihapus tidak dapat dikembalikan. Silakan tekan OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											LampiranLain d = maps.remove(lampiranLain.getId());
											System.out.println("d = > " + d);

											try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reloadDataGambar();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu seluruh data lain yang terkait dengan data ini; (2) pastikan data tidak sedang digunakan; (3) ulangi kembali proses penghapusan.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
			button.setParent(vbox);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl) throws Exception {
		try {

			if (mahasiswaDapatKelompokPkl != null && mahasiswaDapatKelompokPkl.getKelompokPkl() != null
					&& mahasiswaDapatKelompokPkl.getKelompokPkl().getSertifikat() != null) {

				Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokPkl.getDetailperkuliahan();

				KelompokPkl kelompokPkl = mahasiswaDapatKelompokPkl.getKelompokPkl();
				LampiranLain lampiran = LampiranLain.ambil(
						mahasiswaDapatKelompokPkl.getKelompokPkl().getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				Mahasiswa mahasiswa = mahasiswaDapatKelompokPkl.getMahasiswa();

				PerguruanTinggi perguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null
						? null
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();

				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

				try {
					if (mahasiswaDapatKelompokPkl.getKelompokPkl().getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref",
										mahasiswaDapatKelompokPkl.getKelompokPkl().getSertifikat().getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:702");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:712");
				}

				parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
				parameters.put("qr_code", BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, "").getAbsolutePath());

				parameters.put("nilai_huruf", detailperkuliahan == null ? null : detailperkuliahan.getNilaiHuruf());
				parameters.put("nilai_angka", detailperkuliahan == null ? null : detailperkuliahan.getTotalNilai());
				parameters.put("nilai_ip", detailperkuliahan == null ? null : detailperkuliahan.getTotalIP());

				parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektor());
				parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektorNip());

				parameters.put("alamat_kelompok", mahasiswaDapatKelompokPkl.getKelompokPkl().getAlamat());
				parameters.put("mulai", mahasiswaDapatKelompokPkl.getKelompokPkl().getTanggal_mulai());
				parameters.put("sampai", mahasiswaDapatKelompokPkl.getKelompokPkl().getTanggal_selesai());
				parameters.put("nama_kelompok", mahasiswaDapatKelompokPkl.getKelompokPkl().getNama());
				parameters.put("nama_pkl", mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getNama());
				parameters.put("tahun_akademik",
						mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getTahunAkademik());
				parameters.put("jenis_semester", mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getSemester());

				parameters.put("nama", mahasiswa.getNama());
				parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());
				parameters.put("nim", mahasiswa.getNim());
				parameters.put("jurusan", mahasiswa.getJurusan().getNama());
				parameters.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

				parameters.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				parameters.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				parameters.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				parameters.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				parameters.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				parameters.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				parameters.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				parameters.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				parameters.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				parameters.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				parameters.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				parameters.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				parameters.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				parameters.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

				parameters.put("nama_perguruan_tinggi",
						mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				parameters.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
				parameters.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
				parameters.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
				parameters.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

				parameters.put("tempatlahir", mahasiswa.getTempatlahir());
				parameters.put("tanggallahir", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
				parameters.put("tanggallahir_1", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));

				Common.insertProperty(KelompokPkl.class, kelompokPkl, parameters, "kelompokPkl");

				parameters.put("keterangan_text", mahasiswaDapatKelompokPkl.getKeterangan());
				parameters.put("hasil_text", mahasiswaDapatKelompokPkl.getHasil());
				parameters.put("detailNilai_text", mahasiswaDapatKelompokPkl.getDetailNilai());
				parameters.put("nilaiHuruf_text", mahasiswaDapatKelompokPkl.getNilaiHuruf());
				parameters.put("TotalNilai_number", mahasiswaDapatKelompokPkl.getTotalNilai());
				parameters.put("TotalIP_number", mahasiswaDapatKelompokPkl.getTotalIP());

				List<KomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls = HibernateUtil.currentSession()
						.createCriteria(PklPunyaKomponenPenilaianPkl.class)
						.setProjection(Projections.groupProperty("komponenPenilaianPkl"))
						.createAlias("komponenPenilaianPkl", "komponenPenilaianPkl")
						.add(Restrictions.or(Restrictions.isNull("komponenPenilaianPkl.aktif"),
								Restrictions.eq("komponenPenilaianPkl.aktif", true)))
						.add(Restrictions.eq("pkl", kelompokPkl.getPkl())).list();
				for (KomponenPenilaianPkl komponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {
					parameters.put("komponen_nilai_" + komponenPenilaianPkl.getNama(),
							mahasiswaDapatKelompokPkl.retreiveDetailNilai(komponenPenilaianPkl));
				}

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setTitle("Sertifikat " + mahasiswaDapatKelompokPkl.getKelompokPkl().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kelompokPkl.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn) throws Exception {
		try {

			if (mahasiswaDapatKelompokKkn != null && mahasiswaDapatKelompokKkn.getKelompokKkn() != null
					&& mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat() != null) {

				Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn.getDetailperkuliahan();

				LampiranLain lampiran = LampiranLain.ambil(
						mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				KelompokKkn kelompokKkn = mahasiswaDapatKelompokKkn.getKelompokKkn();

				Mahasiswa mahasiswa = mahasiswaDapatKelompokKkn.getMahasiswa();
				PerguruanTinggi perguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null
						? null
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();

				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

				try {
					if (mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref",
										mahasiswaDapatKelompokKkn.getKelompokKkn().getSertifikat().getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:883");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:893");
				}

				parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
				parameters.put("qr_code", BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, "").getAbsolutePath());
				parameters.put("nilai_huruf", detailperkuliahan == null ? null : detailperkuliahan.getNilaiHuruf());
				parameters.put("nilai_angka", detailperkuliahan == null ? null : detailperkuliahan.getTotalNilai());
				parameters.put("nilai_ip", detailperkuliahan == null ? null : detailperkuliahan.getTotalIP());

				parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektor());
				parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektorNip());

				parameters.put("alamat_kelompok", mahasiswaDapatKelompokKkn.getKelompokKkn().getAlamat());
				parameters.put("mulai", mahasiswaDapatKelompokKkn.getKelompokKkn().getTanggal_mulai());
				parameters.put("sampai", mahasiswaDapatKelompokKkn.getKelompokKkn().getTanggal_selesai());
				parameters.put("nama_kelompok", mahasiswaDapatKelompokKkn.getKelompokKkn().getNama());
				parameters.put("nama_kkn", mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getNama());
				parameters.put("tahun_akademik",
						mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getTahunAkademik());
				parameters.put("jenis_semester", mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getSemester());

				parameters.put("nama", mahasiswa.getNama());
				parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());
				parameters.put("nim", mahasiswa.getNim());
				parameters.put("jurusan", mahasiswa.getJurusan().getNama());
				parameters.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

				parameters.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				parameters.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				parameters.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				parameters.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				parameters.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				parameters.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				parameters.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				parameters.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				parameters.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				parameters.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				parameters.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				parameters.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				parameters.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				parameters.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

				parameters.put("nama_perguruan_tinggi",
						mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				parameters.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
				parameters.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
				parameters.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
				parameters.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

				parameters.put("tempatlahir", mahasiswa.getTempatlahir());
				parameters.put("tanggallahir", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
				parameters.put("tanggallahir_1", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));

				Common.insertProperty(KelompokKkn.class, kelompokKkn, parameters, "kelompokKkn");

				parameters.put("keterangan_text", mahasiswaDapatKelompokKkn.getKeterangan());
				parameters.put("hasil_text", mahasiswaDapatKelompokKkn.getHasil());
				parameters.put("detailNilai_text", mahasiswaDapatKelompokKkn.getDetailNilai());
				parameters.put("nilaiHuruf_text", mahasiswaDapatKelompokKkn.getNilaiHuruf());
				parameters.put("TotalNilai_number", mahasiswaDapatKelompokKkn.getTotalNilai());
				parameters.put("TotalIP_number", mahasiswaDapatKelompokKkn.getTotalIP());

				List<KomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns = HibernateUtil.currentSession()
						.createCriteria(KknPunyaKomponenPenilaianKkn.class)
						.setProjection(Projections.groupProperty("komponenPenilaianKkn"))
						.createAlias("komponenPenilaianKkn", "komponenPenilaianKkn")
						.add(Restrictions.or(Restrictions.isNull("komponenPenilaianKkn.aktif"),
								Restrictions.eq("komponenPenilaianKkn.aktif", true)))
						.add(Restrictions.eq("kkn", kelompokKkn.getKkn())).list();
				for (KomponenPenilaianKkn komponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {
					parameters.put("komponen_nilai_" + komponenPenilaianKkn.getNama(),
							mahasiswaDapatKelompokKkn.retreiveDetailNilai(komponenPenilaianKkn));
				}

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setTitle("Sertifikat " + mahasiswaDapatKelompokKkn.getKelompokKkn().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kelompokKkn.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> generateParameterFormulirKegiatan(
			FormulirKegiatanPeserta formulirKegiatanPeserta) {
		Mahasiswa mahasiswa = formulirKegiatanPeserta.getMahasiswa();
		Dosen dosen = formulirKegiatanPeserta.getDosen();
		PerguruanTinggi perguruanTinggi = null;
		// Peserta bisa Mahasiswa SAJA atau Dosen SAJA — getMahasiswa()/getDosen() bisa null.
		// Tanpa guard, dosen.getPerguruanTinggi() pada peserta mahasiswa melempar NullPointerException.
		try {
			if (mahasiswa != null && mahasiswa.getJurusan() != null
					&& mahasiswa.getJurusan().getFakultas() != null) {
				perguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			// Bila peserta adalah dosen, perguruan tinggi diambil dari dosen (menimpa bila ada).
			if (dosen != null && dosen.getPerguruanTinggi() != null) {
				perguruanTinggi = dosen.getPerguruanTinggi();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		FormulirKegiatan formulirKegiatan = formulirKegiatanPeserta.getFormulirKegiatan();

		Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

		try {
			if (formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat() != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id"))
						.add(Restrictions.eq("ref",
								formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat().getId()))
						.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
				int index = 0;
				for (LampiranLain pendukung : lampiranLains) {
					try {
						parameters.put("pendukung_" + index, pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
								: pendukung.ambilFile().getAbsolutePath());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1069");
						// TODO: handle exception
					}
					index++;
				}

				StreamingHibernateUtil.getInstance().closeSession();
			}
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1079");
		}

		// formulirKegiatan bisa null pada data peserta yang tidak lengkap -> jangan di-deref langsung.
		if (formulirKegiatan != null && formulirKegiatan.getId() != null) {
			LampiranLain ttd = LampiranLain.ambil(formulirKegiatan.getId(), LampiranLain.TTD_FORMULIR_KIRI);
			if (ttd != null && ttd.getId() != null) {
				try {
					parameters.put("ttd_formulir_kiri", ttd.ambilFile().getAbsolutePath());
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1089");
				}
			}
			ttd = LampiranLain.ambil(formulirKegiatan.getId(), LampiranLain.TTD_FORMULIR_KANAN);
			if (ttd != null && ttd.getId() != null) {
				try {
					parameters.put("ttd_formulir_kanan", ttd.ambilFile().getAbsolutePath());
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1097");
				}
			}
		}

		parameters.put("jenis_kegiatan", formulirKegiatan == null ? null : formulirKegiatan.getJenisKegiatan());
		parameters.put("nama_rektor",
				perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektor());
		parameters.put("nip_rektor",
				perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektorNip());
		parameters.put("nomor", formulirKegiatanPeserta.getKode());
		parameters.put("seminar", formulirKegiatan == null ? null : formulirKegiatan.getNama());
		parameters.put("mulai", formulirKegiatan == null ? null : formulirKegiatan.getTanggal());
		parameters.put("sampai", formulirKegiatan == null ? null : formulirKegiatan.getSampai());

		if (mahasiswa != null) {
			parameters.put("alamat", mahasiswa.getAlamat());
			parameters.put("telp", mahasiswa.getTelp());
			parameters.put("nama", mahasiswa.getNama());
			try {
				parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			parameters.put("qr_code", BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, "").getAbsolutePath());
			parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());
			parameters.put("tahunAngkatan", mahasiswa.getTahunangkatan());
			parameters.put("nim", mahasiswa.getNim());
			// Rantai jurusan/fakultas/jenjang bisa NULL pada data peserta tak lengkap → bungkus agar
			// tidak menggagalkan seluruh sertifikat (field organisasi dilewati bila datanya kosong).
			try {
			parameters.put("jurusan", mahasiswa.getJurusan().getNama());
			parameters.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
			parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
			parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
			parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
			parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

			parameters.put("nama_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getNama());
			parameters.put("nip_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getCode());
			parameters.put("nidn_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getNidn());

			parameters.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
			parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
			parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

			parameters.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
			parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
			parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

			parameters.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
			parameters.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
			parameters.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

			parameters.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
			parameters.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
			parameters.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

			parameters.put("nama_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
			parameters.put("nip_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
			parameters.put("nidn_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

			parameters.put("nama_perguruan_tinggi",
					mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
			parameters.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
			parameters.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
			parameters.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
			parameters.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());
			} catch (Exception eJurMhs) {
				ais.common.Common.tampilErrorJikaAdmin(eJurMhs);
			}

			parameters.put("tempatlahir", mahasiswa.getTempatlahir());
			parameters.put("tanggallahir",
					mahasiswa.getTanggallahir() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
			parameters.put("tanggallahir_1",
					mahasiswa.getTanggallahir() == null ? "" : Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));
		} else if (dosen != null) {
			parameters.put("alamat", dosen.getAlamat());
			parameters.put("telp", dosen.getTelp());
			try {
				parameters.put("foto_dsn", CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen)));
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			parameters.put("qr_code", BarcodeCommon.generateCrCodeDosen(dosen, "").getAbsolutePath());
			parameters.put("nama", dosen.getNama());
			parameters.put("nim", dosen.getNim());
			// Rantai jurusan/fakultas/jenjang bisa NULL pada data dosen tak lengkap → bungkus agar
			// tidak menggagalkan seluruh sertifikat (field organisasi dilewati bila datanya kosong).
			try {
			parameters.put("jurusan", dosen.getJurusan().getNama());
			parameters.put("id_fakultas", dosen.getFakultas().getId());
			parameters.put("fakultas_id", dosen.getFakultas().getId());
			parameters.put("fakultas", dosen.getFakultas().getNama());
			parameters.put("nama_fakultas", dosen.getFakultas().getNama());
			parameters.put("jenjang", dosen.getJurusan().getJenjang().getNama());

			parameters.put("nama_kaprodi",
					dosen.getJurusan().getKaprodi() == null ? "" : dosen.getJurusan().getKaprodi().getNama());
			parameters.put("nip_kaprodi",
					dosen.getJurusan().getKaprodi() == null ? "" : dosen.getJurusan().getKaprodi().getCode());
			parameters.put("nidn_kaprodi",
					dosen.getJurusan().getKaprodi() == null ? "" : dosen.getJurusan().getKaprodi().getNidn());

			parameters.put("nama_dekan",
					dosen.getFakultas().getDekan() == null ? "" : dosen.getFakultas().getDekan().getNama());
			parameters.put("nip_dekan",
					dosen.getFakultas().getDekan() == null ? "" : dosen.getFakultas().getDekan().getCode());
			parameters.put("nidn_dekan",
					dosen.getFakultas().getDekan() == null ? "" : dosen.getFakultas().getDekan().getNidn());

			parameters.put("nama_pudek1",
					dosen.getFakultas().getPudek1() == null ? "" : dosen.getFakultas().getPudek1().getNama());
			parameters.put("nip_pudek1",
					dosen.getFakultas().getPudek1() == null ? "" : dosen.getFakultas().getPudek1().getCode());
			parameters.put("nidn_pudek1",
					dosen.getFakultas().getPudek1() == null ? "" : dosen.getFakultas().getPudek1().getNidn());

			parameters.put("nama_pudek2",
					dosen.getFakultas().getPudek2() == null ? "" : dosen.getFakultas().getPudek2().getNama());
			parameters.put("nip_pudek2",
					dosen.getFakultas().getPudek2() == null ? "" : dosen.getFakultas().getPudek2().getCode());
			parameters.put("nidn_pudek2",
					dosen.getFakultas().getPudek2() == null ? "" : dosen.getFakultas().getPudek2().getNidn());

			parameters.put("nama_pudek3",
					dosen.getFakultas().getPudek3() == null ? "" : dosen.getFakultas().getPudek3().getNama());
			parameters.put("nip_pudek3",
					dosen.getFakultas().getPudek3() == null ? "" : dosen.getFakultas().getPudek3().getCode());
			parameters.put("nidn_pudek3",
					dosen.getFakultas().getPudek3() == null ? "" : dosen.getFakultas().getPudek3().getNidn());

			parameters.put("nama_kajur",
					dosen.getJurusan().getGrupJurusan() == null
							|| dosen.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: dosen.getJurusan().getGrupJurusan().getKajur().getNama());
			parameters.put("nip_kajur",
					dosen.getJurusan().getGrupJurusan() == null
							|| dosen.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: dosen.getJurusan().getGrupJurusan().getKajur().getCode());
			parameters.put("nidn_kajur",
					dosen.getJurusan().getGrupJurusan() == null
							|| dosen.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: dosen.getJurusan().getGrupJurusan().getKajur().getNidn());

			parameters.put("nama_perguruan_tinggi",
					dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getNama());
			parameters.put("alamat1",
					dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getAlamat1());
			parameters.put("alamat2",
					dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getAlamat2());
			parameters.put("telepon",
					dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getTelepon());
			parameters.put("faksimili",
					dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getFaksimili());
			} catch (Exception eJurDsn) {
				ais.common.Common.tampilErrorJikaAdmin(eJurDsn);
			}

			parameters.put("tempatlahir", dosen.getTempatlahir());
			parameters.put("tanggallahir",
					dosen.getTanggallahir() == null ? "" : Common.dateFormat2.get().format(dosen.getTanggallahir()));
			parameters.put("tanggallahir_1",
					dosen.getTanggallahir() == null ? "" : Common.dateFormat1.get().format(dosen.getTanggallahir()));
		}

		parameters.put("tahunAkademik", formulirKegiatan.getTahunAkademik());
		parameters.put("semester", formulirKegiatan.getSemester());

		Common.insertProperty(FormulirKegiatan.class, formulirKegiatan, parameters, "formulir");

		parameters.put("keterangan", formulirKegiatanPeserta.getKeterangan());
		parameters.put("nilai", formulirKegiatanPeserta.getNilai());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void cetakFormPendafatranKegiatan(FormulirKegiatan formulirKegiatan,
			List<FormulirKegiatanPeserta> formulirKegiatanPesertas) throws Exception {

		final List<String> warnings = new ArrayList<String>();

		try {
			List<Map> maps = new ArrayList<Map>();
			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getFormulirKegiatan() != null) {
					FormulirKegiatan myFormulirKegiatan = formulirKegiatanPeserta.getFormulirKegiatan();
					Mahasiswa mahasiswa = formulirKegiatanPeserta.getMahasiswa();
					if (mahasiswa != null && !myFormulirKegiatan.getKodeItemBiaya().trim().isEmpty()) {

						Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
						String semesterMulai = myFormulirKegiatan.getSemester();
						String ta = myFormulirKegiatan.getTahunAkademik();
						Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
						Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
								mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

						Session session = HibernateUtil.currentSession();
						for (String kode : myFormulirKegiatan.getKodeItemBiaya().trim().split(",")) {
							if (!kode.trim().isEmpty()) {

								String[] spl = StringUtils.split(kode.trim(), ":");
								String code = spl.length > 0 ? spl[0] : "";
								String tahunAngkatan = spl.length > 1 ? spl[1] : "";

								if (tahunAngkatan.trim().isEmpty() || (mahasiswa.getTahunangkatan() != null && mahasiswa
										.getTahunangkatan().toString().equalsIgnoreCase(tahunAngkatan.trim()))) {

									ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
											session.createCriteria(ItemBiaya.class)
													.add(Restrictions.eq("kode", code.trim())).setMaxResults(1),
											ItemBiaya.class);
									if (itemBiaya != null) {
										int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
												.createAlias("kegiatan", "kegiatan")
												.add(myFormulirKegiatan.getSekaliBayar()
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("kegiatan.semster", smt))
												.add(Restrictions.eq("itemBiaya", itemBiaya))
												.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
												.setProjection(Projections.rowCount()).uniqueResult()).intValue();
										if (jumlah == 0) {
											warnings.add("Mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama "
													+ mahasiswa.getNama() + " belum membayar biaya "
													+ itemBiaya.getKode() + "-" + itemBiaya.getNama()
													+ (myFormulirKegiatan.getSekaliBayar() ? "" : " di semester " + smt)
													+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.");
											continue;
										}
									}
								}
							}
						}

					}

					Map<String, Object> parameters = generateParameterFormulirKegiatan(formulirKegiatanPeserta);
					maps.add(parameters);
				}
			}

			MyWindow prosesUjianHelper = new MyWindow();
			prosesUjianHelper.setTitle("Form Pendaftaran Kegiatan");
			prosesUjianHelper.setClosable(true);
			prosesUjianHelper.setHeight("95%");
			prosesUjianHelper.setWidth("90%");
			prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(prosesUjianHelper);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			Map parameters = ais.common.HashMapGenerator.getRand();

			try {
				if (formulirKegiatan.getSertifikat() != null) {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id"))
							.add(Restrictions.eq("ref", formulirKegiatan.getSertifikat().getId()))
							.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
					int index = 0;
					for (LampiranLain pendukung : lampiranLains) {
						try {
							parameters.put("pendukung_" + index,
									pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
											: pendukung.ambilFile().getAbsolutePath());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1400");
							// TODO: handle exception
						}
						index++;
					}

					StreamingHibernateUtil.getInstance().closeSession();
				}

			} catch (Exception e1) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1411");
			}

			parameters.put("nama_universitas", perguruanTinggi == null ? "" : perguruanTinggi.getNama());

			parameters.put("jenis_kegiatan", formulirKegiatan.getJenisKegiatan());
			parameters.put("nama_rektor",
					perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektor());
			parameters.put("nip_rektor",
					perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektorNip());
			parameters.put("seminar", formulirKegiatan.getNama());
			parameters.put("mulai", formulirKegiatan.getTanggal());
			parameters.put("sampai", formulirKegiatan.getSampai());

			Common.insertProperty(FormulirKegiatan.class, formulirKegiatan, parameters, "formulir");

			parameters.put("maps", maps);
			Report.generatePDFReport("pdf", parameters, "form_pendaftaran_kegiatan", ais.ui.util.WaktuUtil.getDate(),
					Common.locale, null, center);
			prosesUjianHelper.onModal();

			if (!warnings.isEmpty()) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String w = "";
						for (String s : warnings) {
							w += w.isEmpty() ? s : "\n\n" + s;
						}
						MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				});
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void cetakSertifikat(FormulirKegiatanPeserta formulirKegiatanPeserta) throws Exception {
		try {

			if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getFormulirKegiatan() != null
					&& formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat() != null) {

				LampiranLain lampiran = LampiranLain.ambil(
						formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);
				Map<String, Object> parameters = generateParameterFormulirKegiatan(formulirKegiatanPeserta);
				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setTitle("Sertifikat Kegiatan");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(formulirKegiatanPeserta.getFormulirKegiatan().getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) throws Exception {
		try {

			if (kegiatanKedosenanPunyaDosen != null && kegiatanKedosenanPunyaDosen.getKegiatanKedosenan() != null
					&& kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null) {

				KegiatanKedosenan kegiatanKedosenan = kegiatanKedosenanPunyaDosen.getKegiatanKedosenan();

				LampiranLain lampiran = LampiranLain.ambil(kegiatanKedosenan.getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				Dosen dosen = kegiatanKedosenanPunyaDosen.getDosen();

				Jurusan jurusan = kegiatanKedosenan.getJurusan() == null ? dosen.getJurusan()
						: kegiatanKedosenan.getJurusan();
				Fakultas fakultas = kegiatanKedosenan.getFakultas() == null ? dosen.getFakultas()
						: kegiatanKedosenan.getFakultas();

				PerguruanTinggi perguruanTinggi = fakultas == null || fakultas.getPerguruanTinggi() == null ? null
						: fakultas.getPerguruanTinggi();

				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

				try {
					if (kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref",
										kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat().getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1513");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1523");
				}

				parameters.put("foto_dsn", CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen)));
				parameters.put("qr_code", BarcodeCommon.generateCrCodeDosen(dosen, "").getAbsolutePath());
				parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektor());
				parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektorNip());
				parameters.put("nomor", kegiatanKedosenan.getKode());
				parameters.put("nama_kegiatan", kegiatanKedosenan.getNama());
				parameters.put("mulai", kegiatanKedosenan.getMulai());
				parameters.put("sampai", kegiatanKedosenan.getSampai());
				parameters.put("tempat", kegiatanKedosenan.getTempat());
				parameters.put("tahun_akademik", kegiatanKedosenan.getTahunAkademik());
				parameters.put("semester", kegiatanKedosenan.getJenisSemester());
				parameters.put("kelompok", kegiatanKedosenan.getKelompokKegiatanKedosenan() == null ? ""
						: kegiatanKedosenan.getKelompokKegiatanKedosenan().getNama());
				parameters.put("detail", kegiatanKedosenan.getDetailKelompokKegiatanKedosenan() == null ? ""
						: kegiatanKedosenan.getDetailKelompokKegiatanKedosenan().getNama());
				parameters.put("skala_di_kegiatan", kegiatanKedosenan.getSkalaKegiatanKedosenan() == null ? ""
						: kegiatanKedosenan.getSkalaKegiatanKedosenan().getNama());
				parameters.put("jabatan_di_kegiatan", kegiatanKedosenan.getJabatanKegiatanKedosenan() == null ? ""
						: kegiatanKedosenan.getJabatanKegiatanKedosenan().getNama());

				parameters.put("jabatan", kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan() == null ? ""
						: kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan().getNama());

				parameters.put("skala", kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan() == null ? ""
						: kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan().getNama());

				parameters.put("keterangan_di_kegiatan", kegiatanKedosenan.getKeterangan());
				parameters.put("keterangan", kegiatanKedosenanPunyaDosen.getKeterangan());

				parameters.put("nama", dosen.getNama());
				parameters.put("nidn", dosen.getNidn());
				parameters.put("code", dosen.getMycode());
				parameters.put("nip", dosen.getCode());
				parameters.put("jurusan", jurusan == null ? "" : jurusan.getNama());
				parameters.put("id_fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
				parameters.put("fakultas_id", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
				parameters.put("fakultas", fakultas == null ? "" : fakultas.getNama());
				parameters.put("nama_fakultas", fakultas == null ? "" : fakultas.getNama());
				parameters.put("jenjang", jurusan == null ? "" : jurusan.getJenjang().getNama());

				parameters.put("nama_kaprodi",
						jurusan == null ? "" : jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNama());
				parameters.put("nip_kaprodi",
						jurusan == null ? "" : jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getCode());
				parameters.put("nidn_kaprodi",
						jurusan == null ? "" : jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNidn());

				parameters.put("nama_dekan",
						fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getNama());
				parameters.put("nip_dekan",
						fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getCode());
				parameters.put("nidn_dekan",
						fakultas == null || fakultas.getDekan() == null ? "" : fakultas.getDekan().getNidn());

				parameters.put("nama_pudek1",
						fakultas == null || fakultas.getPudek1() == null ? "" : fakultas.getPudek1().getNama());
				parameters.put("nip_pudek1",
						fakultas == null || fakultas.getPudek1() == null ? "" : fakultas.getPudek1().getCode());
				parameters.put("nidn_pudek1",
						fakultas == null || fakultas.getPudek1() == null ? "" : fakultas.getPudek1().getNidn());

				parameters.put("nama_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getNama());
				parameters.put("nip_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getCode());
				parameters.put("nidn_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getNidn());

				parameters.put("nama_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getNama());
				parameters.put("nip_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getCode());
				parameters.put("nidn_pudek2",
						fakultas == null || fakultas.getPudek2() == null ? "" : fakultas.getPudek2().getNidn());

				parameters.put("nama_kajur",
						jurusan == null || jurusan.getGrupJurusan() == null
								|| jurusan.getGrupJurusan().getKajur() == null ? ""
										: jurusan.getGrupJurusan().getKajur().getNama());
				parameters.put("nip_kajur",
						jurusan == null || jurusan.getGrupJurusan() == null
								|| jurusan.getGrupJurusan().getKajur() == null ? ""
										: jurusan.getGrupJurusan().getKajur().getCode());
				parameters.put("nidn_kajur",
						jurusan == null || jurusan.getGrupJurusan() == null
								|| jurusan.getGrupJurusan().getKajur() == null ? ""
										: jurusan.getGrupJurusan().getKajur().getNidn());

				parameters.put("nama_perguruan_tinggi",
						perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getNama());
				parameters.put("alamat1",
						perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getAlamat1());
				parameters.put("alamat2",
						perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getAlamat2());
				parameters.put("telepon",
						perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getTelepon());
				parameters.put("faksimili", perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
						: perguruanTinggi.getFaksimili());

				parameters.put("tempatlahir", dosen.getTempatlahir());
				parameters.put("tanggallahir",
						dosen.getTanggallahir() == null ? "" : Common.dateFormat2.get().format(dosen.getTanggallahir()));
				parameters.put("tanggallahir_1",
						dosen.getTanggallahir() == null ? "" : Common.dateFormat1.get().format(dosen.getTanggallahir()));

				Common.insertProperty(KegiatanKedosenan.class, kegiatanKedosenan, parameters, "kegiatanKedosenan");

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper
						.setTitle("Sertifikat " + kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kegiatanKedosenan.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa)
			throws Exception {
		try {

			if (kegiatanKemahasiswaanPunyaMahasiswa != null
					&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan() != null
					&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat() != null) {

				KegiatanKemahasiswaan kegiatanKemahasiswaan = kegiatanKemahasiswaanPunyaMahasiswa
						.getKegiatanKemahasiswaan();

				LampiranLain lampiran = LampiranLain.ambil(kegiatanKemahasiswaan.getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				Mahasiswa mahasiswa = kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa();
				PerguruanTinggi perguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null
						? null
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();

				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

				FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) HibernateUtil
						.currentSession().createCriteria(FormulirKegiatanPeserta.class)
						.createAlias("formulirKegiatan", "formulirKegiatan")
						.add(Restrictions.eq("formulirKegiatan.kegiatanKemahasiswaan", kegiatanKemahasiswaan))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				if (formulirKegiatanPeserta != null) {
					Map<String, Object> parametersbaru = generateParameterFormulirKegiatan(formulirKegiatanPeserta);
					parameters.putAll(parametersbaru);
				}

				try {
					if (kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref",
										kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat()
												.getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1697");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1707");
				}

				parameters.put("qr_code", BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, "").getAbsolutePath());
				parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
				parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektor());
				parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektorNip());
				parameters.put("nomor", kegiatanKemahasiswaan.getKode());
				parameters.put("nama_kegiatan", kegiatanKemahasiswaan.getNama());
				parameters.put("mulai", kegiatanKemahasiswaan.getMulai());
				parameters.put("sampai", kegiatanKemahasiswaan.getSampai());
				parameters.put("tempat", kegiatanKemahasiswaan.getTempat());
				parameters.put("tahun_akademik", kegiatanKemahasiswaan.getTahunAkademik());
				parameters.put("semester", kegiatanKemahasiswaan.getJenisSemester());
				parameters.put("kelompok", kegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan().getNama());
				parameters.put("detail", kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan().getNama());
				parameters.put("skala_di_kegiatan", kegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaan().getNama());
				parameters.put("jabatan_di_kegiatan",
						kegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaan() == null ? ""
								: kegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaan().getNama());

				parameters.put("jabatan",
						kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan() == null ? ""
								: kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan().getNama());

				parameters.put("skala", kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan().getNama());

				parameters.put("pembina_1", kegiatanKemahasiswaan.getDosenPembina1() == null ? ""
						: kegiatanKemahasiswaan.getDosenPembina1().getNama());

				parameters.put("pembina_2", kegiatanKemahasiswaan.getDosenPembina2() == null ? ""
						: kegiatanKemahasiswaan.getDosenPembina2().getNama());

				parameters.put("keterangan_di_kegiatan", kegiatanKemahasiswaan.getKeterangan());
				parameters.put("keterangan", kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan());

				parameters.put("nama", mahasiswa.getNama());
				parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());
				parameters.put("nim", mahasiswa.getNim());
				parameters.put("jurusan", mahasiswa.getJurusan().getNama());
				parameters.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

				parameters.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				parameters.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				parameters.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				parameters.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				parameters.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				parameters.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				parameters.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				parameters.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				parameters.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				parameters.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				parameters.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				parameters.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				parameters.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				parameters.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

				parameters.put("nama_perguruan_tinggi",
						mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				parameters.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
				parameters.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
				parameters.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
				parameters.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

				parameters.put("tempatlahir", mahasiswa.getTempatlahir());
				parameters.put("tanggallahir", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
				parameters.put("tanggallahir_1", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));

				Common.insertProperty(KegiatanKemahasiswaan.class, kegiatanKemahasiswaan, parameters,
						"kegiatanKemahasiswaan");

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setTitle(
						"Sertifikat " + kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kegiatanKemahasiswaan.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa) throws Exception {
		try {

			if (kegiatanKesiswaanPunyaSiswa != null && kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan() != null
					&& kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat() != null) {

				KegiatanKesiswaan kegiatanKesiswaan = kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan();

				LampiranLain lampiran = LampiranLain.ambil(kegiatanKesiswaan.getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				Siswa siswa = kegiatanKesiswaanPunyaSiswa.getSiswa();
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

				FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) HibernateUtil
						.currentSession().createCriteria(FormulirKegiatanPeserta.class)
						.createAlias("formulirKegiatan", "formulirKegiatan")
						.add(Restrictions.eq("formulirKegiatan.kegiatanKesiswaan", kegiatanKesiswaan))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				if (formulirKegiatanPeserta != null) {
					Map<String, Object> parametersbaru = generateParameterFormulirKegiatan(formulirKegiatanPeserta);
					parameters.putAll(parametersbaru);
				}

				try {
					if (kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref",
										kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat().getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1884");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:1894");
				}

				parameters.put("qr_code", BarcodeCommon.generateCrCodeSiswa(siswa, "").getAbsolutePath());
				parameters.put("foto_siswa", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
				parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektor());
				parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
						: perguruanTinggi.getRektorNip());
				parameters.put("nomor", kegiatanKesiswaan.getKode());
				parameters.put("nama_kegiatan", kegiatanKesiswaan.getNama());
				parameters.put("mulai", kegiatanKesiswaan.getMulai());
				parameters.put("sampai", kegiatanKesiswaan.getSampai());
				parameters.put("tempat", kegiatanKesiswaan.getTempat());
				parameters.put("tahun_akademik", kegiatanKesiswaan.getTahunAkademik());
				parameters.put("semester", kegiatanKesiswaan.getJenisSemester());
				parameters.put("kelompok", kegiatanKesiswaan.getKelompokKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaan.getKelompokKegiatanKesiswaan().getNama());
				parameters.put("detail", kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan().getNama());
				parameters.put("skala_di_kegiatan", kegiatanKesiswaan.getSkalaKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaan.getSkalaKegiatanKesiswaan().getNama());
				parameters.put("jabatan_di_kegiatan", kegiatanKesiswaan.getJabatanKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaan.getJabatanKegiatanKesiswaan().getNama());

				parameters.put("jabatan", kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan().getNama());

				parameters.put("skala", kegiatanKesiswaanPunyaSiswa.getSkalaKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaanPunyaSiswa.getSkalaKegiatanKesiswaan().getNama());

				parameters.put("pembina_1", kegiatanKesiswaan.getGuruPembina1() == null ? ""
						: kegiatanKesiswaan.getGuruPembina1().getNama());

				parameters.put("pembina_2", kegiatanKesiswaan.getGuruPembina2() == null ? ""
						: kegiatanKesiswaan.getGuruPembina2().getNama());

				parameters.put("keterangan_di_kegiatan", kegiatanKesiswaan.getKeterangan());
				parameters.put("keterangan", kegiatanKesiswaanPunyaSiswa.getKeterangan());

				parameters.put("nama", siswa.getNama());
				parameters.put("tahunangkatan", siswa.getTahunMasuk());
				parameters.put("nim", siswa.getNim());
				parameters.put("sekolah", siswa.getSekolah().getNama());
				parameters.put("id_yayasan", siswa.getSekolah().getYayasan().getId());
				parameters.put("yayasan_id", siswa.getSekolah().getYayasan().getId());
				parameters.put("yayasan", siswa.getSekolah().getYayasan().getNama());
				parameters.put("nama_yayasan", siswa.getSekolah().getYayasan().getNama());
				parameters.put("jenjang", siswa.getSekolah().getJenisSekolah().getNama());

				parameters.put("NamaKepalaSekolah", siswa.getSekolah().getNamaKepalaSekolah());
				parameters.put("nip_kaprodi", siswa.getSekolah().getNipKepalaSekolah());

				parameters.put("nama_perguruan_tinggi", perguruanTinggi == null ? "" : perguruanTinggi.getNama());
				parameters.put("alamat1", perguruanTinggi == null ? "" : perguruanTinggi.getAlamat1());
				parameters.put("alamat2", perguruanTinggi == null ? "" : perguruanTinggi.getAlamat2());
				parameters.put("telepon", perguruanTinggi == null ? "" : perguruanTinggi.getTelepon());
				parameters.put("faksimili", perguruanTinggi == null ? "" : perguruanTinggi.getFaksimili());

				parameters.put("tempatlahir", siswa.getTempatLahir());
				parameters.put("tanggallahir",
						siswa.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(siswa.getTanggalLahir()));
				parameters.put("tanggallahir_1",
						siswa.getTanggalLahir() == null ? "" : Common.dateFormat1.get().format(siswa.getTanggalLahir()));

				Common.insertProperty(KegiatanKesiswaan.class, kegiatanKesiswaan, parameters, "kegiatanKesiswaan");

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper
						.setTitle("Sertifikat " + kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kegiatanKesiswaan.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> mapSertifikat(KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa) throws Exception {
		KelasLesSiswa kelasLesSiswa = kelasLesSiswaPunyaSiswa.getKelasLesSiswa();

		Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Map<String, Object> parameters = HashMapGenerator.getRandStringObject();

		try {
			if (kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getSertifikat() != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id"))
						.add(Restrictions.eq("ref", kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getSertifikat().getId()))
						.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
				int index = 0;
				for (LampiranLain pendukung : lampiranLains) {
					try {
						parameters.put("pendukung_" + index, pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
								: pendukung.ambilFile().getAbsolutePath());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:1997");
						// TODO: handle exception
					}
					index++;
				}

				StreamingHibernateUtil.getInstance().closeSession();
			}
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:2007");
		}

		parameters.put("qr_code", BarcodeCommon.generateCrCodeSiswa(siswa, "").getAbsolutePath());
		parameters.put("foto_siswa", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
		parameters.put("nama_rektor",
				perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektor());
		parameters.put("nip_rektor",
				perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi.getRektorNip());
		parameters.put("nomor", kelasLesSiswa.getKode());
		parameters.put("nama_kegiatan", kelasLesSiswa.getNama());

		parameters.put("pembina",
				kelasLesSiswa.getGuruPembina() == null ? "" : kelasLesSiswa.getGuruPembina().getNama());

		parameters.put("keterangan_di_kegiatan", kelasLesSiswa.getKeterangan());
		parameters.put("keterangan", kelasLesSiswaPunyaSiswa.getKeterangan());

		parameters.put("nama", siswa.getNama());
		parameters.put("tahunangkatan", siswa.getTahunMasuk());
		parameters.put("nim", siswa.getNim());
		parameters.put("sekolah", siswa.getSekolah().getNama());
		parameters.put("id_yayasan", siswa.getSekolah().getYayasan().getId());
		parameters.put("yayasan_id", siswa.getSekolah().getYayasan().getId());
		parameters.put("yayasan", siswa.getSekolah().getYayasan().getNama());
		parameters.put("nama_yayasan", siswa.getSekolah().getYayasan().getNama());
		parameters.put("jenjang", siswa.getSekolah().getJenisSekolah().getNama());

		parameters.put("NamaKepalaSekolah", siswa.getSekolah().getNamaKepalaSekolah());
		parameters.put("nip_kaprodi", siswa.getSekolah().getNipKepalaSekolah());

		parameters.put("nama_perguruan_tinggi", perguruanTinggi == null ? "" : perguruanTinggi.getNama());
		parameters.put("alamat1", perguruanTinggi == null ? "" : perguruanTinggi.getAlamat1());
		parameters.put("alamat2", perguruanTinggi == null ? "" : perguruanTinggi.getAlamat2());
		parameters.put("telepon", perguruanTinggi == null ? "" : perguruanTinggi.getTelepon());
		parameters.put("faksimili", perguruanTinggi == null ? "" : perguruanTinggi.getFaksimili());

		parameters.put("tempatlahir", siswa.getTempatLahir());
		parameters.put("tanggallahir",
				siswa.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(siswa.getTanggalLahir()));
		parameters.put("tanggallahir_1",
				siswa.getTanggalLahir() == null ? "" : Common.dateFormat1.get().format(siswa.getTanggalLahir()));

		Common.insertProperty(KelasLesSiswaPunyaSiswa.class, kelasLesSiswaPunyaSiswa, parameters, "", 2);
		return parameters;
	}

	public static void cetakSertifikat(KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa) throws Exception {
		try {

			if (kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getKelasLesSiswa() != null
					&& kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getSertifikat() != null) {

				Map<String, Object> parameters = SertifikatAction.mapSertifikat(kelasLesSiswaPunyaSiswa);

				KelasLesSiswa kelasLesSiswa = kelasLesSiswaPunyaSiswa.getKelasLesSiswa();
				LampiranLain lampiran = LampiranLain.ambil(kelasLesSiswa.getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);
				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setTitle("Sertifikat " + kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getNama());
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters,
						ambilGambarPendukungSertifikat(kelasLesSiswa.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void cetakSertifikat(HasilUjianMahasiswa hasilUjianMahasiswa) throws Exception {
		try {
			Ujian ujian = hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian();
			if (ujian != null && ujian.getSertifikat() != null) {
				Map<String, Object> parameters = HashMapGenerator.getRandStringObject();
				LampiranLain lampiran = LampiranLain.ambil(ujian.getSertifikat().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

				try {
					if (ujian.getSertifikat() != null) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id")).add(Restrictions.eq("ref", ujian.getSertifikat().getId()))
								.add(Restrictions.ilike("jenis", "Galery_Sertifikat_", MatchMode.START)).list();
						int index = 0;
						for (LampiranLain pendukung : lampiranLains) {
							try {
								parameters.put("pendukung_" + index,
										pendukung.getGdrive() != null ? pendukung.exportGDriveUrl()
												: pendukung.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SertifikatAction.java:2100");
								// TODO: handle exception
							}
							index++;
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/SertifikatAction.java:2110");
				}

				Mahasiswa mahasiswa = hasilUjianMahasiswa.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = hasilUjianMahasiswa.getBiodataCalonMahasiswa();
				if (mahasiswa != null) {
					PerguruanTinggi perguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null
							? PerguruanTinggiUtil.getPerguruanTinggi()
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();

					parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
							: perguruanTinggi.getRektor());
					parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
							: perguruanTinggi.getRektorNip());
					parameters.put("nomor",
							hasilUjianMahasiswa.getPertemuanPunyaUjian() == null
									|| hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian() == null ? ""
											: hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().getKode());
					parameters.put("no_peserta",
							hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
									? hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNoRegistrasi()
									: hasilUjianMahasiswa.getMahasiswa() != null
											? hasilUjianMahasiswa.getMahasiswa().getNim()
											: "");

					parameters.put("nama_peserta",
							hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
									? hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNama()
									: hasilUjianMahasiswa.getMahasiswa() != null
											? hasilUjianMahasiswa.getMahasiswa().getNama()
											: "");
					parameters.put("qr_code", BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, "").getAbsolutePath());
					parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
					parameters.put("nama", mahasiswa.getNama());
					parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());
					parameters.put("nim", mahasiswa.getNim());
					parameters.put("jurusan", mahasiswa.getJurusan().getNama());
					parameters.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
					parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
					parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
					parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
					parameters.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

					parameters.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getNama());
					parameters.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getCode());
					parameters.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getNidn());

					parameters.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
					parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
					parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

					parameters.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
					parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
					parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

					parameters.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
					parameters.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
					parameters.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

					parameters.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
					parameters.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
					parameters.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

					parameters.put("nama_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
					parameters.put("nip_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
					parameters.put("nidn_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

					parameters.put("nama_perguruan_tinggi",
							mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
					parameters.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
					parameters.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
					parameters.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
					parameters.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

					parameters.put("tempatlahir", mahasiswa.getTempatlahir());
					parameters.put("tanggallahir", mahasiswa.getTanggallahir() == null ? ""
							: Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
					parameters.put("tanggallahir_1", mahasiswa.getTanggallahir() == null ? ""
							: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));
				} else if (biodataCalonMahasiswa != null) {

					Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
					if (jurusan == null) {
						jurusan = biodataCalonMahasiswa.getProdi1();
					}
					PerguruanTinggi perguruanTinggi = jurusan.getFakultas().getPerguruanTinggi() == null
							? PerguruanTinggiUtil.getPerguruanTinggi()
							: jurusan.getFakultas().getPerguruanTinggi();

					parameters.put("nama_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
							: perguruanTinggi.getRektor());
					parameters.put("nip_rektor", perguruanTinggi == null || perguruanTinggi.getId() == null ? null
							: perguruanTinggi.getRektorNip());
					parameters.put("nomor",
							hasilUjianMahasiswa.getPertemuanPunyaUjian() == null
									|| hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian() == null ? ""
											: hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().getKode());
					parameters.put("no_peserta",
							hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
									? hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNoRegistrasi()
									: hasilUjianMahasiswa.getMahasiswa() != null
											? hasilUjianMahasiswa.getMahasiswa().getNim()
											: "");

					parameters.put("nama_peserta",
							hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
									? hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNama()
									: hasilUjianMahasiswa.getMahasiswa() != null
											? hasilUjianMahasiswa.getMahasiswa().getNama()
											: "");
					parameters.put("qr_code",
							BarcodeCommon.generateCrCodeMahasiswa(biodataCalonMahasiswa, "").getAbsolutePath());
					parameters.put("foto_mhs", CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
					parameters.put("nama", biodataCalonMahasiswa.getNama());
					parameters.put("tahunangkatan", biodataCalonMahasiswa.getTahun());
					parameters.put("nim", biodataCalonMahasiswa.getNoRegistrasi());
					parameters.put("jurusan", jurusan.getNama());
					parameters.put("id_fakultas", jurusan.getFakultas().getId());
					parameters.put("fakultas_id", jurusan.getFakultas().getId());
					parameters.put("fakultas", jurusan.getFakultas().getNama());
					parameters.put("nama_fakultas", jurusan.getFakultas().getNama());
					parameters.put("jenjang", jurusan.getJenjang().getNama());

					parameters.put("nama_kaprodi", jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNama());
					parameters.put("nip_kaprodi", jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getCode());
					parameters.put("nidn_kaprodi", jurusan.getKaprodi() == null ? "" : jurusan.getKaprodi().getNidn());

					parameters.put("nama_dekan",
							jurusan.getFakultas().getDekan() == null ? "" : jurusan.getFakultas().getDekan().getNama());
					parameters.put("nip_dekan",
							jurusan.getFakultas().getDekan() == null ? "" : jurusan.getFakultas().getDekan().getCode());
					parameters.put("nidn_dekan",
							jurusan.getFakultas().getDekan() == null ? "" : jurusan.getFakultas().getDekan().getNidn());

					parameters.put("nama_pudek1", jurusan.getFakultas().getPudek1() == null ? ""
							: jurusan.getFakultas().getPudek1().getNama());
					parameters.put("nip_pudek1", jurusan.getFakultas().getPudek1() == null ? ""
							: jurusan.getFakultas().getPudek1().getCode());
					parameters.put("nidn_pudek1", jurusan.getFakultas().getPudek1() == null ? ""
							: jurusan.getFakultas().getPudek1().getNidn());

					parameters.put("nama_pudek2", jurusan.getFakultas().getPudek2() == null ? ""
							: jurusan.getFakultas().getPudek2().getNama());
					parameters.put("nip_pudek2", jurusan.getFakultas().getPudek2() == null ? ""
							: jurusan.getFakultas().getPudek2().getCode());
					parameters.put("nidn_pudek2", jurusan.getFakultas().getPudek2() == null ? ""
							: jurusan.getFakultas().getPudek2().getNidn());

					parameters.put("nama_pudek3", jurusan.getFakultas().getPudek3() == null ? ""
							: jurusan.getFakultas().getPudek3().getNama());
					parameters.put("nip_pudek3", jurusan.getFakultas().getPudek3() == null ? ""
							: jurusan.getFakultas().getPudek3().getCode());
					parameters.put("nidn_pudek3", jurusan.getFakultas().getPudek3() == null ? ""
							: jurusan.getFakultas().getPudek3().getNidn());

					parameters.put("nama_kajur",
							jurusan.getGrupJurusan() == null || jurusan.getGrupJurusan().getKajur() == null ? ""
									: jurusan.getGrupJurusan().getKajur().getNama());
					parameters.put("nip_kajur",
							jurusan.getGrupJurusan() == null || jurusan.getGrupJurusan().getKajur() == null ? ""
									: jurusan.getGrupJurusan().getKajur().getCode());
					parameters.put("nidn_kajur",
							jurusan.getGrupJurusan() == null || jurusan.getGrupJurusan().getKajur() == null ? ""
									: jurusan.getGrupJurusan().getKajur().getNidn());

					parameters.put("nama_perguruan_tinggi", jurusan.getFakultas().getPerguruanTinggi() == null ? ""
							: jurusan.getFakultas().getPerguruanTinggi().getNama());
					parameters.put("alamat1", jurusan.getFakultas().getPerguruanTinggi() == null ? ""
							: jurusan.getFakultas().getPerguruanTinggi().getAlamat1());
					parameters.put("alamat2", jurusan.getFakultas().getPerguruanTinggi() == null ? ""
							: jurusan.getFakultas().getPerguruanTinggi().getAlamat2());
					parameters.put("telepon", jurusan.getFakultas().getPerguruanTinggi() == null ? ""
							: jurusan.getFakultas().getPerguruanTinggi().getTelepon());
					parameters.put("faksimili", jurusan.getFakultas().getPerguruanTinggi() == null ? ""
							: jurusan.getFakultas().getPerguruanTinggi().getFaksimili());

					parameters.put("tempatlahir", biodataCalonMahasiswa.getTanggalLahir());
					parameters.put("tanggallahir", biodataCalonMahasiswa.getTanggalLahir() == null ? ""
							: Common.dateFormat2.get().format(biodataCalonMahasiswa.getTanggalLahir()));
					parameters.put("tanggallahir_1", biodataCalonMahasiswa.getTanggalLahir() == null ? ""
							: Common.dateFormat1.get().format(biodataCalonMahasiswa.getTanggalLahir()));

				}

				Common.insertProperty(Ujian.class, ujian, parameters, "ujian");

				MyWindow prosesUjianHelper = new MyWindow();
				prosesUjianHelper.setClosable(true);
				prosesUjianHelper.setTitle("Sertifikat Kelulusan");
				prosesUjianHelper.setHeight("95%");
				prosesUjianHelper.setWidth("90%");
				prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				isiParameterGambarPendukungSertifikat(parameters, ambilGambarPendukungSertifikat(ujian.getSertifikat()));
				SertifikatAction.generateReport(prosesUjianHelper, lampiran, parameters);
				prosesUjianHelper.onModal();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu melengkapi Nama Sertifikat terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Nama Sertifikat; (2) pastikan nama tidak dikosongkan; (3) simpan kembali data sertifikat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaSertifikat();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Sertifikat yang Bapak/Ibu masukkan sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan nama sertifikat yang berbeda dan lebih spesifik; (2) periksa kembali daftar sertifikat yang sudah ada; (3) simpan kembali data sertifikat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (sertifikat.getId() != null) {
			sertifikat = (Sertifikat) session.load(Sertifikat.class, sertifikat.getId());

		}

		sertifikat.setNama(nama.getValue());
		sertifikat.setKeterangan(keterangan.getValue());

		Common.refreshUpdate(session, sertifikat);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(sertifikat.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Sertifikat.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Sertifikat> sertifikat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(sertifikat);
		grid.setRowRenderer(new SertifikatRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaSertifikat() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Sertifikat.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.sertifikat.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.sertifikat.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
