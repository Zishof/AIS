package ais.action.master.sirs;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranSemuaBanbox;
import ais.action.master.sirs.helper.AmbilDataTransaksiBanbox;
import ais.action.master.sirs.util.RawatInapCalculationProcessor;
import ais.action.report.Report;
import ais.action.report.format1.sirs.kasir.LaporanBuktiPembayaranWindow;
import ais.action.report.format1.sirs.kasir.LaporanInformasiBiayaDanReturWindow;
import ais.action.report.format1.sirs.kasir.LaporanInformasiTagihanWindow;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Bagian;
import ais.database.model.sirs.DataPasienKeluar;
import ais.database.model.sirs.DepositPasien;
import ais.database.model.sirs.DepositPunyaPembayaran;
import ais.database.model.sirs.DetailTransaksiLayanan;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisPembayaranMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.PembayaranNonTunai;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiRetur;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pembayaran. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel tambahData}, {@code Grid
 * grid}, {@code Paging paging}, {@code MyTextbox searchkode}, {@code MyTextbox searchnama}, {@code MyTextbox
 * searchmr}, {@code Combobox searchbagian}, {@code Combobox searchShift}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code totalPembayaranInit()}, {@code initDeposit()}, {@code mainInit()}, {@code
 * initDetailTransaksi()}, {@code init()}); pembacaan/pencarian ({@code loadDeposit()}, {@code
 * onSearchDefault()}); validasi/perhitungan ({@code hitungDeposit()}); mutasi data ({@code onSave()}, {@code
 * simpanDetailTransaksi()}); penghapusan/pembatalan ({@code onDelete()}); pelaporan/ekspor ({@code onCetak()},
 * {@code onCetakPembayaran()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class PembayaranAction extends GenericAutowireComposer {

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
	private Combobox searchbagian;
	private Combobox searchShift;

	private MyTextbox kode;
	private AmbilDataPasienBanbox pasien;
	private AmbilDataPendaftaranSemuaBanbox pendaftaran;
	private AmbilDataTransaksiBanbox transaksi;
	private Combobox kelasPerawatan;
	private Combobox bagian;
	private MyTextbox namaPenanggung;
	private MyTextbox alamatPenanggung;

	private MyTextbox keterangan;
	private MyDatebox tanggalPembayaran;

	private MyDoublebox biaya;
	private MyDoublebox retur;

	private MyDoublebox totalDeposit;
	private MyDoublebox totalBiaya;

	private MyDoublebox bayarTunai;
	private MyDoublebox bayarNonTunai;
	private MyDoublebox kembalian;

	private Combobox jenisBiayaLain;

	private Label nama;
	private Label umur;
	private Label tglLahir;
	private Label alamat;
	private Label ttl;
	private Combobox jenisPasien;
	private Label jenisKelamin;
	private Label jenis;
	private Label bed;

	private boolean edit = false;
	private boolean delete = false;

	private Pembayaran pembayaran;
	private Toolbarbutton add;

	private Lokasi myLokasi;
	private Shift myShift;

	private PembayaranDetailAction pembayaranDetailAction;
	private PembayaranNonTunaiAction pembayaranNonTunaiAction;
	private ReturDetailAction returDetailAction;

	public ArrayList<Object> pembayaranDetails = new ArrayList<Object>();

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
		if (myLokasi != null) {
			Common.insertComboItems(searchShift, "nama", "jenisShift", CommonSirs.getCurrentShift(myLokasi, true));
		}

		Common.insertCombo(bagian = new Combobox(), "nama", "keterangan", Bagian.class);
		Common.insertCombo(searchbagian, "nama", "keterangan", Bagian.class);
		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Pembayaran Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new Pembayaran());
			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		init(new Pembayaran());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PembayaranAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PembayaranAction
	 */
	class PembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			final Pembayaran pembayaran = (Pembayaran) arg1;

			Pasien pasien = pembayaran.getPasien();

			new Label(pembayaran.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Pembayaran.class, pembayaran,
					pasien == null ? (pembayaran.getTransaksi() == null ? "" : pembayaran.getTransaksi().getNama())
							: pasien.getNama())
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(pembayaran.getTanggalPembayaran())).setParent(arg0);
			new Label(pembayaran.getShift() == null ? "" : pembayaran.getShift().toString()).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);

			new Label(
					pembayaran.getTotalBiaya() == null ? "0.0" : Common.numberFormat.get().format(pembayaran.getTotalBiaya()))
					.setParent(arg0);

			new Label(pembayaran.getTotalDeposit() == null ? "0.0"
					: Common.numberFormat.get().format(pembayaran.getTotalDeposit())).setParent(arg0);

			new Label(
					pembayaran.getBayarTunai() == null ? "0.0" : Common.numberFormat.get().format(pembayaran.getBayarTunai()))
					.setParent(arg0);
			new Label(pembayaran.getBayarNonTunai() == null ? "0.0"
					: Common.numberFormat.get().format(pembayaran.getBayarNonTunai())).setParent(arg0);
			new Label(Common.numberFormat.get().format((pembayaran.getBayar()))).setParent(arg0);

			new Label(pembayaran.getKembalian() == null ? "0.0" : Common.numberFormat.get().format(pembayaran.getKembalian()))
					.setParent(arg0);

			new Label(pembayaran.getPostingHistory() == null ? "Belum" : pembayaran.getPostingHistory().toString())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Struk Pembayaran");
			button.setVisible(pembayaran.getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakPembayaran(pembayaran);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && pembayaran.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembayaran);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && pembayaran.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(pembayaran);
				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final Pembayaran pembayaran) throws Exception {

		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {

								// session.createSQLQuery(
								// "delete from data_pasien_keluar where pembayaran = "
								// + pembayaran.getId())
								// .executeUpdate();
								//

								// Sesi KHUSUS bertransaksi sendiri (openSession) — WAJIB ditutup di blok finally agar koneksi tak bocor.
								Session sesiHapus = null;
								try {
									sesiHapus = HibernateUtil.getSessionFactory().openSession();

								sesiHapus.getTransaction().begin();

								List<DataPasienKeluar> dataPasienKeluars = sesiHapus
										.createCriteria(DataPasienKeluar.class)
										.add(Restrictions.eq("pembayaran", pembayaran)).list();
								DataPasienKeluarAction action = new DataPasienKeluarAction();

								for (DataPasienKeluar dataPasienKeluar : dataPasienKeluars) {
									action.onDelete(dataPasienKeluar, sesiHapus);
								}
								sesiHapus.getTransaction().commit();

								sesiHapus.getTransaction().begin();

								sesiHapus.createSQLQuery(
										"delete from deposit_punya_pembayaran where pembayaran = " + pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update sirs.pendaftaran set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update diagnosa_penyakit set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update data_pasien_keluar set pembayaran = null where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update transaksi_retur set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update transaksi set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update detail_transaksi set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();
								sesiHapus.createSQLQuery(
										"update detail_transaksi_layanan set pembayaran = null, lunas = false where pembayaran = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"delete from pembayaran_non_tunai where pembayaran = " + pembayaran.getId())
										.executeUpdate();

								sesiHapus.createSQLQuery(
										"update sirs.pembayaran set data_pasien_keluar = null where id = "
												+ pembayaran.getId())
										.executeUpdate();

								sesiHapus.getTransaction().commit();

								// sesiHapus.disconnect();
								} catch (Exception eHapus) {
									if (sesiHapus != null) {
										try { if (sesiHapus.getTransaction() != null && sesiHapus.getTransaction().isActive()) sesiHapus.getTransaction().rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/PembayaranAction.java:368");}
									}
									throw eHapus;
								} finally {
									if (sesiHapus != null) {
										try { if (sesiHapus.isOpen()) { sesiHapus.disconnect(); sesiHapus.close(); } } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sirs/PembayaranAction.java:373");}
									}
								}

								Thread.sleep(1000);
								Session session = HibernateUtil.currentSession(); // request session — JANGAN ditutup manual (auto-close).
								session.refresh(pembayaran);
								session.delete(pembayaran);
								// session.getTransaction().commit();

								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepas terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, hubungi Administrator sistem.",
										e.getMessage()));
							}

						}

					}
				});

	}

	public void onAdd(Event event) throws Exception {
		init(new Pembayaran());
	}

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pasien pasien = (Pasien) PembayaranAction.this.pasien.getAttribute("pasien");
			PembayaranAction.this.pasien.setValue(pasien == null ? "" : pasien.getKode().trim());
			nama.setValue(pasien == null ? "" : pasien.getNama());

			umur.setValue(pasien == null ? "" : pasien.getUmur().toString() + " thn");
			tglLahir.setValue(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(
					pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			Common.selectComboItem(jenisPasien, pasien == null ? null : pasien.getJenisPasien());

			Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
			String bed = myPendaftaran == null ? ""
					: (myPendaftaran.getRuangPerawatan() == null ? "" : myPendaftaran.getRuangPerawatan().getNama())
							+ " - "
							+ (myPendaftaran.getKamarPerawatan() == null ? ""
									: myPendaftaran.getKamarPerawatan().getNama())
							+ " - "
							+ (myPendaftaran.getTempatTidur() == null ? "" : myPendaftaran.getTempatTidur().getNama());

			PembayaranAction.this.bed.setValue(bed);

			jenis.setValue(myPendaftaran == null ? "" : myPendaftaran.getJenis());

			pembayaranDetailAction.loadData(null);
			returDetailAction.loadData(null);
			totalBiayaEventListener.onEvent(null);

			loadDeposit();
		}
	};
	private Row rowRetur;

	private Borderlayout totalPembayaranInit(final Pembayaran pembayaran) throws Exception {
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
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya")));
		row.appendChild(biaya = new MyDoublebox(pembayaran.getBiaya()));
		biaya.setWidth("90%");
		biaya.setReadonly(true);
		biaya.setDisabled(true);
		biaya.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bayar Tunai")));
		row.appendChild(bayarTunai = new MyDoublebox(pembayaran.getBayarTunai()));
		bayarTunai.setWidth("90%");
		bayarTunai.setStyle("font-size:xx-large;text-align: right;");

		final Row rowTunai = new Row();
		rowTunai.setStyle("border:0px;background: transparent;");
		rowTunai.setParent(rows);
		rowTunai.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pembayaran Tunai")));
		rowTunai.appendChild(jenisBiayaLain = new Combobox());
		Common.insertCombo(jenisBiayaLain, "nama", "akun", JenisBiayaLain.class,
				Restrictions.eq("jenis", JenisBiayaLain.PEMBAYARAN_KASIR_TUNAI));
		Common.selectComboItem(jenisBiayaLain, pembayaran.getJenisBiayaLain());
		jenisBiayaLain.setWidth("90%");
		if (jenisBiayaLain.getSelectedItem() == null && !jenisBiayaLain.getChildren().isEmpty()) {
			jenisBiayaLain.setSelectedIndex(0);
		}
		jenisBiayaLain.setReadonly(true);

		rowTunai.setVisible(pembayaran.getBayarTunai() > 0.0);
		bayarTunai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowTunai.setVisible(bayarTunai.getValue() != null && bayarTunai.getValue() > 0.0);
			}
		});

		rowRetur = new Row();
		rowRetur.setStyle("border:0px;background: transparent;");
		rowRetur.setParent(rows);
		rowRetur.appendChild(new Label(ais.common.Common.getBahasaConfig("Retur")));
		rowRetur.appendChild(retur = new MyDoublebox(pembayaran.getRetur()));
		retur.setWidth("90%");
		retur.setReadonly(true);
		retur.setDisabled(true);
		retur.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bayar Non Tunai")));
		row.appendChild(bayarNonTunai = new MyDoublebox(pembayaran.getBayarTunai()));
		bayarNonTunai.setWidth("90%");
		bayarNonTunai.setReadonly(true);
		bayarNonTunai.setDisabled(true);
		bayarNonTunai.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Deposit")));
		row.appendChild(totalDeposit = new MyDoublebox(pembayaran.getTotalDeposit()));
		totalDeposit.setWidth("90%");
		totalDeposit.setReadonly(true);
		totalDeposit.setDisabled(true);
		totalDeposit.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Tagihan")));
		row.appendChild(totalBiaya = new MyDoublebox(pembayaran.getTotalBiaya()));
		totalBiaya.setWidth("90%");
		totalBiaya.setReadonly(true);
		totalBiaya.setDisabled(true);
		totalBiaya.setStyle("font-size:xx-large;text-align: right;");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kembalian")));
		row.appendChild(kembalian = new MyDoublebox(pembayaran.getKembalian()));
		kembalian.setWidth("90%");
		kembalian.setReadonly(true);
		kembalian.setDisabled(true);
		kembalian.setStyle("font-size:xx-large;text-align: right;");

		bayarTunai.addEventListener("onChange", totalBiayaEventListener);
		bayarTunai.addEventListener("onOK", totalBiayaEventListener);
		totalBiayaEventListener.onEvent(null);

		return borderlayout;
	}

	private EventListener totalBiayaEventListener = new EventListener() {

		@Override
		public void onEvent(Event event) throws Exception {

			Double myBiaya = biaya.getValue() == null ? 0.0 : biaya.getValue();

			Double myRetur = retur.getValue() == null ? 0.0 : retur.getValue();

			Double myBayarTunai = bayarTunai.getValue() == null ? 0.0 : bayarTunai.getValue();
			Double myBayarNonTunai = bayarNonTunai.getValue() == null ? 0.0 : bayarNonTunai.getValue();

			Double jumlahDeposit = hitungDeposit();

			Double myTotalBiaya = (myBiaya) - (myRetur);

			totalBiaya.setValue(myTotalBiaya);

			Double myKembalian = (myBayarTunai + jumlahDeposit + myBayarNonTunai) - myTotalBiaya;

			System.out.println("myBiaya = " + myBiaya + ", myRetur = " + myRetur + ", myBayarTunai = " + myBayarTunai
					+ ", myBayarNonTunai = " + myBayarNonTunai + ", jumlahDeposit = " + jumlahDeposit
					+ ", myTotalBiaya = " + myTotalBiaya + ", myKembalian = " + myKembalian);

			if (myKembalian < 0.0) {
				kembalian.setValue(0.0);
			} else {
				kembalian.setValue((myKembalian));
			}

		}
	};

	private Grid gridDeposit;
	private Tab tabRetur;
	private Tab tabDeposit;

	private Borderlayout initDeposit(final Pembayaran pembayaran) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		gridDeposit = new Grid();
		gridDeposit.setParent(center);
		gridDeposit.setWidth("100%");
		gridDeposit.setHeight("100%");

		loadDeposit();
		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private Double hitungDeposit() throws Exception {
		List<Row> rows = gridDeposit.getRows().getChildren();
		Double jumlahDeposit = 0.0;
		for (Row row : rows) {
			Combobox jenisPembayaran = (Combobox) row.getChildren().get(1);
			MyDoublebox diambil = (MyDoublebox) row.getChildren().get(5);
			Double nilai = (diambil.getValue() == null ? 0.0 : diambil.getValue());
			if (jenisPembayaran.getSelectedItem() == null && nilai > 0.0) {
				tabDeposit.setSelected(true);
				jenisPembayaran.focus();
				jumlahDeposit = 0.0;
				totalDeposit.setValue(jumlahDeposit);
				return jumlahDeposit;
			}

			jumlahDeposit += nilai;
		}
		totalDeposit.setValue(jumlahDeposit);
		return jumlahDeposit;
	}

	@SuppressWarnings("unchecked")
	private void loadDeposit() {
		Common.clear(gridDeposit);

		Columns columns = new Columns();
		columns.setParent(gridDeposit);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Deposit");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Digunakan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("15%");
		column.setAlign("right");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terpakai");
		column.setWidth("15%");
		column.setAlign("right");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Sisa");
		column.setWidth("15%");
		column.setAlign("right");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Diambil");
		column.setWidth("20%");
		column.setAlign("right");

		final Pasien myPasien = (Pasien) pasien.getAttribute("pasien");

		Session session = HibernateUtil.currentSession();
		List<DepositPasien> deposits = session.createCriteria(DepositPasien.class).addOrder(Order.desc("id"))
				.createAlias("pendaftaran", "pendaftaran")
				.add(myPasien == null ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.eq("pendaftaran.pasien", myPasien))
				.add(Restrictions.sqlRestriction("nilai > diambil")).list();

		Rows rows = new Rows();
		rows.setParent(gridDeposit);

		final Footer footerTotal = new Footer();

		Double jumlahDeposit = 0.0;
		for (final DepositPasien deposit : deposits) {

			DepositPunyaPembayaran depositPunyaPembayaran = pembayaran == null || pembayaran.getId() == null ? null
					: (DepositPunyaPembayaran) session.createCriteria(DepositPunyaPembayaran.class)
							.add(Restrictions.eq("deposit", deposit)).add(Restrictions.eq("pembayaran", pembayaran))
							.setMaxResults(1).uniqueResult();

			final Row row = new Row();
			row.setAttribute("deposit", deposit);
			row.setParent(rows);

			new Label(deposit.getJenisBiayaLain() == null ? "" : deposit.getJenisBiayaLain().getNama()).setParent(row);

			final Combobox jenisPembayaran;
			row.appendChild(jenisPembayaran = new Combobox());
			Common.insertCombo(jenisPembayaran, "nama", "akun", JenisBiayaLain.class,
					Restrictions.eq("jenis", JenisBiayaLain.PEMBAYARAN_DEPOSIT));
			Common.selectComboItem(jenisPembayaran,
					depositPunyaPembayaran == null ? null : depositPunyaPembayaran.getJenisPembayaran());
			jenisPembayaran.setWidth("90%");

			jenisPembayaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Double jumlahDeposit = hitungDeposit();
					footerTotal.setLabel(Common.numberFormat.get().format(jumlahDeposit));
					totalBiayaEventListener.onEvent(null);
				}
			});

			new Label(Common.numberFormat.get().format(deposit.getNilai())).setParent(row);

			deposit.hitungDiambil();

			new Label(Common.numberFormat.get().format(deposit.getDiambil())).setParent(row);

			final Double sisaDeposit = deposit.getNilai() - deposit.getDiambil();

			new Label(Common.numberFormat.get().format(sisaDeposit)).setParent(row);

			final MyDoublebox diambil = new MyDoublebox(
					depositPunyaPembayaran == null ? 0.0 : depositPunyaPembayaran.getNilai());
			diambil.setWidth("90%");
			diambil.setParent(row);
			diambil.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (biaya.getValue() < (diambil.getValue() == null ? 0.0 : diambil.getValue())) {
						MyMessageboxConfig.show("Nilai deposit yang diambil tidak boleh melebihi nilai tagihan. Langkah yang dapat dilakukan: (1) periksa kembali nilai deposit yang dimasukkan; (2) masukkan nilai yang tidak melebihi total tagihan; (3) simpan kembali data pembayaran.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						diambil.setValue(0.0);
					}

					if (sisaDeposit < (diambil.getValue() == null ? 0.0 : diambil.getValue())) {
						MyMessageboxConfig.show("Nilai deposit yang diambil tidak boleh melebihi sisa deposit. Langkah yang dapat dilakukan: (1) periksa kembali sisa deposit yang tersedia; (2) masukkan nilai yang tidak melebihi sisa deposit; (3) simpan kembali data pembayaran.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						diambil.setValue(0.0);
					}

					Double jumlahDeposit = hitungDeposit();
					footerTotal.setLabel(Common.numberFormat.get().format(jumlahDeposit));

					totalBiayaEventListener.onEvent(null);
				}
			});

			jumlahDeposit += deposit.getDiambil();
		}

		Foot foot = new Foot();
		foot.setParent(gridDeposit);

		Footer footer = new Footer("Total Deposit");
		footer.setParent(foot);
		footer = new Footer();
		footer.setParent(foot);
		footer = new Footer();
		footer.setParent(foot);
		footer = new Footer();
		footer.setParent(foot);
		footer = new Footer();
		footer.setParent(foot);
		footerTotal.setLabel(Common.numberFormat.get().format(jumlahDeposit));
		footerTotal.setParent(foot);

		if (totalDeposit != null) {
			totalDeposit.setValue(jumlahDeposit);
		}
	}

	@SuppressWarnings("deprecation")
	private Borderlayout mainInit(final Pembayaran pembayaran) throws Exception {
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

		Row row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pembayaran")));
		String mykode = pembayaran.getKode();
		if (mykode == null || mykode.trim().equals("")) {
			// mykode = Common.generateCode(Pembayaran.class, 8);
		}
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");
		kode.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil Transaksi Pasien Bebas")));
		row.appendChild(transaksi = new AmbilDataTransaksiBanbox(false, null, null, true, true, true));
		transaksi.setAttribute("transaksi", pembayaran.getTransaksi());
		transaksi.setValue(pembayaran.getTransaksi() == null ? "" : pembayaran.getTransaksi().getKode());
		transaksi.setWidth("90%");

		EventListener transaksieventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TransaksiMedis mytransaksi = (TransaksiMedis) transaksi.getAttribute("transaksi");

				@SuppressWarnings("unchecked")
				List<Row> myRows = rows.getChildren();
				for (Row row : myRows) {
					if (row.getAttribute("hide") == null) {
						row.setVisible(mytransaksi == null);
					}
				}

				if (mytransaksi != null) {
					Pembayaran temp = mytransaksi.getPembayaran();
					if (temp != null && pembayaran.getId() == null) {
						init(temp);
					} else {

						nama.setValue(mytransaksi.getNama());

						pasien.setValue(mytransaksi.getPasien() == null ? "" : mytransaksi.getPasien().getKode());
						pasien.setAttribute("pasien", mytransaksi.getPasien());
						pasien.setDisabled(true);
						Common.selectComboItem(kelasPerawatan, ConstantValues.kelasNormal);

						Pendaftaran mypendaftaran = mytransaksi.getPendaftaran();
						if (mypendaftaran != null) {
							pendaftaran.setAttribute("pendaftaran", mypendaftaran);
							pendaftaran.setValue(mypendaftaran == null ? "" : mypendaftaran.getKode());
							pendaftaran.setDisabled(true);
							namaPenanggung.setValue(mypendaftaran.getNamaPenjamin());
							alamatPenanggung.setValue(mypendaftaran.getAlamatPenjamin());
						}
						perubahanPasienListener.onEvent(arg0);
					}

				}
			}
		};

		transaksi.setEventListener(transaksieventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil Pendaftaran")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranSemuaBanbox(false));
		pendaftaran.setAttribute("pendaftaran", pembayaran.getPendaftaran());
		pendaftaran.setValue(pembayaran.getPendaftaran() == null ? "" : pembayaran.getPendaftaran().getKode());
		pendaftaran.setWidth("90%");

		EventListener pendaftaraneventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pendaftaran mypendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");

				if (mypendaftaran != null) {

					Pembayaran temp = mypendaftaran.getPembayaran();
					if (temp != null && pembayaran.getId() == null) {
						init(temp);
					} else {
						pasien.setValue(mypendaftaran.getPasien() == null ? "" : mypendaftaran.getPasien().getKode());
						pasien.setAttribute("pasien", mypendaftaran.getPasien());
						pasien.setDisabled(true);
						Common.selectComboItem(kelasPerawatan,
								mypendaftaran.getKelasPerawatan() == null ? ConstantValues.kelasNormal
										: mypendaftaran.getKamarPerawatan());
						namaPenanggung.setValue(mypendaftaran.getNamaPenjamin());
						alamatPenanggung.setValue(mypendaftaran.getAlamatPenjamin());

						Session session = HibernateUtil.currentSession();
						TransaksiMedis mytransaksi = (TransaksiMedis) session.createCriteria(TransaksiMedis.class)
								.add(Restrictions.eq("bebas", true)).add(Restrictions.eq("pendaftaran", mypendaftaran))
								.setMaxResults(1).uniqueResult();

						if (mytransaksi != null) {
							transaksi.setAttribute("transaksi", mytransaksi);
							transaksi.setValue(mytransaksi == null ? "" : mytransaksi.getKode());
						}
						transaksi.setDisabled(true);
						perubahanPasienListener.onEvent(arg0);
					}
				}
			}
		};

		pendaftaran.setEventListener(pendaftaraneventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));
		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setValue(pembayaran.getPasien() == null ? "" : pembayaran.getPasien().getKode());
		pasien.setAttribute("pasien", pembayaran.getPasien());
		pasien.setWidth("90%");
		pasien.setEventListener(perubahanPasienListener);
		pasien.setDisabled(true);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembayaran")));
		row.appendChild(tanggalPembayaran = new MyDatebox(
				pembayaran.getTanggalPembayaran() == null ? new Date() : pembayaran.getTanggalPembayaran()));
		tanggalPembayaran.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembayaran.setWidth("90%");

		final Pasien pasien = pembayaran.getPasien();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new Label(pasien == null ? "" : pasien.getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bagian (Unit)")));
		Common.selectComboItem(bagian,
				pembayaran.getBagian() == null
						? (Common.getCurrentUser() != null && Common.getCurrentUser().getPegawai() != null
								? Common.getCurrentUser().getPegawai().getBagian()
								: null)
						: pembayaran.getBagian());
		bagian.setDisabled(Common.getCurrentUser() != null && Common.getCurrentUser().getPegawai() != null
				&& Common.getCurrentUser().getPegawai().getBagian() != null);
		row.appendChild(bagian);
		bagian.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur = new Label(pasien == null ? "" : pasien.getUmur().toString() + " thn"));

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat")));
		row.appendChild(ttl = new Label(
				pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir())));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Lahir")));
		row.appendChild(
				tglLahir = new Label(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class);
		Common.selectComboItem(kelasPerawatan,
				pembayaran.getKelasPerawatan() == null ? ConstantValues.kelasNormal : pembayaran.getKelasPerawatan());
		kelasPerawatan.setWidth("90%");
		kelasPerawatan.setDisabled(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin()));

		Pendaftaran myPendaftaran = pembayaran.getPendaftaran();

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Perawatan")));
		row.appendChild(jenis = new Label(myPendaftaran == null ? "" : myPendaftaran.getJenis()));

		String bed = myPendaftaran == null ? ""
				: (myPendaftaran.getRuangPerawatan() == null ? "" : myPendaftaran.getRuangPerawatan().getNama()) + " - "
						+ (myPendaftaran.getKamarPerawatan() == null ? "" : myPendaftaran.getKamarPerawatan().getNama())
						+ " - "
						+ (myPendaftaran.getTempatTidur() == null ? "" : myPendaftaran.getTempatTidur().getNama());

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(this.bed = new Label(bed));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Penjamin")));
		row.appendChild(namaPenanggung = new MyTextbox(pembayaran.getNamaPenanggung()));
		namaPenanggung.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);
		Common.selectComboItem(jenisPasien,
				pembayaran.getPendaftaran() == null ? null : pembayaran.getPendaftaran().getJenisPasien());
		jenisPasien.setWidth("90%");

		row = new Row();
		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat Penjamin")));
		row.appendChild(alamatPenanggung = new MyTextbox(pembayaran.getNamaPenanggung()));
		alamatPenanggung.setWidth("90%");
		alamatPenanggung.setRows(2);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new MyTextbox(pembayaran.getKeterangan() == null ? "" : pembayaran.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		CommonSirs.initLokasiDanShift(pembayaran.getLokasi() == null ? myLokasi : pembayaran.getLokasi(),
				pembayaran.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
						if (kode.getValue().trim().equals("")) {
							String mykode = Common.generateCode(Pembayaran.class, 8, "PEMB", myLokasi);
							kode.setValue(mykode);
						}
					}
				});

		pendaftaraneventListener.onEvent(null);
		if (kode.getValue().trim().equals("") && myLokasi != null) {
			mykode = Common.generateCode(Pembayaran.class, 10, "PEM", myLokasi);
			kode.setValue(mykode);
		}

		return borderlayout;
	}

	private Borderlayout initDetailTransaksi(final Pembayaran pembayaran) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Cetak Informasi Biaya dan Retur",
				"/img/print.png");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
				Pasien myPasien = (Pasien) pasien.getAttribute("pasien");
				TransaksiMedis myTransaksi = (TransaksiMedis) transaksi.getAttribute("transaksi");
				LaporanInformasiBiayaDanReturWindow laporanInformasiBiayaDanReturWindow = new LaporanInformasiBiayaDanReturWindow(
						myPendaftaran, myPasien, myTransaksi);
				laporanInformasiBiayaDanReturWindow.setTitle("Laporan Informasi Biaya dan Retur");
				laporanInformasiBiayaDanReturWindow.setHeight("95%");
				laporanInformasiBiayaDanReturWindow.setWidth("95%");
				laporanInformasiBiayaDanReturWindow.setClosable(true);
				page.getFirstRoot().appendChild(laporanInformasiBiayaDanReturWindow);
				laporanInformasiBiayaDanReturWindow.onModal();
			}
		});

		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Cetak Informasi Tagihan", "/img/print.png");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
				Pasien myPasien = (Pasien) pasien.getAttribute("pasien");
				TransaksiMedis myTransaksi = (TransaksiMedis) transaksi.getAttribute("transaksi");
				LaporanInformasiTagihanWindow laporanInformasiTagihanWindow = new LaporanInformasiTagihanWindow(
						myPendaftaran, myPasien, myTransaksi);
				laporanInformasiTagihanWindow.setTitle("Laporan Informasi Tagihan");
				laporanInformasiTagihanWindow.setHeight("95%");
				laporanInformasiTagihanWindow.setWidth("95%");
				laporanInformasiTagihanWindow.setClosable(true);
				page.getFirstRoot().appendChild(laporanInformasiTagihanWindow);
				laporanInformasiTagihanWindow.onModal();
			}
		});

		Center center = new Center();
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab = new Tab("Data Biaya");
		tab.setParent(tabs);

		tabRetur = new Tab("Data Retur");
		tabRetur.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		pembayaranDetailAction = new PembayaranDetailAction();
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(pembayaranDetailAction);

		returDetailAction = new ReturDetailAction();
		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(returDetailAction);

		return borderlayout;
	}

	private void init(final Pembayaran pembayaran) throws Exception {
		this.pembayaran = pembayaran;
		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab = new Tab("Data Pasien / Pembeli");
		tab.setParent(tabs);

		tabDeposit = new Tab("Deposit Pasien");
		tabDeposit.setParent(tabs);

		Tab tabPembayaranNonTunai = new Tab("Pembayaran Non Tunai");
		tabPembayaranNonTunai.setParent(tabs);

		Tab tabCaraPembayaran = new Tab("Total Pembayaran");
		tabCaraPembayaran.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(mainInit(pembayaran));

		Tabpanel tabpanelDeposit = new ais.ui.util.MyTabpanel();
		tabpanelDeposit.setParent(tabpanels);
		tabpanelDeposit.appendChild(initDeposit(pembayaran));

		pembayaranNonTunaiAction = null;
		pembayaranNonTunaiAction = new PembayaranNonTunaiAction();
		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.appendChild(pembayaranNonTunaiAction);

		final Tabpanel tabpanelCaraPembayaran = new ais.ui.util.MyTabpanel();
		tabpanelCaraPembayaran.setParent(tabpanels);
		tabpanelCaraPembayaran.appendChild(totalPembayaranInit(pembayaran));

		East east = new East();
		east.setStyle("border:0px;background: transparent;");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("55%");
		east.appendChild(initDetailTransaksi(pembayaran));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Pembayaran", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if ((bayarTunai.getValue() == null || bayarTunai.getValue().equals(0.0))
						&& (bayarNonTunai.getValue() == null || bayarNonTunai.getValue().equals(0.0))
						&& (totalDeposit.getValue() == null || totalDeposit.getValue().equals(0.0))) {
					MyMessageboxConfig.show("Jumlah pembayaran belum sesuai. Langkah yang dapat dilakukan: (1) masukkan nilai bayar tunai, bayar non tunai, atau deposit; (2) pastikan salah satu nilai pembayaran lebih besar dari nol; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Double jumlahDibayar = (bayarTunai.getValue() == null ? 0.0 : bayarTunai.getValue())
						+ (bayarNonTunai.getValue() == null ? 0.0 : bayarNonTunai.getValue())
						+ (totalDeposit.getValue() == null ? 0.0 : totalDeposit.getValue());
				if (jumlahDibayar < (totalBiaya.getValue() == null ? 0.0 : totalBiaya.getValue())) {
					MyMessageboxConfig.show("Jumlah pembayaran masih kurang dari total tagihan. Langkah yang dapat dilakukan: (1) periksa kembali total tagihan yang harus dibayar; (2) tambahkan nilai pembayaran agar sama dengan atau melebihi total tagihan; (3) simpan kembali data pembayaran.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;

					// Messagebox
					// .show("Jumlah pembayaran kurang atau lebih kecil dari total tagihan, apakah
					// Anda ingin melanjutkan ?",
					// "Pertanyaan", Messagebox.OK
					// | Messagebox.CANCEL,
					// Messagebox.QUESTION, new EventListener() {
					//
					// @Override
					// public void onEvent(Event event)
					// throws Exception {
					// int i = new Integer(event.getData()
					// .toString());
					// if (i == Messagebox.OK) {
					//
					// if (onSave(event)) {
					// onSearchDefault(null);
					// }
					//
					// }
					//
					// }
					// });

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
				if (pembayaran != null && pembayaran.getId() != null) {
					onDelete(pembayaran);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pembayaran ini? Data pembayaran yang sedang diisi tidak akan disimpan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new Pembayaran());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		pembayaranDetailAction.loadData(null);
		pembayaranNonTunaiAction.loadData(null);
		returDetailAction.loadData(null);

		if (pembayaran.getId() != null) {
			Common.freeze(tambahData, true);
			namaPenanggung.setDisabled(false);
			alamatPenanggung.setDisabled(false);
			keterangan.setDisabled(false);
		}
		add.setDisabled(false);
		save.setDisabled(false);

	}

	private Boolean saving = false;
	public List<TransaksiMedis> transaksis = new ArrayList<TransaksiMedis>();

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Lokasi harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi pada bagian data pembayaran; (2) pastikan lokasi telah terisi; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Shift harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih shift pada bagian data pembayaran; (2) pastikan shift telah terisi; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pembayaranDetails == null || pembayaranDetails.isEmpty()) {
			MyMessageboxConfig.show("Tagihan tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien, pendaftaran, atau transaksi telah dipilih dengan benar; (2) periksa kembali data tagihan pada sistem; (3) apabila kendala berlanjut, hubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalPembayaran.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Pembayaran harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Tanggal Pembayaran; (2) pastikan format tanggal telah benar; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (bagian.getSelectedItem() == null) {
			MyMessageboxConfig.show("Bagian (Unit) harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Bagian (Unit) pada bagian data pembayaran; (2) pastikan Bagian (Unit) telah terisi; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisBiayaLain.getSelectedItem() == null || jenisBiayaLain.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis pembayaran harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih jenis pembayaran pada bagian Total Pembayaran; (2) pastikan jenis pembayaran telah terisi; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pembayaranNonTunaiAction != null) {
			for (PembayaranNonTunai pembayaranNonTunai : pembayaranNonTunaiAction.pembayaranNonTunais.values()) {
				if (pembayaranNonTunai.getAmount() == null || pembayaranNonTunai.getAmount() < 0.1) {
					MyMessageboxConfig.show("Apabila terdapat pembayaran non tunai, jumlah pembayaran tidak boleh bernilai nol. Langkah yang dapat dilakukan: (1) periksa kembali data pembayaran non tunai; (2) isi jumlah pembayaran non tunai dengan nilai yang benar; (3) simpan kembali data pembayaran.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (pembayaranNonTunai.getJenisPembayaran() != null && pembayaranNonTunai.getJenisBiayaLain() == null) {
					MyMessageboxConfig.show("Apabila terdapat pembayaran non tunai, asuransi atau bank harus dipilih. Langkah yang dapat dilakukan: (1) buka data pembayaran non tunai; (2) pilih asuransi atau bank yang sesuai; (3) simpan kembali data pembayaran.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (pembayaranNonTunai.getJenisPembayaran() != null && (pembayaranNonTunai.getNomorKartu() == null
						|| pembayaranNonTunai.getNomorKartu().trim().equals(""))) {
					MyMessageboxConfig.show("Apabila terdapat pembayaran non tunai selain asuransi, nomor kartu harus diisi. Langkah yang dapat dilakukan: (1) buka data pembayaran non tunai; (2) isi nomor kartu dengan benar; (3) simpan kembali data pembayaran.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		List<Row> rows = gridDeposit.getRows().getChildren();
		for (Row row : rows) {
			Combobox jenisPembayaran = (Combobox) row.getChildren().get(1);
			MyDoublebox diambil = (MyDoublebox) row.getChildren().get(5);
			Double nilai = (diambil.getValue() == null ? 0.0 : diambil.getValue());
			if (jenisPembayaran.getSelectedItem() == null && nilai > 0.0) {
				MyMessageboxConfig.show("Jenis pembayaran deposit harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka tab Deposit Pasien; (2) pilih jenis pembayaran deposit pada baris yang bersangkutan; (3) simpan kembali data pembayaran.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				tabDeposit.setSelected(true);
				jenisPembayaran.focus();
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (pembayaran.getId() != null) {
			pembayaran = (Pembayaran) session.load(Pembayaran.class, pembayaran.getId());
		}

		pembayaran.setJenisBiayaLain((JenisBiayaLain) jenisBiayaLain.getSelectedItem().getValue());
		pembayaran.setTotalDeposit(totalDeposit.getValue());
		pembayaran.setHargaPpn(0.0);
		pembayaran.setBiaya(biaya.getValue());
		pembayaran.setRetur(retur.getValue());
		pembayaran.setDiskon(0.0);
		pembayaran.setDiskonDalamPersen(false);
		pembayaran.setTotalDiskon(0.0);
		pembayaran.setPpn(0.0);
		pembayaran.setTotalBiaya(totalBiaya.getValue());
		pembayaran.setBayarTunai(bayarTunai.getValue());
		pembayaran.setBayarNonTunai(bayarNonTunai.getValue());
		pembayaran.setKembalian(kembalian.getValue());

		pembayaran.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		pembayaran.setTransaksi((TransaksiMedis) transaksi.getAttribute("transaksi"));
		pembayaran.setJenisPasien((JenisPasien) (jenisPasien.getSelectedItem() == null ? null
				: jenisPasien.getSelectedItem().getValue()));
		pembayaran.setUmur(umur.getValue().trim().equals("") ? null
				: Integer.parseInt(umur.getValue().trim().replaceAll(" thn", "")));

		pembayaran.setTanggalPembayaran(tanggalPembayaran.getValue());

		pembayaran.setPasien((Pasien) pasien.getAttribute("pasien"));
		pembayaran.setBagian((Bagian) bagian.getSelectedItem().getValue());
		pembayaran.setKode(kode.getValue());
		pembayaran.setKeterangan(keterangan.getValue());
		pembayaran.setTbmuser(Common.getCurrentUser());
		pembayaran.setLokasi(myLokasi);
		pembayaran.setShift(myShift);

		if (pembayaran.getId() != null) {
			saving = false;
			Common.refreshUpdate(session, pembayaran);
		} else {
			saving = true;
			pembayaran.setIndex(Common.generateMaxByLokasi(Pembayaran.class, myLokasi) + 1);
			String mykode = Common.generateCode(Pembayaran.class, 10, "PEM", myLokasi);
			kode.setValue(mykode);
			pembayaran.setKode(mykode);
			session.save(pembayaran);

		}

		simpanDetailTransaksi(pembayaran);

		session.createSQLQuery("delete from sirs.pembayaran_non_tunai where pembayaran = " + pembayaran.getId())
				.executeUpdate();

		if (pembayaranNonTunaiAction != null) {
			for (PembayaranNonTunai pembayaranNonTunai : pembayaranNonTunaiAction.pembayaranNonTunais.values()) {
				pembayaranNonTunai.setId(null);
				pembayaranNonTunai.setPembayaran(pembayaran);
				session.save(pembayaranNonTunai);
			}
		}

		MyMessageboxConfig.show("Data pembayaran berhasil disimpan.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.freeze(tambahData, true);
						add.setDisabled(false);

						if (saving && pembayaran.getPendaftaran() != null
								&& pembayaran.getPendaftaran().getJenis().equals(Pendaftaran.RAWAT_INAP)) {
							MyMessageboxConfig.show(
									"Apakah Bapak/Ibu ingin memasukkan pasien rawat inap ini sebagai pasien keluar? Data pasien keluar akan dibuat berdasarkan pembayaran ini.",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = new Integer(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {

												DataPasienKeluar dataPasienKeluar = new DataPasienKeluar();
												dataPasienKeluar.setPembayaran(pembayaran);
												dataPasienKeluar.setPendaftaran(pembayaran.getPendaftaran());
												DataPasienKeluarAction.addExternal(dataPasienKeluar,
														new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																DataPasienKeluar dataPasienKeluar = (DataPasienKeluar) event
																		.getData();
																Session session = HibernateUtil.currentSession();
																session.refresh(pembayaran);
																pembayaran.setDataPasienKeluar(dataPasienKeluar);

																session.update(pembayaran);

																onCetakPembayaran(pembayaran);
															}
														});

											} else {
												onCetakPembayaran(pembayaran);
											}
										}
									});
						} else {
							onCetakPembayaran(pembayaran);
						}
					}
				});

		return true;
	}

	// @SuppressWarnings("unchecked")
	// private void simpanDiskonDanPajak(Pembayaran pembayaran) {
	//
	// Session session = HibernateUtil.currentSession();
	// List<TransaksiDetail> transaksiDetails = session
	// .createCriteria(TransaksiDetail.class)
	// .add(Restrictions.eq("transaksi", transaksi)).list();
	// session.createSQLQuery(
	// "delete from detail_transaksi where racikan_detail in (select id from
	// racikan_detail where racikan in (select id from racikan where
	// transaksi_detail in (select id from transaksi_detail where transaksi = "
	// + transaksi.getId()
	// + "))) and (kode_transaksi = "
	// + ConstantValues.pajak.getId()
	// + " or kode_transaksi = "
	// + ConstantValues.diskonJual.getId() + ");")
	// .executeUpdate();
	// session.createSQLQuery(
	// "delete from detail_transaksi where transaksi_detail in (select id from
	// transaksi_detail where transaksi = "
	// + transaksi.getId()
	// + ") and (kode_transaksi = "
	// + ConstantValues.pajak.getId()
	// + " or kode_transaksi = "
	// + ConstantValues.diskonJual.getId() + ");")
	// .executeUpdate();
	//
	// session.createSQLQuery(
	// "delete from detail_transaksi_layanan where transaksi_detail in (select id
	// from transaksi_detail where transaksi = "
	// + transaksi.getId()
	// + ") and (kode_transaksi = "
	// + ConstantValues.pajak.getId()
	// + " or kode_transaksi = "
	// + ConstantValues.diskonJual.getId() + ");")
	// .executeUpdate();
	//
	// }

	@SuppressWarnings("unchecked")
	private void simpanDetailTransaksi(Pembayaran pembayaran) {
		Session session = HibernateUtil.currentSession();

		List<Row> rows = gridDeposit.getRows().getChildren();
		for (Row row : rows) {
			Combobox jenisPembayaran = (Combobox) row.getChildren().get(1);
			MyDoublebox diambil = (MyDoublebox) row.getChildren().get(5);
			Double nilai = (diambil.getValue() == null ? 0.0 : diambil.getValue());
			if (nilai > 0.0) {
				DepositPasien deposit = (DepositPasien) row.getAttribute("deposit");
				session.refresh(deposit);
				DepositPunyaPembayaran depositPunyaPembayaran = (DepositPunyaPembayaran) session
						.createCriteria(DepositPunyaPembayaran.class).add(Restrictions.eq("deposit", deposit))
						.add(Restrictions.eq("pembayaran", pembayaran)).setMaxResults(1).uniqueResult();
				if (depositPunyaPembayaran == null) {
					depositPunyaPembayaran = new DepositPunyaPembayaran();
				}
				depositPunyaPembayaran
						.setJenisPembayaran((JenisBiayaLain) jenisPembayaran.getSelectedItem().getValue());
				depositPunyaPembayaran.setDeposit(deposit);
				depositPunyaPembayaran.setPembayaran(pembayaran);
				depositPunyaPembayaran.setNilai(nilai);
				session.saveOrUpdate(depositPunyaPembayaran);

				Common.refreshUpdate(session, deposit);
			}
		}

		for (Object o : pembayaranDetails) {
			if (o instanceof DetailTransaksi) {
				DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) o;
				session.refresh(detailTransaksi);
				detailTransaksi.setLunas(true);
				detailTransaksi.setPembayaran(pembayaran);
				Common.refreshUpdate(session, detailTransaksi);
			}
			if (o instanceof DetailTransaksiLayanan) {
				DetailTransaksiLayanan detailTransaksiLayanan = (DetailTransaksiLayanan) o;
				session.refresh(detailTransaksiLayanan);
				detailTransaksiLayanan.setLunas(true);
				detailTransaksiLayanan.setPembayaran(pembayaran);
				Common.refreshUpdate(session, detailTransaksiLayanan);
			}
		}

		for (TransaksiMedis transaksi : transaksis) {
			TransaksiMedis newTransaksi = (TransaksiMedis) session.createCriteria(TransaksiMedis.class)
					.add(Restrictions.idEq(transaksi.getId())).uniqueResult();
			newTransaksi.setLunas(true);
			newTransaksi.setPembayaran(pembayaran);
			Common.refreshUpdate(session, newTransaksi);

			Pendaftaran pendaftaran = newTransaksi.getPendaftaran();
			if (pendaftaran != null) {
				pendaftaran.setLunas(true);
				pendaftaran.setPembayaran(pembayaran);
				Common.refreshUpdate(session, pendaftaran);
			}
		}

		List<TransaksiRetur> transaksiReturs = session.createCriteria(TransaksiRetur.class)
				.createAlias("transaksi", "transaksi")
				.add(Restrictions.or(Restrictions.isNull("lunas"), Restrictions.eq("lunas", false)))
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("transaksi.pendaftaran", pembayaran.getPendaftaran()),
								Restrictions.eq("transaksi.pasien", pembayaran.getPasien())),
						Restrictions.eq("transaksi", pembayaran.getTransaksi())))
				.list();

		for (TransaksiRetur transaksiRetur : transaksiReturs) {
			transaksiRetur.setLunas(true);
			transaksiRetur.setPembayaran(pembayaran);
			List<DetailTransaksiPasien> detailTransaksis = session.createCriteria(DetailTransaksiPasien.class)
					.createAlias("transaksiReturDetail", "transaksiReturDetail")
					.add(Restrictions.eq("transaksiReturDetail.transaksiRetur", transaksiRetur)).list();
			for (DetailTransaksiPasien detailTransaksi : detailTransaksis) {
				detailTransaksi.setLunas(true);
				detailTransaksi.setPembayaran(pembayaran);
				Common.refreshUpdate(session, (detailTransaksi));
			}
			Common.refreshUpdate(session, transaksiRetur);
		}

		Pendaftaran pendaftaran = (Pendaftaran) this.pendaftaran.getAttribute("pendaftaran");
		if (pendaftaran != null) {
			pendaftaran.setLunas(true);
			pendaftaran.setPembayaran(pembayaran);
			Common.refreshUpdate(session, pendaftaran);
		}
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pembayaran.class).createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmr.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add(searchbagian.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("bagian", searchbagian.getSelectedItem().getValue()))

				.add(searchShift.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("shift", searchShift.getSelectedItem().getValue()))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Pembayaran> pembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaran);
		grid.setRowRenderer(new PembayaranRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Pembayaran pembayaran) {

		try {
			Pasien pasien = pembayaran.getPasien();
			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(Pembayaran.class, pembayaran, parameters, "byr");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			File file = Report.generateFileReport("sirs/data_identitas_pasien", Report.PDF, parameters,
					"sirs/data_identitas_pasien", new Date());

			Report.tampil(file, parameters, "sirs/data_identitas_pasien");

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Tipe implementasi bersarang {@link PembayaranDetailAction} milik {@link PembayaranAction}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}, {@code Footer
	 * totalDiskon}, {@code Footer totalPajak}, {@code Footer total}, {@code Footer totalHrg}; operasi lokal:
	 * {@code loadData()}, {@code loadTotal()}, {@code display}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see PembayaranAction
	 */
	public class PembayaranDetailAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;
		private Footer totalDiskon;
		private Footer totalPajak;
		private Footer total;
		private Footer totalHrg;

		public PembayaranDetailAction() throws Exception {
			super();
			display();
		}

		/**
		 * Renderer lokal untuk layar/komponen {@link PembayaranDetailAction}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranDetailAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see PembayaranDetailAction
		 */
		class PembayaranDetailRenderer extends ais.ui.util.MyRowRenderer {

			public PembayaranDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");

				if (data instanceof DetailTransaksiPasien) {

					final DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) data;

					new Label(detailTransaksi.getTransaksiDetail() == null ? ""
							: detailTransaksi.getTransaksiDetail().getTransaksi().getKode()).setParent(row);
					ItemMedis item = detailTransaksi.getItem();
					new Label(item == null ? "" : item.getKode()).setParent(row);
					new Label(item == null ? "" : item.getNama()).setParent(row);
					new Label(item == null || item.getJenisItem() == null ? "" : item.getJenisItem().getNama())
							.setParent(row);
					new Label(Common.numberFormat.get().format(detailTransaksi.getQty())).setParent(row);
					new Label(item == null || item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama())
							.setParent(row);
					new Label(detailTransaksi.getAmount() == null ? "0.0"
							: Common.numberFormat.get().format(detailTransaksi.getAmount())).setParent(row);

					Label diskon = new Label(Common.numberFormat.get().format(detailTransaksi.getDiskonPersen()) + "% ("
							+ Common.numberFormat.get().format(detailTransaksi.getDiskon()) + ")");
					diskon.setParent(row);
					Label pajak = new Label(Common.numberFormat.get().format(detailTransaksi.getPajakPersen()) + "% ("
							+ Common.numberFormat.get().format(detailTransaksi.getPajak()) + ")");
					pajak.setParent(row);

					(new Label(Common.numberFormat.get().format(detailTransaksi.getHasilPenghitunganTotal()))).setParent(row);

				} else if (data instanceof DetailTransaksiLayanan) {
					final DetailTransaksiLayanan detailTransaksiLayanan = (DetailTransaksiLayanan) data;
					if (detailTransaksiLayanan.getPendaftaran() != null) {
						new Label(detailTransaksiLayanan.getPendaftaran().getKode()).setParent(row);
					}

					else {
						new Label("").setParent(row);
					}
					Tindakan layanan = detailTransaksiLayanan.getTindakan();
					AlatMedis alatMedis = detailTransaksiLayanan.getAlatMedis();

					if (layanan != null) {

						new Label(layanan == null ? "" : layanan.getKode()).setParent(row);
						new Label(layanan == null ? "" : layanan.getNama()).setParent(row);
						new Label(layanan == null || layanan.getJenisTindakan() == null ? ""
								: layanan.getJenisTindakan().getNama()).setParent(row);
						new Label(detailTransaksiLayanan.getQty() == null ? "0.0"
								: Common.numberFormat.get().format(detailTransaksiLayanan.getQty())).setParent(row);
						new Label(ais.common.Common.getBahasaConfig("Perawatan")).setParent(row);
					} else if (alatMedis != null) {
						new Label(alatMedis == null ? "" : alatMedis.getKode()).setParent(row);
						new Label(alatMedis == null ? "" : alatMedis.getNama()).setParent(row);
						new Label(ais.common.Common.getBahasaConfig("Alat Medis")).setParent(row);
						new Label(detailTransaksiLayanan.getQty() == null ? "0.0"
								: Common.numberFormat.get().format(detailTransaksiLayanan.getQty())).setParent(row);
						new Label(alatMedis == null ? "" : alatMedis.getPer()).setParent(row);
					}

					new Label(detailTransaksiLayanan.getAmount() == null ? "0.0"
							: Common.numberFormat.get().format(detailTransaksiLayanan.getAmount())).setParent(row);

					Label diskon = new Label(Common.numberFormat.get().format(detailTransaksiLayanan.getDiskonPersen())
							+ "% (" + Common.numberFormat.get().format(detailTransaksiLayanan.getDiskon()) + ")");
					diskon.setParent(row);
					Label pajak = new Label(Common.numberFormat.get().format(detailTransaksiLayanan.getPajakPersen()) + "% ("
							+ Common.numberFormat.get().format(detailTransaksiLayanan.getPajak()) + ")");
					pajak.setParent(row);

					(new Label(Common.numberFormat.get().format(detailTransaksiLayanan.getHasilPenghitunganTotal())))
							.setParent(row);

				}

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) throws Exception {
			if (pendaftaran == null || pasien == null || transaksi == null) {
				return;
			}

			pembayaranDetails = null;
			pembayaranDetails = new ArrayList<Object>();

			Session session = HibernateUtil.currentSession();

			Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");

			if (myPendaftaran != null) {
				RawatInapCalculationProcessor.checkPendaftaran(myPendaftaran);
			}

			TransaksiMedis myTransaksi = (TransaksiMedis) transaksi.getAttribute("transaksi");

			transaksis = session.createCriteria(TransaksiMedis.class)
					.add(Restrictions.or(
							pembayaran.getId() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.eq("pembayaran", pembayaran),
							Restrictions.or(Restrictions.isNull("lunas"), Restrictions.eq("lunas", false))))

					.add(Restrictions.or(Restrictions.eq("pendaftaran", myPendaftaran),
							Restrictions.idEq(myTransaksi == null ? null : myTransaksi.getId())))
					.list();

			Criterion or = Restrictions.sqlRestriction("false");

			or = Restrictions.or(or, transaksis.isEmpty() ? Restrictions.sqlRestriction("false")
					: Restrictions.in("transaksiDetail.transaksi", transaksis));

			or = Restrictions.or(or, Restrictions.eq("pendaftaran", myPendaftaran));

			List<DetailTransaksiPasien> detailTransaksis = session.createCriteria(DetailTransaksiPasien.class)
					.add(Restrictions.isNull("transaksiReturDetail"))
					.add(Restrictions.gt("hasilPenghitunganTotal", 0.0))
					.createAlias("transaksiDetail", "transaksiDetail", Criteria.LEFT_JOIN).add(or).list();

			pembayaranDetails.addAll(detailTransaksis);

			List<DetailTransaksiLayanan> detailTransaksiLayanans = session.createCriteria(DetailTransaksiLayanan.class)
					.add(Restrictions.gt("hasilPenghitunganTotal", 0.0))
					.add(Restrictions.or(
							pembayaran.getId() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.eq("pembayaran", pembayaran),
							Restrictions.or(Restrictions.isNull("lunas"), Restrictions.eq("lunas", false))))

					.createAlias("transaksiDetail", "transaksiDetail", Criteria.LEFT_JOIN).add(or).list();

			pembayaranDetails.addAll(detailTransaksiLayanans);

			ListModel strset = new SimpleListModel(pembayaranDetails);
			grid.setRowRenderer(new PembayaranDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

			loadTotal();

			if (pembayaranDetails.size() == 0 && (myPendaftaran != null || myTransaksi != null)) {
				MyMessageboxConfig.show("Tagihan tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien, pendaftaran, atau transaksi telah dipilih dengan benar; (2) periksa kembali data tagihan pada sistem; (3) apabila kendala berlanjut, hubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		}

		public void loadTotal() throws Exception {

			if (pendaftaran == null || pasien == null || transaksi == null) {
				return;
			}

			Double mytotal = 0.0;
			Double myhrg = 0.0;
			Double mydiskon = 0.0;
			Double mypajak = 0.0;

			for (Object data : pembayaranDetails) {
				if (data instanceof DetailTransaksiPasien) {
					DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) data;

					mydiskon += detailTransaksi.getDiskon() * detailTransaksi.getQty();
					mypajak += detailTransaksi.getPajak() * detailTransaksi.getQty();

					mytotal += detailTransaksi.getQty() == null ? 0.0 : (detailTransaksi.getQty());

					Double totalAmount = detailTransaksi.getHasilPenghitunganTotal();

					myhrg += totalAmount;
				} else if (data instanceof DetailTransaksiLayanan) {
					DetailTransaksiLayanan detailTransaksi = (DetailTransaksiLayanan) data;
					mytotal += detailTransaksi.getQty() == null ? 0.0 : detailTransaksi.getQty();

					Double totalAmount = detailTransaksi.getHasilPenghitunganTotal();

					myhrg += totalAmount;

					mydiskon += detailTransaksi.getDiskon() * detailTransaksi.getQty();
					mypajak += detailTransaksi.getPajak() * detailTransaksi.getQty();
				}
			}

			totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));
			totalPajak.setLabel(mypajak == null ? "0.0" : Common.numberFormat.get().format(mypajak));

			total.setLabel(mytotal == null ? "0.0" : Common.numberFormat.get().format(mytotal));
			totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));

			biaya.setValue(myhrg);

			totalBiayaEventListener.onEvent(null);

		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			Columns columns = new Columns();

			columns.setParent(grid);
			Column column = new Column();
			column.setParent(columns);
			column.setLabel("No. Nota");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Kode");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama");
			column.setWidth("10%");

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
			column.setLabel("Satuan");
			column.setWidth("10%");

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
			column.setLabel("Pajak %");
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

			totalPajak = new Footer();
			totalPajak.setParent(foot);
			totalPajak.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			totalHrg = new Footer();
			totalHrg.setParent(foot);
			totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			loadData(null);

		}
	}

	/**
	 * Tipe implementasi bersarang {@link ReturDetailAction} milik {@link PembayaranAction}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}, {@code Footer
	 * totalDiskon}, {@code Footer totalPajak}, {@code Footer total}, {@code Footer totalHrg}; operasi lokal:
	 * {@code loadData()}, {@code loadTotal()}, {@code display}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see PembayaranAction
	 */
	public class ReturDetailAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;
		private Footer totalDiskon;
		private Footer totalPajak;
		private Footer total;
		private Footer totalHrg;

		// private Object pembayaranDetails;

		public ReturDetailAction() throws Exception {
			super();
			display();
		}

		/** Renderer detail pembayaran retur yang memakai state layar dan lifecycle event {@link ReturDetailAction}. */
		class PembayaranDetailRenderer extends ais.ui.util.MyRowRenderer {

			public PembayaranDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");

				if (data instanceof DetailTransaksiPasien) {

					final DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) data;

					new Label(detailTransaksi.getTransaksiDetail() == null ? ""
							: detailTransaksi.getTransaksiDetail().getTransaksi().getKode()).setParent(row);
					ItemMedis item = detailTransaksi.getItem();
					new Label(item == null ? "" : item.getKode()).setParent(row);
					new Label(item == null ? "" : item.getNama()).setParent(row);
					new Label(item == null || item.getJenisItem() == null ? "" : item.getJenisItem().getNama())
							.setParent(row);
					new Label(Common.numberFormat.get().format(detailTransaksi.getQty())).setParent(row);
					new Label(item == null || item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama())
							.setParent(row);
					new Label(detailTransaksi.getAmount() == null ? "0.0"
							: Common.numberFormat.get().format(detailTransaksi.getAmount())).setParent(row);

					Label diskon = new Label(Common.numberFormat.get().format(detailTransaksi.getDiskonPersen()) + "% ("
							+ Common.numberFormat.get().format(detailTransaksi.getDiskon()) + ")");
					diskon.setParent(row);
					Label pajak = new Label(Common.numberFormat.get().format(detailTransaksi.getPajakPersen()) + "% ("
							+ Common.numberFormat.get().format(detailTransaksi.getPajak()) + ")");
					pajak.setParent(row);

					(new Label(Common.numberFormat.get().format(detailTransaksi.getHasilPenghitunganTotal()))).setParent(row);

				}

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) throws Exception {
			if (pendaftaran == null || pasien == null || transaksi == null) {
				return;
			}

			Session session = HibernateUtil.currentSession();

			Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
			Pasien myPasien = (Pasien) pasien.getAttribute("pasien");
			TransaksiMedis myTransaksi = (TransaksiMedis) transaksi.getAttribute("transaksi");

			List<TransaksiRetur> transaksis = session.createCriteria(TransaksiRetur.class)
					.createAlias("transaksi", "transaksi")
					.add(Restrictions.or(
							pembayaran.getId() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.eq("pembayaran", pembayaran),
							Restrictions.or(Restrictions.isNull("lunas"), Restrictions.eq("lunas", false))))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.eq("transaksi.pendaftaran", myPendaftaran),
									Restrictions.eq("transaksi.pasien", myPasien)),
							Restrictions.eq("transaksi", myTransaksi)))
					.list();

			List<DetailTransaksiPasien> detailTransaksis = session.createCriteria(DetailTransaksiPasien.class)
					.createAlias("transaksiReturDetail", "transaksiReturDetail")
					.add(transaksis == null || transaksis.size() == 0 ? Restrictions.sqlRestriction("1!=1")
							: Restrictions.in("transaksiReturDetail.transaksiRetur", transaksis))
					.list();

			ListModel strset = new SimpleListModel(detailTransaksis);
			grid.setRowRenderer(new PembayaranDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

			loadTotal(detailTransaksis);

			tabRetur.setVisible(!detailTransaksis.isEmpty());
			rowRetur.setVisible(!detailTransaksis.isEmpty());
		}

		public void loadTotal(List<DetailTransaksiPasien> pembayaranDetails) throws Exception {

			Double mytotal = 0.0;
			Double myhrg = 0.0;
			Double mydiskon = 0.0;
			Double mypajak = 0.0;

			for (Object data : pembayaranDetails) {
				if (data instanceof DetailTransaksiPasien) {
					DetailTransaksiPasien detailTransaksi = (DetailTransaksiPasien) data;

					mydiskon += detailTransaksi.getDiskon() * detailTransaksi.getQty();
					mypajak += detailTransaksi.getPajak() * detailTransaksi.getQty();

					mytotal += detailTransaksi.getQty() == null ? 0.0 : (detailTransaksi.getQty());

					Double totalAmount = detailTransaksi.getHasilPenghitunganTotal();

					myhrg += totalAmount;
				} else if (data instanceof DetailTransaksiLayanan) {
					DetailTransaksiLayanan detailTransaksi = (DetailTransaksiLayanan) data;
					mytotal += detailTransaksi.getQty() == null ? 0.0 : detailTransaksi.getQty();

					Double totalAmount = detailTransaksi.getHasilPenghitunganTotal();

					myhrg += totalAmount;

					mydiskon += detailTransaksi.getDiskon() * detailTransaksi.getQty();
					mypajak += detailTransaksi.getPajak() * detailTransaksi.getQty();
				}
			}

			totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));
			totalPajak.setLabel(mypajak == null ? "0.0" : Common.numberFormat.get().format(mypajak));

			total.setLabel(mytotal == null ? "0.0" : Common.numberFormat.get().format(mytotal));
			totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));

			retur.setValue(myhrg);

			totalBiayaEventListener.onEvent(null);

		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			// North north = new North();
			// north.setParent(this);
			// ais.ui.util.ZkCompat.setFlex(north, true);
			//
			// Toolbar toolbar = new Toolbar();
			// toolbar.setParent(north);
			// Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Cetak
			// Tagihan",
			// "/img/print.png");
			// toolbarbutton.setParent(toolbar);
			// toolbarbutton.addEventListener("onClick", new EventListener() {
			//
			// @Override
			// public void onEvent(Event event) throws Exception {
			// // TODO Auto-generated method stub
			//
			// Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran
			// .getAttribute("pendaftaran");
			// Pasien myPasien = (Pasien) pasien.getAttribute("pasien");
			// Transaksi myTransaksi = (Transaksi) transaksi
			// .getAttribute("transaksi");
			// LaporanInformasiTagihanWindow laporanInformasiTagihanWindow = new
			// LaporanInformasiTagihanWindow(
			// myPendaftaran, myPasien, myTransaksi);
			// laporanInformasiTagihanWindow
			// .setTitle("Laporan Informasi Tagihan");
			// laporanInformasiTagihanWindow.setHeight("95%");
			// laporanInformasiTagihanWindow.setWidth("95%");
			// laporanInformasiTagihanWindow.setClosable(true);
			// page.getFirstRoot().appendChild(
			// laporanInformasiTagihanWindow);
			// laporanInformasiTagihanWindow.onModal();
			// }
			// });

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			Columns columns = new Columns();

			columns.setParent(grid);
			Column column = new Column();
			column.setParent(columns);
			column.setLabel("No. Nota");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Kode");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama");
			column.setWidth("10%");

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
			column.setLabel("Satuan");
			column.setWidth("10%");

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
			column.setLabel("Pajak %");
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

			totalPajak = new Footer();
			totalPajak.setParent(foot);
			totalPajak.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			totalHrg = new Footer();
			totalHrg.setParent(foot);
			totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			loadData(null);

		}
	}

	/**
	 * Tipe implementasi bersarang {@link PembayaranNonTunaiAction} milik {@link PembayaranAction}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}, {@code Long index},
	 * {@code Footer totalHrg}, {@code TreeMap pembayaranNonTunais}; operasi lokal: {@code loadData()}, {@code
	 * loadTotal()}, {@code display}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see PembayaranAction
	 */
	public class PembayaranNonTunaiAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;

		private Long index = 0L;

		private Footer totalHrg;

		public TreeMap<Long, PembayaranNonTunai> pembayaranNonTunais = new TreeMap<Long, PembayaranNonTunai>();

		public PembayaranNonTunaiAction() throws Exception {
			super();
			display();
		}

		/**
		 * Renderer lokal untuk layar/komponen {@link PembayaranNonTunaiAction}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranNonTunaiAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see PembayaranNonTunaiAction
		 */
		class PembayaranNonTunaiRenderer extends ais.ui.util.MyRowRenderer {

			public PembayaranNonTunaiRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final PembayaranNonTunai pembayaranNonTunai = (PembayaranNonTunai) data;
				row.setValign("top");
				Grid grid = new Grid();
				grid.setParent(row);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				Column column = new Column();
				column.setParent(columns);
				column.setLabel("");
				column.setWidth("35%");

				column = new Column();
				column.setParent(columns);
				column.setLabel("");

				Rows rows = new Rows();
				rows.setParent(grid);

				Row myrow = new Row();
				myrow.setStyle("border:0px;background: transparent;");
				myrow.setParent(rows);
				myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pemb.")));
				final Combobox jenispembayaran = new Combobox();
				jenispembayaran.setWidth("90%");
				jenispembayaran.setParent(myrow);
				Common.insertCombo(jenispembayaran, "nama", JenisPembayaranMedis.class);
				Common.selectComboItem(jenispembayaran, pembayaranNonTunai.getJenisPembayaran());

				myrow = new Row();
				myrow.setStyle("border:0px;background: transparent;");
				myrow.setParent(rows);
				myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Bank")));

				final Combobox bank = new Combobox();

				Common.insertCombo(bank, "nama", "akun", JenisBiayaLain.class,
						Restrictions.eq("jenis", JenisBiayaLain.PEMBAYARAN_KASIR_BUKAN_TUNAI));
				Common.selectComboItem(bank, pembayaranNonTunai.getJenisBiayaLain());

				bank.setDisabled(true);
				bank.setWidth("90%");
				bank.setParent(myrow);

				myrow = new Row();
				myrow.setStyle("border:0px;background: transparent;");
				myrow.setParent(rows);
				myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Kartu")));
				final MyTextbox nomorkartu = new MyTextbox(pembayaranNonTunai.getNomorKartu());
				nomorkartu.setWidth("90%");
				nomorkartu.setParent(myrow);
				nomorkartu.setDisabled(true);

				final Combobox asuransi = new Combobox();
				asuransi.setWidth("90%");
				asuransi.setParent(row);
				Common.insertCombo(asuransi, "nama", "akun", JenisBiayaLain.class,
						Restrictions.eq("jenis", JenisBiayaLain.PEMBAYARAN_KASIR_ASURANSI));
				Common.selectComboItem(asuransi, pembayaranNonTunai.getJenisBiayaLain());

				final MyDoublebox jumlah = new MyDoublebox(pembayaranNonTunai.getAmount());
				jumlah.setWidth("90%");
				jumlah.setParent(row);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						JenisPembayaranMedis myJenisPembayaran = (JenisPembayaranMedis) (jenispembayaran
								.getSelectedItem() == null ? null : jenispembayaran.getSelectedItem().getValue());
						JenisBiayaLain myBank = (JenisBiayaLain) (bank.getSelectedItem() == null ? null
								: bank.getSelectedItem().getValue());

						JenisBiayaLain myAsuransi = (JenisBiayaLain) (asuransi.getSelectedItem() == null ? null
								: asuransi.getSelectedItem().getValue());

						String mynomorKartu = nomorkartu.getValue();

						Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();

						if (myJenisPembayaran != null) {
							bank.setDisabled(false);
							nomorkartu.setDisabled(false);
							asuransi.setSelectedItem(null);
							asuransi.setDisabled(true);
							myAsuransi = null;
						} else {
							bank.setDisabled(true);
							nomorkartu.setDisabled(true);
							asuransi.setDisabled(false);
						}

						if (myAsuransi != null) {
							bank.setDisabled(true);
							nomorkartu.setDisabled(true);
							asuransi.setDisabled(false);
							jenispembayaran.setSelectedItem(null);
							jenispembayaran.setDisabled(true);
							bank.setSelectedItem(null);
							nomorkartu.setValue("");
							myJenisPembayaran = null;
						} else {
							bank.setDisabled(false);
							nomorkartu.setDisabled(false);
							jenispembayaran.setDisabled(false);
						}

						pembayaranNonTunai.setAmount(myJumlah);

						if (myBank != null) {
							pembayaranNonTunai.setJenisBiayaLain(myBank);
						} else {
							pembayaranNonTunai.setJenisBiayaLain(myAsuransi);
						}
						pembayaranNonTunai.setJenisPembayaran(myJenisPembayaran);
						pembayaranNonTunai.setNomorKartu(mynomorKartu);
						pembayaranNonTunai.setPembayaran(pembayaran);

						loadTotal();
					}
				};

				jenispembayaran.addEventListener("onChange", eventListener);
				bank.addEventListener("onChange", eventListener);
				nomorkartu.addEventListener("onChange", eventListener);
				asuransi.addEventListener("onChange", eventListener);
				jumlah.addEventListener("onChange", eventListener);

				eventListener.onEvent(null);

				Hbox toolbar = new Hbox();

				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");

				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												pembayaranNonTunais.remove(pembayaranNonTunai.getId());
												row.detach();

											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepas terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, hubungi Administrator sistem.",
														e.getMessage()));
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
		public void loadData(Object value) throws Exception {
			Session session = HibernateUtil.currentSession();
			List<PembayaranNonTunai> pembayaranNonTunais = pembayaran == null || pembayaran.getId() == null
					? new ArrayList<PembayaranNonTunai>()
					: session.createCriteria(PembayaranNonTunai.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("pembayaran", pembayaran)).list();

			for (PembayaranNonTunai myPembayaranNonTunai : pembayaranNonTunais) {
				this.pembayaranNonTunais.put(myPembayaranNonTunai.getId(), myPembayaranNonTunai);
			}

			ListModel strset = new SimpleListModel(this.pembayaranNonTunais.values().toArray());
			grid.setRowRenderer(new PembayaranNonTunaiRenderer());
			grid.setModel(strset);
			grid.renderAll();

			loadTotal();
		}

		public void loadTotal() throws Exception {
			if (bayarNonTunai == null) {
				return;
			}
			Double myhrg = 0.0;
			for (PembayaranNonTunai pembayaranNonTunai : pembayaranNonTunais.values()) {
				myhrg += pembayaranNonTunai.getAmount() == null ? 0.0 : pembayaranNonTunai.getAmount();
			}
			totalHrg.setLabel(myhrg == null ? "0.0" : Common.numberFormat.get().format(myhrg));

			bayarNonTunai.setValue(myhrg);
			totalBiayaEventListener.onEvent(null);
		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			North north = new North();
			north.setParent(this);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Div div = new Div();
			div.setParent(north);

			Grid searchgrid = new Grid();
			searchgrid.setParent(div);

			Columns columns = new Columns();
			columns.setParent(searchgrid);

			Column column = new Column();
			column.setParent(columns);
			column.setWidth("25%");

			column = new Column();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(div);

			final Toolbarbutton tambahPembayaranNonTunai = new ais.ui.util.MyToolbarbuttonConfig(
					"Tambah Pembayaran Non Tunai", "/img/add_item.png");

			tambahPembayaranNonTunai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					PembayaranNonTunai pembayaranNonTunai = new PembayaranNonTunai();
					pembayaranNonTunai.setId(--index);
					pembayaranNonTunai.setPembayaran(pembayaran);
					pembayaranNonTunais.put(pembayaranNonTunai.getId(), pembayaranNonTunai);

					loadData(null);

				}

			});
			tambahPembayaranNonTunai.setParent(toolbar);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			columns = new Columns();
			columns.setParent(grid);

			column = new Column();
			column.setParent(columns);
			column.setLabel("Cara Pembayaran");
			column.setWidth("40%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Asuransi");
			column.setWidth("30%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Jumlah");
			column.setAlign("right");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");

			Foot foot = new Foot();
			foot.setParent(grid);

			foot.appendChild(new Footer("Total"));

			foot.appendChild(new Footer());

			totalHrg = new Footer();
			totalHrg.setParent(foot);
			totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			foot.appendChild(new Footer());

			loadData(null);

		}
	}

	public void onCetakPembayaran(Pembayaran pembayaran) throws Exception {
		LaporanBuktiPembayaranWindow laporanBuktiPembayaranWindow = new LaporanBuktiPembayaranWindow(pembayaran);
		laporanBuktiPembayaranWindow.setTitle("Laporan Bukti Pembayaran");
		laporanBuktiPembayaranWindow.setClosable(true);
		laporanBuktiPembayaranWindow.setWidth("750px");
		laporanBuktiPembayaranWindow.setHeight("95%");
		laporanBuktiPembayaranWindow.setParent(page.getFirstRoot());
		laporanBuktiPembayaranWindow.onModal();
	}

}
