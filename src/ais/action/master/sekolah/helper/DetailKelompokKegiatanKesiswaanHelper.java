package ais.action.master.sekolah.helper;

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
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailKelompokKegiatanKesiswaanHelper implements DataLoader {

	private MyGrid grid;
	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan;

	class DetailKelompokKegiatanKesiswaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan = (DetailKelompokKegiatanKesiswaan) data;

			new Label(detailKelompokKegiatanKesiswaan.getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			int i = 1;
			// JANGAN TreeSet: comparator GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0
			// bila sama -> item ber-nomorUrut sama/null saling MENIMPA sehingga tak semua tampil.
			// Pakai List (urut by compareTo, seri tetap dipertahankan) agar SEMUA item muncul.
			java.util.List<JabatanKegiatanKesiswaan> jabatanUrutTampil = new java.util.ArrayList<JabatanKegiatanKesiswaan>(
					detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans());
			java.util.Collections.sort(jabatanUrutTampil);
			for (JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan : jabatanUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + jabatanKegiatanKesiswaan.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(row);
			i = 1;
			java.util.List<SkalaKegiatanKesiswaan> skalaUrutTampil = new java.util.ArrayList<SkalaKegiatanKesiswaan>(
					detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans());
			java.util.Collections.sort(skalaUrutTampil);
			for (SkalaKegiatanKesiswaan skalaKegiatanKesiswaan : skalaUrutTampil) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + skalaKegiatanKesiswaan.getNama()));
				i++;
			}

			final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKesiswaan.getNomorUrut());
			nomorUrut.setParent(row);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKesiswaan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(detailKelompokKegiatanKesiswaan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(detailKelompokKegiatanKesiswaan.getAktif());
			checkbox.setParent(row);row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKesiswaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKesiswaan);
				}
			});

			final MyCheckboxConfig bisaDipilihSiswa = new MyCheckboxConfig("Bisa Dipilih Siswa");
			bisaDipilihSiswa.setChecked(detailKelompokKegiatanKesiswaan.getBisaDipilihSiswa());
			bisaDipilihSiswa.setParent(row);
			bisaDipilihSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailKelompokKegiatanKesiswaan.setBisaDipilihSiswa(bisaDipilihSiswa.isChecked());
					Common.refreshSaveOrUpdate(detailKelompokKegiatanKesiswaan);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					init(detailKelompokKegiatanKesiswaan);
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

									Common.refreshDelete(detailKelompokKegiatanKesiswaan);

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
													+ e.getMessage());
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
		List<DetailKelompokKegiatanKesiswaan> detailKelompokKegiatanKesiswaan = session
				.createCriteria(DetailKelompokKegiatanKesiswaan.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("kelompokKegiatanKesiswaan", kelompokKegiatanKesiswaan)).list();

		ListModel strset = new SimpleListModel(detailKelompokKegiatanKesiswaan);
		grid.setRowRenderer(new DetailKelompokKegiatanKesiswaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailKelompokKegiatanKesiswaan(
			final KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan, final Component component) {
		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
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
				init(new DetailKelompokKegiatanKesiswaan());
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
		column.setLabel("Bisa Dipilih Siswa");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public void init(final DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan) throws Exception {
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
		final Textbox nama = new Textbox(detailKelompokKegiatanKesiswaan.getNama());
		row.appendChild(nama);
		nama.setWidth("90%");
		nama.setRows(3);

		Session session = HibernateUtil.currentSession();
		Number urut = detailKelompokKegiatanKesiswaan.getId() != null
				? detailKelompokKegiatanKesiswaan.getNomorUrut()
				: ((Number) session.createCriteria(DetailKelompokKegiatanKesiswaan.class)
						.add(Restrictions.eq("kelompokKegiatanKesiswaan", kelompokKegiatanKesiswaan))
						.setProjection(Projections.max("nomorUrut")).uniqueResult());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		final Intbox nomorUrut = new Intbox(detailKelompokKegiatanKesiswaan.getId() != null
				? detailKelompokKegiatanKesiswaan.getNomorUrut() : (urut == null ? 1 : urut.intValue() + 1));
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

		if (detailKelompokKegiatanKesiswaan.getId() != null) {
			session.refresh(detailKelompokKegiatanKesiswaan);
		}

		// Pilihan dikelola per-ID (LinkedHashMap), BUKAN TreeSet bawaan koleksi entitas: comparator
		// GeneralValueObject mengurutkan by nomorUrut & mengembalikan 0 bila sama -> mencentang dua
		// item ber-nomorUrut sama membuat yang kedua DITOLAK TreeSet.add() sehingga hilang saat disimpan.
		final java.util.LinkedHashMap<Long, JabatanKegiatanKesiswaan> pilihJabatan = new java.util.LinkedHashMap<Long, JabatanKegiatanKesiswaan>();
		for (JabatanKegiatanKesiswaan jx : detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans()) {
			if (jx != null && jx.getId() != null) {
				pilihJabatan.put(jx.getId(), jx);
			}
		}
		final java.util.LinkedHashMap<Long, SkalaKegiatanKesiswaan> pilihSkala = new java.util.LinkedHashMap<Long, SkalaKegiatanKesiswaan>();
		for (SkalaKegiatanKesiswaan sx : detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans()) {
			if (sx != null && sx.getId() != null) {
				pilihSkala.put(sx.getId(), sx);
			}
		}

		List<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans = session
				.createCriteria(JabatanKegiatanKesiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		List<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans = session
				.createCriteria(SkalaKegiatanKesiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).list();

		Vbox vboxJabatan = new Vbox();
		vboxJabatan.setPack("top");
		vboxJabatan.setParent(subRow);
		for (final JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan : jabatanKegiatanKesiswaans) {
			final Checkbox checkbox = new Checkbox(jabatanKegiatanKesiswaan.getNama());
			checkbox.setParent(vboxJabatan);
			checkbox.setChecked(jabatanKegiatanKesiswaan.getId() != null && pilihJabatan.containsKey(jabatanKegiatanKesiswaan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihJabatan.put(jabatanKegiatanKesiswaan.getId(), jabatanKegiatanKesiswaan);
					} else {
						pilihJabatan.remove(jabatanKegiatanKesiswaan.getId());
					}
				}
			});
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final SkalaKegiatanKesiswaan skalaKegiatanKesiswaan : skalaKegiatanKesiswaans) {
			final Checkbox checkbox = new Checkbox(skalaKegiatanKesiswaan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(skalaKegiatanKesiswaan.getId() != null && pilihSkala.containsKey(skalaKegiatanKesiswaan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						pilihSkala.put(skalaKegiatanKesiswaan.getId(), skalaKegiatanKesiswaan);
					} else {
						pilihSkala.remove(skalaKegiatanKesiswaan.getId());
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
					MyMessageboxConfig.show("Rincian aspek harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				detailKelompokKegiatanKesiswaan.setNama(nama.getValue().trim());
				detailKelompokKegiatanKesiswaan.setNomorUrut(nomorUrut.getValue());
				detailKelompokKegiatanKesiswaan.setKelompokKegiatanKesiswaan(kelompokKegiatanKesiswaan);
				// Simpan sebagai LinkedHashSet (bukan TreeSet) -> tak ada penciutan by nomorUrut.
				detailKelompokKegiatanKesiswaan.setSkalaKegiatanKesiswaans(
						new java.util.LinkedHashSet<SkalaKegiatanKesiswaan>(pilihSkala.values()));
				detailKelompokKegiatanKesiswaan.setJabatanKegiatanKesiswaans(
						new java.util.LinkedHashSet<JabatanKegiatanKesiswaan>(pilihJabatan.values()));
				Common.refreshSaveOrUpdate(detailKelompokKegiatanKesiswaan);

				window.detach();

				loadData(null);

			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}
}
