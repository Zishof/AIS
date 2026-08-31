package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
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

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranPengumumanAkademis;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper diskusi/komentar berulir (threaded) pada satu {@link PengumumanAkademis} (pengumuman
 * akademik), analog forum diskusi: pengguna (mahasiswa, dosen, siswa, guru, staf, atau tamu
 * anonim — bila belum login, wajib mengisi nama) dapat menambah komentar
 * ({@link DiskusiPengumumanAkademis}) berikut lampiran, membalas komentar orang lain (thread
 * bersarang, indentasi visual bertambah per level via {@code index*35px}), mengubah/menghapus
 * komentar milik sendiri (atau siapa pun bila pengguna admin), dan melihat lampiran resmi
 * pengumuman ({@link LampiranPengumumanAkademis}) terpisah dari lampiran komentar.
 *
 * <p>
 * Sebagai {@link EventListener} ({@link #onEvent}), kelas ini juga berfungsi sebagai callback
 * penyegaran tampilan detail (dipanggil ulang setelah simpan/hapus komentar lewat
 * {@link Common#createDefaultTimer(EventListener)}) — merender ulang seluruh blok lampiran
 * pengumuman + daftar komentar ke dalam komponen {@link #detail} yang sama.
 * </p>
 *
 * <p>
 * Menyimpan komentar baru memicu {@link ais.action.master.helper.BroadcastHelper#kirimEmail}
 * untuk memberi tahu pihak terkait via email.
 * </p>
 */
public class DiskusiPengumumanAkademisHelper implements EventListener {

	protected LampiranLain lampiranLain;

	private DiskusiPengumumanAkademis diskusiPengumumanAkademis;

	private PengumumanAkademis pengumumanAkademis;

	private Textbox catatan;

	private Component detail;

	private Tbmuser tbmuser;

	private Mahasiswa mahasiswa;

	private Textbox oleh;

	private DiskusiPengumumanAkademis parent = null;

	/**
	 * @param detail             komponen tempat blok lampiran+diskusi dirender ({@link #onEvent} akan mengisi ulang komponen ini)
	 * @param pengumumanAkademis pengumuman yang diskusinya dikelola
	 */
	public DiskusiPengumumanAkademisHelper(Component detail, PengumumanAkademis pengumumanAkademis) {
		this.detail = detail;
		this.pengumumanAkademis = pengumumanAkademis;
		this.tbmuser = Common.getCurrentUser();
		this.mahasiswa = this.tbmuser == null ? null : this.tbmuser.getMahasiswa();
	}

	/**
	 * Membuka jendela modal "Komentar Pengumuman" untuk menambah komentar baru atau mengubah
	 * komentar existing. Menampilkan konteks "Balasan untuk" bila {@code parent} diberikan
	 * (mode balas thread), field nama pengirim hanya untuk tamu yang belum login, dan
	 * pengunggahan satu lampiran opsional. Menyimpan lewat {@link #onSave(Event)}, lalu memicu
	 * {@link #onEvent} untuk menyegarkan tampilan diskusi.
	 *
	 * @param diskusiPengumumanAkademis entitas baru (belum tersimpan) atau existing yang diedit
	 * @param parent                    komentar induk bila ini balasan; {@code null} bila komentar level teratas
	 * @param pengumumanAkademis        pengumuman terkait
	 */
	public void init(DiskusiPengumumanAkademis diskusiPengumumanAkademis, DiskusiPengumumanAkademis parent,
			final PengumumanAkademis pengumumanAkademis) throws Exception {
		this.diskusiPengumumanAkademis = diskusiPengumumanAkademis;
		this.parent = parent;
		this.pengumumanAkademis = pengumumanAkademis;
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		addWindow.setTitle("Komentar Pengumuman");
		addWindow.setHeight("250px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "500px");

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
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("85%");
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (parent != null) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Balasan untuk"));
			row.appendChild(new Label(parent.getCatatan()));

		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Komentar"));
		catatan = new Textbox();
		catatan.setValue(diskusiPengumumanAkademis.getCatatan() == null ? "" : diskusiPengumumanAkademis.getCatatan());
		catatan.setWidth("90%");
		catatan.setRows(3);
		row.appendChild(catatan);

		this.tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setVisible(tbmuser == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Anda"));
		oleh = new Textbox();
		oleh.setWidth("90%");
		row.appendChild(oleh);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, diskusiPengumumanAkademis.getId(),
				"Lampiran Komentar Pengumuman Akademik", "Lampiran", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						lampiranLain = (LampiranLain) arg0.getData();

					}
				}, null, false, false, false, true);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Kirim Komentar", "/img/Comments-icon.png");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					Common.createDefaultTimer(DiskusiPengumumanAkademisHelper.this);

					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

		addWindow.onModal();

	}

	/**
	 * Memvalidasi (komentar tidak boleh kosong; nama wajib diisi untuk tamu belum login) dan
	 * menyimpan komentar: menyusun label "oleh" otomatis dari peran pengguna login (mahasiswa/
	 * dosen/siswa/guru/role lain — mahasiswa memakai NIM+nama, selebihnya nama+jenis peran
	 * dalam kurung), menautkan lampiran yang sudah diunggah (bila ada) ke id komentar yang baru
	 * tersimpan lewat sesi Hibernate terpisah ({@link StreamingHibernateUtil}), lalu memicu
	 * pengiriman email notifikasi via {@code BroadcastHelper.kirimEmail}.
	 *
	 * @return {@code true} bila berhasil disimpan; {@code false} bila validasi gagal
	 */
	public boolean onSave(Event event) throws Exception {

		if (catatan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Masukkan isi pengumuman", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tbmuser == null && oleh.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Masukkan nama Anda", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		String myoleh = oleh.getValue();
		if (tbmuser != null && tbmuser.hakAkses() != null) {
			if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama() + " (Mahasiswa)";
			} else if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
			} else if (tbmuser.getSiswa() != null) {
				myoleh = tbmuser.getSiswa().getNama() + " (Siswa)";
			} else if (tbmuser.ambilGuru() != null) {
				myoleh = tbmuser.ambilGuru().getNama() + " (Guru)";
			} else {
				myoleh = tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")";
			}
		}

		if (mahasiswa != null) {
			tbmuser = null;
		}

		Session session = HibernateUtil.currentSession();
		if (diskusiPengumumanAkademis.getId() != null) {
			diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) session.load(DiskusiPengumumanAkademis.class,
					diskusiPengumumanAkademis.getId());
		}
		diskusiPengumumanAkademis.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPengumumanAkademis.setOleh(myoleh);
		diskusiPengumumanAkademis.setPengguna(myoleh);
		diskusiPengumumanAkademis.setCatatan(catatan.getValue());
		diskusiPengumumanAkademis.setPengumumanAkademis(pengumumanAkademis);
		diskusiPengumumanAkademis.setTbmuser(tbmuser);
		diskusiPengumumanAkademis.setMahasiswa(mahasiswa);
		diskusiPengumumanAkademis.setParent(parent);
		diskusiPengumumanAkademis.setDosen(tbmuser == null ? null : tbmuser.ambilDosen());
		diskusiPengumumanAkademis.setBiodataCalonMahasiswa(tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa());
		diskusiPengumumanAkademis.setCalonSiswa(tbmuser == null ? null : tbmuser.getCalonSiswa());
		diskusiPengumumanAkademis.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

		Common.refreshSaveOrUpdate(session, diskusiPengumumanAkademis);

		try {
			Session sessionStream = StreamingHibernateUtil.getInstance().currentSession();
			System.out.println("lampiranLain => " + lampiranLain);
			if (lampiranLain != null && lampiranLain.getId() != null) {
				sessionStream.refresh(lampiranLain);
				lampiranLain.setRef(diskusiPengumumanAkademis.getId());

				sessionStream.getTransaction().begin();
				sessionStream.update(lampiranLain);
				sessionStream.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		lampiranLain = null;

		ais.action.master.helper.BroadcastHelper.kirimEmail(diskusiPengumumanAkademis);

		return true;
	}

	/**
	 * Merender satu komentar (dan secara rekursif seluruh balasannya, tersarang di dalam
	 * {@link MyDetail} yang selalu terbuka) ke {@code row}: foto profil penulis (mahasiswa/
	 * dosen/siswa/guru/calon mahasiswa/calon siswa/user lain — dicek berurutan), label "oleh"
	 * (dengan beberapa fallback jika field terkait kosong), isi komentar, lampiran, dan tombol
	 * aksi kontekstual — "Balas" (bila pengumuman mengizinkan komentar) + "Hapus" (hanya admin)
	 * untuk komentar orang lain; "Ubah"+"Hapus" untuk komentar milik sendiri (dicocokkan
	 * berdasarkan user id atau id mahasiswa yang login).
	 *
	 * @param row                          baris ZK yang akan diisi
	 * @param diskusiPengumumanAkademisId  id komentar yang direpresentasikan
	 * @param index                        kedalaman thread (0 = level teratas), menentukan indentasi visual
	 */
	public void displayRow(Row row, final Long diskusiPengumumanAkademisId, int index) {

		final DiskusiPengumumanAkademis diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) DiskusiPengumumanAkademis
				.ambilData(DiskusiPengumumanAkademis.class, diskusiPengumumanAkademisId.toString());
		if (diskusiPengumumanAkademis == null) {
			new Label().setParent(row);
		} else {
			TreeSet<Long> diskusiPengumumanAkademissa = pengumumanAkademis.ambilDiskusiPengumumanAkademisTotal(true,
					diskusiPengumumanAkademisId, false);

			if (diskusiPengumumanAkademissa.isEmpty()) {
				new Label().setParent(row);
			} else {
				MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.setOpen(true);

				Grid gridKomentar = new Grid();
				gridKomentar.setMold("paging");
				gridKomentar.setPageSize(100);
				gridKomentar.setParent(detail);

				Columns columns = new Columns();
				columns.setParent(gridKomentar);

				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("0px");
				column.setParent(columns);
				column.setLabel("");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("");

				Rows rows = new Rows();
				rows.setParent(gridKomentar);

				for (Long balasan : diskusiPengumumanAkademissa) {
					MyFormRow rowBalasan = new MyFormRow();
					rowBalasan.setParent(rows);

					displayRow(rowBalasan, balasan, 1 + index);

				}
			}

			String oleh = diskusiPengumumanAkademis.getBiodataCalonMahasiswa() != null
					? (diskusiPengumumanAkademis.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)")
					: (diskusiPengumumanAkademis.getMahasiswa() != null
							? diskusiPengumumanAkademis.getMahasiswa().getNama() + " (Mahasiswa)"
							: "");

			try {
				if (oleh.trim().equals("")) {
					oleh = diskusiPengumumanAkademis.getDosen() != null
							? diskusiPengumumanAkademis.getDosen().getNama() + " (Dosen)"
							: "";
				}

				if (oleh.trim().equals("")) {
					oleh = diskusiPengumumanAkademis.getTbmuser() != null
							? diskusiPengumumanAkademis.getTbmuser().getUserNama() + " ("
									+ diskusiPengumumanAkademis.getTbmuser().hakAkses().getRoleName() + ")"
							: "";
				}

				if (oleh.trim().isEmpty()) {
					oleh = (diskusiPengumumanAkademis.getOleh() + diskusiPengumumanAkademis.getPengguna());
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DiskusiPengumumanAkademisHelper.java:342");
				// TODO: handle exception
			}

			Hbox hboxUtama = new Hbox();
			hboxUtama.setStyle("padding-left: " + (index * 35) + "px;");
			try {
				if (diskusiPengumumanAkademis.getMahasiswa() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getMahasiswa()).setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getDosen() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getDosen()).setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getSiswa() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getSiswa()).setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getGuru() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getGuru()).setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getBiodataCalonMahasiswa() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getBiodataCalonMahasiswa())
							.setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getCalonSiswa() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getCalonSiswa()).setParent(hboxUtama);
				} else if (diskusiPengumumanAkademis.getTbmuser() != null) {
					CommonMedia.tampilkanGambarKecil(diskusiPengumumanAkademis.getTbmuser()).setParent(hboxUtama);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DiskusiPengumumanAkademisHelper.java:366");
			}

			ais.ui.util.MenuAksiBaris.pasang(hboxUtama);
			hboxUtama.setParent(row);
			Vbox vbox = new Vbox();
			vbox.setParent(hboxUtama);

			String isi = diskusiPengumumanAkademis.getCatatan();

			new Label(isi).setParent(vbox);

			Hbox hboxA = new Hbox();
			hboxA.setParent(vbox);

			Hbox hbox = new Hbox();

			hbox.setParent(hboxA);
			LampiranLain.createDownloadUploadFileLain(hbox, diskusiPengumumanAkademis.getId(),
					"Lampiran Komentar Pengumuman Akademik", "Lampiran Komentar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							lampiranLain = (LampiranLain) arg0.getData();

						}
					}, null, false, false, false, false);

			boolean punya = (diskusiPengumumanAkademis.getTbmuser() != null && tbmuser != null
					&& tbmuser.getUserId() != null
					&& diskusiPengumumanAkademis.getTbmuser().getUserId().equals(tbmuser.getUserId()))
					|| (diskusiPengumumanAkademis.getMahasiswa() != null && mahasiswa != null
							&& diskusiPengumumanAkademis.getMahasiswa().getId().equals(mahasiswa.getId()));

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);

			if (!punya) {
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Balas");
				toolbarbutton.setStyle(
						"font-size:9px;color: #0000EE;text-decoration-color: #0000EE;text-decoration: underline;webkit-text-decoration-color: #0000EE;");

				toolbarbutton.setParent(toolbar);
				toolbarbutton.setVisible(pengumumanAkademis.getBolehDiberiKomentar());
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(new DiskusiPengumumanAkademis(), diskusiPengumumanAkademis, pengumumanAkademis);
					}
				});

				if (Common.getApakahAdmin()) {
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus");
					button.setStyle(
							"font-size:9px;color: #0000EE;text-decoration-color: #0000EE;text-decoration: underline;webkit-text-decoration-color: #0000EE;");

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
													Common.refreshDelete(diskusiPengumumanAkademis);
													Common.createDefaultTimer(DiskusiPengumumanAkademisHelper.this);
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
				}

			} else {

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah");
				button.setStyle(
						"font-size:9px;color: #0000EE;text-decoration-color: #0000EE;text-decoration: underline;webkit-text-decoration-color: #0000EE;");

				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(diskusiPengumumanAkademis, parent, diskusiPengumumanAkademis.getPengumumanAkademis());
					}
				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Hapus");
				button.setStyle(
						"font-size:9px;color: #0000EE;text-decoration-color: #0000EE;text-decoration: underline;webkit-text-decoration-color: #0000EE;");

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
												Common.refreshDelete(diskusiPengumumanAkademis);
												Common.createDefaultTimer(DiskusiPengumumanAkademisHelper.this);
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

			}

		}
	}

	/** Merender satu baris grid komentar dengan mendelegasikan ke {@link #displayRow(Row, Long, int)} pada level teratas (index 0). */
	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			Long diskusiPengumumanAkademisId = (Long) data;

			displayRow(row, diskusiPengumumanAkademisId, 0);
		}

	}

	/** Merender satu baris grid lampiran resmi pengumuman: pratinjau media, tanggal unggah, dan tombol unduh. */
	class DetailLampiranPengumumanAkademisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPengumumanAkademis lampiranPengumumanAkademis = (LampiranPengumumanAkademis) arg1;
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPengumumanAkademis, vbox);

			new Label(Common.dateFormat.get().format(lampiranPengumumanAkademis.getUploadDate())).setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(lampiranPengumumanAkademis.getNama(),
					lampiranPengumumanAkademis.iconDonwload());
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPengumumanAkademis content = (LampiranPengumumanAkademis) HibernateUtil.currentSession()
							.createCriteria(LampiranPengumumanAkademis.class)
							.add(Restrictions.idEq(lampiranPengumumanAkademis.getId())).setMaxResults(1).uniqueResult();

					Filedownload.save(content.ambilFile(), lampiranPengumumanAkademis.getMimeType());
				}

			});

		}

	}

	/** Memuat grid {@code grids} dengan seluruh {@link LampiranPengumumanAkademis} yang boleh ditampilkan untuk {@link #pengumumanAkademis}, diurutkan terbaru dulu. */
	@SuppressWarnings("unchecked")
	public void loadDataAttachment(Grid grids) {
		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanAkademis> lampiranPengumumanAkademis = session
				.createCriteria(LampiranPengumumanAkademis.class).addOrder(Order.desc("id"))
				.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).list();

		ListModel strset = new SimpleListModel(lampiranPengumumanAkademis);

		grids.setRowRenderer(new DetailLampiranPengumumanAkademisRenderer());
		grids.setModel(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	/** Memuat grid {@code gridKomentar} dengan seluruh id komentar level teratas (thread root) untuk {@code pengumumanAkademis}; balasan dimuat secara rekursif di dalam {@link #displayRow}. */
	public void loadData(PengumumanAkademis pengumumanAkademis, Grid gridKomentar) {

		TreeSet<Long> diskusiPengumumanAkademissa = pengumumanAkademis.ambilDiskusiPengumumanAkademisTotal(true, null,
				false);
		List<Long> d = new ArrayList<Long>(diskusiPengumumanAkademissa);
		ListModel strset = new SimpleListModel(d);
		gridKomentar.setRowRenderer(new DetailPengumumanRenderer());
		gridKomentar.setModel(strset);
		d = null;
		diskusiPengumumanAkademissa = null;
	}

	/**
	 * Merender ulang seluruh blok lampiran+diskusi ke {@link #detail}: grid lampiran resmi
	 * pengumuman (hanya bila ada), tombol "Tambahkan Komentar" (hanya bila
	 * {@link PengumumanAkademis#getBolehDiberiKomentar()}), dan grid komentar berulir (hanya
	 * bila komentar diizinkan). Dipanggil awal saat detail dibuka dan berulang setelah setiap
	 * operasi simpan/hapus komentar sebagai mekanisme penyegaran.
	 */
	@Override
	public void onEvent(Event arg0) throws Exception {
		Common.clear(detail);

		Session session = HibernateUtil.currentSession();
		int jumlahLampiranPengumumanAkademis = ((Number) session.createCriteria(LampiranPengumumanAkademis.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).uniqueResult()).intValue();

		if (jumlahLampiranPengumumanAkademis > 0) {
			Grid grids = new Grid();
			grids.setMold("paging");
			grids.setPageSize(10);
			grids.setParent(detail);

			loadDataAttachment(grids);
		}

		Hbox toolbar = new Hbox();
		toolbar.setParent(detail);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambahkan Komentar",
				"/img/comment-edit-icon.png");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(pengumumanAkademis.getBolehDiberiKomentar());
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(new DiskusiPengumumanAkademis(), null, pengumumanAkademis);

			}
		});

		if (pengumumanAkademis.getBolehDiberiKomentar()) {
			Grid gridKomentar = new Grid();
			gridKomentar.setMold("paging");
			gridKomentar.setPageSize(100);
			gridKomentar.setParent(detail);

			Columns columns = new Columns();
			columns.setParent(gridKomentar);

			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("0px");
			column.setParent(columns);
			column.setLabel("");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");

			loadData(pengumumanAkademis, gridKomentar);
		}
	}

}
