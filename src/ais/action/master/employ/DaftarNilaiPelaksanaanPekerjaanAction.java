package ais.action.master.employ;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.employ.util.NilaiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.DaftarNilaiPelaksanaanPekerjaanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.DaftarNilaiPelaksanaanPekerjaan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk daftar nilai pelaksanaan pekerjaan. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataPegawaiBanbox searchyangDinilai}, {@code AmbilDataPegawaiBanbox
 * searchpenilai}, {@code AmbilDataPegawaiBanbox searchatasanPenilai}, {@code AmbilDataPegawaiBanbox
 * yangDinilai}, {@code AmbilDataPegawaiBanbox penilai}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class DaftarNilaiPelaksanaanPekerjaanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPegawaiBanbox searchyangDinilai;
	private AmbilDataPegawaiBanbox searchpenilai;
	private AmbilDataPegawaiBanbox searchatasanPenilai;

	private AmbilDataPegawaiBanbox yangDinilai;
	private AmbilDataPegawaiBanbox penilai;
	private AmbilDataPegawaiBanbox atasanPenilai;

	private Doublebox kesetiaan;
	private Doublebox prestasiKerja;
	private Doublebox tanggungJawab;
	private Doublebox ketaatan;
	private Doublebox kejujuran;
	private Doublebox kerjasama;
	private Doublebox prakarsa;
	private Doublebox kepimpinan;

	private Textbox sebutankesetiaan;
	private Textbox sebutanprestasiKerja;
	private Textbox sebutantanggungJawab;
	private Textbox sebutanketaatan;
	private Textbox sebutankejujuran;
	private Textbox sebutankerjasama;
	private Textbox sebutanprakarsa;
	private Textbox sebutankepimpinan;

	private Textbox keterangankesetiaan;
	private Textbox keteranganprestasiKerja;
	private Textbox keterangantanggungJawab;
	private Textbox keteranganketaatan;
	private Textbox keterangankejujuran;
	private Textbox keterangankerjasama;
	private Textbox keteranganprakarsa;
	private Textbox keterangankepimpinan;

	private boolean edit = false;
	private boolean delete = false;

	private DaftarNilaiPelaksanaanPekerjaan daftarNilaiPelaksanaanPekerjaan;
	private MyToolbarbuttonConfig add;
	private Doublebox jumlah;
	private Textbox sebutanjumlah;
	private Textbox keteranganjumlah;
	private Doublebox rataRata;
	private Textbox sebutanrataRata;
	private Textbox keteranganrataRata;

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

	class DaftarNilaiPelaksanaanPekerjaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DaftarNilaiPelaksanaanPekerjaan daftarNilaiPelaksanaanPekerjaan = (DaftarNilaiPelaksanaanPekerjaan) arg1;

			RevisiHelper.createNewRevisi(DaftarNilaiPelaksanaanPekerjaan.class, daftarNilaiPelaksanaanPekerjaan,
					daftarNilaiPelaksanaanPekerjaan.getYangDinilai().toString()).setParent(arg0);
			new Label(daftarNilaiPelaksanaanPekerjaan.getPenilai().toString()).setParent(arg0);
			new Label(daftarNilaiPelaksanaanPekerjaan.getAtasanPenilai().toString()).setParent(arg0);

			new Label(daftarNilaiPelaksanaanPekerjaan.getJumlah() == null ? ""
					: Common.numberFormat.get().format(daftarNilaiPelaksanaanPekerjaan.getJumlah())).setParent(arg0);

			new Label(daftarNilaiPelaksanaanPekerjaan.getRataRata() == null ? ""
					: Common.numberFormat.get().format(daftarNilaiPelaksanaanPekerjaan.getRataRata())).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(daftarNilaiPelaksanaanPekerjaan);
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
											DaftarNilaiPelaksanaanPekerjaanDao daftarNilaiPelaksanaanPekerjaanDao = DaoFactory
													.getInstance().getDaftarNilaiPelaksanaanPekerjaanDao();
											// daftarNilaiPelaksanaanPekerjaanDao.beginTransaction();
											daftarNilaiPelaksanaanPekerjaanDao
													.delete((daftarNilaiPelaksanaanPekerjaan));
											// daftarNilaiPelaksanaanPekerjaanDao.commitTransaction();
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
		init(new DaftarNilaiPelaksanaanPekerjaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DaftarNilaiPelaksanaanPekerjaan daftarNilaiPelaksanaanPekerjaan) {
		this.daftarNilaiPelaksanaanPekerjaan = daftarNilaiPelaksanaanPekerjaan;
		addWindow.setTitle(daftarNilaiPelaksanaanPekerjaan.getId() == null ? "Tambah Daftar Nilai Pelaksanaan Pekerjaan" : "Ubah Daftar Nilai Pelaksanaan Pekerjaan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout secondborderlayout = new ais.ui.util.MyBorderlayout();
		secondborderlayout.setParent(center);

		North north = new North();
		north.setParent(secondborderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("380px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("1. Pegawai yang dinilai"));
		row.appendChild(yangDinilai = new AmbilDataPegawaiBanbox());
		yangDinilai.setAttribute("pegawai", daftarNilaiPelaksanaanPekerjaan.getYangDinilai());
		yangDinilai.setValue(daftarNilaiPelaksanaanPekerjaan.getYangDinilai() == null ? ""
				: daftarNilaiPelaksanaanPekerjaan.getYangDinilai().toString());
		yangDinilai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("2. Pejabat penilai"));
		row.appendChild(penilai = new AmbilDataPegawaiBanbox());
		penilai.setAttribute("pegawai", daftarNilaiPelaksanaanPekerjaan.getPenilai());
		penilai.setValue(daftarNilaiPelaksanaanPekerjaan.getPenilai() == null ? ""
				: daftarNilaiPelaksanaanPekerjaan.getPenilai().toString());
		penilai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("3. Atasan pejabat penilai"));
		row.appendChild(atasanPenilai = new AmbilDataPegawaiBanbox());
		atasanPenilai.setAttribute("pegawai", daftarNilaiPelaksanaanPekerjaan.getAtasanPenilai());
		atasanPenilai.setValue(daftarNilaiPelaksanaanPekerjaan.getAtasanPenilai() == null ? ""
				: daftarNilaiPelaksanaanPekerjaan.getAtasanPenilai().toString());
		atasanPenilai.setWidth("90%");

		center = new Center();
		center.setParent(secondborderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig("Unsur yang dinilai");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Nilai Angka");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Nilai Sebutan");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("a. Kesetiaan"));
		row.appendChild(kesetiaan = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getKesetiaan()));
		kesetiaan.setWidth("90%");
		row.appendChild(sebutankesetiaan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutankesetiaan()));
		sebutankesetiaan.setWidth("90%");
		row.appendChild(keterangankesetiaan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeterangankesetiaan()));
		keterangankesetiaan.setWidth("90%");

		kesetiaan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kesetiaan.getValue() == null) {
					kesetiaan.setValue(0);
				}

				if (kesetiaan.getValue() > 100.0 || kesetiaan.getValue() < 0.0) {
					MyMessageboxConfig.show("Nilai harus antara 0 s.d 100", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				sebutankesetiaan.setValue(NilaiHelper.nilais[kesetiaan.getValue().intValue()]);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("b. Prestasi Kerja"));
		row.appendChild(prestasiKerja = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getPrestasiKerja()));
		prestasiKerja.setWidth("90%");
		row.appendChild(sebutanprestasiKerja = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutanprestasiKerja()));
		sebutanprestasiKerja.setWidth("90%");
		row.appendChild(
				keteranganprestasiKerja = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeteranganprestasiKerja()));
		keteranganprestasiKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("c. Tanggung Jawab"));
		row.appendChild(tanggungJawab = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getTanggungJawab()));
		tanggungJawab.setWidth("90%");
		row.appendChild(sebutantanggungJawab = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutantanggungJawab()));
		sebutantanggungJawab.setWidth("90%");
		row.appendChild(
				keterangantanggungJawab = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeterangantanggungJawab()));
		keterangantanggungJawab.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("d. Ketaatan"));
		row.appendChild(ketaatan = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getKetaatan()));
		ketaatan.setWidth("90%");
		row.appendChild(sebutanketaatan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutanketaatan()));
		sebutanketaatan.setWidth("90%");
		row.appendChild(keteranganketaatan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeteranganketaatan()));
		keteranganketaatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("e. Kejujuran"));
		row.appendChild(kejujuran = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getKejujuran()));
		kejujuran.setWidth("90%");
		row.appendChild(sebutankejujuran = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutankejujuran()));
		sebutankejujuran.setWidth("90%");
		row.appendChild(keterangankejujuran = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeterangankejujuran()));
		keterangankejujuran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("f. Kerjasama"));
		row.appendChild(kerjasama = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getKerjasama()));
		kerjasama.setWidth("90%");
		row.appendChild(sebutankerjasama = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutankerjasama()));
		sebutankerjasama.setWidth("90%");
		row.appendChild(keterangankerjasama = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeterangankerjasama()));
		keterangankerjasama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("g. Prakarsa"));
		row.appendChild(prakarsa = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getPrakarsa()));
		prakarsa.setWidth("90%");
		row.appendChild(sebutanprakarsa = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutanprakarsa()));
		sebutanprakarsa.setWidth("90%");
		row.appendChild(keteranganprakarsa = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeteranganprakarsa()));
		keteranganprakarsa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("h. Kepimpinan"));
		row.appendChild(kepimpinan = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getKepimpinan()));
		kepimpinan.setWidth("90%");
		row.appendChild(sebutankepimpinan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutankepimpinan()));
		sebutankepimpinan.setWidth("90%");
		row.appendChild(keterangankepimpinan = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeterangankepimpinan()));
		keterangankepimpinan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah"));
		row.appendChild(jumlah = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getJumlah()));
		jumlah.setWidth("90%");
		row.appendChild(sebutanjumlah = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutanjumlah()));
		sebutanjumlah.setWidth("90%");
		row.appendChild(keteranganjumlah = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeteranganjumlah()));
		keteranganjumlah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rata-rata"));
		row.appendChild(rataRata = new Doublebox(daftarNilaiPelaksanaanPekerjaan.getRataRata()));
		rataRata.setWidth("90%");
		row.appendChild(sebutanrataRata = new Textbox(daftarNilaiPelaksanaanPekerjaan.getSebutanrataRata()));
		sebutanrataRata.setWidth("90%");
		row.appendChild(keteranganrataRata = new Textbox(daftarNilaiPelaksanaanPekerjaan.getKeteranganrataRata()));
		keteranganrataRata.setWidth("90%");

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

	}

	public boolean onSave(Event event) throws Exception {
		if (yangDinilai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai yang dinilai belum dipilih. Langkah yang dapat dilakukan: (1) pilih Pegawai yang dinilai menggunakan tombol cari; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (penilai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pejabat Penilai belum dipilih. Langkah yang dapat dilakukan: (1) pilih Pejabat Penilai menggunakan tombol cari; (2) pastikan data pegawai penilai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (atasanPenilai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Atasan Pejabat Penilai belum dipilih. Langkah yang dapat dilakukan: (1) pilih Atasan Pejabat Penilai menggunakan tombol cari; (2) pastikan data pegawai atasan sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		DaftarNilaiPelaksanaanPekerjaanDao daftarNilaiPelaksanaanPekerjaanDao = DaoFactory.getInstance()
				.getDaftarNilaiPelaksanaanPekerjaanDao();
		if (daftarNilaiPelaksanaanPekerjaan.getId() != null) {
			daftarNilaiPelaksanaanPekerjaan = daftarNilaiPelaksanaanPekerjaanDao
					.load(daftarNilaiPelaksanaanPekerjaan.getId());
		}

		daftarNilaiPelaksanaanPekerjaan.setAtasanPenilai((Pegawai) atasanPenilai.getAttribute("pegawai"));
		daftarNilaiPelaksanaanPekerjaan.setYangDinilai((Pegawai) yangDinilai.getAttribute("pegawai"));
		daftarNilaiPelaksanaanPekerjaan.setPenilai((Pegawai) penilai.getAttribute("pegawai"));

		daftarNilaiPelaksanaanPekerjaan.setJumlah(jumlah.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKejujuran(kejujuran.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKepimpinan(kepimpinan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKerjasama(kerjasama.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKesetiaan(kesetiaan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKetaatan(ketaatan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setPrestasiKerja(prestasiKerja.getValue());
		daftarNilaiPelaksanaanPekerjaan.setTanggungJawab(tanggungJawab.getValue());
		daftarNilaiPelaksanaanPekerjaan.setPrakarsa(prakarsa.getValue());
		daftarNilaiPelaksanaanPekerjaan.setRataRata(rataRata.getValue());

		daftarNilaiPelaksanaanPekerjaan.setKeteranganjumlah(keteranganjumlah.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeterangankejujuran(keterangankejujuran.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeterangankepimpinan(keterangankepimpinan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeterangankerjasama(keterangankerjasama.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeterangankesetiaan(keterangankesetiaan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeteranganketaatan(keteranganketaatan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeteranganprestasiKerja(keteranganprestasiKerja.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeterangantanggungJawab(keterangantanggungJawab.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeteranganprakarsa(keteranganprakarsa.getValue());
		daftarNilaiPelaksanaanPekerjaan.setKeteranganrataRata(keteranganrataRata.getValue());

		daftarNilaiPelaksanaanPekerjaan.setSebutanjumlah(sebutanjumlah.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutankejujuran(sebutankejujuran.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutankepimpinan(sebutankepimpinan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutankerjasama(sebutankerjasama.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutankesetiaan(sebutankesetiaan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutanketaatan(sebutanketaatan.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutanprestasiKerja(sebutanprestasiKerja.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutantanggungJawab(sebutantanggungJawab.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutanprakarsa(sebutanprakarsa.getValue());
		daftarNilaiPelaksanaanPekerjaan.setSebutanrataRata(sebutanrataRata.getValue());

		if (daftarNilaiPelaksanaanPekerjaan.getId() != null) {
			daftarNilaiPelaksanaanPekerjaanDao.update(daftarNilaiPelaksanaanPekerjaan);
		} else {
			daftarNilaiPelaksanaanPekerjaanDao.save(daftarNilaiPelaksanaanPekerjaan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DaftarNilaiPelaksanaanPekerjaan.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchyangDinilai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchyangDinilai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("yangDinilai", searchyangDinilai.getAttribute("pegawai"))))
				.add((searchpenilai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpenilai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penilai", searchpenilai.getAttribute("pegawai"))))
				.add((searchatasanPenilai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchatasanPenilai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("atasanPenilai", searchatasanPenilai.getAttribute("pegawai"))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DaftarNilaiPelaksanaanPekerjaan> daftarNilaiPelaksanaanPekerjaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(daftarNilaiPelaksanaanPekerjaan);
		grid.setRowRenderer(new DaftarNilaiPelaksanaanPekerjaanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
