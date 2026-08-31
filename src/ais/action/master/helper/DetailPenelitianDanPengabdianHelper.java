package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.sql.Blob;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
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
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.penelitiandanpengabdian.PenelitianDanPengabdianAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPenelitianDanPengabdian;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.FilePengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk dua sub-fitur pendukung satu {@link PenelitianDanPengabdian} (kegiatan
 * penelitian/pengabdian masyarakat dosen atau mahasiswa): thread diskusi dan daftar lampiran.
 *
 * <p>
 * {@link #displayDetailPengumuman} menampilkan grid berpaging {@link
 * DiskusiPenelitianDanPengabdian} (catatan/komentar dengan penulis dosen atau mahasiswa, dapat
 * membalas komentar lain lewat {@link #init(DiskusiPenelitianDanPengabdian)} yang menandai
 * relasi balasan {@code tbmuserBalasan}/{@code mahasiswaBalasan}). Setiap diskusi baru otomatis
 * dikaitkan ke lampiran pengajuan terbaru penulisnya (dicari lewat {@link
 * FilePengajuanPenelitianDanPengabdian}) dan memicu pengiriman email lewat {@link
 * PenelitianDanPengabdianAction#kirimEmail}.
 * </p>
 *
 * <p>
 * {@link #displayAttachment} menampilkan/mengelola daftar berkas lampiran ({@link
 * LampiranPenelitianDanPengabdian}, disimpan sebagai BLOB) terkait kegiatan atau — bila dipanggil
 * lewat overload {@link PengajuanPenelitianDanPengabdian} — terkait satu pengajuan spesifik di
 * dalamnya. Flag {@link #readonly} menyembunyikan tombol tambah/hapus untuk tampilan baca-saja.
 * </p>
 */
public class DetailPenelitianDanPengabdianHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid grids;

	private PenelitianDanPengabdian penelitianDanPengabdian;
	private DiskusiPenelitianDanPengabdian diskusiPenelitianDanPengabdian;
	private Boolean readonly = false;

	private Textbox catatan;
	private Textbox cariDiskusi;
	private DiskusiPenelitianDanPengabdian diskusiPenelitianDanPengabdianBalasDiskusi = null;
	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = null;

	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		private HttpServletRequest request;

		public DetailPengumumanRenderer() {
			request = (HttpServletRequest) (ExecutionsCtrl.getCurrent() == null ? null
					: ExecutionsCtrl.getCurrent().getNativeRequest());
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DiskusiPenelitianDanPengabdian diskusiPenelitianDanPengabdian = (DiskusiPenelitianDanPengabdian) data;

			new Label(diskusiPenelitianDanPengabdian.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diskusiPenelitianDanPengabdian.getTanggal())).setParent(row);
			new ais.ui.util.MyHtml(diskusiPenelitianDanPengabdian.getCatatan()).setParent(row);

			String oleh = "";
			if (diskusiPenelitianDanPengabdian.getMahasiswa() != null) {
				oleh = (diskusiPenelitianDanPengabdian.getMahasiswa().getNim() + " "
						+ diskusiPenelitianDanPengabdian.getMahasiswa().getNama());
			} else if (diskusiPenelitianDanPengabdian.getTbmuser() != null) {
				oleh = (diskusiPenelitianDanPengabdian.getTbmuser().getUserNama() + " ("
						+ diskusiPenelitianDanPengabdian.getTbmuser().getUserId() + ")");
			}

			if (diskusiPenelitianDanPengabdian.getMahasiswaBalasan() != null) {
				oleh += ", Reply untuk : " + (diskusiPenelitianDanPengabdian.getMahasiswaBalasan().getNim() + " "
						+ diskusiPenelitianDanPengabdian.getMahasiswaBalasan().getNama());
			} else if (diskusiPenelitianDanPengabdian.getTbmuserBalasan() != null) {
				oleh += ", Reply untuk : " + (diskusiPenelitianDanPengabdian.getTbmuserBalasan().getUserNama() + " ("
						+ diskusiPenelitianDanPengabdian.getTbmuserBalasan().getUserId() + ")");
			}

			new Label(oleh).setParent(row);

			if (diskusiPenelitianDanPengabdian.getFilePengajuanPengajuanPenelitianDanPengabdian() != null) {
				FilePengajuanPenelitianDanPengabdian content = diskusiPenelitianDanPengabdian
						.getFilePengajuanPengajuanPenelitianDanPengabdian();
				String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName() + ":"
						+ request.getServerPort() + request.getContextPath()
						+ "/FilePengajuanPengajuanPenelitianDanPengabdian?id=" + content.getId();
				A a;
				(a = new A(content.getNama())).setParent(row);
				a.setHref(url);
				a.setTarget("_blank");

			} else {
				new Label().setParent(row);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
			button.setTooltiptext("Reply Data");
			button.setVisible(diskusiPenelitianDanPengabdian.getTbmuserBalasan() == null
					&& diskusiPenelitianDanPengabdian.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					diskusiPenelitianDanPengabdianBalasDiskusi = diskusiPenelitianDanPengabdian;
					HibernateUtil.currentSession().refresh(diskusiPenelitianDanPengabdianBalasDiskusi);
					init(new DiskusiPenelitianDanPengabdian());
				}
			});
			button.setParent(row);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(diskusiPenelitianDanPengabdian);
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
											Common.refreshDelete(diskusiPenelitianDanPengabdian);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}

	}

	/** Implementasi {@link DataLoader#loadData}: memuat ulang daftar diskusi (maks 300 baris) sesuai kata kunci {@code cariDiskusi} yang dicocokkan ke user id/nama pengirim maupun penerima balasan. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.or(
				Restrictions.or(Restrictions.ilike("tbmuser.userId", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("tbmuser.userNama", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE)),
				Restrictions.or(Restrictions.ilike("mahasiswa.nim", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("mahasiswa.nama", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE)));

		criterion = Restrictions.or(criterion, Restrictions.or(
				Restrictions.or(
						Restrictions.ilike("tbmuserBalasan.userId", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("tbmuserBalasan.userNama", cariDiskusi.getValue().trim(),
								MatchMode.ANYWHERE)),
				Restrictions.or(
						Restrictions.ilike("mahasiswaBalasan.nim", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("mahasiswaBalasan.nama", cariDiskusi.getValue().trim(),
								MatchMode.ANYWHERE))));

		List<DiskusiPenelitianDanPengabdian> diskusiPenelitianDanPengabdian = session
				.createCriteria(DiskusiPenelitianDanPengabdian.class)
				.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("tbmuserBalasan", "tbmuserBalasan", Criteria.LEFT_JOIN)
				.createAlias("mahasiswaBalasan", "mahasiswaBalasan", Criteria.LEFT_JOIN)

				.add(cariDiskusi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion)
				.addOrder(Order.asc("id")).add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
				.setMaxResults(300).list();

		ListModel strset = new SimpleListModel(diskusiPenelitianDanPengabdian);
		grid.setRowRenderer(new DetailPengumumanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Titik masuk thread diskusi: membangun toolbar (Tambah Data, cetak, pencarian "Diskusi oleh")
	 * dan grid berpaging {@link DiskusiPenelitianDanPengabdian} untuk {@code
	 * penelitianDanPengabdian} tertentu.
	 *
	 * @param penelitianDanPengabdian kegiatan yang diskusinya ditampilkan
	 * @param component               komponen induk (dibersihkan lebih dulu)
	 * @param window                  window pembungkus (tidak dipakai langsung selain sebagai konteks)
	 */
	public void displayDetailPengumuman(final PenelitianDanPengabdian penelitianDanPengabdian,
			final Component component, final MyWindow window) {

		this.penelitianDanPengabdian = penelitianDanPengabdian;
		Common.clear(component);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Diskusi Penelitian dan Pengabdian"));
		
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
				init(new DiskusiPenelitianDanPengabdian());

			}

		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "penelitianDanPengabdian", "tbmuser", "mahasiswa", "catatan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();

				Criterion criterion = Restrictions.or(
						Restrictions.or(
								Restrictions.ilike("tbmuser.userId", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userNama", cariDiskusi.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", cariDiskusi.getValue().trim(),
										MatchMode.ANYWHERE)));

				criterion = Restrictions.or(criterion, Restrictions.or(Restrictions.or(
						Restrictions.ilike("tbmuserBalasan.userId", cariDiskusi.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("tbmuserBalasan.userNama", cariDiskusi.getValue().trim(),
								MatchMode.ANYWHERE)),
						Restrictions.or(
								Restrictions.ilike("mahasiswaBalasan.nim", cariDiskusi.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswaBalasan.nama", cariDiskusi.getValue().trim(),
										MatchMode.ANYWHERE))));

				return session.createCriteria(DiskusiPenelitianDanPengabdian.class)
						.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN)
						.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
						.createAlias("tbmuserBalasan", "tbmuserBalasan", Criteria.LEFT_JOIN)
						.createAlias("mahasiswaBalasan", "mahasiswaBalasan", Criteria.LEFT_JOIN)

						.add(cariDiskusi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion)

						.addOrder(Order.desc("id"))
						.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian));
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Diskusi oleh : ")));
		cariDiskusi = new Textbox();
		cariDiskusi.setParent(toolbar);
		cariDiskusi.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setTooltiptext("Cari");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengajuan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Balas");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		loadData(null);

	}

	/**
	 * Membuka dialog modal tambah/ubah satu {@link DiskusiPenelitianDanPengabdian}. Bila {@link
	 * #diskusiPenelitianDanPengabdianBalasDiskusi} sedang terisi (dipicu tombol Reply pada
	 * renderer), header "Balas ke" ditampilkan menunjukkan penulis komentar yang dibalas.
	 *
	 * @param diskusiPenelitianDanPengabdian data baru (kosong) untuk tambah, atau data existing untuk ubah
	 */
	private void init(DiskusiPenelitianDanPengabdian diskusiPenelitianDanPengabdian) throws Exception {
		this.diskusiPenelitianDanPengabdian = diskusiPenelitianDanPengabdian;

		final MyWindow addWindow = new MyWindow("", "none", false);
		addWindow.setTitle("Form Diskusi Penelitian dan Pengabdian");
		addWindow.setHeight("95%");
		addWindow.setWidth("90%");
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Common.clear(addWindow);
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

		if (diskusiPenelitianDanPengabdianBalasDiskusi != null
				&& diskusiPenelitianDanPengabdianBalasDiskusi.getMahasiswa() != null) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Balas ke"));
			row.appendChild(
					new ais.ui.util.MyLabelConfig(diskusiPenelitianDanPengabdianBalasDiskusi.getMahasiswa().getNim()
							+ " " + diskusiPenelitianDanPengabdianBalasDiskusi.getMahasiswa().getNama()));
		} else if (diskusiPenelitianDanPengabdianBalasDiskusi != null
				&& diskusiPenelitianDanPengabdianBalasDiskusi.getTbmuser() != null) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Balas ke"));
			row.appendChild(
					new ais.ui.util.MyLabelConfig(diskusiPenelitianDanPengabdianBalasDiskusi.getTbmuser().getUserNama()
							+ " (" + diskusiPenelitianDanPengabdianBalasDiskusi.getTbmuser().getUserId() + ")"));
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		catatan = new Textbox();
		catatan.setValue(diskusiPenelitianDanPengabdian.getCatatan());
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
				diskusiPenelitianDanPengabdianBalasDiskusi = null;
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Memvalidasi (catatan tidak boleh kosong) dan menyimpan satu {@link
	 * DiskusiPenelitianDanPengabdian}: penulis diisi dari user login (dosen atau mahasiswa),
	 * lampiran pengajuan terkait dicari otomatis berdasarkan penulis dan kegiatan, lalu email
	 * notifikasi dikirim lewat {@link PenelitianDanPengabdianAction#kirimEmail}.
	 *
	 * @return {@code true} bila berhasil disimpan; {@code false} bila catatan kosong (pesan sudah ditampilkan)
	 */
	public boolean onSave(Event event) throws Exception {
		if (catatan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, judul penelitian/pengabdian belum diisi. Langkah yang dapat dilakukan: (1) isi kolom judul pada form yang tersedia; (2) pastikan judul tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (diskusiPenelitianDanPengabdian.getId() != null) {
			diskusiPenelitianDanPengabdian = (DiskusiPenelitianDanPengabdian) session
					.load(DiskusiPenelitianDanPengabdian.class, diskusiPenelitianDanPengabdian.getId());
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa != null) {
			tbmuser = null;
		}

		diskusiPenelitianDanPengabdian.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPenelitianDanPengabdian.setJudul(catatan.getValue());
		diskusiPenelitianDanPengabdian.setTbmuser(tbmuser);
		diskusiPenelitianDanPengabdian.setMahasiswa(mahasiswa);
		diskusiPenelitianDanPengabdian.setCatatan(catatan.getValue());
		diskusiPenelitianDanPengabdian.setPenelitianDanPengabdian(penelitianDanPengabdian);
		diskusiPenelitianDanPengabdian.setTbmuserBalasan(
				diskusiPenelitianDanPengabdian == null ? null : diskusiPenelitianDanPengabdian.getTbmuser());
		diskusiPenelitianDanPengabdian.setMahasiswaBalasan(
				diskusiPenelitianDanPengabdian == null ? null : diskusiPenelitianDanPengabdian.getMahasiswa());

		if (diskusiPenelitianDanPengabdian == null) {
			FilePengajuanPenelitianDanPengabdian content = (FilePengajuanPenelitianDanPengabdian) HibernateUtil
					.currentSession().createCriteria(FilePengajuanPenelitianDanPengabdian.class)
					.createAlias("pengajuanPenelitianDanPengabdian", "pengajuanPenelitianDanPengabdian")
					.add(Restrictions.eq("pengajuanPenelitianDanPengabdian.penelitianDanPengabdian",
							penelitianDanPengabdian))
					.add(Restrictions.or(Restrictions.eq("pengajuanPenelitianDanPengabdian.tbmuser", tbmuser),
							Restrictions.eq("pengajuanPenelitianDanPengabdian.mahasiswa", mahasiswa)))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			diskusiPenelitianDanPengabdian.setFilePengajuanPengajuanPenelitianDanPengabdian(content);
		} else {
			tbmuser = diskusiPenelitianDanPengabdian.getTbmuser();
			mahasiswa = diskusiPenelitianDanPengabdian.getMahasiswa();
			if (mahasiswa != null) {
				tbmuser = null;
			}
			FilePengajuanPenelitianDanPengabdian content = (FilePengajuanPenelitianDanPengabdian) HibernateUtil
					.currentSession().createCriteria(FilePengajuanPenelitianDanPengabdian.class)
					.createAlias("pengajuanPenelitianDanPengabdian", "pengajuanPenelitianDanPengabdian")
					.add(Restrictions.eq("pengajuanPenelitianDanPengabdian.penelitianDanPengabdian",
							penelitianDanPengabdian))
					.add(Restrictions.or(Restrictions.eq("pengajuanPenelitianDanPengabdian.tbmuser", tbmuser),
							Restrictions.eq("pengajuanPenelitianDanPengabdian.mahasiswa", mahasiswa)))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			diskusiPenelitianDanPengabdian.setFilePengajuanPengajuanPenelitianDanPengabdian(content);
		}

		Common.refreshSaveOrUpdate(session, diskusiPenelitianDanPengabdian);

		PenelitianDanPengabdianAction.kirimEmail(diskusiPenelitianDanPengabdian);

		diskusiPenelitianDanPengabdianBalasDiskusi = null;
		return true;
	}

	/** Seperti {@link #displayAttachment(PenelitianDanPengabdian, Component, MyWindow)}, tetapi lampiran difilter juga ke satu {@code pengajuanPenelitianDanPengabdian} spesifik. */
	public void displayAttachment(final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian,
			final Component component, final MyWindow window) {
		this.pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian;
		displayAttachment(pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian(), component, window);
	}

	/**
	 * Titik masuk daftar lampiran: membangun toolbar unggah (bila {@link #readonly} false) dan
	 * grid maksimal 5 lampiran terbaru ({@link LampiranPenelitianDanPengabdian}) untuk kegiatan
	 * ini.
	 *
	 * @param penelitianDanPengabdian kegiatan yang lampirannya ditampilkan
	 * @param component               komponen induk
	 * @param window                  window pembungkus (tidak dipakai langsung selain sebagai konteks)
	 */
	public void displayAttachment(final PenelitianDanPengabdian penelitianDanPengabdian, final Component component,
			final MyWindow window) {
		this.penelitianDanPengabdian = penelitianDanPengabdian;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Daftar file terkait dengan Penelitian dan Pengabdian ini"));
		
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
				LampiranPenelitianDanPengabdian lampiranPenelitianDanPengabdian = new LampiranPenelitianDanPengabdian();
				lampiranPenelitianDanPengabdian.setFoto(blob);
				lampiranPenelitianDanPengabdian.setMimeType(media.getContentType());
				lampiranPenelitianDanPengabdian.setNama(media.getName());
				lampiranPenelitianDanPengabdian.setPenelitianDanPengabdian(penelitianDanPengabdian);
				lampiranPenelitianDanPengabdian.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.save(lampiranPenelitianDanPengabdian);

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

	/** Memuat ulang grid lampiran (maksimal 5 baris terbaru, difilter juga ke {@link #pengajuanPenelitianDanPengabdian} bila diset). */
	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Session session = HibernateUtil.currentSession();
		List<LampiranPenelitianDanPengabdian> lampiranPenelitianDanPengabdian = session
				.createCriteria(LampiranPenelitianDanPengabdian.class)
				.add(pengajuanPenelitianDanPengabdian == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))
				.addOrder(Order.desc("id")).add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
				.setMaxResults(5).list();

		ListModel strset = new SimpleListModel(lampiranPenelitianDanPengabdian);

		grids.setRowRenderer(new DetailLampiranPenelitianDanPengabdianRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	class DetailLampiranPenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPenelitianDanPengabdian lampiranPenelitianDanPengabdian = (LampiranPenelitianDanPengabdian) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(lampiranPenelitianDanPengabdian.getNama()).setParent(vbox);
			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPenelitianDanPengabdian, vbox);

			new Label(Common.dateFormat.get().format(lampiranPenelitianDanPengabdian.getUploadDate())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download",
					lampiranPenelitianDanPengabdian.iconDonwload());
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPenelitianDanPengabdian content = (LampiranPenelitianDanPengabdian) HibernateUtil
							.currentSession().createCriteria(LampiranPenelitianDanPengabdian.class)
							.add(Restrictions.idEq(lampiranPenelitianDanPengabdian.getId())).setMaxResults(1)
							.uniqueResult();

					Filedownload.save(content.ambilFile(), lampiranPenelitianDanPengabdian.getMimeType());
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

											Common.refreshDelete((lampiranPenelitianDanPengabdian));

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

}
