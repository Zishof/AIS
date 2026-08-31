package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.KegiatanKedosenanAction;
import ais.action.master.SertifikatAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.Dosen;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK yang menampilkan daftar {@link KegiatanKedosenan} yang telah ditugaskan ke
 * satu {@link Dosen} tertentu (lewat relasi {@link KegiatanKedosenanPunyaDosen}), atau — bila
 * dibuat lewat konstruktor kedua — daftar lintas dosen yang disaring berdasarkan kelompok/detail
 * kelompok kegiatan, jabatan, skala, dan tahun akademik (dipakai dari layar rekap/monitoring
 * admin). Setiap baris dapat diperluas untuk menampilkan detail: unggah bukti kegiatan, tombol
 * cetak sertifikat (bila kegiatan sudah disetujui dan punya template sertifikat), serta — bagi
 * dosen pemilik kegiatan sendiri (sebelum disetujui) atau atasan langsungnya — kontrol edit
 * (jabatan, skala, tanggal mulai/selesai, keterangan) dan checkbox persetujuan atasan, dengan
 * tombol hapus yang hilang otomatis setelah disetujui.
 *
 * <p>
 * Toolbar menyediakan pencarian nama kegiatan/dosen, tombol "Ajukan Kegiatan Baru" (membuka
 * {@code KegiatanKedosenanAction.onAddExternal}), "Ambil Kegiatan Yang Ada" (membuka
 * {@link AmbilDataKegiatanForKegiatanKedosenanHelper} untuk menugaskan kegiatan yang sudah ada ke
 * dosen), dan tombol unduh Excel (dengan kolom tambahan hyperlink ke lampiran SK, disusun manual
 * lewat Apache POI/ZK POI).
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} dan {@link DataCriteria}. Bila helper dibuat dengan
 * {@code kegiatanKedosenanPunyaDosen} spesifik (lewat
 * {@link #display(Dosen, Component, KegiatanKedosenanPunyaDosen)}), baris tersebut selalu
 * ditampilkan PALING ATAS (disorot kuning) di halaman manapun grid berada, terlepas dari hasil
 * paging normal.
 * </p>
 */
public class DosenPunyaKegiatanKedosenanHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Dosen dosen;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan = null;
	private DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = null;
	private JabatanKegiatanKedosenan jabatanKegiatanKedosenan = null;
	private SkalaKegiatanKedosenan skalaKegiatanKedosenan = null;
	private String tahunAkademik = null;
	private KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen;

	/** Membuat helper tanpa filter kelompok/jabatan/skala/tahun akademik (hanya menyaring berdasarkan {@link Dosen} yang diberikan ke {@link #display}); menyiapkan paging server-side. */
	public DosenPunyaKegiatanKedosenanHelper() {

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Membuat helper dengan filter tambahan, dipakai dari layar rekap/monitoring lintas dosen.
	 *
	 * @param kelompokKegiatanKedosenan       filter kelompok kegiatan, boleh {@code null}
	 * @param detailKelompokKegiatanKedosenan filter detail kelompok kegiatan, boleh {@code null}
	 * @param jabatanKegiatanKedosenan        filter jabatan pada kegiatan, boleh {@code null}
	 * @param skalaKegiatanKedosenan          filter skala kegiatan, boleh {@code null}
	 * @param tahunAkademik                   filter tahun akademik kegiatan, boleh {@code null}
	 */
	public DosenPunyaKegiatanKedosenanHelper(KelompokKegiatanKedosenan kelompokKegiatanKedosenan,
			DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan,
			JabatanKegiatanKedosenan jabatanKegiatanKedosenan, SkalaKegiatanKedosenan skalaKegiatanKedosenan,
			String tahunAkademik) {

		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
		this.detailKelompokKegiatanKedosenan = detailKelompokKegiatanKedosenan;
		this.jabatanKegiatanKedosenan = jabatanKegiatanKedosenan;
		this.skalaKegiatanKedosenan = skalaKegiatanKedosenan;
		this.tahunAkademik = tahunAkademik;

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Perender baris grid: menyorot baris kuning bila cocok dengan {@link #kegiatanKedosenanPunyaDosen}
	 * yang diminta helper dibuka. Menampilkan foto+identitas dosen, kegiatan+kelompok+tahun ajaran
	 * (dengan riwayat revisi), area unggah bukti kegiatan, dan — untuk dosen pemilik (sebelum
	 * disetujui) atau atasan langsungnya — kontrol edit lengkap (jabatan, skala, tanggal, keterangan,
	 * checkbox persetujuan) beserta tombol hapus; bagi user lain, kolom-kolom tersebut ditampilkan
	 * sebagai label read-only. Tombol cetak sertifikat muncul hanya setelah kegiatan disetujui dan
	 * memiliki template sertifikat.
	 */
	class DetailDosenRenderer extends ais.ui.util.MyRowRenderer {

		public DetailDosenRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) data;
			final KegiatanKedosenan kegiatanKedosenan = kegiatanKedosenanPunyaDosen.getKegiatanKedosenan();

			try {
				if (DosenPunyaKegiatanKedosenanHelper.this.kegiatanKedosenanPunyaDosen != null
						&& DosenPunyaKegiatanKedosenanHelper.this.kegiatanKedosenanPunyaDosen.getId()
								.equals(kegiatanKedosenanPunyaDosen.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DosenPunyaKegiatanKedosenanHelper.java:136");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(kegiatanKedosenanPunyaDosen.getDosen());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKedosenanPunyaDosen.getDosen().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKedosenanPunyaDosen.getDosen().getNidn()));
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKedosenanPunyaDosen.getDosen().getJurusan() == null ? ""
					: kegiatanKedosenanPunyaDosen.getDosen().getJurusan().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(KegiatanKedosenanPunyaDosen.class, kegiatanKedosenanPunyaDosen,
					kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(
					kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getKelompokKegiatanKedosenan().getNama()));
			aa.appendChild(new MyLabelAgakKecil(
					kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getDetailKelompokKegiatanKedosenan().getNama()));

			aa.appendChild(new MyLabelAgakKecil(kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getTahunAkademik()
					+ "/" + kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getJenisSemester()));

			vbox = new Vbox();
			vbox.setParent(detail);

			boolean bolehEdit = tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& tbmuser.getDosen().getId().equals(kegiatanKedosenanPunyaDosen.getDosen().getId())
					&& !kegiatanKedosenanPunyaDosen.getPersetujuan();

			boolean merupakanAtasanLangsung = (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& kegiatanKedosenanPunyaDosen.getDosen() != null
					&& kegiatanKedosenanPunyaDosen.getDosen().getAtasanlangsung() != null
					&& kegiatanKedosenanPunyaDosen.getDosen().getAtasanlangsung().equals(tbmuser.getDosen().getId()));

			System.out.println("merupakanAtasanLangsung => " + merupakanAtasanLangsung);

			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKedosenanPunyaDosen.getId(),
					KegiatanKedosenanPunyaDosen.class.getName(), "Bukti Kegiatan Dosen", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit || merupakanAtasanLangsung, null);
			hbox.setParent(vbox);
			Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			cetakToolbarbuttonSertifikat.setVisible(kegiatanKedosenanPunyaDosen.getPersetujuan()
					&& kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null);
			if (bolehEdit || merupakanAtasanLangsung) {

				final MyTextbox keterangan = new MyTextbox(kegiatanKedosenanPunyaDosen.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				final MyDatebox mulai = new MyDatebox(kegiatanKedosenanPunyaDosen.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(kegiatanKedosenanPunyaDosen.getSampai());
				sampai.setWidth("90%");

				mulai.setParent(row);
				sampai.setParent(row);

				DetailKelompokKegiatanKedosenan kedosenan = kegiatanKedosenan.getDetailKelompokKegiatanKedosenan();
				HibernateUtil.currentSession().refresh(kedosenan);

				List<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new ArrayList<JabatanKegiatanKedosenan>(
						kedosenan.getJabatanKegiatanKedosenans());
				List<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new ArrayList<SkalaKegiatanKedosenan>(
						kedosenan.getSkalaKegiatanKedosenans());

				Collections.sort(jabatanKegiatanKedosenans);
				Collections.sort(skalaKegiatanKedosenans);

				final Combobox jabatanKegiatanKedosenan = new Combobox();
				jabatanKegiatanKedosenan.setVisible(!jabatanKegiatanKedosenans.isEmpty());
				Common.insertComboItems(jabatanKegiatanKedosenan, "nama", jabatanKegiatanKedosenans);
				Common.selectComboItem(true, jabatanKegiatanKedosenan,
						kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan());
				jabatanKegiatanKedosenan.setParent(row);
				jabatanKegiatanKedosenan.setReadonly(true);
				jabatanKegiatanKedosenan.setWidth("97%");

				final Combobox skalaKegiatanKedosenan = new Combobox();
				skalaKegiatanKedosenan.setVisible(!skalaKegiatanKedosenans.isEmpty());
				Common.insertComboItems(skalaKegiatanKedosenan, "nama", skalaKegiatanKedosenans);
				Common.selectComboItem(true, skalaKegiatanKedosenan,
						kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan());
				skalaKegiatanKedosenan.setParent(row);
				skalaKegiatanKedosenan.setReadonly(true);
				skalaKegiatanKedosenan.setWidth("97%");

				EventListener eventListener = new EventListener() {

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
				keterangan.setParent(row);

				final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

				buttonDelete.setVisible(!kegiatanKedosenanPunyaDosen.getPersetujuan());
				jabatanKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
				skalaKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
				keterangan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
				mulai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
				sampai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());

				if ((tbmuser.ambilDosen() == null && tbmuser.ambilDosen().getAtasanlangsung() == null)
						|| merupakanAtasanLangsung) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(kegiatanKedosenanPunyaDosen.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							kegiatanKedosenanPunyaDosen.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(kegiatanKedosenanPunyaDosen);
							buttonDelete.setVisible(!kegiatanKedosenanPunyaDosen.getPersetujuan());

							jabatanKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
							skalaKegiatanKedosenan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
							keterangan.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
							mulai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());
							sampai.setDisabled(kegiatanKedosenanPunyaDosen.getPersetujuan());

							cetakToolbarbuttonSertifikat.setVisible(kegiatanKedosenanPunyaDosen.getPersetujuan()
									&& kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getSertifikat() != null);
						}
					});
				} else {
					Label label;
					(label = new Label(kegiatanKedosenanPunyaDosen.getPersetujuan() == null
							|| kegiatanKedosenanPunyaDosen.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				buttonDelete.setOrient("vertical");
				buttonDelete.setVisible(!kegiatanKedosenanPunyaDosen.getPersetujuan());
				buttonDelete.setTooltiptext("Hapus Data");
				buttonDelete.addEventListener("onClick", new EventListener() {
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

												try {
													if (DosenPunyaKegiatanKedosenanHelper.this.kegiatanKedosenanPunyaDosen != null
															&& DosenPunyaKegiatanKedosenanHelper.this.kegiatanKedosenanPunyaDosen
																	.getId()
																	.equals(kegiatanKedosenanPunyaDosen.getId())) {
														DosenPunyaKegiatanKedosenanHelper.this.kegiatanKedosenanPunyaDosen = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DosenPunyaKegiatanKedosenanHelper.java:327");
													// TODO: handle exception
												}

												Common.refreshDelete(kegiatanKedosenanPunyaDosen);
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
				buttonDelete.setParent(toolbar);

				jabatanKegiatanKedosenans = null;
				skalaKegiatanKedosenans = null;
			} else {
				new Label(kegiatanKedosenanPunyaDosen.getMulai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKedosenanPunyaDosen.getMulai())).setParent(row);
				new Label(kegiatanKedosenanPunyaDosen.getSampai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKedosenanPunyaDosen.getSampai())).setParent(row);
				new Label(kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan() == null ? ""
						: kegiatanKedosenanPunyaDosen.getJabatanKegiatanKedosenan().getNama()).setParent(row);
				new Label(kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan() == null ? ""
						: kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan().getNama()).setParent(row);
				new Label(kegiatanKedosenanPunyaDosen.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(kegiatanKedosenanPunyaDosen.getPersetujuan() == null
						|| kegiatanKedosenanPunyaDosen.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			toolbar.setParent(row);
			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKedosenanPunyaDosen);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);

		}

	}

	/**
	 * Membangun kriteria {@link KegiatanKedosenanPunyaDosen} sesuai pencarian nama (kegiatan atau
	 * dosen, ilike), dan seluruh filter opsional helper (dosen — atau bawahan langsung dosen login
	 * bila filter dosen tidak diisi, kelompok/detail kelompok/jabatan/skala kegiatan, tahun
	 * akademik).
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menurun berdasarkan id
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Long loginAtasan = tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") ? tbmuser.getDosen().getId() : null;

		System.out.println("loginAtasan => " + loginAtasan);

		Criteria criteria = session.createCriteria(KegiatanKedosenanPunyaDosen.class);

		criteria.createAlias("kegiatanKedosenan", "kegiatanKedosenan").createAlias("dosen", "dosen")

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("kegiatanKedosenan.nama", nama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("dosen.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(dosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("dosen", dosen),
								Restrictions.eq("dosen.atasanlangsung", loginAtasan)))

				.add(kelompokKegiatanKedosenan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKedosenan.kelompokKegiatanKedosenan", kelompokKegiatanKedosenan))

				.add(detailKelompokKegiatanKedosenan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKedosenan.detailKelompokKegiatanKedosenan",
								detailKelompokKegiatanKedosenan))

				.add(jabatanKegiatanKedosenan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanKegiatanKedosenan", jabatanKegiatanKedosenan))

				.add(skalaKegiatanKedosenan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("skalaKegiatanKedosenan", skalaKegiatanKedosenan))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKedosenan.tahunAkademik", tahunAkademik));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat ulang grid secara asinkron: menghitung total baris untuk paging, lalu mengambil satu
	 * halaman {@link KegiatanKedosenanPunyaDosen} sesuai kriteria. Bila helper dibuka dengan
	 * {@link #kegiatanKedosenanPunyaDosen} spesifik, baris tersebut SELALU disisipkan di posisi
	 * pertama daftar (dikecualikan dari hasil paging normal via {@code Restrictions.ne("id", ...)})
	 * agar tetap terlihat di halaman manapun.
	 *
	 * @param value tidak digunakan; ada untuk memenuhi kontrak {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);

				List<KegiatanKedosenanPunyaDosen> myKegiatanKedosenanPunyaDosens;

				if (kegiatanKedosenanPunyaDosen != null) {
					myKegiatanKedosenanPunyaDosens = new ArrayList<KegiatanKedosenanPunyaDosen>();
					myKegiatanKedosenanPunyaDosens.add(kegiatanKedosenanPunyaDosen);
					myKegiatanKedosenanPunyaDosens
							.addAll(initCriteria(true).add(Restrictions.ne("id", kegiatanKedosenanPunyaDosen.getId()))
									.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
									.setFirstResult(
											Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
									.list());
				} else {
					myKegiatanKedosenanPunyaDosens = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myKegiatanKedosenanPunyaDosens);
				grid.setRowRenderer(new DetailDosenRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	/** @return referensi ke helper ini sendiri, dipakai sebagai {@link DataLoader} oleh helper penambah data. */
	private DataLoader getDataloader() {
		return this;
	}

	/** Seperti {@link #display(Dosen, Component, KegiatanKedosenanPunyaDosen)} tanpa baris yang perlu disorot/ditampilkan lebih dulu. */
	public void display(final Dosen dosen, Component component) {
		display(dosen, component, null);
	}

	/**
	 * Membangun dan menampilkan UI daftar kegiatan kedosenan ke dalam {@code component}: toolbar
	 * pencarian dan aksi (Ajukan Kegiatan Baru, Ambil Kegiatan Yang Ada — keduanya hanya tampil bila
	 * user login, unduh Excel dengan kolom hyperlink lampiran SK), dan grid ber-paging di dalam
	 * area scroll tetap (60vh).
	 *
	 * @param dosen                       dosen konteks (menentukan filter kegiatan miliknya)
	 * @param component                   komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param kegiatanKedosenanPunyaDosen bila diberikan, baris relasi ini disorot dan selalu
	 *                                    ditampilkan pertama di grid; boleh {@code null}
	 */
	public void display(final Dosen dosen, Component component,
			KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) {
		this.dosen = dosen;
		this.kegiatanKedosenanPunyaDosen = kegiatanKedosenanPunyaDosen;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 3200px;");
		groupbox.setParent(Common.tampilanScroll(component));

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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ajukan Kegiatan Baru", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				KegiatanKedosenan kegiatanKedosenan = new KegiatanKedosenan();
				kegiatanKedosenan.setDiajukanOleh(dosen);
				KegiatanKedosenanAction.onAddExternal(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				}, kegiatanKedosenan);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Kegiatan Yang Ada", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				MyWindow window = new MyWindow();
				window.setHeight("97%");
				window.setWidth("800px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				AmbilDataKegiatanForKegiatanKedosenanHelper dataDosenHelper = new AmbilDataKegiatanForKegiatanKedosenanHelper(
						dosen);
				dataDosenHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

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
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
									.createHyperlink(Hyperlink.LINK_URL);
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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KegiatanKedosenanPunyaDosen.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, "id", "kegiatanKedosenan", "dosen",
				"jabatanKegiatanKedosenan", "skalaKegiatanKedosenan", "persetujuan", "mulai", "sampai", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		if (dosen != null) {

			// MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak
			// Angka Kredit", "/img/print.png");
			// cetak.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			// CommonReportHelper.onCetakAngkaKreditDosen(dosen);
			// }
			// });
			// cetak.setParent(toolbar);
		}

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
		grid.setPageSize(10);
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kegiatan");

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
		column.setWidth("5%");

		loadData(null);

	}

}
