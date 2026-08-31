package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.SertifikatAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.Html2Text;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK yang menampilkan dan mengelola daftar dosen peserta satu
 * {@link KegiatanKedosenan}, lewat relasi {@link KegiatanKedosenanPunyaDosen}. Melengkapi
 * {@link DosenPunyaKegiatanKedosenanHelper} (yang berpusat pada satu dosen) dengan sudut pandang
 * sebaliknya: satu kegiatan, banyak dosen. Grid menampilkan identitas dosen (NIP/NIDN, nama, ikatan
 * kerja, status kepegawaian, jurusan), rentang tanggal, jabatan/skala kegiatan, keterangan, status
 * persetujuan (checkbox bagi atasan langsung dosen atau admin non-dosen, label read-only bagi
 * lainnya), dan tombol Sertifikat/Hapus per baris — logika rendering baris bersama dipisah ke
 * method statis {@link #displayRow} agar dapat dipakai ulang dari konteks lain (mis. tab penilaian
 * asesor BKD, ditandai parameter {@code ases}).
 *
 * <p>
 * Toolbar menyediakan pencarian NIDN/nama/fakultas/jurusan, "Ambil Dosen" (membuka
 * {@code AmbilDataDosenForKegiatanKedosenanHelper}), "Bersihkan" (hapus SQL langsung seluruh
 * relasi yang belum disetujui untuk kegiatan ini), unduh/unggah data Excel, dan — bila fitur
 * repository DSpace aktif ({@code terhubung_ke_dspace} + {@code kegiatan_dosen_terhubung_ke_dspace})
 * — "Ekspor" dan "Batalkan Ekspor" yang memproses seluruh relasi yang sudah disetujui secara
 * asinkron di thread terpisah dengan progress bar ({@link Common#displayLoadBar}), memanggil
 * {@link #getDspace} untuk membangun metadata Dublin Core dan mengunggah item+lampiran ke koleksi
 * DSpace yang sesuai fakultas/jurusan dosen (koleksi/komunitas dicari-atau-dibuat otomatis lewat
 * {@link #getDspaceTipeKegiatanKedosenanPunyaDosen}/{@link #getDspaceTipeKegiatanKedosenanPunyaDosenJurusan}).
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader}, {@link DataCriteria}, dan {@link DataSearchDefault}.
 * </p>
 */
public class KegiatanKedosenanPunyaDosenHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private KegiatanKedosenan kegiatanKedosenan;
	private Textbox nim;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	/** Membuat helper: menginisialisasi combobox fakultas/jurusan dan komponen paging server-side. */
	public KegiatanKedosenanPunyaDosenHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	/**
	 * Merender satu baris detail {@link KegiatanKedosenanPunyaDosen} ke {@code arg0}, dipakai baik
	 * oleh {@link DetailKegiatanKedosenanRenderer} kelas ini maupun konteks lain (mis. tab penilaian
	 * asesor BKD bila {@code ases} bernilai {@code true} dan {@code pegawai} diberikan — dalam mode
	 * ini area detail berisi tab "Penilaian Asesor" ({@link PenilaianAsesorHelper#formNilai}) dan
	 * "Lampiran Kegiatan Dosen" dimuat lazy, alih-alih langsung menampilkan area unggah bukti).
	 * Menampilkan riwayat revisi, identitas dosen, kontrol edit (jabatan/skala/tanggal/keterangan,
	 * dikunci setelah disetujui), checkbox/label persetujuan (checkbox hanya untuk atasan langsung
	 * dosen atau admin non-dosen saat kegiatan berstatus {@link KegiatanKedosenan#DISETUJUI}), dan
	 * tombol aksi Sertifikat/Hapus.
	 *
	 * @param arg0                  baris grid tujuan render
	 * @param kegiatanKedosenanPunyaDosen relasi dosen-kegiatan yang dirender
	 * @param pegawai               pegawai konteks penilaian asesor, boleh {@code null}
	 * @param ases                  bila {@code true}, tampilkan tab penilaian asesor alih-alih area
	 *                              unggah langsung
	 * @param delete                izin tampil tombol hapus
	 * @param deleteEventListener   callback dipanggil setelah baris berhasil dihapus
	 */
	public static void displayRow(Row arg0, final KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen,
			final Pegawai pegawai, final Boolean ases, final Boolean delete, final EventListener deleteEventListener)
			throws Exception {

		final MyDetail detail = new MyDetail();
		detail.setParent(arg0);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					if (ases && pegawai != null) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
						tabSoal.setParent(tabs);

						final MyTabConfig tabPengajaran = new MyTabConfig("Lampiran Kegiatan Dosen");
						tabPengajaran.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setStyle("min-height: 300px;");
						tabpanelUtama.setParent(tabpanels);

						PenilaianAsesorHelper
								.formNilai(pegawai, "kegiatanKedosenanPunyaDosen", kegiatanKedosenanPunyaDosen, null,
										kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getTahunAkademik(),
										kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getJenisSemester(),
										"Kegiatan dosen \""
												+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama() + "\"",
										PenilaianAsesor.KEGIATAN_DOSEN, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

											}
										})
								.setParent(tabpanelUtama);

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setStyle("min-height: 1100px;");
						jurusanTabpanel.setWidth("100%");

						tabPengajaran.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									Vbox vbox = new Vbox();
									vbox.setParent(jurusanTabpanel);
									Hbox hbox = new Hbox();
									LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenanPunyaDosen.getId(),
											KegiatanKedosenanPunyaDosen.class.getName(), "Bukti Kegiatan Dosen", false,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, true);
									hbox.setParent(vbox);

									hbox = new Hbox();
									LampiranLain.createDownloadUploadFileLain(hbox,
											kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getId(),
											KegiatanKedosenan.class.getName(), "Lampiran Kegiatan", false,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											});
									hbox.setParent(vbox);
								}
							}
						});

					} else {
						Vbox vbox = new Vbox();
						vbox.setParent(detail);
						Hbox hbox = new Hbox();

						LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenanPunyaDosen.getId(),
								KegiatanKedosenanPunyaDosen.class.getName(), "Bukti Kegiatan Dosen", false,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, null, false, false, false, true);

						hbox.setParent(vbox);
					}

				}

			}
		};

		detail.addEventListener("onOpen", eventListener);

		detail.setOpen(true);
		if (ases) {
			eventListener.onEvent(null);
		} else {
			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenanPunyaDosen.getId(),
					KegiatanKedosenanPunyaDosen.class.getName(), "Bukti Kegiatan Dosen", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);

			hbox.setParent(vbox);
		}

		RevisiHelper.createNewRevisi(KegiatanKedosenanPunyaDosen.class, kegiatanKedosenanPunyaDosen,
				kegiatanKedosenanPunyaDosen.getDosen().getCode() + " "
						+ kegiatanKedosenanPunyaDosen.getDosen().getNidn())
				.setParent(arg0);

		new Label(kegiatanKedosenanPunyaDosen.getDosen().getNama()).setParent(arg0);

		new Label(kegiatanKedosenanPunyaDosen.getDosen().getIkatanKerjaDosen() == null ? ""
				: kegiatanKedosenanPunyaDosen.getDosen().getIkatanKerjaDosen().getNama()).setParent(arg0);

		new Label(kegiatanKedosenanPunyaDosen.getDosen().getStatusKepegawaian() == null ? ""
				: kegiatanKedosenanPunyaDosen.getDosen().getStatusKepegawaian().getNama()).setParent(arg0);

		new Label(kegiatanKedosenanPunyaDosen.getDosen().getJurusan() == null ? ""
				: kegiatanKedosenanPunyaDosen.getDosen().getJurusan().getNama() + "").setParent(arg0);

		DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) HibernateUtil
				.currentSession().createCriteria(DetailKelompokKegiatanKedosenan.class)
				.add(Restrictions.idEq(kegiatanKedosenanPunyaDosen.getKegiatanKedosenan()
						.getDetailKelompokKegiatanKedosenan().getId()))
				.uniqueResult();
		ArrayList<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new ArrayList<JabatanKegiatanKedosenan>(
				detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans());
		ArrayList<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new ArrayList<SkalaKegiatanKedosenan>(
				detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans());

		Collections.sort(jabatanKegiatanKedosenans);
		Collections.sort(skalaKegiatanKedosenans);

		final MyTextbox keterangan = new MyTextbox(kegiatanKedosenanPunyaDosen.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		final MyDatebox mulai = new MyDatebox(kegiatanKedosenanPunyaDosen.getMulai());
		mulai.setWidth("90%");
		final MyDatebox sampai = new MyDatebox(kegiatanKedosenanPunyaDosen.getSampai());
		sampai.setWidth("90%");

		mulai.setParent(arg0);
		sampai.setParent(arg0);

		final Combobox jabatanKegiatanKedosenan = new Combobox();
		jabatanKegiatanKedosenan.setVisible(!jabatanKegiatanKedosenans.isEmpty());
		Common.insertComboItems(jabatanKegiatanKedosenan, "nama", jabatanKegiatanKedosenans);
		Common.selectComboItem(true, jabatanKegiatanKedosenan,
				kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan());
		jabatanKegiatanKedosenan.setParent(arg0);
		jabatanKegiatanKedosenan.setReadonly(true);
		jabatanKegiatanKedosenan.setWidth("97%");

		final Combobox skalaKegiatanKedosenan = new Combobox();
		skalaKegiatanKedosenan.setVisible(!skalaKegiatanKedosenans.isEmpty());
		Common.insertComboItems(skalaKegiatanKedosenan, "nama", skalaKegiatanKedosenans);
		Common.selectComboItem(true, skalaKegiatanKedosenan, kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan());
		skalaKegiatanKedosenan.setParent(arg0);
		skalaKegiatanKedosenan.setReadonly(true);
		skalaKegiatanKedosenan.setWidth("97%");

		eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kegiatanKedosenanPunyaDosen.setMulai(mulai.getValue());
				kegiatanKedosenanPunyaDosen.setSampai(sampai.getValue());
				kegiatanKedosenanPunyaDosen.setSkalaKegiatanKedosenan(
						(SkalaKegiatanKedosenan) (skalaKegiatanKedosenan.getSelectedItem() == null ? null
								: skalaKegiatanKedosenan.getSelectedItem().getValue()));
				kegiatanKedosenanPunyaDosen.setKeterangan(keterangan.getValue());
				kegiatanKedosenanPunyaDosen.setJabatanKegiatanKedosenan(
						((JabatanKegiatanKedosenan) (jabatanKegiatanKedosenan.getSelectedItem() == null ? null
								: jabatanKegiatanKedosenan.getSelectedItem().getValue())));
				Common.refreshUpdate(kegiatanKedosenanPunyaDosen);

			}
		};

		skalaKegiatanKedosenan.addEventListener("onChange", eventListener);
		jabatanKegiatanKedosenan.addEventListener("onChange", eventListener);
		keterangan.addEventListener("onChange", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);
		keterangan.setParent(arg0);

		jabatanKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
		skalaKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
		keterangan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
		mulai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
		sampai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());

		Tbmuser tbmuser = Common.getCurrentUser();

		final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
				"/img/certificate-icon.png");
		cetakToolbarbuttonSertifikat.setOrient("vertical");
		final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		deleteButton.setOrient("vertical");

		cetakToolbarbuttonSertifikat.setVisible(kegiatanKedosenanPunyaDosen.getPersetujuan()
				&& kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null);

		Dosen dsn = kegiatanKedosenanPunyaDosen.getDosen();

		final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
				new java.util.ArrayList<org.zkoss.zk.ui.Component>();
		deleteButton.setVisible(!kegiatanKedosenanPunyaDosen.getPersetujuan() && !ases);
		if ((dsn != null && dsn.yangLoginMerupakanAtasan())
				|| (!ases && tbmuser.ambilDosen() == null && kegiatanKedosenanPunyaDosen.getKegiatanKedosenan()
						.getStatus().equals(KegiatanKedosenan.DISETUJUI))) {
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
			checkbox.setChecked(kegiatanKedosenanPunyaDosen.getPersetujuan());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanKedosenanPunyaDosen.setPersetujuan(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kegiatanKedosenanPunyaDosen);
					deleteButton.setVisible(!kegiatanKedosenanPunyaDosen.getPersetujuan());

					cetakToolbarbuttonSertifikat.setVisible(kegiatanKedosenanPunyaDosen.getPersetujuan()
							&& kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null);

					jabatanKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
					skalaKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
					keterangan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
					mulai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
					sampai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
				}
			});
		} else {
			Label label;
			(label = new Label(
					kegiatanKedosenanPunyaDosen.getPersetujuan() == null || kegiatanKedosenanPunyaDosen.getPersetujuan()
							? "Ya"
							: "Belum")).setParent(arg0);
			label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
			label.setParent(arg0);
		}

		cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SertifikatAction.cetakSertifikat(kegiatanKedosenanPunyaDosen);
			}
		});
		aksiButtons.add(cetakToolbarbuttonSertifikat);

		deleteButton.setOrient("vertical");
		deleteButton.setVisible(delete);
		deleteButton.setTooltiptext("Hapus Data");
		deleteButton.addEventListener("onClick", new EventListener() {
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

										Common.refreshDelete(kegiatanKedosenanPunyaDosen);
										deleteEventListener.onEvent(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		aksiButtons.add(deleteButton);

		ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

	}

	/** Perender baris grid; mendelegasikan seluruhnya ke {@link #displayRow} dalam mode non-asesor, dengan izin hapus sesuai hak akses {@link CommonPrivilages#DELETE} user saat ini. */
	class DetailKegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailKegiatanKedosenanRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) data;

			KegiatanKedosenanPunyaDosenHelper.displayRow(row, kegiatanKedosenanPunyaDosen, null, false, delete,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					});

		}

	}

	/**
	 * Membangun kriteria {@link KegiatanKedosenanPunyaDosen} milik {@link #kegiatanKedosenan} saat
	 * ini, disaring berdasarkan jurusan/fakultas dosen dan NIDN/nama dosen (ilike).
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menaik berdasarkan nama dosen
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKedosenanPunyaDosen.class);

		criteria.createAlias("dosen", "dosen")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("dosen.fakultas", searchfakultas, false))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("dosen.nidn", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("dosen.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan));

		if (order)
			criteria.addOrder(Order.asc("dosen.nama"));

		return criteria;
	}

	/**
	 * Memuat ulang grid secara asinkron: menghitung total baris untuk paging, lalu mengambil satu
	 * halaman {@link KegiatanKedosenanPunyaDosen} sesuai kriteria pencarian saat ini.
	 *
	 * @param value tidak digunakan; ada untuk memenuhi kontrak {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKedosenanPunyaDosen> myKegiatanKedosenanPunyaDosens = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myKegiatanKedosenanPunyaDosens);
				grid.setRowRenderer(new DetailKegiatanKedosenanRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	/** @return referensi ke helper ini sendiri, dipakai sebagai {@link DataLoader} oleh helper penambah data. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun dan menampilkan UI daftar dosen peserta {@code kegiatanKedosenan} ke dalam
	 * {@code component}: toolbar pencarian dan aksi (Ambil Dosen, Bersihkan, unduh/unggah Excel,
	 * dan bila fitur DSpace aktif: Ekspor/Batalkan Ekspor), lalu grid ber-paging di dalam area
	 * scroll tetap (60vh). Fakultas/jurusan pencarian otomatis dipilihkan sesuai
	 * {@code kegiatanKedosenan} bila ada.
	 *
	 * @param kegiatanKedosenan kegiatan yang daftar dosennya dikelola
	 * @param component         komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param window            window pemanggil, diteruskan ke helper "Ambil Dosen"
	 */
	public void display(final KegiatanKedosenan kegiatanKedosenan, final Component component, final MyWindow window) {
		this.kegiatanKedosenan = kegiatanKedosenan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar dosen yang mengikuti  " + kegiatanKedosenan.getNama()));

		final boolean mobileTampil = Common.isMobile();
		org.zkoss.zk.ui.HtmlBasedComponent toolbar;
		if (mobileTampil) {
			org.zkoss.zul.Div barMobile = new org.zkoss.zul.Div();
			barMobile.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px 4px;width:100%;box-sizing:border-box;");
			toolbar = barMobile;
		} else {
			toolbar = new Toolbar();
		}
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIDN : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setCols(10);
		nim.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchfakultas, kegiatanKedosenan.getFakultas());
