package ais.action.master.sirs;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.TransaksiItemDetailHelper;
import ais.action.master.sirs.helper.AmbilDataResepBanbox;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.common.listener.GetTransaksi;
import ais.common.listener.TransaksiListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk transaksi. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code Paging paging},
 * {@code Tabpanel tambahData}, {@code MyTextbox searchkode}, {@code MyTextbox searchmr}, {@code MyTextbox
 * searchnama}, {@code MyTextbox searchtelp}, {@code Combobox searchkelas}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()},
 * {@code getTransaksi()}, {@code getLokasi()}, {@code getAdd()}, {@code getSimpan()}, {@code
 * getKelasPerawatan()}); validasi/perhitungan ({@code checkKodeTransaksi()}); mutasi data ({@code onSave()},
 * {@code onBerubah()}); penghapusan/pembatalan ({@code onDelete()}); pelaporan/ekspor ({@code onCetak()});
 * operasi domain lain ({@code onAdd()}, {@code createMain()}, {@code onBebas()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class TransaksiAction extends GenericAutowireComposer implements GetTransaksi, TransaksiListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Grid grid;
	private Paging paging;

	private Tabpanel tambahData;

	private MyTextbox searchkode;
	private MyTextbox searchmr;
	private MyTextbox searchnama;
	private MyTextbox searchtelp;

	private Combobox searchkelas;
	private Combobox searchruang;
	private Combobox searchkamar;
	private AmbilDataTempatTidurBanbox searchbed;

	private AmbilDataResepBanbox resep;

	private Label kode;
	private Pasien pasien;
	private Pendaftaran pendaftaran;
	private String keterangan;
	private Date tanggalTransaksi;
	private Boolean bebas;
	private KelasPerawatan kelasPerawatan;
	private String nama;

	private boolean edit = false;
	private boolean delete = false;

	private TransaksiMedis transaksi;
	private Toolbarbutton add;
	private Toolbarbutton simpan;
	private Toolbarbutton validasi;

	protected Set<Tindakan> pakets;

	private Center center = new Center();

	private String SUMBER = TransaksiMedis.SUMBER_APOTIK;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

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

		Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
		Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchkamar);
				if (searchkelas.getSelectedItem() != null && searchruang.getSelectedItem() != null) {
					Common.insertCombo(searchkamar, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()),
									Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue())));
				}
			}
		};

		searchkelas.addEventListener("onChange", myEventListener);
		searchruang.addEventListener("onChange", myEventListener);

		searchkelas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (searchkelas.getSelectedItem() == null
						? ConstantValues.kelasNormal
						: searchkelas.getSelectedItem().getValue());
				if (mykelasPerawatan != null) {
					searchbed.setMyKelasPerawatan(mykelasPerawatan);
				}
			}
		});

		searchruang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (searchruang.getSelectedItem() == null ? null
						: searchruang.getSelectedItem().getValue());
				if (myRuang != null) {
					searchbed.setMyRuang(myRuang);
				}
			}
		});

		searchkamar.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (searchkamar.getSelectedItem() == null ? null
						: searchkamar.getSelectedItem().getValue());
				if (myKamar != null) {
					searchbed.setMyKamar(myKamar);
				}
			}
		});

		add = new ais.ui.util.MyToolbarbuttonConfig("Transaksi Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new TransaksiMedis());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new TransaksiMedis());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TransaksiMedis transaksi = (TransaksiMedis) arg1;

			if (transaksi.getValidasi() == null || !transaksi.getValidasi()) {
				arg0.setStyle("background-color:yellow;");
			} else {
				arg0.setStyle("background-color:#DBFDF3;");
			}

			Pasien pasien = transaksi.getPasien();

			new ais.action.master.sirs.detail.TransaksiDetailAction(transaksi).setParent(arg0);

			RevisiHelper.createNewRevisi(TransaksiMedis.class, transaksi, transaksi.getKode()).setParent(arg0);
			// new Label(pasien == null ? "" :
			// pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? transaksi.getNama() : pasien.getNama()).setParent(arg0);
			new Label(transaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(transaksi.getTanggalTransaksi())).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);
			new Label(transaksi.getKelasPerawatan() == null ? "" : transaksi.getKelasPerawatan().getNama())
					.setParent(arg0);

			String bed = "";
			if (transaksi.getPendaftaran() != null && transaksi.getPendaftaran().getTempatTidur() != null) {
				bed = (transaksi.getPendaftaran().getRuangPerawatan() == null ? ""
						: transaksi.getPendaftaran().getRuangPerawatan().getNama())
						+ " - "
						+ (transaksi.getPendaftaran().getKamarPerawatan() == null ? ""
								: transaksi.getPendaftaran().getKamarPerawatan().getNama())
						+ " - " + (transaksi.getPendaftaran().getTempatTidur() == null ? ""
								: transaksi.getPendaftaran().getTempatTidur().getNama());
			}

			new Label(bed).setParent(arg0);

			new Label(transaksi.getBebas() ? "Ya" : "Tidak").setParent(arg0);
			new Label(transaksi.getValidasi() == null || !transaksi.getValidasi() ? "Belum" : "Ya").setParent(arg0);
			new Label(transaksi.getLunas() ? "Ya" : "Belum").setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transaksi");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					if (true) {
						onCetak(transaksi);
					}
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					TransaksiMedis myTransaksi = (TransaksiMedis) HibernateUtil.currentSession().createCriteria(TransaksiMedis.class)
							.add(Restrictions.idEq(transaksi.getId())).uniqueResult();
					init(myTransaksi);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(transaksi);

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransaksiMedis());
	}

	private EventListener perubahanPasienListener;

	EventListener resepEventListener = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {

			if (myLokasi == null) {
				if (arg0 != null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
				resep.setValue("");
				resep.setAttribute("resep", null);
				return;
			}

			Resep myResep = (Resep) resep.getAttribute("resep");
			if (myResep == null || myResep.getDiagnosaPenyakit() == null
					|| myResep.getDiagnosaPenyakit().getPendaftaran() == null) {
				return;
			}
			Pendaftaran myPendaftaran = myResep.getDiagnosaPenyakit().getPendaftaran();
			perubahanPasienListener.onEvent(new Event("", null, myPendaftaran));

			Common.freeze(TransaksiAction.this.center, true);
			resep.setDisabled(true);

			if (transaksi.getId() == null) {

				if (!TransaksiAction.this.onSave(arg0)) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				List<ResepDetail> resepDetails = session.createCriteria(ResepDetail.class)
						.add(Restrictions.eq("resep", myResep)).list();

				simpan.setDisabled(resepDetails.size() == 0);
				add.setDisabled(resepDetails.size() != 0);

				for (ResepDetail resepDetail : resepDetails) {
					ItemMedis item = resepDetail.getItem();
					Racikan racikan = resepDetail.getRacikan();
					if (item != null) {

						HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
								transaksi.getPendaftaran().getDokter(), transaksi.getPendaftaran().getAsuransi(),
								transaksi.getPendaftaran().getKomunitass(), transaksi.getPendaftaran().getPasien());

						final Double diskon = CommonSirs.getTotalDiskonDalamPersen(item, null, null, 1,
								transaksi.getTanggalTransaksi(), transaksi.getPendaftaran().getAsuransi(),
								transaksi.getPendaftaran().getKomunitass());
						final Double pajak = CommonSirs.getTotalPajakDalamPersen(item, null, null,
								transaksi.getPendaftaran().getAsuransi(), transaksi.getPendaftaran().getKomunitass());

						TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
						transaksiDetail.setDiskonPersen(diskon);
						transaksiDetail.setPajakPersen(pajak);
						transaksiDetail.getPajaks().addAll(CommonSirs.getPajakSekarang(item, null, null,
								transaksi.getPendaftaran().getAsuransi(), transaksi.getPendaftaran().getKomunitass()));
						transaksiDetail.getDiskons()
								.addAll(CommonSirs.getDiskonSekarang(item, null, null, 1,
										transaksi.getTanggalTransaksi(), transaksi.getPendaftaran().getAsuransi(),
										transaksi.getPendaftaran().getKomunitass()));

						transaksiDetail.setAmount(hargaJualItem.getHargaJual());
						transaksiDetail.setItem(item);
						transaksiDetail.setQty(resepDetail.getJumlah() == null ? 0.0 : resepDetail.getJumlah());
						transaksiDetail.setKeterangan("Transaksi obat penjualan di lokasi " + myLokasi.getNama());
						transaksiDetail.setTransaksi(transaksi);
						session.save(transaksiDetail);
					} else if (racikan != null) {
						TransaksiMedisDetail transaksiDetail = new TransaksiMedisDetail();
						transaksiDetail.setAmount(CommonSirs.hitungHargaJualRacikan(racikan, kelasPerawatan,
								transaksi.getPendaftaran().getDokter(), transaksi.getPendaftaran().getAsuransi(),
								transaksi.getPendaftaran().getKomunitass(), transaksi.getPendaftaran().getPasien()));
						transaksiDetail.setDiskon(CommonSirs.hitungDiskonRacikan(racikan, kelasPerawatan,
								transaksi.getTanggalTransaksi(), transaksi.getPendaftaran().getDokter(),
								transaksi.getPendaftaran().getAsuransi(), transaksi.getPendaftaran().getKomunitass(),
								transaksi.getPendaftaran().getPasien()));
						transaksiDetail.setPajak(CommonSirs.hitungPajakRacikan(racikan, kelasPerawatan,
								transaksi.getPendaftaran().getDokter(), transaksi.getPendaftaran().getAsuransi(),
								transaksi.getPendaftaran().getKomunitass(), transaksi.getPendaftaran().getPasien()));
						transaksiDetail.setRacikan(racikan);
						transaksiDetail.setQty(resepDetail.getJumlah() == null ? 0.0 : resepDetail.getJumlah());
						transaksiDetail.setKeterangan("Transaksi racikan penjualan di lokasi " + myLokasi.getNama());
						transaksiDetail.setTransaksi(transaksi);
						session.save(transaksiDetail);
					}
				}
			}

			Common.clear(eastInfoPasien);
			transaksiDetailAction = new TransaksiItemDetailHelper(TransaksiAction.this);
			eastInfoPasien.appendChild(transaksiDetailAction);
			transaksiDetailAction.loadData(null);

		}
	};

	private TransaksiItemDetailHelper transaksiDetailAction;
	private East eastInfoPasien;
	private Row rowResep;

	@SuppressWarnings("deprecation")
	private Borderlayout createMain(final TransaksiMedis transaksi) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");

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
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		kode = new Label(transaksi.getKode());
		perubahanPasienListener = CommonPendaftaranUtil.initTransaksi(rows, kode, transaksi, this);

		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		rowResep = new Row();
		ais.ui.util.ZkCompat.setSpans(rowResep, "1,3");
		rowResep.setStyle("border:0px;background: transparent;");
		rowResep.setParent(rows);
		rowResep.appendChild(new Label(ais.common.Common.getBahasaConfig("Resep Dokter")));
		rowResep.appendChild(resep = new AmbilDataResepBanbox());
		resep.setWidth("90%");
		resep.setValue(transaksi.getResep() == null ? "" : transaksi.getResep().getKode());
		resep.setAttribute("resep", transaksi.getResep());
		resep.setEventListener(resepEventListener);

		CommonSirs.initLokasiDanShift(transaksi.getLokasi() == null ? myLokasi : transaksi.getLokasi(),
				transaksi.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];

						kode.setValue(Common.generateCode(TransaksiMedis.class, 8, "TRX", myLokasi));
					}
				});

		return borderlayout;
	}

	private void init(final TransaksiMedis transaksi) throws Exception {
		this.transaksi = transaksi;

		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();

		Common.clear(center);
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(createMain(transaksi));
		if (transaksi.getId() != null) {
			Common.freeze(center, true);
		}

		eastInfoPasien = new East();
		ais.ui.util.ZkCompat.setFlex(eastInfoPasien, true);
		eastInfoPasien.setParent(borderlayout);
		eastInfoPasien.setWidth("60%");

		if (transaksi != null && transaksi.getId() != null) {
			pakets = transaksi.getPendaftaran() == null ? new HashSet<Tindakan>()
					: transaksi.getPendaftaran().getPakets();
			System.out.println("pakets = " + pakets);
			if (!pakets.isEmpty()) {
				System.out.println("masuk pakets = " + pakets);
				CommonPendaftaranUtil.transaksiDetailPaket(eastInfoPasien, pakets, transaksi.getPendaftaran(),
						PaketPerawatanDetail.PAKET_OBAT);
				add.setDisabled(true);
				simpan.setDisabled(false);
			} else {
				Common.clear(eastInfoPasien);
				transaksiDetailAction = new TransaksiItemDetailHelper(TransaksiAction.this);
				eastInfoPasien.appendChild(transaksiDetailAction);
				rowResep.setVisible(true);
			}
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		simpan = new ais.ui.util.MyToolbarbuttonConfig("Simpan Transaksi", "/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.setDisabled(true);
		add.setDisabled(false);
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (onSave(event)) {

					MyMessageboxConfig.show("Alhamdulillah, data transaksi telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (true) {
										onCetak(transaksi);
									}
									validasi.setVisible(true);
								}
							});
					add.setDisabled(false);
					simpan.setDisabled(true);
					add.setDisabled(false);
					onSearchDefault(null);
				}
			}
		});
		simpan.setParent(toolbar);

		validasi = new ais.ui.util.MyToolbarbuttonConfig("Validasi Transaksi", "/img/Ok-icon_kecil.png");
		validasi.setTooltiptext("Validasi");
		validasi.setVisible(transaksi.getId() != null);
		validasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin memvalidasi transaksi ini? Setelah divalidasi, transaksi akan dianggap sah dan tidak dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (onSave(event)) {

										if (pakets != null && !pakets.isEmpty()) {
											if (!CommonPendaftaranUtil.validasiTransaksiDetailPaket(pakets, transaksi,
													PaketPerawatanDetail.PAKET_OBAT)) {
												return;
											}
										} else {
											CommonPendaftaranUtil.validasiTransaksiItem(transaksi, SUMBER, true);

										}

										MyMessageboxConfig.show("Alhamdulillah, data transaksi telah berhasil divalidasi. Terima kasih, Bapak/Ibu.", "Informasi",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {

														if (pakets == null || pakets.isEmpty()) {
															onCetak(transaksi);
														}
													}
												});

										add.setDisabled(false);
										validasi.setDisabled(true);
										simpan.setDisabled(true);
										add.setDisabled(false);
										onSearchDefault(null);

										Common.freeze(center, true);
										Common.freeze(eastInfoPasien, true);
										Common.freeze(transaksiDetailAction, true);

									}

								}
							}
						});
			}
		});
		validasi.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Transaksi", "/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Transaksi");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (transaksi != null && transaksi.getId() != null) {
					onDelete(transaksi);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan transaksi ini? Perlu diketahui bahwa transaksi yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new TransaksiMedis());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		add.setDisabled(false);

		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);
	}

	public void onDelete(final TransaksiMedis transaksi) throws Exception {

		MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan transaksi ini? Perlu diketahui bahwa transaksi yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();
								

								session.createSQLQuery(
										"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ")));")
										.executeUpdate();

								String sql = "delete from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
										+ transaksi.getId() + "));";
								session.createSQLQuery(sql).executeUpdate();

								session.createSQLQuery("update sirs.transaksi_medis_detail set racikan = null where transaksi = "
										+ transaksi.getId() + ";").executeUpdate();

								session.createSQLQuery(
										"delete from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.transaksi_medis_detail where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								
								List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
										.add(Restrictions.eq("transaksi", transaksi)).list();

								for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
									Common.refreshDelete(session, transaksiDetail);
								}

								Common.refreshDelete(session, transaksi);
								init(new TransaksiMedis());

								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
												e.getMessage()));
							}

						}

					}
				});
	}

	public void onCetak(TransaksiMedis transaksi) throws Exception {
		if (pakets == null || pakets.isEmpty()) {
			final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
			parameters.put("id", transaksi.getId());
			Report.generateWindowReport(Report.PDF, parameters, "sirs/transaksi_item", transaksi.getTanggalTransaksi());
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Lokasi yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Shift terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Shift yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalTransaksi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi Tanggal Transaksi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) tentukan Tanggal Transaksi; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (!bebas && pendaftaran == null) {
			MyMessageboxConfig.show("Mohon maaf, untuk pasien yang bukan berstatus BEBAS, data Pendaftaran wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih data Pendaftaran pasien; (2) atau tandai transaksi sebagai pasien BEBAS bila memang sesuai; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama == null || nama.trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan kolom Nama; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transaksi.getId() != null) {
			transaksi = (TransaksiMedis) session.load(TransaksiMedis.class, transaksi.getId());
		}

		transaksi.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
		transaksi.setNama(nama);
		transaksi.setAlamat(pasien == null ? "" : pasien.getAlamatLengkap());
		transaksi.setResep(resep == null ? null : (Resep) resep.getAttribute("resep"));
		transaksi.setKelasPerawatan((KelasPerawatan) (kelasPerawatan));

		transaksi.setUmur(pasien == null ? "" : pasien.getUmur() + " thn");
		transaksi.setTanggalTransaksi(tanggalTransaksi);

		transaksi.setPasien((Pasien) pasien);
		transaksi.setKode(kode.getValue());
		transaksi.setKeterangan(keterangan);
		transaksi.setLokasi(myLokasi);
		transaksi.setShift(myShift);
		transaksi.setPendaftaran((Pendaftaran) pendaftaran);
		transaksi.setBebas(bebas);
		transaksi.setSumber(SUMBER);

		if (transaksi.getId() != null) {
			Common.refreshUpdate(session, transaksi);
		} else {
			String mykode = Common.generateCode(TransaksiMedis.class, 8, "TRX", myLokasi);
			transaksi.setIndex(Common.generateMaxByLokasi(TransaksiMedis.class, myLokasi) + 1);
			kode.setValue(mykode);
			session.save(transaksi);
		}

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiMedis.class);

		if (order)
			criteria.addOrder(Order.desc("tanggalTransaksi"));

		criteria.add(Restrictions.or(Restrictions.isNull("sumber"), Restrictions.eq("sumber", SUMBER)))
				.add(Restrictions.eq("jenisTransaksi", TransaksiMedis.TRX_ITEM))

				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)

				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", searchtelp.getValue(), MatchMode.ANYWHERE))))

				.add(searchkelas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
				.add(searchruang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.ruangPerawatan", searchruang.getSelectedItem().getValue()))
				.add(searchkamar.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.kamarPerawatan", searchkamar.getSelectedItem().getValue()))
				.add((searchbed == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchbed.getAttribute("tempatTidur") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.tempatTidur", searchbed.getAttribute("tempatTidur"))))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))))
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmr.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TransaksiMedis> transaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public Boolean checkKodeTransaksi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TransaksiMedis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.transaksi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.transaksi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public TransaksiMedis getTransaksi() {
		return transaksi;
	}

	@Override
	public Lokasi getLokasi() {
		return myLokasi;
	}

	@Override
	public Button getAdd() {
		// TODO Auto-generated method stub
		return add;
	}

	@Override
	public Button getSimpan() {
		// TODO Auto-generated method stub
		return simpan;
	}

	@Override
	public KelasPerawatan getKelasPerawatan() {
		KelasPerawatan mykelasPerawatan = (KelasPerawatan) (kelasPerawatan == null ? ConstantValues.kelasNormal
				: kelasPerawatan);
		return mykelasPerawatan;
	}

	@Override
	public Bandbox getResep() {
		// TODO Auto-generated method stub
		return resep;
	}

	@Override
	public String getSumber() {
		// TODO Auto-generated method stub
		return SUMBER;
	}

	@Override
	public void onBebas(Boolean checked) throws Exception {
		this.bebas = checked;
		Common.clear(eastInfoPasien);
		if (checked) {
			transaksiDetailAction = new TransaksiItemDetailHelper(TransaksiAction.this);
			eastInfoPasien.appendChild(transaksiDetailAction);
		}
	}

	@Override
	public void onBerubah(Boolean bebas, Pendaftaran pendaftaran, Pasien pasien, String nama, Date tanggalTransaksi,
			KelasPerawatan kelasPerawatan, String keterangan) throws Exception {
		this.bebas = bebas;
		this.pendaftaran = pendaftaran;
		this.pasien = pasien;
		this.tanggalTransaksi = tanggalTransaksi;
		this.keterangan = keterangan;
		this.nama = nama;
		this.kelasPerawatan = kelasPerawatan;

		Pendaftaran myPendaftaran = pendaftaran;
		if (myPendaftaran != null && myPendaftaran.getId() != null) {
			pakets = myPendaftaran.getPakets();
			System.out.println("pakets = " + pakets);
			if (!pakets.isEmpty()) {
				final TransaksiMedis mytransaksi = (TransaksiMedis) HibernateUtil.currentSession().createCriteria(TransaksiMedis.class)
						.add(Restrictions.eq("sumber", SUMBER))
						.add(Restrictions.eq("jenisTransaksi", TransaksiMedis.TRX_ITEM))
						.add(Restrictions.eq("pendaftaran", myPendaftaran)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				if (mytransaksi != null && transaksi.getId() == null) {
					init(mytransaksi);
				} else {
					System.out.println("masuk pakets = " + pakets);
					CommonPendaftaranUtil.transaksiDetailPaket(eastInfoPasien, pakets, myPendaftaran,
							PaketPerawatanDetail.PAKET_OBAT);
					add.setDisabled(true);
					simpan.setDisabled(false);
					rowResep.setVisible(false);
				}
			} else {
				Common.clear(eastInfoPasien);
				transaksiDetailAction = new TransaksiItemDetailHelper(TransaksiAction.this);
				eastInfoPasien.appendChild(transaksiDetailAction);
				rowResep.setVisible(true);
			}
		} else {
			Common.clear(eastInfoPasien);
			transaksiDetailAction = new TransaksiItemDetailHelper(TransaksiAction.this);
			eastInfoPasien.appendChild(transaksiDetailAction);
			rowResep.setVisible(true);
		}
	}

}
