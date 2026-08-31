package ais.action.master.asset.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
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

import ais.action.master.asset.SaldoAwalMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.asset.Asset;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk penerimaan pengadaan master asset detail. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PenerimaanPengadaanMasterAsset
 * penerimaanPengadaanMasterAsset}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code
 * boolean persetujuan}, {@code boolean ubah}, {@code boolean beliLangsung}, {@code List
 * penerimaanPengadaanMasterAssetDetails}; pembacaan/pencarian ({@code tampilInventaris()}, {@code loadData()});
 * validasi/perhitungan ({@code hitungUlang()}); operasi domain lain ({@code display()}); konfigurasi
 * constructor: {@code delete}, {@code edit}, {@code persetujuan}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class PenerimaanPengadaanMasterAssetDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private boolean persetujuan;

	private boolean ubah = false;

	private boolean beliLangsung;

	/**
	 * Tipe implementasi bersarang {@link TerimaTagihan} milik {@link PenerimaanPengadaanMasterAssetDetailAction}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * PenerimaanPengadaanMasterAssetDetailAction}. Dependensi yang diperlukan harus diberikan secara eksplisit
	 * agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code LampiranLain lainMahasiswa}, {@code
	 * LampiranLain lainMahasiswa1}, {@code LampiranLain lainMahasiswa12}, {@code LampiranLain lainMahasiswa2},
	 * {@code LampiranLain lainMahasiswa3}, {@code LampiranLain lainMahasiswa4}, {@code LampiranLain
	 * lainMahasiswa5}; operasi lokal: {@code init()}, {@code init}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see PenerimaanPengadaanMasterAssetDetailAction
	 */
	public static class TerimaTagihan {

		protected LampiranLain lainMahasiswa;
		protected LampiranLain lainMahasiswa1;
		protected LampiranLain lainMahasiswa12;
		protected LampiranLain lainMahasiswa2;
		protected LampiranLain lainMahasiswa3;
		protected LampiranLain lainMahasiswa4;
		protected LampiranLain lainMahasiswa5;

		public void init(final SaldoAwalMasterAsset saldoAwalMasterAssetData, final Textbox kodeTagihan,
				final MyDatebox tanggalTagihan, final EventListener eventListener) throws Exception {
			final MyWindow window = new MyWindow("Cetak Data", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("650px");
			window.setWidth("700px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Tagihan / Invoice *"));

			kodeTagihan.setValue(saldoAwalMasterAssetData.getKodeTagihan());
			row.appendChild(kodeTagihan);
			kodeTagihan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan / Invoice *"));

			tanggalTagihan.setValue(saldoAwalMasterAssetData.getTanggalTagihan());
			row.appendChild(tanggalTagihan);
			tanggalTagihan.setFormat(Common.dateFormat1.get().toPattern());

			lainMahasiswa = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan / invoice *"));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName(), "Lampiran Tagihan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa1 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Faktur Pajak"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Faktur_Pajak", "Faktur Pajak", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa1 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa12 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Biiling Pajak"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Kode_Pajak", "Kode Biiling Pajak",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa12 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa2 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kwitansi"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Kwitansi", "Kwitansi", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa2 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa3 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain I"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_I", "Dokumen Lain I",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa3 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa4 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain II"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_II", "Dokumen Lain II",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa4 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa5 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain III"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, saldoAwalMasterAssetData.getIdTemp(),
					"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_III", "Dokumen Lain III",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa5 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			South south = new South();
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Simpan dan Terima Tagihan", "/img/excel.png");
			print.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (kodeTagihan.getValue().trim().equals("")) {
						MyMessageboxConfig.show("Mohon maaf, Kode Tagihan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Tagihan sesuai nomor tagihan dari vendor; (2) Kode tagihan biasanya tercantum pada invoice/faktur yang diterima; (3) ulangi proses simpan dan terima tagihan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (tanggalTagihan.getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Tanggal Tagihan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal Tagihan dan pilih tanggal dari kalender; (2) Pastikan tanggal sesuai dengan tanggal tagihan pada invoice; (3) ulangi proses simpan dan terima tagihan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					saldoAwalMasterAssetData.setKodeTagihan(kodeTagihan.getValue().trim());
					saldoAwalMasterAssetData.setTanggalTagihan(tanggalTagihan.getValue());

					if (saldoAwalMasterAssetData.getId() != null) {
						Common.refreshUpdate(saldoAwalMasterAssetData);
					}

					Session streamingSession;
					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa);
							lainMahasiswa.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa1 != null && lainMahasiswa1.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa1);
							lainMahasiswa1.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa1);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa12 != null && lainMahasiswa12.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa12);
							lainMahasiswa1.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa12);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa2 != null && lainMahasiswa2.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa2);
							lainMahasiswa2.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa2);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa3 != null && lainMahasiswa3.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa3);
							lainMahasiswa3.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa3);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa4 != null && lainMahasiswa4.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa4);
							lainMahasiswa4.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa4);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa5 != null && lainMahasiswa5.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa5);
							lainMahasiswa5.setRef(saldoAwalMasterAssetData.getIdTemp());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa5);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					LampiranLain fileFotoLain = LampiranLain.ambil(false, saldoAwalMasterAssetData.getIdTemp(),
							"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName());
					if (fileFotoLain == null || fileFotoLain.getId() == null) {
						MyMessageboxConfig.show("Mohon maaf, File/scan tagihan belum diunggah. Langkah yang dapat dilakukan: (1) Scan atau foto dokumen tagihan/invoice dari vendor; (2) Klik tombol Upload dan pilih file hasil scan (format JPG, PDF, atau PNG); (3) Pastikan file berhasil diunggah sebelum menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					window.detach();

					if (saldoAwalMasterAssetData.getId() != null) {
						Session session = HibernateUtil.currentSession();
						List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
								.createCriteria(SaldoAwalMasterAssetDetail.class)
								.add(Restrictions.eq("saldoAwal", saldoAwalMasterAssetData)).list();

						session.createSQLQuery(
								"delete from asset.detail_transaksi_asset where saldo_awal_master_asset_detail in (select id from asset.saldo_awal_master_asset_detail where saldo_awal_master_asset = "
										+ saldoAwalMasterAssetData.getId() + ");")
								.executeUpdate();
						Set<Long> longs = new HashSet<Long>();

						for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
							if (saldoAwalMasterAssetDetail.getMasterAsset() != null
									&& !longs.contains(saldoAwalMasterAssetDetail.getMasterAsset().getId())) {

								DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
								detailTransaksiAsset.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
								detailTransaksiAsset.setQtyBonus(0.0);

								detailTransaksiAsset.setMasterAsset(saldoAwalMasterAssetDetail.getMasterAsset());
								detailTransaksiAsset.setKeterangan("Transaksi Saldo Awal");
								detailTransaksiAsset.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
								detailTransaksiAsset.setPemilikAsset(saldoAwalMasterAssetData.getPemilikAsset());
								detailTransaksiAsset.setLokasi(saldoAwalMasterAssetData.getLokasi());
								detailTransaksiAsset.setRuang(saldoAwalMasterAssetData.getRuang());
								detailTransaksiAsset.setQty(saldoAwalMasterAssetDetail.getJumlah());
								detailTransaksiAsset.setTanggal(saldoAwalMasterAssetData.getTanggalPembuatan());

								session.save(detailTransaksiAsset);
								session.flush();
							}
						}
					}
					eventListener.onEvent(event);
				}
			});
			print.setParent(toolbar);

			window.setVisible(true);
			window.onModal();
		}

		public void init(final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset,
				final SaldoAwalMasterAsset saldoAwalMasterAssetData, final EventListener eventListener)
				throws Exception {
			final MyWindow window = new MyWindow("Cetak Data", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("650px");
			window.setWidth("700px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Tagihan / Invoice *"));

			final Textbox kodeTagihan = new Textbox(penerimaanPengadaanMasterAsset.getKodeTagihan());
			row.appendChild(kodeTagihan);
			kodeTagihan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan / Invoice *"));

			final MyDatebox tanggalTagihan = new MyDatebox(penerimaanPengadaanMasterAsset.getTanggalTagihan());
			row.appendChild(tanggalTagihan);
			tanggalTagihan.setFormat(Common.dateFormat1.get().toPattern());

			lainMahasiswa = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan / invoice *"));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName(), "Lampiran Tagihan", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa1 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Faktur Pajak"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Faktur_Pajak", "Faktur Pajak", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa1 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa12 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Biiling Pajak"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Kode_Pajak", "Kode Biiling Pajak", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa12 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa2 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kwitansi"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Kwitansi", "Kwitansi", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa2 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa3 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain I"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_I", "Dokumen Lain I", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa3 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa4 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain II"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_II", "Dokumen Lain II", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa4 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			lainMahasiswa5 = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Lain III"));
			hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_III", "Dokumen Lain III", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa5 = (LampiranLain) arg0.getData();
						}
					});
			hbox.setParent(row);

			South south = new South();
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Simpan dan Terima Tagihan", "/img/excel.png");
			print.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (kodeTagihan.getValue().trim().equals("")) {
						MyMessageboxConfig.show("Mohon maaf, Kode Tagihan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Tagihan sesuai nomor invoice/faktur dari vendor; (2) Kode tagihan biasanya tercantum pada dokumen tagihan yang diterima; (3) ulangi proses simpan dan terima tagihan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (tanggalTagihan.getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Tanggal Tagihan belum diisi. Langkah yang dapat dilakukan: (1) Klik field Tanggal Tagihan dan pilih tanggal dari kalender; (2) Pastikan tanggal sesuai dengan tanggal tagihan pada invoice; (3) ulangi proses simpan dan terima tagihan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					Session session = HibernateUtil.currentSession();
					if (penerimaanPengadaanMasterAsset.getId() != null) {
						session.refresh(penerimaanPengadaanMasterAsset);
					}

					penerimaanPengadaanMasterAsset.setKodeTagihan(kodeTagihan.getValue().trim());
					penerimaanPengadaanMasterAsset.setTanggalTagihan(tanggalTagihan.getValue());

					session.update(penerimaanPengadaanMasterAsset);
					session.flush();

					Session streamingSession;
					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa);
							lainMahasiswa.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa1 != null && lainMahasiswa1.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa1);
							lainMahasiswa1.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa1);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa12 != null && lainMahasiswa12.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa12);
							lainMahasiswa1.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa12);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa2 != null && lainMahasiswa2.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa2);
							lainMahasiswa2.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa2);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa3 != null && lainMahasiswa3.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa3);
							lainMahasiswa3.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa3);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa4 != null && lainMahasiswa4.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa4);
							lainMahasiswa4.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa4);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					if (lainMahasiswa5 != null && lainMahasiswa5.getId() != null) {
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							streamingSession.refresh(lainMahasiswa5);
							lainMahasiswa5.setRef(penerimaanPengadaanMasterAsset.getId());

							streamingSession.getTransaction().begin();
							streamingSession.update(lainMahasiswa5);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					LampiranLain fileFotoLain = LampiranLain.ambil(false, penerimaanPengadaanMasterAsset.getId(),
							PenerimaanPengadaanMasterAsset.class.getName());
					if (fileFotoLain == null || fileFotoLain.getId() == null) {
						MyMessageboxConfig.show("Mohon maaf, File/scan tagihan pada penerimaan ini belum diunggah. Langkah yang dapat dilakukan: (1) Siapkan scan atau foto dokumen invoice/tagihan dari vendor; (2) Klik tombol Upload dan pilih file tersebut; (3) Pastikan file berhasil diunggah sebelum melanjutkan proses. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					window.detach();

					SaldoAwalMasterAsset saldoAwalMasterAsset = saldoAwalMasterAssetData;

					if (saldoAwalMasterAsset == null || saldoAwalMasterAsset.getId() == null) {
						saldoAwalMasterAsset = new SaldoAwalMasterAsset();
						saldoAwalMasterAsset
								.setKode(SaldoAwalMasterAssetAction.generateCode(WaktuUtil.getDate(), true));
						saldoAwalMasterAsset.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);

						session.save(saldoAwalMasterAsset);
						session.flush();

					} else {
						session.refresh(saldoAwalMasterAsset);
					}

					penerimaanPengadaanMasterAsset.setSaldoAwalMasterAsset(saldoAwalMasterAsset);
					Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);
					session.flush();

					List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
							.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
							.list();

					for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
						SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = penerimaanPengadaanMasterAssetDetail
								.getSaldoAwalMasterAssetDetail();

						if (saldoAwalMasterAssetDetail == null) {
							saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();

							saldoAwalMasterAssetDetail
									.setPenerimaanPengadaanMasterAssetDetail(penerimaanPengadaanMasterAssetDetail);
							saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
							session.save(saldoAwalMasterAssetDetail);
							session.flush();

							penerimaanPengadaanMasterAssetDetail
									.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
							session.save(penerimaanPengadaanMasterAssetDetail);
							session.flush();
						}
					}

					List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
							.createCriteria(SaldoAwalMasterAssetDetail.class)
							.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();

					session.createSQLQuery(
							"delete from asset.detail_transaksi_asset where saldo_awal_master_asset_detail in (select id from asset.saldo_awal_master_asset_detail where saldo_awal_master_asset = "
									+ saldoAwalMasterAsset.getId() + ");")
							.executeUpdate();
					Set<Long> longs = new HashSet<Long>();

					for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
						if (saldoAwalMasterAssetDetail.getMasterAsset() != null
								&& !longs.contains(saldoAwalMasterAssetDetail.getMasterAsset().getId())) {

							DetailTransaksiAsset detailTransaksiAsset = new DetailTransaksiAsset();
							detailTransaksiAsset.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
							detailTransaksiAsset.setQtyBonus(0.0);

							detailTransaksiAsset.setMasterAsset(saldoAwalMasterAssetDetail.getMasterAsset());
							detailTransaksiAsset.setKeterangan("Transaksi Saldo Awal");
							detailTransaksiAsset.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
							detailTransaksiAsset.setPemilikAsset(saldoAwalMasterAsset.getPemilikAsset());
							detailTransaksiAsset.setLokasi(saldoAwalMasterAsset.getLokasi());
							detailTransaksiAsset.setRuang(saldoAwalMasterAsset.getRuang());
							detailTransaksiAsset.setQty(saldoAwalMasterAssetDetail.getJumlah());
							detailTransaksiAsset.setTanggal(saldoAwalMasterAsset.getTanggalPembuatan());

							session.save(detailTransaksiAsset);
							session.flush();
						}
					}

					eventListener.onEvent(event);
				}
			});
			print.setParent(toolbar);

			window.setVisible(true);
			window.onModal();
		}

	}

	public PenerimaanPengadaanMasterAssetDetailAction(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset,
			boolean ubah) {
		super();
		this.ubah = ubah;
		this.beliLangsung = (penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getPembelianLangsung());
		persetujuan = (penerimaanPengadaanMasterAsset != null
				&& penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null) || beliLangsung;
		setAttribute("janganDisabled", true);
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PenerimaanPengadaanMasterAssetDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenerimaanPengadaanMasterAssetDetailAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenerimaanPengadaanMasterAssetDetailAction}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenerimaanPengadaanMasterAssetDetailAction
	 */
	class PenerimaanPengadaanMasterAssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PenerimaanPengadaanMasterAssetDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) data;

			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAssetDetail
					.getPenerimaanPengadaanMasterAsset();

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			new Label(penerimaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
					: penerimaanPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);

			LampiranLain.createDownloadUploadFileLain(hbox,
					penerimaanPengadaanMasterAssetDetail.getMasterAsset().getId(), LampiranLain.GAMBAR_MASTER_ASSET,
					"Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, !persetujuan, null, false, true);

			final Label jumlah = new Label(
					Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()));

			final MyDoublebox diterima = new MyDoublebox(
					penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? 0.0
							: penerimaanPengadaanMasterAssetDetail.getDiterima());

			final Label sisa = new Label(penerimaanPengadaanMasterAssetDetail.getJumlah() == null
					|| penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? ""
							: Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()
									- penerimaanPengadaanMasterAssetDetail.getDiterima()));

			final Label total = new Label(
					Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

			final MyDoublebox hargaPotongan = new MyDoublebox(penerimaanPengadaanMasterAssetDetail.getHargaPotongan());
			final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
			diskonDalamBentukPersen.setChecked(penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen());

			Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
					* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

			Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);

			final Label ppnNilai = new MyLabelKecil(Common.numberFormat.get().format(ppn));
			row.setValign("top");
			row.setAttribute("ppnNilai", ppnNilai);

			// Nilai PPH (nominal) — sejajar dengan "Nilai PPN", memakai dasar DPP yang sama.
			Double pph = ((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp);
			final Label pphNilai = new MyLabelKecil(Common.numberFormat.get().format(pph));
			pphNilai.setStyle("text-align:right");
			row.setAttribute("pphNilai", pphNilai);
			final Combobox persenPpn = new Combobox();
			Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
					"Tanpa PPN", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPpn, penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn());

			final Combobox persenPph = new Combobox();
			Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPph, penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang());

			final MyDoublebox hargaBeli = new MyDoublebox(
					penerimaanPengadaanMasterAssetDetail.getHargaBeli() == null ? 0.0
							: penerimaanPengadaanMasterAssetDetail.getHargaBeli());

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAssetDetail.class,
					penerimaanPengadaanMasterAssetDetail,
					penerimaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getMasterAsset().getNama()))
					.setParent(vbox);

			if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class,
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPemesananPengadaanMasterAsset(),
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPemesananPengadaanMasterAsset().getKode())
						.setParent(a);
			}

			if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null
					&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset(),
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
								.getKode())
						.setParent(a);
			}

			SatuanKerja satuanKerja = penerimaanPengadaanMasterAssetDetail == null
					|| penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() == null
					|| penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAssetDetail() == null
					|| penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset() == null
									? null
									: penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAsset().getSatuanKerja();
			if (satuanKerja == null) {
				satuanKerja = penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getSatuanKerja();
			}

			new Label(satuanKerja == null ? "" : satuanKerja.getNama()).setParent(a);

			if (ubah) {
				(jumlah).setParent(row);
			} else if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()))
						.setParent(row);
			} else {
				(jumlah).setParent(row);
			}

			if (ubah) {
				(diterima).setParent(row);
			} else if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getDiterima()))
						.setParent(row);
			} else {
				(diterima).setParent(row);
			}

			if (!ubah)
				diterima.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
						.getDisetujuiOleh() != null || !edit);

			diterima.setStyle("text-align:right");
			diterima.setWidth("90%");
			diterima.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail.setDiterima(diterima.getValue());
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

					String mysisa = penerimaanPengadaanMasterAssetDetail.getJumlah() == null
							|| penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? ""
									: Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()
											- penerimaanPengadaanMasterAssetDetail.getDiterima());
					sisa.setValue(mysisa);

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					hitungUlang();
				}
			});

			(sisa).setParent(row);

			if (!beliLangsung) {

				if (penerimaanPengadaanMasterAssetDetail != null
						&& !penerimaanPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah()) {
					new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaBeli()))
							.setParent(row);
				} else if (penerimaanPengadaanMasterAsset != null
						&& penerimaanPengadaanMasterAsset.getJsonTermin() != null) {
					new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaBeli()))
							.setParent(row);
				} else if (ubah) {
					(hargaBeli).setParent(row);
				} else if (persetujuan) {
					new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaBeli()))
							.setParent(row);
				} else {
					(hargaBeli).setParent(row);
				}

				if (!ubah)
					hargaBeli.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				hargaBeli.setStyle("text-align:right");
				hargaBeli.setWidth("90%");
				hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
						penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(hargaBeli.getValue());
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

						MasterAsset masterAsset = penerimaanPengadaanMasterAssetDetail.getMasterAsset();
						session.refresh(masterAsset);
						masterAsset.setHargaBeliDefault(hargaBeli.getValue());
						Common.refreshUpdate(session, masterAsset);
						total.setValue(
								Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
						Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
								* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

						Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(Common.numberFormat.get().format(
								((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp)));

						hitungUlang();
					}
				});

				vbox = new Vbox();
				vbox.setWidth("99%");
				vbox.setParent(row);

				if (ubah) {
					(diskonDalamBentukPersen).setParent(vbox);
				} else if (persetujuan) {
					new Label(penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak")
							.setParent(vbox);
				} else {
					(diskonDalamBentukPersen).setParent(vbox);
				}

				if (!ubah)
					diskonDalamBentukPersen.setDisabled(penerimaanPengadaanMasterAssetDetail
							.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() != null || !edit);
				diskonDalamBentukPersen.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail
								.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

						total.setValue(
								Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
						Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
								* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

						Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(Common.numberFormat.get().format(
								((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp)));

						hitungUlang();
					}
				});

				if (ubah) {
					(hargaPotongan).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil(
							Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaPotongan()))
							.setParent(vbox);
				} else {
					(hargaPotongan).setParent(vbox);
				}

				if (!ubah)
					hargaPotongan.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				hargaPotongan.setStyle("text-align:right");
				hargaPotongan.setWidth("90%");
				hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

						total.setValue(
								Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
						Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
								* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

						Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(Common.numberFormat.get().format(
								((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp)));

						hitungUlang();
					}
				});

				if (ubah) {
					(persenPpn).setParent(row);
				} else if (persetujuan) {
					new Label(penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
				} else {
					(persenPpn).setParent(row);
				}

				if (!ubah)
					persenPpn.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				persenPpn.setStyle("text-align:right");
				persenPpn.setWidth("90%");
				persenPpn.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail
								.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
										: persenPpn.getSelectedItem().getValue()));
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}

						total.setValue(
								Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
						Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
								* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

						Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(Common.numberFormat.get().format(
								((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp)));

						hitungUlang();
					}
				});

				ppnNilai.setStyle("text-align:right");
				ppnNilai.setParent(row);

				if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getJsonTermin() != null) {
					new Label(penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
				} else if (ubah) {
					(persenPph).setParent(row);
				} else if (persetujuan) {
					new Label(penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang() == null ? ""
							: penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
				} else {
					(persenPph).setParent(row);
				}

				if (!ubah)
					persenPph.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				persenPph.setStyle("text-align:right");
				persenPph.setWidth("90%");
				persenPph.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail
								.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
										: persenPph.getSelectedItem().getValue()));
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

						total.setValue(
								Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

						Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
								* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

						Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
						ppnNilai.setValue(Common.numberFormat.get().format(ppn));
						pphNilai.setValue(Common.numberFormat.get().format(
								((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp)));

						hitungUlang();
					}
				});

				pphNilai.setStyle("text-align:right");
				pphNilai.setParent(row);

			}

			total.setStyle("text-align:right");
			total.setParent(row);
			Vbox toolbarData = new Vbox();
			if (!persetujuan || beliLangsung) {

				vbox = new Vbox();
				vbox.setParent(row);
				vbox.setWidth("98%");

				final MyTextbox keterangan = new MyTextbox(
						penerimaanPengadaanMasterAssetDetail.getKeterangan() == null ? ""
								: penerimaanPengadaanMasterAssetDetail.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setHeight("95%");

				if (ubah) {
					(keterangan).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil(penerimaanPengadaanMasterAssetDetail.getKeterangan()).setParent(vbox);
				} else {
					keterangan.setParent(vbox);
				}

				if (!ubah)
					keterangan.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());

						row.setValign("top");
						row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}
					}
				});

				final MyTextbox kondisi = new MyTextbox(penerimaanPengadaanMasterAssetDetail.getKondisi() == null ? ""
						: penerimaanPengadaanMasterAssetDetail.getKondisi());
				kondisi.setWidth("90%");
				kondisi.setHeight("95%");
				if (ubah) {
					(kondisi).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil(penerimaanPengadaanMasterAssetDetail.getKondisi()).setParent(vbox);
				} else {
					kondisi.setParent(vbox);
				}

				if (!ubah)
					kondisi.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null || !edit);
				kondisi.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setKondisi(kondisi.getValue());

						row.setValign("top");
						row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}
					}
				});

				final MyCheckboxConfig kartuGaransi = new MyCheckboxConfig("Ada garansi ?");
				kartuGaransi.setChecked(penerimaanPengadaanMasterAssetDetail.getKartuGaransi());

				if (ubah) {
					(kartuGaransi).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil(
							"Ada garansi ?" + (penerimaanPengadaanMasterAssetDetail.getKartuGaransi() ? "Ya" : "Tidak"))
							.setParent(vbox);
				} else {
					kartuGaransi.setParent(vbox);
				}

				final MyCheckboxConfig boxDus = new MyCheckboxConfig("Ada boks / dus ?");
				boxDus.setChecked(penerimaanPengadaanMasterAssetDetail.getBoxDus());
				if (ubah) {
					(boxDus).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil(
							"Ada boks / dus ?" + (penerimaanPengadaanMasterAssetDetail.getBoxDus() ? "Ya" : "Tidak"))
							.setParent(vbox);
				} else {
					boxDus.setParent(vbox);
				}

				final MyCheckboxConfig manualBook = new MyCheckboxConfig("Ada manual book ?");
				manualBook.setChecked(penerimaanPengadaanMasterAssetDetail.getManualBook());

				if (ubah) {
					(manualBook).setParent(vbox);
				} else if (persetujuan) {
					new MyLabelKecil("Ada manual book ?"
							+ (penerimaanPengadaanMasterAssetDetail.getManualBook() ? "Ya" : "Tidak")).setParent(vbox);
				} else {
					manualBook.setParent(vbox);
				}
				kartuGaransi.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setKartuGaransi(kartuGaransi.isChecked());

						row.setValign("top");
						row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}
					}
				});

				boxDus.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setBoxDus(boxDus.isChecked());

						row.setValign("top");
						row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}
					}
				});

				manualBook.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						penerimaanPengadaanMasterAssetDetail.setManualBook(manualBook.isChecked());

						row.setValign("top");
						row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
						if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
							Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
						}
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setDisabled(penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
						.getDisetujuiOleh() != null || !delete);
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

												Common.refreshDelete(penerimaanPengadaanMasterAssetDetail);

												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

											}

										}

									}
								});

					}

				});
				button.setParent(toolbarData);

			}
			toolbarData.setParent(row);

			PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris(penerimaanPengadaanMasterAssetDetail,
					penerimaanPengadaanMasterAsset, toolbarData, edit);
		}
	}

	public static void tampilInventaris(final PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail,
			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset, Component toolbarData, boolean edit) {
		final MyToolbarbuttonConfig asset = new MyToolbarbuttonConfig("Jadikan Inventaris",
				"/img/svg/edit-box-line.svg");
		final MyToolbarbuttonConfig hapusAsset = new MyToolbarbuttonConfig("Hapus Inventaris",
				"/img/svg/edit-box-line.svg");

		final SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = penerimaanPengadaanMasterAssetDetail == null
				? null
				: penerimaanPengadaanMasterAssetDetail.getSaldoAwalMasterAssetDetail();

		asset.setDisabled(
				penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() == null
						|| !edit);

		if (penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getLunas()) {
			asset.setDisabled(false);
		}

		asset.setVisible((saldoAwalMasterAssetDetail == null || saldoAwalMasterAssetDetail.getAsset() == null)
				&& penerimaanPengadaanMasterAssetDetail.getMasterAsset().getTipe()
						.equalsIgnoreCase(MasterAsset.TIPE_TIDAK_HABIS_PAKAI));
		asset.setTooltiptext("Jadikan Inventaris/Sarpras");
		asset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(
						"Apakah yakin ingin menjadikan pengadaan ini menjadi barang inventaris/Sarpras ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();
									SaldoAwalMasterAsset saldoAwalMasterAsset = penerimaanPengadaanMasterAsset
											.getSaldoAwalMasterAsset();

									if (saldoAwalMasterAsset == null) {
										saldoAwalMasterAsset = new SaldoAwalMasterAsset();
										saldoAwalMasterAsset.setKode(
												SaldoAwalMasterAssetAction.generateCode(WaktuUtil.getDate(), true));
										saldoAwalMasterAsset
												.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);

										session.save(saldoAwalMasterAsset);
										session.flush();
									} else {
										session.refresh(saldoAwalMasterAsset);
									}

									penerimaanPengadaanMasterAsset.setSaldoAwalMasterAsset(saldoAwalMasterAsset);
									Common.refreshUpdate(session, penerimaanPengadaanMasterAsset);
									session.flush();

									SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = penerimaanPengadaanMasterAssetDetail
											.getSaldoAwalMasterAssetDetail();

									if (saldoAwalMasterAssetDetail == null) {
										saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) session
												.createCriteria(SaldoAwalMasterAssetDetail.class)
												.add(Restrictions.eq("masterAsset",
														penerimaanPengadaanMasterAssetDetail.getMasterAsset()))
												.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset))
												.setMaxResults(1).uniqueResult();
									}

									if (saldoAwalMasterAssetDetail == null) {
										saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();

										saldoAwalMasterAssetDetail.setPenerimaanPengadaanMasterAssetDetail(
												penerimaanPengadaanMasterAssetDetail);
										saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
										session.save(saldoAwalMasterAssetDetail);
										session.flush();

									}

									penerimaanPengadaanMasterAssetDetail
											.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
									Common.refreshUpdate(session, penerimaanPengadaanMasterAssetDetail);
									session.flush();

									SaldoAwalMasterAssetDetailAction
											.pindahkanMenjadiBarangInventaris(saldoAwalMasterAssetDetail);
									asset.setVisible(false);
									hapusAsset.setVisible(true);
								}

							}
						});

			}

		});
		asset.setParent(toolbarData);

		hapusAsset.setDisabled(!edit);
		hapusAsset.setVisible(saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getAsset() != null
				&& penerimaanPengadaanMasterAssetDetail.getMasterAsset().getTipe()
						.equalsIgnoreCase(MasterAsset.TIPE_TIDAK_HABIS_PAKAI));
		hapusAsset.setTooltiptext("Hapus Inventaris/Sarpras");
		hapusAsset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menhapus barang inventaris/Sarpras dari pengadaan ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Asset myAsset = saldoAwalMasterAssetDetail.getAsset();
									Session session = HibernateUtil.currentSession();
									session.createSQLQuery("delete from asset.asset where id = " + myAsset.getId())
											.executeUpdate();
									hapusAsset.setVisible(false);
									asset.setVisible(true);
								}

							}
						});

			}

		});
		hapusAsset.setParent(toolbarData);
	}

	private List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = null;

	private Footer footerTotalSemua;

	private Footer footerTotalSemuaPpn;

	private Footer footerTotalSemuaPph;

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		penerimaanPengadaanMasterAssetDetails = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset)).list();

		ListModel strset = new SimpleListModel(penerimaanPengadaanMasterAssetDetails);
		grid.setRowRenderer(new PenerimaanPengadaanMasterAssetDetailRenderer());
		grid.setModelCheckMobile(strset);
		hitungUlang();
	}

	private void hitungUlang() {
		if (penerimaanPengadaanMasterAssetDetails != null) {
			Double nilai = 0.0;
			Double ppnt = 0.0;
			Double ppht = 0.0;
			for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
				Double total = penerimaanPengadaanMasterAssetDetail.getHargaTotal();
				nilai += total;

				Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
						* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

				Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
				ppnt += ppn;

				Double pph = ((penerimaanPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp);
				ppht += pph;
			}
			footerTotalSemua.setLabel(Common.numberFormat.get().format(nilai));
			footerTotalSemuaPpn.setLabel(Common.numberFormat.get().format(ppnt));
			if (footerTotalSemuaPph != null) {
				footerTotalSemuaPph.setLabel(Common.numberFormat.get().format(ppht));
			}
			if (penerimaanPengadaanMasterAsset.getNilai().intValue() != nilai.intValue()) {
				penerimaanPengadaanMasterAsset.setNilai(nilai);
				Common.refreshUpdate(penerimaanPengadaanMasterAsset);
			}
		}
	}

	public void display() throws Exception {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Penerimaan Pengadaan Barang/Jasa"));
		Toolbar toolbar = new Toolbar();
		if (penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() == null) {

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/add_item.png");
			button.setDisabled(penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();

					List<MasterAsset> masterAssets = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
							.setProjection(Projections.groupProperty("masterAsset"))
							.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
							.list();

					AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
							null);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataMasterAssetBanyak);
					ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							for (MasterAsset masterAsset : masterAssets) {
								PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
								penerimaanPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
								penerimaanPengadaanMasterAssetDetail.setJumlah(0.0);
								penerimaanPengadaanMasterAssetDetail.setDiterima(0.0);
								penerimaanPengadaanMasterAssetDetail.setKeterangan("");
								penerimaanPengadaanMasterAssetDetail
										.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
								session.save(penerimaanPengadaanMasterAssetDetail);
							}

							loadData(null);
						}
					});
					ambilDataMasterAssetBanyak.setWidth("97%");
					ambilDataMasterAssetBanyak.setHeight("97%");
					ambilDataMasterAssetBanyak.setVisible(true);
					ambilDataMasterAssetBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setVisible(penerimaanPengadaanMasterAsset.getId() != null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiPenerimaanPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPenerimaanPengadaanMasterAssetDetailHelper(
							penerimaanPengadaanMasterAsset, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(groupbox);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Nama/Gambar");
		column.setWidth("14%");

		if (!persetujuan || beliLangsung) {

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setWidth("7%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Diterima");
			column.setWidth("7%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Selisih");
			column.setWidth("7%");
			column.setAlign("right");

			if (!beliLangsung) {

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Harga");
				column.setAlign("right");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Diskon");
				column.setAlign("right");
				column.setWidth("7%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("PPN");
				column.setAlign("right");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Nilai PPN");
				column.setAlign("right");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("PPH");
				column.setWidth("7%");
				column.setAlign("right");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Nilai PPH");
				column.setAlign("right");
				column.setWidth("9%");

			}

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");
		} else {

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setWidth("7%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Diterima");
			column.setWidth("7%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Selisih");
			column.setWidth("7%");
			column.setAlign("right");

			if (!beliLangsung) {

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Harga");
				column.setAlign("right");
				column.setWidth("12%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Diskon");
				column.setAlign("right");
				column.setWidth("7%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("PPN");
				column.setAlign("right");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Nilai PPN");
				column.setAlign("right");
				column.setWidth("10%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("PPH");
				column.setWidth("10%");
				column.setAlign("right");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Nilai PPH");
				column.setAlign("right");
				column.setWidth("9%");

			}

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");
			column.setWidth("12%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");

		}

		footerTotalSemua = new Footer(Common.numberFormat.get().format(0.0));
		footerTotalSemuaPpn = new Footer(Common.numberFormat.get().format(0.0));
		footerTotalSemuaPph = new Footer(Common.numberFormat.get().format(0.0));

		loadData(null);

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer("Sub Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		if (!beliLangsung) {

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);

			foot.appendChild(footerTotalSemuaPpn);

			footer = new Footer();
			foot.appendChild(footer);

			foot.appendChild(footerTotalSemuaPph);

		}

		foot.appendChild(footerTotalSemua);

		if (!persetujuan) {
			footer = new Footer();
			foot.appendChild(footer);
			footer = new Footer();
			foot.appendChild(footer);
		}

		hitungUlang();

	}

}