//		if (kegiatanKedosenan.getFakultas() != null) {
//			searchfakultas.setDisabled(true);
//		}

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (kegiatanKedosenan.getJurusan() != null) {
			Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
					|| searchfakultas.getSelectedItem().getValue() == null
					|| searchfakultas.getSelectedItem().getValue() == null ? null
							: searchfakultas.getSelectedItem().getValue());
			if (selectedFakultas != null) {
				Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
						Jurusan.class, Restrictions.eq("fakultas", selectedFakultas));
				Common.selectComboItem(searchjurusan, kegiatanKedosenan.getJurusan());
//				searchjurusan.setDisabled(true);
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Dosen", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataDosenForKegiatanKedosenanHelper dataDosenHelper = new AmbilDataDosenForKegiatanKedosenanHelper(
						kegiatanKedosenan);
				dataDosenHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from kegiatan_kedosenan_punya_dosen where (persetujuan is null or persetujuan = false) and kegiatan_kedosenan = "
														+ kegiatanKedosenan.getId())
												.executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Bukti");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				class DataAddingHelper {
					public void process(XSSFRow row, int index, KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen,
							String jenis) throws Exception {
						LampiranLain lam = LampiranLain.ambil(kegiatanKedosenanPunyaDosen.getId(), jenis);

						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}

					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 9, kegiatanKedosenanPunyaDosen,
						KegiatanKedosenanPunyaDosen.class.getName());

			}
		};

		String[] contents = new String[] { "id", "kegiatanKedosenan", "dosen", "mulai", "sampai",
				"jabatanKegiatanKedosenan", "skalaKegiatanKedosenan", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KegiatanKedosenanPunyaDosen.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKedosenanPunyaDosen.class, contents);
		upload.setVisible(Common.getApakahAdmin() || Common.getApakahAdminLain());
		toolbar.appendChild(upload);

		// SCROLL (pola Center->Grid->Rows->Row): grid dibungkus Borderlayout -> Center(autoscroll)
		// dgn tinggi terikat agar baris banyak / tabel lebar memunculkan scrollbar. Caption+toolbar
		// tetap di luar borderlayout (hindari North-collapse ZK5.5).
		ais.ui.util.MyBorderlayout blScroll = new ais.ui.util.MyBorderlayout();
		blScroll.setHeight("60vh");
		blScroll.setWidth("100%");
		blScroll.setStyle("min-height:280px;");
		blScroll.setParent(groupbox);
		org.zkoss.zul.Center centerScroll = new org.zkoss.zul.Center();
		centerScroll.setBorder("none");
		centerScroll.setAutoscroll(true);
		centerScroll.setParent(blScroll);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP/NIDN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ikatan Kerja");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan/Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skala");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		loadData(null);

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_dosen_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, khusus untuk kegiatan dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<KegiatanKedosenanPunyaDosen> kegiatanKedosenanPunyaDosens = initCriteria(true)
									.add(Restrictions.eq("persetujuan", true)).list();
							intbox.setValue(kegiatanKedosenanPunyaDosens.size());

							int rowIndex = 1;
							for (KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen : kegiatanKedosenanPunyaDosens) {
								label.setValue(
										"Sedang memproses data " + kegiatanKedosenanPunyaDosen.toString() + " ("
												+ Common.numberFormat.get().format(
														(rowIndex++) * 100.0 / kegiatanKedosenanPunyaDosens.size())
												+ " %)");
								KegiatanKedosenanPunyaDosenHelper.getDspace(cookie, kegiatanKedosenanPunyaDosen, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_dosen_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<KegiatanKedosenanPunyaDosen> kegiatanKedosenanPunyaDosens = initCriteria(
														true).add(Restrictions.isNotNull("dosen.jurusan"))
																.add(Restrictions.eq("persetujuan", true)).list();

												int rowIndex = 1;
												for (KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen : kegiatanKedosenanPunyaDosens) {
													label.setValue(
															"Sedang memproses data "
																	+ kegiatanKedosenanPunyaDosen.toString() + " ("
																	+ Common.numberFormat.get().format((rowIndex++) * 100.0
																			/ kegiatanKedosenanPunyaDosens.size())
																	+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(
																	KegiatanKedosenanPunyaDosen.class.getName(),
																	kegiatanKedosenanPunyaDosen.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
	}

	/**
	 * Membangun metadata Dublin Core (penulis, tanggal, judul, subjek, penerbit, URI, dst.) dari
	 * satu {@link KegiatanKedosenanPunyaDosen} dan mengirimkannya ke DSpace via
	 * {@link DspaceInformation#dspaceProcess} — item dibuat pada koleksi yang sesuai
	 * fakultas/jurusan dosen (dicari-atau-dibuat via {@link #getDspaceTipeKegiatanKedosenanPunyaDosen}).
	 * Bila ada lampiran bukti kegiatan tersimpan, lampiran tersebut ikut diunggah sebagai berkas
	 * item DSpace.
	 *
	 * @param cookie                        sesi login DSpace aktif
	 * @param kegiatanKedosenanPunyaDosen    relasi dosen-kegiatan yang akan diekspor
	 * @param update                         bila {@code true}, perbarui item DSpace yang sudah ada; bila
	 *                                       {@code false}, buat item baru
	 * @return informasi item DSpace yang dibuat/diperbarui (termasuk UUID)
	 */
	public static DspaceInformation getDspace(String cookie, KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen,
			boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (kegiatanKedosenanPunyaDosen.getDosen() != null) {
			nama = kegiatanKedosenanPunyaDosen.getDosen().getNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(kegiatanKedosenanPunyaDosen.getKeterangan()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama());
		jsonArray.put(jsonMetadata);

		if (kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getTempat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getKode());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", kegiatanKedosenanPunyaDosen.getDosen().getBahasa());
		jsonArray.put(jsonMetadata);

		if (kegiatanKedosenanPunyaDosen.getMulai() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(kegiatanKedosenanPunyaDosen.getMulai()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(kegiatanKedosenanPunyaDosen.getId(),
				KegiatanKedosenanPunyaDosen.class.getName());
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, kegiatanKedosenanPunyaDosen,
				jsonPost.toString(), jsonArray.toString(), update, "items", "collections/"
						+ getDspaceTipeKegiatanKedosenanPunyaDosen(cookie, kegiatanKedosenanPunyaDosen) + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Sertifikat / Lampiran Bukti Ikut Kegiatan");
		}

		return dspaceInformation;
	}

	/**
	 * Mencari-atau-membuat koleksi DSpace "Prestasi Dosen" khusus untuk jurusan dosen pemilik
	 * {@code kegiatanKedosenanPunyaDosen}, di dalam komunitas jurusan yang dikembalikan
	 * {@link #getDspaceTipeKegiatanKedosenanPunyaDosenJurusan}. UUID koleksi di-cache pada
	 * {@link Konfigurasi} bernama {@code dspace_label_collection_kegiatanKedosenanPunyaDosen_jurusan_<idKegiatan>_<idJurusan>}.
	 *
	 * @param cookie                       sesi login DSpace aktif
	 * @param kegiatanKedosenanPunyaDosen   relasi dosen-kegiatan penentu jurusan/koleksi tujuan
	 * @return informasi koleksi DSpace yang dipakai/dibuat
	 */
	public static DspaceInformation getDspaceTipeKegiatanKedosenanPunyaDosen(String cookie,
			KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) throws Exception {

		String description = "Kegiatan dosen yang berupa "
				+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama() + " pada kelompok "
				+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getDetailKelompokKegiatanKedosenan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Kegiatan Dosen " + kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKedosenanPunyaDosen_jurusan_"
						+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getId() + "_"
						+ kegiatanKedosenanPunyaDosen.getDosen().getJurusan().getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), true, "collections",
				"communities/" + getDspaceTipeKegiatanKedosenanPunyaDosenJurusan(cookie, kegiatanKedosenanPunyaDosen)
						+ "/collections");
	}

	/**
	 * Mencari-atau-membuat komunitas DSpace "Prestasi Dosen" untuk jurusan dosen pemilik
	 * {@code kegiatanKedosenanPunyaDosen}, di dalam komunitas jurusan (level lebih tinggi)
	 * milik {@link JurusanAction#getDspace}. UUID komunitas di-cache pada {@link Konfigurasi}
	 * bernama {@code dspace_label_collection_kegiatanKedosenanPunyaDosen_<idJurusan>}.
	 *
	 * @param cookie                       sesi login DSpace aktif
	 * @param kegiatanKedosenanPunyaDosen   relasi dosen-kegiatan penentu jurusan/komunitas tujuan
	 * @return informasi komunitas DSpace yang dipakai/dibuat
	 */
	public static DspaceInformation getDspaceTipeKegiatanKedosenanPunyaDosenJurusan(String cookie,
			KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) throws Exception {
		Jurusan jurusan = kegiatanKedosenanPunyaDosen.getDosen().getJurusan();

		String description = "Prestasi dosen untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ kegiatanKedosenanPunyaDosen.getDosen().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Prestasi Dosen");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Prestasi Dosen "
				+ kegiatanKedosenanPunyaDosen.getDosen().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKedosenanPunyaDosen_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	/** Implementasi {@link DataSearchDefault}; mendelegasikan langsung ke {@link #loadData(Object)}. */
	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
