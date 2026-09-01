package ais.action.master;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.PengecualianJadwalPenilaianDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk pengecualian jadwal penilaian admin. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchdosen}, {@code boolean edit},
 * {@code boolean delete}, {@code PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code tampilkanAksesDitolak()}, {@code onSearchDefault()},
 * {@code ambil()}, {@code ambilClass()}); validasi/perhitungan ({@code bolehProsesStatus()}); mutasi data
 * ({@code onSave()}, {@code setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi domain lain
 * ({@code diajukanOlehPenggunaAktif()}, {@code onAdd()}, {@code form()}, {@code istilah()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PengecualianJadwalPenilaianAdminAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchdosen;

	private boolean edit = false;
	private boolean delete = false;

	private PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;

	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private AmbilDataTbmuserBanbox tbmuser;

	private Checkbox searchaktif;
	private Checkbox searchbelum;
	private Checkbox searchtolak;

	private MyDatebox waktuSampai;
	private boolean persetujuan = false;
	private Combobox tahunAkademik;
	private Combobox semester;
	private boolean approve = false;
	private boolean reject = false;

	private boolean diajukanOlehPenggunaAktif(PengecualianJadwalPenilaianDosen data) {
		Tbmuser pengguna = Common.getCurrentUser();
		return pengguna != null && pengguna.getUserId() != null && data != null && data.getDibuatOleh() != null
				&& data.getDibuatOleh().getUserId() != null
				&& pengguna.getUserId().equalsIgnoreCase(data.getDibuatOleh().getUserId());
	}

	private boolean bolehProsesStatus(PengecualianJadwalPenilaianDosen data) {
		return Common.getApakahAdmin() && approve && reject;
	}

	private void tampilkanAksesDitolak() throws InterruptedException {
		MyMessageboxConfig.show(
				"Status hanya dapat diproses oleh Admin default (roleId am).",
				"Akses Ditolak", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
	}

	public PengecualianJadwalPenilaianAdminAction() {
		super();
	}

	public PengecualianJadwalPenilaianAdminAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		/* Role am adalah penyetuju eksplisit; halaman include tidak selalu membawa
		 * konteks RolePrivilage APPROVE/REJECT yang tepat dari menu induknya. */
		approve = Common.getApakahAdmin();
		reject = Common.getApakahAdmin();

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "dosen", "tbmuser", "tahunAkademik", "jenisSemester", "tanggalMulai",
				"tanggalSampai", "keterangan", "status", "disposisiSop", "dibuatOleh", "disetujuiOleh",
				"tanggalPersetujuanManual", "tanggalPersetujuan", "tanggalPembuatan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PengecualianJadwalPenilaianDosen.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengecualianJadwalPenilaianDosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PengecualianJadwalPenilaianAdminAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PengecualianJadwalPenilaianAdminAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PengecualianJadwalPenilaianAdminAction
	 */
	class PengecualianJadwalPenilaianDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen = (PengecualianJadwalPenilaianDosen) arg1;

			Tbmuser tbmuser = pengecualianJadwalPenilaianDosen.getTbmuser();
			Dosen dosen = pengecualianJadwalPenilaianDosen.getDosen();
			if (tbmuser != null) {
				RevisiHelper.createNewRevisi(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen,
						tbmuser.getUserId()).setParent(arg0);

				new Label(tbmuser.getNama()).setParent(arg0);
				new Label(tbmuser == null || tbmuser.ambilJurusan() == null ? "" : tbmuser.ambilJurusan().getNama())
						.setParent(arg0);
				new Label(tbmuser == null || tbmuser.ambilFakultas() == null ? "" : tbmuser.ambilFakultas().getNama())
						.setParent(arg0);
			} else if (dosen != null) {
				RevisiHelper.createNewRevisi(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen,
						dosen.getNidn()).setParent(arg0);

				new Label(dosen.getNama()).setParent(arg0);
				new Label(dosen == null || dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama())
						.setParent(arg0);
				new Label(dosen == null || dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama())
						.setParent(arg0);
			} else {
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
			}

			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			Common.selectComboItem(true, tahunAkademik, pengecualianJadwalPenilaianDosen.getTahunAkademik());
			tahunAkademik.setParent(arg0);
			tahunAkademik.setWidth("90%");
			tahunAkademik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String tahun = (String) (tahunAkademik.getSelectedItem() == null
							|| tahunAkademik.getSelectedItem().getValue() == null ? ""
									: tahunAkademik.getSelectedItem().getValue());
					pengecualianJadwalPenilaianDosen.setTahunAkademik(tahun);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
				}
			});

			final Combobox semester = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			semester.appendChild(comboitem);
			semester.setReadonly(true);

			semester.setWidth("90%");
			Common.selectComboItem(semester, pengecualianJadwalPenilaianDosen.getJenisSemester());
			semester.setParent(arg0);
			semester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mysemester = (String) (semester.getSelectedItem() == null ? ""
							: semester.getSelectedItem().getValue());

					pengecualianJadwalPenilaianDosen.setJenisSemester(mysemester);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
				}
			});

			final MyDatebox mulai = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalMulai());
			mulai.setWidth("90%");
			mulai.setParent(arg0);
			mulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mymulai = mulai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPenilaianDosen);
					pengecualianJadwalPenilaianDosen.setTanggalMulai(mymulai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPenilaianDosen);
				}
			});

			final MyDatebox sampai = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalSampai());
			sampai.setWidth("90%");
			sampai.setParent(arg0);
			sampai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mysampai = sampai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPenilaianDosen);
					pengecualianJadwalPenilaianDosen.setTanggalSampai(mysampai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPenilaianDosen);
				}
			});
			final Combobox status = new Combobox();

			final EventListener semesterEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mystatus = (String) (status.getSelectedItem() == null
							|| status.getSelectedItem().getValue() == null ? "" : status.getSelectedItem().getValue());

					tahunAkademik.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					semester.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					mulai.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					sampai.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));

				}
			};

			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.PENGAJUAN);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.PENGAJUAN);
			status.appendChild(comboitem);
			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.DISETUJU);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.DISETUJU);
			status.appendChild(comboitem);
			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.DITOLAK);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.DITOLAK);
			status.appendChild(comboitem);
			status.setReadonly(true);

			if (pengecualianJadwalPenilaianDosen.getDisposisiSop() != null) {
				status.setDisabled(true);
			}

			status.setWidth("90%");
			Common.selectComboItem(status, pengecualianJadwalPenilaianDosen.getStatus());

			if (bolehProsesStatus(pengecualianJadwalPenilaianDosen)) {
				status.setParent(arg0);
			} else {
				new Label(pengecualianJadwalPenilaianDosen.getStatus()).setParent(arg0);
			}
			status.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!bolehProsesStatus(pengecualianJadwalPenilaianDosen)) {
						tampilkanAksesDitolak();
						Common.selectComboItem(status, pengecualianJadwalPenilaianDosen.getStatus());
						return;
					}
					String mystatus = (String) (status.getSelectedItem() == null
							|| status.getSelectedItem().getValue() == null ? "" : status.getSelectedItem().getValue());

					if (mystatus.equals(PengecualianJadwalPenilaianDosen.DISETUJU)) {
						pengecualianJadwalPenilaianDosen.setDisetujuiOleh(Common.getCurrentUser());
						pengecualianJadwalPenilaianDosen.setTanggalPersetujuanManual(WaktuUtil.getDate());
					}

					pengecualianJadwalPenilaianDosen.setStatus(mystatus);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
					semesterEventListener.onEvent(null);
				}
			});

			semesterEventListener.onEvent(null);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			hbox.appendChild(new Label(pengecualianJadwalPenilaianDosen.getKeterangan()));

			if (pengecualianJadwalPenilaianDosen.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(hbox);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengecualianJadwalPenilaianDosen.getDisposisiSop().getKeterangan() + " ("
						+ pengecualianJadwalPenilaianDosen.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengecualianJadwalPenilaianDosen.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit, delete, pengecualianJadwalPenilaianDosen,
					PengecualianJadwalPenilaianAdminAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakPengecualianJadwalPenilaianDosen(pengecualianJadwalPenilaianDosen);
				}
			});
			button.setParent(hbx);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PengecualianJadwalPenilaianDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengecualianJadwalPenilaianDosen = (PengecualianJadwalPenilaianDosen) obj;
		init(pengecualianJadwalPenilaianDosen);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pengecualianJadwalPenilaianDosen = (PengecualianJadwalPenilaianDosen) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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

		tahunAkademik = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademik.appendChild(comboitem);
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

		semester = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semester.appendChild(comboitem);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, pengecualianJadwalPenilaianDosen.getTahunAkademik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setWidth("90%");
		Common.selectComboItem(semester, pengecualianJadwalPenilaianDosen.getJenisSemester());
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Admin (*)"));
		tbmuser = new AmbilDataTbmuserBanbox();

		if (persetujuan) {
			row.appendChild(new Label(pengecualianJadwalPenilaianDosen.getTbmuser() == null ? ""
					: pengecualianJadwalPenilaianDosen.getTbmuser().getUserNama()));
		} else {
			row.appendChild(tbmuser);
		}

		tbmuser.setAttribute("myValue", pengecualianJadwalPenilaianDosen.getTbmuser());
		tbmuser.setAttribute("tbmuser", pengecualianJadwalPenilaianDosen.getTbmuser());
		tbmuser.setValue(pengecualianJadwalPenilaianDosen.getTbmuser() == null ? ""
				: pengecualianJadwalPenilaianDosen.getTbmuser().getUserNama());
		tbmuser.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembukaan Izin Penilaian *"));

		if (pengecualianJadwalPenilaianDosen.getId() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 8);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			pengecualianJadwalPenilaianDosen.setTanggalMulai(calendar.getTime());
		}

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		waktu = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalMulai());
		if (persetujuan) {
			hbox.appendChild(new Label(Common.dateFormat6.get().format(pengecualianJadwalPenilaianDosen.getTanggalMulai())));
		} else {
			hbox.appendChild(waktu);
		}
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setCols(6);
		waktu.setReadonly(true);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" sd ")));

		waktuSampai = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalSampai());
		if (persetujuan) {
			hbox.appendChild(new Label(Common.dateFormat6.get().format(pengecualianJadwalPenilaianDosen.getTanggalSampai())));
		} else {
			hbox.appendChild(waktuSampai);
		}
		waktuSampai.setFormat(Common.dateFormat3.get().toPattern());
		waktuSampai.setCols(6);
		waktuSampai.setReadonly(true);

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Keterangan *"));

		keterangan = new Textbox(pengecualianJadwalPenilaianDosen.getKeterangan());
		if (persetujuan) {
			row.appendChild(new Label(pengecualianJadwalPenilaianDosen.getKeterangan()));
		} else {
			rowUsernameDisposisi.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		return grid;
	}

	private void init(final PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen) throws Exception {
		this.pengecualianJadwalPenilaianDosen = pengecualianJadwalPenilaianDosen;
		addWindow.setTitle(pengecualianJadwalPenilaianDosen.getId() == null ? "Tambah Pengajuan Izin Pembukaan Penilaian" : "Ubah Pengajuan Izin Pembukaan Penilaian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengecualianJadwalPenilaianDosen, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

		if (tbmuser.getAttribute("tbmuser") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pengecualian Jadwal Penilaian",
					"Kolom Admin belum Bapak/Ibu pilih, padahal kolom ini wajib diisi sebelum data pengecualian "
							+ "jadwal penilaian dapat disimpan.",
					new String[] {
							"Pilih terlebih dahulu Admin pada kolom yang tersedia.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengecualianJadwalPenilaianDosen.getId() != null) {
			pengecualianJadwalPenilaianDosen = (PengecualianJadwalPenilaianDosen) session
					.load(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen.getId());

		}
		pengecualianJadwalPenilaianDosen.setTbmuser((Tbmuser) tbmuser.getAttribute("tbmuser"));
		pengecualianJadwalPenilaianDosen.setTanggalMulai(waktu.getValue());
		pengecualianJadwalPenilaianDosen.setTanggalSampai(waktuSampai.getValue());
		pengecualianJadwalPenilaianDosen.setTahunAkademik(
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? null
						: tahunAkademik.getSelectedItem().getValue().toString());
		pengecualianJadwalPenilaianDosen.setJenisSemester(
				semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null ? null
						: semester.getSelectedItem().getValue().toString());
		pengecualianJadwalPenilaianDosen.setKeterangan(keterangan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengecualianJadwalPenilaianDosen.setDisposisiSop(disposisiSop);
		}

		if (pengecualianJadwalPenilaianDosen.getId() != null) {
			Common.refreshUpdate(session, pengecualianJadwalPenilaianDosen);
		} else {
			pengecualianJadwalPenilaianDosen.setDisetujuiOleh(null);
			pengecualianJadwalPenilaianDosen.setTanggalPersetujuan(null);
			pengecualianJadwalPenilaianDosen.setStatus(PengecualianJadwalPenilaianDosen.PENGAJUAN);
			session.save(pengecualianJadwalPenilaianDosen);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterion = Restrictions.sqlRestriction("false");
		if (searchaktif.isChecked() && searchbelum.isChecked() && searchtolak.isChecked()) {
			criterion = Restrictions.sqlRestriction("true");
		} else {
			if (searchaktif != null && searchaktif.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.or(Restrictions.isNull("status"),
						Restrictions.eq("status", PengecualianJadwalPenilaianDosen.DISETUJU)));
			}
			if (searchbelum.isChecked()) {
				criterion = Restrictions.or(criterion,
						Restrictions.eq("status", PengecualianJadwalPenilaianDosen.PENGAJUAN));
			}
			if (searchtolak.isChecked()) {
				criterion = Restrictions.or(criterion,
						Restrictions.eq("status", PengecualianJadwalPenilaianDosen.DITOLAK));
			}
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengecualianJadwalPenilaianDosen.class)
				.add(Restrictions.isNull("dosen")).createAlias("tbmuser", "tbmuser", Criteria.INNER_JOIN)

				.add(criterion)

				.add(searchdosen.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("tbmuser.userNama", searchdosen.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userId", searchdosen.getValue().trim(),
										MatchMode.ANYWHERE)))

		;

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengecualianJadwalPenilaianDosen> pengecualianJadwalPenilaianDosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengecualianJadwalPenilaianDosen);
		grid.setRowRenderer(new PengecualianJadwalPenilaianDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Izin Buka Penilaian Admin";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengecualianJadwalPenilaianDosen;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengecualianJadwalPenilaianDosen.class;
	}

	@SuppressWarnings({})
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {

		return null;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
