package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.TransferItemDetailAction;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanTransferItemWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.TransferItem;
import ais.database.model.sirs.TransferItemDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk transfer item. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Window
 * window}, {@code Grid grid}, {@code Paging paging}, {@code MyTextbox searchkode}, {@code MyTextbox kode},
 * {@code MyTextbox keterangan}, {@code MyDatebox tanggalPembuatan}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class TransferItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Window window;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private Combobox lokasi;
	private Combobox lokasiTujuan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private TransferItem transferItem;
	private Toolbarbutton add;
	private Lokasi myLokasi;

	private Lokasi lokasi1;
	private Lokasi lokasi2;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		lokasi1 = (Lokasi) window.getAttribute("lokasi1");
		lokasi2 = (Lokasi) window.getAttribute("lokasi2");
		myLokasi = Common.getCurrentLokasi();

		if (myLokasi == null || myLokasi.getId() == null) {
			HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			String remoteIp = request.getRemoteAddr();
			MyMessageboxConfig.showFormat("Mohon maaf, lokasi Bapak/Ibu dengan alamat IP {V1} belum ditentukan. Untuk saat ini Anda belum dapat melakukan transfer (mutasi) barang, persetujuan, maupun penerimaan antar lokasi. Langkah yang dapat dilakukan: (1) hubungi administrator untuk menetapkan lokasi bagi alamat IP tersebut; (2) setelah lokasi ditetapkan, silakan ulangi kembali proses Anda.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, remoteIp);
		}

		// if (lokasi1 == null) {
		// lokasi1 = myLokasi;
		// }
		// if (lokasi2 == null) {
		// lokasi2 = myLokasi;
		// }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public void onCetak(Event event) throws Exception {
		LaporanTransferItemWindow laporanHargaJualItem = new LaporanTransferItemWindow(lokasi1, lokasi2);
		laporanHargaJualItem.setTitle("Laporan Data Transfer Per Periode");
		laporanHargaJualItem.setClosable(true);
		laporanHargaJualItem.setWidth("750px");
		laporanHargaJualItem.setHeight("95%");
		laporanHargaJualItem.setParent(page.getFirstRoot());
		laporanHargaJualItem.onModal();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TransferItemAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransferItemAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TransferItemAction
	 */
	class TransferItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TransferItem transferItem = (TransferItem) arg1;

			final TransferItemDetailAction detail;
			(detail = new TransferItemDetailAction(transferItem, myLokasi)).setParent(arg0);

			RevisiHelper.createNewRevisi(TransferItem.class, transferItem, transferItem.getKode()).setParent(arg0);

			new Label(transferItem.getLokasi() == null ? "" : transferItem.getLokasi().getNama()).setParent(arg0);

			new Label(transferItem.getLokasiTujuan() == null ? "" : transferItem.getLokasiTujuan().getNama())
					.setParent(arg0);

			new Label(transferItem.getDibuatOleh() == null ? "" : transferItem.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(transferItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(transferItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(transferItem.getDisetujuiOleh() == null ? ""
					: transferItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(transferItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(transferItem.getTanggalPersetujuan()))).setParent(arg0);

			final Label diterimaOleh;
			(diterimaOleh = new Label(transferItem.getDiterimaOleh() == null ? ""
					: transferItem.getDiterimaOleh().getUserNama())).setParent(arg0);

			final Label diterimaTanggal;
			(diterimaTanggal = new Label(transferItem.getTanggalPenerimaan() == null ? ""
					: Common.dateFormat3.get().format(transferItem.getTanggalPenerimaan()))).setParent(arg0);

			new Label(transferItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transfer Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = new HashMap();
					parameters.put("id", transferItem.getId());
					Report.generateWindowReport(Report.PDF, parameters, "sirs/transfer_item",
							transferItem.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final Toolbarbutton diterima = new ais.ui.util.MyToolbarbuttonConfig("", "/img/sent.png");

			final Toolbarbutton ditolak = new ais.ui.util.MyToolbarbuttonConfig("", "/img/block.gif");

			ditolak.setTooltiptext("Ditolak");
			ditolak.setVisible(
					reject && transferItem.getDisetujuiOleh() != null && transferItem.getDiterimaOleh() != null
							&& (myLokasi != null && myLokasi.getId().equals(transferItem.getLokasiTujuan().getId())));

			diterima.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menerima Transfer Item ini? Setelah diterima, stok barang akan ditambahkan ke lokasi tujuan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferItem.setDiterimaOleh(Common.getCurrentUser());
										transferItem.setTanggalPenerimaan(new Date());
										Common.refreshUpdate(session, (transferItem));

										List<TransferItemDetail> transferItemDetails = session
												.createCriteria(TransferItemDetail.class)
												.add(Restrictions.eq("transferItem", transferItem)).list();

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where lokasi = "
														+ myLokasi.getId()
														+ "  and transfer_item_detail in (select id from sirs.transfer_item_detail where transfer_item = "
														+ transferItem.getId() + ");")
												.executeUpdate();

										for (TransferItemDetail transferItemDetail : transferItemDetails) {
											// Ke Lokasi
											DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi.setTransferItemDetail(transferItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(transferItemDetail.getItem());
											detailTransaksi.setAmount(transferItemDetail.getHargaJual());
											detailTransaksi.setKeterangan(
													"Transfer Item dari Lokasi " + transferItem.getLokasi().getNama()
															+ " ke Lokasi " + transferItem.getLokasiTujuan().getNama());
											detailTransaksi.setKodeTransaksi(ConstantValues.transferItemKe);
											detailTransaksi.setLokasi(transferItem.getLokasiTujuan());
											detailTransaksi.setQty(transferItemDetail.getJumlah() == null ? 0.0
													: transferItemDetail.getJumlah());
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);

											// Simpan Selisih Ke Lokasi
											detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi.setTransferItemDetail(transferItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(transferItemDetail.getItem());
											detailTransaksi.setAmount(transferItemDetail.getHargaJual());
											detailTransaksi.setKeterangan("Selisih Transfer Item dari Lokasi "
													+ transferItem.getLokasi().getNama() + " ke Lokasi "
													+ transferItem.getLokasiTujuan().getNama());
											detailTransaksi.setKodeTransaksi(ConstantValues.transferItemSelisih);
											detailTransaksi.setLokasi(transferItem.getLokasiTujuan());
											detailTransaksi.setQty(transferItemDetail.getSelisih() == null ? 0.0
													: transferItemDetail.getSelisih());
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);
										}

										diterimaTanggal.setValue(transferItem.getTanggalPenerimaan() == null ? ""
												: Common.dateFormat3.get().format(transferItem.getTanggalPenerimaan()));
										diterimaOleh.setValue(transferItem.getDiterimaOleh() == null ? ""
												: transferItem.getDiterimaOleh().getUserNama());
										diterima.setVisible(approve && transferItem.getDiterimaOleh() == null);
										ditolak.setVisible(reject && transferItem.getDiterimaOleh() != null);

										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			diterima.setParent(toolbar);

			ditolak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menolak Transfer Item ini? Setelah ditolak, transfer tidak akan diproses lebih lanjut.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferItem.setDiterimaOleh(null);
										transferItem.setTanggalPenerimaan(null);
										Common.refreshUpdate(session, (transferItem));

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where lokasi = "
														+ myLokasi.getId()
														+ "  and transfer_item_detail in (select id from transfer_item_detail where transfer_item = "
														+ transferItem.getId() + ");")
												.executeUpdate();

										diterimaTanggal.setValue(transferItem.getTanggalPenerimaan() == null ? ""
												: Common.dateFormat3.get().format(transferItem.getTanggalPenerimaan()));
										diterimaOleh.setValue(transferItem.getDiterimaOleh() == null ? ""
												: transferItem.getDiterimaOleh().getUserNama());
										diterima.setVisible(approve && transferItem.getDiterimaOleh() == null);
										ditolak.setVisible(reject && transferItem.getDiterimaOleh() != null);

										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			ditolak.setParent(toolbar);

			diterima.setTooltiptext("Penerimaan");
			diterima.setVisible(
					approve && transferItem.getDisetujuiOleh() != null && transferItem.getDiterimaOleh() == null
							&& (myLokasi != null && myLokasi.getId().equals(transferItem.getLokasiTujuan().getId())));

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");

			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			hapus.setVisible(
					delete && transferItem.getDiterimaOleh() == null && transferItem.getDisetujuiOleh() == null);

			rubah.setVisible(edit && transferItem.getDiterimaOleh() == null && transferItem.getDisetujuiOleh() == null);

			disetujui.setVisible(
					approve && transferItem.getDiterimaOleh() == null && transferItem.getDisetujuiOleh() == null
							&& (myLokasi != null && myLokasi.getId().equals(transferItem.getLokasi().getId())));
			dibatalkan.setVisible(
					reject && transferItem.getDiterimaOleh() == null && transferItem.getDisetujuiOleh() != null
							&& (myLokasi != null && myLokasi.getId().equals(transferItem.getLokasi().getId())));

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Transfer Item ini? Setelah disetujui, transfer akan diproses lebih lanjut.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferItem.setDisetujuiOleh(Common.getCurrentUser());
										transferItem.setTanggalPersetujuan(new Date());

										transferItem.setTanggalPenerimaan(null);
										transferItem.setDiterimaOleh(null);

										Common.refreshUpdate(session, (transferItem));

										List<TransferItemDetail> transferItemDetails = session
												.createCriteria(TransferItemDetail.class)
												.add(Restrictions.eq("transferItem", transferItem)).list();

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where lokasi = "
														+ myLokasi.getId()
														+ "  and transfer_item_detail in (select id from sirs.transfer_item_detail where transfer_item = "
														+ transferItem.getId() + ");")
												.executeUpdate();

										for (TransferItemDetail transferItemDetail : transferItemDetails) {

											// Dari Lokasi
											DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi.setTransferItemDetail(transferItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(transferItemDetail.getItem());
											detailTransaksi.setAmount(transferItemDetail.getHargaJual());
											detailTransaksi.setKeterangan(
													"Transfer Item dari Lokasi " + transferItem.getLokasi().getNama()
															+ " ke Lokasi " + transferItem.getLokasiTujuan().getNama());
											detailTransaksi.setKodeTransaksi(ConstantValues.transferItemDari);
											detailTransaksi.setLokasi(transferItem.getLokasi());
											detailTransaksi.setQty(transferItemDetail.getJumlah());
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);
										}

										disetujuiTanggal.setValue(transferItem.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(transferItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(transferItem.getDisetujuiOleh() == null ? ""
												: transferItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && transferItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && transferItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && transferItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && transferItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan Transfer Item ini? Perlu diketahui bahwa transfer yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferItem.setDisetujuiOleh(null);
										transferItem.setTanggalPersetujuan(null);

										transferItem.setTanggalPenerimaan(null);
										transferItem.setDiterimaOleh(null);

										Common.refreshUpdate(session, (transferItem));

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where lokasi = "
														+ myLokasi.getId()
														+ "  and transfer_item_detail in (select id from sirs.transfer_item_detail where transfer_item = "
														+ transferItem.getId() + ");")
												.executeUpdate();

										disetujuiTanggal.setValue(transferItem.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(transferItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(transferItem.getDisetujuiOleh() == null ? ""
												: transferItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && transferItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && transferItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && transferItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && transferItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setDisabled(myLokasi == null || transferItem.getLokasi() == null
					|| !transferItem.getLokasi().getId().equals(myLokasi.getId()));

			hapus.setDisabled(myLokasi == null || transferItem.getLokasi() == null
					|| !transferItem.getLokasi().getId().equals(myLokasi.getId()));

			rubah.setTooltiptext("Rubah Data");
			rubah.setVisible(edit && transferItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(transferItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && transferItem.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											List<TransferItemDetail> transferItemDetails = session
													.createCriteria(TransferItemDetail.class)
													.add(Restrictions.eq("transferItem", transferItem)).list();
											for (TransferItemDetail transferItemDetail : transferItemDetails) {
												Common.refreshDelete(session, transferItemDetail);
											}

											Common.refreshDelete(session, transferItem);
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
			});
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransferItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final TransferItem transferItem) throws Exception {
		this.transferItem = transferItem;
		addWindow.setTitle(transferItem.getId() == null ? "Tambah Transfer Item" : "Ubah Transfer Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transfer Item")));
		String mykode = transferItem.getKode();

		row.appendChild(kode = new MyTextbox(transferItem.getKode() == null ? mykode : transferItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan")));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				transferItem.getTanggalPembuatan() == null ? new Date() : transferItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dari Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (lokasi1 == null) {
			Common.selectComboItem(lokasi, transferItem.getLokasi() == null ? myLokasi : transferItem.getLokasi());
			lokasi.setDisabled(myLokasi != null);
		} else {
			Common.selectComboItem(lokasi, lokasi1);
			lokasi.setDisabled(true);
			mykode = Common.generateCode(TransferItem.class, 8, "TRF", lokasi1);
			kode.setValue(mykode);
		}
		lokasi.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
				if (myLokasi == null) {
					return;
				}
				String mykode = Common.generateCode(TransferItem.class, 8, "TRF", myLokasi);
				kode.setValue(mykode);

				Common.clear(lokasiTujuan);
				Common.insertCombo(lokasiTujuan, "nama", Lokasi.class, Restrictions.ne("id", myLokasi.getId()));
				Common.selectComboItem(lokasiTujuan,
						transferItem.getLokasiTujuan() == null ? null : transferItem.getLokasiTujuan());
			}
		};
		lokasi.addEventListener("onChange", eventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ke Lokasi")));
		row.appendChild(lokasiTujuan = new Combobox());

		if (lokasi2 == null) {
			if (transferItem.getLokasiTujuan() != null) {
				Common.insertCombo(lokasiTujuan, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(lokasiTujuan,
						transferItem.getLokasiTujuan() == null ? null : transferItem.getLokasiTujuan());
			}
			eventListener.onEvent(null);
		} else {
			Common.insertCombo(lokasiTujuan, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(lokasiTujuan, lokasi2);
			lokasiTujuan.setDisabled(true);
		}
		lokasiTujuan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new MyTextbox(transferItem.getKeterangan() == null ? "" : transferItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi Kode Transfer terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan Kode Transfer; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Dari Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih lokasi asal (Dari Lokasi); (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasiTujuan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Ke Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih lokasi tujuan (Ke Lokasi); (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transferItem.getId() != null) {
			transferItem = (TransferItem) session.load(TransferItem.class, transferItem.getId());

		}

		transferItem.setLokasiTujuan((Lokasi) lokasiTujuan.getSelectedItem().getValue());
		transferItem.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		transferItem.setKode(kode.getValue());
		transferItem.setKeterangan(keterangan.getValue());
		transferItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (transferItem.getId() != null) {
			Common.refreshUpdate(session, transferItem);
		} else {
			transferItem.setDibuatOleh(Common.getCurrentUser());

			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			transferItem.setIndex(Common.generateMaxByLokasi(TransferItem.class, myLokasi) + 1);
			String mykode = Common.generateCode(TransferItem.class, 8, "TRF", myLokasi);
			kode.setValue(mykode);
			transferItem.setKode(mykode);
			session.save(transferItem);
		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransferItem.class)
				.add(lokasi1 == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("lokasi", lokasi1), Restrictions.eq("lokasiTujuan", lokasi1)))
				.add(lokasi2 == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("lokasi", lokasi2), Restrictions.eq("lokasiTujuan", lokasi2)))
				.add(myLokasi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("lokasi", myLokasi),
								Restrictions.eq("lokasiTujuan", myLokasi)))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<TransferItem> transferItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transferItem);
		grid.setRowRenderer(new TransferItemRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}
}
