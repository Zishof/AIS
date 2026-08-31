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
import java.util.List;
import java.util.TreeSet;

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
import ais.common.MemoryDbUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
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

/**
 * Helper UI besar untuk layar rincian tagihan biaya calon siswa PMB: menampilkan matriks
 * "calon siswa &times; item biaya" untuk satu {@link PengaturanBiaya} (konfigurasi paket biaya
 * PMB, mis. biaya formulir/daftar ulang/SPP), tempat setiap sel menunjukkan status tagihan
 * ({@link Tagihan}) dan pembayaran ({@link PembayaranSiswaDetail}) calon siswa untuk item biaya
 * tersebut, dengan opsi tampilan per-bulan (kolom bergeser bulan ke bulan untuk item biaya
 * berulang seperti SPP) lewat filter rentang bulan mulai-sampai. Menyediakan aksi "Singkronkan"
 * (menjalankan ulang {@link PengaturanBiaya#reloadTagihan} untuk membuat/menyesuaikan baris
 * tagihan sesuai konfigurasi terbaru) dan unggah massal status pembayaran dari Excel
 * ({@link #uploadDataCalonSiswa}).
 *
 * <p>
 * Kepesertaan calon siswa terhadap satu {@link PengaturanBiaya} ditentukan lewat kriteria
 * penargetan yang sangat kaya di {@link #initCriteria(Session, PengaturanBiaya, CalonSiswa,
 * Textbox, PengaturanBiayaItemBiaya, boolean, boolean)}: dicocokkan lewat gelombang pendaftaran
 * PSB atau paket PSB (langsung, atau tidak langsung lewat {@link GelombangPendaftaranPsb} yang
 * ditandai "sesuai kelas"/"sesuai kelas saat diterima" terhadap jenis biaya sekolah terkait),
 * dapat dipersempit ke daftar siswa tertentu ({@code khususBuatSiswaTertentu}), satu kelas
 * ({@code kelasSiswa}), banyak nama kelas sekaligus ({@code kelasBanyak}, dipisah koma), kelas
 * les, tahun angkatan, sekolah, dan jurusan/penjurusan sekolah — kombinasi filter ini
 * memungkinkan satu paket biaya berlaku sangat spesifik (satu siswa) hingga sangat luas (seluruh
 * angkatan).
 * </p>
 *
 * <p>
 * {@link #onTagihanRinciBaru} adalah komponen UI terpisah yang menangani pemecahan satu nominal
 * pembayaran menjadi beberapa cicilan/tagihan ({@link Tagihan}) sekaligus — pengguna mencentang
 * tagihan mana yang dibayar dan mengisi nominal per tagihan, dengan total berjalan ditampilkan di
 * footer grid (termasuk persentase dan sisa bila item biaya boleh diangsur bebas).
 * </p>
 */
