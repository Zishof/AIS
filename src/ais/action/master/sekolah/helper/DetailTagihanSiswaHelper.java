package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.EntityIdentityMap;
import ais.common.DataRecoveryHelper;
import ais.common.MemoryDbUtil;
import ais.common.ProgressListener;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.PengaturanBiayaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DetailTagihanSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private List<PengaturanBiayaItemBiaya> pengaturanBiayaItemBiayas;
	private PengaturanBiaya pengaturanBiaya;

	private Textbox nama;

	private Siswa siswa;
	private boolean edit = false;
	private boolean approve = false;

	/**
	 * Membuat atau mengambil tagihan baru. Dioptimasi: Method ini sekarang
	 * difaktorkan ulang untuk memanggil fungsi utama dari Tagihan.ambilAtauBuat
	 * agar tidak ada duplikasi kueri, sekaligus mempertahankan logika perhitungan
	 * nominal angsuran khusus.
	 */
	public static Tagihan butTagihanBaru(Tagihan tagihan, Session session, Integer bayarKe, NominalBiaya nominalBiaya,
			Integer bulan, Integer tahun, List<Long> notPembayaran, PengaturanBiaya pengaturanBiaya, boolean paksa) {

		// Langsung kembalikan jika tagihan sudah ada dan tidak dipaksa
		if (tagihan != null && !paksa) {
			return tagihan;
		}

		// Pencegahan NullPointerException (Safety Check)
		if (nominalBiaya == null || nominalBiaya.getPengaturanBiaya() == null || nominalBiaya.getSiswa() == null) {
			return tagihan;
		}

		// Format tahun dan bulan menjadi format tahunbulan (YYYYMM) untuk dieksekusi
		// oleh ambilAtauBuat
		Integer tahunbulan = PembayaranSiswa.convert(tahun, bulan);

		// MENGGUNAKAN ULANG (REUSE) METHOD UTAMA
		tagihan = ais.database.model.sekolah.Tagihan.ambilAtauBuat(session, nominalBiaya.getItemBiayaSekolah(),
				pengaturanBiaya, nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya,
				tahunbulan, null, // dibayarManual (null karena di butTagihanBaru tidak digunakan)
				paksa // ambilManual disamakan dengan flag paksa
		);

		return tagihan;
	}

	public DetailTagihanSiswaHelper(Siswa siswa, boolean edit, boolean approve) {
		this.siswa = siswa;
		this.edit = edit;
		this.approve = approve;
	}

	private void tambahkanBantuanReset(Component parent, final boolean resetSatuItemBiaya) {
		MyToolbarbuttonConfig bantuan = new MyToolbarbuttonConfig("", "/img/svg/question-circle.svg");
		bantuan.setTooltiptext("Bantuan tombol Reset");
		bantuan.setStyle("font-size:8px;font-weight:bold;cursor:pointer;margin-left:2px;");
		bantuan.setParent(parent);
		bantuan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String pesan;
				if (resetSatuItemBiaya) {
					pesan = "Tombol Reset digunakan untuk menghitung ulang tagihan siswa pada item biaya ini.\n\n"
							+ "Yang dilakukan sistem:\n"
							+ "1. Menghapus data NominalBiaya dan Tagihan yang BELUM dibayar untuk siswa dan item biaya tersebut.\n"
							+ "2. Membuat ulang tagihan berdasarkan Setting Biaya yang sedang aktif.\n"
							+ "3. Memuat ulang tampilan agar nominal terbaru terlihat.\n\n"
							+ "Tagihan yang sudah memiliki pembayaran tidak ikut dihapus. Gunakan tombol ini jika nominal, bulan, status Bukan Tagihan, atau setting biaya berubah dan data di baris siswa perlu disegarkan.";
				} else {
					pesan = "Tombol Reset pada rincian tagihan digunakan untuk menghapus nominal/tagihan yang belum dibayar pada bagian tersebut, lalu tampilan dimuat ulang agar sistem bisa membentuk ulang data sesuai setting biaya.\n\n"
							+ "Tagihan yang sudah dibayar tidak boleh dihapus otomatis. Gunakan ini saat nominal atau status tagihan terlihat tidak sesuai setelah perubahan setting biaya.";
				}
				MyMessageboxConfig.show(pesan, "Bantuan Tombol Reset", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();
		private PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya;
		private Integer pembayaranTerakhir;

		public DetailPARenderer(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Integer pembayaranTerakhir) {
			this.pengaturanBiayaItemBiaya = pengaturanBiayaItemBiaya;
			this.pembayaranTerakhir = pembayaranTerakhir;
		}

		@Override
		public void render(Row row, Object data) throws Exception {
			render(row, data, false);
		}

		public void render(final Row row, Object data, boolean bagi) throws Exception {
			row.setValign("top");
			final Siswa siswa = (Siswa) data;

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);

			CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox1);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(hbox1);

			RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNomorInduk()).setParent(vbox1);

			NominalBiaya nominalBiaya = null;
			Session sessionNominal = null;
			try {
				sessionNominal = HibernateUtil.getSessionFactory().openSession();
				nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa, pembayaranTerakhir,
						sessionNominal);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:192");
			} finally {
				if (sessionNominal != null && sessionNominal.isOpen()) {
					try {
						sessionNominal.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:197");
					}
					try {
						sessionNominal.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:201");
					}
					try {
						sessionNominal.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:205");
					}
				}
			}

			if (nominalBiaya == null) {
				new Label(siswa.getNama()).setParent(vbox1);
			} else {
				RevisiHelper.createNewRevisi(NominalBiaya.class, nominalBiaya, siswa.getNama()).setParent(vbox1);
				KelasSiswa kelasSiswa = Siswa.ambilKelas(siswa, nominalBiaya.getPengaturanBiaya().getTahunAjaran());
				if (kelasSiswa != null) {
					new Label(kelasSiswa.getNama()).setParent(vbox1);
				}
			}
			siswa.tampilkanHp(vbox1);
			siswa.tampilkanEmail(vbox1);

			final NominalBiaya nb = nominalBiaya;

			// Jika tidak ada nominalBiaya, kita hentikan render agar tidak terjadi NPE
			if (nb == null) {
				return;
			}

			final Checkbox bukanTagihan = new Checkbox(
					"\"" + pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama() + "\" Bukan Tagihan");
			bukanTagihan.setChecked(nb.getBukanTagihan());
			bukanTagihan.setStyle("font-size:8px;");
			final Hbox hboxBukanReset = new Hbox();
			hboxBukanReset.setStyle("align-items:center;gap:4px;");
			hboxBukanReset.setParent(vbox1);
			bukanTagihan.setParent(hboxBukanReset);
			bukanTagihan.setDisabled(!edit);
			bukanTagihan.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {
					MyMessageboxConfig.showFormatCb(
							"Apakah Anda yakin \"{V1}\" ini {V2} tagihan? Perubahan ini akan memengaruhi status penagihan item tersebut.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										boolean _sqlOk = false;
										Session sessionUpdate = null;
										Transaction tx = null;
										try {
											sessionUpdate = HibernateUtil.getSessionFactory().openSession();
											tx = sessionUpdate.beginTransaction();
											long _siswaId = siswa.getId();
											long _itemId  = nb.getItemBiayaSekolah().getId();
											long _pbId    = nb.getPengaturanBiaya().getId();
											// Update semua tagihan belum-bayar milik siswa+item+pb
											sessionUpdate.createSQLQuery(
													"UPDATE sekolah.tagihan SET aktif = " + (!bukanTagihan.isChecked())
															+ " WHERE siswa_id = " + _siswaId
															+ " AND item_biaya_id = " + _itemId
															+ " AND pengaturan_biaya = " + _pbId
															+ " AND pembayaran_siswa_detail_id IS NULL")
													.executeUpdate();
											// Update bukantagihan di semua bulan NominalBiaya milik item+pb+siswa
											sessionUpdate.createSQLQuery(
													"UPDATE sekolah.nominal_biaya SET bukantagihan = " + bukanTagihan.isChecked()
															+ " WHERE item_biaya_sekolah_id = " + _itemId
															+ " AND pengaturan_biaya_id = " + _pbId
															+ " AND siswa_id = " + _siswaId)
													.executeUpdate();
											tx.commit();
											_sqlOk = true;
										} catch (Exception eUpd) {
											if (tx != null && tx.isActive())
												tx.rollback();
											eUpd.printStackTrace(); ais.common.ErrorAuditUtil.record(eUpd, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:279");
										} finally {
											if (sessionUpdate != null && sessionUpdate.isOpen()) {
												try {
													sessionUpdate.clear();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:284");
												}
												try {
													sessionUpdate.disconnect();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:288");
												}
												try {
													sessionUpdate.close();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:292");
												}
											}
										}


										// Sinkronkan in-memory cache setelah SQL commit berhasil
										if (_sqlOk) {
											Session sessionCache = null;
											try {
												sessionCache = HibernateUtil.getSessionFactory().openSession();
												@SuppressWarnings("unchecked")
												List<NominalBiaya> freshNbs = sessionCache
													.createCriteria(NominalBiaya.class)
													.add(Restrictions.eq("pengaturanBiaya", nb.getPengaturanBiaya()))
													.add(Restrictions.eq("itemBiayaSekolah", nb.getItemBiayaSekolah()))
													.add(Restrictions.eq("siswa", siswa))
													.list();
												for (NominalBiaya freshNb : freshNbs) {
													EntityIdentityMap.canonical(freshNb);
												}
												@SuppressWarnings("unchecked")
												List<Tagihan> freshTagihans = sessionCache
													.createCriteria(Tagihan.class)
													.createAlias("nominalBiaya", "nb_cache")
													.add(Restrictions.eq("nb_cache.pengaturanBiaya", nb.getPengaturanBiaya()))
													.add(Restrictions.eq("nb_cache.itemBiayaSekolah", nb.getItemBiayaSekolah()))
													.add(Restrictions.eq("siswa", siswa))
													.list();
												for (Tagihan freshTag : freshTagihans) {
													EntityIdentityMap.canonical(freshTag);
												}
											} catch (Exception eCache) { ais.common.ErrorAuditUtil.record(eCache, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:324");
												// non-fatal
											} finally {
												if (sessionCache != null && sessionCache.isOpen()) {
													try { sessionCache.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:328");}
													try { sessionCache.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:329");}
												}
											}
											// Reload MemoryDbUtil.getAllTagihan() cache untuk PengaturanBiaya ini
											PengaturanBiaya.reloadTagihan(nb.getPengaturanBiaya(), true);
										}
										nb.setBukanTagihan(bukanTagihan.isChecked());
										Common.refreshUpdate(nb);
										Common.clear(row);
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												render(row, siswa);
											}
										});
									} else {
										// Revert checkbox state if user canceled
										bukanTagihan.setChecked(!bukanTagihan.isChecked());
									}
								}
							}, nb.getItemBiayaSekolah().getNama(), (bukanTagihan.isChecked() ? "bukan" : "adalah"));
				}
			});

			// Tombol Reset: hapus NominalBiaya + Tagihan yang belum dibayar, lalu reload
			if (edit) {
				final Toolbarbutton btnReset = new Toolbarbutton("Reset");
				btnReset.setStyle("font-size:8px;color:red;font-weight:bold;cursor:pointer;");
				btnReset.setTooltiptext("Hapus NominalBiaya + Tagihan yang belum dibayar, lalu buat ulang");
				btnReset.setParent(hboxBukanReset);
				tambahkanBantuanReset(hboxBukanReset, true);
				btnReset.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event eRst) throws Exception {
						MyMessageboxConfig.showFormatCb(
							"Apakah Anda yakin ingin mereset \"{V1}\"? Seluruh NominalBiaya dan Tagihan yang belum dibayar akan dihapus lalu dibuat ulang.",
							"Konfirmasi Reset",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event evOk) throws Exception {
									if (Integer.parseInt(evOk.getData().toString()) != MyMessageboxConfig.OK) return;
								TagihanUtil.resetNominalBiayaDanTagihan(nb, siswa, null);
								PengaturanBiaya.reloadTagihan(nb.getPengaturanBiaya(), true);
								Common.clear(row);
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										render(row, siswa);
									}
								});
							}
						}, nb.getItemBiayaSekolah().getNama());
				}
			});
		}

		PengaturanBiaya pengaturanBiaya = nb.getPengaturanBiaya();

			if (mul != null && sam != null) {
				List<Long> notPembayaran = new ArrayList<Long>();

				// Gunakan single session untuk satu iterasi full tagihan agar I/O lebih efisien
				Session sessionLoop = null;
				try {
					sessionLoop = HibernateUtil.getSessionFactory().openSession();

					for (int pembayaranTrx = mul; pembayaranTrx <= sam; pembayaranTrx++) {

						if ((pengaturanBiaya.getBulanMulai() != null && pembayaranTrx < pengaturanBiaya.getBulanMulai())
								|| (pengaturanBiaya.getBulanSampai() != null
										&& pembayaranTrx > pengaturanBiaya.getBulanSampai())) {
							continue;
						}

						int tahun = Integer.parseInt((pembayaranTrx + "").substring(0, 4));
						int bulan = Integer.parseInt((pembayaranTrx + "").substring(4));
						if (bulan > 12 || bulan < 1) {
							continue;
						}

						// Jika item ini "bukan tagihan", tampilkan sel kosong — jangan buat tagihan
						if (nb.getBukanTagihan()) {
							Vbox vbBukan = new Vbox();
							vbBukan.setParent(row);
							vbBukan.setSclass("ais-tagihan-bln-cell");
							continue;
						}

						int bayarKe = 1;
						String kodeUnik = Tagihan.genCode(nb.getItemBiayaSekolah(), nb.getPengaturanBiaya(),
								pembayaranTrx, nb.getSiswa(), nb.getCalonSiswa(), bayarKe);

						Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

						boolean paksa = false;
						try {
							if (!tagihan.getTahunbulan().equals(pembayaranTrx)) {
								paksa = true;
							}
						} catch (Exception e) {
							paksa = true;
						}

						try {
							tagihan = butTagihanBaru(tagihan, sessionLoop, bayarKe, nb, bulan, tahun, notPembayaran,
									pengaturanBiaya, paksa);
						} catch (Exception exTagihanBaru) {
							// Isolasi kegagalan per-kolom (mis. data kelas siswa sudah tidak valid/basi
							// di cache in-memory) supaya satu baris data stale tidak menggagalkan seluruh
							// render Grid (baris/kolom lain tetap tampil).
							ais.common.ErrorAuditUtil.record(exTagihanBaru,
									"DetailTagihanSiswaHelper.render: gagal buat/ambil Tagihan siswaId="
											+ (siswa != null ? siswa.getId() : null) + " kodeUnik=" + kodeUnik);
							Vbox vbErr = new Vbox();
							vbErr.setParent(row);
							vbErr.setSclass("ais-tagihan-bln-cell");
							vbErr.setWidth("100%");
							vbErr.setAlign("center");
							MyLabelAgakKecil lblErr = new MyLabelAgakKecil("Data kelas siswa sudah tidak valid, silakan refresh halaman");
							lblErr.setStyle("color:red;font-size:8px;");
							lblErr.setParent(vbErr);
							continue;
						}

						// Guard: jika tagihan yang dikembalikan memiliki tahunbulan berbeda dari kolom ini,
						// jangan render agar satu tagihan tidak muncul di banyak kolom sekaligus.
						if (tagihan != null && tagihan.getTahunbulan() != null
								&& !tagihan.getTahunbulan().equals(pembayaranTrx)) {
							Vbox vbKhusus = new Vbox();
							vbKhusus.setParent(row);
							vbKhusus.setSclass("ais-tagihan-bln-cell");
							vbKhusus.setWidth("100%");
							vbKhusus.setAlign("center");
							continue;
						}

						if (tagihan != null) {

							System.out.println("tagihan -> " + tagihan + ", paksa -> " + paksa + ", pembayaranTrx "
									+ pembayaranTrx + ", kodeUnik " + kodeUnik + ", tahun " + tahun + ", bulan " + bulan
									+ ", tagihan.getTahun() " + tagihan.getTahun() + ", tagihan.getBulan() "
									+ tagihan.getBulan() + ", tagihan.getTahunbulan() " + tagihan.getTahunbulan());

							Vbox vbox = new Vbox();
							vbox.setParent(row);
							final Tagihan tag = tagihan;

							if (!nb.getBukanTagihan() && !tag.ambilBukanTagihanData() && !tag.ambilBukanTagihan()) {
								// Tampilan VERTIKAL: nominal & tombol riwayat "H" ditumpuk ke bawah (Vbox)
								// agar sel tidak sempit dan lebih enak dipandang.
								Vbox hbox = new Vbox();
								hbox.setParent(vbox);

								final MyDoublebox nilai = new MyDoublebox(tagihan.getNominal());
								nilai.setCols(7);

								if (nb.getPengaturanBiayaItemBiaya() != null
										&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya() != null
										&& nb.getPengaturanBiayaItemBiaya().getMinimalBiaya() != null
										&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya() > 0.1
										&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya().intValue() == nb
												.getPengaturanBiayaItemBiaya().getMinimalBiaya().intValue()) {
									new Label(Common.numberFormat.get().format(nb.getNominal())).setParent(hbox);
								} else if (nb.getDibayarSebayak() == 1 && tagihan.getPembayaranSiswaDetail() != null) {
									new Label(Common.numberFormat.get()
											.format(tagihan.getNominal() + tagihan.getDiskon())).setParent(hbox);
								} else if (!edit || tag.getPembayaranSiswaDetail() != null || tag.getKunci() != null
										|| tag.getPengaturanBiaya().getKunci() != null
										|| tag.getItemBiayaSekolah().getParameterTambahan() != null) {
									new Label(Common.numberFormat.get()
											.format(tagihan.getNominal() + tagihan.getDiskon())).setParent(hbox);
								} else {
									nilai.setParent(hbox);
								}

								nilai.addEventListener("onChange", new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										if (nb.getPengaturanBiayaItemBiaya() != null && nilai.getValue() != null && nb
												.getPengaturanBiayaItemBiaya().getMinimalBiaya() > nilai.getValue()) {

											MyMessageboxConfig.showFormatCb(
													"Nominal yang Anda masukkan kurang dari batas minimal tagihan. Minimal tagihan yang diperbolehkan adalah {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nominal yang dimasukkan; (2) masukkan nominal sesuai batas minimal yang ditentukan; (3) simpan ulang perubahan Anda.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															nilai.setValue(nb.getNominal());
														}
													}, Common.numberFormat.get().format(nb.getPengaturanBiayaItemBiaya().getMinimalBiaya()));
											return;
										}

										Session sessionT = null;
										Transaction txT = null;
										try {
											sessionT = HibernateUtil.getSessionFactory().openSession();
											sessionT.refresh(tag);
											tag.setNominal(nilai.getValue());
											tag.setNominalManual(nilai.getValue());

											txT = sessionT.beginTransaction();
											sessionT.update(tag);

											if (nb.getDibayarSebayak().intValue() == 1) {
												if (!nb.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode()
														.equalsIgnoreCase("Bulanan")) {
													nb.setNominal(nilai.getValue());
													sessionT.update(nb);
												}
											}
											txT.commit();
										} catch (Exception eUp) {
											if (txT != null && txT.isActive())
												txT.rollback();
											eUp.printStackTrace(); ais.common.ErrorAuditUtil.record(eUp, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:526");
										} finally {
											if (sessionT != null && sessionT.isOpen()) {
												try {
													sessionT.clear();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:531");
												}
												try {
													sessionT.disconnect();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:535");
												}
												try {
													sessionT.close();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:539");
												}
											}
										}
									}
								});

								PembayaranSiswaDetail a = tagihan.getPembayaranSiswaDetail();
								if (a != null && a.getId() != null) {
									RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, a,
											"Dibayar : " + Common.numberFormat.get().format(a.getNominal()),
											"font-size:9px;").setParent(vbox);
								} else {
									if (edit) {
										TagihanUtilCalonSiswa.tampilkanKunci(vbox, tag, new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.clear(row);
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														render(row, siswa);
													}
												});
											}
										}, tbmuser, nilai, approve, true);
									}
								}

								RevisiHelper.createNewRevisi(Tagihan.class, tagihan, "H").setParent(hbox);

							}

							if (!nb.getBukanTagihan() && !tag.ambilBukanTagihan()) {
								if (tag.getPembayaranSiswaDetail() == null) {
									final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
									bukanTagihana.setChecked(tag.ambilBukanTagihanData());
									bukanTagihana.setDisabled(!edit);
									bukanTagihana.setStyle("font-size:8px;");
									if (tag.getKunci() == null && tag.getPengaturanBiaya().getKunci() == null) {
										// Tampilan VERTIKAL: "Bukan Tagihan" & badge ditumpuk ke bawah (Vbox).
										Vbox hbox = new Vbox();
										hbox.setParent(vbox);
										bukanTagihana.setParent(hbox);
										RevisiHelper.createNewRevisi(Tagihan.class, tag,
										tag.getId() != null ? String.valueOf(tag.getId() % 10000L) : "H",
										"font-size:8px;padding:0 1px;").setParent(hbox);
									}
									bukanTagihana.addEventListener("onCheck", new EventListener() {
										@Override
										public void onEvent(final Event event1) throws Exception {
											MyMessageboxConfig.showFormatCb("Apakah Anda yakin item ini {V1} tagihan? Perubahan ini akan memengaruhi status penagihan item tersebut.",
													"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
													MyMessageboxConfig.QUESTION, new EventListener() {
														@Override
														public void onEvent(Event event) throws Exception {
															int i = Integer.parseInt(event.getData().toString());
															if (i == MyMessageboxConfig.OK) {
																Session sessionB = null;
																Transaction txB = null;
																try {
																	sessionB = HibernateUtil.getSessionFactory()
																			.openSession();
																	Tagihan tagihanA = (Tagihan) sessionB
																			.createCriteria(Tagihan.class)
																			.add(Restrictions.idEq(tag.getId()))
																			.uniqueResult();
																	if (tagihanA == null) {
																		tagihanA = butTagihanBaru(tag, sessionB,
																				tag.getBayarKe(), tag.getNominalBiaya(),
																				tag.getBulan(), tag.getTahun(),
																				new ArrayList<Long>(),
																				nb.getPengaturanBiaya(), true);
																	}
																	tagihanA.setBukanTagihan(bukanTagihana.isChecked());

																	txB = sessionB.beginTransaction();
																	Common.refreshUpdate(sessionB, tagihanA);
																	txB.commit();

																	Common.clear(row);
																	Common.createDefaultTimer(new EventListener() {
																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {
																			render(row, siswa);
																		}
																	});
																} catch (Exception eB) {
																	if (txB != null && txB.isActive())
																		txB.rollback();
																	eB.printStackTrace(); ais.common.ErrorAuditUtil.record(eB, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:630");
																} finally {
																	if (sessionB != null && sessionB.isOpen()) {
																		try {
																			sessionB.clear();
																		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:635");
																		}
																		try {
																			sessionB.disconnect();
																		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:639");
																		}
																		try {
																			sessionB.close();
																		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:643");
																		}
																	}
																}
															} else {
																bukanTagihana.setChecked(!bukanTagihana.isChecked());
															}
														}
													}, (bukanTagihana.isChecked() ? "bukan" : "adalah"));
										}
									});
								} else {
									if (edit) {
										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pindahkan",
												"/img/stock_data_edit_table.png");
										button.setTooltiptext("Pindah Data");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												MyMessageboxConfig.show(
														"Apakah Anda yakin ingin memindahkan pembayaran siswa ini? Data pembayaran akan dipindahkan ke lokasi tujuan yang dipilih.",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {
																	try {
																		Tagihan.pindahkan(tag, new EventListener() {
																			@Override
																			public void onEvent(Event arg0)
																					throws Exception {
																				render(row, siswa);
																			}
																		});
																	} catch (Exception e) {
																		Common.tampilErrorJikaAdmin(e);
																	}
																}
															}
														});
											}
										});
										button.setParent(vbox);
									}
								}
							}
						}
					}
				} catch (Exception loopEx) {
					loopEx.printStackTrace(); ais.common.ErrorAuditUtil.record(loopEx, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:693");
				} finally {
					if (sessionLoop != null && sessionLoop.isOpen()) {
						try {
							sessionLoop.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:698");
						}
						try {
							sessionLoop.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:702");
						}
						try {
							sessionLoop.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:706");
						}
					}
				}

			} else {
				Vbox vb = new Vbox();
				vb.setParent(row);
				// Rapikan tampilan sel tagihan bulanan (nominal + Kunci/Bayar + Bukan Tagihan):
				// styling via CSS .ais-tagihan-bln-cell (lihat css_utama.css). Hanya kosmetik.
				vb.setSclass("ais-tagihan-bln-cell");
				vb.setWidth("100%");
				vb.setAlign("center");

				if (!nb.getBukanTagihan()) {
					Hbox hbox = new Hbox();
					hbox.setParent(vb);

					List<Tagihan> listTagihans = nb.ambilTagihans();

					final MyDoublebox nilai = new MyDoublebox(nb.getNominal());
					nilai.setCols(6);

					if (nb.getPengaturanBiayaItemBiaya() != null
							&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya() != null
							&& nb.getPengaturanBiayaItemBiaya().getMinimalBiaya() != null
							&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya() > 0.1
							&& nb.getPengaturanBiayaItemBiaya().getMaksimalBiaya().intValue() == nb
									.getPengaturanBiayaItemBiaya().getMinimalBiaya().intValue()) {
						new Label(Common.numberFormat.get().format(nb.getNominal())).setParent(hbox);
					} else if (nb.getItemBiayaSekolah().getParameterTambahan() != null || !edit) {
						new Label(Common.numberFormat.get().format(nb.getNominal())).setParent(hbox);
					} else {
						nilai.setParent(hbox);
					}

					nilai.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (nb.getPengaturanBiayaItemBiaya() != null && nilai.getValue() != null
									&& nb.getPengaturanBiayaItemBiaya().getMinimalBiaya() > nilai.getValue()) {

								MyMessageboxConfig.showFormatCb(
										"Nominal yang Anda masukkan kurang dari batas minimal tagihan. Minimal tagihan yang diperbolehkan adalah {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nominal yang dimasukkan; (2) masukkan nominal sesuai batas minimal yang ditentukan; (3) simpan ulang perubahan Anda.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												nilai.setValue(nb.getNominal());
											}
										}, Common.numberFormat.get().format(nb.getPengaturanBiayaItemBiaya().getMinimalBiaya()));
								return;
							}

							nb.setNominal(nilai.getValue());
							Common.refreshUpdate(nb);
						}
					});
					RevisiHelper.createNewRevisi(NominalBiaya.class, nb, " x ").setParent(hbox);

					boolean tagihanDIbuatOtomatisMenghitungSisa = Common.bolehKonfigurasi("tagihan_dibuat_otomatis_menghitung_sisa", Konfigurasi.TIDAK_AKTIF);

					if (nb.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Harian")) {
						new Label(Common.numberFormat.get().format(nb.getDibayarSebayak())).setParent(hbox);
					} else {
						final MyIntbox dibayarSebayak = new MyIntbox(
								nb.getDibayarSebayakTransient() != null ? nb.getDibayarSebayakTransient()
										: nb.getDibayarSebayak());
						dibayarSebayak.setCols(1);
						dibayarSebayak.setDisabled(!edit);
						dibayarSebayak.setParent(hbox);
						dibayarSebayak.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session sessionC = null;
								Transaction txC = null;
								try {
									sessionC = HibernateUtil.getSessionFactory().openSession();
									sessionC.refresh(nb);
									nb.setDibayarSebayakManual(dibayarSebayak.getValue());
									nb.setDibayarSebayak(dibayarSebayak.getValue());
									nb.setDibayarSebayakTransient(dibayarSebayak.getValue());

									txC = sessionC.beginTransaction();
									Common.refreshUpdate(sessionC, nb);
									txC.commit();

									Common.clear(row);
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											render(row, siswa);
										}
									});
								} catch (Exception eC) {
									if (txC != null && txC.isActive())
										txC.rollback();
									eC.printStackTrace(); ais.common.ErrorAuditUtil.record(eC, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:803");
								} finally {
									if (sessionC != null && sessionC.isOpen()) {
										try {
											sessionC.clear();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:808");
										}
										try {
											sessionC.disconnect();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:812");
										}
										try {
											sessionC.close();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:816");
										}
									}
								}
							}
						});
					}

					Toolbarbutton reset = new ais.ui.util.MyToolbarbuttonConfig("Reset", "/img/svg/deny.svg");
					reset.setDisabled(!edit);
					reset.setVisible(!tagihanDIbuatOtomatisMenghitungSisa);
					reset.setParent(hbox);
					tambahkanBantuanReset(hbox, false);
					reset.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah Anda yakin ingin mereset tagihan ini? Tindakan ini akan menghapus data tagihan yang belum dibayar.", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													TagihanUtil.resetNominalBiayaDanTagihan(nb, siswa, null);
													Common.clear(row);
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															render(row, siswa);
														}
													});
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.showFormat(
															"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakannya; (3) ulangi kembali proses penghapusan.",
															"Kesalahan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
															e.getMessage());
												}
											}
										}
									});
						}
					});

					if (!nb.getBukanTagihan()) {
						vb.appendChild(onTagihanRinciBaru(nilai, reset, nb, tbmuser, pengaturanBiayaItemBiaya, edit,
								approve, listTagihans));
					}
				}

				final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
				bukanTagihana.setChecked(nb.getBukanTagihan());
				bukanTagihana.setDisabled(!edit);
				bukanTagihana.setStyle("font-size:8px;");
				bukanTagihana.setParent(vb);
				bukanTagihana.setVisible(false);
				bukanTagihana.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(final Event event1) throws Exception {
						MyMessageboxConfig.showFormatCb(
								"Apakah Anda yakin item ini {V1} tagihan? Perubahan ini akan memengaruhi status penagihan item tersebut.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											nb.setBukanTagihan(bukanTagihana.isChecked());
											Common.refreshUpdate(nb);
											Common.clear(row);
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													render(row, siswa);
												}
											});
										} else {
											bukanTagihana.setChecked(!bukanTagihana.isChecked());
										}
									}
								}, (bukanTagihana.isChecked() ? "bukan" : "adalah"));
					}
				});
			}
		}
	}

	public static MyGrid onTagihanRinciBaru(final MyDoublebox nilai, final Toolbarbutton reset,
			final NominalBiaya nominalBiaya, final Tbmuser tbmuser,
			final PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, final boolean edit, final boolean approve,
			final List<Tagihan> tagihans) throws Exception {

		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		// Rapikan sub-grid rincian tagihan per bulan (kosmetik via CSS .ais-tagihan-bln-rinci).
		grid.setSclass("ais-tagihan-bln-rinci");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.getPagingChild().setMold("os");
		grid.getPagingChild().setPageIncrement(5);

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer();
		footer.setParent(foot);
		footer.setLabel("Total");

		footer = new Footer();
		footer.setParent(foot);

		final Footer footerTotal = new Footer();
		footerTotal.setParent(foot);
		footerTotal.setLabel("");

		final EventListener eventListenerHitung = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				Double total = 0.0;
				for (Row row : rows) {
					MyDoublebox nominal = (MyDoublebox) row.getAttribute("nominal");
					Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
					if (checkbox.isChecked()) {
						total += nominal.getValue() == null ? 0.0 : nominal.getValue();
					}
				}

				footerTotal.setLabel(
						Common.numberFormat.get().format(total) + (nominalBiaya.getItemBiayaSekolah().getBolehDiangsur()
								&& nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getBolehAngsurBerapapun()
										? " dari " + Common.numberFormat.get().format(nominalBiaya.getNominal()) + " ("
												+ Common.numberFormat.get().format(
														(total * 100.0 / nominalBiaya.getNominal()))
												+ "%) sisa "
												+ Common.numberFormat.get().format(nominalBiaya.getNominal() - total)
										: ""));
			}
		};

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		EventListener baruEventListener = new EventListener() {

			private EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rows);

				final Integer tahunbulan = nominalBiaya.getTahunbulan() != null ? nominalBiaya.getTahunbulan()
						: PembayaranSiswa.convert(
								nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukTahun(),
								nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukBulan());

				for (final Tagihan tagihan : tagihans) {
					if (tagihan == null)
						continue;

					final int bayarKe = tagihan.getBayarKe();

					if (nominalBiaya.getDibayarSebayak() == 1 && tagihan.getPembayaranSiswaDetail() != null) {
						reset.setDisabled(true);
						nilai.setValue(tagihan.getPembayaranSiswaDetail().getNominal());
						nilai.setDisabled(true);
					}

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					PembayaranSiswaDetail a = tagihan.getPembayaranSiswaDetail();

					if (a != null) {
						reset.setVisible(false);
					}

					RevisiHelper.createNewRevisi(Tagihan.class, tagihan, "ke-" + bayarKe).setParent(row);

					if (tagihan.getPengaturanBiaya().getTanggalTagihanMengikutiDefault() || !edit) {
						new Label(Common.dateFormat11.get().format(tagihan.getTanggalTagihan()));
					} else {

						final MyDatebox tanggalTagihan = new MyDatebox(tagihan.getTanggalTagihan());
						tanggalTagihan.setFormat(Common.dateFormat11.get().toPattern());
						tanggalTagihan.setDisabled(!((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan()));
						tanggalTagihan.setWidth("90%");
						tanggalTagihan.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
										pengaturanBiayaItemBiaya, true);
								tagihan.setTanggalTagihan(tanggalTagihan.getValue());
								Common.refreshSaveOrUpdate(tagihan);

								eventListenerHitung.onEvent(arg0);
							}
						});
					}
					Double nominalTampil = tagihan.getNominal();
					// Pembayaran 1x: nominal cicilan harus sama dengan total tagihan. Bila hasil hitung 0/kosong
					// (mis. baris tagihan belum terisi nominal), pakai nominal tagihan induk agar tidak tampil 0.
					if ((nominalTampil == null || nominalTampil <= 0.0) && nominalBiaya.getDibayarSebayak() != null
							&& nominalBiaya.getDibayarSebayak() == 1 && !nominalBiaya.getBukanTagihan()
							&& !tagihan.ambilBukanTagihanData() && !tagihan.ambilBukanTagihan()) {
						nominalTampil = nominalBiaya.getNominal();
					}
					final MyDoublebox nominal = new MyDoublebox(nominalTampil);

					String sa = "";
					if (nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Harian")) {
						int tahun = Integer.parseInt((tahunbulan + "").substring(0, 4));
						int bulan = Integer.parseInt((tahunbulan + "").substring(4));
						Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
						cal.set(Calendar.DAY_OF_MONTH, bayarKe);
						cal.set(Calendar.MONTH, bulan - 1);
						cal.set(Calendar.YEAR, tahun);
						sa = Common.dateFormat41.get().format(cal.getTime());
					}

					final MyCheckboxConfig checkbox = new MyCheckboxConfig();
					checkbox.setDisabled(!edit);
					checkbox.setChecked(((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
							&& !tagihan.getNominalBiaya().getBukanTagihan())
							|| (tagihan.getAktifkanmanual() != null && tagihan.getAktifkanmanual()));
					row.setValign("top");
					row.setAttribute("checkbox", checkbox);
					if ((a != null && a.getId() != null) || tagihan.ambilBukanTagihanData()
							|| tagihan.ambilBukanTagihan()) {
						new Label().setParent(row);
					} else {
						checkbox.setParent(row);
					}
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
									nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
									nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
									pengaturanBiayaItemBiaya, true);
							tagihan.setAktifkanmanual(checkbox.isChecked());
							tagihan.setNominalManual(nominal.getValue());
							tagihan.setAktif(checkbox.isChecked());
							tagihan.setNonaktifManual(!checkbox.isChecked()); // true = paksa NON-AKTIF (revisi keliru)
							Common.refreshSaveOrUpdate(tagihan);
							nominal.setDisabled(!tagihan.getAktifkanmanual());

							eventListenerHitung.onEvent(arg0);
						}
					});

					nominal.setDisabled(!((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
							&& !tagihan.getNominalBiaya().getBukanTagihan()) || !edit);

					if ((a != null && a.getId() != null) || tagihan.ambilBukanTagihan()) {
						Vbox vbox = new Vbox();
						vbox.setWidth("95%");
						vbox.setPack("end");
						vbox.setAlign("right");
						vbox.setSpacing("2px"); // Rapikan: beri jarak antar baris nominal/Dibayar/Diskon/tanggal (kosmetik)
						vbox.setParent(row);
						RevisiHelper
								.createNewRevisi(Tagihan.class, tagihan,
										Common.numberFormat.get()
												.format((a.getNominal() + tagihan.getDiskon() + tagihan.getDenda())))
								.setParent(vbox);

						nominal.setValue((a.getNominal() + tagihan.getDiskon() + tagihan.getDenda()));

						RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, a,
								"Dibayar : " + Common.numberFormat.get().format(a.getNominal() + tagihan.getDenda()),
								"font-size:9px;").setParent(vbox);

						if (tagihan.getDiskonSiswa() != null) {
							RevisiHelper.createNewRevisi(DiskonSiswa.class, tagihan.getDiskonSiswa(),
									"Diskon : " + Common.numberFormat.get().format(tagihan.getDiskon()),
									"font-size:9px;").setParent(vbox);
						}

						if (!sa.isEmpty()) {
							vbox.appendChild(new Label(sa));
						}

						if (a != null && a.getId() != null) {
							final Tagihan tag = tagihan;
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pindahkan",
									"/img/stock_data_edit_table.png");
							button.setTooltiptext("Pindah Data");
							button.setDisabled(!edit);
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah Anda yakin ingin memindahkan pembayaran siswa ini? Data pembayaran akan dipindahkan ke lokasi tujuan yang dipilih.",
											"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {

															Tagihan.pindahkan(tag, new EventListener() {

																@Override
																public void onEvent(Event arg0) throws Exception {
																	getThis().onEvent(arg0);
																}
															});

														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);

														}

													}

												}
											});

								}

							});
							button.setParent(vbox);
						}
					} else {

						Vbox vbox = new Vbox();
						vbox.setWidth("100%");
						row.appendChild(vbox);
						vbox.setPack("end");
						vbox.setAlign("right");

						if (tagihan.getKunci() != null || !edit || tagihan.getPengaturanBiaya().getKunci() != null) {
							vbox.appendChild(new Label(Common.numberFormat.get().format(tagihan.getNominal())));
						} else if (!tagihan.ambilBukanTagihanData()) {
							vbox.appendChild(nominal);
						}

						if (tagihan.getDiskonSiswa() != null) {
							RevisiHelper.createNewRevisi(DiskonSiswa.class, tagihan.getDiskonSiswa(),
									"Diskon : " + Common.numberFormat.get().format(tagihan.getDiskon()),
									"font-size:9px;").setParent(vbox);
						}

						if (tagihan.getPembayaranSiswaDetail() == null) {
//							if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() > 1) {
							final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
							bukanTagihana.setChecked(tagihan.ambilBukanTagihanData());
							bukanTagihana.setDisabled(!edit);
							bukanTagihana.setStyle("font-size:7px;");
							if (tagihan.getKunci() == null && tagihan.getPengaturanBiaya().getKunci() == null)
								bukanTagihana.setParent(vbox);
							bukanTagihana.addEventListener("onCheck", new EventListener() {
								@Override
								public void onEvent(final Event event1) throws Exception {

									MyMessageboxConfig.showFormatCb(
											"Apakah Anda yakin item ini {V1} tagihan? Perubahan ini akan memengaruhi status penagihan item tersebut.",
											"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {

														Tagihan tagihan = Tagihan.ambilAtauBuat(
																nominalBiaya.getItemBiayaSekolah(),
																nominalBiaya.getPengaturanBiaya(),
																nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(),
																bayarKe, nominalBiaya, tahunbulan,
																pengaturanBiayaItemBiaya, true);

														tagihan.setBukanTagihan(bukanTagihana.isChecked());
														Common.refreshUpdate(tagihan);

														Common.createDefaultTimer(new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																getThis().onEvent(arg0);
															}
														});
													}
												}
											}, (bukanTagihana.isChecked() ? "bukan" : "adalah"));
								}
							});
