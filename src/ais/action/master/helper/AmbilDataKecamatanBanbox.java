package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.KotaAction;
import ais.action.master.NegaraAction;
import ais.action.master.PropinsiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PmbArkatama;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.Negara;
import ais.database.model.Propinsi;
import ais.database.model.Wilayah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Wilayah} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Wilayah} adalah model hierarki wilayah administratif Indonesia (negara &gt; propinsi &gt;
 * kota/kabupaten &gt; kecamatan) yang menyimpan seluruh tingkatan dalam satu tabel self-referencing
 * lewat {@code wilayahInduk}. Nama kelas ini menyiratkan "kecamatan" karena {@code level = "3"}
 * (kecamatan) adalah default constructor tanpa argumen, TAPI kelas ini sebenarnya lebih GENERIK:
 * constructor kedua menerima {@code level} eksplisit sehingga instance yang sama juga dipakai
 * memilih tingkat wilayah lain (mis. level di bawah kecamatan) — perilaku {@code display()}/
 * {@code onSearchDefault()} bercabang berdasar {@code level.equalsIgnoreCase("3")}: untuk level 3
 * field pencarian adalah nama kecamatan + nama kota/kab + nama propinsi (join 2 tingkat ke atas via
 * alias {@code kab}/{@code prop}), untuk level lain field {@code namaKota} berfungsi sebagai nama
 * entity level itu sendiri dan {@code namaProp} sebagai nama satu tingkat di atasnya. Filter
 * tambahan {@code integrasi_pmb_arkatama} (dari {@link ais.common.Common#bolehKonfigurasi}) bila
 * aktif membatasi hasil hanya wilayah dengan {@code keterangan == "PmbArkatama"} — integrasi data
 * wilayah dari sistem PMB pihak ketiga. Level 3 memakai daftar client-side ({@link ais.common.Common#MAX_RESULT});
 * level lain memakai paging server-side {@link ais.ui.util.AmbilDataPagingHelper}. Pemilihan
 * bersifat TUNGGAL (Radiogroup). Khusus level 3 dan bila konfigurasi {@code tambah_kecamatan_baru}
 * aktif, toolbar menampilkan tombol tambah cepat dari {@link #tambah(EventListener)} — modal
 * berjenjang untuk membuat negara/propinsi/kota/kecamatan baru langsung dari popup picker ini.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKecamatanBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private String level = "3";

	/**
	 * Konstruktor default: menetapkan {@link #level} ke {@code "3"} (kecamatan) secara implisit
	 * lewat inisialisasi field, lalu memasang listener {@code onOpen} standar yang membangun popup
	 * pencarian secara lazy pada pembukaan pertama — lihat {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataKecamatanBanbox() {
		super();
		setReadonly(true);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});
	}

	/**
	 * Konstruktor dengan {@code level} eksplisit — parameter tambahan khusus kelas ini yang
	 * membedakannya dari kebanyakan subclass sejenis: mengubah tingkat hierarki {@link Wilayah}
	 * yang dicari (lihat penjelasan cabang level di Javadoc class), BUKAN filter dari entity induk
	 * biasa. Selebihnya mengikuti kerangka constructor standar.
	 *
	 * @param level kode tingkat wilayah yang dicari (mis. {@code "3"} untuk kecamatan); tingkat
	 *              lain mengubah pemetaan field pencarian, lihat {@link #display()} dan
	 *              {@link #onSearchDefault(Event)}
	 */
	public AmbilDataKecamatanBanbox(String level) {
		super();
		this.level = level;
		setReadonly(true);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});
	}

	/** Kriteria pencarian: nama wilayah level ini sendiri (kecamatan, bila {@code level == "3"}). */
	private Textbox nama;
	/** Kriteria pencarian: nama wilayah satu tingkat di atas (kota/kab. bila level 3). */
	private Textbox namaKota;
	/** Kriteria pencarian: nama wilayah dua tingkat di atas (propinsi bila level 3). */
	private Textbox namaProp;
	// private Textbox kode;

	/**
	 * Renderer baris grid hasil pencarian {@link Wilayah}: menampilkan label wilayah induk
	 * (kota/kab.) dan induk-dari-induk (propinsi), plus satu radio button pilihan. Mengikuti
	 * kerangka renderer standar di {@link ais.ui.util.GetEventListener} — listener {@code onCheck}
	 * menutup popup, menyimpan entity terpilih ke atribut {@code "wilayah"} dan teks tampilan
	 * {@code wilayah.getNama()}, lalu meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataKecamatanBanbox
	 */
	class WilayahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Wilayah wilayah = (Wilayah) arg1;
			MyRadioConfig checkbox = new MyRadioConfig(wilayah.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(wilayah.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKecamatanBanbox.this.setOpen(false);
					AmbilDataKecamatanBanbox.this.setAttribute("wilayah", wilayah);
					AmbilDataKecamatanBanbox.this.setValue(wilayah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			Label kab = new Label(wilayah.getWilayahInduk() == null ? "" : wilayah.getWilayahInduk().getNama());
			kab.setParent(arg0);

			Label prop = new Label(
					wilayah.getWilayahInduk() == null || wilayah.getWilayahInduk().getWilayahInduk() == null ? ""
							: wilayah.getWilayahInduk().getWilayahInduk().getNama());
			prop.setParent(arg0);

		}

	}

	/**
	 * Method statis (BUKAN bagian kerangka standar {@link ais.ui.util.GetEventListener}) yang
	 * membangun tombol toolbar untuk membuat {@link Wilayah} kecamatan baru langsung dari popup
	 * picker, tanpa berpindah layar. Menampilkan {@link ais.ui.util.MyWindow} modal berjenjang:
	 * pilih/tambah {@link Negara} → pilih/tambah {@link Propinsi} → pilih/tambah {@link Kota} →
	 * isi kode dan nama kecamatan. Tombol "Xxx Baru" di tiap tingkat memanggil
	 * {@code onAddExternal} action terkait ({@link NegaraAction}, {@link PropinsiAction},
	 * {@link KotaAction}) secara inline. Saat disimpan, mencari dulu apakah {@link Wilayah}
	 * kecamatan dengan nama sama di bawah kota/kab. terpilih sudah ada (hindari duplikat) sebelum
	 * membuat baris baru dengan {@code level = "3"}, lalu memanggil {@code eventListener} yang
	 * diberikan dengan wilayah hasil (baru atau yang sudah ada) sebagai data event.
	 *
	 * @param eventListener listener yang dipanggil dengan {@link Wilayah} hasil setelah tersimpan
	 * @return tombol toolbar siap dipasang sebagai parent dari toolbar pemanggil
	 */
	public static Toolbarbutton tambah(final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Kecamatan Baru", "/img/svg/addthis.svg");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Buat Kecamatan Baru", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("380px");
				window.setWidth("700px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("25%");

				column = new MyColumnConfig();
				column.setParent(columns);

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("25%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Negara *"));
				final Combobox negara;
				row.appendChild(negara = new Combobox());
				Common.insertCombo(negara, "namaNegara", Negara.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				negara.setWidth("90%");
				negara.setReadonly(true);
				MyToolbarbuttonConfig buttonTambah = new MyToolbarbuttonConfig("Negara Baru",
						"/img/svg/addthis.svg");
				buttonTambah.setWidth("90%");
				row.appendChild(buttonTambah);

				buttonTambah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						NegaraAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Negara n = (Negara) arg0.getData();
								if (n != null) {
									Common.selectComboItem(true, negara, n);
									negara.setDisabled(true);
								}
							}
						}, new Negara());
					}
				});

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi *"));
				final Combobox propinsi;
				row.appendChild(propinsi = new Combobox());

				EventListener eventListenerNegara = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Negara n = (Negara) (negara.getSelectedItem() == null ? null
								: negara.getSelectedItem().getValue());
						Common.insertCombo(propinsi, "nama", "negara", Propinsi.class,
								Restrictions.and(
										n == null ? Restrictions.sqlRestriction("false") : Restrictions.eq("negara", n),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					}
				};

				negara.addEventListener("onChange", eventListenerNegara);

				propinsi.setWidth("90%");
				propinsi.setReadonly(true);
				buttonTambah = new MyToolbarbuttonConfig("Propinsi Baru", "/img/svg/addthis.svg");
				buttonTambah.setWidth("90%");
				row.appendChild(buttonTambah);

				buttonTambah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Negara n = (Negara) (negara.getSelectedItem() == null ? null
								: negara.getSelectedItem().getValue());
						if (n == null) {
							MyMessageboxConfig.show("Mohon maaf, negara belum dipilih. Langkah yang dapat dilakukan: (1) pilih negara dari daftar yang tersedia; (2) pastikan data negara sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Propinsi pp = new Propinsi();
						pp.setNegara(n);
						PropinsiAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Propinsi p = (Propinsi) arg0.getData();
								if (p != null) {
									Common.selectComboItem(true, propinsi, p);
									propinsi.setDisabled(true);
								}
							}
						}, pp);
					}
				});

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kota/Kabupaten *"));
				final Combobox kota;
				row.appendChild(kota = new Combobox());

				EventListener eventListenerProponsi = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Propinsi p = (Propinsi) (propinsi.getSelectedItem() == null ? null
								: propinsi.getSelectedItem().getValue());

						if (p != null) {
							p.simpanWilayah();
						}

						Common.insertCombo(kota, "nama", "propinsi", Kota.class,
								Restrictions.and(
										p == null ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("propinsi", p),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

					}
				};
				propinsi.addEventListener("onChange", eventListenerProponsi);

				kota.setWidth("90%");
				kota.setReadonly(true);
				buttonTambah = new MyToolbarbuttonConfig("Kota/Kabupaten Baru", "/img/svg/addthis.svg");
				buttonTambah.setWidth("90%");
				row.appendChild(buttonTambah);

				buttonTambah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Propinsi p = (Propinsi) (propinsi.getSelectedItem() == null ? null
								: propinsi.getSelectedItem().getValue());
						if (p == null) {
							MyMessageboxConfig.show("Mohon maaf, provinsi belum dipilih. Langkah yang dapat dilakukan: (1) pastikan negara sudah dipilih terlebih dahulu; (2) kemudian pilih provinsi dari daftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}
						p.simpanWilayah();
						Kota kk = new Kota();
						kk.setPropinsi(p);
						KotaAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Kota k = (Kota) arg0.getData();
								if (k != null) {
									Common.selectComboItem(true, kota, k);
									kota.setDisabled(true);
								}
							}
						}, kk);
					}
				});

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "1,2");
				row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kecamatan"));
				final Textbox kecamatanKode;
				row.appendChild(kecamatanKode = new Textbox());
				kecamatanKode.setWidth("90%");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "1,2");
				row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kecamatan *"));
				final Textbox kecamatan;
				row.appendChild(kecamatan = new Textbox());
				kecamatan.setWidth("90%");

				South south = new South();
				south.setParent(borderlayout);

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

				MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tambah Kecamatan Baru", "/img/excel.png");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Negara n = (Negara) (negara.getSelectedItem() == null ? null
								: negara.getSelectedItem().getValue());
						if (n == null) {
							MyMessageboxConfig.show("Mohon maaf, negara belum dipilih. Langkah yang dapat dilakukan: (1) pilih negara dari daftar yang tersedia; (2) pastikan data negara sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Propinsi p = (Propinsi) (propinsi.getSelectedItem() == null ? null
								: propinsi.getSelectedItem().getValue());
						if (p == null) {
							MyMessageboxConfig.show("Mohon maaf, provinsi belum dipilih. Langkah yang dapat dilakukan: (1) pastikan negara sudah dipilih terlebih dahulu; (2) kemudian pilih provinsi dari daftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						p.simpanWilayah();

						Kota k = (Kota) (kota.getSelectedItem() == null ? null : kota.getSelectedItem().getValue());
						if (k == null) {
							MyMessageboxConfig.show("Mohon maaf, kota/kabupaten belum dipilih. Langkah yang dapat dilakukan: (1) pastikan provinsi sudah dipilih terlebih dahulu; (2) kemudian pilih kota/kabupaten dari daftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						k.simpanWilayah();

						if (kecamatan.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show("Mohon maaf, nama kecamatan belum diisi. Langkah yang dapat dilakukan: (1) isi nama kecamatan pada kolom yang tersedia; (2) pastikan nama kecamatan tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						Wilayah wilayah = (Wilayah) ConstantValues
								.simpleObject(
										session.createCriteria(Wilayah.class)
												.add(Restrictions.eq("wilayahInduk", k.getWilayah())).add(Restrictions
														.ilike("nama", kecamatan.getValue().trim(), MatchMode.EXACT)),
										Wilayah.class);
						if (wilayah == null) {
							wilayah = new Wilayah();
							wilayah.setInduk(k.getWilayah() == null ? "" : k.getWilayah().getKode());
							wilayah.setKode(kecamatanKode.getValue().trim());
							wilayah.setFeeder(kecamatanKode.getValue().trim());
							wilayah.setNama(kecamatan.getValue().trim());
							wilayah.setWilayahInduk(k.getWilayah());
							wilayah.setNegara(n.getKode());
							wilayah.setLevel("3");
							session.save(wilayah);
							session.flush();
						}

						if (eventListener != null) {
							eventListener.onEvent(new Event("", event.getTarget(), wilayah));
						}

						window.detach();

					}
				});
				print.setParent(toolbar);

				window.setVisible(true);
				window.onModal();

			}
		});

		return button;
	}

	/**
	 * Membangun popup pencarian {@link Wilayah} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form pencarian yang field-nya bercabang menurut {@link #level} (lihat
	 * Javadoc class), tombol Cari, filter toggle {@link ais.ui.util.BanboxFilterToggle}, tombol
	 * tambah cepat {@link #tambah(EventListener)} (khusus level 3 dan konfigurasi
	 * {@code tambah_kecamatan_baru} aktif), dan grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal) dengan paging server-side. Kolom grid dan
	 * lebar disesuaikan untuk tampilan mobile ({@link ais.common.Common#isMobile()}). Mengikuti
	 * kerangka {@code display()} standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	@SuppressWarnings("deprecation")
	public void display() {

		boolean mobile = Common.isMobile();

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth(mobile ? "97%" : "800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		if (level.equalsIgnoreCase("3")) {
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");
			nama.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});

			if (mobile) {
				row = new MyFormRow();
				row.setParent(rows);
			}
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota/Kab."));
		row.appendChild(namaKota = new Textbox());
		namaKota.setWidth("90%");
		namaKota.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		if (mobile) {
			row = new MyFormRow();
			row.setParent(rows);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prop."));
		row.appendChild(namaProp = new Textbox());
		namaProp.setWidth("90%");
		namaProp.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, mobile ? "2" : "6");

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		if (level.equalsIgnoreCase("3")) {
			if (Common.bolehKonfigurasi("tambah_kecamatan_baru", Konfigurasi.TIDAK_AKTIF)) {

				AmbilDataKecamatanBanbox.tambah(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Wilayah wilayah = (Wilayah) arg0.getData();

						AmbilDataKecamatanBanbox.this.setOpen(false);
						AmbilDataKecamatanBanbox.this.setAttribute("wilayah", wilayah);
						AmbilDataKecamatanBanbox.this.setValue(wilayah.getNama());

						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
					}
				}).setParent(toolbar);
			}
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();

		if (level.equalsIgnoreCase("3")) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kecamatan");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kota/Kab.");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Propinsi");
		} else {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kota/Kab.");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Propinsi");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("0px");
			column.setLabel("");
		}

		if (mobile) {
			column.setWidth("0px");
			column.setVisible(false);
		}

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link Wilayah} pada tingkat {@link #level}, dengan cabang query
	 * berbeda untuk level 3 (kecamatan, filter nama+kota+propinsi lewat join alias, hasil
	 * client-side dibatasi {@link ais.common.Common#MAX_RESULT}) vs level lain (filter
	 * {@code namaKota}/{@code namaProp} dipetakan ke nama entity level ini dan induknya, hasil
	 * lewat paging server-side {@link ais.ui.util.AmbilDataPagingHelper#cariDenganCriteria}).
	 * Kedua cabang menghormati filter {@code integrasi_pmb_arkatama} bila konfigurasi terkait
	 * aktif (membatasi ke wilayah dengan {@code keterangan == "PmbArkatama"}), lalu memasang
	 * {@link WilayahRenderer} dan model hasil ke {@link #grid}. Mengikuti kerangka
	 * {@code onSearchDefault} standar — lihat {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari, atau tekan Enter di salah satu field); boleh
	 *              {@code null} saat dipanggil dari {@link #display()}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		boolean integrasi_pmb_arkatama = Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF);
		Session session = HibernateUtil.currentSession();
		List<Wilayah> wilayah = level.equalsIgnoreCase("3")
				? ConstantValues.simpleList(session.createCriteria(Wilayah.class)

						.add(integrasi_pmb_arkatama ? Restrictions.eq("keterangan", PmbArkatama.class.getSimpleName())
								: Restrictions.sqlRestriction("true"))

						.createAlias("wilayahInduk", "kab", Criteria.LEFT_JOIN)
						.createAlias("kab.wilayahInduk", "prop", Criteria.LEFT_JOIN)

						.addOrder(Order.asc("nama")).add(Restrictions.eq("level", level))

						.add(nama.getText().trim().isEmpty() ? Restrictions.isNotNull("wilayahInduk")
								: Restrictions.sqlRestriction("true"))
						.add(nama.getText().trim().isEmpty() ? Restrictions.isNotNull("kab.wilayahInduk")
								: Restrictions.sqlRestriction("true"))

						.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

						.add(namaKota.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kab.nama", namaKota.getText().trim(), MatchMode.ANYWHERE))

						.add(namaProp.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("prop.nama", namaProp.getText().trim(), MatchMode.ANYWHERE))

						.setMaxResults(Common.MAX_RESULT), Wilayah.class)

				: pagingHelper.cariDenganCriteria(session.createCriteria(Wilayah.class)

						.add(integrasi_pmb_arkatama ? Restrictions.eq("keterangan", PmbArkatama.class.getSimpleName())
								: Restrictions.sqlRestriction("true"))

						.createAlias("wilayahInduk", "kab", Criteria.LEFT_JOIN)

						.addOrder(Order.asc("nama")).add(Restrictions.eq("level", level))

						.add(namaKota.getText().trim().isEmpty() ? Restrictions.isNotNull("wilayahInduk")
								: Restrictions.sqlRestriction("true"))

						.add(namaKota.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", namaKota.getText().trim(), MatchMode.ANYWHERE))

						.add(namaProp.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kab.nama", namaProp.getText().trim(), MatchMode.ANYWHERE))

						.setMaxResults(Common.MAX_RESULT), Wilayah.class)

		;

//		System.out.println(wilayah);
		ListModel strset = new SimpleListModel(wilayah);
		grid.setRowRenderer(new WilayahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}
