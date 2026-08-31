package ais.action.master;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlacklistIp;
import ais.database.model.DetailLogLogin;
import ais.database.model.Dosen;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.render.TampilDetailLog;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk detail log login. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code MyGrid grid}, {@code Paging paging}, {@code Textbox
 * searchnim}, {@code Textbox searchnama}, {@code Textbox searchip}, {@code Textbox searchLinkProfile};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code refreshDashboardAman()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class DetailLogLoginAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;

	private MyGrid grid;

	private Paging paging;
	private Textbox searchnim;
	private Textbox searchnama;
	private Textbox searchip;
	private Textbox searchLinkProfile;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private MyToolbarbuttonConfig find;
	private MyDatebox start;
	private MyDatebox end;

	private String loginFrom = "";
	private MyCheckboxConfig mahasiswa;
	private MyCheckboxConfig dosen;
	private MyCheckboxConfig admin;
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (execution.getParameter("loginFrom") != null) {
			loginFrom = execution.getParameter("loginFrom").trim();
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "halaman", "waktu", "keterangan",
				"logLogin.nama", "logLogin.ip", "logLogin.keterangan", "logLogin.login", "logLogin.logout",
				"logLogin.dosen", "logLogin.pegawai", "logLogin.mahasiswa", "logLogin.tbmuser", "logLogin.jurusan",
				"logLogin.fakultas", "logLogin.siswa", "logLogin.guru", "logLogin.sekolah", "logLogin.yayasan",
				"logLogin.hostname", "logLogin.success_status", "logLogin.description", "logLogin.linkProfile",
				"logLogin.header");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.appendKeToolbar(mahasiswa = new MyCheckboxConfig("Mahasiswa"), find, comp);
		Common.appendKeToolbar(dosen = new MyCheckboxConfig("Dosen"), find, comp);

		Common.appendKeToolbar(siswa = new MyCheckboxConfig("Siswa"), find, comp);
		Common.appendKeToolbar(guru = new MyCheckboxConfig("Guru"), find, comp);

		Common.appendKeToolbar(admin = new MyCheckboxConfig("Admin"), find, comp);

		if (mahasiswa != null) { mahasiswa.setChecked(true); }
		if (dosen != null) { dosen.setChecked(true); }
		if (siswa != null) { siswa.setChecked(true); }
		if (guru != null) { guru.setChecked(true); }
		if (admin != null) { admin.setChecked(true); }

		mahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		dosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		siswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		guru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		admin.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
	}

	class LogLoginRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private Map<Long, BlacklistIp> blacklistIps = ConstantValues.ambilBerdasarClass(BlacklistIp.class);

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			DetailLogLogin detailLogLogin = (DetailLogLogin) arg1;
			final LogLogin logLogin = detailLogLogin.getLogLogin();
			if ((logLogin.getSuccess_status() != null && !logLogin.getSuccess_status())) {
				arg0.setStyle("background-color: rgba(205,92,92,0.4);font-weight: bold;");
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new TampilDetailLog(detail, logLogin));

			RevisiHelper.createNewRevisi(DetailLogLogin.class, detailLogLogin,
					Common.dateFormat3.get().format(detailLogLogin.getWaktu())).setParent(arg0);

			Vbox myVbox = new Vbox();
			myVbox.setParent(arg0);
			new Label(detailLogLogin.getHalaman()).setParent(myVbox);
			new Label(detailLogLogin.getKeterangan()).setParent(myVbox);

			myVbox = new Vbox();
			myVbox.setParent(arg0);

			A myIp = new A(logLogin.getIp());
			myIp.setTarget("new");
			myIp.setHref("http://whatismyipaddress.com/ip/" + logLogin.getIp());
			myIp.setParent(myVbox);

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Blacklist IP ini");
			checkboxConfig.setParent(myVbox);
			for (BlacklistIp blacklistIp : blacklistIps.values()) {
				if (blacklistIp.getAktif() && blacklistIp.getKode().equalsIgnoreCase(logLogin.getIp())) {
					checkboxConfig.setChecked(true);
					break;
				}
			}
			checkboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					BlacklistIp blacklistIpcari = null;
					for (BlacklistIp blacklistIp : blacklistIps.values()) {
						if (blacklistIp.getKode().equalsIgnoreCase(logLogin.getIp())) {
							blacklistIpcari = blacklistIp;
							break;
						}
					}

					Session session = HibernateUtil.currentSession();
					if (blacklistIpcari == null) {
						blacklistIpcari = new BlacklistIp();
						blacklistIpcari.setKode(logLogin.getIp());
						blacklistIpcari.setNama("Blacklist IP " + logLogin.getIp());
					}

					blacklistIpcari.setAktif(checkboxConfig.isChecked());
					Common.refreshSaveOrUpdate(session, blacklistIpcari);
					session.flush();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
		refreshDashboardAman();
		}
					});
				}
			});

			Tbmuser tbmuser = logLogin.getTbmuser();
			Dosen dosen = logLogin.getDosen();
			Mahasiswa mahasiswa = logLogin.getMahasiswa();
			Siswa siswa = logLogin.getSiswa();
			Guru guru = logLogin.getGuru();

			String nama = siswa != null ? siswa.getNama() + " (" + siswa.getNomorIndukNasional() + ")"
					: mahasiswa != null ? mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")"
							: dosen != null ? dosen.getNama() : tbmuser == null ? "" : tbmuser.getUserNama();

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (dosen != null) {
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			} else if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
			} else if (guru != null) {
				CommonMedia.tampilkanGambarKecil(guru).setParent(vbox);
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
			} else {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
			}
			vbox.appendChild(new Label(nama));

			new Label(tbmuser == null || tbmuser.hakAkses() == null
					? (guru != null ? "Guru"
							: siswa != null ? "Siswa"
									: mahasiswa != null ? Common.getBahasa("label_mahasiswa")
											: dosen != null ? Common.getBahasa("label_dosen") : "")
					: tbmuser.hakAkses().getRoleName()).setParent(arg0);
			if (siswa != null) {
				new Label(siswa.getYayasan() == null ? "" : logLogin.getYayasan().getNama()).setParent(arg0);
				new Label(logLogin.getSekolah() == null ? "" : logLogin.getSekolah().getNama()).setParent(arg0);
			} else {

				new Label(logLogin.getFakultas() == null ? "" : logLogin.getFakultas().getNama()).setParent(arg0);
				new Label(logLogin.getJurusan() == null ? "" : logLogin.getJurusan().getNama()).setParent(arg0);
			}

			new Label(logLogin.getSuccess_status() == null || logLogin.getSuccess_status() ? "Sukses" : "Gagal")
					.setParent(arg0);

		}
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterion = Restrictions.sqlRestriction("false");

		if (mahasiswa.isChecked() && dosen.isChecked() && guru.isChecked() && admin.isChecked()) {
			criterion = Restrictions.sqlRestriction("true");
		} else {

			if (mahasiswa.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("logLogin.mahasiswa"));
			}
			if (dosen.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("logLogin.dosen"));
			}

			if (siswa.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("logLogin.siswa"));
			}
			if (guru.isChecked()) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("logLogin.guru"));
			}

			if (admin.isChecked()) {
				criterion = Restrictions
						.or(criterion,
								Restrictions.and(
										Restrictions.and(Restrictions.isNotNull("logLogin.tbmuser"),
												Restrictions.isNull("logLogin.dosen")),
										Restrictions.isNull("logLogin.guru"))

						);
			}
		}

		Session session = HibernateUtil.currentSession();
		session.createSQLQuery("set statement_timeout to 6000000; commit;").executeUpdate();
		Criteria criteria = session.createCriteria(DetailLogLogin.class).createAlias("logLogin", "logLogin")
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.waktu) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))
				.add(criterion);
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(loginFrom == null || loginFrom.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("logLogin.description", loginFrom, MatchMode.START))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("logLogin.fakultas", searchfakultas, false))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("logLogin.jurusan", searchjurusan, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("logLogin.yayasan", searchyayasan, false))
				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("logLogin.sekolah", searchsekolah, false))

				.add(searchnim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("keterangan", searchnim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("halaman", searchnim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchip.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("logLogin.ip", searchip.getValue(), MatchMode.ANYWHERE))

				.add(searchLinkProfile == null || searchLinkProfile.getValue().trim().equals("")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("logLogin.linkProfile", searchLinkProfile.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("logLogin.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)

				);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DetailLogLogin> detailLogLogins = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(detailLogLogins);
		grid.setRowRenderer(new LogLoginRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			// paging.getTotalSize() sudah dihitung onSearchDefault (selalu dipanggil tepat sebelum
			// method ini) -> hindari SELECT count(*) yang sama dijalankan dua kali.
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Detail Akses Menu", "Lihat halaman yang paling sering dibuka dan pola akses pengguna untuk membantu evaluasi pemakaian sistem.",
					paging == null ? -1L : paging.getTotalSize());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/DetailLogLoginAction.java:424");
			}
		}
	}

}
