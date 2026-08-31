package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk verifikasi kelengkapan calon mahasiswa. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkNamaVerifikasiKelengkapanCalonMahasiswa()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
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
public class VerifikasiKelengkapanCalonMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa;
	private MyToolbarbuttonConfig add;
	protected LampiranLain lainMahasiswa;

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

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(VerifikasiKelengkapanCalonMahasiswa.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			String[] verifikasiKelengkapanCalonMahasiswaes = new String[] {
					"Dua (2) lembar fotocopy Ijazah atau Surat Keterangan Hasil Ujian (SKHU) yang telah dilegalisir dari sekolah",
					"Fotocopy raport yang telah dilegalisir dari kelas 10 s.d kelas 12 smt.1",
					"Bukti prestasi asli dan 1 (satu ) lembar fotocopy sertifikat kejuaraan/lomba/olimpiade bidang sains, teknologi, serta seni lukis dan/atau seni rupa ditingkat nasional/internasional dan olahraga sesuai data yang diupload sebanyak 1 (satu) lembar",
					"Satu (1) lembar fotocopy identitas diri (Kartu Pelajar/KTP/SIM)", "Kartu peserta SNMPTN",
					"Satu (1) lembar fotocopy Kartu Keluarga yang telah dilegalisir pejabat yang berwenang",
					"Surat keterangan tidak mampu dari lurah setempat dan kartu Raskin atau kartu penerima BLSM bagi peserta dari keluarga tidak mampu",
					"Satu (1) lembar fotocopy rekening listrik 1 bulan terakhir",
					"Foto rumah (tampak depan, samping kiri, kanan belakang dan dalam rumah) sesuai alamat yang tercantum di kartu keluarga   dan ditandatangani oleh orang tua / wali dan RT setempat",
					"Kartu tanda/bukti pelamar BIDIKMISI bagi calon BIDIKMISI",
					"Formulir Rekomendasi Kepala Sekolah bagi calon BIDIKMISI",
					"Formulir Pendaftaran bagi calon BIDIKMISI",
					"Tiga (3) lembar materai Rp. 6000,- untuk calon mahasiswa Bidikmisi, dua (2) lembar untuk calon non bidikmisi",
					"Dua (2) lembar pas photo warna ukuran 3 x 4 terbaru." };
			for (String k : verifikasiKelengkapanCalonMahasiswaes) {
				if (k != null) {
					VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa = new VerifikasiKelengkapanCalonMahasiswa();
					verifikasiKelengkapanCalonMahasiswa.setNama(k.toString().trim());
					verifikasiKelengkapanCalonMahasiswa.setKeterangan("" + k.toString().trim());
					session.save(verifikasiKelengkapanCalonMahasiswa);
				}
			}

		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "aktif", "wajib", "verifikasi", "wajibUploadSebelumUjian", "wajibUploadSebelumInterview", "wajibVerifikasiSebelumUjian", "wajibVerifikasiSebelumInterview" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, VerifikasiKelengkapanCalonMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link VerifikasiKelengkapanCalonMahasiswaAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link VerifikasiKelengkapanCalonMahasiswaAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see VerifikasiKelengkapanCalonMahasiswaAction
	 */
	class VerifikasiKelengkapanCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa = (VerifikasiKelengkapanCalonMahasiswa) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(VerifikasiKelengkapanCalonMahasiswa.class,
					verifikasiKelengkapanCalonMahasiswa, verifikasiKelengkapanCalonMahasiswa.getNama()))
							.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonMahasiswa.getId(),
					VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", false, null, null, false, false,
					false, false);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(verifikasiKelengkapanCalonMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});
			final MyCheckboxConfig verifikasi = new MyCheckboxConfig("Wajib Verifikasi");
			final MyCheckboxConfig wajib = new MyCheckboxConfig("Wajib Upload");
			wajib.setDisabled(!edit);
			wajib.setChecked(verifikasiKelengkapanCalonMahasiswa.getWajib());
			wajib.setParent(arg0);
			arg0.setAttribute("wajib", wajib);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setWajib(wajib.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);

					verifikasi.setDisabled(!wajib.isChecked());
					if (!wajib.isChecked()) {
						verifikasi.setChecked(false);
					}
				}
			});
			verifikasi.setDisabled(!edit || !wajib.isChecked());
			verifikasi.setChecked(verifikasiKelengkapanCalonMahasiswa.getVerifikasi());
			verifikasi.setParent(arg0);
			arg0.setAttribute("verifikasi", verifikasi);
			verifikasi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setVerifikasi(verifikasi.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});

			final MyCheckboxConfig cbUjian = new MyCheckboxConfig("Wajib Upload sebelum Ujian");
			cbUjian.setDisabled(!edit);
			cbUjian.setChecked(verifikasiKelengkapanCalonMahasiswa.getWajibUploadSebelumUjian());
			cbUjian.setParent(arg0);
			cbUjian.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setWajibUploadSebelumUjian(cbUjian.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});

			final MyCheckboxConfig cbInterview = new MyCheckboxConfig("Wajib Upload sebelum Interview");
			cbInterview.setDisabled(!edit);
			cbInterview.setChecked(verifikasiKelengkapanCalonMahasiswa.getWajibUploadSebelumInterview());
			cbInterview.setParent(arg0);
			cbInterview.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setWajibUploadSebelumInterview(cbInterview.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});

			final MyCheckboxConfig cbVerifUjian = new MyCheckboxConfig("Wajib Verifikasi sebelum Ujian");
			cbVerifUjian.setDisabled(!edit);
			cbVerifUjian.setChecked(verifikasiKelengkapanCalonMahasiswa.getWajibVerifikasiSebelumUjian());
			cbVerifUjian.setParent(arg0);
			cbVerifUjian.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setWajibVerifikasiSebelumUjian(cbVerifUjian.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});

			final MyCheckboxConfig cbVerifInterview = new MyCheckboxConfig("Wajib Verifikasi sebelum Interview");
			cbVerifInterview.setDisabled(!edit);
			cbVerifInterview.setChecked(verifikasiKelengkapanCalonMahasiswa.getWajibVerifikasiSebelumInterview());
			cbVerifInterview.setParent(arg0);
			cbVerifInterview.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonMahasiswa.setWajibVerifikasiSebelumInterview(cbVerifInterview.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonMahasiswa);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(verifikasiKelengkapanCalonMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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
											Common.refreshDelete(verifikasiKelengkapanCalonMahasiswa);
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
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new VerifikasiKelengkapanCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa) {
		this.verifikasiKelengkapanCalonMahasiswa = verifikasiKelengkapanCalonMahasiswa;
		addWindow.setTitle(verifikasiKelengkapanCalonMahasiswa.getId() == null ? "Tambah Verifikasi Kelengkapan Berkas Calon Mahasiswa" : "Ubah Verifikasi Kelengkapan Berkas Calon Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Verifikasi Kelengkapan"));
		row.appendChild(nama = new Textbox(verifikasiKelengkapanCalonMahasiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(verifikasiKelengkapanCalonMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Berkas"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonMahasiswa.getId(),
				VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran lebih dari satu file, zip dulu semua file tersebut");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Verifikasi Kelengkapan Calon Mahasiswa",
					"Kolom Nama Verifikasi Kelengkapan Calon Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Verifikasi Kelengkapan Calon Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaVerifikasiKelengkapanCalonMahasiswa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Verifikasi Kelengkapan Calon Mahasiswa",
					"Nama Verifikasi Kelengkapan Calon Mahasiswa sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama verifikasi kelengkapan calon mahasiswa yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (verifikasiKelengkapanCalonMahasiswa.getId() != null) {
			verifikasiKelengkapanCalonMahasiswa = (VerifikasiKelengkapanCalonMahasiswa) session
					.load(VerifikasiKelengkapanCalonMahasiswa.class, verifikasiKelengkapanCalonMahasiswa.getId());

		}

		verifikasiKelengkapanCalonMahasiswa.setNama(nama.getValue());
		verifikasiKelengkapanCalonMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, verifikasiKelengkapanCalonMahasiswa);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(verifikasiKelengkapanCalonMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(VerifikasiKelengkapanCalonMahasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(verifikasiKelengkapanCalonMahasiswa);
		grid.setRowRenderer(new VerifikasiKelengkapanCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaVerifikasiKelengkapanCalonMahasiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(VerifikasiKelengkapanCalonMahasiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.verifikasiKelengkapanCalonMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.verifikasiKelengkapanCalonMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
