package ais.action.master.sekolah;

import java.util.ArrayList;
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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiTagihanHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.TagihanUtil;

/**
 * Controller/action ZK untuk tagihan. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Textbox searchbulan}, {@code Textbox searchtahun}, {@code Textbox
 * searchitembiaya}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); operasi domain lain ({@code isiComboTahunAjaran()}, {@code
 * autoGenerateTagihanBulanYangHilang()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class TagihanAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchbulan;
	private Textbox searchtahun;
	private Textbox searchitembiaya;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchTahunAjaran;

	private Checkbox searchbelumlunas;
	private Checkbox searchaktif;
	private MyToolbarbuttonConfig find;
	private Siswa selectedSiswa = null;
	private CalonSiswa selectedCalonSiswa = null;
	private Label siswatampil, nimsiswatampil, kelassiswatampil;
	private PengaturanBiaya selectedPengaturanBiaya = null;
	private boolean delete;
	private AmbilDataSiswaBanbox banboxSiswa;
	private AmbilDataCalonSiswaBanbox banboxCalonSiswa;

	public static String[] DATA = new String[] { "id", "siswa.nomorInduk", "siswa.nama", "calonSiswa.nomorInduk",
			"calonSiswa.nama", "bulan", "tahun", "tahunAjaran", "sekolah", "siswa.tahunMasuk", "siswa.statusSiswa.nama",
			"calonSiswa.tahunMasuk", "calonSiswa.statusSiswa.nama", "itemBiayaSekolah.nama", "nominal", "dibayar",
			"tanggalBayar", "tanggalTagihan", "tanggalDeadline", "bayarKe", "kelasSiswa", "diskonSiswa",
			"diskonTidakLangsung", "denda" };

	public static String[] DATA_CALON = new String[] { "id", "calonSiswa.nomorInduk", "calonSiswa.nama", "bulan",
			"tahun", "tahunAjaran", "sekolah", "calonSiswa.tahunMasuk", "calonSiswa.statusSiswa.nama",
			"itemBiayaSekolah.nama", "nominal", "dibayar", "tanggalBayar", "tanggalTagihan", "tanggalDeadline",
			"bayarKe", "kelasSiswa", "diskonSiswa", "diskonTidakLangsung", "denda" };

	public static String[] DATA_SISWA = new String[] { "id", "siswa.nomorInduk", "siswa.nama", "bulan", "tahun",
			"tahunAjaran", "sekolah", "siswa.tahunMasuk", "siswa.statusSiswa.nama", "itemBiayaSekolah.nama", "nominal",
			"dibayar", "tanggalBayar", "tanggalTagihan", "tanggalDeadline", "bayarKe", "kelasSiswa", "diskonSiswa",
			"diskonTidakLangsung", "denda" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(),
					Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa")));
			if (selectedSiswa != null) {
				searchnama.setValue(selectedSiswa.getNomorInduk());
				searchnama.setDisabled(true);

				if (siswatampil != null) siswatampil.setValue("Nama Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNama()));
				if (nimsiswatampil != null) nimsiswatampil.setValue("NIS Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNim()));
				if (kelassiswatampil != null) kelassiswatampil.setValue("Kelas Siswa : " + (selectedSiswa == null ? ""
						: (selectedSiswa.getKelas() == null ? "" : selectedSiswa.getKelas().getNama())));
			}
		} else if (ExecutionsCtrl.getCurrent().getParameter("calon_siswa") != null) {
			selectedCalonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
					Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("calon_siswa")));

			if (selectedCalonSiswa != null) {
				searchnama.setValue(selectedCalonSiswa.getNoRegistrasi());
				searchnama.setDisabled(true);

				siswatampil.setValue(
						"Nama Calon Siswa : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getNama()));
				nimsiswatampil.setValue(
						"Kode Calon Siswa : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getKode()));
				kelassiswatampil.setValue(
						"Sekolah Asal : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getSekolahAsal()));
			}
		} else if (ExecutionsCtrl.getCurrent().getParameter("pengaturanBiaya") != null) {
			selectedPengaturanBiaya = (PengaturanBiaya) ConstantValues.ambil(PengaturanBiaya.class.getName(),
					Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("pengaturanBiaya")));

		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			selectedSiswa = tbmuser.getSiswa();
			if (siswatampil != null) siswatampil.setValue("Nama Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNama()));
			if (nimsiswatampil != null) nimsiswatampil.setValue("NIS Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNim()));
			if (kelassiswatampil != null) kelassiswatampil.setValue("Kelas Siswa : " + (selectedSiswa == null ? ""
					: (selectedSiswa.getKelas() == null ? "" : selectedSiswa.getKelas().getNama())));
		} else if (tbmuser != null && tbmuser.getCalonSiswa() != null) {
			selectedCalonSiswa = tbmuser.getCalonSiswa();

			if (siswatampil != null) siswatampil
					.setValue("Nama Calon Siswa : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getNama()));
			if (nimsiswatampil != null) nimsiswatampil
					.setValue("Kode Calon Siswa : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getKode()));
			if (kelassiswatampil != null) kelassiswatampil.setValue(
					"Sekolah Asal : " + (selectedCalonSiswa == null ? "" : selectedCalonSiswa.getSekolahAsal()));
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		// Wire event listener ke banbox yang sudah di-create oleh ZUL (auto-wired).
		// Auto-generate tagihan bulanan HANYA dipicu saat siswa dipilih interaktif.
		banboxSiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				selectedSiswa = (Siswa) banboxSiswa.getAttribute("siswa");
				selectedCalonSiswa = null;
				if (selectedSiswa != null) {
					autoGenerateTagihanBulanYangHilang();
				}
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		});

		banboxCalonSiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				selectedCalonSiswa = (CalonSiswa) banboxCalonSiswa.getAttribute("calonSiswa");
				selectedSiswa = null;
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		});

		// Jika siswa/calon sudah di-pre-select (via URL param atau tbmuser login),
		// isi banbox sebagai tampilan dan kunci agar tidak bisa diganti.
		// Auto-generate dipanggil langsung karena tidak ada interaksi banbox.
		if (selectedSiswa != null) {
			banboxSiswa.setAttribute("siswa", selectedSiswa);
			banboxSiswa.setValue(selectedSiswa.getNama());
			banboxSiswa.setDisabled(true);
			banboxCalonSiswa.setDisabled(true);
			autoGenerateTagihanBulanYangHilang();
		} else if (selectedCalonSiswa != null) {
			banboxCalonSiswa.setAttribute("calonSiswa", selectedCalonSiswa);
			banboxCalonSiswa.setValue(selectedCalonSiswa.getNama());
			banboxSiswa.setDisabled(true);
			banboxCalonSiswa.setDisabled(true);
		}

		isiComboTahunAjaran();

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, DATA);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_singkronkan_tagihan_siswa"));
		Common.appendKeToolbar(cetakSksDosen, find, comp);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronkan tagihan siswa"));
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Tagihan Siswa");

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								List<Tagihan> tagihans = initCriteria(true).list();

								int i = 0;
								int size = tagihans.size();

								for (Tagihan tagihan : tagihans) {

									String kunci = String.valueOf(tagihan);
									Session session = HibernateUtil.currentNativeSession();
									try {
										session.getTransaction().begin();
										session.update(tagihan);
										session.getTransaction().commit();
										laporan.catatBerhasil(i, kunci, "Sinkronisasi berhasil");
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
										laporan.catatGagalDetail(i, kunci, e);
									}
									HibernateUtil.closeSession();
									if (label != null) {
										label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
												+ " %) singkronkan tagihan siswa  " + tagihan + " ..");
									}
									i++;
								}

															} finally {
									label.setValue("");
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								// System.out.println("process = " +
								// label.getValue());
								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {
									Clients.clearBusy();
									laporan.selesaikan(new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											onSearchDefault(null);
										}
									});
									timer.detach();
								}

							}
						});
						timer.start();

					}
				});
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiTagihanHelper revisiHelper = new RevisiTagihanHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(find.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	class TagihanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Tagihan tagihan = (Tagihan) arg1;

			if (tagihan.getSiswa() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(tagihan.getSiswa()).setParent(hbox);
				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getSiswa().getNomorInduk()));
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getSiswa().getNamaSiswa()));

			} else if (tagihan.getCalonSiswa() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(tagihan.getCalonSiswa()).setParent(hbox);
				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getCalonSiswa().getNomorInduk()));
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getCalonSiswa().getNamaSiswa()));

			}

			Vbox vbox;
			(vbox = RevisiHelper.createNewRevisi(Tagihan.class, tagihan, tagihan.getItemBiayaSekolah().getNama()
					+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "")))
					.setParent(arg0);

			if (tagihan.getSiswa() != null) {
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getSiswa().getSekolah().getNama()));
			} else if (tagihan.getCalonSiswa() != null) {
				vbox.appendChild(new MyLabelAgakKecil(tagihan.getCalonSiswa().getSekolah().getNama()));
			}

			if (Common.isMobile()) {
				new MyLabelAgakKecil(("Bulan : ") + (tagihan.getBulan() == null ? "" : tagihan.getBulan().toString()))
						.setParent(vbox);
				new MyLabelAgakKecil(("Tahun : ") + (tagihan.getTahun() == null ? "" : tagihan.getTahun().toString()))
						.setParent(vbox);
				new MyLabelAgakKecil(("Lunas : ") + (tagihan.getPembayaranSiswaDetail() == null ? "Belum" : "Ya"))
						.setParent(vbox);
				new MyLabelAgakKecil("Nominal : " + Common.numberFormat.get().format(tagihan.getNominal())).setParent(vbox);
				new MyLabelAgakKecil("Denda : " + Common.numberFormat.get().format(tagihan.getDenda())).setParent(vbox);
				new MyLabelAgakKecil(tagihan.getInformasi()).setParent(vbox);

			}

			new Label(tagihan.getBulan() == null ? "" : tagihan.getBulan().toString()).setParent(arg0);
			new Label(tagihan.getTahun() == null ? "" : tagihan.getTahun().toString()).setParent(arg0);
			new Label(tagihan.getNominalBiaya() == null || tagihan.getPengaturanBiaya() == null
					|| tagihan.getPengaturanBiaya().getTahunAjaran() == null ? ""
							: tagihan.getPengaturanBiaya().getTahunAjaran())
					.setParent(arg0);
			new Label(tagihan.getPembayaranSiswaDetail() == null ? "Belum" : "Ya").setParent(arg0);
			new Label(Common.numberFormat.get().format(tagihan.getNominal())).setParent(arg0);
			new Label(Common.numberFormat.get().format(tagihan.getDenda())).setParent(arg0);
			new Label(tagihan.getInformasi()).setParent(arg0);

			if (Common.getApakahAdmin() && tagihan.getPembayaranSiswaDetail() == null
					&& !tagihan.getNominalBiaya().getBukanTagihan()) {

				vbox = new Vbox();
				vbox.setParent(arg0);

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
				checkbox.setChecked(tagihan.getAktif());
				checkbox.setParent(vbox);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final boolean cek = checkbox.isChecked();
						// Muat ulang instance MANAGED di sesi request ini — objek 'tagihan' dari
						// render sebelumnya sudah detached, sehingga update langsung bisa gagal
						// diam-diam dan uncheck "tidak nempel".
						Session s = HibernateUtil.currentSession();
						Tagihan t = tagihan.getId() == null ? tagihan
								: (Tagihan) s.get(Tagihan.class, tagihan.getId());
						if (t == null) t = tagihan;
						t.setAktif(cek);
						t.setAktifkanmanual(cek);   // true = paksa AKTIF
						t.setNonaktifManual(!cek);  // true = paksa NON-AKTIF (tagihan revisi keliru)
						Common.refreshSaveOrUpdate(s, t);
						try { s.flush(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/TagihanAction.java:424"); }
					}
				});

			} else {
				new Label((tagihan.getAktif() ? "Ya" : "Tidak")).setParent(arg0);
			}
			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (tagihan.getPembayaranSiswaDetail() != null && tagihan.getPembayaranSiswaDetail().getId() != null
					&& !tagihan.getNominalBiaya().getBukanTagihan()) {
				final Tagihan tag = tagihan;
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Pindah Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin memindahkan pembayaran siswa ini ? ", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Tagihan.pindahkan(tag, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
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
				aksiButtons.add(button);
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(tagihan);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tagihan.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (selectedPengaturanBiaya != null) {
			criteria.createAlias("nominalBiaya", "nominalBiaya")
					.add(selectedPengaturanBiaya == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("nominalBiaya.pengaturanBiaya", selectedPengaturanBiaya));
		}

		if (order)
			criteria.addOrder(Order.asc("tahunbulan")).addOrder(Order.asc("pembayaranSiswaDetail"));
		criteria

				.add(selectedSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa", selectedSiswa))
				.add(selectedCalonSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("calonSiswa", selectedCalonSiswa))

				.add(searchbulan.getValue().trim().isEmpty() || !Common.isNumber(searchbulan.getValue().trim())
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("bulan", ((int) Double.parseDouble(searchbulan.getValue().trim()))))

				.add(searchtahun.getValue().trim().isEmpty() || !Common.isNumber(searchtahun.getValue().trim())
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", ((int) Double.parseDouble(searchtahun.getValue().trim()))))

				.add(searchTahunAjaran == null || searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null
						|| ((String) searchTahunAjaran.getSelectedItem().getValue()).trim().isEmpty()
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAjaran",
										((String) searchTahunAjaran.getSelectedItem().getValue()).trim()))

				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.createAlias("itemBiayaSekolah", "itemBiayaSekolah", Criteria.LEFT_JOIN)

				.add(searchitembiaya.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("itemBiayaSekolah.kode", searchitembiaya.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("itemBiayaSekolah.nama", searchitembiaya.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("siswa.nomorInduk", searchnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("siswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("calonSiswa.noRegistrasi", searchnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("calonSiswa.nama", searchnama.getValue().trim(),
												MatchMode.ANYWHERE)))

				)

				.add(searchbelumlunas == null ? Restrictions.sqlRestriction("1=1")
						: (searchbelumlunas.isChecked()
								? Restrictions.or(
										Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
												Restrictions.sqlRestriction(
														"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
										Restrictions.isNull("pembayaranSiswaDetail"))
								: Restrictions.sqlRestriction("true")))

				.add(selectedSiswa != null || selectedCalonSiswa != null ? Restrictions.sqlRestriction("true")
						: searchsekolah == null || searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null
								|| searchsekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("itemBiayaSekolah.sekolah", searchsekolah, false))

				.add(selectedSiswa != null || selectedCalonSiswa != null ? Restrictions.sqlRestriction("true")
						: searchyayasan == null || searchyayasan.getSelectedItem() == null
								|| searchyayasan.getSelectedItem().getValue() == null
								|| searchyayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("itemBiayaSekolah.yayasan", searchyayasan, false));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		return criteria;
	}

	/**
	 * Isi combobox filter "Tahun Ajaran" dengan nilai UNIK dari Tagihan (mis. 2026/2027),
	 * diurut terbaru dahulu, plus opsi "-- Semua --" (value null = tanpa filter) sebagai default.
	 */
	private void isiComboTahunAjaran() {
		if (searchTahunAjaran == null) {
			return;
		}
		searchTahunAjaran.getItems().clear();

		org.zkoss.zul.Comboitem semua = new org.zkoss.zul.Comboitem("-- Semua --");
		semua.setValue(null);
		searchTahunAjaran.appendChild(semua);

		try {
			List<?> tahunAjarans = HibernateUtil.currentSession().createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("tahunAjaran"))
					.setProjection(org.hibernate.criterion.Projections
							.distinct(org.hibernate.criterion.Projections.property("tahunAjaran")))
					.addOrder(Order.desc("tahunAjaran")).list();
			for (Object o : tahunAjarans) {
				if (o == null || o.toString().trim().isEmpty()) {
					continue;
				}
				org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(o.toString());
				ci.setValue(o.toString());
				searchTahunAjaran.appendChild(ci);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// "-- Semua --" selalu ada -> aman memilih indeks 0 (tak akan out-of-bound).
		searchTahunAjaran.setSelectedIndex(0);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Tagihan> tagihan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tagihan);
		grid.setRowRenderer(new TagihanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Generate tagihan bulanan yang belum ada untuk siswa ini, berdasarkan
	 * PengaturanBiaya yang aktif di sekolahnya. Idempoten: aman dipanggil berulang.
	 */
	@SuppressWarnings("unchecked")
	private void autoGenerateTagihanBulanYangHilang() {
		if (selectedSiswa == null || selectedSiswa.getSekolah() == null) return;

		// Muat semua PengaturanBiaya Bulanan untuk sekolah siswa ini menggunakan
		// sesi dedikasi (bukan currentNativeSession) agar tidak terganggu oleh
		// closeSession() yang dipanggil di dalam doGenerateTagihanBulanan().
		// createAlias("jenisBiayaSekolah") memaksa JOIN sehingga asosiasi lazy
		// ter-inisialisasi sebelum sesi ditutup.
		List<PengaturanBiaya> daftarPb = new ArrayList<PengaturanBiaya>();
		Session sessionQuery = HibernateUtil.openSession();
		try {
			daftarPb = ConstantValues.simpleList(
				sessionQuery.createCriteria(PengaturanBiaya.class)
					.createAlias("jenisBiayaSekolah", "jbs")
					.add(Restrictions.eq("jbs.periode", "Bulanan"))
					.add(Restrictions.eq("sekolah", selectedSiswa.getSekolah()))
					.add(Restrictions.isNotNull("bulanSampai")),
				PengaturanBiaya.class);
		} catch (Exception e) {
			return;
		} finally {
			try { sessionQuery.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sekolah/TagihanAction.java:682");}
		}

		for (PengaturanBiaya pb : daftarPb) {
			try {
				if (pb.getBulanSampai() == null) continue;

				int mulai = TagihanUtil.getBulanMulai(pb.getJenisBiayaSekolah());

				// Tambah 1 bulan pada bulanSampai — loop doGenerateTagihanBulanan
				// pakai kondisi strict ">" sehingga bulan terakhir harus dilewati 1.
				int bs = pb.getBulanSampai();
				int bsMonth = bs % 100;
				int bsYear = bs / 100;
				int tahunSampai, bulanSampai;
				if (bsMonth < 12) {
					tahunSampai = bsYear;
					bulanSampai = bsMonth + 1;
				} else {
					tahunSampai = bsYear + 1;
					bulanSampai = 1;
				}

				TagihanUtil.doGenerateTagihanBulanan(selectedSiswa, pb, tahunSampai, bulanSampai, mulai);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/TagihanAction.java:706");
				// Lanjut ke PengaturanBiaya berikutnya; jangan biarkan satu gagal
				// menghentikan seluruh proses
			}
		}
	}

}
