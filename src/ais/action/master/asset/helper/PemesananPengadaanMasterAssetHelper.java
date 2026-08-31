package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.PenerimaanPengadaanMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK modul aset untuk mengelola daftar barang/jasa yang dipesan pada satu transaksi
 * {@link PemesananPengadaanMasterAsset}, lewat baris {@link PemesananPengadaanMasterAssetDetail}
 * (kuantitas, harga beli, diskon, pajak PPN/PPH, keterangan). Panel menyesuaikan diri terhadap
 * tiga mode: edit biasa, {@code persetujuan} (read-only, kolom diganti label), dan
 * {@code pembelianLangsung} (menyembunyikan kolom rincian harga/diskon/pajak, hanya menampilkan
 * kolom ringkas — total 7 kolom, bukan 12).
 *
 * <p>
 * Tombol "Ambil Data Barang/Jasa" membuka dialog {@code AmbilDataMasterAssetBanyak} (pemilih
 * aset multi-pilih, dengan aset yang sudah ada di daftar dikecualikan), hanya tampak bila belum
 * disetujui dan checkbox {@code tampaPermintaan} dicentang (menandakan pemesanan tanpa permintaan
 * pengadaan formal terlebih dulu). Setiap baris menghitung ulang PPN/PPH/total lewat method
 * entitas ({@code hitungPpn}/{@code hitungPph}/{@code getHargaTotal}) tiap kali salah satu field
 * berubah, dan mengubah harga beli default pada {@link MasterAsset} induk juga ikut diperbarui
 * (lewat timer default). {@link #eventListenerHitungUlang} menjumlahkan seluruh baris menjadi
 * total keseluruhan, ditampilkan di baris kaki tabel dan disinkronkan ke kolom {@code nilai}
 * pada {@link PemesananPengadaanMasterAsset} induk bila berbeda. Baris juga menampilkan tautan
 * riwayat revisi berjenjang (ke permintaan pengadaan dan/atau penerimaan pengadaan asal, bila
 * ada) serta unggah/unduh gambar aset lewat {@link LampiranLain}.
 * </p>
 */
public class PemesananPengadaanMasterAssetHelper {

	private MyGrid gridMasterAsset;

	private boolean edit = true;
	private boolean delete = true;
	private Double totalSemua = 0.0;

	private Footer footerTotalSemua;

	private boolean persetujuan = false;

	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = null;

	private boolean pembelianLangsung;

	/** Membangun helper terikat pada {@code gridMasterAsset}; hak edit/hapus dan konteks transaksi ditentukan penuh saat {@link #initDetail} dipanggil. */
	public PemesananPengadaanMasterAssetHelper(MyGrid gridMasterAsset) {
		this.gridMasterAsset = gridMasterAsset;
	}

	/**
	 * Membangun panel (groupbox) "Daftar Pemesanan Barang/Jasa" untuk
	 * {@code pemesananPengadaanMasterAsset}: toolbar (Ambil Data, Refresh, History — tampil
	 * sesuai kondisi), grid kolom yang menyesuaikan mode {@code pembelianLangsung}, dan baris
	 * kaki total. Hak edit/hapus dihitung dari status persetujuan transaksi.
	 *
	 * @param pemesananPengadaanMasterAsset transaksi pemesanan yang menjadi konteks detail
	 * @param persetujuan                   bila {@code true}, panel dalam mode read-only (dipaksa juga bila {@code pembelianLangsung})
	 * @param pembelianLangsung              bila {@code true}, kolom rincian harga/diskon/pajak disembunyikan dan panel dipaksa read-only
	 * @param tampaPermintaan                checkbox penanda "tanpa permintaan pengadaan formal", mengendalikan visibilitas tombol tambah
	 * @return groupbox siap disisipkan sebagai konten form pemesanan pengadaan
	 */
	public MyGroupboxStyled initDetail(final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset,
			final boolean persetujuan, boolean pembelianLangsung, final MyCheckboxConfig tampaPermintaan)
			throws Exception {
		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
		this.persetujuan = persetujuan || pembelianLangsung;
		this.pembelianLangsung = pembelianLangsung;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Pemesanan Barang/Jasa"));

		edit = pemesananPengadaanMasterAsset.getDisetujuiOleh() == null;
		delete = pemesananPengadaanMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!this.persetujuan);
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);
		final MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/new.gif");
		add.setVisible(pemesananPengadaanMasterAsset.getDisetujuiOleh() == null && tampaPermintaan.isChecked());
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
				List<Row> myrows = gridMasterAsset.getRows().getChildren();
				for (Row row : myrows) {
					masterAssets.add(((PemesananPengadaanMasterAssetDetail) row
							.getAttribute("pemesananPengadaanMasterAssetDetail")).getMasterAsset());
				}
				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ambilDataMasterAssetBanyak.setHeight("95%");
				ambilDataMasterAssetBanyak.setWidth("90%");
				ambilDataMasterAssetBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						for (MasterAsset masterAsset : masterAssets) {
							PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = new PemesananPengadaanMasterAssetDetail();
							pemesananPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
							pemesananPengadaanMasterAssetDetail.setJumlah(1.0);
							pemesananPengadaanMasterAssetDetail.setKeterangan("");
							pemesananPengadaanMasterAssetDetail
									.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);

							if (pemesananPengadaanMasterAsset.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(pemesananPengadaanMasterAssetDetail);
							}

							Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
							rows.setParent(gridMasterAsset);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, pemesananPengadaanMasterAssetDetail);
						}
						eventListenerHitungUlang.onEvent(arg0);
					}
				});

				ambilDataMasterAssetBanyak.onModal();

			}
		});

		tampaPermintaan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				add.setVisible(pemesananPengadaanMasterAsset.getDisetujuiOleh() == null && tampaPermintaan.isChecked());
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setVisible(pemesananPengadaanMasterAsset.getId() != null);
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(pemesananPengadaanMasterAsset);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(pemesananPengadaanMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPemesananPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPemesananPengadaanMasterAssetDetailHelper(
						pemesananPengadaanMasterAsset, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(pemesananPengadaanMasterAsset);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		Div scrollDiv = new Div();
		scrollDiv.setStyle("overflow-x:auto; width:100%;");
		scrollDiv.setParent(myGroupboxStyled);
		gridMasterAsset.setParent(scrollDiv);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:350px; min-width:1100px;");
		gridMasterAsset.setMold("paging");
		gridMasterAsset.setPageSize(100);
		gridMasterAsset.getPagingChild().setMold("os");

		gridMasterAsset.setAttribute("janganDisabled", true);

		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);

		// Lebar kolom dirancang agar teks tidak terpotong di min-width 1100px.
		// !pembelianLangsung (12 kolom): 18+5+9+6+6+7+10+8+8+10+10+3 = 100%
		// pembelianLangsung  (7  kolom): 28+6+12+12+16+21+5           = 100%

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth(pembelianLangsung ? "28%" : "18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth(pembelianLangsung ? "6%" : "5%");

		if (!pembelianLangsung) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Harga");
			column.setAlign("right");
			column.setWidth("9%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Persen");
			column.setAlign("right");
			column.setWidth("6%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Diskon");
			column.setAlign("right");
			column.setWidth("6%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPN");
			column.setWidth("7%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPH");
			column.setWidth("10%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPN");
		column.setAlign("right");
		column.setWidth(pembelianLangsung ? "12%" : "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPH");
		column.setAlign("right");
		column.setWidth(pembelianLangsung ? "12%" : "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth(pembelianLangsung ? "16%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth(pembelianLangsung ? "21%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(pembelianLangsung ? "5%" : "3%");

		footerTotalSemua = new Footer(Common.numberFormat1.get().format(totalSemua));

		Foot foot = new Foot();
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer("Total");
		foot.appendChild(footer);

		if (!pembelianLangsung) {

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

		}

		footer = new Footer();
		foot.appendChild(footer);
		
		footer = new Footer();
		foot.appendChild(footer);


		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);
		footer = new Footer();
		foot.appendChild(footer);

		loadDataDetail(pemesananPengadaanMasterAsset);

		return myGroupboxStyled;
	}

	/** Menjumlahkan ulang total semua baris yang tampil di grid, menyinkronkannya ke kolom {@code nilai} transaksi induk (bila berbeda dan sudah tersimpan), dan memperbarui label total di kaki tabel. */
	public EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridMasterAsset.getRows().getChildren();

			totalSemua = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = (PemesananPengadaanMasterAssetDetail) row
							.getAttribute("pemesananPengadaanMasterAssetDetail");

					Double j = pemesananPengadaanMasterAssetDetail.getHargaTotal();

					totalSemua += j;
				}
			}

			if (pemesananPengadaanMasterAsset.getId() != null
					&& pemesananPengadaanMasterAsset.getNilai().intValue() != totalSemua.intValue()) {
				pemesananPengadaanMasterAsset.setNilai(totalSemua);
				Common.refreshUpdate(pemesananPengadaanMasterAsset);
			}

			footerTotalSemua.setLabel(Common.numberFormat1.get().format(totalSemua));
		}
	};

	/** Memuat baris-baris pemesanan tersimpan untuk {@code pemesananPengadaanMasterAsset} dari database, merendernya ke grid, lalu menghitung ulang total lewat timer default. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) throws Exception {

		List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = pemesananPengadaanMasterAsset == null
				|| pemesananPengadaanMasterAsset.getId() == null
						? new ArrayList<PemesananPengadaanMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PemesananPengadaanMasterAssetDetail.class)
								.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
								.list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		Common.clear(rows);

		for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pemesananPengadaanMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	/**
	 * Mengisi {@code row} dengan tautan riwayat revisi berjenjang (aset, permintaan pengadaan
	 * asal, penerimaan pengadaan asal — bila ada), unggah/unduh gambar aset, dan kolom
	 * kuantitas/harga beli/diskon/pajak PPN-PPH/keterangan — masing-masing dirender sebagai
	 * komponen editable (bila bukan mode persetujuan dan berhak ubah) atau label read-only (mode
	 * persetujuan/kolom disembunyikan pada pembelian langsung), dengan kolom rincian
	 * harga/diskon/pajak seluruhnya disembunyikan pada mode {@code pembelianLangsung}. Perubahan
	 * pada field manapun memicu penghitungan ulang PPN/PPH/total baris dan total keseluruhan,
	 * serta memperbarui harga beli default pada {@link MasterAsset} induk. Tombol hapus (bila
	 * pengguna berhak dan bukan mode persetujuan) meminta konfirmasi sebelum menghapus baris.
	 */
	public void initRow(final Row row, final PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail)
			throws Exception {

		row.setValign("top");
		row.setAttribute("pemesananPengadaanMasterAssetDetail", pemesananPengadaanMasterAssetDetail);

		final Label total = new MyLabelKecil(
				Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));

		final MyDoublebox jumlah = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getJumlah());

		final MyDoublebox hargaBeli = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getHargaBeli());
		final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
		diskonDalamBentukPersen.setChecked(pemesananPengadaanMasterAssetDetail.getDiskonDalamBentukPersen());

		final MyDoublebox hargaPotongan = new MyDoublebox(pemesananPengadaanMasterAssetDetail.getHargaPotongan());

		final Combobox persenPpn = new Combobox();
		Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class, "Tanpa Pajak",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPpn, pemesananPengadaanMasterAssetDetail.getJenisPajakPpn());

		final Combobox persenPph = new Combobox();
		Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan", JenisPajakBarang.class,
				"Tanpa Pajak", Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPph, pemesananPengadaanMasterAssetDetail.getJenisPajakBarang());

		final MyTextbox keterangan = new MyTextbox(pemesananPengadaanMasterAssetDetail.getKeterangan());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pemesananPengadaanMasterAssetDetail.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
				pemesananPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
				pemesananPengadaanMasterAssetDetail.setJumlah(jumlah.getValue());
				pemesananPengadaanMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
				pemesananPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());

				pemesananPengadaanMasterAssetDetail
						.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
								: persenPph.getSelectedItem().getValue()));
				pemesananPengadaanMasterAssetDetail
						.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
								: persenPpn.getSelectedItem().getValue()));

				row.setValign("top");
				row.setAttribute("pemesananPengadaanMasterAssetDetail", pemesananPengadaanMasterAssetDetail);
				if (pemesananPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(pemesananPengadaanMasterAssetDetail);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						MasterAsset masterAsset = pemesananPengadaanMasterAssetDetail.getMasterAsset();
						session.refresh(masterAsset);
						masterAsset.setHargaBeliDefault(hargaBeli.getValue());
						Common.refreshUpdate(session, masterAsset);

						total.setValue(Common.numberFormat1.get().format(pemesananPengadaanMasterAssetDetail.getHargaTotal()));
						eventListenerHitungUlang.onEvent(null);
					}
				});

			}
		};

		Vbox a;
		(a = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAssetDetail.class,
				pemesananPengadaanMasterAssetDetail, pemesananPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
						: pemesananPengadaanMasterAssetDetail.getMasterAsset().getNama()))
				.setParent(row);
		if (pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() != null) {
			RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
					pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset(),
					pemesananPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset().getKode())
					.setParent(a);
		}
		if (pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail() != null
				&& pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null) {

			A aa = new A(pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getKode());
			aa.setParent(a);
			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PenerimaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, pemesananPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset());

				}
			});
			aa.setStyle("font-size:9px;");
		}

		new Label(pemesananPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
				: pemesananPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(a);

		Hbox hbox = new Hbox();
		hbox.setParent(a);

		LampiranLain.createDownloadUploadFileLain(hbox,
				pemesananPengadaanMasterAssetDetail.getMasterAsset() == null ? null
						: pemesananPengadaanMasterAssetDetail.getMasterAsset().getId(),
				LampiranLain.GAMBAR_MASTER_ASSET, "Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, !persetujuan, null, false, true);

		if (persetujuan) {
			new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getJumlah()))
					.setParent(row);
		} else {
			(jumlah).setParent(row);
		}
		jumlah.setDisabled(
				pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener("onChange", eventListener);

		if (!pembelianLangsung) {
			if (persetujuan || !pemesananPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah()) {
				new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getHargaBeli()))
						.setParent(row);
			} else {
				(hargaBeli).setParent(row);
			}
			hargaBeli.setDisabled(
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			hargaBeli.setStyle("text-align:right");
			hargaBeli.setWidth("90%");
			hargaBeli.addEventListener("onChange", eventListener);

			if (persetujuan) {
				new Label(pemesananPengadaanMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak")
						.setParent(row);
			} else {
				(diskonDalamBentukPersen).setParent(row);
			}

			diskonDalamBentukPersen.setDisabled(
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			diskonDalamBentukPersen.addEventListener("onClick", eventListener);

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.getHargaPotongan()))
						.setParent(row);
			} else {
				(hargaPotongan).setParent(row);
			}
			hargaPotongan.setDisabled(
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			hargaPotongan.setStyle("text-align:right");
			hargaPotongan.setWidth("90%");
			hargaPotongan.addEventListener("onChange", eventListener);

			if (persetujuan) {
				new Label(pemesananPengadaanMasterAssetDetail.getJenisPajakPpn() == null ? ""
						: pemesananPengadaanMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
			} else {
				(persenPpn).setParent(row);
			}

			persenPpn.setDisabled(
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			persenPpn.setStyle("text-align:right");
			persenPpn.setWidth("90%");
			persenPpn.addEventListener("onChange", eventListener);

			if (persetujuan) {
				new Label(pemesananPengadaanMasterAssetDetail.getJenisPajakBarang() == null ? ""
						: pemesananPengadaanMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
			} else {
				(persenPph).setParent(row);
			}
			persenPph.setDisabled(
					pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			persenPph.setStyle("text-align:right");
			persenPph.setWidth("90%");
			persenPph.addEventListener("onChange", eventListener);

		}
		new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.hitungPpn())).setParent(row);

		new Label(Common.numberFormat.get().format(pemesananPengadaanMasterAssetDetail.hitungPph())).setParent(row);

		total.setStyle("text-align:right");
		total.setParent(row);

		keterangan.setWidth("90%");
		keterangan.setHeight("95%");

		if (persetujuan) {
			new MyLabelKecil(Common.simpleString(pemesananPengadaanMasterAssetDetail.getKeterangan())).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.setDisabled(
				pemesananPengadaanMasterAssetDetail.getPemesananPengadaanMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		keterangan.addEventListener("onChange", eventListener);

		hbox = new Hbox();
		hbox.setParent(row);
		if (!persetujuan) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setParent(hbox);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										if (pemesananPengadaanMasterAssetDetail.getId() != null) {
											Session session = HibernateUtil.currentSession();
											session.delete(pemesananPengadaanMasterAssetDetail);
										}
										row.setVisible(false);
										row.detach();

										eventListenerHitungUlang.onEvent(null);
									}

								}
							});

				}
			});
		}
	}

}
