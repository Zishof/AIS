package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.sql.Blob;
import java.util.List;

import org.hibernate.Session;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.penelitiandanpengabdian.PengumumanPenelitianAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanPenelitian;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPengumumanPenelitian;
import ais.database.model.penelitiandanpengabdian.PengumumanPenelitian;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk dua sub-bagian layar detail {@link PengumumanPenelitian}: (1) diskusi/komentar
 * ({@link DiskusiPengumumanPenelitian}, lewat {@link #displayDetailPengumuman}) dengan form tambah/
 * ubah catatan yang otomatis mengirim email notifikasi ({@code PengumumanPenelitianAction.kirimEmail})
 * setiap kali disimpan, dan (2) lampiran file terkait ({@link LampiranPengumumanPenelitian}, lewat
 * {@link #displayAttachment}) dengan unggah dan pratinjau file. Kedua bagian dapat dijadikan
 * baca-saja lewat {@link #setReadonly(Boolean)}, yang menyembunyikan toolbar tambah dan tombol hapus
 * pada bagian lampiran.
 */
public class DetailPengumumanPenelitianHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid grids;
	private PengumumanPenelitian pengumumanPenelitian;
	private DiskusiPengumumanPenelitian diskusiPengumumanPenelitian;
	private Boolean readonly = false;
	private Textbox catatan;

	/** Row renderer grid diskusi: tanggal, isi catatan (HTML), penulis (mahasiswa atau pengguna sistem), serta tombol ubah/hapus. */
	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		public DetailPengumumanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DiskusiPengumumanPenelitian diskusiPengumumanPenelitian = (DiskusiPengumumanPenelitian) data;

			String oleh = "";
			if (diskusiPengumumanPenelitian.getMahasiswa() != null) {
				oleh = (diskusiPengumumanPenelitian.getMahasiswa().getNim() + " "
						+ diskusiPengumumanPenelitian.getMahasiswa().getNama());
			} else if (diskusiPengumumanPenelitian.getTbmuser() != null) {
				oleh = (diskusiPengumumanPenelitian.getTbmuser().getUserNama() + " ("
						+ diskusiPengumumanPenelitian.getTbmuser().getUserId() + ")");
			}

			new Label(diskusiPengumumanPenelitian.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diskusiPengumumanPenelitian.getTanggal())).setParent(row);
			new ais.ui.util.MyHtml(ais.ui.util.DiskusiUiHelper.escapeHtml(diskusiPengumumanPenelitian.getCatatan())).setParent(row);
			new Label(oleh).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(diskusiPengumumanPenelitian);
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
											Common.refreshDelete(diskusiPengumumanPenelitian);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/** Memuat ulang daftar diskusi pengumuman penelitian saat ini dan me-render ulang grid. Parameter {@code value} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<DiskusiPengumumanPenelitian> diskusiPengumumanPenelitian = session
				.createCriteria(DiskusiPengumumanPenelitian.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pengumumanPenelitian", pengumumanPenelitian)).list();

		ListModel strset = new SimpleListModel(diskusiPengumumanPenelitian);
		grid.setRowRenderer(new DetailPengumumanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun UI grid diskusi (tombol "Tambah Data", grid dengan kolom tanggal/catatan/oleh) di
	 * dalam {@code component} untuk pengumuman penelitian yang diberikan dan memuat data awal.
	 *
	 * @param pengumumanPenelitian pengumuman penelitian yang diskusinya ditampilkan
	 * @param component            container ZK yang akan diisi
	 */
	public void displayDetailPengumuman(final PengumumanPenelitian pengumumanPenelitian, final Component component) {

		this.pengumumanPenelitian = pengumumanPenelitian;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Diskusi Pengumuman Penelitian"));
		
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				init(new DiskusiPengumumanPenelitian());
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

	private void init(DiskusiPengumumanPenelitian diskusiPengumumanPenelitian) throws Exception {
		this.diskusiPengumumanPenelitian = diskusiPengumumanPenelitian;

		final MyWindow addWindow = new MyWindow("", "none", false);
		addWindow.setTitle("Form Diskusi");
		addWindow.setHeight("95%");
		addWindow.setWidth("90%");
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
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
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		catatan = new Textbox();
		catatan.setValue(diskusiPengumumanPenelitian.getCatatan());
		catatan.setHeight("320px");
		catatan.setRows(3);
		row.appendChild(catatan);

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
					// onSearchDefault(null);
					loadData(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

		addWindow.onModal();
	}

	/**
	 * Memvalidasi dan menyimpan catatan diskusi yang sedang diedit di dialog form, mencatat penulis
	 * (mahasiswa yang login, atau {@link Tbmuser} bila bukan mahasiswa) dan waktu penyimpanan, lalu
	 * memicu pengiriman email notifikasi lewat {@code PengumumanPenelitianAction.kirimEmail}.
	 *
	 * @param event event pemicu (tidak dipakai isinya)
	 * @return {@code true} bila berhasil disimpan; {@code false} bila validasi gagal (catatan kosong)
	 * @throws Exception diteruskan dari kegagalan Hibernate atau pengiriman email
	 */
	public boolean onSave(Event event) throws Exception {
		if (catatan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, judul pengumuman penelitian belum diisi. Langkah yang dapat dilakukan: (1) isi kolom judul pada form yang tersedia; (2) pastikan judul tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (diskusiPengumumanPenelitian.getId() != null) {
			diskusiPengumumanPenelitian = (DiskusiPengumumanPenelitian) session.load(DiskusiPengumumanPenelitian.class,
					diskusiPengumumanPenelitian.getId());
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa != null) {
			tbmuser = null;
		}

		diskusiPengumumanPenelitian.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPengumumanPenelitian.setJudul(catatan.getValue());
		diskusiPengumumanPenelitian.setTbmuser(tbmuser);
		diskusiPengumumanPenelitian.setMahasiswa(mahasiswa);
		diskusiPengumumanPenelitian.setCatatan(catatan.getValue());
		diskusiPengumumanPenelitian.setPengumumanPenelitian(pengumumanPenelitian);

		Common.refreshSaveOrUpdate(session, diskusiPengumumanPenelitian);

		PengumumanPenelitianAction.kirimEmail(diskusiPengumumanPenelitian);

		return true;
	}

	/**
	 * Membangun UI daftar lampiran file (tombol unggah bila tidak {@code readonly}, grid pratinjau)
	 * di dalam {@code component} untuk pengumuman penelitian yang diberikan dan memuat 5 lampiran
	 * terbaru.
	 *
	 * @param pengumumanPenelitian pengumuman penelitian yang lampirannya ditampilkan
	 * @param component            container ZK yang akan diisi
	 * @param window                tidak dipakai langsung di badan method
	 */
	public void displayAttachment(final PengumumanPenelitian pengumumanPenelitian, final Component component,
			final MyWindow window) {
		this.pengumumanPenelitian = pengumumanPenelitian;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Daftar file terkait dengan Pengumuman Penelitian ini"));
		
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data" + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				Blob blob = Common.getBlobFromMedia(media, session);
				LampiranPengumumanPenelitian lampiranPengumumanPenelitian = new LampiranPengumumanPenelitian();
				lampiranPengumumanPenelitian.setFoto(blob);
				lampiranPengumumanPenelitian.setMimeType(media.getContentType());
				lampiranPengumumanPenelitian.setNama(media.getName());
				lampiranPengumumanPenelitian.setPengumumanPenelitian(pengumumanPenelitian);
				lampiranPengumumanPenelitian.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.save(lampiranPengumumanPenelitian);

				loadDataAttachment();
			}

		});
		button.setParent(toolbar);

		grids = new MyGrid();
		grids.setMold("paging");
		grids.setPageSize(1000);
		grids.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grids);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Link File");
		column.setWidth("75%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataAttachment();

	}

	/** Memuat ulang 5 lampiran terbaru milik pengumuman penelitian saat ini dan me-render ulang grid lampiran. */
	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanPenelitian> lampiranPengumumanPenelitian = session
				.createCriteria(LampiranPengumumanPenelitian.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanPenelitian", pengumumanPenelitian)).setMaxResults(5).list();

		ListModel strset = new SimpleListModel(lampiranPengumumanPenelitian);

		grids.setRowRenderer(new DetailLampiranPengumumanPenelitianRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	/** Row renderer grid lampiran: nama+pratinjau file, tanggal unggah, tombol unduh dan (bila tidak readonly) hapus. */
	class DetailLampiranPengumumanPenelitianRenderer extends ais.ui.util.MyRowRenderer {

		public DetailLampiranPengumumanPenelitianRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPengumumanPenelitian lampiranPengumumanPenelitian = (LampiranPengumumanPenelitian) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(lampiranPengumumanPenelitian.getNama()).setParent(vbox);
			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPengumumanPenelitian, vbox);

			new Label(Common.dateFormat.get().format(lampiranPengumumanPenelitian.getUploadDate())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download",
					lampiranPengumumanPenelitian.iconDonwload());
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPengumumanPenelitian content = (LampiranPengumumanPenelitian) HibernateUtil.currentSession()
							.createCriteria(LampiranPengumumanPenelitian.class)
							.add(Restrictions.idEq(lampiranPengumumanPenelitian.getId())).setMaxResults(1)
							.uniqueResult();

					Filedownload.save(content.ambilFile(),
							lampiranPengumumanPenelitian.getMimeType());
				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setVisible(!readonly);
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

											Common.refreshDelete((lampiranPengumumanPenelitian));

											loadDataAttachment();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