//							}
							if (edit) {
								TagihanUtilCalonSiswa.tampilkanKunci(vbox, tagihan, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												getThis().onEvent(arg0);
											}
										});
									}
								}, tbmuser, nominal, approve);
							}

						}
						if (!sa.isEmpty()) {
							vbox.appendChild(new Label(sa));
						}
					}

					nominal.setCols(7);
					nominal.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
									nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
									nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
									pengaturanBiayaItemBiaya, true);

							tagihan.setNominal(nominal.getValue());
							tagihan.setNominalManual(nominal.getValue());

							Common.refreshSaveOrUpdate(tagihan);

							eventListenerHitung.onEvent(arg0);
						}

					});

					row.setValign("top");
					row.setAttribute("nominal", nominal);
					row.setValign("top");
					row.setAttribute("checkbox", checkbox);
				}
				eventListenerHitung.onEvent(null);

			}
		};

		baruEventListener.onEvent(null);

		return grid;

	}

	public static boolean apakahAda(PengaturanBiaya pengaturanBiaya, Siswa siswa) {
		return ((Number) initCriteria(pengaturanBiaya, siswa, new Textbox(), null, false, false)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;
	}

	public Criteria initCriteria(boolean order) {

		PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) (biayaItem
				.getSelectedItem() == null || biayaItem.getSelectedItem().getValue() == null ? null
						: biayaItem.getSelectedItem().getValue());

		return initCriteria(pengaturanBiaya, siswa, nama, pengaturanBiayaItemBiaya, sudahBayar.isChecked(), order);
	}

	public static Criteria initCriteria(PengaturanBiaya pengaturanBiaya, Siswa siswa, Textbox nama,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean sudahBayar, boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, pengaturanBiaya, siswa, nama, pengaturanBiayaItemBiaya, sudahBayar, order);
	}

	@SuppressWarnings("unchecked")
	public static Criteria initCriteria(Session session, PengaturanBiaya pengaturanBiaya, Siswa siswa, Textbox nama,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean sudahBayar, boolean order) {
		String nilaiNama = nama == null ? "" : nama.getValue();
		return initCriteriaDenganNama(session, pengaturanBiaya, siswa, nilaiNama,
				pengaturanBiayaItemBiaya, sudahBayar, order);
	}

	@SuppressWarnings("unchecked")
	public static Criteria initCriteriaDenganNama(Session session, PengaturanBiaya pengaturanBiaya, Siswa siswa,
			String nama, PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean sudahBayar, boolean order) {

		Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
				.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
				.add(pengaturanBiaya.getKelasLesSiswa() != null || pengaturanBiaya.getStatusAwalSiswa() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusAwalSiswa", pengaturanBiaya.getStatusAwalSiswa()))

				.add(pengaturanBiaya.getPenjurusanSekolah() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penjurusanSekolah", pengaturanBiaya.getPenjurusanSekolah()));

		if (pengaturanBiaya.getGelombangPendaftaranPsb() != null) {
			criteria.add(Restrictions.eq("gelombangPendaftaranPsb", pengaturanBiaya.getGelombangPendaftaranPsb()));
		} else if (pengaturanBiaya.getPaketPsb() != null) {
			criteria.add(Restrictions.eq("paketPsb", pengaturanBiaya.getPaketPsb()));
		} else {
			List<Long> gelombangPendaftaranPsbs = session.createCriteria(GelombangPendaftaranPsb.class)
					.setProjection(Projections.property("id"))
					.add(Restrictions.or(
							Restrictions.and(Restrictions.eq("sesuaiKelas", true),
									Restrictions.eq("jenisBiayaSekolah", pengaturanBiaya.getJenisBiayaSekolah())),
							Restrictions.and(Restrictions.eq("sesuaiKelasSaatDiterima", true),
									Restrictions.eq("jenisBiayaSekolahLulus", pengaturanBiaya.getJenisBiayaSekolah()))))
					.list();

			if (!gelombangPendaftaranPsbs.isEmpty()) {
				criteria.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
						Restrictions.in("gelombangPendaftaranPsb.id", gelombangPendaftaranPsbs)));
			}
		}

		if (pengaturanBiaya.getKhususBuatSiswaTertentu()) {

			List<Long> idsiswas = session.createCriteria(PengaturanBiayaPunyaSiswa.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).add(Restrictions.isNotNull("siswa"))
					.setProjection(Projections.groupProperty("siswa.id")).list();

			criteria = criteria
					.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsiswas));
		} else {

			try {
				Integer tahun = Integer.parseInt(StringUtils.split(pengaturanBiaya.getTahunAjaran(), "/")[0].trim());
				criteria.add(Restrictions.or(Restrictions.isNull("tahunLulus"), Restrictions.gt("tahunLulus", tahun)));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1362");
			}

		}

		if (pengaturanBiaya.getTanpaAsrama()) {
			List<Long> idsiswas = session.createCriteria(AsramaSiswaPunyaSiswa.class)
					.add(Restrictions.isNotNull("siswa")).setProjection(Projections.groupProperty("siswa.id")).list();
			criteria = criteria.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.not(Restrictions.in("id", idsiswas)));
		}

		else if (pengaturanBiaya.getAsramaSiswa() != null) {
			List<Long> idsiswas = session.createCriteria(AsramaSiswaPunyaSiswa.class)
					.add(Restrictions.eq("asramaSiswa", pengaturanBiaya.getAsramaSiswa()))
					.add(Restrictions.isNotNull("siswa")).setProjection(Projections.groupProperty("siswa.id")).list();
			criteria = criteria
					.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsiswas));
		}

		if (pengaturanBiaya.getKelasSiswa() != null) {
			List<Long> idsiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", pengaturanBiaya.getKelasSiswa()))
					.add(Restrictions.isNotNull("siswa")).setProjection(Projections.groupProperty("siswa.id")).list();
			criteria = criteria
					.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsiswas));
		} else if (pengaturanBiaya.getKelasBanyak() != null && !pengaturanBiaya.getKelasBanyak().trim().isEmpty()) {
			List<String> namaKelas = new ArrayList<String>();
			for (String kelas : pengaturanBiaya.getKelasBanyak().trim().split(",")) {
				if (!kelas.trim().isEmpty()) {
					namaKelas.add(kelas.trim());
				}
			}
			List<Long> idsiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasSiswa").createAlias("siswa", "siswa")
					.add(Restrictions.eq("siswa.aktif", true)).add(Restrictions.eq("kelasSiswa.aktif", true))
					.add(Restrictions.eq("aktif", true))
					.add(namaKelas.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("kelasSiswa.nama", namaKelas))
					.add(Restrictions.eq("kelasSiswa.tahunAjaran", pengaturanBiaya.getTahunAjaran()))
					.add(Restrictions.isNotNull("siswa")).setProjection(Projections.groupProperty("siswa.id")).list();

			System.out.println("kelas -> " + pengaturanBiaya.getTahunAjaran() + " -> " + idsiswas.size());

			criteria = criteria
					.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsiswas));
		}

		if (pengaturanBiaya.getKelasLesSiswa() != null) {
			List<Long> idsiswas = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasLesSiswa", pengaturanBiaya.getKelasLesSiswa()))
					.add(Restrictions.isNotNull("siswa")).setProjection(Projections.groupProperty("siswa.id")).list();
			criteria = criteria
					.add(idsiswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsiswas));
		}

		criteria = criteria.add(siswa == null ? Restrictions.sqlRestriction("1=1") : Restrictions.idEq(siswa.getId()))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(pengaturanBiaya.getKelasSiswa() != null || pengaturanBiaya.getKhususBuatSiswaTertentu()
						|| pengaturanBiaya.getKelasLesSiswa() != null || pengaturanBiaya.getTahunAngkatan().equals(0)
						|| (pengaturanBiaya.getKelasBanyak() != null
								&& !pengaturanBiaya.getKelasBanyak().trim().isEmpty())
										? Restrictions.sqlRestriction("1=1")
										: pengaturanBiaya.getTahunAngkatan().equals(0)
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunMasuk", pengaturanBiaya.getTahunAngkatan()))

				.add(pengaturanBiaya.getKhususBuatSiswaTertentu() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sekolah", pengaturanBiaya.getSekolah()))

				.add(pengaturanBiaya.getKhususBuatSiswaTertentu() ? Restrictions.sqlRestriction("1=1")
						: pengaturanBiaya.getPenjurusanSekolah() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("penjurusanSekolah", pengaturanBiaya.getPenjurusanSekolah()))

				.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("namaSiswa", nama.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nomorIndukNasional", nama.trim(), MatchMode.ANYWHERE)));

		if (order) {
			criteria.addOrder(Order.asc("namaSiswa")).addOrder(Order.asc("id"));
		}

		if (sudahBayar) {
			List<Long> siswas = session.createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
					.setProjection(Projections.groupProperty("siswa.id")).add(Restrictions.isNotNull("siswa"))
					.add(pengaturanBiayaItemBiaya == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).list();
			System.out.println("Sudah bayar -> " + siswas);
			criteria.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", siswas));

		}

		return criteria;
	}

	public void loadData(Object value) {
		if (grid == null) {
			return;
		}

		Comboitem comboitem = bulans == null ? null : bulans.getSelectedItem();
		Integer pembayaranTerakhir = null;
		if (comboitem != null) {
			pembayaranTerakhir = PembayaranSiswa.convert((Integer) comboitem.getAttribute("tahun"),
					((Integer) comboitem.getAttribute("bulan")) + 1);
		}
		List<Siswa> siswa = ConstantValues.simpleList(initCriteria(true), Siswa.class);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer(pengaturanBiayaItemBiaya, pembayaranTerakhir));
		grid.setModelCheckMobile(strset);

	}

	final String[] contents = new String[] { "id", "nomorInduk", "namaSiswa", "tahunMasuk", "sekolah.nama", "namaAyah",
			"namaIbu" };
	private Combobox mybulansMulai = null;
	private Combobox mybulansSampai = null;
	protected Integer mul = null;
	protected Integer sam = null;
	private Columns columns;
	private Auxhead auxhead;
	private Combobox biayaItem;
	private PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = null;
	private Combobox bulans;
	private MyCheckboxConfig sudahBayar;

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final PengaturanBiaya pengaturanBiaya, final Component component) {

		this.pengaturanBiaya = pengaturanBiaya;
		Session session = HibernateUtil.currentSession();
		pengaturanBiayaItemBiayas = ConstantValues.simpleList(session.createCriteria(PengaturanBiayaItemBiaya.class)
				.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.add(Restrictions.eq("itemBiayaSekolah.aktif", true)).addOrder(Order.asc("itemBiayaSekolah.nama"))
				.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)), PengaturanBiayaItemBiaya.class);

		PengaturanBiaya.reloadTagihan(pengaturanBiaya);
		Common.clear(component);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadGrid((PengaturanBiayaItemBiaya) (biayaItem.getSelectedItem() == null
						|| biayaItem.getSelectedItem().getValue() == null ? null
								: biayaItem.getSelectedItem().getValue()));

			}
		};

		Vlayout vlayout = new Vlayout();
		vlayout.setStyle("min-height: 300px; width:100%; max-width:100%; box-sizing:border-box;");
		vlayout.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(vlayout);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		nama = new Textbox();
		if (siswa == null) {
			toolbar.appendChild(nama);
		} else {
			toolbar.appendChild(new Label(siswa.getNama()));
		}
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Sinkronkan", "/img/Configure.png");
		buttonTagihan.setTooltiptext("Sinkronkan ulang data siswa dan tagihan pembayaran dari database");
		buttonTagihan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// 1. OPTIMASI FATAL: Baca semua komponen UI di Main Thread!
				// Jangan pernah membaca getSelectedItem() di dalam background Thread ZKoss.
				Comboitem comboitem = bulans == null ? null : bulans.getSelectedItem();
				Integer tempPembayaranTerakhir = null;
				if (comboitem != null) {
					Integer tahun = (Integer) comboitem.getAttribute("tahun");
					Integer bulan = (Integer) comboitem.getAttribute("bulan");
					if (tahun != null && bulan != null) {
						tempPembayaranTerakhir = PembayaranSiswa.convert(tahun, bulan + 1);
					}
				}
				final Integer pembayaranTerakhir = tempPembayaranTerakhir;

				final PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (biayaItem == null
						|| biayaItem.getSelectedItem() == null || biayaItem.getSelectedItem().getValue() == null) ? null
								: (PengaturanBiayaItemBiaya) biayaItem.getSelectedItem().getValue();

				// Simpan isian filter sebelum display() membangun ulang UI (agar tidak hilang)
				final String namaBefore = nama == null ? "" : nama.getValue();

				// 2. Tampilkan Loading Bar
				final Label label = Common.displayLoadBar(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								PengaturanBiaya.reloadTagihan(pengaturanBiaya, true);
								display(pengaturanBiaya, component);
								// Kembalikan isian filter yang tersimpan
								if (nama != null && namaBefore != null && !namaBefore.isEmpty()) {
									nama.setValue(namaBefore);
								}
							}
						});
					}
				});

				// 3. Jalankan proses Sinkronisasi di Background Thread
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							// Gunakan variabel final yang sudah diekstrak di atas
							TagihanUtil.doSinkronkanTagihanSiswa(pengaturanBiaya, pengaturanBiayaItemBiaya,
									pembayaranTerakhir, label, namaBefore, true);

							Thread.sleep(1000);

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						} finally {
							// 4. OPTIMASI: Pastikan label loading SELALU di-clear,
							// meskipun terjadi error/crash saat proses sinkronisasi.
							try {
								label.setValue("");
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1616");
							}
						}
					}
				}).start();
			}

		});
		buttonTagihan.setParent(toolbar);

		MyToolbarbuttonConfig buttonRecovery = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
		buttonRecovery.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// 1. Tampilkan Dialog Konfirmasi terlebih dahulu
				MyMessageboxConfig.show(
						"Apakah Anda yakin ingin melakukan pemulihan (recovery) data dari tabel audit? Tindakan ini akan memulihkan data yang sebelumnya terhapus.",
						"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								// 2. Cek apakah pengguna mengklik tombol "YES"
								int response = Integer.parseInt(event.getData().toString());
								if (response == MyMessageboxConfig.OK) {

									// =========================================================================
									// 3. LOGIKA RECOVERY DIMULAI DI SINI (Hanya tereksekusi jika dijawab YES)
									// =========================================================================

									final List<String> warnings = java.util.Collections
											.synchronizedList(new ArrayList<String>());

									final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent()
											.getDesktop();
									if (!desktop.isServerPushEnabled()) {
										desktop.enableServerPush(true);
									}
									final String namaSiswa = nama == null ? "" : nama.getValue().trim();
									final Label label = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {

													if (!warnings.isEmpty()) {
														StringBuilder sb = new StringBuilder();
														synchronized (warnings) {
															for (String w : warnings) {
																sb.append(w).append("\n");
															}
														}
														MyMessageboxConfig.show(sb.toString(), "Peringatan",
																MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
														return;
													}

													PengaturanBiaya.reloadTagihan(pengaturanBiaya, true);
													display(pengaturanBiaya, component);
												}
											});
										}
									});

									new Thread(new Runnable() {
										@Override
										public void run() {
											try {
												// Panggil fungsi dan kirimkan pendengar progress (ProgressListener)
												DataRecoveryHelper.restoreDeletedDataFromAudit(pengaturanBiaya,
														namaSiswa, warnings, new ProgressListener() {
															@Override
															public void onProgress(final int percent,
																	final String message) {

																// Update Label UI (Harus dijembatani dengan
																// Executions.schedule)
																try {
																	org.zkoss.zk.ui.Executions.schedule(desktop,
																			new EventListener() {
																				@Override
																				public void onEvent(Event event)
																						throws Exception {
																					if (label != null) {
																						// Contoh Tampilan: "Loading...
																						// 50% (Mengembalikan Header
																						// Pembayaran...)"
																						label.setValue("Loading... "
																								+ percent + "% ("
																								+ message + ")");
																					}
																				}
																			}, null);
																} catch (Exception e) {
																	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1713");
																}
															}
														});

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1719");
											} finally {
												try {
													org.zkoss.zk.ui.Executions.schedule(desktop, new EventListener() {
														@Override
														public void onEvent(Event event) throws Exception {
															if (label != null) {
																label.setValue(""); // Bersihkan loading bar di akhir
															}
														}
													}, null);
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1731");
												}
											}
										}
									}).start();
									// =========================================================================
									// AKHIR LOGIKA RECOVERY
									// =========================================================================
								}
							}
						});
			}

		});

		buttonRecovery.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/sekolah/tagihan.zul?pengaturanBiaya=" + pengaturanBiaya.getId(),
						true, "95%", Common.isMobile() ? "100%" : "1250px", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, "", false);
			}

		});
		button.setParent(toolbar);

		if (pengaturanBiaya.getKhususBuatSiswaTertentu()) {
			button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataSiswaForPengaturanBiayaHelper dataSiswaHelper = new AmbilDataSiswaForPengaturanBiayaHelper(
							pengaturanBiaya);
					dataSiswaHelper.display(getDataloader());
				}

			});
			button.setParent(toolbar);
		}

		bulans = null;
		if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Harian")
				&& pengaturanBiaya.getBulanMulai() != null) {
			int tahunMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(0, 4));
			int bulanMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(4));

			// FIX NumberFormatException "For input string: null" (KE-2): getBulanMulai() sudah dijaga
			// null di baris pengecekan atas (&& pengaturanBiaya.getBulanMulai() != null), tapi
			// getBulanSampai() TIDAK -- bila null, (null+"") jadi literal "null" & parseInt meledak.
			// Bila belum diisi, anggap bulan akhir = bulan mulai (rentang satu bulan), bukan crash.
			String bulanSampaiStr = pengaturanBiaya.getBulanSampai() == null ? (pengaturanBiaya.getBulanMulai() + "")
					: (pengaturanBiaya.getBulanSampai() + "");
			int tahunSampai = Integer.parseInt(bulanSampaiStr.substring(0, 4));
			int bulanSampai = Integer.parseInt(bulanSampaiStr.substring(4));

			Calendar calD = ais.ui.util.WaktuUtil.getCalendar();

			int bulanTahunSekarang = PembayaranSiswa.convert(calD.get(Calendar.YEAR), calD.get(Calendar.MONTH));
			int bulanTahunAkhir = PembayaranSiswa.convert(tahunSampai, bulanSampai);

			List<String> columnHeadersAdding = new ArrayList<String>();
			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.set(Calendar.DATE, 1);
				cal.set(Calendar.MONTH, bulanMulai - 1);
				cal.set(Calendar.YEAR, tahunMulai);
				Integer pembayaranTerakhir = 0;
				while (bulanTahunAkhir > pembayaranTerakhir) {
					int tahunCurrent = cal.get(Calendar.YEAR);
					int bulanCurrent = cal.get(Calendar.MONTH);
					int bulanCurrentPlus = bulanCurrent + 1;
					pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

					if (pengaturanBiaya.getBulanMulai() != null
							&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai()) {
						cal.add(Calendar.MONTH, 1);
						continue;
					}
					if (pengaturanBiaya.getBulanSampai() != null
							&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai()) {
						break;
					}

					for (int bayarKe = 1; bayarKe <= 31; bayarKe++) {
						columnHeadersAdding
								.add(pembayaranTerakhir + "" + bayarKe + "-" + pengaturanBiayaItemBiaya.getId() + "-"
										+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());
						columnHeadersAdding.add("Bukan Tagihan");
					}

					cal.add(Calendar.MONTH, 1);

				}
			}

			Integer pembayaranTerakhir = 0;
			int sekarang = 0;
			Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.MONTH, bulanMulai - 1);
			cal.set(Calendar.YEAR, tahunMulai);
			bulans = new Combobox();
			while (bulanTahunAkhir > pembayaranTerakhir) {
				int tahunCurrent = cal.get(Calendar.YEAR);
				int bulanCurrent = cal.get(Calendar.MONTH);
				int bulanCurrentPlus = bulanCurrent + 1;
				pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

				if (pengaturanBiaya.getBulanMulai() != null && pembayaranTerakhir < pengaturanBiaya.getBulanMulai()) {
					cal.add(Calendar.MONTH, 1);
					continue;
				}
				if (pengaturanBiaya.getBulanSampai() != null && pembayaranTerakhir > pengaturanBiaya.getBulanSampai()) {
					break;
				}

				if (bulanTahunSekarang >= pembayaranTerakhir) {
					sekarang++;
				}

				Comboitem comboitem = new Comboitem();
				comboitem.setLabel(Common.BULAN[bulanCurrent] + " " + tahunCurrent);
				comboitem.setValue(pembayaranTerakhir);
				comboitem.setAttribute("bulan", bulanCurrent);
				comboitem.setAttribute("tahun", tahunCurrent);
				bulans.appendChild(comboitem);

				cal.add(Calendar.MONTH, 1);
			}

			bulans.setReadonly(true);
			// Guard out-of-bound: 'bulans' bisa kosong / 'sekarang' bisa >= jumlah item.
			if (sekarang >= 0 && sekarang < bulans.getItemCount()) {
				bulans.setSelectedIndex(sekarang);
			}
			bulans.addEventListener("onChange", eventListener);

			toolbar.appendChild(bulans);

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Siswa siswa = (Siswa) objects[0];

					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setUnderline(XSSFFont.U_SINGLE);
					hlink_font.setColor(new XSSFColor(Color.BLUE));

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
					hlink_style.setFont(hlink_font);

					final XSSFCellStyle aahlink = workbook.createCellStyle();
					aahlink.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					aahlink.setFillForegroundColor(new XSSFColor(Color.RED));
					aahlink.setFont(hlink_font);

					Comboitem comboitem = (Comboitem) bulans.getSelectedItem();
					Integer tahunCurrent = (Integer) comboitem.getAttribute("tahun");
					Integer bulanCurrent = (Integer) comboitem.getAttribute("bulan");

					Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
					cal.set(Calendar.DAY_OF_MONTH, 1);
					cal.set(Calendar.MONTH, bulanCurrent);
					cal.set(Calendar.YEAR, tahunCurrent);

					List<Integer> dates = new ArrayList<Integer>();
					while (bulanCurrent == cal.get(Calendar.MONTH)) {
						dates.add(cal.get(Calendar.DAY_OF_MONTH));
						cal.add(Calendar.DAY_OF_MONTH, 1);
					}

					System.out.println("dates -> " + dates);

					Integer pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrent + 1);

					XSSFRow row = (XSSFRow) objects[2];
					Session session = HibernateUtil.currentNativeSession();
					int index = 0;
					for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

						try {

							NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
									pembayaranTerakhir, session);

							for (int bayarKe = 1; bayarKe <= 31; bayarKe++) {
								Tagihan tagihan = null;
								String kodeUnik = null;
								boolean ada = dates.contains(bayarKe);
								if (ada) {
									kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
											nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir,
											nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

									tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);
									if (tagihan != null) {
										session.refresh(tagihan);
										session.getTransaction().begin();
										Common.refreshUpdate(session, tagihan);
										session.getTransaction().commit();
									}
								}

								System.out.println("reload tagihan -> " + tagihan + ", kodeUnik => " + kodeUnik);

								XSSFCell cell = row.createCell(contents.length + index);
								cell.setCellValue(tagihan == null ? 0.0 : (tagihan.getNominal() + tagihan.getDenda()));
								cell.setCellStyle(!ada ? aahlink : hlink_style);

								index++;

								cell = row.createCell(contents.length + index);
								cell.setCellValue(tagihan == null ? false : tagihan.ambilBukanTagihanData());
								cell.setCellStyle(!ada ? aahlink : hlink_style);
								index++;

							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:1961");
						}

					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Siswa.class, this, "Download",
					"/img/print.png", columnHeadersAdding, dataAdding, false, null, null, contents);
			toolbar.appendChild(cetakToolbarbutton);

		}

		else if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")
				&& pengaturanBiaya.getBulanMulai() != null) {
			List<String> columnHeadersAdding = new ArrayList<String>();

			int tahunMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(0, 4));
			int bulanMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(4));

			// FIX NumberFormatException "For input string: null" (KE-2): getBulanMulai() sudah dijaga
			// null di baris pengecekan atas (&& pengaturanBiaya.getBulanMulai() != null), tapi
			// getBulanSampai() TIDAK -- bila null, (null+"") jadi literal "null" & parseInt meledak.
			// Bila belum diisi, anggap bulan akhir = bulan mulai (rentang satu bulan), bukan crash.
			String bulanSampaiStr = pengaturanBiaya.getBulanSampai() == null ? (pengaturanBiaya.getBulanMulai() + "")
					: (pengaturanBiaya.getBulanSampai() + "");
			int tahunSampai = Integer.parseInt(bulanSampaiStr.substring(0, 4));
			int bulanSampai = Integer.parseInt(bulanSampaiStr.substring(4));

			final int bulanTahunAkhir = PembayaranSiswa.convert(tahunSampai, bulanSampai);

			final TreeSet<Integer> bulans = new TreeSet<Integer>();

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.set(Calendar.DATE, 1);
				cal.set(Calendar.MONTH, bulanMulai - 1);
				cal.set(Calendar.YEAR, tahunMulai);

				Integer pembayaranTerakhir = 0;
				while (bulanTahunAkhir > pembayaranTerakhir) {
					int tahunCurrent = cal.get(Calendar.YEAR);
					int bulanCurrent = cal.get(Calendar.MONTH);
					int bulanCurrentPlus = bulanCurrent + 1;
					pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

					if (pengaturanBiaya.getBulanMulai() != null
							&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai()) {
						cal.add(Calendar.MONTH, 1);
						continue;
					}
					if (pengaturanBiaya.getBulanSampai() != null
							&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai()) {
						break;
					}

					columnHeadersAdding.add(pembayaranTerakhir + "-" + pengaturanBiayaItemBiaya.getId() + "-"
							+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());
					columnHeadersAdding.add("Bukan Tagihan");
					cal.add(Calendar.MONTH, 1);

					bulans.add(pembayaranTerakhir);
				}

			}

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Siswa siswa = (Siswa) objects[0];

					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setUnderline(XSSFFont.U_SINGLE);
					hlink_font.setColor(new XSSFColor(Color.BLUE));

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
					hlink_style.setFont(hlink_font);

					XSSFRow row = (XSSFRow) objects[2];
					Session session = HibernateUtil.currentNativeSession();
					int index = 0;
					for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

						try {

							NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
									session);

							for (Integer pembayaranTerakhir : bulans) {

								int bayarKe = 1;
								String kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir, nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), bayarKe);

								Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

								if (tagihan != null) {
									session.refresh(tagihan);
									session.getTransaction().begin();
									Common.refreshUpdate(session, tagihan);
									session.getTransaction().commit();
								}

								XSSFCell cell = row.createCell(contents.length + index);
								cell.setCellValue(tagihan == null ? 0.0 : (tagihan.getNominal() + tagihan.getDenda()));
								cell.setCellStyle(hlink_style);

								index++;

								cell = row.createCell(contents.length + index);
								cell.setCellValue(tagihan == null ? false : tagihan.ambilBukanTagihanData());
								cell.setCellStyle(hlink_style);
								index++;

							}

							if (!nominalBiaya.getItemBiayaSekolah().getAngsuranSeragam()) {
								if (!pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equals("Bulanan")) {
									Number maks = (Number) session.createCriteria(Tagihan.class)
											.add(Restrictions.eq("nominalBiaya", nominalBiaya))
											.setProjection(Projections.rowCount()).add(Restrictions.gt("nominal", 0.1))
											.uniqueResult();

									if (nominalBiaya.getDibayarSebayak()
											.intValue() != (maks == null ? 1 : maks.intValue())) {
										nominalBiaya.setDibayarSebayak((maks == null ? 1 : maks.intValue()));
										session.getTransaction().begin();
										Common.refreshUpdate(session, nominalBiaya);
										session.getTransaction().commit();
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2102");
						}

					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				}
			};

			mybulansMulai = new Combobox();
			mybulansSampai = new Combobox();

			for (Integer bul : bulans) {

				int tahun = Integer.parseInt((bul + "").substring(0, 4));
				int bulan = Integer.parseInt((bul + "").substring(4));
				if (bulan > 12 || bulan < 1) {
					continue;
				}

				Comboitem comboitem = new Comboitem(tahun + "-" + bulan);
				comboitem.setValue(bul);
				mybulansMulai.appendChild(comboitem);

				comboitem = new Comboitem(tahun + "-" + bulan);
				comboitem.setValue(bul);
				mybulansSampai.appendChild(comboitem);
			}

			mybulansMulai.setReadonly(true);
			mybulansSampai.setReadonly(true);

			if (!bulans.isEmpty()) {
				Common.selectComboItem(true, mybulansMulai, bulans.first());
				Common.selectComboItem(true, mybulansSampai, bulans.last());
			}

			toolbar.appendChild(new Space());
			toolbar.appendChild(mybulansMulai);
			mybulansMulai.setCols(3);
			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
			toolbar.appendChild(mybulansSampai);
			mybulansSampai.setCols(3);

			EventListener bulanEvents = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mul = (Integer) mybulansMulai.getSelectedItem().getValue();
					sam = (Integer) mybulansSampai.getSelectedItem().getValue();

					Common.clear(auxhead);
					Common.clear(columns);
					Common.createDefaultTimer(eventListener);
				}
			};

			mybulansMulai.addEventListener("onChange", bulanEvents);
			mybulansSampai.addEventListener("onChange", bulanEvents);
			try {
				mul = (Integer) mybulansMulai.getSelectedItem().getValue();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2167");
				// TODO: handle exception
			}
			try {
				sam = (Integer) mybulansSampai.getSelectedItem().getValue();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2172");
				// TODO: handle exception
			}

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Siswa.class, this, "Download",
					"/img/print.png", columnHeadersAdding, dataAdding, false, null, null, contents);
			toolbar.appendChild(cetakToolbarbutton);

		} else {
			List<String> columnHeadersAdding = new ArrayList<String>();

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {
				columnHeadersAdding.add(pengaturanBiayaItemBiaya.getId() + "-"
						+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());
				columnHeadersAdding.add("Dibayar sebanyak (kali)");
				columnHeadersAdding.add("Bukan Tagihan");
			}

			System.out.println("columnHeadersAdding => " + columnHeadersAdding);

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Siswa siswa = (Siswa) objects[0];

					try {
						XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

						XSSFFont hlink_font = workbook.createFont();
						hlink_font.setUnderline(XSSFFont.U_SINGLE);
						hlink_font.setColor(new XSSFColor(Color.BLUE));

						XSSFCellStyle hlink_style = workbook.createCellStyle();
						hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
						hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
						hlink_style.setFont(hlink_font);

						XSSFRow row = (XSSFRow) objects[2];
						Session session = HibernateUtil.currentNativeSession();
						int index = 0;
						for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

							NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
									session);

							XSSFCell cell = row.createCell(contents.length + index);

							cell.setCellValue(nominalBiaya.getNominal());
							cell.setCellStyle(hlink_style);

							index++;

							cell = row.createCell(contents.length + index);
							cell.setCellValue(nominalBiaya.getDibayarSebayak());
							cell.setCellStyle(hlink_style);
							index++;

							cell = row.createCell(contents.length + index);
							cell.setCellValue(nominalBiaya.getBukanTagihan());
							cell.setCellStyle(hlink_style);
							index++;
						}

						// session.disconnect();
						if (session.isOpen()) {
							session.disconnect();
							session.close();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2242");
						// TODO: handle exception
					}

					HibernateUtil.closeSession();
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Siswa.class, this, "Download",
					"/img/print.png", columnHeadersAdding, dataAdding, false, null, null, contents);
			toolbar.appendChild(cetakToolbarbutton);

		}

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " +
					// file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataSiswa(file, pengaturanBiaya, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.showFormat(
							"File yang Anda unggah harus berformat Excel Open XML Spreadsheet (xlsx). Berkas yang terdeteksi: {V1}. Langkah yang dapat dilakukan: (1) buka file tersebut menggunakan aplikasi Excel; (2) pilih menu Save As lalu simpan dengan format Excel Open XML Spreadsheet (xlsx); (3) unggah kembali file yang telah disimpan.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
				}
			}
		});
		toolbar.appendChild(upload);

		sudahBayar = new MyCheckboxConfig("Hanya yang sudah bayar");
		toolbar.appendChild(sudahBayar);
		sudahBayar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});

		button = new MyToolbarbuttonConfig("Rekap", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Comboitem comboitem = bulans == null ? null : bulans.getSelectedItem();
				Integer pembayaranTerakhir = null;
				if (comboitem != null) {
					pembayaranTerakhir = PembayaranSiswa.convert((Integer) comboitem.getAttribute("tahun"),
							((Integer) comboitem.getAttribute("bulan")) + 1);
				}

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				XSSFSheet sheet = workbook.createSheet("REKAP");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIS");
				rowhead.createCell(1).setCellValue("Nama Siswa");
				rowhead.createCell(2).setCellValue("Tahun Masuk");
				rowhead.createCell(3).setCellValue("Sekolah");
				rowhead.createCell(4).setCellValue("Nilai Tagihan");
				rowhead.createCell(5).setCellValue("Dibayar Sebanyak");
				rowhead.createCell(6).setCellValue("Cicilan I");
				rowhead.createCell(7).setCellValue("Cicilan II");
				rowhead.createCell(8).setCellValue("Cicilan III");
				rowhead.createCell(9).setCellValue("Cicilan IV");
				rowhead.createCell(10).setCellValue("Cicilan V");
				rowhead.createCell(11).setCellValue("Cicilan VI");
				rowhead.createCell(12).setCellValue("Cicilan VII");
				rowhead.createCell(13).setCellValue("Cicilan VIII");
				rowhead.createCell(14).setCellValue("Cicilan IX");
				rowhead.createCell(15).setCellValue("Cicilan X");
				rowhead.createCell(16).setCellValue("Cicilan XI");
				rowhead.createCell(17).setCellValue("Cicilan XII");
				rowhead.createCell(18).setCellValue("Cicilan XIII");
				rowhead.createCell(19).setCellValue("Cicilan XIV");
				rowhead.createCell(20).setCellValue("Cicilan XV");

				rowhead.createCell(21).setCellValue("TOTAL TAGIHAN");
				rowhead.createCell(22).setCellValue("TOTAL DISKON");
				rowhead.createCell(23).setCellValue("TOTAL DENDA");
				rowhead.createCell(24).setCellValue("SISA BELUM SESUAI");

				List<Siswa> siswas = ConstantValues.simpleList(initCriteria(true), Siswa.class);
				int rowIndex = 1;
				Session session = HibernateUtil.currentNativeSession();
				for (Siswa siswa : siswas) {
					XSSFRow row = sheet.createRow(rowIndex);

					XSSFCell cell = row.createCell(0);
					cell.setCellValue(siswa.getNomorInduk());

					cell = row.createCell(1);
					cell.setCellValue(siswa.getNamaSiswa());

					cell = row.createCell(2);
					cell.setCellValue(siswa.getTahunMasuk() + "");

					cell = row.createCell(3);
					cell.setCellValue(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama());

					NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
							pembayaranTerakhir, session);

					cell = row.createCell(4);
					cell.setCellValue(nominalBiaya.getNominal());

					cell = row.createCell(5);
					cell.setCellValue(nominalBiaya.getDibayarSebayak());

					Double total = 0.0;
					Double totalDiskon = 0.0;
					Double totalDenda = 0.0;
					for (int bayarKe = 1; bayarKe <= nominalBiaya.getDibayarSebayak(); bayarKe++) {

						String kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
								nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir, nominalBiaya.getSiswa(),
								nominalBiaya.getCalonSiswa(), bayarKe);

						Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

						Double d = tagihan == null || !((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan()) ? 0.0 : tagihan.getNominal();
						Double diskon = tagihan == null || !((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan()) ? 0.0 : tagihan.getDiskon();
						Double denda = tagihan == null || !((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
								&& !tagihan.getNominalBiaya().getBukanTagihan()) ? 0.0 : tagihan.getDenda();

						cell = row.createCell(5 + bayarKe);
						cell.setCellValue((d < 0.0 ? "**RED" : "") + Common.numberFormat.get().format(d));

						total += d;
						totalDiskon += diskon;
						totalDenda += denda;
					}
					cell = row.createCell(21);
					cell.setCellValue("**" + Common.numberFormat.get().format(total));
					cell.setCellStyle(hlink_style);

					cell = row.createCell(22);
					cell.setCellValue("**" + Common.numberFormat.get().format(totalDiskon));
					cell.setCellStyle(hlink_style);

					cell = row.createCell(23);
					cell.setCellValue("**" + Common.numberFormat.get().format(totalDenda));
					cell.setCellStyle(hlink_style);

					cell = row.createCell(24);
					cell.setCellValue("**" + Common.numberFormat.get()
							.format((nominalBiaya.getNominal() + totalDenda) - (total + totalDiskon)));
					cell.setCellStyle(hlink_style);

					rowIndex++;
				}

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

				Common.setStyled(sheet);
				String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/rekap_tagihan_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				try {
					Filedownload.save(new FileInputStream(filename),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "rekap_tagihan.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2471");

				}
			}

		});
		button.setParent(toolbar);

		biayaItem = new Combobox();
		for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {
			Comboitem comboitem = new Comboitem(pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());
			comboitem.setValue(pengaturanBiayaItemBiaya);
			biayaItem.appendChild(comboitem);
		}
		toolbar.appendChild(biayaItem);
		biayaItem.setCols(10);
		// "Out of bound: 0 while size=0" bila combobox KOSONG (pengaturanBiayaItemBiayas kosong)
		// -> pilih indeks 0 hanya bila ada item.
		if (biayaItem.getItemCount() > 0) {
			biayaItem.setSelectedIndex(0);
		}
		biayaItem.setReadonly(true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.getPagingChild().setMold("os");
		/*
		 * Pembungkus scroll horizontal. Pada reloadGrid() lebar grid diset piksel
		 * tetap (320 + jumlah kolom pembayaran * lebar) sehingga bisa lebih lebar
		 * dari sel detail -> sebelumnya konten kanan (tombol Bayar, Kunci, Total)
		 * TERPOTONG karena tidak ada scroll. Div ini membuat grid bisa di-scroll
		 * horizontal di dalam baris detail, jadi seluruh kolom tetap terjangkau.
		 */
		org.zkoss.zul.Div gridScroll = new org.zkoss.zul.Div();
		gridScroll.setStyle("width:100%; max-width:100%; overflow-x:auto; box-sizing:border-box;");
		gridScroll.setParent(vlayout);
		grid.setParent(gridScroll);
		grid.setSclass("dgrid");

		columns = new Columns();

		auxhead = new Auxhead();

		auxhead.setParent(grid);
		columns.setParent(grid);

		biayaItem.addEventListener("onChange", eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2524");
		}
	}

	@SuppressWarnings("deprecation")
	public void reloadGrid(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya) {

		Common.clear(columns);
		Common.clear(auxhead);

		this.pengaturanBiayaItemBiaya = pengaturanBiayaItemBiaya;
		if (pengaturanBiayaItemBiaya == null) {
			return;
		}

		if (mul != null && sam != null) {
			Auxheader auxheader = new Auxheader("");
			auxheader.setColspan(1);
			auxheader.setParent(auxhead);
		}

		Column column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Siswa");
		column.setStyle("min-width: 300px;");

		int size = 1;

		if (mul != null && sam != null) {
			int banyak = 0;
			for (int m = mul; m <= sam; m++) {
				banyak++;
			}
			grid.setWidth((320 + ((size * banyak) * 130)) + "px");
		} else {
			grid.setWidth((320 + (size * 300)) + "px");
		}
		ais.ui.util.ZkCompat.setFixedLayout(grid, false);

		if (mul != null && sam != null) {

			Auxheader auxheader = new Auxheader(pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());

			auxheader.setParent(auxhead);
			int jumlahSpan = 0;
			for (int m = mul; m <= sam; m++) {
				if ((pengaturanBiaya.getBulanMulai() != null && m < pengaturanBiaya.getBulanMulai())
						|| (pengaturanBiaya.getBulanSampai() != null && m > pengaturanBiaya.getBulanSampai())) {
					continue;
				}

				int tahun = Integer.parseInt((m + "").substring(0, 4));
				final int bulan = Integer.parseInt((m + "").substring(4));
				if (bulan > 12 || bulan < 1) {
					continue;
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, 1);
				calendar.set(Calendar.MONTH, bulan - 1);
				calendar.set(Calendar.YEAR, tahun);
				if (!pengaturanBiaya.getTanggalTagihanMengikutiBulanBerjalan()) {

					if (bulan == 1) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan1());
					} else if (bulan == 2) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan2());
					} else if (bulan == 3) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan3());
					} else if (bulan == 4) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan4());
					} else if (bulan == 5) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan5());
					} else if (bulan == 6) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan6());
					} else if (bulan == 7) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan7());
					} else if (bulan == 8) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan8());
					} else if (bulan == 9) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan9());
					} else if (bulan == 10) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan10());
					} else if (bulan == 11) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan11());
					} else if (bulan == 12) {
						calendar.setTime(pengaturanBiaya.getTanggalTagihanBulan12());
					}
				}

				MyLabelAgakKecil label = new MyLabelAgakKecil(
						tahun + "-" + bulan + "\n" + Common.dateFormat11.get().format(calendar.getTime()));
				label.setMultiline(true);

				column = new Column();
				column.setParent(columns);
				column.appendChild(label);
				column.setWidth("130px");
				jumlahSpan++;
			}
			auxheader.setColspan(jumlahSpan == 0 ? 1 : jumlahSpan);
		} else {

			MyLabelAgakKecil label = new MyLabelAgakKecil(pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama()
					+ "\n" + Common.dateFormat11.get().format(pengaturanBiaya.getTanggalTagihan()));
			label.setMultiline(true);

			column = new Column();
			column.setParent(columns);
			column.appendChild(label);

			column.setWidth("300px");
		}

		loadData(null);
	}

	public void uploadDataSiswa(final File file, final PengaturanBiaya pengaturanBiaya,
			final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");
		final Label downloadPath = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try {
							Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) DetailTagihanSiswaHelper download laporan"); }
					}
					MyMessageboxConfig.showFormatCb(
							"Upload data siswa selesai.{V1}",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
							(peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()));
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final Integer pembayaranTerakhir;
		final List<Integer> dates = new ArrayList<Integer>();
		if (bulans != null) {
			Comboitem comboitem = (Comboitem) bulans.getSelectedItem();
			Integer tahunCurrent = (Integer) comboitem.getAttribute("tahun");
			Integer bulanCurrent = (Integer) comboitem.getAttribute("bulan");

			Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.MONTH, bulanCurrent);
			cal.set(Calendar.YEAR, tahunCurrent);

			while (bulanCurrent == cal.get(Calendar.MONTH)) {
				dates.add(cal.get(Calendar.DAY_OF_MONTH));
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}

			System.out.println("dates -> " + dates);

			pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrent + 1);
		} else {
			pembayaranTerakhir = null;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = null;
				ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Tagihan Siswa");
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Map<Long, TreeSet<Integer>> bulans = new HashMap<Long, TreeSet<Integer>>();

					int col = 0;
					while (true) {
						try {
							String header = Common.getSheetContentAsString(sheet, col, 0);
							if (header == null || header.isEmpty()) {
								break;
							}
							if (header.contains("-")) {
								String[] a = header.split("-");
								Long idpengaturanBiayaItemBiaya = Long.parseLong(a[1]);
								Integer bulanDanTahun = Integer.parseInt(a[0]);

								TreeSet<Integer> integers = bulans.get(idpengaturanBiayaItemBiaya);
								if (integers == null) {
									integers = new TreeSet<Integer>();
									bulans.put(idpengaturanBiayaItemBiaya, integers);
								}
								integers.add(bulanDanTahun);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2727");
						}
						col++;
					}

					System.out.println("bulans -> " + bulans);

					// Thread latar: pakai openSession DEDIKASI (bukan currentNativeSession yang bisa
					// ditutup helper lain via HibernateUtil.closeSession() di tengah loop → "Session is
					// closed!" saat ambilNominalBiaya.createCriteria). Ditutup di finally.
					session = HibernateUtil.getSessionFactory().openSession();

					// Pre-load: satu bulk query untuk semua Tagihan milik pengaturanBiaya ini.
					// Tanpa ini tiap baris Excel + tiap bulan = 1 query lookup → N×M round-trip.
					// Dengan pre-load: 1 query di awal, sisanya Map.get() O(1).
					@SuppressWarnings("unchecked")
					final Map<String, Tagihan> tagihanPreload = new java.util.HashMap<String, Tagihan>();
					try {
						java.util.List<Tagihan> allTagihanPb = session.createCriteria(Tagihan.class)
								.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
								.list();
						for (Tagihan t : allTagihanPb) {
							if (t.getKodeUnik() != null && !t.getKodeUnik().isEmpty()) {
								tagihanPreload.put(t.getKodeUnik(), t);
							}
						}
					} catch (Exception ePreload) {
						ePreload.printStackTrace(); ais.common.ErrorAuditUtil.record(ePreload, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:2754"); // opsional — proses tetap jalan meski pre-load gagal
					}

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i, Siswa.class);
							String nomorInduk = Common.getSheetContentAsString(sheet, 1, i);
							if (siswa == null && nomorInduk != null && !nomorInduk.trim().isEmpty()) {
								siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
										.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
										.add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.eq("nomorInduk", nomorInduk))
										.add(Restrictions.eq("sekolah", pengaturanBiaya.getSekolah())).setMaxResults(1)
										.addOrder(Order.desc("id")), Siswa.class);
							}

							String nama = Common.getSheetContentAsString(sheet, 2, i);
							if (siswa == null && nama != null && !nama.trim().isEmpty()) {
								siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
										.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
										.add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT))
										.add(Restrictions.eq("sekolah", pengaturanBiaya.getSekolah())).setMaxResults(1)
										.addOrder(Order.desc("id")), Siswa.class);
							}

							if (siswa != null && siswa.getId() != null) {

								if (pengaturanBiaya.getKhususBuatSiswaTertentu()) {
									int pengaturanBiayaPunyaSiswaCount = ((Number) session
											.createCriteria(PengaturanBiayaPunyaSiswa.class)
											.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
											.add(Restrictions.eq("siswa", siswa)).setProjection(Projections.rowCount())
											.uniqueResult()).intValue();
									if (pengaturanBiayaPunyaSiswaCount == 0) {
										PengaturanBiayaPunyaSiswa pengaturanBiayaPunyaSiswa = new PengaturanBiayaPunyaSiswa();
										pengaturanBiayaPunyaSiswa.setSiswa(siswa);
										pengaturanBiayaPunyaSiswa.setPengaturanBiaya(pengaturanBiaya);
										session.getTransaction().begin();
										session.save(pengaturanBiayaPunyaSiswa);
										session.getTransaction().commit();
									}
								}

								List<Long> notPembayaran = new ArrayList<Long>();
								int index = 0;
								for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {

									NominalBiaya nominalBiaya = null;
									if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Harian")
											&& pengaturanBiaya.getBulanMulai() != null) {
										nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
												pembayaranTerakhir, session);

										for (int bayarKe = 1; bayarKe <= 31; bayarKe++) {
											Tagihan tagihan = null;
											String kodeUnik = null;
											boolean ada = dates.contains(bayarKe);
											if (ada) {
												kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
														nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir,
														nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

												tagihan = tagihanPreload.get(kodeUnik);
												if (tagihan == null) tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

											}

											int j = contents.length + index;
											Double nominal = Common.getSheetContentAsDouble(sheet, j, i);
											nominal = nominal == null ? 0.0 : nominal;
											PembayaranSiswaDetail pembayaranSiswaDetail = null;

											if (ada) {
												System.out.println("nominal -> " + nominal + " pembayaranTerakhir "
														+ pembayaranTerakhir + " tagihan -> " + tagihan);

												if (tagihan == null) {
													pembayaranSiswaDetail = (PembayaranSiswaDetail) session
															.createCriteria(PembayaranSiswaDetail.class)
															.createAlias("tagihan", "tagihan")
															.add(Restrictions.eq("tagihan.bayarKe", bayarKe))
															.add(Restrictions.eq("nominalBiaya", nominalBiaya))

															.add(notPembayaran.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions
																			.not(Restrictions.in("id", notPembayaran)))

															.add(Restrictions.eq("itemBiayaSekolah",
																	pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
															.createCriteria("pembayaranSiswa")
															.add(Restrictions.eq("siswa", siswa))
															.add(Restrictions.eq("jenisBiayaSekolah",
																	pengaturanBiaya.getJenisBiayaSekolah()))
															.add(Restrictions.eq("tahunDanBulan", pembayaranTerakhir))
															.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

													if (pembayaranSiswaDetail != null
															&& pembayaranSiswaDetail.getId() != null) {
														notPembayaran.add(pembayaranSiswaDetail.getId());
													}
													if (pembayaranSiswaDetail == null
															|| pembayaranSiswaDetail.getTagihan() == null) {
														tagihan = new Tagihan();
														tagihan.setNominalBiaya(nominalBiaya);
														tagihan.setTahunbulan(pembayaranTerakhir);
														tagihan.setBulan(Integer
																.parseInt(pembayaranTerakhir.toString().substring(4)));
														tagihan.setTahun(Integer.parseInt(
																pembayaranTerakhir.toString().substring(0, 4)));
														tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
														tagihan.setSiswa(siswa);
														tagihan.setItemBiayaSekolah(
																pengaturanBiayaItemBiaya.getItemBiayaSekolah());
														tagihan.setBayarKe(bayarKe);

														Double n = (nominalBiaya.getItemBiayaSekolah()
																.getBolehDiangsur()
																&& nominalBiaya.getPengaturanBiaya()
																		.getJenisBiayaSekolah()
																		.getBolehAngsurBerapapun()
																				? (nominal / nominalBiaya
																						.getDibayarSebayak())
																				: nominal);

														tagihan.setNominal(n);
														tagihan.setNominalManual(n);
													} else {
														tagihan = pembayaranSiswaDetail.getTagihan();
													}

												} else {
													tagihan.setNominalManual(nominal);
													tagihan.setNominal(nominal);
												}
											}
											index++;

											j = contents.length + index;
											Boolean bukanTagihan = Common.getSheetContentAsBoolean(sheet, j, i);
											index++;

											if (ada) {
												tagihan.setBayarKe(bayarKe);
												tagihan.setBukanTagihan(bukanTagihan);

												session.getTransaction().begin();
												session.saveOrUpdate(tagihan);
												session.getTransaction().commit();
												session.flush();

												if (pembayaranSiswaDetail != null
														&& pembayaranSiswaDetail.getId() != null) {
													pembayaranSiswaDetail.setTagihan(tagihan);
													session.getTransaction().begin();
													session.saveOrUpdate(pembayaranSiswaDetail);
													session.getTransaction().commit();

													if (tagihan.getDiskonSiswa() != null
															&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
														DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
													}
												}

												System.out.println("nomorInduk => " + nomorInduk + ", siswa => " + siswa
														+ ", nominal = " + nominal + ", j = " + j + ", item biaya = "
														+ pengaturanBiayaItemBiaya.getItemBiayaSekolah()
														+ ", kodeUnik = " + kodeUnik + ", tagihan = "
														+ tagihan.toString() + " bukanTagihan " + bukanTagihan);
											}

										}

									}

									else if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode()
											.equalsIgnoreCase("Bulanan")) {

										nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
												session);

										TreeSet<Integer> integers = bulans.get(pengaturanBiayaItemBiaya.getId());
										if (integers != null) {
											List<Tagihan> tagihanDiskonBatch = new ArrayList<Tagihan>();
											Transaction txBulanan = session.beginTransaction();
											try {
											for (Integer pembayaranTerakhir : integers) {

												int bayarKe = 1;
												String kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
														nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir,
														nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

												Tagihan tagihan = tagihanPreload.get(kodeUnik);
												if (tagihan == null) tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

												// Fallback DB: cache tidak selalu memuat tagihan lama.
												// Tanpa ini, tagihan yang sudah ada di DB tapi tidak di-cache
												// akan di-INSERT ulang → kodeUnik duplicate → update gagal diam-diam.
												if (tagihan == null && kodeUnik != null && !kodeUnik.isEmpty()) {
													tagihan = (Tagihan) session.createCriteria(Tagihan.class)
															.add(Restrictions.eq("kodeUnik", kodeUnik))
															.setMaxResults(1).uniqueResult();
												}

												int j = contents.length + index;
												Double nominal = Common.getSheetContentAsDouble(sheet, j, i);
												nominal = nominal == null ? 0.0 : nominal;
												System.out.println("nominal -> " + nominal + " pembayaranTerakhir "
														+ pembayaranTerakhir + " tagihan -> " + tagihan);

												PembayaranSiswaDetail pembayaranSiswaDetail = null;
												if (tagihan == null) {
													pembayaranSiswaDetail = (PembayaranSiswaDetail) session
															.createCriteria(PembayaranSiswaDetail.class)
															.createAlias("tagihan", "tagihan")
															.add(Restrictions.eq("tagihan.bayarKe", bayarKe))
															.add(Restrictions.eq("nominalBiaya", nominalBiaya))

															.add(notPembayaran.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions
																			.not(Restrictions.in("id", notPembayaran)))

															.add(Restrictions.eq("itemBiayaSekolah",
																	pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
															.createCriteria("pembayaranSiswa")
															.add(Restrictions.eq("siswa", siswa))
															.add(Restrictions.eq("jenisBiayaSekolah",
																	pengaturanBiaya.getJenisBiayaSekolah()))
															.add(Restrictions.eq("tahunDanBulan", pembayaranTerakhir))
															.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

													if (pembayaranSiswaDetail != null
															&& pembayaranSiswaDetail.getId() != null) {
														notPembayaran.add(pembayaranSiswaDetail.getId());
													}
													if (pembayaranSiswaDetail == null
															|| pembayaranSiswaDetail.getTagihan() == null) {
														tagihan = new Tagihan();
														tagihan.setNominalBiaya(nominalBiaya);
														tagihan.setTahunbulan(pembayaranTerakhir);
														tagihan.setBulan(Integer
																.parseInt(pembayaranTerakhir.toString().substring(4)));
														tagihan.setTahun(Integer.parseInt(
																pembayaranTerakhir.toString().substring(0, 4)));
														tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
														tagihan.setSiswa(siswa);
														tagihan.setItemBiayaSekolah(
																pengaturanBiayaItemBiaya.getItemBiayaSekolah());
														tagihan.setBayarKe(bayarKe);

														Double n = (nominalBiaya.getItemBiayaSekolah()
																.getBolehDiangsur()
																&& nominalBiaya.getPengaturanBiaya()
																		.getJenisBiayaSekolah()
																		.getBolehAngsurBerapapun()
																				? (nominal / nominalBiaya
																						.getDibayarSebayak())
																				: nominal);

														tagihan.setNominal(n);
														tagihan.setNominalManual(n);
													} else {
														tagihan = pembayaranSiswaDetail.getTagihan();
													}

												} else {
													tagihan.setNominalManual(nominal);
													tagihan.setNominal(nominal);
												}

												index++;

												j = contents.length + index;
												Boolean bukanTagihan = Common.getSheetContentAsBoolean(sheet, j, i);
												index++;

												tagihan.setBukanTagihan(bukanTagihan);

												// Batch: simpan tanpa commit — semua bulan dikumpulkan dan di-commit sekaligus
												session.saveOrUpdate(tagihan);
												tagihanPreload.put(kodeUnik, tagihan);

												if (pembayaranSiswaDetail != null
														&& pembayaranSiswaDetail.getId() != null) {
													pembayaranSiswaDetail.setTagihan(tagihan);
													session.saveOrUpdate(pembayaranSiswaDetail);
													if (tagihan.getDiskonSiswa() != null
															&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
														tagihanDiskonBatch.add(tagihan);
													}
												}

												System.out.println("nomorInduk => " + nomorInduk + ", siswa => " + siswa
														+ ", nominal = " + nominal + ", j = " + j + ", item biaya = "
														+ pengaturanBiayaItemBiaya.getItemBiayaSekolah()
														+ ", kodeUnik = " + kodeUnik + ", tagihan = "
														+ tagihan.toString() + " bukanTagihan " + bukanTagihan);

											}
											txBulanan.commit();
										} catch (Exception eTxBulanan) {
											try {
												if (session.getTransaction() != null && session.getTransaction().isActive()) {
													session.getTransaction().rollback();
												}
											} catch (Exception er) { ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3064");}
											throw eTxBulanan;
										}
										for (Tagihan tdBatch : tagihanDiskonBatch) {
											try {
												DaftarPengajuanTransfer.simpanDiskonPembayaran(tdBatch);
											} catch (Exception eDiskon) {
												eDiskon.printStackTrace(); ais.common.ErrorAuditUtil.record(eDiskon, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3071");
											}
										}
										}
									} else {

										nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
												session);

										int j = contents.length + index;
										Double nominal = Common.getSheetContentAsDouble(sheet, j, i);
										nominal = nominal == null ? 0.0 : nominal;
										nominalBiaya.setNominal(nominal);

										index++;

										int k = contents.length + index;
										Integer dibayarSebayak = Common.getSheetContentAsInteger(sheet, k, i);
										nominalBiaya.setDibayarSebayak(dibayarSebayak);

										index++;

										k = contents.length + index;
										Boolean bukanTagihan = Common.getSheetContentAsBoolean(sheet, k, i);
										index++;

										nominalBiaya.setBukanTagihan(bukanTagihan);
										session.getTransaction().begin();
										try {
											session.saveOrUpdate(nominalBiaya);
										} catch (org.hibernate.NonUniqueObjectException nuoe) {
											// FIX NonUniqueObjectException: instance nominalBiaya di tangan kita
											// (hasil TagihanUtil.ambilNominalBiaya) bukan instance yang sedang
											// dikelola sesi untuk id yang sama -- mis. baris Excel lain sudah
											// memuat NominalBiaya id yang sama ke sesi lebih dulu dalam batch
											// upload ini. merge() menyalin state ke instance yang sudah
											// terkelola alih-alih melempar exception; pakai hasil merge
											// (instance terkelola) untuk sisa proses baris ini.
											ais.common.ErrorAuditUtil.record(nuoe, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:nominalBiaya-nonunique");
											nominalBiaya = (NominalBiaya) session.merge(nominalBiaya);
										}
										session.getTransaction().commit();

										// Sinkronkan Tagihan.nominalManual dan .nominal agar tampilan
										// grid mencerminkan nilai yang baru di-upload.
										@SuppressWarnings("unchecked")
										java.util.List<Tagihan> tagihanUploadList = session
												.createCriteria(Tagihan.class)
												.add(Restrictions.eq("nominalBiaya", nominalBiaya)).list();
										for (Tagihan tagihanUpload : tagihanUploadList) {
											tagihanUpload.setNominalManual(nominal);
											tagihanUpload.setNominal(nominal);
											tagihanUpload.setBukanTagihan(bukanTagihan);
											session.getTransaction().begin();
											session.saveOrUpdate(tagihanUpload);
											session.getTransaction().commit();
										}

										System.out.println("nomorInduk => " + nomorInduk + ", siswa => " + siswa
												+ ", nominal = " + nominal + ", j = " + j + ", item biaya = "
												+ pengaturanBiayaItemBiaya.getItemBiayaSekolah() + ", nominalBiaya = "
												+ nominalBiaya.toString() + " bukanTagihan " + bukanTagihan);
									}

									if (nominalBiaya != null) {
										Number maks = (Number) session.createCriteria(Tagihan.class)
												.add(Restrictions.eq("nominalBiaya", nominalBiaya))
												.setProjection(Projections.rowCount())
												.add(Restrictions.gt("nominal", 0.1)).uniqueResult();

										if (nominalBiaya.getDibayarSebayak()
												.intValue() != (maks == null ? 1 : maks.intValue())) {
											nominalBiaya.setDibayarSebayak((maks == null ? 1 : maks.intValue()));
											session.getTransaction().begin();
											Common.refreshUpdate(session, nominalBiaya);
											session.getTransaction().commit();
										}
									}
								}

								report.sukses(i, siswa.getNim() + " - " + siswa.getNama(), "Tagihan berhasil diperbarui");
								label.setValue("Upload data \"" + siswa.getNim() + " - " + siswa.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								String nis = Common.getSheetContentAsString(sheet, 1, i);
								String nm = Common.getSheetContentAsString(sheet, 2, i);
								String id = (nis != null && !nis.trim().isEmpty() ? "NIS: " + nis : (nm != null && !nm.trim().isEmpty() ? "Nama: " + nm : "Baris " + i));
								report.gagal(i, id, "Siswa tidak ditemukan di database", "Pastikan NIS atau nama siswa sudah benar dan terdaftar di sekolah ini.");
							}

						} catch (Exception e) {
							String idSiswa = "Baris " + i;
							try {
								String nis = Common.getSheetContentAsString(sheet, 1, i);
								String nm = Common.getSheetContentAsString(sheet, 2, i);
								if (nis != null && !nis.trim().isEmpty()) idSiswa = "NIS: " + nis;
								else if (nm != null && !nm.trim().isEmpty()) idSiswa = nm;
							} catch (Exception eId) {}
							report.gagal(i, idSiswa, e, "Periksa data pada baris ini. Pastikan NIS/nama siswa valid dan pengaturan biaya aktif.");
							Common.tampilErrorJikaAdmin(e);
						}

					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3151");
				} finally {
					// openSession dedikasi WAJIB ditutup di finally (clear/disconnect/close).
					if (session != null) {
						try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3155");}
						try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3156");}
						try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanSiswaHelper.java:3157");}
					}
				}

				try {
					java.io.File reportFile = report.simpanLaporan();
					downloadPath.setValue(reportFile.getAbsolutePath());
					peringatan.setValue(report.getRingkasan());
				} catch (Exception eReport) { ais.common.ErrorAuditUtil.record(eReport, "auto-audit(empty-catch) DetailTagihanSiswaHelper report gen"); }
				HibernateUtil.closeSession();

				label.setValue("");
			}
		}).start();
	}

}