public class DetailTagihanCalonSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private List<PengaturanBiayaItemBiaya> pengaturanBiayaItemBiayas;
	private PengaturanBiaya pengaturanBiaya;

	private Textbox nama;

	private CalonSiswa calonSiswa;

	private boolean edit = false;
	private boolean approve = false;

	/**
	 * Membuat helper. Bila {@code calonSiswa} tidak {@code null}, tampilan terikat pada satu calon
	 * siswa tersebut (label nama statis, bukan kolom pencarian); {@code edit} mengizinkan
	 * perubahan nominal/tanggal tagihan, {@code approve} mengizinkan aksi persetujuan pembayaran.
	 */
	public DetailTagihanCalonSiswaHelper(CalonSiswa calonSiswa, boolean edit, boolean approve) {
		this.calonSiswa = calonSiswa;
		this.edit = edit;
		this.approve = approve;
	}

	/** Renderer baris grid utama: satu baris per {@link CalonSiswa}, kolom-kolom menampilkan status tagihan/pembayaran item biaya {@code pengaturanBiayaItemBiaya} (satu kolom per bulan bila {@code pembayaranTerakhir} mengindikasikan tampilan multi-bulan aktif). */
	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		private PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya;

		private Integer pembayaranTerakhir;

		/** Membuat renderer untuk item biaya {@code pengaturanBiayaItemBiaya} tertentu, dengan {@code pembayaranTerakhir} sebagai penanda periode tagihan terakhir yang relevan untuk tampilan multi-bulan. */
		public DetailPARenderer(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Integer pembayaranTerakhir) {
			this.pengaturanBiayaItemBiaya = pengaturanBiayaItemBiaya;
			this.pembayaranTerakhir = pembayaranTerakhir;
		}

		/** Merender satu baris calon siswa untuk tampilan kolom tunggal (bukan mode "bagi" pembagian nominal antar tagihan); mendelegasikan ke {@link #render(Row, Object, boolean)}. */
		@Override
		public void render(Row row, Object data) throws Exception {
			render(row, data, false);
		}

		/**
		 * Implementasi inti render satu baris calon siswa: menampilkan status tagihan (lunas/
		 * belum/sebagian) dan tombol aksi (bayar, revisi, hapus, lihat rincian) untuk item biaya
		 * {@link #pengaturanBiayaItemBiaya}. Parameter {@code bagi} mengaktifkan mode di mana
		 * satu pembayaran dapat dipecah ke beberapa tagihan sekaligus (memakai UI
		 * {@link DetailTagihanCalonSiswaHelper#onTagihanRinciBaru}) — dipakai saat item biaya
		 * mengizinkan pembayaran gabungan/angsuran lintas periode.
		 */
		public void render(final Row row, Object data, boolean bagi) throws Exception {
			row.setValign("top");
			final CalonSiswa calonSiswa = (CalonSiswa) data;

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);

			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(hbox1);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(hbox1);

			RevisiHelper.createNewRevisi(CalonSiswa.class, calonSiswa, calonSiswa.getNomorInduk()).setParent(vbox1);

			new Label(calonSiswa.getNama()).setParent(vbox1);

			calonSiswa.tampilkanHp(vbox1);
			calonSiswa.tampilkanEmail(vbox1);

			Session session1 = HibernateUtil.currentNativeSession();
			NominalBiaya nominalBiaya = TagihanUtilCalonSiswa.ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa,
					pembayaranTerakhir, session1);
			session1.disconnect();
			session1.close();
			HibernateUtil.closeSession();

			final NominalBiaya nb = nominalBiaya;

			final Checkbox bukanTagihan = new Checkbox(
					"\"" + pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama() + "\" Bukan Tagihan");
			bukanTagihan.setChecked(nb.getBukanTagihan());
			bukanTagihan.setDisabled(!edit);
			bukanTagihan.setStyle("font-size:8px;");
			bukanTagihan.setParent(vbox1);
			bukanTagihan.addEventListener("onClick", new EventListener() {
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

										HibernateUtil.currentSession()
												.createSQLQuery("update sekolah.tagihan set aktif="
														+ (!bukanTagihan.isChecked()) + " where nominal_biaya_id="
														+ nb.getId())
												.executeUpdate();

										nb.setBukanTagihan(bukanTagihan.isChecked());
										Common.refreshUpdate(nb);
										Common.clear(row);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												render(row, calonSiswa);
											}
										});
									}
								}
							}, nb.getItemBiayaSekolah().getNama(), (bukanTagihan.isChecked() ? "bukan" : "adalah"));
				}
			});
			if (mul != null && sam != null) {
				List<Long> notPembayaran = new ArrayList<Long>();
				for (int pembayaranTerakhir = mul; pembayaranTerakhir <= sam; pembayaranTerakhir++) {

					if ((pengaturanBiaya.getBulanMulai() != null
							&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai())
							|| (pengaturanBiaya.getBulanSampai() != null
									&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai())) {
						continue;
					}

					int tahun = Integer.parseInt((pembayaranTerakhir + "").substring(0, 4));
					int bulan = Integer.parseInt((pembayaranTerakhir + "").substring(4));
					if (bulan > 12 || bulan < 1) {
						continue;
					}

					int bayarKe = 1;
					String kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
							nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir, nominalBiaya.getSiswa(),
							nominalBiaya.getCalonSiswa(), bayarKe);

					Session session = HibernateUtil.currentNativeSession();
					Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);
					System.out.println("reload tagihan -> " + tagihan + ", kodeUnik => " + kodeUnik);
					if (tagihan == null) {
						try {

							PembayaranSiswaDetail pembayaranSiswaDetail = (PembayaranSiswaDetail) session
									.createCriteria(PembayaranSiswaDetail.class)

									.createAlias("tagihan", "tagihan").add(Restrictions.eq("tagihan.bayarKe", bayarKe))
									.add(Restrictions.eq("nominalBiaya", nominalBiaya))

									.add(Restrictions.eq("itemBiayaSekolah", nominalBiaya.getItemBiayaSekolah()))
									.createCriteria("pembayaranSiswa")
									.add(Restrictions.eq("calonSiswa", nominalBiaya.getCalonSiswa()))
									.add(Restrictions.eq("jenisBiayaSekolah",
											nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()))
									.add(Restrictions.or(Restrictions.isNull("bulan"), Restrictions.eq("bulan", bulan)))
									.add(Restrictions.or(Restrictions.isNull("tahun"), Restrictions.eq("tahun", tahun)))

									.add(notPembayaran.isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.not(Restrictions.in("id", notPembayaran)))

									.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

							if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
								notPembayaran.add(pembayaranSiswaDetail.getId());
							}

							if (pembayaranSiswaDetail == null || pembayaranSiswaDetail.getTagihan() == null) {
								System.out.println("simpan tagihan baru -> tahun " + tahun + ", bulan => " + bulan);
								tagihan = new Tagihan();
								tagihan.setNominal(nominalBiaya.getItemBiayaSekolah().getBolehDiangsur()
										&& pengaturanBiaya.getJenisBiayaSekolah().getBolehAngsurBerapapun()
												? (nominalBiaya.getNominal() / nb.getDibayarSebayak())
												: nominalBiaya.getNominal());
								tagihan.setNominalBiaya(nominalBiaya);
								tagihan.setBulan(bulan);
								tagihan.setTahun(tahun);
								tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
								tagihan.setCalonSiswa(nominalBiaya.getCalonSiswa());
								tagihan.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
								tagihan.setBayarKe(bayarKe);
								session.getTransaction().begin();
								session.save(tagihan);
								session.getTransaction().commit();

								if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
									pembayaranSiswaDetail.setTagihan(tagihan);

									session.getTransaction().begin();
									session.update(pembayaranSiswaDetail);
									session.getTransaction().commit();

									if (tagihan.getDiskonSiswa() != null
											&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
										DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
									}
								}

							} else {
								tagihan = pembayaranSiswaDetail.getTagihan();
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();

					Vbox vbox = new Vbox();
					vbox.setParent(row);

					final Tagihan tag = tagihan;

					if (!nb.getBukanTagihan() && !tag.ambilBukanTagihanData() && !tag.ambilBukanTagihan()) {

						Hbox hbox = new Hbox();
						hbox.setParent(vbox);
						final MyDoublebox nilai = new MyDoublebox(tagihan.getNominal());
						nilai.setCols(7);

						if (nominalBiaya.getPengaturanBiayaItemBiaya() != null
								&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() != null
								&& nominalBiaya.getPengaturanBiayaItemBiaya().getMinimalBiaya() != null
								&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() > 0.1
								&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya()
										.intValue() == nominalBiaya.getPengaturanBiayaItemBiaya().getMinimalBiaya()
												.intValue()) {
							new Label(Common.numberFormat.get().format(nominalBiaya.getNominal())).setParent(hbox);
						}

						else

						if (nominalBiaya.getDibayarSebayak() == 1 && tagihan != null
								&& tagihan.getPembayaranSiswaDetail() != null) {
							new Label(Common.numberFormat.get().format(tagihan.getNominal() + tagihan.getDiskon()))
									.setParent(hbox);
						}

						else if (tag.getPembayaranSiswaDetail() != null
								|| tag.getItemBiayaSekolah().getParameterTambahan() != null || !edit) {
							new Label(Common.numberFormat.get().format(tagihan.getNominal() + tagihan.getDiskon()))
									.setParent(hbox);
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

								Session session = HibernateUtil.currentNativeSession();

								session.refresh(tag);
								tag.setNominal(nilai.getValue());
								tag.setNominalManual(nilai.getValue());
								session.getTransaction().begin();
								session.update(tag);
								session.getTransaction().commit();

								if (nb != null && nb.getDibayarSebayak().intValue() == 1) {
									if (!nb.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode()
											.equalsIgnoreCase("Bulanan")) {
										nb.setNominal(nilai.getValue());
										session.getTransaction().begin();
										session.update(nb);
										session.getTransaction().commit();
									}
								}

								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}

								HibernateUtil.closeSession();
							}
						});
						PembayaranSiswaDetail a = tagihan.getPembayaranSiswaDetail();
						if (a != null && a.getId() != null) {

//							System.out.println("1 pembayaranSiswaDetail -> " + a.getNominal());

							RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, a,
									"Dibayar : " + Common.numberFormat.get().format(a.getNominal()), "font-size:9px;")
									.setParent(vbox);
						} else {
							if (edit) {
								TagihanUtilCalonSiswa.tampilkanKunci(vbox, tag, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(row);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												render(row, calonSiswa);
											}
										});
									}
								}, tbmuser, nilai, approve);
							}
						}

						RevisiHelper.createNewRevisi(Tagihan.class, tagihan, "H").setParent(hbox);

					}
					if (!nb.getBukanTagihan() && !tag.ambilBukanTagihan()) {
						if (tag.getPembayaranSiswaDetail() == null) {
//							if (nb.getDibayarSebayak().intValue() > 1) {
							final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
							bukanTagihana.setChecked(tag.ambilBukanTagihanData());
							bukanTagihana.setDisabled(!edit);
							bukanTagihana.setStyle("font-size:8px;");
							if (tag.getKunci() == null && tag.getPengaturanBiaya().getKunci() == null)
								bukanTagihana.setParent(vbox);
							bukanTagihana.addEventListener("onClick", new EventListener() {
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
														tag.setBukanTagihan(bukanTagihana.isChecked());
														Common.refreshUpdate(tag);
														Common.clear(row);
														Common.createDefaultTimer(new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																render(row, calonSiswa);
															}
														});
													}
												}
											}, (bukanTagihana.isChecked() ? "bukan" : "adalah"));
								}
							});
