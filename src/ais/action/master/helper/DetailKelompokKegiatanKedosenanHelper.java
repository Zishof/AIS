package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;
import java.util.Set;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer untuk mengelola daftar "Rincian Aspek" ({@link DetailKelompokKegiatanKedosenan})
 * milik satu {@link KelompokKegiatanKedosenan} (kelompok kegiatan penilaian kinerja dosen, mis.
 * Tridarma). Setiap rincian aspek dapat ditautkan ke banyak {@link JabatanKegiatanKedosenan}
 * (jabatan/status/tugas yang berlaku) dan banyak {@link SkalaKegiatanKedosenan} (skala penilaian),
 * memiliki nomor urut tampil, serta dua flag: aktif dan bisa-dipilih-dosen.
 *
 * <p>
 * <b>Catatan teknis penting</b>: koleksi {@code jabatanKegiatanKedosenans}/
 * {@code skalaKegiatanKedosenans} pada entitas memakai comparator berbasis {@code nomorUrut} (dari
 * {@code GeneralValueObject}) yang mengembalikan 0 untuk item ber-{@code nomorUrut} sama/null.
 * Karena itu, kode ini SENGAJA menghindari {@code TreeSet} bawaan koleksi saat menampilkan
 * ({@link DetailKelompokKegiatanKedosenanRenderer}, memakai {@link List} biasa yang diurutkan
 * lewat {@code Collections.sort}) maupun saat memilih ({@link #init}, memakai
 * {@code LinkedHashMap}/{@code LinkedHashSet} per-id) — memakai {@code TreeSet} di titik-titik ini
 * akan membuat item dengan nomor urut kembar saling menimpa/hilang. Jangan mengubah pola ini
 * tanpa memahami implikasi comparator tersebut.
 * </p>
 */
public class DetailKelompokKegiatanKedosenanHelper implements DataLoader {

	/** Grid daftar rincian aspek yang sedang ditampilkan; dibangun di {@link #displayDetailKelompokKegiatanKedosenan}, diisi ulang oleh {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Kelompok kegiatan kedosenan yang rincian aspeknya dikelola, ditetapkan di {@link #displayDetailKelompokKegiatanKedosenan}. */
	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan;

	/** Perender baris grid: nama rincian aspek, daftar jabatan & skala tertaut (masing-masing diurutkan dan dinomori tampil), nomor urut (editable), checkbox aktif/bisa-dipilih-dosen (tersimpan otomatis on-check), tombol edit (buka {@link #init}) dan hapus (dengan konfirmasi). */
	class DetailKelompokKegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris rincian aspek ({@code data}, harus {@link DetailKelompokKegiatanKedosenan});
		 * lihat dokumentasi kelas {@link DetailKelompokKegiatanKedosenanRenderer} untuk rincian kontrol
		 * yang dibangun.
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) data;

			new Label(detailKelompokKegiatanKedosenan.getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			int i = 1;
			// JANGAN TreeSet: comparator GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0
			// bila sama -> item ber-nomorUrut sama/null saling MENIMPA sehingga tak semua tampil.
			// Pakai List (urut by compareTo, seri tetap dipertahankan) agar SEMUA item muncul.
			java.util.List<JabatanKegiatanKedosenan> jabatanUrutTampil = new java.util.ArrayList<JabatanKegiatanKedosenan>(
					detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans());
			java.util.Collections.sort(jabatanUrutTampil);
			for (JabatanKegiatanKedosenan jabatanKegiatanKedosenan : jabatanUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + jabatanKegiatanKedosenan.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(row);
			i = 1;
			java.util.List<SkalaKegiatanKedosenan> skalaUrutTampil = new java.util.ArrayList<SkalaKegiatanKedosenan>(
					detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans());
			java.util.Collections.sort(skalaUrutTampil);
			for (SkalaKegiatanKedosenan skalaKegiatanKedosenan : skalaUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + skalaKegiatanKedosenan.getNama()));
				i++;
			}

			final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKedosenan.getNomorUrut());
			nomorUrut.setParent(row);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKedosenan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(detailKelompokKegiatanKedosenan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(detailKelompokKegiatanKedosenan.getAktif());
			checkbox.setParent(row);row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKedosenan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKedosenan);
				}
			});

			final MyCheckboxConfig bisaDipilihDosen = new MyCheckboxConfig("Bisa Dipilih Dosen");
			bisaDipilihDosen.setChecked(detailKelompokKegiatanKedosenan.getBisaDipilihDosen());
			bisaDipilihDosen.setParent(row);
			bisaDipilihDosen.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKedosenan.setBisaDipilihDosen(bisaDipilihDosen.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKedosenan);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					init(detailKelompokKegiatanKedosenan);
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

									Common.refreshDelete(detailKelompokKegiatanKedosenan);

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

	/** Memuat ulang grid dengan seluruh {@link DetailKelompokKegiatanKedosenan} milik {@link #kelompokKegiatanKedosenan} yang sedang ditampilkan, terurut nomor urut. Kontrak {@link DataLoader#loadData(Object)}; {@code value} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<DetailKelompokKegiatanKedosenan> detailKelompokKegiatanKedosenan = session
				.createCriteria(DetailKelompokKegiatanKedosenan.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("kelompokKegiatanKedosenan", kelompokKegiatanKedosenan)).list();

		ListModel strset = new SimpleListModel(detailKelompokKegiatanKedosenan);
		grid.setRowRenderer(new DetailKelompokKegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun panel daftar rincian aspek (toolbar tambah + grid berpaging) ke dalam
	 * {@code component}, untuk {@code kelompokKegiatanKedosenan} yang diberikan. Memanggil
	 * {@link #loadData} di akhir untuk mengisi grid.
	 */
	public void displayDetailKelompokKegiatanKedosenan(
			final KelompokKegiatanKedosenan kelompokKegiatanKedosenan, final Component component) {
		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Rincian Aspek", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event event) throws Exception {
				init(new DetailKelompokKegiatanKedosenan());
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
		column.setLabel("Rincian Aspek");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skala");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Urut");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bisa Dipilih Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	/**
	 * Membangun dan menampilkan jendela modal "Pendataan Rincian Aspek" untuk menambah
	 * ({@code detailKelompokKegiatanKedosenan} baru/belum tersimpan) atau mengedit satu rincian
	 * aspek: nama, nomor urut (default otomatis nomor urut tertinggi + 1 pada kelompok yang sama
	 * untuk data baru), serta dua daftar checkbox untuk memilih
	 * {@link JabatanKegiatanKedosenan}/{@link SkalaKegiatanKedosenan} yang berlaku (hanya opsi
	 * aktif yang ditawarkan). Lihat javadoc kelas untuk alasan pemilihan dikelola lewat
	 * {@code LinkedHashMap} per-id, bukan koleksi entitas langsung. Validasi: nama tidak boleh
	 * kosong sebelum disimpan. Menyimpan lewat {@link Common#refreshSaveOrUpdate} lalu memuat
	 * ulang grid pemanggil ({@link #loadData}).
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void init(final DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan) throws Exception {
		final MyWindow window = new MyWindow("Pendataan Rincian Aspek", "normal", false);
		window.setHeight("95%");
		window.setWidth("550px");

		Component component = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		window.setParent(component);

		window.setPosition("center");

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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Aspek"));
		final Textbox nama = new Textbox(detailKelompokKegiatanKedosenan.getNama());
		row.appendChild(nama);
		nama.setWidth("90%");
		nama.setRows(3);

		Session session = HibernateUtil.currentSession();
		Number urut = detailKelompokKegiatanKedosenan.getId() != null
				? detailKelompokKegiatanKedosenan.getNomorUrut()
				: ((Number) session.createCriteria(DetailKelompokKegiatanKedosenan.class)
						.add(Restrictions.eq("kelompokKegiatanKedosenan", kelompokKegiatanKedosenan))
						.setProjection(Projections.max("nomorUrut")).uniqueResult());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKedosenan.getId() != null
				? detailKelompokKegiatanKedosenan.getNomorUrut() : (urut == null ? 1 : urut.intValue() + 1));
		row.appendChild(nomorUrut);
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Jabatan/Status/Tugas"));
		subColumns.appendChild(new Column("Skala"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		if (detailKelompokKegiatanKedosenan.getId() != null) {
			session.refresh(detailKelompokKegiatanKedosenan);
		}

		// Pilihan dikelola per-ID (LinkedHashMap), BUKAN TreeSet bawaan koleksi entitas: comparator
		// GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0 bila sama -> mencentang dua
		// item ber-nomorUrut sama membuat yang kedua DITOLAK TreeSet.add() sehingga hilang saat disimpan.
		final java.util.LinkedHashMap<Long, JabatanKegiatanKedosenan> pilihJabatan = new java.util.LinkedHashMap<Long, JabatanKegiatanKedosenan>();
		for (JabatanKegiatanKedosenan jx : detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans()) {
			if (jx != null && jx.getId() != null) {
				pilihJabatan.put(jx.getId(), jx);
			}
		}
		final java.util.LinkedHashMap<Long, SkalaKegiatanKedosenan> pilihSkala = new java.util.LinkedHashMap<Long, SkalaKegiatanKedosenan>();
		for (SkalaKegiatanKedosenan sx : detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans()) {
			if (sx != null && sx.getId() != null) {
				pilihSkala.put(sx.getId(), sx);
			}
		}

		List<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = session
				.createCriteria(JabatanKegiatanKedosenan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		List<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = session
				.createCriteria(SkalaKegiatanKedosenan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		Vbox vboxJabatan = new Vbox();
		vboxJabatan.setPack("top");
		vboxJabatan.setParent(subRow);
		for (final JabatanKegiatanKedosenan jabatanKegiatanKedosenan : jabatanKegiatanKedosenans) {
			final Checkbox checkbox = new Checkbox(jabatanKegiatanKedosenan.getNama());
			checkbox.setParent(vboxJabatan);
			checkbox.setChecked(jabatanKegiatanKedosenan.getId() != null && pilihJabatan.containsKey(jabatanKegiatanKedosenan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihJabatan.put(jabatanKegiatanKedosenan.getId(), jabatanKegiatanKedosenan);
					} else {
						pilihJabatan.remove(jabatanKegiatanKedosenan.getId());
					}
				}
			});
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final SkalaKegiatanKedosenan skalaKegiatanKedosenan : skalaKegiatanKedosenans) {
			final Checkbox checkbox = new Checkbox(skalaKegiatanKedosenan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(skalaKegiatanKedosenan.getId() != null && pilihSkala.containsKey(skalaKegiatanKedosenan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihSkala.put(skalaKegiatanKedosenan.getId(), skalaKegiatanKedosenan);
					} else {
						pilihSkala.remove(skalaKegiatanKedosenan.getId());
					}
				}
			});
		}

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
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

				if (nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, rincian aspek belum diisi. Langkah yang dapat dilakukan: (1) isi nama rincian aspek pada kolom yang tersedia; (2) pastikan nama tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				detailKelompokKegiatanKedosenan.setNama(nama.getValue().trim());
				detailKelompokKegiatanKedosenan.setNomorUrut(nomorUrut.getValue());
				detailKelompokKegiatanKedosenan.setKelompokKegiatanKedosenan(kelompokKegiatanKedosenan);
				// Simpan sebagai LinkedHashSet (bukan TreeSet) -> tak ada penciutan by nomorUrut.
				detailKelompokKegiatanKedosenan.setSkalaKegiatanKedosenans(
						new java.util.LinkedHashSet<SkalaKegiatanKedosenan>(pilihSkala.values()));
				detailKelompokKegiatanKedosenan.setJabatanKegiatanKedosenans(
						new java.util.LinkedHashSet<JabatanKegiatanKedosenan>(pilihJabatan.values()));
				Common.refreshSaveOrUpdate(detailKelompokKegiatanKedosenan);

				window.detach();

				loadData(null);

			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}
}
