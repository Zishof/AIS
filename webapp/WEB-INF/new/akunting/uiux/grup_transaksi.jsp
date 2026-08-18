<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.security.SecureRandom" %>
<%@ page import="ais.common.newui.NewUiPermission" %>
<%@ page import="ais.common.newui.NewUiRouteGuard" %>
<%!
private String h(Object v){return v==null?"":String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String token(){byte[]b=new byte[24];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();for(int i=0;i<b.length;i++)s.append(Integer.toHexString((b[i]&255)|256).substring(1));return s.toString();}
%>
<%
String csrf=(String)session.getAttribute("newUiCsrfToken");
if(csrf==null){csrf=token();session.setAttribute("newUiCsrfToken",csrf);}
String journalPage=request.getAttribute("nuiJournalPage")==null?"grup_transaksi":String.valueOf(request.getAttribute("nuiJournalPage"));
NewUiPermission p=NewUiRouteGuard.permissionFor(request,"akunting",journalPage);
String menu=request.getParameter("menuId");
String endpoint=request.getContextPath()+"/new?service=1&module=akunting&page="+journalPage+("semua_grup_transaksi".equals(journalPage)?"&semua_jurnal=true":"")+(menu==null?"":"&menuId="+h(menu));
%>
  <style>
    .jr-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(150px, 1fr));
      gap: 10px;
    }
    .jr-lines input,
    .jr-lines select {
      min-width: 120px;
    }
    .jr-status {
      display: inline-flex;
      padding: 3px 9px;
      border-radius: 99px;
      font-size: 12px;
      font-weight: 700;
    }
    .jr-draft {
      background: #fff4d6;
      color: #8a5600;
    }
    .jr-posted {
      background: #dcfce7;
      color: #166534;
    }
    .jr-closing {
      background: #e2e8f0;
      color: #334155;
    }
    .jr-unbalanced {
      color: #b91c1c;
      font-weight: 700;
    }
    .jr-modal {
      border: 0;
      border-radius: 18px;
      padding: 0;
      max-width: 1100px;
      width: calc(100% - 32px);
      max-height: 92vh;
    }
    .jr-modal::backdrop {
      background: rgba(15, 23, 42, 0.55);
    }
    .jr-modal-body {
      padding: 22px;
      overflow: auto;
      max-height: 86vh;
    }
    @media (max-width: 900px) {
      .jr-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .jr-grid {
        grid-template-columns: 1fr;
      }
    }
  </style>
  <section
    class="nui-page"
    id="journalRoot"
    data-endpoint="<%=h(endpoint)%>"
    data-csrf="<%=h(csrf)%>"
    data-create="<%=p!=null&&p.isCanCreate()%>"
    data-update="<%=p!=null&&p.isCanUpdate()%>"
    data-delete="<%=p!=null&&p.isCanDelete()%>"
    data-approve="<%=p!=null&&p.isCanApprove()%>"
  >
    <header class="nui-module-dashboard-hero">
      <div>
        <div class="nui-breadcrumb">Akuntansi / Jurnal Umum</div>
        <p class="nui-dashboard-eyebrow">
          <i class="fa-solid fa-book"></i> GENERAL LEDGER
        </p>
        <h1>Grup Transaksi</h1>
        <p>
          Jurnal debet/kredit, posting, closing guard, bukti transaksi,
          sinkronisasi total, impor, ekspor, dan audit.
        </p>
      </div>
      <div class="nui-toolbar">
        <button id="jrImport" class="nui-btn nui-btn-hero">
          <i class="fa-solid fa-file-import"></i> Impor</button
        ><a id="jrExport" class="nui-btn nui-btn-hero"
          ><i class="fa-solid fa-file-export"></i> Ekspor</a
        ><button id="jrAdd" class="nui-btn nui-btn-hero">
          <i class="fa-solid fa-plus"></i> Tambah Jurnal
        </button>
      </div>
    </header>
    <article class="nui-card" style="margin-top: 14px">
      <div class="jr-grid">
        <input
          id="jrQuery"
          class="nui-input"
          type="search"
          placeholder="Nomor atau keterangan jurnal"
        /><input
          id="jrLineQuery"
          class="nui-input"
          type="search"
          placeholder="Keterangan detail transaksi"
        /><label
          >Mulai <input id="jrStart" class="nui-input" type="date" /></label
        ><label>s.d. <input id="jrEnd" class="nui-input" type="date" /></label
        ><select id="jrType" class="nui-input">
          <option value="">Semua jenis transaksi</option></select
        ><select id="jrJournalType" class="nui-input">
          <option value="">Semua jenis jurnal</option>
          <option>Umum</option>
          <option>Kas Masuk</option>
          <option>Kas Keluar</option>
          <option>Transaksi</option></select
        ><select id="jrStatus" class="nui-input">
          <option value="">Semua status</option>
          <option value="false">Draft</option>
          <option value="true">Sudah diposting</option>
          <option value="active">Belum terposting aktif</option></select
        ><input
          id="jrPostingId"
          class="nui-input"
          type="number"
          placeholder="No. bukti posting"
        /><select id="jrWorkUnit" class="nui-input">
          <option value="">Semua satuan kerja</option></select
        ><select id="jrWorkspace" class="nui-input">
          <option value="">Semua workspace</option></select
        ><input
          id="jrMin"
          class="nui-input"
          type="number"
          min="0"
          placeholder="Nominal minimum"
        /><input
          id="jrMax"
          class="nui-input"
          type="number"
          min="0"
          placeholder="Nominal maksimum"
        />
      </div>
      <details style="margin-top: 10px">
        <summary>Filter akun dan kondisi khusus</summary>
        <div class="nui-toolbar" style="margin-top: 8px">
          <select
            id="jrAccounts"
            class="nui-input"
            multiple
            size="7"
            style="min-width: 360px; flex: 1"
          ></select
          ><label
            ><input id="jrUnbalanced" type="checkbox" /> Hanya tidak
            balance</label
          ><button id="jrRefresh" class="nui-btn">
            <i class="fa-solid fa-magnifying-glass"></i> Terapkan
          </button>
        </div>
      </details>
    </article>
    <div class="nui-dashboard-kpis" style="margin-top: 14px">
      <article class="nui-card">
        <small>Jurnal dalam filter</small><strong id="jrTotal">0</strong>
      </article>
      <article class="nui-card">
        <small>Total debet</small><strong id="jrDebit">Rp0</strong>
      </article>
      <article class="nui-card">
        <small>Total kredit</small><strong id="jrCredit">Rp0</strong>
      </article>
      <article class="nui-card">
        <small>Selisih</small><strong id="jrDifference">Rp0</strong>
      </article>
    </div>
    <article class="nui-card" style="margin-top: 14px">
      <div class="nui-toolbar">
        <button id="jrSync" class="nui-btn">
          <i class="fa-solid fa-arrows-rotate"></i> Sinkronkan Total</button
        ><button id="jrPostBatch" class="nui-btn">
          <i class="fa-solid fa-circle-check"></i> Posting Filter</button
        ><button id="jrActivateBatch" class="nui-btn">
          <i class="fa-solid fa-toggle-on"></i> Aktifkan Bukti</button
        ><button id="jrUnpostBatch" class="nui-btn">
          <i class="fa-solid fa-rotate-left"></i> Batalkan Posting Filter</button
        ><button id="jrCombined" class="nui-btn">
          <i class="fa-solid fa-print"></i> Cetak Gabungan</button
        ><button id="jrCleanDuplicates" class="nui-btn" hidden>
          <i class="fa-solid fa-broom"></i> Bersihkan Duplikat</button
        ><button id="jrDeleteAll" class="nui-btn" hidden>
          <i class="fa-solid fa-triangle-exclamation"></i> Hapus Semua</button
        ><span id="jrBatchInfo"></span>
      </div>
      <div class="nui-table-wrap">
        <table class="nui-table">
          <thead>
            <tr>
              <th>Jurnal</th>
              <th>Tanggal / jenis</th>
              <th>Satuan kerja / workspace</th>
              <th>Keterangan</th>
              <th>Debet</th>
              <th>Kredit</th>
              <th>Status</th>
              <th>Aksi</th>
            </tr>
          </thead>
          <tbody id="jrRows">
            <tr>
              <td colspan="8">Memuat data...</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="nui-toolbar">
        <button id="jrPrev" class="nui-btn">← Sebelumnya</button
        ><span id="jrPager">Halaman 1</span
        ><button id="jrNext" class="nui-btn">Berikutnya →</button>
      </div>
    </article>
    <dialog id="jrFormDialog" class="jr-modal">
      <form id="jrForm" class="jr-modal-body">
        <div class="nui-toolbar">
          <h2 id="jrFormTitle" style="flex: 1">Jurnal Umum</h2>
          <button type="button" class="nui-btn jr-close">Tutup</button>
        </div>
        <input id="jfId" type="hidden" />
        <div class="jr-grid">
          <label
            >Nomor jurnal *<input
              id="jfCode"
              class="nui-input"
              required /></label
          ><label
            >Tanggal *<input
              id="jfDate"
              class="nui-input"
              type="date"
              required /></label
          ><label
            >Jenis transaksi *<select id="jfType" class="nui-input" required>
              <option value="">Pilih jenis</option>
            </select></label
          ><label
            >Jenis jurnal<select id="jfJournalType" class="nui-input">
              <option>Umum</option>
              <option>Kas Masuk</option>
              <option>Kas Keluar</option>
              <option>Transaksi</option>
            </select></label
          ><label
            >Satuan kerja<select id="jfWorkUnit" class="nui-input">
              <option value="">Pilih bila ada</option>
            </select></label
          ><label
            >Workspace<select id="jfWorkspace" class="nui-input">
              <option value="">Pilih bila ada</option>
            </select></label
          ><label>No. tagihan<input id="jfInvoice" class="nui-input" /></label
          ><label
            >Dibayarkan kepada<input id="jfPayee" class="nui-input"
          /></label>
        </div>
        <label
          >Keterangan jurnal<textarea
            id="jfDescription"
            class="nui-input"
            rows="2"
          ></textarea>
        </label>
        <div class="nui-toolbar">
          <h3 style="flex: 1">Detail Debet / Kredit</h3>
          <button id="jfAddLine" type="button" class="nui-btn">
            <i class="fa-solid fa-plus"></i> Tambah Baris
          </button>
        </div>
        <div class="nui-table-wrap">
          <table class="nui-table jr-lines">
            <thead>
              <tr>
                <th>Akun *</th>
                <th>Keterangan *</th>
                <th>Debet</th>
                <th>Kredit</th>
                <th></th>
              </tr>
            </thead>
            <tbody id="jfLines"></tbody>
            <tfoot>
              <tr>
                <th colspan="2">Total / selisih</th>
                <th id="jfDebit">Rp0</th>
                <th id="jfCredit">Rp0</th>
                <th id="jfDiff">Rp0</th>
              </tr>
            </tfoot>
          </table>
        </div>
        <p id="jfError" class="jr-unbalanced"></p>
        <div class="nui-toolbar">
          <button type="button" class="nui-btn jr-close">Batal</button
          ><button id="jfSave" class="nui-btn nui-btn-primary">
            <i class="fa-solid fa-floppy-disk"></i> Simpan Jurnal
          </button>
        </div>
      </form>
    </dialog>
    <dialog id="jrDetailDialog" class="jr-modal">
      <div class="jr-modal-body">
        <div class="nui-toolbar">
          <h2 id="jdTitle" style="flex: 1">Detail Jurnal</h2>
          <button class="nui-btn jr-close">Tutup</button>
        </div>
        <div id="jdMeta"></div>
        <div class="nui-table-wrap">
          <table class="nui-table">
            <thead>
              <tr>
                <th>Akun</th>
                <th>Keterangan</th>
                <th>Debet</th>
                <th>Kredit</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody id="jdLines"></tbody>
          </table>
        </div>
      </div>
    </dialog>
    <dialog id="jrHistoryDialog" class="jr-modal">
      <div class="jr-modal-body">
        <div class="nui-toolbar">
          <h2 style="flex: 1">History Perubahan Jurnal</h2>
          <button class="nui-btn jr-close">Tutup</button>
        </div>
        <div class="nui-table-wrap">
          <table class="nui-table">
            <thead>
              <tr>
                <th>Revisi</th>
                <th>Tanggal</th>
                <th>Jenis</th>
                <th>Nomor</th>
                <th>Keterangan</th>
                <th>Debet</th>
                <th>Kredit</th>
                <th>Bukti</th>
              </tr>
            </thead>
            <tbody id="jhRows"></tbody>
          </table>
        </div>
      </div>
    </dialog>
    <dialog id="jrImportDialog" class="jr-modal">
      <div class="jr-modal-body">
        <div class="nui-toolbar">
          <h2 style="flex: 1">Impor Jurnal CSV</h2>
          <button class="nui-btn jr-close">Tutup</button>
        </div>
        <p>
          Kolom:
          <code
            >kode,tanggal,jenis_transaksi_id,keterangan,akun_id,keterangan_detail,debet,kredit</code
          >. Baris dengan kode sama digabung menjadi satu jurnal dan tetap
          divalidasi balance.
        </p>
        <input
          id="jiFile"
          type="file"
          accept=".csv,text/csv"
          class="nui-input"
        /><textarea
          id="jiCsv"
          class="nui-input"
          rows="14"
          placeholder="Tempel data CSV atau pilih file..."
        ></textarea>
        <div class="nui-toolbar">
          <button id="jiRun" class="nui-btn nui-btn-primary">
            <i class="fa-solid fa-file-import"></i> Validasi & Impor
          </button>
        </div>
      </div>
    </dialog>
  </section>
  <script>
    (function () {
      "use strict";
      var root = document.getElementById("journalRoot"),
        endpoint = root.dataset.endpoint,
        csrf = root.dataset.csrf,
        canCreate = root.dataset.create === "true",
        canUpdate = root.dataset.update === "true",
        canDelete = root.dataset.delete === "true",
        canApprove = root.dataset.approve === "true",
        page = 0,
        size = 20,
        total = 0,
        accounts = [],
        types = [],
        units = [],
        workspaces = [],
        money = new Intl.NumberFormat("id-ID", {
          style: "currency",
          currency: "IDR",
          maximumFractionDigits: 2,
        });
      function el(x) {
        return document.getElementById(x);
      }
      function esc(v) {
        return String(v == null ? "" : v).replace(/[&<>"']/g, function (c) {
          return {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
          }[c];
        });
      }
      function selected(s) {
        return Array.prototype.filter
          .call(s.options, function (x) {
            return x.selected;
          })
          .map(function (x) {
            return x.value;
          })
          .join(",");
      }
      function filter(p, z) {
        return {
          q: el("jrQuery").value,
          lineQuery: el("jrLineQuery").value,
          start: el("jrStart").value,
          end: el("jrEnd").value,
          typeId: el("jrType").value,
          journalType: el("jrJournalType").value,
          posted: el("jrStatus").value === "active" ? "" : el("jrStatus").value,
          unpostedActive: el("jrStatus").value === "active",
          postingId: el("jrPostingId").value,
          workUnitId: el("jrWorkUnit").value,
          workspaceId: el("jrWorkspace").value,
          min: el("jrMin").value,
          max: el("jrMax").value,
          accountIds: selected(el("jrAccounts")),
          unbalanced: el("jrUnbalanced").checked,
          page: p == null ? page : p,
          size: z || size,
        };
      }
      async function call(action, body, query, id) {
        var u =
            endpoint +
            "&action=" +
            action +
            (id ? "&id=" + encodeURIComponent(id) : "") +
            "&" +
            new URLSearchParams(query || {}),
          o = { credentials: "same-origin" };
        if (body) {
          o.method = "POST";
          o.headers = {
            "X-CSRF-Token": csrf,
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          };
          o.body = new URLSearchParams(body);
        }
        var r = await fetch(u, o),
          j = await r.json();
        if (!r.ok) throw new Error(j.message || "Operasi gagal");
        return j;
      }
      function fill(s, a, first) {
        s.innerHTML = first || "";
        a.forEach(function (x) {
          var o = document.createElement("option");
          o.value = x.id;
          o.textContent = x.label;
          if (x.parentId != null) o.dataset.parent = x.parentId;
          s.appendChild(o);
        });
      }
      async function options() {
        var j = await call("options");
        accounts = j.accounts;
        types = j.types;
        units = j.workUnits;
        workspaces = j.workspaces;
        fill(el("jrAccounts"), accounts);
        fill(
          el("jrType"),
          types,
          '<option value="">Semua jenis transaksi</option>',
        );
        fill(el("jfType"), types, '<option value="">Pilih jenis</option>');
        fill(
          el("jrWorkUnit"),
          units,
          '<option value="">Semua satuan kerja</option>',
        );
        fill(
          el("jfWorkUnit"),
          units,
          '<option value="">Pilih bila ada</option>',
        );
        fill(
          el("jrWorkspace"),
          workspaces,
          '<option value="">Semua workspace</option>',
        );
        fill(
          el("jfWorkspace"),
          workspaces,
          '<option value="">Pilih bila ada</option>',
        );
        if (j.lastClosing) el("jfDate").min = j.lastClosing;
        el("jrCombined").hidden = !j.admin;
        el("jrCleanDuplicates").hidden = !(j.admin && canDelete);
        el("jrDeleteAll").hidden = !(j.deleteAllEnabled && canDelete);
      }
      function status(x) {
        return x.closingId
          ? '<span class="jr-status jr-closing">Closing</span>'
          : x.postingId
            ? '<span class="jr-status jr-posted">Posted #' +
              x.postingId +
              (x.postingActive === false ? " (belum aktif)" : "") +
              "</span>"
            : '<span class="jr-status jr-draft">Draft</span>';
      }
      async function load() {
        el("jrRows").innerHTML = '<tr><td colspan="8">Memuat data...</td></tr>';
        try {
          var j = await call("list", null, filter());
          total = j.total;
          el("jrTotal").textContent = j.total;
          el("jrDebit").textContent = money.format(j.debit);
          el("jrCredit").textContent = money.format(j.credit);
          el("jrDifference").textContent = money.format(
            Math.abs(j.debit - j.credit),
          );
          el("jrRows").innerHTML =
            j.rows
              .map(function (x) {
                var edit = canUpdate && !x.postingId && !x.closingId,
                  del = canDelete && !x.postingId && !x.closingId,
                  post =
                    canApprove && !x.postingId && !x.closingId && x.balanced,
                  unpost = canApprove && x.postingId && !x.closingId,
                  proof = x.hasAttachment
                    ? '<a class="nui-btn" href="' +
                      endpoint +
                      "&action=attachment&id=" +
                      x.id +
                      '" title="Unduh bukti"><i class="fa-solid fa-paperclip"></i></a>'
                    : "";
                return (
                  '<tr><td><button class="nui-btn jr-detail" data-id="' +
                  x.id +
                  '"><strong>' +
                  esc(x.code) +
                  "</strong></button><br><small>" +
                  esc(x.user) +
                  "</small></td><td>" +
                  esc(x.date) +
                  "<br>" +
                  esc(x.type) +
                  " / " +
                  esc(x.journalType) +
                  "</td><td>" +
                  esc(x.workUnit) +
                  "<br><small>" +
                  esc(x.workspace) +
                  "</small></td><td>" +
                  esc(x.description) +
                  (x.invoice
                    ? "<br><small>Tagihan " + esc(x.invoice) + "</small>"
                    : "") +
                  '</td><td class="' +
                  (!x.balanced ? "jr-unbalanced" : "") +
                  '">' +
                  money.format(x.debit) +
                  '</td><td class="' +
                  (!x.balanced ? "jr-unbalanced" : "") +
                  '">' +
                  money.format(x.credit) +
                  "</td><td>" +
                  status(x) +
                  '</td><td><div class="nui-toolbar"><a class="nui-btn" target="_blank" href="' +
                  endpoint +
                  "&action=receipt&id=" +
                  x.id +
                  '" title="Cetak bukti" ' +
                  (!x.balanced ? 'aria-disabled="true"' : "") +
                  '><i class="fa-solid fa-print"></i></a>' +
                  proof +
                  (canUpdate && !x.closingId
                    ? '<label class="nui-btn" title="Unggah bukti"><i class="fa-solid fa-upload"></i><input class="jr-upload" data-id="' +
                      x.id +
                      '" type="file" hidden></label>'
                    : "") +
                  '<button class="nui-btn jr-history" data-id="' +
                  x.id +
                  '" title="History"><i class="fa-solid fa-clock-rotate-left"></i></button><button class="nui-btn jr-edit" data-id="' +
                  x.id +
                  '" ' +
                  (!edit ? "disabled" : "") +
                  ' title="Edit"><i class="fa-solid fa-pen"></i></button><button class="nui-btn jr-post" data-id="' +
                  x.id +
                  '" ' +
                  (!post ? "disabled" : "") +
                  ' title="Posting"><i class="fa-solid fa-circle-check"></i></button><button class="nui-btn jr-unpost" data-id="' +
                  x.id +
                  '" ' +
                  (!unpost ? "disabled" : "") +
                  ' title="Batalkan posting"><i class="fa-solid fa-rotate-left"></i></button><button class="nui-btn jr-delete" data-id="' +
                  x.id +
                  '" ' +
                  (!del ? "disabled" : "") +
                  ' title="Hapus"><i class="fa-solid fa-trash"></i></button></div></td></tr>'
                );
              })
              .join("") ||
            '<tr><td colspan="8">Tidak ada jurnal pada filter ini.</td></tr>';
          el("jrPager").textContent =
            "Halaman " +
            (page + 1) +
            " dari " +
            Math.max(1, Math.ceil(total / size));
          el("jrPrev").disabled = page === 0;
          el("jrNext").disabled = (page + 1) * size >= total;
          el("jrExport").href =
            endpoint + "&action=export&" + new URLSearchParams(filter(0, 100));
        } catch (e) {
          el("jrRows").innerHTML =
            '<tr><td colspan="8" class="jr-unbalanced">' +
            esc(e.message) +
            "</td></tr>";
        }
      }
      function accountOptions(id) {
        return (
          '<option value="">Pilih akun</option>' +
          accounts
            .map(function (x) {
              return (
                '<option value="' +
                x.id +
                '" ' +
                (String(x.id) === String(id) ? "selected" : "") +
                ">" +
                esc(x.label) +
                "</option>"
              );
            })
            .join("")
        );
      }
      function addLine(x) {
        x = x || {};
        var tr = document.createElement("tr");
        tr.dataset.id = x.id || "";
        tr.innerHTML =
          '<td><select class="nui-input jl-account">' +
          accountOptions(x.accountId) +
          '</select></td><td><input class="nui-input jl-description" value="' +
          esc(x.description) +
          '"></td><td><input class="nui-input jl-debit" type="number" min="0" step="0.01" value="' +
          (x.debit || 0) +
          '"></td><td><input class="nui-input jl-credit" type="number" min="0" step="0.01" value="' +
          (x.credit || 0) +
          '"></td><td><button type="button" class="nui-btn jl-remove"><i class="fa-solid fa-trash"></i></button></td>';
        el("jfLines").appendChild(tr);
        tr.oninput = totals;
        tr.querySelector(".jl-remove").onclick = function () {
          tr.remove();
          totals();
        };
        totals();
      }
      function totals() {
        var d = 0,
          c = 0;
        Array.prototype.forEach.call(el("jfLines").rows, function (r) {
          d += Number(r.querySelector(".jl-debit").value) || 0;
          c += Number(r.querySelector(".jl-credit").value) || 0;
        });
        el("jfDebit").textContent = money.format(d);
        el("jfCredit").textContent = money.format(c);
        el("jfDiff").textContent = money.format(Math.abs(d - c));
        el("jfError").textContent =
          d > 0 && Math.abs(d - c) < 0.005
            ? "Jurnal balance."
            : "Total debet dan kredit wajib sama dan lebih dari nol.";
      }
      function blank() {
        el("jfId").value = "";
        el("jfCode").value = "";
        el("jfDate").value = new Date().toISOString().slice(0, 10);
        el("jfType").value = "";
        el("jfJournalType").value = "Umum";
        el("jfWorkUnit").value = "";
        el("jfWorkspace").value = "";
        el("jfInvoice").value = "";
        el("jfPayee").value = "";
        el("jfDescription").value = "";
        el("jfLines").innerHTML = "";
        addLine();
        addLine();
        el("jrFormTitle").textContent = "Tambah Jurnal Umum";
        el("jrFormDialog").showModal();
      }
      async function edit(id) {
        var j = await call("detail", null, {}, id),
          x = j.row;
        el("jfId").value = x.id;
        el("jfCode").value = x.code || "";
        el("jfDate").value = x.date || "";
        el("jfType").value = x.typeId || "";
        el("jfJournalType").value = x.journalType || "Umum";
        el("jfWorkUnit").value = x.workUnitId || "";
        el("jfWorkspace").value = x.workspaceId || "";
        el("jfInvoice").value = x.invoice || "";
        el("jfPayee").value = x.payee || "";
        el("jfDescription").value = x.description || "";
        el("jfLines").innerHTML = "";
        x.lines.forEach(addLine);
        el("jrFormTitle").textContent = "Edit Jurnal " + x.code;
        el("jrFormDialog").showModal();
      }
      function payload() {
        return {
          id: el("jfId").value || null,
          code: el("jfCode").value,
          date: el("jfDate").value,
          typeId: el("jfType").value || null,
          journalType: el("jfJournalType").value,
          workUnitId: el("jfWorkUnit").value || null,
          workspaceId: el("jfWorkspace").value || null,
          invoice: el("jfInvoice").value,
          payee: el("jfPayee").value,
          description: el("jfDescription").value,
          lines: Array.prototype.map.call(el("jfLines").rows, function (r) {
            return {
              id: r.dataset.id || null,
              accountId: r.querySelector(".jl-account").value || null,
              description: r.querySelector(".jl-description").value,
              debit: Number(r.querySelector(".jl-debit").value) || 0,
              credit: Number(r.querySelector(".jl-credit").value) || 0,
            };
          }),
        };
      }
      async function detail(id) {
        var x = (await call("detail", null, {}, id)).row;
        el("jdTitle").textContent = x.code + " — " + (x.description || "");
        el("jdMeta").innerHTML =
          "<p><strong>" +
          esc(x.date) +
          "</strong> · " +
          esc(x.type) +
          " · " +
          status(x) +
          "</p><p>" +
          esc(x.workUnit) +
          " " +
          esc(x.workspace) +
          "</p>";
        el("jdLines").innerHTML = x.lines
          .map(function (l) {
            return (
              "<tr><td>" +
              esc(l.accountCode) +
              " — " +
              esc(l.account) +
              "</td><td>" +
              esc(l.description) +
              "</td><td>" +
              money.format(l.debit) +
              "</td><td>" +
              money.format(l.credit) +
              "</td><td>" +
              (l.status === 1 ? "Posted" : "Draft") +
              "</td></tr>"
            );
          })
          .join("");
        el("jrDetailDialog").showModal();
      }
      async function history(id) {
        el("jhRows").innerHTML = '<tr><td colspan="8">Memuat...</td></tr>';
        el("jrHistoryDialog").showModal();
        try {
          var j = await call("history", null, {}, id);
          el("jhRows").innerHTML =
            j.rows
              .map(function (x) {
                return (
                  "<tr><td>" +
                  x.revision +
                  "</td><td>" +
                  esc(x.date) +
                  "</td><td>" +
                  esc(x.type) +
                  "</td><td>" +
                  esc(x.code) +
                  "</td><td>" +
                  esc(x.description) +
                  "</td><td>" +
                  money.format(x.debit) +
                  "</td><td>" +
                  money.format(x.credit) +
                  "</td><td>" +
                  esc(x.postingId) +
                  "</td></tr>"
                );
              })
              .join("") || '<tr><td colspan="8">Belum ada history.</td></tr>';
        } catch (e) {
          el("jhRows").innerHTML =
            '<tr><td colspan="8">' + esc(e.message) + "</td></tr>";
        }
      }
      async function mutate(action, id, body, question) {
        if (question && !confirm(question)) return;
        try {
          await call(action, body || {}, filter(), id);
          await load();
        } catch (e) {
          alert(e.message);
        }
      }
      el("jrRows").onclick = function (e) {
        var b = e.target.closest("button");
        if (!b || b.disabled) return;
        var id = b.dataset.id;
        if (b.classList.contains("jr-detail")) detail(id);
        else if (b.classList.contains("jr-history")) history(id);
        else if (b.classList.contains("jr-edit")) edit(id);
        else if (b.classList.contains("jr-delete"))
          mutate(
            "delete",
            id,
            { id: id },
            "Hapus jurnal draft beserta seluruh detailnya?",
          );
        else if (b.classList.contains("jr-post"))
          mutate(
            "post",
            id,
            {
              postingDate: new Date().toISOString().slice(0, 10),
              note: "Posting manual dari New UI",
            },
            "Posting jurnal ini?",
          );
        else if (b.classList.contains("jr-unpost"))
          mutate("unpost", id, { id: id }, "Batalkan posting jurnal ini?");
      };
      el("jrRows").onchange = async function (e) {
        if (!e.target.classList.contains("jr-upload") || !e.target.files[0])
          return;
        var d = new FormData();
        d.append("id", e.target.dataset.id);
        d.append("file", e.target.files[0]);
        try {
          var r = await fetch(endpoint + "&action=upload", {
              method: "POST",
              credentials: "same-origin",
              headers: { "X-CSRF-Token": csrf },
              body: d,
            }),
            j = await r.json();
          if (!r.ok) throw new Error(j.message || "Unggah gagal");
          load();
        } catch (x) {
          alert(x.message);
        }
      };
      el("jrForm").onsubmit = async function (e) {
        e.preventDefault();
        try {
          await call("save", { payload: JSON.stringify(payload()) });
          el("jrFormDialog").close();
          load();
        } catch (x) {
          el("jfError").textContent = x.message;
        }
      };
      el("jfAddLine").onclick = function () {
        addLine();
      };
      el("jrAdd").onclick = blank;
      el("jrAdd").hidden = !canCreate;
      el("jrImport").onclick = function () {
        el("jrImportDialog").showModal();
      };
      el("jrImport").hidden = !canUpdate;
      el("jiFile").onchange = async function () {
        if (this.files[0]) el("jiCsv").value = await this.files[0].text();
      };
      el("jiRun").onclick = async function () {
        try {
          var j = await call("import", { csv: el("jiCsv").value });
          alert(
            j.success +
              " jurnal berhasil. " +
              j.errors.length +
              " gagal.\n" +
              j.errors.join("\n"),
          );
          el("jrImportDialog").close();
          load();
        } catch (e) {
          alert(e.message);
        }
      };
      Array.prototype.forEach.call(
        document.querySelectorAll(".jr-close"),
        function (b) {
          b.onclick = function () {
            b.closest("dialog").close();
          };
        },
      );
      function batch(action, question, extra) {
        if (!confirm(question)) return;
        var body = extra || {};
        call(action, body, filter())
          .then(function (j) {
            el("jrBatchInfo").textContent =
              j.success + " berhasil; " + j.errors.length + " gagal";
            if (j.errors.length) alert(j.errors.join("\n"));
            load();
          })
          .catch(function (e) {
            alert(e.message);
          });
      }
      el("jrSync").onclick = function () {
        batch(
          "sync",
          "Sinkronkan total header dari seluruh detail dalam filter?",
        );
      };
      el("jrPostBatch").onclick = function () {
        batch(
          "postBatch",
          "Posting semua jurnal draft yang balance dalam filter?",
          {
            postingDate: new Date().toISOString().slice(0, 10),
            note: "Posting massal dari New UI",
          },
        );
      };
      el("jrActivateBatch").onclick = function () {
        batch("activateBatch", "Aktifkan semua bukti posting dalam filter?");
      };
      el("jrUnpostBatch").onclick = function () {
        batch("unpostBatch", "Batalkan posting SEMUA jurnal dalam filter?");
      };
      el("jrCombined").onclick = function () {
        window.open(
          endpoint + "&action=combinedReceipt&" + new URLSearchParams(filter()),
          "_blank",
        );
      };
      async function maintenance(action, phrase) {
        if (prompt("Ketik " + phrase + " untuk melanjutkan:") !== phrase) return;
        try {
          var j = await call(action, { confirmation: phrase });
          alert(
            "Operasi selesai. Grup: " +
              (j.groups || 0) +
              ", detail: " +
              (j.lines || 0),
          );
          load();
        } catch (e) {
          alert(e.message);
        }
      }
      el("jrCleanDuplicates").onclick = function () {
        maintenance("cleanDuplicates", "BERSIHKAN DUPLIKAT");
      };
      el("jrDeleteAll").onclick = function () {
        maintenance("deleteAll", "HAPUS SEMUA JURNAL");
      };
      [el("jrPostBatch"), el("jrActivateBatch"), el("jrUnpostBatch")].forEach(
        function (x) {
          x.hidden = !canApprove;
        },
      );
      el("jrSync").hidden = !canUpdate;
      var timer;
      [el("jrQuery"), el("jrLineQuery")].forEach(function (x) {
        x.oninput = function () {
          clearTimeout(timer);
          timer = setTimeout(function () {
            page = 0;
            load();
          }, 350);
        };
      });
      [
        "jrStart",
        "jrEnd",
        "jrType",
        "jrJournalType",
        "jrStatus",
        "jrPostingId",
        "jrWorkUnit",
        "jrWorkspace",
        "jrMin",
        "jrMax",
        "jrAccounts",
        "jrUnbalanced",
      ].forEach(function (id) {
        el(id).onchange = function () {
          page = 0;
          load();
        };
      });
      el("jrRefresh").onclick = function () {
        page = 0;
        load();
      };
      el("jrPrev").onclick = function () {
        if (page) {
          page--;
          load();
        }
      };
      el("jrNext").onclick = function () {
        if ((page + 1) * size < total) {
          page++;
          load();
        }
      };
      el("jfWorkUnit").onchange = function () {
        Array.prototype.forEach.call(
          el("jfWorkspace").options,
          function (o, i) {
            if (i)
              o.hidden =
                el("jfWorkUnit").value &&
                o.dataset.parent !== el("jfWorkUnit").value;
          },
        );
        el("jfWorkspace").value = "";
      };
      var today = new Date();
      var sixMonthsAgo = new Date(today.getFullYear(), today.getMonth() - 6, today.getDate());
      var tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
      el("jrStart").value = sixMonthsAgo.toISOString().slice(0, 10);
      el("jrEnd").value = tomorrow.toISOString().slice(0, 10);
      options()
        .then(load)
        .catch(function (e) {
          el("jrRows").innerHTML =
            '<tr><td colspan="8">' + esc(e.message) + "</td></tr>";
        });
    })();
  </script>
