package ais.action.master;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.DefaultJenisParsingReconsile;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.UserOnlineCounter;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.util.ClientCredentials;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.common.MemoryDbUtil;
import ais.common.PmbArkatama;
import ais.common.gdrive.BackupUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import nl.captcha.backgrounds.FlatColorBackgroundProducer;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.backgrounds.SquigglesBackgroundProducer;
import nl.captcha.backgrounds.TransparentBackgroundProducer;
import nl.captcha.gimpy.BlockGimpyRenderer;
import nl.captcha.gimpy.DropShadowGimpyRenderer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.gimpy.RippleGimpyRenderer;
import nl.captcha.gimpy.ShearGimpyRenderer;
import nl.captcha.noise.CurvedLineNoiseProducer;
import nl.captcha.noise.StraightLineNoiseProducer;
import nl.captcha.text.producer.ChineseTextProducer;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.producer.FiveLetterFirstNameTextProducer;
import nl.captcha.text.renderer.ColoredEdgesWordRenderer;
import nl.captcha.text.renderer.DefaultWordRenderer;

/**
 * Controller/action ZK untuk konfigurasi new. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabs tabsKonfigurasi}, {@code Tabpanels
 * tabpanelsKonfigurasi}, {@code org.zkoss.zul.Div outerTabsDiv}, {@code ais.ui.util.MyButtonTabbox mbt}, {@code
 * int mbtNextIdx}, {@code Tabpanel downloadFileAutoBackup}, {@code Tabpanel backupLogData}, {@code Row row};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code doAfterComposeOri()},
 * {@code initTabSistemInti()}, {@code initTabWebsite()}, {@code initTabModulAplikasi()}); pembacaan/pencarian
 * ({@code onDownloadFileAutoBackup()}, {@code buildSkripPencarianKonfigurasi()}, {@code
 * jadwalkanPencarianKonfigurasi()}, {@code getKonfigurasiNilai()}, {@code getThrowableMessage()}, {@code
 * onSearchDefault()}); validasi/perhitungan ({@code isValidBackupFile()}); mutasi data ({@code
 * setLabelValueQuietly()}, {@code resetTabKonfigurasi()}); penghapusan/pembatalan ({@code deleteQuietly()});
 * operasi domain lain ({@code onBackupLog()}, {@code createBackupDriveProbeFile()}, {@code
 * prepareDesktopForBackgroundAlert()}, {@code containsIgnoreCase()}, {@code isPgDumpNotFound()}, {@code
 * buildBackupErrorMessage()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class KonfigurasiNewAction extends GenericAutowireComposer {

	/**
	 *
	 */
	protected static final long serialVersionUID = -5779730267402400328L;
	protected Tabs tabsKonfigurasi;
	protected Tabpanels tabpanelsKonfigurasi;
	protected org.zkoss.zul.Div outerTabsDiv;
	protected ais.ui.util.MyButtonTabbox mbt;
	protected int mbtNextIdx = 0;

	protected Tabpanel downloadFileAutoBackup;
	protected Tabpanel backupLogData;

	// Variabel kerja yang dipakai lintas method tab.
	// Dipertahankan sebagai field agar hasil reorganisasi method per tab tetap kompatibel
	// dengan potongan kode lama yang sebelumnya berada dalam satu method besar onSearchDefault().
	protected Row row;
	protected Groupbox groupbox;
	protected Hbox hbox;
	protected MyButtonConfig button;
	protected String defaultValue;

	public void onDownloadFileAutoBackup(Event event) {
		if (downloadFileAutoBackup == null) {
			return;
		}
		if (downloadFileAutoBackup.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(downloadFileAutoBackup);
			MyInclude iframe = new MyInclude("/pages/master/download_auto_backup_action.zul");
			iframe.setParent(window);
		}
	}

	public void onBackupLog(Event event) {
		if (backupLogData == null) {
			return;
		}
		if (backupLogData.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(backupLogData);
			MyInclude iframe = new MyInclude("/pages/master/backup_log.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (outerTabsDiv == null && (tabsKonfigurasi == null || tabpanelsKonfigurasi == null)) {
			return;
		}

		if (outerTabsDiv == null && Common.isMobile()) {
			tabsKonfigurasi.setWidth("30px");
		}

		if (outerTabsDiv == null) {
			Tbmuser tbmuserH = Common.getCurrentUser();
			if (tbmuserH != null && tbmuserH.getUserId() != null) {
				Integer desktopHeight = MainAction.desktopHeights.get(tbmuserH.getUserId());
				if (desktopHeight != null) {
					tabsKonfigurasi.setHeight((desktopHeight * 0.96) + "px");
				}
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
				try {
					Clients.evalJavaScript(buildSkripPencarianKonfigurasi());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

	/**
	 * Kotak pencarian global konfigurasi — best practice halaman pengaturan
	 * besar (pola Settings VS Code/WordPress): satu kotak cari di atas tabbox
	 * menyaring SEMUA baris konfigurasi pada tab yang sedang aktif berdasarkan
	 * label, nama kunci, maupun nilai. Baris yang tidak cocok disembunyikan dan
	 * penghitung "cocok/total" ditampilkan; mengosongkan kotak mengembalikan
	 * semua baris. Bekerja murni di sisi client sehingga tidak membebani server.
	 */
	/**
	 * Membangun skrip JS yang menyisipkan tombol "Cari" di bawah rail tab
	 * (di dalam .z-tabs, BUKAN di luar panel). Klik tombol membuka modal
	 * popup tiga-field: Label/Kunci, Nilai, Keterangan — pencarian AND.
	 * Hasilnya diterapkan ke semua tabpanel (atau hanya tab aktif bila
	 * checkbox "semua tab" dimatikan). Badge merah di tiap tab menampilkan
	 * jumlah baris cocok; chip kuning di bawah tombol memperlihatkan
	 * ringkasan filter aktif dan bisa diklik untuk reset.
	 */
	protected String buildSkripPencarianKonfigurasi() {
		String uuid = tabsKonfigurasi == null ? "" : tabsKonfigurasi.getUuid();
		StringBuilder js = new StringBuilder();
		js.append("(function(){try{");
		js.append("if(document.getElementById('aisKonfigCariBox')){return;}");
		// Bersihkan overlay modal basi dari kunjungan sebelumnya (overlay ditambahkan ke body
		// sehingga tidak ikut terhapus saat tabbox lama dilepas) agar tidak menumpuk.
		js.append("var oldOv=document.getElementById('aisKonfigCariOverlay');if(oldOv&&oldOv.parentNode){oldOv.parentNode.removeChild(oldOv);}");

		// --- Temukan tabbox dari UUID tabs ---
		js.append("var tabsEl=document.getElementById('").append(uuid).append("');if(!tabsEl){return;}");
		js.append("var tabbox=tabsEl;");
		js.append("while(tabbox&&(' '+(tabbox.className||'')+' ').indexOf(' z-tabbox ')<0){tabbox=tabbox.parentNode;}");
		js.append("if(!tabbox){return;}");
		// --- Tombol "Cari" MELAYANG (floating action button) di kanan-bawah, tepat di atas
		// tombol scroll-to-top. Menggantikan bar pencarian full-width lama yang dulu menempel
		// di bawah rail tab. Wrap ditempel ke TABBOX (bukan document.body) agar hanya tampil di
		// halaman Pengaturan Konfigurasi & ikut hilang saat pindah ke tab aplikasi lain.
		js.append("var btnWrap=document.createElement('div');btnWrap.id='aisKonfigCariBox';");
		js.append("btnWrap.style.cssText='position:fixed;right:18px;bottom:80px;z-index:19998;display:flex;flex-direction:column-reverse;align-items:flex-end;gap:6px;';");

		js.append("var btnCari=document.createElement('button');");
		js.append("btnCari.innerHTML='\\uD83D\\uDD0D\\u00A0Cari';");
		js.append("btnCari.style.cssText='min-height:44px;padding:0 18px;background:#1e3a5f;color:#ffffff!important;border:none;border-radius:999px;cursor:pointer;font-size:13px;font-weight:800;letter-spacing:.2px;box-sizing:border-box;box-shadow:0 4px 14px rgba(15,63,109,.38);line-height:1.2;display:flex;align-items:center;justify-content:center;white-space:nowrap;';");
		js.append("btnCari.onmouseover=function(){this.style.background='#2d5a8e';};");
		js.append("btnCari.onmouseout=function(){this.style.background='#1e3a5f';};");

		// chip kuning – ringkasan filter aktif, klik = reset (melayang di atas tombol)
		js.append("var chipAktif=document.createElement('div');chipAktif.id='aisKonfigCariChip';");
		js.append("chipAktif.style.cssText='display:none;max-width:200px;padding:4px 8px;background:#fef3c7;border:1px solid #f59e0b;border-radius:8px;font-size:9px;color:#92400e;text-align:center;cursor:pointer;word-break:break-word;line-height:1.4;box-shadow:0 2px 8px rgba(15,23,42,.15);';");
		js.append("chipAktif.title='Klik untuk reset filter';");

		js.append("btnWrap.appendChild(btnCari);btnWrap.appendChild(chipAktif);");
		js.append("tabbox.appendChild(btnWrap);");

		// --- Modal overlay (position:fixed, ditambah ke document.body) ---
		js.append("var overlay=document.createElement('div');overlay.id='aisKonfigCariOverlay';");
		js.append("overlay.style.cssText='display:none;position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(15,23,42,.52);z-index:19999;';");

		js.append("var modal=document.createElement('div');");
		js.append("modal.style.cssText='position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);");
		js.append("width:400px;max-width:94vw;background:#fff;border-radius:14px;padding:22px 24px 18px;");
		js.append("box-shadow:0 12px 40px rgba(15,23,42,.25);box-sizing:border-box;';");

		js.append("var mtitle=document.createElement('div');");
		js.append("mtitle.innerHTML='\\uD83D\\uDD0D\\u00A0<strong>Pencarian Konfigurasi</strong>';");
		js.append("mtitle.style.cssText='font-size:15px;color:#1e3a5f;margin-bottom:16px;';");
		js.append("modal.appendChild(mtitle);");

		// helper buat field label+input
		js.append("var inpNama,inpNilai,inpKet;");
		js.append("(function(){");
		js.append("function mk(lbl,ph){");
		js.append("var w=document.createElement('div');w.style.cssText='margin-bottom:12px;';");
		js.append("var l=document.createElement('label');l.textContent=lbl;");
		js.append("l.style.cssText='display:block;font-size:10.5px;font-weight:700;color:#475569;margin-bottom:4px;text-transform:uppercase;letter-spacing:.5px;';");
		js.append("w.appendChild(l);");
		js.append("var i=document.createElement('input');i.type='text';i.placeholder=ph;");
		js.append("i.style.cssText='width:100%;padding:8px 10px;border:1.5px solid #cbd5e1;border-radius:8px;font-size:13px;box-sizing:border-box;outline:none;color:#1e293b;';");
		js.append("i.onfocus=function(){this.style.borderColor='#2563eb';this.style.boxShadow='0 0 0 3px rgba(37,99,235,.12)';};");
		js.append("i.onblur=function(){this.style.borderColor='#cbd5e1';this.style.boxShadow='none';};");
		js.append("w.appendChild(i);modal.appendChild(w);return i;}");
		js.append("inpNama=mk('Label / Nama Kunci','Contoh: nama kampus, smtp_host...');");
		js.append("inpNilai=mk('Nilai','Contoh: Aktif, ya, 1, true...');");
		js.append("inpKet=mk('Keterangan','Kata kunci dalam deskripsi...');");
		js.append("})();");

		// checkbox semua tab
		js.append("var cbWrap=document.createElement('div');cbWrap.style.cssText='margin-bottom:14px;display:flex;align-items:center;gap:7px;';");
		js.append("var cbAll=document.createElement('input');cbAll.type='checkbox';cbAll.id='aisKonfigCbAll';cbAll.checked=true;");
		js.append("cbAll.style.cssText='width:14px;height:14px;cursor:pointer;';");
		js.append("var cbLbl=document.createElement('label');cbLbl.htmlFor='aisKonfigCbAll';");
		js.append("cbLbl.textContent='Cari di semua tab (termasuk tab tidak aktif)';");
		js.append("cbLbl.style.cssText='font-size:12px;color:#475569;cursor:pointer;';");
		js.append("cbWrap.appendChild(cbAll);cbWrap.appendChild(cbLbl);modal.appendChild(cbWrap);");

		// divider
		js.append("var hr=document.createElement('hr');hr.style.cssText='border:none;border-top:1px solid #e2e8f0;margin:0 0 14px;';modal.appendChild(hr);");

		// tombol Cari / Reset / Tutup
		js.append("var bRow=document.createElement('div');bRow.style.cssText='display:flex;gap:8px;';");
		js.append("var bOk=document.createElement('button');bOk.textContent='Cari';");
		js.append("bOk.style.cssText='flex:1;padding:9px 16px;background:#1e3a5f;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:13px;font-weight:700;';");
		js.append("bOk.onmouseover=function(){this.style.background='#2d5a8e';};bOk.onmouseout=function(){this.style.background='#1e3a5f';};");
		js.append("var bReset=document.createElement('button');bReset.textContent='Reset';");
		js.append("bReset.style.cssText='padding:9px 13px;background:#f1f5f9;color:#475569;border:1px solid #cbd5e1;border-radius:8px;cursor:pointer;font-size:13px;';");
		js.append("var bTutup=document.createElement('button');bTutup.textContent='Tutup';");
		js.append("bTutup.style.cssText='padding:9px 13px;background:#f1f5f9;color:#475569;border:1px solid #cbd5e1;border-radius:8px;cursor:pointer;font-size:13px;';");
		js.append("bRow.appendChild(bOk);bRow.appendChild(bReset);bRow.appendChild(bTutup);modal.appendChild(bRow);");

		js.append("overlay.appendChild(modal);document.body.appendChild(overlay);");

		// --- Fungsi saring ---
		js.append("function saring(q1,q2,q3,semuaTab){try{");
		js.append("q1=(q1||'').toLowerCase().replace(/^\\s+|\\s+$/g,'');");
		js.append("q2=(q2||'').toLowerCase().replace(/^\\s+|\\s+$/g,'');");
		js.append("q3=(q3||'').toLowerCase().replace(/^\\s+|\\s+$/g,'');");
		js.append("var aktif=!!(q1||q2||q3);");
		js.append("var panels=tabbox.querySelectorAll('.z-tabpanel');");
		js.append("var tabEls=tabbox.querySelectorAll('.z-tab');");
		js.append("var totalCocok=0,totalSemua=0;");
		js.append("for(var p=0;p<panels.length;p++){");
		js.append("var tab=tabEls[p];");
		js.append("if(!semuaTab&&tab&&tab.style.display==='none'){continue;}");
		js.append("var rows=panels[p].querySelectorAll('tr.z-row');var n=0;");
		js.append("for(var i=0;i<rows.length;i++){");
		js.append("var r=rows[i];var cells=r.querySelectorAll('td');");
		js.append("var tNama=(cells.length>0?(cells[0].innerText||cells[0].textContent||''):'').toLowerCase();");
		js.append("var tNilai=(cells.length>1?(cells[1].innerText||cells[1].textContent||''):'').toLowerCase();");
		js.append("var tKet=(r.innerText||r.textContent||'').toLowerCase();");
		js.append("var ok=(!q1||tNama.indexOf(q1)>=0)");
		js.append("&&(!q2||tNilai.indexOf(q2)>=0)");
		js.append("&&(!q3||tKet.indexOf(q3)>=0);");
		js.append("r.style.display=ok?'':'none';if(ok){n++;}}");
		js.append("totalCocok+=n;totalSemua+=rows.length;");
		js.append("if(tab){var b=tab.querySelector('.ais-konfig-tab-badge');");
		js.append("if(aktif&&rows.length>0){");
		js.append("if(!b){b=document.createElement('span');b.className='ais-konfig-tab-badge';");
		js.append("b.style.cssText='margin-left:4px;background:#dc3545;color:#fff;border-radius:999px;padding:0 5px;font-size:9px;font-weight:800;display:inline-block;line-height:14px;vertical-align:middle;';");
		js.append("var cnt=tab.querySelector('.z-tab-text')||tab;cnt.appendChild(b);}");
		js.append("b.textContent=String(n);b.style.display=n>0?'inline-block':'none';");
		js.append("tab.style.opacity=n>0?'1':'.35';");
		js.append("}else{if(b){b.style.display='none';}tab.style.opacity='1';}}");
		js.append("}"); // end for panels
		js.append("chipAktif.style.display=aktif?'block':'none';");
		js.append("if(aktif){chipAktif.textContent=totalCocok+'/'+totalSemua+' cocok \\u2014 klik reset';}");
		js.append("}catch(e){}}");

		// --- Open / close modal ---
		js.append("function bukaModal(){overlay.style.display='block';setTimeout(function(){inpNama.focus();},60);}");
		js.append("function tutupModal(){overlay.style.display='none';}");
		js.append("function resetFilter(){inpNama.value='';inpNilai.value='';inpKet.value='';saring('','','',true);}");

		// --- Wire events ---
		js.append("btnCari.onclick=function(){bukaModal();};");
		js.append("bOk.onclick=function(){saring(inpNama.value,inpNilai.value,inpKet.value,cbAll.checked);tutupModal();};");
		js.append("bReset.onclick=function(){resetFilter();tutupModal();};");
		js.append("bTutup.onclick=function(){tutupModal();};");
		js.append("chipAktif.onclick=function(){resetFilter();};");

		// Enter di input = klik Cari
		js.append("function onEnter(e){if((e.keyCode||e.which)===13){bOk.click();}}");
		js.append("inpNama.addEventListener('keydown',onEnter);");
		js.append("inpNilai.addEventListener('keydown',onEnter);");
		js.append("inpKet.addEventListener('keydown',onEnter);");

		// Klik backdrop = tutup modal
		js.append("overlay.addEventListener('click',function(e){if(e.target===overlay){tutupModal();}});");

		// Pindah tab → terapkan ulang filter jika aktif
		js.append("document.addEventListener('click',function(){");
		js.append("if(inpNama.value||inpNilai.value||inpKet.value){");
		js.append("setTimeout(function(){saring(inpNama.value,inpNilai.value,inpKet.value,cbAll.checked);},300);}");
		js.append("},true);");

		js.append("}catch(e){}})();");
		return js.toString();
	}

	/**
	 * Menjadwalkan pemasangan kotak pencarian SEKALI per halaman. Dipanggil dari
	 * createSpan() sehingga SEMUA class turunan KonfigurasiNewAction
	 * (KonfigurasiDashboardAction, KonfigurasiSekolahAction, KonfigurasiLogoAction,
	 * KonfigurasiPerpustakaanAction, KonfigurasiTampilan*Action, dst.) otomatis
	 * mendapat fitur pencarian tanpa perlu mengubah kodenya masing-masing —
	 * mereka membangun tab lewat createSpan() yang sama.
	 */
	private boolean pencarianKonfigurasiTerjadwal = false;

	protected void jadwalkanPencarianKonfigurasi() {
		if (pencarianKonfigurasiTerjadwal) {
			return;
		}
		pencarianKonfigurasiTerjadwal = true;
		try {
			Clients.evalJavaScript(buildSkripPencarianKonfigurasi());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:422");
			// Jangan ganggu pembangunan halaman hanya karena pencarian gagal dipasang.
		}
	}

	public void doAfterComposeOri(Component comp) throws Exception {
		super.doAfterCompose(comp);
	}

	private String getKonfigurasiNilai(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			return konfigurasi == null || konfigurasi.getNilai() == null ? defaultValue : konfigurasi.getNilai().trim();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private File createBackupDriveProbeFile(String prefix) throws IOException {
		String dirName = getKonfigurasiNilai("lokasi_directory_file_backup", "/backup/");
		File dir = new File(dirName == null || dirName.trim().isEmpty() ? System.getProperty("java.io.tmpdir") : dirName);
		try {
			if (!dir.exists()) {
				dir.mkdirs();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
			dir = new File(System.getProperty("java.io.tmpdir"));
		}
		File file = File.createTempFile(prefix == null ? "ecampus_backup_drive_probe_" : prefix, ".txt", dir);
		ais.common.BacaTulisUtil.tulis(file,
				"ECAMPUS_BACKUP_DRIVE_PROBE " + Common.datetimeFormat2s.get().format(WaktuUtil.getDate()));
		file.deleteOnExit();
		return file;
	}

	private boolean isValidBackupFile(File file) {
		return file != null && file.exists() && file.isFile() && file.length() > 0;
	}

	private void setLabelValueQuietly(Label label, String value) {
		if (label == null) {
			return;
		}
		try {
			label.setValue(value == null ? "" : value);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void deleteQuietly(File file) {
		try {
			if (file != null && file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	/**
	 * OPTIMASI FASE 5: dahulu method ini memanggil {@code desktop.enableServerPush(true)}
	 * secara langsung dan TIDAK PERNAH mematikannya, sehingga browser terus melakukan polling
	 * (masing-masing menahan satu thread Tomcat) selama tab terbuka walau proses backup sudah
	 * lama selesai. Sekarang push dinyalakan lewat reference counting AsyncTaskManager dan
	 * DILEPAS di {@link #showBackupAlert} yang merupakan titik akhir seluruh alur backup.
	 */
	private Desktop prepareDesktopForBackgroundAlert() {
		try {
			Desktop desktop = Executions.getCurrent() == null ? null : Executions.getCurrent().getDesktop();
			ais.common.AsyncTaskManager.tambahPush(desktop);
			return desktop;
		} catch (Exception e) {
			return null;
		}
	}

	private String getThrowableMessage(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.trim().length() > 0) {
				if (sb.length() > 0) {
					sb.append("\nDisebabkan oleh: ");
				}
				sb.append(message.trim());
			}
			current = current.getCause();
		}
		return sb.toString();
	}

	private boolean containsIgnoreCase(String value, String pattern) {
		return value != null && pattern != null && value.toLowerCase().indexOf(pattern.toLowerCase()) >= 0;
	}

	private boolean isPgDumpNotFound(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (containsIgnoreCase(message, "Cannot run program")
					&& (containsIgnoreCase(message, "pg_dump") || containsIgnoreCase(message, "error=2")
							|| containsIgnoreCase(message, "No such file"))) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String buildBackupErrorMessage(String proses, Throwable throwable) {
		StringBuilder sb = new StringBuilder();
		sb.append(proses == null ? "Backup database" : proses).append(" gagal diproses.");
		sb.append("\n\nDetail error:\n").append(getThrowableMessage(throwable));

		String pgDump = getKonfigurasiNilai("lokasi_pg_dump", "pg_dump");
		String backupDir = getKonfigurasiNilai("lokasi_directory_file_backup", "/backup/");
		sb.append("\n\nKonfigurasi saat ini:");
		sb.append("\n- Perintah pg_dump: ").append(pgDump == null || pgDump.trim().length() == 0 ? "pg_dump" : pgDump);
		sb.append("\n- Lokasi backup sementara: ").append(backupDir == null || backupDir.trim().length() == 0 ? "/backup/" : backupDir);

		if (isPgDumpNotFound(throwable)) {
			sb.append("\n\nKemungkinan penyebab utama: command pg_dump tidak ditemukan oleh proses Java/Tomcat.");
			sb.append("\nSolusi:");
			sb.append("\n1. Install PostgreSQL client pada server aplikasi, misalnya paket postgresql-client.");
			sb.append("\n2. Isi konfigurasi 'Perintah pg_dump' dengan path lengkap, contoh: /usr/bin/pg_dump atau /usr/pgsql-*/bin/pg_dump.");
			sb.append("\n3. Pastikan user yang menjalankan Tomcat/JBoss mempunyai permission execute ke pg_dump.");
			sb.append("\n4. Pastikan folder backup sementara dapat ditulis oleh user aplikasi.");
		}
		return sb.toString();
	}

	private void showBackupAlert(final Desktop desktop, final String title, final String message, final Throwable throwable) {
		final String safeTitle = title == null || title.trim().length() == 0 ? "Backup Database" : title;
		final String safeMessage = message == null || message.trim().length() == 0 ? "Terjadi kesalahan backup database." : message;
		boolean shown = false;
		try {
			if (desktop != null) {
				Executions.activate(desktop);
				try {
					MyMessageboxConfig.show(safeMessage, safeTitle, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					shown = true;
				} finally {
					Executions.deactivate(desktop);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (!shown) {
			try {
				MyMessageboxConfig.show(safeMessage, safeTitle, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				shown = true;
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		if (!shown) {
			try {
				Messagebox.show(safeMessage, safeTitle, Messagebox.OK, Messagebox.EXCLAMATION);
				shown = true;
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		try {
			if (throwable != null) {
				if (throwable instanceof Exception) {
					Common.tampilErrorJikaAdmin((Exception) throwable);
				} else {
					Common.tampilErrorJikaAdmin(new Exception(throwable));
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		/* OPTIMASI FASE 5: titik AKHIR alur backup -- lepaskan server push yang dinyalakan
		 * prepareDesktopForBackgroundAlert(). Reference counting menjamin push baru benar-benar
		 * dimatikan bila tidak ada tugas async lain yang masih berjalan pada desktop ini. */
		try {
			ais.common.AsyncTaskManager.lepasPush(desktop);
		} catch (Throwable abaikan) {
		}
	}

	private void showBackupAlert(final Desktop desktop, final String proses, final Throwable throwable) {
		showBackupAlert(desktop, "Backup Database Gagal", buildBackupErrorMessage(proses, throwable), throwable);
	}

	/**
	 * Inisialisasi seluruh tab konfigurasi.
	 *
	 * Versi ini sengaja dibuat berbasis method per tab agar file lebih mudah dirawat.
	 * Setiap tab vertical memiliki method initTab... sendiri.
	 */
	@SuppressWarnings("unchecked")
	/*
	 * Pemuatan halaman konfigurasi dipecah menjadi tiga fase yang masing-masing
	 * berjalan pada event timer terpisah, supaya kartu progress (pola
	 * DasboardSop) benar-benar terlihat bertahap saat data yang dimuat banyak.
	 */
	public void onSearchDefault(Event event) {
		if (konfigSedangMemuat) {
			return;
		}
		konfigSedangMemuat = true;
		tampilkanLoadingKonfigurasi("Menyiapkan halaman dan membaca konfigurasi dari database...", 6);
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					resetTabKonfigurasi();
					initTabSistemInti();
					updateLoadingKonfigurasi(
							"Tab sistem inti siap. Memuat tab modul aplikasi (akademik, keuangan, kartu, notifikasi)...",
							42);
				} catch (Exception e) {
					selesaikanLoadingKonfigurasiKarenaError(e);
					return;
				}
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							initTabModulAplikasi();
							updateLoadingKonfigurasi(
									"Tab modul siap. Memuat tab hasil pemindaian otomatis seluruh modul...", 74);
						} catch (Exception e) {
							selesaikanLoadingKonfigurasiKarenaError(e);
							return;
						}
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								try {
									initTabPemindaianOtomatis();
									updateLoadingKonfigurasi("Merapikan tampilan tab dan pencarian global...", 96);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								} finally {
									selesaiLoadingKonfigurasi();
									konfigSedangMemuat = false;
								}
							}
						});
					}
				});
			}
		});
	}

	/** Fase 1: tab pengaturan sistem inti. */
	private void initTabSistemInti() {
		initTabWebsite();
		initTabAktifitasUmum();
		initTabDashboardStatistik();
		initTabLaporanDanEkspor();
		initTabMonitoringAuditError();
		initTabDatabaseHibernateSession();
		initTabIntegrasiApiAbsensi();
		initTabFileMediaLampiran();
		initTabStartupCacheIndex();
		initTabAngketKuesioner();
		initPengaturanAI();
		initTabTampilanSistem();
		initTabPortalHalamanDepan();
	}

	protected void initTabWebsite() {
		Rows rows = createSpan("Website");

		createSpan("Pengaturan Umum Website Kampus", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan website kampus publik pada alamat /web",
				"website_kampus_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan tampilan Website Institusi V4 pada alamat /web",
				"website_ui_v4", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Judul kecil Website Institusi V4", "website_v4_eyebrow",
				"Website Resmi Institusi"));
		rows.appendChild(createRowNilai("Headline utama Website Institusi V4", "website_v4_headline",
				"Informasi resmi, layanan terpadu, dan dampak nyata", 3, null));
		rows.appendChild(createRowNilai("Deskripsi singkat Website Institusi V4", "website_v4_description",
				"Temukan informasi, layanan, berita, agenda, dan akses digital institusi melalui website resmi yang mudah digunakan.",
				3, null));
		rows.appendChild(createRowActiveDefault("Tampilkan bar pengumuman Website Institusi V4",
				"website_v4_announcement_enabled", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Isi bar pengumuman Website Institusi V4", "website_v4_announcement_text", ""));
		rows.appendChild(createRowNilai("Judul SEO Website Institusi V4", "website_v4_meta_title", ""));
		rows.appendChild(createRowNilai("Deskripsi SEO Website Institusi V4", "website_v4_meta_description", "", 3, null));
		rows.appendChild(createRowNilai("URL publik resmi Website Institusi V4", "website_v4_public_base_url", ""));
		rows.appendChild(createRowNilai("Izinkan agenda kampus bersama (tanpa scope tenant)",
				"website_v4_college_agenda_shared", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Tautan Google Play Website Institusi V4", "website_v4_mobile_app_android_url",
				"https://play.google.com/store/apps/details?id=com.ecampus.zishof"));
		rows.appendChild(createRowNilai("Tautan App Store Website Institusi V4", "website_v4_mobile_app_ios_url",
				"https://apps.apple.com/id/app/ecampus/id6503487876?l=id"));
		rows.appendChild(createRowNilai("Tautan aplikasi Desktop Website Institusi V4", "website_v4_desktop_app_url",
				"https://github.com/Zishof/ecampus-eschool-releases/releases/latest"));
		rows.appendChild(createRowNilai("Tautan Google Play eMedic pada Website Institusi V4", "website_v4_health_mobile_app_android_url", ""));
		rows.appendChild(createRowNilai("Tautan App Store eMedic pada Website Institusi V4", "website_v4_health_mobile_app_ios_url", ""));
		rows.appendChild(createRowNilai("Tautan aplikasi Desktop eMedic pada Website Institusi V4", "website_v4_health_desktop_app_url", ""));
		rows.appendChild(createRowActiveDefault("Tampilkan program pendidikan pada Website Institusi V4",
				"website_v4_show_programs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan penerimaan pada Website Institusi V4",
				"website_v4_show_admission", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan berita pada Website Institusi V4",
				"website_v4_show_news", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan agenda pada Website Institusi V4",
				"website_v4_show_agenda", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pencarian website pada Website Institusi V4",
				"website_v4_show_site_search", Konfigurasi.AKTIF));

		createSpan("Konten Kepatuhan dan Layanan Publik Website V4", rows);
		rows.appendChild(createRowNilai("Visi, misi, dan nilai", "website_v4_profile_vision_mission", "", 8, null));
		rows.appendChild(createRowNilai("Identitas legal, izin, dan registrasi", "website_v4_profile_legal", "", 8, null));
		rows.appendChild(createRowNilai("Informasi jalur dan tanggal penerimaan", "website_v4_admission_routes", "", 8, null));
		rows.appendChild(createRowNilai("Persyaratan penerimaan", "website_v4_admission_requirements", "", 8, null));
		rows.appendChild(createRowNilai("Biaya, keringanan, dan bantuan", "website_v4_admission_fees", "", 8, null));
		rows.appendChild(createRowNilai("Pemberitahuan privasi - data dan tujuan", "website_v4_privacy_data", "", 8, null));
		rows.appendChild(createRowNilai("Pemberitahuan privasi - retensi dan keamanan", "website_v4_privacy_retention", "", 8, null));
		rows.appendChild(createRowNilai("Pernyataan aksesibilitas", "website_v4_accessibility_intro", "", 8, null));
		rows.appendChild(createRowNilai("Kanal pelaporan hambatan aksesibilitas", "website_v4_accessibility_feedback", "", 6, null));
		rows.appendChild(createRowNilai("Profil dan cakupan layanan PPID", "website_v4_ppid_intro", "", 8, null));
		rows.appendChild(createRowNilai("Permohonan informasi dan keberatan PPID", "website_v4_ppid_request", "", 8, null));
		rows.appendChild(createRowNilai("Kebijakan perlindungan anak dan anti-perundungan", "website_v4_safeguarding_policy", "", 8, null));
		rows.appendChild(createRowNilai("Judul kecil website kampus", "website_eyebrow", "Website Resmi Kampus"));
		rows.appendChild(createRowNilai("Tagline hero website kampus", "website_tagline",
				"Pusat informasi resmi kampus untuk akademik, kemahasiswaan, layanan digital, prestasi, program studi, beasiswa, dan komunikasi publik institusi.",
				3, null));
		rows.appendChild(createRowNilai("Meta description website kampus", "website_meta_description",
				"Website resmi kampus yang memuat profil, program studi, berita, prestasi, beasiswa, panduan, fasilitas, layanan, dan kontak institusi.",
				3, null));

		createSpan("Naskah Formal Halaman Website", rows);
		rows.appendChild(createRowNilai("Judul bagian Selamat Datang", "website_judul_selamat_datang",
				"Selamat Datang di Website Resmi Kampus"));
		rows.appendChild(createRowNilai("Isi Selamat Datang (disarankan minimal 1000 kata)", "website_selamat_datang",
				"Selamat datang di website resmi kampus. Naskah ini dapat disesuaikan oleh administrator agar memuat sambutan formal, komitmen pelayanan, nilai institusi, layanan akademik, layanan kemahasiswaan, dan arah pengembangan kampus.",
				10, null));
		rows.appendChild(createRowNilai("Isi Profil Kampus (disarankan minimal 1000 kata)", "website_profil_kampus",
				"Profil kampus memuat identitas, mandat pendidikan, tata kelola akademik, budaya mutu, layanan mahasiswa, pengembangan dosen, fasilitas, kerja sama, dan komitmen institusi terhadap masyarakat.",
				10, null));
		rows.appendChild(createRowNilai("Isi Sejarah Kampus (disarankan minimal 1000 kata)", "website_sejarah_kampus",
				"Sejarah kampus memuat latar pendirian, perjalanan kelembagaan, perkembangan program, penguatan tata kelola, kontribusi sivitas akademika, dan arah pengembangan masa depan.",
				10, null));
		rows.appendChild(createRowNilai("Isi Struktur Organisasi (disarankan minimal 1000 kata)", "website_struktur_organisasi",
				"Struktur organisasi menjelaskan prinsip tata pamong, koordinasi pimpinan, unit akademik, unit pendukung, layanan administrasi, mekanisme pertanggungjawaban, dan budaya kerja institusi.",
				10, null));
		rows.appendChild(createRowNilai("Isi Fasilitas dan Layanan (disarankan minimal 1500 kata)", "website_fasilitas_layanan",
				"Fasilitas dan layanan kampus menjelaskan layanan akademik, perpustakaan, repository, dokumen, laboratorium, layanan digital, layanan kemahasiswaan, sarana umum, dan kanal pendukung lain.",
				10, null));
		rows.appendChild(createRowNilai("Isi Akademik dan Kemahasiswaan (disarankan minimal 1500 kata)",
				"website_akademik_kemahasiswaan",
				"Akademik dan kemahasiswaan menjelaskan kurikulum, pembelajaran, penelitian, pengabdian, organisasi mahasiswa, pembinaan prestasi, beasiswa, etika akademik, dan layanan pengembangan diri.",
				10, null));
		rows.appendChild(createRowNilai("Isi Kerja Sama, Alumni, dan Karir (disarankan minimal 1500 kata)",
				"website_kerja_sama_alumni",
				"Kerja sama, alumni, dan karir menjelaskan hubungan kemitraan, tracer study, pengembangan jejaring, peluang magang, kesiapan kerja, dan kontribusi lulusan.",
				10, null));

		createSpan("Kontak, Lokasi, dan Link Layanan", rows);
		rows.appendChild(createRowNilai("Kontak person website kampus", "website_kontak_person",
				"Kontak resmi kampus dilayani melalui nomor telepon, email, dan alamat yang tercantum pada halaman ini. Apabila institusi memiliki beberapa lokasi kampus, alamat dapat dicantumkan secara berurutan dalam konfigurasi kontak website.",
				5, null));
		rows.appendChild(createRowNilai("Catatan link layanan website kampus", "website_catatan_link_layanan",
				"Website kampus otomatis memakai link modul yang sama dengan home.jsp: login eCampus, PMB, alumni, pustaka, repository, dokumen, dashboard, karir, dan buku tamu. Link tetap dapat diubah pada tab Portal Halaman Depan.",
				4, null));
		rows.appendChild(createRowNilai("Catatan berita website kampus", "website_catatan_berita",
				"Berita kampus dikelola pada menu Pengumuman Akademik tab Berita Website. Data yang tampil di website publik hanya berita yang berstatus aktif dan terkait kampus yang sedang diakses.",
				4, null));
		rows.appendChild(createRowNilai("Catatan profil program studi", "website_catatan_profil_prodi",
				"Profil program studi diambil dari kolom profil pada tabel jurusan. Jika profil per program studi belum diisi, sistem menampilkan teks formal default yang dapat diedit melalui konfigurasi website_profil_prodi_{idJurusan}.",
				4, null));
	}

	/** Fase 2: tab modul aplikasi. */
	private void initTabModulAplikasi() {
		initTabElearning();
		initTabKarir();
		initTabVendor();
		initTabPengaturanLabelUmum();
		initTabLabelPerguruanTinggi();
		initTabKalenderPenjadwalan();
		initTabObe();
		initTabKartuUts();
		initTabKartuUas();
		initTabKartuMahasiswa();
		initTabKartuPegawai();
		initTabAlumni();
		initTabPengaturanKeuangan();
		initTabPaymentGateway();
		initTabSpmb();
		initTabPengaturanCuti();
		initTabKelulusanWisuda();
		initTabPengaturanNotifikasiWa();
		initTabPengaturanEmail();
		initTabGrupNotifikasi();
		initTabTombolLaporan();
		initTabPostingJurnal();
		initTabMenuKantin();
		initTabKelompokAsetAkun();
		initTabManajemenDokumenDms();
		initTabPengaturanDspace();
		initTabPenelitianDanPengabdian();
		initTabBackup();
		initTabMediaSosial();
		initTabCaptcha();
		initTabFileBantuan();
		initTabAplikasiMobile();
	}

	/**
	 * Tab "Tombol Laporan": mengaktifkan/menonaktifkan tombol pada toolbar pratinjau laporan
	 * (PDF, XLS, DOCX, PPTX, Download JRXML, Upload JRXML, Sejarah, Parameter). SEMUA default ON.
	 * Kondisi lama tetap dipertahankan (di-AND di CommonReport.exportReport): XLS/DOCX/PPTX hanya
	 * tampil untuk non-mahasiswa/siswa; Download/Upload/Sejarah/Parameter hanya untuk peran ber-hak
	 * "Update Format Laporan". Kunci dibaca via Common.bolehKonfigurasi(...) di
	 * ais.action.report.helper.CommonReport.
	 */
	protected void initTabTombolLaporan() {
		Rows rows = createSpan("Tombol Laporan");

		createSpan("Tombol Ekspor Format", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan tombol PDF pada pratinjau laporan",
				"report_tombol_pdf", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol XLS (Excel) pada pratinjau laporan (tetap hanya untuk non-mahasiswa/siswa)",
				"report_tombol_xls", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol DOCX (Word) pada pratinjau laporan (tetap hanya untuk non-mahasiswa/siswa)",
				"report_tombol_docx", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol PPTX (PowerPoint) pada pratinjau laporan (tetap hanya untuk non-mahasiswa/siswa)",
				"report_tombol_pptx", Konfigurasi.AKTIF));

		createSpan("Tombol Pengelolaan Template (khusus peran ber-hak \"Update Format Laporan\")", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Download (unduh JRXML aktif)",
				"report_tombol_download", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Upload (unggah JRXML baru)",
				"report_tombol_upload", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Sejarah (riwayat upload JRXML)",
				"report_tombol_sejarah", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Parameter (lihat parameter laporan)",
				"report_tombol_parameter", Konfigurasi.AKTIF));
	}

	/**
	 * Tab "Posting Jurnal": mengaktifkan/menonaktifkan tiap tab pada halaman "Posting Jurnal"
	 * (Draft Jurnal, Jurnal Umum, dst., termasuk "Posting HPP"). SEMUA default ON. Kunci =
	 * {@code Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "<slug>"}, dibaca via Common.bolehKonfigurasi(...)
	 * di ais.action.master.akunting.PostingJurnalAction. Mematikan "Posting HPP" juga menyembunyikan
	 * barisnya di ringkasan kesiapan posting jurnal (tab "Draft Jurnal").
	 */
	protected void initTabPostingJurnal() {
		String P = Konfigurasi.POSTING_JURNAL_TAB_PREFIX;
		Rows rows = createSpan("Posting Jurnal");

		createSpan("Tab Halaman Posting Jurnal", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Draft Jurnal\"", P + "draft_jurnal", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Jurnal Umum\"", P + "jurnal_umum", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Uang Muka dan Kas\"", P + "uang_muka_kas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Pajak\"", P + "pajak", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Transaksi Vendor\"", P + "transaksi_vendor", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Gaji\"", P + "gaji", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Siswa dan Mahasiswa\"", P + "siswa_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Penyusutan\"", P + "penyusutan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Pengajuan Transfer\"", P + "pengajuan_transfer", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Transitori\"", P + "transitori", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Closing\"", P + "closing", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tab \"Posting HPP\" (juga menampilkan/menyembunyikan barisnya di Ringkasan Kesiapan Posting Jurnal)",
				P + "posting_hpp", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tab \"Posting Penjualan\"", P + "posting_penjualan",
				Konfigurasi.AKTIF));
	}

	/**
	 * Tab "Menu e-Kantin": tampil/sembunyi tiap menu sidebar e-Kantin (menu utama + submenu
	 * Pengaturan). SEMUA default ON (tampil). Kunci = {@code Konfigurasi.KANTIN_MENU_PREFIX + "<id>"},
	 * dibaca di {@code nav.jsp}/{@code menu.jsp} via {@code Common.bolehKonfigurasi(...)}. Mematikan
	 * "pengaturan" menyembunyikan seluruh submenu-nya.
	 */
	protected void initTabMenuKantin() {
		String P = Konfigurasi.KANTIN_MENU_PREFIX;
		Rows rows = createSpan("Menu e-Kantin");

		createSpan("Menu Utama e-Kantin", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"Beranda\" (admin)", P + "beranda", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"Ringkasan\"", P + "ringkasan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"POS / Kasir\"", P + "pos", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"Pesanan\"", P + "pesanan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"Pengaturan\" (beserta seluruh submenu di dalamnya)", P + "pengaturan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu \"Laporan-Laporan\"", P + "laporan_laporan", Konfigurasi.AKTIF));

		createSpan("Submenu Pengaturan e-Kantin", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Anggota\"", P + "anggota", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Barang / Produk\"", P + "barang", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Kulakan (Pembelian Stok)\"", P + "kulakan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Diskon\"", P + "diskon", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Cara Pembayaran\"", P + "pembayaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Pedagang\"", P + "pedagang", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Stok\"", P + "stok", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Meja\"", P + "meja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Penyedia / Vendor\"", P + "penyedia", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Kas Kasir\"", P + "kas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Setoran Tenant\"", P + "tenant", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Jadwal Opname\"", P + "opname", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Stok Min & Expired\"", P + "stok_expired", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Limit Kredit Anggota\"", P + "limit_kredit", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Rekening Koran (Rekonsiliasi)\"", P + "mutasi_rekening", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Produksi Kantin\"", P + "produksi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan submenu \"Konfigurasi Laporan\"", P + "pengaturan_laporan", Konfigurasi.AKTIF));

		createSpan("Kasir (POS)", rows);
		rows.appendChild(createRowActiveDefault(
				"Cegah Oversell Kasir — blokir penambahan item melebihi stok pada POS (default MATI; aktifkan setelah toko rutin mencatat stok masuk lewat Pengadaan/Stok Opname, jika belum akan memblokir seluruh penjualan produk yang stoknya belum pernah tercatat)",
				Konfigurasi.KANTIN_POS_CEGAH_OVERSELL, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Reservasi Work Order mengunci stok kasir — stok yang dikunci reservasi produksi AKTIF ikut mengurangi stok yang boleh dijual di POS (default MATI = reservasi hanya informasi; menuntut Cegah Oversell juga aktif agar berdampak memblokir)",
				Konfigurasi.KANTIN_POS_RESERVASI_MENGUNCI, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Wajibkan Sesi Kas Kasir sebelum pembayaran (default MATI; aktifkan hanya untuk toko yang benar-benar memakai buka-tutup kas per shift -- bila aktif, verifikasi pesanan otomatis H+1 di halaman Pesanan ikut menuntut sesi kas terbuka)",
				Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS, Konfigurasi.TIDAK_AKTIF));

		createSpan("Satuan / UOM", rows);
		rows.appendChild(createRowActiveDefault(
				"BATALKAN pengisian satuan dasar Pcs massal saat aplikasi dijalankan ulang (default MATI; bila diaktifkan, satuan dasar produk yang dahulu diisi otomatis dikosongkan kembali pada boot berikutnya, lalu saklar ini MEMATIKAN DIRINYA SENDIRI. Produk yang sesudah pengisian sudah dikoreksi manual -- misalnya menjadi Kilogram -- tidak ikut dikosongkan)",
				Konfigurasi.KANTIN_UOM_BALIKKAN_PCS_MASSAL, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Pengisian satuan dasar Pcs massal SUDAH dijalankan di lingkungan ini (penanda otomatis; matikan hanya bila memang ingin pengisian diulang pada boot berikutnya)",
				Konfigurasi.KANTIN_UOM_ISI_PCS_MASSAL_SELESAI, Konfigurasi.TIDAK_AKTIF));

		createSpan("Pengadaan / Tagihan Vendor", rows);
		rows.appendChild(createRowActiveDefault(
				"Wajibkan anggaran pada setiap rincian tagihan rutin tanpa BAST (default MATI; bila tidak aktif, anggaran boleh dikosongkan)",
				Konfigurasi.PENGADAAN_TAGIHAN_RUTIN_ANGGARAN_WAJIB,
				Konfigurasi.TIDAK_AKTIF));


		createSpan("Price Tag / Label Harga", rows);
		rows.appendChild(createRowNilai(
				"URL logo default Price Tag jika belum ada upload khusus",
				"kantin_price_tag_logo_default_url", "/img/logo.png"));
		rows.appendChild(createRowNilai(
				"Margin tiap kotak Price Tag (mm). Default 0 = sama seperti tampilan saat ini",
				"kantin_price_tag_margin_kotak_mm", "0"));

		MyFormRow rowLogoPriceTag = new MyFormRow();
		rowLogoPriceTag.setParent(rows);
		Groupbox groupboxLogoPriceTag = new Groupbox();
		groupboxLogoPriceTag.setParent(rowLogoPriceTag);
		groupboxLogoPriceTag.appendChild(new Caption(LampiranLain.LOGO_PRICE_TAG_STR));
		Hbox hboxLogoPriceTag = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hboxLogoPriceTag, LampiranLain.LOGO_PRICE_TAG,
				LampiranLain.LOGO_PRICE_TAG_STR, LampiranLain.LOGO_PRICE_TAG_STR, false, null);
		hboxLogoPriceTag.setParent(groupboxLogoPriceTag);
	}

	/**
	 * Tab "Kelompok Barang/Jasa (Akun)": mengatur tampil/sembunyi tiap kolom akun pada form Ubah
	 * Kelompok Barang/Jasa (KelompokAsset) — Akun Fix Aset, Akun Akumulasi Penyusutan, Akun Biaya
	 * Penyusutan, dan Akun Beban Pokok Penjualan (HPP). SEMUA default ON (tampil). Kunci dibaca via
	 * {@code Common.bolehKonfigurasi(...)} di {@code ais.action.master.asset.KelompokAssetAction}.
	 */
	protected void initTabKelompokAsetAkun() {
		Rows rows = createSpan("Kelompok Barang/Jasa (Akun)");
		rows.appendChild(createRowActiveDefault(
				"Tampilkan \"Akun Fix Aset\" (Akun Pembelian) pada form Kelompok Barang/Jasa",
				Konfigurasi.KELOMPOK_ASET_AKUN_FIX_ASET, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan \"Akun Akumulasi Penyusutan\" pada form Kelompok Barang/Jasa",
				Konfigurasi.KELOMPOK_ASET_AKUN_AKUMULASI_PENYUSUTAN, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan \"Akun Biaya Penyusutan\" pada form Kelompok Barang/Jasa",
				Konfigurasi.KELOMPOK_ASET_AKUN_BIAYA_PENYUSUTAN, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan \"Akun Beban Pokok Penjualan (HPP)\" pada form Kelompok Barang/Jasa",
				Konfigurasi.KELOMPOK_ASET_AKUN_BEBAN_POKOK_PENJUALAN, Konfigurasi.AKTIF));

		createSpan("Barang/Jasa (Field)", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan field \"Kategori\" pada form Barang/Jasa",
				Konfigurasi.MASTER_ASET_KATEGORI, Konfigurasi.AKTIF));
	}

	/**
	 * Fase 3: tab hasil pemindaian otomatis — semua konfigurasi yang tercipta
	 * di class/jsp lain tetapi belum terdaftar di halaman ini, dikelompokkan
	 * per modul. Lihat method initTabAuto*().
	 */
	private void initTabPemindaianOtomatis() {
		initTabAutoKoperasiDanKantin();
		initTabAutoPerpustakaan();
		initTabAutoPmbDanRegistrasi();
		initTabAutoELearningTambahan();
		initTabAutoKepegawaianDanPayroll();
		initTabAutoSuratDanSop();
		initTabAutoKeuanganDanPembayaran();
		initTabAutoRabDanAnggaran();
		initTabAutoNotifikasiEmailDanWa();
		initTabAutoIntegrasiEksternal();
		initTabAutoSekolahDanYayasan();
		initTabAutoAkademikKampus();
		initTabAutoAsetDanPengadaan();
		initTabAutoTampilanDanLabelTambahan();
		initTabAutoBackupDanPemeliharaan();
		initTabAutoLainLainTerdeteksiOtomatis();
	}

	// =====================================================================
	// KARTU PROGRESS PEMUATAN KONFIGURASI (pola DasboardSop)
	// =====================================================================

	private transient org.zkoss.zul.Window konfigLoadingWindow;
	private transient org.zkoss.zul.Html konfigLoadingHtml;
	private boolean konfigSedangMemuat = false;

	private void tampilkanLoadingKonfigurasi(String message, int percent) {
		try {
			selesaiLoadingKonfigurasi();
			konfigLoadingHtml = new org.zkoss.zul.Html(buildLoadingKonfigurasiHtml(message, percent));
			konfigLoadingWindow = new org.zkoss.zul.Window();
			konfigLoadingWindow.setBorder("none");
			konfigLoadingWindow.setStyle("background:transparent;border:0;box-shadow:none;padding:0;");
			konfigLoadingWindow.setWidth(Common.isMobile() ? "94%" : "560px");
			konfigLoadingWindow.setParent(page.getFirstRoot());
			konfigLoadingHtml.setParent(konfigLoadingWindow);
			konfigLoadingWindow.setPosition("center,center");
			konfigLoadingWindow.doOverlapped();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void updateLoadingKonfigurasi(String message, int percent) {
		try {
			if (konfigLoadingHtml != null) {
				konfigLoadingHtml.setContent(buildLoadingKonfigurasiHtml(message, percent));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:881");
		}
	}

	private void selesaiLoadingKonfigurasi() {
		try {
			if (konfigLoadingWindow != null) {
				konfigLoadingWindow.detach();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:890");
		}
		konfigLoadingWindow = null;
		konfigLoadingHtml = null;
	}

	private void selesaikanLoadingKonfigurasiKarenaError(Exception e) {
		Common.tampilErrorJikaAdmin(e);
		selesaiLoadingKonfigurasi();
		konfigSedangMemuat = false;
	}

	private String buildLoadingKonfigurasiHtml(String message, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		String aman = message == null ? "Memuat pengaturan konfigurasi..."
				: message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<div style=\"padding:22px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb; "
				+ "box-shadow:0 14px 32px rgba(15,23,42,.18); color:#0f172a;\">"
				+ "<div style=\"display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;\">"
				+ "<div>"
				+ "<div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;\">"
				+ "Pengaturan Konfigurasi</div>"
				+ "<div style=\"font-size:18px; font-weight:900; margin-top:6px;\">"
				+ "<i class=\"fa fa-spinner fa-spin\"></i> Memuat Seluruh Tab Konfigurasi</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;\">"
				+ aman + "</div>"
				+ "</div>"
				+ "<div style=\"min-width:86px; text-align:right; font-size:30px; font-weight:900; color:#0f172a;\">"
				+ percent + "%</div>"
				+ "</div>"
				+ "<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:18px;\">"
				+ "<div style=\"height:12px; width:" + percent + "%; border-radius:999px; "
				+ "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));\"></div>"
				+ "</div>"
				+ "<div style=\"display:flex; gap:8px; flex-wrap:wrap; margin-top:14px; font-size:11px; color:#475569;\">"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-weight:800;\">Sistem Inti</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#fef3c7; color:#92400e; font-weight:800;\">Modul Aplikasi</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#ecfdf5; color:#166534; font-weight:800;\">Pemindaian Otomatis</span>"
				+ "</div>"
				+ "</div>";
	}

	private void resetTabKonfigurasi() {
		try {
			if (outerTabsDiv != null) {
				java.util.List<org.zkoss.zk.ui.Component> kids = new java.util.ArrayList<org.zkoss.zk.ui.Component>(outerTabsDiv.getChildren());
				for (org.zkoss.zk.ui.Component k : kids) k.detach();
				mbt = null;
				mbtNextIdx = 0;
				return;
			}
			if (tabsKonfigurasi != null) {
				tabsKonfigurasi.getChildren().clear();
			}
			if (tabpanelsKonfigurasi != null) {
				tabpanelsKonfigurasi.getChildren().clear();
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:948");
			}
		}
	}




	/**
	 * Pengaturan database, Hibernate Session, dan proteksi Lazy proxy.
	 *
	 * Tab ini mengumpulkan konfigurasi hasil perbaikan Session is closed,
	 * LazyInitializationException, currentNativeSession/openSession yang wajib ditutup,
	 * serta aturan agar currentSession milik request tidak ditutup manual.
	 */
	/**
	 * <b>Grup Notifikasi</b> — kustomisasi seluruh kata/kalimat baku yang membuat
	 * setiap pesan ke pengguna (Email, Notifikasi aplikasi, dan WhatsApp) menjadi
	 * <b>sangat formal</b> serta memenuhi <b>panjang minimal</b> (baku 1.000 kata).
	 *
	 * <p>
	 * Seluruh nilai pada tab ini dibaca oleh {@link ais.common.FormalisasiPesanUtil}
	 * dan dipakai bersama oleh {@code CommonNotifikasi.formatBaku} serta
	 * {@code MailSender} (email + WhatsApp). Mengubah nilai di sini akan langsung
	 * mengubah gaya bahasa pada semua kanal tanpa menyentuh kode.
	 * </p>
	 */
	protected void initTabGrupNotifikasi() {
		Rows rows = createSpan("Grup Notifikasi");

		createSpan("Aktivasi dan Ambang Panjang Pesan", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan formalisasi semua pesan ke pengguna (Email, Notifikasi, WhatsApp) menjadi sangat resmi dan memenuhi panjang minimal — dipakai di: FormalisasiPesanUtil, MailSender, CommonNotifikasi",
				ais.common.FormalisasiPesanUtil.K_AKTIF, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Terapkan formalisasi pada Email",
				ais.common.FormalisasiPesanUtil.K_TERAPKAN_EMAIL, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Terapkan formalisasi pada Notifikasi aplikasi (lonceng ZKoss / pusat notifikasi JSP) — default TIDAK AKTIF agar notif in-app ringkas",
				ais.common.FormalisasiPesanUtil.K_TERAPKAN_NOTIF, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Terapkan formalisasi pada WhatsApp",
				ais.common.FormalisasiPesanUtil.K_TERAPKAN_WA, Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Panjang minimal kata setiap pesan (baku 1000, maksimum dibatasi 6000)",
				ais.common.FormalisasiPesanUtil.K_MINIMAL_KATA, "1000"));
		rows.appendChild(createRowNilai("Nama institusi (dicantumkan pada tanda tangan pesan)",
				ais.common.FormalisasiPesanUtil.K_NAMA_INSTITUSI, ais.common.FormalisasiPesanUtil.D_NAMA_INSTITUSI));

		createSpan("Kata-kata Pembuka", rows);
		rows.appendChild(createRowNilai("Salam pembuka", ais.common.FormalisasiPesanUtil.K_SALAM,
				ais.common.FormalisasiPesanUtil.D_SALAM, 2, null));
		rows.appendChild(createRowNilai("Paragraf pembuka", ais.common.FormalisasiPesanUtil.K_PEMBUKA,
				ais.common.FormalisasiPesanUtil.D_PEMBUKA, 6, null));

		createSpan("Klausul Baku (Isi Formal)", rows);
		rows.appendChild(createRowNilai("Judul klausul \"pentingnya pesan\"",
				ais.common.FormalisasiPesanUtil.K_PENTING_JUDUL, ais.common.FormalisasiPesanUtil.D_PENTING_JUDUL));
		rows.appendChild(createRowNilai("Isi klausul \"pentingnya pesan\"", ais.common.FormalisasiPesanUtil.K_PENTING,
				ais.common.FormalisasiPesanUtil.D_PENTING, 6, null));
		rows.appendChild(createRowNilai("Judul klausul \"langkah tindak lanjut\"",
				ais.common.FormalisasiPesanUtil.K_LANGKAH_JUDUL, ais.common.FormalisasiPesanUtil.D_LANGKAH_JUDUL));
		rows.appendChild(createRowNilai("Isi klausul \"langkah tindak lanjut\" (URL & menu tujuan ditambahkan otomatis)",
				ais.common.FormalisasiPesanUtil.K_LANGKAH, ais.common.FormalisasiPesanUtil.D_LANGKAH, 6, null));
		rows.appendChild(createRowNilai("Judul klausul \"integritas & ketepatan waktu\"",
				ais.common.FormalisasiPesanUtil.K_INTEGRITAS_JUDUL, ais.common.FormalisasiPesanUtil.D_INTEGRITAS_JUDUL));
		rows.appendChild(createRowNilai("Isi klausul \"integritas & ketepatan waktu\"",
				ais.common.FormalisasiPesanUtil.K_INTEGRITAS, ais.common.FormalisasiPesanUtil.D_INTEGRITAS, 6, null));
		rows.appendChild(createRowNilai("Judul klausul \"bantuan & informasi\"",
				ais.common.FormalisasiPesanUtil.K_BANTUAN_JUDUL, ais.common.FormalisasiPesanUtil.D_BANTUAN_JUDUL));
		rows.appendChild(createRowNilai("Isi klausul \"bantuan & informasi\"",
				ais.common.FormalisasiPesanUtil.K_BANTUAN, ais.common.FormalisasiPesanUtil.D_BANTUAN, 6, null));

		createSpan("Kata-kata Penutup", rows);
		rows.appendChild(createRowNilai("Paragraf penutup", ais.common.FormalisasiPesanUtil.K_PENUTUP,
				ais.common.FormalisasiPesanUtil.D_PENUTUP, 5, null));
		rows.appendChild(createRowNilai("Salam penutup / tanda tangan", ais.common.FormalisasiPesanUtil.K_TANDA_TANGAN,
				ais.common.FormalisasiPesanUtil.D_TANDA_TANGAN));
		rows.appendChild(createRowNilai("Disclaimer (catatan kaki otomatis)", ais.common.FormalisasiPesanUtil.K_DISCLAIMER,
				ais.common.FormalisasiPesanUtil.D_DISCLAIMER, 4, null));

		createSpan("Paragraf Tambahan (Pemenuh Panjang Minimal)", rows);
		rows.appendChild(createRowNilai("Paragraf tambahan 1", ais.common.FormalisasiPesanUtil.K_TAMBAHAN_1,
				ais.common.FormalisasiPesanUtil.D_TAMBAHAN_1, 5, null));
		rows.appendChild(createRowNilai("Paragraf tambahan 2", ais.common.FormalisasiPesanUtil.K_TAMBAHAN_2,
				ais.common.FormalisasiPesanUtil.D_TAMBAHAN_2, 5, null));
		rows.appendChild(createRowNilai("Paragraf tambahan 3", ais.common.FormalisasiPesanUtil.K_TAMBAHAN_3,
				ais.common.FormalisasiPesanUtil.D_TAMBAHAN_3, 5, null));
	}

	protected void initTabDatabaseHibernateSession() {
		Rows rows = createSpan("Database, Hibernate, dan Session");

		createSpan("Proteksi Session Hibernate", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan guard global agar currentSession tidak mengembalikan session ZK yang sudah tertutup",
				"hibernate_session_guard_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Validasi session dengan isOpen sebelum dipakai createCriteria/createQuery",
				"hibernate_validasi_session_is_open", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika currentSession ZK sudah tertutup, fallback ke native session yang masih open",
				"hibernate_current_session_fallback_native", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bersihkan ThreadLocal currentNativeSession jika session lama sudah tertutup",
				"hibernate_current_native_bersihkan_threadlocal_closed", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tutup native session hanya jika session tersebut benar-benar session ThreadLocal yang sama",
				"hibernate_close_hanya_session_threadlocal_yang_sama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan menutup currentSession milik request/ZK secara manual",
				"hibernate_current_session_jangan_ditutup_manual", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Session dari openSession wajib ditutup pada blok finally",
				"hibernate_open_session_wajib_close_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Session dari currentNativeSession wajib ditutup pada blok finally jika dibuat untuk proses lokal/background",
				"hibernate_current_native_wajib_close_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pesan standar jika method menerima Session yang sudah tertutup",
				"hibernate_pesan_session_closed",
				"Session database sudah tertutup. Sistem akan membuka session baru untuk proses yang berdiri sendiri, atau menghentikan proses jika transaksi caller wajib dipertahankan.",
				3, null));
		rows.appendChild(createRowNilai("Catatan proteksi Session Hibernate",
				"catatan_hibernate_session_guard",
				"currentSession digunakan untuk lifecycle halaman ZK dan tidak ditutup manual. openSession/currentNativeSession digunakan untuk proses lokal, API, timer, background, dan report; session jenis ini harus ditutup di finally agar tidak bocor memori/koneksi.",
				4, null));

		createSpan("Proteksi Criteria, Query, dan Method Static", rows);
		rows.appendChild(createRowActiveDefault("Gunakan guard sebelum session.createCriteria pada method static yang menerima parameter Session",
				"hibernate_createcriteria_guard_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika method static berdiri sendiri, izinkan fallback membuka native session baru saat session parameter sudah tertutup",
				"hibernate_static_method_fallback_open_native", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika method static berada dalam transaksi caller, hentikan proses saat session sudah tertutup agar data tidak salah transaksi",
				"hibernate_static_method_transaksi_wajib_stop_jika_closed", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Batasi query menu/pengumuman agar tidak mengambil data terlalu banyak saat awal login",
				"hibernate_batasi_query_menu_awal_login", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Batas data menu/pengumuman yang dimuat saat awal login",
				"hibernate_menu_pengumuman_max_result", "500"));
		rows.appendChild(createRowNilai("Jumlah data batch sebelum session.clear pada proses besar",
				"hibernate_batch_clear_interval", "50"));
		rows.appendChild(createRowNilai("Catatan Criteria dan Query",
				"catatan_hibernate_criteria_query",
				"Method static yang menerima Session harus memastikan session masih open sebelum membuat Criteria. Untuk proses besar, gunakan paging/batch dan clear session berkala agar first-level cache tidak membengkak.",
				4, null));

		createSpan("Lazy Relation dan Proxy Detached", rows);
		rows.appendChild(createRowActiveDefault("Gunakan resolver general pada GeneralValueObject.check/chek untuk lazy proxy detached",
				"hibernate_lazy_resolver_general_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika proxy lazy sudah detached, reload entity berdasarkan identifier",
				"hibernate_lazy_reload_by_identifier", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Ambil identifier proxy dari HibernateProxy tanpa memaksa inisialisasi proxy",
				"hibernate_lazy_proxy_get_identifier_aman", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Touch field ringan setelah reload entity lazy agar aman dipakai di tampilan",
				"hibernate_lazy_touch_basic_fields", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan touch koleksi besar saat resolver lazy agar memori tetap ringan",
				"hibernate_lazy_jangan_touch_collection_besar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Catat warning admin jika lazy proxy gagal di-resolve",
				"hibernate_lazy_log_warning_admin", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan lazy relation",
				"catatan_hibernate_lazy_relation",
				"Getter lazy sebaiknya memakai pola field = check(field); return field;. Jika proxy sudah detached, resolver akan mencoba reload berdasarkan ID, bukan mengembalikan proxy lama yang bisa memicu LazyInitializationException.",
				4, null));

		createSpan("Efisiensi Memori Database", rows);
		rows.appendChild(createRowActiveDefault("Gunakan paging default 10 baris untuk tabel yang berpotensi besar",
				"hibernate_ui_paging_default_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah baris default paging tabel besar",
				"hibernate_ui_paging_default_size", "10"));
		rows.appendChild(createRowActiveDefault("Setelah list besar selesai dipakai untuk render UI, panggil clear pada list tersebut",
				"hibernate_clear_list_setelah_render", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Hindari menyimpan entity Hibernate besar ke session HTTP/ZK; simpan ID jika memungkinkan",
				"hibernate_simpan_id_bukan_entity_besar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan projection/count untuk dashboard ringkasan agar tidak memuat seluruh entity",
				"hibernate_dashboard_gunakan_projection_count", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan efisiensi memori Hibernate",
				"catatan_hibernate_memori",
				"Untuk data besar, baca hanya kolom yang diperlukan, gunakan setMaxResults/paging, clear session per batch, dan jangan menyimpan entity berat dalam session user. Cara ini mengurangi risiko OutOfMemory dan session tertahan terlalu lama.",
				4, null));
	}

	/**
	 * Pengaturan laporan, progress bar, export Excel/PDF, dan keamanan session report.
	 */
	protected void initTabLaporanDanEkspor() {
		Rows rows = createSpan("Laporan dan Ekspor");

		createSpan("Progress Laporan", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan progress saat laporan PDF, Excel, dan file ekspor sedang dibuat",
				"report_progress_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sembunyikan progress otomatis setelah laporan selesai 100%",
				"report_progress_hide_setelah_selesai", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Judul progress laporan",
				"report_progress_judul", "Sedang membuat laporan"));
		rows.appendChild(createRowNilai("Keterangan progress laporan",
				"report_progress_keterangan", "Data sedang disiapkan, lalu file laporan akan dibuat.", 3, null));
		rows.appendChild(createRowNilai("Minimal selisih persen untuk update progress laporan",
				"report_progress_min_update_percent", "1"));
		rows.appendChild(createRowActiveDefault("Gunakan progress otomatis pada semua pemanggilan Report.generatePDFReport",
				"report_generate_pdf_progress_otomatis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan progress otomatis saat tombol export PDF/Excel ditekan",
				"report_export_progress_otomatis", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan progress laporan",
				"catatan_progress_laporan",
				"Pengguna dapat melihat laporan sedang membaca data, menyusun isi, dan membuat file. Setelah selesai, indikator hilang sendiri agar halaman tetap rapi.",
				4, null));

		createSpan("Notifikasi Error Laporan", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan pesan error laporan yang ramah untuk pengguna umum",
				"report_error_notifikasi_ramah_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol download detail error saat laporan gagal dibuat",
				"report_error_download_detail_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sertakan stack trace teknis di file detail error laporan",
				"report_error_detail_tampilkan_stacktrace", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sertakan nilai parameter laporan di file detail error. Nonaktifkan jika parameter berisi data sensitif",
				"report_error_detail_tampilkan_parameter", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tulis detail error laporan ke console/log server untuk membantu admin",
				"report_error_log_teknis_ke_console", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Folder penyimpanan detail error laporan",
				"report_error_detail_folder", ""));
		rows.appendChild(createRowNilai("Pesan umum saat laporan gagal dibuat",
				"report_error_pesan_user",
				"Maaf, laporan belum berhasil dibuat. Data yang Anda pilih tetap aman dan tidak berubah. Silakan coba kembali. Jika pesan ini muncul lagi, unduh detail error dan berikan kepada admin.",
				3, null));
		rows.appendChild(createRowNilai("Pesan saat template JRXML/Jasper tidak valid",
				"report_error_pesan_template_rusak",
				"Template laporan belum dapat dibaca. Kemungkinan file JRXML/Jasper rusak, tidak cocok dengan versi JasperReports, atau ada tag XML yang tidak valid.",
				3, null));
		rows.appendChild(createRowNilai("Pesan saat JRXML memiliki UUID tidak valid",
				"report_error_pesan_invalid_uuid",
				"Template laporan belum sesuai format JasperReports. Biasanya ada elemen JRXML yang memiliki UUID tidak valid. Silakan hubungi admin untuk memperbaiki template laporan.",
				3, null));
		rows.appendChild(createRowNilai("Pesan saat parameter laporan belum sesuai",
				"report_error_pesan_parameter",
				"Laporan belum dapat dibuat karena ada parameter laporan yang belum lengkap atau tidak sesuai. Silakan periksa pilihan filter lalu coba kembali.",
				3, null));
		rows.appendChild(createRowNilai("Catatan error laporan",
				"catatan_error_laporan",
				"Jika laporan gagal, pengguna cukup melihat pesan sederhana. Admin dapat mengunduh detail error untuk melihat nama template, penyebab teknis, saran perbaikan, dan stack trace.",
				4, null));

		createSpan("Session Database Laporan", rows);
		rows.appendChild(createRowActiveDefault("Gunakan session database baru untuk proses laporan background",
				"report_gunakan_native_open_session", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tutup session database laporan di blok finally",
				"report_tutup_native_session_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan menutup currentSession milik request dari proses laporan background",
				"report_jangan_tutup_current_session_request", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tutup currentSession di finally hanya jika benar-benar diperlukan",
				"report_tutup_current_session_di_finally", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Bersihkan cache Hibernate sebelum native session laporan ditutup",
				"report_clear_session_sebelum_close", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan session laporan",
				"catatan_session_laporan",
				"Laporan yang berjalan lama memakai session sendiri agar tidak memakai session halaman yang sudah tertutup. Ini mencegah error Session is closed dan menjaga request lain tetap aman.",
				4, null));

		createSpan("Rentang Tanggal dan Data Laporan", rows);
		rows.appendChild(createRowActiveDefault("Hitung rentang tanggal laporan secara inklusif tanpa lebih satu hari",
				"report_tanggal_inklusif_aman", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Normalisasi tanggal mulai ke awal hari dan tanggal sampai ke akhir hari",
				"report_normalisasi_awal_akhir_hari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan logika overlap saat membaca data cuti, izin, dan pengajuan berdasarkan periode",
				"report_query_overlap_periode", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan rentang tanggal laporan",
				"catatan_tanggal_laporan",
				"Jika memilih 1 sampai 31, laporan hanya berhenti di tanggal 31. Data yang mulai sebelum periode tetapi masih berlangsung di periode itu tetap terbaca.",
				4, null));

		createSpan("Tampilan Laporan dan Export", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan tabel/grid sebelum pengguna mengunduh Excel",
				"report_excel_tampil_grid_lebih_dulu", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan chart HTML/CSS/SVG ringan untuk ringkasan laporan, bukan JFreeChart",
				"report_gunakan_chart_html_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan deskripsi sederhana pada ringkasan laporan",
				"report_tampilkan_deskripsi_sederhana", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah baris per halaman preview laporan",
				"report_preview_jumlah_baris", "10"));
		rows.appendChild(createRowNilai("Deskripsi umum laporan",
				"report_deskripsi_umum",
				"Ringkasan ini membantu melihat isi laporan sebelum file diunduh atau dicetak.", 3, null));
	}

	/**
	 * Pengaturan monitoring error, audit trail, revisi, dan proteksi error global.
	 *
	 * Seluruh konfigurasi tambahan hasil perbaikan error global dikumpulkan di sini
	 * agar admin tidak perlu mencari ke banyak tab. Default dibuat aman: error
	 * penting tetap masuk database, sedangkan koneksi client terputus seperti
	 * Broken pipe tidak memenuhi tabel ErrorLog.
	 */
	protected void initTabMonitoringAuditError() {
		Rows rows = createSpan("Monitoring, Audit, dan Error");

		createSpan("ErrorLog Global dan Listener Error", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan pencatatan error Java, ZKoss, servlet, dan filter ke database ErrorLog",
				"global_error_log_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tangkap error dari filter global sebelum masuk ke FilterJSP",
				"global_error_log_filter_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tangkap error dari listener global aplikasi ketika context berjalan",
				"global_error_log_context_listener_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Abaikan error koneksi client terputus seperti ClientAbortException, Broken pipe, dan Connection reset",
				"global_error_log_abaikan_client_abort", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pola pesan error client terputus yang diabaikan",
				"global_error_log_client_abort_patterns",
				"ClientAbortException|Broken pipe|Connection reset by peer|Connection reset|Software caused connection abort", 3, null));
		rows.appendChild(createRowNilai("Batas panjang detail error yang disimpan ke database",
				"global_error_log_maks_panjang_keterangan", "20000"));
		rows.appendChild(createRowNilai("Batas panjang ringkasan error",
				"global_error_log_maks_panjang_ringkasan", "500"));
		rows.appendChild(createRowActiveDefault("Saat admin login, error tetap ditampilkan lebih jelas untuk membantu perbaikan",
				"global_error_log_tampilkan_detail_jika_admin", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Simpan informasi request URI, parameter, IP, dan user agent jika tersedia",
				"global_error_log_simpan_request_context", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan catat error yang berasal dari proses pencatatan ErrorLog sendiri",
				"global_error_log_cegah_rekursif", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan ErrorLog global",
				"catatan_global_error_log",
				"Error yang benar-benar berasal dari program tetap disimpan ke database. Error karena browser ditutup, jaringan user putus, atau halaman direfresh tidak perlu memenuhi log karena bukan kerusakan data aplikasi.",
				4, null));

		createSpan("Dashboard ErrorLog", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan dasbor ringkasan error pada halaman ErrorLog",
				"error_log_tampilkan_dashboard", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan chart HTML/CSS/SVG ringan pada dasbor ErrorLog",
				"error_log_gunakan_chart_html_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah data terbaru yang dibaca untuk dasbor ErrorLog",
				"error_log_dashboard_limit", "1000"));
		rows.appendChild(createRowNilai("Jumlah hari tren harian pada dasbor ErrorLog",
				"error_log_trend_days", "14"));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Copy Ringkasan pada setiap baris ErrorLog",
				"error_log_tampilkan_copy_ringkasan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol Copy Lengkap untuk AI pada setiap baris ErrorLog",
				"error_log_tampilkan_copy_ai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan fallback prompt jika browser memblokir clipboard otomatis",
				"error_log_clipboard_fallback_prompt", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label tombol copy lengkap untuk AI",
				"error_log_label_copy_ai", "Copy Lengkap untuk AI"));
		rows.appendChild(createRowNilai("Deskripsi dasbor error",
				"error_log_deskripsi_dashboard",
				"Catatan ini membantu melihat bagian sistem yang paling sering bermasalah, kapan error biasanya muncul, dan area yang perlu diperiksa lebih dulu.",
				3, null));

		createSpan("Audit Trail Data", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan AuditListener untuk mencatat create, edit, dan delete data",
				"audit_listener_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan AuditTimestampInterceptor untuk mengisi tanggal_dirubah, oleh, dan olehId",
				"audit_timestamp_interceptor_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan debug audit ke log untuk monitoring sementara",
				"audit_debug", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Simpan audit hanya jika ada kolom yang benar-benar berubah",
				"audit_hanya_simpan_jika_ada_perubahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Kolom yang tidak dihitung sebagai perubahan audit",
				"audit_ignore_columns", "tanggal_dirubah,olehId,oleh"));
		rows.appendChild(createRowNilai("Batas panjang callFrom pada olehId audit",
				"audit_callfrom_max_length", "150"));
		rows.appendChild(createRowActiveDefault("Gunakan nama class sederhana tanpa package pada callFrom audit",
				"audit_callfrom_simple_class_name", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya ambil call stack dari package ais.*",
				"audit_callfrom_only_ais_package", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cegah audit berulang ketika proses audit sedang berjalan",
				"audit_cegah_rekursif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan baca BLOB/lampiran langsung dari AuditListener",
				"audit_skip_ambil_file_lampiran_dalam_listener", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan audit trail",
				"catatan_audit_trail",
				"Audit menyimpan perubahan data utama, bukan perubahan teknis seperti tanggal_dirubah, olehId, dan oleh. CallFrom dibuat singkat agar mudah dibaca dan tidak membuat kolom terlalu panjang.",
				4, null));

		createSpan("Tampilan Riwayat Revisi", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan kolom Perubahan Dari Data Sebelumnya dalam bentuk grid Field, Sebelum, Sesudah",
				"revisi_perubahan_dalam_grid", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Saat filter di Riwayat ID Ini, tetap berada di tab Riwayat ID Ini",
				"revisi_filter_tetap_di_tab_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Saat filter di Seluruh Data Revisi, tetap berada di tab Seluruh Data Revisi",
				"revisi_seluruh_data_tetap_di_tab_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah baris per halaman pada tabel revisi",
				"revisi_jumlah_data_per_halaman", "10"));
		rows.appendChild(createRowNilai("Rentang default seluruh data revisi dalam bulan",
				"revisi_default_range_bulan", "6"));
		rows.appendChild(createRowActiveDefault("Tampilkan progress bar saat data revisi besar sedang dimuat",
				"revisi_tampilkan_progress_loading", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sembunyikan progress bar setelah proses revisi mencapai 100%",
				"revisi_progress_hide_setelah_selesai", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tinggi minimal tabel riwayat revisi",
				"revisi_tinggi_minimal_tabel", "500px"));
		rows.appendChild(createRowNilai("Deskripsi sederhana tab revisi",
				"revisi_deskripsi_sederhana",
				"Riwayat perubahan membantu melihat data lama dan data baru secara cepat tanpa membaca teks panjang.",
				3, null));

		createSpan("Chart dan Panel Dashboard Umum", rows);
		rows.appendChild(createRowActiveDefault("Gunakan HTML/CSS/SVG untuk chart dashboard, bukan JFreeChart",
				"dashboard_gunakan_chart_html_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tabel/grid terlebih dahulu sebelum download Excel",
				"dashboard_excel_tampil_grid_lebih_dulu", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah data per halaman dashboard umum",
				"dashboard_jumlah_data_per_halaman", "10"));
		rows.appendChild(createRowActiveDefault("Tampilkan deskripsi sederhana pada setiap panel dashboard",
				"dashboard_tampilkan_deskripsi_panel", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan kalimat deskripsi yang mudah dipahami pengguna non-teknis",
				"dashboard_deskripsi_sederhana", Konfigurasi.AKTIF));
	}

	/**
	 * Pengaturan integrasi API, servlet AI/API, absensi fingerprint, dan response JSON.
	 */
	protected void initTabIntegrasiApiAbsensi() {
		Rows rows = createSpan("API, Integrasi, dan Absensi");

		createSpan("Servlet API dan Response JSON", rows);
		rows.appendChild(createRowActiveDefault("Selalu kembalikan response API dalam format JSON aman",
				"api_response_selalu_json", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika response remote bukan JSON, ubah menjadi warning JSON agar tidak memicu JSONException",
				"api_remote_non_json_jadi_warning", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Catat warning ketika response remote tidak berbentuk JSONObject",
				"api_log_warning_non_json_remote", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Lepaskan koneksi HTTP client di blok finally",
				"api_release_connection_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Timeout koneksi API remote dalam milidetik",
				"api_remote_connect_timeout_ms", "30000"));
		rows.appendChild(createRowNilai("Timeout baca API remote dalam milidetik",
				"api_remote_read_timeout_ms", "60000"));
		rows.appendChild(createRowNilai("Access-Control-Allow-Origin API",
				"api_cors_allow_origin", "*"));
		rows.appendChild(createRowNilai("Content-Type default API",
				"api_content_type", "application/json"));
		rows.appendChild(createRowActiveDefault("Simpan log request API mobile jika data tersedia",
				"api_mobile_logger_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan tampilkan stack trace API ke user umum",
				"api_sembunyikan_stacktrace_user", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pesan default saat format JSON API tidak valid",
				"api_pesan_json_tidak_valid", "Format JSON request tidak valid"));
		rows.appendChild(createRowNilai("Catatan API",
				"catatan_api_json",
				"API tetap mengembalikan JSON meskipun server tujuan mengirim HTML, kosong, atau teks biasa. Ini mencegah error JSONObject text must begin with '{'.",
				4, null));

		createSpan("Absensi Fingerprint dan Online", rows);
		rows.appendChild(createRowNilaiPassword("Password header p untuk servlet /Absen",
				"password_absen", ""));
		rows.appendChild(createRowActiveDefault("Servlet /Absen mengembalikan JSON gagal, bukan melempar NotFoundException",
				"absen_fingerprint_return_json_on_error", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Catat error absensi fingerprint ke ErrorLog jika bukan client abort",
				"absen_fingerprint_catat_error", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Format tanggal waktu fingerprint",
				"absen_fingerprint_format_waktu", "yyyy-MM-dd HH:mm:ss"));
		rows.appendChild(createRowNilai("Toleransi waktu fingerprint ke masa depan dalam menit",
				"absen_fingerprint_toleransi_masa_depan_menit", "120"));
		rows.appendChild(createRowNilai("Nilai state untuk absen masuk",
				"absen_fingerprint_state_masuk", "0"));
		rows.appendChild(createRowNilai("Nilai state untuk absen pulang",
				"absen_fingerprint_state_pulang", "1"));
		rows.appendChild(createRowActiveDefault("Jika state kosong, sistem otomatis mengisi masuk dulu lalu pulang",
				"absen_fingerprint_state_otomatis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cari pegawai berdasarkan idfinger pegawai, guru, dan dosen",
				"absen_fingerprint_cari_pegawai_guru_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cari mahasiswa berdasarkan idfinger dan NIM",
				"absen_fingerprint_cari_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cari siswa berdasarkan idfinger, NISN, dan nomor induk",
				"absen_fingerprint_cari_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pesan ketika waktu fingerprint belum boleh diproses",
				"absen_fingerprint_pesan_waktu_belum_masuk", "Gagal, waktu belum masuk"));
		rows.appendChild(createRowNilai("Pesan ketika password servlet absen salah",
				"absen_pesan_password_salah", "Password salah"));
		rows.appendChild(createRowNilai("Catatan absensi",
				"catatan_absensi_fingerprint",
				"Absensi fingerprint dibuat aman untuk perangkat luar. Jika data tidak lengkap atau terjadi error, response tetap JSON agar perangkat tidak menggantung.",
				4, null));

		createSpan("Integrasi AI Lokal dan Proxy", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan log debug servlet AI saat pengujian integrasi",
				"AI_DEBUG", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Endpoint Ollama lokal langsung",
				"AI_OLLAMA_LOCAL_BASE_URL", "http://192.168.88.128:11434"));
		rows.appendChild(createRowNilai("Endpoint Ollama melalui Apache proxy",
				"AI_OLLAMA_PROXY_BASE_URL", "http://38.47.178.42:9002"));
		rows.appendChild(createRowNilai("Model ringan untuk generator teks",
				"AI_OLLAMA_AKADEMIK_MODEL", "qwen2.5:7b"));
		rows.appendChild(createRowNilai("Model coding yang disarankan",
				"AI_OLLAMA_CODING_MODEL", "qwen2.5-coder:7b"));
		rows.appendChild(createRowNilai("Catatan integrasi AI",
				"catatan_integrasi_ai_lokal",
				"Gunakan model 7B untuk server terbatas agar response lebih ringan. Model besar seperti 32B sebaiknya hanya dipakai jika CPU/RAM/GPU mencukupi.",
				4, null));
	}

	/**
	 * Pengaturan file, media, upload lampiran, BLOB PostgreSQL, dan cache file.
	 */
	protected void initTabFileMediaLampiran() {
		Rows rows = createSpan("File, Media, dan Lampiran");

		createSpan("Penyimpanan File dan Media", rows);
		rows.appendChild(createRowActiveDefault("Gunakan cache file di folder media sebelum membaca BLOB database",
				"file_media_gunakan_cache_disk", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Buat folder tujuan otomatis sebelum copy/upload file",
				"file_media_buat_folder_otomatis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Copy file melalui file temporary terlebih dahulu agar tidak setengah tertulis",
				"file_media_copy_melalui_temp", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cegah copy jika source dan target file sama",
				"file_media_cegah_copy_file_sama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan nama file aman untuk spasi, persen, pagar, tanda kurung, dan kutip",
				"file_media_sanitize_nama_file", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Ukuran buffer copy file dalam byte",
				"file_media_buffer_size", "65536"));
		rows.appendChild(createRowNilai("Lokasi fallback logo jika file lampiran belum tersedia",
				"file_media_logo_fallback", "/img/logo.png"));
		rows.appendChild(createRowNilai("Catatan file media",
				"catatan_file_media",
				"File diprioritaskan dari folder media agar lebih cepat dan tidak selalu membaca BLOB database. Jika belum ada di disk, sistem baru mengambil dari BLOB atau Google Drive.",
				4, null));

		createSpan("BLOB PostgreSQL dan Large Object", rows);
		rows.appendChild(createRowActiveDefault("Baca BLOB PostgreSQL menggunakan session khusus read-only",
				"file_blob_readonly_session", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan FlushMode.MANUAL saat ekstraksi BLOB agar tidak memicu audit/flush",
				"file_blob_flush_mode_manual", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Akhiri transaksi baca BLOB dengan rollback read-only, bukan commit",
				"file_blob_read_transaction_rollback", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan guard ThreadLocal agar ekstraksi BLOB tidak berulang tanpa batas",
				"file_blob_threadlocal_guard", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika BLOB gagal dibaca karena invalid large-object descriptor, coba baca ulang dari managed entity",
				"file_blob_retry_managed_entity", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah retry baca BLOB",
				"file_blob_retry_count", "1"));
		rows.appendChild(createRowNilai("Catatan BLOB PostgreSQL",
				"catatan_file_blob_postgresql",
				"Large object PostgreSQL harus dibaca saat connection masih valid. Pembacaan dibuat read-only dan tidak commit agar tidak memicu AuditListener berulang.",
				4, null));

		createSpan("Upload Lampiran dan Logo", rows);
		rows.appendChild(createRowActiveDefault("Saat cek logo/banner, refresh lampiran agar tidak memakai cache lama",
				"check_logo_upload_refresh_lampiran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika logo/banner belum ada, gunakan fallback agar halaman tetap tampil",
				"check_logo_upload_gunakan_fallback", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tutup streaming/session upload di blok finally",
				"upload_lampiran_tutup_session_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tutup FileInputStream upload di blok finally",
				"upload_lampiran_tutup_stream_finally", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tolak zip slip saat extract file ZIP",
				"upload_lampiran_zip_slip_protection", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Setelah upload berhasil, reset cache lokasi lampiran",
				"upload_lampiran_reset_cache_lokasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol download dan upload data pada master data jika didukung",
				"master_data_tampilkan_download_upload", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan upload lampiran",
				"catatan_upload_lampiran",
				"Upload dibuat aman agar stream tertutup, cache lokasi diperbarui, folder dibuat otomatis, dan file tidak hilang ketika proses copy terganggu.",
				4, null));

		createSpan("Hak Akses Tombol Upload Data", rows);
		createCatatanTerkait(
				"KETERKAITAN ANTAR-KONFIGURASI. Tombol \"Upload data\" baru MUNCUL bila DUA syarat terpenuhi: "
						+ "(1) role pengguna termasuk daftar di bawah (per modul), DAN "
						+ "(2) lolos sakelar global \"Hanya admin saja yg boleh upload\" (kunci: hanya_admin_saja_yg_boleh_uload, ada di span \"Lain-lain\"). "
						+ "Bila sakelar itu AKTIF, hanya pengguna yang lolos getApakahAdminBolehUpload() — yaitu Administrator, atau role yang terdaftar di \"admin_yg_boleh_upload\" — yang boleh. "
						+ "PENTING: untuk pengguna ber-konteks Sekolah (mis. Admin Sekolah), pemeriksaan getApakahAdminBolehUpload() SELALU gagal, sehingga daftar \"admin_yg_boleh_upload\" TIDAK berlaku bagi mereka dan tombol tetap tersembunyi walau role-nya sudah ada di daftar per modul di bawah. "
						+ "Jadi bila ingin Admin Sekolah tetap bisa upload, NON-AKTIFKAN \"hanya_admin_saja_yg_boleh_uload\". "
						+ "Catatan: saat pengguna tidak berhak, tombol kini LANGSUNG tidak ditampilkan (tidak lagi tampil sebentar lalu hilang).",
				rows);
		rows.appendChild(createRowNilai(
				"Role yang boleh menekan tombol Upload data Siswa (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role). Baku \"*\" agar Admin Sekolah (roleId kustom) tetap bisa upload; batasi ke roleId tertentu bila perlu. TERKAIT: hanya_admin_saja_yg_boleh_uload + admin_yg_boleh_upload (lihat catatan di atas).",
				"hak_akses_upload_data_siswa", "*"));
		rows.appendChild(createRowNilai(
				"Role yang boleh menekan tombol Upload data Calon Siswa (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role)",
				"hak_akses_upload_data_calon_siswa", ais.database.model.Tbmrole.ADMINISTRATOR));
		rows.appendChild(createRowNilai(
				"Role yang boleh menekan tombol Upload data Mahasiswa (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role)",
				"hak_akses_upload_data_mahasiswa", ais.database.model.Tbmrole.ADMINISTRATOR));
		rows.appendChild(createRowNilai(
				"Role yang boleh menekan tombol Upload data Registrasi/PMB (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role)",
				"hak_akses_upload_data_registrasi", ais.database.model.Tbmrole.ADMINISTRATOR));

		createSpan("Hak Akses Setujui Massal (Dasbor Pengajuan Anda / Workflow SOP)", rows);
		createCatatanTerkait(
				"Tombol \"Setujui Semua Antrian\" (select all) pada panel \"Proses yang sedang menunggu disposisi Anda\" "
						+ "di Dasbor Pengajuan Anda / Workflow SOP. Bila pengguna berhak, satu klik menyetujui SEMUA pengajuan "
						+ "yang menunggu disposisi-nya sekaligus (item yang butuh pilih rute manual atau catatan wajib DILEWATI). "
						+ "Otorisasi tetap aman: hanya langkah yang memang menunggu aktor = pengguna tersebut yang diproses. "
						+ "Dipakai di: DasboardSop.bolehSetujuiMassal.",
				rows);
		rows.appendChild(createRowNilai(
				"Role yang boleh memakai tombol Setujui Semua Antrian / select all (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role). Baku \"*\"; batasi mis. ke roleId pimpinan bila hanya pimpinan yang boleh melakukan setujui massal.",
				"hak_akses_setujui_massal_sop", "*"));

		createSpan("Hak Akses Download Password Pegawai", rows);
		createCatatanTerkait(
				"Tombol \"Password Pegawai\" / \"Password Pegawai bukan Dosen/Guru\" di menu Pendataan Pegawai "
						+ "(PegawaiAction) — membuatkan &amp; mengunduh Excel username/password pegawai (non-Dosen/Guru "
						+ "bila kolom Dosen/Guru aktif pada instansi ini). Data sensitif; batasi hanya ke role yang "
						+ "benar-benar berwenang. Dipakai di: PegawaiAction.generatePasswordDosen.",
				rows);
		rows.appendChild(createRowNilai(
				"Role yang boleh menekan tombol Password Pegawai (daftar roleId dipisah koma; \"am\" = Administrator, \"*\"/\"semua\" = semua role). Baku HANYA Administrator.",
				"hak_akses_download_password_pegawai", ais.database.model.Tbmrole.ADMINISTRATOR));

		createSpan("Kompatibilitas PostgreSQL dan Index", rows);
		rows.appendChild(createRowActiveDefault("Gunakan mode kompatibel PostgreSQL lama untuk pembuatan index",
				"init_index_kompatibel_postgresql_lama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jangan gunakan INCLUDE pada CREATE INDEX jika PostgreSQL lama belum mendukung",
				"init_index_hindari_include_postgresql_lama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cek dukungan CREATE INDEX IF NOT EXISTS sebelum menjalankan index otomatis",
				"init_index_check_if_not_exists_support", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan log debug proses InitIndex",
				"init_index_debug", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Catatan index database",
				"catatan_init_index",
				"Mode kompatibel membantu server PostgreSQL lama agar tidak gagal saat menemukan sintaks INCLUDE atau fitur index baru.",
				4, null));
	}


	/**
	 * Pengaturan portal publik/home.jsp versi modern.
	 *
	 * Tab ini mengumpulkan pengaturan modul yang tampil di halaman depan agar admin
	 * dapat membuka/menutup tombol layanan dan mengganti link tujuan tanpa mengubah
	 * file JSP. Default disusun konservatif: modul inti publik aktif, sedangkan modul
	 * yang biasanya membutuhkan integrasi/penyiapan khusus dibuat tidak aktif terlebih
	 * dahulu sampai admin mengaktifkannya.
	 */
	protected void initTabPortalHalamanDepan() {
		Rows rows = createSpan("Portal Halaman Depan");

		createSpan("Pengaturan Umum Home / Landing Page", rows);

		createSpan("Pengaturan Halaman Utama Setelah Login (ZKoss 5.5)", rows);
		rows.appendChild(createRowActiveDefault(
				"Gunakan tampilan utama ZKoss versi baru melalui servlet Main.java. Jika aktif, /main akan diarahkan ke /WEB-INF/z/x/y/pages/main/index2.zul selama parameter versilama tidak dikirim.",
				"default_gunakan_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Gunakan tampilan utama full HTML/JSP setelah login. Jika aktif, konfigurasi ini menjadi prioritas dan /main diarahkan ke /WEB-INF/baru/index.jsp.",
				"default_gunakan_versi_baru_full", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol menuju tampilan full HTML/JSP pada index2.zul",
				"main2_tampilkan_tombol_full_html", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol kembali ke tampilan lama pada index2.zul",
				"main2_tampilkan_tombol_versi_lama", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tinggi area konten index2.zul pada desktop", "main2_tinggi_iframe_desktop",
				"otomatis"));
		rows.appendChild(createRowNilai("Tinggi area konten index2.zul pada mobile/tablet", "main2_tinggi_iframe_mobile",
				"otomatis"));
		rows.appendChild(createRowNilai("Lebar menu samping index2.zul pada desktop", "main2_lebar_sidebar_desktop",
				"248px"));
		rows.appendChild(createRowNilai("Tinggi header index2.zul pada desktop", "main2_tinggi_header_desktop",
				"96px"));
		rows.appendChild(createRowNilai("Tinggi header index2.zul pada mobile/tablet", "main2_tinggi_header_mobile",
				"86px"));
		rows.appendChild(createRowNilai("Label tombol tampilan full HTML/JSP di index2.zul", "main2_label_tampilan_full",
				"Tampilan Full"));
		rows.appendChild(createRowNilai("Label tombol tampilan lama di index2.zul", "main2_label_versi_lama",
				"Versi Lama"));
		rows.appendChild(createRowNilai("Label judul menu samping di index2.zul", "main2_label_menu",
				"Menu"));
		rows.appendChild(createRowNilai("Label tab utama/home di index2.zul", "main2_label_home_tab",
				"Home"));
		rows.appendChild(createRowActiveDefault("Gunakan gaya tampilan index2.zul mengikuti template HTML/JSP modern",
				"main2_gunakan_template_jsp_modern", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan shortcut modul utama pada header index2.zul",
				"main2_tampilkan_shortcut_header", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan menu samping putih/terang pada index2.zul. Jika tidak aktif, menu mengikuti gradient theme institusi seperti template JSP modern.",
				"main2_sidebar_terang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Cadangan tinggi footer index2.zul dalam px untuk perhitungan scroll tunggal", "main2_footer_reserve_px",
				"0px"));
		rows.appendChild(createRowNilai("Catatan konfigurasi halaman utama ZKoss versi baru", "catatan_main2_zkoss",
				"index2.zul tetap mempertahankan mekanisme menu, tab, hak akses, pengumuman, notifikasi, customer service, dan session lama. Perubahan hanya pada shell visual agar lebih modern, responsif, memakai satu scroll utama pada area konten, panel profil kanan tidak terpotong, sidebar mengikuti theme aktif, dan menu dibuat lebih compact.",
				4, null));

		rows.appendChild(createRowActiveDefault(
				"Aktifkan home.jsp versi baru melalui servlet Index.java (default_home_versi_baru)",
				"default_home_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Judul kecil/eyebrow halaman depan", "judul_kecil_home_portal",
				"Enterprise Education Portal"));
		rows.appendChild(createRowNilai("Kalimat pengantar halaman depan", "deskripsi_home_portal",
				"Satu pintu layanan digital untuk mempercepat akses informasi, pendaftaran, akademik, dokumen, perpustakaan, repository, layanan sekolah, dan sistem pendukung operasional institusi pendidikan secara lebih tertib, modern, dan mudah digunakan.",
				4, null));
		rows.appendChild(createRowNilai("Tautan Google Play Home V3", "home_v3_mobile_app_android_url",
				"https://play.google.com/store/apps/details?id=com.ecampus.zishof"));
		rows.appendChild(createRowNilai("Tautan App Store Home V3", "home_v3_mobile_app_ios_url",
				"https://apps.apple.com/id/app/ecampus/id6503487876?l=id"));
		rows.appendChild(createRowNilai("Tautan aplikasi Desktop Home V3", "home_v3_desktop_app_url",
				"https://github.com/Zishof/ecampus-eschool-releases/releases/latest"));
		rows.appendChild(createRowNilai("Tautan Google Play eMedic Home V3", "home_v3_health_mobile_app_android_url", ""));
		rows.appendChild(createRowNilai("Tautan App Store eMedic Home V3", "home_v3_health_mobile_app_ios_url", ""));
		rows.appendChild(createRowNilai("Tautan aplikasi Desktop eMedic Home V3", "home_v3_health_desktop_app_url", ""));

		createSpan("Pengaturan Tampilan Panel Informasi Home", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan judul kecil/eyebrow di hero halaman depan",
				"tampilkan_judul_kecil_home_portal", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan kalimat pengantar/deskripsi di hero halaman depan",
				"tampilkan_deskripsi_home_portal", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan blok Portal Layanan Terpadu di atas grup portal",
				"tampilkan_section_portal_layanan_terpadu", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ringkasan manfaat empat kartu pada blok Portal Layanan Terpadu",
				"tampilkan_ringkasan_manfaat_portal_home", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan deskripsi panjang pada kotak pengantar Portal Perguruan Tinggi",
				"tampilkan_deskripsi_portal_perguruan_tinggi_home", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Catatan aturan tampilan panel informasi home", "catatan_tampilan_panel_informasi_home",
				"Secara default, blok Portal Layanan Terpadu, ringkasan manfaat, kalimat pengantar hero, dan deskripsi panjang portal perguruan tinggi dibuat tidak aktif agar home.jsp tampil lebih sederhana. Jika Portal Layanan Terpadu tidak aktif, maka setelah tombol Masuk ke Sistem halaman langsung menampilkan grup Portal Perguruan Tinggi atau grup lain yang aktif.",
				4, null));

		createSpan("Panel Home Internal Setelah Login — Dasbor eMedic", rows);
		rows.appendChild(createRowActiveDefault(
				"Tampilkan Dasbor eMedic (ringkasan rumah sakit/klinik: okupansi tempat tidur, pendapatan, "
				+ "diagnosa terbanyak, dll) secara otomatis di bawah Papan Pengumuman pada tab Home setelah login. "
				+ "Konfigurasi ini TERPISAH dari tombol \"eMedic\" pada header (digerbangi Tbmrole.emedic) — "
				+ "menonaktifkan konfigurasi ini TIDAK menyembunyikan tombol tersebut, hanya menghilangkan dashboard "
				+ "yang tertanam otomatis di tab Home. Default tidak aktif agar tab Home tetap ringkas untuk institusi "
				+ "yang tidak memakai modul rumah sakit/klinik.",
				"home_tampilkan_dashboard_emedic", Konfigurasi.TIDAK_AKTIF));

		createSpan("Label dan Copywriting Home", rows);
		rows.appendChild(createRowNilai("Petunjuk pengaturan teks home", "petunjuk_home_text_config",
				"Seluruh label dan kalimat utama pada home.jsp versi modern disiapkan sebagai konfigurasi. Admin dapat menyesuaikan gaya bahasa, nama layanan, deskripsi tombol, label navigasi, footer, dan copywriting halaman tanpa mengubah JSP.",
				4, null));
		rows.appendChild(createRowNilai("Teks Home - Institusi Pendidikan", "home_text_institusi_pendidikan",
				"Institusi Pendidikan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Enterprise Education - Portal Terpadu", "home_text_enterprise_education_portal_terpadu",
				"Enterprise Education - Portal Terpadu", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Logo Institusi", "home_text_logo_institusi",
				"Logo Institusi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal", "home_text_portal",
				"Portal", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Sistem Pendukung", "home_text_sistem_pendukung",
				"Sistem Pendukung", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Kontak", "home_text_kontak",
				"Kontak", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Login", "home_text_login",
				"Login", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Masuk ke Sistem", "home_text_masuk_ke_sistem",
				"Masuk ke Sistem", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Lihat Portal Layanan", "home_text_lihat_portal_layanan",
				"Lihat Portal Layanan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pendaftaran Online", "home_text_pendaftaran_online",
				"Pendaftaran Online", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Layanan Terpadu", "home_text_portal_layanan_terpadu",
				"Portal Layanan Terpadu", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses Cepat Layanan Digital Institusi", "home_text_akses_cepat_layanan_digital_institusi",
				"Akses Cepat Layanan Digital Institusi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Setiap tombol layanan di bawah ditampilkan berdasarkan tipe institusi dan konfigurasi modul yang diakt...", "home_text_setiap_tombol_layanan_di_bawah_ditampilkan_berdasarkan_tipe_institusi_da",
				"Setiap tombol layanan di bawah ditampilkan berdasarkan tipe institusi dan konfigurasi modul yang diaktifkan oleh admin. Dengan demikian, halaman depan tetap rapi, relevan, dan hanya menampilkan portal yang benar-benar digunakan oleh institusi.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Akses Lebih Cepat", "home_text_akses_lebih_cepat",
				"Akses Lebih Cepat", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pengguna langsung diarahkan ke layanan utama tanpa harus mencari menu terlalu jauh.", "home_text_pengguna_langsung_diarahkan_ke_layanan_utama_tanpa_harus_mencari_menu_te",
				"Pengguna langsung diarahkan ke layanan utama tanpa harus mencari menu terlalu jauh.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Terukur & Terkendali", "home_text_terukur_dan_terkendali",
				"Terukur & Terkendali", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Modul dapat dibuka atau ditutup dari pusat konfigurasi sesuai kebutuhan operasional.", "home_text_modul_dapat_dibuka_atau_ditutup_dari_pusat_konfigurasi_sesuai_kebutuhan",
				"Modul dapat dibuka atau ditutup dari pusat konfigurasi sesuai kebutuhan operasional.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Ekosistem Terpadu", "home_text_ekosistem_terpadu",
				"Ekosistem Terpadu", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Kampus, sekolah, yayasan, pesantren, unit usaha, dan layanan publik berada dalam satu pintu.", "home_text_kampus_sekolah_yayasan_pesantren_unit_usaha_dan_layanan_publik_berada_da",
				"Kampus, sekolah, yayasan, pesantren, unit usaha, dan layanan publik berada dalam satu pintu.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Ramah Perangkat", "home_text_ramah_perangkat",
				"Ramah Perangkat", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Tampilan disusun responsif agar nyaman digunakan dari desktop, tablet, maupun ponsel.", "home_text_tampilan_disusun_responsif_agar_nyaman_digunakan_dari_desktop_tablet_mau",
				"Tampilan disusun responsif agar nyaman digunakan dari desktop, tablet, maupun ponsel.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Perguruan Tinggi", "home_text_portal_perguruan_tinggi",
				"Portal Perguruan Tinggi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan digital untuk tata kelola kampus yang lebih terintegrasi", "home_text_layanan_digital_untuk_tata_kelola_kampus_yang_lebih_terintegrasi",
				"Layanan digital untuk tata kelola kampus yang lebih terintegrasi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Kelompok menu ini disiapkan untuk mendukung layanan kampus mulai dari login eCampus, penerimaan mahasi...", "home_text_kelompok_menu_ini_disiapkan_untuk_mendukung_layanan_kampus_mulai_dari_lo",
				"Kelompok menu ini disiapkan untuk mendukung layanan kampus mulai dari login eCampus, penerimaan mahasiswa baru, tracer study, dokumen, dashboard, pustaka digital, repository, pelaporan, akreditasi, jurnal, hingga penelitian sesuai modul yang diaktifkan.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Login eCampus", "home_text_login_ecampus",
				"Login eCampus", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses utama admin, dosen, mahasiswa, pimpinan, dan operator kampus.", "home_text_akses_utama_admin_dosen_mahasiswa_pimpinan_dan_operator_kampus",
				"Akses utama admin, dosen, mahasiswa, pimpinan, dan operator kampus.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Mahasiswa Baru", "home_text_mahasiswa_baru",
				"Mahasiswa Baru", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pendaftaran mahasiswa baru, informasi jalur masuk, dan proses registrasi online.", "home_text_pendaftaran_mahasiswa_baru_informasi_jalur_masuk_dan_proses_registrasi_o",
				"Pendaftaran mahasiswa baru, informasi jalur masuk, dan proses registrasi online.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Tracer Study", "home_text_tracer_study",
				"Tracer Study", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pendataan alumni, umpan balik lulusan, dan dukungan pelaporan kinerja lulusan.", "home_text_pendataan_alumni_umpan_balik_lulusan_dan_dukungan_pelaporan_kinerja_lulu",
				"Pendataan alumni, umpan balik lulusan, dan dukungan pelaporan kinerja lulusan.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - E-Library", "home_text_e_library",
				"E-Library", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan katalog pustaka, sirkulasi, dan akses informasi perpustakaan digital.", "home_text_layanan_katalog_pustaka_sirkulasi_dan_akses_informasi_perpustakaan_digit",
				"Layanan katalog pustaka, sirkulasi, dan akses informasi perpustakaan digital.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Dokumen", "home_text_dokumen",
				"Dokumen", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Publikasi dokumen, arsip, pedoman, dan file resmi sesuai hak akses pengguna.", "home_text_publikasi_dokumen_arsip_pedoman_dan_file_resmi_sesuai_hak_akses_pengguna",
				"Publikasi dokumen, arsip, pedoman, dan file resmi sesuai hak akses pengguna.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - E-Repository", "home_text_e_repository",
				"E-Repository", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pusat penyimpanan karya ilmiah, publikasi, dan dokumen akademik institusi.", "home_text_pusat_penyimpanan_karya_ilmiah_publikasi_dan_dokumen_akademik_institusi",
				"Pusat penyimpanan karya ilmiah, publikasi, dan dokumen akademik institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Dashboard", "home_text_dashboard",
				"Dashboard", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Ringkasan data strategis untuk pemantauan pimpinan dan unit kerja.", "home_text_ringkasan_data_strategis_untuk_pemantauan_pimpinan_dan_unit_kerja",
				"Ringkasan data strategis untuk pemantauan pimpinan dan unit kerja.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Feeder & EMIS", "home_text_feeder_dan_emis",
				"Feeder & EMIS", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Dukungan integrasi pelaporan eksternal sesuai kebutuhan institusi.", "home_text_dukungan_integrasi_pelaporan_eksternal_sesuai_kebutuhan_institusi",
				"Dukungan integrasi pelaporan eksternal sesuai kebutuhan institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akreditasi", "home_text_akreditasi",
				"Akreditasi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pengelolaan dokumen dan data pendukung akreditasi secara lebih rapi.", "home_text_pengelolaan_dokumen_dan_data_pendukung_akreditasi_secara_lebih_rapi",
				"Pengelolaan dokumen dan data pendukung akreditasi secara lebih rapi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - E-Journal", "home_text_e_journal",
				"E-Journal", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses publikasi jurnal, artikel, dan kanal ilmiah institusi.", "home_text_akses_publikasi_jurnal_artikel_dan_kanal_ilmiah_institusi",
				"Akses publikasi jurnal, artikel, dan kanal ilmiah institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Simlitabmas", "home_text_simlitabmas",
				"Simlitabmas", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Dukungan informasi penelitian dan pengabdian kepada masyarakat.", "home_text_dukungan_informasi_penelitian_dan_pengabdian_kepada_masyarakat",
				"Dukungan informasi penelitian dan pengabdian kepada masyarakat.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal perguruan tinggi hanya ditampilkan saat sistem terdeteksi sebagai PT dan modul terkait aktif di...", "home_text_portal_perguruan_tinggi_hanya_ditampilkan_saat_sistem_terdeteksi_sebagai",
				"Portal perguruan tinggi hanya ditampilkan saat sistem terdeteksi sebagai PT dan modul terkait aktif di konfigurasi.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Sekolah, Yayasan, dan Pondok Pesantren", "home_text_portal_sekolah_yayasan_dan_pondok_pesantren",
				"Portal Sekolah, Yayasan, dan Pondok Pesantren", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal operasional sekolah dan yayasan dalam satu halaman layanan", "home_text_portal_operasional_sekolah_dan_yayasan_dalam_satu_halaman_layanan",
				"Portal operasional sekolah dan yayasan dalam satu halaman layanan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Kelompok menu ini mendukung layanan sekolah, yayasan, dan pesantren seperti login pengelola, PPDB onli...", "home_text_kelompok_menu_ini_mendukung_layanan_sekolah_yayasan_dan_pesantren_sepert",
				"Kelompok menu ini mendukung layanan sekolah, yayasan, dan pesantren seperti login pengelola, PPDB online, akses siswa/wali, absensi siswa, serta layanan tamu sesuai konfigurasi yang diaktifkan oleh admin.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Sekolah & Pesantren", "home_text_portal_sekolah_dan_pesantren",
				"Portal Sekolah & Pesantren", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Sekolah / Yayasan", "home_text_portal_sekolah_yayasan",
				"Portal Sekolah / Yayasan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses pengelola, admin unit, guru, operator, dan pimpinan sekolah/yayasan.", "home_text_akses_pengelola_admin_unit_guru_operator_dan_pimpinan_sekolah_yayasan",
				"Akses pengelola, admin unit, guru, operator, dan pimpinan sekolah/yayasan.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - PPDB Online", "home_text_ppdb_online",
				"PPDB Online", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pendaftaran peserta didik baru, pengisian data calon siswa, dan informasi penerimaan.", "home_text_pendaftaran_peserta_didik_baru_pengisian_data_calon_siswa_dan_informasi",
				"Pendaftaran peserta didik baru, pengisian data calon siswa, dan informasi penerimaan.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Login Siswa / Wali", "home_text_login_siswa_wali",
				"Login Siswa / Wali", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses informasi akademik, pembayaran, kehadiran, dan layanan sekolah untuk siswa/wali.", "home_text_akses_informasi_akademik_pembayaran_kehadiran_dan_layanan_sekolah_untuk",
				"Akses informasi akademik, pembayaran, kehadiran, dan layanan sekolah untuk siswa/wali.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Absensi Siswa", "home_text_absensi_siswa",
				"Absensi Siswa", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pintu cepat untuk layanan presensi siswa sesuai kebutuhan operasional sekolah.", "home_text_pintu_cepat_untuk_layanan_presensi_siswa_sesuai_kebutuhan_operasional_se",
				"Pintu cepat untuk layanan presensi siswa sesuai kebutuhan operasional sekolah.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Buku Tamu", "home_text_buku_tamu",
				"Buku Tamu", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pencatatan tamu, kunjungan, dan layanan penerimaan pengunjung secara digital.", "home_text_pencatatan_tamu_kunjungan_dan_layanan_penerimaan_pengunjung_secara_digit",
				"Pencatatan tamu, kunjungan, dan layanan penerimaan pengunjung secara digital.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal sekolah/yayasan/pesantren hanya ditampilkan saat sistem terdeteksi sebagai sekolah atau yayasan...", "home_text_portal_sekolah_yayasan_pesantren_hanya_ditampilkan_saat_sistem_terdeteks",
				"Portal sekolah/yayasan/pesantren hanya ditampilkan saat sistem terdeteksi sebagai sekolah atau yayasan dan modul terkait aktif di konfigurasi.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan pendukung untuk memperluas kualitas pengalaman digital institusi", "home_text_layanan_pendukung_untuk_memperluas_kualitas_pengalaman_digital_institusi",
				"Layanan pendukung untuk memperluas kualitas pengalaman digital institusi", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Selain layanan inti akademik dan administrasi, institusi dapat menampilkan pintu masuk untuk eKantin, ...", "home_text_selain_layanan_inti_akademik_dan_administrasi_institusi_dapat_menampilka",
				"Selain layanan inti akademik dan administrasi, institusi dapat menampilkan pintu masuk untuk eKantin, anjungan mandiri, POS, kursus, karir, tamu, pengunjung pustaka, portal rekanan, dan layanan pendukung lain yang relevan.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - eKantin", "home_text_ekantin",
				"eKantin", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan transaksi kantin/unit usaha yang lebih praktis dan terdokumentasi.", "home_text_layanan_transaksi_kantin_unit_usaha_yang_lebih_praktis_dan_terdokumentas",
				"Layanan transaksi kantin/unit usaha yang lebih praktis dan terdokumentasi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Anjungan", "home_text_anjungan",
				"Anjungan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan mandiri untuk akses informasi dan transaksi tertentu di area publik institusi.", "home_text_layanan_mandiri_untuk_akses_informasi_dan_transaksi_tertentu_di_area_pub",
				"Layanan mandiri untuk akses informasi dan transaksi tertentu di area publik institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Point Of Sale", "home_text_point_of_sale",
				"Point Of Sale", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pengelolaan penjualan, transaksi kasir, dan unit usaha institusi.", "home_text_pengelolaan_penjualan_transaksi_kasir_dan_unit_usaha_institusi",
				"Pengelolaan penjualan, transaksi kasir, dan unit usaha institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Sistem Kursus", "home_text_sistem_kursus",
				"Sistem Kursus", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pengelolaan program kursus, kelas tambahan, jadwal, dan peserta.", "home_text_pengelolaan_program_kursus_kelas_tambahan_jadwal_dan_peserta",
				"Pengelolaan program kursus, kelas tambahan, jadwal, dan peserta.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Sistem Les / Private", "home_text_sistem_les_private",
				"Sistem Les / Private", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan pendampingan belajar, private class, dan pengelolaan peserta bimbingan.", "home_text_layanan_pendampingan_belajar_private_class_dan_pengelolaan_peserta_bimbi",
				"Layanan pendampingan belajar, private class, dan pengelolaan peserta bimbingan.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Lowongan Pekerjaan / Karir", "home_text_lowongan_pekerjaan_karir",
				"Lowongan Pekerjaan / Karir", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal rekrutmen resmi untuk publikasi lowongan, seleksi calon pegawai, dan informasi karir institusi.", "home_text_portal_rekrutmen_resmi_untuk_publikasi_lowongan_seleksi_calon_pegawai_da",
				"Portal rekrutmen resmi untuk publikasi lowongan, seleksi calon pegawai, dan informasi karir institusi.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Pengunjung Pustaka", "home_text_pengunjung_pustaka",
				"Pengunjung Pustaka", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Pencatatan kunjungan perpustakaan secara digital dan mudah dipantau.", "home_text_pencatatan_kunjungan_perpustakaan_secara_digital_dan_mudah_dipantau",
				"Pencatatan kunjungan perpustakaan secara digital dan mudah dipantau.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan pencatatan tamu dan kunjungan agar lebih tertib dan terdokumentasi.", "home_text_layanan_pencatatan_tamu_dan_kunjungan_agar_lebih_tertib_dan_terdokumenta",
				"Layanan pencatatan tamu dan kunjungan agar lebih tertib dan terdokumentasi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Absen Siswa", "home_text_absen_siswa",
				"Absen Siswa", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses cepat pencatatan kehadiran siswa untuk kebutuhan sekolah.", "home_text_akses_cepat_pencatatan_kehadiran_siswa_untuk_kebutuhan_sekolah",
				"Akses cepat pencatatan kehadiran siswa untuk kebutuhan sekolah.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Portal Rekanan", "home_text_portal_rekanan",
				"Portal Rekanan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Akses informasi dan layanan rekanan/vendor untuk kebutuhan pengadaan institusi.", "home_text_akses_informasi_dan_layanan_rekanan_vendor_untuk_kebutuhan_pengadaan_ins",
				"Akses informasi dan layanan rekanan/vendor untuk kebutuhan pengadaan institusi.", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Layanan Kustom Tambahan", "home_text_layanan_kustom_tambahan",
				"Layanan Kustom Tambahan", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Sistem pendukung dapat digunakan sebagai pintu masuk layanan publik maupun internal sehingga halaman d...", "home_text_sistem_pendukung_dapat_digunakan_sebagai_pintu_masuk_layanan_publik_maup",
				"Sistem pendukung dapat digunakan sebagai pintu masuk layanan publik maupun internal sehingga halaman depan terasa lebih hidup, profesional, dan bermanfaat bagi banyak jenis pengguna.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Belum ada modul halaman depan yang diaktifkan. Silakan aktifkan modul melalui menu Konfigurasi > Porta...", "home_text_belum_ada_modul_halaman_depan_yang_diaktifkan_silakan_aktifkan_modul_mel",
				"Belum ada modul halaman depan yang diaktifkan. Silakan aktifkan modul melalui menu Konfigurasi > Portal Halaman Depan.", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Infrastruktur digital terintegrasi yang didedikasikan untuk mengakselerasi kualitas pendidikan melalui...", "home_text_infrastruktur_digital_terintegrasi_yang_didedikasikan_untuk_mengakselera",
				"Infrastruktur digital terintegrasi yang didedikasikan untuk mengakselerasi kualitas pendidikan melalui otomatisasi birokrasi, keteraturan layanan, validitas data, dan kemudahan akses di lingkungan", 3, null));
		rows.appendChild(createRowNilai("Teks Home - Unduh Aplikasi Mobile", "home_text_unduh_aplikasi_mobile",
				"Unduh Aplikasi Mobile", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Google Play", "home_text_google_play",
				"Google Play", 2, null));
		rows.appendChild(createRowNilai("Teks Home - App Store", "home_text_app_store",
				"App Store", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Informasi Kontak", "home_text_informasi_kontak",
				"Informasi Kontak", 2, null));
		rows.appendChild(createRowNilai("Teks Home - Seluruh Hak Cipta Dilindungi.", "home_text_seluruh_hak_cipta_dilindungi",
				"Seluruh Hak Cipta Dilindungi.", 2, null));

		createSpan("Portal Perguruan Tinggi", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan Login eCampus pada portal perguruan tinggi",
				"tampilkan_modul_login_ecampus", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Login eCampus", "link_modul_login_ecampus", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan Dashboard pimpinan/monitoring", "tampilkan_modul_dashboard_pimpinan",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Dashboard pimpinan/monitoring", "link_modul_dashboard_pimpinan", "/dsh"));
		rows.appendChild(createRowActiveDefault("Tampilkan PMB / Penerimaan Mahasiswa Baru", "tampilkan_modul_pmb",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link PMB / Penerimaan Mahasiswa Baru", "link_modul_pmb", "/pmb"));
		rows.appendChild(createRowActiveDefault("Tampilkan Tracer Study / Alumni", "tampilkan_modul_alumni",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Tracer Study / Alumni", "link_modul_alumni", "/alumni"));
		rows.appendChild(createRowActiveDefault("Tampilkan E-Library / Sistem Perpustakaan", "tampilkan_modul_pustaka",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link E-Library / Sistem Perpustakaan", "link_modul_pustaka", "/pustaka"));
		rows.appendChild(createRowActiveDefault("Tampilkan Dokumen / DMS publik", "tampilkan_modul_dokumen",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Dokumen / DMS publik", "link_modul_dokumen", "/document"));
		rows.appendChild(createRowActiveDefault("Tampilkan E-Repository", "tampilkan_modul_repository",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link E-Repository", "link_modul_repository", "/repository"));
		rows.appendChild(createRowActiveDefault("Tampilkan Feeder & EMIS pada halaman depan", "tampilkan_modul_feeder_emis",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Feeder & EMIS", "link_modul_feeder_emis", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan Akreditasi pada halaman depan", "tampilkan_modul_akreditasi",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Akreditasi", "link_modul_akreditasi", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan E-Journal pada halaman depan", "tampilkan_modul_ejournal",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link E-Journal", "link_modul_ejournal", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan Simlitabmas / Penelitian dan Pengabdian pada halaman depan",
				"tampilkan_modul_simlitabmas", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Simlitabmas / Penelitian dan Pengabdian", "link_modul_simlitabmas", "/login"));
		rows.appendChild(createRowNilai("Catatan portal perguruan tinggi", "catatan_home_portal_perguruan_tinggi",
				"Grup portal perguruan tinggi hanya ditampilkan pada home.jsp jika hasil Common.chekPtAtauSekolah(null) menunjukkan ptData=true dan konfigurasi modul terkait aktif.",
				3, null));

		createSpan("Portal Sekolah, Yayasan, dan Pondok Pesantren", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan Portal Sekolah / Yayasan / Pondok Pesantren",
				"tampilkan_modul_portal_sekolah", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Portal Sekolah / Yayasan / Pondok Pesantren",
				"link_modul_portal_sekolah", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan PPDB Online sekolah", "tampilkan_modul_ppdb_sekolah",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link PPDB Online sekolah", "link_modul_ppdb_sekolah", "/ppdb"));
		rows.appendChild(createRowActiveDefault("Tampilkan Login Siswa / Wali", "tampilkan_modul_login_siswa_wali",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Login Siswa / Wali", "link_modul_login_siswa_wali", "/login"));
		rows.appendChild(createRowActiveDefault("Tampilkan Absensi Siswa", "tampilkan_modul_absen_siswa",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Absensi Siswa", "link_modul_absen_siswa", "/welsis"));
		rows.appendChild(createRowNilai("Catatan portal sekolah/yayasan/pesantren", "catatan_home_portal_sekolah",
				"Grup portal sekolah, yayasan, dan pondok pesantren hanya ditampilkan pada home.jsp jika hasil Common.chekPtAtauSekolah(null) menunjukkan yaData=true dan konfigurasi modul terkait aktif.",
				3, null));

		createSpan("Sistem Pendukung", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan eKantin", "tampilkan_modul_e_kantin", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link eKantin", "link_modul_e_kantin", "/kantin"));
		rows.appendChild(createRowActiveDefault("Tampilkan Anjungan", "tampilkan_modul_anjungan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Anjungan", "link_modul_anjungan", "/anjungan"));
		rows.appendChild(createRowActiveDefault("Tampilkan Point Of Sale", "tampilkan_modul_pos",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Point Of Sale", "link_modul_pos", "/pos"));
		rows.appendChild(createRowActiveDefault("Tampilkan Sistem Kursus", "tampilkan_modul_kursus",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Sistem Kursus", "link_modul_kursus", "/krrs"));
		rows.appendChild(createRowActiveDefault("Tampilkan Sistem Les / Private", "tampilkan_modul_les_private",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link Sistem Les / Private", "link_modul_les_private", "/les"));
		rows.appendChild(createRowActiveDefault("Tampilkan Lowongan Pekerjaan / Karir", "tampilkan_modul_karir",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Lowongan Pekerjaan / Karir", "link_modul_karir", "/karir"));
		rows.appendChild(createRowActiveDefault("Tampilkan Pengunjung Pustaka", "tampilkan_modul_pengunjung_pustaka",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Pengunjung Pustaka", "link_modul_pengunjung_pustaka", "/welpus"));
		rows.appendChild(createRowActiveDefault("Tampilkan Buku Tamu", "tampilkan_modul_buku_tamu", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Buku Tamu", "link_modul_buku_tamu", "/tamu"));
		rows.appendChild(createRowActiveDefault("Tampilkan Portal Rekanan / Vendor", "tampilkan_modul_portal_rekanan",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Portal Rekanan / Vendor", "link_modul_portal_rekanan", "/vendor"));
		rows.appendChild(createRowNilai("Catatan sistem pendukung", "catatan_home_sistem_pendukung",
				"Sistem pendukung dapat diaktifkan bertahap sesuai kesiapan modul, operator, perangkat, dan kebutuhan publikasi layanan. Link dapat diisi path relatif seperti /karir atau URL penuh seperti https://subdomain.domain.id/karir.",
				4, null));

		createSpan("Tombol Tambahan Kustom", rows);
		rows.appendChild(createRowNilai("Petunjuk tombol tambahan kustom", "petunjuk_home_button_tambahan",
				"Disediakan 10 tombol tambahan untuk kebutuhan link khusus institusi, misalnya portal eksternal, subdomain unit, formulir layanan, kanal bantuan, atau aplikasi pendukung lain. Jika tombol diaktifkan, maka Label dan URL/Link wajib diisi. Tombol yang aktif tetapi Label atau URL kosong tidak akan ditampilkan di home.jsp agar halaman depan tetap rapi.",
				5, null));
		for (int i = 1; i <= 10; i++) {
			String nomor = i < 10 ? "0" + i : String.valueOf(i);
			createSpan("Button Tambahan " + nomor, rows);
			rows.appendChild(createRowActiveDefault("Aktifkan Button Tambahan " + nomor,
					"tampilkan_button_tambahan_home_" + nomor, Konfigurasi.TIDAK_AKTIF));
			rows.appendChild(createRowNilai("Label Button Tambahan " + nomor + " (wajib diisi jika aktif)",
					"label_button_tambahan_home_" + nomor, ""));
			rows.appendChild(createRowNilai("URL / Link Button Tambahan " + nomor + " (wajib diisi jika aktif)",
					"link_button_tambahan_home_" + nomor, ""));
			rows.appendChild(createRowNilai("Deskripsi Button Tambahan " + nomor + " (opsional)",
					"deskripsi_button_tambahan_home_" + nomor,
					"Layanan tambahan yang dikonfigurasi oleh admin institusi.", 2, null));
			rows.appendChild(createRowNilai("Icon Font Awesome Button Tambahan " + nomor + " (opsional)",
					"icon_button_tambahan_home_" + nomor, "fas fa-link"));
		}


		createSpan("Rekomendasi Default Aktivasi", rows);
		rows.appendChild(createRowNilai("Rekomendasi modul yang sebaiknya aktif secara default", "rekomendasi_home_modul_aktif_default",
				"Login eCampus, Dashboard, PMB/PPDB, Tracer Study, E-Library, Dokumen/DMS, Repository, Portal Sekolah/Yayasan jika yaData=true, eKantin, Anjungan, Lowongan Pekerjaan/Karir, Pengunjung Pustaka, Buku Tamu, dan Portal Rekanan.",
				4, null));
		rows.appendChild(createRowNilai("Rekomendasi modul yang sebaiknya diaktifkan manual oleh admin",
				"rekomendasi_home_modul_aktif_manual",
				"Feeder & EMIS, Akreditasi, E-Journal, Simlitabmas, Point Of Sale, Sistem Kursus, dan Sistem Les/Private karena umumnya membutuhkan kesiapan integrasi, operator, data, atau kebijakan layanan khusus.",
				4, null));
	}




	/**
	 * Pengaturan khusus e-Learning.
	 *
	 * E-Learning pada versi ini diposisikan sebagai bagian dari login utama
	 * eCampus/eSchool, bukan portal publik yang berdiri sendiri. Karena itu
	 * header, footer, dan layout utama tetap mengikuti include aplikasi utama,
	 * sedangkan pengaturan teks, theme, dashboard, fitur pertemuan, absensi,
	 * materi, tugas, ujian, dan tampilan panel disediakan di tab ini agar admin
	 * dapat menyesuaikan tanpa mengubah kode JSP/Java.
	 */
	protected void initTabElearning() {
		Rows rows = createSpan("Elearning");
		appendElearningToggleRows(rows);
		appendElearningAksesRows(rows);
		appendElearningLayoutRows(rows);
		appendElearningAgendaRows(rows);
		appendElearningAbsensiRows(rows);
		appendElearningUjianRows(rows);
		appendElearningDashboardRows(rows);
		appendElearningTrenRows(rows);
		appendElearningLabelRows(rows);
		appendElearningPanelDescriptionRows(rows);
		appendElearningAdminNotes(rows);
	}

	/**
	 * Toggle-toggle utama e-Learning yang langsung memperbarui variabel in-memory
	 * DashboardTimelinePertemuan saat nilai diubah tanpa restart server.
	 */
	protected void appendElearningToggleRows(Rows rows) {
		createSpan("Toggle Fitur e-Learning (Berlaku Langsung)", rows);

		final Combobox cmbDasbor;
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol Dasbor di toolbar e-Learning (Linimasa). Dasbor menampilkan ringkasan aktivitas perkuliahan secara interaktif.",
				"tampilkan_dasbor_di_elearning", Konfigurasi.AKTIF,
				cmbDasbor = createComboActive()));
		cmbDasbor.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardTimelinePertemuan.tampilkan_dasbor_di_elearning =
						cmbDasbor.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
			}
		});

		final Combobox cmbObe;
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol OBE di toolbar e-Learning. Menampilkan Dasbor OBE per Semester (status RPS OBE, CPL, CPMK). Aktifkan hanya untuk Perguruan Tinggi yang sudah menggunakan kurikulum OBE.",
				"tampilkan_obe_di_elearning", Konfigurasi.TIDAK_AKTIF,
				cmbObe = createComboActive()));
		cmbObe.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardTimelinePertemuan.tampilkan_obe_di_elearning =
						cmbObe.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
			}
		});

		final Combobox cmbKal;
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kalender di e-Learning (Linimasa). Menampilkan widget kalender pertemuan.",
				"tampilkan_kalendar_di_elearning", Konfigurasi.TIDAK_AKTIF,
				cmbKal = createComboActive()));
		cmbKal.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardTimelinePertemuan.tampilkan_kalendar_di_elearning =
						cmbKal.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
			}
		});

		final Combobox cmbSingkron;
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol Sinkron ke Google Calendar di e-Learning. Mengizinkan pengguna menyinkronkan jadwal pertemuan ke Google Calendar.",
				"tampilkan_singkron_kalendar_di_elearning", Konfigurasi.TIDAK_AKTIF,
				cmbSingkron = createComboActive()));
		cmbSingkron.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardTimelinePertemuan.tampilkan_singkron_kalendar_di_elearning =
						cmbSingkron.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
			}
		});
	}

	protected void appendElearningAksesRows(Rows rows) {
		createSpan("Akses Login dan Logout", rows);
		rows.appendChild(createRowActiveDefault(
				"Login e-Learning menggunakan cookie tersendiri. Default tidak aktif karena e-Learning mengikuti login utama eCampus/eSchool.",
				"elearning_login_gunakan_cookie", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"E-Learning menggunakan session utama aplikasi. Pengguna yang sudah masuk ke eCampus/eSchool tidak perlu login ulang.",
				"elearning_login_pakai_session_utama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Saat keluar dari e-Learning, arahkan proses keluar ke mekanisme logout utama aplikasi.",
				"elearning_logout_ikuti_logout_utama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Saat logout e-Learning, hapus cookie e-Learning jika pernah dibuat.",
				"elearning_logout_hapus_cookie", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Cookie login e-Learning hanya dikirim melalui HTTPS. Aktifkan jika aplikasi selalu memakai HTTPS.",
				"elearning_login_cookie_secure", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Nama cookie login e-Learning", "elearning_login_cookie_name",
				"ECAMPUS_ELEARNING_LOGIN"));
		rows.appendChild(createRowNilai(
				"Lama cookie login e-Learning dalam hari. Isi 0 agar cookie tidak menyimpan login.",
				"elearning_login_cookie_hari", "0"));
		rows.appendChild(createRowNilai("Path cookie login e-Learning", "elearning_login_cookie_path", "/"));
		rows.appendChild(createRowNilai("URL tujuan setelah logout e-Learning", "elearning_logout_forward_url",
				"main"));
		rows.appendChild(createRowNilai("Pesan ketika sesi e-Learning berakhir", "elearning_pesan_session_habis",
				"Sesi belajar sudah berakhir. Silakan masuk kembali melalui halaman utama.", 2, null));
		rows.appendChild(createRowNilai("Pesan ketika pengguna belum login", "elearning_pesan_belum_login",
				"Silakan masuk terlebih dahulu untuk membuka ruang belajar.", 2, null));
	}

	protected void appendElearningLayoutRows(Rows rows) {
		createSpan("Layout, Header, Footer, CSS, dan Theme", rows);
		rows.appendChild(createRowActiveDefault(
				"E-Learning mengikuti header utama dari WEB-INF/baru/include/header.jsp.",
				"elearning_header_mengikuti_include_utama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Gunakan header mandiri khusus e-Learning. Default tidak aktif karena e-Learning berada di login utama.",
				"elearning_header_mandiri", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"E-Learning mengikuti footer utama dari folder include aplikasi.",
				"elearning_footer_mengikuti_include_utama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan base-theme.css dari theme utama", "elearning_gunakan_base_theme",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Lokasi base-theme.css", "elearning_css_base_theme",
				"/css/baru/base-theme.css"));
		rows.appendChild(createRowNilai("Lokasi CSS utama e-Learning", "elearning_css_base",
				"/css/baru/base-elearning.css"));
		rows.appendChild(createRowNilai("Lokasi CSS tambahan e-Learning", "elearning_css_custom",
				"/css/baru/custom-elearning.css"));
		rows.appendChild(createRowNilai("Theme fallback e-Learning", "elearning_theme_default",
				"/css/baru/hijau_kuning.css"));
		rows.appendChild(createRowActiveDefault("Gunakan kartu modern pada panel e-Learning",
				"elearning_gunakan_kartu_modern", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan chart HTML/CSS ringan, bukan JFreeChart atau ZK Chart",
				"elearning_chart_html_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan deskripsi singkat pada setiap panel e-Learning",
				"elearning_tampilkan_deskripsi_panel", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sederhanakan teks deskripsi agar mudah dipahami pengguna umum",
				"elearning_deskripsi_sederhana", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah data per halaman pada daftar e-Learning", "elearning_jumlah_data_per_halaman",
				"10"));
		rows.appendChild(createRowNilai("Jumlah data per halaman pada linimasa pertemuan", "elearning_timeline_per_page",
				"10"));
		rows.appendChild(createRowNilai("Batas data grafik ringkasan", "elearning_chart_limit", "12"));
		rows.appendChild(createRowActiveDefault("Muat isi tab e-Learning hanya saat tab dibuka",
				"elearning_lazy_load_tab", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bersihkan komponen lama sebelum dashboard dimuat ulang",
				"elearning_bersihkan_container_sebelum_reload", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Batas aman data dashboard yang dirender sekaligus",
				"elearning_maksimum_data_dashboard", "1000"));
	}

	protected void appendElearningAgendaRows(Rows rows) {
		createSpan("Agenda, Pertemuan, Materi, dan Kalender", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan tahun akademik pada perkuliahan",
				"tampilkanPilihanTaDiPerkuliakan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload RPS di agenda perkuliahan", "tampilkan_rps",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload SAP di agenda perkuliahan", "tampilkan_sap",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload absen manual di agenda perkuliahan",
				"tampilkan_absen_manual", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload soal UTS di agenda perkuliahan", "tampilkan_soal_uts",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload soal UAS di agenda perkuliahan", "tampilkan_soal_uas",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Lampiran lain di agenda perkuliahan. Pisahkan dengan koma jika lebih dari satu.",
				"tampilkan_lampiran_lain_di_agenda", ""));
		rows.appendChild(createRowNilai("Petunjuk konfigurasi lampiran dinamis tampilkan_[nama_lampiran]",
				"elearning_petunjuk_lampiran_dinamis",
				"Jika ada jenis lampiran tambahan dari AktifitasPerkuliahanHelper.lampiranLain, sistem membaca konfigurasi dengan pola tampilkan_NamaLampiran. Buat konfigurasi aktif untuk jenis lampiran yang ingin ditampilkan.",
				4, null));
		rows.appendChild(createRowActiveDefault("Komentar perkuliahan tampil langsung di halaman utama e-Learning",
				"komentar_tampil_di_halaman_utama_elearning", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan live streaming baru pada pertemuan",
				"aktifkan_live_streaming_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Urutkan absensi berdasarkan NIM", "absensi_urut_berdasarkan_nim",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Guru dan siswa hanya boleh melihat mata pelajaran dari sekolah yang sama",
				"guru_dan_siswa_hanya_boleh_melihat_matpel_satu_sekolah", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Instruksi AI default untuk membuat catatan pertemuan",
				"llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ", 3, null));
	}

	protected void appendElearningAbsensiRows(Rows rows) {
		createSpan("Absensi Online", rows);
		rows.appendChild(createRowActiveDefault("Absensi online meminta foto", "absen_online_menggunakan_foto",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Absensi online meminta lokasi", "absen_online_menggunakan_lokasi",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Absensi online meminta keterangan", "absen_online_menggunakan_keterangan",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Absensi online meminta video", "absen_online_menggunakan_video",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Absensi pegawai menggunakan foto",
				"aktifkan_absensi_pegawai_menggunakan_foto", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Batas waktu absensi memakai jumlah hari default",
				"jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jumlah hari batas waktu absensi", "jumlah_hari_batas_waktu_dalam_hari", "0"));
		rows.appendChild(createRowNilai("Teks bantuan absensi online", "elearning_text_bantuan_absensi",
				"Pastikan data kehadiran dikirim sesuai aturan pengajar atau sekolah.", 2, null));
	}

	protected void appendElearningUjianRows(Rows rows) {
		createSpan("Ujian Online", rows);
		rows.appendChild(createRowActiveDefault(
				"Mahasiswa atau siswa wajib melengkapi jawaban soal sebelum menyelesaikan ujian",
				"mahasiswa_harus_melengkapi_jawaban_soal", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Kuota peserta ujian yang diproses bersamaan", "kuota_ujian", "200"));
		rows.appendChild(createRowActiveDefault("Tampilkan pengingat tata tertib sebelum mulai ujian",
				"elearning_tampilkan_tata_tertib_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan progress jawaban saat ujian berlangsung",
				"elearning_tampilkan_progress_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pesan sebelum mulai ujian", "elearning_text_mulai_ujian",
				"Baca petunjuk dengan teliti sebelum mulai mengerjakan.", 2, null));
		rows.appendChild(createRowNilai("Pesan setelah ujian selesai", "elearning_text_selesai_ujian",
				"Jawaban sudah tersimpan. Silakan tunggu informasi hasil sesuai ketentuan pengajar.", 2, null));
		rows.appendChild(createRowNilai("Teks ketika jawaban belum lengkap", "elearning_text_jawaban_belum_lengkap",
				"Masih ada soal yang belum dijawab. Periksa kembali sebelum menyelesaikan ujian.", 2, null));

		// ---- Anti-Curang (CBT) DIPINDAH ke PER-UJIAN --------------------------------------
		// Seluruh pengaturan anti-curang TIDAK LAGI global di sini. Kini diatur per-ujian di tab
		// "Anti Curang" pada dialog "Kelola Soal Ujian" (kolom ac_* di PertemuanPunyaUjian), agar
		// fitur anti-curang boleh aktif/tidak per ujian dan tidak terpusat di konfigurasi.
		// Lihat: DetailUjianHelper (tab Anti Curang) + ProsesUjianHelper.buildCbtAntiCheatScript(ppu, ...).
	}

	protected void appendElearningDashboardRows(Rows rows) {
		createSpan("Dashboard dan Rekapitulasi", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan dashboard linimasa pertemuan",
				"elearning_tampilkan_dashboard_timeline_pertemuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan dashboard tren aktivitas perkuliahan",
				"elearning_tampilkan_dashboard_tren_aktivitas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan dasbor info dan materi",
				"elearning_tampilkan_dasbor_info_materi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi perkuliahan",
				"elearning_tampilkan_rekap_perkuliahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi jadwal pelajaran",
				"elearning_tampilkan_rekap_jadwal_pelajaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi ujian",
				"elearning_tampilkan_rekap_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi tugas",
				"elearning_tampilkan_rekap_tugas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi materi",
				"elearning_tampilkan_rekap_materi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi video",
				"elearning_tampilkan_rekap_video", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekapitulasi audio",
				"elearning_tampilkan_rekap_audio", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan buku bahan ajar",
				"elearning_tampilkan_buku_bahan_ajar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan artikel pendukung pembelajaran",
				"elearning_tampilkan_artikel", Konfigurasi.AKTIF));
	}

	protected void appendElearningTrenRows(Rows rows) {
		createSpan("Dasbor Tren Aktivitas", rows);
		rows.appendChild(createRowNilai("Cara perhitungan rekap aktivitas online",
				"perhitungan_rekap_online_dihitung_berdasarkan", "akses"));
		rows.appendChild(createRowNilai("Judul dashboard tren aktivitas", "elearning_title_dashboard_tren_aktivitas",
				"Tren Aktivitas Perkuliahan", 2, null));
		rows.appendChild(createRowNilai("Penjelasan dashboard tren aktivitas", "elearning_desc_dashboard_tren_aktivitas",
				"Ringkasan aktivitas belajar membantu melihat kelas yang aktif, kelas yang sepi, dan bagian yang perlu perhatian.",
				3, null));
		rows.appendChild(createRowNilai("Penjelasan filter fakultas/prodi", "elearning_desc_filter_tren_aktivitas",
				"Gunakan filter untuk melihat ringkasan berdasarkan fakultas, program studi, dosen, mahasiswa, tahun akademik, atau semester.",
				3, null));
		rows.appendChild(createRowNilai("Penjelasan grafik aktivitas peserta", "elearning_desc_chart_aktivitas_peserta",
				"Angka ini memperlihatkan bagian belajar yang paling sering digunakan peserta.", 2, null));
		rows.appendChild(createRowNilai("Penjelasan grafik aktivitas dosen", "elearning_desc_chart_aktivitas_dosen",
				"Angka ini memperlihatkan bahan, tugas, diskusi, dan aktivitas yang sudah dibuat pengajar.", 2, null));
	}

	protected void appendElearningLabelRows(Rows rows) {
		createSpan("Label dan Teks Utama", rows);
		appendElearningNilaiRows(rows, new String[][] {
				{ "Judul utama halaman e-Learning", "elearning_judul_utama", "Ruang Belajar Digital", "2" },
				{ "Deskripsi utama halaman e-Learning", "elearning_deskripsi_utama", "Akses jadwal, pertemuan, tugas, ujian, materi, dan kalender belajar dalam satu halaman yang mudah dipantau.", "3" },
				{ "Label portal", "elearning_label_portal", "E-Learning", "2" },
				{ "Label tombol muat ulang", "elearning_label_muat_ulang", "Muat Ulang", "2" },
				{ "Label tombol buka", "elearning_label_buka", "Buka", "2" },
				{ "Label tombol kembali", "elearning_label_kembali", "Kembali", "2" },
				{ "Label pencarian", "elearning_label_cari", "Cari", "2" },
				{ "Label semua data", "elearning_label_semua", "Semua", "2" },
				{ "Label tidak ada data", "elearning_label_tidak_ada_data", "Belum ada data yang dapat ditampilkan.", "2" },
				{ "Label loading", "elearning_label_loading", "Memuat data belajar...", "2" }
		});

		createSpan("Label dan Penjelasan Tab (index.jsp)", rows);
		appendElearningNilaiRows(rows, new String[][] {
				{ "Label tab Linimasa", "elearning_tab_linimasa", "Linimasa", "2" },
				{ "Penjelasan tab Linimasa", "elearning_desc_linimasa", "Urutan kegiatan belajar terbaru agar pengguna tahu apa yang perlu dibuka lebih dulu.", "3" },
				{ "Label tab Ringkasan", "elearning_tab_ringkasan", "Ringkasan", "2" },
				{ "Penjelasan tab Ringkasan", "elearning_desc_ringkasan", "Ikhtisar perkuliahan, kehadiran, materi, dan aktivitas belajar yang sedang berjalan.", "3" },
				{ "Label tab Ujian", "elearning_tab_ujian", "Ujian", "2" },
				{ "Penjelasan tab Ujian", "elearning_desc_ujian", "Daftar ujian yang tersedia, status pengerjaan, dan hasil yang dapat dilihat sesuai hak akses.", "3" },
				{ "Label tab Tugas", "elearning_tab_tugas", "Tugas", "2" },
				{ "Penjelasan tab Tugas", "elearning_desc_tugas", "Tugas individu atau kelompok yang perlu dikerjakan, dikumpulkan, dan dipantau nilainya.", "3" },
				{ "Label tab Materi", "elearning_tab_materi", "Materi", "2" },
				{ "Penjelasan tab Materi", "elearning_desc_materi", "Bahan ajar, file, audio, video, dan referensi belajar yang dibagikan pengajar.", "3" },
				{ "Label tab Kalender", "elearning_tab_kalender", "Kalender", "2" },
				{ "Penjelasan tab Kalender", "elearning_desc_kalender", "Jadwal belajar, pertemuan, ujian, dan agenda akademik yang tersusun berdasarkan waktu.", "3" },
				{ "Label tab Dasbor", "elearning_tab_dasbor", "Dasbor", "2" },
				{ "Penjelasan tab Dasbor", "elearning_desc_dasbor", "Ringkasan aktivitas belajar: pertemuan, materi, tugas, ujian, video, dan audio per semester.", "3" },
				{ "Label tab OBE", "elearning_tab_obe", "OBE", "2" },
				{ "Penjelasan tab OBE", "elearning_desc_obe", "Capaian Pembelajaran Lulusan (CPL), CPMK, dan status RPS OBE per tahun akademik dan semester.", "3" }
		});
		createSpan("Label Tab (ZK / TampilanELearning)", rows);
		appendElearningNilaiRows(rows, new String[][] {
				{ "Label tab agenda (ZK)", "elearning_tab_agenda", "Agenda", "2" },
				{ "Penjelasan tab agenda (ZK)", "elearning_desc_agenda", "Jadwal belajar tersusun agar pengguna tahu kegiatan yang perlu diikuti.", "3" },
				{ "Label tab pembelajaran (ZK)", "elearning_tab_pembelajaran", "Pembelajaran", "2" },
				{ "Penjelasan tab pembelajaran (ZK)", "elearning_desc_pembelajaran", "Daftar kelas, mata kuliah, pelajaran, bimbingan, dan kegiatan belajar yang bisa dibuka sesuai peran pengguna.", "3" },
				{ "Label tab info dan materi (ZK)", "elearning_tab_info_materi", "Info & Materi", "2" },
				{ "Penjelasan tab info dan materi (ZK)", "elearning_desc_info_materi_tab", "Bahan ajar, file, audio, video, tugas, dan informasi belajar dikumpulkan agar mudah ditemukan.", "3" }
		});
	}

	protected void appendElearningPanelDescriptionRows(Rows rows) {
		createSpan("Penjelasan Panel dan Dashboard", rows);
		appendElearningNilaiRows(rows, new String[][] {
				{ "Timeline Pertemuan", "elearning_desc_timeline_pertemuan", "Jadwal dan riwayat pertemuan membantu pengguna mengetahui kegiatan yang sudah dan akan berlangsung.", "3" },
				{ "Tren Aktivitas Perkuliahan", "elearning_desc_tren_aktivitas_perkuliahan", "Aktivitas belajar terlihat lebih jelas sehingga kelas yang perlu perhatian bisa cepat diketahui.", "3" },
				{ "Dasbor Info dan Materi", "elearning_desc_info_materi", "File, video, audio, tugas, dan materi diringkas agar bahan belajar cepat ditemukan.", "3" },
				{ "Rekapitulasi Perkuliahan", "elearning_desc_rekap_perkuliahan", "Kelas, bimbingan, KKN, PKL, dan kegiatan belajar ditampilkan sesuai peran pengguna.", "3" },
				{ "Rekapitulasi Jadwal Pelajaran", "elearning_desc_rekap_jadwal_pelajaran", "Jadwal pelajaran membantu sekolah melihat pembelajaran yang berlangsung setiap kelas.", "3" },
				{ "Rekapitulasi Ujian", "elearning_desc_rekap_ujian", "Daftar ujian, peserta, jadwal, dan hasil pengerjaan dapat dipantau dari satu tempat.", "3" },
				{ "Rekapitulasi Tugas", "elearning_desc_rekap_tugas", "Tugas yang sudah dibagikan, sudah dikumpulkan, dan belum selesai lebih mudah dipantau.", "3" },
				{ "Rekapitulasi Materi", "elearning_desc_rekap_materi", "Materi yang tersedia tampil rapi agar peserta langsung menemukan bahan yang dibutuhkan.", "3" },
				{ "Rekapitulasi Video", "elearning_desc_rekap_video", "Video pembelajaran mudah dibuka kembali sebagai bahan belajar ulang.", "3" },
				{ "Rekapitulasi Audio", "elearning_desc_rekap_audio", "Rekaman suara pembelajaran dapat diputar kembali saat dibutuhkan.", "3" },
				{ "Buku Bahan Ajar", "elearning_desc_buku_bahan_ajar", "Buku rujukan yang terhubung dengan mata kuliah atau pelajaran lebih mudah ditemukan.", "3" },
				{ "Artikel Pembelajaran", "elearning_desc_artikel", "Artikel ilmiah dan referensi pendukung dapat dipakai untuk memperkaya pembelajaran.", "3" },
				{ "Proses Ujian", "elearning_desc_proses_ujian", "Peserta dibantu membaca syarat, mengerjakan soal, dan melihat progres jawaban.", "3" },
				{ "Ringkasan Aktivitas Peserta", "elearning_desc_ringkasan_aktivitas_peserta", "Angka ini membantu melihat bagian belajar yang paling sering digunakan peserta.", "2" },
				{ "Ringkasan Aktivitas Dosen", "elearning_desc_ringkasan_aktivitas_dosen", "Angka ini membantu melihat bahan, tugas, diskusi, dan aktivitas yang sudah berjalan.", "2" },
				{ "Persentase Akses Materi", "elearning_desc_persentase_akses_materi", "Perbandingan peserta yang sudah membuka materi dengan seluruh peserta.", "2" },
				{ "Ringkasan Bahan dan Aktivitas Pertemuan", "elearning_desc_ringkasan_bahan_aktivitas", "Isi pertemuan dan aktivitas akses peserta terlihat dalam bentuk ringkas.", "2" },
				{ "Ringkasan Akses Media", "elearning_desc_ringkasan_akses_media", "Akses halaman, video, audio, dan materi ditampilkan agar penggunaan media belajar mudah dibaca.", "2" }
		});
	}

	protected void appendElearningAdminNotes(Rows rows) {
		createSpan("Catatan Admin", rows);
		rows.appendChild(createRowNilai("Panduan pengaturan e-Learning", "elearning_panduan_konfigurasi",
				"Tab ini mengatur e-Learning yang berjalan di dalam login utama eCampus/eSchool. Header dan layout tetap mengikuti aplikasi utama, sedangkan CSS, teks, deskripsi panel, paging, dashboard, login, logout, cookie, agenda, absensi, materi, tugas, ujian, video, audio, buku ajar, dan artikel dapat diatur dari konfigurasi.",
				5, null));
		rows.appendChild(createRowNilai("Catatan performa e-Learning", "elearning_catatan_performa",
				"Untuk menjaga memori tetap ringan, gunakan paging 10 data, lazy load tab, batasi data grafik, bersihkan komponen lama sebelum reload, dan hindari menampilkan data mentah terlalu banyak dalam satu halaman.",
				4, null));
		rows.appendChild(createRowNilai("Catatan header e-Learning", "elearning_catatan_header",
				"E-Learning tidak memakai header publik seperti vendor, pustaka, atau karir. Modul ini mengikuti header utama karena dibuka setelah pengguna masuk ke aplikasi utama.",
				4, null));
	}

	protected void appendElearningNilaiRows(Rows rows, String[][] data) {
		if (rows == null || data == null) {
			return;
		}
		for (int i = 0; i < data.length; i++) {
			String[] item = data[i];
			if (item == null || item.length < 3) {
				continue;
			}
			int rowCount = 2;
			if (item.length > 3) {
				try {
					rowCount = Integer.parseInt(item[3]);
				} catch (Exception e) {
					rowCount = 2;
				}
			}
			rows.appendChild(createRowNilai(item[0], item[1], item[2], rowCount, null));
		}
	}

	protected String defaultPanduanPortalKarir() {
		return "Panduan Portal Karir ini disusun sebagai pedoman resmi bagi setiap pelamar yang akan mengikuti proses penerimaan pegawai " +
				"melalui sistem informasi institusi. Portal ini menjadi pintu masuk utama untuk melihat informasi lowongan, mengirim " +
				"pendaftaran, melengkapi data diri, mengunggah dokumen persyaratan, serta memantau perkembangan seleksi. Seluruh proses " +
				"dirancang agar lebih tertib, transparan, terdokumentasi, dan mudah dipantau oleh pelamar maupun panitia. Oleh karena " +
				"itu, setiap pelamar diharapkan membaca panduan ini dengan saksama sebelum melakukan pendaftaran.\n\nTahap pertama yang " +
				"perlu dilakukan adalah memeriksa daftar lowongan yang tersedia. Pada bagian informasi lowongan, pelamar dapat melihat " +
				"nama posisi, periode pendaftaran, kuota atau kebutuhan formasi, jalur atau kelompok pendaftaran apabila tersedia, serta " +
				"keterangan tambahan yang berkaitan dengan persyaratan, tugas, tanggung jawab, fasilitas, dan catatan penting dari " +
				"panitia seleksi. Pelamar sebaiknya memastikan bahwa posisi yang dipilih benar-benar sesuai dengan latar belakang " +
				"pendidikan, pengalaman, kompetensi, dan minat kerja. Kesalahan memilih lowongan dapat menyebabkan proses verifikasi " +
				"menjadi lebih lama atau data pendaftaran tidak sesuai dengan kebutuhan seleksi. Apabila suatu lowongan belum dibuka, " +
				"sudah ditutup, atau dinonaktifkan oleh panitia, tombol pendaftaran dapat tidak tersedia. Kondisi tersebut bukan " +
				"merupakan gangguan sistem, melainkan mengikuti jadwal resmi yang ditetapkan oleh institusi.\n\nTahap kedua adalah " +
				"melakukan pendaftaran melalui tombol daftar pada lowongan yang dipilih. Sistem akan menampilkan formulir pendaftaran " +
				"calon pegawai. Pelamar wajib mengisi data diri dengan benar, lengkap, dan konsisten dengan dokumen resmi yang dimiliki. " +
				"Data seperti nama lengkap, alamat email, nomor telepon, tempat lahir, tanggal lahir, jenis kelamin, agama, dan alamat " +
				"tempat tinggal harus ditulis secara jelas. Alamat email harus aktif karena sistem dapat mengirimkan informasi akun, " +
				"pemberitahuan, atau instruksi lanjutan melalui email tersebut. Nomor telepon juga sebaiknya merupakan nomor yang masih " +
				"digunakan agar panitia dapat menghubungi pelamar apabila diperlukan. Jika terdapat perbedaan antara data yang diisi " +
				"dengan dokumen yang diunggah, panitia berhak meminta klarifikasi, revisi, atau menunda proses verifikasi sampai data " +
				"dianggap benar.\n\nTahap ketiga adalah mengunggah dokumen persyaratan. Dokumen yang umum diminta antara lain kartu " +
				"identitas, surat keterangan pendidikan, surat pengalaman kerja apabila tersedia, surat lamaran, curriculum vitae, dan " +
				"pas foto terbaru. Setiap dokumen perlu diunggah pada kategori yang tepat. File harus jelas, terbaca, tidak rusak, dan " +
				"tidak berisi informasi yang tidak relevan. Untuk dokumen hasil pindai, pastikan seluruh bagian terlihat utuh, tidak " +
				"terpotong, dan tidak buram. Untuk foto, gunakan gambar yang sopan dan sesuai kebutuhan administrasi. Pelamar tidak " +
				"disarankan mengunggah file berukuran sangat besar apabila tidak diperlukan, karena dapat memperlambat proses unggah. " +
				"Apabila sistem atau panitia menetapkan batas ukuran dan format file, pelamar wajib mengikuti ketentuan tersebut. Dokumen " +
				"yang tidak sesuai kategori, tidak terbaca, atau tidak memenuhi syarat dapat diberi status revisi.\n\nSetelah pendaftaran " +
				"berhasil dikirim, sistem dapat membuat akun akses Portal Karir bagi pelamar. Informasi username dan password dikirimkan " +
				"melalui email yang telah didaftarkan. Pelamar wajib menyimpan informasi tersebut secara aman dan tidak membagikannya " +
				"kepada pihak lain. Akun tersebut digunakan untuk masuk ke dashboard pelamar. Apabila email akses tidak ditemukan, " +
				"pelamar dapat menggunakan fitur kirim ulang akses dengan memasukkan email yang sudah terdaftar. Jika email tetap tidak " +
				"diterima, periksa folder spam, promosi, atau kotak masuk lain yang mungkin digunakan oleh penyedia layanan email. " +
				"Pelamar juga dapat menghubungi layanan bantuan sesuai kontak resmi institusi.\n\nPada dashboard pelamar, sistem " +
				"menampilkan ringkasan status seleksi, progres dokumen, informasi revisi, jadwal seleksi, ruang atau lokasi kegiatan, " +
				"serta pengumuman yang berkaitan dengan proses penerimaan. Informasi ini membantu pelamar mengetahui apakah dokumen sudah " +
				"diperiksa, apakah ada catatan perbaikan, apakah jadwal interview atau tes sudah tersedia, dan apakah hasil seleksi sudah " +
				"ditetapkan. Dashboard bukan hanya tempat melihat pengumuman, tetapi juga menjadi ruang pemantauan pribadi yang terhubung " +
				"dengan data pendaftaran. Pelamar dianjurkan login secara berkala agar tidak melewatkan informasi terbaru.\n\nApabila " +
				"dokumen mendapatkan catatan revisi, pelamar perlu membaca catatan tersebut dengan cermat. Revisi dapat berarti file " +
				"belum jelas, dokumen tidak sesuai kategori, data tidak sama dengan formulir, atau terdapat kekurangan informasi. Setelah " +
				"memperbaiki dokumen, pelamar dapat mengunggah ulang file melalui dashboard. Proses revisi harus dilakukan sebelum batas " +
				"waktu yang ditentukan oleh panitia. Keterlambatan memperbaiki dokumen dapat memengaruhi kelanjutan seleksi. Panitia " +
				"dapat menyetujui dokumen setelah file dianggap memenuhi ketentuan. Status terverifikasi menunjukkan bahwa dokumen telah " +
				"diperiksa dan diterima secara administrasi, tetapi tidak selalu berarti pelamar langsung dinyatakan lulus keseluruhan " +
				"seleksi.\n\nSelama proses seleksi, pelamar wajib menjaga kejujuran dan keabsahan data. Setiap pernyataan, dokumen, " +
				"sertifikat, pengalaman kerja, dan informasi pendidikan harus dapat dipertanggungjawabkan. Jika di kemudian hari " +
				"ditemukan data yang tidak benar, dipalsukan, atau menyesatkan, institusi berhak membatalkan pendaftaran, menggugurkan " +
				"pelamar dari proses seleksi, atau mengambil tindakan sesuai ketentuan yang berlaku. Pendaftaran melalui Portal Karir " +
				"tidak boleh disalahgunakan untuk mencoba mengakses data pihak lain, mengunggah file berbahaya, mengirim spam, atau " +
				"melakukan tindakan yang dapat mengganggu keamanan sistem.\n\nPelamar juga perlu memahami bahwa setiap informasi resmi " +
				"hanya berlaku apabila diumumkan melalui kanal yang ditentukan oleh institusi, seperti Portal Karir, email resmi, " +
				"pengumuman tertulis, atau kontak panitia yang sah. Hati-hati terhadap pihak yang mengatasnamakan panitia dan meminta " +
				"pembayaran, imbalan, atau data rahasia di luar prosedur resmi. Pada prinsipnya, proses rekrutmen institusi harus " +
				"berjalan objektif, tertib, dan sesuai aturan. Apabila terdapat informasi yang meragukan, pelamar sebaiknya menghubungi " +
				"layanan bantuan sebelum mengambil tindakan.\n\nPanduan ini dapat disesuaikan oleh admin melalui menu konfigurasi. " +
				"Institusi dapat menambahkan ketentuan khusus, memperbarui cara pendaftaran, mengubah instruksi dokumen, atau " +
				"menyesuaikan narasi sesuai kebijakan internal. Apabila terdapat perbedaan antara panduan umum ini dengan pengumuman " +
				"resmi terbaru, maka pengumuman resmi terbaru dari institusi menjadi acuan utama. Dengan membaca dan mengikuti panduan " +
				"ini, pelamar diharapkan dapat menjalani proses pendaftaran secara lebih mudah, tertib, dan bertanggung jawab.\n\nDalam " +
				"penggunaan sehari-hari, pelamar dianjurkan menggunakan perangkat pribadi atau perangkat yang dipercaya, terutama ketika " +
				"mengisi data penting dan mengunggah dokumen. Setelah selesai menggunakan portal, khususnya pada komputer bersama, " +
				"pelamar sebaiknya menekan tombol keluar agar sesi tidak digunakan oleh orang lain. Periksa kembali setiap isian sebelum " +
				"menekan tombol kirim, karena data yang sudah masuk akan menjadi dasar pemeriksaan administrasi. Apabila terjadi " +
				"kesalahan pengisian, segera perbaiki melalui dashboard jika fitur perbaikan tersedia, atau hubungi panitia dengan " +
				"menjelaskan bagian yang perlu dikoreksi. Gunakan bahasa yang sopan dan jelas saat berkomunikasi dengan panitia. Hindari " +
				"mengirim pertanyaan berulang secara berlebihan karena dapat memperlambat proses pelayanan. Simpan salinan dokumen asli " +
				"dan bukti pendaftaran secara rapi sampai seluruh proses seleksi selesai. Dengan disiplin mengikuti petunjuk ini, pelamar " +
				"turut membantu panitia menjaga proses rekrutmen tetap tertib, adil, profesional, dan mudah ditelusuri.";
	}

	protected String defaultPanduanPortalVendor() {
		return "Panduan Portal Vendor ini disusun sebagai acuan resmi bagi perusahaan, badan usaha, penyedia barang, penyedia " +
				"jasa, konsultan, kontraktor, distributor, pelaku usaha, dan pihak lain yang ingin menjadi rekanan institusi me" +
				"lalui sistem pendaftaran vendor secara daring. Portal ini dibuat untuk membantu proses pendataan, verifikasi, " +
				"pemutakhiran profil, pengelolaan dokumen legalitas, serta komunikasi awal antara calon vendor dan unit pengada" +
				"an. Dengan adanya portal ini, proses administrasi diharapkan berjalan lebih tertib, transparan, terdokumentasi" +
				", dan mudah ditelusuri kembali apabila diperlukan. Setiap calon vendor dianjurkan membaca panduan ini sebelum " +
				"melakukan pendaftaran agar memahami alur, kewajiban, batasan, dan tata cara penggunaan layanan.\n\nLangkah perta" +
				"ma adalah memastikan bahwa perusahaan atau usaha yang didaftarkan benar-benar memiliki hubungan dengan kegiata" +
				"n penyediaan barang, pekerjaan konstruksi, jasa konsultansi, jasa lainnya, pemeliharaan, distribusi, produksi," +
				" layanan teknologi, logistik, keamanan, kebersihan, katering, percetakan, pelatihan, atau bidang lain yang rel" +
				"evan dengan kebutuhan institusi. Data yang dimasukkan harus menggambarkan identitas usaha yang sebenarnya. Nam" +
				"a perusahaan, nama pemilik atau penanggung jawab, alamat, nomor telepon, kontak utama, alamat email, NPWP, inf" +
				"ormasi rekening, bidang pekerjaan, dan keterangan usaha harus ditulis secara benar, jelas, dan konsisten denga" +
				"n dokumen resmi. Kesalahan penulisan dapat memperlambat proses pemeriksaan karena petugas perlu melakukan klar" +
				"ifikasi ulang.\n\nLangkah kedua adalah melakukan pendaftaran awal melalui formulir yang tersedia pada halaman Po" +
				"rtal Vendor. Calon vendor perlu mengisi data pokok usaha dan menggunakan alamat email aktif. Email tersebut me" +
				"njadi salah satu identitas utama untuk pengiriman informasi akun, pemberitahuan, dan komunikasi administratif." +
				" Satu alamat email sebaiknya digunakan oleh satu vendor agar tidak terjadi tumpang tindih data. Apabila vendor" +
				" pernah mendaftar tetapi lupa username atau password, gunakan fitur kirim ulang akses apabila tersedia. Jika f" +
				"itur tersebut belum menyelesaikan kendala, hubungi unit pengadaan atau admin sistem dengan menyebutkan nama ve" +
				"ndor, email terdaftar, dan nomor kontak yang dapat dihubungi.\n\nSetelah pendaftaran berhasil, sistem dapat memb" +
				"uat akun akses vendor. Akun ini dipakai untuk masuk ke dashboard vendor dan melengkapi data yang belum tersedi" +
				"a. Username dan password harus disimpan dengan aman oleh petugas yang ditunjuk perusahaan. Hindari membagikan " +
				"akses kepada pihak yang tidak berwenang. Apabila terjadi pergantian staf internal vendor, perusahaan bertanggu" +
				"ng jawab memastikan akses tetap dikelola oleh orang yang sah. Untuk keamanan, gunakan perangkat yang dipercaya" +
				", hindari login dari komputer umum, dan selalu tekan tombol keluar setelah selesai menggunakan portal, terutam" +
				"a jika perangkat digunakan bersama.\n\nPada dashboard vendor, pengguna dapat melihat ringkasan status profil, ke" +
				"lengkapan data, dokumen yang sudah diunggah, dokumen yang masih perlu diperbaiki, dan informasi pengumuman yan" +
				"g disediakan oleh institusi. Dashboard dirancang agar vendor mudah memahami apa yang perlu dilengkapi tanpa ha" +
				"rus datang langsung ke kantor. Informasi yang tampil pada dashboard bukan berarti vendor otomatis disetujui se" +
				"bagai rekanan aktif. Status akhir tetap mengikuti proses verifikasi, evaluasi, dan kebijakan internal institus" +
				"i. Vendor yang datanya lengkap akan lebih mudah diperiksa karena petugas memiliki dasar informasi yang jelas.\n" +
				"\nBagian dokumen merupakan bagian penting dalam Portal Vendor. Dokumen yang dapat diminta antara lain identitas" +
				" perusahaan, rekening bank, akta pendirian, akta perubahan, NPWP, pakta integritas, surat pernyataan kebenaran" +
				" dokumen, izin usaha, sertifikat pendukung, pengalaman pekerjaan, katalog produk, atau dokumen lain sesuai keb" +
				"utuhan. File yang diunggah harus jelas, terbaca, tidak rusak, tidak terkunci, dan sesuai kategori. Dokumen has" +
				"il pemindaian sebaiknya menampilkan seluruh halaman secara utuh. Jika dokumen terdiri atas beberapa halaman, g" +
				"abungkan dalam satu file yang rapi apabila memungkinkan. Jangan mengunggah file yang tidak relevan, berbahaya," +
				" atau berisi informasi yang menyesatkan.\n\nSetiap dokumen dapat memiliki status pemeriksaan. Status belum diver" +
				"ifikasi berarti dokumen sudah tercatat atau belum diperiksa oleh petugas. Status harus direvisi berarti dokume" +
				"n belum memenuhi ketentuan, kurang jelas, salah kategori, tidak lengkap, atau memerlukan perbaikan data. Statu" +
				"s terverifikasi berarti dokumen telah diperiksa dan dinyatakan sesuai secara administrasi pada saat pemeriksaa" +
				"n dilakukan. Vendor wajib membaca catatan revisi dengan teliti dan mengunggah ulang file yang benar sebelum ba" +
				"tas waktu yang ditetapkan. Keterlambatan memperbaiki dokumen dapat memengaruhi kelanjutan proses administrasi." +
				"\n\nVendor bertanggung jawab atas kebenaran seluruh data dan dokumen yang disampaikan. Apabila di kemudian hari " +
				"ditemukan data palsu, dokumen tidak sah, informasi yang sengaja disembunyikan, atau keterangan yang tidak sesu" +
				"ai keadaan sebenarnya, institusi berhak menolak pendaftaran, menonaktifkan akun vendor, membatalkan proses ker" +
				"ja sama, atau mengambil tindakan sesuai ketentuan yang berlaku. Prinsip utama pendaftaran vendor adalah kejuju" +
				"ran, keterbukaan, kepatuhan terhadap aturan, dan kesiapan menjalankan kewajiban sebagai rekanan.\n\nInformasi re" +
				"kening bank harus diisi secara hati-hati. Nama bank, nomor rekening, dan nama pemilik rekening perlu sesuai de" +
				"ngan dokumen pendukung. Kesalahan rekening dapat menimbulkan hambatan dalam proses administrasi pembayaran apa" +
				"bila vendor kelak terlibat dalam kerja sama atau pekerjaan. Apabila perusahaan memiliki lebih dari satu rekeni" +
				"ng, pilih rekening yang resmi digunakan untuk transaksi institusi. Setiap perubahan rekening harus diperbarui " +
				"dan dapat diminta bukti pendukung oleh petugas.\n\nBagian bidang pekerjaan atau kualifikasi membantu institusi m" +
				"emahami jenis barang dan jasa yang dapat disediakan oleh vendor. Isian ini sebaiknya dibuat ringkas tetapi jel" +
				"as, misalnya menyebutkan bidang utama, pengalaman, produk unggulan, wilayah layanan, tenaga pendukung, sertifi" +
				"kasi, atau kemampuan khusus. Data kualifikasi yang baik memudahkan unit pengadaan mengidentifikasi vendor sesu" +
				"ai kebutuhan. Namun, pencantuman bidang pekerjaan pada portal tidak selalu berarti vendor otomatis diundang pa" +
				"da setiap proses pengadaan. Undangan, evaluasi, dan keputusan kerja sama tetap mengikuti prosedur resmi instit" +
				"usi.\n\nPengumuman pada Portal Vendor digunakan sebagai media informasi umum. Isi pengumuman dapat berupa pember" +
				"itahuan jadwal, kebutuhan dokumen, perubahan kebijakan, informasi pemeliharaan sistem, atau informasi pengadaa" +
				"n yang dapat diketahui oleh rekanan. Vendor dianjurkan memeriksa portal secara berkala agar tidak melewatkan i" +
				"nformasi penting. Apabila terdapat perbedaan antara informasi pada portal dan surat resmi terbaru yang dikelua" +
				"rkan institusi, maka dokumen atau pengumuman resmi terbaru yang dinyatakan berlaku menjadi acuan utama.\n\nPorta" +
				"l Vendor tidak boleh digunakan untuk tindakan yang merugikan sistem, institusi, atau pihak lain. Vendor dilara" +
				"ng mencoba mengakses data vendor lain, mengunggah file berbahaya, mengirimkan spam, memanipulasi data, menggun" +
				"akan identitas palsu, atau melakukan tindakan yang dapat mengganggu keamanan layanan. Aktivitas penggunaan dap" +
				"at dicatat oleh sistem untuk kebutuhan pengawasan dan audit. Apabila ditemukan penyalahgunaan, admin berhak me" +
				"mbatasi akses atau melakukan tindakan pengamanan sesuai kebutuhan.\n\nApabila mengalami kendala, vendor dapat me" +
				"nghubungi layanan bantuan yang disediakan institusi. Jelaskan kendala secara singkat dan jelas, sebutkan nama " +
				"vendor, alamat email terdaftar, nomor telepon, serta lampirkan tangkapan layar apabila diperlukan. Hindari men" +
				"girim pertanyaan berulang tanpa informasi yang lengkap karena dapat memperlambat proses bantuan. Unit pengadaa" +
				"n atau admin sistem akan membantu sesuai kewenangan dan jam layanan yang berlaku.\n\nPanduan ini dapat disesuaik" +
				"an oleh admin melalui menu konfigurasi tanpa mengubah kode program. Institusi dapat menambahkan ketentuan khus" +
				"us, memperbarui standar dokumen, mengganti narasi bantuan, menyesuaikan kebijakan privasi, atau menambahkan in" +
				"struksi sesuai prosedur internal. Dengan mengikuti panduan ini, vendor diharapkan dapat menggunakan Portal Ven" +
				"dor secara tertib, profesional, bertanggung jawab, dan mendukung proses pengadaan yang lebih rapi, akuntabel, " +
				"serta mudah dipantau.";
	}

	protected String defaultSyaratKetentuanPortalVendor() {
		return "Syarat dan ketentuan Portal Vendor mengatur tata cara penggunaan layanan pendaftaran dan pengelolaan data reka" +
				"nan secara daring. Vendor wajib memberikan data yang benar, lengkap, dan dapat dipertanggungjawabkan. Setiap d" +
				"okumen yang diunggah harus sesuai dengan kategori yang diminta, masih berlaku apabila memiliki masa berlaku, j" +
				"elas terbaca, serta tidak mengandung informasi palsu atau menyesatkan. Institusi berhak memeriksa, meminta per" +
				"baikan, menolak, menonaktifkan, atau membatalkan data vendor apabila ditemukan ketidaksesuaian. Penggunaan por" +
				"tal harus dilakukan oleh pihak yang berwenang mewakili vendor. Akun, username, password, dan akses dashboard w" +
				"ajib dijaga kerahasiaannya. Vendor dilarang memakai portal untuk mengganggu sistem, mengakses data pihak lain," +
				" mengunggah file berbahaya, mengirim spam, atau melakukan tindakan yang bertentangan dengan peraturan. Pendaft" +
				"aran pada portal tidak otomatis menjadikan vendor sebagai rekanan aktif atau pemenang pengadaan. Keputusan ver" +
				"ifikasi, evaluasi, undangan, dan kerja sama tetap mengikuti kebijakan serta prosedur resmi institusi.";
	}

	protected String defaultKebijakanPrivasiPortalVendor() {
		return "Kebijakan privasi Portal Vendor menjelaskan bahwa data yang dikirimkan melalui portal digunakan untuk kebutuha" +
				"n administrasi rekanan, verifikasi dokumen, komunikasi resmi, pemutakhiran profil, evaluasi awal, dan pengelol" +
				"aan informasi pengadaan sesuai kewenangan institusi. Data yang dapat diproses meliputi identitas perusahaan, k" +
				"ontak, alamat, email, nomor telepon, informasi rekening, dokumen legalitas, bidang pekerjaan, catatan verifika" +
				"si, serta informasi pendukung lain yang diunggah oleh vendor. Institusi berupaya menjaga data tersebut melalui" +
				" pembatasan akses internal dan tata kelola sistem yang tersedia. Vendor tetap bertanggung jawab menjaga keaman" +
				"an akun, perangkat, dan dokumen yang diunggah. Informasi dapat digunakan oleh unit terkait sepanjang diperluka" +
				"n untuk proses administrasi, audit, evaluasi, pelaporan, atau pemenuhan kewajiban hukum dan kebijakan institus" +
				"i.";
	}

	protected String defaultBantuanPortalVendor() {
		return "Layanan bantuan Portal Vendor disediakan untuk membantu calon vendor atau vendor terdaftar ketika mengalami ke" +
				"ndala pendaftaran, login, pengiriman ulang akses, pengisian profil, unggah dokumen, pembacaan status verifikas" +
				"i, atau pemahaman informasi pengumuman. Sebelum menghubungi admin, vendor disarankan memeriksa kembali koneksi" +
				" internet, ukuran file, format dokumen, alamat email yang digunakan, folder spam email, serta kebenaran data y" +
				"ang dimasukkan. Saat mengirim permintaan bantuan, sebutkan nama vendor, email terdaftar, nomor telepon aktif, " +
				"penjelasan singkat kendala, dan tangkapan layar apabila tersedia. Informasi yang lengkap membantu admin melaku" +
				"kan pengecekan lebih cepat. Bantuan diberikan sesuai jam layanan dan kewenangan unit pengadaan atau pengelola " +
				"sistem.";
	}


	protected void initTabVendor() {
		Rows rows = createSpan("Vendor");

		createSpan("Pengaturan Umum Portal Vendor", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan Portal Rekanan / Vendor pada halaman depan", "tampilkan_modul_portal_rekanan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link Portal Rekanan / Vendor", "link_modul_portal_rekanan", "/vendor"));
		rows.appendChild(createRowActiveDefault("Login dan logout Portal Vendor menggunakan cookie. Default tidak aktif agar sesi vendor hanya tersimpan di server aplikasi.", "vendor_login_gunakan_cookie", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Masa berlaku cookie login Vendor dalam hari", "vendor_login_cookie_hari", "7"));
		rows.appendChild(createRowNilai("File theme CSS Portal Vendor", "theme_css_portal_vendor", "hijau_kuning.css"));
		rows.appendChild(createRowNilai("Catatan pengaturan theme Vendor", "vendor_catatan_theme_css", "Header Portal Vendor memuat base-theme.css, base-vendor.css, lalu file theme aktif seperti hijau_kuning.css. Style khusus Vendor dipusatkan di base-vendor.css agar warna tema tetap mudah diganti dari konfigurasi.", 4, null));

		createSpan("Teks Header dan Menu Vendor", rows);
		rows.appendChild(createRowNilai("Judul header Portal Vendor", "judul_header_vendor", "Pendaftaran Vendor"));
		rows.appendChild(createRowNilai("Label menu Daftar Vendor", "vendor_menu_daftar", "Daftar Vendor"));
		rows.appendChild(createRowNilai("Label menu Login Vendor", "vendor_menu_login", "Login Vendor"));
		rows.appendChild(createRowNilai("Label menu Dashboard Vendor", "vendor_menu_dashboard", "Dashboard Vendor"));
		rows.appendChild(createRowNilai("Label menu Logout Vendor", "vendor_menu_logout", "Keluar"));

		createSpan("Teks Halaman Utama Sebelum Login", rows);
		rows.appendChild(createRowNilai("Badge hero Vendor", "vendor_teks_hero_badge", "Portal e-Procurement & Rekanan"));
		rows.appendChild(createRowNilai("Judul hero Vendor", "vendor_teks_hero_title", "Pendaftaran Vendor Terintegrasi ERP"));
		rows.appendChild(createRowNilai("Deskripsi hero Vendor", "vendor_teks_hero_deskripsi", "Daftarkan perusahaan, pantau pengumuman proyek, lengkapi legalitas, unggah dokumen kualifikasi, dan kelola profil rekanan secara mandiri dalam satu portal modern.", 4, null));
		rows.appendChild(createRowNilai("Informasi form daftar Vendor", "vendor_teks_daftar_info", "Isi form pendaftaran awal. Satu email hanya dapat digunakan untuk satu vendor. Apabila akun pernah dibuat namun email akses hilang, gunakan fitur kirim ulang akses.", 4, null));

		createSpan("Teks Dashboard Vendor Setelah Login", rows);
		rows.appendChild(createRowNilai("Deskripsi dashboard Vendor", "vendor_dashboard_deskripsi", "Lengkapi profil, legalitas, kualifikasi, dan dokumen agar data vendor siap diverifikasi oleh tim pengadaan.", 4, null));
		rows.appendChild(createRowNilai("Deskripsi status akun Vendor", "vendor_dashboard_teks_status", "Status menunjukkan apakah akun sudah dapat digunakan dalam proses vendor.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi kelengkapan profil Vendor", "vendor_dashboard_teks_profil", "Semakin lengkap data profil, semakin mudah tim pengadaan menilai kesiapan vendor.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi dokumen terverifikasi Vendor", "vendor_dashboard_teks_dokumen", "Dokumen terverifikasi adalah berkas yang sudah diperiksa dan dinyatakan sesuai.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi dokumen revisi Vendor", "vendor_dashboard_teks_revisi", "Angka revisi menunjukkan dokumen yang perlu diperbaiki atau diunggah ulang.", 3, null));

		createSpan("Footer Portal Vendor", rows);
		rows.appendChild(createRowNilai("Informasi pengenalan footer Vendor", "informasi_pengenalan_vendor", "Pusat Manajemen Pengadaan dan Layanan Rekanan membantu vendor mendaftar, melengkapi dokumen, dan mengikuti informasi pengadaan secara lebih tertib.", 4, null));
		rows.appendChild(createRowNilai("Judul kolom menu footer Vendor", "vendor_footer_menu_title", "Menu Vendor"));
		rows.appendChild(createRowNilai("Judul kolom bantuan footer Vendor", "vendor_footer_help_title", "Layanan Bantuan"));
		rows.appendChild(createRowActiveDefault("Tampilkan link Pendaftaran Vendor di footer", "vendor_footer_tampilkan_pendaftaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Syarat dan Ketentuan di footer", "vendor_footer_tampilkan_syarat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Pengumuman Proyek di footer", "vendor_footer_tampilkan_pengumuman", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Kontak di footer", "vendor_footer_tampilkan_kontak", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Panduan Pendaftaran di footer", "vendor_footer_tampilkan_panduan_pendaftaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Kebijakan Privasi di footer", "vendor_footer_tampilkan_kebijakan_privasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Bantuan Vendor di footer", "vendor_footer_tampilkan_bantuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label footer Pendaftaran Vendor", "vendor_footer_label_pendaftaran", "Pendaftaran Vendor"));
		rows.appendChild(createRowNilai("Label footer Syarat dan Ketentuan", "vendor_footer_label_syarat", "Syarat & Ketentuan"));
		rows.appendChild(createRowNilai("Label footer Pengumuman Proyek", "vendor_footer_label_pengumuman", "Pengumuman Proyek"));
		rows.appendChild(createRowNilai("Label footer Kontak", "vendor_footer_label_kontak", "Kontak"));
		rows.appendChild(createRowNilai("Label footer Panduan Pendaftaran", "vendor_footer_label_panduan_pendaftaran", "Panduan Pendaftaran"));
		rows.appendChild(createRowNilai("Label footer Kebijakan Privasi", "vendor_footer_label_kebijakan_privasi", "Kebijakan Privasi"));
		rows.appendChild(createRowNilai("Label footer Bantuan Vendor", "vendor_footer_label_bantuan", "Bantuan Vendor"));

		createSpan("Halaman Panduan Footer Vendor", rows);
		rows.appendChild(createRowNilai("File JSP Panduan Pendaftaran jika ingin memakai halaman custom", "vendor_footer_file_panduan_pendaftaran", "/WEB-INF/baru/modul/vendor/panduan_pendaftaran.jsp"));
		rows.appendChild(createRowNilai("File JSP Syarat dan Ketentuan jika ingin memakai halaman custom", "vendor_footer_file_syarat_ketentuan", "/WEB-INF/baru/modul/vendor/syarat_ketentuan.jsp"));
		rows.appendChild(createRowNilai("File JSP Kebijakan Privasi jika ingin memakai halaman custom", "vendor_footer_file_kebijakan_privasi", "/WEB-INF/baru/modul/vendor/kebijakan_privasi.jsp"));
		rows.appendChild(createRowNilai("File JSP Bantuan Vendor jika ingin memakai halaman custom", "vendor_footer_file_bantuan", "/WEB-INF/baru/modul/vendor/bantuan.jsp"));
		rows.appendChild(createRowNilai("Catatan fallback halaman footer Vendor", "vendor_footer_catatan_fallback", "Jika file JSP custom tidak ditemukan, Portal Vendor otomatis menampilkan teks default dari konfigurasi ini. Path file custom dibatasi di folder /WEB-INF/baru/modul/vendor/ dan harus berakhiran .jsp agar lebih aman.", 4, null));
		rows.appendChild(createRowNilai("Judul halaman Panduan Pendaftaran Vendor", "vendor_panduan_pendaftaran_judul", "Panduan Pendaftaran Portal Vendor"));
		rows.appendChild(createRowNilai("Teks halaman Panduan Pendaftaran Vendor", "vendor_panduan_pendaftaran_text", defaultPanduanPortalVendor(), 24, null));
		rows.appendChild(createRowNilai("Judul halaman Syarat dan Ketentuan Vendor", "vendor_syarat_ketentuan_judul", "Syarat dan Ketentuan Portal Vendor"));
		rows.appendChild(createRowNilai("Teks halaman Syarat dan Ketentuan Vendor", "vendor_syarat_ketentuan_text", defaultSyaratKetentuanPortalVendor(), 10, null));
		rows.appendChild(createRowNilai("Judul halaman Kebijakan Privasi Vendor", "vendor_kebijakan_privasi_judul", "Kebijakan Privasi Portal Vendor"));
		rows.appendChild(createRowNilai("Teks halaman Kebijakan Privasi Vendor", "vendor_kebijakan_privasi_text", defaultKebijakanPrivasiPortalVendor(), 10, null));
		rows.appendChild(createRowNilai("Judul halaman Bantuan Vendor", "vendor_bantuan_judul", "Bantuan Portal Vendor"));
		rows.appendChild(createRowNilai("Teks halaman Bantuan Vendor", "vendor_bantuan_text", defaultBantuanPortalVendor(), 10, null));

		createSpan("Email Akun Portal Vendor", rows);
		rows.appendChild(createRowNilai("Judul email akun Vendor", "vendor_email_judul", "Informasi Akun Portal Vendor"));
		rows.appendChild(createRowNilai("Subjudul email akun Vendor", "vendor_email_subjudul", "Akses resmi untuk melengkapi profil, legalitas, kualifikasi, dan dokumen rekanan.", 3, null));
		rows.appendChild(createRowNilai("Paragraf pembuka email Vendor", "vendor_email_paragraf_pembuka", "Terima kasih telah mendaftar sebagai calon vendor. Akun ini digunakan untuk melengkapi profil, legalitas, kualifikasi, dan dokumen rekanan secara mandiri.", 3, null));
		rows.appendChild(createRowNilai("Paragraf akun email Vendor", "vendor_email_paragraf_akun", "Berikut username dan password untuk masuk ke Portal Vendor. Simpan informasi ini dengan aman dan gunakan hanya oleh petugas resmi perusahaan.", 3, null));
		rows.appendChild(createRowNilai("Paragraf profil email Vendor", "vendor_email_paragraf_profil", "Setelah login, lengkapi data perusahaan agar tim pengadaan mudah mengenali identitas, kontak, dan informasi rekening vendor.", 3, null));
		rows.appendChild(createRowNilai("Paragraf legalitas email Vendor", "vendor_email_paragraf_legalitas", "Lengkapi legalitas dan kualifikasi agar proses evaluasi administrasi dapat dilakukan lebih cepat, transparan, dan terdokumentasi.", 3, null));
		rows.appendChild(createRowNilai("Paragraf kualifikasi email Vendor", "vendor_email_paragraf_kualifikasi", "Bidang pekerjaan khusus dapat dilengkapi dengan pengalaman, izin usaha, tenaga ahli, dukungan distributor, layanan purna jual, dan produk atau jasa utama.", 3, null));
		rows.appendChild(createRowNilai("Paragraf dokumen email Vendor", "vendor_email_paragraf_dokumen", "Setiap dokumen memiliki status sehingga vendor dapat melihat mana yang belum diperiksa, perlu revisi, atau sudah terverifikasi.", 3, null));
		rows.appendChild(createRowNilai("Paragraf pengumuman email Vendor", "vendor_email_paragraf_pengumuman", "Akun vendor juga dapat digunakan untuk memantau pengumuman pengadaan dan menjaga data perusahaan tetap mutakhir.", 3, null));
		rows.appendChild(createRowNilai("Paragraf penutup email Vendor", "vendor_email_paragraf_penutup", "Mohon segera login dan lengkapi data dengan benar. Status akhir vendor tetap mengikuti proses verifikasi dan evaluasi internal.", 3, null));
	}


	protected void initTabKarir() {
		Rows rows = createSpan("Karir");

		createSpan("Pengaturan Umum Portal Karir", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan modul Lowongan Pekerjaan / Karir pada halaman depan", "tampilkan_modul_karir", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Link modul Lowongan Pekerjaan / Karir", "link_modul_karir", "/karir"));
		rows.appendChild(createRowActiveDefault("Login dan logout Portal Karir menggunakan cookie. Default tidak aktif agar sesi utama tetap berada di server.", "karir_login_logout_menggunakan_cookie", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Masa berlaku cookie login Karir dalam hari", "karir_cookie_login_max_age_hari", "7"));
		rows.appendChild(createRowNilai("File theme CSS Portal Karir", "theme_css_portal_karir", "hijau_kuning.css"));
		rows.appendChild(createRowNilai("Label judul Portal Karir", "label_karir_portal", "Portal KARIR"));
		rows.appendChild(createRowNilai("Warna utama Portal Karir", "warna_utama_portal_karir", "var(--theme-primary, #2563eb)"));
		rows.appendChild(createRowNilai("Warna kedua Portal Karir", "warna_kedua_portal_karir", "var(--theme-hover, #7c3aed)"));
		rows.appendChild(createRowNilai("Catatan pengaturan theme Karir", "karir_catatan_theme_css", "Header Portal Karir memuat base-theme.css, base-karir.css, lalu file theme aktif seperti hijau_kuning.css. Style khusus Karir dipusatkan di base-karir.css agar theme warna tetap mudah diganti dari konfigurasi.", 4, null));

		createSpan("Teks Halaman Utama Karir", rows);
		rows.appendChild(createRowNilai("Badge hero Karir", "karir_hero_badge", "Portal Seleksi Terintegrasi ERP"));
		rows.appendChild(createRowNilai("Judul hero Karir", "karir_hero_judul", "Bergabung Bersama Tim Terbaik Kami"));
		rows.appendChild(createRowNilai("Deskripsi hero Karir", "karir_hero_deskripsi", "Temukan lowongan sesuai kompetensi Anda, lengkapi data diri, unggah dokumen persyaratan, dan pantau hasil seleksi dalam satu halaman.", 4, null));
		rows.appendChild(createRowNilai("Judul alur seleksi Karir", "karir_alur_judul", "Alur Seleksi Singkat"));
		rows.appendChild(createRowNilai("Deskripsi alur seleksi Karir", "karir_alur_deskripsi", "Pilih lowongan, kirim data, lalu pantau pengumuman melalui akun calon pegawai.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi pengumuman Karir", "karir_pengumuman_deskripsi", "Informasi resmi untuk pelamar ditampilkan di sini.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi syarat Karir", "karir_syarat_deskripsi", "Siapkan data diri dan dokumen dengan benar agar proses pemeriksaan lebih cepat.", 3, null));

		createSpan("Teks Dashboard Karir Setelah Login", rows);
		rows.appendChild(createRowNilai("Deskripsi dashboard Karir", "karir_dashboard_deskripsi", "Pantau dokumen, jadwal, dan hasil seleksi tanpa perlu datang ke kantor.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi panel pengumuman dashboard Karir", "karir_dashboard_pengumuman_deskripsi", "Ringkasan jadwal, lokasi, dan keputusan seleksi ditampilkan di tabel ini.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi panel progres dashboard Karir", "karir_dashboard_progres_deskripsi", "Tahapan ini membantu Anda melihat posisi proses seleksi saat ini.", 3, null));
		rows.appendChild(createRowNilai("Deskripsi panel dokumen dashboard Karir", "karir_dashboard_dokumen_deskripsi", "Dokumen yang belum disetujui masih dapat dikirim ulang sesuai catatan panitia.", 3, null));

		createSpan("Menu Header dan Footer Karir", rows);
		rows.appendChild(createRowNilai("Judul grup menu footer Karir", "karir_footer_menu_judul", "Menu KARIR"));
		rows.appendChild(createRowNilai("Judul grup panduan footer Karir", "karir_footer_panduan_judul", "Panduan & Informasi"));
		rows.appendChild(createRowNilai("Label menu Lowongan", "karir_menu_label_lowongan", "Info Lowongan Kerja"));
		rows.appendChild(createRowNilai("Label menu Pendaftaran", "karir_menu_label_pendaftaran", "Pendaftaran Calon Pegawai"));
		rows.appendChild(createRowNilai("Label menu Syarat", "karir_menu_label_syarat", "Syarat & Ketentuan"));
		rows.appendChild(createRowNilai("Label menu Login Pengumuman", "karir_menu_label_login_pengumuman", "Login Pengumuman"));
		rows.appendChild(createRowActiveDefault("Tampilkan link Info Lowongan Kerja di footer", "karir_footer_tampilkan_info_lowongan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Pendaftaran Calon Pegawai di footer", "karir_footer_tampilkan_pendaftaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Syarat dan Ketentuan di footer", "karir_footer_tampilkan_syarat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Login Pengumuman di footer", "karir_footer_tampilkan_login_pengumuman", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Informasi pengenalan footer Karir", "informasi_pengenalan_karir", "Portal KARIR membantu pendaftaran, unggah dokumen, verifikasi, jadwal seleksi, dan pengumuman berjalan lebih rapi.", 4, null));

		createSpan("Halaman Panduan Footer Karir", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan link Panduan Pendaftaran di footer", "karir_footer_tampilkan_panduan_pendaftaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Kebijakan Privasi di footer", "karir_footer_tampilkan_kebijakan_privasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link Bantuan Pelamar di footer", "karir_footer_tampilkan_bantuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label Panduan Pendaftaran", "karir_footer_label_panduan_pendaftaran", "Panduan Pendaftaran"));
		rows.appendChild(createRowNilai("Label Kebijakan Privasi", "karir_footer_label_kebijakan_privasi", "Kebijakan Privasi"));
		rows.appendChild(createRowNilai("Label Bantuan Pelamar", "karir_footer_label_bantuan", "Bantuan Pelamar"));
		rows.appendChild(createRowNilai("File JSP Panduan Pendaftaran jika ingin memakai halaman custom", "karir_footer_file_panduan_pendaftaran", "/WEB-INF/baru/modul/karir/panduan_pendaftaran.jsp"));
		rows.appendChild(createRowNilai("File JSP Syarat dan Ketentuan jika ingin memakai halaman custom", "karir_footer_file_syarat_ketentuan", "/WEB-INF/baru/modul/karir/syarat_ketentuan.jsp"));
		rows.appendChild(createRowNilai("File JSP Kebijakan Privasi jika ingin memakai halaman custom", "karir_footer_file_kebijakan_privasi", "/WEB-INF/baru/modul/karir/kebijakan_privasi.jsp"));
		rows.appendChild(createRowNilai("File JSP Bantuan Pelamar jika ingin memakai halaman custom", "karir_footer_file_bantuan", "/WEB-INF/baru/modul/karir/bantuan.jsp"));
		rows.appendChild(createRowNilai("Catatan fallback halaman footer Karir", "karir_footer_catatan_fallback", "Jika file JSP custom tidak ditemukan, Portal Karir otomatis menampilkan teks default dari konfigurasi ini. Path file custom dibatasi di folder /WEB-INF/baru/modul/karir/ dan harus berakhiran .jsp agar lebih aman.", 4, null));
		rows.appendChild(createRowNilai("Judul halaman Panduan Pendaftaran", "karir_panduan_pendaftaran_judul", "Panduan Pendaftaran Portal Karir"));
		rows.appendChild(createRowNilai("Teks halaman Panduan Pendaftaran", "karir_panduan_pendaftaran_text", defaultPanduanPortalKarir(), 18, null));
		rows.appendChild(createRowNilai("Judul halaman Syarat dan Ketentuan", "karir_syarat_ketentuan_judul", "Syarat dan Ketentuan Portal Karir"));
		rows.appendChild(createRowNilai("Teks halaman Syarat dan Ketentuan", "karir_syarat_ketentuan_text", "Syarat dan ketentuan Portal Karir mengatur tata cara penggunaan layanan rekrutmen resmi institusi. Pelamar wajib " +
				"memberikan data yang benar, memakai alamat email dan nomor telepon aktif, memilih lowongan sesuai minat dan kompetensi, " +
				"serta mengunggah dokumen yang jelas dan dapat dipertanggungjawabkan. Panitia berwenang memeriksa, menyetujui, meminta " +
				"revisi, menolak, atau membatalkan data apabila ditemukan ketidaksesuaian. Keputusan seleksi mengikuti kebijakan " +
				"institusi dan informasi resmi yang diumumkan melalui portal atau kanal komunikasi yang ditentukan. Pelamar dilarang " +
				"menyalahgunakan akun, mengunggah file berbahaya, menggunakan data pihak lain tanpa izin, atau melakukan tindakan yang " +
				"dapat mengganggu keamanan sistem.", 8, null));
		rows.appendChild(createRowNilai("Judul halaman Kebijakan Privasi", "karir_kebijakan_privasi_judul", "Kebijakan Privasi Portal Karir"));
		rows.appendChild(createRowNilai("Teks halaman Kebijakan Privasi", "karir_kebijakan_privasi_text", "Kebijakan privasi Portal Karir menjelaskan bahwa data pelamar digunakan untuk kebutuhan administrasi penerimaan pegawai, " +
				"verifikasi dokumen, penjadwalan seleksi, komunikasi resmi, dan pengambilan keputusan oleh panitia yang berwenang. Data " +
				"pribadi, dokumen, dan informasi kontak diproses secara terbatas sesuai kebutuhan seleksi. Institusi berupaya menjaga " +
				"kerahasiaan data dengan mekanisme akses internal dan tata kelola sistem yang tersedia. Pelamar tetap bertanggung jawab " +
				"menjaga kerahasiaan username, password, dan perangkat yang digunakan untuk login.", 8, null));
		rows.appendChild(createRowNilai("Judul halaman Bantuan Pelamar", "karir_bantuan_judul", "Bantuan Pelamar Portal Karir"));
		rows.appendChild(createRowNilai("Teks halaman Bantuan Pelamar", "karir_bantuan_text", "Layanan bantuan Portal Karir disediakan untuk membantu pelamar ketika mengalami kendala saat melihat lowongan, " +
				"mendaftar, menerima email akses, login, mengunggah dokumen, membaca status verifikasi, atau memahami jadwal seleksi. " +
				"Sebelum menghubungi panitia, pelamar disarankan memeriksa kembali koneksi internet, ukuran file, format dokumen, folder " +
				"spam email, dan kebenaran data yang dimasukkan. Jelaskan kendala secara singkat, sertakan nama lengkap, email terdaftar, " +
				"nomor telepon, dan tangkapan layar apabila diperlukan agar panitia dapat melakukan pengecekan lebih cepat.", 8, null));

		createSpan("Email Portal Karir", rows);
		rows.appendChild(createRowNilai("Email pengirim pendaftaran Karir", "email_pendaftaran_karir_sender", Common.getKonfigurasi("default_email", "info@ecampus.id").getNilai()));
		rows.appendChild(createRowNilai("Email admin penerima notifikasi Karir", "email_pendaftaran_karir_admin", Common.getKonfigurasi("default_email", "info@ecampus.id").getNilai()));
		rows.appendChild(createRowNilai("Subjek email akses calon pegawai", "email_pendaftaran_karir_subject", "Username dan Password Portal KARIR"));
		rows.appendChild(createRowNilai("Judul email calon pegawai", "karir_email_candidate_title", "Informasi Akun Pendaftaran Calon Pegawai"));
		rows.appendChild(createRowNilai("Pembuka email calon pegawai", "karir_email_candidate_intro", "Data pendaftaran Anda telah diterima dan akun akses Portal KARIR telah dibuat.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 1", "karir_email_candidate_p1", "Terima kasih sudah mendaftar. Data Anda sudah tercatat dan akan diperiksa oleh panitia seleksi.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 2", "karir_email_candidate_p2", "Setelah login, Anda dapat melihat data diri, dokumen, jadwal, dan hasil seleksi pada Portal KARIR.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 3", "karir_email_candidate_p3", "Simpan username dan password dengan baik. Gunakan fitur Kirim Ulang Akses jika email akses tidak ditemukan.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 4", "karir_email_candidate_p4", "Pastikan dokumen yang diunggah jelas dan sesuai. Jika ada revisi, segera unggah ulang dokumen yang diminta.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 5", "karir_email_candidate_p5", "Alur seleksi dimulai dari pendaftaran, pemeriksaan berkas, jadwal seleksi, lalu pengumuman hasil.", 3, null));
		rows.appendChild(createRowNilai("Isi email calon pegawai 6", "karir_email_candidate_p6", "Hubungi panitia bila ada kendala. Cek portal dan email secara berkala agar tidak melewatkan informasi penting.", 3, null));
		rows.appendChild(createRowNilai("Judul email admin Karir", "karir_email_admin_title", "Pendaftaran Calon Pegawai Baru"));
		rows.appendChild(createRowNilai("Pembuka email admin Karir", "karir_email_admin_intro", "Ada pendaftaran baru dari Portal KARIR yang perlu diverifikasi admin.", 3, null));
	}

	protected void initTabAngketKuesioner() {
		Rows rows = createSpan("Angket Kuesioner");

		createSpan("Pengaturan Umum Angket", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan pemeriksaan angket/kuesioner umum saat pengguna login atau masuk halaman utama",
				"angket_saat_login_diaktifkan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label header halaman kuesioner", "label_kuesioner_header",
				"Sistem Informasi Kuesioner"));
		rows.appendChild(createRowNilai("Label nama instansi default pada halaman kuesioner", "label_universitas",
				"Nama Instansi Kampus"));

		createSpan("Angket Dosen oleh Mahasiswa", rows);
		rows.appendChild(createRowActiveDefault("Input angket penilaian dosen harus berdasarkan kalender akademik",
				"input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester selanjutnya. Misal: jika sekarang semester 2, maka mahasiswa wajib mengisi angket di semester 1",
				"checklist_penilaian_dosen", Konfigurasi.AKTIF, true));
		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester berlangsung. Misal: jika sekarang semester 1, maka mahasiswa wajib mengisi angket di semester 1",
				"checklist_penilaian_dosen_semester_berlangsung", Konfigurasi.TIDAK_AKTIF, true));
		rows.appendChild(createRowActiveDefault(
				"Masukan/Saran/Komentar mahasiswa pada saat penilaian terhadap dosen harus diisi",
				"masukan_penialain_dosen_harus_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Petunjuk default angket penilaian dosen oleh mahasiswa",
				"keterangan_checklist_penilaian_dosen_oleh_mahasiswa",
				"Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap dosen Saudara. Informasi yang Saudara berikan hanya akan dipergunakan untuk peningkatan kualitas pembelajaran dan tidak akan berpengaruh terhadap status Saudara sebagai mahasiswa. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka pada kolom skor.",
				6, null));
		rows.appendChild(createRowNilai("Jumlah pilihan/skor angket penilaian dosen oleh mahasiswa",
				"jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5"));

		createSpan("Angket oleh Dosen", rows);
		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan dosen melakukan penilaian terhadap mata kuliah/perkuliahan yang diajar",
				"checklist_penilaian_oleh_dosen", Konfigurasi.TIDAK_AKTIF, true));

		createSpan("Angket Guru oleh Siswa", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan pemeriksaan angket penilaian guru oleh siswa saat siswa login atau masuk halaman utama",
				"checklist_penilaian_guru_oleh_siswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Input angket penilaian guru harus berdasarkan kalender akademik",
				"input_angket_penilaian_guru_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan siswa melakukan penilaian terhadap guru-gurunya di semester selanjutnya",
				"checklist_penilaian_guru", Konfigurasi.TIDAK_AKTIF, true));
		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan siswa melakukan penilaian terhadap guru-gurunya di semester berlangsung",
				"checklist_penilaian_guru_semester_berlangsung", Konfigurasi.TIDAK_AKTIF, true));
		rows.appendChild(createRowActiveDefault("Masukan/Saran/Komentar siswa pada saat penilaian guru harus diisi",
				"masukan_penilaian_guru_harus_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Petunjuk default angket penilaian guru oleh siswa",
				"keterangan_checklist_penilaian_guru_oleh_siswa",
				"Sesuai dengan yang Ananda ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap guru. Informasi yang diberikan digunakan untuk peningkatan kualitas pembelajaran. Penilaian dilakukan dengan memilih angka pada kolom skor.",
				6, null));
		rows.appendChild(createRowNilai("Jumlah pilihan/skor angket penilaian guru oleh siswa",
				"jumlah_pilihan_checklist_penilaian_guru_oleh_siswa", "5"));

		createSpan("Angket Umum untuk Mahasiswa, Dosen, Guru, Siswa, dan Pengguna Umum", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan validasi jadwal angket umum untuk mahasiswa, dosen, guru, siswa, orang tua, admin, dan pengguna umum",
				"angket_saat_login_diaktifkan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Catatan internal konfigurasi angket umum",
				"catatan_konfigurasi_angket_umum",
				"Pengaturan jadwal, grup, pertanyaan, dan parameter tambahan angket umum tetap dikelola melalui menu Angket/Checklist Penilaian Umum. Tab ini hanya mengumpulkan konfigurasi aktivasi dan perilaku angket agar mudah ditemukan.",
				4, null));
	}

	@SuppressWarnings("unchecked")
	protected void initTabAktifitasUmum() {
		Rows rows = null;
		rows = (createSpan("Aktifitas Umum"));

		createSpan("Pencatatan Error Sistem", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan pencatatan error otomatis ke database Error Log. Default aktif agar gangguan Java, ZKoss, servlet, dan proses background dapat ditelusuri dari menu Error Log.",
				"global_error_log_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Simpan error ke tabel ErrorLog. Pengaturan ini menjadi pusat audit bug sehingga tidak perlu membuat file error fisik di server.",
				"global_error_log_simpan_database", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tulis ringkasan error juga ke log server Tomcat. Ini membantu jika database sedang bermasalah sehingga error tetap terlihat di log aplikasi.",
				"global_error_log_server_log", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Simpan informasi alamat halaman, URL, IP pengguna, dan browser pada catatan error agar penyebab gangguan lebih mudah dicari.",
				"global_error_log_request_detail", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Batas maksimal panjang catatan error yang disimpan ke database",
				"global_error_log_max_content_length", "250000", 1, null));
		rows.appendChild(createRowNilai(
				"Catatan pencatatan error sistem",
				"catatan_global_error_log",
				"Semua error yang tertangkap otomatis disimpan di database Error Log. Dasbor Error Log menampilkan tren harian, jenis masalah, sumber error, jam rawan, dan peta risiko agar tim lebih mudah menentukan prioritas perbaikan.",
				4, null));

//		rows.appendChild(createRowNilai(
//				"Nomor Whatsapp \"Help Desk\" yang bisa dihubungi, kasih tanda koma (,) jika nomor WA lebih dari satu. Kosongkan jika tidak ada help desk",
//				"no_whatsapp_customer_service", "0811111111111111", 3, null, null));
//
//		rows.appendChild(createRowNilai("Tanya Whatsapp \"Help Desk\"", "tanya_whatsapp_customer_service",
//				"\"Help Desk\" Salamat Datang, apa yang bisa kami bantu?"));
//
//		rows.appendChild(createRowNilai("Jawab Whatsapp \"Help Desk\"", "jawab_whatsapp_customer_service",
//				"Saya ingin menanyakan tentang informasi .....(isi), apakah Anda bisa membantu?"));

		final Combobox passwordKuat = createComboActive();
		passwordKuat.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.passwordKuat = passwordKuat.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.passwordKuat => " + ConstantValues.passwordKuat);
			}
		});

		rows.appendChild(createRowNotActive(
				"Apakah pengguna diwajibkan saat login mengunakan password dengan syarat :  (1). Password harus minimal 8 karakter. (2). Password harus terdiri dari kombinasi huruf, angka, dan karakter spesial (misal ! @ # ) ?",
				"password_kuat", passwordKuat));

		final Combobox satuperangkat = createComboActive();
		satuperangkat.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.satuperangkat = satuperangkat.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.satuperangkat => " + ConstantValues.satuperangkat);
			}
		});

		rows.appendChild(createRowActive("Apakah pengguna hanya bisa login di satu perangkat ?", "satuperangkat",
				satuperangkat));

		final Combobox satuperangkat_mahasiswa = createComboActive();
		satuperangkat_mahasiswa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.satuperangkat_mahasiswa = satuperangkat_mahasiswa.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out
						.println("ConstantValues.satuperangkat_mahasiswa => " + ConstantValues.satuperangkat_mahasiswa);
			}
		});

		rows.appendChild(createRowActive("Apakah pengguna hanya bisa login di satu perangkat untuk mahasiswa/siswa ?",
				"satuperangkat_mahasiswa", satuperangkat_mahasiswa));

		final Combobox satuperangkatipygbeda = createComboActive();
		satuperangkatipygbeda.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.satuperangkatipygbeda = satuperangkatipygbeda.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.satuperangkatipygbeda => " + ConstantValues.satuperangkatipygbeda);
			}
		});

		rows.appendChild(createRowActive("Apakah pengguna hanya bisa login di satu perangkat dengan IP yang sama ?",
				"satuperangkatipygbeda", satuperangkatipygbeda));

		final Combobox aktifkanApakahJumlahLoginDibatasi = createComboActive();
		aktifkanApakahJumlahLoginDibatasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanApakahJumlahLoginDibatasi = aktifkanApakahJumlahLoginDibatasi.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanApakahJumlahLoginDibatasi => "
						+ ConstantValues.aktifkanApakahJumlahLoginDibatasi);
			}
		});

		rows.appendChild(createRowActive("Apakah jumlah mahasiswa yang login ke sistem dibatasi?",
				"apakah_jumlah_login_dibatasi", aktifkanApakahJumlahLoginDibatasi));

		rows.appendChild(createRowNilai(
				"Jika jumlah mahasiswa yang login ke sistem dibatasi, berapa jumlah maksimal mahasiswa yang diperbolehkan login ?",
				"nilai_jumlah_login_dibatasi", ConstantValues.nilaiJumlahLoginDibatasi + "", 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.nilaiJumlahLoginDibatasi = Integer
								.parseInt(((Textbox) arg0.getTarget()).getValue().trim());
					}
				}));
		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Tahun angkatan mahasiswa yang tidak bisa login ke sistem", "mahasiswa_tidak_bisa_login", "", 1, null));

		final Combobox aktifkanApakahJumlahLoginDosenDibatasi = createComboActive();
		aktifkanApakahJumlahLoginDosenDibatasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanApakahJumlahLoginDosenDibatasi = aktifkanApakahJumlahLoginDosenDibatasi
						.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanApakahJumlahLoginDosenDibatasi => "
						+ ConstantValues.aktifkanApakahJumlahLoginDosenDibatasi);
			}
		});

		rows.appendChild(createRowActive("Apakah jumlah dosen yang login ke sistem dibatasi?",
				"apakah_jumlah_login_dosen_dibatasi", aktifkanApakahJumlahLoginDosenDibatasi));

		rows.appendChild(createRowNilai(
				"Jika jumlah dosen yang login ke sistem dibatasi, berapa jumlah maksimal dosen yang diperbolehkan login ?",
				"nilai_jumlah_dosen_dibatasi", ConstantValues.nilaiJumlahLoginDosenDibatasi + "", 1,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.nilaiJumlahLoginDosenDibatasi = Integer
								.parseInt(((Textbox) arg0.getTarget()).getValue().trim());
					}
				}));

		rows.appendChild(createRowNilai("Lama waktu otomatis logout bagi mahasiswa (satuan menit)",
				"session_timeout_mahasiswa", "10"));

		final Combobox ketikaUbahDataPenggunaKirimke = createComboActive();
		ketikaUbahDataPenggunaKirimke.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ketikaUbahDataPenggunaKirimke = ketikaUbahDataPenggunaKirimke.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.ketikaUbahDataPenggunaKirimke => "
						+ ConstantValues.ketikaUbahDataPenggunaKirimke);
			}
		});

		rows.appendChild(createRowActive("Apakah ketika ubah data pengguna kirim kan infoke ?",
				"ketikaUbahDataPenggunaKirimke", ketikaUbahDataPenggunaKirimke));

		rows.appendChild(
				createRowNilai("Alamat / link ketika ubah data pengguna kirim", "ketikaUbahDataPenggunaKirimkeLink",
						ConstantValues.ketikaUbahDataPenggunaKirimkeLink, 1, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ConstantValues.ketikaUbahDataPenggunaKirimkeLink = ((Textbox) arg0.getTarget())
										.getValue().trim();
							}
						}));

		final Combobox ketikaUbahSemuaDataKirimke = createComboActive();
		ketikaUbahSemuaDataKirimke.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ketikaUbahSemuaDataKirimke = ketikaUbahSemuaDataKirimke.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println(
						"ConstantValues.ketikaUbahSemuaDataKirimke => " + ConstantValues.ketikaUbahSemuaDataKirimke);
			}
		});

		rows.appendChild(createRowActive("Apakah ketika ubah semua data kirim kan infoke ?",
				"ketikaUbahSemuaDataKirimke", ketikaUbahSemuaDataKirimke));

		rows.appendChild(createRowNilai("Alamat / link ketika ubah semua data kirim", "ketikaUbahSemuaDataKirimkeLink",
				ConstantValues.ketikaUbahSemuaDataKirimkeLink, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.ketikaUbahSemuaDataKirimkeLink = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		rows.appendChild(
				createRowNilai("Lama waktu otomatis logout bagi dosen (satuan menit)", "session_timeout_dosen", "60"));
		rows.appendChild(
				createRowNilai("Lama waktu otomatis logout bagi admin (satuan menit)", "session_timeout_admin", "120"));

		rows.appendChild(createRowActive("Sistem ini tidak mengizinkan dibuka di dua tab browser atau refresh browser",
				"apakah_jumlah_tab_dan_login_dibatasi"));

		rows.appendChild(createRowNilai("Alamat utama atau home", "CURRENT_URL", Common.CURRENT_URL_TEMP, 1,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.CURRENT_URL_TEMP = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		final Combobox aktifkanApakahMenggunakanLabelBahasa = createComboActive();
		aktifkanApakahMenggunakanLabelBahasa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.penggunaanLabelBahasa = aktifkanApakahMenggunakanLabelBahasa.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.penggunaanLabelBahasa => " + ConstantValues.penggunaanLabelBahasa);
			}
		});

		rows.appendChild(createRowActive("Apakah boleh skip password jika belum diganti ?",
				"boleh_skip_password_jika_belum_diganti"));

		rows.appendChild(createRowActive("Apakah menggunakan label bahasa ?", "apakah_menggunakan_label_bahasa",
				aktifkanApakahMenggunakanLabelBahasa));

		rows.appendChild(createRowActive("Aktifkan absensi pegawai harian menggunakan foto",
				"aktifkan_absensi_pegawai_menggunakan_foto"));

		rows.appendChild(
				createRowActive("Aktifkan Live Streaming di setiap pertemuan perkuliahan", "aktifkan_live_streaming"));

		rows.appendChild(createRowNotActive(
				"Tampilkan tombol \"Ajukan Tiket\" mengambang (Ticketing) di kanan-bawah halaman",
				ais.action.master.ticket.TicketKonfigurasiAction.KEY_FAB));

		// rows.appendChild(createRowActive("Aktifkan Chat", "chat_enabled"));

		rows.appendChild(createRowActive("Mahasiswa harus melengkapi biodatanya sebelum bisa menggunakan menu lain-nya",
				"apakah_mahasiswa_harus_melengkapi_biodata_nya"));

		rows.appendChild(createRowActive("Mahasiswa boleh mengganti foto profile sendiri",
				"mahasiswa_boleh_mengubah_foto_profile"));

		rows.appendChild(createRowActive("Mahasiswa boleh mengganti tempat lahir, tanggal lahir, dan nama ibu sendiri",
				"mahasiswa_boleh_mengubah_ttl_dan_ibu_profile"));

		rows.appendChild(createRowActiveDefault("Mahasiswa bisa atau boleh memilih dosen pembimbing akademik sendiri",
				"mahasiswa_bisa_memilih_dosen_pa_sendiri", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Mahasiswa tidak perlu upload ijazah (pendidikan sebelumnya)",
				"mahasiswa_tidak_perlu_upload_ijazah", createComboActive(true)));

		rows.appendChild(createRowActive("Mahasiswa tidak perlu upload transkrip nilai (pendidikan sebelumnya)",
				"mahasiswa_tidak_perlu_upload_nilai", createComboActive(true)));

		rows.appendChild(createRowActive("Mahasiswa tidak perlu upload ktp / kartu pelajar (pendidikan sebelumnya)",
				"mahasiswa_tidak_perlu_upload_ktp", createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-1", "mahasiswa_upload_lampiran_1",
				"Upload lampiran lain ke-1", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-2", "mahasiswa_upload_lampiran_2",
				"Upload lampiran lain ke-2", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-3", "mahasiswa_upload_lampiran_3",
				"Upload lampiran lain ke-3", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-4", "mahasiswa_upload_lampiran_4",
				"Upload lampiran lain ke-4", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-5", "mahasiswa_upload_lampiran_5",
				"Upload lampiran lain ke-5", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-6", "mahasiswa_upload_lampiran_6",
				"Upload lampiran lain ke-6", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-7", "mahasiswa_upload_lampiran_7",
				"Upload lampiran lain ke-7", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-8", "mahasiswa_upload_lampiran_8",
				"Upload lampiran lain ke-8", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-9", "mahasiswa_upload_lampiran_9",
				"Upload lampiran lain ke-9", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-10", "mahasiswa_upload_lampiran_10",
				"Upload lampiran lain ke-10", Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		rows.appendChild(createRowActive("Dosen harus melengkapi biodatanya sebelum bisa menggunakan menu lain-nya",
				"apakah_dosen_harus_melengkapi_biodata_nya"));

		rows.appendChild(
				createRowActive("Dosen boleh mengganti foto profile sendiri", "dosen_boleh_mengubah_foto_profile"));

		rows.appendChild(createRowActive("Admin harus melengkapi email jika belum diisi",
				"admin_harus_melengkapi_email_jika_belum_diisi"));
		rows.appendChild(createRowActive("Dosen harus melengkapi email jika belum diisi",
				"dosen_harus_melengkapi_email_jika_belum_diisi"));
		rows.appendChild(createRowActive("Mahasiswa harus melengkapi email jika belum diisi",
				"mahasiswa_harus_melengkapi_email_jika_belum_diisi"));

		rows.appendChild(createRowNilai("Filter data yang tidak boleh ada di catatan, komentar, dan pengumuman",
				"filter_tidak_boleh_ada", ConstantValues.filter_tidak_boleh_ada, 5, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						ConstantValues.filter_tidak_boleh_ada = Common
								.getKonfigurasi("filter_tidak_boleh_ada", ConstantValues.filter_tidak_boleh_ada)
								.getNilai();
					}
				}));

//		rows.appendChild(createRowNilai("Kata kata yang di blokir dalam request ke server",
//				"kata_kata_yang_di_blokir_dalam_request_ke_server", FilterJSP.NEED_TO_BLOCKED, 2, new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						FilterJSP.NEED_TO_BLOCKED = ((Textbox) arg0.getTarget()).getValue().trim();
//					}
//				}));

		rows.appendChild(createRowActive("Tampilkan upload RPS di agenda perkuliahan", "tampilkan_rps"));

		rows.appendChild(createRowActive("Tampilkan upload SAP di agenda perkuliahan", "tampilkan_sap"));

		rows.appendChild(
				createRowActive("Tampilkan upload Absen Manual di agenda perkuliahan", "tampilkan_absen_manual"));

		rows.appendChild(createRowActiveDefault(
				"Dosen boleh mengubah absen perkuliahan \"Pertemuan dan absensi harus sesuai dengan jadwal yang telah ditentukan\"",
				"absen_tanpa_batas_waktu", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan dosen harus absen sesuai jadwal yang ditentukan",
				"absen_harus_sesuai_waktu", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Jumlah hari yang ditentukan untuk dosen harus absen sesuai jadwal menggunakan default",
				"jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Jumlah hari default untuk dosen harus absen sesuai jadwal",
				"jumlah_hari_batas_waktu_dalam_hari", "0"));

		rows.appendChild(createRowActive("Tampilkan upload Soal UTS di agenda perkuliahan", "tampilkan_soal_uts"));

		rows.appendChild(createRowActive("Tampilkan upload Soal UAS di agenda perkuliahan", "tampilkan_soal_uas"));

		rows.appendChild(createRowNilai("Lampiran lain di agenda perkuliahan (jika banyak pisah dengan koma)",
				"tampilkan_lampiran_lain_di_agenda", ""));

		rows.appendChild(
				createRowActive("Tampilkan upload langsung (tanpa menggunakan drive)", "boleh_upload_file_langsung"));

		rows.appendChild(
				createRowNilai("Alamat email untuk monitoring email, pisahkan dengan koma jika lebih dari satu",
						"alamat_email_monitoring", ""));

		rows.appendChild(createRowActiveDefault("Admin bisa membuka kunci penilaian walaupun sudah terkunci",
				"kunci_nilai_untuk_admin", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Admin yang boleh mengkunci nilai, jika kosong artinya boleh semua mengkunci nilai, kasih tanda koma jika lebih dari satu",
				"admin_yg_boleh_kunci_nilai", ""));

		rows.appendChild(createRowActive("Tampilkan info asesor pada menu BKD", "tampilkan_asesor"));

		rows.appendChild(createRowActive("Tampilkan pilihan \"nilai 0 tidak masuk penghitungan nilai akhir\"",
				"tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir"));
		rows.appendChild(createRowActive("Tampilkan jika ada \"jika ada nilai 0 tidak menghitung nilai akhir\"",
				"tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir"));
		rows.appendChild(
				createRowActiveDefault("Nilai default BENAR jika ada \"jika ada nilai 0 tidak menghitung nilai akhir\"",
						"jika_ada_nilai_0_tidak_menghitung_nilai_akhir", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowActiveTahunAkademikSemester("Aktivasi penilaian oleh dosen", Konfigurasi.PENILAIAN, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi penilaian semester pendek oleh dosen",
				Konfigurasi.PENILAIAN_SP, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi persetujuan KRS oleh dosen",
				"aktivasi_persetujuan_KRS_oleh_dosen", false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi persetujuan KRS SP oleh dosen",
				"aktivasi_persetujuan_KRS_sp_oleh_dosen", false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi pembuatan jadwal perkuliahan",
				Konfigurasi.PENJADWALAN, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi pembuatan jadwal perkuliahan semester pendek",
				Konfigurasi.PENJADWALAN_SP, false));

		rows.appendChild(createRowActive("Jadwal perkuliahan tidak bisa diubah ketika diedit",
				"jadwal_perkuliahan_tidak_bisa_diubah_ketika_diedit"));

		rows.appendChild(createRowActive("Secara default dosen bisa mengubah tanggal perkuliahan",
				"secara_default_dosen_bisa_merubah_tanggal_perkuliahan"));

		rows.appendChild(createRowActive("Komentar perkuliahan tampil langsung di halaman utama elearning",
				"komentar_tampil_di_halaman_utama_elearning"));

		rows.appendChild(createRowActive("Tampilkan info kehadiran pengajar di halaman utama",
				"tampilkan_info_kehadiran_pengajar_di_halaman_utama"));

		rows.appendChild(createRowActive(
				"Tanggal realisasi perkuliahan harus diisi sesuai dengan tanggal mengajar dosen dengan mahasiswa",
				"tanggal_realisasi_perkuliahan_harus_diisi_sesuai_pertemuan_perkuliahan"));

		rows.appendChild(
				createRowActiveDefault("Terdapat proses verifikasi penilaian kepada dosen yang dilakukan oleh admin",
						"ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan honor koreksi yang telah diverifikasi",
				"tampilkan_honor_koreksi_yang_telah_diverifikasi", Konfigurasi.TIDAK_AKTIF));

		final Combobox jenisUjian = new Combobox();
		String[] data = new String[] { "UTS", "UAS" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			jenisUjian.appendChild(comboitem);
		}
		jenisUjian.setReadonly(true);

		MyComboitemConfig semuaUjian = new MyComboitemConfig("Semua Ujian");
		semuaUjian.setValue("");
		jenisUjian.appendChild(semuaUjian);
		jenisUjian.setSelectedItem(semuaUjian);

		rows.appendChild(createRowNilaiProgramDanJurusan("Default nilai honor koreksi", "default_nilai_honor_koreksi",
				"0", 1, null, jenisUjian));

		// rows.appendChild(createRowNilai("Ikuran maksimal file upload",
		// "ukuran_maksimal_file_diupload", "1024"));

		// rows.appendChild(createRowNilai("Lokasi file Temporary",
		// "lokasi_file_temporary_data",
		// ConstantValues.lokasiFileTemprorary, 1, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// ConstantValues.lokasiFileTemprorary = ((Textbox)
		// arg0.getTarget()).getValue().trim();
//		ConstantValues.panjangLokasiFileTemprorary = ConstantValues.lokasiFileTemprorary.length();
		// }
		// }));

		rows.appendChild(createRowActive(
				"Pengguna boleh meng-uplaod file langsung ke sistem, jika tidak diaktikan, maka hanya boleh menguplaod via media (Google Drive atau Dropbox)",
				"boleh_upload_file_langsung"));

		rows.appendChild(createRowActive("Aktifkan video conference menggunakan Jitsi", "aktifkan_video_conference"));

		rows.appendChild(createRowNilai("Alamat server Jitsi video conference", "alamat_server_video_conference",
				"https://meet.jit.si", 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}));

		// Toggle tampilkan_singkron_kalendar_di_elearning dan
		// tampilkan_kalendar_di_elearning dipindahkan ke tab Elearning
		// (appendElearningToggleRows) agar semua pengaturan e-Learning terkumpul.

//		final Combobox harus_https;
//		rows.appendChild(createRowActiveDefault(
//				"Koneksi harus menggunakan https, jika belum https, secara otomatis akan di arahkan ke https",
//				"harus_https", Konfigurasi.TIDAK_AKTIF, harus_https = createComboActive()));
//		harus_https.addEventListener("onChange", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				DashboardTimelinePertemuan.harus_https = harus_https.getSelectedItem().getValue()
//						.equals(Konfigurasi.AKTIF);
//
//			}
//		});

		rows.appendChild(createRowNilai("Jumlah digit angka dibelakang koma pada tampilan angka pecaham",
				"JUMLAH_DIGIT_DIBELAKANG_KOMA", ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA.toString(), 1,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA = Integer
									.parseInt(Common.getKonfigurasi("JUMLAH_DIGIT_DIBELAKANG_KOMA",
											ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA.toString()).getNilai());
							Common.numberFormat.get().setMaximumFractionDigits(ConstantValues.JUMLAH_DIGIT_DIBELAKANG_KOMA);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:3232");
							// TODO: handle exception
						}
					}
				}));

		rows.appendChild(createRowActive("Dosen dapat menambah format penilaian sendiri",
				"dosen_dapat_menambah_format_penilaian_sendiri"));

		rows.appendChild(createRowActive("Aktifkan upload nilai pada menu download dan upload nilai",
				"aktifkan_upload_nilai_pada_menu_download_dan_upload_nilai"));

		rows.appendChild(createRowActive("Aktifkan upload nilai pada menu download dan upload nilai konversi",
				"aktifkan_upload_nilai_pada_menu_download_dan_upload_nilai_konversi"));

		rows.appendChild(createRowNilai("Kode role admin penerima email saat dosen mengirim komentar",
				"kode_role_penerima_email_saat_dosen_mengirim_komentar", "am"));

		rows.appendChild(createRowActive("Tampilkan informasi jadwal kegiatan di halaman depan",
				"tampilkan_informasi_jadwal_kegiatan_di_halaman_depan"));

		rows.appendChild(createRowNilai(
				"Kode role selain super admin yang bisa mengubah informasi jadwal kegiatan di halaman depan, pisahkan dengan tanda ; jika lebih dari satu",
				"kode_role_informasi_jadwal_kegiatan_di_halaman_depan", ""));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi pengambilan KRS oleh mahasiswa",
				Konfigurasi.KRS, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi pengambilan KRS semester pendek oleh mahasiswa",
				Konfigurasi.KRS_SP, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi pengambilan KRS remedial oleh mahasiswa",
				Konfigurasi.KRS_REMEDIAL, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi perbaikan KRS oleh mahasiswa",
				Konfigurasi.PERBAIKAN_KRS, Konfigurasi.TIDAK_AKTIF, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi perbaikan KRS semester pendek oleh mahasiswa",
				Konfigurasi.PERBAIKAN_KRS_SP, Konfigurasi.TIDAK_AKTIF, false));

		rows.appendChild(createRowActiveTahunAkademikSemester("Aktivasi perbaikan KRS remedial oleh mahasiswa",
				Konfigurasi.PERBAIKAN_KRS_REMEDIAL, Konfigurasi.TIDAK_AKTIF, false));

		rows.appendChild(createRowActive("Input KRS yang dilakukan oleh mahasiswa harus berdasarkan kalender akademik",
				"input_krs_harus_berdasarkan_kalender_akademik"));

		rows.appendChild(createRowActive("Saat ambil krs boleh mengambil dari prodi lain",
				"saat_ambil_krs_boleh_mengambil_dari_prodi_lain"));

		rows.appendChild(createRowActive("Dosen pembimbing akademik / admin bisa mengambilkan KRS mahasiswa",
				"dosen_pa_boleh_mengambilkan_krs_mahasiswa"));

		rows.appendChild(createRowActiveDefault(
				"Dosen pembimbing akademik / admin bisa mengambilkan KRS mahasiswa diluar jadwal masa pengambilan KRS",
				"dosen_pa_boleh_mengambilkan_krs_mahasiswa_walaupn_diluar_jadwal", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Matakuliah Konversi masuk akumulasi jumlah sks pengambilan krs",
				"konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs"));

		rows.appendChild(createRowActiveDefault("Aktifkan tombol reset ulang ujian di menu ujian online",
				"aktifkan_reset_ulang_ujian_di_menu_ujian_online", Konfigurasi.TIDAK_AKTIF));

		Combobox combo = new Combobox();
		String[] dataCombo = new String[] { "Online Dosen dan Mahasiswa", "Online Mahasiswa 15%",
				"Online Mahasiswa 25%", "Online Mahasiswa 30%", "Online Mahasiswa 50%", "Online Mahasiswa 60%",
				"Online Mahasiswa 75%" };
		for (String d : dataCombo) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			combo.appendChild(comboitem);
		}
		combo.setReadonly(true);
		rows.appendChild(createRowActiveWithDefault("Perhitungan Rekap Online dihitung berdasarkan",
				"perhitungan_rekap_online_dihitung_berdasarkan", "", "Online Dosen dan Mahasiswa", combo));

		rows.appendChild(
				createRowActiveDefault("Saat pengambilan krs oleh mahasiswa, tidak diperbolehkan ada-nya jam bentrok",
						"saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowActiveDefault("Saat persetujuan krs oleh dosen, tidak diperbolehkan ada-nya jam bentrok",
						"saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok_dosen", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Masa perkuliahan hanya boleh diubah oleh admin",
				"masa_perkuliahan_hanya_boleh_diubah_oleh_admin", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Pada saat mahasiswa mengambil KRS paket, jika jadwal belum dibuat, secara otomatis akan membuat jadwal dengan waktu, ruang, dan dosen yang kosong",
				"untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_otomatis_membuat_jadwal_dengan_waktu_ruang_dosen_yang_kosong",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Saat cetak krs harus telah disetujui",
				"saat_cetak_krs_harus_telah_disetujui", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Tampilkan informasi dosen saat mahasiswa mengambil KRS",
				"tampilkan_dosen_saat_ambil_krs"));

		rows.appendChild(
				createRowActiveDefault("Aktifkan angket umum", "angket_saat_login_diaktifkan", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Input angket penilaian dosen harus berdasarkan kalender akademik",
				"input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester selanjutnya. Misal: jika sekarang semester 2, maka mahasiswa wajib mengisi angket di semester 1",
				"checklist_penilaian_dosen", true));

		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan mahasiswa melakukan penilaian terhadap dosen-dosen-nya di semester berlangsung. Misal: jika sekarang semester 1, maka mahasiswa wajib mengisi angket di semester 1",
				"checklist_penilaian_dosen_semester_berlangsung", Konfigurasi.TIDAK_AKTIF, true));

		rows.appendChild(createRowActiveTahunAkademikSemester(
				"Aktivasi keharusan dosen melakukan penilaian terhadap matakuliah yang diajar-nya",
				"checklist_penilaian_oleh_dosen", Konfigurasi.TIDAK_AKTIF, true));

		rows.appendChild(createRowActiveDefault(
				"Masukan/Saran/Komentar mahasiswa pada saat penialain terhadap dosen harus diisi",
				"masukan_penialain_dosen_harus_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Maksimal jumlah SKS dosen mengajar dalam satu semester",
				"maksimal_dosen_mengajar_dalam_satu_semester", "50"));

		rows.appendChild(createRowNilai("Maksimal jumlah perkuliahan dosen mengajar dalam satu semester",
				"maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester", "50"));

		rows.appendChild(createRowNilai("Maksimal jumlah bimbingan skripsi dosen dalam satu semester",
				"maksimal_bimbingan_dosen_mengajar_dalam_satu_semester", "50"));

		rows.appendChild(createRowNilai("Maksimal jumlah penguji sidang dosen dalam satu semester",
				"maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester", "50"));

		rows.appendChild(createRowNilai("Default kapasitas perkuliahan", "default_kapasitas_perkuliahan", "30"));

		// rows.appendChild(createRowActiveDefault("Pada komentar e-learning
		// bisa menyisipkan lampiran",
		// "pada_komentar_e_learning_bisa_menyisipkan_lampiran",
		// Konfigurasi.AKTIF));

		rows.appendChild(createRowActive(
				"Pembatasan maksimal sks pada pegambilan krs berdasarkan ip semester sebelum nya, jika tidak aktif, akan mengambil dari IPK",
				"pembatasan_maksimal_sks_pada_pegambilan_krs_berdasarkan_ip_semester_sebelum_nya"));

		rows.appendChild(createRowActiveDefault("Tampilkan menu pintas di halaman utama",
				"tampilkan_menu_pintas_di_halaman_utama", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Default jumlah maksimal SKS untuk pembatasan pengambilan KRS / atau semester 1 yang belum memiliki nilai IP",
				"default_pembatasan_nilai_ip_untuk_ambil_KRS", "24"));

		rows.appendChild(createRowNilai("Minimal semester berlaku syarat minimal pengambilan krs",
				"minimal_smt_syarat_krs", "2"));

		rows.appendChild(createRowActive("Tampilkan tombol download persetujuan KRS",
				"tampilkan_tombol_download_persetujuan_krs"));
		rows.appendChild(createRowActive("Tampilkan tombol download persetujuan KRS untuk login sebagai dosen",
				"tampilkan_tombol_upload_persetujuan_krs_di_dosen"));

		rows.appendChild(createRowActive("Tampilkan pilihan konsentrasi", "tampil_konsentrasi_mahasiswa"));

		rows.appendChild(
				createRowActive("Tampilkan tombol upload persetujuan KRS", "tampilkan_tombol_upload_persetujuan_krs"));

		rows.appendChild(
				createRowActive("Tampilkan tombol bersihkan KRS yang double", "tampilkan_tombol_bersihkan_krs_double"));

		rows.appendChild(createRowActiveDefault(
				"Admin (jenis pengguna admin) bisa menghapus langsung data nila mahasiswa di menu krs",
				"admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Admin lain bisa menghapus langsung data nila mahasiswa di menu krs",
				"admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", ""));

		rows.appendChild(createRowNilai("Admin lain bisa mengubah status mahasiswa",
				"admin_lain_bisa_mengubah_status_mahasiswa", ""));

		rows.appendChild(createRowNilai("Admin lain bisa menambah pengecualian penilaian",
				"admin_lain_bisa_menambah_pengecualian_penilaian", ""));

		rows.appendChild(
				createRowNilai("Admin lain bisa membatalkan posting", "admin_lain_bisa_membatalkan_posting", "keu"));

		rows.appendChild(createRowActive(
				"Untuk membatalkan persetujuan suatu perkuliahan yang diikuti mahasiswa, nilai mahasiswa di perkuliahan tersebut harus nol",
				"batalkan_persetujuan_harus_memiliki_nilai_nol"));

		rows.appendChild(createRowActiveDefault(
				"Mahasiswa boleh diambilkan suatu matakuliah konversi tertentu lebih dari satu kali",
				"boleh_ambil_matakuliah_konversi_lebih_dari_satu_kali", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Boleh upload data mahasiswa", "aktifkan_upload_data_mahasiswa"));

		rows.appendChild(createRowActive("Boleh upload data kelas", "boleh_upload_data_kelas"));

		rows.appendChild(createRowActive("Boleh upload data asrama", "boleh_upload_data_asrama"));

		rows.appendChild(createRowActive("Kode matakuliah di dalam satu prodi tidak diperbolehkan sama",
				"matakuliah_kode_jurusan_gak_blh_sama"));
		rows.appendChild(createRowActive("Kode matakuliah tidak diperbolehkan sama", "matakuliah_kode_gak_blh_sama"));

		rows.appendChild(createRowNilai("Default Status Kehadiran (M=Masuk, A=Alpa, S=Sakit, I=Ijin, - = Belum Absen)",
				"default_status_kehadiran", ConstantValues.BELUM_ABSEN.getKode()));

		final Combobox kehadiranHarusMulaiDanSampai = createComboActive();
		kehadiranHarusMulaiDanSampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.kehadiranHarusMulaiDanSampai = kehadiranHarusMulaiDanSampai.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.kehadiranHarusMulaiDanSampai => "
						+ ConstantValues.kehadiranHarusMulaiDanSampai);
			}
		});

		rows.appendChild(createRowActiveDefault(
				"Absensi kehadiran dihitung harus dengan kondisi absen kedatangan dan kepulangan",
				"kehadiran_harus_mulai_dan_sampai", Konfigurasi.TIDAK_AKTIF, kehadiranHarusMulaiDanSampai));

		final Combobox absenDosenCombo = createComboActive();
		absenDosenCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT = absenDosenCombo.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT => "
						+ ConstantValues.ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT);
			}
		});

		rows.appendChild(
				createRowActiveDefault("Absensi kehadiran dosen secara otomatis terintegrasi dengan finger print",
						"ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT", Konfigurasi.AKTIF, absenDosenCombo));

		final Combobox absenGuruCombo = createComboActive();
		absenGuruCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT = absenGuruCombo.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT => "
						+ ConstantValues.ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT);
			}
		});

		rows.appendChild(
				createRowActiveDefault("Absensi kehadiran guru secara otomatis terintegrasi dengan finger print",
						"ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT", Konfigurasi.AKTIF, absenGuruCombo));

		final Combobox absenMahasiswaCombo = createComboActive();
		absenMahasiswaCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = absenMahasiswaCombo.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT => "
						+ ConstantValues.ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT);
			}
		});

		rows.appendChild(
				createRowActiveDefault("Absensi kehadiran mahasiswa secara otomatis terintegrasi dengan finger print",
						"ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT", Konfigurasi.AKTIF, absenMahasiswaCombo));

		final Combobox absenSiswaCombo = createComboActive();
		absenSiswaCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT = absenSiswaCombo.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT => "
						+ ConstantValues.ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT);
			}
		});

		rows.appendChild(
				createRowActiveDefault("Absensi kehadiran siswa secara otomatis terintegrasi dengan finger print",
						"ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT", Konfigurasi.AKTIF, absenSiswaCombo));

		rows.appendChild(createRowActive("Tampilkan pilihan waktu perkuliahan PAGI, SIANG, SORE, MALAM",
				"waktu_perkuliahan_pagi_siang_sore_malam_ditampilkan"));

		rows.appendChild(createRowActive("Tampilkan pilihan minggu 1,2,3,4 dan 5 pada saat penjadwalan perkuliahan",
				"tampilkan_minggu_perkuliahan"));
		rows.appendChild(
				createRowActive("Tampilkan abaikan jam bentrok", "tampilkan_abaikan_waktu_bentrok_dengan_jadwal_lain"));

		final Combobox aktifkanTahapanKurikulumCombo = createComboActive();
		aktifkanTahapanKurikulumCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanTahapanKurikulum = aktifkanTahapanKurikulumCombo.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				ConstantValues.initJumlahTahapan();
				System.out.println(
						"ConstantValues.aktifkanTahapanKurikulum => " + ConstantValues.aktifkanTahapanKurikulum);
			}
		});
		rows.appendChild(createRowActiveDefault(
				"Aktfikan tahap di kurikulum, misalnya dalam satu tahun akademik ada 3 atau 4 tahapan di kurikulum",
				"aktifkan_tahapan_kurikulum_dalam_satu_tahun_akademik", Konfigurasi.TIDAK_AKTIF,
				aktifkanTahapanKurikulumCombo));

		rows.appendChild(createRowActive("Kurikulum yang sudah dibuatkan jadwal perkuliahan tidak bisa dihapus",
				"kurikulum_yang_sudah_dijadwal_tidak_bisa_dihapus"));

		final Combobox aktifkanTahapanCombo = createComboActive();
		aktifkanTahapanCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanTahapan = aktifkanTahapanCombo.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				ConstantValues.initJumlahTahapan();
				System.out.println("ConstantValues.aktifkanTahapan => " + ConstantValues.aktifkanTahapan);
			}
		});
		rows.appendChild(createRowActiveDefault(
				"Aktfikan tahap perkuliahan, misalnya dalam satu tahun akademik ada 3 atau 4 tahapan perkuliahan",
				"aktifkan_tahapan_perkuliahan_dalam_satu_tahun_akademik", Konfigurasi.TIDAK_AKTIF,
				aktifkanTahapanCombo));

		final Combobox tahapan = new Combobox();
		data = new String[] { "2", "3", "4" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			tahapan.appendChild(comboitem);
		}
		tahapan.setReadonly(true);

		final Combobox programTahapan = Common.initPrograms(null);
		Common.selectComboItem(programTahapan, "Reguler");

		final Combobox jurusanTahapan = new Combobox();
		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusans = session.createCriteria(Jurusan.class).list();
		MyComboitemConfig mycomboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		mycomboitem.setDescription("Semua " + Common.getBahasaConfig("Jurusan"));
		mycomboitem.setValue("");
		jurusanTahapan.appendChild(mycomboitem);

		for (Jurusan d : jurusans) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d.getKode() + "-" + d.getNama());
			comboitem.setDescription(d.getFakultas().getNama());
			comboitem.setValue(d.getId());
			jurusanTahapan.appendChild(comboitem);
		}
		jurusanTahapan.setReadonly(true);
		Common.selectComboItem(jurusanTahapan, "");

		rows.appendChild(
				createRowActiveWithTreeCombo("Jumlah tahapan perkuliahan dalam satu tahun akademik jika diaktifkan",
						"jumlah_tahapan_perkuliahan_dalam_satu_tahun_akademik", "", "Reguler", "2", jurusanTahapan,
						programTahapan, tahapan, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ConstantValues.initJumlahTahapan();
							}
						}));

		final Combobox aktifkanTahapanTerhubungKeKeuanganCombo = createComboActive();
		aktifkanTahapanTerhubungKeKeuanganCombo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanTahapanTerhubungKeKeuangan = aktifkanTahapanTerhubungKeKeuanganCombo
						.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				ConstantValues.initJumlahTahapan();
				System.out.println("ConstantValues.aktifkanTahapanTerhubungKeKeuangan => "
						+ ConstantValues.aktifkanTahapanTerhubungKeKeuangan);
			}
		});
		rows.appendChild(createRowActiveDefault("Aktfikan tahap perkuliahan (2,3,4), terhubung ke bagian keuangan",
				"aktifkan_tahapan_perkuliahan_terhubung_kebagian_keuangan", Konfigurasi.TIDAK_AKTIF,
				aktifkanTahapanTerhubungKeKeuanganCombo));

		rows.appendChild(createRowActive("Mahasiswa harus membayar perkuliahan sebelum mengisi KRS",
				"mahasiswa_harus_bayar_sebelum_isi_krs"));

		rows.appendChild(
				createRowNilaiSemesterDanAngkatanDanJurusan("Batas terendah persen pembayaran boleh mengambil KRS",
						"batas_terendah_persen_pembayaran_boleh_ambil_krs", "", 1, null));

		rows.appendChild(createRowActiveDefault(
				"Mahasiswa baru (semester 1) harus mengikuti persyaratan pembayaran krs spt mahasiswa lama",
				"mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Kode item biaya yang harus dibayar sebelum mengisi mengisi KRS, jika lebih dari satu dipisah dengan ;",
				"kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs", "", 1, null));

		rows.appendChild(createRowActive("Mahasiswa harus membayar perkuliahan sebelum mengisi KRS SP",
				"mahasiswa_harus_bayar_sebelum_isi_krs_sp"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Kode item biaya yang harus dibayar sebelum mengisi mengisi KRS Semester Pendek (SP), jika lebih dari satu dipisah dengan ;",
				"kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs_sp", "", 1, null));

		rows.appendChild(createRowActive("Mahasiswa harus membayar perkuliahan sebelum KRS-nya bisa disetujui",
				"mahasiswa_harus_bayar_sebelum_persetujuan_krs"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen pembayaran sebelum KRS-nya bisa disetujui",
				"batas_terendah_persen_pembayaran_boleh_persetujuan_krs", "", 1, null));

		// rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
		// "Batas terendah persen pembayaran mahasiswa baru sebelum KRS-nya bisa
		// disetujui",
		// "batas_terendah_persen_pembayaran_boleh_persetujuan_krs_smt_1", "",
		// 1, null));

		rows.appendChild(createRowActive("Mahasiswa harus melunasi biaya semester yang lalu sebelum mengisi KRS",
				"mahasiswa_harus_lunas_semester_sebelumnya_sebelum_mengambil_krs"));

		rows.appendChild(createRowNilai("Batas terendah persen pembayaran semester yang lalu boleh mengisi KRS",
				"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90"));

		rows.appendChild(createRowActiveDefault(
				"Mahasiswa harus melunasi biaya semester yang lalu sebelum bisa disetuji KRS yang diambil",
				"mahasiswa_harus_lunas_semester_sebelumnya_sebelum_persetujuan_krs", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Batas terendah persen pembayaran semester yang lalu sebelum bisa disetuji KRS yang diambil",
				"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs", "90"));

		rows.appendChild(createRowActive("Mahasiswa harus melunasi biaya semester yang lalu sebelum melihat nilai",
				"mahasiswa_harus_lunas_semester_sebelumnya_sebelum_melihat_nilai"));

		rows.appendChild(createRowActive("Mahasiswa boleh melihat / mencetak khs sendiri",
				"mahasiswa_boleh_melihat_khs_sendiri"));

		rows.appendChild(createRowActive("Syarat melihat nilai tidak termasuk mahasiswa semester 1",
				"syarat_melihat_nilai_tidak_termasuk_smt_1"));

		rows.appendChild(
				createRowNilaiSemesterDanAngkatanDanJurusan("Kode item biaya yang harus dibayar sebelum melihat nilai",
						"kode_item_biaya_mahasiswa_harus_bayar_sebelum_melihat_nilai", "", 1, null));

		rows.appendChild(createRowNilai("Batas terendah persen pembayaran semester saat ini boleh melihat nilai",
				"batas_terendah_persen_pembayaran_semester_saat_ini_boleh_melihat_nilai", "0"));

		rows.appendChild(createRowNilai("Batas terendah persen pembayaran semester yang lalu boleh melihat nilai",
				"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_melihat_nilai", "90"));

		rows.appendChild(
				createRowActiveDefault("Mahasiswa harus membayar perkuliahan sebelum bisa diabsen secara online",
						"mahasiswa_yang_belum_membayar_tidak_bisa_absen_perkuliahan", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowActive("Dosen pembimbing akademik harus ada sebelum mahasiswa diperbolehkan mengisi KRS",
						"dosen_pa_harus_ada_sebelum_isi_krs"));

		rows.appendChild(createRowActiveDefault("Kelas harus ada sebelum mahasiswa diperbolehkan mengisi KRS",
				"kelas_harus_ada_sebelum_isi_krs", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowActiveDefault("Kelas terpilih otomatis dan tidak bisa diubah ketika mahasiswa mengisi KRS",
						"Pada_saat_mengambil_KRS_otomatis_kelas_terisi_dengan_kelas_mahasiswa_dan_tidak_bisa_diubah",
						Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Kelas terpilih otomatis dan bisa diubah ketika mahasiswa mengisi KRS",
				"Pada_saat_mengambil_KRS_otomatis_kelas_terisi_dengan_kelas_mahasiswa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActive("Status mahasiswa harus aktif sebelum mahasiswa diperbolehkan mengisi KRS",
				"status_mahasiswa_harus_aktif_sebelum_isi_krs"));

		rows.appendChild(createRowActiveDefault(
				"Prosentase presensi kehadiran di menu penilaian mengikuti default jumlah pertemuan",
				"prosentase_mengikuti_default_jumlah_pertemuan", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowNilai("Jumlah pertemuan perkuliahan default", "jumlah_pertemuan_perkuliahan_default", "16"));

		// final Combobox comboboxTerlambatBayarTidakAktif =
		// createComboActive();
		// comboboxTerlambatBayarTidakAktif.addEventListener("onChange", new
		// EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// ConstantValues.terlambarLangsungTidakAktif =
		// comboboxTerlambatBayarTidakAktif.getSelectedItem() != null
		// &&
		// comboboxTerlambatBayarTidakAktif.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
		// System.out.println(
		// "ConstantValues.terlambarLangsungTidakAktif=>" +
		// ConstantValues.terlambarLangsungTidakAktif);
		// }
		// });
		rows.appendChild(createRowAktifSemesterDanAngkatanDanJurusan(
				"Semua mahasiswa yang terlambat membayar secara otomatis langsung tidak aktif",
				"mhs_all_lambat_bayar_langsung_tidak_aktif", "", null));

		rows.appendChild(
				createRowActive("Mahasiswa dengan status tidak aktif bisa melakukan pembayaran seperti status aktif",
						"mahasiswa_dengan_status_non_aktif_bisa_melakukan_pembayaran_seperti_status_aktif"));

		// rows.appendChild(createRowNilai(
		// "Semua mahasiswa yang terlambat membayar secara otomatis langsung
		// tidak aktif mulai tahun",
		// "tahun_mulai_auto_not_activating_mhs_belum_bayar", "2014"));
		// rows.appendChild(createRowActive(
		// "Matakuliah praktek dan teori menjadi satu",
		// "matakuliah_praktek_dan_teori_menjadi_satu"));

		// rows.appendChild(createRowActive(
		// "Matakuliah diskusi dan teori menjadi satu",
		// "matakuliah_diskusi_dan_teori_menjadi_satu"));

		rows.appendChild(createRowActive(
				"Secara default, nilai 0 (di dalam Tugas, UTS, UAS) tidak masuk dalam perhitungan nilai akhir",
				"nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir"));

		rows.appendChild(createRowActive(
				"Terdapat nilai minimal (0.1 atau lebih) yang tidak masuk dalam perhitungan SKS, IP dan IPK",
				"nilai_0_tidak_masuk_dalam_perhitungan_ipk"));

		rows.appendChild(createRowActiveDefault(
				"Nilai yang belum di verifikasi di semester berjalan tidak masuk dalam perhitungan SKS, IP dan IPK",
				"nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Niai huruf yang tidak masuk dalam perhitungan SKS, IP dan IPK",
				"nilai_huruf_yg_tidak_masuk_perhitungan_ip", ""));

		rows.appendChild(createRowNilai(
				"Jika nilai minimal diaktifkan, nilai berapa yang tidak masuk perhitungan SKS, IP dan IPK ?",
				"nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1"));

		rows.appendChild(
				createRowActive("Tampilkan rincian nilai di menu mahasiswa", "tampilkan_nilai_rinci_di_mahasiswa"));

		rows.appendChild(
				createRowActive("Tampilkan mulai dan batas waktu berlakukanya jadwal perkuliahan atau masa perkuliahan",
						"tampilkan_masa_perkuliahan"));

		rows.appendChild(createRowActive("Aktifkan upload password mahasiswa", "aktifkan_upload_password_mahasiswa"));
		rows.appendChild(
				createRowActive("Aktifkan generate password mahasiswa", "aktifkan_generate_password_mahasiswa"));

		rows.appendChild(createRowActiveDefault("Aktifkan download password mahasiswa",
				"aktifkan_download_password_mahasiswa", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan upload rfid mahasiswa", "aktifkan_upload_rfid_mahasiswa",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download rfid mahasiswa", "aktifkan_download_rfid_mahasiswa",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan upload rfid dosen", "aktifkan_upload_rfid_dosen",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download rfid dosen", "aktifkan_download_rfid_dosen",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan upload rfid pegawai", "aktifkan_upload_rfid_pegawai",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download rfid pegawai", "aktifkan_download_rfid_pegawai",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan upload rfid guru", "aktifkan_upload_rfid_guru",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download rfid guru", "aktifkan_download_rfid_guru",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan upload rfid siswa", "aktifkan_upload_rfid_siswa",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download rfid siswa", "aktifkan_download_rfid_siswa",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Aktifkan Download Format Mahasiwa", "aktifkan_download_format_mahasiwa"));
		rows.appendChild(createRowActive("Aktifkan Singkronkan Status Mahasiswa", "aktifkan_synchronize_status"));

		rows.appendChild(createRowNilai("Admin lain yg boleh Singkronkan Status Mahasiswa (pisah dengan tanda ;) ?",
				"admin_lain_yg_boleh_singkonkan_status", ""));

		rows.appendChild(createRowActive(
				"Aktifkan Nama dan SKS Matakuliah tidak dapat diubah jika terdapat mahasiswa yang sudah mengambil",
				"aktifkan_Nama_dan_SKS_Matakuliah_tidak_dapat_diubah_jika_terdapat_matakuliah_yang_sudah_mengambil"));

		// rows.appendChild(createRowNilai("Jumlah semester yang ditampilkan di
		// dalam kurikulum",
		// "jumlah_semester_yang_ditampilkan_di_kurikulum", "8"));

		// rows.appendChild(createRowActive("Aktifkan item penilaian Absensi",
		// "aktifkan_item_penilaian_absensi",
		// ConstantValues.ABSEN));

		// rows.appendChild(createRowNilai("Skor untuk absensi Masuk (M)",
		// "skor_absensi_masuk", "1.0"));
		// rows.appendChild(createRowNilai("Skor untuk absensi Sakit (S)",
		// "skor_absensi_sakit", "0.0"));
		// rows.appendChild(createRowNilai("Skor untuk absensi Izin (I)",
		// "skor_absensi_izin", "0.0"));
		// rows.appendChild(createRowNilai("Skor untuk absensi Tidak Ada
		// Keterangan (A)", "skor_absensi_alpa", "0.0"));

		// rows.appendChild(
		// createRowActive("Aktifkan item penilaian Tugas",
		// "aktifkan_item_penilaian_tugas", ConstantValues.FORM));
		//
		// rows.appendChild(createRowActive("Aktifkan item penilaian Tugas 1",
		// "aktifkan_item_penilaian_tugas_1",
		// ConstantValues.TUGAS_1));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Tugas 2",
		// "aktifkan_item_penilaian_tugas_2",
		// ConstantValues.TUGAS_2));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Tugas 3",
		// "aktifkan_item_penilaian_tugas_3",
		// ConstantValues.TUGAS_3));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Tugas 4",
		// "aktifkan_item_penilaian_tugas_4",
		// ConstantValues.TUGAS_4));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Tugas 5",
		// "aktifkan_item_penilaian_tugas_5",
		// ConstantValues.TUGAS_5));
		//
		// rows.appendChild(createRowActive("Aktifkan item penilaian Quiz 1",
		// "aktifkan_item_penilaian_quiz_1",
		// ConstantValues.QUIZ_1));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Quiz 2",
		// "aktifkan_item_penilaian_quiz_2",
		// ConstantValues.QUIZ_2));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Quiz 3",
		// "aktifkan_item_penilaian_quiz_3",
		// ConstantValues.QUIZ_3));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Quiz 4",
		// "aktifkan_item_penilaian_quiz_4",
		// ConstantValues.QUIZ_4));
		// rows.appendChild(createRowActive("Aktifkan item penilaian Quiz 5",
		// "aktifkan_item_penilaian_quiz_5",
		// ConstantValues.QUIZ_5));
		//
		// rows.appendChild(
		// createRowActive("Aktifkan item penilaian UTS",
		// "aktifkan_item_penilaian_uts", ConstantValues.UTS));
		// rows.appendChild(
		// createRowActive("Aktifkan item penilaian UAS",
		// "aktifkan_item_penilaian_uas", ConstantValues.UAS));

		rows.appendChild(createRowNilai("Default prosentasi UTS", "default_prentasi_uts", "0"));
		rows.appendChild(createRowNilai("Default prosentasi UAS", "default_prentasi_uas", "0"));

		rows.appendChild(
				createRowActive("Nilai UTS dan UAS default bisa diubah", "nilai_UTS_dan_UAS_default_bisa_diubah"));

		rows.appendChild(createRowNilai("Lokasi atau Directory file-file video tutorial",
				"lokasi_directory_video_tutorial", "/opt/videos"));

		rows.appendChild(createRowActive("Pengguna ecampus bisa menambah data Kota/Kabupaten jika tidak ditemukan",
				"pengguna_bisa_menambah_data_kota_kabupaten"));

		rows.appendChild(createRowActive("Pada saat mengambil KRS, otomatis kelas terisi dengan kelas mahasiswa",
				"Pada_saat_mengambil_KRS_otomatis_kelas_terisi_dengan_kelas_mahasiswa"));

		rows.appendChild(createRowActive(
				"Jika sudah mendapatkan beasiswa, mahasiswa tidak diperbolehkan mengajukan beasiswa kembali",
				"jika_sudah_dapat_beasiswa_mahasiswa_tidak_boleh_mengajukan_beasiswa"));

		rows.appendChild(createRowActive(
				"Jika sudah mendapatkan beasiswa dalam satu tahun, mahasiswa tidak diperbolehkan mengajukan beasiswa kembali",
				"jika_sudah_dapat_beasiswa_dalam_satu_tahun_mahasiswa_tidak_boleh_mengajukan_beasiswa"));
	}

	@SuppressWarnings("unchecked")
	protected void initTabKalenderPenjadwalan() {
		Rows rows = null;
		rows = (createSpan("Kalender Penjadwalan"));

		rows.appendChild(createRowActive("Selain admin tidak boleh mengubah status mahasiswa",
				"selain_admin_boleh_merubah_status_mahasiswa"));

		rows.appendChild(createRowActive("Timezone Penjadwalan", "penjadwalan_timezone", "Jakarta=GMT+7"));

		rows.appendChild(createRowNilai("Jumlah penambahan atau pengurangan waktu, default menggunakan WIB",
				"PENAMBAHAN_WAKTU", WaktuUtil.PENAMBAHAN_WAKTU + "", 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						WaktuUtil.reinit();
					}
				}));

		rows.appendChild(createRowActive("Penjadwalan Jam Mulai", "penjadwalan_jam_mulai", "7"));
		rows.appendChild(createRowActive("Penjadwalan Jam Selesai", "penjadwalan_jam_selesai", "23"));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanLabelUmum() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Label Umum"));
		rows.appendChild(createRowNilai("Label Kota", "label_kota", "Jakarta"));
		rows.appendChild(createRowNilai("Label Transkrip Akademik", "label_transkrip_akademik", "TRANSKRIP AKADEMIK"));
		rows.appendChild(createRowNilai("Label Kartu Hasil Studi", "label_khs", "KARTU HASIL STUDI"));
		rows.appendChild(createRowNilai("Label Indeks Prestasi Kumulatif", "label_indeks_prestasi_kumulatif",
				"Indeks Prestasi Kumulatif"));
		rows.appendChild(createRowNilai("Label Indeks Prestasi Semester", "label_indeks_prestasi_semester",
				"Indeks Prestasi Semester"));

		rows.appendChild(createRowNilai("Label Menyetujui", "label_menyetujui", "Menyetujui"));
		rows.appendChild(createRowNilai("Label Ketua Prodi", "label_ka_prodi", "Ketua Prodi"));
		rows.appendChild(createRowNilai("Label Disetujui Oleh", "label_disetujui_oleh", "Disetujui Oleh"));
		rows.appendChild(
				createRowNilai("Label Dosen Pembimbing Akademik", "label_dosen_pa", "Dosen Pembimbing Akademik"));
		rows.appendChild(createRowNilai("Label Mahasiswa", "label_mahasiswa", "Mahasiswa"));
		rows.appendChild(createRowNilai("Label Dosen", "label_dosen", "Dosen"));
		rows.appendChild(createRowNilai("Label Dosen 1", "label_dosen_1", "Dosen Utama"));
		rows.appendChild(createRowNilai("Label Dosen 2", "label_dosen_2", "Dosen 2"));

		rows.appendChild(createRowNilai("Label NIP di laporan", "tulisan_nip", "Nip."));

		rows.appendChild(createRowNilai("Label Kartu Rencana Studi Simple", "label_krs", "KRS"));
		rows.appendChild(
				createRowNilai("Label Kartu Rencana Studi", "label_kartu_rencana_studi", "Kartu Rencana Studi"));
	}

	@SuppressWarnings("unchecked")
	protected void initTabLabelPerguruanTinggi() {
		Rows rows = null;
		rows = (createSpan("Label Perguruan Tinggi"));

		rows.appendChild(createRowNilai("Label Instansi", "label_instansi", "Instansi"));
		rows.appendChild(createRowNilai("Label Alamat Instansi", "alamat_instansi", "Alamat Instansi"));

		rows.appendChild(createRowNilai("Label Nama Perguruan Tinggi", "label_universitas", "Universitas"));

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.BACKGROUND_DEPAN_1_STR));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BACKGROUND_DEPAN_1,
				LampiranLain.BACKGROUND_DEPAN_1_STR, LampiranLain.BACKGROUND_DEPAN_1_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkBackgroundUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);

		groupbox = new Groupbox();
		groupbox.setParent(row);

		groupbox.appendChild(new Caption(LampiranLain.BACKGROUND_DEPAN_2_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BACKGROUND_DEPAN_2,
				LampiranLain.BACKGROUND_DEPAN_2_STR, LampiranLain.BACKGROUND_DEPAN_2_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkBackgroundUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);

		groupbox = new Groupbox();
		groupbox.setParent(row);

		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN, LampiranLain.LOGO_DEPAN_STR,
				LampiranLain.LOGO_DEPAN_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);

		groupbox = new Groupbox();
		groupbox.setParent(row);

		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_DASHBOARD_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN_DASHBOARD,
				LampiranLain.LOGO_DEPAN_DASHBOARD_STR, LampiranLain.LOGO_DEPAN_DASHBOARD_STR, false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);

		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.BANNER_DEPAN_DASHBOARD_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BANNER_DEPAN_DASHBOARD,
				LampiranLain.BANNER_DEPAN_DASHBOARD_STR, LampiranLain.BANNER_DEPAN_DASHBOARD_STR, false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		rows.appendChild(
				createRowActive("Tampilkan Tulisan Teks dashboard di banner", "tampilkan_dashboard_di_banner"));

		String defaultValue = "Dashboard menyajikan informasi sekilas dari keseluruhan aktifitas yang terjadi di "
				+ ais.common.Common.getKonfigurasi("label_universitas", "").getNilai()
				+ " secara sederhana dan mudah dipahami.";

		rows.appendChild(createRowNilai("Informasi yang muncul di banner dashboard", "info_banner_dashboard",
				defaultValue, 5, null));

		rows.appendChild(createRowNilai("Tinggi banner dashboard", "tinggi_banner_dashboard", ""));

		rows.appendChild(createRowNilai("Tinggi halaman utama dashboard", "tinggi_halaman_utama_dashboard", "3000"));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.FAVICON_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.FAVICON, LampiranLain.FAVICON_STR,
				LampiranLain.FAVICON_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkFaviconUpload();
					}
				});
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label Motto Perguruan Tinggi", "label_motto", "Motto"));
		rows.appendChild(createRowNilai("Label Tanda Tangan IPK", "label_rekaman_nilai_msh", "Ketua"));
		rows.appendChild(createRowNilai("Label Atas Nama Tanda Tangan IPK", "pembantu_rektor", "...."));
		rows.appendChild(createRowNilai("Label NIP Tanda Tangan IPK", "pembantu_rektor_nip", "...."));

		rows.appendChild(createRowNilai("Label Alamat Perguruan Tinggi", "alamat_kampus", "Alamat Kampus"));
		rows.appendChild(createRowNilai("Label Telp. Perguruan Tinggi", "label_telp_kampus", "Telp. "));
		rows.appendChild(createRowNilai("Label Direktur Kampus", "direktur_akademi", "Direktur"));

		rows.appendChild(createRowNilai("Label Atas Nama Rektor", "label_an_rektor", "a.n. Rektor"));

		rows.appendChild(createRowNilai("Label Nama Rektor", "label_nama_rektor", "Nama Rektor"));
		rows.appendChild(createRowNilai("Label NIP Rektor", "label_nip_rektor", "NIP Rektor"));

		rows.appendChild(createRowNilai("Label Fakultas", "label_fakultas", "Fakultas"));
		rows.appendChild(createRowNilai("Label Dekan", "label_dekan", "Dekan"));
		rows.appendChild(createRowNilai("Label Atas Nama Dekan", "label_an_dekan", "a.n. Dekan"));

		rows.appendChild(createRowNilai("Label Prodi", "label_jurusan", "Prodi"));
		rows.appendChild(
				createRowNilai("Label Atas Nama Ketua Prodi", "label_ka_prodi", "Ketua / Sekretaris Program Studi"));

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
//
//		String nama = Common.getKonfigurasi("label_universitas", "kampus").getNilai();
		defaultValue = ""
				+ "<div style='padding: 3px;border: 1px solid #4CAF50;'><div style=\"color: rgb(0, 0, 0); font-family: arial; font-weight: 700; text-align: center;\">"
				+ "<img alt=\"\" src=\"{LOGO}\" style=\"border: 0px; width: 128px; height: 130px;\" /></div>"
				+ "<h2 style=\"text-align: center\">Selamat Datang di Dashboard Informasi {NAMA}</h2>"
				+ "<p>Dashboard Ecampus atau yang biasa disebut Digital Dashboard Ecampus adalah sebuah tampilan panel yang dibuat dengan tujuan menampilkan informasi yang mudah dibaca."
				+ " Disini akan ditampilkan informasi seputar aktifitas akademik di {NAMA} sebagai media informasi yang dapat menyajikan informasi secara efisien dengan adanya grafik atapun kalimat ringkasan dari informasi akademik serta sebagai media monitoring yang dapat memantau progress atau perkembangan dari suatu kegiatan akademik.</p>"
				+ "<br><p>Pusat Pangkalan Data dan Informasi<br>{NAMA}</p><div>";

		rows.appendChild(createRowNilai("Pengumuman di dashboard",
				"dashboard_content_baru_"
						+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getId()),
				defaultValue, 5, null));

		/*
		 * rows.appendChild(createRowNilai("Style Title Banner", "title_style",
		 * "font-size: xx-large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null)); rows.appendChild(createRowNilai("Style Motto Banner",
		 * "motto_style",
		 * "font-size: medium;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null)); rows.appendChild(createRowNilai("Style Alamat Banner",
		 * "alamat_style",
		 * "font-size: 11px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null));
		 * 
		 * rows.appendChild(createRowNilai("Style Copyright", "copyright_style_dekstop",
		 * "font-size: 11px;color:black;font-weight: bold;", 5, null));
		 * 
		 * rows.appendChild(createRowNilai("Style Footer", "footer_style_dekstop",
		 * "border-top: 1px dotted;position:fixed;bottom:0px;left:0px;", 5, null));
		 * 
		 * rows.appendChild(createRowNilai("Style Title Banner Mobile",
		 * "title_style_mobile",
		 * "font-size: large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null)); rows.appendChild(createRowNilai("Style Motto Banner Mobile",
		 * "motto_style_mobile",
		 * "font-size: 12px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null)); rows.appendChild(createRowNilai("Style Alamat Banner Mobile",
		 * "alamat_style_mobile",
		 * "font-size: 9px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"
		 * , 5, null));
		 * 
		 * rows.appendChild(createRowNilai("Style Copyright Mobile",
		 * "copyright_style_mobile", "font-size: 8px;color:black;font-weight: bold;", 5,
		 * null));
		 * 
		 * rows.appendChild(createRowNilai("Style Footer Mobile", "footer_style_mobile",
		 * "border-top: 1px dotted;position:fixed;bottom:0px;left:0px;", 5, null));
		 * 
		 * // rows.appendChild(createRowNilai("Embeded Script pada halaman utama", //
		 * "embeded_script_pada_halaman_utama", // "" +
		 * "<script id=\"cid0020000096856442526\" data-cfasync=\"false\" // async
		 * src=\"http://st.chatango.com/js/gz/emb.js\" style=\"width: // 100%;height: //
		 * 100%;\
		 * ">{\"handle\":\"zishof-ecampus\",\"arch\":\"js\",\"styles\":{\"a\":\"33ccff\",\"b\":100,\"c\":\"000000\",\"d\":\"000000\",\"k\":\"33ccff\",\"l\":\"33ccff\",\"m\":\"33ccff\",\"p\":\"10\",\"q\":\"33ccff\",\"r\":100,\"surl\":0,\"cnrs\":\"0.35\",\"fwtickm\":1}}</script>",
		 * // 5, null));
		 * 
		 * // rows.appendChild(createRowNilai("Embeded Script pada halaman // Penerimaan
		 * Mahasiswa Baru", // "embeded_script_pada_halaman_pmb", // "" +
		 * "<script id=\"cid0020000096856442526\" data-cfasync=\"false\" // async
		 * src=\"http://st.chatango.com/js/gz/emb.js\" style=\"width: // 100%;height: //
		 * 100%;\
		 * ">{\"handle\":\"zishof-ecampus\",\"arch\":\"js\",\"styles\":{\"a\":\"33ccff\",\"b\":100,\"c\":\"000000\",\"d\":\"000000\",\"k\":\"33ccff\",\"l\":\"33ccff\",\"m\":\"33ccff\",\"p\":\"10\",\"q\":\"33ccff\",\"r\":100,\"surl\":0,\"cnrs\":\"0.35\",\"fwtickm\":1}}</script>",
		 * // 5, null));
		 * 
		 * rows.appendChild(
		 * createRowNilai("Keterangan checklist penilaian dosen oleh mahasiswa",
		 * "keterangan_checklist_penilaian_dosen_oleh_mahasiswa",
		 * "Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap dosen Saudara. Informasi yang Saudara berikan hanya akan dipergunakan dalam proses sertifikasi dosen dan tidak akan berpengaruh terhadap status Saudara sebagai mahasiswa. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka (1-5) pada kolom skor.  "
		 * + "\n 1 = sangat tidak baik/sangat rendah/tidak pernah" +
		 * "\n 2 = tidak baik/rendah/jarang " + "\n 3 = biasa/cukup/kadang-kadang " +
		 * "\n 4 = baik/tinggi/sering " + "\n 5 = sangat baik/sangat tinggi/selalu", 15,
		 * null));
		 * 
		 * rows.appendChild(
		 * createRowNilai("Jumlah pilihan checklist penilaian dosen oleh mahasiswa",
		 * "jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5"));
		 * 
		 * rows.appendChild(createRowNilai("Keterangan checklist penilaian umum",
		 * "keterangan_checklist_penilaian_umum",
		 * "Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka (1-5) pada kolom skor.  "
		 * +
		 * "\n 1 = sangat tidak baik/sangat rendah/tidak pernah\n 2 = tidak baik/rendah/jarang "
		 * + "\n 3 = biasa/cukup/kadang-kadang \n 4 = baik/tinggi/sering " +
		 * "\n 5 = sangat baik/sangat tinggi/selalu", 15, null));
		 * 
		 * rows.appendChild(createRowNilai("Jumlah pilihan checklist penilaian umum",
		 * "jumlah_pilihan_checklist_penilaian_umum", "5"));
		 */
	}

	@SuppressWarnings("unchecked")
	protected void initTabKartuUts() {
		Rows rows = null;
		rows = (createSpan("Kartu UTS"));

		rows.appendChild(createRowActive("Tampilkan tombol cetak kartu UTS", "tampilkan_tombol_cetak_kartu_uts"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena A (Tidak Ada Alasan) boleh mengikuti UTS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_uts", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena S (Sakit) boleh mengikuti UTS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_uts", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena I (Izin) boleh mengikuti UTS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_uts", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah semua tidak masuk kuliah boleh mengikuti UTS",
				"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_uts", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal persen tidak masuk kuliah boleh mengikuti UTS",
				"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_uts", "0", 1, null));

		rows.appendChild(createRowActiveDefault(
				"Aturan batas maksimal tidak masuk kuliah ini juga berlaku saat proses penilaian UTS",
				"aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Status pertemuan terkait aturan batas maksimal tidak masuk kuliah ini juga berlaku saat proses penilaian UTS (kosongkan jika berlaku untuk semua)",
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts",
				""));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen dari total pembayaran boleh men-cetak kartu UTS",
				"batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts", "60", 1, null));

		rows.appendChild(createRowActiveWithDefault(
				"Mahasiswa harus bayar item biaya UTS sebelum ikut ujian UTS (jika kode lebih dari satu, pisahkan dengan tanda koma(,)",
				"mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts", "508", "100", null,
				Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		////// Semester pendek
		rows.appendChild(createRowActive("Tampilkan tombol cetak kartu UTS Semester Pendek",
				"tampilkan_tombol_cetak_kartu_uts_sp"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen dari total pembayaran boleh men-cetak kartu UTS Semester Pendek",
				"batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts_sp", "0", 1, null));

		rows.appendChild(createRowActiveWithDefault(
				"Mahasiswa harus bayar item biaya UTS semester pendek sebelum ikut ujian UTS semester pendek (jika kode lebih dari satu, pisahkan dengan tanda koma(,)",
				"mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts_sp", "5081", "100", null,
				Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		////// Semester pendek

		rows.appendChild(createRowActiveDefault("Mahasiswa harus upload foto sebelum ikut ujian UTS",
				"mahasiswa_harus_upload_foto_sebelum_ikut_ujian_uts", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Tata tertib kartu UTS", "tata_tertib_kartu_uts", "TATA TERTIB :\n"
				+ "1.   Mahasiswa harus berada diruangan 5 (lima) menit sebelum ujian dimulai, mahasiswa yang terlambat tidak diperkenankan memasuki\n"
				+ "      ruang ujian, kecuali mendapat izin dari panitia/pengawas ujian.\n"
				+ "2.   Mahasiswa diwajibkan memakai Jas Almamater, berpakaian rapi (tidak boleh memakai jeans, kaos dan pakaian pres body.\n"
				+ "3.   Mahasiswa diwajibkan Memakai sepatu, Memakai kaos kaki dan bagi wanita memakai androk panjang.\n"
				+ "4.   Mahasiswa tidak diperkenankan membawa buku atau catatan dalam bentuk apapun kedalam ruang ujian.\n"
				+ "5.   Mahasiswa tidak diperkenankan membawa atau mengaktifkan HP atau telepon genggam selama ujian berlangsung.\n"
				+ "6.   Mahasiswa tidak diperkenankan meminta atau memberikan jawaban kepada mahasiswa lainnya selama ujian berlangsung.\n"
				+ "7.   Mahasiswa tidak diperkenankan memindahkan atau mengubah kursi ujian selama ujian berlangsung.\n"
				+ "8.   Mahasiswa diwajibkan memperlihatkan Kartu Peserta Ujian, identitas diri serta kartu aktifitas yang telah di ACC oleh dosen pengampu\n"
				+ "      mata kuliah kepada penguji, dan bagi yang belum di ACC tidak diperkenankan mengikuti ujian.\n"
				+ "9.   Kartu aktifitas yang telah di ACC dan tertera tanda tangan dosen dipotong dan dimasukkan ke dalam amplop beserta lembar jawaban.\n"
				+ "10. Mahasiswa yang melanggar tata tertib butir 1 s/d 8 akan diberikan sangsi berupa :\n"
				+ " * Teguran\n" + " * Tertulis\n" + " * Dibatalkan hak ujian semester", 15, null));

		rows.appendChild(createRowNilai("Label Kartu UTS", "label_kartu_uts", "KARTU UJIAN TENGAH SEMESTER"));
		rows.appendChild(createRowNilai("Label Persetujuan Kartu UTS Oleh", "label_persetujuan_uts", "Kepala BAAK"));
		rows.appendChild(createRowNilai("Nama Persetujuan UTS", "nama_persetujuan_uts", "T. Yuliana Purba, Ir., Msi"));
		rows.appendChild(createRowNilai("NIP Persetujuan UTS", "nip_persetujuan_uts", ""));
	}

	@SuppressWarnings("unchecked")
	protected void initTabKartuUas() {
		Rows rows = null;
		rows = (createSpan("Kartu UAS"));

		rows.appendChild(createRowActive("Tampilkan tombol cetak kartu UAS", "tampilkan_tombol_cetak_kartu_uas"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena A (Tidak Ada Alasan) boleh mengikuti UAS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_uas", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena S (Sakit) boleh mengikuti UAS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_uas", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah tidak masuk kuliah karena I (Izin) boleh mengikuti UAS",
				"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_uas", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal jumlah semua tidak masuk kuliah boleh mengikuti UAS",
				"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_uas", "34", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas maksimal persen tidak masuk kuliah boleh mengikuti UAS",
				"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_uas", "0", 1, null));

		rows.appendChild(createRowActiveDefault(
				"Aturan batas maksimal tidak masuk kuliah ini juga berlaku saat proses penilaian UAS",
				"aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Status pertemuan terkait aturan batas maksimal tidak masuk kuliah ini juga berlaku saat proses penilaian UAS (kosongkan jika berlaku untuk semua)",
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas",
				""));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen dari total pembayaran boleh men-cetak kartu UAS",
				"batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas", "99", 1, null));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen dari total pembayaran (semester saat ini +1) boleh men-cetak kartu UAS",
				"batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas_plus_satu", "0", 1, null));

		rows.appendChild(createRowActiveWithDefault(
				"Mahasiswa harus bayar item biaya UAS sebelum ikut ujian UAS (jika kode lebih dari satu, pisahkan dengan tanda koma(,))",
				"mahasiswa_wajib_bayar_item_biaya_uas_sebelum_ikut_ujian_uas", "509", "100", "",
				Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		////// Semester pendek
		rows.appendChild(createRowActive("Tampilkan tombol cetak kartu UAS Semester Pendek",
				"tampilkan_tombol_cetak_kartu_uas_sp"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Batas terendah persen dari total pembayaran boleh men-cetak kartu UAS Semester Pendek",
				"batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas_sp", "0", 1, null));

		rows.appendChild(createRowActiveWithDefault(
				"Mahasiswa harus bayar item biaya UAS semester pendek sebelum ikut ujian UAS semester pendek (jika kode lebih dari satu, pisahkan dengan tanda koma(,)",
				"mahasiswa_wajib_bayar_item_biaya_uas_sebelum_ikut_ujian_uas_sp", "5091", "100", "",
				Konfigurasi.TIDAK_AKTIF, createComboActive(true)));

		////// Semester pendek

		rows.appendChild(createRowActiveDefault("Mahasiswa harus upload foto sebelum ikut ujian UAS",
				"mahasiswa_harus_upload_foto_sebelum_ikut_ujian_uas", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Tata tertib kartu UAS", "tata_tertib_kartu_uas", "TATA TERTIB :\n"
				+ "1.   Mahasiswa harus berada diruangan 5 (lima) menit sebelum ujian dimulai, mahasiswa yang terlambat tidak diperkenankan memasuki\n"
				+ "      ruang ujian, kecuali mendapat izin dari panitia/pengawas ujian.\n"
				+ "2.   Mahasiswa diwajibkan memakai Jas Almamater, berpakaian rapi (tidak boleh memakai jeans, kaos dan pakaian pres body.\n"
				+ "3.   Mahasiswa diwajibkan Memakai sepatu, Memakai kaos kaki dan bagi wanita memakai androk panjang.\n"
				+ "4.   Mahasiswa tidak diperkenankan membawa buku atau catatan dalam bentuk apapun kedalam ruang ujian.\n"
				+ "5.   Mahasiswa tidak diperkenankan membawa atau mengaktifkan HP atau telepon genggam selama ujian berlangsung.\n"
				+ "6.   Mahasiswa tidak diperkenankan meminta atau memberikan jawaban kepada mahasiswa lainnya selama ujian berlangsung.\n"
				+ "7.   Mahasiswa tidak diperkenankan memindahkan atau mengubah kursi ujian selama ujian berlangsung.\n"
				+ "8.   Mahasiswa diwajibkan memperlihatkan Kartu Peserta Ujian, identitas diri serta kartu aktifitas yang telah di ACC oleh dosen pengampu\n"
				+ "      mata kuliah kepada penguji, dan bagi yang belum di ACC tidak diperkenankan mengikuti ujian.\n"
				+ "9.   Kartu aktifitas yang telah di ACC dan tertera tanda tangan dosen dipotong dan dimasukkan ke dalam amplop beserta lembar jawaban.\n"
				+ "10. Mahasiswa yang melanggar tata tertib butir 1 s/d 8 akan diberikan sangsi berupa :\n"
				+ " * Teguran\n" + " * Tertulis\n" + " * Dibatalkan hak ujian semester", 15, null));

		rows.appendChild(createRowNilai("Label Kartu UAS", "label_kartu_uas", "KARTU UJIAN AKHIR SEMESTER"));
		rows.appendChild(createRowNilai("Label Persetujuan Kartu UAS Oleh", "label_persetujuan_uas", "Kepala BAAK"));
		rows.appendChild(createRowNilai("Nama Persetujuan UAS", "nama_persetujuan_uas", "T. Yuliana Purba, Ir., Msi"));
		rows.appendChild(createRowNilai("NIP Persetujuan UAS", "nip_persetujuan_uas", ""));
	}

	@SuppressWarnings("unchecked")
	protected void initTabObe() {
		Rows rows = null;
		rows = (createSpan("OBE"));

		createSpan("Integrasi OBE dengan e-Learning", rows);
		final Combobox cmbObeElearning;
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol OBE di toolbar e-Learning (Linimasa). Aktifkan hanya untuk Perguruan Tinggi yang sudah menggunakan kurikulum OBE. Pengaturan ini juga tersedia di tab Elearning.",
				"tampilkan_obe_di_elearning", Konfigurasi.TIDAK_AKTIF,
				cmbObeElearning = createComboActive()));
		cmbObeElearning.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardTimelinePertemuan.tampilkan_obe_di_elearning =
						cmbObeElearning.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
			}
		});
		rows.appendChild(createRowNilai(
				"Nilai minimal ketercapaian default OBE jika belum diisi pada RPS",
				"nilai_minimal_ketercapaian_default_obe", "60", 1, null));

		createSpan("Hak Akses RPS OBE", rows);
		rows.appendChild(createRowNilai(
				"Kode role default yang boleh mengubah RPS OBE. Nilai ini menjadi fallback untuk fitur yang belum punya pengaturan khusus. Pisahkan dengan koma. Contoh: am,admfak,admprd,Akademik. Isi * jika semua role internal boleh.",
				"hak_akses_yang_boleh_mengubah_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah bagian Mata Kuliah / initMk()",
				"hak_akses_yang_boleh_mengubah_mk_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah bagian Otoritas / initOtoritas()",
				"hak_akses_yang_boleh_mengubah_otoritas_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Profil Lulusan / initPl()",
				"hak_akses_yang_boleh_mengubah_pl_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah CPL / initCpl()",
				"hak_akses_yang_boleh_mengubah_cpl_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah CPMK / initCpmk()",
				"hak_akses_yang_boleh_mengubah_cpmk_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Sub-CPMK / initSubCpmk()",
				"hak_akses_yang_boleh_mengubah_sub_cpmk_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Korelasi Sub-CPMK / initSubCpmkKorelasi()",
				"hak_akses_yang_boleh_mengubah_sub_cpmk_korelasi_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Relasi CPL-CPMK / initCplCpmk()",
				"hak_akses_yang_boleh_mengubah_cpl_cpmk_rps_obe", "am,admfak,admprd,Akademik", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Deskripsi / initDeskripsi()",
				"hak_akses_yang_boleh_mengubah_deskripsi_rps_obe", "am,admfak,admprd,Akademik,Dosen", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Rincian RPS / initRinci()",
				"hak_akses_yang_boleh_mengubah_rincian_rps_obe", "am,admfak,admprd,Akademik,Dosen", 2, null));
		rows.appendChild(createRowNilai(
				"Hak akses ubah Catatan / initCatatan()",
				"hak_akses_yang_Boleh_mengubah_catatan_obe", "am,admfak,admprd,Akademik,Dosen", 2, null));
		rows.appendChild(createRowActiveDefault(
				"Admin root boleh membuka kunci RPS/Nilai OBE walaupun RPS sudah dikunci oleh user lain",
				"kunci_nilai_obe_untuk_admin", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Keterangan bantuan pengaturan hak akses OBE",
				"keterangan_hak_akses_obe",
				"Setiap fitur RPS OBE sekarang dapat dibuka/ditutup per bagian. Role yang tidak masuk daftar pada bagian tersebut hanya dapat melihat. Kosongkan daftar role jika bagian tersebut harus tertutup untuk semua role non-root. Gunakan * untuk semua role internal.",
				3, null));
	}

	@SuppressWarnings("unchecked")
	protected void initTabKartuMahasiswa() {
		Rows rows = null;
		rows = (createSpan("Kartu Mahasiswa"));

		defaultValue = "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini harus dibawa sebagai identitas mahasiswa.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Mahasiswa harus mematuhi semua tata tertib .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
				+ "website : " + Common.getRequestHostWithProtocol();

		rows.appendChild(
				createRowNilai("Tata Tertib Kartu Mahasiswa", "tata_tertib_kartu_mahasiswa", defaultValue, 15, null));

		row = new MyFormRow();
		row.setParent(rows);

		groupbox = new Groupbox();
		groupbox.setParent(row);

		groupbox.appendChild(new Caption("Tanda Tangan Untuk Kartu Mahasiswa (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.TANDA_TANGAN_KARTU_MAHASISWA_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_MAHASISWA_PERPUSTAKAAN_STR, "Tanda Tangan", false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Stempel Untuk Kartu Mahasiswa (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.STEMPEL_KARTU_MAHASISWA_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_MAHASISWA_PERPUSTAKAAN_STR, "Stempel", false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label Jabatan Kartu Mahasiswa", "label_jabatan_kartu_mahasiswa", "Rektor"));
		rows.appendChild(
				createRowNilai("Label TTD Kartu Mahasiswa", "label_ttd_kartu_mahasiswa", "...................."));

		rows.appendChild(createRowNilai("NIP Kartu Mahasiswa", "nip_ttd_kartu_mahasiswa", "...................."));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Depan kartu Mahasiswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_1_KARTU_MAHASISWA_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_MAHASISWA_PERPUSTAKAAN_STR, LampiranLain.BG_1_KARTU_MAHASISWA_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Belakang kartu Mahasiswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_2_KARTU_MAHASISWA_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_MAHASISWA_PERPUSTAKAAN_STR, LampiranLain.BG_2_KARTU_MAHASISWA_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Masa berlaku kartu mahasiswa", "masa_berlaku_kartu_mahasiswa", "4"));

		rows.appendChild(createRowActiveDefault("Tamilkan CR Code di belakang kartu", "apakah_tampilan_cr_code",
				Konfigurasi.AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabKartuPegawai() {
		Rows rows = null;
		rows = (createSpan("Kartu Pegawai"));

		defaultValue = "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini harus dibawa sebagai identitas pegawai.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Pegawai harus mematuhi semua tata tertib .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
				+ "website : " + Common.getRequestHostWithProtocol();

		rows.appendChild(
				createRowNilai("Tata Tertib Kartu Pegawai", "tata_tertib_kartu_pegawai", defaultValue, 15, null));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Tanda Tangan Untuk Kartu Pegawai (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.TANDA_TANGAN_KARTU_PEGAWAI_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_PEGAWAI_PERPUSTAKAAN_STR, "Tanda Tangan", false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Stempel Untuk Kartu Pegawai (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.STEMPEL_KARTU_PEGAWAI_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_PEGAWAI_PERPUSTAKAAN_STR, "Stempel", false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label Jabatan Kartu Pegawai", "label_jabatan_kartu_pegawai", "Rektor"));
		rows.appendChild(createRowNilai("Label TTD Kartu Pegawai", "label_ttd_kartu_pegawai", "...................."));

		rows.appendChild(createRowNilai("NIP Kartu Pegawai", "nip_ttd_kartu_pegawai", "...................."));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Depan kartu Pegawai"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_1_KARTU_PEGAWAI_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_PEGAWAI_PERPUSTAKAAN_STR, LampiranLain.BG_1_KARTU_PEGAWAI_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Belakang kartu Pegawai"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_2_KARTU_PEGAWAI_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_PEGAWAI_PERPUSTAKAAN_STR, LampiranLain.BG_2_KARTU_PEGAWAI_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Masa berlaku kartu pegawai", "masa_berlaku_kartu_pegawai", "4"));

		rows.appendChild(createRowActiveDefault("Tamilkan CR Code di belakang kartu",
				"apakah_tampilan_cr_code_kartu_pegawai", Konfigurasi.AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabAlumni() {
		Rows rows = null;
		rows = (createSpan("Alumni"));

		rows.appendChild(createRowActive("Galeri gambar tampil di halaman alumni", "tampil_galery_gambar_alumni"));

		rows.appendChild(
				createRowActive("Pengisian tracer study tampil di halaman alumni", "tampil_input_tracer_study"));

		rows.appendChild(createRowActive("Daftar alumni tampil di halaman alumni", "tampil_daftar_alumni"));

		rows.appendChild(createRowActive("Statistik alumni tampil di halaman alumni", "tampil_statistik_alumni"));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_ALUMNI_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN_ALUMNI,
				LampiranLain.LOGO_DEPAN_ALUMNI_STR, LampiranLain.LOGO_DEPAN_ALUMNI_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(LampiranLain.BANNER_DEPAN_ALUMNI_STR));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BANNER_DEPAN_ALUMNI,
				LampiranLain.BANNER_DEPAN_ALUMNI_STR, LampiranLain.BANNER_DEPAN_ALUMNI_STR, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.checkLogoUpload();
					}
				});
		hbox.setParent(groupbox);

		rows.appendChild(
				createRowActive("Tampilkan Tulisan Teks tracer study alumni di banner", "tampilkan_alumni_di_banner"));

		defaultValue = "Tracer study adalah studi pelacakan jejak lulusan/alumni yang " + "dilakukan kepada alumni "
				+ ais.common.Common.getKonfigurasi("label_universitas", "").getNilai() + "."
				+ "Tracer study bertujuan untuk mengetahui outcomependidikan dalam "
				+ "bentuk transisi dari dunia pendidikan tinggi ke dunia kerja, output "
				+ "pendidikan yaitu penilaian diri terhadap penguasaan dan pemerolehan "
				+ "kompetensi, proses pendidikan berupa evaluasi proses pembelajaran "
				+ "dan kontribusi pendidikan tinggi terhadap pemerolehan kompetensi "
				+ "serta input pendidikan berupa penggalian lebih lanjut terhadap "
				+ "informasi sosiobiografis lulusan.";

		rows.appendChild(
				createRowNilai("Informasi yang muncul di banner alumni", "info_banner_alumni", defaultValue, 5, null));

		rows.appendChild(createRowNilai("Tinggi banner alumni", "tinggi_banner_alumni", ""));

		rows.appendChild(createRowNilai("Tinggi halaman utama alumni", "tinggi_halaman_utama_alumni_baru", "8800"));

		rows.appendChild(createRowNilai(
				"Nomor Whatsapp yang bisa dihubungi, kasih tanda koma (,) jika nomor WA lebih dari satu. Kosongkan jika tidak ada help desk",
				"no_whatsapp_alumni", "0811111111111111", 3, null, null));

		rows.appendChild(
				createRowNilai("Tanya Whatsapp", "tanya_whatsapp_alumni", "Salamat Datang, apa yang bisa kami bantu?"));

		rows.appendChild(createRowNilai("Jawab Whatsapp", "jawab_whatsapp_alumni",
				"Saya ingin menanyakan tentang informasi penerimaan mahasiswa baru, apakah Anda bisa membantu?"));

		defaultValue = "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini harus dibawa sebagai identitas alumni.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Alumni harus mematuhi semua tata tertib .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
				+ "website : " + Common.getRequestHostWithProtocol();

		rows.appendChild(
				createRowNilai("Tata Tertib Kartu Alumni", "tata_tertib_kartu_alumni", defaultValue, 15, null));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Tanda Tangan Untuk Kartu Alumni (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.TANDA_TANGAN_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_ALUMNI_PERPUSTAKAAN_STR, "Tanda Tangan", false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Stempel Untuk Kartu Alumni (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN_STR, "Stempel", false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label Jabatan Kartu Alumni", "label_jabatan_kartu_alumni", "Rektor"));
		rows.appendChild(createRowNilai("Label TTD Kartu Alumni", "label_ttd_kartu_alumni", "...................."));

		rows.appendChild(createRowNilai("NIP Kartu Alumni", "nip_ttd_kartu_alumni", "...................."));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Depan kartu Alumni"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN_STR, LampiranLain.BG_1_KARTU_ALUMNI_PERPUSTAKAAN_STR, false,
				null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Belakang kartu Alumni"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR, LampiranLain.BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR, false,
				null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Masa berlaku kartu alumni", "masa_berlaku_kartu_alumni", "4"));

		rows.appendChild(createRowActiveDefault("Tamilkan CR Code di belakang kartu", "apakah_tampilan_cr_code",
				Konfigurasi.AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanKeuangan() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Keuangan"));
		rows.appendChild(createRowActive("Terintegrasi dengan modul akuntansi", "integrasi_modul_akuntansi"));
		rows.appendChild(createRowActiveDefault(
				"Sembunyikan nominal Tabungan/Deposit ke mahasiswa & calon mahasiswa — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction",
				"sembunyikan_nominal_tabungan_ke_mahasiswa", Konfigurasi.AKTIF));

		createSpan("Dashboard Jurnal Keuangan", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan semua jenis jurnal keuangan di dashboard draft jurnal",
				"dashboard_draft_jurnal_tampilkan_semua_jurnal", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan progress saat dashboard jurnal, tagihan, dan pembayaran dimuat",
				"dashboard_keuangan_progress_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sembunyikan progress otomatis setelah proses 100 persen",
				"dashboard_keuangan_progress_hide_100", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah baris per halaman tabel dashboard keuangan",
				"dashboard_keuangan_page_size", "10"));
		rows.appendChild(createRowNilai("Jumlah data maksimal untuk ringkasan dashboard keuangan",
				"dashboard_keuangan_max_summary_rows", "1200"));
		rows.appendChild(createRowActiveDefault("Aktifkan grafik HTML/CSS pada dashboard jurnal dan pembayaran",
				"dashboard_keuangan_html_chart_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan radar/spider sederhana pada dashboard jurnal dan pembayaran",
				"dashboard_keuangan_spider_chart_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan preview grid sebelum download Excel",
				"dashboard_keuangan_preview_grid_sebelum_excel", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Judul progress dashboard keuangan",
				"dashboard_keuangan_progress_judul", "Sedang memuat data keuangan"));
		rows.appendChild(createRowNilai("Keterangan progress dashboard keuangan",
				"dashboard_keuangan_progress_keterangan",
				"Data sedang dihitung dan disusun menjadi ringkasan, grafik, dan tabel.", 3, null));

		createSpan("Dashboard Pembayaran Mahasiswa", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan ringkasan tagihan dan pembayaran mahasiswa",
				"dashboard_pembayaran_mahasiswa_ringkasan_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tren pembayaran mahasiswa per bulan",
				"dashboard_pembayaran_mahasiswa_tren_bulanan_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan komposisi item biaya mahasiswa",
				"dashboard_pembayaran_mahasiswa_item_biaya_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan riwayat pembayaran terakhir mahasiswa",
				"dashboard_pembayaran_mahasiswa_riwayat_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan dashboard jurnal pembayaran mahasiswa",
				"dashboard_jurnal_pembayaran_mahasiswa_aktif", Konfigurasi.AKTIF));

		createSpan("Dashboard Pembayaran Siswa", rows);
		rows.appendChild(createRowActiveDefault("Tampilkan dashboard jurnal pembayaran siswa",
				"dashboard_jurnal_pembayaran_siswa_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tren pembayaran siswa per bulan",
				"dashboard_pembayaran_siswa_tren_bulanan_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ringkasan piutang, denda, dan diskon siswa",
				"dashboard_pembayaran_siswa_ringkasan_piutang_aktif", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Tampilkan filter kelas pada billing pembayaran",
				"tampilkan_filter_kelas_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Tampilkan pengecualian KRS mahasiswa di pembayaran mahasiswa",
				"tampilkan_pengecualian_krs_mahasiswa_di_pembayaran", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Tampilkan filter jenis tempat tinggal mahasiswa pada billing pembayaran",
				"tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF));

		List<GeneralValueObject> parameterTambahans = HibernateUtil.currentSession()
				.createCriteria(ParameterTambahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		rows.appendChild(createRowActiveWithDefaultCombo(
				"Tambah dan aktifkan filter ke-1 paramater tambahan di billimg pembayaran",
				"tambah_dan_aktifkan_filter_ke_1_paramater_tambahan", "-1", Konfigurasi.TIDAK_AKTIF,
				createComboActive(false), parameterTambahans));
		rows.appendChild(createRowActiveWithDefaultCombo(
				"Tambah dan aktifkan filter ke-2 paramater tambahan di billimg pembayaran",
				"tambah_dan_aktifkan_filter_ke_2_paramater_tambahan", "-1", Konfigurasi.TIDAK_AKTIF,
				createComboActive(false), parameterTambahans));
		rows.appendChild(createRowActiveWithDefaultCombo(
				"Tambah dan aktifkan filter ke-3 paramater tambahan di billimg pembayaran",
				"tambah_dan_aktifkan_filter_ke_3_paramater_tambahan", "-1", Konfigurasi.TIDAK_AKTIF,
				createComboActive(false), parameterTambahans));

		rows.appendChild(createRowActive(
				"Mahasiswa boleh meng-angsur atau men-cicil pembayaran daftar ulang (pembayaran per semester)",
				"mahasiswa_boleh_mencicil_pembayaran_daftar_ulang"));

		rows.appendChild(
				createRowActive("Calon mahasiswa baru boleh meng-angsur atau men-cicil pembayaran daftar ulang",
						"cicilan_daftar_ulang_mahasiswa_baru"));

		rows.appendChild(createRowActive("Mengaktifkan / tidak mengaktifkan perubahan detail biaya",
				Konfigurasi.DETAIL_BIAYA_EXCEL));

		rows.appendChild(
				createRowActiveDefault("Apabila bank host tidak ditemukan, secara otomatis akan membuat data bank host",
						"apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran mahasiswa host to host per bulan",
				"aktifkan_biaya_host_to_host_per_bulan", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan auto reconsile pembayaran mahasiswa host to host",
				"aktifkan_auto_reconsile_biaya_host_to_host", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Direktori / folder tempat file auto reconsile",
				"direktori_folder_tempat_file_auto_reconsile", "/opt"));

		rows.appendChild(createRowNilai("Jenis file auto reconsile", "jenis_file_auto_reconsile", "csv"));

		rows.appendChild(
				createRowNilai("Default class yang digunakan untuk memproses reconsile pembayaran host to host",
						"default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host",
						DefaultJenisParsingReconsile.class.getName()));

		rows.appendChild(createRowActiveDefault(
				"Tagihan pembayaran host to host per bulan dihitung berdasarkan akumulasi bulanan yg belum dibayar",
				"tagihan_pembayaran_host_to_host_per_bulan_dihitung_berdasarkan_akumulasi_bulanan_yg_belum_dibayar",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Pada saat pengaturan tagihan per-bulan, nilai total tagihan harus sama dengan biaya pembayaran selama satu semester.",
				"nilai_total_tagihan_harus_sama_dengan_biaya_pembayaran_selama_satu_semester", Konfigurasi.AKTIF));

		final Combobox pembayaran_semester_ganjil_mulai_di_bulan = new Combobox();
		String[] data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			pembayaran_semester_ganjil_mulai_di_bulan.appendChild(comboitem);
		}
		pembayaran_semester_ganjil_mulai_di_bulan.setReadonly(true);

		pembayaran_semester_ganjil_mulai_di_bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.pembayaranSemesterGanjilMulaiDiBulan = Integer
						.parseInt(pembayaran_semester_ganjil_mulai_di_bulan.getSelectedItem().getValue().toString());
			}
		});

		rows.appendChild(createRowActiveWithDefault(
				"Jika pembayaran / angsuran mahasiswa per bulan diaktifkan, pembayaran semester ganjil mulai di bulan",
				"pembayaran_semester_ganjil_mulai_di_bulan", "", "9", pembayaran_semester_ganjil_mulai_di_bulan));

		final Combobox pembayaran_semester_genap_mulai_di_bulan = new Combobox();
		data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			pembayaran_semester_genap_mulai_di_bulan.appendChild(comboitem);
		}
		pembayaran_semester_genap_mulai_di_bulan.setReadonly(true);

		pembayaran_semester_genap_mulai_di_bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.pembayaranSemesterGenapMulaiDiBulan = Integer
						.parseInt(pembayaran_semester_genap_mulai_di_bulan.getSelectedItem().getValue().toString());
			}
		});

		rows.appendChild(createRowActiveWithDefault(
				"Jika pembayaran / angsuran mahasiswa per bulan diaktifkan, pembayaran semester genap mulai di bulan",
				"pembayaran_semester_genap_mulai_di_bulan", "", "3", pembayaran_semester_genap_mulai_di_bulan));

		Combobox jumlahangsuran = new Combobox();
		data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			jumlahangsuran.appendChild(comboitem);
		}
		jumlahangsuran.setReadonly(true);

		rows.appendChild(createRowActiveWithDefault(
				"Jika pembayaran / angsuran mahasiswa per bulan diaktifkan, Jumlah bulan angsuran selama satu semester",
				"jumlah_bulan_angsuran", "", "6", jumlahangsuran));

		rows.appendChild(createRowActiveWithDefault(
				"Jika pembayaran / angsuran calon mahasiswa per bulan diaktifkan, Jumlah bulan angsuran selama satu semester",
				"jumlah_bulan_angsuran_calon", "", "6", (Combobox) jumlahangsuran.clone()));

		rows.appendChild(createRowActiveDefault(
				"Sisipkan kode pembayaran di akhir, pada saat pembayaran via h2h. Contoh: jika nim 123 dan kode pembayaran 100, maka menjadi 123100",
				"sisipkan_kode_pembayaran_di_akhir_pada_saat_pembayaran_via_h2h", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Panjang digit kode pembayaran untuk sisipan pada saat pembayaran via h2h.",
				"panjang_sisipkan_kode_pembayaran_di_akhir_pada_saat_pembayaran_via_h2h", "3"));

		rows.appendChild(
				createRowActive("Nominal pembayaran mahasiswa pada host to host harus sama dengan jumlah tagihan",
						"nominal_pembayaran_h2h_harus_sama_dengan_tagihan"));

		rows.appendChild(createRowActive(
				"Nominal pembayaran No. Reg. calon mahasiswa pada host to host harus sama dengan jumlah tagihan",
				"nominal_pembayaran_no_reg_h2h_harus_sama_dengan_tagihan"));

		rows.appendChild(createRowActive(
				"Nominal pembayaran Daftar Ulang calon mahasiswa pada host to host harus sama dengan jumlah tagihan",
				"nominal_pembayaran_daftar_ulang_h2h_harus_sama_dengan_tagihan"));

		rows.appendChild(createRowActive("Mahasiswa tidak boleh mencicil pembayaran via host to host",
				"mahasiswa_tidak_boleh_mencicil_pembayaran_via_h2h"));

		rows.appendChild(createRowActive("Calon Mahasiswa tidak boleh mencicil pembayaran No. Reg. via host to host",
				"calon_mahasiswa_tidak_boleh_mencicil_pembayaran_no_reg_via_h2h"));

		rows.appendChild(
				createRowActive("Calon Mahasiswa tidak boleh mencicil pembayaran Daftar Ulang via host to host",
						"calon_mahasiswa_tidak_boleh_mencicil_pembayaran_daftar_ulang_via_h2h"));

		rows.appendChild(createRowActive(
				"Aktifkan pengaturan pembayaran bulanan (bulan ke-1 s.d ke-6) selama satu semester untuk pembayaran angsuran",
				"aktifkan_rencana_pembayaran_bulanan"));

		rows.appendChild(createRowActiveDefault(
				"Pada saat pembayaran melalui pembayaran langsung, bukti pembayaran harus diupload",
				"harus_menyertakan_bukti_pembayaran", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Biaya administrasi pembayaran manual", "manual_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Kode akun pembayaran manual untuk biaya administrasi",
				"kode_akun_manual_biaya_administrasi", ""));

		rows.appendChild(createRowActiveDefault("Aktifkan keranjang pembayaran", "aktifkan_keranjang_pembayaran",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Cetak bukti setiap kali proses pembayaran dilakukan",
				"cetak_bukti_pembayaran_setelah_proses_pembayaran"));

		rows.appendChild(createRowActiveDefault("Bukti pembayaran berdasarkan sejarah pembayaran",
				"bukti_pembayaran_berdasarkan_sejarah_pembayaran", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai(
				"Kode role yang bisa menghapus data pembayaran mahasiswa, bisa dipisahkan dengan semikolon (;) jika lebih dari satu",
				"admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", ""));

		rows.appendChild(createRowNilai(
				"Admin lain (berupa username) yang bisa menghapus pembayaran mahasiswa, bisa dipisahkan dengan semikolon (;) jika lebih dari satu",
				"admin_lain_bisa_menghapus_pembayaran_mahasiswa", ""));

		rows.appendChild(createRowNilai(
				"Username yang bisa menghapus data pembayaran Virtual Account, bisa dipisahkan dengan semikolon (;) jika lebih dari satu",
				"admin_yang_bisa_menghapus_data_pembayaran_va", ""));

		rows.appendChild(createRowActive("Admin yang bisa mengaktifkan atau men-non aktifkan billing pembayaran",
				"admin_yang_bisa_menonaktifkan_tagihan"));

		rows.appendChild(createRowNilai(
				"daftar username pengguna yang bisa mengaktifkan atau men-non aktifkan billing pembayaran, bisa dipisahkan dengan semikolon (;) jika lebih dari satu",
				"pengguna_yang_bisa_menonaktifkan_tagihan", ""));
	}

	@SuppressWarnings("unchecked")

	private String defaultPanduanPendaftaranPmbFooter() {
		return "Panduan Pendaftaran Lengkap PMB ini disusun untuk membantu calon mahasiswa memahami setiap tahapan pendaftaran secara tertib, jelas, dan mudah diikuti. Pendaftaran mahasiswa baru merupakan proses resmi untuk mencatat identitas calon mahasiswa, pilihan program studi, jalur seleksi, kelengkapan dokumen, serta informasi pembayaran yang diperlukan oleh perguruan tinggi. Calon mahasiswa diharapkan membaca panduan ini dengan saksama sebelum mulai mengisi formulir, karena data yang dimasukkan akan menjadi dasar proses seleksi, verifikasi, komunikasi, dan penerbitan dokumen administrasi pendaftaran. "
				+ "Langkah pertama adalah memastikan bahwa calon mahasiswa membuka halaman PMB resmi milik institusi. Gunakan perangkat yang stabil, koneksi internet yang memadai, dan alamat email atau nomor telepon yang aktif. Data kontak sangat penting karena panitia dapat menggunakannya untuk mengirim pemberitahuan, konfirmasi pembayaran, hasil seleksi, atau informasi tambahan. Hindari menggunakan data milik orang lain apabila tidak benar-benar diperlukan, karena seluruh pemberitahuan pendaftaran sebaiknya diterima langsung oleh calon mahasiswa atau wali yang bertanggung jawab. "
				+ "Langkah berikutnya adalah memilih jalur pendaftaran atau gelombang yang masih aktif. Setiap gelombang dapat memiliki ketentuan waktu, biaya, pilihan program studi, dan persyaratan yang berbeda. Calon mahasiswa perlu memperhatikan tanggal pembukaan, batas akhir pendaftaran, batas unggah berkas, jadwal seleksi, serta ketentuan daftar ulang. Apabila terdapat beberapa jalur seleksi, pilih jalur yang paling sesuai dengan kondisi dan prestasi calon mahasiswa. Jalur seleksi dapat berupa jalur reguler, prestasi, kerja sama, beasiswa, undangan, atau bentuk lain sesuai kebijakan perguruan tinggi. "
				+ "Setelah memilih jalur, calon mahasiswa mengisi biodata pribadi. Data yang perlu disiapkan antara lain nama lengkap sesuai dokumen resmi, tempat dan tanggal lahir, jenis kelamin, alamat, nomor identitas, asal sekolah, data orang tua atau wali, serta informasi pendidikan sebelumnya. Pengisian nama harus mengikuti dokumen resmi seperti ijazah, KTP, kartu keluarga, atau surat keterangan lulus. Kesalahan penulisan nama dapat berpengaruh pada pencetakan kartu peserta, bukti pendaftaran, hingga dokumen akademik pada tahap berikutnya. Oleh karena itu, periksa kembali setiap kolom sebelum menyimpan data. "
				+ "Calon mahasiswa kemudian memilih program studi atau paket pendaftaran yang tersedia. Pemilihan program studi sebaiknya dilakukan berdasarkan minat, kemampuan akademik, tujuan karier, serta informasi kurikulum yang diberikan oleh institusi. Apabila sistem menyediakan pilihan pertama dan pilihan kedua, urutkan pilihan tersebut secara cermat. Pilihan pertama sebaiknya merupakan program studi yang paling diminati. Pilihan kedua dapat digunakan sebagai alternatif apabila kuota, hasil seleksi, atau kebijakan akademik mengharuskan penempatan pada program lain. "
				+ "Tahap berikutnya adalah mengunggah berkas. Dokumen yang umum diminta meliputi pas foto, kartu identitas, kartu keluarga, rapor, ijazah atau surat keterangan lulus, bukti prestasi, dan dokumen pendukung lain. Pastikan file yang diunggah terbaca jelas, tidak terpotong, tidak buram, dan sesuai format yang ditentukan. Apabila berkas berupa foto, gunakan pencahayaan yang cukup dan hindari bayangan. Apabila berupa PDF, pastikan seluruh halaman tersusun rapi. Berkas yang tidak jelas dapat menyebabkan proses verifikasi tertunda atau calon mahasiswa diminta mengunggah ulang. "
				+ "Setelah biodata dan berkas tersimpan, calon mahasiswa perlu memperhatikan status pembayaran. Beberapa jalur pendaftaran mensyaratkan pembayaran registrasi sebelum biodata dapat dilengkapi, sedangkan jalur lain mengizinkan pengisian biodata terlebih dahulu. Ikuti petunjuk pembayaran yang tampil pada halaman PMB. Simpan bukti pembayaran dengan baik, lalu unggah bukti tersebut jika sistem meminta. Apabila pembayaran dilakukan melalui kanal otomatis, tunggu beberapa saat sampai status pembayaran diperbarui. Bila status belum berubah setelah waktu yang wajar, hubungi panitia dengan menyertakan nomor registrasi dan bukti pembayaran. "
				+ "Pada tahap seleksi, calon mahasiswa harus mengikuti ketentuan yang berlaku. Seleksi dapat berbentuk ujian daring, wawancara, penilaian rapor, verifikasi dokumen, atau kombinasi beberapa metode. Pastikan membaca jadwal dan instruksi sebelum seleksi dimulai. Untuk ujian daring, gunakan perangkat yang layak, baterai cukup, koneksi stabil, dan lingkungan yang tenang. Untuk wawancara, siapkan identitas diri, penjelasan singkat mengenai minat kuliah, serta alasan memilih program studi. Datang atau masuk ke ruang daring lebih awal agar tidak terlambat. "
				+ "Hasil seleksi biasanya diumumkan melalui halaman PMB, pesan resmi, atau dokumen yang dapat dicetak. Calon mahasiswa yang dinyatakan lulus perlu membaca instruksi daftar ulang, pembayaran lanjutan, pengisian data tambahan, dan jadwal kegiatan awal perkuliahan. Jangan menunda daftar ulang sampai batas akhir, karena kuota, kelas, atau fasilitas tertentu dapat mengikuti kebijakan institusi. Bila terdapat kendala, segera hubungi panitia melalui kontak resmi yang tersedia pada halaman PMB. "
				+ "Panduan ini juga mengingatkan calon mahasiswa agar menjaga kerahasiaan akun. Nomor registrasi, kata sandi, tanggal lahir, atau PIN tidak boleh diberikan kepada pihak yang tidak berwenang. Pastikan selalu keluar dari akun setelah selesai menggunakan komputer umum. Semua informasi yang dikirimkan melalui sistem PMB harus benar, dapat dipertanggungjawabkan, dan sesuai dokumen asli. Apabila ditemukan data tidak benar, institusi berhak melakukan klarifikasi, meminta perbaikan, atau mengambil keputusan sesuai aturan yang berlaku. "
				+ "Dengan mengikuti panduan ini, calon mahasiswa diharapkan dapat menyelesaikan pendaftaran secara mandiri, tertib, dan tepat waktu. Proses PMB bukan hanya kegiatan administrasi, tetapi juga langkah awal untuk memasuki lingkungan akademik yang menuntut kedisiplinan, kejujuran, tanggung jawab, serta kesiapan belajar. Bacalah setiap petunjuk yang tampil pada layar, periksa kembali data sebelum menyimpan, simpan bukti pendaftaran, dan pantau status secara berkala sampai seluruh proses dinyatakan selesai.";
	}

	private String defaultPersyaratanBerkasPmbFooter() {
		return "Persyaratan kelengkapan berkas digunakan untuk memastikan bahwa data calon mahasiswa sesuai dengan dokumen resmi. Calon mahasiswa perlu menyiapkan dokumen identitas, data pendidikan, pas foto, dokumen keluarga, serta berkas pendukung lain sesuai jalur pendaftaran. Setiap file sebaiknya dipindai atau difoto dengan jelas, tidak buram, tidak terpotong, dan memuat seluruh informasi penting. Nama pada dokumen harus sama dengan nama yang diisikan pada formulir. Apabila terdapat perbedaan penulisan, calon mahasiswa sebaiknya menyiapkan keterangan pendukung agar proses verifikasi dapat berjalan lancar. Berkas yang belum lengkap dapat menyebabkan status pendaftaran tertunda sampai calon mahasiswa melakukan perbaikan. Panitia dapat meminta unggah ulang apabila dokumen tidak terbaca, format tidak sesuai, ukuran file terlalu besar, atau informasi penting tidak terlihat. Simpan dokumen asli dengan baik karena dapat diminta pada saat daftar ulang, wawancara, atau verifikasi akhir. Pastikan seluruh dokumen yang diunggah merupakan dokumen yang benar dan dapat dipertanggungjawabkan.";
	}

	private String defaultBiayaPendidikanPmbFooter() {
		return "Informasi biaya pendidikan membantu calon mahasiswa dan keluarga menyiapkan rencana pembiayaan sejak awal. Komponen biaya dapat meliputi biaya pendaftaran, biaya seleksi, biaya daftar ulang, biaya pengembangan, biaya perkuliahan, biaya praktikum, atau komponen lain sesuai kebijakan institusi. Nominal biaya dapat berbeda berdasarkan program studi, gelombang, jalur seleksi, sistem kuliah, potongan biaya, beasiswa, atau kerja sama tertentu. Calon mahasiswa dianjurkan membaca informasi tagihan yang tampil pada akun masing-masing karena data tersebut biasanya sudah disesuaikan dengan pilihan pendaftaran. Pembayaran harus dilakukan melalui kanal resmi yang ditentukan oleh institusi. Simpan bukti pembayaran sampai status pada sistem berubah menjadi valid atau lunas. Apabila pembayaran belum terkonfirmasi, hubungi panitia dengan menyertakan nomor registrasi, tanggal pembayaran, nominal, nama pengirim, dan bukti transaksi. Hindari melakukan pembayaran ke rekening pribadi atau pihak yang tidak tercantum dalam informasi resmi PMB.";
	}

	private String defaultFaqPmbFooter() {
		return "Tanya jawab PMB disediakan untuk membantu calon mahasiswa memahami hal-hal yang paling sering ditanyakan. Apabila belum memiliki akun, calon mahasiswa dapat memulai dari menu pendaftaran dan mengikuti langkah yang tersedia. Apabila lupa nomor registrasi, periksa kembali bukti pendaftaran, email, pesan singkat, atau hubungi panitia dengan menyebutkan nama lengkap dan data identitas. Apabila tidak dapat masuk ke akun, pastikan nomor registrasi, tanggal lahir, PIN, atau kata sandi sudah benar sesuai petunjuk. Apabila berkas gagal diunggah, periksa format, ukuran, dan kualitas file. Apabila status pembayaran belum berubah, tunggu proses validasi atau konfirmasi ke panitia melalui kontak resmi. Apabila ingin mengubah pilihan program studi, ikuti aturan yang berlaku karena perubahan dapat dibatasi setelah pembayaran, verifikasi, atau seleksi dilakukan. Apabila dinyatakan lulus, segera baca instruksi daftar ulang dan selesaikan kewajiban sesuai batas waktu yang ditetapkan.";
	}


	/**
	 * Pengaturan SPMB modern dipisahkan agar tab SPMB lebih mudah dirawat.
	 * Key lama tetap dipertahankan, sedangkan key baru dipakai oleh PMB versi JSP
	 * dan ZKoss agar login, logout, theme, footer, dan dashboard calon mahasiswa
	 * bisa dikendalikan tanpa mengubah kode program.
	 */
	protected void initTabSpmbPengaturanModern(Rows rows) {
		if (rows == null) {
			return;
		}

		createSpan("SPMB - Login, Logout, dan Keamanan Sesi", rows);
		rows.appendChild(createRowActiveDefault(
				"Gunakan cookie untuk login dan logout PMB. Default tidak aktif agar akses calon mahasiswa memakai session server dan tidak otomatis login dari browser lama.",
				"pmb_login_logout_menggunakan_cookie", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Izinkan auto-login PMB dari cookie jika pengaturan cookie PMB diaktifkan",
				"pmb_auto_login_dari_cookie", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Saat calon mahasiswa logout, hapus hanya cookie PMB agar cookie modul lain tidak ikut hilang",
				"pmb_logout_hapus_cookie_pmb_saja", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Nama cookie ID calon mahasiswa PMB", "pmb_cookie_biodata_name", "biodataCalonMahasiswa"));
		rows.appendChild(createRowNilai(
				"Nama cookie user calon mahasiswa PMB", "pmb_cookie_userid_name", "userid"));
		rows.appendChild(createRowNilai(
				"Masa berlaku cookie PMB dalam hari jika cookie diaktifkan", "pmb_cookie_masa_berlaku_hari", "7"));
		rows.appendChild(createRowNilai(
				"Batas waktu tidak aktif sesi calon mahasiswa PMB dalam menit", "pmb_session_timeout_menit", "60"));
		rows.appendChild(createRowActiveDefault(
				"Logout PMB menjalankan Common.setLogout(request, response) terlebih dahulu agar sesi server dan data login aktif dibersihkan dengan benar",
				"pmb_logout_gunakan_common_setlogout", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Setelah logout PMB, gunakan JavaScript window.location.replace agar redirect tetap berjalan meskipun header response sudah dipakai oleh JSP atau ZK",
				"pmb_logout_gunakan_javascript_replace", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Forward URL logout PMB", "pmb_logout_forward_url", "pmb"));
		rows.appendChild(createRowNilai(
				"Path redirect logout PMB setelah sesi dibersihkan", "pmb_logout_redirect_path", "/logoff?forward_url=pmb"));
		rows.appendChild(createRowNilai(
				"Catatan login dan logout PMB untuk admin",
				"catatan_pmb_login_logout_cookie",
				"Secara default, PMB tidak memakai cookie login agar calon mahasiswa tidak otomatis masuk dari perangkat yang sama. Aktifkan hanya jika institusi benar-benar membutuhkan fitur ingat sesi. Saat tombol keluar ditekan, proses yang direkomendasikan adalah memanggil Common.setLogout(request, response), lalu mengarahkan ulang halaman dengan window.location.replace(Common.ROOT + '/logoff?forward_url=pmb'). Cara ini lebih aman untuk halaman JSP/ZK karena redirect tetap berjalan walaupun header response sudah digunakan.",
				5, null));

		createSpan("SPMB - Theme, Font Awesome, dan Tampilan Publik", rows);
		rows.appendChild(createRowActiveDefault(
				"Gunakan cara baca theme seperti login2.jsp, yaitu dari PerguruanTinggi.getCss() dan ditimpa Sekolah.getCss() jika konteks sekolah tersedia",
				"pmb_theme_mengikuti_login2", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Fallback theme PMB jika theme institusi kosong. Isi nama file saja, contoh: hijau_kuning.css",
				"tema_pmb_css", ""));
		rows.appendChild(createRowActiveDefault(
				"Muat base-theme.css pada header PMB", "pmb_muat_base_theme_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Muat base-pmb.css pada header PMB", "pmb_muat_base_pmb_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Gunakan Font Awesome berbasis CSS/webfont saja agar icon tidak berubah menjadi SVG tanda tanya",
				"pmb_fontawesome_css_only", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Versi Font Awesome PMB", "pmb_fontawesome_version", "6.5.2"));
		rows.appendChild(createRowActiveDefault(
				"Aktifkan tampilan PMB publik versi HTML/JSP modern", "pmb_tampilan_html_jsp_modern", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Aktifkan penyegaran tampilan PMB ZKoss 5.5", "pmb_tampilan_zkoss_modern", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Catatan theme PMB",
				"catatan_theme_pmb",
				"Theme PMB cukup berisi konfigurasi warna :root. Layout, tombol, card, dashboard, dan style Font Awesome diletakkan di base-theme.css dan base-pmb.css agar semua theme tetap ringan, konsisten, dan mudah dirawat.",
				4, null));

		createSpan("SPMB - Teks Sederhana untuk Panel dan Dashboard Calon Mahasiswa", rows);
		rows.appendChild(createRowNilai(
				"Deskripsi ringkas dashboard calon mahasiswa",
				"pmb_desc_dashboard_calon_mahasiswa",
				"Ringkasan status pendaftaran, pembayaran, berkas, seleksi, dan daftar ulang dalam satu halaman agar peserta mudah mengetahui langkah berikutnya.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi profil calon mahasiswa",
				"pmb_desc_profil_calon_mahasiswa",
				"Data utama peserta, nomor registrasi, pilihan program studi, dan status proses pendaftaran ditampilkan agar mudah diperiksa kembali.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi formulir biodata PMB",
				"pmb_desc_formulir_biodata",
				"Lengkapi data diri, asal sekolah, pilihan program studi, serta informasi orang tua/wali sesuai dokumen resmi.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi berkas pendaftaran PMB",
				"pmb_desc_berkas_pendaftaran",
				"Unggah dokumen yang diminta dengan file yang jelas, benar, dan mudah dibaca oleh panitia verifikasi.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi pembayaran PMB",
				"pmb_desc_pembayaran",
				"Lihat tagihan, ikuti instruksi pembayaran, lalu pantau perubahan status setelah pembayaran berhasil diproses.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi kelulusan PMB",
				"pmb_desc_kelulusan",
				"Hasil seleksi dan instruksi daftar ulang ditampilkan setelah panitia menetapkan keputusan resmi.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi kartu ujian PMB",
				"pmb_desc_kartu_ujian",
				"Kartu ujian berisi identitas peserta, nomor ujian, jadwal, dan informasi yang perlu dibawa saat seleksi.",
				3, null));
		rows.appendChild(createRowNilai(
				"Deskripsi alur pendaftaran PMB",
				"pmb_desc_alur_pendaftaran",
				"Ikuti urutan pendaftaran mulai dari memilih jalur, mengisi data, mengunggah berkas, membayar, mengikuti seleksi, sampai daftar ulang.",
				3, null));

		createSpan("SPMB - Alur dan Nomor Ujian", rows);
		rows.appendChild(createRowActiveDefault(
				"Generate nomor ujian hanya jika nomor ujian masih kosong", "pmb_generate_no_ujian_hanya_jika_kosong", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Gunakan penguncian transaksi saat generate nomor ujian untuk mencegah nomor ganda ketika banyak peserta mencetak kartu bersamaan",
				"pmb_generate_no_ujian_gunakan_advisory_lock", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Lock timeout generate nomor ujian dalam detik", "pmb_generate_no_ujian_lock_timeout_detik", "15"));
		rows.appendChild(createRowNilai(
				"Statement timeout generate nomor ujian dalam detik", "pmb_generate_no_ujian_statement_timeout_detik", "120"));
		rows.appendChild(createRowActiveDefault(
				"Simpan perubahan nomor ujian dengan update singkat agar tidak ikut menyimpan seluruh field biodata yang tidak berubah",
				"pmb_generate_no_ujian_update_ringkas", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Catatan nomor ujian PMB",
				"catatan_pmb_generate_no_ujian",
				"Nomor ujian sebaiknya dibuat sekali saja dalam transaksi pendek. Hindari update ganda dari JSP atau background thread agar baris biodata calon mahasiswa tidak terkunci terlalu lama.",
				4, null));

		createSpan("SPMB - Pusat Informasi Footer", rows);
		rows.appendChild(createRowNilai(
				"Judul grup pusat informasi di footer PMB", "pmb_footer_info_title", "Pusat Informasi"));
		rows.appendChild(createRowNilai(
				"Label link panduan pendaftaran di footer PMB", "pmb_footer_label_panduan", "Panduan Pendaftaran Lengkap"));
		rows.appendChild(createRowNilai(
				"Label link persyaratan berkas di footer PMB", "pmb_footer_label_berkas", "Persyaratan Kelengkapan Berkas"));
		rows.appendChild(createRowNilai(
				"Label link informasi biaya di footer PMB", "pmb_footer_label_biaya", "Informasi Biaya Pendidikan"));
		rows.appendChild(createRowNilai(
				"Label link tanya jawab di footer PMB", "pmb_footer_label_faq", "Tanya Jawab (FAQ)"));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan teks default jika file pusat informasi PMB belum diunggah",
				"pmb_footer_gunakan_default_jika_file_kosong", Konfigurasi.AKTIF));

		createSpan("SPMB - Validasi dan Upload Berkas", rows);
		rows.appendChild(createRowNilai(
				"Ukuran maksimal file berkas PMB dalam MB", "pmb_upload_maksimal_mb", "10"));
		rows.appendChild(createRowNilai(
				"Ekstensi file berkas PMB yang diperbolehkan", "pmb_upload_ekstensi_diperbolehkan", "pdf,jpg,jpeg,png,doc,docx"));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan tombol pratinjau berkas setelah peserta mengunggah file",
				"pmb_upload_tampilkan_pratinjau", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Izinkan peserta mengganti berkas selama belum diverifikasi panitia",
				"pmb_upload_boleh_ganti_sebelum_verifikasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Catatan upload berkas PMB",
				"catatan_pmb_upload_berkas",
				"Berkas sebaiknya berukuran wajar, jelas dibaca, dan sesuai kategori. Dokumen yang sudah diverifikasi sebaiknya dikunci agar data administrasi tetap tertib.",
				4, null));
	}

	protected void initTabSpmb() {
		Rows rows = null;
		rows = (createSpan("SPMB"));

		rows.appendChild(createRowActive("Aktifkan login PMB", "tampilkan_login_pmb"));
		initTabSpmbPengaturanModern(rows);

		createSpan("SPMB - Gelombang dan Hak Ubah Calon Mahasiswa", rows);
		rows.appendChild(createRowActiveDefault(
				"Calon mahasiswa boleh mengubah gelombang yang sudah dipilih selama belum dinyatakan diterima atau prodi lulus masih kosong",
				"calon_mahasiswa_boleh_mengubah_gelombang_sebelum_diterima", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Catatan aturan perubahan gelombang calon mahasiswa",
				"catatan_perubahan_gelombang_calon_mahasiswa",
				"Jika konfigurasi aktif, calon mahasiswa dapat memperbaiki pilihan gelombang sebelum diterima. Jika sudah memiliki prodi lulus, status lulus, tanggal diterima, atau gelombang diterima, gelombang dikunci.",
				3, null));

		{
			final String keyTaPmb = "tahunAkademikPenerimaanMahasiswaBaru";
			final Konfigurasi konfigTaPmb = Common.getKonfigurasi(keyTaPmb, Common.getCurrentTahunAkademik());
			final Combobox cboTaPmb = Common.generateTahunAjaran(null);
			cboTaPmb.setReadonly(true);
			cboTaPmb.setWidth("250px");
			Common.selectComboItem(cboTaPmb, konfigTaPmb.getNilai());
			cboTaPmb.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (cboTaPmb.getSelectedItem() == null) return;
					konfigTaPmb.setNilai(cboTaPmb.getSelectedItem().getLabel());
					Session sSaveTa = HibernateUtil.currentNativeSession();
					sSaveTa.getTransaction().begin();
					sSaveTa.saveOrUpdate(konfigTaPmb);
					sSaveTa.getTransaction().commit();
					ais.common.KarirConfigUtil.closeNativeSession(sSaveTa);
					MemoryDbUtil.getKonfigurasi().put(konfigTaPmb.getNama(), konfigTaPmb);
				}
			});
			MyFormRow rowTaPmb = new MyFormRow();
			rowTaPmb.setValign("top");
			Groupbox gbTaPmb = new Groupbox();
			gbTaPmb.setParent(rowTaPmb);
			Caption capTaPmb = new Caption();
			capTaPmb.setSclass("ais-caption-styled");
			gbTaPmb.appendChild(capTaPmb);
			Hbox ubsTaPmb = new Hbox();
			ubsTaPmb.setParent(capTaPmb);
			new Label(ais.common.Common.getBahasaConfig("Penerimaan Mahasiswa baru untuk tahun akademik")).setParent(ubsTaPmb);
			cboTaPmb.setParent(gbTaPmb);
			rows.appendChild(rowTaPmb);
		}

//		row = new MyFormRow();
////		row.setParent(rows);
//		groupbox = new Groupbox();
//		groupbox.setParent(row);
//		groupbox.appendChild(new Caption(LampiranLain.LOGO_DEPAN_PMB_STR));
//		hbox = new Hbox();
//		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO_DEPAN_PMB, LampiranLain.LOGO_DEPAN_PMB_STR,
//				LampiranLain.LOGO_DEPAN_PMB_STR, false, new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						Common.checkLogoUpload();
//					}
//				});
//		hbox.setParent(groupbox);
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		groupbox = new Groupbox();
//		groupbox.setParent(row);
//		groupbox.appendChild(new Caption(LampiranLain.BANNER_DEPAN_PMB_STR));
//		hbox = new Hbox();
//		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BANNER_DEPAN_PMB,
//				LampiranLain.BANNER_DEPAN_PMB_STR, LampiranLain.BANNER_DEPAN_PMB_STR, false, new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						Common.checkLogoUpload();
//					}
//				});
//		hbox.setParent(groupbox);
//
//		rows.appendChild(createRowActive("Tampilkan Tulisan Teks penerimaan mahasiswa baru di banner",
//				"tampilkan_pmb_di_banner"));
//
//		defaultValue = "Kegiatan seleksi penerimaan mahasiswa baru merupakan kegiatan yang bertujuan mendapatkan calon mahasiswa yang berkualitas dan memiliki kompetensi dasar yang baik sesuai dengan standar yang ditetapkan. Kegiatan ini merupaka kegiatan rutin bagi "
//				+ ais.common.Common.getKonfigurasi("label_universitas", "").getNilai()
//				+ ", karena itu penyelenggaraannya harus profesional, terjamin, terukur dan efesien.";
//
//		rows.appendChild(createRowNilai("Informasi yang muncul di banner penerimaan mahasiswa baru", "info_banner_pmb",
//				defaultValue, 5, null));

		rows.appendChild(createRowNilai("Tinggi banner penerimaan mahasiswa baru", "tinggi_banner_pmb", ""));

		rows.appendChild(
				createRowNilai("Tinggi halaman utama penerimaan mahasiswa baru", "tinggi_halaman_utama_pmb", "850"));

//		rows.appendChild(createRowNilai("Nomor Whatsapp yang bisa dihubungi", "no_whatsapp_pmb", "0811111111111111"));

		rows.appendChild(createRowNilai(
				"Nomor Whatsapp yang bisa dihubungi, kasih tanda koma (,) jika nomor WA lebih dari satu. Kosongkan jika tidak ada help desk",
				"no_whatsapp_pmb", "0811111111111111", 3, null, null));

		rows.appendChild(
				createRowNilai("Tanya Whatsapp", "tanya_whatsapp_pmb", "Salamat Datang, apa yang bisa kami bantu?"));

		rows.appendChild(createRowNilai("Jawab Whatsapp", "jawab_whatsapp_pmb",
				"Saya ingin menanyakan tentang informasi penerimaan mahasiswa baru, apakah Anda bisa membantu?"));

		rows.appendChild(
				createRowActive("Tampilkan tombol login/logout calon mahasiswa", "tampilkan_halaman_login_di_pmb"));
		rows.appendChild(createRowActive("Tampilkan tombol alur pendaftaran calon mahasiswa", "tampilkan_alur_pmb"));
		rows.appendChild(
				createRowActive("Tampilkan tombol formulir pendaftaran calon mahasiswa", "tampilkan_formulir_pmb"));
		rows.appendChild(createRowActive("Tampilkan tombol Info Pembayaran calon mahasiswa",
				"tampilkan_informasiPembayaran_pmb"));
		rows.appendChild(createRowActive("Tampilkan tombol login calon mahasiswa", "tampilkan_loginCalonMhs_pmb"));
		rows.appendChild(createRowActive("Tampilkan tombol informasi kelulusan calon mahasiswa",
				"tampilkan_informasiKelulusan_pmb"));

		rows.appendChild(
				createRowActive("Tampilkan tombol payment gateway calon mahasiswa", "tampilkan_payment_gateway_pmb"));

		rows.appendChild(createRowActive("Calon mahasiswa harus melakukan pembayaran sebelum bisa login",
				"calon_mahasiswa_harus_melakukan_pembayaran_sebelum_bisa_login"));

		rows.appendChild(createRowActiveDefault("Aktifkan tombol upload data calon mahasiswa",
				"aktifkan_tombol_upload_data_calon_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActive("Aktifkan tombol upload Gen. NIM calon mahasiswa",
				"aktifkan_tombol_upload_gen_nim_data_calon_mahasiswa"));

		rows.appendChild(createRowActive("Jika sudah menjadi mahasiswa, data calon mahasiswa tidak boleh diubah",
				"jika_sudah_ada_data_mahasiswa_data_calon_mahasiswa_tidak_bisa_diubah_baru"));

		rows.appendChild(createRowActive(
				"Calon mahasiswa harus dinyatakan lulus sebelum dapat melakukan pembayaran daftar ulang",
				"calon_mahasiswa_harus_lulus_sebelum_bayar_daftar_ulang"));

		rows.appendChild(createRowNilai(
				"Informasi peringatan yang muncul saat calon mahasiswa belum bayar mencoba login",
				"infoBelumbayarSaatLogincalonMahasiswa",
				"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat login karena belum melakukan proses pembayaran."));

		rows.appendChild(createRowActiveDefault("Keterangan di paket digunakan sebagai info kelulusan jika di-isi",
				"keterangan_paket_digunakan_sebagai_info_kelulusan_jika_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Umur calon mahasiswa dibatasi", "umur_calon_mahasiswa_dibatasi",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jika umur calon mahasiswa dibatasi, berapa umur-nya",
				"nilai_umur_calon_mahasiswa_dibatasi", "27"));

		rows.appendChild(
				createRowNilai("Jumlah maksimal tahun kelulusan ke-belakang yang bisa diambil oleh calon mahasiswa",
						"jumlah_maks_tahun_kelulusan_kebelakang", "50"));

		rows.appendChild(createRowActive(
				"Tampilkan form pertanyaan ke calon mahasiswa, tentang dari mana mendapatkan informasi Penerimaan Mahasiswa Baru",
				"tampilkan_info_sekolah_dari_mana_pada_pmb"));

		rows.appendChild(createRowNilai("Apa saja info pertanyaan yang ditampilkan ?", "info_dari_mana_pmb",
				"Website,Teman,Radio,Koran,Lain-lain"));

		rows.appendChild(createRowActive("Aktifkan Daftar Mahasiswa Baru", "daftar_s1"));
		rows.appendChild(createRowNilai("Nama Universitas", "label_universitas_pmb", "Universitas"));
		rows.appendChild(createRowNilai("Alamat Universitas", "label_alamat_pmb", "Alamat"));

		rows.appendChild(createRowNilaiSemesterDanAngkatanDanJurusan(
				"Alamat email monitoring calon mahasiswa cetak nomor regitrasi", "alamat_email_monitoring_regitrasi",
				"", 1, null));

		rows.appendChild(
				createRowNilaiSemesterDanAngkatanDanJurusan("Alamat email monitoring calon mahasiswa cetak nomor ujian",
						"alamat_email_monitoring_cetak_no_ujian", "", 1, null));

		// rows.appendChild(createRowNilai(
		// "Jumlah pilihan prodi yang bisa dipilih saat penerimaan mahasiswa
		// baru",
		// "jumlah_prodi_yang_bisa_dipilih_saat_pmb", "1"));

		rows.appendChild(createRowNotActive("Calon mahasiswa wajib melakukan pembayaran daftar ulang mahasiswa baru",
				"calon_mahasiswa_wajib_melakukan_pembayaran_daftar_ulang_mahasiswa_baru"));
		
		rows.appendChild(createRowNotActive("Calon mahasiswa wajib otomatis dapat email ketika mendapatkan NIM",
				"broadcast_ketika_dapat_nim"));
		

		rows.appendChild(
				createRowNotActive("Calon mahasiswa wajib melakukan pelunasan pembayaran daftar ulang mahasiswa baru",
						"calon_mahasiswa_wajib_melakukan_pembayaran_lunas_daftar_ulang_mahasiswa_baru"));

		rows.appendChild(createRowActive("Program (Reguler / Non Reguler) di formulir pmb bisa dipilih",
				"program_di_formulir_pmb_bisa_dipilih"));

		rows.appendChild(createRowActive("Saat pendaftaran mahasiswa baru, email wajib diisi",
				"saat_pendaftaran_pmb_email_wajib_diisi"));

		rows.appendChild(createRowActive("Saat login sebagai calon mahasiswa baru, pendidikan asal tidak boleh diubah",
				"pendidikan_asal_saat_login_pmb_tidak_boleh_diubah"));

		rows.appendChild(createRowActive("Calon mahasiswa tidak perlu upload ijazah",
				"calon_mahasiswa_tidak_perlu_upload_ijazah"));

		rows.appendChild(createRowActive("Calon mahasiswa tidak perlu upload transkrip nilai",
				"calon_mahasiswa_tidak_perlu_upload_nilai"));

		rows.appendChild(createRowActive("Calon mahasiswa tidak perlu upload ktp / kartu pelajar",
				"calon_mahasiswa_tidak_perlu_upload_ktp"));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-1", "calon_mahasiswa_upload_lampiran_1",
				"Upload lampiran lain ke-1", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-2", "calon_mahasiswa_upload_lampiran_2",
				"Upload lampiran lain ke-2", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-3", "calon_mahasiswa_upload_lampiran_3",
				"Upload lampiran lain ke-3", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-4", "calon_mahasiswa_upload_lampiran_4",
				"Upload lampiran lain ke-4", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-5", "calon_mahasiswa_upload_lampiran_5",
				"Upload lampiran lain ke-5", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-6", "calon_mahasiswa_upload_lampiran_6",
				"Upload lampiran lain ke-6", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-7", "calon_mahasiswa_upload_lampiran_7",
				"Upload lampiran lain ke-7", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-8", "calon_mahasiswa_upload_lampiran_8",
				"Upload lampiran lain ke-8", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-9", "calon_mahasiswa_upload_lampiran_9",
				"Upload lampiran lain ke-9", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveWithDefault("Upload lampiran lain ke-10", "calon_mahasiswa_upload_lampiran_10",
				"Upload lampiran lain ke-10", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Calon mahasiswa baru otomatis mendapatkan nim saat mahasiswa tersebut membayar pembayaran daftar ulang",
				"calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang",
				Konfigurasi.TIDAK_AKTIF));

		Combobox angkatanMhs = new Combobox();
		Common.generateTahunAngkatan(angkatanMhs);
		MyComboitemConfig comboitemAngkatanMhs = new MyComboitemConfig("Semua Angkatan");
		comboitemAngkatanMhs.setValue("");
		angkatanMhs.appendChild(comboitemAngkatanMhs);
		angkatanMhs.setSelectedItem(comboitemAngkatanMhs);
		angkatanMhs.setReadonly(true);

		rows.appendChild(createRowNilaiProgramDanJurusan(
				"Kode item biaya untuk pembayaran mahasiswa baru otomatis dapat NIM",
				"kode_item_biaya_untuk_pembayaran_mahasiswa_baru_otomatis_dapat_nim", "", 1, null, angkatanMhs));

		rows.appendChild(createRowNilaiProgramDanJurusan(
				"Jika kode item biaya kosong, minimal jumlah pembayaran daftar ulang agar mahasiswa otomatis mendapatkan NIM",
				"minimal_jumlah_pembayaran_mahasiswa_otomatis_mendapatkan_nim", "0", 1, null, angkatanMhs));

		rows.appendChild(createRowActiveDefault(
				"Calon mahasiswa baru otomatis mendapatkan nim saat mahasiswa tersebut membayar sekian persen dari daftar ulang",
				"calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_membayar_persen_pembayaran_daftar_ulang",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilaiProgramDanJurusan(
				"Jika membayar sekian persen dari daftar ulang diaktfikan, minimal jumlah persen pembayaran daftar ulang agar mahasiswa otomatis mendapatkan NIM",
				"minimal_jumlah_persen_pembayaran_mahasiswa_otomatis_mendapatkan_nim", "0", 1, null, angkatanMhs));

		rows.appendChild(createRowNilai("Class yang digunakan untuk generate Nomor Registrasi Mahasiswa Baru",
				"class_untuk_generate_no_reg", "ais.action.master.pmb.noreg.DefaultNoRegGenerator"));

		rows.appendChild(createRowNilai("Class yang digunakan untuk generate Nomor Ujian Mahasiswa Baru",
				"class_untuk_generate_no_ujian", "ais.action.master.pmb.noujian.DefaultNoUjianGenerator"));

		rows.appendChild(createRowNilai("Class yang digunakan untuk generate nim", "class_untuk_generate_nim",
				"ais.action.master.pmb.nim.DefaultNimGenerator"));

		rows.appendChild(
				createRowNilai("Jumlah digit penambahan nomor otomatis pada nomor registrasi penerimaan mahasiswa baru",
						"jumlah_increments_no_registrasi_pmb", "8"));
		rows.appendChild(
				createRowNilai("Jumlah digit penambahan nomor otomatis pada nomor ujian penerimaan mahasiswa baru",
						"jumlah_increments_no_ujian_pmb", "8"));

		rows.appendChild(createRowNilai("Informasi login calon mahasiswa", "info_login_calon_mahasiswa_baru_lagi",
				"Untuk dapat melakukan login, silahkan masukkan Nomor Registrasi yang anda dapatkan pada saat melakukan pendaftaran dan masukkan TANGGAL LAHIR."));

		rows.appendChild(createRowNilai("Informasi mahasiswa dinyatakan lulus", "informasi_telah_lulus",
				"Silahkan melakukan daftar ulang dengan melakukan pembayaran di bank dengan menunjukkan nomor ujian Anda."));

		rows.appendChild(createRowNilai("Informasi kelulusan NIM ke mahasiswa baru", "informasi_kelulusan",
				"NIM Anda [nim], nim ini bisa Anda gunakan untuk login ke http://ecampus dengan username NIM password NIM.",
				3, null));

		rows.appendChild(createRowNilai("Informasi tambahan kelulusan ke mahasiswa baru",
				"informasi_kelulusan_tambahan",
				"Jika Anda belum melakukan pembayaran, silahkan lakukan pembayaran di ....(tanya ke akademik);Kode pembayaran dapat dilihat di ....(tanya ke akademik)",
				3, null));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("File Alur Pendaftaran Penerimaan Mahasiswa Baru"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_ALUR_REGISTRASI_PMB,
				LampiranLain.ALUR_REGISTRASI_PMB, "Alur", false, null);
		hbox.setParent(groupbox);

		// -------------------------------------------------------------------------
		// 1. FILE PANDUAN PENDAFTARAN LENGKAP
		// -------------------------------------------------------------------------
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("File Panduan Pendaftaran Lengkap")));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_ALUR_REGISTRASI_PMB, "PANDUAN_PMB",
				Common.getBahasaConfig("Panduan"), false, null);
		hbox.setParent(groupbox);

		// -------------------------------------------------------------------------
		// 2. FILE PERSYARATAN KELENGKAPAN BERKAS
		// -------------------------------------------------------------------------
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("File Persyaratan Kelengkapan Berkas")));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_ALUR_REGISTRASI_PMB, "BERKAS_PMB",
				Common.getBahasaConfig("Berkas"), false, null);
		hbox.setParent(groupbox);

		// -------------------------------------------------------------------------
		// 3. FILE INFORMASI BIAYA PENDIDIKAN
		// -------------------------------------------------------------------------
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("File Informasi Biaya Pendidikan")));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_ALUR_REGISTRASI_PMB, "BIAYA_PMB",
				Common.getBahasaConfig("Biaya"), false, null);
		hbox.setParent(groupbox);

		// -------------------------------------------------------------------------
		// 4. FILE TANYA JAWAB (FAQ)
		// -------------------------------------------------------------------------
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("File Tanya Jawab (FAQ)")));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_ALUR_REGISTRASI_PMB, "FAQ_PMB",
				Common.getBahasaConfig("FAQ"), false, null);
		hbox.setParent(groupbox);


		// -------------------------------------------------------------------------
		// DEFAULT TEKS PUSAT INFORMASI PMB
		// Teks ini dipakai sebagai pengganti otomatis jika file panduan/berkas/biaya/FAQ belum diunggah.
		// Tetap dapat diubah oleh admin melalui Tab SPMB.
		// -------------------------------------------------------------------------
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Teks Default Panduan Pendaftaran Lengkap PMB"),
				"pmb_footer_panduan_default_text", defaultPanduanPendaftaranPmbFooter(), 10, null));
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Teks Default Persyaratan Kelengkapan Berkas PMB"),
				"pmb_footer_berkas_default_text", defaultPersyaratanBerkasPmbFooter(), 7, null));
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Teks Default Informasi Biaya Pendidikan PMB"),
				"pmb_footer_biaya_default_text", defaultBiayaPendidikanPmbFooter(), 7, null));
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Teks Default Tanya Jawab PMB"),
				"pmb_footer_faq_default_text", defaultFaqPmbFooter(), 7, null));

		// Konfigurasi Deskripsi Pengenalan PMB (Versi Formal, Rinci, dan Detail)
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Informasi Pengenalan / Deskripsi PMB"),
				"informasi_pengenalan_pmb",
				"Sistem Informasi Penerimaan Mahasiswa Baru (PMB) merupakan instrumen strategis institusi dalam menjaring talenta akademik terbaik untuk bergabung dalam komunitas intelektual kami. Kami berkomitmen penuh untuk menyelenggarakan proses seleksi yang transparan, akuntabel, dan inklusif. Fokus utama kami adalah membentuk generasi penerus bangsa yang unggul, adaptif, dan inovatif, serta didasari oleh integritas moral yang luhur guna memberikan kontribusi nyata bagi kemajuan bangsa dan negara.",
				5, null));
		// -------------------------------------------------------------------------
		// TAUTAN MEDIA SOSIAL INSTITUSI
		// -------------------------------------------------------------------------
		rows.appendChild(createRowNilai(Common.getBahasaConfig("Tautan Media Sosial Facebook"), "link_facebook",
				"https://facebook.com/..."));

		rows.appendChild(createRowNilai(Common.getBahasaConfig("Tautan Media Sosial Twitter / X"), "link_twitter",
				"https://twitter.com/..."));

		rows.appendChild(createRowNilai(Common.getBahasaConfig("Tautan Media Sosial Instagram"), "link_instagram",
				"https://instagram.com/..."));

		rows.appendChild(createRowNilai(Common.getBahasaConfig("Tautan Media Sosial YouTube"), "link_youtube",
				"https://youtube.com/c/..."));

		rows.appendChild(createRowNilai("Informasi Registrasi PMB berhasil", "informasi_registrasi_pmb_berhasil_login",
				"Registrasi berhasil dilakukan. Nomor registrasi : [no_reg]. Nomor registrasi ini digunakan untuk proses login. Silahkan catat nomor pendaftaran tersebut dan selanjutnya klik tombol Login.",
				5, null));

		rows.appendChild(createRowActiveDefault("Terintegrasi dengan Feeder PMB Kementerian Pertanian",
				"integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Username Feeder PMB Kementerian Pertanian", "pmb_arkatama_host_url",
				"https://pmb.pusdiktan.id"));

		rows.appendChild(createRowNilai("Username Feeder PMB Kementerian Pertanian", "pmb_arkatama_username", ""));
		rows.appendChild(
				createRowNilaiPassword("Password Feeder PMB Kementerian Pertanian", "pmb_arkatama_password", ""));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Singkonkan data referensi"));

		MyButtonConfig button;
		groupbox.appendChild(
				button = new MyButtonConfig("Singkonkan data referensi http://feeder-pmb.arkatama.id Sekarang",
						"/img/Actions-view-media-equalizer-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PmbArkatama.synRef();
			}
		});
	}


	/**
	 * Konfigurasi dashboard dan statistik ditempatkan pada tab khusus agar tidak
	 * bercampur dengan konfigurasi umum. Key ini dipakai oleh beberapa dashboard
	 * besar untuk menentukan role yang boleh melihat data lintas unit.
	 */
	protected void initTabDashboardStatistik() {
		Rows rows = createSpan("Dashboard & Statistik");

		createSpan("Hak Akses Dashboard, Statistik, dan Audit", rows);
		rows.appendChild(createRowNilai(
				"Role yang boleh melihat semua statistik/data dashboard lintas unit, pisahkan dengan koma",
				"daftar_hak_akses_yg_bisa_lihat_statistik_data", "am,admfak,admprd,Akademik,amp"));
		rows.appendChild(createRowNilai(
				"Role/User ID yang boleh melihat riwayat revisi data. Pisahkan dengan koma, contoh: am,amp",
				"boleh_lihat_revisi", "am,amp"));

		createSpan("Performa Tampilan Dashboard", rows);
		rows.appendChild(createRowActiveDefault(
				"Tampilkan loading/progress saat dashboard besar mengambil data",
				"dashboard_tampilkan_loading_progress", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Jumlah data per halaman pada popup/detail dashboard",
				"dashboard_default_page_size", "10"));
		rows.appendChild(createRowActiveDefault(
				"Aktifkan debug ringkas dashboard hanya untuk admin",
				"dashboard_debug_admin", Konfigurasi.TIDAK_AKTIF));
	}

	/**
	 * Konfigurasi startup, cache, dan index ditempatkan pada tab sendiri karena
	 * sifatnya teknis dan berpengaruh ke performa/memory aplikasi.
	 */
	protected void initTabStartupCacheIndex() {
		Rows rows = createSpan("Startup, Cache & Index");

		createSpan("Startup Aplikasi", rows);
		rows.appendChild(createRowActiveDefault("Jalankan pemeriksaan/index database saat startup",
				"jalankan_init_index_saat_startup", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jalankan maintenance startup di background thread",
				"jalankan_maintenance_startup_background", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan log debug startup/cache/index hanya untuk admin",
				"debug_startup_cache_index", Konfigurasi.TIDAK_AKTIF));

		createSpan("Cache Data Master (EhCache)", rows);
		rows.appendChild(createRowNilai("Jumlah entry cache memory (legacy, tidak dipakai EhCache)",
				"mapdb_cache_entries", "65536"));
		rows.appendChild(createRowNilai("Ukuran cache legacy / size_map (tidak dipakai EhCache)",
				"size_map", "50"));
		rows.appendChild(createRowActiveDefault(
				"Aktifkan fallback ambil data dari database jika object tidak ditemukan di cache",
				"cache_fallback_ke_database_jika_tidak_ada", Konfigurasi.AKTIF));

		createSpan("Absensi dan Kehadiran", rows);
		rows.appendChild(createRowNilai("Tanggal mulai absensi default",
				"tanggal_mulai_absensi", "1"));
		rows.appendChild(createRowNilai("Hari default tidak aktif/libur absensi, contoh ,1,7,",
				"hari_default_tidak_aktif", ",1,7,"));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanCuti() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Cuti"));

		rows.appendChild(
				createRowActiveTahunAkademikSemester("Mahasiswa bisa mengajukan cuti", "aktivasi_cuti", false));

		rows.appendChild(createRowNilai("Mahasiswa bisa melakukan cuti minimal di semester",
				"mahasiswa_bisa_cuti_minimal_di_semester", "1"));

		rows.appendChild(createRowActiveDefault("Apakah mahasiswa bisa melakukan cuti berturut turut?",
				"mahasiswa_bisa_melakukan_cuti_berturut_turut", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(
				createRowActiveDefault("Apakah mahasiswa bisa melakukan cuti jika sudah membayar daftar ulang?",
						"mahasiswa_bisa_melakukan_cuti_jika_sudah_bayar_daftar_ulang", Konfigurasi.TIDAK_AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabKelulusanWisuda() {
		Rows rows = null;
		rows = (createSpan("Kelulusan/Wisuda"));

		rows.appendChild(createRowNilai("Syarat wisuda administrasi perguruan tinggi", "wisuda_administrasi",
				"Transkrip Akademik;Biaya Perkuliahan;Biaya Wisuda;TandaLulus Ujian Komprehensive;Propesa;Lembar Pengesahan Skripsi;Tanda Lulus Toafl/Toefl;Pas Photo;Administrasi",
				4, null, null));

		rows.appendChild(createRowNilai("Syarat wisuda administrasi fakultas", "wisuda_administrasi_fakultas",
				"Kartu Hasil Studi;Tanda Lulus Ujian;Lembar Pengesahan Skripsi;Tanda Lulus Toafl/Toefl;Pas Photo;Administrasi Fakultas",
				4, null, null));

		rows.appendChild(createRowNilai("Syarat wisuda keuangan", "wisuda_keuangan",
				"Melunasi Biaya Kelulusan;Melunasi Biaya UKT;Melunasi Biaya Ujian", 4, null, null));

		rows.appendChild(createRowNilai("Syarat wisuda perpustakkan perguruan tinggi", "wisuda_perpustakaan",
				"Mengembalikan semua buku perpustakaan;Tidak ada denda pengembalian buku", 4, null, null));

		rows.appendChild(createRowNilai("Syarat wisuda perpustakkan perpustakaan", "wisuda_perpustakaan_perpustakaan",
				"Mengembalikan semua buku perpustakaan;Tidak ada denda pengembalian buku", 4, null, null));

		rows.appendChild(
				createRowActive("File pdf dan cover wajib diupoad saat mengajukan sidang skripsi / tugas akhir",
						"file_pdf_dan_cover_skripsi_wajib_diupload"));

		// ------------ Wisuda

		rows.appendChild(createRowNilai("Kode Item biaya yang harus dibayar mahasiswa sebelum bisa mengajukan wisuda",
				"kode_item_biaya_wisuda", ""));

		rows.appendChild(createRowActive("Mahasiswa harus melunasi biaya semester sebelum dapat mengajukan wisuda",
				"mahasiswa_harus_lunas_sebelum_wisuda"));

		rows.appendChild(createRowNilai("Batas terendah persen pembayaran semester sebelum dapat mengajukan wisuda",
				"batas_terendah_persen_pembayaran_sebelum_wisuda", "90"));

		rows.appendChild(createRowActive("Pengajuan wisuda harus telah mengembalikan semua buku perpustakaan",
				"pengajuan_wisuda_harus_telah_mengembalikan_buku_perpustakaan"));

		rows.appendChild(createRowActive("Data skripsi / tugas akhir harus di-input sebelum daftar wisuda",
				"data_skripsi_harus_diinput_sebelum_daftar_wisuda"));

		rows.appendChild(createRowActive("Tracer study harus di-input sebelum daftar wisuda",
				"saat_pendaftaran_wisuda_harus_mengisi_tracer"));

		// ------

		initModulPerpustakaan();
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanNotifikasiWa() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Notifikasi WA"));
		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA", "aktifkan_reply_chatbot", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA saat pembayaran berhasil dilakukan",
				"aktifkan_kirim_notif_pembayaran_ke_wa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Aktifkan notifikasi WA saat peserta didik baru (mahasiswa/siswa baru) berhasil daftar",
				"aktifkan_kirim_notif_daftar_peserta_didik_baru_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Aktifkan notifikasi WA saat peserta didik baru (mahasiswa/siswa baru) telah siap meakukan interview",
				"aktifkan_kirim_notif_interview_calon_mahasiswa_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Aktifkan notifikasi WA saat peserta didik baru (mahasiswa/siswa baru) dinyatakan diterima",
				"aktifkan_kirim_notif_diterima_peserta_didik_baru_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA disposisi surat",
				"aktifkan_kirim_notif_surat_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA peminjaman buku perpustakaan",
				"aktifkan_kirim_notif_pinjam_buku_perpustakaan_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA pengembalian buku perpustakaan",
				"aktifkan_kirim_notif_pengembalian_buku_perpustakaan_ke_wa", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan notifikasi WA saat pengiriman disposisi SOP",
				"aktifkan_kirim_notif_disposisi_sop_ke_wa", Konfigurasi.AKTIF));
	}

	/**
	 * Pengaturan terpusat untuk modul Document Management System (DMS).
	 *
	 * Modul DMS memakai Akreditasi sebagai folder/root utama dan
	 * DokumenAkreditasi sebagai folder/file child. Konfigurasi pada tab ini
	 * mengumpulkan pengaturan yang sebelumnya tersebar di menu umum, DSpace,
	 * dan front-end /WEB-INF/baru/modul/dms agar lebih mudah ditemukan.
	 */
	@SuppressWarnings("unchecked")
	protected void initTabManajemenDokumenDms() {
		Rows rows = createSpan("Manajemen Dokumen DMS");

		createSpan("Pengaturan Umum DMS", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan modul manajemen dokumen / Document Management System",
				"dms_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan tampilan Explorer untuk pengelolaan folder dan file dokumen",
				"dms_tampilan_explorer", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tree folder dokumen di sisi kiri halaman pengelolaan",
				"dms_tampilkan_tree_folder", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah dokumen per halaman pada daftar DMS",
				"dms_jumlah_data_per_halaman", "25"));
		rows.appendChild(createRowNilai("Urutan default daftar dokumen DMS",
				"dms_default_sorting", "nomorUrut,kode,nama"));
		rows.appendChild(createRowNilai("Catatan internal konfigurasi DMS",
				"dms_catatan_konfigurasi",
				"Akreditasi digunakan sebagai folder/root dokumen utama, sedangkan DokumenAkreditasi digunakan sebagai folder/file child melalui kolom induk. Lampiran file tetap dikelola melalui LampiranLain.",
				4, null));

		createSpan("Jenis, Tingkat, dan Klasifikasi Dokumen", rows);
		rows.appendChild(createRowNilai("Jenis dokumen DMS tambahan. Pisahkan dengan koma, titik koma, pipe, atau baris baru",
				"jenis_dokumen_dms_tambahan",
				"", 6, null));
		rows.appendChild(createRowNilai("Label default tipe dokumen umum",
				"dms_label_tipe_dokumen_umum", "Dokumen"));
		rows.appendChild(createRowNilai("Pilihan tingkat dokumen tambahan. Pisahkan dengan koma/titik koma jika diperlukan",
				"dms_tingkat_dokumen_tambahan", ""));
		rows.appendChild(createRowNilai("Pilihan lingkup dokumen tambahan. Pisahkan dengan koma/titik koma jika diperlukan",
				"dms_lingkup_dokumen_tambahan", ""));
		rows.appendChild(createRowActiveDefault("Tampilkan dokumen nonaktif pada halaman admin DMS",
				"dms_admin_tampilkan_dokumen_nonaktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan dokumen tanpa lampiran pada halaman publik/front-end DMS",
				"dms_frontend_tampilkan_dokumen_tanpa_lampiran", Konfigurasi.AKTIF));

		createSpan("Pengaturan Front-End DMS", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan halaman front-end DMS",
				"dms_frontend_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Judul halaman front-end DMS",
				"dms_frontend_judul", "Pusat Dokumen"));
		rows.appendChild(createRowNilai("Subjudul halaman front-end DMS",
				"dms_frontend_subjudul",
				"Temukan dokumen perguruan tinggi, yayasan, unit kerja, sertifikasi, akreditasi, audit, dan dokumen pendukung lainnya.",
				3, null));
		rows.appendChild(createRowNilai("Teks bantuan pencarian dokumen",
				"dms_frontend_placeholder_pencarian", "Cari nama dokumen, lembaga, tahun, prodi, unit, atau kata kunci lainnya"));
		rows.appendChild(createRowActiveDefault("Tampilkan filter jenis dokumen di front-end DMS",
				"dms_frontend_tampilkan_filter_jenis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan filter tingkat dokumen di front-end DMS",
				"dms_frontend_tampilkan_filter_tingkat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan filter lingkup dokumen di front-end DMS",
				"dms_frontend_tampilkan_filter_lingkup", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan filter tahun dokumen di front-end DMS",
				"dms_frontend_tampilkan_filter_tahun", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan filter satuan kerja/unit di front-end DMS",
				"dms_frontend_tampilkan_filter_satuan_kerja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Izinkan download dokumen dari halaman front-end DMS",
				"dms_frontend_izinkan_download", Konfigurasi.AKTIF));

		createSpan("Upload, Lampiran, dan File", rows);
		rows.appendChild(createRowActiveDefault("Pengguna boleh upload file langsung ke sistem",
				"boleh_upload_file_langsung", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Ukuran maksimal file DMS yang direkomendasikan dalam MB",
				"dms_upload_maksimal_mb", "25"));
		rows.appendChild(createRowNilai("Ekstensi file DMS yang diperbolehkan. Kosongkan jika mengikuti validasi upload global",
				"dms_upload_ekstensi_diperbolehkan", "pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,zip,rar",
				3, null));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol upload/download LampiranLain pada daftar dokumen",
				"dms_tampilkan_upload_download_lampiran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan riwayat/revisi dokumen pada DMS",
				"dms_tampilkan_riwayat_revisi", Konfigurasi.AKTIF));

		createSpan("Hak Akses DMS", rows);
		rows.appendChild(createRowNilai("Kode role yang boleh mengelola semua dokumen DMS. Pisahkan dengan koma",
				"dms_role_boleh_mengelola_semua", "am,admfak,admprd,Akademik"));
		rows.appendChild(createRowNilai("Kode role yang boleh melihat semua dokumen DMS. Kosongkan jika mengikuti filter bawaan",
				"dms_role_boleh_melihat_semua", ""));
		rows.appendChild(createRowNilai("Kode grup pengguna default yang boleh melihat dokumen tertentu",
				"dms_kode_grup_pengguna_default", ""));
		rows.appendChild(createRowActiveDefault("Dokumen publik tetap dapat dilihat tanpa login",
				"dms_frontend_publik_tanpa_login", Konfigurasi.TIDAK_AKTIF));

		createSpan("Integrasi DSpace untuk Dokumen", rows);
		rows.appendChild(createRowActiveDefault("Sistem terhubung ke DSpace secara global",
				"terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dokumen DMS terhubung dan dapat diekspor ke DSpace",
				"dokumen_terhubung_ke_dspace", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Ekspor DMS hanya mengirim dokumen yang memiliki lampiran file",
				"dms_dspace_hanya_dokumen_berlampiran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Saat ekspor DMS, gunakan nama instansi sebagai publisher metadata DSpace",
				"dms_dspace_gunakan_label_universitas", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Nama instansi/publisher untuk metadata dokumen",
				"label_universitas", "Nama Instansi"));
		rows.appendChild(createRowNilai("Catatan template copyright metadata DSpace dokumen",
				"dms_dspace_template_copyright",
				"Semua hak cipta dilindungi oleh {label_universitas}", 2, null));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanDspace() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Dspace"));
		rows.appendChild(createRowActiveDefault("Terhubung ke dspace", "terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Artikel Terhubung ke dspace", "artikel_terhubung_ke_dspace",
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Penunjang Kegiatan Dosen Terhubung ke dspace",
				"penunjang_kegiatan_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Kegiatan Dosen Terhubung ke dspace",
				"kegiatan_dosen_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Kegiatan Kemahasiwaan Terhubung ke dspace",
				"kegiatan_mahasiswa_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Saldo Awal Pustaka Terhubung ke dspace",
				"saldo_awal_pustaka_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Item / Buku Pustaka Terhubung ke dspace",
				"item_pustaka_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Penelitian dan Pengabdian Terhubung ke dspace",
				"penelitian_dan_pengabdian_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Buku bahan ajar Terhubung ke dspace", "bahan_ajar_terhubung_ke_dspace",
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Karya Dosen Terhubung ke dspace", "karya_dosen_terhubung_ke_dspace",
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Karya Mahasiswa Terhubung ke dspace",
				"karya_mahasiswa_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Pertemuan Elearning Terhubung ke dspace",
				"pertemuan_elearning_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Prestasi Dosen Terhubung ke dspace",
				"prestasi_dosen_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Prestasi Mahasiswa Terhubung ke dspace",
				"prestasi_mahasiswa_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Tugas Akhir / Skripsi / Thesis Mahasiswa Terhubung ke dspace",
				"ta_skripsi_mahasiswa_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dokumen Terhubung ke dspace", "dokumen_terhubung_ke_dspace",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Biodata Dosen Terhubung ke dspace",
				"biodata_dosen_terhubung_ke_dspace", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Export artikel ke dspace berdasarkan tahun",
				"export_artikel_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Export buku ke dspace berdasarkan tahun",
				"export_buku_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Export skripsi/TA/Tesis ke dspace berdasarkan tahun",
				"export_skripsi_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Export penelitian dan pengabdian ke dspace berdasarkan tahun",
				"export_penelitian_dan_pengabdian_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Alamat Publik URL Dspace", "dspace_url", ConstantValues.DSPACE_URL_PUBLIK, 1,
				null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Konfigurasi konfigurasi = (Konfigurasi) arg0.getData();
						ConstantValues.DSPACE_URL_PUBLIK = konfigurasi.getNilai();
					}
				}));
		rows.appendChild(createRowNilai("Alamat Private URL Dspace", "dspace_private_url",
				ConstantValues.DSPACE_URL_PRIVATE, 1, null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Konfigurasi konfigurasi = (Konfigurasi) arg0.getData();
						ConstantValues.DSPACE_URL_PRIVATE = konfigurasi.getNilai();
					}
				}));
		rows.appendChild(createRowNilai("Username Dspace", "dspace_username", ""));
		rows.appendChild(createRowNilaiPassword("Username Password", "dspace_password", ""));
		rows.appendChild(
				createRowNilai("Dspace Antarmuka", "dspace_ui", ConstantValues.DSPACE_UI, 1, null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Konfigurasi konfigurasi = (Konfigurasi) arg0.getData();
						ConstantValues.DSPACE_UI = konfigurasi.getNilai();
					}
				}));

		rows.appendChild(createRowActiveDefault("Jadikan " + Common.getBahasaConfig("Fakultas") + " sebagai root ?",
				"dpsace_jadikan_fakultas_sebagai_root", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Jadikan " + Common.getBahasaConfig("Jurusan") + " sebagai root ?",
				"dpsace_jadikan_jurusan_sebagai_root", Konfigurasi.TIDAK_AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPengaturanEmail() {
		Rows rows = null;
		rows = (createSpan("Pengaturan Email"));
		rows.appendChild(
				createRowActiveDefault("Aktfikan pengiriman email", "aktfikan_pengiriman_email", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktfikan pengiriman notif/mobile", "aktfikan_pengiriman_notif",
				Konfigurasi.AKTIF));

//		rows.appendChild(createRowActiveDefault("Pengiriman email diteruskan lewat URL ?",
//				"aktifkan_email_diteruskan_menggunakan_url_baru", Konfigurasi.AKTIF));
//		rows.appendChild(createRowNilai("Alamat URL jika email diteruskan via URL", "email_menggunakan_alamat_url",
//				"http://mail.ecampus.id:8080/mailsender/send/index"));

		rows.appendChild(createRowNilai("Protocol yang digunakan ?", "default_mail_protocol", "smtp"));
		rows.appendChild(createRowNilai("Apa alamat SMTP ?", "default_mailhost", "smtp.gmail.com"));
		rows.appendChild(createRowNilai("Apakah menggunakan autentikasi ?", "default_mail_auth", "true"));
		rows.appendChild(createRowNilai("Apakah menggunakan SSL ?", "mail.smtp.ssl.enable", ""));

		rows.appendChild(createRowNilai("SMTP Port ?", "default_mail_port", "465"));
		rows.appendChild(createRowNilai("SMTP Soket Port ?", "default_mail_soket_port", "465"));
		rows.appendChild(
				createRowNilai("SMTP Soket Class ?", "default_mail_soket_class", "javax.net.ssl.SSLSocketFactory"));
		rows.appendChild(createRowNilai("SMTP fallback ?", "default_mail_soket_fallback", "false"));
		rows.appendChild(createRowNilai("SMTP quitwait ?", "default_mail_soket_quitwait", "false"));

		rows.appendChild(createRowNilai("SMTP starttls ?", "mail.smtp.starttls.enable", ""));

		rows.appendChild(createRowNilai("SMTP ssl protocols ?", "mail.smtp.ssl.protocols", ""));

		rows.appendChild(createRowNilai("Username", "default_email_username", "zishof"));
		rows.appendChild(createRowNilaiPassword("Password", "default_email_password", "zishof"));
		rows.appendChild(createRowNilai("Email pengirim", "default_email", "info@zishof.com"));
		rows.appendChild(
				createRowActiveDefault("Aktfikan Debug Pengiriman Email", "mail_debug", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Uji coba pengiriman Email", "default_test_email", "info@test.com"));

		rows.appendChild(createRowActiveDefault("Aktfikan pengiriman email menggunakan sendinblue.com",
				"aktfikan_pengiriman_email_menggunakan_sendinblue.com", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Key sendinblue.com jika diaktifkan", "key_sendinblue.com", ""));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Uji coba kirim email"));
		groupbox.appendChild(button = new MyButtonConfig("Coba Kirimkan Email Sekarang", "/img/sent.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				final PrintStream ps = new PrintStream(os);
				File file = new File("/opt/test_kirim_email.txt");
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				FileUtils.writeStringToFile(file, "Uji Coba Pengiriman Email dari " + perguruanTinggi.getNama());
				JSONArray userIds = new JSONArray();
				userIds.put(Common.getCurrentUser().getUserId());

				MailSender.sendMailLampiranAll(userIds, "Uji Coba Pengiriman Email dari " + perguruanTinggi.getNama(),
						"Uji Coba Pengiriman Email dari " + perguruanTinggi.getNama(),
						Common.getKonfigurasi("default_email", "info@zishof.com").getNilai(),
						Common.getKonfigurasi("default_test_email", "info@test.com").getNilai(), ps, perguruanTinggi,
						file);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show(os.toString(), "Hasil Pengiriman Email", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						ps.close();
						os.close();
					}
				}, "Harap tunggu....", false, 5000);
			}
		});

		rows.appendChild(createRowActiveDefault("Aktifkan pengiriman semua fitur akademik",
				"aktfikan_pengiriman_email_akademik", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Aktifkan pengiriman fitur broadcast surat tagihan",
				"aktfikan_pengiriman_email_tagihan", Konfigurasi.AKTIF));

		// FINPAY
	}

	@SuppressWarnings("unchecked")
	protected void initTabPaymentGateway() {
		Rows rows = null;
		rows = (createSpan("Payment Gateway"));

		createSpan("Dashboard Tagihan dan Pembayaran", rows);
		rows.appendChild(createRowActiveDefault("Pisahkan nilai tagihan minus sebagai bantuan, beasiswa, potongan, atau koreksi agar tidak tampil sebagai tagihan terbesar",
				"pembayaran_dashboard_pisahkan_tagihan_minus", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan kartu Bantuan/Potongan pada dashboard pembayaran jika ada tagihan bernilai minus",
				"pembayaran_dashboard_tampilkan_bantuan_potongan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan chart HTML/CSS ringan pada dashboard pembayaran, bukan JFreeChart",
				"pembayaran_dashboard_chart_html_css", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan progress saat tagihan dan riwayat pembayaran sedang dimuat",
				"pembayaran_tampilkan_progress_loading", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sembunyikan progress otomatis setelah proses pembayaran/tagihan selesai 100%",
				"pembayaran_progress_hide_setelah_selesai", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jumlah data per halaman pada grid riwayat pembayaran",
				"pembayaran_grid_page_size", "10"));
		rows.appendChild(createRowNilai("Catatan dashboard pembayaran",
				"catatan_dashboard_pembayaran",
				"Nilai minus seperti beasiswa, bantuan pemerintah, bantuan swasta, potongan, atau koreksi tidak diperlakukan sebagai tagihan terbesar. Dashboard menampilkan nilai minus sebagai pengurang agar petugas tidak keliru membaca prioritas pembayaran.",
				4, null));

		createSpan("Keamanan dan Validasi Payment Gateway", rows);
		rows.appendChild(createRowActiveDefault("Tolak pembuatan VA atau transaksi online jika total tagihan yang dipilih bernilai nol atau minus",
				"payment_gateway_tolak_total_nol_atau_minus", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cegah pembayaran ganda jika tagihan sudah memiliki pembayaran/VA aktif",
				"payment_gateway_cegah_double_payment", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan pengecekan status VA sebelum membuat VA baru",
				"payment_gateway_cek_va_aktif_sebelum_buat_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Catat request dan response payment gateway ke ErrorLog jika response tidak valid atau bukan JSON",
				"payment_gateway_catat_response_tidak_valid", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Timeout request payment gateway dalam detik",
				"payment_gateway_timeout_detik", "60"));
		rows.appendChild(createRowNilai("Catatan validasi payment gateway",
				"catatan_validasi_payment_gateway",
				"Transaksi online hanya dibuat jika total akhir yang harus dibayar masih bernilai positif. Jika item yang dipilih hanya berisi bantuan, beasiswa, atau potongan minus, sistem memberi peringatan agar tidak membuat VA dengan nominal tidak valid.",
				4, null));

		createSpan("Integrasi FinPay", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via finpay ?", "aktifkan_pembayaran_via_finpay",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Kode Merchant", "finpay_merchant_id", "AK444"));
		rows.appendChild(createRowNilaiPassword("Password Merchant", "finpay_password_merchant", "ak2016"));
		rows.appendChild(createRowNilai("Gateway URL", "new_finpay_gateway_url",
				"https://sandbox.finpay.co.id/servicescode/api/apiFinpay.php"));
		rows.appendChild(createRowNilai("Check Status Payment Code URL", "finpay_check_ctatus_payment_code_url",
				"https://sandbox.finpay.co.id/servicescode/check-status-ex.php"));
		rows.appendChild(createRowNilai("Cancel Transaction URL", "finpay_cancel_transaction_url",
				"https://sandbox.finpay.co.id/servicescode/cancel-transaction-ex.php"));
		rows.appendChild(createRowNilai("Change Amount URL", "finpay_change_amount_url",
				"https://sandbox.finpay.co.id/servicescode/change-transaction.php"));
		rows.appendChild(createRowNilai("Timeout", "finpay_timeout", "30"));
		rows.appendChild(createRowNilai("Path URL response", "finpay_path_url_response", "/FinPayResponse"));
		rows.appendChild(createRowNilai("Informasi Channel Pembayaran", "finpay_payment_info",
				"http://portalfinpay.com/index.php/bank"));
		// FINPAY

		// IPAYMU
		createSpan("Integrasi iPaymu", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via iPaymu ?", "aktifkan_pembayaran_via_ipaymu",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilaiPassword("iPaymu key", "ipaymu_key", ""));
		rows.appendChild(createRowNilai("Gateway URL", "ipaymu_gateway_url", "https://my.ipaymu.com/payment.htm"));
		rows.appendChild(
				createRowNilai("Check URL", "ipaymu_cek_transaksi_url", "https://my.ipaymu.com/api/CekTransaksi.php"));

		rows.appendChild(createRowNilai("Path URL response", "ipaymu_path_url_response", "/IPayMuResponse"));
		// IPAYMU

		// FASPAY
		createSpan("Integrasi Faspay", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Faspay ?", "aktifkan_pembayaran_via_faspay",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Label tombol pembayaran via Faspay", "label_pembayaran_via_faspay",
				"Bayar Via Faspay"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Faspay"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_FASPAY,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_FASPAY_STR, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_FASPAY_STR,
				false, null);
		hbox.setParent(groupbox);
		rows.appendChild(createRowNilai("Faspay merchant id", "faspay_merchant_id", "31503"));
		rows.appendChild(createRowNilai("Faspay nama merchant", "faspay_merchant_name", "eCampus"));
		rows.appendChild(createRowNilai("Faspay user id", "faspay_user_id", ""));
		rows.appendChild(createRowNilaiPassword("Faspay password", "faspay_password", ""));

		rows.appendChild(createRowNilai("Faspay Payment Channel URL", "faspay_payment_channel_url",
				"http://faspaydev.mediaindonusa.com/pws/100001/182xx00010100000"));
		rows.appendChild(createRowNilai("Faspay Post URL", "faspay_gateway_url",
				"http://faspaydev.mediaindonusa.com/pws/300002/183xx00010100000"));
		rows.appendChild(createRowNilai("Faspay Redirect URL", "faspay_redirect_url",
				"http://faspaydev.mediaindonusa.com/pws/100003/0830000010100000"));
		rows.appendChild(createRowNilai("Faspay Check Transaksi URL", "faspay_check_status_url",
				"http://faspaydev.mediaindonusa.com/pws/100004/183xx00010100000"));
		rows.appendChild(createRowNilai("Faspay biaya administrasi", "faspay_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Kode akun faspay", "kode_akun_faspay", ""));
		rows.appendChild(
				createRowNilai("Kode akun faspay untuk biaya administrasi", "kode_akun_faspay_biaya_administrasi", ""));

		rows.appendChild(createRowNilai("Faspay biaya payment gateway", "faspay_biaya_payment_gateway", "0.0"));
		rows.appendChild(createRowNilai("Kode akun faspay untuk biaya payment gateway",
				"kode_akun_faspay_biaya_payment_gateway", ""));
		// FASPAY

		// Jatelindo
		createSpan("Integrasi Jatelindo", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Jatelindo ?",
				"aktifkan_pembayaran_via_jatelindo", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Label tombol pembayaran via Jatelindo", "label_pembayaran_via_jatelindo",
				"Bayar Via Mandiri"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Jatelindo"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO_STR,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO_STR, false, null);
		hbox.setParent(groupbox);
		rows.appendChild(createRowNilai("Jatelindo Kode Mitra", "jatelindo_merchant_id", "31503"));
		rows.appendChild(createRowNilai("Jatelindo No Rekening Alias", "jatelindo_no_rek_alias", "ecamp"));
		rows.appendChild(createRowNilai("Jatelindo biaya administrasi", "jatelindo_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Kode akun jatelindo", "kode_akun_jatelindo", ""));
		rows.appendChild(createRowNilai("Kode akun jatelindo untuk biaya administrasi",
				"kode_akun_jatelindo_biaya_administrasi", ""));

		rows.appendChild(createRowNilai("Jatelindo biaya payment gateway", "jatelindo_biaya_payment_gateway", "0.0"));
		rows.appendChild(createRowNilai("Kode akun jatelindo untuk biaya payment gateway",
				"kode_akun_jatelindo_biaya_payment_gateway", ""));
		// Jatelindo

		// CIMB NIAGA
		createSpan("Integrasi CIMB Niaga", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Cimb ?", "aktifkan_pembayaran_via_cimb",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Label tombol pembayaran via Cimb", "label_pembayaran_via_cimb",
				"Bayar Via Cimb Niaga"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Cimb"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB_STR, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB_STR, false,
				null);
		hbox.setParent(groupbox);

		// CIMB NIAGA

		// BNI
		createSpan("Integrasi Bni", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Bni ?", "aktifkan_pembayaran_via_bni",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Label tombol pembayaran via Bni", "label_pembayaran_via_bni", "Bayar Via Bni"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Bni"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BNI,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BNI_STR, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BNI_STR, false,
				null);
		hbox.setParent(groupbox);
		rows.appendChild(createRowNilai("Client ID", "bni_merchant_id", "000"));
		rows.appendChild(createRowNilaiPassword("Bni password", "bni_password", ""));

		rows.appendChild(createRowNilai("Bni Post URL", "bni_gateway_url", "https://apibeta.bni-ecollection.com/"));
		rows.appendChild(createRowTanggal("Tanggal Berakhir Pembayaran / Expired (Kosongkan jika berlaku 24 jam)",
				"tanggal_terakhir_pembayaran", ""));

		rows.appendChild(createRowNilai("Jumlah jam kadaluwarsa pembayaran", "jam_terakhir_pembayaran", ""));

		rows.appendChild(createRowActiveDefault("Angka va bni menggunakan nim ?", "angka_va_bni_menggunakan_nim",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Angka prefix va bni", "angka_prefix_va_bni", "8"));
		rows.appendChild(createRowNilai("Generated digit angka bni", "generated_angka_digit_bni", "8"));
		rows.appendChild(createRowNilai("BNI biaya administrasi", "bni_biaya_administrasi", "0.0"));

		rows.appendChild(createRowNilai("BNI Client IP", "bni_ip_client", ""));
		rows.appendChild(createRowNilai("Kode akun BNI", "kode_akun_bni", ""));
		rows.appendChild(
				createRowNilai("Kode akun BNI untuk biaya administrasi", "kode_akun_bni_biaya_administrasi", ""));

		rows.appendChild(createRowNilai("BNI biaya payment gateway", "bni_biaya_payment_gateway", "0.0"));
		rows.appendChild(
				createRowNilai("Kode akun bni untuk biaya payment gateway", "kode_akun_bni_biaya_payment_gateway", ""));

		rows.appendChild(createRowActiveDefault("Generate nomor pembayaran saat formulir mahasiswa baru",
				"generate_nomor_pembayaran_saat_formulir_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF));

		// BNI

		// BSI
		createSpan("Integrasi Bsi", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Bsi ?", "aktifkan_pembayaran_via_bsi",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Label tombol pembayaran via Bsi", "label_pembayaran_via_bsi", "Bayar Via Bsi"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Bsi"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BSI,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BSI_STR, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BSI_STR, false,
				null);
		hbox.setParent(groupbox);
		rows.appendChild(createRowNilai("Client ID", "bsi_merchant_id", "000"));
		rows.appendChild(createRowNilaiPassword("Bsi password", "bsi_password", ""));

		rows.appendChild(
				createRowNilai("Bsi Post URL", "bsi_gateway_url", "https://billing-bpi.maja.id/bni/register/"));
		rows.appendChild(createRowTanggal("Tanggal Berakhir Pembayaran / Expired (Kosongkan jika berlaku 24 jam)",
				"tanggal_terakhir_pembayaran", ""));

		rows.appendChild(createRowNilai("Jumlah jam kadaluwarsa pembayaran", "jam_terakhir_pembayaran", ""));

		rows.appendChild(createRowActiveDefault("Angka va bsi menggunakan nim ?", "angka_va_bsi_menggunakan_nim",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowNilai("Angka prefix va bsi", "angka_prefix_va_bsi", "8"));
		rows.appendChild(createRowNilai("Generated digit angka bsi", "generated_angka_digit_bsi", "8"));
		rows.appendChild(createRowNilai("BSI biaya administrasi", "bsi_biaya_administrasi", "0.0"));

		rows.appendChild(createRowNilai("BSI Client IP", "bsi_ip_client", ""));
		rows.appendChild(createRowNilai("Kode akun BSI", "kode_akun_bsi", ""));
		rows.appendChild(
				createRowNilai("Kode akun BSI untuk biaya administrasi", "kode_akun_bsi_biaya_administrasi", ""));

		rows.appendChild(createRowNilai("BSI biaya payment gateway", "bsi_biaya_payment_gateway", "0.0"));
		rows.appendChild(
				createRowNilai("Kode akun bsi untuk biaya payment gateway", "kode_akun_bsi_biaya_payment_gateway", ""));

		rows.appendChild(createRowActiveDefault("Generate nomor pembayaran saat formulir mahasiswa baru",
				"generate_nomor_pembayaran_saat_formulir_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF));

		// Wizard Pembayaran Mahasiswa
		createSpan("Wizard Pembayaran Mahasiswa", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan Wizard Pembayaran Mahasiswa (tombol \"Wizard\" di DaftarUlangMahasiswaLama/BaruAction, "
						+ "Profil Mahasiswa, dan Dasbor Pembayaran Mahasiswa) ?",
				ais.action.master.helper.WizardPembayaranMhsHelper.KONFIGURASI_AKTIF, Konfigurasi.AKTIF));

		// BSI

		// BTN
		createSpan("Integrasi BTN", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via BTN ?", "aktifkan_pembayaran_via_bank_btn",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("BTN secret key", "btn_secret", "1"));
		rows.appendChild(createRowNilai("BTN key", "btn_key", "1"));
		rows.appendChild(createRowNilai("BTN Company ID", "btn_company_id", "1"));
		rows.appendChild(createRowNilai("BTN Kode Institusi", "btn_kode_institusi", "1"));
		rows.appendChild(createRowNilai("BTN Kode Payment", "btn_kode_payment", "1"));
		rows.appendChild(createRowNilai("BTN Gateway URL", "btn_gateway_url", "1"));
		rows.appendChild(createRowNilai("BTN Jumlah Digit yang digenerate", "btn_generated_payment", "10"));

		// BRI
		createSpan("Integrasi Bri", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Bri ?", "aktifkan_pembayaran_via_bri",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowNilai("Label tombol pembayaran via Bri", "label_pembayaran_via_bri", "Bayar Via Bri"));
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo tombol pembayaran via Bri"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BRI,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BRI_STR, LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BRI_STR, false,
				null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Briva Institution Code", "bri_institution_code", "J104408"));
		rows.appendChild(createRowNilai("Briva No", "bri_briva_no", "77777"));

		rows.appendChild(createRowNilai("Client ID", "bri_merchant_id", "000"));
		rows.appendChild(createRowNilaiPassword("CLIENT SECRET", "bri_password", ""));
		rows.appendChild(createRowNilaiPassword("X-BRI-KEY / API KEY", "bri_api_key",
				""));
		rows.appendChild(createRowNilaiPassword("Authorization / Auth Code", "bri_auth_code",
				""));

		rows.appendChild(createRowNilai("Bri Post URL", "bri_gateway_url", "https://developer.bri.co.id/v1/api/briva"));
		rows.appendChild(createRowTanggal("Tanggal Berakhir Pembayaran / Expired (Kosongkan jika berlaku 2 bulan)",
				"tanggal_terakhir_pembayaran_bri", ""));
		// rows.appendChild(createRowActiveDefault("Angka va bri menggunakan nim
		// ?", "angka_va_bri_menggunakan_nim",
		// Konfigurasi.TIDAK_AKTIF));

		// rows.appendChild(createRowNilai("Angka prefix va bri",
		// "angka_prefix_va_bri", "8"));
		// rows.appendChild(createRowNilai("Generated digit angka bri",
		// "generated_angka_digit_bri", "8"));
		rows.appendChild(createRowNilai("BRI biaya administrasi", "bri_biaya_administrasi", "0.0"));

		// rows.appendChild(createRowNilai("BRI Client IP", "bri_ip_client",
		// ""));
		rows.appendChild(createRowNilai("Kode akun BRI", "kode_akun_bri", ""));
		rows.appendChild(
				createRowNilai("Kode akun BRI untuk biaya administrasi", "kode_akun_bri_biaya_administrasi", ""));

		rows.appendChild(createRowNilai("BRI biaya payment gateway", "bri_biaya_payment_gateway", "0.0"));
		rows.appendChild(
				createRowNilai("Kode akun bri untuk biaya payment gateway", "kode_akun_bri_biaya_payment_gateway", ""));

		rows.appendChild(createRowActiveDefault("Generate nomor pembayaran saat formulir mahasiswa baru",
				"generate_nomor_pembayaran_bri_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF));

		// BRI

		// DOKU
		createSpan("Integrasi Doku", rows);
		rows.appendChild(createRowActiveDefault("Aktifkan Pembayaran via Doku ?", "aktifkan_pembayaran_via_doku",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilaiPassword("Shared Key", "doku_key", ""));
		rows.appendChild(createRowNilai("Gateway URL", "doku_gateway_url",
				"https://apps.myshortcart.com/payment/request-payment/"));
		rows.appendChild(createRowNilai("Store ID", "doku_merchant_id", "10444535"));

		// DOKU

		// rows = (createSpan("Sistem"));
		// row = new MyFormRow();
		//		// row.setParent(rows);
		// groupbox.appendChild(new Caption("Re-init data"));
		// MyToolbarbuttonConfig reiit;
		// row.appendChild(reiit = new MyToolbarbuttonConfig("Re-init data",
		// "/img/Check-icon.png"));
		// reiit.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// ConstantValues.reInitDataDiMemory();
		// MyMessageboxConfig.show("Re-init data telah dilakukan", "Info",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// }
		// });
	}

	@SuppressWarnings("unchecked")
	protected void initTabTampilanSistem() {
		Rows rows = null;
		rows = (createSpan("Tampilan Sistem"));

		// BLANK FIX: blok upload Skin membaca LampiranLain (DB streaming) saat render; bila gagal
		// (mis. sesi/streaming), exception dulu menggagalkan SISA tab ini (toggle tak terpasang →
		// tab tampak KOSONG) sekaligus berpotensi mengabortkan pembangunan tab fase berikutnya.
		// Dibungkus try/catch agar tab tetap tampil (minimal toggle di bawah) & tidak mengganggu tab lain.
		try {
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Ubah Tampilan (Skin) Sistem"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_SKIN, LampiranLain.SKIN, "Skin", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent()
								.getNativeRequest();
						String content_path = request.getContextPath().replace("/", "");

						File theme = new File("/opt/" + content_path + ".zip");
						if (theme != null && theme.exists()) {
							theme.delete();
							theme.createNewFile();
						}

						FileInputStream inputStream = new FileInputStream(lainMahasiswa.ambilFile());
						FileOutputStream outputStream = new FileOutputStream(theme);
						IOUtils.copy(inputStream, outputStream);
						outputStream.close();
						inputStream.close();

						@SuppressWarnings("deprecation")
						String path = request.getRealPath("/");

						@SuppressWarnings("deprecation")
						String path1 = request.getRealPath("/WEB-INF/j/");

						File dir = new File(path1);
						try {
							FileUtils.deleteDirectory(dir);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						dir.mkdirs();

						Path source = Paths.get(theme.getAbsolutePath());
						Path target = Paths.get(dir.getAbsolutePath());

						Common.unzipFolder(source, target);
						File[] files = dir.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.toLowerCase().endsWith(".jsp");
							}
						});
						for (File file : files) {
							if (file.getName().endsWith(".jsp")) {
								System.out.println("Ubah file " + file.getAbsolutePath());
								String s = FileUtils.readFileToString(file);
								s = org.apache.commons.lang3.StringUtils.replace(s, "tampilanSocialLogin()",
										"tampilanSocialLogin(request, response)");
								s = org.apache.commons.lang3.StringUtils.replace(s, "/pages/ux", "/WEB-INF/o/ux");

								FileUtils.writeStringToFile(file, s);
							}
						}

						target = Paths.get(path);
						Common.unzipFolder(source, target);

						Common.checkLogoUpload();
						Common.checkBackgroundUpload();
						Common.checkFaviconUpload();
					}

				});
		hbox.setParent(groupbox);
		} catch (Exception eSkin) {
			ais.common.Common.tampilErrorJikaAdmin(eSkin);
			ais.common.ErrorAuditUtil.record(eSkin, "auto-audit(blank-fix) KonfigurasiNewAction.initTabTampilanSistem-skin");
		}

		rows.appendChild(createRowActiveDefault("Paksa halaman utama menggunakan skin yang diunggah",
				"paksa_halaman_utama_menggunakan_skin", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Menu dapat tertutup otomatis jika halaman telah terbuka",
				"otomatis_tertutup_menu_jika_buka_halaman", Konfigurasi.AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabPenelitianDanPengabdian() {
		Rows rows = null;
		rows = (createSpan("Penelitian dan Pengabdian"));

		rows.appendChild(createRowActiveDefault("Aktifkan Terintegrasi dengan Penelitian dan Pengabdian ?",
				"terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Alamat URL OJS", "alamat_url_ojs", "http://ojs.ecampus.id"));
		rows.appendChild(createRowNilai("Alamat URL Sistem Penelitian dan Pengabdian Masyarakat",
				"alamat_url_simlitabmas", "http://simlitabmas.ecampus.id"));

		rows.appendChild(createRowActiveDefault("Tampilkan fitur penelitian dan pengabdian di halaman mahasiswa",
				"tampilkan_penelitian_dan_pengabdian_di_mahasiswa", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Tampilkan fitur penelitian dan pengabdian di halaman pegawai",
				"tampilkan_penelitian_dan_pengabdian_di_pegawai", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Tampilkan fitur penelitian dan pengabdian di halaman dosen",
				"tampilkan_penelitian_dan_pengabdian_di_dosen", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Surat tugas wajib diupload saat mengajukan artikel",
				"surat_tugas_wajib_diupload_saat_mengajukan_artikel", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Surat keterangan wajib diupload saat mengajukan artikel",
				"surat_keterangan_wajib_diupload_saat_mengajukan_artikel", Konfigurasi.TIDAK_AKTIF));
	}

	@SuppressWarnings("unchecked")
	protected void initTabBackup() {
		Rows rows = null;
		rows = (createSpan("Backup"));
		// rows.appendChild(createRowNilai("Database IP", "SERVER_BACKUP_IP",
		// "localhost"));
		// rows.appendChild(createRowNilai("Database Name",
		// "SERVER_BACKUP_DATABASE", "ecampus"));
		rows.appendChild(createRowNilai("Perintah pg_dump", "lokasi_pg_dump", "pg_dump"));
		rows.appendChild(createRowNilai("Lokasi backup sementara", "lokasi_directory_file_backup", "/backup/"));

//		rows.appendChild(
//				createRowNilai("Lokasi penyimpanan file", "lokasi_penyimpanan_file", "/opt/lampiran_file_lain"));
		rows.appendChild(createRowNilai("Lokasi penyimpanan lampiran perpustakaan",
				"lokasi_penyimpanan_lampiran_perpustakaan", "/opt/gambar_perpus"));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Backup Database dan Download Langsung"));
		groupbox.appendChild(
				button = new MyButtonConfig("Proses Backup dan Download Langsung", "/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				/* OPTIMASI FASE 5: sebelumnya dipakai prepareDesktopForBackgroundAlert() yang MENYALAKAN
				 * server push tetapi TIDAK PERNAH mematikannya, sehingga browser terus polling (menahan
				 * thread Tomcat) selama tab terbuka walau backup sudah selesai. Di sini desktop diambil
				 * tanpa menyalakan push; siklus hidup push sepenuhnya diurus jalankanDenganPush() di bawah. */
				final Desktop desktop = Executions.getCurrent() == null ? null : Executions.getCurrent().getDesktop();
				final Label labelFilename = new Label();

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							String filename = labelFilename.getValue();
							File backupFile = filename == null || filename.trim().isEmpty() ? null : new File(filename);
							if (isValidBackupFile(backupFile)) {
								Filedownload.save(backupFile, "application/backup");
							} else {
								MyMessageboxConfig.show(
										"Mohon maaf, berkas backup basis data belum tersedia atau gagal dibuat sehingga proses unduhan tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) Pastikan proses backup telah selesai dan berhasil dibuat; (2) Periksa kembali konfigurasi Perintah pg_dump serta Lokasi backup sementara pada menu Backup; (3) Pastikan folder backup sementara dapat ditulis oleh aplikasi, kemudian ulangi proses Backup dan Download.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				});

				/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
				 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
				 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
				 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
				 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
				ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

					@Override
					public void run() {
						try {
							File file = BackupUtil.backupPGSQL(label);
							if (isValidBackupFile(file)) {
								labelFilename.setValue(file.getAbsolutePath());
								setLabelValueQuietly(label, "");
							} else {
								String msg = "Backup gagal dibuat. Periksa konfigurasi pg_dump dan lokasi backup.";
								setLabelValueQuietly(label, msg);
								showBackupAlert(desktop, "Backup Database Gagal", msg, null);
							}
						} catch (Exception e) {
							setLabelValueQuietly(label, "Backup gagal: " + e.getMessage());
							showBackupAlert(desktop, "Backup Database dan Download Langsung", e);
						}
					}
				});
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Reset Deadlock"));
		groupbox.appendChild(button = new MyButtonConfig("Proses Reset Deadlock", "/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				session.createSQLQuery("SELECT\r\n    activity.pid,\r\n    activity.usename,\r\n"
						+ "    pg_terminate_backend(blocking.pid) AS blocking_id,\r\n"
						+ "    blocking.query AS blocking_query\r\n FROM pg_stat_activity AS activity\r\n"
						+ "JOIN pg_stat_activity AS blocking ON blocking.pid = ANY(pg_blocking_pids(activity.pid))")
						.list();

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Backup Database dan kirim ke Drive"));
		groupbox.appendChild(
				button = new MyButtonConfig("Proses Backup dan Kirim Sekarang", "/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Desktop desktop = prepareDesktopForBackgroundAlert();
				final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method
						// stub

					}
				});

				Tbmuser tbmuser = Common.getCurrentUser();
				final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
				final File fileProbe = createBackupDriveProbeFile("ecampus_backup_db_drive_probe_");

				driveUtilPerPengguna.prosesBackup(fileProbe, "Verifikasi Backup Database",

						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
										.getData();
								deleteQuietly(fileProbe);

								if (fileUpload != null && fileUpload.getId() != null) {
									File file = null;
									try {
										file = BackupUtil.backupPGSQL(label);
									} catch (Exception e) {
										setLabelValueQuietly(label, "Backup database gagal: " + e.getMessage());
										showBackupAlert(desktop, "Backup Database dan Kirim ke Drive", e);
										return;
									}
									if (!isValidBackupFile(file)) {
										String msg = "Backup database gagal dibuat. Periksa pg_dump dan lokasi backup.";
										setLabelValueQuietly(label, msg);
										showBackupAlert(desktop, "Backup Database Gagal", msg, null);
										return;
									}
									driveUtilPerPengguna.kirimBackupLangsung(label, file, perguruanTinggi,
											"Backup Database", new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													setLabelValueQuietly(label, "");
												}
											});
								} else {
									String msg = "Verifikasi Google Drive gagal. Periksa otorisasi akun Google Drive.";
									setLabelValueQuietly(label, msg);
									showBackupAlert(desktop, "Verifikasi Google Drive Gagal", msg, null);
								}
							}
						});

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Backup Database Streaming dan kirim ke Drive"));

		groupbox.appendChild(
				button = new MyButtonConfig("Proses Backup Database Streaming dan Kirim Sekarang", "/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Desktop desktop = prepareDesktopForBackgroundAlert();
				final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method
						// stub

					}
				});

				Tbmuser tbmuser = Common.getCurrentUser();
				final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
				final File fileProbe = createBackupDriveProbeFile("ecampus_backup_db_stream_probe_");

				driveUtilPerPengguna.prosesBackup(fileProbe, "Verifikasi Backup Database Streaming",

						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
										.getData();
								deleteQuietly(fileProbe);

								if (fileUpload != null && fileUpload.getId() != null) {
									File file = null;
									try {
										file = BackupUtil.backupPGSQLStream(label);
									} catch (Exception e) {
										setLabelValueQuietly(label, "Backup database streaming gagal: " + e.getMessage());
										showBackupAlert(desktop, "Backup Database Streaming dan Kirim ke Drive", e);
										return;
									}
									if (!isValidBackupFile(file)) {
										String msg = "Backup database streaming gagal dibuat. Periksa pg_dump dan lokasi backup.";
										setLabelValueQuietly(label, msg);
										showBackupAlert(desktop, "Backup Database Streaming Gagal", msg, null);
										return;
									}
									driveUtilPerPengguna.kirimBackupLangsung(label, file, perguruanTinggi,
											"Backup Database Streaming", new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													setLabelValueQuietly(label, "");
												}
											});
								} else {
									String msg = "Verifikasi Google Drive gagal. Periksa otorisasi akun Google Drive.";
									setLabelValueQuietly(label, msg);
									showBackupAlert(desktop, "Verifikasi Google Drive Gagal", msg, null);
								}
							}
						});
			}
		});

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// groupbox.appendChild(new Caption("Reset Drive"));
		//
		// row.appendChild(button = new MyButtonConfig("Reset Drive sekarang"));
		// button.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if (new File("/opt/gdrive_temp/StoredCredential").delete()) {
		// MyMessageboxConfig.show("Reset Drive berhasil dilakukan.",
		// "Pemberitahuan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// } else {
		// MyMessageboxConfig.show("Reset Drive gagal dilakukan.",
		// "Pemberitahuan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.EXCLAMATION);
		// }
		// }
		// });

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(
				"Kirim file lampiran ecampus ke Drive (file asli akan terhapus dan dipindahkan ke google drive)"));

		groupbox.appendChild(button = new MyButtonConfig("Proses pengiriman lampiran ke Drive sekarang",
				"/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null) {

					final MyWindow window = new MyWindow("Pilih Tanggal File Materi Perkuliahan", "none", true);
					window.setParent(page.getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setSclass("dgrid");
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("20%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows	rows = new Rows();
					rows.setParent(grid);

					final MyDatebox mulai = new MyDatebox(WaktuUtil.getDate());
					final MyDatebox sampai = new MyDatebox(WaktuUtil.getDate());
					mulai.setReadonly(true);
					sampai.setReadonly(true);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
					row.appendChild(mulai);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
					row.appendChild(sampai);

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
							"Proses pengiriman lampiran ke Drive sekarang", "/img/Google-Drive-icon.png");
					save.setTooltiptext("Download");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
							final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
							final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							});

							final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
							final File fileProbe = createBackupDriveProbeFile("ecampus_backup_file_probe_");
							driveUtilPerPengguna.prosesBackup(fileProbe, "Verifikasi Backup File",

									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
													.getData();
											deleteQuietly(fileProbe);

											if (fileUpload != null && fileUpload.getId() != null) {

												new Thread(new Runnable() {

													@Override
													public void run() {
														Session session = StreamingHibernateUtil.getInstance()
																.currentSession();

														String[] daftarJenisDihapus = new String[] { "Absen Manual",
																"Jawaban ke-1", "Tugas Mandiri Perkuliahan",
																"Susunan silabus perkuliahan", "File Penelitian Ilmiah",
																"Skripsi", "Susunan SAP perkuliahan", "Soal I",
																"Soal II", "Soal III", "Soal IV", "Soal V",
																"ais.database.model.FormulirKegiatanPeserta",
																"ais.database.model.PenghargaanDosen",
																"ais.database.model.PrestasiDosen",
																"ais.database.model.KegiatanKedosenanPunyaDosen",
																"Cover Skripsi", "File Publikasi Ilmiah",
																"Bukti Pembayaran",
																"ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa",
																"ais.database.model.MahasiswaRequestTugasAkhir",
																"ais.database.model.BuktiPembayaran",
																"ais.database.model.sekolah.CalonSiswaPunyaVerifikasiParameter",
																"Tugas Kelompok Perkuliahan",
																"Izin Tidak Masuk Perkuliahan",
																"ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter",
																"Sertifikasi Dosen",
																"ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas",
																"ais.database.model.NamaTugasKelompok",
																"Catatan Perkuliahan", "Jawaban I",
																"Lampiran Komentar Pengumuman Akademik", "Gambar Asset",
																"Gambar Master Asset", "Buku", "Cover Buku",
																"Surat Tugas Mengajar di PT Lain",
																"Surat Keputusan Tugas Belajar",
																"Laporan Hasil Perkuliahan", "Susunan Pembelajaran",
																"Catatan Konsultasi", "Sertifikasi Pegawai",
																"Izin Tidak Masuk Perkuliahan",
																"Tugas Kelompok Perkuliahan", "Item" };

														String s = "('Diskusi'";
														for (String ss : daftarJenisDihapus) {
															s += ",'" + ss + "'";
														}
														s += ")";

														List<Object[]> inds = session.createSQLQuery(
																"select id,foto from lampiran_lain where foto is not null and nama not ilike '%.jrxml%' and jenis in "
																		+ s
																		+ " and date(tanggal_dirubah) between date('"
																		+ Common.databaseDateFormat.get()
																				.format(mulai.getValue())
																		+ "') and date('"
																		+ Common.databaseDateFormat.get()
																				.format(sampai.getValue())
																		+ "') order by id desc;")
																.list();
														StreamingHibernateUtil.getInstance().closeSession();

														int size = inds.size();
														int index = 0;
														for (Object[] o : inds) {
															index++;

															try {
																Object id = o[0];
																final Object fotoId = o[1];

																session = StreamingHibernateUtil.getInstance()
																		.currentSession();
																final LampiranLain lampiranLain = (LampiranLain) session
																		.createCriteria(LampiranLain.class)
																		.add(Restrictions
																				.idEq(Long.parseLong(id.toString())))
																		.uniqueResult();
																StreamingHibernateUtil.getInstance().closeSession();
																if (lampiranLain != null) {
																	File file = lampiranLain.ambilFile();
																	if (file != null && file.exists()) {
																		s = "Mengirim file " + file.getName() + " "
																				+ lampiranLain.getJenis() + " ("
																				+ Common.numberFormat.get().format(
																						(index * 100.0) / size)
																				+ "%)";
																		System.out.println(s);
																		label.setValue(s);
																		com.google.api.services.drive.model.File fileKirim = driveUtilPerPengguna
																				.kirimBackupLangsung(null, file,
																						perguruanTinggi,
																						lampiranLain.getJenis(),
																						new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
																										.getData();

																								if (fileUpload != null
																										&& fileUpload
																												.getId() != null) {

																									Session session = StreamingHibernateUtil
																											.getInstance()
																											.currentSession();
																									try {

																										session.refresh(
																												lampiranLain);
																										lampiranLain
																												.setFoto(
																														null);
																										lampiranLain
																												.setGdrive(
																														fileUpload
																																.getId());
																										lampiranLain
																												.setGdriveUsername(
																														tbmuser.getUserId());

																										session.getTransaction()
																												.begin();
																										session.update(
																												lampiranLain);
																										session.getTransaction()
																												.commit();

																										FileFoto.hapusTotal(
																												fotoId.toString(),
																												session);

																										LampiranLain
																												.resetLokasi(
																														false,
																														lampiranLain
																																.getRef(),
																														lampiranLain
																																.getJenis());

																									} catch (Exception e) {
																										ais.common.Common.tampilErrorJikaAdmin(e);
																										StreamingHibernateUtil
																												.getInstance()
																												.rollbackTransaction();
																									}

																									StreamingHibernateUtil
																											.getInstance()
																											.closeSession();
																								}

																							}
																						});

																		if (fileKirim == null) {
																			System.out.println("Gagal Terkirim "
																					+ file.getAbsolutePath());
																			break;
																		} else {
																			System.out.println("Terkirim "
																					+ fileKirim.toPrettyString());
																		}
																	}

																}

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
																break;
															}

														}
														label.setValue("Selesai");
													}
												}).start();
											}

										}
									});
						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			}
		});

		displayBackup(rows, "materi perkuliahan", "pertemuan_file_content", "filecontent", PertemuanFileContent.class);
		displayBackup(rows, "tugas perkuliahan", "tugas_file_content", "filecontent", TugasFileContent.class);

//		displayBackup(rows, "foto calon mahasiswa", "foto_biodata_calon_mahasiswa", "foto",
//				FotoBiodataCalonMahasiswa.class);
//		displayBackup(rows, "foto mahasiswa", "foto_mahasiswa", "foto", FotoMahasiswa.class);
//		displayBackup(rows, "foto dosen", "foto_dosen", "foto", FotoDosen.class);
//		displayBackup(rows, "foto pegawai", "foto_pegawai", "foto", FotoPegawai.class);

		rows.appendChild(
				createRowActiveDefault("Aktifkan restart otomatis", "aktifkan_auto_restart", Konfigurasi.TIDAK_AKTIF));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Jika restart otomatis aktif, masukkan jam restart"));
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		waktuMulai.setCols(2);
		waktuMulai.setFormat(Common.timeFormat.get().toPattern());
		try {
			String restartTime = Common.getKonfigurasi("auto_restart", "").getNilai();
			waktuMulai.setValue(restartTime == null || restartTime.trim().isEmpty() ? null
					: Common.timeFormat2.get().parse(restartTime.trim()));
		} catch (Exception e) {
			// Nilai konfigurasi "auto_restart" bisa saja BUKAN format jam (mis. tersimpan "aktif"
			// akibat salah isi). Jangan gagalkan render tab & jangan spam log — cukup kosongkan
			// input jam-nya (ParseException "Unparseable date").
			waktuMulai.setValue(null);
		}
		groupbox.appendChild(waktuMulai);
		waktuMulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi("auto_restart", "");
				konfigurasi.setNilai(
						waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
			}
		});

		rows.appendChild(createRowNilai("Otomatis run GC apabila memori yang tersedia tinggal berapa persen ?",
				"persen_auto_run_gc_baru", "10.0"));

		rows.appendChild(createRowNilai("Otomatis restart apabila memori yang tersedia tinggal berapa persen ?",
				"persen_auto_restart", "0.0"));

		rows.appendChild(createRowNilai("Perintah restart ke sistem operasi", "perintah_restart", ""));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Restart"));

		groupbox.appendChild(button = new MyButtonConfig("Restart sekarang", "/img/Apps-session-logout-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin melakukan restart sistem sekarang? Mohon diperhatikan bahwa seluruh pengguna yang sedang aktif akan terputus sementara selama proses restart berlangsung. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									String perintah_restart = Common.getKonfigurasi("perintah_restart", "").getNilai();
									MyMessageboxConfig.show(UserOnlineCounter.doRestart(perintah_restart), "Informasi",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

								}

							}
						});
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected void initTabMediaSosial() {
		Rows rows = null;
		rows = (createSpan("Media Sosial"));

		final Combobox aktifkanLoginHanyaViaMediaSocial = createComboActive();
		aktifkanLoginHanyaViaMediaSocial.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanLoginHanyaViaMediaSocial = aktifkanLoginHanyaViaMediaSocial.getSelectedItem()
						.getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanLoginHanyaViaMediaSocial => "
						+ ConstantValues.aktifkanLoginHanyaViaMediaSocial);
			}
		});

		final Combobox aktifkanRememeberMe = createComboActive();
		aktifkanRememeberMe.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanRememeberMe = aktifkanRememeberMe.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanRememeberMe => " + ConstantValues.aktifkanRememeberMe);
			}
		});

		rows.appendChild(createRowActiveDefault("Aktifkan \"Ingat akun selama menggunakan browser ini\"",
				"aktifkanRememeberMe", Konfigurasi.AKTIF, aktifkanRememeberMe));

		final Combobox aktifkanRememeberMeOtomatisTerpilih = createComboActive();
		aktifkanRememeberMeOtomatisTerpilih.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanRememeberMeOtomatisTerpilih = aktifkanRememeberMeOtomatisTerpilih
						.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanRememeberMeOtomatisTerpilih => "
						+ ConstantValues.aktifkanRememeberMeOtomatisTerpilih);
			}
		});

		rows.appendChild(createRowActiveDefault("Langsung terpilih \"Ingat akun selama menggunakan browser ini\"",
				"aktifkanRememeberMeOtomatisTerpilih", Konfigurasi.TIDAK_AKTIF, aktifkanRememeberMeOtomatisTerpilih));

		rows.appendChild(createRowActiveDefault("Login atau autentikasi pengguna harus menggunakan media sosial",
				"login_harus_via_media_sosial", Konfigurasi.TIDAK_AKTIF, aktifkanLoginHanyaViaMediaSocial));

		rows.appendChild(createRowActiveDefault(
				"Pengguna bisa memasukkan sendiri akun (username dan password) jika email belum terdaftar",
				"pengguna_bisa_memasukkan_sendiri_akun_jika_email_belum_terdaftar", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Jika email tidak terdaftar, maka pengguna tidak bisa login",
				"login_menggunakan_email_terdaftar", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Jika pengguna belum memiliki media sosial yang terdaftar, sistem akan menanyakan ke pengguna supaya mendaftarkan akun-nya",
				"tanya_media_sosial_jika_belum_punya", Konfigurasi.AKTIF));

		rows.appendChild(createRowNilai(
				"Kode grup pengguna yang tidak diizinkan untuk melakukan login via media sosial. (Jika lebih dari satu, pisah dengan tanda koma)",
				"grup_pengguna_blok", ConstantValues.grupPenggunaBlok, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.grupPenggunaBlok = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		createSpan("Facebook", rows);

		final Combobox aktifkanFacebook = createComboActive();
		aktifkanFacebook.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanIntegrasiFacebook = aktifkanFacebook.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println(
						"ConstantValues.aktifkanIntegrasiFacebook => " + ConstantValues.aktifkanIntegrasiFacebook);
			}
		});
		rows.appendChild(createRowActiveDefault("Integrasikan dengan facebook", "aktifkan_integrasi_facebook",
				Konfigurasi.AKTIF, aktifkanFacebook));

		rows.appendChild(createRowNilai("ID Aplikasi", "id_aplikasi_facebook", "1134829156583318"));
		rows.appendChild(createRowNilaiPassword("Kunci Rahasia Aplikasi", "kunci_rahasia_aplikasi_facebook",
				"454cfff40912a1a306bcf8edf36d0790"));

		createSpan("Google", rows);

		final Combobox aktifkanGoogle = createComboActive();
		aktifkanGoogle.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanIntegrasiGoogle = aktifkanGoogle.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out
						.println("ConstantValues.aktifkanIntegrasiGoogle => " + ConstantValues.aktifkanIntegrasiGoogle);
			}
		});

		rows.appendChild(createRowNilai("ID Aplikasi untuk login", "id_aplikasi_google",
				"659282761898-am1cl4kjbnupq3gjehantrfr77qmdp8u.apps.googleusercontent.com"));
		rows.appendChild(createRowNilaiPassword("Kunci Rahasia Aplikasi untuk login", "kunci_rahasia_aplikasi_google",
				"wSJ8-Sb4rx3LseH0k8HIiUqr"));

		rows.appendChild(createRowActiveDefault("Integrasikan dengan google untuk login", "aktifkan_integrasi_google",
				Konfigurasi.AKTIF, aktifkanGoogle));

		rows.appendChild(createRowNilai("Client ID untuk Google Calendar", "google_calendar_client_id",
				GoogleCommon.getGoogle_calendar_client_id(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setGoogle_calendar_client_id(Common.getKonfigurasi("google_calendar_client_id",
								GoogleCommon.getGoogle_calendar_client_id()).getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Key untuk Google Calendar", "google_calendar_key",
				GoogleCommon.getGoogle_calendar_key(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setGoogle_calendar_key(
								Common.getKonfigurasi("google_calendar_key", GoogleCommon.getGoogle_calendar_key())
										.getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Redirect URL untuk Google Calendar", "redirect_url_calendar_https",
				GoogleCommon.getRedirect_url_calendar(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setRedirect_url_calendar(Common
								.getKonfigurasi("redirect_url_calendar_https", GoogleCommon.getRedirect_url_calendar())
								.getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Client ID untuk Google Drive", "google_drive_client_id_https",
				GoogleCommon.getGoogle_drive_client_id(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setGoogle_drive_client_id(Common.getKonfigurasi("google_drive_client_id_https",
								GoogleCommon.getGoogle_drive_client_id()).getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Key untuk Google Drive", "google_drive_key_https",
				GoogleCommon.getGoogle_drive_key(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setGoogle_drive_key(
								Common.getKonfigurasi("google_drive_key_https", GoogleCommon.getGoogle_drive_key())
										.getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Redirect URL untuk Google Drive", "redirect_url_drive_https",
				GoogleCommon.getRedirect_url_drive(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GoogleCommon.setRedirect_url_drive(
								Common.getKonfigurasi("redirect_url_drive_https", GoogleCommon.getRedirect_url_drive())
										.getNilai());
					}
				}));

		rows.appendChild(createRowNilai("Key untuk Google Book", "google_book_key", ClientCredentials.KEY_PERPUS,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ClientCredentials.KEY_PERPUS = Common
								.getKonfigurasi("google_book_key", ClientCredentials.KEY_PERPUS).getNilai();
					}
				}));

		createSpan("Twitter", rows);

		final Combobox aktifkanTwitter = createComboActive();
		aktifkanTwitter.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanIntegrasiTwitter = aktifkanTwitter.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println(
						"ConstantValues.aktifkanIntegrasiTwitter => " + ConstantValues.aktifkanIntegrasiTwitter);
			}
		});
		rows.appendChild(createRowActiveDefault("Integrasikan dengan twitter", "aktifkan_integrasi_twitter",
				Konfigurasi.AKTIF, aktifkanTwitter));

		rows.appendChild(createRowNilai("ID Aplikasi", "id_aplikasi_twitter", "31JDGYztaFvIXDuZyhpfSIh80"));
		rows.appendChild(createRowNilaiPassword("Kunci Rahasia Aplikasi", "kunci_rahasia_aplikasi_twitter",
				"mqPwaSByVkda3OvTCZckpi2tbUu7olkp7eCQOGpkCD07cWJEoD"));

		createSpan("LinkedIn", rows);

		final Combobox aktifkanLinkedin = createComboActive();
		aktifkanLinkedin.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanIntegrasiLinkedin = aktifkanLinkedin.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println(
						"ConstantValues.aktifkanIntegrasiLinkedin => " + ConstantValues.aktifkanIntegrasiLinkedin);
			}
		});
		rows.appendChild(createRowActiveDefault("Integrasikan dengan linkedin", "aktifkan_integrasi_linkedin",
				Konfigurasi.AKTIF, aktifkanLinkedin));

		rows.appendChild(createRowNilai("ID Aplikasi", "id_aplikasi_linkedin", "75l33zqsb4wucn"));
		rows.appendChild(createRowNilaiPassword("Kunci Rahasia Aplikasi", "kunci_rahasia_aplikasi_linkedin",
				"O2qbKAgQ9haUlgzd"));
	}

	@SuppressWarnings("unchecked")
	protected void initTabCaptcha() {
		Rows rows = null;
		rows = createSpan("Captcha");

		createSpan("Captcha Lokal", rows);

		final Combobox aktifkanCaptchaLokal = createComboActive();
		aktifkanCaptchaLokal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanCaptchaLokal = aktifkanCaptchaLokal.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanCaptchaLokal => " + ConstantValues.aktifkanCaptchaLokal);
			}
		});

		rows.appendChild(createRowActiveDefault("Login atau autentikasi pengguna harus menggunakan captcha lokal",
				"login_harus_menggunakan_capcha_lokal", Konfigurasi.TIDAK_AKTIF, aktifkanCaptchaLokal));

		rows.appendChild(createRowNilai("Lebar Captcha", "lebar_capcha_lokal", ConstantValues.aktifkanCaptchaLokalLebar,
				1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.aktifkanCaptchaLokalLebar = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		rows.appendChild(createRowNilai("Tinggi Captcha", "tinggi_capcha_lokal",
				ConstantValues.aktifkanCaptchaLokalTinggi, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.aktifkanCaptchaLokalTinggi = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		final Combobox aktifkanCaptchaLokalNoice = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(StraightLineNoiseProducer.class.getSimpleName());
		comboitem.setValue(StraightLineNoiseProducer.class.getName());
		aktifkanCaptchaLokalNoice.appendChild(comboitem);
		comboitem = new MyComboitemConfig(CurvedLineNoiseProducer.class.getSimpleName());
		comboitem.setValue(CurvedLineNoiseProducer.class.getName());
		aktifkanCaptchaLokalNoice.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Kosong");
		comboitem.setValue("");
		aktifkanCaptchaLokalNoice.appendChild(comboitem);

		aktifkanCaptchaLokalNoice.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanCaptchaLokalNoice = (String) aktifkanCaptchaLokalNoice.getSelectedItem()
						.getValue();
				System.out.println(
						"ConstantValues.aktifkanCaptchaLokalNoice => " + ConstantValues.aktifkanCaptchaLokalNoice);
			}
		});

		Common.selectComboItem(aktifkanCaptchaLokalNoice, ConstantValues.aktifkanCaptchaLokalNoice);
		aktifkanCaptchaLokalNoice.setReadonly(true);

		rows.appendChild(createRowActiveDefault("Jenis Noice Captcha", "jenis_noice_capcha_lokal",
				ConstantValues.aktifkanCaptchaLokalNoice, aktifkanCaptchaLokalNoice));

		final Combobox aktifkanCaptchaLokalRender = new Combobox();
		comboitem = new MyComboitemConfig(FishEyeGimpyRenderer.class.getSimpleName());
		comboitem.setValue(FishEyeGimpyRenderer.class.getName());
		aktifkanCaptchaLokalRender.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BlockGimpyRenderer.class.getSimpleName());
		comboitem.setValue(BlockGimpyRenderer.class.getName());
		aktifkanCaptchaLokalRender.appendChild(comboitem);

		comboitem = new MyComboitemConfig(DropShadowGimpyRenderer.class.getSimpleName());
		comboitem.setValue(DropShadowGimpyRenderer.class.getName());
		aktifkanCaptchaLokalRender.appendChild(comboitem);

		comboitem = new MyComboitemConfig(RippleGimpyRenderer.class.getSimpleName());
		comboitem.setValue(RippleGimpyRenderer.class.getName());
		aktifkanCaptchaLokalRender.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ShearGimpyRenderer.class.getSimpleName());
		comboitem.setValue(ShearGimpyRenderer.class.getName());
		aktifkanCaptchaLokalRender.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Kosong");
		comboitem.setValue("");
		aktifkanCaptchaLokalRender.appendChild(comboitem);

		aktifkanCaptchaLokalRender.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanCaptchaLokalRender = (String) aktifkanCaptchaLokalRender.getSelectedItem()
						.getValue();
				System.out.println(
						"ConstantValues.aktifkanCaptchaLokalRender => " + ConstantValues.aktifkanCaptchaLokalRender);
			}
		});

		Common.selectComboItem(aktifkanCaptchaLokalRender, ConstantValues.aktifkanCaptchaLokalRender);
		aktifkanCaptchaLokalRender.setReadonly(true);

		rows.appendChild(createRowActiveDefault("Jenis Render Captcha", "jenis_render_capcha_lokal",
				ConstantValues.aktifkanCaptchaLokalRender, aktifkanCaptchaLokalRender));

		final Combobox aktifkanCaptchaLokalBackground = new Combobox();
		comboitem = new MyComboitemConfig(FlatColorBackgroundProducer.class.getSimpleName());
		comboitem.setValue(FlatColorBackgroundProducer.class.getName());
		aktifkanCaptchaLokalBackground.appendChild(comboitem);
		comboitem = new MyComboitemConfig(GradiatedBackgroundProducer.class.getSimpleName());
		comboitem.setValue(GradiatedBackgroundProducer.class.getName());
		aktifkanCaptchaLokalBackground.appendChild(comboitem);

		comboitem = new MyComboitemConfig(SquigglesBackgroundProducer.class.getSimpleName());
		comboitem.setValue(SquigglesBackgroundProducer.class.getName());
		aktifkanCaptchaLokalBackground.appendChild(comboitem);

		comboitem = new MyComboitemConfig(TransparentBackgroundProducer.class.getSimpleName());
		comboitem.setValue(TransparentBackgroundProducer.class.getName());
		aktifkanCaptchaLokalBackground.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Kosong");
		comboitem.setValue("");
		aktifkanCaptchaLokalBackground.appendChild(comboitem);

		aktifkanCaptchaLokalBackground.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanCaptchaLokalBackground = (String) aktifkanCaptchaLokalBackground
						.getSelectedItem().getValue();
				System.out.println("ConstantValues.aktifkanCaptchaLokalBackground => "
						+ ConstantValues.aktifkanCaptchaLokalBackground);
			}
		});

		Common.selectComboItem(aktifkanCaptchaLokalBackground, ConstantValues.aktifkanCaptchaLokalBackground);
		aktifkanCaptchaLokalBackground.setReadonly(true);

		rows.appendChild(createRowActiveDefault("Jenis Background Captcha", "jenis_background_capcha_lokal",
				ConstantValues.aktifkanCaptchaLokalBackground, aktifkanCaptchaLokalBackground));

		final Combobox aktifkanCaptchaLokalText = new Combobox();
		comboitem = new MyComboitemConfig(DefaultTextProducer.class.getSimpleName());
		comboitem.setValue(DefaultTextProducer.class.getName());
		aktifkanCaptchaLokalText.appendChild(comboitem);
		comboitem = new MyComboitemConfig(FiveLetterFirstNameTextProducer.class.getSimpleName());
		comboitem.setValue(FiveLetterFirstNameTextProducer.class.getName());
		aktifkanCaptchaLokalText.appendChild(comboitem);
		comboitem = new MyComboitemConfig(ChineseTextProducer.class.getSimpleName());
		comboitem.setValue(ChineseTextProducer.class.getName());
		aktifkanCaptchaLokalText.appendChild(comboitem);

		comboitem = new MyComboitemConfig(DefaultWordRenderer.class.getSimpleName());
		comboitem.setValue(DefaultWordRenderer.class.getName());
		aktifkanCaptchaLokalText.appendChild(comboitem);
		comboitem = new MyComboitemConfig(ColoredEdgesWordRenderer.class.getSimpleName());
		comboitem.setValue(ColoredEdgesWordRenderer.class.getName());
		aktifkanCaptchaLokalText.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Kosong");
		comboitem.setValue("");
		aktifkanCaptchaLokalText.appendChild(comboitem);

		aktifkanCaptchaLokalText.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanCaptchaLokalText = (String) aktifkanCaptchaLokalText.getSelectedItem()
						.getValue();
				System.out.println(
						"ConstantValues.aktifkanCaptchaLokalText => " + ConstantValues.aktifkanCaptchaLokalText);
			}
		});

		Common.selectComboItem(aktifkanCaptchaLokalText, ConstantValues.aktifkanCaptchaLokalText);
		aktifkanCaptchaLokalText.setReadonly(true);

		rows.appendChild(createRowActiveDefault("Jenis Text Captcha", "jenis_text_capcha_lokal",
				ConstantValues.aktifkanCaptchaLokalText, aktifkanCaptchaLokalText));

		createSpan("Google Captcha", rows);

		final Combobox aktifkanRecapcha = createComboActive();
		aktifkanRecapcha.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanRecapcha = aktifkanRecapcha.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanRecapcha => " + ConstantValues.aktifkanRecapcha);
			}
		});

		rows.appendChild(createRowActiveDefault("Login atau autentikasi pengguna harus menggunakan google captcha",
				"login_harus_menggunakan_capcha", Konfigurasi.TIDAK_AKTIF, aktifkanRecapcha));

		rows.appendChild(createRowNilai("Key server untuk google captcha", "key_server_untuk_google_capcha",
				ConstantValues.recapchaKey, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.recapchaKey = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		rows.appendChild(createRowNilai("Key client untuk google captcha", "key_client_untuk_google_capcha",
				ConstantValues.recapchaClientKey, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.recapchaClientKey = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));

		rows.appendChild(createRowNilai("Login home jika captcha salah input", "recapcha_home",
				ConstantValues.recapchaHome, 1, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ConstantValues.recapchaHome = ((Textbox) arg0.getTarget()).getValue().trim();
					}
				}));
	}

	@SuppressWarnings("unchecked")
	protected void initTabFileBantuan() {
		Rows rows = null;
		rows = (createSpan("File Bantuan"));

		@SuppressWarnings("rawtypes")
		Map d = ConstantValues.ambilBerdasarClass(Tbmrole.class);
		for (Object o : d.values()) {
			Tbmrole tbmrole = (Tbmrole) o;
			if (tbmrole.getAktif()) {
				row = new MyFormRow();
				row.setParent(rows);
				groupbox = new Groupbox();
				groupbox.setParent(row);
				groupbox.appendChild(new Caption("File Bantuan " + tbmrole.getRoleName()));
				hbox = new Hbox();
				LampiranLain.createDownloadUploadFileLain(hbox, 1L,
						"nama_usermanual_" + tbmrole.getRoleId().toLowerCase(), "Bantuan " + tbmrole.getRoleName(),
						false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
				hbox.setParent(groupbox);
			}
		}
	}

	/**
	 * Tab Konfigurasi Aplikasi Mobile — mengelompokkan semua pengaturan yang
	 * secara langsung memengaruhi perilaku aplikasi Flutter (Android dan iOS)
	 * AIS Mobile yang berjalan di perangkat pengguna akhir.
	 *
	 * <p>Aplikasi mobile AIS mengonsumsi berbagai nilai konfigurasi dari server
	 * melalui API dengan action "konfigurasi". Setiap kunci konfigurasi dibaca
	 * saat startup atau pada saat fitur tertentu dipanggil, sehingga perubahan
	 * nilai di halaman ini akan langsung tercermin pada perilaku aplikasi mobile
	 * tanpa perlu me-rilis ulang APK maupun IPA. Tab ini terdiri dari sembilan
	 * kelompok pengaturan: (1) versi dan pembaruan aplikasi, (2) susunan menu
	 * navigasi, (3) pengaturan pembayaran mobile, (4) tampilan nomor HP di
	 * halaman profil pengguna, (5) log dan monitoring API mobile, (6) notifikasi
	 * push, (7) tata letak antarmuka web saat diakses dari perangkat mobile,
	 * (8) viewer dokumen PDF di mobile, dan (9) teks halaman depan untuk unduhan
	 * aplikasi. Setiap kelompok dideskripsikan secara lengkap agar administrator
	 * dapat memahami dampak perubahan nilai terhadap pengalaman pengguna di
	 * aplikasi mobile.</p>
	 */
	protected void initTabAplikasiMobile() {
		Rows rows = createSpan("Aplikasi Mobile");

		// =====================================================================
		// Kelompok 1: Versi dan Pembaruan Aplikasi
		// =====================================================================
		// Konfigurasi ini digunakan oleh Upgrader.dart di aplikasi Flutter.
		// Saat aplikasi diluncurkan, Upgrader memanggil API dengan action=konfigurasi
		// dan nama=mobile_version_<packageName> (Android) atau nama=mobile_version_ios_
		// <packageName> (iOS). Server mengembalikan nilai versi yang diharapkan;
		// jika berbeda dengan versi terpasang, aplikasi menampilkan dialog pembaruan.
		// Di Android sistem juga mencoba in-app update otomatis via Google Play.
		// Di iOS pengguna diarahkan ke App Store. Penamaan kunci HARUS mengikuti pola
		// mobile_version_<packageName> dan mobile_version_ios_<packageName>, di mana
		// <packageName> adalah nama paket terdaftar (mis. com.kampus.ais). Isi nilai
		// dengan string versi seperti "1.0.5". Kosongkan atau samakan dengan versi
		// terpasang jika tidak ingin memunculkan notifikasi pembaruan kepada pengguna.
		createSpan("Versi dan Pembaruan Aplikasi", rows);
		rows.appendChild(createRowNilai(
				"Versi terbaru aplikasi Android — pola kunci: mobile_version_<packageName> "
				+ "(contoh: mobile_version_com.kampus.ais). Isi string versi seperti '1.0.5'. "
				+ "Jika berbeda dengan versi terpasang, aplikasi menampilkan dialog pembaruan.",
				"mobile_version_default", ""));
		rows.appendChild(createRowNilai(
				"Versi terbaru aplikasi iOS — pola kunci: mobile_version_ios_<packageName> "
				+ "(contoh: mobile_version_ios_com.kampus.ais). Buat entri konfigurasi baru "
				+ "dengan kunci sesuai package name masing-masing aplikasi iOS.",
				"mobile_version_ios_default", ""));
		rows.appendChild(createRowNilai(
				"Catatan: Pola Penamaan Kunci Versi Mobile",
				"catatan_mobile_versi",
				"Kunci versi dibaca secara dinamis oleh Flutter menggunakan package name aplikasi. "
				+ "Untuk Android dengan package 'com.sekolah.ais', buat konfigurasi dengan kunci "
				+ "'mobile_version_com.sekolah.ais'. Untuk iOS buat 'mobile_version_ios_com.sekolah.ais'. "
				+ "Nilai berisi string versi (contoh: 2.3.1). Jika nilai sama dengan versi terpasang, "
				+ "tidak ada dialog pembaruan yang muncul.",
				4, null));

		// =====================================================================
		// Kelompok 2: Menu Navigasi Mobile
		// =====================================================================
		// Konfigurasi menu_mobile diambil oleh aplikasi Flutter melalui method
		// ApiCall.ambilMenuMobile() yang memanggil API dengan action=konfigurasi
		// dan nama=menu_mobile. Nilai berupa JSON yang mendefinisikan struktur
		// navigasi: daftar menu, ikon, label, dan hak akses per peran pengguna
		// (mahasiswa, dosen, pegawai, siswa, guru, dll). Dengan mengubah JSON ini,
		// administrator dapat menyesuaikan menu navigasi di aplikasi mobile tanpa
		// merilis ulang APK/IPA. Pastikan format JSON valid sebelum disimpan karena
		// JSON yang rusak dapat menyebabkan menu tidak tampil atau aplikasi jatuh ke
		// menu default bawaan. Perubahan berlaku setelah pengguna me-refresh atau
		// membuka ulang aplikasi mobile dari layar beranda.
		createSpan("Menu Navigasi Mobile", rows);
		rows.appendChild(createRowNilai(
				"Konfigurasi menu navigasi aplikasi mobile dalam format JSON (menu_mobile). "
				+ "Mendefinisikan daftar menu, ikon, label, urutan tampil, dan peran yang berhak "
				+ "melihat tiap item. Diambil oleh Flutter via ApiCall.ambilMenuMobile(). "
				+ "Pastikan JSON valid sebelum disimpan.",
				"menu_mobile", ""));
		rows.appendChild(createRowNilai(
				"Catatan: Format dan Cara Penggunaan menu_mobile",
				"catatan_menu_mobile",
				"Nilai konfigurasi menu_mobile adalah string JSON yang dibaca oleh aplikasi Flutter "
				+ "setiap kali layar utama dimuat. Struktur JSON mendefinisikan item navigasi beserta "
				+ "label, ikon (material icon name), route tujuan, dan peran pengguna yang berhak "
				+ "melihatnya. Jika nilai kosong, aplikasi menggunakan menu default yang dikodekan "
				+ "langsung di dalam APK/IPA. Ubah nilai ini untuk mengaktifkan atau menyembunyikan "
				+ "fitur tertentu di mobile tanpa perlu rilis ulang aplikasi.",
				4, null));

		// =====================================================================
		// Bahasa Default Aplikasi Mobile
		// =====================================================================
		// Konfigurasi mobile_default_language menentukan bahasa yang dipakai
		// aplikasi Flutter (eCampus/eSchool) saat pengguna login/membuka aplikasi
		// untuk pertama kali dan belum pernah mengubah bahasa secara pribadi lewat
		// menu Profil > Pengaturan > Bahasa di aplikasi mobile. Setiap pengguna
		// yang sudah pernah memilih bahasanya sendiri akan tetap memakai pilihan
		// pribadinya (disimpan lokal di perangkat per akun) walaupun bahasa
		// default ini diubah; nilai ini hanya berlaku sebagai bahasa awal bagi
		// pengguna yang belum pernah mengatur preferensi bahasanya. Diambil oleh
		// Flutter via ApiCall.ambilDefaultLanguage() yang memanggil API dengan
		// action=konfigurasi dan nama=mobile_default_language. Perubahan berlaku
		// setelah pengguna membuka ulang/login ulang aplikasi mobile.
		createSpan("Bahasa Default Aplikasi Mobile", rows);
		rows.appendChild(createRowActiveDefault(
				"Bahasa default aplikasi mobile (mobile_default_language). Dipakai sebagai "
				+ "bahasa awal untuk pengguna yang belum pernah mengatur bahasa pribadinya "
				+ "lewat menu Profil > Pengaturan > Bahasa pada aplikasi mobile. "
				+ "Diambil oleh Flutter via ApiCall.ambilDefaultLanguage().",
				"mobile_default_language", "en", createComboBahasaDefaultMobile()));

		// =====================================================================
		// Kelompok 3: Pengaturan Pembayaran Mobile
		// =====================================================================
		// Konfigurasi mobile_bayar_boleh_pilih_bulan_mundur menentukan apakah
		// pengguna diperbolehkan memilih bulan di masa lalu saat melakukan pembayaran
		// melalui aplikasi mobile. Ketika diaktifkan (aktif), dropdown pemilihan bulan
		// menampilkan semua bulan termasuk bulan-bulan sebelum bulan berjalan,
		// memungkinkan pengguna membayar tagihan lampau. Ketika tidak aktif, hanya
		// bulan berjalan dan bulan berikutnya yang dapat dipilih, mencegah pembayaran
		// retroaktif. Pengaturan ini diimplementasikan di screen pembayaran Flutter
		// melalui ApiCall.ambilConfigBayarPilihBulanMundur(). Ubah sesuai kebijakan
		// keuangan institusi masing-masing.
		createSpan("Pengaturan Pembayaran Mobile", rows);
		rows.appendChild(createRowActiveDefault(
				"Izinkan pengguna memilih bulan di masa lampau saat membayar tagihan via aplikasi mobile "
				+ "(mobile_bayar_boleh_pilih_bulan_mundur). Jika tidak aktif, hanya bulan berjalan ke "
				+ "depan yang dapat dipilih. Ubah sesuai kebijakan keuangan institusi.",
				"mobile_bayar_boleh_pilih_bulan_mundur", Konfigurasi.TIDAK_AKTIF));

		// =====================================================================
		// Kelompok 4: Tampilan Nomor HP di Halaman Profil Pengguna
		// =====================================================================
		// Enam konfigurasi berikut mengontrol apakah kolom nomor telepon selular
		// (HP/mobile) ditampilkan di halaman profil masing-masing jenis pengguna
		// dalam sistem. Ketika aktif, kolom nomor HP tampil dan dapat diisi atau
		// diubah oleh pengguna maupun administrator. Ketika tidak aktif, kolom
		// tersebut disembunyikan dari antarmuka sehingga tidak dapat dilihat maupun
		// diedit. Pengaturan ini berguna bagi institusi yang memiliki kebijakan privasi
		// terkait penyimpanan data kontak pengguna, atau bagi institusi yang ingin
		// menyederhanakan form biodata. Di aplikasi mobile Flutter, data nomor HP
		// yang tersimpan juga digunakan untuk fitur kontak dan verifikasi identitas.
		createSpan("Tampilan Nomor HP di Halaman Profil Pengguna", rows);
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Mahasiswa — dipakai di: BiodataMahasiswaAction",
				"tampilkan_mobile_di_profile_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Dosen — dipakai di: BiodataDosenAction",
				"tampilkan_mobile_di_profile_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Pegawai — dipakai di: BiodataPegawaiAction",
				"tampilkan_mobile_di_profile_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Guru — dipakai di: GuruAction",
				"tampilkan_mobile_di_profile_guru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Siswa — dipakai di: SiswaAction",
				"tampilkan_mobile_di_profile_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault(
				"Tampilkan kolom nomor HP/mobile di halaman profil Orang Tua — dipakai di: OrangTuaAction",
				"tampilkan_mobile_di_profile_orang_tua", Konfigurasi.AKTIF));

		// =====================================================================
		// Kelompok 5: Log dan Monitoring API Mobile
		// =====================================================================
		// Konfigurasi api_mobile_logger_aktif mengontrol pencatatan log untuk setiap
		// request API yang berasal dari aplikasi mobile. Ketika diaktifkan, request
		// yang teridentifikasi berasal dari aplikasi Flutter akan dicatat ke log
		// sistem termasuk detail action, token, waktu eksekusi, dan data yang
		// dikirimkan. Catatan ini berguna untuk debugging masalah di production,
		// pemantauan performa response API, analisis pola penggunaan aplikasi, dan
		// identifikasi error yang terjadi di sisi client mobile. Log dapat diperiksa
		// melalui menu Monitoring dan Audit. Menonaktifkan konfigurasi ini mengurangi
		// volume log tetapi mempersulit diagnosa masalah. Disarankan tetap aktif
		// selama masa pengembangan dan pengujian awal go-live.
		createSpan("Log dan Monitoring API Mobile", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan pencatatan log setiap request API yang berasal dari aplikasi mobile "
				+ "(api_mobile_logger_aktif). Berguna untuk debugging, monitoring performa, dan "
				+ "analisis penggunaan. Periksa hasilnya di menu Monitoring dan Audit.",
				"api_mobile_logger_aktif", Konfigurasi.AKTIF));

		// =====================================================================
		// Kelompok 6: Notifikasi Push ke Perangkat Mobile
		// =====================================================================
		// Konfigurasi aktfikan_pengiriman_notif mengontrol pengiriman notifikasi
		// push ke perangkat mobile pengguna melalui Firebase Cloud Messaging (FCM).
		// Token FCM perangkat disimpan saat pertama kali aplikasi Flutter dijalankan
		// dan server menggunakan token tersebut untuk mengirim notifikasi secara
		// real-time. Ketika diaktifkan, sistem mengirimkan notifikasi untuk berbagai
		// kejadian: pengumuman baru, jadwal perkuliahan/pelajaran, nilai yang keluar,
		// tagihan pembayaran, absensi, dan lain-lain sesuai konfigurasi tiap modul.
		// Menonaktifkan konfigurasi ini akan menghentikan SEMUA pengiriman notifikasi
		// push ke aplikasi mobile. Gunakan opsi ini hanya dalam kondisi darurat atau
		// saat layanan FCM sedang dalam pemeliharaan, karena akan berdampak pada
		// seluruh pengguna aplikasi mobile di semua perangkat.
		createSpan("Notifikasi Push ke Perangkat Mobile", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan pengiriman notifikasi push via Firebase Cloud Messaging (FCM) ke seluruh "
				+ "perangkat mobile pengguna (aktfikan_pengiriman_notif). PERHATIAN: menonaktifkan "
				+ "ini menghentikan SEMUA notifikasi push ke aplikasi mobile.",
				"aktfikan_pengiriman_notif", Konfigurasi.AKTIF));

		// =====================================================================
		// Kelompok 7: Tata Letak Antarmuka Web Saat Diakses dari Perangkat Mobile
		// =====================================================================
		// Tiga konfigurasi ini mengontrol dimensi dan tampilan antarmuka web AIS
		// ketika diakses melalui browser di perangkat mobile (smartphone/tablet),
		// bukan melalui aplikasi Flutter. Tinggi area konten (iframe) dan tinggi
		// header diatur terpisah antara desktop dan mobile agar tata letak optimal
		// di layar kecil. Nilai "otomatis" menyerahkan perhitungan ukuran ke browser,
		// sedangkan nilai piksel (contoh: "400px") menetapkan ukuran secara eksplisit.
		// Konfigurasi jml_tampil_kehadiran_dalam_satu_baris_mobile mengontrol berapa
		// kolom kehadiran ditampilkan per baris di tampilan mobile—nilai default 2
		// lebih cocok untuk layar sempit dibanding tampilan desktop yang menggunakan
		// nilai 5 kolom. Sesuaikan nilai ini berdasarkan feedback pengguna yang
		// mengakses sistem AIS melalui browser di smartphone mereka.
		createSpan("Tata Letak Antarmuka Web di Perangkat Mobile", rows);
		rows.appendChild(createRowNilai(
				"Tinggi area konten (iframe) index2.zul saat diakses dari mobile/tablet. "
				+ "Gunakan 'otomatis' agar browser menghitung sendiri, atau nilai piksel seperti '500px' "
				+ "untuk mengunci tinggi secara eksplisit.",
				"main2_tinggi_iframe_mobile", "otomatis"));
		rows.appendChild(createRowNilai(
				"Tinggi header index2.zul saat diakses dari mobile/tablet. "
				+ "Default: 86px. Sesuaikan jika header terpotong atau terlalu besar di layar kecil.",
				"main2_tinggi_header_mobile", "86px"));
		rows.appendChild(createRowNilai(
				"Jumlah kolom kehadiran per baris pada tampilan mobile — dipakai di: PengumumanAkademisAction. "
				+ "Default 2 (lebih kecil dari desktop yang 5 kolom). Sesuaikan dengan lebar layar perangkat target.",
				"jml_tampil_kehadiran_dalam_satu_baris_mobile", "2"));

		// =====================================================================
		// Kelompok 8: Viewer Dokumen PDF di Perangkat Mobile
		// =====================================================================
		// Konfigurasi gunakan_google_view_saat_mobile mengontrol cara sistem
		// menampilkan dokumen PDF dan laporan ketika pengguna mengaksesnya melalui
		// perangkat mobile. Ketika diaktifkan, sistem meneruskan URL dokumen ke
		// Google Docs Viewer (docs.google.com/viewer) sehingga PDF dapat dibuka
		// di browser mobile tanpa memerlukan aplikasi PDF reader terpisah. Ketika
		// tidak aktif, dokumen diunduh langsung atau dibuka menggunakan viewer bawaan
		// browser. Aktifkan jika pengguna melaporkan kesulitan membuka PDF di perangkat
		// mobile tertentu, terutama Android lama tanpa PDF viewer bawaan. Perlu
		// diperhatikan bahwa Google Docs Viewer memerlukan koneksi internet aktif
		// dan URL dokumen harus dapat diakses secara publik oleh server Google,
		// sehingga dokumen pada server dengan IP lokal/intranet tidak akan bisa
		// dibuka melalui Google Viewer.
		createSpan("Viewer Dokumen PDF di Perangkat Mobile", rows);
		rows.appendChild(createRowActiveDefault(
				"Gunakan Google Docs Viewer untuk membuka PDF dan laporan saat diakses dari "
				+ "perangkat mobile — dipakai di: Report, _pembayaran_online_services. "
				+ "Aktifkan jika browser mobile kesulitan membuka PDF secara langsung. "
				+ "Catatan: URL dokumen harus dapat diakses oleh server Google.",
				"gunakan_google_view_saat_mobile", Konfigurasi.TIDAK_AKTIF));

		// =====================================================================
		// Kelompok 9: Teks Halaman Depan untuk Promosi Unduhan Aplikasi Mobile
		// =====================================================================
		// Tiga konfigurasi berikut mengontrol teks yang ditampilkan di halaman
		// depan (landing page) portal AIS pada bagian promosi dan tautan unduhan
		// aplikasi mobile. Teks-teks ini ditampilkan kepada calon pengguna yang
		// belum login dan mengunjungi halaman utama melalui browser, sebagai ajakan
		// untuk mengunduh dan menggunakan aplikasi mobile. Kunci home_text_unduh_
		// aplikasi_mobile adalah judul bagian unduhan; home_text_google_play adalah
		// label tombol tautan ke Google Play Store untuk pengguna Android; dan
		// home_text_app_store adalah label tombol tautan ke Apple App Store untuk
		// pengguna iOS. Ubah nilai-nilai ini untuk menyesuaikan bahasa atau
		// terminologi institusi dalam mempromosikan aplikasi mobile kepada civitas
		// akademika dan masyarakat umum yang mengunjungi portal.
		createSpan("Teks Halaman Depan - Promosi Unduhan Aplikasi Mobile", rows);
		rows.appendChild(createRowNilai(
				"Judul bagian unduhan aplikasi mobile di halaman depan portal "
				+ "(home_text_unduh_aplikasi_mobile). Tampil sebagai heading ajakan kepada "
				+ "pengunjung untuk mengunduh aplikasi.",
				"home_text_unduh_aplikasi_mobile", "Unduh Aplikasi Mobile", 2, null));
		rows.appendChild(createRowNilai(
				"Label tombol tautan ke Google Play Store untuk pengguna Android "
				+ "(home_text_google_play). Ditampilkan di bagian unduhan aplikasi pada halaman depan portal.",
				"home_text_google_play", "Google Play", 2, null));
		rows.appendChild(createRowNilai(
				"Label tombol tautan ke Apple App Store untuk pengguna iOS "
				+ "(home_text_app_store). Ditampilkan berdampingan dengan tombol Google Play pada halaman depan.",
				"home_text_app_store", "App Store", 2, null));
	}

	protected void initPengaturanAI() {
		Rows rows = createSpan("Pengaturan AI");

		createSpan("Konfigurasi Utama Servlet AI", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan fitur AI Text Generator di editor / text area",
				"AI_GENERATOR_AKTIF", Konfigurasi.AKTIF));

		rows.appendChild(createRowPilihanAiProvider(
				"Provider AI yang aktif dipakai oleh servlet /Ai. Default: Google Gemini API",
				"AI_PROVIDER_AKTIF", "GEMINI"));

		rows.appendChild(createRowNilai(
				"Base URL aktif OpenAI-compatible yang dipakai servlet /Ai. Default Gemini; untuk Ollama lokal gunakan http://IP:11434, untuk proxy gunakan http://38.47.178.42:9002",
				"AI_OPENAI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta/openai"));

		rows.appendChild(createRowNilai(
				"Full endpoint AI jika ingin override base URL. Kosongkan agar servlet otomatis membentuk endpoint chat/completions sesuai provider",
				"AI_OPENAI_URL", ""));

		rows.appendChild(createRowNilai(
				"Model aktif yang dipakai servlet /Ai",
				"AI_OPENAI_MODEL", "gemini-1.5-flash"));

		rows.appendChild(createRowNilaiPassword(
				"API Key aktif. Kosongkan untuk Ollama lokal. Isi jika memakai Gemini, Groq, Cloudflare, OpenAI, DeepSeek, atau provider cloud lain.",
				"AI_OPENAI_KEY", "AIzaSyAFSzVMA8o9DWZpHCsTQT8Mf4M5SN77e2E"));

		rows.appendChild(createRowNilai(
				"Timeout koneksi/read AI dalam milidetik",
				"AI_TIMEOUT_MS", "240000"));

		rows.appendChild(createRowNilai(
				"Batas token output AI / max_tokens / num_predict",
				"AI_NUM_PREDICT", "700"));

		rows.appendChild(createRowNilai(
				"Temperature default AI. Nilai rendah lebih formal/konsisten, nilai tinggi lebih kreatif.",
				"AI_TEMPERATURE_DEFAULT", "0.4"));

		rows.appendChild(createRowActiveDefault(
				"Tampilkan debug log detail di AiGenerateServlet",
				"AI_DEBUG", Konfigurasi.AKTIF));

		rows.appendChild(createRowEditor(
				"System prompt default untuk kebutuhan akademik sekolah/perguruan tinggi",
				"AI_SYSTEM_PROMPT",
				"Anda adalah asisten akademik dan penulisan profesional untuk sekolah dan perguruan tinggi di Indonesia. "
						+ "Gunakan Bahasa Indonesia formal, jelas, rapi, dan siap ditempel ke editor. "
						+ "Untuk kebutuhan akademik, gunakan istilah yang sesuai seperti RPS, CPL, CPMK, Sub-CPMK, CP, TP, ATP, asesmen, rubrik, materi, dan evaluasi jika relevan. "
						+ "Jangan gunakan HTML kecuali diminta secara eksplisit.",
				8, null));

		createSpan("Ollama Lokal / Open-Weight Model", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider Ollama lokal sebagai pilihan AI",
				"AI_OLLAMA_LOCAL_AKTIF", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL Ollama lokal langsung dari server aplikasi",
				"AI_OLLAMA_LOCAL_BASE_URL", "http://192.168.88.128:11434"));
		rows.appendChild(createRowNilai(
				"Base URL Ollama via Apache proxy",
				"AI_OLLAMA_PROXY_BASE_URL", "http://38.47.178.42:9002"));
		rows.appendChild(createRowNilai(
				"Model Ollama untuk akademik umum",
				"AI_OLLAMA_AKADEMIK_MODEL", "qwen2.5:7b"));
		rows.appendChild(createRowNilai(
				"Model Ollama untuk coding Java/JSP/ZKoss",
				"AI_OLLAMA_CODING_MODEL", "qwen2.5-coder:7b"));
		rows.appendChild(createRowNilai(
				"Model Ollama ringan untuk chat cepat",
				"AI_OLLAMA_RINGAN_MODEL", "llama3.2:3b"));

		createSpan("Google Gemini API", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider Google Gemini API sebagai pilihan AI cloud/fallback",
				"AI_GEMINI_AKTIF", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL Gemini OpenAI-compatible",
				"AI_GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta/openai"));
		rows.appendChild(createRowNilai(
				"Model Gemini default",
				"AI_GEMINI_MODEL", "gemini-1.5-flash"));
		rows.appendChild(createRowNilaiPassword(
				"API Key Gemini",
				"AI_GEMINI_KEY", "AIzaSyAFSzVMA8o9DWZpHCsTQT8Mf4M5SN77e2E"));

		createSpan("Groq API", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider Groq sebagai pilihan AI cloud/fallback cepat",
				"AI_GROQ_AKTIF", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL Groq OpenAI-compatible",
				"AI_GROQ_BASE_URL", "https://api.groq.com/openai/v1"));
		rows.appendChild(createRowNilai(
				"Model Groq default",
				"AI_GROQ_MODEL", "llama-3.1-8b-instant"));
		rows.appendChild(createRowNilaiPassword(
				"API Key Groq",
				"AI_GROQ_KEY", ""));

		createSpan("Cloudflare Workers AI", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider Cloudflare Workers AI sebagai pilihan AI cloud/fallback",
				"AI_CLOUDFLARE_AKTIF", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL Cloudflare Workers AI OpenAI-compatible. Ganti ACCOUNT_ID sesuai akun Cloudflare.",
				"AI_CLOUDFLARE_BASE_URL", "https://api.cloudflare.com/client/v4/accounts/ACCOUNT_ID/ai/v1"));
		rows.appendChild(createRowNilai(
				"Model Cloudflare Workers AI default",
				"AI_CLOUDFLARE_MODEL", "@cf/meta/llama-3.1-8b-instruct"));
		rows.appendChild(createRowNilaiPassword(
				"API Token Cloudflare",
				"AI_CLOUDFLARE_KEY", ""));

		createSpan("OpenAI API", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider OpenAI sebagai pilihan AI cloud/fallback",
				"AI_OPENAI_CLOUD_AKTIF", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL OpenAI",
				"AI_OPENAI_CLOUD_BASE_URL", "https://api.openai.com/v1"));
		rows.appendChild(createRowNilai(
				"Model OpenAI default",
				"AI_OPENAI_CLOUD_MODEL", "gpt-4o-mini"));
		rows.appendChild(createRowNilaiPassword(
				"API Key OpenAI",
				"AI_OPENAI_CLOUD_KEY", ""));

		createSpan("DeepSeek API", rows);
		rows.appendChild(createRowActiveDefault(
				"Aktifkan provider DeepSeek sebagai pilihan AI cloud/fallback coding",
				"AI_DEEPSEEK_AKTIF", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Base URL DeepSeek OpenAI-compatible",
				"AI_DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"));
		rows.appendChild(createRowNilai(
				"Model DeepSeek default",
				"AI_DEEPSEEK_MODEL", "deepseek-chat"));
		rows.appendChild(createRowNilaiPassword(
				"API Key DeepSeek",
				"AI_DEEPSEEK_KEY", ""));

		createSpan("Catatan Penggunaan", rows);
		rows.appendChild(createRowNilai(
				"Contoh kode membaca provider aktif",
				"AI_CONTOH_CARA_BACA_KONFIGURASI",
				"String provider = Common.getKonfigurasi(\"AI_PROVIDER_AKTIF\", \"GEMINI\").getNilai(); "
						+ "boolean aktif = Common.bolehKonfigurasi(\"AI_GENERATOR_AKTIF\");",
				3, null));
	}

	protected Row createRowPilihanAiProvider(final String label, final String key, final String defaultValue) {
		final Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);

		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox provider = createComboAiProvider();
		provider.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (provider.getSelectedItem() == null || provider.getSelectedItem().getValue() == null) {
					return;
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
				konfigurasi.setNilai((String) provider.getSelectedItem().getValue());

				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		groupbox.appendChild(provider);
		Common.selectComboItem(provider, konfigurasi.getNilai());

		return row;
	}

	protected Combobox createComboAiProvider() {
		Combobox provider = new Combobox();

		MyComboitemConfig comboitem = new MyComboitemConfig("Ollama Lokal (gratis, internal/LAN)");
		comboitem.setValue("OLLAMA_LOCAL");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Ollama via Apache Proxy");
		comboitem.setValue("OLLAMA_PROXY");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Google Gemini API");
		comboitem.setValue("GEMINI");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Groq API");
		comboitem.setValue("GROQ");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Cloudflare Workers AI");
		comboitem.setValue("CLOUDFLARE");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("OpenAI API");
		comboitem.setValue("OPENAI");
		provider.appendChild(comboitem);

		comboitem = new MyComboitemConfig("DeepSeek API");
		comboitem.setValue("DEEPSEEK");
		provider.appendChild(comboitem);

		provider.setReadonly(true);
		return provider;
	}

	protected void initModulPerpustakaan() {
		Rows rows = (createSpan("Sistem Perpustakaan"));

		// =========================================================================================
		// KONFIGURASI UNGGAH LAMPIRAN PERPUSTAKAAN (PROFIL, FASILITAS, & LAYANAN) - ZK 5 COMPATIBLE
		// =========================================================================================
		Row row; // Deklarasi variabel row untuk digunakan kembali

		// 1. KELOMPOK PROFIL PERPUSTAKAAN
		row = new MyFormRow();
		row.setParent(rows);
		Groupbox	groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("Profil Perpustakaan")));
		
		
		
		// =========================================================================================
		// 1. KELOMPOK PROFIL PERPUSTAKAAN
		// =========================================================================================

		// A. Sejarah Perpustakaan
		String defaultValueSejarah = "<p>Sejarah pendirian Perpustakaan berawal dari komitmen luhur institusi perguruan tinggi untuk menyediakan pusat sumber belajar yang komprehensif, mutakhir, dan relevan dengan perkembangan ilmu pengetahuan. Pada masa awal berdirinya, perpustakaan ini menempati ruangan yang sederhana dengan koleksi cetak yang masih terbatas, berfokus pada buku-buku teks wajib untuk mendukung perkuliahan dasar. Seiring dengan peningkatan jumlah sivitas akademika dan pembukaan program studi baru, perpustakaan mengalami transformasi yang signifikan, baik dari segi infrastruktur fisik maupun sistem tata kelola.</p>"
		    + "<p>Memasuki era digitalisasi, perpustakaan melakukan lompatan strategis dengan mengimplementasikan sistem otomasi perpustakaan secara menyeluruh. Hal ini mencakup digitalisasi katalog, integrasi sistem sirkulasi terpadu, hingga pengembangan repositori institusi untuk mengelola karya ilmiah mahasiswa dan dosen. Saat ini, Perpustakaan tidak hanya berfungsi sebagai tempat penyimpanan buku, melainkan telah berevolusi menjadi <em>Learning Commons</em>—sebuah ruang interaktif yang memfasilitasi diskusi akademik, penelitian kolaboratif, dan inovasi pembelajaran yang selaras dengan nilai-nilai Tridharma Perguruan Tinggi.</p>";
		rows.appendChild(createRowEditor("Keterangan Sejarah", "sejarah_pustaka_default", defaultValueSejarah, 15, null));


		// B. Visi dan Misi
		String defaultValueVisiMisi = "<p><strong>Visi Perpustakaan:</strong></p>"
		    + "<p>Menjadi pusat layanan informasi dan manajemen pengetahuan yang unggul, inovatif, dan berstandar internasional dalam rangka mendukung pencapaian visi universitas sebagai institusi pendidikan yang riset-sentris dan berdaya saing global pada tahun 2030.</p>"
		    + "<p><strong>Misi Perpustakaan:</strong></p>"
		    + "<ol>"
		    + "<li>Menyediakan, mengelola, dan mendiseminasikan sumber informasi cetak maupun digital yang mutakhir, relevan, dan terpercaya guna menunjang kegiatan pendidikan, pengajaran, penelitian, dan pengabdian kepada masyarakat.</li>"
		    + "<li>Memberikan layanan perpustakaan prima yang berbasis teknologi informasi dan komunikasi (TIK) dengan mengedepankan prinsip kemudahan akses, keramahan, dan kepuasan pemustaka.</li>"
		    + "<li>Mengembangkan literasi informasi (<em>information literacy</em>) bagi seluruh sivitas akademika agar memiliki keterampilan dalam menelusuri, mengevaluasi, dan memanfaatkan informasi secara etis dan legal.</li>"
		    + "<li>Melestarikan hasil karya intelektual institusi (<em>local content</em>) melalui pembangunan pangkalan data repositori institusi yang terbuka dan terintegrasi.</li>"
		    + "<li>Menjalin kerja sama strategis (jejaring perpustakaan) di tingkat regional, nasional, maupun internasional untuk memperluas jangkauan akses literatur bagi pemustaka.</li>"
		    + "</ol>";
		rows.appendChild(createRowEditor("Visi dan Misi", "visi_misi_pustaka_default", defaultValueVisiMisi, 15, null));


		// C. Struktur Organisasi
		String defaultValueStruktur = "<p>Untuk menjamin penyelenggaraan layanan yang profesional dan terstruktur, Perpustakaan dikelola oleh sumber daya manusia yang kompeten dengan pembagian tugas yang jelas. Struktur organisasi perpustakaan terdiri dari:</p>"
		    + "<ul>"
		    + "<li><strong>Kepala Perpustakaan:</strong> Bertanggung jawab penuh atas perencanaan strategis, penganggaran, pembinaan SDM, serta evaluasi seluruh program kerja perpustakaan guna memastikan keselarasan dengan kebijakan universitas.</li>"
		    + "<li><strong>Sub-Bagian Tata Usaha:</strong> Mengelola urusan persuratan, administrasi kepegawaian, inventarisasi aset, serta laporan kinerja perpustakaan secara berkala.</li>"
		    + "<li><strong>Layanan Teknis (Pengolahan):</strong> Divisi yang bertugas menangani akuisisi bahan pustaka baru, inventarisasi, klasifikasi, pengkatalogan, penyelesaian fisik buku, hingga digitalisasi dokumen karya ilmiah.</li>"
		    + "<li><strong>Layanan Pemustaka (Sirkulasi & Referensi):</strong> Ujung tombak pelayanan yang berhadapan langsung dengan pengguna. Divisi ini mengelola pendaftaran anggota, peminjaman, pengembalian, penagihan denda, serta memberikan bimbingan referensi dan literasi penelusuran informasi.</li>"
		    + "<li><strong>Divisi Teknologi Informasi (TI):</strong> Mengelola pemeliharaan perangkat keras, jaringan, sistem otomasi perpustakaan, repositori institusi, dan integrasi pangkalan data jurnal elektronik.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Struktur Organisasi", "struktur_organisasi_pustaka_default", defaultValueStruktur, 15, null));


		// D. Tata Tertib
		String defaultValueTataTertib = "<p>Demi menjaga kenyamanan, ketertiban, dan keamanan bersama di lingkungan perpustakaan, setiap sivitas akademika dan pengunjung luar diwajibkan untuk mematuhi regulasi berikut:</p>"
		    + "<ol>"
		    + "<li><strong>Kewajiban Pengunjung:</strong> Setiap pemustaka wajib berpakaian rapi, sopan, dan mengenakan sepatu. Mahasiswa wajib mengenakan Kartu Tanda Mahasiswa (KTM) atau Kartu Anggota Perpustakaan yang masih aktif selama berada di area perpustakaan.</li>"
		    + "<li><strong>Penitipan Barang:</strong> Tas, jaket, map, dan kantong plastik harus dititipkan di loker yang telah disediakan. Pemustaka hanya diperkenankan membawa barang berharga (dompet, laptop, gawai) beserta alat tulis ke dalam ruang baca. Pihak perpustakaan tidak bertanggung jawab atas kehilangan barang berharga.</li>"
		    + "<li><strong>Kenyamanan Ruang Baca:</strong> Dilarang keras membuat kegaduhan, berbicara dengan suara keras, atau melakukan aktivitas yang dapat mengganggu konsentrasi pemustaka lain. Dilarang makan, minum, dan merokok di seluruh area ruang koleksi dan ruang baca, kecuali di area kantin/lounge yang telah ditentukan.</li>"
		    + "<li><strong>Penggunaan Koleksi:</strong> Pemustaka dipersilakan mengambil buku dari rak secara mandiri. Buku yang telah selesai dibaca dilarang dikembalikan ke rak semula oleh pemustaka; buku wajib diletakkan di meja/troli khusus yang telah disediakan untuk menghindari kesalahan penempatan (<em>mis-shelving</em>).</li>"
		    + "<li><strong>Sanksi Pelanggaran:</strong> Pelanggaran terhadap tata tertib ini akan dikenakan sanksi bertahap, mulai dari teguran lisan, pencabutan hak akses layanan perpustakaan secara sementara, hingga pelaporan kepada program studi/fakultas yang bersangkutan.</li>"
		    + "</ol>";
		rows.appendChild(createRowEditor("Tata Tertib", "tata_tertib_pustaka_default", defaultValueTataTertib, 15, null));


		// =========================================================================================
		// 2. KELOMPOK FASILITAS DAN LAYANAN
		// =========================================================================================

		// A. Sarana dan Prasarana
		String defaultValueSarpras = "<p>Perpustakaan berdedikasi untuk menciptakan atmosfer akademik yang kondusif dengan menyediakan infrastruktur yang modern dan ergonomis. Fasilitas sarana dan prasarana yang kami miliki meliputi:</p>"
		    + "<ul>"
		    + "<li><strong>Gedung Representatif:</strong> Terdiri dari beberapa lantai yang dikategorikan berdasarkan jenis layanan dan koleksi, dilengkapi dengan sistem pendingin ruangan (AC) sentral, pencahayaan berstandar ergonomis, serta fasilitas kebersihan yang terawat dengan baik.</li>"
		    + "<li><strong>Ruang Diskusi Kolaboratif:</strong> Ruangan kedap suara yang dapat dipesan oleh mahasiswa atau dosen untuk keperluan diskusi kelompok, bimbingan tugas akhir, maupun rapat proyek penelitian, dilengkapi dengan papan tulis interaktif dan monitor presentasi.</li>"
		    + "<li><strong>Area Loker Penitipan:</strong> Sistem loker berbasis kunci atau kartu pintar yang aman untuk menitipkan barang bawaan pengunjung sebelum memasuki area sirkulasi utama.</li>"
		    + "<li><strong>Aksesibilitas Difabel:</strong> Perpustakaan dilengkapi dengan fasilitas ramah difabel, termasuk ramp (jalur kursi roda), lift khusus, dan toilet yang dirancang khusus untuk mempermudah aksesibilitas seluruh elemen sivitas akademika tanpa terkecuali.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Sarana dan Prasarana", "sarpras_pustaka_default", defaultValueSarpras, 15, null));


		// B. Free Wifi
		String defaultValueWifi = "<p>Untuk mendukung paradigma pembelajaran digital dan mobilitas informasi, seluruh gedung perpustakaan telah dilingkupi oleh jaringan nirkabel (Free Wi-Fi) berkecepatan tinggi. Ketentuan layanan Wi-Fi perpustakaan adalah sebagai berikut:</p>"
		    + "<ul>"
		    + "<li><strong>Autentikasi Terpusat:</strong> Akses Wi-Fi dikelola melalui sistem otentikasi tunggal (<em>Single Sign-On / SSO</em>). Mahasiswa dan staf diwajibkan untuk masuk menggunakan kredensial akademik resmi (NIM/NIP dan kata sandi) melalui portal otorisasi jaringan institusi.</li>"
		    + "<li><strong>Kecepatan dan Kuota Bandwidth:</strong> Perpustakaan menyediakan <em>bandwidth</em> prioritas untuk menunjang aktivitas penelusuran jurnal, pengunduhan referensi akademik, dan pembelajaran daring.</li>"
		    + "<li><strong>Kebijakan Penggunaan Wajar (Fair Usage Policy):</strong> Pengguna jaringan dilarang keras menggunakan Wi-Fi perpustakaan untuk aktivitas ilegal, peretasan, pengunduhan berkas bajakan, atau mengakses situs-situs yang melanggar norma susila dan undang-undang yang berlaku. Tim IT Perpustakaan memantau lalu lintas jaringan dan berhak memblokir akses pengguna yang terindikasi melakukan pelanggaran.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Fasilitas Free Wifi", "wifi_pustaka_default", defaultValueWifi, 15, null));


		// C. Ruang Baca
		String defaultValueRuangBaca = "<p>Memahami bahwa pemustaka memiliki preferensi belajar yang beragam, perpustakaan menyediakan tata ruang baca yang diklasifikasikan ke dalam beberapa zona khusus:</p>"
		    + "<ul>"
		    + "<li><strong>Zona Tenang (Quiet Zone):</strong> Area yang didedikasikan mutlak untuk pemustaka yang membutuhkan tingkat konsentrasi tinggi. Di zona ini, dilarang keras melakukan percakapan, menerima panggilan telepon, atau membunyikan perangkat elektronik.</li>"
		    + "<li><strong>Zona Interaktif (Lounge Area):</strong> Ruang baca kasual dengan sofa yang nyaman, dirancang untuk relaksasi dan membaca ringan. Pada area ini, percakapan dengan nada suara rendah masih diperkenankan.</li>"
		    + "<li><strong>Karel Baca Pribadi (Individual Carrels):</strong> Bilik-bilik meja mandiri yang dilengkapi dengan partisi pembatas dan stop kontak listrik, dikhususkan bagi mahasiswa tingkat akhir yang sedang menyelesaikan skripsi, tesis, atau disertasi.</li>"
		    + "<li><strong>Ruang Multimedia:</strong> Area baca digital yang dilengkapi dengan perangkat komputer terkoneksi internet dan perpustakaan digital (e-library), ditujukan untuk memutar koleksi audio-visual dan mengakses pangkalan data elektronik.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Ruang Baca", "ruang_baca_pustaka_default", defaultValueRuangBaca, 15, null));


		// =========================================================================================
		// 3. LAYANAN SIRKULASI
		// =========================================================================================

		// A. Waktu Layanan
		String defaultValueWaktuLayanan = "<p>Dalam rangka memfasilitasi kebutuhan riset dan pembelajaran yang padat, Perpustakaan menetapkan jam operasional pelayanan secara ekstensif dengan rincian jadwal sebagai berikut:</p>"
		    + "<ul>"
		    + "<li><strong>Senin - Kamis:</strong> Buka pukul 08.00 WIB s/d 16.00 WIB. Terdapat jeda istirahat pelayanan sirkulasi pada pukul 12.00 - 13.00 WIB, namun ruang baca tetap dapat diakses oleh pemustaka.</li>"
		    + "<li><strong>Jumat:</strong> Buka pukul 08.00 WIB s/d 16.00 WIB. Jeda istirahat pelayanan dan ibadah pada pukul 11.30 - 13.30 WIB.</li>"
		    + "<li><strong>Sabtu:</strong> Buka pukul 09.00 WIB s/d 13.00 WIB (Berlaku khusus untuk layanan sirkulasi terbatas dan pemanfaatan ruang baca).</li>"
		    + "<li><strong>Minggu dan Hari Libur Nasional:</strong> Layanan fisik perpustakaan TUTUP. Namun, portal repositori, e-journal, dan katalog perpustakaan digital tetap dapat diakses secara daring 24/7 dari mana saja.</li>"
		    + "</ul>"
		    + "<p><em>Catatan:</em> Menjelang masa Ujian Tengah Semester (UTS) dan Ujian Akhir Semester (UAS), perpustakaan dapat memberlakukan perpanjangan jam operasional yang akan diinformasikan kemudian melalui papan pengumuman dan portal akademik.</p>";
		rows.appendChild(createRowEditor("Waktu Layanan", "waktu_layanan_pustaka_default", defaultValueWaktuLayanan, 15, null));


		// B. Peminjaman
		String defaultValuePeminjaman = "<p>Layanan peminjaman koleksi sirkulasi merupakan layanan inti yang disediakan bagi seluruh anggota perpustakaan yang berstatus aktif. Ketentuan baku dalam layanan peminjaman adalah sebagai berikut:</p>"
		    + "<ol>"
		    + "<li><strong>Syarat Utama:</strong> Pemustaka wajib datang langsung dan menunjukkan Kartu Anggota Perpustakaan/KTM miliknya sendiri. Peminjaman tidak dapat diwakilkan oleh pihak mana pun demi menghindari penyalahgunaan akun.</li>"
		    + "<li><strong>Kuota dan Durasi:</strong> Mahasiswa jenjang Sarjana (S1) berhak meminjam maksimal 3 (tiga) eksemplar buku dengan batas waktu peminjaman selama 7 (tujuh) hari kalender. Dosen dan Mahasiswa Pascasarjana (S2/S3) berhak meminjam maksimal 5 (lima) eksemplar buku selama 14 (empat belas) hari kalender.</li>"
		    + "<li><strong>Bahan Pustaka Non-Sirkulasi:</strong> Koleksi referensi (kamus, ensiklopedia, atlas), jurnal cetak, majalah, surat kabar, dan koleksi karya ilmiah (skripsi/tesis/disertasi) <strong>tidak untuk dipinjamkan</strong> bawa pulang, dan hanya diperkenankan untuk dibaca di tempat atau difotokopi sebagian sesuai ketentuan hak cipta.</li>"
		    + "</ol>";
		rows.appendChild(createRowEditor("Peminjaman Sirkulasi", "peminjaman_pustaka_default", defaultValuePeminjaman, 15, null));


		// C. Peminjaman Online
		String defaultValuePinjamOnline = "<p>Menjawab tantangan era digital dan mengutamakan efisiensi waktu, Perpustakaan menghadirkan layanan pemesanan dan peminjaman secara daring (<em>Online Reservation</em>). Layanan ini dirancang untuk memudahkan pemustaka dalam mengamankan koleksi yang dibutuhkan sebelum datang ke perpustakaan.</p>"
		    + "<ul>"
		    + "<li><strong>Prosedur Reservasi:</strong> Anggota dapat masuk (<em>login</em>) ke portal katalog perpustakaan, menelusuri buku yang diinginkan, dan menekan tombol 'Pesan/Booking'. Sistem hanya akan menyetujui reservasi jika buku tersebut berstatus 'Tersedia' di rak.</li>"
		    + "<li><strong>Batas Waktu Pengambilan:</strong> Setelah sistem mengeluarkan nomor reservasi dan notifikasi persetujuan, pemustaka diberikan tenggat waktu maksimal 1x24 jam kerja untuk mendatangi meja sirkulasi perpustakaan guna mengambil fisik buku tersebut.</li>"
		    + "<li><strong>Pembatalan Otomatis:</strong> Apabila pemustaka gagal mengambil buku dalam kurun waktu yang telah ditetapkan, maka sistem sirkulasi akan membatalkan status pesanan secara otomatis, dan buku tersebut akan kembali tersedia untuk dipinjam oleh pemustaka lain.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Peminjaman Online", "peminjaman_online_pustaka_default", defaultValuePinjamOnline, 15, null));


		// D. Pengembalian
		String defaultValuePengembalian = "<p>Pengembalian koleksi pustaka yang tepat waktu sangat krusial untuk menjamin sirkulasi literatur yang merata bagi seluruh pengguna. Berikut adalah regulasi layanan pengembalian:</p>"
		    + "<ol>"
		    + "<li><strong>Prosedur Pengembalian:</strong> Pemustaka wajib menyerahkan buku langsung kepada petugas di meja sirkulasi atau menggunakan fasilitas <em>Book Drop</em> (jika tersedia di luar jam operasional). Petugas akan memindai kode bar buku dan memastikan status pinjaman pada sistem berubah menjadi selesai.</li>"
		    + "<li><strong>Sanksi Keterlambatan:</strong> Keterlambatan pengembalian bahan pustaka akan dikenakan sanksi denda administratif sebesar Rp1.000,- (Seribu Rupiah) per buku, per hari keterlambatan. Denda ini diberlakukan semata-mata untuk mendisiplinkan pengguna, bukan sebagai sumber pendapatan institusi.</li>"
		    + "<li><strong>Ganti Rugi Kerusakan atau Kehilangan:</strong> Jika buku yang dikembalikan ditemukan dalam kondisi rusak berat (halaman robek, basah) atau hilang, maka peminjam <strong>wajib mengganti dengan buku yang sama persis</strong> (judul, pengarang, penerbit, dan edisi terbaru) atau membayar denda penggantian senilai dua kali lipat dari harga buku di pasaran saat ini, ditambah biaya administrasi pengolahan.</li>"
		    + "</ol>";
		rows.appendChild(createRowEditor("Pengembalian Koleksi", "pengembalian_pustaka_default", defaultValuePengembalian, 15, null));


		// E. Perpanjangan
		String defaultValuePerpanjangan = "<p>Layanan perpanjangan durasi peminjaman disediakan bagi pemustaka yang masih membutuhkan referensi terkait namun batas waktu peminjamannya hampir habis. Ketentuan perpanjangan bahan pustaka diatur secara ketat sebagai berikut:</p>"
		    + "<ul>"
		    + "<li><strong>Frekuensi Perpanjangan:</strong> Masa peminjaman buku dapat diperpanjang maksimal 1 (satu) kali periode pinjaman (misalnya tambahan 7 hari untuk mahasiswa S1).</li>"
		    + "<li><strong>Batas Waktu Perpanjangan:</strong> Proses perpanjangan wajib dilakukan paling lambat 1 (satu) hari sebelum jatuh tempo tanggal pengembalian. Jika buku telah melewati tanggal jatuh tempo, sistem akan menolak opsi perpanjangan dan pemustaka wajib mengembalikan fisik buku terlebih dahulu serta melunasi denda keterlambatan yang berjalan.</li>"
		    + "<li><strong>Mekanisme Eksekusi:</strong> Perpanjangan dapat dilakukan secara mandiri oleh pemustaka melalui panel 'Beranda Anggota' pada portal perpustakaan, atau dengan mendatangi langsung petugas sirkulasi.</li>"
		    + "<li><strong>Pengecualian:</strong> Perpanjangan akan ditolak secara otomatis oleh sistem apabila buku tersebut sedang masuk dalam daftar tunggu pemesanan (<em>booking list</em>) oleh mahasiswa atau pemustaka lain.</li>"
		    + "</ul>";
		rows.appendChild(createRowEditor("Perpanjangan Pinjaman", "perpanjangan_pustaka_default", defaultValuePerpanjangan, 15, null));
		
		
		// =========================================================================================
		// KONFIGURASI MENU FOOTER (BANTUAN & REFERENSI)
		// =========================================================================================

		// A. PANDUAN PEMINJAMAN (Super Lengkap)
		String defaultPanduan = "<p><strong>Panduan Komprehensif Layanan Peminjaman Perpustakaan Digital</strong></p>"
		    + "<p>Perpustakaan menyediakan layanan peminjaman bahan pustaka (sirkulasi) sebagai fasilitas fundamental guna mendukung kegiatan akademik, pengajaran, dan penelitian bagi seluruh sivitas akademika. Layanan ini diselenggarakan dengan mengedepankan prinsip keadilan akses, efisiensi, dan tertib administrasi. Agar pemanfaatan layanan sirkulasi dapat berjalan dengan optimal, setiap pemustaka diwajibkan untuk memahami dan mematuhi seluruh prosedur dan ketentuan peminjaman yang berlaku secara menyeluruh.</p>"
		    + "<p><strong>1. Syarat dan Ketentuan Keanggotaan</strong><br>"
		    + "Hak untuk melakukan peminjaman koleksi fisik perpustakaan diberikan secara eksklusif kepada sivitas akademika yang berstatus aktif. Mahasiswa wajib telah menyelesaikan proses registrasi akademik pada semester berjalan. Peminjaman mutlak memerlukan Kartu Tanda Mahasiswa (KTM) fisik atau Kartu Anggota Perpustakaan digital yang terintegrasi di dalam sistem aplikasi kampus. Penggunaan identitas milik orang lain untuk tujuan peminjaman adalah pelanggaran administratif berat yang dapat mengakibatkan pencabutan hak akses layanan perpustakaan bagi kedua belah pihak yang terlibat.</p>"
		    + "<p><strong>2. Kuota dan Durasi Peminjaman Berdasarkan Strata</strong><br>"
		    + "Demi menjamin pemerataan distribusi literatur, perpustakaan memberlakukan kebijakan pembatasan kuota dan durasi pinjaman berdasarkan jenjang atau status akademik pemustaka:"
		    + "<ul>"
		    + "<li><strong>Mahasiswa Program Diploma dan Sarjana (D3/D4/S1):</strong> Diperkenankan meminjam maksimal 3 (tiga) eksemplar buku dengan judul yang berbeda, dengan batas waktu peminjaman selama 7 (tujuh) hari kalender.</li>"
		    + "<li><strong>Mahasiswa Program Pascasarjana (Magister dan Doktoral):</strong> Diperkenankan meminjam maksimal 5 (lima) eksemplar buku dengan batas waktu peminjaman selama 14 (empat belas) hari kalender.</li>"
		    + "<li><strong>Dosen, Peneliti, dan Tenaga Kependidikan:</strong> Diperkenankan meminjam maksimal 7 (tujuh) eksemplar buku dengan batas waktu peminjaman selama 30 (tiga puluh) hari kalender, guna mendukung penyusunan materi perkuliahan dan riset jangka panjang.</li>"
		    + "</ul></p>"
		    + "<p><strong>3. Prosedur Peminjaman Fisik (On-Site)</strong><br>"
		    + "Pemustaka dipersilakan untuk menelusuri koleksi melalui terminal Katalog Publik (OPAC) yang tersedia di area lobi, kemudian mengambil fisik buku secara mandiri dari rak penyimpanan. Setelah memastikan kondisi fisik buku tidak mengalami kerusakan (halaman robek, coretan, atau cacat lainnya), pemustaka wajib membawa buku tersebut ke meja layanan sirkulasi. Petugas akan memindai kode batang (<em>barcode</em>) pada buku dan mengaitkannya dengan akun pemustaka. Struk peminjaman digital akan dikirimkan ke email institusi pemustaka sebagai bukti transaksi yang sah.</p>"
		    + "<p><strong>4. Prosedur Peminjaman Daring (Online Reservation)</strong><br>"
		    + "Untuk meningkatkan efisiensi waktu, perpustakaan memfasilitasi pemesanan buku secara daring (<em>booking</em>). Pemustaka dapat masuk ke portal anggota, mencari buku, dan menekan tombol pemesanan. Sistem akan mengamankan buku tersebut selama 1x24 jam kerja. Pemustaka cukup mendatangi loket khusus reservasi untuk mengambil buku tanpa perlu mencarinya di rak. Apabila dalam kurun waktu yang ditentukan buku tidak diambil, sistem akan membatalkan pesanan secara otomatis dan buku akan dikembalikan ke sirkulasi umum.</p>"
		    + "<p><strong>5. Koleksi Non-Sirkulasi (Tidak Dipinjamkan)</strong><br>"
		    + "Perlu digarisbawahi bahwa tidak semua bahan pustaka dapat dibawa pulang. Koleksi khusus seperti buku referensi (kamus, ensiklopedia, atlas, direktori), koleksi tandon (buku wajib cadangan), terbitan berkala (jurnal cetak, majalah, koran), koleksi langka (manuskrip), serta laporan karya ilmiah akhir (skripsi, tesis, disertasi) bersifat <em>read-only</em>. Koleksi-koleksi ini hanya diperkenankan untuk dibaca di area ruang baca perpustakaan, atau difotokopi pada bagian-bagian tertentu saja dengan mematuhi batasan hukum Hak Cipta yang berlaku.</p>"
		    + "<p><strong>6. Kebijakan Perpanjangan (Renewal)</strong><br>"
		    + "Apabila pemustaka masih membutuhkan bahan pustaka melewati tenggat waktu yang telah ditentukan, perpanjangan durasi pinjaman dapat dilakukan maksimal 1 (satu) kali periode (misal: tambahan 7 hari untuk S1). Perpanjangan dapat dilakukan melalui portal perpustakaan secara mandiri sebelum tanggal jatuh tempo. Sistem akan menolak perpanjangan jika buku tersebut sedang dalam status antrean pemesanan (<em>waiting list</em>) oleh sivitas akademika lain, atau jika pemustaka memiliki tanggungan denda keterlambatan yang belum diselesaikan.</p>";
		rows.appendChild(createRowEditor("Panduan Peminjaman", "panduan_pustaka_default", defaultPanduan, 15, null));


		// B. TANYA JAWAB / FAQ (Super Lengkap)
		String defaultFaq = "<p><strong>Pertanyaan Umum (Frequently Asked Questions - FAQ) Layanan Perpustakaan</strong></p>"
		    + "<p>Halaman ini disusun untuk memberikan jawaban atas berbagai pertanyaan teknis dan administratif yang paling sering diajukan oleh sivitas akademika terkait pemanfaatan layanan, fasilitas, dan tata tertib perpustakaan. Kami menyarankan Anda untuk membaca rubrik ini secara saksama sebelum menghubungi pusat bantuan pelanggan (<em>helpdesk</em>) perpustakaan.</p>"
		    + "<p><strong>Q1: Bagaimana cara mengaktifkan keanggotaan perpustakaan saya?</strong><br>"
		    + "<strong>Jawaban:</strong> Bagi mahasiswa baru, keanggotaan perpustakaan secara otomatis aktif bersamaan dengan terbitnya Nomor Induk Mahasiswa (NIM) dan Kartu Tanda Mahasiswa (KTM) fisik maupun digital. Anda tidak perlu melakukan pendaftaran ulang. Akun perpustakaan terintegrasi menggunakan <em>Single Sign-On</em> (SSO) institusi. Anda dapat langsung menggunakan identitas SSO Anda untuk masuk ke portal perpustakaan dan mengakses seluruh layanan digital maupun peminjaman fisik.</p>"
		    + "<p><strong>Q2: Mengapa saya tidak bisa masuk (login) ke portal perpustakaan?</strong><br>"
		    + "<strong>Jawaban:</strong> Kegagalan akses masuk umumnya disebabkan oleh beberapa faktor: (a) Anda belum menyelesaikan proses Her-Registrasi akademik pada semester berjalan; (b) Terdapat kesalahan penulisan kata sandi SSO Anda; atau (c) Akun Anda sedang dalam status penangguhan (<em>suspended</em>) akibat belum mengembalikan buku yang telah melewati batas waktu peminjaman lebih dari 30 hari. Silakan periksa status akademik Anda atau hubungi bagian layanan sirkulasi untuk pengecekan status akun.</p>"
		    + "<p><strong>Q3: Berapa denda yang harus dibayar jika terlambat mengembalikan buku?</strong><br>"
		    + "<strong>Jawaban:</strong> Institusi memberlakukan denda keterlambatan sebesar Rp 1.000,- (Seribu Rupiah) per eksemplar buku, per hari kalender. Denda ini akan terus berakumulasi hingga hari Anda mengembalikan buku tersebut. Perlu ditekankan bahwa penerapan denda bukanlah upaya komersialisasi institusi, melainkan instrumen edukasi kedisiplinan agar sirkulasi literatur dapat terdistribusi secara adil dan merata kepada ribuan sivitas akademika lainnya yang juga membutuhkan buku tersebut.</p>"
		    + "<p><strong>Q4: Apa yang harus saya lakukan jika buku perpustakaan yang saya pinjam hilang atau rusak parah?</strong><br>"
		    + "<strong>Jawaban:</strong> Pemustaka wajib segera melapor kepada petugas sirkulasi agar denda keterlambatan (jika ada) dihentikan arusnya. Sesuai dengan Peraturan Tata Tertib Perpustakaan, pemustaka wajib mengganti buku yang hilang/rusak dengan <strong>buku yang sama persis</strong> (mencakup judul, pengarang, penerbit, dan edisi/tahun terbit terbaru). Jika buku tersebut sudah tidak dicetak lagi (<em>out of print</em>), pemustaka wajib menggantinya dengan buku subjek sejenis yang direkomendasikan oleh Kepala Perpustakaan, atau membayar uang tunai sebesar dua kali lipat harga pasar buku tersebut ditambah biaya administrasi pengolahan sebesar Rp 50.000,-.</p>"
		    + "<p><strong>Q5: Bagaimana cara mengakses E-Journal internasional berbayar dari luar kampus (rumah/kos)?</strong><br>"
		    + "<strong>Jawaban:</strong> Pangkalan data jurnal ilmiah elektronik yang dilanggan oleh institusi (seperti Scopus, ScienceDirect, IEEE, ProQuest) dapat diakses dari luar jaringan Wi-Fi kampus menggunakan sistem <em>Remote Access</em> (VPN / Proxy Institusi). Silakan masuk ke portal perpustakaan, pilih menu 'Layanan Referensi', lalu klik 'Akses E-Journal'. Sistem akan meminta Anda memasukkan akun SSO kembali untuk memverifikasi bahwa Anda adalah sivitas akademika aktif, sebelum mengarahkan Anda ke portal jurnal tersebut dengan status <em>Institutional Access</em>.</p>"
		    + "<p><strong>Q6: Apakah masyarakat umum atau mahasiswa dari universitas lain boleh berkunjung ke perpustakaan ini?</strong><br>"
		    + "<strong>Jawaban:</strong> Tentu saja. Perpustakaan kami mendukung prinsip inklusivitas pendidikan. Pengunjung dari luar institusi diperkenankan membaca buku di tempat dan memanfaatkan fasilitas ruang baca dengan syarat meninggalkan kartu identitas diri resmi (KTP/SIM) di meja resepsionis untuk mendapatkan 'Kartu Pengunjung Tamu' (<em>Visitor Pass</em>). Namun, pengunjung tamu tidak diberikan hak untuk meminjam buku untuk dibawa pulang, dan tidak diberikan akses ke jaringan Wi-Fi prioritas internal kampus.</p>"
		    + "<p><strong>Q7: Saya sedang menyusun tugas akhir dan butuh buku spesifik, namun tidak ada di katalog. Bisakah saya mengusulkan pengadaan buku tersebut?</strong><br>"
		    + "<strong>Jawaban:</strong> Sangat bisa. Kami mendorong partisipasi aktif pemustaka dalam pengembangan koleksi. Anda dapat mengajukan usulan pengadaan bahan pustaka melalui menu 'Usulan Buku' di portal anggota. Tim Akuisisi kami akan mengevaluasi usulan tersebut berdasarkan relevansi kurikulum, urgensi, dan ketersediaan anggaran. Jika disetujui, buku tersebut akan diprioritaskan pada siklus pembelian periode berikutnya, dan Anda akan menjadi orang pertama yang mendapatkan notifikasi ketika buku tersebut telah selesai diolah dan siap dipinjam.</p>";
		rows.appendChild(createRowEditor("Tanya Jawab (FAQ)", "faq_pustaka_default", defaultFaq, 15, null));


		// C. JAM OPERASIONAL / WAKTU LAYANAN (Super Lengkap)
		String defaultWaktuLayanan = "<p><strong>Regulasi Jam Operasional dan Ketentuan Waktu Layanan Perpustakaan</strong></p>"
		    + "<p>Sebagai pusat jantung akademik perguruan tinggi, Perpustakaan berkomitmen untuk memberikan fleksibilitas waktu akses yang maksimal guna memfasilitasi kebutuhan riset, pengajaran, dan literasi mahasiswa maupun dosen. Jam operasional dirancang sedemikian rupa dengan memperhatikan ritme kalender akademik tahunan, beban puncak kunjungan menjelang masa ujian, serta alokasi waktu yang esensial bagi perawatan sistem, kebersihan gedung, dan pemeliharaan koleksi fisik (<em>stock opname</em>).</p>"
		    + "<p><strong>1. Jam Operasional Reguler (Semester Berjalan)</strong><br>"
		    + "Selama periode perkuliahan aktif (semester ganjil maupun genap), perpustakaan membuka pintu layanan sirkulasi dan pemanfaatan fasilitas ruang baca secara penuh dengan rincian jadwal operasional harian sebagai berikut:"
		    + "<ul>"
		    + "<li><strong>Senin s.d. Kamis:</strong> Layanan buka mulai pukul 08:00 WIB hingga 16:30 WIB. Terdapat jeda istirahat pelayanan di meja sirkulasi pada pukul 12:00 - 13:00 WIB. Selama jeda istirahat ini, mahasiswa tetap diperkenankan berada di dalam ruang baca, namun proses transaksi peminjaman, pengembalian, dan layanan administrasi keanggotaan akan dihentikan sementara.</li>"
		    + "<li><strong>Jumat:</strong> Layanan buka mulai pukul 08:00 WIB hingga 16:30 WIB. Demi menghormati waktu ibadah salat Jumat, seluruh aktivitas pelayanan, baik meja sirkulasi maupun layanan referensi, ditutup total mulai pukul 11:30 WIB hingga 13:30 WIB. Area ruang baca juga akan dikosongkan sementara selama durasi tersebut.</li>"
		    + "<li><strong>Sabtu:</strong> Layanan terbatas dibuka mulai pukul 09:00 WIB hingga 13:00 WIB tanpa jam istirahat. Pada hari Sabtu, pelayanan difokuskan pada sirkulasi pengembalian buku dan penyediaan ruang baca mandiri. Layanan konsultasi referensi mendalam (seperti bimbingan pencarian jurnal) tidak tersedia di hari Sabtu.</li>"
		    + "</ul></p>"
		    + "<p><strong>2. Jam Operasional Ekstensi (Masa Ujian Tengah & Akhir Semester)</strong><br>"
		    + "Memahami bahwa kebutuhan akan ruang belajar yang tenang meningkat drastis menjelang dan selama periode Ujian Tengah Semester (UTS) dan Ujian Akhir Semester (UAS), perpustakaan akan menerapkan kebijakan perpanjangan waktu layanan (<em>Extended Hours</em>). Kebijakan ini biasanya mulai diberlakukan H-7 sebelum ujian pertama berlangsung."
		    + "<ul>"
		    + "<li><strong>Senin s.d. Jumat (Masa Ujian):</strong> Layanan ruang baca, fasilitas Wi-Fi, dan karel diskusi diperpanjang operasionalnya hingga pukul 20:00 WIB (Malam). Namun demikian, layanan sirkulasi (peminjaman buku fisik) tetap ditutup pada pukul 16:30 WIB. Petugas yang berjaga di sif malam berfokus pada penjagaan keamanan dan ketertiban gedung.</li>"
		    + "<li><strong>Sabtu (Masa Ujian):</strong> Layanan ruang baca diperpanjang hingga pukul 15:00 WIB.</li>"
		    + "</ul></p>"
		    + "<p><strong>3. Jam Operasional Masa Libur Jeda Semester (Intersesi)</strong><br>"
		    + "Pada saat kalender akademik memasuki masa libur antar semester (setelah UAS hingga menjelang perkuliahan semester baru), mobilitas sivitas akademika umumnya menurun drastis. Oleh karena itu, jam operasional perpustakaan akan mengalami penyesuaian untuk mendukung efisiensi energi bangunan dan memberikan waktu bagi staf untuk melakukan penataan ulang koleksi (<em>shelving</em> besar-besaran) serta perbaikan infrastruktur. Pada masa libur semester, perpustakaan hanya beroperasi pada hari <strong>Senin s.d. Jumat dari pukul 08:30 WIB hingga 15:00 WIB</strong>, dan tutup sepenuhnya pada akhir pekan (Sabtu & Minggu).</p>"
		    + "<p><strong>4. Layanan Digital 24/7 dan Ketentuan Hari Libur Nasional</strong><br>"
		    + "Perpustakaan <strong>tutup secara fisik</strong> pada hari Minggu, hari cuti bersama, dan seluruh Hari Libur Nasional yang ditetapkan oleh pemerintah Republik Indonesia (tanggal merah). Meskipun gedung fisik terkunci, infrastruktur layanan perpustakaan digital kami beroperasi tanpa henti (24/7, 365 hari setahun). Sivitas akademika tetap dapat melakukan penelusuran Katalog Bersama (OPAC), memesan buku (<em>online booking</em>), mengakses jurnal elektronik (e-Journal), membaca buku elektronik (e-Book), serta mengunduh manuskrip skripsi/tesis melalui portal repositori institusi dari mana pun mereka berada.</p>"
		    + "<p><strong>5. Keadaan Kahar (Force Majeure) dan Pemeliharaan Sistem</strong><br>"
		    + "Dalam kondisi-kondisi pengecualian tertentu yang berada di luar kendali manajemen, seperti pemadaman listrik skala besar, bencana alam, ancaman keamanan gedung, atau pemeliharaan server pangkalan data secara darurat (<em>emergency server maintenance</em>), Kepala Perpustakaan berwenang penuh untuk menutup operasional fisik maupun akses portal digital sewaktu-waktu tanpa pemberitahuan sebelumnya. Kebijakan denda keterlambatan akan secara otomatis dibekukan (dianggap libur) selama masa gangguan sistem atau penutupan darurat tersebut berlangsung.</p>";
		rows.appendChild(createRowEditor("Jam Operasional / Layanan", "waktu_layanan_pustaka_default", defaultWaktuLayanan, 15, null));


		// =========================================================================================
		// 4. LAYANAN REFERENSI
		// =========================================================================================

		// A. Katalog Bersama (E-Library)
		String defaultValueKatalogBersama = "<p>Sebagai bagian dari komitmen keterbukaan informasi, Perpustakaan mengembangkan layanan Katalog Bersama (E-Library) yang mengintegrasikan meta-data dan repositori digital ke dalam satu pintu penelusuran (<em>Single Window Search</em>).</p>"
		    + "<p>Melalui E-Library, pemustaka dapat dengan mudah menelusuri ketersediaan fisik buku, melihat abstrak dari karya ilmiah (skripsi, tesis, dan disertasi) sivitas akademika terdahulu, serta mengunduh berkas digital artikel jurnal internal (<em>Open Access</em>) yang dipublikasikan oleh berbagai program studi di lingkungan kampus. Sistem katalog bersama ini dikembangkan dengan antarmuka yang responsif, memastikan kemudahan pencarian menggunakan variasi kata kunci seperti Judul, Pengarang, Penerbit, maupun Subjek spesifik keilmuan.</p>";
		rows.appendChild(createRowEditor("Katalog Bersama (E-Library)", "katalog_bersama_pustaka_default", defaultValueKatalogBersama, 15, null));


		// B. Katalog Nasional (OPAC)
		String defaultValueKatalogNasional = "<p>Guna memperluas cakrawala sumber referensi, institusi kami telah secara resmi terafiliasi dengan jaringan Perpustakaan Nasional Republik Indonesia (Perpusnas) melalui integrasi pangkalan data <em>Online Public Access Catalog</em> (OPAC) dan portal Indonesia OneSearch (IOS).</p>"
		    + "<p>Melalui layanan ini, katalog perpustakaan perguruan tinggi kita dapat diakses secara silang oleh pemustaka dari universitas lain di seluruh penjuru tanah air, begitu pula sebaliknya. Sivitas akademika institusi dapat mencari keberadaan buku atau manuskrip langka di perpustakaan lain, serta memanfaatkan fasilitas keanggotaan Perpusnas (K-Perpusnas) untuk mengakses ratusan juta referensi digital berstandar global secara gratis. Ini adalah bentuk manifestasi dari semangat kolaborasi literasi tingkat nasional guna mewujudkan masyarakat akademik yang cerdas dan unggul.</p>";
		rows.appendChild(createRowEditor("Katalog Nasional (OPAC)", "katalog_nasional_pustaka_default", defaultValueKatalogNasional, 15, null));


		// C. E-Journal
		String defaultValueEJournal = "<p>Layanan E-Journal merupakan fasilitas penelusuran literatur primer berbasis elektronik yang didedikasikan secara khusus untuk menopang kebutuhan riset tingkat lanjut, penyusunan tugas akhir, dan publikasi dosen. Institusi telah melanggan berbagai pangkalan data jurnal akademik bereputasi internasional (seperti ProQuest, IEEE Xplore, EBSCO, ScienceDirect, dan Scopus) dengan anggaran miliaran rupiah per tahun.</p>"
		    + "<p>Layanan E-Journal ini dilengkapi dengan mekanisme akses jarak jauh (<em>Remote Access</em> / VPN) yang memungkinkan seluruh sivitas akademika untuk membaca dan mengunduh <em>full-text</em> jurnal ilmiah berkualitas tinggi dari rumah masing-masing, tanpa harus terhubung secara fisik ke jaringan internet kampus. Pemustaka diwajibkan untuk menaati etika akademik dengan tidak mengunduh artikel secara masif menggunakan mesin (<em>systematic downloading</em>) dan dilarang keras memperjualbelikan dokumen artikel jurnal yang diunduh dari basis data langganan kampus untuk kepentingan komersial pribadi.</p>";
		rows.appendChild(createRowEditor("E-Journal", "e_journal_pustaka_default", defaultValueEJournal, 15, null));
		
		

		Hbox hbox = new Hbox();
		hbox.setParent(groupbox);
		// Sejarah, Visi Misi, Struktur, Tata Tertib
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "sejarah", Common.getBahasaConfig("Sejarah"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "visi_misi", Common.getBahasaConfig("Visi & Misi"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "struktur_organisasi", Common.getBahasaConfig("Struktur"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "tata_tertib", Common.getBahasaConfig("Tata Tertib"), false, null);


		// 2. KELOMPOK FASILITAS PERPUSTAKAAN
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("Fasilitas Perpustakaan")));

		hbox = new Hbox();
		hbox.setParent(groupbox);
		// Sarpras, Wifi, Ruang Baca
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "sarpras", Common.getBahasaConfig("Sarpras"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "wifi", Common.getBahasaConfig("Free Wifi"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "ruang_baca", Common.getBahasaConfig("Ruang Baca"), false, null);


		// 3. KELOMPOK LAYANAN (WAKTU & SIRKULASI)
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("Layanan & Sirkulasi")));

		hbox = new Hbox();
		hbox.setParent(groupbox);
		// Waktu, Peminjaman (Offline/Online), Kembali, Perpanjangan
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "waktu_layanan", Common.getBahasaConfig("Jam Layanan"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "peminjaman", Common.getBahasaConfig("Peminjaman"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "peminjaman_online", Common.getBahasaConfig("Pinjam Online"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "pengembalian", Common.getBahasaConfig("Pengembalian"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "perpanjangan", Common.getBahasaConfig("Perpanjangan"), false, null);


		// 4. KELOMPOK LAYANAN REFERENSI (DIGITAL)
		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption(Common.getBahasaConfig("Layanan Referensi & Katalog")));

		hbox = new Hbox();
		hbox.setParent(groupbox);
		// E-Library, OPAC Nasional, E-Journal
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "katalog_bersama", Common.getBahasaConfig("E-Library"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "katalog_nasional", Common.getBahasaConfig("OPAC Nasional"), false, null);
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.ID_PANDUAN_PUSTAKA, "e_journal", Common.getBahasaConfig("E-Journal"), false, null);

		rows.appendChild(createRowActiveDefault(
				"Apakah mahasiswa tidak bisa login sebelum mengembalikan buku perpustakaan jika terlambat sebanyak beberapa hari?",
				"apakah_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat_sebanyak_beberapa_hari",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Jumlah hari mahasiswa tidak bisa login sebelum mengembalikan buku perpustakaan jika terlambat ?",
				"jumlah_hari_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat",
				"100"));

		rows.appendChild(createRowActiveDefault(
				"Apakah anggota perpustakaan tidak boleh meminjam lagi jika peminjaman sebelumnya belum dikembalikan ?",
				"anggota_tidak_boleh_meminjam_lagi_meskipun_peminjaman_sebelumnya_belum_dikembalikan",
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Apakah saat pendataan item perpustakaan, tampilkan pilihan fakultas dan program studi ?",
				"saat_pendataan_item_perpustakaan_tampilkan_pilihan_fakultas_dan_prodi", Konfigurasi.TIDAK_AKTIF));

		// terintegrasi_dengan_google_book
		rows.appendChild(createRowActiveDefault("Terintegrasi dengan google book",
				"terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Item yang disirkulasikan tidak boleh sama dalam satu kali peminjaman",
				"item_yg_disirkulasikan_tidak_boleh_sama", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Generate barcode saldo awal otomatis",
				"generate_barcode_saldo_awal_otomatis", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Mahasiswa dengan status tidak aktif tidak diizinkan meminjam buku perpustakaan",
				"mahasiswa_dengan_status_tidak_aktif_tidak_diizinkan_meminjam_buku_perpustakaan", Konfigurasi.AKTIF));

		final Combobox googleBookAktif = createComboActive();
		googleBookAktif.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.googleBookAktif = googleBookAktif.getSelectedItem().getValue().equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.googleBookAktif => " + ConstantValues.googleBookAktif);
			}
		});

		rows.appendChild(createRowActive("Apakah google book aktif ?", "google_book_aktif", googleBookAktif));

		final Combobox aktifkanHariLibur = createComboActive();
		aktifkanHariLibur.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanHariLibur = aktifkanHariLibur.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println("ConstantValues.aktifkanHariLibur => " + ConstantValues.aktifkanHariLibur);
			}
		});
		rows.appendChild(createRowActiveDefault(
				"Hari sabtu dan minggu (libur) dihitung dalam lama waktu peminjaman dan pengembalian",
				"aktifkan_tidak_dihitung_hari_libur", Konfigurasi.AKTIF, aktifkanHariLibur));

		final Combobox aktifkanHariLiburMingguSaja = createComboActive();
		aktifkanHariLiburMingguSaja.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.aktifkanHariLiburMingguSaja = aktifkanHariLiburMingguSaja.getSelectedItem().getValue()
						.equals(Konfigurasi.AKTIF);
				System.out.println(
						"ConstantValues.aktifkanHariLiburMingguSaja => " + ConstantValues.aktifkanHariLiburMingguSaja);
			}
		});
		rows.appendChild(
				createRowActiveDefault("Hari minggu (libur) dihitung dalam lama waktu peminjaman dan pengembalian",
						"aktifkan_tidak_dihitung_hari_minggu", Konfigurasi.AKTIF, aktifkanHariLiburMingguSaja));

		// final Combobox aktifkanHariLiburNasional = createComboActive();
		// aktifkanHariLiburNasional.addEventListener("onChange", new
		// EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// ConstantValues.aktifkanHariLiburNasional =
		// aktifkanHariLiburNasional.getSelectedItem().getValue()
		// .equals(Konfigurasi.AKTIF);
		// System.out.println(
		// "ConstantValues.aktifkanHariLiburNasional => " +
		// ConstantValues.aktifkanHariLiburNasional);
		// }
		// });
		// rows.appendChild(
		// createRowActiveDefault("Hari libur nasional dihitung dalam lama waktu
		// peminjaman dan pengembalian",
		// "aktifkan_tidak_dihitung_hari_libur_nasional", Konfigurasi.AKTIF,
		// aktifkanHariLiburNasional));

		rows.appendChild(createRowActiveDefault(
				"Jika tanggal kembali merupakan hari minggu, maka hari kembali diganti hari setelahnya yang bukan libur",
				"minggu_hari_libur_tanggal_kembali_mundur", Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Jika tanggal kembali merupakan hari sabtu dan minggu, maka hari kembali diganti hari setelahnya yang bukan libur",
				"sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault(
				"Jika tanggal kembali merupakan hari libur nasional, maka hari kembali diganti hari setelahnya yang bukan libur",
				"libur_nasional_hari_libur_tanggal_kembali_mundur", Konfigurasi.AKTIF));

		String defaultValue = "1. Kartu ini ditertibkan oleh Perpustakaan ....... Segala penggunaan kartu oleh Perpustakaan ....... sesuai ketentuan dan syarat yang berlaku.\n"
				+ "2. Kartu ini wajib dibawa setiap masuk ke perpustakaan.\n"
				+ "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
				+ "4. Setiap pengunjung harus mematuhi semua tata tertib perpustakaan .......\n"
				+ "5. Bila menemukan kartu ini mohon mengembalikan ke perpustakaan .......\n" + "\n\n\n"
				+ "Perpustakaan .......\n" + "website : " + Common.getRequestHostWithProtocol();

		rows.appendChild(
				createRowNilai("Tata tertib perpustakaan", "tata_tertib_perpustakaan", defaultValue, 15, null));

		rows.appendChild(createRowNilai("Class untuk generate barcode", "class_untuk_generate_barcode",
				"ais.action.master.library.barcode.DefaultBarcodeGenerator"));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Tanda Tangan Untuk Kartu Anggota Perpustakaan (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.TANDA_TANGAN_KARTU_ANGGOTA_PERPUSTAKAAN,
				LampiranLain.TTD_KARTU_ANGGOTA_PERPUSTAKAAN_STR, "Tanda Tangan", false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Stempel Untuk Kartu Anggota Perpustakaan (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN,
				LampiranLain.STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN_STR, "Stempel", false, null);
		hbox.setParent(groupbox);

		rows.appendChild(createRowNilai("Label ttd kartu anggota perpustakaan", "label_ttd_perpustakaan",
				"...................."));

		rows.appendChild(
				createRowNilai("NIP kartu anggota perpustakaan", "nip_ttd_perpustakaan", "...................."));

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Depan kartu Anggota"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN,
				LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR, LampiranLain.BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Background Belakang kartu Anggota"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN,
				LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN_STR, LampiranLain.BG_2_KARTU_ANGGOTA_PERPUSTAKAAN_STR,
				false, null);
		hbox.setParent(groupbox);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Check dan proses data pengembalian"));
		MyButtonConfig button;
		groupbox.appendChild(button = new MyButtonConfig("Check dan Proses Sekarang"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentNativeSession();
						List<Long> s = session.createCriteria(PeminjamanPengadaanItemDetail.class)
								.setProjection(Projections.property("id"))
								.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
								.addOrder(Order.desc("peminjamanPengadaanItem.tanggalPembuatan"))
								.add(Restrictions.isNull("kembaliPengadaanItemDetail")).list();
						System.out.println("Jumlah total -> " + s.size());
						for (Long id : s) {
							try {
								PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = (PeminjamanPengadaanItemDetail) session
										.createCriteria(PeminjamanPengadaanItemDetail.class).add(Restrictions.idEq(id))
										.uniqueResult();

								if (peminjamanPengadaanItemDetail != null
										&& peminjamanPengadaanItemDetail.getKembaliPengadaanItemDetail() == null) {
									KembaliPengadaanItemDetail kembaliPengadaanItemDetail = (KembaliPengadaanItemDetail) session
											.createCriteria(KembaliPengadaanItemDetail.class).add(Restrictions
													.eq("peminjamanPengadaanItemDetail", peminjamanPengadaanItemDetail))
											.setMaxResults(1).uniqueResult();
									System.out.println("Proses -> " + peminjamanPengadaanItemDetail
											+ ", kembaliPengadaanItemDetail = " + kembaliPengadaanItemDetail + ", tgl :"
											+ (peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem()
													.getTanggalPembuatan() == null
															? ""
															: Common.dateFormat6.get().format(peminjamanPengadaanItemDetail
																	.getPeminjamanPengadaanItem()
																	.getTanggalPembuatan())));
									peminjamanPengadaanItemDetail
											.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);
									session.getTransaction().begin();
									Common.refreshUpdate(session, peminjamanPengadaanItemDetail);
									session.getTransaction().commit();
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
						ais.common.KarirConfigUtil.closeNativeSession(session);
					}
				});
			}
		});
	}

	protected Rows createSpan(String title) {

		if (outerTabsDiv != null) {
			if (mbt == null) {
				mbt = ais.ui.util.MyButtonTabbox.buat(outerTabsDiv, "100%", new int[] { 0 });
				mbtNextIdx = 0;
			}
			final int myIdx = mbtNextIdx++;
			boolean aktif = Common
					.getKonfigurasi("aktifkan_konfigurasi_" + title.replaceAll(" ", "_").toLowerCase(), Konfigurasi.AKTIF)
					.getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);
			org.zkoss.zul.Div panel = mbt.tambahTab(myIdx, title, "/img/svg/gear.svg");
			mbt.setVisibleTombol(myIdx, aktif);
			if (myIdx == 0) {
				mbt.pilih(0);
			}
			org.zkoss.zul.Div konfigScrollWrap = new org.zkoss.zul.Div();
			konfigScrollWrap.setWidth("100%");
			konfigScrollWrap.setStyle("min-height:10000px; overflow:visible; box-sizing:border-box;");
			konfigScrollWrap.setParent(panel);
			Grid grid = new Grid();
			grid.setSclass("fgrid ais-konfig-grid");
			grid.setWidth("100%");
			grid.setParent(konfigScrollWrap);
			Rows rows = new Rows();
			rows.setParent(grid);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new MyLabelStyled(Common.getBahasaConfig(title)));
			return rows;
		}

		if (tabsKonfigurasi == null || tabpanelsKonfigurasi == null) {
			return new Rows();
		}

		/* Pasang kotak pencarian konfigurasi sekali per halaman — di sini agar
		 * SEMUA subclass yang membangun tab via createSpan otomatis kebagian. */
		jadwalkanPencarianKonfigurasi();

		/* Penanda styling rail tab + kontras label: css_utama.css blok
		 * "PENGATURAN KONFIGURASI". Dipasang di sini agar semua subclass
		 * konfigurasi otomatis kebagian tanpa mengubah zul-nya. */
		try {
			if (tabsKonfigurasi.getParent() instanceof org.zkoss.zul.Tabbox) {
				org.zkoss.zul.Tabbox tabboxKonfig = (org.zkoss.zul.Tabbox) tabsKonfigurasi.getParent();
				String sclassLama = tabboxKonfig.getSclass() == null ? "" : tabboxKonfig.getSclass();
				if (sclassLama.indexOf("ais-konfig-tabbox") < 0) {
					tabboxKonfig.setSclass((sclassLama.trim().isEmpty() ? "" : sclassLama + " ") + "ais-konfig-tabbox");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiNewAction.java:8720");
		}

		MyTabConfig tab = new MyTabConfig(title);
		tab.setParent(tabsKonfigurasi);

		if (Common.isMobile()) {
			tab.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		}

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanelsKonfigurasi);
		tabpanel.setStyle("overflow:auto; background:#f8fafc; padding:0; box-sizing:border-box;");

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				tabpanel.setHeight((desktopHeight * 0.96) + "px");
			}
		}

		boolean aktif = Common
				.getKonfigurasi("aktifkan_konfigurasi_" + title.replaceAll(" ", "_").toLowerCase(), Konfigurasi.AKTIF)
				.getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);

		tab.setVisible(aktif);
		tabpanel.setVisible(aktif);

		// Area konten konfigurasi dibuat TINGGI (min 10000px) di dalam tabpanel ber-overflow:auto,
		// menggantikan Borderlayout/Center yang membatasi tinggi konten = tinggi tabpanel sehingga
		// konfigurasi di bagian bawah ter-CLIP. Dengan wadah setinggi ini SELURUH konfigurasi
		// ter-render dan dapat digulir penuh dalam satu scroll.
		org.zkoss.zul.Div konfigScrollWrap = new org.zkoss.zul.Div();
		konfigScrollWrap.setWidth("100%");
		konfigScrollWrap.setStyle("min-height:10000px; overflow:visible; box-sizing:border-box;");
		konfigScrollWrap.setParent(tabpanel);

		Grid grid = new Grid();
		grid.setSclass("fgrid ais-konfig-grid");
		grid.setWidth("100%");
		grid.setParent(konfigScrollWrap);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelStyled(Common.getBahasaConfig(title)));

		if (tabsKonfigurasi.getChildren().size() == 1) {
			tab.setSelected(true);
		}

		return rows;
	}

	protected void createSpan(String title, Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelStyled(Common.getBahasaConfig(title)));
	}

	/**
	 * Tambahkan baris <b>CATATAN keterkaitan</b> (bukan konfigurasi, tidak menyimpan nilai apa pun)
	 * ke dalam sebuah span. Dipakai ketika sebuah konfigurasi <b>saling terkait</b> dengan konfigurasi
	 * lain, sehingga admin paham efek gabungannya SEBELUM mengubah nilai — misalnya tombol Upload data
	 * yang kemunculannya bergantung pada beberapa konfigurasi sekaligus. Ditampilkan sebagai kotak
	 * info kecil (aksen kiri biru) yang membungkus teks ke bawah.
	 *
	 * @param catatan teks penjelasan (di-<i>translate</i> lewat {@link Common#getBahasaConfig}).
	 * @param rows    kontainer baris span tujuan.
	 */
	protected void createCatatanTerkait(String catatan, Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		Label lbl = new Label(Common.getBahasaConfig(catatan));
		lbl.setMultiline(true);
		lbl.setStyle("display:block; font-size:11px; color:#475569; line-height:1.55; background:#eff6ff; "
				+ "border-left:3px solid #2563eb; border-radius:6px; padding:8px 11px; white-space:normal; "
				+ "box-sizing:border-box;");
		row.appendChild(lbl);
	}

	protected Row createRowActiveTahunAkademikSemester(final String label, final String key, boolean sp) {
		return createRowActiveTahunAkademikSemester(label, key, Konfigurasi.AKTIF, sp);
	}

	protected Row createRowActiveTahunAkademikSemester(final String label, final String key, final String defaultNilai,
			boolean sp) {

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);

		final Combobox tahunAjaranInputNilai = Common.generateTahunAjaran(null);
		final Combobox nilaiInputSemester = Common.initJenisSemester(null, sp);
		final Combobox nilaiInputNilai = createComboActive();
		Konfigurasi konfigurasi = Common.getKonfigurasi(key,
				(String) tahunAjaranInputNilai.getSelectedItem().getValue(),
				(String) nilaiInputSemester.getSelectedItem().getValue(), null, null, null, defaultNilai);

		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		EventListener nilaiInputEventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tahunAjaranInputNilai.getSelectedItem() == null) {
					return;
				}
				if (nilaiInputSemester.getSelectedItem() == null) {
					return;
				}
				Konfigurasi konfigurasi = Common.getKonfigurasi(key,
						(String) tahunAjaranInputNilai.getSelectedItem().getValue(),
						(String) nilaiInputSemester.getSelectedItem().getValue(), null, null, null, defaultNilai);

				Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};
		tahunAjaranInputNilai.addEventListener("onChange", nilaiInputEventListener);
		nilaiInputSemester.addEventListener("onChange", nilaiInputEventListener);

		nilaiInputNilai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tahunAjaranInputNilai.getSelectedItem() == null) {
					return;
				}
				if (nilaiInputSemester.getSelectedItem() == null) {
					return;
				}
				if (nilaiInputNilai.getSelectedItem() == null) {
					return;
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key,
						(String) tahunAjaranInputNilai.getSelectedItem().getValue(),
						(String) nilaiInputSemester.getSelectedItem().getValue(), null, null, null, defaultNilai);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);

				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		groupbox.appendChild(tahunAjaranInputNilai);
		groupbox.appendChild(nilaiInputSemester);
		groupbox.appendChild(nilaiInputNilai);

		try {
			nilaiInputEventListener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return row;
	}

	protected Row createRowActiveDefault(final String label, final String key, final String defaultValue) {
		return createRowActiveDefault(label, key, defaultValue, createComboActive());
	}

	protected Row createRowActiveDefault(final String label, final String key, final String defaultValue,
			final Combobox nilaiInputNilai) {
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
		MyFormRow row = new MyFormRow();
		row.setValign("top");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		nilaiInputNilai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		groupbox.appendChild(nilaiInputNilai);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		return row;
	}

	protected Row createRowActive(final String label, final String key) {
		return createRowActiveDefault(label, key, Konfigurasi.AKTIF);
	}

	protected Row createRowNotActive(final String label, final String key) {
		return createRowActiveDefault(label, key, Konfigurasi.TIDAK_AKTIF);
	}

	protected Row createRowActive(final String label, final String key, final Combobox nilaiInputNilai) {
		return createRowActiveDefault(label, key, Konfigurasi.AKTIF, nilaiInputNilai);
	}

	protected Row createRowNotActive(final String label, final String key, final Combobox nilaiInputNilai) {
		return createRowActiveDefault(label, key, Konfigurasi.TIDAK_AKTIF, nilaiInputNilai);
	}

	protected Row createRowActive(final String label, final String key, final StatusPertemuan statusPertemuan) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key,
				statusPertemuan.getAktif() ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox nilaiInputNilai = createComboActive();
		nilaiInputNilai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key,
						statusPertemuan.getAktif() ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());

				Session session = HibernateUtil.currentNativeSession();

				session.refresh(statusPertemuan);
				statusPertemuan.setAktif(konfigurasi.getNilai().equals(Konfigurasi.AKTIF));

				session.getTransaction().begin();
				session.update(statusPertemuan);
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				if (statusPertemuan.getId().equals(ConstantValues.ABSEN.getId())) {
					ConstantValues.ABSEN = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.FORM.getId())) {
					ConstantValues.FORM = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.UTS.getId())) {
					ConstantValues.UTS = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.UAS.getId())) {
					ConstantValues.UAS = statusPertemuan;
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		groupbox.appendChild(nilaiInputNilai);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		return row;
	}

	protected Row createRowActive(final String label, final String key, final String info1) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, Konfigurasi.AKTIF, info1, "", "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox nilaiInputNilai = createComboActive();
		final Textbox info1Textbox = new Textbox();

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, Konfigurasi.AKTIF);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				konfigurasi.setInfo1(info1Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		info1Textbox.setValue(konfigurasi.getInfo1());

		groupbox.appendChild(nilaiInputNilai);
		groupbox.appendChild(info1Textbox);

		return row;
	}

	protected Row createRowActiveWithDefault(final String label, final String key, final String info1,
			final String withDefault) {
		return createRowActiveWithDefault(label, key, info1, withDefault, createComboActive());
	}

	protected Row createRowActiveWithDefault(final String label, final String key, final String info1,
			final String withDefault, final Combobox nilaiInputNilai) {
		return createRowActiveWithDefault(label, key, info1, null, null, withDefault, nilaiInputNilai);
	}

	protected Row createRowActiveWithDefault(final String label, final String key, final String info1,
			final String info2, final String info3, final String withDefault, final Combobox nilaiInputNilai) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault, info1, info2, info3);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		final Textbox info2Textbox = new Textbox();
		final Textbox info3Textbox = new Textbox();

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				konfigurasi.setInfo1(info1Textbox.getValue().trim());
				konfigurasi.setInfo2(info2Textbox.getValue().trim());
				konfigurasi.setInfo3(info3Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);
		info3Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		info1Textbox.setValue(konfigurasi.getInfo1());
		info2Textbox.setValue(konfigurasi.getInfo2());
		info3Textbox.setValue(konfigurasi.getInfo3());

		groupbox.appendChild(nilaiInputNilai);
		if (info1 != null && !info1.trim().isEmpty()) {
			groupbox.appendChild(info1Textbox);
		}
		if (info2 != null && !info2.trim().isEmpty()) {
			groupbox.appendChild(info2Textbox);
		}
		if (info3 != null && !info3.trim().isEmpty()) {
			groupbox.appendChild(info2Textbox);
		}

		return row;
	}

	protected Row createRowTanggal(final String label, final String key, final String withDefault) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault, "", "", "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final MyDatebox nilaiInputNilai = new MyDatebox();
		nilaiInputNilai.setFormat(Common.dateFormat1.get().toPattern());
		if (!konfigurasi.getNilai().trim().isEmpty()) {
			try {
				nilaiInputNilai.setValue(Common.dateFormat1.get().parse(konfigurasi.getNilai().trim()));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault);
				konfigurasi.setNilai(nilaiInputNilai.getValue() == null ? ""
						: Common.dateFormat1.get().format(nilaiInputNilai.getValue()));
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		groupbox.appendChild(nilaiInputNilai);

		return row;
	}

	protected Row createRowActiveWithDefaultCombo(final String label, final String key, final String info1,
			final String withDefault, final Combobox nilaiInputNilai,
			final List<GeneralValueObject> generalValueObjects) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault, info1, "", "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox info1Textbox = new Combobox();
		info1Textbox.setReadonly(true);
		for (GeneralValueObject generalValueObject : generalValueObjects) {
			MyComboitemConfig comboitem = new MyComboitemConfig(generalValueObject.getNama());
			comboitem.setDescription(generalValueObject.getKeterangan());
			comboitem.setValue(generalValueObject.getId().toString());
			info1Textbox.appendChild(comboitem);
		}

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, withDefault);
				konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				konfigurasi.setInfo1(info1Textbox.getSelectedItem() == null ? ""
						: info1Textbox.getSelectedItem().getValue().toString());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
				try {
					info1Textbox
							.setDisabled(nilaiInputNilai.getSelectedItem().getValue().equals(Konfigurasi.TIDAK_AKTIF));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		Common.selectComboItem(info1Textbox, konfigurasi.getInfo1().trim());

		groupbox.appendChild(nilaiInputNilai);
		groupbox.appendChild(info1Textbox);

		try {
			info1Textbox.setDisabled(nilaiInputNilai.getSelectedItem().getValue().equals(Konfigurasi.TIDAK_AKTIF));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		groupbox.appendChild(createButtonLihat(key, true));
		return row;
	}

	protected Row createRowActiveWithTwoCombo(final String label, final String key, final String info1,
			final String withDefault, final Combobox info1Textbox, final Combobox nilaiInputNilai,
			final EventListener listener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key + info1, withDefault, info1, "", "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String info1 = info1Textbox.getSelectedItem() == null ? ""
						: info1Textbox.getSelectedItem().getValue().toString();
				Konfigurasi konfigurasi = Common.getKonfigurasi(key + info1, withDefault, info1, "", "");
				if (event.getTarget() == nilaiInputNilai) {
					konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				} else if (event.getTarget() == info1Textbox) {
					konfigurasi.setInfo1(info1Textbox.getSelectedItem() == null ? ""
							: info1Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				listener.onEvent(event);

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		Common.selectComboItem(info1Textbox, konfigurasi.getInfo1().trim());

		groupbox.appendChild(info1Textbox);
		groupbox.appendChild(nilaiInputNilai);
		groupbox.appendChild(createButtonLihat(key, true));

		return row;
	}

	protected Row createRowActiveWithTreeCombo(final String label, final String key, final String info1,
			final String info2, final String withDefault, final Combobox info1Textbox, final Combobox info2Textbox,
			final Combobox nilaiInputNilai, final EventListener listener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key + info1 + info2, withDefault, info1, info2, "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());
		c.setSclass("ais-caption-styled");

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String info1 = info1Textbox.getSelectedItem() == null
						|| info1Textbox.getSelectedItem().getValue() == null ? ""
								: info1Textbox.getSelectedItem().getValue().toString();
				String info2 = info2Textbox.getSelectedItem() == null
						|| info2Textbox.getSelectedItem().getValue() == null ? ""
								: info2Textbox.getSelectedItem().getValue().toString();
				Konfigurasi konfigurasi = Common.getKonfigurasi(key + info1 + info2, withDefault, info1, info2, "");
				if (event.getTarget() == nilaiInputNilai) {
					konfigurasi.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				} else if (event.getTarget() == info1Textbox) {
					konfigurasi.setInfo1(
							info1Textbox.getSelectedItem() == null || info1Textbox.getSelectedItem().getValue() == null
									? ""
									: info1Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
				} else if (event.getTarget() == info2Textbox) {
					konfigurasi.setInfo2(
							info2Textbox.getSelectedItem() == null || info2Textbox.getSelectedItem().getValue() == null
									? ""
									: info2Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				if (listener != null) {
					listener.onEvent(event);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, konfigurasi.getNilai());
		Common.selectComboItem(info1Textbox, konfigurasi.getInfo1().trim());
		Common.selectComboItem(info2Textbox, konfigurasi.getInfo2().trim());

		groupbox.appendChild(info1Textbox);
		groupbox.appendChild(info2Textbox);
		groupbox.appendChild(nilaiInputNilai);
		groupbox.appendChild(createButtonLihat(key, true));

		return row;
	}

	protected Row createRowNilai(final String label, final String key, final String nilai) {
		return createRowNilai(label, key, nilai, 1, null, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai,
			EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, 1, null, paramEventListener);
	}

	protected Row createRowNilaiPassword(final String label, final String key, final String nilai) {
		return createRowNilaiPassword(label, key, nilai, 1, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, null, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, pilihan1, pilihan1, null, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final Combobox pilihan2, final Combobox pilihan3,
			final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		String newKey = key;
		if (pilihan != null && pilihan.getSelectedItem() != null && pilihan.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan1 != null && pilihan1.getSelectedItem() != null && pilihan1.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan2 != null && pilihan2.getSelectedItem() != null && pilihan2.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan3 != null && pilihan3.getSelectedItem() != null && pilihan3.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);

		Caption c;
		groupbox.appendChild(c = new Caption());
		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String newKey = key;
				if (pilihan != null && pilihan.getSelectedItem() != null
						&& pilihan.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan1 != null && pilihan1.getSelectedItem() != null
						&& pilihan1.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan2 != null && pilihan2.getSelectedItem() != null
						&& pilihan2.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan3 != null && pilihan3.getSelectedItem() != null
						&& pilihan3.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai);

				if ((pilihan != null && event.getTarget() == pilihan)
						|| (pilihan1 != null && event.getTarget() == pilihan1)
						|| (pilihan2 != null && event.getTarget() == pilihan2)
						|| (pilihan3 != null && event.getTarget() == pilihan3)) {
					info1Textbox.setValue(konfigurasi.getNilai());
				} else {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.update(konfigurasi);
					session.getTransaction().commit();

					ais.common.KarirConfigUtil.closeNativeSession(session);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

					try {
						if (paramEventListener != null) {
							paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		if (pilihan == null && pilihan1 == null && pilihan2 == null && pilihan3 == null) {
			groupbox.appendChild(info1Textbox);
		} else {

			if (pilihan != null) {
				groupbox.appendChild(pilihan);
				pilihan.addEventListener("onChange", eventListener);
			}
			if (pilihan1 != null) {
				groupbox.appendChild(pilihan1);
				pilihan1.addEventListener("onChange", eventListener);
			}
			if (pilihan2 != null) {
				groupbox.appendChild(pilihan2);
				pilihan2.addEventListener("onChange", eventListener);
			}
			if (pilihan3 != null) {
				groupbox.appendChild(pilihan3);
				pilihan3.addEventListener("onChange", eventListener);
			}
			groupbox.appendChild(info1Textbox);

			groupbox.appendChild(createButtonLihat(key, true));
		}

		return row;
	}
	
	
	
	
	
	
	
	
	
	
	
	protected Row createRowEditor(final String label, final String key, final String nilai) {
		return createRowEditor(label, key, nilai, 1, null, null);
	}

	protected Row createRowEditor(final String label, final String key, final String nilai,
			EventListener paramEventListener) {
		return createRowEditor(label, key, nilai, 1, null, paramEventListener);
	}

	protected Row createRowEditor(final String label, final String key, final String nilai, int rowCount,
			final EventListener paramEventListener) {
		return createRowEditor(label, key, nilai, rowCount, null, paramEventListener);
	}

	protected Row createRowEditor(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final EventListener paramEventListener) {
		return createRowEditor(label, key, nilai, rowCount, null, null, paramEventListener);
	}

	protected Row createRowEditor(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final EventListener paramEventListener) {
		return createRowEditor(label, key, nilai, rowCount, pilihan1, pilihan1, null, null, paramEventListener);
	}

	protected Row createRowEditor(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final Combobox pilihan2, final Combobox pilihan3,
			final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		String newKey = key;
		if (pilihan != null && pilihan.getSelectedItem() != null && pilihan.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan1 != null && pilihan1.getSelectedItem() != null && pilihan1.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan2 != null && pilihan2.getSelectedItem() != null && pilihan2.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan3 != null && pilihan3.getSelectedItem() != null && pilihan3.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);

		Caption c;
		groupbox.appendChild(c = new Caption());
		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final MyCkEditor info1Textbox = new MyCkEditor();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String newKey = key;
				if (pilihan != null && pilihan.getSelectedItem() != null
						&& pilihan.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan1 != null && pilihan1.getSelectedItem() != null
						&& pilihan1.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan2 != null && pilihan2.getSelectedItem() != null
						&& pilihan2.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan3 != null && pilihan3.getSelectedItem() != null
						&& pilihan3.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai);

				if ((pilihan != null && event.getTarget() == pilihan)
						|| (pilihan1 != null && event.getTarget() == pilihan1)
						|| (pilihan2 != null && event.getTarget() == pilihan2)
						|| (pilihan3 != null && event.getTarget() == pilihan3)) {
					info1Textbox.setValue(konfigurasi.getNilai());
				} else {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.update(konfigurasi);
					session.getTransaction().commit();

					ais.common.KarirConfigUtil.closeNativeSession(session);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

					try {
						if (paramEventListener != null) {
							paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		if (pilihan == null && pilihan1 == null && pilihan2 == null && pilihan3 == null) {
			groupbox.appendChild(info1Textbox);
		} else {

			if (pilihan != null) {
				groupbox.appendChild(pilihan);
				pilihan.addEventListener("onChange", eventListener);
			}
			if (pilihan1 != null) {
				groupbox.appendChild(pilihan1);
				pilihan1.addEventListener("onChange", eventListener);
			}
			if (pilihan2 != null) {
				groupbox.appendChild(pilihan2);
				pilihan2.addEventListener("onChange", eventListener);
			}
			if (pilihan3 != null) {
				groupbox.appendChild(pilihan3);
				pilihan3.addEventListener("onChange", eventListener);
			}
			groupbox.appendChild(info1Textbox);

			groupbox.appendChild(createButtonLihat(key, true));
		}

		return row;
	}
	
	
	
	
	
	
	
	
	

	protected Row createRowNilaiSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			int rowCount, final EventListener paramEventListener) {

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Combobox program = Common.initPrograms(null);

		final Combobox statusAwal = new Combobox();

		Common.insertComboDanSemua(statusAwal, new String[] { "nama", }, "kode", StatusAwalMahasiswa.class,
				"Semua Status Awal", Restrictions.eq("aktif", true));

		Common.selectComboItem(statusAwal, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0"
						: "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0"
						: "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0"
						: "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();
				String pro = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue() == null ? "_pro:0"
								: "_pro:" + program.getSelectedItem().getValue();

				String stsAwl = statusAwal.getSelectedItem() == null || statusAwal.getSelectedItem().getValue() == null
						? ""
						: "_statusAwal:" + ((StatusAwalMahasiswa) statusAwal.getSelectedItem().getValue()).getId();

				String semua = smt + ang + jur + pro + stsAwl;

				if (semua.equals("_smt:0_ang:0_jur:0") || (semester.getSelectedItem() == null
						|| semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								|| program.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)
						&& (statusAwal.getSelectedItem() == null || statusAwal.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(konfigurasi.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		statusAwal.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		groupbox.appendChild(info1Textbox);

		groupbox.appendChild(semester);
		groupbox.appendChild(angkatan);
		groupbox.appendChild(jurusan);
		groupbox.appendChild(program);
		groupbox.appendChild(statusAwal);

		semester.setCols(8);
		angkatan.setCols(8);
		jurusan.setCols(8);
		program.setCols(8);
		statusAwal.setCols(8);

		groupbox.appendChild(createButtonLihat(key));

		return row;

	}

	protected Row createRowAktifSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			final EventListener paramEventListener) {

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Combobox program = Common.initPrograms(null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox info1Textbox = createComboActive();
		comboitem = new MyComboitemConfig("Tidak ada");
		comboitem.setValue("");
		info1Textbox.appendChild(comboitem);
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0"
						: "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0"
						: "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0"
						: "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();
				String pro = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue() == null ? "_pro:0"
								: "_pro:" + program.getSelectedItem().getValue();

				String semua = smt + ang + jur + pro;

				if (semua.equals("_smt:0_ang:0_jur:0") || (semester.getSelectedItem() == null
						|| semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								|| program.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
				} else {
					Common.selectComboItem(info1Textbox, konfigurasi.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);

		Common.selectComboItem(info1Textbox, konfigurasi.getNilai());
		info1Textbox.setReadonly(true);

		groupbox.appendChild(info1Textbox);

		groupbox.appendChild(semester);
		groupbox.appendChild(angkatan);
		groupbox.appendChild(jurusan);
		groupbox.appendChild(program);

		semester.setCols(8);
		angkatan.setCols(8);
		jurusan.setCols(8);
		program.setCols(8);

		groupbox.appendChild(createButtonLihat(key));

		return row;

	}

	public static Row createRowNilaiProgramDanJurusan(final String label, final String key, final String nilai,
			int rowCount, final EventListener paramEventListener, final Combobox customCombo) {

		final Combobox program = Common.initPrograms(null);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String prog = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue().equals("") ? "_prog:0"
								: "_prog:" + program.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0"
						: "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();

				String cust = customCombo == null || customCombo.getSelectedItem() == null
						|| customCombo.getSelectedItem().getValue() == null ? "_cust:0"
								: "_cust:" + customCombo.getSelectedItem().getValue();

				if (cust.trim().equals("_cust:")) {
					cust = "_cust:0";
				}

				String semua = prog + cust + jur;

				if (semua.equals("_prog:0_cust:0_jur:0")
						|| (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								|| program.getSelectedItem().getValue().equals(""))
								&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)
								&& (customCombo == null || customCombo.getSelectedItem() == null
										|| customCombo.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(konfigurasi.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		groupbox.appendChild(info1Textbox);

		groupbox.appendChild(program);
		groupbox.appendChild(jurusan);

		program.setCols(5);
		jurusan.setCols(8);

		if (customCombo != null) {
			groupbox.appendChild(customCombo);
			customCombo.setCols(5);
			customCombo.addEventListener("onChange", eventListener);
		}

		groupbox.appendChild(createButtonLihatProgramJurusan(key));

		return row;

	}

	public static MyButtonConfig createButtonLihat(final String key) {
		return createButtonLihat(key, false);
	}

	public static MyButtonConfig createButtonLihat(final String key, final Boolean hanyaTampilKeyDanNilai) {
		MyButtonConfig button = new MyButtonConfig("Lihat", "/img/print.png");
		button.setWidth("60px");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(8);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								print = new MyToolbarbuttonConfig("Upload Data", "/img/excel.png");
								print.setUpload(Common.ukuranFileUpload());
								print.addEventListener("onUpload", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										UploadEvent uploadEvent = (UploadEvent) event;
										final Media media = uploadEvent.getMedia();
										if (!ais.action.master.helper.generic.AmbilDataTugasFileContent
												.checkFile(media))
											return;
										if (media.getName().toLowerCase().endsWith("xlsx")) {
											InputStream inputStream = media.getStreamData();
											// System.out.println("media = " +
											// media);
											final File file = new File(Sessions.getCurrent().getWebApp()
													.getRealPath("/temp/" + media.getName()));
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

											File folder = CommonMedia.getMediaDirectory();

											File f = new File(
													folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(
																	ais.ui.util.WaktuUtil.getCalendar()
																			.getTimeInMillis() + "_" + media.getName(),
																	"UTF-8"));

											Path sourceFile = Paths.get(file.getAbsolutePath());
											Path targetFile = Paths.get(f.getAbsolutePath());

											try {
												Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
											} catch (IOException ex) {
												System.err.format("I/O Error when copying file");
											}

											final UploadLogInfo uploadLog = new UploadLogInfo();
											uploadLog.setNama(media.getName());
											uploadLog.setKeterangan(f.getAbsolutePath());
											uploadLog.setClassName(Konfigurasi.class.getName());
											uploadLog.setDiuploadOleh(Common.getCurrentUser());
											Common.refreshSaveOrUpdate(uploadLog);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
													XSSFSheet sheet = workbook.getSheetAt(0);
													int rowCount = (sheet.getLastRowNum() + 1);
													for (int i = 1; i < rowCount; i++) {
														try {
															String key;
															if (hanyaTampilKeyDanNilai) {
																key = Common.getSheetContentAsString(sheet, 1, i);
															} else {
																key = Common.getSheetContentAsString(sheet, 6, i);
															}
															Konfigurasi konfigurasi = (Konfigurasi) MemoryDbUtil
																	.getKonfigurasi().get(key);
															if (konfigurasi != null) {
																konfigurasi.setNilai(
																		Common.getSheetContentAsString(sheet, 0, i));
																Common.refreshUpdate(konfigurasi);
															}
															MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(),
																	konfigurasi);
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
													}

													window.detach();
												}
											});

										} else {
											MyMessageboxConfig.showFormat(
													"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) Buka kembali berkas Excel tersebut menggunakan aplikasi Microsoft Excel atau aplikasi sejenis; (2) Pilih menu Save As, kemudian pilih tipe berkas Excel Open XML Spreadsheet (xlsx); (3) Simpan berkas tersebut, lalu lakukan proses unggah kembali menggunakan berkas berformat xlsx.",
													"Kesalahan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
										}
									}
								});
								print.setParent(toolbar);

								print = new MyToolbarbuttonConfig("Reset", "/img/svg/trash.svg");
								print.addEventListener("onClick", new EventListener() {
									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {

										MyMessageboxConfig.show(
												"Apakah Bapak/Ibu yakin ingin menghapus atau mengatur ulang (reset) seluruh konfigurasi ini? Mohon diperhatikan bahwa tindakan ini akan menghapus data konfigurasi terkait secara permanen dan tidak dapat dibatalkan. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
												"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
												MyMessageboxConfig.QUESTION, new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														int i = Integer.parseInt(event.getData().toString());
														if (i == MyMessageboxConfig.OK) {
															Session session = HibernateUtil.currentNativeSession();
															try {

																List<Konfigurasi> konfigurasis = ConstantValues
																		.simpleList(session
																				.createCriteria(Konfigurasi.class)
																				.addOrder(Order.desc("id"))
																				.add(Restrictions.ilike("nama", key,
																						MatchMode.START))
																				.addOrder(Order.asc("nama")),
																				Konfigurasi.class);
																for (Konfigurasi konfigurasi : konfigurasis) {
																	try {
																		MemoryDbUtil.getKonfigurasi()
																				.put(konfigurasi.getNama(), null);
																		session.getTransaction().begin();
																		Common.refreshDelete(session, konfigurasi);
																		session.getTransaction().commit();
																	} catch (Exception e) {
																		ais.common.Common.tampilErrorJikaAdmin(e);
																	}
																}

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
																MyMessageboxConfig.showFormat(
																		"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) Pastikan seluruh data lain yang berkaitan dengan konfigurasi ini telah dihapus atau dilepas keterkaitannya terlebih dahulu; (2) Periksa kembali data yang masih menggunakan konfigurasi ini; (3) Ulangi proses penghapusan setelah keterkaitan data tersebut teratasi.",
																		"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, e.getMessage());
															}
															ais.common.KarirConfigUtil.closeNativeSession(session);
															window.detach();
														}

													}
												});

									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {
							Session session = HibernateUtil.currentNativeSession();
							try {

								@SuppressWarnings("unchecked")
								List<Konfigurasi> konfigurasis = ConstantValues
										.simpleList(session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
												.add(Restrictions.ilike("nama", key, MatchMode.START))
												.add(Restrictions.ne("nilai", "")).add(Restrictions.isNotNull("nilai"))
												.addOrder(Order.asc("nama")), Konfigurasi.class);
								intbox.setValue(konfigurasis.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("KONFIGURASI");

								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead;

								if (hanyaTampilKeyDanNilai) {
									rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Nilai");
									rowhead.createCell(1).setCellValue("Key");
								} else {
									rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Nilai");
									rowhead.createCell(1).setCellValue("Info1");
									rowhead.createCell(2).setCellValue("Semester");
									rowhead.createCell(3).setCellValue("Tahun Angkatan");
									rowhead.createCell(4).setCellValue("Jurusan");
									rowhead.createCell(5).setCellValue("Program");
									rowhead.createCell(6).setCellValue("Status Awal");
									rowhead.createCell(7).setCellValue("Key");
								}

								for (Konfigurasi konfigurasi : konfigurasis) {
									try {
										rowIndex++;
										if (konfigurasi == null) {
											continue;
										}
										label.setValue("Sedang memproses data " + konfigurasi.getNama() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / konfigurasis.size())
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);

										if (hanyaTampilKeyDanNilai) {
											row.createCell(0).setCellValue(konfigurasi.getNilai());
											row.createCell(1).setCellValue(konfigurasi.getNama());
										} else {

											String[] t = konfigurasi.getNama().split("smt:");
											String smt = t.length == 1 ? "" : t[1].split("_")[0];
											t = konfigurasi.getNama().split("ang:");
											String ang = t.length == 1 ? "" : t[1].split("_")[0];
											t = konfigurasi.getNama().split("jur:");
											String jur = t.length == 1 ? "" : t[1].split("_")[0];
											t = konfigurasi.getNama().split("pro:");
											String pro = t.length == 1 ? "" : t[1].split("_")[0];
											t = konfigurasi.getNama().split("statusAwal:");
											String statusAwal = t.length == 1 ? "" : t[1].split("_")[0];

											StatusAwalMahasiswa awalMahasiswa = (StatusAwalMahasiswa) (statusAwal
													.isEmpty() || !Common.isNumber(statusAwal) ? null
															: ConstantValues.ambil(StatusAwalMahasiswa.class.getName(),
																	Long.parseLong(statusAwal)));

											String namaJurusan = jur.isEmpty() || jur.equals("0") ? ""
													: (String) session.createCriteria(Jurusan.class)
															.add(Restrictions.idEq(Long.parseLong(jur)))
															.setProjection(Projections.property("nama")).uniqueResult();

											row.createCell(0).setCellValue(konfigurasi.getNilai());
											row.createCell(1).setCellValue(konfigurasi.getInfo1());
											row.createCell(2)
													.setCellValue(smt.isEmpty() || smt.equals("0") ? "Semua" : smt);
											row.createCell(3)
													.setCellValue(ang.isEmpty() || ang.equals("0") ? "Semua" : ang);
											row.createCell(4)
													.setCellValue(namaJurusan == null || namaJurusan.isEmpty() ? "Semua"
															: namaJurusan);
											row.createCell(5)
													.setCellValue(pro.isEmpty() || pro.equals("0") ? "Semua" : pro);
											row.createCell(6).setCellValue(
													awalMahasiswa == null ? "Semua" : awalMahasiswa.getNama());
											row.createCell(7).setCellValue(konfigurasi.getNama());

											MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");
								konfigurasis.clear();
								konfigurasis = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							} finally {
								ais.common.KarirConfigUtil.closeNativeSession(session); // KE: pastikan close di FINALLY (thread latar)
							}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		return button;
	}

	public static MyButtonConfig createButtonLihatProgramJurusan(final String key) {
		MyButtonConfig button = new MyButtonConfig("Lihat", "/img/print.png");
		button.setWidth("60px");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(6);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {
							Session session = HibernateUtil.currentNativeSession();
							try {

								@SuppressWarnings("unchecked")
								List<Konfigurasi> konfigurasis = ConstantValues
										.simpleList(session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
												.add(Restrictions.ilike("nama", key, MatchMode.START))
												.add(Restrictions.ne("nilai", "")).add(Restrictions.isNotNull("nilai"))
												.addOrder(Order.asc("nama")), Konfigurasi.class);
								intbox.setValue(konfigurasis.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("KONFIGURASI");

								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);
								rowhead.createCell(0).setCellValue("Nilai");
								rowhead.createCell(1).setCellValue("Info1");
								rowhead.createCell(2).setCellValue("Program");
								rowhead.createCell(3).setCellValue("Jurusan");
								rowhead.createCell(4).setCellValue("Custom");
								rowhead.createCell(5).setCellValue("Key");

								for (Konfigurasi konfigurasi : konfigurasis) {
									try {
										rowIndex++;
										if (konfigurasi == null) {
											continue;
										}
										label.setValue("Sedang memproses data " + konfigurasi.getNama() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / konfigurasis.size())
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);

										String[] t = konfigurasi.getNama().split("_prog:");
										String prog = t.length == 1 ? "" : t[1].split("_")[0];
										t = konfigurasi.getNama().split("jur:");
										String jur = t.length == 1 ? "" : t[1].split("_")[0];
										String namaJurusan = jur.isEmpty() || jur.equals("0") ? ""
												: (String) session.createCriteria(Jurusan.class)
														.add(Restrictions.idEq(Long.parseLong(jur)))
														.setProjection(Projections.property("nama")).uniqueResult();

										t = konfigurasi.getNama().split("_cust:");
										String cust = t.length == 1 ? "" : t[1].split("_")[0];

										row.createCell(0).setCellValue(konfigurasi.getNilai());
										row.createCell(1).setCellValue(konfigurasi.getInfo1());
										row.createCell(2)
												.setCellValue(prog.isEmpty() || prog.equals("0") ? "Semua" : prog);
										row.createCell(3).setCellValue(
												namaJurusan == null || namaJurusan.isEmpty() ? "Semua" : namaJurusan);
										row.createCell(4).setCellValue(
												cust == null || cust.isEmpty() || cust.equals("0") ? "Semua" : cust);
										row.createCell(5).setCellValue(konfigurasi.getNama());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");
								konfigurasis.clear();
								konfigurasis = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							} finally {
								ais.common.KarirConfigUtil.closeNativeSession(session); // KE: pastikan close di FINALLY (thread latar)
							}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		return button;
	}

	protected Row createRowNilaiPassword(final String label, final String key, final String nilai, int rowCount,
			final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		Konfigurasi konfigurasi = Common.getKonfigurasi(key, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setType("password");
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi(key, nilai);
				konfigurasi.setNilai(info1Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		groupbox.appendChild(info1Textbox);

		return row;
	}

	public Row createRowNilai(final String label, final String key, final String nilai, final String nilai2) {
		return createRowNilai(label, key, nilai, nilai2, 1, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			int rowCount, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, key, nilai, nilai2, rowCount, null, paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, mykey, nilai, nilai2, null, rowCount, pilihan, paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			final String nilai3, int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, mykey, nilai, nilai2, nilai3, rowCount, pilihan, null, null, null,
				paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			final String nilai3, int rowCount, final Combobox pilihan, final Combobox pilihan1, final Combobox pilihan2,
			final Combobox pilihan3, final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		String newKey = mykey;
		if (pilihan != null && pilihan.getSelectedItem() != null && pilihan.getSelectedItem().getValue() != null) {
			if (pilihan.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan.getSelectedItem().getValue();
			}
		}
		if (pilihan1 != null && pilihan1.getSelectedItem() != null && pilihan1.getSelectedItem().getValue() != null) {
			if (pilihan1.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan1.getSelectedItem().getValue();
			}
		}
		if (pilihan2 != null && pilihan2.getSelectedItem() != null && pilihan2.getSelectedItem().getValue() != null) {
			if (pilihan2.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan2.getSelectedItem().getValue();
			}
		}
		if (pilihan3 != null && pilihan3.getSelectedItem() != null && pilihan3.getSelectedItem().getValue() != null) {
			if (pilihan3.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan3.getSelectedItem().getValue();
			}
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai, nilai2, nilai3 == null ? "" : nilai3, "");

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption(Common.getBahasaConfig(label) + "\n(" + mykey + ")"));

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final Textbox info3Textbox = new Textbox();
		info3Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String newKey = mykey;
				if (pilihan != null && pilihan.getSelectedItem() != null
						&& pilihan.getSelectedItem().getValue() != null) {
					if (pilihan.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan.getSelectedItem().getValue();
					}
				}
				if (pilihan1 != null && pilihan1.getSelectedItem() != null
						&& pilihan1.getSelectedItem().getValue() != null) {
					if (pilihan1.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan1.getSelectedItem().getValue();
					}
				}
				if (pilihan2 != null && pilihan2.getSelectedItem() != null
						&& pilihan2.getSelectedItem().getValue() != null) {
					if (pilihan2.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan2.getSelectedItem().getValue();
					}
				}
				if (pilihan3 != null && pilihan3.getSelectedItem() != null
						&& pilihan3.getSelectedItem().getValue() != null) {
					if (pilihan3.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan3.getSelectedItem().getValue();
					}
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(newKey, nilai, info2Textbox.getValue().trim(),
						info3Textbox.getValue().trim(), "");

				System.out.println("newKey = " + newKey + ", konfigurasi " + konfigurasi);

				if ((pilihan != null && event.getTarget() == pilihan)
						|| (pilihan1 != null && event.getTarget() == pilihan1)
						|| (pilihan2 != null && event.getTarget() == pilihan2)
						|| (pilihan3 != null && event.getTarget() == pilihan3)) {
					info1Textbox.setValue(konfigurasi.getNilai());
					info2Textbox.setValue(konfigurasi.getInfo1());
					info3Textbox.setValue(konfigurasi.getInfo2());
				} else {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
					konfigurasi.setInfo1(info2Textbox.getValue().trim());
					konfigurasi.setInfo2(info3Textbox.getValue().trim());
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.update(konfigurasi);
					session.getTransaction().commit();

					ais.common.KarirConfigUtil.closeNativeSession(session);
					MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

					try {
						if (paramEventListener != null) {
							paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}
				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);
		info3Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(konfigurasi.getInfo1());
		info2Textbox.setRows(rowCount);

		info3Textbox.setValue(konfigurasi.getInfo2());
		info3Textbox.setRows(rowCount);

		if (pilihan != null) {
			groupbox.appendChild(pilihan);
			pilihan.addEventListener("onChange", eventListener);
		}
		if (pilihan1 != null) {
			groupbox.appendChild(pilihan1);
			pilihan1.addEventListener("onChange", eventListener);
		}
		if (pilihan2 != null) {
			groupbox.appendChild(pilihan2);
			pilihan2.addEventListener("onChange", eventListener);
		}
		if (pilihan3 != null) {
			groupbox.appendChild(pilihan3);
			pilihan3.addEventListener("onChange", eventListener);
		}

		groupbox.appendChild(info1Textbox);

		if (nilai2 != null) {
			groupbox.appendChild(info2Textbox);
		}

		if (nilai3 != null) {
			groupbox.appendChild(info3Textbox);
		}

		groupbox.appendChild(createButtonLihat(mykey, true));

		return row;
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			final Combobox pilihan) {
		return createRowNilai(label, key, nilai, nilai2, 1, pilihan, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");

		String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
		Konfigurasi konfigurasi = Common.getKonfigurasi(key + pil, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String pil = (String) (pilihan.getSelectedItem() == null || pilihan.getSelectedItem().getValue() == null
						? ""
						: pilihan.getSelectedItem().getValue());

				Konfigurasi konfigurasi = Common.getKonfigurasi(key + pil, nilai);
				konfigurasi.setNilai(info1Textbox.getValue().trim());
				konfigurasi.setInfo1(info2Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.clear(ubs);
				try {
					RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label))
							.setParent(ubs);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(konfigurasi.getInfo1());
		info2Textbox.setRows(rowCount);

		groupbox.appendChild(pilihan);
		pilihan.setReadonly(true);
		pilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
				Konfigurasi konfigurasi = Common.getKonfigurasi(key + pil, nilai);
				info1Textbox.setValue(konfigurasi.getNilai());
				info2Textbox.setValue(konfigurasi.getInfo1());
			}
		});

		groupbox.appendChild(info1Textbox);
		groupbox.appendChild(info2Textbox);

		return row;
	}

	protected Row createRowNilaiSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			final String nilai2, int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
		Konfigurasi konfigurasi = Common.getKonfigurasi(key + pil, nilai);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		Caption c;
		groupbox.appendChild(c = new Caption());

		final Hbox ubs = new Hbox();
		ubs.setParent(c);
		try {
			RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, Common.getBahasaConfig(label)).setParent(ubs);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0"
						: "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0"
						: "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0"
						: "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();

				String semua = smt + ang + jur;

				if (semua.equals("_smt:0_ang:0_jur:0") || (semester.getSelectedItem() == null
						|| semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				Konfigurasi konfigurasi = Common.getKonfigurasi(key + pil + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					konfigurasi.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(konfigurasi.getNilai());
				}

				if (event.getTarget() == info2Textbox) {
					konfigurasi.setInfo1(info2Textbox.getValue().trim());
				} else {
					info2Textbox.setValue(konfigurasi.getInfo1());
				}

				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.common.KarirConfigUtil.closeNativeSession(session);
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, konfigurasi));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);

		info1Textbox.setValue(konfigurasi.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(konfigurasi.getInfo1());
		info2Textbox.setRows(rowCount);

		groupbox.appendChild(pilihan);
		pilihan.setReadonly(true);
		pilihan.addEventListener("onChange", eventListener);

		groupbox.appendChild(info1Textbox);
		groupbox.appendChild(info2Textbox);

		groupbox.appendChild(semester);
		groupbox.appendChild(angkatan);
		groupbox.appendChild(jurusan);

		semester.setCols(8);
		angkatan.setCols(8);
		jurusan.setCols(8);

		groupbox.appendChild(createButtonLihat(key));

		return row;
	}

	// Dropdown pilihan bahasa default aplikasi mobile (mobile_default_language):
	// Inggris/Indonesia/Arab, dipakai bersama createRowActiveDefault(..).
	protected Combobox createComboBahasaDefaultMobile() {
		Combobox nilai = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("English");
		comboitem.setValue("en");
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Bahasa Indonesia");
		comboitem.setValue("id");
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Arabic (العربية)");
		comboitem.setValue("ar");
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Chinese (简体中文)");
		comboitem.setValue("zh");
		nilai.appendChild(comboitem);
		nilai.setReadonly(true);
		return nilai;
	}

	protected Combobox createComboActive() {
		return createComboActive(false);
	}

	protected Combobox createComboActive(boolean tambahkanTidakWajib) {
		Combobox nilai = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Aktif");
		comboitem.setValue(Konfigurasi.AKTIF);
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Tidak Aktif");
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		nilai.appendChild(comboitem);
		if (tambahkanTidakWajib) {
			comboitem = new MyComboitemConfig("Tidak Wajib");
			comboitem.setValue(Konfigurasi.AKTIF_TIDAK_WAJIB);
			nilai.appendChild(comboitem);
		}
		nilai.setReadonly(true);
		return nilai;
	}

	protected Combobox createComboActiveAndReadOnly(boolean tambahkanTidakWajib) {
		Combobox nilai = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Aktif");
		comboitem.setValue(Konfigurasi.AKTIF);
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Tidak Aktif");
		comboitem.setValue(Konfigurasi.TIDAK_AKTIF);
		nilai.appendChild(comboitem);
		if (tambahkanTidakWajib) {
			comboitem = new MyComboitemConfig("Tidak Wajib");
			comboitem.setValue(Konfigurasi.AKTIF_TIDAK_WAJIB);
			nilai.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig("Tidak Bisa Diubah (read only)");
		comboitem.setValue(Konfigurasi.READ_ONLY);
		nilai.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Tidak Bisa Diubah (read only) kecuali oleh admin");
		comboitem.setValue(Konfigurasi.READ_ONLY_KECUALI_ADMIN);
		nilai.appendChild(comboitem);

		nilai.setReadonly(true);
		return nilai;
	}

	public void displayBackup(Rows rows, final String keterangan, final String tableName, final String colFotoName,
			final Class<? extends FileFoto> fotosClass) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);

		groupbox.appendChild(new Caption(
				"Kirim " + keterangan + " ke Drive (file asli akan terhapus dan dipindahkan ke google drive)"));

		MyButtonConfig button;
		groupbox.appendChild(button = new MyButtonConfig("Proses pengiriman " + keterangan + " ke Drive sekarang",
				"/img/Google-Drive-icon.png"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null) {

					final MyWindow window = new MyWindow("Pilih Tanggal", "none", true);
					window.setParent(page.getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setSclass("dgrid");
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("20%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					final MyDatebox mulai = new MyDatebox(WaktuUtil.getDate());
					final MyDatebox sampai = new MyDatebox(WaktuUtil.getDate());
					mulai.setReadonly(true);
					sampai.setReadonly(true);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
					row.appendChild(mulai);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
					row.appendChild(sampai);

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
							"Proses pengiriman " + keterangan + " ke Drive sekarang", "/img/Google-Drive-icon.png");
					save.setTooltiptext("Download");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
							final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
							final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							});

							final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
							File file = new File("/opt/ecampus/test.txt");
							ais.common.BacaTulisUtil.tulis(file, "test send..");
							driveUtilPerPengguna.prosesBackup(file, "test_files",

									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
													.getData();

											if (fileUpload != null && fileUpload.getId() != null) {

												new Thread(new Runnable() {

													@SuppressWarnings("unchecked")
													@Override
													public void run() {
														Session session = StreamingHibernateUtil.getInstance()
																.currentSession();

														List<Object[]> inds = session.createSQLQuery("select id,"
																+ colFotoName + " from " + tableName + " where "
																+ colFotoName
																+ " is not null and date(tanggal_dirubah) between date('"
																+ Common.databaseDateFormat.get().format(mulai.getValue())
																+ "') and date('"
																+ Common.databaseDateFormat.get().format(sampai.getValue())
																+ "') order by id desc;").list();
														StreamingHibernateUtil.getInstance().closeSession();

														int size = inds.size();
														int index = 0;
														for (Object[] o : inds) {
															index++;

															try {
																Object id = o[0];
																final Object fotoId = o[1];

																session = StreamingHibernateUtil.getInstance()
																		.currentSession();
																final FileFoto fileFoto = (FileFoto) session
																		.createCriteria(fotosClass)
																		.add(Restrictions
																				.idEq(Long.parseLong(id.toString())))
																		.uniqueResult();
																StreamingHibernateUtil.getInstance().closeSession();
																if (fileFoto != null) {
																	File file = fileFoto.ambilFile();
																	if (file != null && file.exists()) {
																		String s = "Mengirim file " + file.getName()
																				+ " (" + Common.numberFormat.get().format(
																						(index * 100.0) / size)
																				+ "%)";
																		System.out.println(s);
																		label.setValue(s);

																		com.google.api.services.drive.model.File fileKirim = driveUtilPerPengguna
																				.kirimBackupLangsung(null, file,
																						perguruanTinggi,
																						fileFoto.getClass()
																								.getSimpleName(),
																						new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {
																								com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
																										.getData();

																								if (fileUpload != null
																										&& fileUpload
																												.getId() != null) {

																									Session session = StreamingHibernateUtil
																											.getInstance()
																											.currentSession();
																									try {

																										session.refresh(
																												fileFoto);

																										fileFoto.setFoto(
																												null);
																										fileFoto.setGdrive(
																												fileUpload
																														.getId());
																										fileFoto.setGdriveUsername(
																												tbmuser.getUserId());

																										session.getTransaction()
																												.begin();
																										session.update(
																												fileFoto);
																										session.getTransaction()
																												.commit();

																										FileFoto.hapusTotal(
																												fotoId.toString(),
																												session);

																									} catch (Exception e) {
																										StreamingHibernateUtil
																												.getInstance()
																												.rollbackTransaction();
																										ais.common.Common.tampilErrorJikaAdmin(e);
																									}

																									StreamingHibernateUtil
																											.getInstance()
																											.closeSession();
																								}

																							}
																						});

																		if (fileKirim == null) {
																			System.out.println("Gagal Terkirim "
																					+ file.getAbsolutePath());
																			break;
																		} else {
																			System.out.println("Terkirim "
																					+ fileKirim.toPrettyString());

																		}
																	}
																}
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
																break;
															}

														}
														label.setValue("Selesai");
													}
												}).start();
											}

										}
									});
						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			}
		});
	}



	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Koperasi dan Kantin" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoKoperasiDanKantin() {
		Rows rows = createSpan("Koperasi dan Kantin (Auto)");
		rows.appendChild(createRowActiveDefault("Aktifkan bayar via qr topup — dipakai di: _beranda_anggota", "aktifkan_bayar_via_qr_topup", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan topup di anggota — dipakai di: _beranda_anggota, beranda", "aktifkan_topup_di_anggota", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika pengguna login secara otomatis jadi anggota — dipakai di: beranda", "jika_pengguna_login_secara_otomatis_jadi_anggota", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Pedagang boleh mengubah harga barang — dipakai di: index", "pedagang_boleh_mengubah_harga_barang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan dosen dengan anggotaKoperasi koperasi — dipakai di: AnggotaKoperasiAction", "singkronkan_dosen_dengan_anggotaKoperasi_koperasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan dosen dengan anggota perpustakaan — dipakai di: AnggotaAction", "singkronkan_dosen_dengan_anggota_perpustakaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan mahasiswa dengan anggotaKoperasi koperasi — dipakai di: AnggotaKoperasiAction", "singkronkan_mahasiswa_dengan_anggotaKoperasi_koperasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan mahasiswa dengan anggota perpustakaan — dipakai di: AnggotaAction", "singkronkan_mahasiswa_dengan_anggota_perpustakaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan pegawai dengan anggotaKoperasi koperasi — dipakai di: AnggotaKoperasiAction", "singkronkan_pegawai_dengan_anggotaKoperasi_koperasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan pegawai dengan anggota perpustakaan — dipakai di: AnggotaAction", "singkronkan_pegawai_dengan_anggota_perpustakaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan siswa dengan anggotaKoperasi koperasi — dipakai di: AnggotaKoperasiAction", "singkronkan_siswa_dengan_anggotaKoperasi_koperasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan siswa dengan anggota perpustakaan — dipakai di: AnggotaAction", "singkronkan_siswa_dengan_anggota_perpustakaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ringkasan penjualan ekantin — dipakai di: _info_transaksi_header", "tampilkan_ringkasan_penjualan_ekantin", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tabungan anggotaKoperasi — dipakai di: PembayaranKoperasiOnline", "tampilkan_tabungan_anggotaKoperasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan topup di virtual account — dipakai di: VirtualAccountBankAction", "tampilkan_topup_di_virtual_account", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Perpustakaan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoPerpustakaan() {
		Rows rows = createSpan("Perpustakaan (Auto)");
		rows.appendChild(createRowActiveDefault("Default pustaka gunakan versi baru — dipakai di: Pustaka", "default_pustaka_gunakan_versi_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Input data skripsi otomatis masuk perpustakaan — dipakai di: LibraryUtil", "input_data_skripsi_otomatis_masuk_perpustakaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label pustaka kampus — dipakai di: HalamanUtamaAction, PustakaAction", "label_pustaka_kampus", "Sistem Informasi Pustaka"));
		rows.appendChild(createRowActiveDefault("PeminjamSurat tidak boleh meminjam lagi meskipun peminjaman sebelumnya belum dikembalikan — dipakai di: PeminjamanSuratItemAction", "peminjamSurat_tidak_boleh_meminjam_lagi_meskipun_peminjaman_sebelumnya_belum_dikembalikan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Prefix barcode perpustakaan — dipakai di: TahunDanNomorUrutGenerator", "prefix_barcode_perpustakaan", ""));
		rows.appendChild(createRowActiveDefault("Tampilkan daftar pustaka di pengajuan — dipakai di: MahasiswaRequestTugasAkhirAction, SkripsiAction", "tampilkan_daftar_pustaka_di_pengajuan", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "PMB dan Registrasi" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoPmbDanRegistrasi() {
		Rows rows = createSpan("PMB dan Registrasi (Auto)");
		rows.appendChild(createRowNilai("Body login pmb — dipakai di: TampilanPengumumanAkademisAction, _sebelum_login", "body_login_pmb", ""));
		rows.appendChild(createRowActiveDefault("Default pmb gunakan versi baru — dipakai di: Pmb", "default_pmb_gunakan_versi_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Nonaktifkan PMB versi baru (jika aktif, semua akses /pmb diarahkan ke pmb.zul) — dipakai di: Pmb, PMBAction", "pmb_versi_baru_dinonaktifkan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Default ta pmb adalah semua — dipakai di: PMBAction", "default_ta_pmb_adalah_semua", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Email pendaftaran vendor subject — dipakai di: _vendor_service", "email_pendaftaran_vendor_subject", "Informasi Akun Portal Vendor"));
		rows.appendChild(createRowActiveDefault("Gelombang pendaftaran tidak tampil di billing pembayaran biaya registrasi — dipakai di: NewDetailBiayaExcelAction", "gelombang_pendaftaran_tidak_tampil_di_billing_pembayaran_biaya_registrasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Header login pmb — dipakai di: TampilanPengumumanAkademisAction, _sebelum_login", "header_login_pmb", "Login"));
		rows.appendChild(createRowNilai("Info lulus ujian pmb — dipakai di: TampilanUjianCalonMahasiswa, _ikut_ujian_online_service", "info_lulus_ujian_pmb", "Selamat saudara dinyatakan telah selesai mengikuti proses seleksi tahap ujian online. Ikuti tahap selanjutnya."));
		rows.appendChild(createRowNilai("Info nilia lulus ujian pmb — dipakai di: TampilanUjianCalonMahasiswa, _ikut_ujian_online_service", "info_nilia_lulus_ujian_pmb", "Nilai yang Anda peroleh adalah : "));
		rows.appendChild(createRowNilai("Jumlah increments no registrasi pegawai — dipakai di: DefaultNoRegGeneratorPegawai", "jumlah_increments_no_registrasi_pegawai", "5"));
		rows.appendChild(createRowNilai("Jumlah increments no registrasi psb — dipakai di: DefaultNoRegGeneratorPsb", "jumlah_increments_no_registrasi_psb", "5"));
		rows.appendChild(createRowNilai("Label pmb kampus — dipakai di: PMBAction", "label_pmb_kampus", "Seleksi Penerimaan Mahasiswa Baru"));
		rows.appendChild(createRowActiveDefault("Nomor ujian calon mahasiswa sama dengan no reg — dipakai di: BiodataCalonMahasiswa", "nomor_ujian_calon_mahasiswa_sama_dengan_no_reg", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pilih salah satu info pmb dari mana — dipakai di: BiodataCalonMahasiswaAction, _pendaftaran_mahasiswa", "pilih_salah_satu_info_pmb_dari_mana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Posisi pengumuman pmb dibawah pilihan daftar — dipakai di: PMBAction", "posisi_pengumuman_pmb_dibawah_pilihan_daftar", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Prefix center pmb — dipakai di: YY_PREFIX_URUT_NimGenerator", "prefix_center_pmb", ""));
		rows.appendChild(createRowNilai("Prefix no registrasi — dipakai di: StikesSbyNoRegGenerator", "prefix_no_registrasi", "77883"));
		rows.appendChild(createRowNilai("Prefix no registrasi upload — dipakai di: UploadBiodataCalonMahasiswaSPANPTKINAction", "prefix_no_registrasi_upload", ""));
		rows.appendChild(createRowNilai("Prefix pmb — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator, PRODI_URUT_YYYY_NimGenerator, PRODI_YY_BARUPINDAHAN_URUT_NimGenerator, PRODI_YY_URUT_NimGenerator, dll", "prefix_pmb", ""));
		rows.appendChild(createRowNilai("Script spmb — dipakai di: PMBAction", "script_spmb", ""));
		rows.appendChild(createRowActiveDefault("Setelah daftar pmb langsung cetak kartu — dipakai di: BiodataCalonMahasiswaAction, _cetak_kartu_pendaftaran", "setelah_daftar_pmb_langsung_cetak_kartu", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tambah peserta pmb di menu pengguna — dipakai di: TbmuserAction", "tambah_peserta_pmb_di_menu_pengguna", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil gelombang sederhana — dipakai di: GelombangPendaftaranAction", "tampil_gelombang_sederhana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil konsentrasi calon mahasiswa — dipakai di: BiodataCalonMahasiswaAction", "tampil_konsentrasi_calon_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan info ukt ke di pmb — dipakai di: CariDataPesertaUjianAction", "tampilkan_info_ukt_ke_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasi bukti diterima di pmb — dipakai di: TampilanPengumumanAkademisAction, _sukses_login", "tampilkan_informasi_bukti_diterima_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasi kelulusan di pmb — dipakai di: TampilanPengumumanAkademisAction", "tampilkan_informasi_kelulusan_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasi pembyaran di pmb — dipakai di: TampilanPengumumanAkademisAction, _sukses_login", "tampilkan_informasi_pembyaran_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasi ujian di pmb — dipakai di: CetakRegistrasiAction, TampilanPengumumanAkademisAction, _sukses_login", "tampilkan_informasi_ujian_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan interview di pmb — dipakai di: TampilanPengumumanAkademisAction, _sukses_login", "tampilkan_interview_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan label bahasa form PMB — dipakai di: BiodataCalonMahasiswaAction", "tampilkan_label_bahasa_form_PMB", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan label besar pada form PMB — dipakai di: BiodataCalonMahasiswaAction, ParameterTambahanListener, ParameterTambahanPsbListener", "tampilkan_label_besar_pada_form_PMB", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data calon mahasiswa — dipakai di: BiodataCalonMahasiswaAction", "tampilkan_link_login_oleh_admin_di_data_calon_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan gelombang pmb — dipakai di: TampilanPengumumanAkademisAction", "tampilkan_pilihan_gelombang_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan jenis seleksi pmb — dipakai di: TampilanPengumumanAkademisAction", "tampilkan_pilihan_jenis_seleksi_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan tahun akademik pmb — dipakai di: PMBAction, TampilanPengumumanAkademisAction", "tampilkan_pilihan_tahun_akademik_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan tahun akademik semua pmb — dipakai di: TampilanPengumumanAkademisAction", "tampilkan_pilihan_tahun_akademik_semua_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ujian online di pmb — dipakai di: TampilanPengumumanAkademisAction, _sukses_login", "tampilkan_ujian_online_di_pmb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan username dan password form PMB — dipakai di: BiodataCalonMahasiswaAction, UjianOnlineCalonMahasiswaAction", "tampilkan_username_dan_password_form_PMB", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Tombol login pmb — dipakai di: TampilanPengumumanAkademisAction, _sebelum_login", "tombol_login_pmb", "Login Sekarang"));
		rows.appendChild(createRowActiveDefault("Username pmb harus menggunakan format email — dipakai di: BiodataCalonMahasiswaAction", "username_pmb_harus_menggunakan_format_email", Konfigurasi.TIDAK_AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "e-Learning Tambahan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoELearningTambahan() {
		Rows rows = createSpan("e-Learning Tambahan (Auto)");
		rows.appendChild(createRowActiveDefault("AdminBolehMerubahJawabanUjian — dipakai di: KoreksiHasilUjian", "adminBolehMerubahJawabanUjian", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan rekam video manual — dipakai di: LiveStreamingPlayerWindow", "aktifkan_rekam_video_manual", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Bisa upload video langsung di eleraning — dipakai di: AmbilDataVideoPertemuan", "bisa_upload_video_langsung_di_eleraning", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Boleh tambah status pertemuan sendiri — dipakai di: StatusPertemuanAction", "boleh_tambah_status_pertemuan_sendiri", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Default tugas dan penilaian — dipakai di: KurikulumPunyaMatakuliahDetail, Pertemuan", "default_tugas_dan_penilaian", "Ketepatan menjelaskan...., Ketepatan menyebutkan..., dan lain sebagainya"));
		rows.appendChild(createRowActiveDefault("Email diskusi aktif — dipakai di: CommonEmail", "email_diskusi_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Get default jumlah boleh ikut ujian — dipakai di: PertemuanPunyaUjian", "get_default_jumlah_boleh_ikut_ujian", ""));
		rows.appendChild(createRowActiveDefault("Jika tidak ketemu jadwal ujian maka tidak tampil — dipakai di: CommonReportHelper", "jika_tidak_ketemu_jadwal_ujian_maka_tidak_tampil", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jumlah digit no ujian calon mhs — dipakai di: PrefixNoUjianGenerator", "jumlah_digit_no_ujian_calon_mhs", "3"));
		rows.appendChild(createRowNilai("Jumlah increments no ujian pegawai — dipakai di: DefaultNoUjianGeneratorPegawai", "jumlah_increments_no_ujian_pegawai", "8"));
		rows.appendChild(createRowNilai("Jumlah increments no ujian psb — dipakai di: DefaultNoUjianGeneratorPsb", "jumlah_increments_no_ujian_psb", "8"));
		rows.appendChild(createRowNilai("Llama system buat soal — dipakai di: BankSoalAction", "llama_system_buat_soal", "Kamu adalah Pengajar atau Dosen atau Guru "));
		rows.appendChild(createRowNilai("Llama system buat tugas — dipakai di: TugasMandiriHelper", "llama_system_buat_tugas", "Kamu adalah Pengajar atau Dosen atau Guru "));
		rows.appendChild(createRowNilai("Llama system buat tugas kelompok — dipakai di: TugasKelompokHelper", "llama_system_buat_tugas_kelompok", "Kamu adalah Pengajar atau Dosen atau Guru "));
		rows.appendChild(createRowActiveDefault("Nilai ujian ditampilkan ke mahasiswa — dipakai di: PertemuanPunyaUjianHelper", "nilai_ujian_ditampilkan_ke_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Nilai ujian ditampilkan ke siswa — dipakai di: PertemuanPunyaUjianSiswaHelper", "nilai_ujian_ditampilkan_ke_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Prefix no ujian calon mhs — dipakai di: PrefixNoUjianGenerator", "prefix_no_ujian_calon_mhs", "EXAM."));
		rows.appendChild(createRowActiveDefault("Saat cetak absensi ujian check pembayaran mahasiswa — dipakai di: CommonReportHelper", "saat_cetak_absensi_ujian_check_pembayaran_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Setelah daftar psb langsung generate nomor ujian — dipakai di: CalonSiswaAction, ElearningApiUtil", "setelah_daftar_psb_langsung_generate_nomor_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Setelah klik selesai tidak boleh ikut ujian kembali — dipakai di: PertemuanPunyaUjianHelper", "setelah_klik_selesai_tidak_boleh_ikut_ujian_kembali", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil colPesertaUjian — dipakai di: RekapPendaftarSpmb, RekapPendaftarSpmbSemua", "tampil_colPesertaUjian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil menu soal di manajemen ujian — dipakai di: PertemuanPunyaUjianHelper, PertemuanPunyaUjianSiswaHelper", "tampil_menu_soal_di_manajemen_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil nilai tugas di mahasiswa — dipakai di: PenilaianMahasiswaHelper", "tampil_nilai_tugas_di_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil nilai ujian di mahasiswa — dipakai di: PenilaianMahasiswaHelper", "tampil_nilai_ujian_di_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan hasil ujian mahasiswa — dipakai di: BiodataMahasiswaAction", "tampilkan_hasil_ujian_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan honor pertemuan yang telah diverifikasi — dipakai di: LaporanPenilaianOlehDosenPerDosenWindow, LaporanPenilaianPerDosenWindow", "tampilkan_honor_pertemuan_yang_telah_diverifikasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan prodi dan maakuliah saat buat ujian — dipakai di: UjianAction", "tampilkan_pilihan_prodi_dan_maakuliah_saat_buat_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan rekap hasil ujian — dipakai di: HasilUjianMahasiswaHelper, HasilUjianSiswaHelper, PertemuanPunyaUjianHelper, PertemuanPunyaUjianSiswaHelper", "tampilkan_rekap_hasil_ujian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ujian dibatasi waktu — dipakai di: PertemuanPunyaUjianHelper, PertemuanPunyaUjianSiswaHelper", "tampilkan_ujian_dibatasi_waktu", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan urutkan manual di agenda pertemuan — dipakai di: PenjadwalanHelper", "tampilkan_urutkan_manual_di_agenda_pertemuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tanggal realisasi jadwalPelajaran harus diisi sesuai pertemuan jadwalPelajaran — dipakai di: AbsensiSiswaHelper", "tanggal_realisasi_jadwalPelajaran_harus_diisi_sesuai_pertemuan_jadwalPelajaran", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Kepegawaian dan Payroll" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoKepegawaianDanPayroll() {
		Rows rows = createSpan("Kepegawaian dan Payroll (Auto)");
		rows.appendChild(createRowNilai("Admin yg boleh lihat semua data cuti pegawai — dipakai di: CommonCurrentSessionHelper", "admin_yg_boleh_lihat_semua_data_cuti_pegawai", ""));
		rows.appendChild(createRowNilai("Admin yg boleh lihat semua data pegawai — dipakai di: CommonCurrentSessionHelper", "admin_yg_boleh_lihat_semua_data_pegawai", ""));
		rows.appendChild(createRowNilai("Beban lembur pegawai max — dipakai di: DashboardKehadiranExpert", "beban_lembur_pegawai_max", ""));
		rows.appendChild(createRowActiveDefault("Fingerprint hanya gunakan finger — dipakai di: InitDataHelper", "fingerprint_hanya_gunakan_finger", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Gaji masuk standing instruction baru — dipakai di: StandingInstruction", "gaji_masuk_standing_instruction_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Hak akses yg boleh akses sk sangsi — dipakai di: BiodataPegawaiAction", "hak_akses_yg_boleh_akses_sk_sangsi", ""));
		rows.appendChild(createRowActiveDefault("Hanya admin yg bisa ubah pilihan pegawai pada surat keluar — dipakai di: SuratKeluarAction", "hanya_admin_yg_bisa_ubah_pilihan_pegawai_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Integrasi kepegawaian — dipakai di: TbmuserAction", "integrasi_kepegawaian", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Kenaikan pangkat menggunakan langsung gaji pokok — dipakai di: KenaikanPangkatAction, KenaikanPangkatFungsionalAction", "kenaikan_pangkat_menggunakan_langsung_gaji_pokok", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Kode item penggajian tidak terpilih — dipakai di: LaporanRekapPembayaranGaji", "kode_item_penggajian_tidak_terpilih", ""));
		rows.appendChild(createRowActiveDefault("Pegawai non aktif otomatis tidak bisa login — dipakai di: InitDataHelper", "pegawai_non_aktif_otomatis_tidak_bisa_login", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penghitungan bkd pengajaran menggunakan per perkuliahan — dipakai di: BkdPengajaranHelper", "penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Plus minus penambahan bulan penggajian — dipakai di: BayarGajiPegawaiAction, PembayaranGajiPunyaPegawai", "plus_minus_penambahan_bulan_penggajian", "0"));
		rows.appendChild(createRowActiveDefault("Singkronkan pegawai dengan peminjam surat — dipakai di: PeminjamSuratAction", "singkronkan_pegawai_dengan_peminjam_surat", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tahun slip gaji — dipakai di: SlipGajiPegawaiBulananAction", "tahun_slip_gaji", "20"));
		rows.appendChild(createRowActiveDefault("TampilRincianDataPegawai — dipakai di: BiodataPegawaiAction", "tampilRincianDataPegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil pilihan dosen pada data pegawai — dipakai di: BiodataPegawaiAction", "tampil_pilihan_dosen_pada_data_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil pilihan guru pada data pegawai — dipakai di: BiodataPegawaiAction", "tampil_pilihan_guru_pada_data_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil pilihan user pada data pegawai — dipakai di: BiodataPegawaiAction", "tampil_pilihan_user_pada_data_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data pegawai — dipakai di: BiodataPegawaiAction", "tampilkan_link_login_oleh_admin_di_data_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile pegawai — dipakai di: BiodataPegawaiAction", "tampilkan_mobile_di_profile_pegawai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan slip gaji di menu — dipakai di: MainAction, MainAction2", "tampilkan_slip_gaji_di_menu", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Terdapat cuti sp — dipakai di: PendaftaranCutiMahasiswaAction", "terdapat_cuti_sp", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload SK oleh admin — dipakai di: BiodataDosenAction, GuruAction", "upload_SK_oleh_admin", Konfigurasi.TIDAK_AKTIF));

		// ── ATURAN CUTI PEGAWAI ────────────────────────────────────────────────────────────
		// Dipakai di: AturanCutiHelper (CutiDanIzinAction saat Ajukan & saat Setujui).
		// Semua GERBANG default TIDAK AKTIF (opt-in): aturan ini memblokir simpan/persetujuan dan
		// bergantung pada data historis (tanggal masuk pegawai, kalender libur nasional) yang
		// mungkin belum lengkap. Aktifkan setelah data siap. Angkanya boleh disetel lebih dulu.
		rows.appendChild(createRowNilai(
				"Cuti — jumlah bulan kerja berturut-turut agar berhak cuti tahunan — dipakai di: AturanCutiHelper",
				"cuti_tahunan_minimal_bulan_kerja", "12"));
		rows.appendChild(createRowNilai(
				"Cuti — jumlah hari kerja cuti tahunan — dipakai di: AturanCutiHelper",
				"cuti_tahunan_jumlah_hari", "12"));

		rows.appendChild(createRowActiveDefault(
				"Cuti — AKTIFKAN batas paling lambat pengajuan (tolak bila terlambat) — dipakai di: AturanCutiHelper",
				"cuti_gerbang_batas_pengajuan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Cuti — pengajuan paling lambat berapa hari sebelum tanggal mulai — dipakai di: AturanCutiHelper",
				"cuti_minimal_hari_sebelum_mulai", "2"));

		rows.appendChild(createRowActiveDefault(
				"Cuti — AKTIFKAN blokir persetujuan di sekitar libur panjang — dipakai di: AturanCutiHelper",
				"cuti_gerbang_blokir_libur_panjang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai(
				"Cuti — panjang rentang libur (hari) agar dianggap libur panjang — dipakai di: AturanCutiHelper",
				"cuti_libur_panjang_minimal_hari", "3"));
		rows.appendChild(createRowNilai(
				"Cuti — jumlah hari blokir SEBELUM libur panjang — dipakai di: AturanCutiHelper",
				"cuti_blokir_hari_sebelum_libur_panjang", "7"));
		rows.appendChild(createRowNilai(
				"Cuti — jumlah hari blokir SESUDAH libur panjang — dipakai di: AturanCutiHelper",
				"cuti_blokir_hari_sesudah_libur_panjang", "14"));

		rows.appendChild(createRowActiveDefault(
				"Cuti — AKTIFKAN batas durasi baku cuti khusus (menikah/melahirkan/dll) — dipakai di: AturanCutiHelper",
				"cuti_gerbang_durasi_baku", Konfigurasi.TIDAK_AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Surat dan SOP" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoSuratDanSop() {
		Rows rows = createSpan("Surat dan SOP (Auto)");
		rows.appendChild(createRowActiveDefault("Cetak ktm di surat keterangan lulus — dipakai di: CommonReportHelper, _cetak_bukti_diterima", "cetak_ktm_di_surat_keterangan_lulus", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Cetak ktm di surat keterangan lulus harus mendapatkan nim — dipakai di: CommonReportHelper, _cetak_bukti_diterima", "cetak_ktm_di_surat_keterangan_lulus_harus_mendapatkan_nim", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Daftar username yg bisa lihat semua surat — dipakai di: DasboardAlurSurat, DasboardSurat, SuratKeluarAction, SuratMasukAction", "daftar_username_yg_bisa_lihat_semua_surat", ""));
		rows.appendChild(createRowActiveDefault("Hanya admin yg bisa ubah pilihan dosen pada surat keluar — dipakai di: SuratKeluarAction", "hanya_admin_yg_bisa_ubah_pilihan_dosen_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya admin yg bisa ubah pilihan guru pada surat keluar — dipakai di: SuratKeluarAction", "hanya_admin_yg_bisa_ubah_pilihan_guru_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya admin yg bisa ubah pilihan mahasiswa pada surat keluar — dipakai di: SuratKeluarAction", "hanya_admin_yg_bisa_ubah_pilihan_mahasiswa_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya admin yg bisa ubah pilihan siswa pada surat keluar — dipakai di: SuratKeluarAction", "hanya_admin_yg_bisa_ubah_pilihan_siswa_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Konfigurasi urutan sop — dipakai di: DasboardSop, sop_monitor_data", "konfigurasi_urutan_sop", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Nomor surat boleh diubah manual — dipakai di: SuratKeluarAction", "nomor_surat_boleh_diubah_manual", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Opsi surat hanya satu — dipakai di: SuratKeluarAction, SuratMasukAction", "opsi_surat_hanya_satu", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan dosen dengan peminjam surat — dipakai di: PeminjamSuratAction", "singkronkan_dosen_dengan_peminjam_surat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan guru dengan peminjam surat — dipakai di: PeminjamSuratAction", "singkronkan_guru_dengan_peminjam_surat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan mahasiswa dengan peminjam surat — dipakai di: PeminjamSuratAction", "singkronkan_mahasiswa_dengan_peminjam_surat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan siswa dengan peminjam surat — dipakai di: PeminjamSuratAction", "singkronkan_siswa_dengan_peminjam_surat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Sop alur terakhir otomatis jadi persetujuan — dipakai di: InitDataHelper", "sop_alur_terakhir_otomatis_jadi_persetujuan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan cara pembayaran di surat tagihan — dipakai di: CommonReportHelper", "tampilkan_cara_pembayaran_di_surat_tagihan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan lampiran catatan disposisi — dipakai di: DisposisiAlurSopAction, DisposisiSopAction", "tampilkan_lampiran_catatan_disposisi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol surat tagihan — dipakai di: KegiatanAction", "tampilkan_tombol_surat_tagihan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tanggal diterima di surat masuk boleh diubah — dipakai di: SuratMasukAction", "tanggal_diterima_di_surat_masuk_boleh_diubah", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Keuangan dan Pembayaran" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoKeuanganDanPembayaran() {
		Rows rows = createSpan("Keuangan dan Pembayaran (Auto)");
		initTabAutoKeuanganDanPembayaranBagian2(rows);
		rows.appendChild(createRowNilai("BrivaPassApp — dipakai di: Inquiry, Payment", "BrivaPassApp", "1234567890"));
		rows.appendChild(createRowActiveDefault("Tampilkan Rekap Biaya Administrasi — dipakai di: LaporanRekapHostToHostWindow", "Tampilkan_Rekap_Biaya_Administrasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan Rekap Biaya Ecampus — dipakai di: LaporanRekapHostToHostWindow", "Tampilkan_Rekap_Biaya_Ecampus", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan Rekap payment Gateway — dipakai di: LaporanRekapHostToHostWindow", "Tampilkan_Rekap_payment_Gateway", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Admin lain yang tidak bisa membayar langsung — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, _lanjut_bayar", "admin_lain_yang_tidak_bisa_membayar_langsung", ""));
		rows.appendChild(createRowActiveDefault("Aktifkan admin boleh verifikasi bayar — dipakai di: _draft_pesanan_anggota", "aktifkan_admin_boleh_verifikasi_bayar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan check ulang otomatis pembayaran via bni — dipakai di: BniBackandProsess", "aktifkan_check_ulang_otomatis_pembayaran_via_bni", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan check ulang otomatis pembayaran via bsi — dipakai di: BsiBackandProsess", "aktifkan_check_ulang_otomatis_pembayaran_via_bsi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran manual — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, _lanjut_bayar", "aktifkan_pembayaran_manual", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank bankaltimtara — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_bankaltimtara", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank bjb — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_bjb", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank briva — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_briva", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank finpay — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_finpay", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank flip — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_flip", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank maja — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_maja", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank ntt — dipakai di: BiodataCalonMahasiswaAction, DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, PembayaranMahasiswaAction, dll", "aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank online — dipakai di: BiodataCalonMahasiswaAction, DaftarUlangMahasiswaBaruAction, KegiatanTemporaryAction, PembayaranAction, dll", "aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank online 2 — dipakai di: DaftarUlangMahasiswaBaruAction, PembayaranAction, PembayaranOnline, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank online smartlink — dipakai di: DaftarUlangMahasiswaBaruAction", "aktifkan_pembayaran_via_bank_online_smartlink", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via Online BMT — gerbang global; wajib dipadukan dengan sakelar tenant PT/sekolah/kanal", Konfigurasi.ONLINE_BMT_AKTIF, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Prefix invoice Online BMT (huruf/angka, maksimum 8 karakter)", Konfigurasi.ONLINE_BMT_PREFIX_INVOICE, "BMT"));
		rows.appendChild(createRowNilai("Biaya administrasi Online BMT", "online_bmt_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank online smartlink 2 — dipakai di: TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_online_smartlink_2", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank otto — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_otto", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via bank qris — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "aktifkan_pembayaran_via_bank_qris", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via faspay auto — dipakai di: FaspayBackandProsess", "aktifkan_pembayaran_via_faspay_auto", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pembayaran via maja — dipakai di: PembayaranOnline", "aktifkan_pembayaran_via_maja", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol check ulang bni — dipakai di: BniRequestAction", "aktifkan_tombol_check_ulang_bni", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol check ulang bsi — dipakai di: BsiRequestAction", "aktifkan_tombol_check_ulang_bsi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol singkronkan tagihan siswa — dipakai di: TagihanAction", "aktifkan_tombol_singkronkan_tagihan_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan va bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, VirtualAccountBankAction", "aktifkan_va_bankaltimtara_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan va bjb langsung — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline, dll", "aktifkan_va_bjb_langsung", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan va e smartlink — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, PembayaranKoperasiOnline, dll", "aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan va jaring — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "aktifkan_va_jaring", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan va maja — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "aktifkan_va_maja", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Bank bisa melakukan reversal data kegiatan pembayaran — dipakai di: PembayaranUtil", "bank_bisa_melakukan_reversal_data_kegiatan_pembayaran", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bankaltimtara biaya administrasi — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction, TampilanPaymentGateway, _lanjut_bayar_services", "bankaltimtara_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Bankaltimtara va sleep — dipakai di: Bankaltimtara", "bankaltimtara_va_sleep", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Biaya admin jangan dimasukkan saat simpan va — dipakai di: BSI", "biaya_admin_jangan_dimasukkan_saat_simpan_va", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Biaya transaksi ecampus — dipakai di: LaporanRekapBiayaEcampusWindow", "biaya_transaksi_ecampus", "0.0"));
		rows.appendChild(createRowNilai("Billing type bni — dipakai di: BniCommon", "billing_type_bni", "c"));
		rows.appendChild(createRowActiveDefault("Bms va sleep — dipakai di: BMS, Nagari", "bms_va_sleep", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bni inquiry gateway url — dipakai di: BniRequestAction", "bni_inquiry_gateway_url", "https://billing-bpi.maja.id/bni/inquiry/"));
		rows.appendChild(createRowActiveDefault("Bni va sleep — dipakai di: Bniresponse", "bni_va_sleep", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Briva bank host ip — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "briva_bank_host_ip", ""));
		rows.appendChild(createRowNilai("Briva biaya administrasi — dipakai di: DaftarUlangMahasiswaBaruAction, TampilanPaymentGateway", "briva_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Bsi inquiry gateway url — dipakai di: BsiRequestAction", "bsi_inquiry_gateway_url", "https://billing-bpi.maja.id/bsi/inquiry/"));
		rows.appendChild(createRowActiveDefault("Bsi paksa gagal — dipakai di: BSI", "bsi_paksa_gagal", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bsi paksa gagal msg — dipakai di: BSI", "bsi_paksa_gagal_msg", "Error saat Update Transaksi"));
		rows.appendChild(createRowNilai("Bsi paksa gagal rc — dipakai di: BSI", "bsi_paksa_gagal_rc", "ERR-DB"));
		rows.appendChild(createRowNilai("Btn biaya administrasi — dipakai di: PembayaranOnline, TagihanSiswa, TopupHelper", "btn_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Bukan prefix wajib diterima pembayaran mandiri — dipakai di: Mandiri", "bukan_prefix_wajib_diterima_pembayaran_mandiri", ""));
		rows.appendChild(createRowNilai("Bukan prefix wajib diterima pembayaran ocbc — dipakai di: OcbcNisp", "bukan_prefix_wajib_diterima_pembayaran_ocbc", ""));
		rows.appendChild(createRowNilai("Bulan mulai tagihan — dipakai di: DetailTagihanCalonSiswaHelper, PengaturanBiayaAction, TagihanUtil", "bulan_mulai_tagihan", "8"));
		rows.appendChild(createRowActiveDefault("Calon siswa harus melakukan pembayaran sebelum bisa login — dipakai di: CommonReportPsb", "calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Calon siswa harus melakukan pembayaran sebelum bisa login baru — dipakai di: LoginCalonSiswaAction", "calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Cannel va e smartlink — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "cannel_va_e_smartlink", "VA_CIMB,VA_BRI"));
		rows.appendChild(createRowActiveDefault("Check apakah melebihi tagihan — dipakai di: DaftarUlangMahasiswaBaruAction, DaftarUlangMahasiswaLamaAction", "check_apakah_melebihi_tagihan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Chek tunggakan sebelum bayar — dipakai di: DaftarUlangMahasiswaLamaAction", "chek_tunggakan_sebelum_bayar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Custom bayar formulir jenis seleksi — dipakai di: RekapPendaftarSpmbSemua", "custom_bayar_formulir_jenis_seleksi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Custom bayar formulir pembayaran tidak dihitung — dipakai di: RekapPendaftarSpmbSemua", "custom_bayar_formulir_pembayaran_tidak_dihitung", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Default validator bni — dipakai di: Bniresponse, VirtualAccountBank", "default_validator_bni", "BNI"));
		rows.appendChild(createRowNilai("Default validator bsi — dipakai di: Bsiresponse, VirtualAccountBank", "default_validator_bsi", "Bank Syariah Indonesia"));
		rows.appendChild(createRowActiveDefault("File bukti transaksi jurnal wajib diupload — dipakai di: TransaksiJurnalUmumHelper", "file_bukti_transaksi_jurnal_wajib_diupload", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Finpay biaya administrasi — dipakai di: TampilanPaymentGateway", "finpay_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Flip biaya administrasi — dipakai di: TampilanPaymentGateway", "flip_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Gateway url va e smartlink — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "gateway_url_va_e_smartlink", "https://payment-service-sbx.pakar-digital.com/api/payment/create-order"));
		rows.appendChild(createRowActiveDefault("Gen va menggunakan nis — dipakai di: DownloadTagihanSiswaBankOnline", "gen_va_menggunakan_nis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Generate nomor pembayaran bni saat formulir siswa baru — dipakai di: CalonSiswaAction, _cetak_kartu_pendaftaran", "generate_nomor_pembayaran_bni_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Generate va langsung saat daftar — dipakai di: BiodataCalonMahasiswaAction, _cetak_kartu_pendaftaran", "generate_va_langsung_saat_daftar", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("InfoBelumbayarSaatProsescalonMahasiswa — dipakai di: BukittinggiNoUjianGenerator, CommonPMB, CommonReportHelper, DefaultNoUjianGenerator, dll", "infoBelumbayarSaatProsescalonMahasiswa", "Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran."));
		rows.appendChild(createRowNilai("Ip bank pembayaran online default — dipakai di: VirtualAccountBankAction", "ip_bank_pembayaran_online_default", "0.0.0.0"));
		rows.appendChild(createRowActiveDefault("Ipaymu langsung menggunakan virtual account — dipakai di: IpaymuCommon", "ipaymu_langsung_menggunakan_virtual_account", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jenis pembayaran bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "jenis_pembayaran_bankaltimtara_baru", "Uang Kuliah Tunggal"));
		rows.appendChild(createRowNilai("Jenis tagihan bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "jenis_tagihan_bankaltimtara_baru", "01"));
		rows.appendChild(createRowActiveDefault("Jika tabungan minus tidak boleh membayar — dipakai di: PembayaranSiswaAction", "jika_tabungan_minus_tidak_boleh_membayar", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jml digit prefix va bank online — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "jml_digit_prefix_va_bank_online", ""));
		rows.appendChild(createRowNilai("Kode akun manual biaya payment gateway — dipakai di: PostingBiayaPaymentGatewayPembayaranMahasiswaAction", "kode_akun_manual_biaya_payment_gateway", ""));
		rows.appendChild(createRowActiveDefault("Looping menggunakan tahun pada jurnal umum — dipakai di: TransaksiJurnalUmumHelper", "looping_menggunakan_tahun_pada_jurnal_umum", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa dengan status kampus merdeka bisa melakukan pembayaran seperti status aktif — dipakai di: PembayaranUtilHelper", "mahasiswa_dengan_status_kampus_merdeka_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa dengan status non lulus bisa melakukan pembayaran seperti status aktif — dipakai di: PembayaranUtilHelper", "mahasiswa_dengan_status_non_lulus_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa harus bayar sebelum isi krs paket — dipakai di: StudiMahasiswaHelper", "mahasiswa_harus_bayar_sebelum_isi_krs_paket", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa s1 lambat bayar langsung tidak aktif — dipakai di: AutoNotActivatingMahasiswaS1Processor", "mahasiswa_s1_lambat_bayar_langsung_tidak_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa s2 lambat bayar langsung tidak aktif — dipakai di: AutoNotActivatingMahasiswaS2Processor", "mahasiswa_s2_lambat_bayar_langsung_tidak_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa s3 lambat bayar langsung tidak aktif — dipakai di: AutoNotActivatingMahasiswaS3Processor", "mahasiswa_s3_lambat_bayar_langsung_tidak_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Maja biaya administrasi — dipakai di: TampilanPaymentGateway", "maja_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Mandiri va sleep — dipakai di: Mandiri", "mandiri_va_sleep", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Masih hanya pakai bni — dipakai di: TagihanSiswa, TopupHelper", "masih_hanya_pakai_bni", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Menggunakan prefix bni — dipakai di: BniCommon", "menggunakan_prefix_bni", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Menggunakan prefix bsi — dipakai di: BsiCommon", "menggunakan_prefix_bsi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Mhs yg belum bayar belum bisa di ntry nilai — dipakai di: DetailperkuliahanForPenilaianHelper", "mhs_yg_belum_bayar_belum_bisa_di_ntry_nilai", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mhs yg belum bayar tidak bisa lihat nilai — dipakai di: NilaiValidator", "mhs_yg_belum_bayar_tidak_bisa_lihat_nilai", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("NgakUsahCheckCicilanLama — dipakai di: Va", "ngakUsahCheckCicilanLama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Nol masuk filter pembayaran — dipakai di: PembayaranUtil, PembayaranUtilHelper", "nol_masuk_filter_pembayaran", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Nomor jurnal tidak boleh diubah — dipakai di: TransaksiJurnalUmumHelper", "nomor_jurnal_tidak_boleh_diubah", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Online biaya administrasi — dipakai di: BiodataCalonMahasiswaAction, TagihanMahasiswa, TagihanSiswa, TampilanPaymentGateway, dll", "online_biaya_administrasi", "0.0"));
		rows.appendChild(createRowNilai("Online biaya administrasi 2 — dipakai di: TampilanPaymentGateway", "online_biaya_administrasi_2", "0.0"));
		rows.appendChild(createRowNilai("Online biaya maja — dipakai di: TagihanSiswa, TopupHelper", "online_biaya_maja", "0.0"));
		rows.appendChild(createRowNilai("Online smartlink biaya administrasi — dipakai di: TagihanMahasiswa, TampilanPaymentGateway", "online_smartlink_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Otomatis verifikasi bayar setelah jam 24 — dipakai di: _draft_pesanan_anggota", "otomatis_verifikasi_bayar_setelah_jam_24", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Otto biaya administrasi — dipakai di: TampilanPaymentGateway", "otto_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Otto va sleep — dipakai di: Briva, Otto", "otto_va_sleep", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilaiPassword("Password va e smartlink — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, VirtualAccountBankAction, dll", "password_va_e_smartlink", ""));
		rows.appendChild(createRowActiveDefault("Pembayaran siswa yang sudah dibayar tidak bisa dihapus — dipakai di: ItemBiayaSekolahAction, JenisBiayaSekolahAction, PengaturanBiayaAction", "pembayaran_siswa_yang_sudah_dibayar_tidak_bisa_dihapus", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Pembayaran via bank online harus via va — dipakai di: PembayaranAction", "pembayaran_via_bank_online_harus_via_va", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Pembayaran via va bisa berdasarkan nim — dipakai di: Va", "pembayaran_via_va_bisa_berdasarkan_nim", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Prefix va bank online — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "prefix_va_bank_online", ""));
		rows.appendChild(createRowNilai("Prefix wajib diterima pembayaran mandiri — dipakai di: Mandiri", "prefix_wajib_diterima_pembayaran_mandiri", ""));
		rows.appendChild(createRowNilai("Prefix wajib diterima pembayaran ocbc — dipakai di: OcbcNisp", "prefix_wajib_diterima_pembayaran_ocbc", ""));
		rows.appendChild(createRowActiveDefault("Proses tagihan otomatis — dipakai di: TagihanProcessor", "proses_tagihan_otomatis", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Qris biaya administrasi — dipakai di: TampilanPaymentGateway", "qris_biaya_administrasi", "0.0"));
		rows.appendChild(createRowActiveDefault("Saldo harus cukup sebelum mengajukan realisasi anggaran — dipakai di: PermintaanPengadaanMasterAssetAction, SaldoAwalMasterAssetDetailAction, SaldoAwalPunyaMasterAssetHelper, TransaksiJurnalUmumHelper, dll", "saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Subva paka nim — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "subva_paka_nim", Konfigurasi.TIDAK_AKTIF));
	}

	protected void initTabAutoKeuanganDanPembayaranBagian2(Rows rows) {
		rows.appendChild(createRowActiveDefault("Tagihan dibuat otomatis menghitung sisa — dipakai di: DetailTagihanCalonSiswaHelper, DetailTagihanSiswaHelper, NominalBiaya, TagihanUtil, dll", "tagihan_dibuat_otomatis_menghitung_sisa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tagihan expired akhir hari — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBjb, DownloadNoRegistrasiCalonMahasiswaBankBtn, DownloadNoRegistrasiCalonMahasiswaBankNtt, DownloadNoRegistrasiCalonMahasiswaBankOnline, dll", "tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Tagihan expired day — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoRegistrasiCalonMahasiswaBankBjb, DownloadNoRegistrasiCalonMahasiswaBankBtn, DownloadNoRegistrasiCalonMahasiswaBankNtt, dll", "tagihan_expired_day", "0"));
		rows.appendChild(createRowNilai("Tagihan expired jam — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoRegistrasiCalonMahasiswaBankBjb, DownloadNoRegistrasiCalonMahasiswaBankBtn, DownloadNoRegistrasiCalonMahasiswaBankNtt, dll", "tagihan_expired_jam", ""));
		rows.appendChild(createRowNilai("Tagihan ui builder max thread — dipakai di: TagihanUIBuilder", "tagihan_ui_builder_max_thread", "4"));
		rows.appendChild(createRowActiveDefault("Tambah tahun di penomoran jurnal umum — dipakai di: TransaksiJurnalUmumHelper", "tambah_tahun_di_penomoran_jurnal_umum", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tambahkan merchan id di bni — dipakai di: BniCommon, BniRequestAction", "tambahkan_merchan_id_di_bni", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tambahkan merchan id di bsi — dipakai di: BsiCommon, BsiRequestAction", "tambahkan_merchan_id_di_bsi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan bersihkan jurnal — dipakai di: GrupTransaksiAction", "tampilkan_bersihkan_jurnal", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan biaya admin di virtual account — dipakai di: BniRequestAction, VirtualAccountBankAction", "tampilkan_biaya_admin_di_virtual_account", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan cetak bayar — dipakai di: _tampil_va, no_va", "tampilkan_cetak_bayar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan cetak tagihan di pendatan mahasiswa — dipakai di: MahasiswaAction", "tampilkan_cetak_tagihan_di_pendatan_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan check ulang pembayaran via bni — dipakai di: BniRequestAction", "tampilkan_check_ulang_pembayaran_via_bni", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan check ulang pembayaran via bsi — dipakai di: BsiRequestAction", "tampilkan_check_ulang_pembayaran_via_bsi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasiPembayaran psb — dipakai di: PSBAction", "tampilkan_informasiPembayaran_psb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan qr code bayar — dipakai di: no_va", "tampilkan_qr_code_bayar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ringkasan keseluruhan informasi pembayaran mahasiswa — dipakai di: InformasiPembayaranMahasiswaAction", "tampilkan_ringkasan_keseluruhan_informasi_pembayaran_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tanggal kwitansi di pembayaran — dipakai di: CicilanPembayaranAction, DaftarUlangMahasiswaBaruAction", "tampilkan_tanggal_kwitansi_di_pembayaran", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tanpa anggaran — dipakai di: PembayaranGajiAction, PemesananPengadaanMasterAssetAction, PenerimaanPengadaanMasterAssetAction, PermintaanPengadaanMasterAssetAction, dll", "tampilkan_tanpa_anggaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol proses tagihan — dipakai di: InformasiPembayaranMahasiswaAction, KegiatanAction", "tampilkan_tombol_proses_tagihan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan calon dengan pembayaran — dipakai di: CalonSiswaAction, CetakRegistrasiAction", "tampilkan_tombol_singkronkan_calon_dengan_pembayaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan siswa dengan pembayaran di kelas leas — dipakai di: KelasLesSiswaAction", "tampilkan_tombol_singkronkan_siswa_dengan_pembayaran_di_kelas_leas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan va custom — dipakai di: LogHostToHostAction", "tampilkan_va_custom", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan va per bank — dipakai di: LogHostToHostAction", "tampilkan_va_per_bank", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Type inquirybilling bni — dipakai di: BniBackandProsess", "type_inquirybilling_bni", "inquirybilling"));
		rows.appendChild(createRowNilai("Type transaksi bni — dipakai di: BniCommon", "type_transaksi_bni", "createbilling"));
		rows.appendChild(createRowActiveDefault("Untuk login mahasiswa tidak ditampilkan pilihan pembayaran detail — dipakai di: DetailPembayaranMahasiswaRenderer", "untuk_login_mahasiswa_tidak_ditampilkan_pilihan_pembayaran_detail", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Url create va bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "url_create_va_bankaltimtara_baru", "http://36.66.232.249:8017/ubt/create_va"));
		rows.appendChild(createRowNilai("Url status va bankaltimtara — dipakai di: Bankaltimtara", "url_status_va_bankaltimtara", "https://api-dev.bankaltimtara.co.id:8081/api-service/api/va/paid/nova"));
		rows.appendChild(createRowNilai("Url status va bankaltimtara baru — dipakai di: VirtualAccountBankAction", "url_status_va_bankaltimtara_baru", "http://36.66.232.249:8017/ubt/status_va"));
		rows.appendChild(createRowNilai("Url status va smartlink — dipakai di: VirtualAccountBankAction, _check_ulang_pembayaran", "url_status_va_smartlink", "https://payment-service.pakar-digital.com/api/payment/inquiry-order/"));
		rows.appendChild(createRowNilai("Username va e smartlink — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, VirtualAccountBankAction, dll", "username_va_e_smartlink", ""));
		rows.appendChild(createRowNilai("Va jaring expire — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "va_jaring_expire", "1440"));
		rows.appendChild(createRowNilai("Va jaring gateway url — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "va_jaring_gateway_url", "http://sandbox.jaring.host/api/v3/billpay/inquiry"));
		rows.appendChild(createRowNilai("Va jaring payment type — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "va_jaring_payment_type", "04"));
		rows.appendChild(createRowNilai("Va jaring produk id — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "va_jaring_produk_id", "207"));
		rows.appendChild(createRowNilaiPassword("Va jaring screet key — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "va_jaring_screet_key", ""));
		rows.appendChild(createRowActiveDefault("Va tampil semua di pt — dipakai di: VirtualAccountBankAction", "va_tampil_semua_di_pt", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Virtual account angka digit bni — dipakai di: BniCommon, BniKeranjangPembayaran", "virtual_account_angka_digit_bni", "16"));
		rows.appendChild(createRowNilai("Virtual account angka digit bsi — dipakai di: BsiCommon, BsiKeranjangPembayaran", "virtual_account_angka_digit_bsi", "16"));
		rows.appendChild(createRowActiveDefault("Virtual account muncul di halaman mahasiswa — dipakai di: InformasiPembayaranMahasiswaAction, informasi_pembayaran_mahasiswa", "virtual_account_muncul_di_halaman_mahasiswa", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "RAB dan Anggaran" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoRabDanAnggaran() {
		Rows rows = createSpan("RAB dan Anggaran (Auto)");
		rows.appendChild(createRowActiveDefault("Integrasi rab — dipakai di: SopAction", "integrasi_rab", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Kode profesi program bahasa arab — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "kode_profesi_program_bahasa_arab", "1"));
		rows.appendChild(createRowNilai("Rab mulai bulan — dipakai di: RealisasiBulananAction, WorkspaceRevisiBulananAction", "rab_mulai_bulan", "1"));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Notifikasi, Email, dan WA" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoNotifikasiEmailDanWa() {
		Rows rows = createSpan("Notifikasi, Email, dan WA (Auto)");
		rows.appendChild(createRowNilai("Alamat email default — dipakai di: AnggotaKoperasi, CalonAnggotaKoperasi, CalonSiswa, Penduduk, dll", "alamat_email_default", "@eschool.id"));
		rows.appendChild(createRowActiveDefault("Apakah aktifkan modul pesantren — dipakai di: AmbilDataAlurPersetujuanSuratKeluarBanbox, Common, HukumanAction, PelanggaranAction, dll", "apakah_aktifkan_modul_pesantren", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Api data mahasiswa password — dipakai di: DataMahasiswa", "api_data_mahasiswa_password", ""));
		rows.appendChild(createRowActiveDefault("Broadcast email alumni mahasiswa — dipakai di: MahasiswaAction", "broadcast_email_alumni_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Default email sender — dipakai di: _vendor_service", "default_email_sender", "no-reply@ecampus.id"));
		rows.appendChild(createRowNilai("Email tidak boleh kirim dari — dipakai di: MailSender, _vendor_service", "email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id"));
		rows.appendChild(createRowActiveDefault("Foto mahasiswa syncrhonizer — dipakai di: FotoMahasiswaSyncrhonizerProcessor", "foto_mahasiswa_syncrhonizer", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Foto mahasiswa syncrhonizer clean — dipakai di: FotoMahasiswaSyncrhonizerProcessor", "foto_mahasiswa_syncrhonizer_clean", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Foto mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "foto_mahasiswa_wajib_diisi", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jawab whatsapp karir — dipakai di: KarirAction", "jawab_whatsapp_karir", "Saya ingin menanyakan tentang informasi seleksi karir, apakah Anda bisa membantu?"));
		rows.appendChild(createRowNilai("Jawab whatsapp psb — dipakai di: PSBAction", "jawab_whatsapp_psb", "Saya ingin menanyakan tentang informasi penerimaan siswa baru, apakah Anda bisa membantu?"));
		rows.appendChild(createRowNilai("Jawab whatsapp vendor — dipakai di: VendorAction", "jawab_whatsapp_vendor", "Saya ingin menanyakan tentang informasi seleksi vendor, apakah Anda bisa membantu?"));
		rows.appendChild(createRowActiveDefault("Jika sudah dapat kkn mahasiswa tidak boleh mengajukan kkn — dipakai di: KknUntukMahasiswaAction", "jika_sudah_dapat_kkn_mahasiswa_tidak_boleh_mengajukan_kkn", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika sudah dapat pkl mahasiswa tidak boleh mengajukan pkl — dipakai di: PklUntukMahasiswaAction", "jika_sudah_dapat_pkl_mahasiswa_tidak_boleh_mengajukan_pkl", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Ktp mahasiswa harus 16 karakter — dipakai di: BiodataMahasiswaAction", "ktp_mahasiswa_harus_16_karakter", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link push multiple devices notif — dipakai di: MailSender", "link_push_multiple_devices_notif", "http://dev.ecampus.id:3000/push_multiple_devices"));
		rows.appendChild(createRowActiveDefault("Aktifkan push multiple devices notif (push ke server mobile) — dipakai di: MailSender", "aktifkan_push_multiple_devices_notif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa boleh memilih sendiri dosen pembimbing skripsi — dipakai di: MahasiswaRequestTugasAkhirAction", "mahasiswa_boleh_memilih_sendiri_dosen_pembimbing_skripsi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa tidak boleh memilih dosen pembimbing sendiri — dipakai di: KelompokKknAction, KelompokPklAction", "mahasiswa_tidak_boleh_memilih_dosen_pembimbing_sendiri", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa upload lampiran pengajuan skripsi 1 — dipakai di: LampiranLainMahasiswa", "mahasiswa_upload_lampiran_pengajuan_skripsi_1", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa upload lampiran pengajuan skripsi 2 — dipakai di: LampiranLainMahasiswa", "mahasiswa_upload_lampiran_pengajuan_skripsi_2", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa upload lampiran pengajuan skripsi 3 — dipakai di: LampiranLainMahasiswa", "mahasiswa_upload_lampiran_pengajuan_skripsi_3", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa upload lampiran pengajuan skripsi 4 — dipakai di: LampiranLainMahasiswa", "mahasiswa_upload_lampiran_pengajuan_skripsi_4", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Mahasiswa upload lampiran pengajuan skripsi 5 — dipakai di: LampiranLainMahasiswa", "mahasiswa_upload_lampiran_pengajuan_skripsi_5", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Nilai mahasiswa otomatis terkoreksi — dipakai di: CommonAcademicKrsNilaiHelper", "nilai_mahasiswa_otomatis_terkoreksi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Nilai umur calon siswa dibatasi — dipakai di: GelombangPendaftaranPsbAction, PenjurusanSekolahAction", "nilai_umur_calon_siswa_dibatasi", "27"));
		rows.appendChild(createRowActiveDefault("Nisn mahasiswa harus 10 karakter — dipakai di: BiodataMahasiswaAction", "nisn_mahasiswa_harus_10_karakter", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("No whatsapp karir — dipakai di: KarirAction", "no_whatsapp_karir", "0811111111111111"));
		rows.appendChild(createRowNilai("No whatsapp operator — dipakai di: MainAction, MainAction2, PSBAction, footer", "no_whatsapp_operator", ""));
		rows.appendChild(createRowNilai("No whatsapp vendor — dipakai di: VendorAction", "no_whatsapp_vendor", "0811111111111111"));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen per matakuliah — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_per_matakuliah", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen per perbandingan — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_per_perbandingan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen per perkuliahan — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_per_perkuliahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen per prodi — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_per_prodi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen per prodi untuk dosen — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_per_prodi_untuk_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen rangking angket dosen — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_rangking_angket_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen rangking angket jurusan — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_rangking_angket_jurusan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen rekap angket per dosen dan matakuliah — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_rekap_angket_per_dosen_dan_matakuliah", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Penilaian mahasiswa thd dosen tampil mahasiswa — dipakai di: LaporanAngketDosenPerDosenWindow", "penilaian_mahasiswa_thd_dosen_tampil_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Pesan tambahan notif awal — dipakai di: BroadcastHelper, CommonEmail, CommonReportHelper, KembaliPengadaanItemAction, dll", "pesan_tambahan_notif_awal", "*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n"));
		rows.appendChild(createRowNilai("Pesan tambahan wa awal — dipakai di: Wa", "pesan_tambahan_wa_awal", ""));
		rows.appendChild(createRowNilai("Pesan tambahan wa awal baru — dipakai di: Wa", "pesan_tambahan_wa_awal_baru", "*Pesan Ini Dibuat Otomatis Sebagai Helpdesk eCampus dan eSchool*\n\n"));
		rows.appendChild(createRowActiveDefault("Pesan tambahan wa dibuat statis — dipakai di: Wa", "pesan_tambahan_wa_dibuat_statis", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Saat daftar sidang mahasiswa bisa menentukan pembimbing — dipakai di: PendaftaranWisudaMahasiswaAction, SkripsiAction", "saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Siswa tidak boleh absen online — dipakai di: AbsensiApiAction, ScanBerhasilAction", "siswa_tidak_boleh_absen_online", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Status awal mahasiswa hanya boleh diubah oleh — dipakai di: MahasiswaAction, TampilStudiMahasiswaHelper", "status_awal_mahasiswa_hanya_boleh_diubah_oleh", ""));
		rows.appendChild(createRowActiveDefault("Status mahasiswa harus aktif sebelum isi krs paket — dipakai di: StudiMahasiswaHelper", "status_mahasiswa_harus_aktif_sebelum_isi_krs_paket", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil alamat sekolah calon siswa pindah dari — dipakai di: CalonSiswaAction", "tampil_alamat_sekolah_calon_siswa_pindah_dari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil kelas sebelum pindah calon siswa pindah dari — dipakai di: CalonSiswaAction", "tampil_kelas_sebelum_pindah_calon_siswa_pindah_dari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil keterangan calon siswa pindah dari — dipakai di: CalonSiswaAction", "tampil_keterangan_calon_siswa_pindah_dari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil nama sekolah calon siswa pindah dari — dipakai di: CalonSiswaAction", "tampil_nama_sekolah_calon_siswa_pindah_dari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil tanggal pindah calon siswa pindah dari — dipakai di: CalonSiswaAction", "tampil_tanggal_pindah_calon_siswa_pindah_dari", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan notif di dafboard — dipakai di: MainAction, MainAction2", "tampilkan_notif_di_dafboard", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pengecualian kkn mahasiswa di seleksi — dipakai di: PendaftarKknHelper", "tampilkan_pengecualian_kkn_mahasiswa_di_seleksi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pengecualian pkl mahasiswa di seleksi — dipakai di: PendaftarPklHelper", "tampilkan_pengecualian_pkl_mahasiswa_di_seleksi", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tanya whatsapp karir — dipakai di: KarirAction", "tanya_whatsapp_karir", "Salamat Datang, apa yang bisa kami bantu?"));
		rows.appendChild(createRowNilai("Tanya whatsapp psb — dipakai di: PSBAction", "tanya_whatsapp_psb", "Salamat Datang, apa yang bisa kami bantu?"));
		rows.appendChild(createRowNilai("Tanya whatsapp vendor — dipakai di: VendorAction", "tanya_whatsapp_vendor", "Salamat Datang, apa yang bisa kami bantu?"));
		rows.appendChild(createRowActiveDefault("Terdapat pengajuan mahasiswa sp — dipakai di: PengajuanMahasiswaAction", "terdapat_pengajuan_mahasiswa_sp", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Terdapat pengajuan siswa sp — dipakai di: PengajuanSiswaAction", "terdapat_pengajuan_siswa_sp", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload akte mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_akte_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload download email mahasiswa — dipakai di: MahasiswaAction", "upload_download_email_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload ijazah mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_ijazah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload kk mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_kk_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload ktp ayah mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_ktp_ayah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload ktp ibu mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_ktp_ibu_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload ktp mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_ktp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload ktp wali mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_ktp_wali_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload npwp mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_npwp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload transkrip nilai mahasiswa wajib diisi — dipakai di: BiodataMahasiswaAction", "upload_transkrip_nilai_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Integrasi Eksternal" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoIntegrasiEksternal() {
		Rows rows = createSpan("Integrasi Eksternal (Auto)");
		rows.appendChild(createRowNilai("Admin yg boleh kirim ke feeder (SUDAH TIDAK DIPAKAI -- digantikan flag per-Role \"Akses Feeder\" di Kelola Role/Grup Pengguna)", "admin_yg_boleh_kirim_ke_feeder", ""));
		rows.appendChild(createRowNilai("Ai chatbot api key gemini — dipakai di: AIGenerator, Wa (kunci lama yang bocor sudah dihapus 2026-09-01, admin WAJIB isi ulang)", "ai_chatbot_api_key_gemini", ""));
		rows.appendChild(createRowActiveDefault("Aktifkan ambil buku dari google book — dipakai di: FilePerkuliahanHelper", "aktifkan_ambil_buku_dari_google_book", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan https ke feeder — dipakai di: FeederConnector", "aktifkan_https_ke_feeder", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan terhubung langsung ke feeder — dipakai di: AktifitasPerkuliahanHelper, CabangPrestasiMahasiswaAction, DetailSemesterKurikulumHelper, DetailperkuliahanAction, dll", "aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload ajar dosen di feeder integrator — dipakai di: UploadAjarDosen", "aktifkan_upload_ajar_dosen_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload ajar dosen pada menu feeder integrator — dipakai di: AjarDosenIntegrator", "aktifkan_upload_ajar_dosen_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload kelas di feeder integrator — dipakai di: UploadKelas", "aktifkan_upload_kelas_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload kelas pada menu feeder integrator — dipakai di: KelasIntegrator", "aktifkan_upload_kelas_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload kelulusan di feeder integrator — dipakai di: UploadKelulusan", "aktifkan_upload_kelulusan_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload kelulusan pada menu feeder integrator — dipakai di: KelulusanIntegrator", "aktifkan_upload_kelulusan_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload krs di feeder integrator — dipakai di: UploadKrs", "aktifkan_upload_krs_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload krs pada menu feeder integrator — dipakai di: KrsIntegrator", "aktifkan_upload_krs_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload nilai di feeder integrator — dipakai di: UploadNilai", "aktifkan_upload_nilai_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload nilai pada menu feeder integrator — dipakai di: NilaiIntegrator", "aktifkan_upload_nilai_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload nilai transfer di feeder integrator — dipakai di: UploadNilaiTransfer", "aktifkan_upload_nilai_transfer_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload nilai transfer pada menu feeder integrator — dipakai di: NilaiTransferIntegrator", "aktifkan_upload_nilai_transfer_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload prestasi di feeder integrator — dipakai di: UploadPrestasiMahasiswa", "aktifkan_upload_prestasi_di_feeder_integrator", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload prestasi pada menu feeder integrator — dipakai di: PrestasiMahasiswaIntegrator", "aktifkan_upload_prestasi_pada_menu_feeder_integrator", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Download google drive otomatis — dipakai di: GDriveUtilPerPengguna", "download_google_drive_otomatis", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Flip gateway api key flip — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "flip_gateway_api_key_flip", ""));
		rows.appendChild(createRowNilai("Flip gateway api token flip — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "flip_gateway_api_token_flip", ""));
		rows.appendChild(createRowNilai("Google classroom client id baru — dipakai di: GoogleCommon", "google_classroom_client_id_baru", ""));
		rows.appendChild(createRowNilai("Google classroom key baru — dipakai di: GoogleCommon", "google_classroom_key_baru", ""));
		rows.appendChild(createRowActiveDefault("Gunakan google view saat mobile — dipakai di: Report, _pembayaran_online_services", "gunakan_google_view_saat_mobile", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Gunakan google view saat tampilkan pdf — dipakai di: Report, _pembayaran_online_services", "gunakan_google_view_saat_tampilkan_pdf", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Kelas digabung dengan semester saat export feeder — dipakai di: FeederExporter, FeederExporterGenerator", "kelas_digabung_dengan_semester_saat_export_feeder", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Kelas digabung dengan semester saat export feeder tanpa spasi — dipakai di: FeederExporter, FeederExporterGenerator", "kelas_digabung_dengan_semester_saat_export_feeder_tanpa_spasi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Lokasi penyimpanan upload feeder — dipakai di: ImportFromFeederAction", "lokasi_penyimpanan_upload_feeder", ""));
		rows.appendChild(createRowActiveDefault("Masing masing pt koneksi langsung ke feeder — dipakai di: EksporFromFeederAction", "masing_masing_pt_koneksi_langsung_ke_feeder", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilaiPassword("Otto api key — dipakai di: OttoUtil", "otto_api_key", ""));
		rows.appendChild(createRowActiveDefault("SemuaProdiDimasukkanSaatImportFeeder — dipakai di: FeederJSONImport", "semuaProdiDimasukkanSaatImportFeeder", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Setiap kali menyimpan item check dengan google — dipakai di: ItemAction", "setiap_kali_menyimpan_item_check_dengan_google", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Sister host url — dipakai di: DataSisterAction, DataSisterApi", "sister_host_url", "https://sister-api.kemdikbud.go.id/ws.php/1.0"));
		rows.appendChild(createRowNilai("Sister id pengguna — dipakai di: DataSisterAction, DataSisterApi", "sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408"));
		rows.appendChild(createRowNilai("Sister password — dipakai di: DataSisterAction, DataSisterApi", "sister_password", "MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN"));
		rows.appendChild(createRowNilai("Sister username — dipakai di: DataSisterAction, DataSisterApi", "sister_username", "knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE="));
		rows.appendChild(createRowActiveDefault("Terintegrasi dengan emis — dipakai di: BiodataMahasiswaAction", "terintegrasi_dengan_emis", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Terintegrasi dengan google book untuk isbn — dipakai di: GoogleBookSynchronized", "terintegrasi_dengan_google_book_untuk_isbn", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("UntukImportMatakuliahHanyaMenggunakanKodeFeeder — dipakai di: FeederJSONImport", "untukImportMatakuliahHanyaMenggunakanKodeFeeder", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Username feeder — dipakai di: ImportFromFeederAction", "username_feeder", ""));
		rows.appendChild(createRowNilai("Watzap api key — dipakai di: WaApi", "watzap_api_key", "YBIYGXHPIVEVHT3G"));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Sekolah dan Yayasan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoSekolahDanYayasan() {
		Rows rows = createSpan("Sekolah dan Yayasan (Auto)");
		rows.appendChild(createRowNilai("Report Cetak KRS Mahasiswa — dipakai di: LaporanKRSPerProdiDanAngkatan", "Report_Cetak_KRS_Mahasiswa", ""));
		rows.appendChild(createRowActiveDefault("Aktifkan download data mahasiswa — dipakai di: MahasiswaAction", "aktifkan_download_data_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download upload calon siswa — dipakai di: CalonSiswaAction", "aktifkan_download_upload_calon_siswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan filter per sekolah — dipakai di: CommonMenu, MenuHelper", "aktifkan_filter_per_sekolah", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol setujui semua karya mahasiswa — dipakai di: PenghargaanMahasiswaAction", "aktifkan_tombol_setujui_semua_karya_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol setujui semua kegiatan mahasiswa — dipakai di: KegiatanKemahasiswaanAction", "aktifkan_tombol_setujui_semua_kegiatan_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol setujui semua kegiatan siswa — dipakai di: KegiatanKesiswaanAction", "aktifkan_tombol_setujui_semua_kegiatan_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol upload data calon siswa — dipakai di: CalonSiswaAction", "aktifkan_tombol_upload_data_calon_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload password siswa — dipakai di: SiswaAction", "aktifkan_upload_password_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Apakah aktifkan modul perguruan tinggi — dipakai di: AmbilDataAlurPersetujuanSuratKeluarBanbox, BniRequestAction, Common, ParameterTambahanAction, dll", "apakah_aktifkan_modul_perguruan_tinggi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Apakah aktifkan modul sekolah — dipakai di: AmbilDataAlurPersetujuanSuratKeluarBanbox, BniRequestAction, Common, HukumanAction, dll", "apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Body login psb — dipakai di: TampilanPengumumanAkademisAction", "body_login_psb", ""));
		rows.appendChild(createRowActiveDefault("Chek bentrok jadwal pelajaran — dipakai di: JadwalPelajaranAction", "chek_bentrok_jadwal_pelajaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Class untuk generate no reg psb — dipakai di: CommonPMB", "class_untuk_generate_no_reg_psb", "ais.action.master.sekolah.psb.DefaultNoRegGeneratorPsb"));
		rows.appendChild(createRowNilai("Debug gen nim mahasiswa — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "debug_gen_nim_mahasiswa", "false"));
		rows.appendChild(createRowNilai("Default linkAndroid sekolah — dipakai di: MainHelper, qr_login", "default_linkAndroid_sekolah", "https://play.google.com/store/apps/details?id=com.eschool.zishof"));
		rows.appendChild(createRowNilai("Default linkIphone sekolah — dipakai di: MainHelper, qr_login", "default_linkIphone_sekolah", "https://apps.apple.com/us/app/eschool/id6503661156?l=id"));
		rows.appendChild(createRowActiveDefault("Fungsi pin di psb diaktfikan — dipakai di: CariDataPembayaranAction, PembayaranOnlineAction", "fungsi_pin_di_psb_diaktfikan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Guru wajib menggunakan tombol start stop di absensi — dipakai di: AbsensiSiswaHelper", "guru_wajib_menggunakan_tombol_start_stop_di_absensi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Informasi kelulusan sekolah — dipakai di: CariDataPesertaUjianAction", "informasi_kelulusan_sekolah", "NISN Anda [nis], nis ini bisa Anda gunakan untuk login ke http://ecampus dengan username NISN password NISN."));
		rows.appendChild(createRowNilai("Judul header sekolah — dipakai di: MainAction, MainAction2, MyInit, header", "judul_header_sekolah", "eSchool"));
		rows.appendChild(createRowNilai("Jumlah digit gen nim mahasiswa — dipakai di: BinaInsaniNimGenerator, PRODI_URUT_YYYY_NimGenerator, PRODI_YY_BARUPINDAHAN_URUT_NimGenerator, PRODI_YY_SMT_URUT_NimGenerator, dll", "jumlah_digit_gen_nim_mahasiswa", "4"));
		rows.appendChild(createRowActiveDefault("Kkm tampil di input nilai raport — dipakai di: VerifikasiMatapelajaranPMBHelper", "kkm_tampil_di_input_nilai_raport", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label instansi sekolah — dipakai di: InformasiPSB", "label_instansi_sekolah", "Nama Instansi Sekolah"));
		rows.appendChild(createRowNilai("Label psb kampus — dipakai di: PSBAction", "label_psb_kampus", "Seleksi Penerimaan Siswa Baru"));
		rows.appendChild(createRowNilai("Maksimal data dasbor angket guru — dipakai di: LaporanAngketGuruDashboardWindow", "maksimal_data_dasbor_angket_guru", "20000"));
		rows.appendChild(createRowNilai("Masa berlaku kartu siswa — dipakai di: LaporanKartuSiswa", "masa_berlaku_kartu_siswa", ""));
		rows.appendChild(createRowActiveDefault("Menggunakan kode generate guru — dipakai di: GuruAction", "menggunakan_kode_generate_guru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Nama usermanual mahasiswa — dipakai di: MainHelper", "nama_usermanual_mahasiswa", "Presentasi_eCampus_Modul mahasiswa.pdf"));
		rows.appendChild(createRowActiveDefault("Pengumuman tampil semua sekolah — dipakai di: TampilanPengumumanAkademisAction", "pengumuman_tampil_semua_sekolah", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Pengumuman tampil semua yayasan — dipakai di: TampilanPengumumanAkademisAction", "pengumuman_tampil_semua_yayasan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Pilihan semester di jadwalPelajaran dibuat default semua aja — dipakai di: AbsensiAction", "pilihan_semester_di_jadwalPelajaran_dibuat_default_semua_aja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Posisi pengumuman psb dibawah pilihan daftar — dipakai di: PSBAction", "posisi_pengumuman_psb_dibawah_pilihan_daftar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Setelah daftar psb langsung cetak kartu — dipakai di: CalonSiswaAction, _cetak_kartu_pendaftaran", "setelah_daftar_psb_langsung_cetak_kartu", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan orang tua mahasiswa — dipakai di: OrangTuaAction", "singkronkan_orang_tua_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Singkronkan orang tua siswa — dipakai di: OrangTuaAction", "singkronkan_orang_tua_siswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Tampilan jumlah agenda jadwalPelajaran — dipakai di: CalendarJadwalPelajaranMingguIniComposer", "tampilan_jumlah_agenda_jadwalPelajaran", ""));
		rows.appendChild(createRowNilai("Tampilan jumlah agenda jadwal pelajaran — dipakai di: PertemuanJadwalPelajaranAction", "tampilan_jumlah_agenda_jadwal_pelajaran", ""));
		rows.appendChild(createRowActiveDefault("Tampilan pilihan hanya dosen dan guru saja — dipakai di: LaporanAbsensiPegawai, LaporanAbsensiPegawaiPerHari, LaporanAbsensiPegawaiPerOrang, LaporanAbsensiPegawaiPerOrangHorizontal, dll", "tampilan_pilihan_hanya_dosen_dan_guru_saja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan alur psb — dipakai di: PSBAction", "tampilkan_alur_psb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan formulir psb — dipakai di: PSBAction", "tampilkan_formulir_psb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan info sekolah dari mana pada ppdb — dipakai di: CalonSiswaAction", "tampilkan_info_sekolah_dari_mana_pada_ppdb", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan informasiKelulusan psb — dipakai di: PSBAction", "tampilkan_informasiKelulusan_psb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan jam masuk absen untuk mahasiswa — dipakai di: AbsensiGrupPertemuanHelper, AbsensiHelper", "tampilkan_jam_masuk_absen_untuk_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan jam masuk absen untuk siswa — dipakai di: AbsensiSiswaHelper", "tampilkan_jam_masuk_absen_untuk_siswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan komentar di aktifitas jadwalPelajaran — dipakai di: AktifitasPembelajaranHelper", "tampilkan_komentar_di_aktifitas_jadwalPelajaran", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tampilkan lampiran lain di agenda pelajaran — dipakai di: AktifitasPembelajaranHelper", "tampilkan_lampiran_lain_di_agenda_pelajaran", ""));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data calon siswa — dipakai di: CalonSiswaAction", "tampilkan_link_login_oleh_admin_di_data_calon_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data guru — dipakai di: GuruAction", "tampilkan_link_login_oleh_admin_di_data_guru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data mahasiswa — dipakai di: MahasiswaAction", "tampilkan_link_login_oleh_admin_di_data_mahasiswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data siswa — dipakai di: SiswaAction", "tampilkan_link_login_oleh_admin_di_data_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan loginCalonMhs psb — dipakai di: PSBAction", "tampilkan_loginCalonMhs_psb", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile guru — dipakai di: GuruAction", "tampilkan_mobile_di_profile_guru", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile siswa — dipakai di: SiswaAction", "tampilkan_mobile_di_profile_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan search kelas di penjadwalan — dipakai di: AbsensiAction, PenilaianAction, PerkuliahanAction, PertemuanAction", "tampilkan_search_kelas_di_penjadwalan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tabungan siswa — dipakai di: PembayaranOnline, PembayaranSiswaAction", "tampilkan_tabungan_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan calon dengan mahasiswa — dipakai di: CetakRegistrasiAction", "tampilkan_tombol_singkronkan_calon_dengan_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan calon dengan siswa — dipakai di: CalonSiswaAction", "tampilkan_tombol_singkronkan_calon_dengan_siswa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tanggal lahir manual tampil di mahasiswa — dipakai di: BiodataMahasiswaAction, MahasiswaAction", "tanggal_lahir_manual_tampil_di_mahasiswa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Tata tertib kartu siswa — dipakai di: LaporanKartuSiswa", "tata_tertib_kartu_siswa", ""));
		rows.appendChild(createRowActiveDefault("Upload file di konfigurasi tiap sekolah bisa beda — dipakai di: LaporanKartuSiswa", "upload_file_di_konfigurasi_tiap_sekolah_bisa_beda", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("User sekolah — dipakai di: SopAction", "user_sekolah", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("User yayasan — dipakai di: AlurSopAction, DisposisiAlurSopAction, DisposisiSopAction, SopAction", "user_yayasan", Konfigurasi.TIDAK_AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Akademik Kampus" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoAkademikKampus() {
		Rows rows = createSpan("Akademik Kampus (Auto)");
		rows.appendChild(createRowActiveDefault("Rekaman Nilai 2 Kolom harus berdasarkan jenjang — dipakai di: LaporanRekamanNilai2Kolom", "Rekaman_Nilai_2_Kolom_harus_berdasarkan_jenjang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Report Ijazah — dipakai di: LaporanIjazahPerProdiDanAngkatan", "Report_Ijazah", ""));
		rows.appendChild(createRowActiveDefault("Absen piket otomatis belum — dipakai di: DetailAbsenGuruPiketHelper, DetailAbsenPiketHelper, DetailAbsenPiketMahasiswaHelper", "absen_piket_otomatis_belum", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Admin yg boleh buka link login dosen — dipakai di: BiodataDosenAction", "admin_yg_boleh_buka_link_login_dosen", ""));
		rows.appendChild(createRowNilai("Role yang boleh menampilkan tombol Ambil Mahasiswa pada Dosen PA. "
				+ "Isi dengan tbmrole.roleid; jika lebih dari satu pisahkan dengan koma — dipakai di: DetailPAHelper",
				"hak_akses_ambil_mahasiswa_dosen_pa", "am,admfak,admjur"));
		rows.appendChild(createRowActiveDefault("Aktifkan proses migrasi nilai — dipakai di: JamPerkuliahanSyncrhonizerProcessor", "aktifkan_proses_migrasi_nilai", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan simpan format nilai ke semua smt — dipakai di: FormatPenilaianHelper", "aktifkan_simpan_format_nilai_ke_semua_smt", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol setujui semua karya dosen — dipakai di: PenghargaanDosenAction", "aktifkan_tombol_setujui_semua_karya_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol setujui semua kegiatan dosen — dipakai di: KegiatanKedosenanAction", "aktifkan_tombol_setujui_semua_kegiatan_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan upload data absen — dipakai di: AbsensiHelper, AbsensiSiswaHelper", "aktifkan_upload_data_absen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bisa pilih prodi lain saat pilih dosen pa — dipakai di: AmbilDataDosenBanbox", "bisa_pilih_prodi_lain_saat_pilih_dosen_pa", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bisa pilih semua tahun akademik — dipakai di: CommonCurrentSessionHelper", "bisa_pilih_semua_tahun_akademik", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Buka kunci nilai untuk jadwal tanpa dosen — dipakai di: DetailperkuliahanForPenilaianHelper", "buka_kunci_nilai_untuk_jadwal_tanpa_dosen", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Debug ambil ambilDetailperkuliahan — dipakai di: KrsDetailHelper, _krs_paket_service, _krs_service", "debug_ambil_ambilDetailperkuliahan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("DosenBolehVerifikasiNilaiSendiri — dipakai di: Perkuliahan", "dosenBolehVerifikasiNilaiSendiri", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen ikut masuk ketika di akses dianggap hadir — dipakai di: PertemuanPunyaDiskusiHelper", "dosen_ikut_masuk_ketika_di_akses_dianggap_hadir", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen ikut masuk ketika di dikusi dianggap hadir — dipakai di: PertemuanPunyaDiskusiHelper", "dosen_ikut_masuk_ketika_di_dikusi_dianggap_hadir", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen langsung ke manajemen krs — dipakai di: AutoStarter", "dosen_langsung_ke_manajemen_krs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen penguji dan pembimbing skrips boleh sama — dipakai di: MahasiswaRequestTugasAkhirAction", "dosen_penguji_dan_pembimbing_skrips_boleh_sama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen penguji dan pembimbing skripsi boleh sama — dipakai di: SkripsiAction", "dosen_penguji_dan_pembimbing_skripsi_boleh_sama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dosen wajib menggunakan tombol start stop di absensi — dipakai di: AbsensiHelper", "dosen_wajib_menggunakan_tombol_start_stop_di_absensi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya dosen yg boleh entry nilai — dipakai di: AktifitasPerkuliahanHelper, DetailperkuliahanForPenilaianHelper, PenilaianAction", "hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya super admin yang boleh menambah format nilai baru — dipakai di: FormatPenilaianHelper", "hanya_super_admin_yang_boleh_menambah_format_nilai_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Import nilai huruf — dipakai di: FeederJSONImport", "import_nilai_huruf", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Ips tidak menghitung nilai konversi — dipakai di: KrsDanSkripsiHelper", "ips_tidak_menghitung_nilai_konversi", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jam perkuliahan synchronizer — dipakai di: JamPerkuliahanSyncrhonizerProcessor", "jam_perkuliahan_synchronizer", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Jam perkuliahan syncrhonizer min jam — dipakai di: JamPerkuliahanSyncrhonizerProcessor", "jam_perkuliahan_syncrhonizer_min_jam", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jam perkuliahan wajib dipilih — dipakai di: PenjadwalanUtil", "jam_perkuliahan_wajib_dipilih", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jumlah kalender akademik home — dipakai di: MainAction", "jumlah_kalender_akademik_home", "12"));
		rows.appendChild(createRowNilai("Jumlah pilihan checklist penilaian dosen oleh dosen — dipakai di: ChecklistPenilaianOlehDosenAction", "jumlah_pilihan_checklist_penilaian_dosen_oleh_dosen", "5"));
		rows.appendChild(createRowActiveDefault("Khs ambil dari parameter — dipakai di: LaporanKHS", "khs_ambil_dari_parameter", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Khs dipisahkan tiap jenjang — dipakai di: CommonReportHelper, LaporanKHS, LaporanKRS", "khs_dipisahkan_tiap_jenjang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Kode fakultas default gen nim stai — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "kode_fakultas_default_gen_nim_stai", "01"));
		rows.appendChild(createRowNilai("Kode prodi default gen nim stai — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "kode_prodi_default_gen_nim_stai", "01"));
		rows.appendChild(createRowActiveDefault("Krs ambil dari parameter — dipakai di: CommonReportHelper", "krs_ambil_dari_parameter", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Label skripsi — dipakai di: AmbilDataMahasiswaMendaftarWisudaHelper, BiodataMahasiswaAction, DetailwisudaHelper, FormatPenilaianSkripsiHelper, dll", "label_skripsi", "skripsi"));
		rows.appendChild(createRowActiveDefault("Masa perkuliahan di dibuat berdasar jadwal perkuliahan — dipakai di: MasaPerkuliahanSyncrhonizerProcessor", "masa_perkuliahan_di_dibuat_berdasar_jadwal_perkuliahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Masa perkuliahan di jadwal perkuliahan wajib diisi — dipakai di: PenjadwalanUtil", "masa_perkuliahan_di_jadwal_perkuliahan_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Masa perkuliahan synchronizer — dipakai di: MasaPerkuliahanSyncrhonizerProcessor", "masa_perkuliahan_synchronizer", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Masukkan mk konversi di khs — dipakai di: CommonReportHelper, LaporanKHS", "masukkan_mk_konversi_di_khs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Masukkan mk konversi di krs — dipakai di: CommonReportHelper, LaporanKRSPerProdiDanAngkatan", "masukkan_mk_konversi_di_krs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Matakuliah tanpa spasi — dipakai di: MemoryDbUtil", "matakuliah_tanpa_spasi", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Max semester pilihan — dipakai di: AmbilDataIkutPerkuliahanHelper, AmbilDataKurikulumPerkuliahanHelper, AmbilDataPaketPerkuliahanHelper, AmbilDataPerkuliahanBandbox, dll", "max_semester_pilihan", "25"));
		rows.appendChild(createRowNilai("Minimal catatan khs — dipakai di: CatatanHelper", "minimal_catatan_khs", "0"));
		rows.appendChild(createRowNilai("Minimal catatan krs — dipakai di: CatatanHelper", "minimal_catatan_krs", "0"));
		rows.appendChild(createRowNilai("Nama usermanual dosen — dipakai di: MainHelper", "nama_usermanual_dosen", "Presentasi_eCampus_Modul dosen.pdf"));
		rows.appendChild(createRowActiveDefault("Pilihan semester di perkuliahan dibuat default semua aja — dipakai di: AbsensiAction, DetailperkuliahanAction, DownloadNilaiTransfer, LaporanRekapAngketDosenWindow, dll", "pilihan_semester_di_perkuliahan_dibuat_default_semua_aja", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Prosentasi nilai skp kualitas — dipakai di: LaporanRealisasiLkpCatatanHarianWindow, LaporanRealisasiLkpDetailWindow, LaporanRealisasiLkpTerpaduWindow, LaporanRealisasiLkpWindow", "prosentasi_nilai_skp_kualitas", "10"));
		rows.appendChild(createRowNilai("Prosentasi nilai skp kuantitas — dipakai di: LaporanRealisasiLkpCatatanHarianWindow, LaporanRealisasiLkpDetailWindow, LaporanRealisasiLkpTerpaduWindow, LaporanRealisasiLkpWindow", "prosentasi_nilai_skp_kuantitas", "70"));
		rows.appendChild(createRowNilai("Prosentasi nilai skp waktu — dipakai di: LaporanRealisasiLkpCatatanHarianWindow, LaporanRealisasiLkpDetailWindow, LaporanRealisasiLkpTerpaduWindow, LaporanRealisasiLkpWindow", "prosentasi_nilai_skp_waktu", "20"));
		rows.appendChild(createRowActiveDefault("Saat ambil krs secara default hanya pilih smt berjalan — dipakai di: AmbilDataPerkuliahanHelper", "saat_ambil_krs_secara_default_hanya_pilih_smt_berjalan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Saat ambil krs secara default pilih semua prodi — dipakai di: AmbilDataPerkuliahanHelper", "saat_ambil_krs_secara_default_pilih_semua_prodi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Saat ambil krs secara default pilih semua semester — dipakai di: AmbilDataPerkuliahanHelper", "saat_ambil_krs_secara_default_pilih_semua_semester", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Saat cetak krs tidak tampil export — dipakai di: CommonReportHelper", "saat_cetak_krs_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Saring nilai ipk juga berdasarkan nama — dipakai di: Mahasiswa", "saring_nilai_ipk_juga_berdasarkan_nama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("SembunyikanNilaiJikaBelumDiverifikasi — dipakai di: Perkuliahan", "sembunyikanNilaiJikaBelumDiverifikasi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Semester mulai — dipakai di: pembayaran_online_mhs_services", "semester_mulai", "Ganjil"));
		rows.appendChild(createRowActiveDefault("TampilRiwayatAbsen — dipakai di: BiodataPegawaiAction", "tampilRiwayatAbsen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil nilai buku minimal di asset — dipakai di: MasterAssetAction", "tampil_nilai_buku_minimal_di_asset", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil prefix matakuliah — dipakai di: MatakuliahAction", "tampil_prefix_matakuliah", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil tingkat kesulitan matakuliah — dipakai di: MatakuliahAction", "tampil_tingkat_kesulitan_matakuliah", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tampilan jumlah agenda perkuliahan — dipakai di: AbsensiKehadiranDosenHarianDetailHelper, AmbilDataIkutPerkuliahanHelper, CalendarPerkuliahanMingguIniComposer, DosenMengajarHelper, dll", "tampilan_jumlah_agenda_perkuliahan", ""));
		rows.appendChild(createRowActiveDefault("Tampilkan filter prodi di daftar kehadiran — dipakai di: HadirAction", "tampilkan_filter_prodi_di_daftar_kehadiran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan foto di absensi kehadiran — dipakai di: AbsensiHelper, AbsensiSiswaHelper", "tampilkan_foto_di_absensi_kehadiran", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan hanya input nilai huruf — dipakai di: DetailperkuliahanForPenilaianHelper", "tampilkan_hanya_input_nilai_huruf", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ijazah ke admin — dipakai di: LaporanTranskipAkademik", "tampilkan_ijazah_ke_admin", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ijazah ke mhs — dipakai di: LaporanTranskipAkademik", "tampilkan_ijazah_ke_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan kkn di dashboard samping — dipakai di: ProfileDosen, ProfileMahasiswa", "tampilkan_kkn_di_dashboard_samping", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan komentar di aktifitas perkuliahan — dipakai di: AktifitasPerkuliahanHelper", "tampilkan_komentar_di_aktifitas_perkuliahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data dosen — dipakai di: BiodataDosenAction", "tampilkan_link_login_oleh_admin_di_data_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile dosen — dipakai di: BiodataDosenAction", "tampilkan_mobile_di_profile_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pengaturan bulanan nol nilai bisa diubah — dipakai di: DetailPembayaranMahasiswaRenderer, PembayaranUtilHelper", "tampilkan_pengaturan_bulanan_nol_nilai_bisa_diubah", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan semua bentuk beda transkrip — dipakai di: LaporanTranskipAkademik", "tampilkan_semua_bentuk_beda_transkrip", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan semua bentuk transkrip — dipakai di: LaporanTranskipAkademik", "tampilkan_semua_bentuk_transkrip", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol hapus semua di krs — dipakai di: StudiMahasiswaHelper", "tampilkan_tombol_hapus_semua_di_krs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol krs di profile — dipakai di: ProfileMahasiswa", "tampilkan_tombol_krs_di_profile", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol krs paket di profile — dipakai di: ProfileMahasiswa", "tampilkan_tombol_krs_paket_di_profile", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol transkrip di profile — dipakai di: ProfileMahasiswa", "tampilkan_tombol_transkrip_di_profile", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol upload ref di dosen — dipakai di: SkripsiAction", "tampilkan_tombol_upload_ref_di_dosen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan total mutu di krs — dipakai di: AmbilLaporanMahasiswa, CommonReportHelper, LaporanKHS, LaporanKHSPerProdiDanAngkatan, dll", "tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan total nilai di krs — dipakai di: AmbilLaporanMahasiswa, CommonReportHelper, LaporanKHS, LaporanKHSPerProdiDanAngkatan, dll", "tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan transkrip 2 kolom ke mhs — dipakai di: LaporanTranskipAkademik", "tampilkan_transkrip_2_kolom_ke_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan upload nilai di modul penilaian — dipakai di: DetailperkuliahanForPenilaianHelper", "tampilkan_upload_nilai_di_modul_penilaian", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan yang belum telah ambil krs — dipakai di: MahasiswaAction", "tampilkan_yang_belum_telah_ambil_krs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tetap tampil nilai walau 0 — dipakai di: LaporanKHS", "tetap_tampil_nilai_walau_0", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Toleransi jam masuk perkuliahan dalam menit sebelum — dipakai di: CommonPayroll", "toleransi_jam_masuk_perkuliahan_dalam_menit_sebelum", "60"));
		rows.appendChild(createRowNilai("Toleransi jam masuk perkuliahan dalam menit setelah — dipakai di: CommonPayroll", "toleransi_jam_masuk_perkuliahan_dalam_menit_setelah", "60"));
		rows.appendChild(createRowActiveDefault("Upload file di konfigurasi tiap jurusan bisa beda — dipakai di: FileFotoLain, LaporanKartuAlumni, LaporanKartuMahasiswa, LaporanKartuPegawai", "upload_file_di_konfigurasi_tiap_jurusan_bisa_beda", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("User fakultas — dipakai di: AlurSopAction, DisposisiAlurSopAction, DisposisiSopAction, SopAction", "user_fakultas", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("User jurusan — dipakai di: SopAction", "user_jurusan", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Aset dan Pengadaan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoAsetDanPengadaan() {
		Rows rows = createSpan("Aset dan Pengadaan (Auto)");
		rows.appendChild(createRowNilai("Label penyedia header — dipakai di: VendorAction", "label_penyedia_header", "Informasi Proyek Pekerjaan dan Pengumuman"));
		rows.appendChild(createRowNilai("Link post perbaikan-asset — dipakai di: PerbaikanAssetAction", "link_post_perbaikan-asset", ""));
		rows.appendChild(createRowActiveDefault("Tampil berat unit di asset — dipakai di: MasterAssetAction", "tampil_berat_unit_di_asset", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil p l t di asset — dipakai di: MasterAssetAction", "tampil_p_l_t_di_asset", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil umur ekonomis di asset — dipakai di: MasterAssetAction", "tampil_umur_ekonomis_di_asset", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("TampilkanRuanganDamPemilikAset — dipakai di: PemesananPengadaanMasterAssetAction, PenerimaanPengadaanMasterAssetAction, PerjanjianKerjasamaMasterAssetAction, PermintaanPengadaanMasterAssetAction, dll", "tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan stok persediakan — dipakai di: PenerimaanPengadaanMasterAssetAction", "tampilkan_stok_persediakan", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Tampilan dan Label Tambahan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoTampilanDanLabelTambahan() {
		Rows rows = createSpan("Tampilan dan Label Tambahan (Auto)");
		rows.appendChild(createRowActiveDefault("Aktifkan menu baru untuk pengguna — dipakai di: MainAction, MainAction2, TampilanPengumumanAkademisAction, indexl, dll", "aktifkan_menu_baru_untuk_pengguna", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tampilan baru untuk pengguna — dipakai di: main, sub_main", "aktifkan_tampilan_baru_untuk_pengguna", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Css cell spacing tampilan kehadiran — dipakai di: PengumumanAkademisAction", "css_cell_spacing_tampilan_kehadiran", "cellpadding=\"10\" cellspacing=\"15\""));
		rows.appendChild(createRowActiveDefault("Default home login versi baru — dipakai di: Index", "default_home_login_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Footer style main — dipakai di: MainAction", "footer_style_main", ""));
		rows.appendChild(createRowNilai("Judul header — dipakai di: MainAction, MainAction2, MyInit, _belum_login_anjungan, dll", "judul_header", "eCampus"));
		rows.appendChild(createRowNilai("Jumlah pengumuman home modern — dipakai di: MainAction", "jumlah_pengumuman_home_modern", "30"));
		rows.appendChild(createRowNilai("Label alumni kampus — dipakai di: AlumniAction, LoginAlumniAction", "label_alumni_kampus", "Informasi dan Tracer Study Alumni"));
		rows.appendChild(createRowNilai("Label dashboard kampus — dipakai di: TampilanDashboardAction", "label_dashboard_kampus", "Sistem Informasi Dashboard"));
		rows.appendChild(createRowNilai("Label dokumen kampus — dipakai di: DashboardDokumenAkreditasi", "label_dokumen_kampus", "Sistem Informasi Dokumen"));
		rows.appendChild(createRowNilai("Label hadir kampus — dipakai di: HadirAction", "label_hadir_kampus", "Informasi Kehadiran Dosen Harian"));
		rows.appendChild(createRowNilai("Label karir header — dipakai di: KarirAction", "label_karir_header", "Bergabunglah bersama kami!"));
		rows.appendChild(createRowNilai("Login button css — dipakai di: CommonUiFactoryHelper", "login_button_css", ""));
		rows.appendChild(createRowNilai("Login remember css — dipakai di: CommonUiFactoryHelper", "login_remember_css", ""));
		rows.appendChild(createRowNilai("Menu bg color — dipakai di: MainAction, MainAction2, TampilanAlurSopAction, TampilanPengumumanAkademisAction, dll", "menu_bg_color", "#F5F5F5"));
		rows.appendChild(createRowActiveDefault("Pengumuman home modern di center — dipakai di: MainAction", "pengumuman_home_modern_di_center", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilan nama kurir di penerimaan — dipakai di: PenerimaanPengadaanMasterAssetAction", "tampilan_nama_kurir_di_penerimaan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu bantuan — dipakai di: MainMenuHelper, MainTreeMenuHelper", "tampilkan_menu_bantuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan menu utama sebagai dropdown header — dipakai di: MainAction", "tampilkan_menu_utama_sebagai_dropdown_header", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan shortcut repository antar jemput di header — dipakai di: MainAction", "tampilkan_shortcut_repository_antar_jemput_di_header", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tombol tampilan baru — dipakai di: MainAction, PMBAction, index", "tombol_tampilan_baru", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Backup dan Pemeliharaan" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoBackupDanPemeliharaan() {
		Rows rows = createSpan("Backup dan Pemeliharaan (Auto)");
		rows.appendChild(createRowActiveDefault("Log cleaner processor — dipakai di: LogCleanerProcessor", "log_cleaner_processor", Konfigurasi.AKTIF));
	}

	/**
	 * [AUTO-TERDETEKSI] Konfigurasi modul "Lain-lain (Terdeteksi Otomatis)" yang sebelumnya tercipta
	 * tersebar di berbagai class tetapi belum terdaftar di halaman ini.
	 * Daftar dihasilkan dari pemindaian getKonfigurasi() di seluruh source
	 * (java + jsp/zul). Deskripsi tiap baris menyebut class/halaman pemakai
	 * sehingga admin tahu dampak perubahan nilainya.
	 */
	protected void initTabAutoLainLainTerdeteksiOtomatis() {
		Rows rows = createSpan("Lain-lain (Terdeteksi Otomatis) (Auto)");
		initTabAutoLainLainTerdeteksiOtomatisBagian2(rows);
		initTabAutoLainLainTerdeteksiOtomatisBagian3(rows);
		rows.appendChild(createRowNilai("BRI CHANNEL ID — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_CHANNEL_ID", "12345"));
		rows.appendChild(createRowNilai("BRI CLIENT ID — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_CLIENT_ID", "WAVmwxO0EXUJyW4SDiY4ydUAe3gUvQYD"));
		rows.appendChild(createRowNilai("BRI CLIENT SECRET — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_CLIENT_SECRET", "IGa7p9oeRJUhfdVR"));
		rows.appendChild(createRowNilai("BRI EXTERNAL ID — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_EXTERNAL_ID", "1262222"));
		rows.appendChild(createRowNilai("BRI PARTNER ID — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_PARTNER_ID", "ECAMPUS"));
		rows.appendChild(createRowNilai("BRI PRIVATE KEY — dipakai di: BRIUtil", "BRI_PRIVATE_KEY", ""));
		rows.appendChild(createRowNilai("BRI SRV ID — dipakai di: BRIDataUtil, BcaDataUtil2", "BRI_SRV_ID", "23212"));
		rows.appendChild(createRowNilai("CURRENT LOCAL URL — dipakai di: Report", "CURRENT_LOCAL_URL", "http://localhost/ecampus"));
		rows.appendChild(createRowActiveDefault("ONLINE BOOK DIR — dipakai di: OnlineBookExplorer", "ONLINE_BOOK_DIR", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("ONLINE BOOK ROOT URL — dipakai di: OnlineBookExplorer", "ONLINE_BOOK_ROOT_URL", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Report Kartu Hasil Studi — dipakai di: LaporanKHSPerProdiDanAngkatan", "Report_Kartu_Hasil_Studi", ""));
		rows.appendChild(createRowActiveDefault("Admin pt tampilkan profile — dipakai di: ProfileAction", "admin_pt_tampilkan_profile", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Admin yg boleh buka link login pengguna — dipakai di: TbmuserAction", "admin_yg_boleh_buka_link_login_pengguna", ""));
		rows.appendChild(createRowNilai("Admin yg boleh ubah dan tambah admin — dipakai di: TbmuserAction", "admin_yg_boleh_ubah_dan_tambah_admin", ""));
		rows.appendChild(createRowNilai("Admin yg boleh upload (daftar roleId dipisah koma) — role tambahan yang dianggap 'admin boleh upload'. HANYA berpengaruh saat 'hanya_admin_saja_yg_boleh_uload' AKTIF. CATATAN: daftar ini DIABAIKAN untuk pengguna ber-konteks Sekolah (getApakahAdminBolehUpload selalu false di konteks sekolah). TERKAIT: hanya_admin_saja_yg_boleh_uload, hak_akses_upload_data_siswa. — dipakai di: CommonCurrentSessionHelper", "admin_yg_boleh_upload", ""));
		rows.appendChild(createRowActiveDefault("Ai menggunakan gemini (DEFAULT MATI = pakai Ollama; nyalakan hanya bila ingin Gemini) — dipakai di: AIGenerator, Wa", "ai_menggunakan_gemini", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Akreditasi content — dipakai di: DashboardAction", "akreditasi_content", ""));
		rows.appendChild(createRowActiveDefault("Akses ke dashboard tanpa login tidak diizinkan — dipakai di: Dashboard", "akses_ke_dashboard_tanpa_login_tidak_diizinkan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan akun demo — dipakai di: InitDataHelper", "aktifkan_akun_demo", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Data sample eBisnis — mengizinkan provisioning data contoh apotik, tenaga medis, inventory, dan unit usaha (DEFAULT NONAKTIF)", Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan chatbot — dipakai di: Wa", "aktifkan_chatbot", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan chek ulang bank online — dipakai di: VirtualAccountBankAction", "aktifkan_chek_ulang_bank_online", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan chek ulang semua mandiri — dipakai di: LogHostToHostAction", "aktifkan_chek_ulang_semua_mandiri", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan chek ulang semua ocbc — dipakai di: LogHostToHostAction", "aktifkan_chek_ulang_semua_ocbc", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan download password orang tua — dipakai di: MahasiswaAction", "aktifkan_download_password_orang_tua", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan ekivalen — dipakai di: Mahasiswa", "aktifkan_ekivalen", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan finger print otomatis dari keterangan — dipakai di: InitDataHelper", "aktifkan_finger_print_otomatis_dari_keterangan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan kesamaan kode — dipakai di: Mahasiswa", "aktifkan_kesamaan_kode", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan kesamaan nama — dipakai di: Mahasiswa", "aktifkan_kesamaan_nama", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan modul rumah sakit — dipakai di: InitSirs", "aktifkan_modul_rumah_sakit", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pemilihan bahasa — dipakai di: HadirAction, PMBAction", "aktifkan_pemilihan_bahasa", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pilihan meja — dipakai di: _beranda_anggota, toko_online", "aktifkan_pilihan_meja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan pin smartcard mhs — dipakai di: BiodataMahasiswaAction", "aktifkan_pin_smartcard_mhs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tidak dihitung hari sabtu minggu — dipakai di: InitDataHelper", "aktifkan_tidak_dihitung_hari_sabtu_minggu", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol check ulang faspay — dipakai di: FaspayRequestAction", "aktifkan_tombol_check_ulang_faspay", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol hitung ulang semua — dipakai di: PenilaianAction", "aktifkan_tombol_hitung_ulang_semua", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol sinkronkan semua — dipakai di: DetailperkuliahanForPenilaianHelper, DetailperkuliahanHelper, PenilaianAction, PerkuliahanAction", "aktifkan_tombol_sinkronkan_semua", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Aktifkan tombol verifikasi semua baru — dipakai di: PenilaianAction", "aktifkan_tombol_verifikasi_semua_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Akun utang id default data — dipakai di: PenyediaAsset", "akun_utang_id_default_data", ""));
		rows.appendChild(createRowNilai("Alamat url sistem penelitian dan pengabdian — dipakai di: PenelitianDanPengabdianAction, PengumumanPenelitianAction", "alamat_url_sistem_penelitian_dan_pengabdian", "http://simlitabmas.ecampus.id"));
		rows.appendChild(createRowActiveDefault("Ambil code local — dipakai di: ApiUtil", "ambil_code_local", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Ambil kode url — dipakai di: DosenAction, GuruAction, MahasiswaAction, MainHelper, dll", "ambil_kode_url", "https://dev.ecampus.id/ecampus/Api"));
		rows.appendChild(createRowActiveDefault("Apakah lampiran pr wajib — dipakai di: PermintaanPengadaanMasterAssetAction", "apakah_lampiran_pr_wajib", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("App id bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, VirtualAccountBankAction", "app_id_bankaltimtara_baru", ""));
		rows.appendChild(createRowActiveDefault("Author importer processor — dipakai di: AuthorImporterProcessor", "author_importer_processor", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Auto proses tunggakan — dipakai di: TunggakanMahasiswaDaftarUlangProcessor", "auto_proses_tunggakan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Auto proses tunggakan mhs b — dipakai di: TunggakanMahasiswaBaruProcessor", "auto_proses_tunggakan_mhs_b", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bank bisa melakukan reversal — dipakai di: PembayaranAction", "bank_bisa_melakukan_reversal", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bankaltimtara gateway qris url autentication — dipakai di: Bankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_gateway_qris_url_autentication", "https://api-dev.bankaltimtara.co.id:8084/api/user/auth"));
		rows.appendChild(createRowNilai("Bankaltimtara gateway qris url check status — dipakai di: Bankaltimtara", "bankaltimtara_gateway_qris_url_check_status", "https://api-dev.bankaltimtara.co.id:8084/api/qrismpm/transaction/status"));
		rows.appendChild(createRowNilai("Bankaltimtara gateway qris url create va — dipakai di: DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_gateway_qris_url_create_va", "https://api-dev.bankaltimtara.co.id:8084/api/qrismpm/generate"));
		rows.appendChild(createRowNilai("Bankaltimtara gateway url autentication — dipakai di: Bankaltimtara, DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_gateway_url_autentication", "https://api-dev.bankaltimtara.co.id:8300/api/user/auth"));
		rows.appendChild(createRowNilai("Bankaltimtara gateway url create va — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_gateway_url_create_va", "https://api-dev.bankaltimtara.co.id:8300/api/va/create"));
		rows.appendChild(createRowNilai("Bankaltimtara inst id qris — dipakai di: Bankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_inst_id_qris", "211028001"));
		rows.appendChild(createRowNilai("Bankaltimtara inst id va — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_inst_id_va", "0099"));
		rows.appendChild(createRowNilai("Bankaltimtara panjang va — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_panjang_va", ""));
		rows.appendChild(createRowNilai("Bankaltimtara password — dipakai di: Bankaltimtara, DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_password", "12345678"));
		rows.appendChild(createRowNilai("Bankaltimtara prefix va — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_prefix_va", "0099"));
		rows.appendChild(createRowNilai("Bankaltimtara qris password — dipakai di: Bankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_qris_password", "PB@|1Kp@paN19112021"));
		rows.appendChild(createRowNilai("Bankaltimtara qris username — dipakai di: Bankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_qris_username", "qrisdev"));
		rows.appendChild(createRowNilai("Bankaltimtara username — dipakai di: Bankaltimtara, DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara, DownloadNoUjianCalonMahasiswaBankBankaltimtara, DownloadTagihanMahasiswaBankBankaltimtara", "bankaltimtara_username", "ubtva1"));
		rows.appendChild(createRowActiveDefault("Bisa membuat institusi pendidikan baru langsung dari pilihan — dipakai di: AmbilDataNamaSekolahBanbox", "bisa_membuat_institusi_pendidikan_baru_langsung_dari_pilihan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bisa pilih semua tahun — dipakai di: CommonCurrentSessionHelper", "bisa_pilih_semua_tahun", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Bisa upload audio langsung di eleraning — dipakai di: AmbilDataAudioPertemuan", "bisa_upload_audio_langsung_di_eleraning", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bjb company id — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBjb, DownloadNoUjianCalonMahasiswaBankBjb", "bjb_company_id", "456"));
		rows.appendChild(createRowNilai("Bjb langsung cin — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline, dll", "bjb_langsung_cin", "530"));
		rows.appendChild(createRowNilai("Bjb langsung client secret — dipakai di: BJBSUtil, BJBUtil", "bjb_langsung_client_secret", "pf-f1gKNtV58qL9mbojMiILOJ2JGg6OA6YzZ9FSGP9I"));
		rows.appendChild(createRowNilai("Bjb langsung host — dipakai di: BJBSUtil, BJBUtil", "bjb_langsung_host", "http://10.44.224.31:23808"));
		rows.appendChild(createRowNilai("Bjb langsung kid — dipakai di: BJBUtil", "bjb_langsung_kid", "7KPDFVEA"));
		rows.appendChild(createRowNilai("Bjbs host — dipakai di: BJBSUtil", "bjbs_host", "http://183.91.79.70:3002"));
		rows.appendChild(createRowNilai("Bjbs password — dipakai di: BJBSUtil", "bjbs_password", "004unb"));
		rows.appendChild(createRowNilai("Bjbs username — dipakai di: BJBSUtil", "bjbs_username", "unb004"));
		rows.appendChild(createRowActiveDefault("BolehPilihLihatSemuaAkun — dipakai di: AmbilDataBanyakAkun", "bolehPilihLihatSemuaAkun", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Bri auth code barier — dipakai di: BriCommon", "bri_auth_code_barier", ""));
		rows.appendChild(createRowNilai("Bri gateway url token — dipakai di: BriCommon", "bri_gateway_url_token", "https://developer.bri.co.id/v1/api/token"));
		rows.appendChild(createRowNilai("Btn forward url — dipakai di: DownloadTagihanMahasiswaBankBtn", "btn_forward_url", ""));
		rows.appendChild(createRowActiveDefault("Btn forward url aktif — dipakai di: DownloadTagihanMahasiswaBankBtn", "btn_forward_url_aktif", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Channel id bca — dipakai di: BCA", "channel_id_bca", "95231"));
		rows.appendChild(createRowNilai("Chat bot nomor tidak direponse — dipakai di: Wa", "chat_bot_nomor_tidak_direponse", ",6287829714073,6289507007777,"));
		rows.appendChild(createRowActiveDefault("Chatbot pakai watzap — dipakai di: Wa", "chatbot_pakai_watzap", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Chek kadaluarsa — dipakai di: MncBank, Va", "chek_kadaluarsa", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Curl e smartlink via IP — dipakai di: VirtualAccountBank", "curl_e_smartlink_via_IP", "38.47.178.46"));
		rows.appendChild(createRowNilai("Curl e smartlink via PORT — dipakai di: VirtualAccountBank", "curl_e_smartlink_via_PORT", "22031"));
		rows.appendChild(createRowNilai("Curl e smartlink via USER — dipakai di: VirtualAccountBank", "curl_e_smartlink_via_USER", "zishof"));
		rows.appendChild(createRowActiveDefault("Curl e smartlink via server lain — dipakai di: VirtualAccountBank", "curl_e_smartlink_via_server_lain", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Dapatkan code via url custom — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Data member harus refernece ke data master — dipakai di: anggota_koperasi", "data_member_harus_refernece_ke_data_master", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Default alumni gunakan versi baru — dipakai di: Alumni", "default_alumni_gunakan_versi_baru", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Default indikator pembelajaran — dipakai di: KurikulumPunyaMatakuliahDetail, Pertemuan", "default_indikator_pembelajaran", "Mahasiswa mampu menjelaskan dan mendiskusikan ...."));
		rows.appendChild(createRowActiveDefault("Default lat — dipakai di: AssetDetailAction, AssetDetailPosisiHelper", "default_lat", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Default linkAndroid — dipakai di: MainHelper, qr_login", "default_linkAndroid", "https://play.google.com/store/apps/details?id=com.ecampus.zishof"));
		rows.appendChild(createRowNilai("Default linkIphone — dipakai di: MainHelper, qr_login", "default_linkIphone", "https://apps.apple.com/id/app/ecampus/id6503487876?l=id"));
		rows.appendChild(createRowActiveDefault("Default lng — dipakai di: AssetDetailAction, AssetDetailPosisiHelper", "default_lng", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Default login3 versi baru — dipakai di: Login", "default_login3_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Default login5 versi baru — dipakai di: Login", "default_login5_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Default login ke erp — dipakai di: Index", "default_login_ke_erp", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Default login versi baru — dipakai di: Login", "default_login_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Default pengalaman belajar — dipakai di: KurikulumPunyaMatakuliahDetail, Pertemuan", "default_pengalaman_belajar", "Menyimak, Mengamati, Mendiskusikan, dan Menjawab soal"));
		rows.appendChild(createRowActiveDefault("Default ppdb gunakan versi baru — dipakai di: Ppdb", "default_ppdb_gunakan_versi_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Default title forgot password — dipakai di: CommonSecurityLoginHelper, MailHelper", "default_title_forgot_password", "Pemberitahuan password untuk login ke Sistem Informasi Akademik "));
		rows.appendChild(createRowNilai("Default validator bri — dipakai di: VirtualAccountBank", "default_validator_bri", "BRI"));
		rows.appendChild(createRowNilai("Default waktu pembelajaran — dipakai di: KurikulumPunyaMatakuliahDetail, Pertemuan", "default_waktu_pembelajaran", "... x 50 menit"));
		rows.appendChild(createRowActiveDefault("Dibalik nim dan lulus — dipakai di: RekapPendaftarSpmbSemua", "dibalik_nim_dan_lulus", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Directory report bersama — dipakai di: Common", "directory_report_bersama", ""));
		rows.appendChild(createRowActiveDefault("Dokumen tampil utama — dipakai di: DashboardDokumenAkreditasi", "dokumen_tampil_utama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("File gambar biodata harus berformat jpg — dipakai di: LoginAlumniAction", "file_gambar_biodata_harus_berformat_jpg", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Finpay apikeyfinpay data — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "finpay_apikeyfinpay_data", ""));
		rows.appendChild(createRowNilai("Finpay bank host ip — dipakai di: TampilanPaymentGateway", "finpay_bank_host_ip", ""));
		rows.appendChild(createRowNilai("Finpay gateway url data — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "finpay_gateway_url_data", "https://devo.finnet.co.id/pg/payment/card/initiate"));
		rows.appendChild(createRowNilai("Finpay sof id — dipakai di: FinpayCommon", "finpay_sof_id", "finpay021"));
		rows.appendChild(createRowNilai("Finpay sof type — dipakai di: FinpayCommon", "finpay_sof_type", ""));
	}

	protected void initTabAutoLainLainTerdeteksiOtomatisBagian2(Rows rows) {
		rows.appendChild(createRowNilai("Finpay tokenFinpay data — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "finpay_tokenFinpay_data", ""));
		rows.appendChild(createRowActiveDefault("Flag data menggunakan database — dipakai di: InitDataHelper", "flag_data_menggunakan_database", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Flip bank host ip — dipakai di: TampilanPaymentGateway", "flip_bank_host_ip", ""));
		rows.appendChild(createRowNilai("Flip gateway url v2 — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanMahasiswaBankOnline, dll", "flip_gateway_url_v2", "https://bigflip.id/api/v2/pwf/bill"));
		rows.appendChild(createRowNilai("Generated angka digit jatelindo — dipakai di: JatelindoCommon, JatelindoKeranjangPembayaran", "generated_angka_digit_jatelindo", "8"));
		rows.appendChild(createRowActiveDefault("Grup transaksi order by tanggal — dipakai di: GrupTransaksiAction", "grup_transaksi_order_by_tanggal", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya admin saja yg boleh upload (sakelar global). AKTIF = tombol Upload data hanya untuk pengguna yang lolos getApakahAdminBolehUpload() (Administrator atau role di 'admin_yg_boleh_upload'). PENTING: pengguna ber-konteks Sekolah (mis. Admin Sekolah) SELALU gagal cek ini → tombol upload disembunyikan LANGSUNG meski role-nya sudah ada di 'Hak Akses Tombol Upload Data'. Non-aktifkan bila ingin mengandalkan gate per-modul saja. TERKAIT: admin_yg_boleh_upload, hak_akses_upload_data_siswa/…mahasiswa/…calon_siswa/…registrasi. — dipakai di: CommonDownloadUpload", "hanya_admin_saja_yg_boleh_uload", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya admin yg boleh ubah admin — dipakai di: TbmuserAction", "hanya_admin_yg_boleh_ubah_admin", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Hanya boleh melihat lampirannya sendiri — dipakai di: AmbilDataLampiranFileLain, AmbilDataPertemuanFileContent, AmbilDataTugasFileContent", "hanya_boleh_melihat_lampirannya_sendiri", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Hapus temporary lama — dipakai di: InitDataHelper", "hapus_temporary_lama", "2000"));
		rows.appendChild(createRowActiveDefault("Harus menggunakan satker yg sama — dipakai di: FilterLoginAis", "harus_menggunakan_satker_yg_sama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Hubungi admin calon mhs — dipakai di: CalonSiswaAction, PPDB1, PPDB2, PPDB_Alumni, dll", "hubungi_admin_calon_mhs", ""));
		rows.appendChild(createRowNilai("Id aplikasi azure — dipakai di: AzureActiveDirectory20Action, SharepointUtilPerPengguna", "id_aplikasi_azure", "id_aplikasi_azure"));
		rows.appendChild(createRowNilai("Id kode akun penutup — dipakai di: TransaksiJurnalUmumHelper", "id_kode_akun_penutup", ""));
		rows.appendChild(createRowNilai("Info dari mana ppdb — dipakai di: CalonSiswaAction", "info_dari_mana_ppdb", "Website,Teman,Radio,Koran,Lain-lain"));
		rows.appendChild(createRowNilai("Informasi kelulusan belum dapat nim — dipakai di: CariDataPesertaUjianAction", "informasi_kelulusan_belum_dapat_nim", "Anda belum mendapatkan NIM"));
		rows.appendChild(createRowNilai("Ipaymu gateway url va — dipakai di: IpaymuCommon", "ipaymu_gateway_url_va", "https://my.ipaymu.com/api/GetVa.php"));
		rows.appendChild(createRowActiveDefault("Ips juga dihitung dari sp — dipakai di: KrsDanSkripsiHelper", "ips_juga_dihitung_dari_sp", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Item importer processor — dipakai di: ItemImporterProcessor", "item_importer_processor", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Jangka waktu default — dipakai di: MahasiswaSmartlinkChannelWindow, SmartlinkChannelWindow, _lanjut_bayar_services", "jangka_waktu_default", ""));
		rows.appendChild(createRowActiveDefault("Jangka waktu default ditampilkan — dipakai di: MahasiswaSmartlinkChannelWindow, SmartlinkChannelWindow, _lanjut_bayar_services", "jangka_waktu_default_ditampilkan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Jika calon mhs sudah terdaftar dan mensetujui maka tidak boleh simpan ulang — dipakai di: BiodataCalonMahasiswaAction", "jika_calon_mhs_sudah_terdaftar_dan_mensetujui_maka_tidak_boleh_simpan_ulang", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Jika login sebagai member kecuali admin maka langsung ke halaman member — dipakai di: Main", "jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Jml jdul max — dipakai di: MahasiswaRequestTugasAkhirAction", "jml_jdul_max", "10"));
		rows.appendChild(createRowNilai("Jml system content — dipakai di: Wa", "jml_system_content", ""));
		rows.appendChild(createRowNilai("Jml tampil kehadiran dalam satu baris dekstop — dipakai di: PengumumanAkademisAction", "jml_tampil_kehadiran_dalam_satu_baris_dekstop", "5"));
		rows.appendChild(createRowNilai("Jml tampil kehadiran dalam satu baris mobile — dipakai di: PengumumanAkademisAction", "jml_tampil_kehadiran_dalam_satu_baris_mobile", "2"));
		rows.appendChild(createRowNilai("Jumlah digit no reg calon mhs — dipakai di: PrefixNoRegGenerator, PrefixTglNoRegGenerator, TAHUN_NoRegGenerator, TAHUN_PRODI_NoRegGenerator, dll", "jumlah_digit_no_reg_calon_mhs", "3"));
		rows.appendChild(createRowNilai("Kadaluarsa pemesanan item — dipakai di: UserPerpustakaanResource", "kadaluarsa.pemesanan.item", "24"));
		rows.appendChild(createRowNilai("Karir tampilkan lowongan terlewat — dipakai di: _karir_service", "karir_tampilkan_lowongan_terlewat", "TAMPIL_DISABLED"));
		rows.appendChild(createRowNilai("Kas kecil dimulai — dipakai di: KasKecilAction", "kas_kecil_dimulai", ""));
		rows.appendChild(createRowNilai("Keterangan nisn di biodata calon mhs — dipakai di: BiodataCalonMahasiswaAction", "keterangan_nisn_di_biodata_calon_mhs", ""));
		rows.appendChild(createRowNilaiPassword("Key bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, VirtualAccountBankAction", "key_bankaltimtara_baru", ""));
		rows.appendChild(createRowActiveDefault("Kirim file via watzap — dipakai di: WaApi", "kirim_file_via_watzap", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Kode institusi bankaltimtara baru — dipakai di: DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline", "kode_institusi_bankaltimtara_baru", "6001"));
		rows.appendChild(createRowNilai("Kode profesi default — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "kode_profesi_default", ""));
		rows.appendChild(createRowNilai("Kode profesi program sanad — dipakai di: JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator", "kode_profesi_program_sanad", "2"));
		rows.appendChild(createRowNilai("Konfigurasi abnormal duplicate trx id — dipakai di: BniCommon", "konfigurasi_abnormal_duplicate_trx_id", ""));
		rows.appendChild(createRowNilai("Kubuku app id — dipakai di: Kubuku", "kubuku_app_id", "app_sso_kubuku"));
		rows.appendChild(createRowNilai("Kubuku app key — dipakai di: Kubuku", "kubuku_app_key", "iniKunciSSOkubuku"));
		rows.appendChild(createRowNilai("Kunci rahasia aplikasi azure — dipakai di: AzureActiveDirectory20Action, SharepointUtilPerPengguna", "kunci_rahasia_aplikasi_azure", "kunci_rahasia_aplikasi_azure"));
		rows.appendChild(createRowActiveDefault("Laporan saldo harus berdasarkan jenis laporan — dipakai di: LaporanAkuntingSaldoBulanMaster", "laporan_saldo_harus_berdasarkan_jenis_laporan", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Link mencari nisn — dipakai di: SiswaAction", "link_mencari_nisn", "https://nisn.data.kemdikbud.go.id/page/data"));
		rows.appendChild(createRowNilai("Link mencari nisn data — dipakai di: CalonSiswaAction", "link_mencari_nisn_data", "https://nisn.data.kemdikbud.go.id/index.php/Cindex/formcaribynama"));
		rows.appendChild(createRowNilai("Llama model — dipakai di: AIGenerator, Wa", "llama_model", "llama3.2"));
		rows.appendChild(createRowNilai("Llama system pengajar — dipakai di: AktifitasPerkuliahanHelper", "llama_system_pengajar", "Kamu adalah Pengajar atau Dosen atau Guru "));
		rows.appendChild(createRowNilai("Llama system pengumuman — dipakai di: PengumumanAkademisAction", "llama_system_pengumuman", "Kamu adalah operator Sistem Informasi Akademik Perguruan Tinggi "));
		rows.appendChild(createRowNilai("Llama url — dipakai di: AIGenerator, Wa", "llama_url", "http://38.47.178.42:8011/api/chat"));
		rows.appendChild(createRowActiveDefault("Login cleaner processor — dipakai di: LoginCleanerProcessor", "login_cleaner_processor", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Login via link menggunakan domain masing masing — dipakai di: BiodataCalonMahasiswa, CalonSiswa, Mahasiswa, Siswa", "login_via_link_menggunakan_domain_masing_masing", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Lokasi file temprorary lain — dipakai di: ConstantValues", "lokasi_file_temprorary_lain", ""));
		rows.appendChild(createRowNilai("Lokasi penyimpanan file data — dipakai di: Common", "lokasi_penyimpanan_file_data", "/backup2/backup_file"));
		rows.appendChild(createRowNilai("Maja BILLING HOST — dipakai di: BSIMajaUtil", "maja_BILLING_HOST", "https://billing-bpi-dev.maja.id"));
		rows.appendChild(createRowNilai("Maja CLIENT ID — dipakai di: BSIMajaUtil", "maja_CLIENT_ID", ""));
		rows.appendChild(createRowNilai("Maja CLIENT SECRET — dipakai di: BSIMajaUtil", "maja_CLIENT_SECRET", ""));
		rows.appendChild(createRowNilai("Maja PASSWORD — dipakai di: BSIMajaUtil", "maja_PASSWORD", ""));
		rows.appendChild(createRowNilai("Maja TOKEN URL — dipakai di: BSIMajaUtil", "maja_TOKEN_URL", "https://account.makaramas.com/auth/realms/bpi-dev/protocol/openid-connect/token"));
		rows.appendChild(createRowNilai("Maja USERNAME — dipakai di: BSIMajaUtil", "maja_USERNAME", ""));
		rows.appendChild(createRowNilai("Maja bank host ip — dipakai di: TampilanPaymentGateway", "maja_bank_host_ip", ""));
		rows.appendChild(createRowActiveDefault("Maja pakai tanpa amount — dipakai di: DownloadTagihanAnggotaKoperasiBankOnline, DownloadTagihanSiswaBankOnline", "maja_pakai_tanpa_amount", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Max upload via drive baru — dipakai di: AmbilDataAudioPertemuan, AmbilDataLampiranFileLain, AmbilDataPertemuanFileContent, AmbilDataTugasFileContent, dll", "max_upload_via_drive_baru", "300"));
		rows.appendChild(createRowActiveDefault("Melihat kode mk syn data — dipakai di: MatakuliahAction", "melihat_kode_mk_syn_data", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Member tampil di deposit — dipakai di: DepositAction", "member_tampil_di_deposit", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("MenggunakanPtDefault — dipakai di: PerguruanTinggiAction", "menggunakanPtDefault", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Menggunakan lokal url di report — dipakai di: Report", "menggunakan_lokal_url_di_report", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Message enabled — dipakai di: MainAction, MainAction2", "message_enabled", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Nama usermanual — dipakai di: MainHelper", "nama_usermanual", "User_Manual__Sistem_Informasi_Akademik.pdf"));
		rows.appendChild(createRowNilai("Nama usermanual keu — dipakai di: MainHelper", "nama_usermanual_keu", "User_Manual__Keuagan_Sistem_Informasi_Akademik.pdf"));
		rows.appendChild(createRowActiveDefault("Nggak usah pakai barcode di report — dipakai di: Report", "nggak_usah_pakai_barcode_di_report", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Online 2 bank host ip — dipakai di: TampilanPaymentGateway", "online_2_bank_host_ip", ""));
		rows.appendChild(createRowNilai("Online bank host ip — dipakai di: BiodataCalonMahasiswaAction, KegiatanTemporaryAction, PembayaranKoperasiOnline, PembayaranOnline, dll", "online_bank_host_ip", ""));
		rows.appendChild(createRowActiveDefault("Otomatis terposting — dipakai di: InitDataHelper", "otomatis_terposting", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Otto bank host ip — dipakai di: TampilanPaymentGateway", "otto_bank_host_ip", ""));
		rows.appendChild(createRowNilaiPassword("Otto mid — dipakai di: OttoUtil", "otto_mid", ""));
		rows.appendChild(createRowNilai("Otto token url — dipakai di: OttoUtil", "otto_token_url", "https://dev-secure.ottopay.id/payment-services/v2.1.0/api/token"));
		rows.appendChild(createRowActiveDefault("Pakai include saat menu — dipakai di: CommonMenuAccessHelper", "pakai_include_saat_menu", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Partner id bca — dipakai di: BCA", "partner_id_bca", "14275"));
		rows.appendChild(createRowActiveDefault("Pengumuman admin tampil semua — dipakai di: TampilanPengumumanAkademisAction", "pengumuman_admin_tampil_semua", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("PersenIzin — dipakai di: Detailperkuliahan", "persenIzin", "0.5"));
		rows.appendChild(createRowNilai("PersenMasuk — dipakai di: Detailperkuliahan", "persenMasuk", "1.0"));
		rows.appendChild(createRowNilai("PersenSakit — dipakai di: Detailperkuliahan", "persenSakit", "0.5"));
		rows.appendChild(createRowActiveDefault("Pilih salah satu info ppdb dari mana — dipakai di: CalonSiswaAction", "pilih_salah_satu_info_ppdb_dari_mana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pilihan peminat 3 — dipakai di: DashboardCalonMahasiswa, RekapPendaftarSpmbSemua", "pilihan_peminat_3", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pilihan peminat 4 — dipakai di: DashboardCalonMahasiswa, RekapPendaftarSpmbSemua", "pilihan_peminat_4", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pilihan peminat 5 — dipakai di: DashboardCalonMahasiswa, RekapPendaftarSpmbSemua", "pilihan_peminat_5", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pos tanpa login — dipakai di: _pos", "pos_tanpa_login", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Pph mengurangi lpj — dipakai di: LaporanPertangungjawaban, LaporanPertangungjawabanKasBesar, PemesananPengadaanMasterAssetDetail, PenerimaanPengadaanMasterAssetDetail, dll", "pph_mengurangi_lpj", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Prefix kode bank lain online — dipakai di: KegiatanTemporaryAction, TagihanMahasiswa, TagihanSiswa, TopupHelper", "prefix_kode_bank_lain_online", ""));
		rows.appendChild(createRowNilai("Prefix kode bank lain online 2 — dipakai di: TampilanPaymentGateway", "prefix_kode_bank_lain_online_2", ""));
		rows.appendChild(createRowNilai("Prefix no reg calon mhs — dipakai di: PrefixNoRegGenerator", "prefix_no_reg_calon_mhs", "REG."));
		rows.appendChild(createRowNilai("Qris bank host ip — dipakai di: TampilanPaymentGateway", "qris_bank_host_ip", ""));
		rows.appendChild(createRowNilai("Qris jaring expire — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "qris_jaring_expire", "7200"));
		rows.appendChild(createRowNilai("Qris jaring gateway url — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "qris_jaring_gateway_url", "http://api.jsa2.host/agg/api/v1/qris/generate"));
		rows.appendChild(createRowNilai("Qris jaring merchantId — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "qris_jaring_merchantId", "3200124010015"));
		rows.appendChild(createRowNilaiPassword("Qris jaring screet key — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "qris_jaring_screet_key", ""));
		rows.appendChild(createRowNilai("Qris jaring terminalId — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankOnline, DownloadNoUjianCalonMahasiswaBankOnline, DownloadTagihanMahasiswaBankOnline, DownloadTagihanSiswaBankOnline", "qris_jaring_terminalId", "10010005"));
		rows.appendChild(createRowActiveDefault("Radius syncrhonizer — dipakai di: RadiusProcessor", "radius_syncrhonizer", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Redirect url classroom — dipakai di: GoogleCommon", "redirect_url_classroom", ""));
		rows.appendChild(createRowActiveDefault("Rekomendasi wajib penelitian dan pengabdian — dipakai di: PengajuanPenelitianDanPengabdianHelper", "rekomendasi_wajib_penelitian_dan_pengabdian", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Repository default access policy — dipakai di: RepositorySyncService", "repository_default_access_policy", "OPEN_ACCESS"));
		rows.appendChild(createRowActiveDefault("Repository turnitin index default — dipakai di: RepositorySyncService", "repository_turnitin_index_default", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Root path scorm — dipakai di: AmbilDataPertemuanFileContent", "root_path_scorm", "/opt"));
		rows.appendChild(createRowNilai("Rtmp server — dipakai di: ELearningResource, LiveStreamingPlayerWindow, VideoPertemuan", "rtmp_server", "live.ecampus.id"));
		rows.appendChild(createRowActiveDefault("Saat cetak kartu uas tidak tampil export — dipakai di: CommonReportHelper", "saat_cetak_kartu_uas_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Saat cetak kartu uts tidak tampil export — dipakai di: CommonReportHelper", "saat_cetak_kartu_uts_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Saldo awal default — dipakai di: LaporanBukuBesar, LaporanTrialBalance", "saldo_awal_default", ""));
		rows.appendChild(createRowActiveDefault("Sebelum dikunci harus diverifikasi dulu — dipakai di: DetailperkuliahanForPenilaianHelper", "sebelum_dikunci_harus_diverifikasi_dulu", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Selain admin tidak boleh merubah konversi — dipakai di: StudiMahasiswaHelper", "selain_admin_tidak_boleh_merubah_konversi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Semua akun tampil — dipakai di: AmbilDataAkunBanbox", "semua_akun_tampil", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Session timeout calon — dipakai di: SessionCounter", "session_timeout_calon", "20"));
	}

	protected void initTabAutoLainLainTerdeteksiOtomatisBagian3(Rows rows) {
		rows.appendChild(createRowNilaiPassword("Sharepoint client id — dipakai di: ApplicationProperties", "sharepoint.client.id", ""));
		rows.appendChild(createRowNilaiPassword("Sharepoint client secret — dipakai di: ApplicationProperties", "sharepoint.client.secret", ""));
		rows.appendChild(createRowNilai("Sharepoint scope — dipakai di: ApplicationProperties", "sharepoint.scope", "https://graph.microsoft.com/.default"));
		rows.appendChild(createRowNilaiPassword("Sharepoint tenant id — dipakai di: ApplicationProperties", "sharepoint.tenant.id", ""));
		rows.appendChild(createRowNilai("Sharepoint user name — dipakai di: ApplicationProperties", "sharepoint.user.name", "your microsoft account username"));
		rows.appendChild(createRowNilai("Sharepoint user password — dipakai di: ApplicationProperties", "sharepoint.user.password", "your password"));
		rows.appendChild(createRowActiveDefault("Sponsor tampil lpj — dipakai di: PertangungjawabanAction, PertangungjawabanKasBesarAction", "sponsor_tampil_lpj", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("StrClientId bca — dipakai di: BCA", "strClientId_bca", "68533592-83d5-462f-9335-9283d5a62f2d"));
		rows.appendChild(createRowNilai("StrClientScret bca — dipakai di: BCA", "strClientScret_bca", "YQegLaFKEzCU8c1kAefEGWm2scOsoA6LRO_EFZCjGvc"));
		rows.appendChild(createRowNilai("StrPublicKey bca — dipakai di: BCA", "strPublicKey_bca", ""));
		rows.appendChild(createRowNilai("StrPublicKey bri — dipakai di: Briva", "strPublicKey_bri", ""));
		rows.appendChild(createRowNilai("StrPublicKey ocbc — dipakai di: OcbcNisp", "strPublicKey_ocbc", ""));
		rows.appendChild(createRowNilai("SubstrBriOnline — dipakai di: Inquiry, Payment", "substrBriOnline", "0"));
		rows.appendChild(createRowNilai("Tahun login alumni — dipakai di: LoginAlumniAction", "tahun_login_alumni", "50"));
		rows.appendChild(createRowNilai("Tahun login calon mhs — dipakai di: LoginCalonAction", "tahun_login_calon_mhs", "50"));
		rows.appendChild(createRowActiveDefault("Tambah kecamatan baru — dipakai di: AmbilDataKecamatanBanbox", "tambah_kecamatan_baru", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("TampilKepangkatanKesana — dipakai di: BiodataPegawaiAction", "tampilKepangkatanKesana", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("TampilKeteranganBadan — dipakai di: BiodataPegawaiAction", "tampilKeteranganBadan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("TampilRiwayatBekerja — dipakai di: BiodataPegawaiAction", "tampilRiwayatBekerja", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("TampilRiwayatMediaSosial — dipakai di: BiodataDosenAction, BiodataMahasiswaAction, BiodataPegawaiAction, GuruAction, dll", "tampilRiwayatMediaSosial", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("TampilRiwayatSeminar — dipakai di: BiodataPegawaiAction", "tampilRiwayatSeminar", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil dashbord di panel utama — dipakai di: BlankAction", "tampil_dashbord_di_panel_utama", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil formulir sederhana — dipakai di: AmbilDataBiodataCalonMahasiswaBanyak, AmbilDataCalonSiswaBanyak, BiodataCalonMahasiswaAction", "tampil_formulir_sederhana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil isi form tambahan — dipakai di: RekapPendaftarSpmbSemua", "tampil_isi_form_tambahan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil pengguna sederhana — dipakai di: TbmuserAction", "tampil_pengguna_sederhana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil pengumuman sederhana — dipakai di: AlumniAction, PengumumanAkademisAction", "tampil_pengumuman_sederhana", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampil ta di status online — dipakai di: MainAction, MainAction2", "tampil_ta_di_status_online", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan file presentasi di pengajuan — dipakai di: SkripsiAction", "tampilkan_file_presentasi_di_pengajuan", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan fitur hapus atau reset angket — dipakai di: LaporanAngketDosenPerDosenWindow", "tampilkan_fitur_hapus_atau_reset_angket", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan form download dan upload nim di calon mhs — dipakai di: CetakRegistrasiAction", "tampilkan_form_download_dan_upload_nim_di_calon_mhs", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan form download dan upload ukt — dipakai di: CetakRegistrasiAction, MahasiswaAction", "tampilkan_form_download_dan_upload_ukt", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan gambar mhs — dipakai di: PengajuanMahasiswaAction, PengajuanSiswaAction", "tampilkan_gambar_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan info asal instansi — dipakai di: BiodataCalonMahasiswaAction", "tampilkan_info_asal_instansi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan ktm di profile mhs — dipakai di: BiodataMahasiswaAction", "tampilkan_ktm_di_profile_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan link login oleh admin di data pengguna — dipakai di: TbmuserAction", "tampilkan_link_login_oleh_admin_di_data_pengguna", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile mhs — dipakai di: BiodataMahasiswaAction", "tampilkan_mobile_di_profile_mhs", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan mobile di profile orang tua — dipakai di: OrangTuaAction", "tampilkan_mobile_di_profile_orang_tua", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pemesanan langsung di po — dipakai di: PemesananPengadaanMasterAssetAction", "tampilkan_pemesanan_langsung_di_po", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan penerimaan langsung di po — dipakai di: PenerimaanPengadaanMasterAssetAction", "tampilkan_penerimaan_langsung_di_po", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan pilihan paket — dipakai di: BiodataCalonMahasiswaAction", "tampilkan_pilihan_paket", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan teori praktek — dipakai di: LaporanDaftarPrestasiBelajarWindow", "tampilkan_teori_praktek", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol nonaktifkan mk double — dipakai di: MatakuliahAction", "tampilkan_tombol_nonaktifkan_mk_double", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol semua diterima — dipakai di: CalonSiswaAction", "tampilkan_tombol_semua_diterima", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan calon dengan diskon — dipakai di: CetakRegistrasiAction", "tampilkan_tombol_singkronkan_calon_dengan_diskon", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol singkronkan calon dengan nim — dipakai di: CetakRegistrasiAction", "tampilkan_tombol_singkronkan_calon_dengan_nim", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tampilkan tombol upload ref — dipakai di: SkripsiAction", "tampilkan_tombol_upload_ref", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tanggal lahir login alumni — dipakai di: LoginAlumniAction", "tanggal_lahir_login_alumni", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tanya tombol cetak kartu — dipakai di: StudiMahasiswaHelper", "tanya_tombol_cetak_kartu", Konfigurasi.AKTIF));
		rows.appendChild(createRowNilai("Tgl laporan pengajuan uang muka — dipakai di: UangMuka, UangMukaAction", "tgl_laporan_pengajuan_uang_muka", ""));
		rows.appendChild(createRowActiveDefault("Tidak boleh edit manual data di revisi data — dipakai di: GenericRevisiHelper", "tidak_boleh_edit_manual_data_di_revisi_data", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Tidak boleh entry kegiatan yang sudah terlewat — dipakai di: RealisasiKerjaPegawaiDetailAction", "tidak_boleh_entry_kegiatan_yang_sudah_terlewat", Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Tidak boleh kembalikan data di revisi data — dipakai di: GenericRevisiHelper", "tidak_boleh_kembalikan_data_di_revisi_data", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("TinggiFrame — dipakai di: dekstop, oldd_index", "tinggiFrame", "20000px"));
		rows.appendChild(createRowNilai("Tinggi halaman utama karir — dipakai di: KarirAction", "tinggi_halaman_utama_karir", "800"));
		rows.appendChild(createRowNilai("Tinggi halaman utama vendor — dipakai di: VendorAction", "tinggi_halaman_utama_vendor", "800"));
		rows.appendChild(createRowNilai("Token instance id baru — dipakai di: WaApi", "token_instance_id_baru", "instance101739"));
		rows.appendChild(createRowNilai("Token ocbc response code — dipakai di: OcbcNisp", "token_ocbc_response_code", "2007300"));
		rows.appendChild(createRowNilai("Token ocbc response message — dipakai di: OcbcNisp", "token_ocbc_response_message", "Success"));
		rows.appendChild(createRowNilai("Token ultramsg baru — dipakai di: WaApi", "token_ultramsg_baru", "dd9gcfbnp928paj0"));
		rows.appendChild(createRowNilai("Upload scrorm url — dipakai di: doUpload", "upload_scrorm_url", ""));
		rows.appendChild(createRowNilai("User bjbs — dipakai di: DownloadNoRegistrasiCalonMahasiswaBankBjb, DownloadNoUjianCalonMahasiswaBankBjb, DownloadTagihanMahasiswaBankBjb", "user_bjbs", "s1627"));
		rows.appendChild(createRowActiveDefault("Username menggunkana nidn — dipakai di: DosenAction", "username_menggunkana_nidn", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Wajib https — dipakai di: CommonCurrentSessionHelper, PerguruanTinggiUtil", "wajib_https", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowNilai("Warming up code watzap — dipakai di: UserOnlineCounter", "warming_up_code_watzap", ""));
		rows.appendChild(createRowNilai("Watzap number key — dipakai di: WaApi", "watzap_number_key", "u3w09ScxqJsNIrpG"));
	}

}
