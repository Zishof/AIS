package ais.action.master.penelitiandanpengabdian.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.penelitiandanpengabdian.FilePengajuanTahapanPelaporanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanTahapanPelaporanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPelaporanPenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper pengelola pengajuan laporan untuk satu tahap pelaporan
 * ({@link TahapanPelaporanPenelitianDanPengabdian}) pada modul Penelitian dan Pengabdian: dosen
 * mengunggah laporan (catatan + berkas opsional) untuk suatu tahap tertentu, terkait ke proposal
 * pengajuan penelitian/pengabdian ({@link PengajuanPenelitianDanPengabdian}) miliknya, disimpan
 * sebagai {@link PengajuanTahapanPelaporanPenelitianDanPengabdian} beserta berkas terkait
 * ({@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}).
 *
 * <p>
 * {@link #displayWindowPengajuan} membuka jendela modal berisi form pengajuan laporan tahap: pilih
 * proposal terkait (bila belum ditentukan lewat field {@link #pengajuanPenelitianDanPengabdian}),
 * catatan (CKEditor), dan opsional berkas yang sudah diunggah sebelumnya ({@code Media}/{@code File}
 * dari komponen upload). Setelah disimpan, penyimpanan berkas (bila ada) dan pembentukan URL akses
 * publiknya dijalankan asinkron lewat timer agar transaksi utama cepat selesai. Bila tahap sudah
 * berstatus {@code DISETUJUI}, form dikunci (read-only, tombol Simpan disembunyikan) dan catatan
 * ditampilkan sebagai HTML statis, bukan editor.
 * </p>
 * <p>
 * {@link #displayPengajuan} membangun panel daftar pengajuan laporan tahap (grid dengan pencarian
 * berdasarkan catatan atau identitas pengaju — user id/nama, atau NIM/nama mahasiswa) yang
 * dipasang ke {@code component} pemanggil; pencarian dijalankan lewat {@link #initCriteria(boolean)}
 * dan {@link #loadDataPengajuan()}.
 * </p>
 */
public class PengajuanTahapanPelaporanPenelitianDanPengabdianHelper implements DataCriteria {

	private MyGrid gridPengajuan;

	private Textbox cariPengaju;
	private Textbox cariCatatanPengaju;

	private Paging paging;

	private Boolean readonly = false;
	private TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdianData;

	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian;

	/**
	 * Membuka jendela modal form pengajuan laporan untuk {@code tahapanPelaporanPenelitianDanPengabdianData}.
	 * Menyimpan/memperbarui baris {@link PengajuanTahapanPelaporanPenelitianDanPengabdian} terkait
	 * proposal terpilih dan catatan yang diisi; bila {@code f}/{@code media} diberikan, berkas
	 * disimpan sebagai {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian} secara asinkron
	 * (lewat timer) setelah baris utama tersimpan, lengkap dengan URL akses publik berkas. Form
	 * dikunci (read-only) bila data yang diedit sudah berstatus {@code DISETUJUI}.
	 *
	 * @param tahapanPelaporanPenelitianDanPengabdianData tahap pelaporan target
	 * @param pengajuanTahapanPelaporanPenelitianDanPengabdianData data pengajuan yang diedit, boleh {@code null} untuk pengajuan baru
	 * @param media informasi berkas yang diunggah (nama, tipe konten), boleh {@code null}
	 * @param f     berkas fisik hasil unggahan pada disk server, boleh {@code null}
	 */
	public void displayWindowPengajuan(
			final TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdianData,
			final PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdianData,
			final Media media, final File f) throws Exception {
		final MyWindow window = new MyWindow();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("400px");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
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
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Pengajuan Proposal " + tahapanPelaporanPenelitianDanPengabdianData
						.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian().getIsi()));
		final Combobox pengajuanPenelitianDanPengabdian = new Combobox();
		row.appendChild(pengajuanPenelitianDanPengabdian);
		pengajuanPenelitianDanPengabdian.setWidth("90%");
		pengajuanPenelitianDanPengabdian.setReadonly(true);

		Common.insertCombo(pengajuanPenelitianDanPengabdian, "olehPenguna", "judul",
				PengajuanPenelitianDanPengabdian.class, Restrictions.eq("penelitianDanPengabdian",
						tahapanPelaporanPenelitianDanPengabdianData.getPenelitianDanPengabdian()));

		if (PengajuanTahapanPelaporanPenelitianDanPengabdianHelper.this.pengajuanPenelitianDanPengabdian != null) {
			Common.selectComboItem(pengajuanPenelitianDanPengabdian,
					PengajuanTahapanPelaporanPenelitianDanPengabdianHelper.this.pengajuanPenelitianDanPengabdian);
			pengajuanPenelitianDanPengabdian.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		final MyCkEditor keterangan = new MyCkEditor();
		if (pengajuanTahapanPelaporanPenelitianDanPengabdianData != null
				&& pengajuanTahapanPelaporanPenelitianDanPengabdianData.getStatus()
						.equals(PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI)) {
			row.appendChild(new ais.ui.util.MyHtml(pengajuanTahapanPelaporanPenelitianDanPengabdianData == null ? ""
					: pengajuanTahapanPelaporanPenelitianDanPengabdianData.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setValue(pengajuanTahapanPelaporanPenelitianDanPengabdianData == null ? ""
				: pengajuanTahapanPelaporanPenelitianDanPengabdianData.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("300px");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (pengajuanPenelitianDanPengabdian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pengajuan Proposal harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

//				if (keterangan.getValue().trim().isEmpty()) {
//					MyMessageboxConfig.show("Catatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//							MyMessageboxConfig.EXCLAMATION);
//					return;
//				}
				Session session = Common.getManualSession();

				PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian = (PengajuanTahapanPelaporanPenelitianDanPengabdian) session
						.createCriteria(PengajuanTahapanPelaporanPenelitianDanPengabdian.class)
						.add(Restrictions.eq("tahapanPelaporanPenelitianDanPengabdian",
								tahapanPelaporanPenelitianDanPengabdianData))
						.add(Restrictions.eq("pengajuanPenelitianDanPengabdian",
								pengajuanPenelitianDanPengabdian.getSelectedItem().getValue()))
						.setMaxResults(1).uniqueResult();
				if (pengajuanTahapanPelaporanPenelitianDanPengabdian == null) {
					pengajuanTahapanPelaporanPenelitianDanPengabdian = new PengajuanTahapanPelaporanPenelitianDanPengabdian();
				}

				pengajuanTahapanPelaporanPenelitianDanPengabdian
						.setTahapanPelaporanPenelitianDanPengabdian(tahapanPelaporanPenelitianDanPengabdianData);
				pengajuanTahapanPelaporanPenelitianDanPengabdian.setPengajuanPenelitianDanPengabdian(
						(PengajuanPenelitianDanPengabdian) pengajuanPenelitianDanPengabdian.getSelectedItem()
								.getValue());
				pengajuanTahapanPelaporanPenelitianDanPengabdian.setKeterangan(keterangan.getValue());

				session.save(pengajuanTahapanPelaporanPenelitianDanPengabdian);
				session.flush();

				if (f != null && media != null) {
					final PengajuanTahapanPelaporanPenelitianDanPengabdian temp = pengajuanTahapanPelaporanPenelitianDanPengabdian;
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							session.refresh(temp);
							FilePengajuanTahapanPelaporanPenelitianDanPengabdian filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian = new FilePengajuanTahapanPelaporanPenelitianDanPengabdian();
							filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian
									.setMimeType(media == null ? "" : media.getContentType());
							filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian
									.setNama(media == null ? "" : media.getName());
							filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian.setPath(f.getAbsolutePath());
							filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian
									.setPengajuanTahapanPelaporanPenelitianDanPengabdian(temp);
							filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian
									.setUploadDate(ais.ui.util.WaktuUtil.getDate());

							session.save(filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian);

							HttpServletRequest request = (HttpServletRequest) (ExecutionsCtrl.getCurrent() == null
									? null
									: ExecutionsCtrl.getCurrent().getNativeRequest());
							String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName()
									+ ":" + request.getServerPort() + request.getContextPath()
									+ "/FilePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian?id="
									+ filePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian.getId();
							temp.setPathUrl(url);
							Common.refreshSaveOrUpdate(session, temp);

							loadDataPengajuan();
						}
					});
				}

				window.detach();
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(window);

		if (pengajuanTahapanPelaporanPenelitianDanPengabdianData != null
				&& pengajuanTahapanPelaporanPenelitianDanPengabdianData.getStatus()
						.equals(PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI)) {
			Common.freeze(window, true);
			save.setVisible(false);
			cancel.setDisabled(false);
		}

		window.onModal();
	}

	/**
	 * Membangun panel daftar pengajuan laporan untuk {@code tahapanPelaporanPenelitianDanPengabdianData},
	 * opsional difilter ke satu {@code pengajuanPenelitianDanPengabdian} (proposal) tertentu, dan
	 * memasangnya ke {@code component}. Menyediakan kolom pencarian catatan dan identitas pengaju.
	 *
	 * @param tahapanPelaporanPenelitianDanPengabdianData tahap pelaporan target
	 * @param pengajuanPenelitianDanPengabdian             proposal pembatas cakupan, boleh {@code null} untuk semua proposal
	 * @param component                                    komponen ZK induk tempat panel dipasang
	 */
	public void displayPengajuan(
			final TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdianData,
			final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian, final Component component) {

		this.tahapanPelaporanPenelitianDanPengabdianData = tahapanPelaporanPenelitianDanPengabdianData;
		this.pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.appendChild(
				new MyCaptionStyled("Daftar Laporan Tahap " + tahapanPelaporanPenelitianDanPengabdianData.getNama()));
		
		groupbox.setWidth("95%");
		groupbox.setStyle("min-height: 2200px;");
		groupbox.setParent(component);

		MyHboxToolbar toolbar = new MyHboxToolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Ajukan Laporan Tahapan " + tahapanPelaporanPenelitianDanPengabdianData.getPenelitianDanPengabdian()
						.getTipePenelitianDanPengabdian().getIsi() + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				final Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				File folder = CommonMedia.getMediaDirectory();

				final File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
						ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + uploadEvent.getMedia().getName(),
						"UTF-8"));

				f.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(f);
				IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
				fileOutputStream.close();

				PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdianData = new PengajuanTahapanPelaporanPenelitianDanPengabdian();
				pengajuanTahapanPelaporanPenelitianDanPengabdianData
						.setTahapanPelaporanPenelitianDanPengabdian(tahapanPelaporanPenelitianDanPengabdianData);
				pengajuanTahapanPelaporanPenelitianDanPengabdianData
						.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdian);
				displayWindowPengajuan(tahapanPelaporanPenelitianDanPengabdianData,
						pengajuanTahapanPelaporanPenelitianDanPengabdianData, media, f);

			}

		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "tahapanPelaporanPenelitianDanPengabdian", "tbmuser", "mahasiswa",
				"status", "pathUrl", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				PengajuanTahapanPelaporanPenelitianDanPengabdian.class, this, "Download", "/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());

		cariPengaju = new Textbox();
		if (PengajuanTahapanPelaporanPenelitianDanPengabdianHelper.this.pengajuanPenelitianDanPengabdian == null) {
			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Diajukan oleh : ")));
			cariPengaju.setParent(toolbar);
			cariPengaju.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadDataPengajuan();
				}
			});
		}

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan : ")));
		cariCatatanPengaju = new Textbox();
		cariCatatanPengaju.setParent(toolbar);
		cariCatatanPengaju.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		MyToolbarbutton cari = new MyToolbarbutton("fa-search", "Cari");
		cari.setParent(toolbar);
		cari.setTooltiptext("Cari");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDataPengajuan();
			}
		});

		gridPengajuan = new MyGrid();
		gridPengajuan.setMold("paging");
		gridPengajuan.setPageSize(1000);
		gridPengajuan.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(gridPengajuan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penelitian dan pengabdian");
		column.setWidth(tahapanPelaporanPenelitianDanPengabdianData == null ? "20%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diajukan oleh");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");
		column.setWidth("45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});
		paging.setParent(groupbox);

		loadDataPengajuan();

	}

	/**
	 * Membangun kueri Hibernate untuk daftar pengajuan laporan tahap, difilter proposal (bila
	 * ditentukan), tahap pelaporan target, dan kata kunci pencarian catatan/identitas pengaju
	 * (userId/nama user atau NIM/nama mahasiswa).
	 *
	 * @param order {@code true} untuk menyertakan pengurutan hasil (id menurun)
	 * @return kriteria Hibernate siap dieksekusi/dipaginasi
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengajuanTahapanPelaporanPenelitianDanPengabdian.class)
				.createAlias("pengajuanPenelitianDanPengabdian", "pengajuanPenelitianDanPengabdian")
				.createAlias("pengajuanPenelitianDanPengabdian.tbmuser", "tbmuser", Criteria.LEFT_JOIN)
				.createAlias("pengajuanPenelitianDanPengabdian.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.add(pengajuanPenelitianDanPengabdian == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))

				.add(Restrictions.eq("tahapanPelaporanPenelitianDanPengabdian",
						tahapanPelaporanPenelitianDanPengabdianData))
				.add(cariCatatanPengaju.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", cariCatatanPengaju.getValue().trim(), MatchMode.ANYWHERE))

				.add(cariPengaju.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("tbmuser.userId", cariPengaju.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userNama", cariPengaju.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE))));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	/** Mengeksekusi {@link #initCriteria(boolean)} untuk halaman aktif dan merender hasilnya ke {@link #gridPengajuan}, sekaligus memperbarui total halaman {@link #paging}. */
	@SuppressWarnings("unchecked")
	public void loadDataPengajuan() {
		Common.initPaging(initCriteria(false), paging);
		List<PengajuanTahapanPelaporanPenelitianDanPengabdian> pengajuanTahapanPelaporanPenelitianDanPengabdian = initCriteria(
				true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();

		ListModel strset = new SimpleListModel(pengajuanTahapanPelaporanPenelitianDanPengabdian);

		gridPengajuan.setRowRenderer(new DetailPengajuanTahapanPelaporanPenelitianDanPengabdianRenderer());
		gridPengajuan.setModelCheckMobile(strset);
		gridPengajuan.renderAll();

	}

	class DetailPengajuanTahapanPelaporanPenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser;

		public DetailPengajuanTahapanPelaporanPenelitianDanPengabdianRenderer() {
			tbmuser = Common.getCurrentUser();
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian = (PengajuanTahapanPelaporanPenelitianDanPengabdian) arg1;
			final FilePengajuanTahapanPelaporanPenelitianDanPengabdian content = (FilePengajuanTahapanPelaporanPenelitianDanPengabdian) HibernateUtil
					.currentSession().createCriteria(FilePengajuanTahapanPelaporanPenelitianDanPengabdian.class)
					.add(Restrictions.eq("pengajuanTahapanPelaporanPenelitianDanPengabdian",
							pengajuanTahapanPelaporanPenelitianDanPengabdian))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			arg0.setVisible(content != null);

			List<String> koresponden = new ArrayList<String>();
			for (String s : pengajuanTahapanPelaporanPenelitianDanPengabdian
					.getTahapanPelaporanPenelitianDanPengabdian().getPenelitianDanPengabdian().getKorespondensi()
					.split(",")) {
				if (!s.trim().isEmpty()) {
					koresponden.add(s.trim());
				}
			}

			List<String> korespondenGrup = new ArrayList<String>();
			if (pengajuanTahapanPelaporanPenelitianDanPengabdian.getTahapanPelaporanPenelitianDanPengabdian()
					.getPenelitianDanPengabdian() != null) {

				for (String s : pengajuanTahapanPelaporanPenelitianDanPengabdian
						.getTahapanPelaporanPenelitianDanPengabdian().getPenelitianDanPengabdian()
						.getKorespondensiGrupPengguna().split(",")) {
					if (!s.trim().isEmpty()) {
						korespondenGrup.add(s.trim());
					}
				}
			}

			if (content != null) {
				new Label(pengajuanTahapanPelaporanPenelitianDanPengabdian == null ? ""
						: pengajuanTahapanPelaporanPenelitianDanPengabdian.getTahapanPelaporanPenelitianDanPengabdian()
								.getNama()).setParent(arg0);
				final File file = new File(content.getPath());
				arg0.setVisible(file.exists());

				String oleh = "";
				if (pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
						.getMahasiswa() != null) {
					oleh = (pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
							.getMahasiswa().getNim() + " "
							+ pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
									.getMahasiswa().getNama());
				} else if (pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
						.getTbmuser() != null) {
					oleh = (pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
							.getTbmuser().getUserNama() + " ("
							+ pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
									.getTbmuser().getUserId()
							+ ")");
				}

				(new Label(oleh)).setParent(arg0);
				new ais.ui.util.MyHtml(pengajuanTahapanPelaporanPenelitianDanPengabdian.getKeterangan())
						.setParent(arg0);
				new Label(Common.dateFormat.get().format(content.getUploadDate())).setParent(arg0);

				Dosen dsn = pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
						.getTbmuser() == null ? null
								: pengajuanTahapanPelaporanPenelitianDanPengabdian.getPengajuanPenelitianDanPengabdian()
										.getTbmuser().getDosen();
				if ((dsn != null && dsn.yangLoginMerupakanAtasan()) || ((koresponden.contains(tbmuser.getUserId())
						|| korespondenGrup.contains(tbmuser.hakAkses().getRoleId())))) {
					final Combobox status = new Combobox();
					status.setParent(arg0);
					status.setWidth("90%");
					MyComboitemConfig comboitem = new MyComboitemConfig(
							PengajuanTahapanPelaporanPenelitianDanPengabdian.BELUM_DIPROSES);
					comboitem.setValue(PengajuanTahapanPelaporanPenelitianDanPengabdian.BELUM_DIPROSES);
					status.appendChild(comboitem);

					comboitem = new MyComboitemConfig(PengajuanTahapanPelaporanPenelitianDanPengabdian.SEDANG_DIPROSES);
					comboitem.setValue(PengajuanTahapanPelaporanPenelitianDanPengabdian.SEDANG_DIPROSES);
					status.appendChild(comboitem);

					comboitem = new MyComboitemConfig(PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI);
					comboitem.setValue(PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI);
					status.appendChild(comboitem);

					comboitem = new MyComboitemConfig(PengajuanTahapanPelaporanPenelitianDanPengabdian.DITOLAK);
					comboitem.setValue(PengajuanTahapanPelaporanPenelitianDanPengabdian.DITOLAK);
					status.appendChild(comboitem);

					Common.selectComboItem(status, pengajuanTahapanPelaporanPenelitianDanPengabdian.getStatus());
					status.setReadonly(true);

					status.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pengajuanTahapanPelaporanPenelitianDanPengabdian
									.setStatus((String) (status.getSelectedItem() == null
											|| status.getSelectedItem().getValue() == null ? null
													: status.getSelectedItem().getValue()));
							Common.refreshUpdate(pengajuanTahapanPelaporanPenelitianDanPengabdian);
						}
					});
				} else {
					new Label(pengajuanTahapanPelaporanPenelitianDanPengabdian.getStatus()).setParent(arg0);
				}

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				MyToolbarbutton toolbarbutton = new MyToolbarbutton(FileFoto.iconAwesome(file.getName()),
						file.getName());
				toolbarbutton.setParent(hbox);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						final File file = new File(content.getPath());
						Filedownload.save(file, content.getMimeType());
					}

				});

				hbox = new Hbox();
				hbox.setParent(vbox);

				toolbarbutton = new MyToolbarbutton("fa-pencil-square-o", "Ubah");
				toolbarbutton.setParent(hbox);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						displayWindowPengajuan(
								pengajuanTahapanPelaporanPenelitianDanPengabdian
										.getTahapanPelaporanPenelitianDanPengabdian(),
								pengajuanTahapanPelaporanPenelitianDanPengabdian, null, null);
					}

				});

				toolbarbutton = new MyToolbarbutton("fa-trash", "Hapus");
				toolbarbutton.setDisabled(pengajuanTahapanPelaporanPenelitianDanPengabdian.getStatus()
						.equals(PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI));
				toolbarbutton.setTooltiptext("Hapus Data");
				toolbarbutton.setParent(hbox);
				toolbarbutton.addEventListener("onClick", new EventListener() {
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
												Common.refreshDelete(pengajuanTahapanPelaporanPenelitianDanPengabdian);

												loadDataPengajuan();
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
			}
		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
