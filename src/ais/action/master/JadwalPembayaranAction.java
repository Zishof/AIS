package ais.action.master;

import java.util.List;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jadwal pembayaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchnama}, {@code Combobox searchtahunakademik}, {@code
 * Checkbox searchaktif}, {@code Textbox keterangan}, {@code Combobox jenisKegiatan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JadwalPembayaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchnama;
	private Combobox searchtahunakademik;
	private Checkbox searchaktif;

	private Textbox keterangan;
	private Combobox jenisKegiatan;
	private Combobox tahunAkademik;
	private Radiogroup ganjil;
	private MyDatebox startDate;
	private MyDatebox endDate;
	private Combobox jenjang;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalPembayaran jadwalPembayaran;
	private MyToolbarbuttonConfig add;
	private Row rowJenisSeleksi;
	private Combobox jenisSeleksi;
	private Combobox program;
	private Textbox khususUntukNim;
	private Row rowGelombangPendaftaran;
	private Combobox gelombangPendaftaran;
	private MyCheckboxConfig adminBolehMembayarkanDiluarjadwal;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(jenisKegiatan = new Combobox(), "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (jenisKegiatan != null) { jenisKegiatan.setReadonly(true); }

		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchnama, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		Common.selectComboItem(searchtahunakademik, null);

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
	 * Renderer lokal untuk layar/komponen {@link JadwalPembayaranAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JadwalPembayaranAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JadwalPembayaranAction
	 */
	class JadwalPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) arg1;

			new Label(jadwalPembayaran.getStartDate() == null ? ""
					: Common.dateFormat2.get().format(jadwalPembayaran.getStartDate())).setParent(arg0);
			new Label(jadwalPembayaran.getEndDate() == null ? ""
					: Common.dateFormat2.get().format(jadwalPembayaran.getEndDate())).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(JadwalPembayaran.class, jadwalPembayaran,
					jadwalPembayaran.getJenisKegiatan().getNamaKegiatan())).setParent(arg0);
			if (jadwalPembayaran.getJenisSeleksi() != null) {
				a.appendChild(new MyLabelAgakKecil(jadwalPembayaran.getJenisSeleksi().getNama()));
			}
			if (jadwalPembayaran.getGelombangPendaftaran() != null) {
				a.appendChild(new MyLabelAgakKecil(jadwalPembayaran.getGelombangPendaftaran().getNama()));
			}

			new Label(jadwalPembayaran.getJenjang() == null ? "Semua" : jadwalPembayaran.getJenjang().getNama())
					.setParent(arg0);
			new Label(jadwalPembayaran.getProgram() == null ? "Semua" : jadwalPembayaran.getProgram()).setParent(arg0);
			new Label(jadwalPembayaran.getTahunAkademik() == null ? "Semua" : jadwalPembayaran.getTahunAkademik())
					.setParent(arg0);
			new Label(
					jadwalPembayaran.getGanjil() == null ? "Semua" : jadwalPembayaran.getGanjil() ? "Ganjil" : "Genap")
							.setParent(arg0);
			new Label(jadwalPembayaran.getKhususUntukNim()).setParent(arg0);
			new Label(jadwalPembayaran.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jadwalPembayaran.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalPembayaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jadwalPembayaran);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jadwalPembayaran);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											Object masihDipakai = session.createCriteria(KegiatanTemporary.class)
													.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran))
													.setMaxResults(1).uniqueResult();

											if (masihDipakai != null) {
												MyMessageboxConfig.show(
														"Jadwal pembayaran ini masih dipakai oleh data kegiatan sementara mahasiswa, tidak bisa dihapus",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											// PENTING: tabel "kegiatan" juga punya FK ke jadwal_pembayaran
											// (Kegiatan.jadwalPembayaran) tapi belum dicek di atas -- cek dulu di sini
											// supaya user dapat pesan yang jelas, bukan ConstraintViolationException
											// mentah dari Common.refreshDelete.
											Object masihDipakaiKegiatan = session.createCriteria(Kegiatan.class)
													.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran))
													.setMaxResults(1).uniqueResult();

											if (masihDipakaiKegiatan != null) {
												MyMessageboxConfig.show(
														"Jadwal pembayaran ini masih digunakan oleh kegiatan lain dan tidak bisa dihapus. Hapus/ubah kegiatan terkait terlebih dahulu.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											Common.refreshDelete(jadwalPembayaran);

											onSearchDefault(event);
										} catch (org.hibernate.exception.ConstraintViolationException cve) {
											// Jaring pengaman terakhir: seandainya masih ada relasi lain (atau race
											// condition dgn data baru) yang lolos dari pengecekan di atas, jangan
											// biarkan pesan SQL mentah tampil ke user.
											ais.common.ErrorAuditUtil.record(cve,
													"JadwalPembayaranAction: gagal hapus JadwalPembayaran#"
															+ jadwalPembayaran.getId() + ", masih direferensikan tabel lain");
											MyMessageboxConfig.show(
													"Jadwal pembayaran ini masih digunakan oleh data lain dan tidak bisa dihapus. Hapus/ubah data terkait terlebih dahulu.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
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
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		JadwalPembayaran jadwalPembayaran = new JadwalPembayaran();
		jadwalPembayaran.setTahunAkademik((String) (searchtahunakademik.getSelectedItem() == null
				|| searchtahunakademik.getSelectedItem().getValue() == null ? null
						: searchtahunakademik.getSelectedItem().getValue()));
		jadwalPembayaran.setJenisKegiatan((JenisKegiatan) (searchnama.getSelectedItem() == null ? null
				: searchnama.getSelectedItem().getValue()));

		init(jadwalPembayaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
		addWindow.setTitle(jadwalPembayaran.getId() == null ? "Tambah Jadwal Pembayaran" : "Ubah Jadwal Pembayaran");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai *"));
		row.appendChild(startDate = new MyDatebox(
				jadwalPembayaran.getStartDate() == null ? null : jadwalPembayaran.getStartDate()));
		// startDate.setConstraint("no empty");
		startDate.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai *"));
		row.appendChild(
				endDate = new MyDatebox(jadwalPembayaran.getEndDate() == null ? null : jadwalPembayaran.getEndDate()));
		// endDate.setConstraint("no empty");
		endDate.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		Common.selectComboItem(jenisKegiatan, jadwalPembayaran.getJenisKegiatan());
		row.appendChild(jenisKegiatan);
		jenisKegiatan.setWidth("90%");
		// jenisKegiatan.setConstraint("no empty");
		jenisKegiatan.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);

		rowJenisSeleksi = new MyFormRow();
		rowJenisSeleksi.setVisible(false);
		rowJenisSeleksi.setParent(rows);
		rowJenisSeleksi.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		Common.insertComboDanSemua(jenisSeleksi = new Combobox(), "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisSeleksi, jadwalPembayaran.getJenisSeleksi());
		rowJenisSeleksi.appendChild(jenisSeleksi);
		jenisSeleksi.setWidth("90%");
		jenisSeleksi.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);

		rowGelombangPendaftaran = new MyFormRow();
		rowGelombangPendaftaran.setVisible(false);
		rowGelombangPendaftaran.setParent(rows);
		rowGelombangPendaftaran.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		Common.insertComboDanSemua(gelombangPendaftaran = new Combobox(),
				new String[] { "nama", "mulai", "sampai", "jenisSeleksi" }, "tahunAkademik", GelombangPendaftaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(gelombangPendaftaran, jadwalPembayaran.getGelombangPendaftaran());
		rowGelombangPendaftaran.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisKegiatan s = (JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
						: jenisKegiatan.getSelectedItem().getValue());
				rowJenisSeleksi.setVisible(s != null && ((ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(s.getId()))
						|| (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(s.getId()))));
				rowGelombangPendaftaran
						.setVisible(s != null && ((ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
								&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(s.getId()))
								|| (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
										&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(s.getId()))));
			}
		};

		jenisKegiatan.addEventListener("onChange", eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAkademik, jadwalPembayaran.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(ganjil = new Radiogroup());
		MyRadioConfig radio = new MyRadioConfig("Ganjil");
		radio.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);
		radio.setParent(ganjil);
		radio.setChecked(jadwalPembayaran.getGanjil() != null && jadwalPembayaran.getGanjil());

		radio = new MyRadioConfig("Genap");
		radio.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);
		radio.setChecked(jadwalPembayaran.getGanjil() != null && !jadwalPembayaran.getGanjil());
		radio.setParent(ganjil);

		radio = new MyRadioConfig("Semua");
		radio.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);
		radio.setChecked(jadwalPembayaran.getGanjil() == null);
		radio.setParent(ganjil);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, jadwalPembayaran.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");
		jenjang.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		Common.selectComboItem(program, jadwalPembayaran.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		program.setDisabled(jadwalPembayaran != null && jadwalPembayaran.getId() != null);
		Common.initKeterangan(rows, "Berlaku untuk \"Program\" tertantu, kosongkan jika berlaku untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk NIM"));
		row.appendChild(khususUntukNim = new Textbox(jadwalPembayaran.getKhususUntukNim()));
		khususUntukNim.setWidth("90%");
		khususUntukNim.setRows(4);
		Common.initKeterangan(rows,
				"Masukkan daftar NIM mahasiswa yang diberikan jadwal ini, jika lebih dari satu mahasiswa pisahkan dengan tanda koma (,) contoh : 123,124,125. Kemudian kosongkan jika berlaku untuk semua NIM");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(adminBolehMembayarkanDiluarjadwal = new MyCheckboxConfig(
				"Admin Boleh Membayarkan Diluar Jadwal yang ditentukan"));
		adminBolehMembayarkanDiluarjadwal.setChecked(jadwalPembayaran.getAdminBolehMembayarkanDiluarjadwal());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jadwalPembayaran.getKeterangan() == null ? "" : jadwalPembayaran.getKeterangan()));
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

	}

	public boolean onSave(Event event) throws Exception {

		if (jenisKegiatan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran",
					"Kolom Jenis Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Boolean gnj = ganjil.getSelectedItem().getLabel().equals("Semua") ? null
				: ganjil.getSelectedItem().getLabel().equals("Ganjil");
		String ta = (String) (tahunAkademik.getSelectedItem() == null ? null
				: tahunAkademik.getSelectedItem().getValue());
		Jenjang jjg = (Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue());

		JenisSeleksi seleksi = !rowJenisSeleksi.isVisible() ? null
				: (JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
						: jenisSeleksi.getSelectedItem().getValue());

		GelombangPendaftaran gel = !rowGelombangPendaftaran.isVisible() ? null
				: (GelombangPendaftaran) (gelombangPendaftaran.getSelectedItem() == null ? null
						: gelombangPendaftaran.getSelectedItem().getValue());

		String prog = (String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue());

		String nims = JadwalPembayaran.formatNim(khususUntukNim.getValue().trim());

		Session session = HibernateUtil.currentSession();
		JadwalPembayaran count = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan.getSelectedItem().getValue()))

				.add(gel == null ? Restrictions.isNull("gelombangPendaftaran")
						: Restrictions.eq("gelombangPendaftaran", gel))

				.add(seleksi == null ? Restrictions.isNull("jenisSeleksi") : Restrictions.eq("jenisSeleksi", seleksi))

				.add(prog == null ? Restrictions.isNull("program") : Restrictions.eq("program", prog))
				.add(jjg == null ? Restrictions.isNull("tahunAkademik") : Restrictions.eq("jenjang", jjg))
				.add(ta == null ? Restrictions.isNull("jenjang") : Restrictions.eq("tahunAkademik", ta))
				.add(gnj == null ? Restrictions.isNull("ganjil") : Restrictions.eq("ganjil", gnj))
				.add(nims == null || nims.trim().isEmpty() ? Restrictions.isNull("khususUntukNim")
						: Restrictions.eq("khususUntukNim", nims))
				.add(jadwalPembayaran.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", jadwalPembayaran.getId()))

				.setMaxResults(1).uniqueResult();

		if (count != null) {
			MyMessageboxConfig.show(
					"Jadwal pembayaran \""
							+ ((JenisKegiatan) jenisKegiatan.getSelectedItem().getValue()).getNamaKegiatan()
							+ (jjg == null ? " semua jenjang, " : "\" jenjang \"" + jjg.getNama())
							+ (seleksi == null ? " semua jenis seleksi, " : "\" jenis seleksi \"" + seleksi.getNama())
							+ (nims == null || nims.trim().isEmpty() ? "" : " daftar NIM " + nims)
							+ (prog == null ? " semua program, " : "\" program \"" + prog)
							+ (gnj == null ? ", semua semester"
									: "\",  semester \"" + (ganjil.getSelectedItem().getLabel()))
							+ (ta == null ? ", semua tahun akademik " : "\",  Tahun Akademik \"" + ta) + "\" sudah ada",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jadwalPembayaran.getId() != null) {
			jadwalPembayaran = (JadwalPembayaran) session.load(JadwalPembayaran.class, jadwalPembayaran.getId());
		}

		jadwalPembayaran.setGanjil(gnj);
		jadwalPembayaran.setJenjang(jjg);
		jadwalPembayaran.setEndDate(endDate.getValue());
		jadwalPembayaran.setStartDate(startDate.getValue());
		jadwalPembayaran.setJenisKegiatan((JenisKegiatan) jenisKegiatan.getSelectedItem().getValue());
		jadwalPembayaran.setTahunAkademik(ta);
		jadwalPembayaran.setKeterangan(keterangan.getValue());
		jadwalPembayaran.setJenisSeleksi(seleksi);
		jadwalPembayaran.setProgram(prog);
		jadwalPembayaran.setKhususUntukNim(khususUntukNim.getValue());
		jadwalPembayaran.setGelombangPendaftaran(gel);
		jadwalPembayaran.setAdminBolehMembayarkanDiluarjadwal(adminBolehMembayarkanDiluarjadwal.isChecked());

		Common.refreshSaveOrUpdate(session, jadwalPembayaran);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (searchnama.getSelectedItem() == null ? null
				: searchnama.getSelectedItem().getValue());
		String tahunAkademik = (String) (searchtahunakademik.getSelectedItem() == null
				|| searchtahunakademik.getSelectedItem().getValue() == null ? null
						: searchtahunakademik.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalPembayaran.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(jenisKegiatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisKegiatan", jenisKegiatan));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalPembayaran> jadwalPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalPembayaran);
		grid.setRowRenderer(new JadwalPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}

}
