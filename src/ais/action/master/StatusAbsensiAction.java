package ais.action.master;

import java.util.ArrayList;
import java.util.List;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Statusabsensi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk status absensi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Textbox kode},
 * {@code Textbox nama}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkNamaStatusabsensi()}); mutasi data ({@code onSave()}); operasi domain lain
 * ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class StatusAbsensiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;

	private Statusabsensi statusabsensi;
	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;

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

		if (status.isEmpty()) {
			Long[] statusabsensis = new Long[] { ConstantValues.IZIN.getId(), ConstantValues.CUTI_TAHUNAN.getId(),
					ConstantValues.STATUS_CUTI.getId(), ConstantValues.CUTI_SAKIT.getId(),
					ConstantValues.CUTI_HAJI_PEGAWAI.getId(), ConstantValues.CUTI_UMROH_PEGAWAI.getId(),
					ConstantValues.CUTI_MELAHIRKAN.getId(), ConstantValues.CUTI_MELAHIRKAN.getId(),
					ConstantValues.CUTI_PENTING.getId(), ConstantValues.CUTI_ALASAN_PENTING.getId(),
					ConstantValues.CUTI_BESAR.getId(), ConstantValues.CUTI_BESAR_II.getId(),
					ConstantValues.CUTI_HAMIL.getId(), ConstantValues.CUTI_DILUAR_TANGGUNGAN.getId(),
					ConstantValues.CUTI_STUDI_ATAU_PENELITIAN.getId(), ConstantValues.DINAS_LUAR.getId(),
					ConstantValues.TUGAS_BELAJAR.getId(), ConstantValues.MASUK.getId(),
					ConstantValues.TIDAK_ADA_ALASAN.getId(), ConstantValues.SAKIT.getId(),
					ConstantValues.BELUM_ABSEN.getId() };
			for (Long long1 : statusabsensis) {
				status.add(long1);
			}
		}

		boolean admin = Common.getApakahAdmin();

		if (add != null) { add.setVisible(admin); }
		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = admin;
		delete = admin;

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private static List<Long> status = new ArrayList<Long>();

	class StatusabsensiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Statusabsensi statusabsensi = (Statusabsensi) arg1;
			new Label(statusabsensi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Statusabsensi.class, statusabsensi, statusabsensi.getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(statusabsensi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyCheckboxConfig merupakanMerupakanCuti = new MyCheckboxConfig("Merupakan Cuti");
			merupakanMerupakanCuti.setDisabled(!edit);
			merupakanMerupakanCuti.setChecked(statusabsensi.getMerupakanCuti());
			merupakanMerupakanCuti.setParent(arg0);
			merupakanMerupakanCuti.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setMerupakanCuti(merupakanMerupakanCuti.isChecked());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyCheckboxConfig memotongJatahCuti = new MyCheckboxConfig("Dipotong Jatah Cuti");
			memotongJatahCuti.setDisabled(!edit);
			memotongJatahCuti.setChecked(statusabsensi.getMemotongJatahCuti());
			memotongJatahCuti.setParent(arg0);
			arg0.setAttribute("checkbox", memotongJatahCuti);
			memotongJatahCuti.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setMemotongJatahCuti(memotongJatahCuti.isChecked());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyCheckboxConfig dihitungKetidakhadiran = new MyCheckboxConfig("Dihitung Ketidakhadiran");
			dihitungKetidakhadiran.setDisabled(!edit);
			dihitungKetidakhadiran.setChecked(statusabsensi.getDihitungKetidakhadiran());
			dihitungKetidakhadiran.setParent(arg0);
			dihitungKetidakhadiran.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setDihitungKetidakhadiran(dihitungKetidakhadiran.isChecked());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyCheckboxConfig hariLiburDihitung = new MyCheckboxConfig("Hari Libur Dihitung");
			hariLiburDihitung.setDisabled(!edit);
			hariLiburDihitung.setChecked(statusabsensi.getHariLiburDihitung());
			hariLiburDihitung.setParent(arg0);
			hariLiburDihitung.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setHariLiburDihitung(hariLiburDihitung.isChecked());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyIntbox bolehDiambilMinimalBulanKerja = new MyIntbox(
					statusabsensi.getBolehDiambilMinimalBulanKerja());
			bolehDiambilMinimalBulanKerja.setDisabled(!edit);
			bolehDiambilMinimalBulanKerja.setParent(arg0);
			bolehDiambilMinimalBulanKerja.setWidth("90%");
			bolehDiambilMinimalBulanKerja.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setBolehDiambilMinimalBulanKerja(bolehDiambilMinimalBulanKerja.getValue());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			final MyIntbox bolehDiambilSetelahBulanKerja = new MyIntbox(
					statusabsensi.getBolehDiambilSetelahBulanKerja());
			bolehDiambilSetelahBulanKerja.setDisabled(!edit);
			bolehDiambilSetelahBulanKerja.setParent(arg0);
			bolehDiambilSetelahBulanKerja.setWidth("90%");
			bolehDiambilSetelahBulanKerja.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setBolehDiambilSetelahBulanKerja(bolehDiambilSetelahBulanKerja.getValue());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			// Satu sel berisi DUA isian: batas maksimal bulan kerja + DURASI BAKU cuti khusus.
			// Digabung dalam Vbox agar jumlah kolom grid tidak berubah (header kolom ada di ZUL).
			org.zkoss.zul.Vbox selBatasCuti = new org.zkoss.zul.Vbox();
			selBatasCuti.setParent(arg0);

			final MyIntbox bolehDiambilMaksimalBulanKerja = new MyIntbox(
					statusabsensi.getBolehDiambilMaksimalBulanKerja());
			bolehDiambilMaksimalBulanKerja.setDisabled(!edit);
			bolehDiambilMaksimalBulanKerja.setParent(selBatasCuti);
			bolehDiambilMaksimalBulanKerja.setWidth("90%");
			bolehDiambilMaksimalBulanKerja.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setBolehDiambilMaksimalBulanKerja(bolehDiambilMaksimalBulanKerja.getValue());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			// ATURAN CUTI — durasi baku cuti khusus (mis. Menikah 7, Melahirkan 90, Khitanan anak 3).
			// Kosong/0 = tidak dibatasi. Pembatasannya baru berlaku bila gerbang
			// "cuti_gerbang_durasi_baku" diaktifkan di Konfigurasi.
			Label labelDurasiBaku = new Label("Durasi baku (hari)");
			labelDurasiBaku.setStyle("font-size:10px;color:#64748b;");
			labelDurasiBaku.setParent(selBatasCuti);

			final MyIntbox durasiBakuHari = new MyIntbox(statusabsensi.getDurasiBakuHari());
			durasiBakuHari.setDisabled(!edit);
			durasiBakuHari.setParent(selBatasCuti);
			durasiBakuHari.setWidth("90%");
			durasiBakuHari.setTooltiptext("Durasi baku cuti khusus dalam hari, mis. Menikah 7, Menikahkan anak 3, "
					+ "Melahirkan 90, Istri melahirkan/keguguran 3, Orang tua meninggal 7, Keluarga inti meninggal 3, "
					+ "Khitanan anak 3. Kosongkan bila lama cuti tidak dibatasi.");
			durasiBakuHari.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusabsensi.setDurasiBakuHari(durasiBakuHari.getValue());
					Common.refreshSaveOrUpdate(statusabsensi);
				}
			});

			new Label(statusabsensi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(statusabsensi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && !status.contains(statusabsensi.getId()));
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
											Common.refreshDelete(statusabsensi);
											onSearchDefault(event);
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
		init(new Statusabsensi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Statusabsensi statusabsensi) {
		this.statusabsensi = statusabsensi;
		addWindow.setTitle(statusabsensi.getId() == null ? "Tambah Status Kehadiran" : "Ubah Status Kehadiran");
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

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Status Kehadiran"));
		row.appendChild(kode = new Textbox(statusabsensi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Status Kehadiran"));
		row.appendChild(nama = new Textbox(statusabsensi.getNama() == null ? "" : statusabsensi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(statusabsensi.getKeterangan() == null ? "" : statusabsensi.getKeterangan()));
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
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaStatusabsensi();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Status Kehadiran",
					"Nama Status Kehadiran sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama status kehadiran yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		// FIX akar masalah NullPointerException di checkNamaStatusabsensi() (proxy
		// session sudah tertutup): sebelumnya baris ini menimpa field instance
		// `this.statusabsensi` (dipakai lintas request selama window Tambah/Ubah
		// terbuka) dengan proxy hasil session.load() yang terikat ke session
		// request INI SAJA. Begitu request selesai & session ditutup, proxy itu
		// jadi basi -- pemakaian berikutnya (mis. checkNamaStatusabsensi() pada
		// klik Simpan berikutnya) meledak saat proxy mencoba lazy-init via session
		// yang sudah null. Pakai variabel lokal utk objek yang benar-benar
		// disimpan, biarkan `this.statusabsensi` tetap objek POJO biasa.
		Statusabsensi toSave;
		if (statusabsensi.getId() != null) {
			toSave = (Statusabsensi) session.load(Statusabsensi.class, statusabsensi.getId());
		} else {
			statusabsensi.setId(Common.randLong());
			toSave = statusabsensi;
		}

		toSave.setKode(kode.getValue().trim());
		toSave.setNama(nama.getValue().trim());
		toSave.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, toSave);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Statusabsensi.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Statusabsensi> statusabsensi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(statusabsensi);
		grid.setRowRenderer(new StatusabsensiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaStatusabsensi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Statusabsensi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.statusabsensi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.statusabsensi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
