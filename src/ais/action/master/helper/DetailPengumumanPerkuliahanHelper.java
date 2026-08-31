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
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.TampilanPengumumanPerkuliahanAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanPerkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanPerkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranPengumumanPerkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK dengan dua tanggung jawab terpisah pada satu {@link PengumumanPerkuliahan}
 * (pengumuman akademik): (1) mengelola komentar/diskusi ({@link DiskusiPengumumanPerkuliahan}) lewat
 * {@link #displayDetailPengumuman}, dan (2) mengelola lampiran gambar/berkas
 * ({@link LampiranPengumumanPerkuliahan}) lewat {@link #displayAttachment}. Kedua bagian dipakai
 * secara independen sesuai kebutuhan tampilan pemanggil.
 *
 * <p>
 * Bagian komentar: grid daftar komentar dengan tombol tambah/ubah/hapus; penambahan/pengubahan
 * dilakukan lewat window modal ({@link #init}) dan disimpan lewat {@link #onSave(Event)}, yang juga
 * mencatat identitas pengirim (mahasiswa/dosen/role lain) dan memicu email notifikasi lewat
 * {@link TampilanPengumumanPerkuliahanAction#kirimEmail}.
 * </p>
 * <p>
 * Bagian lampiran: bila hanya ada satu lampiran, kontennya ditampilkan langsung; bila lebih dari
 * satu, ditampilkan sebagai tab (accordion di mobile) yang dimuat malas (lazy) saat tab dibuka.
 * Setiap lampiran punya checkbox "Ditampilkan" (langsung menulis ke database lewat SQL native saat
 * diubah), tombol unduh, dan tombol hapus. Visibilitas kontrol edit/hapus diatur lewat
 * {@link #setReadonly(Boolean)}.
 * </p>
 */
public class DetailPengumumanPerkuliahanHelper implements DataLoader {

	private MyGrid grid;
	private PengumumanPerkuliahan pengumumanPerkuliahan;

	private DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan;
	private Boolean readonly = false;

	private Textbox catatan;
	private Center center;

	/**
	 * Perender baris grid untuk satu {@link DiskusiPengumumanPerkuliahan}: tanggal, catatan, nama
	 * pengirim ("oleh"), serta tombol ubah (membuka {@link #init}) dan hapus.
	 */
	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		public DetailPengumumanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan = (DiskusiPengumumanPerkuliahan) data;

			new Label(diskusiPengumumanPerkuliahan.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diskusiPengumumanPerkuliahan.getTanggal())).setParent(row);

			new Label(diskusiPengumumanPerkuliahan.getCatatan()).setParent(row);
			new Label(diskusiPengumumanPerkuliahan.getOleh()).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(diskusiPengumumanPerkuliahan);

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
											Common.refreshDelete(diskusiPengumumanPerkuliahan);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(arg0);
												}
											});

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

	/**
	 * Memuat ulang grid dengan seluruh {@link DiskusiPengumumanPerkuliahan} milik
	 * {@link #pengumumanPerkuliahan}, diurutkan berdasarkan id.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<DiskusiPengumumanPerkuliahan> diskusiPengumumanPerkuliahan = session
				.createCriteria(DiskusiPengumumanPerkuliahan.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).list();

		ListModel strset = new SimpleListModel(diskusiPengumumanPerkuliahan);
		grid.setRowRenderer(new DetailPengumumanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun panel "Komentar Pengumuman Akademik" (tombol tambah + grid berpaging komentar) di
	 * dalam {@code component}, lalu memuat datanya.
	 *
	 * @param pengumumanPerkuliahan pengumuman yang komentarnya ditampilkan/dikelola
	 * @param component             komponen induk ZK; isinya dibersihkan lebih dulu
	 */
	public void displayDetailPengumuman(final PengumumanPerkuliahan pengumumanPerkuliahan, final Component component) {

		this.pengumumanPerkuliahan = pengumumanPerkuliahan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Komentar Pengumuman Akademik"));
		
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Komentar", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				init(new DiskusiPengumumanPerkuliahan());

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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

	/**
	 * Membuka window modal kecil berisi textbox catatan untuk menambah (bila {@code id} belum ada)
	 * atau mengubah (bila sudah ada) satu {@link DiskusiPengumumanPerkuliahan}. Tombol "Simpan"
	 * memanggil {@link #onSave(Event)} lalu memuat ulang grid.
	 *
	 * @param diskusiPengumumanPerkuliahan komentar yang akan diedit, atau instance baru untuk
	 *                                      menambah komentar
	 * @throws Exception diteruskan dari kegagalan pembangunan UI
	 */
	private void init(DiskusiPengumumanPerkuliahan diskusiPengumumanPerkuliahan) throws Exception {
		this.diskusiPengumumanPerkuliahan = diskusiPengumumanPerkuliahan;

		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		addWindow.setTitle("Komentar Pengumuman Perkuliahan");
		addWindow.setHeight("300px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "300px");

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
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("85%");
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Komentar"));
		catatan = new Textbox();
		catatan.setValue(
				diskusiPengumumanPerkuliahan.getCatatan() == null ? "" : diskusiPengumumanPerkuliahan.getCatatan());
		catatan.setWidth("90%");
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
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(pengumumanPerkuliahan);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	/**
	 * Menyimpan {@link #diskusiPengumumanPerkuliahan} (baru atau hasil {@code load} ulang bila
	 * sudah punya id) dengan tanggal saat ini, catatan dari textbox {@link #catatan}, dan identitas
	 * pengirim yang ditentukan dari user login saat ini (format berbeda untuk mahasiswa, dosen, atau
	 * role lain). Setelah tersimpan, memicu email notifikasi lewat
	 * {@link TampilanPengumumanPerkuliahanAction#kirimEmail}.
	 *
	 * @param event tidak dipakai
	 * @return selalu {@code true}
	 * @throws Exception diteruskan dari kegagalan Hibernate atau pengiriman email
	 */
	public boolean onSave(Event event) throws Exception {

		String myoleh = "";
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama() + " (Mahasiswa)";
			} else if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
			} else {
				myoleh = tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")";
			}
		}

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa != null) {
			tbmuser = null;
		}

		Session session = HibernateUtil.currentSession();
		if (diskusiPengumumanPerkuliahan.getId() != null) {
			diskusiPengumumanPerkuliahan = (DiskusiPengumumanPerkuliahan) session
					.load(DiskusiPengumumanPerkuliahan.class, diskusiPengumumanPerkuliahan.getId());
		}
		diskusiPengumumanPerkuliahan.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPengumumanPerkuliahan.setOleh(myoleh);
		diskusiPengumumanPerkuliahan.setCatatan(catatan.getValue());
		diskusiPengumumanPerkuliahan.setPengumumanPerkuliahan(pengumumanPerkuliahan);
		diskusiPengumumanPerkuliahan.setTbmuser(tbmuser);
		diskusiPengumumanPerkuliahan.setMahasiswa(mahasiswa);

		Common.refreshSaveOrUpdate(session, diskusiPengumumanPerkuliahan);

		TampilanPengumumanPerkuliahanAction.kirimEmail(diskusiPengumumanPerkuliahan);

		return true;
	}

	/**
	 * Membangun panel lampiran gambar/berkas untuk {@code pengumumanPerkuliahan}: toolbar unggah
	 * (disembunyikan bila {@link #readonly}) dan area konten yang dimuat lewat
	 * {@link #loadDataAttachment()}.
	 *
	 * @param pengumumanPerkuliahan pengumuman yang lampirannya ditampilkan/dikelola
	 * @param component             komponen induk ZK tempat UI dibangun
	 * @param window                tidak dipakai langsung di badan method (diteruskan untuk
	 *                              kompatibilitas signature pemanggil)
	 */
	public void displayAttachment(final PengumumanPerkuliahan pengumumanPerkuliahan, final Component component,
			final MyWindow window) {
		this.pengumumanPerkuliahan = pengumumanPerkuliahan;
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(component);
		borderlayout.setHeight("3000px");

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);
		toolbar.setParent(north);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Tambah Gambar / Lampiran" + Common.ukuranLabelFileUpload(), "/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				Blob blob = Common.getBlobFromMedia(media, session);
				LampiranPengumumanPerkuliahan lampiranPengumumanPerkuliahan = new LampiranPengumumanPerkuliahan();
				lampiranPengumumanPerkuliahan.setFoto(blob);
				lampiranPengumumanPerkuliahan.setMimeType(media.getContentType());
				lampiranPengumumanPerkuliahan.setNama(media.getName());
				lampiranPengumumanPerkuliahan.setPengumumanPerkuliahan(pengumumanPerkuliahan);
				lampiranPengumumanPerkuliahan.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.save(lampiranPengumumanPerkuliahan);

				loadDataAttachment();
			}

		});
		button.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		loadDataAttachment();

	}

	/**
	 * Memuat ulang area konten lampiran {@link #pengumumanPerkuliahan}: bila hanya ada satu
	 * lampiran, ditampilkan langsung; bila lebih dari satu, dibangun sebagai {@link Tabbox}
	 * (accordion di perangkat mobile) dengan konten tiap tab dimuat malas saat tab dipilih (tab
	 * pertama dimuat langsung).
	 */
	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanPerkuliahan> lampiranPengumumanPerkuliahan = session
				.createCriteria(LampiranPengumumanPerkuliahan.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanPerkuliahan", pengumumanPerkuliahan)).list();

		if (lampiranPengumumanPerkuliahan.size() == 1) {
			try {
				tampilkanKonten(center, lampiranPengumumanPerkuliahan.get(0));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailPengumumanPerkuliahanHelper.java:398");
			}
		} else {

			Tabbox tabbox = new Tabbox();
			if (Common.isMobile()) {
				tabbox.setMold("accordion");
			}
			tabbox.setParent(center);
			tabbox.setHeight("3000px");
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			int index = 0;
			for (final LampiranPengumumanPerkuliahan pengumumanPerkuliahan : lampiranPengumumanPerkuliahan) {

				String n = pengumumanPerkuliahan.getNama();

				Tab tab;
				tabs.appendChild(tab = new Tab(n.length() > 30 ? n.substring(0, 30) : n, FileFoto.icon(n)));
				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanels.appendChild(tabpanelUtama);
				tabpanelUtama.setHeight("2000px");
				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelUtama.getChildren().isEmpty()) {
							tampilkanKonten(tabpanelUtama, pengumumanPerkuliahan);
						}
					}
				};

				tab.addEventListener("onClick", eventListener);

				if (index == 0) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailPengumumanPerkuliahanHelper.java:442");
					}
				}

				index++;
			}
		}
	}

	/**
	 * Menampilkan satu {@link LampiranPengumumanPerkuliahan} secara lengkap di dalam
	 * {@code tabpanelUtama}: tanggal unggah, checkbox "Ditampilkan" (langsung menulis kolom
	 * {@code ditampilkan} lewat SQL {@code UPDATE} native saat diubah), tombol unduh (lewat
	 * {@link Filedownload#save}), tombol hapus (disembunyikan bila {@link #readonly}), dan pratinjau
	 * media lewat {@link CommonMedia#preview}.
	 *
	 * @param tabpanelUtama                komponen induk tempat konten ditampilkan
	 * @param lampiranPengumumanPerkuliahan lampiran yang ditampilkan
	 * @throws Exception diteruskan dari kegagalan pembangunan pratinjau media
	 */
	private void tampilkanKonten(Component tabpanelUtama,
			final LampiranPengumumanPerkuliahan lampiranPengumumanPerkuliahan) throws Exception {
		Vbox vbox = new Vbox();
		vbox.setParent(tabpanelUtama);

		vbox.setWidth("100%");

		new Label(Common.dateFormat.get().format(lampiranPengumumanPerkuliahan.getUploadDate())).setParent(vbox);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig("Ditampilkan");
		checkbox.setChecked(lampiranPengumumanPerkuliahan.getDitampilkan());
		checkbox.setParent(vbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String sql = "update lampiran_pengumuman_perkuliahan set ditampilkan=" + checkbox.isChecked()
						+ " where id = " + lampiranPengumumanPerkuliahan.getId();
				HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(lampiranPengumumanPerkuliahan.getNama(),
				lampiranPengumumanPerkuliahan.iconDonwload());
		toolbarbutton.setParent(hbox);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				LampiranPengumumanPerkuliahan content = (LampiranPengumumanPerkuliahan) HibernateUtil.currentSession()
						.createCriteria(LampiranPengumumanPerkuliahan.class)
						.add(Restrictions.idEq(lampiranPengumumanPerkuliahan.getId())).setMaxResults(1).uniqueResult();

				Filedownload.save(content.ambilFile(), lampiranPengumumanPerkuliahan.getMimeType());
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
										Common.refreshDelete(lampiranPengumumanPerkuliahan);

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

		CommonMedia.preview(lampiranPengumumanPerkuliahan, vbox);
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
