<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="id">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><c:out value="${jurnalAdminTitle}"/></title>
  <style>
    :root{--primary:#3347b0;--accent:#087f78;--nav:#172554;--ink:#17233b;--muted:#65738b;--line:#dfe5ef;--bg:#f5f7fb;--focus:#f59e0b;--danger:#a11616}
    *{box-sizing:border-box}html{scroll-behavior:smooth}body{font-family:system-ui,-apple-system,"Segoe UI",sans-serif;font-size:16px;line-height:1.5;margin:0;background:var(--bg);color:var(--ink)}
    .skip-link{position:fixed;left:12px;top:-80px;z-index:100;background:#fff;color:#111;padding:12px;border:3px solid var(--focus)}.skip-link:focus{top:12px}
    .layout{display:grid;grid-template-columns:290px minmax(0,1fr);min-height:100vh}nav{background:var(--nav);color:#fff;padding:20px;overflow:auto}
    nav h2{margin-top:0}nav a{display:block;color:#eaf1ff;padding:9px 10px;text-decoration:none;border-radius:7px}nav a:hover,nav a[aria-current="page"]{background:#28538c;color:#fff}
    main{padding:28px;min-width:0}.toolbar,.cards{display:flex;gap:12px;align-items:center;flex-wrap:wrap}.toolbar{margin-bottom:16px}
    select,button,input,textarea{font:inherit;min-height:44px;padding:9px 12px;border:1px solid #b8c4d7;border-radius:8px;background:#fff}button{background:var(--primary);color:#fff;border-color:var(--primary);cursor:pointer}a:focus-visible,button:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible{outline:3px solid var(--focus);outline-offset:2px}
    .command{margin-top:18px}.command-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px;margin-top:12px}.field{display:flex;flex-direction:column;gap:5px}.field textarea{min-height:110px;resize:vertical}.command-actions{margin-top:14px}.result{white-space:pre-wrap;overflow:auto;max-height:240px;background:#f7f9fc;padding:10px;border-radius:7px}
    .panel,.card{background:#fff;border:1px solid var(--line);border-radius:12px;padding:18px}.card{min-width:150px}.card strong{display:block;font-size:25px}.card span,.muted{color:var(--muted)}
    .status{min-height:24px;margin:12px 0;color:var(--muted)}.status.error{color:#a11616}.table-wrap{overflow:auto;margin-top:18px}table{width:100%;border-collapse:collapse;min-width:680px}
    th,td{text-align:left;padding:10px;border-bottom:1px solid var(--line)}th{font-size:13px;color:var(--muted);background:#fafbfd}.empty{text-align:center;color:var(--muted);padding:30px}
    @media(prefers-reduced-motion:reduce){html{scroll-behavior:auto}*{animation:none!important;transition:none!important}}
    @media(max-width:850px){.layout{grid-template-columns:1fr}nav{max-height:270px}main{padding:16px}.table-wrap{overflow:visible}table,thead,tbody,tr,th,td{display:block}thead{position:absolute;clip:rect(0 0 0 0)}table{min-width:0}tr{background:#fff;border:1px solid var(--line);border-radius:12px;margin:12px 0;padding:10px}td{border:0;padding:5px 8px;overflow-wrap:anywhere}td:before{font-weight:700;color:var(--muted);margin-right:8px}td:nth-child(1):before{content:"ID: "}td:nth-child(2):before{content:"Tipe: "}td:nth-child(3):before{content:"Judul: "}td:nth-child(4):before{content:"Status: "}td:nth-child(5):before{content:"Pemilik: "}}
  </style>
</head>
<body><a class="skip-link" href="#main-content">Lewati ke konten utama</a>
<div class="layout">
  <nav aria-label="Modul pengelolaan jurnal">
    <h2>Pengelolaan Jurnal</h2>
    <c:forEach items="${jurnalAdminEntries}" var="e">
      <a href="${pageContext.request.contextPath}/jurnal/admin/${e.kunci}" <c:if test="${e.kunci == jurnalAdminKey}">aria-current="page"</c:if>><c:out value="${e.label}"/></a>
    </c:forEach>
  </nav>
  <main id="main-content" tabindex="-1">
    <h1><c:out value="${jurnalAdminTitle}"/></h1>
    <section id="workspace" class="panel" data-module="<c:out value='${jurnalAdminKey}'/>"
      data-api="${pageContext.request.contextPath}/jurnal-api" data-csrf="<c:out value='${jurnalCsrf}'/>">
      <div class="toolbar">
        <label for="journal">Jurnal</label><select id="journal"><option value="">Semua jurnal dalam scope</option></select>
        <button id="refresh" type="button">Muat ulang</button>
      </div>
      <div id="status" class="status" role="status" aria-live="polite">Memuat ruang kerja…</div>
      <div class="cards">
        <div class="card"><strong id="journalCount">0</strong><span>Jurnal dalam scope</span></div>
        <div class="card"><strong id="itemCount">0</strong><span>Item aktif</span></div>
      </div>
      <div class="table-wrap">
        <table aria-label="Item jurnal terbaru"><thead><tr><th>ID</th><th>Tipe</th><th>Judul</th><th>Status</th><th>Pemilik</th></tr></thead><tbody id="items"></tbody></table>
      </div>
      <section class="command" aria-labelledby="commandTitle">
        <h2 id="commandTitle">Tindakan</h2>
        <label for="operation">Pilih tindakan yang diizinkan</label>
        <select id="operation"><option value="">— pilih —</option></select>
        <form id="commandForm"><div id="commandFields" class="command-grid"></div><div class="command-actions"><button id="execute" type="submit" hidden>Jalankan</button></div></form>
        <div id="commandResult" class="status result" role="status" aria-live="polite" hidden></div>
      </section>
      <noscript>JavaScript diperlukan untuk ruang kerja administratif; portal publik tetap dapat dibaca tanpa JavaScript.</noscript>
    </section>
  </main>
</div>
<script>
(function(){
  'use strict';
  var root=document.getElementById('workspace'),module=root.getAttribute('data-module'),api=root.getAttribute('data-api'),csrf=root.getAttribute('data-csrf');
  var status=document.getElementById('status'),journal=document.getElementById('journal'),items=document.getElementById('items');
  function td(row,value){var cell=document.createElement('td');cell.textContent=value==null?'':String(value);row.appendChild(cell);}
  function render(data){
    document.getElementById('journalCount').textContent=data.journals.length;
    document.getElementById('itemCount').textContent=data.totalItems;
    var selected=journal.value;while(journal.options.length>1)journal.remove(1);
    data.journals.forEach(function(j){var o=document.createElement('option');o.value=j.id;o.textContent=j.name+' ('+j.slug+')';journal.appendChild(o);});
    journal.value=selected;items.textContent='';
    if(!data.items.length){var r=document.createElement('tr'),c=document.createElement('td');c.colSpan=5;c.className='empty';c.textContent='Belum ada item yang dapat ditampilkan dalam scope ini.';r.appendChild(c);items.appendChild(r);}
    data.items.forEach(function(i){var r=document.createElement('tr');td(r,i.id);td(r,i.type);td(r,i.title);td(r,i.status);td(r,i.ownerId);items.appendChild(r);});
    status.className='status';status.textContent='Data ruang kerja dimuat. Semua query dibatasi oleh role aktif dan assignment jurnal.';
  }
  function load(){
    status.className='status';status.textContent='Memuat…';
    var url=api+'?action=workspace&module='+encodeURIComponent(module)+'&size=25';if(journal.value)url+='&journalId='+encodeURIComponent(journal.value);
    fetch(url,{credentials:'same-origin',headers:{'Accept':'application/json'}}).then(function(r){return r.json().then(function(x){if(!r.ok||!x.ok)throw new Error(x.message||'Permintaan gagal');return x;});}).then(function(x){render(x.data);}).catch(function(e){status.className='status error';status.textContent=e.message;});
  }
  document.getElementById('refresh').addEventListener('click',load);journal.addEventListener('change',load);load();
  var specs={
    masterJurnal:[['createJournal','Tambah jurnal',[['tenant','Tenant'],['title','Nama jurnal'],['slug','Slug'],['locale','Locale','id_ID']]],['generateDemoData','Generate 500 jurnal demo × 100+ artikel',[['journalCount','Jumlah jurnal (tepat 500)','500'],['articlesPerJournal','Artikel per jurnal (100-200)','100'],['authorId','ID dosen demo','245'],['authorName','Fallback nama penulis','Prof. Dr. ASROFI RIDHO S.AG., M.SI., M.H, M.Pd, M.Psi'],['idempotencyKey','Idempotency key (8-40 karakter)','demo-500x100'],['confirmation','Ketik GENERATE-DEMO-500X100']]],['updateProfiles','Simpan profil',[['collectionId','Collection ID'],['metadataProfile','Metadata profile JSON','textarea'],['workflowProfile','Workflow profile JSON','textarea'],['accessPolicy','Access policy JSON','textarea']]]],
    submission:[['createDraft','Buat draft',[['collectionId','Collection ID'],['title','Judul'],['abstract','Abstrak','textarea'],['language','Bahasa','id']]],['transition','Ubah status',[['itemId','Item ID'],['version','Lock version'],['target','Status tujuan'],['workflowAction','Aksi'],['comment','Catatan','textarea']]],['addExternalContributor','Tambah kontributor',[['itemId','Item ID'],['displayName','Nama'],['email','Email'],['orcid','ORCID'],['affiliation','Afiliasi'],['rorId','ROR'],['role','Peran','AUTHOR'],['sequence','Urutan','0'],['corresponding','Korespondensi','false']]]],
    penggunaPeran:[['issueInvitation','Undang pengguna',[['journalId','Jurnal ID'],['email','Email'],['role','Peran','REVIEWER'],['scopeType','Scope','JOURNAL'],['scopeKey','Scope key'],['ttlMillis','Masa berlaku (ms)','604800000']]],['revokeInvitation','Cabut undangan',[['invitationId','Invitation ID']]],['assignStage','Buat penugasan',[['journalId','Jurnal ID'],['itemId','Item ID'],['userId','User ID'],['role','Peran','EDITOR'],['stage','Tahap','SUBMISSION'],['section','Section'],['startsAt','Mulai (yyyy-MM-dd)'],['endsAt','Selesai (yyyy-MM-dd)'],['provenanceJson','Provenance JSON','textarea']]]],
    penugasanEditor:[['assignStage','Tugaskan editor',[['journalId','Jurnal ID'],['itemId','Item ID'],['userId','User ID'],['role','Peran','EDITOR'],['stage','Tahap','SUBMISSION']]],['endStage','Akhiri penugasan',[['assignmentId','Assignment ID'],['endedAt','Tanggal akhir']]]],
    prosesReview:[['inviteReviewer','Undang reviewer',[['itemId','Item ID'],['reviewerId','Reviewer ID'],['round','Putaran','1'],['anonymity','Anonimitas','DOUBLE_ANONYMOUS'],['responseDue','Batas respons'],['reviewDue','Batas review'],['formVersion','Versi form']]],['respondReview','Respons undangan',[['assignmentId','Assignment ID'],['accept','Terima','true'],['reason','Alasan']]],['submitReview','Kirim review',[['assignmentId','Assignment ID'],['responseJson','Jawaban JSON','textarea'],['recommendation','Rekomendasi']]],['createDiscussion','Buat diskusi',[['journalId','Jurnal ID'],['itemId','Item ID'],['stage','Tahap','REVIEW'],['title','Judul'],['description','Deskripsi','textarea'],['visibility','Visibilitas','ALL_PARTICIPANTS'],['anonymity','Anonimitas','DOUBLE_ANONYMOUS']]],['commentDiscussion','Komentar',[['discussionId','Diskusi ID'],['subject','Subjek'],['body','Isi','textarea']]]],
    copyediting:[['assignStage','Tugaskan copyeditor',[['journalId','Jurnal ID'],['itemId','Item ID'],['userId','User ID'],['role','Peran','COPYEDITOR'],['stage','Tahap','COPYEDITING']]],['transition','Lanjutkan workflow',[['itemId','Item ID'],['version','Lock version'],['target','Status tujuan','PRODUCTION'],['workflowAction','Aksi','COPYEDIT_COMPLETE'],['comment','Catatan']]]],
    produksiGalley:[['assignStage','Tugaskan produksi/proof',[['journalId','Jurnal ID'],['itemId','Item ID'],['userId','User ID'],['role','Peran','PRODUCTION'],['stage','Tahap','PRODUCTION']]],['transition','Lanjutkan workflow',[['itemId','Item ID'],['version','Lock version'],['target','Status tujuan','PROOF'],['workflowAction','Aksi'],['comment','Catatan']]]],
    edisiDaftarIsi:[['createIssue','Buat edisi',[['collectionId','Collection ID'],['title','Judul edisi'],['volume','Volume'],['number','Nomor'],['year','Tahun'],['scheduledAt','Jadwal']]],['placeArticle','Tempatkan artikel',[['issueId','Issue ID'],['articleId','Article ID'],['sortOrder','Urutan','0']]]],
    publikasi:[['publishIssue','Terbitkan edisi',[['issueId','Issue ID'],['publishAt','Tanggal terbit']]],['transition','Jadwalkan artikel',[['itemId','Item ID'],['version','Lock version'],['target','Status','SCHEDULED'],['workflowAction','Aksi','SCHEDULE']]]],
    identifier:[['assignDoi','Tetapkan DOI',[['itemId','Item ID'],['doi','DOI']]],['assignUrn','Tetapkan URN',[['itemId','Item ID'],['urn','URN']]],['markDoiDeposit','Catat hasil deposit',[['itemId','Item ID'],['success','Berhasil','true']]]],
    emailNotifikasi:[['seedEmailDefaults','Pasang 73×2 template',[['journalId','Jurnal ID'],['tenant','Tenant']]]],
    langganan:[['activateSubscription','Aktifkan langganan',[['journalId','Jurnal ID'],['collectionId','Collection ID'],['policyKey','Policy key'],['userId','User ID'],['institutionType','Jenis institusi'],['institutionId','Institution ID'],['startsAt','Mulai'],['endsAt','Selesai'],['paymentId','Payment ID']]],['addIpRange','Tambah rentang IP',[['subscriptionId','Subscription ID'],['startAddress','Alamat awal'],['endAddress','Alamat akhir'],['label','Label']]]],
    pembayaran:[['preparePayment','Siapkan pembayaran',[['subscriptionId','Subscription ID'],['externalReference','Referensi']]],['settlePayment','Settlement',[['subscriptionId','Subscription ID'],['externalReference','Referensi'],['amount','Jumlah'],['currency','Mata uang','IDR'],['provider','Provider'],['providerReference','Referensi provider']]],['failPayment','Tandai gagal',[['subscriptionId','Subscription ID'],['reason','Alasan']]]],
    statistik:[['aggregateUsage','Bangun agregat',[['journalId','Jurnal ID'],['from','Dari'],['to','Sampai']]]],
    pluginIntegrasi:[['beginIntegration','Mulai attempt',[['itemId','Item ID'],['service','Service'],['integrationAction','Aksi'],['requestId','Request ID'],['payload','Payload aman','textarea']]],['finishIntegration','Selesaikan attempt',[['eventId','Event ID'],['success','Berhasil','true'],['response','Respons aman','textarea'],['error','Error']]],['retryIntegration','Ulang attempt',[['eventId','Event ID'],['requestId','Request ID baru']]]],
    importOjs:[['preflightOjs','Periksa sumber',[['connectionReference','Connection reference']]],['registerOjsSource','Daftarkan sumber',[['journalId','Jurnal ID'],['tenant','Tenant'],['sourceKey','Source key'],['displayName','Nama sumber'],['connectionReference','Connection reference']]],['startImportDryRun','Dry-run',[['sourceId','Source ID'],['idempotencyKey','Idempotency key'],['batchSize','Batch size','250']]],['startImportExecute','Eksekusi import',[['sourceId','Source ID'],['idempotencyKey','Idempotency key'],['batchSize','Batch size','250']]],['resumeImport','Lanjutkan job',[['jobId','Job ID'],['batchSize','Batch size','250']]],['finalizeImport','Rekonsiliasi final',[['jobId','Job ID']]],['cancelImport','Batalkan import',[['jobId','Job ID']]]]
  };
  specs.journals=specs.masterJurnal;specs.submissions=specs.submission;specs.people=specs.penggunaPeran;
  specs['editor-assignments']=specs.penugasanEditor;specs['review-assignments']=specs.prosesReview;
  specs.production=specs.produksiGalley;specs.issues=specs.edisiDaftarIsi;specs.publications=specs.publikasi;
  specs.identifiers=specs.identifier;specs.communications=specs.emailNotifikasi;specs.subscriptions=specs.langganan;
  specs.payments=specs.pembayaran;specs.statistics=specs.statistik;specs.integrations=specs.pluginIntegrasi;specs['import-ojs']=specs.importOjs;
  var operations=document.getElementById('operation'),commandFields=document.getElementById('commandFields'),execute=document.getElementById('execute'),commandResult=document.getElementById('commandResult'),active=[];
  (specs[module]||[]).forEach(function(s){var o=document.createElement('option');o.value=s[0];o.textContent=s[1];operations.appendChild(o);});
  operations.addEventListener('change',function(){commandFields.textContent='';active=[];var list=specs[module]||[],spec=null;list.forEach(function(x){if(x[0]===operations.value)spec=x;});execute.hidden=!spec;if(!spec)return;active=spec[2];active.forEach(function(f){var wrap=document.createElement('label');wrap.className='field';var text=document.createElement('span');text.textContent=f[1];var input=f[2]==='textarea'?document.createElement('textarea'):document.createElement('input');input.name=f[0];if(f[2]&&f[2]!=='textarea')input.value=f[2];wrap.appendChild(text);wrap.appendChild(input);commandFields.appendChild(wrap);});});
  document.getElementById('commandForm').addEventListener('submit',function(ev){ev.preventDefault();if(!operations.value)return;var body=new URLSearchParams();body.set('action',operations.value);body.set('nui_csrf',csrf);active.forEach(function(f){var el=commandFields.querySelector('[name="'+f[0]+'"]');if(el&&el.value!=='')body.set(f[0],el.value);});commandResult.hidden=false;commandResult.className='status result';commandResult.textContent='Memproses…';fetch(api,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','Accept':'application/json'},body:body.toString()}).then(function(r){return r.json().then(function(x){if(!r.ok||!x.ok)throw new Error(x.message||'Perintah gagal');return x;});}).then(function(x){commandResult.textContent=JSON.stringify(x,null,2);load();}).catch(function(e){commandResult.className='status result error';commandResult.textContent=e.message;});});
}());
</script>
</body>
</html>
