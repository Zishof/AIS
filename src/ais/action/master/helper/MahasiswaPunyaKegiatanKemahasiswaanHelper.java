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

import ais.action.master.KegiatanKemahasiswaanAction;
import ais.action.master.SertifikatAction;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.database.model.Mahasiswa;
import ais.database.model.SkalaKegiatanKemahasiswaan;
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
 * Helper composer ZK yang menampilkan dan mengelola daftar keikutsertaan mahasiswa pada kegiatan
 * kemahasiswaan ({@link KegiatanKemahasiswaanPunyaMahasiswa}) — dapat dipakai baik untuk satu
 * mahasiswa tertentu (parameter {@code mahasiswa} pada {@link #display}) maupun sebagai rekap lintas
 * mahasiswa difilter kombinasi kelompok/detail-kelompok/jabatan/skala kegiatan dan tahun akademik
 * (konstruktor kedua).
 *
 * <p>
 * Setiap baris dapat dibuka (detail) untuk unggah bukti kegiatan, mengedit tanggal mulai/sampai,
 * jabatan, skala, dan keterangan — hanya oleh mahasiswa pemilik data dan selama belum disetujui
 * ({@code persetujuan=false}). Approval (checkbox "Setujui") hanya tersedia bagi user non-mahasiswa
 * (staf/dosen/admin). Setelah disetujui, tombol cetak sertifikat muncul bila kegiatan terkait punya
 * template sertifikat ({@link SertifikatAction#cetakSertifikat}). Toolbar menyediakan pencarian nama
 * kegiatan, pengajuan kegiatan baru ({@link KegiatanKemahasiswaanAction#onAddExternal}), pendaftaran
 * ke kegiatan yang sudah ada (lewat {@link AmbilDataKegiatanForKegiatanKemahasiswaanHelper}), unduh
 * Excel dengan tautan berkas SK per baris, serta cetak rekap angka kredit
 * ({@link CommonReportHelper#onCetakAngkaKreditMahasiswa}/{@code onCetakRekapAngkaKreditMahasiswa}).
 * Bila dibuka menyorot satu baris tertentu ({@code kegiatanKemahasiswaanPunyaMahasiswa} pada
 * constructor {@link #display(Mahasiswa, Component, KegiatanKemahasiswaanPunyaMahasiswa)}), baris
 * tersebut disorot kuning dan selalu ditampilkan di posisi pertama grid.
 * </p>
 */
public class MahasiswaPunyaKegiatanKemahasiswaanHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Mahasiswa mahasiswa;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan = null;
	private DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = null;
	private JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan = null;
	private SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan = null;
	private String tahunAkademik = null;
	private KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa;

	/** Membuat helper tanpa filter kelompok/jabatan/skala/tahun akademik (dipakai untuk daftar satu mahasiswa). */
	public MahasiswaPunyaKegiatanKemahasiswaanHelper() {

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
	 * Membuat helper dengan filter tetap untuk mode rekap lintas mahasiswa.
	 *
	 * @param kelompokKegiatanKemahasiswaan       filter kelompok kegiatan, boleh {@code null}
	 * @param detailKelompokKegiatanKemahasiswaan filter detail kelompok kegiatan, boleh {@code null}
	 * @param jabatanKegiatanKemahasiswaan        filter jabatan/status dalam kegiatan, boleh {@code null}
	 * @param skalaKegiatanKemahasiswaan          filter skala kegiatan, boleh {@code null}
	 * @param tahunAkademik                       filter tahun akademik kegiatan, boleh {@code null}
	 */
	public MahasiswaPunyaKegiatanKemahasiswaanHelper(KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan,
			DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan,
			JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan,
			SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan, String tahunAkademik) {

		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
		this.detailKelompokKegiatanKemahasiswaan = detailKelompokKegiatanKemahasiswaan;
		this.jabatanKegiatanKemahasiswaan = jabatanKegiatanKemahasiswaan;
		this.skalaKegiatanKemahasiswaan = skalaKegiatanKemahasiswaan;
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
	 * Perender baris grid untuk satu {@link KegiatanKemahasiswaanPunyaMahasiswa}: panel detail
	 * unggah bukti kegiatan, identitas mahasiswa dan info kegiatan (kelompok/detail/tahun-semester),
	 * dan — bila baris ini milik mahasiswa yang sedang login dan belum disetujui — field yang bisa
	 * diedit (tanggal, jabatan, skala, keterangan, tombol hapus); selain itu ditampilkan sebagai
	 * label baca-saja beserta checkbox/label persetujuan (checkbox hanya untuk user non-mahasiswa).
	 * Baris yang cocok dengan {@link #kegiatanKemahasiswaanPunyaMahasiswa} (baris yang sedang
	 * disorot dari pemanggil) diberi latar kuning.
	 */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailMahasiswaRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) data;
			final KegiatanKemahasiswaan kegiatanKemahasiswaan = kegiatanKemahasiswaanPunyaMahasiswa
					.getKegiatanKemahasiswaan();

			try {
				if (MahasiswaPunyaKegiatanKemahasiswaanHelper.this.kegiatanKemahasiswaanPunyaMahasiswa != null
						&& MahasiswaPunyaKegiatanKemahasiswaanHelper.this.kegiatanKemahasiswaanPunyaMahasiswa.getId()
								.equals(kegiatanKemahasiswaanPunyaMahasiswa.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MahasiswaPunyaKegiatanKemahasiswaanHelper.java:138");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNim()));
			vbox.appendChild(
					new MyLabelAgakKecil(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(KegiatanKemahasiswaanPunyaMahasiswa.class,
					kegiatanKemahasiswaanPunyaMahasiswa,
					kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
					.getKelompokKegiatanKemahasiswaan().getNama()));
			aa.appendChild(new MyLabelAgakKecil(kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
					.getDetailKelompokKegiatanKemahasiswaan().getNama()));
			aa.appendChild(new MyLabelAgakKecil(
					kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getTahunAkademik() + "/"
							+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getJenisSemester()));

			vbox = new Vbox();
			vbox.setParent(detail);

			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getId())
					&& !kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan();

			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaanPunyaMahasiswa.getId(),
					KegiatanKemahasiswaanPunyaMahasiswa.class.getName(), "Bukti Kegiatan Mahasiswa", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit, null);
			hbox.setParent(vbox);

			Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
					&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat() != null);

			if (bolehEdit) {

				final MyTextbox keterangan = new MyTextbox(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				final MyDatebox mulai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getSampai());
				sampai.setWidth("90%");

				mulai.setParent(row);
				sampai.setParent(row);

				DetailKelompokKegiatanKemahasiswaan kemahasiswaan = kegiatanKemahasiswaan
						.getDetailKelompokKegiatanKemahasiswaan();
				try {
					HibernateUtil.currentSession().refresh(kemahasiswaan);
				} catch (Exception eRefresh) { ais.common.ErrorAuditUtil.record(eRefresh, "auto-audit(empty-catch) src/ais/action/master/helper/MahasiswaPunyaKegiatanKemahasiswaanHelper.java:211");
					// Session mungkin sudah ditutup; abaikan refresh dan gunakan data yang sudah ada
				}

				List<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans;
				List<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans;
				try {
					jabatanKegiatanKemahasiswaans = new ArrayList<JabatanKegiatanKemahasiswaan>(
							kemahasiswaan.getJabatanKegiatanKemahasiswaans());
					skalaKegiatanKemahasiswaans = new ArrayList<SkalaKegiatanKemahasiswaan>(
							kemahasiswaan.getSkalaKegiatanKemahasiswaans());
				} catch (org.hibernate.LazyInitializationException eLazy) {
					// Koleksi lazy tidak dapat diinisialisasi; pakai list kosong sebagai fallback
					jabatanKegiatanKemahasiswaans = new ArrayList<JabatanKegiatanKemahasiswaan>();
					skalaKegiatanKemahasiswaans = new ArrayList<SkalaKegiatanKemahasiswaan>();
				}

				Collections.sort(jabatanKegiatanKemahasiswaans);
				Collections.sort(skalaKegiatanKemahasiswaans);

				final Combobox jabatanKegiatanKemahasiswaan = new Combobox();
				jabatanKegiatanKemahasiswaan.setVisible(!jabatanKegiatanKemahasiswaans.isEmpty());
				Common.insertComboItems(jabatanKegiatanKemahasiswaan, "nama", jabatanKegiatanKemahasiswaans);
				Common.selectComboItem(true, jabatanKegiatanKemahasiswaan,
						kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan());
				jabatanKegiatanKemahasiswaan.setParent(row);
				jabatanKegiatanKemahasiswaan.setReadonly(true);
				jabatanKegiatanKemahasiswaan.setWidth("97%");

				final Combobox skalaKegiatanKemahasiswaan = new Combobox();
				skalaKegiatanKemahasiswaan.setVisible(!skalaKegiatanKemahasiswaans.isEmpty());
				Common.insertComboItems(skalaKegiatanKemahasiswaan, "nama", skalaKegiatanKemahasiswaans);
				Common.selectComboItem(true, skalaKegiatanKemahasiswaan,
						kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan());
				skalaKegiatanKemahasiswaan.setParent(row);
				skalaKegiatanKemahasiswaan.setReadonly(true);
				skalaKegiatanKemahasiswaan.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKemahasiswaanPunyaMahasiswa.setMulai(mulai.getValue());
						kegiatanKemahasiswaanPunyaMahasiswa.setSampai(sampai.getValue());
						kegiatanKemahasiswaanPunyaMahasiswa.setSkalaKegiatanKemahasiswaan(
								(SkalaKegiatanKemahasiswaan) (skalaKegiatanKemahasiswaan.getSelectedItem() == null
										? null
										: skalaKegiatanKemahasiswaan.getSelectedItem().getValue()));
						kegiatanKemahasiswaanPunyaMahasiswa.setKeterangan(keterangan.getValue());
						kegiatanKemahasiswaanPunyaMahasiswa.setJabatanKegiatanKemahasiswaan(
								((JabatanKegiatanKemahasiswaan) (jabatanKegiatanKemahasiswaan.getSelectedItem() == null
										? null
										: jabatanKegiatanKemahasiswaan.getSelectedItem().getValue())));
						Common.refreshUpdate(kegiatanKemahasiswaanPunyaMahasiswa);

					}
				};

				skalaKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
				jabatanKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
				keterangan.addEventListener("onChange", eventListener);
				mulai.addEventListener("onChange", eventListener);
				sampai.addEventListener("onChange", eventListener);
				keterangan.setParent(row);

				final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

				buttonDelete.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				jabatanKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				skalaKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				if (tbmuser.getMahasiswa() == null) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(kegiatanKemahasiswaanPunyaMahasiswa);
							buttonDelete.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

							jabatanKegiatanKemahasiswaan
									.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
							skalaKegiatanKemahasiswaan
									.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
							keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
							mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
							sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

							cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
									&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
											.getSertifikat() != null);
						}
					});
				} else {
					Label label;
					(label = new Label(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() == null
							|| kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				buttonDelete.setOrient("vertical");
				buttonDelete.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
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
													if (MahasiswaPunyaKegiatanKemahasiswaanHelper.this.kegiatanKemahasiswaanPunyaMahasiswa != null
															&& MahasiswaPunyaKegiatanKemahasiswaanHelper.this.kegiatanKemahasiswaanPunyaMahasiswa
																	.getId().equals(kegiatanKemahasiswaanPunyaMahasiswa
																			.getId())) {
														MahasiswaPunyaKegiatanKemahasiswaanHelper.this.kegiatanKemahasiswaanPunyaMahasiswa = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MahasiswaPunyaKegiatanKemahasiswaanHelper.java:342");
													// TODO: handle exception
												}

												Common.refreshDelete(kegiatanKemahasiswaanPunyaMahasiswa);
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

				jabatanKegiatanKemahasiswaans = null;
				skalaKegiatanKemahasiswaans = null;
			} else {
				new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMulai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKemahasiswaanPunyaMahasiswa.getMulai())).setParent(row);
				new Label(kegiatanKemahasiswaanPunyaMahasiswa.getSampai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKemahasiswaanPunyaMahasiswa.getSampai())).setParent(row);
				new Label(kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan().getNama())
						.setParent(row);
				new Label(kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan() == null ? ""
						: kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan().getNama()).setParent(row);
				new Label(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() == null
						|| kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			toolbar.setParent(row);
			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKemahasiswaanPunyaMahasiswa);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);
		}

	}

	/**
	 * Membangun kriteria Hibernate untuk {@link KegiatanKemahasiswaanPunyaMahasiswa} difilter
	 * berdasarkan nama kegiatan (ILIKE anywhere), {@link #mahasiswa} (bila diberikan), serta filter
	 * tetap yang diberikan lewat konstruktor kedua (kelompok/detail-kelompok/jabatan/skala/tahun
	 * akademik).
	 *
	 * @param order bila {@code true}, hasil diurutkan berdasarkan id menurun
	 * @return criteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class);

		criteria.createAlias("kegiatanKemahasiswaan", "kegiatanKemahasiswaan")

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kegiatanKemahasiswaan.nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))

				.add(kelompokKegiatanKemahasiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKemahasiswaan.kelompokKegiatanKemahasiswaan",
								kelompokKegiatanKemahasiswaan))

				.add(detailKelompokKegiatanKemahasiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKemahasiswaan.detailKelompokKegiatanKemahasiswaan",
								detailKelompokKegiatanKemahasiswaan))

				.add(jabatanKegiatanKemahasiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanKegiatanKemahasiswaan", jabatanKegiatanKemahasiswaan))

				.add(skalaKegiatanKemahasiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("skalaKegiatanKemahasiswaan", skalaKegiatanKemahasiswaan))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKemahasiswaan.tahunAkademik", tahunAkademik));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat ulang grid (lewat timer default) berdasarkan {@link #initCriteria(boolean)}. Bila
	 * {@link #kegiatanKemahasiswaanPunyaMahasiswa} diset (baris yang sedang disorot dari
	 * pemanggil), baris tersebut selalu disisipkan di posisi pertama dan dikecualikan dari hasil
	 * paging normal agar tidak duplikat.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKemahasiswaanPunyaMahasiswa> myKegiatanKemahasiswaanPunyaMahasiswas;

				if (kegiatanKemahasiswaanPunyaMahasiswa != null) {
					myKegiatanKemahasiswaanPunyaMahasiswas = new ArrayList<KegiatanKemahasiswaanPunyaMahasiswa>();
					myKegiatanKemahasiswaanPunyaMahasiswas.add(kegiatanKemahasiswaanPunyaMahasiswa);
					myKegiatanKemahasiswaanPunyaMahasiswas.addAll(initCriteria(true)
							.add(Restrictions.ne("id", kegiatanKemahasiswaanPunyaMahasiswa.getId()))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list());
				} else {
					myKegiatanKemahasiswaanPunyaMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myKegiatanKemahasiswaanPunyaMahasiswas);
				grid.setRowRenderer(new DetailMahasiswaRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	/** Seperti {@link #display(Mahasiswa, Component, KegiatanKemahasiswaanPunyaMahasiswa)} tanpa baris yang disorot. */
	public void display(Mahasiswa mahasiswa, Component component) {
		display(mahasiswa, component, null);
	}

	/**
	 * Membangun UI lengkap: toolbar responsif (Div flex-wrap di mobile, {@link Toolbar} di desktop)
	 * berisi pencarian nama, ajukan kegiatan baru, ikut kegiatan, unduh Excel, dan (bila
	 * {@code mahasiswa} diberikan) cetak angka kredit/rekap angka kredit; lalu grid berpaging. Lalu
	 * memuat datanya.
	 *
	 * @param mahasiswa                             mahasiswa yang keikutsertaannya
	 *                                              ditampilkan/dikelola; boleh {@code null} untuk
	 *                                              mode rekap lintas mahasiswa (memakai filter dari
	 *                                              konstruktor kedua)
	 * @param component                            komponen induk ZK; isinya dibersihkan lebih dulu
	 * @param kegiatanKemahasiswaanPunyaMahasiswa   baris yang akan disorot dan ditampilkan di posisi
	 *                                              pertama grid, boleh {@code null}
	 */
	public void display(final Mahasiswa mahasiswa, Component component,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.kegiatanKemahasiswaanPunyaMahasiswa = kegiatanKemahasiswaanPunyaMahasiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(Common.tampilanScroll(component));

		// Wadah tombol responsif. Di MOBILE, ZK Toolbar menata tombol dalam SATU baris tanpa
		// membungkus sehingga tombol (Ajukan Kegiatan Baru, Ikut Kegiatan, Download, Cetak/Rekap
		// Angka Kredit) meluber ke luar layar & tak terlihat. Maka di mobile dipakai Div flex-wrap
		// agar SEMUA tombol membungkus ke bawah dan tetap tampil. Desktop tetap memakai Toolbar
		// (tidak diubah).
		final boolean mobileTampil = Common.isMobile();
		Component toolbar;
		if (mobileTampil) {
			org.zkoss.zul.Div bar = new org.zkoss.zul.Div();
			bar.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px 4px;"
					+ "width:100%;box-sizing:border-box;");
			bar.setParent(groupbox);
			toolbar = bar;
		} else {
			Toolbar tb = new Toolbar();
			// tb.setHeight("25px");
			tb.setParent(groupbox);
			toolbar = tb;
		}

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
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
				KegiatanKemahasiswaan kegiatanKemahasiswaan = new KegiatanKemahasiswaan();
				kegiatanKemahasiswaan.setDiajukanOleh(mahasiswa);
				KegiatanKemahasiswaanAction.onAddExternal(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				}, kegiatanKemahasiswaan);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ikut Kegiatan", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				MyWindow window = new MyWindow();
				window.setHeight("97%");
				window.setWidth("800px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				AmbilDataKegiatanForKegiatanKemahasiswaanHelper dataMahasiswaHelper = new AmbilDataKegiatanForKegiatanKemahasiswaanHelper(
						mahasiswa);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				/**
				 * Helper implementasi bersarang milik {@link MahasiswaPunyaKegiatanKemahasiswaanHelper} untuk data adding
				 * helper. Kelas ini mengemas langkah lokal yang dipakai kelas induk dan bukan service domain alternatif.
				 *
				 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MahasiswaPunyaKegiatanKemahasiswaanHelper} dan
				 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
				 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code process}(). Aturan bisnis bersama
				 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
				 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
				 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
				 * tambahkan perilaku lintas domain pada service bersama.</p>
				 *
				 * @see MahasiswaPunyaKegiatanKemahasiswaanHelper
				 */
				class DataAddingHelper {
					public void process(XSSFRow row, int index,
							KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(kegiatanKemahasiswaanPunyaMahasiswa.getId(), jenis);

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

				dataAddingHelper.process(row, 9, kegiatanKemahasiswaanPunyaMahasiswa,
						KegiatanKemahasiswaanPunyaMahasiswa.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				KegiatanKemahasiswaanPunyaMahasiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, "id", "kegiatanKemahasiswaan", "mahasiswa", "jabatanKegiatanKemahasiswaan",
				"skalaKegiatanKemahasiswaan", "persetujuan", "mulai", "sampai", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		if (mahasiswa != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Angka Kredit", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakAngkaKreditMahasiswa(mahasiswa);
				}
			});
			cetak.setParent(toolbar);

			cetak = new MyToolbarbuttonConfig("Rekap Angka Kredit", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakRekapAngkaKreditMahasiswa(mahasiswa);
				}
			});
			cetak.setParent(toolbar);

			cetak = new MyToolbarbuttonConfig("Cetak Aktifitas", "/img/print.png");
			cetak.setTooltiptext("Cetak Form A nilai semester dan Form B rekap kumulatif PAKEM");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakAktifitasMahasiswa(mahasiswa);
				}
			});
			cetak.setParent(toolbar);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");
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
		column.setWidth("30%");

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
