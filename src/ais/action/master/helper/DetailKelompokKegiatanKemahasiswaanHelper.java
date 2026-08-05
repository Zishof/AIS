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
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailKelompokKegiatanKemahasiswaanHelper implements DataLoader {

	private MyGrid grid;
	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan;

	class DetailKelompokKegiatanKemahasiswaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = (DetailKelompokKegiatanKemahasiswaan) data;

			new Label(detailKelompokKegiatanKemahasiswaan.getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			int i = 1;
			// JANGAN TreeSet: comparator GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0
			// bila sama -> item ber-nomorUrut sama/null saling MENIMPA sehingga tak semua tampil.
			// Pakai List (urut by compareTo, seri tetap dipertahankan) agar SEMUA item muncul.
			java.util.List<JabatanKegiatanKemahasiswaan> jabatanUrutTampil = new java.util.ArrayList<JabatanKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans());
			java.util.Collections.sort(jabatanUrutTampil);
			for (JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan : jabatanUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + jabatanKegiatanKemahasiswaan.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(row);
			i = 1;
			java.util.List<SkalaKegiatanKemahasiswaan> skalaUrutTampil = new java.util.ArrayList<SkalaKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans());
			java.util.Collections.sort(skalaUrutTampil);
			for (SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan : skalaUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + skalaKegiatanKemahasiswaan.getNama()));
				i++;
			}

			final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKemahasiswaan.getNomorUrut());
			nomorUrut.setParent(row);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKemahasiswaan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(detailKelompokKegiatanKemahasiswaan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(detailKelompokKegiatanKemahasiswaan.getAktif());
			checkbox.setParent(row);row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKemahasiswaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKemahasiswaan);
				}
			});

			final MyCheckboxConfig bisaDipilihMahasiswa = new MyCheckboxConfig("Bisa Dipilih Mahasiswa");
			bisaDipilihMahasiswa.setChecked(detailKelompokKegiatanKemahasiswaan.getBisaDipilihMahasiswa());
			bisaDipilihMahasiswa.setParent(row);
			bisaDipilihMahasiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKemahasiswaan.setBisaDipilihMahasiswa(bisaDipilihMahasiswa.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKemahasiswaan);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					init(detailKelompokKegiatanKemahasiswaan);
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

									Common.refreshDelete(detailKelompokKegiatanKemahasiswaan);

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

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<DetailKelompokKegiatanKemahasiswaan> detailKelompokKegiatanKemahasiswaan = session
				.createCriteria(DetailKelompokKegiatanKemahasiswaan.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("kelompokKegiatanKemahasiswaan", kelompokKegiatanKemahasiswaan)).list();

		ListModel strset = new SimpleListModel(detailKelompokKegiatanKemahasiswaan);
		grid.setRowRenderer(new DetailKelompokKegiatanKemahasiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailKelompokKegiatanKemahasiswaan(
			final KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan, final Component component) {
		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
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
				init(new DetailKelompokKegiatanKemahasiswaan());
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
		column.setLabel("Bisa Dipilih Mahasiswa");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public void init(final DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan) throws Exception {
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
		final Textbox nama = new Textbox(detailKelompokKegiatanKemahasiswaan.getNama());
		row.appendChild(nama);
		nama.setWidth("90%");
		nama.setRows(3);

		Session session = HibernateUtil.currentSession();
		Number urut = detailKelompokKegiatanKemahasiswaan.getId() != null
				? detailKelompokKegiatanKemahasiswaan.getNomorUrut()
				: ((Number) session.createCriteria(DetailKelompokKegiatanKemahasiswaan.class)
						.add(Restrictions.eq("kelompokKegiatanKemahasiswaan", kelompokKegiatanKemahasiswaan))
						.setProjection(Projections.max("nomorUrut")).uniqueResult());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKemahasiswaan.getId() != null
				? detailKelompokKegiatanKemahasiswaan.getNomorUrut() : (urut == null ? 1 : urut.intValue() + 1));
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

		if (detailKelompokKegiatanKemahasiswaan.getId() != null) {
			session.refresh(detailKelompokKegiatanKemahasiswaan);
		}

		// Pilihan dikelola per-ID (LinkedHashMap), BUKAN TreeSet bawaan koleksi entitas: comparator
		// GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0 bila sama -> mencentang dua
		// item ber-nomorUrut sama membuat yang kedua DITOLAK oleh TreeSet.add() sehingga hilang saat
		// disimpan ("yang tampil tidak sesuai dengan yang diceklis"). Map by id aman dari tabrakan ini.
		final java.util.LinkedHashMap<Long, JabatanKegiatanKemahasiswaan> pilihJabatan = new java.util.LinkedHashMap<Long, JabatanKegiatanKemahasiswaan>();
		for (JabatanKegiatanKemahasiswaan jx : detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans()) {
			if (jx != null && jx.getId() != null) {
				pilihJabatan.put(jx.getId(), jx);
			}
		}
		final java.util.LinkedHashMap<Long, SkalaKegiatanKemahasiswaan> pilihSkala = new java.util.LinkedHashMap<Long, SkalaKegiatanKemahasiswaan>();
		for (SkalaKegiatanKemahasiswaan sx : detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans()) {
			if (sx != null && sx.getId() != null) {
				pilihSkala.put(sx.getId(), sx);
			}
		}

		List<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans = session
				.createCriteria(JabatanKegiatanKemahasiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		List<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans = session
				.createCriteria(SkalaKegiatanKemahasiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		Vbox vboxJabatan = new Vbox();
		vboxJabatan.setPack("top");
		vboxJabatan.setParent(subRow);
		for (final JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan : jabatanKegiatanKemahasiswaans) {
			final Checkbox checkbox = new Checkbox(jabatanKegiatanKemahasiswaan.getNama());
			checkbox.setParent(vboxJabatan);
			checkbox.setChecked(jabatanKegiatanKemahasiswaan.getId() != null
					&& pilihJabatan.containsKey(jabatanKegiatanKemahasiswaan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihJabatan.put(jabatanKegiatanKemahasiswaan.getId(), jabatanKegiatanKemahasiswaan);
					} else {
						pilihJabatan.remove(jabatanKegiatanKemahasiswaan.getId());
					}
				}
			});
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan : skalaKegiatanKemahasiswaans) {
			final Checkbox checkbox = new Checkbox(skalaKegiatanKemahasiswaan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(skalaKegiatanKemahasiswaan.getId() != null
					&& pilihSkala.containsKey(skalaKegiatanKemahasiswaan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihSkala.put(skalaKegiatanKemahasiswaan.getId(), skalaKegiatanKemahasiswaan);
					} else {
						pilihSkala.remove(skalaKegiatanKemahasiswaan.getId());
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
					MyMessageboxConfig.show("Mohon maaf, rincian aspek kegiatan kemahasiswaan belum diisi. Langkah yang dapat dilakukan: (1) isi nama rincian aspek pada kolom yang tersedia; (2) pastikan nama tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				detailKelompokKegiatanKemahasiswaan.setNama(nama.getValue().trim());
				detailKelompokKegiatanKemahasiswaan.setNomorUrut(nomorUrut.getValue());
				detailKelompokKegiatanKemahasiswaan.setKelompokKegiatanKemahasiswaan(kelompokKegiatanKemahasiswaan);
				// Simpan sebagai LinkedHashSet (bukan TreeSet) -> tak ada penciutan by nomorUrut;
				// SEMUA yang dicentang tersimpan sesuai tampilan.
				detailKelompokKegiatanKemahasiswaan.setSkalaKegiatanKemahasiswaans(
						new java.util.LinkedHashSet<SkalaKegiatanKemahasiswaan>(pilihSkala.values()));
				detailKelompokKegiatanKemahasiswaan.setJabatanKegiatanKemahasiswaans(
						new java.util.LinkedHashSet<JabatanKegiatanKemahasiswaan>(pilihJabatan.values()));
				Common.refreshSaveOrUpdate(detailKelompokKegiatanKemahasiswaan);

				window.detach();

				loadData(null);

			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}
}
