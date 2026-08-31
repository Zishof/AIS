package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk biodata calon mahasiswa punya verifikasi berkas. Tipe ini merupakan
 * titik masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau
 * dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Textbox searchcalnama}, {@code Combobox searchProdiLulus}, {@code Combobox
 * searchProdiPilihan}, {@code Combobox searchTahunAjaran}, {@code Combobox searchGelombang};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code ambilNamaFileLampiran()}); operasi domain lain ({@code
 * copyFile()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class BiodataCalonMahasiswaPunyaVerifikasiBerkasAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchcalnama;

	private Combobox searchProdiLulus;
	private Combobox searchProdiPilihan;
	private Combobox searchTahunAjaran;
	private Combobox searchGelombang;
	private Combobox searchJenisSeleksi;

	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahdapatNIM;

	private MyCheckboxConfig tampilkanYgBelumBayar;
	private MyCheckboxConfig tampilkanYgBelumBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumdapatNIM;
	private MyCheckboxConfig mengisiFormTambahan;

	private MyCheckboxConfig belumUploadBerkas;
	private MyCheckboxConfig telahUploadBerkas;
	private MyCheckboxConfig belumLolosBerkas;
	private MyCheckboxConfig telahLolosBerkas;

	private MyToolbarbuttonConfig find;
	protected LampiranLain lainMahasiswa;
	private Combobox searchPaket;

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

		Common.insertComboDanSemua(searchProdiLulus, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchProdiPilihan, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchJenisSeleksi, "nama", "deskripsi", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Common.insertComboDanSemua(searchPaket, "nama", "keterangan", Paket.class,
				Restrictions.and(
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();
		if (!tahunAkademikPenerimaanMahasiswaBaru.isEmpty()) {
			Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, "nama", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
				searchGelombang.setReadonly(true);
				searchGelombang.setSelectedIndex(searchGelombang.getChildren().size() - 1);
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		String[] contents = new String[] { "id", "biodataCalonMahasiswa.noRegistrasi", "biodataCalonMahasiswa.noUjian",
				"biodataCalonMahasiswa.nama", "biodataCalonMahasiswa.tahunAkademik",
				"biodataCalonMahasiswa.gelombangPendaftaran.nama", "verifikasiKelengkapanCalonMahasiswa.nama",
				"verifikasiKelengkapanCalonMahasiswa.wajib", "verifikasiKelengkapanCalonMahasiswa.verifikasi",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<BiodataCalonMahasiswaPunyaVerifikasiBerkas> biodataCalonMahasiswaPunyaVerifikasiBerkass = initCriteria(
						true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
				File folderOut = new File(Common.REAL_PATH + "/media/");
				try {
					folderOut.mkdirs();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataCalonMahasiswaPunyaVerifikasiBerkasAction.java:187");
					// TODO: handle exception
				}
				for (BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas : biodataCalonMahasiswaPunyaVerifikasiBerkass) {
					LampiranLain lam = LampiranLain.ambil(biodataCalonMahasiswaPunyaVerifikasiBerkas.getId(),
							BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
					if (lam != null) {
						File file;
						if (lam.getGdrive() != null && !lam.getGdrive().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.forwardGDriveUrl());
						} else if (lam.getLink() != null && !lam.getLink().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.getLink().trim());
						} else {
							file = lam.ambilFile();
						}

						if (file != null && file.exists() && file.isFile()) {
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
									+ URLEncoder.encode(ambilNamaFileLampiran(biodataCalonMahasiswaPunyaVerifikasiBerkas),
											"UTF-8")
									+ "_" + file.getName());
							copyFile(file, fileCopy);
						}
					}
				}
				biodataCalonMahasiswaPunyaVerifikasiBerkass.clear();
				biodataCalonMahasiswaPunyaVerifikasiBerkass = null;
				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);
	        FilterLanjutHelper.setup(comp);
}

	class BiodataCalonMahasiswaPunyaVerifikasiBerkasRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) arg1;
			BiodataCalonMahasiswa calonMahasiswa = biodataCalonMahasiswaPunyaVerifikasiBerkas
					.getBiodataCalonMahasiswa();

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);

			boolean belumExpiredtemp = true;
			try {
				if (biodataCalonMahasiswaPunyaVerifikasiBerkas.getBiodataCalonMahasiswa().getGelombangPendaftaran()
						.getTanggalDaftarUlangBerakhir() != null) {

					Date skrng = WaktuUtil.getDate();
					belumExpiredtemp = biodataCalonMahasiswaPunyaVerifikasiBerkas.getBiodataCalonMahasiswa()
							.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir().after(skrng)
							|| Common.dateFormat8.get()
									.format(biodataCalonMahasiswaPunyaVerifikasiBerkas.getBiodataCalonMahasiswa()
											.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir())
									.equals(Common.dateFormat8.get().format(skrng));

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataCalonMahasiswaPunyaVerifikasiBerkasAction.java:255");
				// TODO: handle exception
			}
			final boolean belumExpired = belumExpiredtemp;
			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa, calonMahasiswa.getNama()))
					.setParent(arg0);
			aa.appendChild(new MyLabelAgakKecilBold(calonMahasiswa.getKeterangan()));

			calonMahasiswa.tampilkanHp(aa);
			calonMahasiswa.tampilkanEmail(aa);

			(RevisiHelper.createNewRevisi(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class,
					biodataCalonMahasiswaPunyaVerifikasiBerkas,
					biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerifikasiKelengkapanCalonMahasiswa().getNama()))
					.setParent(arg0);

			final Vbox myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			hbox.setWidth("100%");
			LampiranLain.createDownloadUploadFileLain(hbox, biodataCalonMahasiswaPunyaVerifikasiBerkas.getId(),
					BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getData();
							if (lampiranLain != null) {
								biodataCalonMahasiswaPunyaVerifikasiBerkas.setUploaded(true);
								biodataCalonMahasiswaPunyaVerifikasiBerkas.setNamaFile(lampiranLain.getNama());
								Common.refreshUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);
							}
						}

					}, null, false, false, false,
					!biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified() && belumExpired, null, false, false);

			final Textbox keterangan = new Textbox(biodataCalonMahasiswaPunyaVerifikasiBerkas.getKeterangan());
			keterangan.setWidth("100%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			new Label(biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerifikasiKelengkapanCalonMahasiswa().getWajib()
					? "Ya"
					: "Tidak").setParent(arg0);
			new Label(
					biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerifikasiKelengkapanCalonMahasiswa().getVerifikasi()
							? "Ya"
							: "Tidak")
					.setParent(arg0);

			final Checkbox checkbox = new Checkbox("Sesuai", "/img/Check-icon.png");
			checkbox.setStyle("font-size:11px;font-weight: bolder;");
			checkbox.setParent(arg0);
			checkbox.setChecked(biodataCalonMahasiswaPunyaVerifikasiBerkas != null
					&& biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified());
			keterangan.setDisabled(checkbox.isChecked());

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					keterangan.setDisabled(checkbox.isChecked());

					biodataCalonMahasiswaPunyaVerifikasiBerkas.setVerified(checkbox.isChecked());
					biodataCalonMahasiswaPunyaVerifikasiBerkas.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);

					Common.clear(myvbox);
					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					hbox.setWidth("100%");
					LampiranLain.createDownloadUploadFileLain(hbox, biodataCalonMahasiswaPunyaVerifikasiBerkas.getId(),
							BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lampiranLain = (LampiranLain) arg0.getData();
									if (lampiranLain != null) {
										biodataCalonMahasiswaPunyaVerifikasiBerkas.setUploaded(true);
										biodataCalonMahasiswaPunyaVerifikasiBerkas.setNamaFile(lampiranLain.getNama());
										Common.refreshUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);
									}
								}

							}, null, false, false, false,
							!biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified() && belumExpired, null, false,
							false);

					if (hbox.getAttribute("jumlah_upload") != null) {
						Integer jumlah_upload = (Integer) hbox.getAttribute("jumlah_upload");
						if (jumlah_upload > 0 && !biodataCalonMahasiswaPunyaVerifikasiBerkas.getUploaded()) {
							biodataCalonMahasiswaPunyaVerifikasiBerkas.setUploaded(true);
							Common.refreshUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);
						} else if (jumlah_upload.equals(0)
								&& biodataCalonMahasiswaPunyaVerifikasiBerkas.getUploaded()) {
							biodataCalonMahasiswaPunyaVerifikasiBerkas.setUploaded(false);
							Common.refreshUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);
						}
					}
				}
			});

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					biodataCalonMahasiswaPunyaVerifikasiBerkas.setVerified(checkbox.isChecked());
					biodataCalonMahasiswaPunyaVerifikasiBerkas.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);

				}
			};

			keterangan.addEventListener("onChange", eventListener);
		}

	}

	public Criteria initCriteria(boolean order) {
		Jurusan prodiPilihan = (Jurusan) (searchProdiPilihan.getSelectedItem() == null ? null
				: searchProdiPilihan.getSelectedItem().getValue());
		Jurusan prodiLulus = (Jurusan) (searchProdiLulus.getSelectedItem() == null ? null
				: searchProdiLulus.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.eq("biodataCalonMahasiswa.prodi1", prodiPilihan);
		criterion = Restrictions.or(criterion, Restrictions.eq("biodataCalonMahasiswa.prodi2", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("biodataCalonMahasiswa.prodi3", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("biodataCalonMahasiswa.prodi4", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("biodataCalonMahasiswa.prodi5", prodiPilihan));

		Criteria criteria = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)

				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")

				.add(searchPaket.getSelectedItem() == null || searchPaket.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa.paket", searchPaket.getSelectedItem().getValue()))

				.add(searchJenisSeleksi.getSelectedItem() == null
						|| searchJenisSeleksi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("biodataCalonMahasiswa.jenisSeleksi",
										searchJenisSeleksi.getSelectedItem().getValue()))

				.add(searchGelombang.getSelectedItem() == null || searchGelombang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa.gelombangPendaftaran",
								searchGelombang.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("biodataCalonMahasiswa.tahunAkademik",
										searchTahunAjaran.getSelectedItem().getValue()))

				.createAlias("verifikasiKelengkapanCalonMahasiswa", "verifikasiKelengkapanCalonMahasiswa")

				.add(searchProdiPilihan.getSelectedItem() == null
						|| searchProdiPilihan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: criterion)

				.add(prodiLulus == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa.prodiLulus", prodiLulus))

				.add(tampilkanYgSudahdapatNIM.isChecked()
						? Restrictions.and(Restrictions.isNotNull("biodataCalonMahasiswa.nim"),
								Restrictions.ne("biodataCalonMahasiswa.nim", ""))
						: Restrictions.sqlRestriction("true"))

				.add(tampilkanYgBelumdapatNIM.isChecked()
						? Restrictions.or(Restrictions.isNull("biodataCalonMahasiswa.nim"),
								Restrictions.eq("biodataCalonMahasiswa.nim", ""))
						: Restrictions.sqlRestriction("true"))

				.add(mengisiFormTambahan.isChecked()
						? Restrictions.ne("biodataCalonMahasiswa.parameterTambahanInds", "")
						: Restrictions.sqlRestriction("true"))

		;

		if (belumUploadBerkas.isChecked()) {
			criteria.add(Restrictions.eq("uploaded", false));
		}
		if (telahUploadBerkas.isChecked()) {
			criteria.add(Restrictions.eq("uploaded", true));
		}
		if (belumLolosBerkas.isChecked()) {
			criteria.add(Restrictions.eq("verified", false));
		}
		if (telahLolosBerkas.isChecked()) {
			criteria.add(Restrictions.eq("verified", true));
		}

		if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
				|| tampilkanYgSudahBayarDaftarUlang.isChecked() || tampilkanYgBelumBayarDaftarUlang.isChecked()) {
			criteria.createAlias("biodataCalonMahasiswa.pembayaranDaftarUlang", "pembayaranDaftarUlang",
					Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"),
									Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
			criteria.createAlias("biodataCalonMahasiswa.pembayaranRegistrasi", "pembayaranRegistrasi",
					Criteria.LEFT_JOIN)
					.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
							: Restrictions.sqlRestriction("true"))
					.add(tampilkanYgBelumBayar.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranRegistrasi"),
									Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		if (order)
			criteria.addOrder(Order.desc("tanggal_dirubah"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("verifikasiKelengkapanCalonMahasiswa.nama", searchnama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(searchcalnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("biodataCalonMahasiswa.nama", searchcalnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("biodataCalonMahasiswa.noUjian",
												searchcalnama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi",
												searchcalnama.getValue().trim(), MatchMode.ANYWHERE))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BiodataCalonMahasiswaPunyaVerifikasiBerkas> biodataCalonMahasiswaPunyaVerifikasiBerkas = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(biodataCalonMahasiswaPunyaVerifikasiBerkas);
		grid.setRowRenderer(new BiodataCalonMahasiswaPunyaVerifikasiBerkasRenderer());
		grid.setModelCheckMobile(strset);

	}


	private static String ambilNamaFileLampiran(BiodataCalonMahasiswaPunyaVerifikasiBerkas data) {
		try {
			if (data != null && data.getBiodataCalonMahasiswa() != null) {
				String noRegistrasi = data.getBiodataCalonMahasiswa().getNoRegistrasi();
				String nama = data.getBiodataCalonMahasiswa().getNama();
				String label = (noRegistrasi == null ? "" : noRegistrasi.trim()) + "_" + (nama == null ? "" : nama.trim());
				return label.trim().length() == 0 ? "calon_mahasiswa" : label;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return "calon_mahasiswa";
	}

	private static void copyFile(File sumber, File tujuan) throws Exception {
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			if (tujuan.getParentFile() != null && !tujuan.getParentFile().exists()) {
				tujuan.getParentFile().mkdirs();
			}
			fileInputStream = new FileInputStream(sumber);
			fileOutputStream = new FileOutputStream(tujuan);
			IOUtils.copyLarge(fileInputStream, fileOutputStream);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

}