//							}
						} else {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pindahkan",
									"/img/stock_data_edit_table.png");
							button.setDisabled(!edit);
							button.setTooltiptext("Pindah Data");
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
																	render(row, calonSiswa);
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

			} else {

				Vbox vb = new Vbox();
				vb.setParent(row);

				if (!nb.getBukanTagihan()) {

					Hbox hbox = new Hbox();
					hbox.setParent(vb);
					List<Tagihan> listTagihans = nb.ambilTagihans();
					final MyDoublebox nilai = new MyDoublebox(nominalBiaya.getNominal());
					nilai.setCols(6);

					if (nominalBiaya.getPengaturanBiayaItemBiaya() != null
							&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() != null
							&& nominalBiaya.getPengaturanBiayaItemBiaya().getMinimalBiaya() != null
							&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() > 0.1
							&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya().intValue() == nominalBiaya
									.getPengaturanBiayaItemBiaya().getMinimalBiaya().intValue()) {
						new Label(Common.numberFormat.get().format(nominalBiaya.getNominal())).setParent(hbox);
					}

					else if (nominalBiaya.getItemBiayaSekolah().getParameterTambahan() != null || !edit) {
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
					RevisiHelper.createNewRevisi(NominalBiaya.class, nominalBiaya, " x ").setParent(hbox);

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
											render(row, calonSiswa);
										}
									});
								} catch (Exception eC) {
									if (txC != null && txC.isActive())
										txC.rollback();
									eC.printStackTrace(); ais.common.ErrorAuditUtil.record(eC, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:592");
								} finally {
									if (sessionC != null && sessionC.isOpen()) {
										try {
											sessionC.clear();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:597");
										}
										try {
											sessionC.disconnect();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:601");
										}
										try {
											sessionC.close();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:605");
										}
									}
								}
							}
						});
					}

					Toolbarbutton reset = new ais.ui.util.MyToolbarbuttonConfig("Reset", "/img/svg/deny.svg");
					reset.setParent(hbox);
					reset.setDisabled(!edit);
					reset.setVisible(!tagihanDIbuatOtomatisMenghitungSisa);
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

													Common.refreshDelete(nb);

													Common.clear(row);
													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															render(row, calonSiswa);
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

					vb.appendChild(onTagihanRinciBaru(nilai, reset, nb, tbmuser, pengaturanBiayaItemBiaya, edit,
							approve, listTagihans));

				}

//				if (nb.getDibayarSebayak().intValue() > 1) {
				final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
				bukanTagihana.setChecked(nb.getBukanTagihan());
				bukanTagihana.setStyle("font-size:8px;");
				bukanTagihana.setParent(vb);
				bukanTagihana.setDisabled(!edit);
				bukanTagihana.setVisible(false);
				bukanTagihana.addEventListener("onClick", new EventListener() {
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
													render(row, calonSiswa);
												}
											});
										}
									}
								}, (bukanTagihana.isChecked() ? "bukan" : "adalah"));
					}
				});
