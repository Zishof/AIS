package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.inventory.helper.AmbilDataProdukBanyak;
import ais.action.master.inventory.helper.ProdukUtil;
import ais.action.master.koperasi.helper.AmbilDataAnggotaKoperasiBanbox;
import ais.action.report.format1.koperasi.LaporanBuktiPembelianAnggotaKoperasiWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.action.master.koperasi.helper.PembatalanTransaksiUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.sirs.Pembayaran;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDiv;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PembelianAnggotaKoperasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchmr;

	private MyTextbox kode;
	private AmbilDataAnggotaKoperasiBanbox anggotaKoperasi;

	private MyTextbox keterangan;
	private MyDatebox tanggalPembayaran;

	private MyDoublebox biaya;

	private MyDoublebox totalBiaya;

	private MyDoublebox bayarTunai;
	private MyDoublebox bayarNonTunai;
	private MyDoublebox kembalian;

	private Combobox jenisBiayaLain;

	private Label nama;
//	private Label umur;
//	private Label tglLahir;
	private Label alamat;
//	private Label ttl;
	private Combobox jenisAnggotaKoperasi;
//	private Label jenisKelamin;
//	private Label jenis;
//	private Label bed;

	private boolean edit = false;
	private boolean delete = false;

	private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;
	private Toolbarbutton add;

	private Lokasi myLokasi;

	private PembelianAnggotaKoperasiDetailAction pembelianAnggotaKoperasiDetailAction;

	public ArrayList<Pembelian> pembelianAnggotaKoperasiDetails = new ArrayList<Pembelian>();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		myLokasi = Common.getCurrentLokasi();

		add = new ais.ui.util.MyToolbarbuttonConfig("Pembelian Baru", "/img/svg/addthis.svg");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pembelianAnggotaKoperasiDetails = null;
				pembelianAnggotaKoperasiDetails = new ArrayList<Pembelian>();

				init(new PembelianAnggotaKoperasi());
			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		init(new PembelianAnggotaKoperasi());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PembelianAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			final PembelianAnggotaKoperasi pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) arg1;

			AnggotaKoperasi anggotaKoperasi = pembelianAnggotaKoperasi.getAnggotaKoperasi();

			new Label(pembelianAnggotaKoperasi.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(PembelianAnggotaKoperasi.class, pembelianAnggotaKoperasi,
					anggotaKoperasi == null ? "" : anggotaKoperasi.getNama()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(pembelianAnggotaKoperasi.getTanggalPembayaran())).setParent(arg0);

			new Label(anggotaKoperasi == null ? "" : anggotaKoperasi.getAlamat()).setParent(arg0);

			new Label(pembelianAnggotaKoperasi.getTotalBiaya() == null ? "0.0"
					: Common.numberFormat.get().format(pembelianAnggotaKoperasi.getTotalBiaya())).setParent(arg0);

			new Label(pembelianAnggotaKoperasi.getBayarTunai() == null ? "0.0"
					: Common.numberFormat.get().format(pembelianAnggotaKoperasi.getBayarTunai())).setParent(arg0);
			new Label(pembelianAnggotaKoperasi.getBayarNonTunai() == null ? "0.0"
					: Common.numberFormat.get().format(pembelianAnggotaKoperasi.getBayarNonTunai())).setParent(arg0);
			new Label(Common.numberFormat.get().format((pembelianAnggotaKoperasi.getBayar()))).setParent(arg0);

			new Label(pembelianAnggotaKoperasi.getKembalian() == null ? "0.0"
					: Common.numberFormat.get().format(pembelianAnggotaKoperasi.getKembalian())).setParent(arg0);

			new Label(pembelianAnggotaKoperasi.getPostingHistory() == null ? "Belum"
					: pembelianAnggotaKoperasi.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Struk PembelianAnggotaKoperasi");
			button.setVisible(pembelianAnggotaKoperasi.getAnggotaKoperasi() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakPembelianAnggotaKoperasi(pembelianAnggotaKoperasi);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && pembelianAnggotaKoperasi.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembelianAnggotaKoperasi);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && pembelianAnggotaKoperasi.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(pembelianAnggotaKoperasi);
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	/**
	 * Membatalkan (menghapus) sebuah transaksi penjualan, dengan ALASAN yang wajib diisi dan tercatat
	 * permanen.
	 *
	 * <p><b>Perubahan 2026-07-26.</b> Sebelumnya method ini langsung menghapus transaksi tanpa jejak
	 * apa pun — tidak ada catatan siapa membatalkan apa, kapan, apalagi kenapa, sehingga pembatalan
	 * transaksi sama sekali tidak dapat diawasi. Sekarang petugas wajib menuliskan alasan lebih dulu,
	 * lalu seluruh isi transaksi dipotret ke arsip {@code koperasi.pembatalan_transaksi} sebelum
	 * barisnya dihapus (lihat {@link PembatalanTransaksiUtil}). Hasilnya bisa dilihat pimpinan di
	 * Dasbor Kepatuhan Operasional.</p>
	 *
	 * <p><b>Penghapusannya sendiri sengaja dipertahankan</b> (bukan diganti penanda "dibatalkan" di
	 * tabelnya) — alasan teknisnya panjang dan penting, ada di JavaDoc
	 * {@code PembatalanTransaksiKantin}: singkatnya, stok di modul ini dihitung ulang dari riwayat
	 * penjualan sehingga hanya pulih bila barisnya benar-benar hilang, dan ~86 pembacaan lain tidak
	 * menyaring kolom {@code aktif} sehingga transaksi yang "ditandai batal" akan tetap menagih siswa.</p>
	 */
	public void onDelete(final PembelianAnggotaKoperasi pembelianAnggotaKoperasi) throws Exception {

		final MyWindow dlg = new MyWindow();
		dlg.setTitle("Pembatalan Transaksi");
		dlg.setWidth("460px");
		dlg.setClosable(true);
		dlg.setBorder("normal");

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;");
		isi.setParent(dlg);

		StringBuilder ringkas = new StringBuilder("Transaksi yang akan dibatalkan");
		if (pembelianAnggotaKoperasi.getKode() != null
				&& pembelianAnggotaKoperasi.getKode().trim().length() > 0) {
			ringkas.append(" — ").append(pembelianAnggotaKoperasi.getKode().trim());
		}
		ringkas.append(". Data transaksi akan dihapus dan TIDAK dapat dikembalikan, tetapi catatan "
				+ "pembatalan ini (isi transaksi, siapa, kapan, dan alasannya) tersimpan permanen dan "
				+ "dapat dilihat pimpinan.");
		Label info = new Label(ringkas.toString());
		info.setMultiline(true);
		info.setStyle("font-size:12px;color:#334155;");
		info.setParent(isi);

		Label lbl = new Label("Alasan pembatalan (wajib diisi):");
		lbl.setStyle("font-weight:700;font-size:12px;margin-top:6px;");
		lbl.setParent(isi);

		final Textbox alasanBox = new Textbox();
		alasanBox.setWidth("99%");
		alasanBox.setRows(3);
		alasanBox.setMultiline(true);
		alasanBox.setTooltiptext("Contoh: salah input jumlah, pembeli membatalkan, barang tidak jadi diambil.");
		alasanBox.setParent(isi);

		Hbox aksi = new Hbox();
		aksi.setStyle("margin-top:10px;");
		aksi.setSpacing("8px");
		aksi.setParent(isi);

		Toolbarbutton batal = new ais.ui.util.MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dlg.detach();
			}
		});
		batal.setParent(aksi);

		Toolbarbutton proses = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Transaksi", "/img/delete.gif");
		proses.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final String alasan = alasanBox.getValue() == null ? "" : alasanBox.getValue().trim();
				if (alasan.length() == 0) {
					MyMessageboxConfig.show(
							"Mohon maaf, alasan pembatalan wajib diisi. Langkah yang dapat dilakukan: (1) tuliskan "
									+ "alasan pembatalan pada kolom yang tersedia; (2) ulangi penekanan tombol "
									+ "Batalkan Transaksi.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin membatalkan transaksi ini? Data transaksi akan dihapus dan "
								+ "tidak dapat dikembalikan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								try {
									// Arsip + hapus dilakukan pada SESI YANG SAMA supaya keduanya berada
									// dalam satu transaksi basis data: mustahil terjadi keadaan
									// "transaksi terhapus tapi arsip pembatalannya gagal tersimpan".
									PembatalanTransaksiUtil.batalkan(HibernateUtil.currentSession(),
											pembelianAnggotaKoperasi, alasan);
									dlg.detach();
									onSearchDefault(event);
									MyMessageboxConfig.show(
											"Transaksi berhasil dibatalkan dan alasannya telah tercatat.",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
									MyMessageboxConfig.show(MyMessageboxConfig.format(
											"Mohon maaf, transaksi ini tidak dapat dibatalkan karena masih berelasi dengan data lainnya. Rincian: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan data terkait terlebih dahulu; (2) ulangi pembatalan; (3) apabila masih gagal, mohon hubungi administrator sistem.",
											e.getMessage()));
								}
							}
						});
			}
		});
		proses.setParent(aksi);

		dlg.setPage(this.self.getPage());
		dlg.doModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PembelianAnggotaKoperasi());
	}

	private EventListener perubahanAnggotaKoperasiListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) PembelianAnggotaKoperasiAction.this.anggotaKoperasi
					.getAttribute("anggotaKoperasi");
			PembelianAnggotaKoperasiAction.this.anggotaKoperasi
					.setValue(anggotaKoperasi == null ? "" : anggotaKoperasi.getKode().trim());
			nama.setValue(anggotaKoperasi == null ? "" : anggotaKoperasi.getNama());

			alamat.setValue(anggotaKoperasi == null ? "" : anggotaKoperasi.getAlamat());

			Common.selectComboItem(jenisAnggotaKoperasi,
					anggotaKoperasi == null ? null : anggotaKoperasi.getJenisAnggotaKoperasi());

			pembelianAnggotaKoperasiDetailAction.loadData(null);
			totalBiayaEventListener.onEvent(null);

		}
	};

	private EventListener totalBiayaEventListener = new EventListener() {

		@Override
		public void onEvent(Event event) throws Exception {

			Double myBiaya = biaya.getValue() == null ? 0.0 : biaya.getValue();

			Double myBayarTunai = bayarTunai.getValue() == null ? 0.0 : bayarTunai.getValue();
			Double myBayarNonTunai = bayarNonTunai.getValue() == null ? 0.0 : bayarNonTunai.getValue();

			Double myTotalBiaya = (myBiaya);

			totalBiaya.setValue(myTotalBiaya);

			Double myKembalian = (myBayarTunai + myBayarNonTunai) - myTotalBiaya;

			System.out.println("myBiaya = " + myBiaya + ", myBayarTunai = " + myBayarTunai + ", myBayarNonTunai = "
					+ myBayarNonTunai + ", myTotalBiaya = " + myTotalBiaya + ", myKembalian = " + myKembalian);

			if (myKembalian < 0.0) {
				kembalian.setValue(0.0);
			} else {
				kembalian.setValue((myKembalian));
			}

		}
	};

	public Textbox barcode;
	private MyToolbarbuttonConfig save;

	@SuppressWarnings("deprecation")
	private Borderlayout mainInit(final PembelianAnggotaKoperasi pembelianAnggotaKoperasi) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pembelian")));
		String mykode = pembelianAnggotaKoperasi.getKode();
		if (mykode == null || mykode.trim().equals("")) {
			// mykode = Common.generateCode(PembelianAnggotaKoperasi.class, 8);
		}
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");
		kode.setReadonly(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Anggota Koperasi")));
		row.appendChild(anggotaKoperasi = new AmbilDataAnggotaKoperasiBanbox());
		anggotaKoperasi.setValue(pembelianAnggotaKoperasi.getAnggotaKoperasi() == null ? ""
				: pembelianAnggotaKoperasi.getAnggotaKoperasi().getKode());
		anggotaKoperasi.setAttribute("anggotaKoperasi", pembelianAnggotaKoperasi.getAnggotaKoperasi());
		anggotaKoperasi.setWidth("90%");
		anggotaKoperasi.setEventListener(perubahanAnggotaKoperasiListener);
//		anggotaKoperasi.setDisabled(true);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembelian")));
		row.appendChild(
				tanggalPembayaran = new MyDatebox(pembelianAnggotaKoperasi.getTanggalPembayaran() == null ? new Date()
						: pembelianAnggotaKoperasi.getTanggalPembayaran()));
		tanggalPembayaran.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembayaran.setWidth("90%");

		final AnggotaKoperasi anggotaKoperasi = pembelianAnggotaKoperasi.getAnggotaKoperasi();
		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Anggota Koperasi")));
		row.appendChild(nama = new Label(anggotaKoperasi == null ? "" : anggotaKoperasi.getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(anggotaKoperasi == null ? "" : anggotaKoperasi.getAlamat()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				pembelianAnggotaKoperasi.getKeterangan() == null ? "" : pembelianAnggotaKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		CommonSirs.initLokasiDanShift(
				pembelianAnggotaKoperasi.getLokasi() == null ? myLokasi : pembelianAnggotaKoperasi.getLokasi(), rows,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						if (kode.getValue().trim().equals("")) {
							String mykode = Common.generateCode(Pembayaran.class, 8, "PEMB", myLokasi);
							kode.setValue(mykode);
						}
					}
				});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya")));
		row.appendChild(biaya = new MyDoublebox(pembelianAnggotaKoperasi.getBiaya()));
		biaya.setWidth("90%");
		biaya.setReadonly(true);
		biaya.setDisabled(true);
		biaya.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bayar Tunai")));
		row.appendChild(bayarTunai = new MyDoublebox(pembelianAnggotaKoperasi.getBayarTunai()));
		bayarTunai.setWidth("90%");
		bayarTunai.setStyle("font-size:xx-large;text-align: right;");

		final Row rowTunai = new Row();
		rowTunai.setStyle("border:0px;background: transparent;");
		rowTunai.setParent(rows);
		rowTunai.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar")));
		rowTunai.appendChild(jenisBiayaLain = new Combobox());
		Common.insertCombo(jenisBiayaLain, "nama", "akun", CaraPembayaranKoperasi.class);
		jenisBiayaLain.setWidth("90%");
		if (jenisBiayaLain.getSelectedItem() == null && !jenisBiayaLain.getChildren().isEmpty()) {
			jenisBiayaLain.setSelectedIndex(0);
		}
		jenisBiayaLain.setReadonly(true);

//		rowTunai.setVisible(pembelianAnggotaKoperasi.getBayarTunai() > 0.0);
//		bayarTunai.addEventListener("onChange", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				rowTunai.setVisible(bayarTunai.getValue() != null && bayarTunai.getValue() > 0.0);
//			}
//		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bayar Non Tunai")));
		row.appendChild(bayarNonTunai = new MyDoublebox(pembelianAnggotaKoperasi.getBayarTunai()));
		bayarNonTunai.setWidth("90%");
		bayarNonTunai.setReadonly(true);
		bayarNonTunai.setDisabled(true);
		bayarNonTunai.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Tagihan")));
		row.appendChild(totalBiaya = new MyDoublebox(pembelianAnggotaKoperasi.getTotalBiaya()));
		totalBiaya.setWidth("90%");
		totalBiaya.setReadonly(true);
		totalBiaya.setDisabled(true);
		totalBiaya.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kembalian")));
		row.appendChild(kembalian = new MyDoublebox(pembelianAnggotaKoperasi.getKembalian()));
		kembalian.setWidth("90%");
		kembalian.setReadonly(true);
		kembalian.setDisabled(true);
		kembalian.setStyle("font-size:xx-large;text-align: right;");

		bayarTunai.addEventListener("onChange", totalBiayaEventListener);
		bayarTunai.addEventListener("onOK", totalBiayaEventListener);
		totalBiayaEventListener.onEvent(null);

		if (kode.getValue().trim().equals("") && myLokasi != null) {
			mykode = Common.generateCode(PembelianAnggotaKoperasi.class, 10, "PEMB", myLokasi);
			kode.setValue(mykode);
		}

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);

		add.setParent(toolbar);
		save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Pembayaran", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if ((bayarTunai.getValue() == null || bayarTunai.getValue().equals(0.0))
						&& (bayarNonTunai.getValue() == null || bayarNonTunai.getValue().equals(0.0))) {
					MyMessageboxConfig.show("Mohon maaf, jumlah pembayaran belum sesuai. Langkah yang dapat dilakukan: (1) isi Bayar Tunai dan/atau Bayar Non Tunai dengan nominal yang benar; (2) pastikan total pembayaran mencukupi total tagihan; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Double jumlahDibayar = (bayarTunai.getValue() == null ? 0.0 : bayarTunai.getValue())
						+ (bayarNonTunai.getValue() == null ? 0.0 : bayarNonTunai.getValue());
				if (jumlahDibayar < (totalBiaya.getValue() == null ? 0.0 : totalBiaya.getValue())) {
					MyMessageboxConfig.show("Mohon maaf, jumlah pembayaran masih kurang dari total tagihan. Langkah yang dapat dilakukan: (1) periksa kembali total tagihan; (2) tambah nominal Bayar Tunai atau Bayar Non Tunai hingga mencukupi; (3) ulangi penyimpanan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;

				} else {
					if (onSave(event)) {
						onSearchDefault(null);
					}
				}

			}
		});
		save.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Pembayaran", "/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Pembayaran");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (pembelianAnggotaKoperasi != null && pembelianAnggotaKoperasi.getId() != null) {
					onDelete(pembelianAnggotaKoperasi);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pembelian anggota koperasi ini? Data yang sedang diisi akan dikosongkan kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new PembelianAnggotaKoperasi());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		return borderlayout;
	}

	private Borderlayout initDetailTransaksi(final PembelianAnggotaKoperasi pembelianAnggotaKoperasi) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		pembelianAnggotaKoperasiDetailAction = new PembelianAnggotaKoperasiDetailAction();
		center.appendChild(pembelianAnggotaKoperasiDetailAction);

		return borderlayout;
	}

	private void init(final PembelianAnggotaKoperasi pembelianAnggotaKoperasi) throws Exception {
		this.pembelianAnggotaKoperasi = pembelianAnggotaKoperasi;
		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(mainInit(pembelianAnggotaKoperasi));

		East east = new East();
		east.setStyle("border:0px;background: transparent;");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("55%");
		east.appendChild(initDetailTransaksi(pembelianAnggotaKoperasi));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		pembelianAnggotaKoperasiDetailAction.loadData(null);

		if (pembelianAnggotaKoperasi.getId() != null) {
			Common.freeze(tambahData, true);
			keterangan.setDisabled(false);
		}
		add.setDisabled(false);
		save.setDisabled(false);

	}

	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon maaf, lokasi belum diisi. Langkah yang dapat dilakukan: (1) pilih lokasi pada bagian formulir; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pembelianAnggotaKoperasiDetails == null || pembelianAnggotaKoperasiDetails.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, rincian tagihan belum ditemukan. Langkah yang dapat dilakukan: (1) tambahkan minimal satu barang pada rincian pembelian; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalPembayaran.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal pembelian belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tanggal Pembelian; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisBiayaLain.getSelectedItem() == null || jenisBiayaLain.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, cara bayar belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu Cara Bayar; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pembelianAnggotaKoperasi.getId() != null) {
			pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) session.load(PembelianAnggotaKoperasi.class,
					pembelianAnggotaKoperasi.getId());
		}

		pembelianAnggotaKoperasi
				.setCaraPembayaranKoperasi((CaraPembayaranKoperasi) jenisBiayaLain.getSelectedItem().getValue());
		pembelianAnggotaKoperasi.setHargaPpn(0.0);
		pembelianAnggotaKoperasi.setBiaya(biaya.getValue());
		pembelianAnggotaKoperasi.setDiskon(0.0);
		pembelianAnggotaKoperasi.setDiskonDalamPersen(false);
		pembelianAnggotaKoperasi.setTotalDiskon(0.0);
		pembelianAnggotaKoperasi.setPpn(0.0);
		pembelianAnggotaKoperasi.setTotalBiaya(totalBiaya.getValue());
		pembelianAnggotaKoperasi.setBayarTunai(bayarTunai.getValue());
		pembelianAnggotaKoperasi.setBayarNonTunai(bayarNonTunai.getValue());
		pembelianAnggotaKoperasi.setKembalian(kembalian.getValue());

		pembelianAnggotaKoperasi.setTanggalPembayaran(tanggalPembayaran.getValue());

		pembelianAnggotaKoperasi.setAnggotaKoperasi((AnggotaKoperasi) anggotaKoperasi.getAttribute("anggotaKoperasi"));
		pembelianAnggotaKoperasi.setKode(kode.getValue());
		pembelianAnggotaKoperasi.setKeterangan(keterangan.getValue());
		pembelianAnggotaKoperasi.setTbmuser(Common.getCurrentUser());
		pembelianAnggotaKoperasi.setLokasi(myLokasi);

		if (pembelianAnggotaKoperasi.getId() != null) {
			Common.refreshUpdate(session, pembelianAnggotaKoperasi);
		} else {
			pembelianAnggotaKoperasi.setIndex(Common.generateMaxByLokasi(PembelianAnggotaKoperasi.class, myLokasi) + 1);
			String mykode = Common.generateCode(PembelianAnggotaKoperasi.class, 10, "PEM", myLokasi);
			kode.setValue(mykode);
			pembelianAnggotaKoperasi.setKode(mykode);
			session.save(pembelianAnggotaKoperasi);

		}

		simpanDetailTransaksi(pembelianAnggotaKoperasi);

		MyMessageboxConfig.show("Data pembayaran telah berhasil disimpan. Struk pembelian akan segera ditampilkan untuk dicetak. Terima kasih.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.freeze(tambahData, true);
						add.setDisabled(false);

						onCetakPembelianAnggotaKoperasi(pembelianAnggotaKoperasi);
					}
				});

		return true;
	}

	private void simpanDetailTransaksi(PembelianAnggotaKoperasi pembelianAnggotaKoperasi) {
		Session session = HibernateUtil.currentSession();

		java.util.Set<Long> produkTerjual = new java.util.HashSet<Long>();
		for (Pembelian detailTransaksi : pembelianAnggotaKoperasiDetails) {
			detailTransaksi.setPembelianAnggotaKoperasi(pembelianAnggotaKoperasi);
			Common.refreshSaveOrUpdate(session, detailTransaksi);
			if (detailTransaksi.getProduk() != null && detailTransaksi.getProduk().getId() != null) {
				produkTerjual.add(detailTransaksi.getProduk().getId());
			}
		}

		// Segarkan stok kantin (kolom cache produk.stok) untuk tiap produk terjual agar laporan &
		// dashboard stok tidak basi setelah penjualan. Dibungkus try/catch agar kegagalan recompute
		// tidak membatalkan transaksi jual yang sudah tersimpan.
		for (Long produkId : produkTerjual) {
			try {
				ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/koperasi/PembelianAnggotaKoperasiAction.java:715");
			}
		}
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembelianAnggotaKoperasi.class)
				.createAlias("anggotaKoperasi", "anggotaKoperasi", Criteria.LEFT_JOIN)
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmr.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("anggotaKoperasi.kode", searchmr.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("anggotaKoperasi.nama", searchnama.getValue(), MatchMode.ANYWHERE)))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembelianAnggotaKoperasi> pembelianAnggotaKoperasi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembelianAnggotaKoperasi);
		grid.setRowRenderer(new PembelianAnggotaKoperasiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public class PembelianAnggotaKoperasiDetailAction extends MyDiv {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;
		private Footer totalDiskon;
		private Footer total;
		private Footer totalHrg;

		public PembelianAnggotaKoperasiDetailAction() throws Exception {
			super();
			display();
		}

		class PembelianAnggotaKoperasiDetailRenderer extends ais.ui.util.MyRowRenderer {

			public PembelianAnggotaKoperasiDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {
				row.setValign("top");

				Pembelian detailTransaksi = (Pembelian) data;
				Produk item = detailTransaksi.getProduk();
				Component image = ProdukUtil.generateImage(item);
				image.setParent(row);

				new Label(item == null ? "" : item.getKode()).setParent(row);
				new Label(item == null ? "" : item.getNama()).setParent(row);
				new Label(item == null || item.getJenisProduk() == null ? "" : item.getJenisProduk().getNama())
						.setParent(row);
				new Label(Common.numberFormat.get().format(detailTransaksi.getQty())).setParent(row);

				new Label(detailTransaksi.getHargaJual() == null ? "0.0"
						: Common.numberFormat.get().format(detailTransaksi.getHargaJual())).setParent(row);

				Label diskon = new Label(Common.numberFormat.get().format(detailTransaksi.getDiskon()));
				diskon.setParent(row);

				(new Label(Common.numberFormat.get().format(detailTransaksi.getTotal()))).setParent(row);

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) throws Exception {
			if (anggotaKoperasi == null) {
				return;
			}

			Session session = HibernateUtil.currentSession();

			List<Pembelian> detailTransaksiLayanans = pembelianAnggotaKoperasi
					.getId() == null
							? new ArrayList<Pembelian>()
							: session.createCriteria(Pembelian.class).add(Restrictions.gt("qty", 0.0))
									.add(pembelianAnggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("false")
											: Restrictions.eq("pembelianAnggotaKoperasi", pembelianAnggotaKoperasi))
									.list();
			ListModel strset = new SimpleListModel(
					pembelianAnggotaKoperasi.getId() == null ? pembelianAnggotaKoperasiDetails
							: detailTransaksiLayanans);
			grid.setRowRenderer(new PembelianAnggotaKoperasiDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

			loadTotal(pembelianAnggotaKoperasi.getId() == null ? pembelianAnggotaKoperasiDetails
					: detailTransaksiLayanans);

		}

		public void loadTotal(List<Pembelian> pembelianAnggotaKoperasiDetails) throws Exception {

			if (anggotaKoperasi == null) {
				return;
			}

			Double mytotal = 0.0;
			Double myhrg = 0.0;
			Double mydiskon = 0.0;

			for (Object data : pembelianAnggotaKoperasiDetails) {
				Pembelian detailTransaksi = (Pembelian) data;

				mydiskon += detailTransaksi.getDiskon() * detailTransaksi.getQty();

				mytotal += detailTransaksi.getQty() == null ? 0.0 : (detailTransaksi.getQty());

				Double totalAmount = detailTransaksi.getTotal();

				myhrg += totalAmount;
			}

			totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));

			total.setLabel(mytotal == null ? "0.0" : Common.numberFormat.get().format(mytotal));
			totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));

			biaya.setValue(myhrg);

			totalBiayaEventListener.onEvent(null);

		}

		private void display() throws Exception {

			Vbox vbox = new Vbox();
			vbox.setParent(this);

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			if (pembelianAnggotaKoperasi.getId() == null) {

				Toolbar toolbar = new Toolbar();
				toolbar.setHeight("70px");
				toolbar.setParent(vbox);

				MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Barang", "/img/new.gif");
				add.setParent(toolbar);
				add.setTooltiptext("Tambah");
				add.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (myLokasi == null || myLokasi.getToko() == null) {
							MyMessageboxConfig.show("Mohon maaf, toko/pedagang belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko/pedagang pada bagian lokasi; (2) ulangi penambahan barang.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						List<Produk> produks = new ArrayList<Produk>();

						for (Pembelian row : pembelianAnggotaKoperasiDetails) {
							produks.add(row.getProduk());
						}

						AmbilDataProdukBanyak ambilDataProdukBanyak = new AmbilDataProdukBanyak(produks,
								myLokasi.getToko());
						ambilDataProdukBanyak.setHeight("95%");
						ambilDataProdukBanyak.setWidth(Common.isMobile() ? "100%" : "750px");
						ambilDataProdukBanyak
								.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						ambilDataProdukBanyak.setEventListener(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Produk> produks = (List<Produk>) arg0.getData();
								for (Produk produk : produks) {

									Pembelian barangBeli = new Pembelian();
									barangBeli.setProduk(produk);
									barangBeli.setKios(myLokasi.getToko().getKode());
									barangBeli.setPembelianAnggotaKoperasi(pembelianAnggotaKoperasi);
									barangBeli.setToko(myLokasi.getToko());
									pembelianAnggotaKoperasiDetails.add(barangBeli);
								}
								loadData(null);
							}
						});

						ambilDataProdukBanyak.onModal();

					}
				});

				new Space().setParent(toolbar);
				new Space().setParent(toolbar);
				new Space().setParent(toolbar);

				new Label(ais.common.Common.getBahasaConfig("Barcode")).setParent(toolbar);
				new Space().setParent(toolbar);
				barcode = new Textbox();
				// barcode.setDisabled(pembelian.getDisetujuiOleh() != null);
				barcode.setStyle("font-size:xx-large");
				barcode.setParent(toolbar);
				barcode.addEventListener("onOK", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (myLokasi == null || myLokasi.getToko() == null) {
							MyMessageboxConfig.show("Mohon maaf, toko/pedagang belum dipilih. Langkah yang dapat dilakukan: (1) pilih toko/pedagang pada bagian lokasi; (2) ulangi penambahan barang.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						Produk produk = (Produk) ConstantValues.simpleObject(
								HibernateUtil.currentSession().createCriteria(Produk.class)
										.add(Restrictions.ilike("kode", barcode.getValue().trim())).setMaxResults(1),
								Produk.class);
						if (produk == null) {
							MyMessageboxConfig.show("Mohon maaf, kode barang tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali barcode yang dipindai atau diketik; (2) pastikan barang telah terdaftar dan aktif; (3) ulangi pemindaian.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Pembelian barangBeli = new Pembelian();
						barangBeli.setProduk(produk);
						barangBeli.setKios(myLokasi.getToko().getKode());
						barangBeli.setPembelianAnggotaKoperasi(pembelianAnggotaKoperasi);
						barangBeli.setToko(myLokasi.getToko());
						pembelianAnggotaKoperasiDetails.add(barangBeli);

						loadData(null);
					}
				});

				new Space().setParent(toolbar);
				new Space().setParent(toolbar);
			}

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(vbox);
			grid.setStyle("min-height: 400px;");

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Gambar");
			column.setWidth("55px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Kode");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Jenis");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setAlign("right");
			column.setWidth("8%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Hrg");
			column.setAlign("right");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Diskon %");
			column.setAlign("right");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");
			column.setWidth("10%");

			Foot foot = new Foot();
			foot.setParent(grid);

			foot.appendChild(new Footer());
			foot.appendChild(new Footer());
			foot.appendChild(new Footer("Total"));
			foot.appendChild(new Footer());

			total = new Footer();
			total.setParent(foot);
			total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			foot.appendChild(new Footer());
			foot.appendChild(new Footer());

			totalDiskon = new Footer();
			totalDiskon.setParent(foot);
			totalDiskon.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			totalHrg = new Footer();
			totalHrg.setParent(foot);
			totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			loadData(null);

		}
	}

	public void onCetakPembelianAnggotaKoperasi(PembelianAnggotaKoperasi pembelianAnggotaKoperasi) throws Exception {
		LaporanBuktiPembelianAnggotaKoperasiWindow laporanBuktiPembelianAnggotaKoperasiWindow = new LaporanBuktiPembelianAnggotaKoperasiWindow(
				pembelianAnggotaKoperasi);
		laporanBuktiPembelianAnggotaKoperasiWindow.setTitle("Laporan Bukti PembelianAnggotaKoperasi");
		laporanBuktiPembelianAnggotaKoperasiWindow.setClosable(true);
		laporanBuktiPembelianAnggotaKoperasiWindow.setWidth("750px");
		laporanBuktiPembelianAnggotaKoperasiWindow.setHeight("95%");
		laporanBuktiPembelianAnggotaKoperasiWindow.setParent(page.getFirstRoot());
		laporanBuktiPembelianAnggotaKoperasiWindow.onModal();
	}

}
