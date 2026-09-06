package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konsentrasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.MediaParameter;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk foto transkrip. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnim}, {@code Textbox searchnama}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Decimalbox searchtahun}, {@code Combobox searchkonsentrasi}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class FotoTranskripAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private Paging paging;

	private MyGrid grid;
	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchkonsentrasi;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private Combobox kewarganegaraan;
	private MyCheckboxConfig searchdosenPA;
	private AmbilDataDosenBanbox searchdosen;

	private Combobox kelamin;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox status;

	private Combobox program;

	private Combobox semesterMulai;
	private Combobox konsentrasi;
	private Combobox waktuKuliah;

	private BiodataMahasiswaAction biodataMahasiswaAction = new BiodataMahasiswaAction();
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		Tbmuser tbmuser = Common.getCurrentUser();
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		kelamin = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Laki-laki"); }
		if (comboitem != null) { comboitem.setValue("Laki-laki"); }
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Perempuan"); }
		if (comboitem != null) { comboitem.setValue("Perempuan"); }
		kelamin.appendChild(comboitem);

		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		kewarganegaraan.appendChild(comboitem);

		if (biodataMahasiswaAction != null) { biodataMahasiswaAction.setTampilFotoBiodata(false); }

		program = Common.initPrograms(program);
		Common.initPrograms(searchprogram);

		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		fakultas = new Combobox();
		jurusan = new Combobox();
		konsentrasi = new Combobox();
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		/**
		 * Event listener lokal milik {@link FotoTranskripAction}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FotoTranskripAction} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see FotoTranskripAction
		 */
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		/**
		 * Event listener lokal milik {@link FotoTranskripAction}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FotoTranskripAction} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see FotoTranskripAction
		 */
		class JurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(konsentrasi);
				konsentrasi.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(konsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));
			}

		}
		jurusan.addEventListener("onChange", new JurusanEventListener());

		/**
		 * Event listener lokal milik {@link FotoTranskripAction}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FotoTranskripAction} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see FotoTranskripAction
		 */
		class SearchJurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchkonsentrasi);
				searchkonsentrasi.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
			}

		}
		searchjurusan.addEventListener("onChange", new SearchJurusanEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link FotoTranskripAction}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FotoTranskripAction} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see FotoTranskripAction
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		Common.insertCombo(status = new Combobox(), new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		status.setDisabled(tbmuser == null || tbmuser.hakAkses() == null || tbmuser.hakAkses().getRoleId() == null
				|| !tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR));

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		semesterMulai = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		semesterMulai.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		semesterMulai.appendChild(comboitem);

		waktuKuliah = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("PAGI"); }
		if (comboitem != null) { comboitem.setValue("PAGI"); }
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SIANG"); }
		if (comboitem != null) { comboitem.setValue("SIANG"); }
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SORE"); }
		if (comboitem != null) { comboitem.setValue("SORE"); }
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("MALAM"); }
		if (comboitem != null) { comboitem.setValue("MALAM"); }
		waktuKuliah.appendChild(comboitem);

		CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.clear(searchjurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
			searchfakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			Common.clear(konsentrasi);
			Common.clear(searchkonsentrasi);
			Common.insertCombo(konsentrasi, "nama", Konsentrasi.class,
					Restrictions.eq("jurusan", tbmuser.ambilJurusan()));

			Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
					Restrictions.eq("jurusan", tbmuser.ambilJurusan()));
			jurusan.setDisabled(true);
			searchjurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
			searchjurusan.setDisabled(false);
		}

		new MyCheckboxConfig();

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link FotoTranskripAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FotoTranskripAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see FotoTranskripAction
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			final Image image = new Image();
			image.setWidth("100%");
			image.setParent(arg0);

			MediaParameter mediaParameter = new MediaParameter(mahasiswa.getId().toString(), "nama", "foto",
					FotoMahasiswaLulus.class, "mahasiswa", 300, 250);
			String src = CommonMedia.getMedia(mediaParameter);
			image.setSrc(src);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(arg0);

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara()).setParent(arg0);

			new Label(mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getNamaNegara()).setParent(arg0);

			final Label label = new Label();
			label.setParent(arg0);

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label((statusMahasiswa.getNama()) + "/"
					+ (mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama()))
							.setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Ganti Foto" + Common.ukuranLabelFileUpload(),
					"/img/File-Upload-icon.png");
			fileupload.setUpload(Common.ukuranFileUpload());
			fileupload.setUpload(Common.ukuranFileUpload());
			fileupload.setParent(toolbar);
			EventListener eventListener = new EventListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						UploadEvent uploadEvent = (UploadEvent) event;
						if (uploadEvent != null) {
							if (!ais.action.master.helper.generic.AmbilDataTugasFileContent
									.validasiFoto(uploadEvent.getMedia())) return;

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							FotoMahasiswaLulus fotoMahasiswaLulus = (FotoMahasiswaLulus) streamingSession
									.createCriteria(FotoMahasiswaLulus.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1)
									.uniqueResult();
							if (fotoMahasiswaLulus != null) {
								streamingSession.getTransaction().begin();
								streamingSession.delete(fotoMahasiswaLulus);
								streamingSession.getTransaction().commit();
							}

							fotoMahasiswaLulus = new FotoMahasiswaLulus();
							fotoMahasiswaLulus.setNama(uploadEvent.getMedia().getName());
							fotoMahasiswaLulus.setKeterangan(uploadEvent.getMedia().getContentType());
							fotoMahasiswaLulus.setMahasiswa(mahasiswa.getId());

							fotoMahasiswaLulus.setFoto(new javax.sql.rowset.serial.SerialBlob(uploadEvent.getMedia().getByteData()));

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoMahasiswaLulus);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							MediaParameter mediaParameter = new MediaParameter(mahasiswa.getId().toString(), "nama",
									"foto", FotoMahasiswaLulus.class, "mahasiswa", 300, 250);
							String src = CommonMedia.getMedia(mediaParameter);
							image.setSrc(src);
						} else {
							if (mahasiswa.getId() != null) {

								MediaParameter mediaParameter = new MediaParameter(mahasiswa.getId().toString(), "nama",
										"foto", FotoMahasiswaLulus.class, "mahasiswa", 300, 250);
								String src = CommonMedia.getMedia(mediaParameter);
								image.setSrc(src);
							}
						}
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
					}

				}
			};
			fileupload.addEventListener("onUpload", eventListener);

			ais.ui.util.MenuAksiBaris.pasangSelalu(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criteriaStatus)
				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchkonsentrasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("konsentrasi", searchkonsentrasi.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		if (searchnama == null) {
			return;
		}

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))

				.list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