//				}
			}

		}

	}

	/**
	 * Membangun grid interaktif untuk memecah satu nilai pembayaran ({@code nilai}) ke beberapa
	 * {@link Tagihan} (cicilan/periode) sekaligus: satu baris per tagihan dengan checkbox pilih
	 * dan input nominal per tagihan; total nominal yang dicentang dijumlahkan dan ditampilkan di
	 * footer grid (termasuk persentase dan sisa terhadap {@code nominalBiaya.getNominal()} bila
	 * item biaya mengizinkan angsuran bebas). Bila tagihan pertama sudah memiliki satu-satunya
	 * pembayaran (dibayarSebayak == 1) dan sudah lunas, input {@code nilai} dan tombol
	 * {@code reset} dikunci.
	 *
	 * @param nilai                     input nominal total yang akan dipecah
	 * @param reset                     tombol reset terkait, dikunci/disembunyikan sesuai kondisi tagihan
	 * @param nominalBiaya              nominal biaya acuan (untuk perhitungan persentase/sisa)
	 * @param tbmuser                   pengguna saat ini
	 * @param pengaturanBiayaItemBiaya  item biaya konteks
	 * @param edit                      izinkan pengubahan tanggal tagihan
	 * @param approve                   izinkan aksi persetujuan
	 * @param tagihans                  daftar tagihan yang akan direpresentasikan sebagai baris
	 * @return grid berpaginasi (3 baris/halaman) siap ditempelkan ke form pembayaran
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen
	 */
	public static MyGrid onTagihanRinciBaru(final MyDoublebox nilai, final Toolbarbutton reset,
			final NominalBiaya nominalBiaya, final Tbmuser tbmuser,
			final PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, final boolean edit, final boolean approve,
			final List<Tagihan> tagihans) throws Exception {

		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(3);
		grid.getPagingChild().setMold("os");
		grid.getPagingChild().setPageIncrement(3);

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
					MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
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
					if (tagihan != null && tagihan.getId() != null) {
						final Tagihan tag = tagihan;

						if (nominalBiaya.getDibayarSebayak() == 1 && tagihan.getPembayaranSiswaDetail() != null) {
							nilai.setValue(tagihan.getPembayaranSiswaDetail().getNominal());
							nilai.setDisabled(true);
							reset.setDisabled(true);
						}

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						PembayaranSiswaDetail a = tagihan.getPembayaranSiswaDetail();
						if (a != null) {
							reset.setVisible(false);
						}
						RevisiHelper.createNewRevisi(Tagihan.class, tagihan, "ke-" + bayarKe).setParent(row);

						if (tagihan.getPengaturanBiaya().getTanggalTagihanMengikutiDefault()
								|| !edit) {
							new Label(Common.dateFormat11.get().format(tagihan.getTanggalTagihan()));
						} else {

							final MyDatebox tanggalTagihan = new MyDatebox(tagihan.getTanggalTagihan());
							tanggalTagihan.setFormat(Common.dateFormat11.get().toPattern());
							tanggalTagihan.setDisabled(!tag.getAktif());
							tanggalTagihan.setWidth("90%");
							tanggalTagihan.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
											nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
											nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
											pengaturanBiayaItemBiaya, true);

									tagihan.setTanggalTagihan(tanggalTagihan.getValue());
									Common.refreshUpdate(tagihan);
									eventListenerHitung.onEvent(arg0);
								}
							});
						}
						final MyDoublebox nominal = new MyDoublebox(tagihan.getNominal());

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

						final MyCheckboxConfig checkbox = new MyCheckboxConfig("");
						checkbox.setDisabled(!edit);
						checkbox.setChecked(
								tag.getAktif() || (tag.getAktifkanmanual() != null && tag.getAktifkanmanual()));
						if ((a != null && a.getId() != null) || tag.ambilBukanTagihanData()
								|| tag.ambilBukanTagihan()) {
							new Label().setParent(row);
						} else {
							checkbox.setParent(row);
						}
						row.setValign("top");
						row.setAttribute("checkbox", checkbox);
						checkbox.addEventListener("onCheck", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
										pengaturanBiayaItemBiaya, true);

								tagihan.setAktifkanmanual(checkbox.isChecked());
								tagihan.setAktif(checkbox.isChecked());
								tagihan.setNominalManual(nominal.getValue());
								Common.refreshSaveOrUpdate(tagihan);
								nominal.setDisabled(!tagihan.getAktifkanmanual());
								eventListenerHitung.onEvent(arg0);
							}
						});

						nominal.setDisabled(!tag.getAktif() || !edit);

						if ((a != null && a.getId() != null) || tag.ambilBukanTagihan()) {
							Vbox vbox = new Vbox();
							vbox.setWidth("95%");
							vbox.setPack("end");
							vbox.setAlign("right");
							vbox.setParent(row);

							RevisiHelper
									.createNewRevisi(Tagihan.class, tagihan,
											Common.numberFormat.get().format(
													(a.getNominal() + tagihan.getDiskon() + tagihan.getDenda())))
									.setParent(vbox);

							nominal.setValue((a.getNominal() + tagihan.getDiskon() + tagihan.getDenda()));

							RevisiHelper
									.createNewRevisi(PembayaranSiswaDetail.class, a,
											"Dibayar : " + Common.numberFormat.get()
													.format(a.getNominal() + tagihan.getDenda()),
											"font-size:9px;")
									.setParent(vbox);

							if (tagihan.getDiskonSiswa() != null) {
								RevisiHelper.createNewRevisi(DiskonSiswa.class, tagihan.getDiskonSiswa(),
										"Diskon : " + Common.numberFormat.get().format(tagihan.getDiskon()),
										"font-size:9px;").setParent(vbox);
							}

							if (!sa.isEmpty()) {
								vbox.appendChild(new Label(sa));
							}

							if (a != null && a.getId() != null) {
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pindahkan",
										"/img/stock_data_edit_table.png");
								button.setTooltiptext("Pindah Data");
								button.setDisabled(!edit);
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										MyMessageboxConfig.show(
												"Apakah Anda yakin ingin memindahkan pembayaran siswa ini? Data pembayaran akan dipindahkan ke lokasi tujuan yang dipilih.", "Pertanyaan",
												MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
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
							if (tag.getKunci() != null || tag.getPengaturanBiaya().getKunci() != null) {
								vbox.appendChild(new Label(Common.numberFormat.get().format(tag.getNominal())));
							} else if (!tag.ambilBukanTagihanData()) {
								vbox.appendChild(nominal);
							}

							if (tagihan.getDiskonSiswa() != null) {
								RevisiHelper.createNewRevisi(DiskonSiswa.class, tagihan.getDiskonSiswa(),
										"Diskon : " + Common.numberFormat.get().format(tagihan.getDiskon()),
										"font-size:9px;").setParent(vbox);
							}

							if (tag.getPembayaranSiswaDetail() == null) {
//								if (tagihan.getNominalBiaya().getDibayarSebayak().intValue() > 1) {
								final MyCheckboxConfig bukanTagihana = new MyCheckboxConfig("Bukan Tagihan");
								bukanTagihana.setChecked(tag.ambilBukanTagihanData());
								bukanTagihana.setDisabled(!edit);
								bukanTagihana.setStyle("font-size:7px;");
								if (tag.getKunci() == null && tag.getPengaturanBiaya().getKunci() == null) {
									Hbox hbox = new Hbox();
									hbox.setParent(vbox);
									bukanTagihana.setParent(hbox);
									RevisiHelper.createNewRevisi(Tagihan.class, tag, "H").setParent(hbox);
								}
								bukanTagihana.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(final Event event1) throws Exception {

										MyMessageboxConfig.showFormatCb("Apakah Anda yakin item ini {V1} tagihan? Perubahan ini akan memengaruhi status penagihan item tersebut.",
												"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
												MyMessageboxConfig.QUESTION, new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														int i = Integer.parseInt(event.getData().toString());
														if (i == MyMessageboxConfig.OK) {

															Tagihan tagihan = Tagihan.ambilAtauBuat(
																	nominalBiaya.getItemBiayaSekolah(),
																	nominalBiaya.getPengaturanBiaya(),
																	nominalBiaya.getSiswa(),
																	nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya,
																	tahunbulan, pengaturanBiayaItemBiaya, true);

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

//								}
								if (edit) {
									TagihanUtilCalonSiswa.tampilkanKunci(vbox, tag, new EventListener() {

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
								Common.refreshUpdate(tagihan);
								eventListenerHitung.onEvent(arg0);

							}
						});

						row.setValign("top");
						row.setAttribute("nominal", nominal);
						row.setValign("top");
						row.setAttribute("checkbox", checkbox);
					}
				}
				eventListenerHitung.onEvent(null);

			}
		};

		baruEventListener.onEvent(null);

		return grid;

	}

	/** Memeriksa apakah {@code calonSiswa} termasuk target {@code pengaturanBiaya} (lewat {@link #initCriteria(PengaturanBiaya, CalonSiswa, Textbox, PengaturanBiayaItemBiaya, boolean, boolean)}), memakai sesi Hibernate baru. */
	public static boolean apakahAda(PengaturanBiaya pengaturanBiaya, CalonSiswa calonSiswa) {
		return ((Number) initCriteria(pengaturanBiaya, calonSiswa, new Textbox(), null, false, false)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;
	}

	/** Sama seperti {@link #apakahAda(PengaturanBiaya, CalonSiswa)}, tetapi memakai {@code session} yang diberikan bila masih terbuka (fallback ke sesi baru bila tidak). */
	public static boolean apakahAda(Session session, PengaturanBiaya pengaturanBiaya, CalonSiswa calonSiswa) {
		if (session == null || !session.isOpen()) {
			return apakahAda(pengaturanBiaya, calonSiswa);
		}
		return ((Number) initCriteria(session, pengaturanBiaya, calonSiswa, new Textbox(), null, false, false)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;
	}

	/** Membangun kriteria pencarian calon siswa target {@link #pengaturanBiaya} saat ini, memakai filter item biaya dan status "sudah bayar" dari komponen UI helper. */
	public Criteria initCriteria(boolean order) {
		PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) (biayaItem
				.getSelectedItem() == null || biayaItem.getSelectedItem().getValue() == null ? null
						: biayaItem.getSelectedItem().getValue());
		return initCriteria(pengaturanBiaya, calonSiswa, nama, pengaturanBiayaItemBiaya, sudahBayar.isChecked(), order);
	}

	/** Varian statis {@link #initCriteria(Session, PengaturanBiaya, CalonSiswa, Textbox, PengaturanBiayaItemBiaya, boolean, boolean)} yang membuka sesi Hibernate baru. */
	public static Criteria initCriteria(PengaturanBiaya pengaturanBiaya, CalonSiswa calonSiswa, Textbox nama,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean sudahBayar, boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(session, pengaturanBiaya, calonSiswa, nama, pengaturanBiayaItemBiaya, sudahBayar, order);
	}

	/**
	 * Membangun kriteria pencarian {@link CalonSiswa} yang termasuk target {@code pengaturanBiaya}
	 * — lihat javadoc kelas untuk penjelasan lengkap aturan penargetan (gelombang/paket PSB,
	 * daftar siswa spesifik, kelas/kelas banyak/kelas les, tahun angkatan, sekolah, penjurusan).
	 * Selain itu difilter opsional oleh {@code calonSiswa} spesifik, nama/NISN (ILIKE sebagian),
	 * dan status "sudah bayar" (bila {@code sudahBayar} true, dibatasi ke siswa yang punya
	 * {@link Tagihan} dengan {@code pembayaranSiswaDetail} terisi untuk item biaya terkait).
	 * Diurutkan menurut nama lalu id bila {@code order} true.
	 */
	@SuppressWarnings("unchecked")
	public static Criteria initCriteria(Session session, PengaturanBiaya pengaturanBiaya, CalonSiswa calonSiswa,
			Textbox nama, PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, boolean sudahBayar, boolean order) {

		Criteria criteria = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

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
				criteria.add(Restrictions.in("gelombangPendaftaranPsb.id", gelombangPendaftaranPsbs));
			}
		}

		if (pengaturanBiaya.getKhususBuatSiswaTertentu()) {

			List<Long> idcalonSiswas = session.createCriteria(PengaturanBiayaPunyaSiswa.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).add(Restrictions.isNotNull("calonSiswa"))
					.setProjection(Projections.groupProperty("calonSiswa.id")).list();

			criteria = criteria.add(idcalonSiswas.isEmpty() ? Restrictions.sqlRestriction("false")
					: Restrictions.in("id", idcalonSiswas));
		}

		if (pengaturanBiaya.getKelasSiswa() != null) {
			List<Long> idcalonSiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", pengaturanBiaya.getKelasSiswa()))
					.add(Restrictions.isNotNull("calonSiswa")).setProjection(Projections.groupProperty("calonSiswa.id"))
					.list();
			criteria = criteria.add(idcalonSiswas.isEmpty() ? Restrictions.sqlRestriction("false")
					: Restrictions.in("id", idcalonSiswas));
		} else if (pengaturanBiaya.getKelasBanyak() != null && !pengaturanBiaya.getKelasBanyak().trim().isEmpty()) {
			List<String> namaKelas = new ArrayList<String>();
			for (String kelas : pengaturanBiaya.getKelasBanyak().trim().split(",")) {
				if (!kelas.trim().isEmpty()) {
					namaKelas.add(kelas.trim());
				}
			}
			List<Long> idcalonSiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasSiswa").createAlias("siswa", "siswa")
					.add(Restrictions.eq("siswa.aktif", true)).add(Restrictions.eq("kelasSiswa.aktif", true))
					.add(Restrictions.eq("aktif", true))
					.add(namaKelas.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("kelasSiswa.nama", namaKelas))
					.add(Restrictions.eq("kelasSiswa.tahunAjaran", pengaturanBiaya.getTahunAjaran()))
					.add(Restrictions.isNotNull("calonSiswa")).setProjection(Projections.groupProperty("calonSiswa.id"))
					.list();
			criteria = criteria.add(idcalonSiswas.isEmpty() ? Restrictions.sqlRestriction("false")
					: Restrictions.in("id", idcalonSiswas));
		}

		criteria = criteria

				.add(pengaturanBiaya.getKelasLesSiswa() != null ? Restrictions.ilike("kelasLesDipilih",
						"," + pengaturanBiaya.getKelasLesSiswa().getId() + ",", MatchMode.ANYWHERE)
						: Restrictions.sqlRestriction("true"))

				.add(calonSiswa == null ? Restrictions.sqlRestriction("1=1") : Restrictions.idEq(calonSiswa.getId()))

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

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nomorIndukNasional", nama.getValue().trim(), MatchMode.ANYWHERE)));

		if (order) {
			criteria.addOrder(Order.asc("namaSiswa")).addOrder(Order.asc("id"));
		}

		if (sudahBayar) {
			List<Long> siswas = session.createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
					.setProjection(Projections.groupProperty("calonSiswa.id")).add(Restrictions.isNotNull("calonSiswa"))
					.add(pengaturanBiayaItemBiaya == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).list();
			System.out.println("Sudah bayar -> " + siswas);
			criteria.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", siswas));
		}

		return criteria;
	}

	/** Memuat daftar calon siswa target sesuai kriteria/filter bulan saat ini dan menyegarkan grid dengan {@link DetailPARenderer}. Tidak melakukan apa pun bila grid belum diinisialisasi. */
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

		List<CalonSiswa> calonSiswa = ConstantValues.simpleList(initCriteria(true), CalonSiswa.class);

		ListModel strset = new SimpleListModel(calonSiswa);
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
	private PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya;
	private Combobox bulans;
	private MyCheckboxConfig sudahBayar;

	/** Mengembalikan {@code this} sebagai {@link DataLoader}, dipakai untuk meneruskan referensi penyegaran data ke komponen anak. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun tampilan lengkap layar rincian tagihan untuk {@code pengaturanBiaya}: memuat
	 * daftar item biaya terkait dan menjalankan {@link PengaturanBiaya#reloadTagihan} untuk
	 * memastikan baris tagihan sinkron dengan konfigurasi terbaru, lalu toolbar pencarian nama
	 * (atau label tetap bila terikat satu calon siswa), tombol "Singkronkan" (menjalankan ulang
	 * sinkronisasi tagihan dengan indikator progres), filter item biaya/bulan, dan grid matriks
	 * calon siswa &times; item biaya.
	 *
	 * @param pengaturanBiaya konfigurasi paket biaya yang rinciannya ditampilkan
	 * @param component       komponen ZK tempat tata letak dibangun (dibersihkan lebih dulu)
	 */
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
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Calon Siswa : ")));
		nama = new Textbox();
		if (calonSiswa == null) {
			toolbar.appendChild(nama);
		} else {
			toolbar.appendChild(new Label(calonSiswa.getNama()));
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

		MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Singkronkan", "/img/Configure.png");
		buttonTagihan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// 1. Tampilkan Loading Bar di UI
				final Label label = Common.displayLoadBar(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						PengaturanBiaya.reloadTagihan(pengaturanBiaya, true);
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								display(pengaturanBiaya, component);
							}
						});
					}
				});

				// 2. Jalankan proses Sinkronisasi berat di Background Thread
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							// Proses ke database
							TagihanUtilCalonSiswa.doSinkronkanTagihanCalonSiswa(pengaturanBiaya, label, nama, true);

							// Jeda sebentar agar transisi loading terlihat halus
							Thread.sleep(1000);

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						} finally {
							// 3. OPTIMASI FATAL: Selalu bersihkan label di dalam blok finally!
							// Ini menjamin layar user tidak akan hang memunculkan loading terus-menerus
							// jika sewaktu-waktu proses sinkronisasi di atas mengalami error/crash.
							try {
								label.setValue("");
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:1356");
							}
						}
					}
				}).start();
			}

		});
		buttonTagihan.setParent(toolbar);

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
			button = new MyToolbarbuttonConfig("Ambil Calon Siswa", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCalonSiswaForPengaturanBiayaHelper dataCalonSiswaHelper = new AmbilDataCalonSiswaForPengaturanBiayaHelper(
							pengaturanBiaya);
					dataCalonSiswaHelper.display(getDataloader());
				}

			});
			button.setParent(toolbar);
		}

		bulans = null;
		if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Harian")
				&& pengaturanBiaya.getBulanMulai() != null) {
			int tahunMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(0, 4));
			int bulanMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(4));

			int tahunSampai = Integer.parseInt((pengaturanBiaya.getBulanSampai() + "").substring(0, 4));
			int bulanSampai = Integer.parseInt((pengaturanBiaya.getBulanSampai() + "").substring(4));

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
			bulans.setSelectedIndex(sekarang);
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:1570");
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

		else if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")) {
			List<String> columnHeadersAdding = new ArrayList<String>();

			int tahunMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(0, 4));
			int bulanMulai = Integer.parseInt((pengaturanBiaya.getBulanMulai() + "").substring(4));

			int tahunSampai = Integer.parseInt((pengaturanBiaya.getBulanSampai() + "").substring(0, 4));
			int bulanSampai = Integer.parseInt((pengaturanBiaya.getBulanSampai() + "").substring(4));

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

					System.out.println(
							"pembayaranTerakhir => " + pembayaranTerakhir + ", bulanTahunAkhir => " + bulanTahunAkhir);

					columnHeadersAdding.add(pembayaranTerakhir + "-" + pengaturanBiayaItemBiaya.getId() + "-"
							+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama());
					columnHeadersAdding.add("Bukan Tagihan");
					cal.add(Calendar.MONTH, 1);

					bulans.add(pembayaranTerakhir);
				}

			}

			System.out.println("columnHeadersAdding => " + columnHeadersAdding);

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					CalonSiswa calonSiswa = (CalonSiswa) objects[0];

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

							NominalBiaya nominalBiaya = TagihanUtilCalonSiswa
									.ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa, session);

							if (nominalBiaya.getPengaturanBiayaItemBiaya() == null) {
								nominalBiaya.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
								session.getTransaction().begin();
								session.update(nominalBiaya);
								session.getTransaction().commit();
							}

							for (Integer pembayaranTerakhir : bulans) {

								int bayarKe = 1;
								String kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir, nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), bayarKe);

								Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:1714");
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
			mul = (Integer) mybulansMulai.getSelectedItem().getValue();
			sam = (Integer) mybulansSampai.getSelectedItem().getValue();

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(CalonSiswa.class, this, "Download",
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
					CalonSiswa calonSiswa = (CalonSiswa) objects[0];

					try {
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
							NominalBiaya nominalBiaya = TagihanUtilCalonSiswa
									.ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa, session);

							if (nominalBiaya.getPengaturanBiayaItemBiaya() == null) {
								nominalBiaya.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
								session.getTransaction().begin();
								session.update(nominalBiaya);
								session.getTransaction().commit();
							}

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:1846");
						// TODO: handle exception
					}

					HibernateUtil.closeSession();
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(CalonSiswa.class, this,
					"Download Tagihan", "/img/print.png", columnHeadersAdding, dataAdding, false, null, null, contents);
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
							uploadDataCalonSiswa(file, pengaturanBiaya, new EventListener() {

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

				List<CalonSiswa> siswas = ConstantValues.simpleList(initCriteria(true), CalonSiswa.class);
				int rowIndex = 1;
				Session session = HibernateUtil.currentNativeSession();
				for (CalonSiswa calonSiswa : siswas) {
					XSSFRow row = sheet.createRow(rowIndex);

					XSSFCell cell = row.createCell(0);
					cell.setCellValue(calonSiswa.getNoRegistrasi());

					cell = row.createCell(1);
					cell.setCellValue(calonSiswa.getNamaSiswa());

					cell = row.createCell(2);
					cell.setCellValue(calonSiswa.getTahunMasuk() + "");

					cell = row.createCell(3);
					cell.setCellValue(calonSiswa.getSekolah() == null ? "" : calonSiswa.getSekolah().getNama());

					NominalBiaya nominalBiaya = TagihanUtilCalonSiswa.ambilNominalBiaya(pengaturanBiayaItemBiaya,
							calonSiswa, pembayaranTerakhir, session);

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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:2074");

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
		biayaItem.setSelectedIndex(0);
		biayaItem.setReadonly(true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.getPagingChild().setMold("os");
		/*
		 * Pembungkus scroll horizontal — sama seperti DetailTagihanSiswaHelper.
		 * Lebar grid diset piksel tetap di reloadGrid() sehingga bisa lebih lebar
		 * dari sel detail dan terpotong di kanan (Bayar/Kunci/Total). Div ini
		 * membuatnya bisa di-scroll horizontal, bukan terpotong.
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:2123");
		}
	}

	/**
	 * Membangun ulang kolom grid ({@link #columns}/{@link #auxhead}) untuk menampilkan item biaya
	 * {@code pengaturanBiayaItemBiaya}: kolom pertama "Calon Siswa" tetap, diikuti satu atau lebih
	 * kolom periode (bulan-bulan dalam rentang {@link #mul}-{@link #sam} bila item biaya berulang
	 * per bulan, atau satu kolom tunggal bila tidak) sebelum data grid dimuat ulang. Tidak
	 * melakukan apa pun bila {@code pengaturanBiayaItemBiaya} {@code null} (kolom dikosongkan).
	 */
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
		column.setLabel("Calon Siswa");
		column.setStyle("min-width: 300px;");

		int size = 1;

		if (mul != null && sam != null) {
			int banyak = 0;
			for (int m = mul; m <= sam; m++) {
				banyak++;
			}
			grid.setWidth((320 + ((size * banyak) * 100)) + "px");
		} else {
			grid.setWidth((320 + (size * 250)) + "px");
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
				int bulan = Integer.parseInt((m + "").substring(4));
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
				column.setWidth("100px");
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

			column.setWidth("250px");
		}

		loadData(null);
	}

	public void uploadDataCalonSiswa(final File file, final PengaturanBiaya pengaturanBiaya,
			final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");

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
					MyMessageboxConfig.showFormatCb(
							"Upload data calonSiswa berhasil dilakukan.{V1}",
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
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					int mulai = 8;
					try {
						mulai = Integer.parseInt(Common.getKonfigurasi("bulan_mulai_tagihan", "8").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:2307");
						// TODO: handle exception
					}

					if (pengaturanBiaya.getJenisBiayaSekolah().getMulaiDitagihDiBulan() != null) {
						mulai = pengaturanBiaya.getJenisBiayaSekolah().getMulaiDitagihDiBulan();
					}

					final int m = mulai;

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + mulai);
					int tahunSampai = calendar.get(Calendar.YEAR);
					int bulanSampai = calendar.get(Calendar.MONTH);

					final int bulanTahunAkhir = PembayaranSiswa.convert(tahunSampai, bulanSampai);

					/*
					 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
					 * dan getSheetContentAsString/Double/Boolean di dalam loop menutup native session
					 * ThreadLocal (HibernateUtil.closeSession()), sehingga session hasil
					 * currentNativeSession() sudah TERTUTUP saat dipakai -> "Session is closed!".
					 */
					session = HibernateUtil.openSession();
					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							CalonSiswa calonSiswa = (CalonSiswa) Common.getSheetContentAsObject(sheet, 0, i,
									CalonSiswa.class);
							String nomorInduk = Common.getSheetContentAsString(sheet, 1, i);
							if (calonSiswa == null && nomorInduk != null && !nomorInduk.trim().isEmpty()) {
								calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
										.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
										.add(Restrictions.eq("nomorInduk", nomorInduk))
										.add(Restrictions.eq("sekolah", pengaturanBiaya.getSekolah())).setMaxResults(1)
										.addOrder(Order.desc("id")).uniqueResult();
							}
							String nama = Common.getSheetContentAsString(sheet, 2, i);
							if (calonSiswa == null && nama != null && !nama.trim().isEmpty()) {
								calonSiswa = (CalonSiswa) ConstantValues.simpleObject(session
										.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
										.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
										.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT))
										.add(Restrictions.eq("sekolah", pengaturanBiaya.getSekolah())).setMaxResults(1)
										.addOrder(Order.desc("id")), CalonSiswa.class);
							}

							if (calonSiswa != null && calonSiswa.getId() != null) {

								if (pengaturanBiaya.getKhususBuatSiswaTertentu()) {
									int pengaturanBiayaPunyaSiswaCount = ((Number) session
											.createCriteria(PengaturanBiayaPunyaSiswa.class)
											.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
											.add(Restrictions.eq("calonSiswa", calonSiswa))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (pengaturanBiayaPunyaSiswaCount == 0) {
										PengaturanBiayaPunyaSiswa pengaturanBiayaPunyaSiswa = new PengaturanBiayaPunyaSiswa();
										pengaturanBiayaPunyaSiswa.setCalonSiswa(calonSiswa);
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

										nominalBiaya = TagihanUtilCalonSiswa.ambilNominalBiaya(pengaturanBiayaItemBiaya,
												calonSiswa, pembayaranTerakhir, session);

										for (int bayarKe = 1; bayarKe <= 31; bayarKe++) {
											Tagihan tagihan = null;
											String kodeUnik = null;
											boolean ada = dates.contains(bayarKe);
											if (ada) {
												kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
														nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir,
														nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

												tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

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
															.add(Restrictions.eq("calonSiswa", calonSiswa))
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
														tagihan.setCalonSiswa(calonSiswa);
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

												System.out.println("nomorInduk => " + nomorInduk + ", calonSiswa => "
														+ calonSiswa + ", nominal = " + nominal + ", j = " + j
														+ ", item biaya = "
														+ pengaturanBiayaItemBiaya.getItemBiayaSekolah()
														+ ", kodeUnik = " + kodeUnik + ", tagihan = "
														+ tagihan.toString() + " bukanTagihan " + bukanTagihan);
											}

										}

									}

									else if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode()
											.equalsIgnoreCase("Bulanan")) {
										String kodeUnik = NominalBiaya.genCode(
												pengaturanBiayaItemBiaya.getItemBiayaSekolah(), pengaturanBiaya, null,
												calonSiswa);
										nominalBiaya = TagihanUtilCalonSiswa.ambilNominalBiaya(pengaturanBiayaItemBiaya,
												calonSiswa, session);

										Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
										cal.set(Calendar.DATE, 1);
										cal.set(Calendar.MONTH, m - 1);
										int tahun = Calendar.getInstance().get(Calendar.YEAR);
										try {
											tahun = Integer.parseInt(pengaturanBiaya.getTahunAjaran().split("/")[0]);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:2513");

										}

										cal.set(Calendar.YEAR, tahun);

										Integer pembayaranTerakhir = 0;
										while (bulanTahunAkhir > pembayaranTerakhir) {
											int tahunCurrent = cal.get(Calendar.YEAR);
											int bulanCurrent = cal.get(Calendar.MONTH);
											int bulanCurrentPlus = bulanCurrent + 1;
											pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent,
													bulanCurrentPlus);
											if (pengaturanBiaya.getBulanMulai() != null
													&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai()) {
												cal.add(Calendar.MONTH, 1);
												continue;
											}

											if ((pengaturanBiaya.getBulanMulai() != null
													&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai())
													|| (pengaturanBiaya.getBulanSampai() != null
															&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai())) {
												continue;
											}

											if (pengaturanBiaya.getBulanSampai() != null
													&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai()) {
												break;
											}

											int bayarKe = 1;
											kodeUnik = Tagihan.genCode(nominalBiaya.getItemBiayaSekolah(),
													nominalBiaya.getPengaturanBiaya(), pembayaranTerakhir,
													nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(), bayarKe);

											Tagihan tagihan = MemoryDbUtil.getAllTagihan().get(kodeUnik);

											int j = contents.length + index;
											Double nominal = Common.getSheetContentAsDouble(sheet, j, i);
											nominal = nominal == null ? 0.0 : nominal;
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
														.add(Restrictions.eq("calonSiswa", calonSiswa))
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
													tagihan.setBulan(bulanCurrentPlus);
													tagihan.setTahun(tahunCurrent);
													tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
													tagihan.setCalonSiswa(calonSiswa);
													tagihan.setItemBiayaSekolah(
															pengaturanBiayaItemBiaya.getItemBiayaSekolah());
													tagihan.setBayarKe(bayarKe);

													Double n = (nominalBiaya.getItemBiayaSekolah().getBolehDiangsur()
															&& nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah()
																	.getBolehAngsurBerapapun()
																			? (nominal
																					/ nominalBiaya.getDibayarSebayak())
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

											session.getTransaction().begin();
											session.saveOrUpdate(tagihan);
											session.getTransaction().commit();

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

											cal.add(Calendar.MONTH, 1);
										}

									} else {

										nominalBiaya = TagihanUtilCalonSiswa.ambilNominalBiaya(pengaturanBiayaItemBiaya,
												calonSiswa, session);

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
										session.saveOrUpdate(nominalBiaya);
										session.getTransaction().commit();

										System.out.println("nomorInduk => " + nomorInduk + ", calonSiswa => "
												+ calonSiswa + ", nominal = " + nominal + ", j = " + j
												+ ", item biaya = " + pengaturanBiayaItemBiaya.getItemBiayaSekolah()
												+ ", nominalBiaya = " + nominalBiaya.toString() + " bukanTagihan "
												+ bukanTagihan);
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

								label.setValue("Upload data \"" + calonSiswa.getNim() + " - " + calonSiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}

					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailTagihanCalonSiswaHelper.java:2706");
				}

				label.setValue("");
							} finally {
					HibernateUtil.closeSessionQuietly(session);
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

}
