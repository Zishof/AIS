<%@page import="ais.common.Common"%>
<%
    String rnd        = Common.getGeneratedBarCode(7);
    String idRepoEdit = request.getParameter("id") == null ? "" : request.getParameter("id").trim();
    boolean isModeEdit = !idRepoEdit.isEmpty();
%>

<div class="card shadow-sm mb-4 border-0">
    <div class="card-header <%=isModeEdit ? "bg-warning" : "bg-success"%> text-white d-flex align-items-center">
        <h5 class="card-title mb-0 <%=isModeEdit ? "text-dark" : ""%>">
            <i class="fas <%=isModeEdit ? "fa-edit" : "fa-cloud-upload-alt"%> me-2"></i>
            <%=isModeEdit
                ? Common.getBahasaConfig("Form Perbarui Repositori")
                : Common.getBahasaConfig("Form Pendaftaran Koleksi Repositori")%>
        </h5>
    </div>

    <div class="card-body bg-light">
        <form id="formRepo<%=rnd%>" onsubmit="event.preventDefault(); simpanRepository<%=rnd%>();">

            <!-- Hidden IDs — JANGAN reset bersama form.reset() -->
            <input type="hidden" id="repoItemId<%=rnd%>"  value="<%=idRepoEdit%>">
            <input type="hidden" id="idDcTitle<%=rnd%>"   value="">
            <input type="hidden" id="idDcAuthor<%=rnd%>"  value="">
            <input type="hidden" id="idDcDate<%=rnd%>"    value="">
            <input type="hidden" id="idDcAbstract<%=rnd%>" value="">

            <!-- Koleksi -->
            <div class="mb-4">
                <label class="form-label fw-bold text-secondary">
                    <i class="fas fa-folder-open me-1"></i>
                    <%=Common.getBahasaConfig("Koleksi Repository")%>
                </label>
                <select id="koleksiId<%=rnd%>" class="form-select border-primary shadow-sm" required>
                    <option value=""><%=Common.getBahasaConfig("-- Pilih Koleksi --")%></option>
                </select>
            </div>

            <!-- Metadata Dublin Core -->
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-header bg-white border-bottom">
                    <h6 class="mb-0 text-primary fw-bold">
                        <i class="fas fa-tags me-1"></i>
                        <%=Common.getBahasaConfig("Metadata Dublin Core (EAV)")%>
                    </h6>
                </div>
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-md-12">
                            <label class="form-label fw-bold">
                                <%=Common.getBahasaConfig("Judul Karya")%>
                                <small class="text-muted">(dc.title)</small>
                            </label>
                            <input type="text" id="dcTitle<%=rnd%>" class="form-control" required
                                placeholder="<%=Common.getBahasaConfig("Masukkan judul karya ilmiah")%>">
                        </div>

                        <div class="col-md-8">
                            <label class="form-label fw-bold">
                                <%=Common.getBahasaConfig("Penulis Utama")%>
                                <small class="text-muted">(dc.contributor.author)</small>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fas fa-user-edit"></i></span>
                                <input type="text" id="dcAuthor<%=rnd%>" class="form-control" required
                                    placeholder="<%=Common.getBahasaConfig("Contoh: Budi Santoso")%>">
                            </div>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label fw-bold">
                                <%=Common.getBahasaConfig("Tahun Terbit")%>
                                <small class="text-muted">(dc.date.issued)</small>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="far fa-calendar-alt"></i></span>
                                <input type="number" id="dcDate<%=rnd%>" class="form-control" required
                                    min="1900" max="2099"
                                    placeholder="<%=Common.getBahasaConfig("Contoh: 2024")%>">
                            </div>
                        </div>

                        <div class="col-md-12">
                            <label class="form-label fw-bold">
                                <%=Common.getBahasaConfig("Abstrak / Ringkasan")%>
                                <small class="text-muted">(dc.description.abstract)</small>
                            </label>
                            <textarea id="dcAbstract<%=rnd%>" class="form-control" rows="4" required
                                placeholder="<%=Common.getBahasaConfig("Tuliskan abstrak dokumen di sini...")%>">
                            </textarea>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Lampiran / Bitstream -->
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                    <h6 class="mb-0 text-primary fw-bold">
                        <i class="fas fa-copy me-1"></i>
                        <%=Common.getBahasaConfig("Lampiran Dokumen (Bitstream)")%>
                    </h6>
                    <button type="button" class="btn btn-sm btn-outline-success fw-bold shadow-sm"
                        onclick="tambahBarisFile<%=rnd%>()">
                        <i class="fas fa-plus-circle me-1"></i>
                        <%=Common.getBahasaConfig("Tambah Lampiran Baru")%>
                    </button>
                </div>
                <div class="card-body bg-light">
                    <!-- File existing (mode edit) -->
                    <div id="wadahLampiranExisting<%=rnd%>"></div>
                    <!-- File baru (input upload) -->
                    <div id="wadahLampiran<%=rnd%>"></div>
                </div>
            </div>

            <!-- Progress indicator -->
            <div id="progressArea<%=rnd%>" class="alert alert-info d-none shadow-sm mb-4">
                <div class="d-flex align-items-center">
                    <i class="fas fa-circle-notch fa-spin fa-2x me-3"></i>
                    <div>
                        <h6 class="mb-0 fw-bold" id="progressTitle<%=rnd%>">
                            <%=Common.getBahasaConfig("Memproses...")%>
                        </h6>
                        <small id="progressText<%=rnd%>">
                            <%=Common.getBahasaConfig("Mohon tunggu, sistem sedang menyimpan data.")%>
                        </small>
                    </div>
                </div>
            </div>

            <!-- Tombol Aksi -->
            <div class="text-end">
                <button type="button" class="btn btn-secondary me-2 shadow-sm rounded-pill px-4"
                    onclick="batalSimpan<%=rnd%>()">
                    <i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Batal")%>
                </button>
                <button type="submit" id="btnSimpan<%=rnd%>"
                    class="btn btn-primary shadow-sm rounded-pill px-4 fw-bold">
                    <i class="fas fa-save me-1"></i>
                    <%=isModeEdit
                        ? Common.getBahasaConfig("Simpan Perubahan")
                        : Common.getBahasaConfig("Simpan ke Repositori")%>
                </button>
            </div>
        </form>
    </div>
</div>

<script>
(function () {
    'use strict';

    // ── Konstanta ──────────────────────────────────────────────────────────
    const ROOT         = '<%=Common.ROOT%>';
    const RND          = '<%=rnd%>';
    const IS_EDIT      = '<%=idRepoEdit%>' !== '';

    // Urutan place (index EAV) sesuai RepositorySyncService
    const MD_FIELDS = [
        { inputId: 'dcTitle'    + RND, hiddenId: 'idDcTitle'    + RND, field: 'dc.title',                place: 0 },
        { inputId: 'dcAuthor'   + RND, hiddenId: 'idDcAuthor'   + RND, field: 'dc.contributor.author',   place: 1 },
        { inputId: 'dcDate'     + RND, hiddenId: 'idDcDate'     + RND, field: 'dc.date.issued',           place: 2 },
        { inputId: 'dcAbstract' + RND, hiddenId: 'idDcAbstract' + RND, field: 'dc.description.abstract', place: 3 }
    ];

    let fileCounter = 0;

    // ── Utility ────────────────────────────────────────────────────────────
    const apiPost = (body) =>
        fetch(ROOT + '/Data', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        }).then(r => r.json());

    const toast = (msg, cls) => {
        if (typeof tampilkanToast === 'function') tampilkanToast(msg, cls);
        else alert(msg);
    };

    const setProgress = (title, text) => {
        document.getElementById('progressTitle' + RND).innerText = title;
        document.getElementById('progressText'  + RND).innerText = text;
    };

    // OAI identifier unik: timestamp + random hex 4 karakter
    const generateOai = () =>
        'oai:onesearch.id:repo/' + Date.now() + '-'
        + Math.floor(Math.random() * 0xFFFF).toString(16).padStart(4, '0');

    // ── Lampiran: tambah / hapus baris ─────────────────────────────────────
    window['tambahBarisFile' + RND] = () => {
        fileCounter++;
        const rowId = 'barisFile_' + fileCounter + '_' + RND;
        document.getElementById('wadahLampiran' + RND).insertAdjacentHTML('beforeend',
            '<div class="row g-2 mb-3 align-items-center p-2 bg-white border rounded shadow-sm" id="' + rowId + '">'
            + '<div class="col-md-10">'
            +   '<label class="form-label text-secondary fw-bold small mb-1">'
            +     '<i class="fas fa-file-alt text-primary me-1"></i>'
            +     '<%=Common.getBahasaConfigJS("Pilih File Baru")%></label>'
            +   '<input type="file" class="form-control file-bitstream-' + RND + '" required'
            +     ' accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg">'
            + '</div>'
            + '<div class="col-md-2 mt-auto text-end">'
            +   '<button type="button" class="btn btn-outline-danger w-100 shadow-sm"'
            +     ' onclick="hapusBarisFile' + RND + '(\'' + rowId + '\')"'
            +     ' title="<%=Common.getBahasaConfig("Batal Upload File Ini")%>">'
            +     '<i class="fas fa-times"></i></button>'
            + '</div>'
            + '</div>');
    };

    window['hapusBarisFile' + RND] = (rowId) => {
        const el = document.getElementById(rowId);
        if (el) el.remove();
    };

    // ── Hapus file existing ────────────────────────────────────────────────
    window['hapusExistingFile' + RND] = async (bitstreamId) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus lampiran ini secara permanen?")%>'))
            return;
        try {
            // 1. Nonaktifkan record RepoBitstream
            await apiPost({
                action: 'simpanDataRinci',
                class:  'ais.database.model.repository.RepoBitstream',
                id:     bitstreamId,
                data:   { aktif: false },
                tanpaLogin: 'true'
            });
            // 2. Hapus file fisik via DoUpload/endpoint hapus file (jenis + sourceId)
            await apiPost({
                action:     'hapusDataRinci',
                class:      'ais.database.model.file.LampiranLain',
                jenis:      'ais.database.model.repository.RepoBitstream',
                sourceId:   bitstreamId
            });
            const row = document.getElementById('existingFile_' + bitstreamId + '_' + RND);
            if (row) row.remove();
            toast('<%=Common.getBahasaConfigJS("File lampiran berhasil dihapus.")%>', 'bg-success text-white');
        } catch (e) {
            console.error(e);
            toast('<%=Common.getBahasaConfigJS("Gagal menghapus file.")%>', 'bg-danger text-white');
        }
    };

    // ── Load koleksi dropdown ──────────────────────────────────────────────
    const loadKoleksi = async () => {
        try {
            const res = await apiPost({
                action:     'daftar',
                tanpaLogin: 'true',
                class:      'ais.database.model.repository.RepoCollection',
                max:        200,
                order1:     'asc',
                sort1:      'nama'
            });
            const sel = document.getElementById('koleksiId' + RND);
            if (res && res.data) {
                res.data.forEach(c => {
                    const opt  = document.createElement('option');
                    opt.value  = c.id;
                    opt.text   = c.nama;
                    sel.appendChild(opt);
                });
            }
        } catch (e) { console.error('Gagal memuat koleksi:', e); }
    };

    // ── Load data edit (mode ubah) ─────────────────────────────────────────
    const loadDataEdit = async () => {
        const repoItemId = document.getElementById('repoItemId' + RND).value;
        try {
            // 1. Load RepoItem utama
            const itemRes = await apiPost({
                action: 'load',
                class:  'ais.database.model.repository.RepoItem',
                id:     repoItemId
            });
            if (itemRes && itemRes.data) {
                document.getElementById('koleksiId' + RND).value = itemRes.data.collectionId;
            }

            // 2. Load Metadata EAV
            const mdRes = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql: 'SELECT id, metadata_field, metadata_value FROM repo_item_metadata'
                   + ' WHERE item_id = ' + repoItemId + ' AND (aktif IS NULL OR aktif = true)'
            });
            if (mdRes && mdRes.data) {
                mdRes.data.forEach(row => {
                    MD_FIELDS.forEach(mf => {
                        if (row.metadata_field === mf.field) {
                            document.getElementById(mf.inputId ).value = row.metadata_value || '';
                            document.getElementById(mf.hiddenId).value = row.id;
                        }
                    });
                });
            }

            // 3. Load Bitstream existing
            const fileRes = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql: 'SELECT id, nama_file FROM repo_bitstream'
                   + ' WHERE item_id = ' + repoItemId + ' AND (aktif IS NULL OR aktif = true)'
            });
            const wadah = document.getElementById('wadahLampiranExisting' + RND);
            if (fileRes && fileRes.data && fileRes.data.length > 0) {
                let html = '';
                fileRes.data.forEach(row => {
                    html +=
                        '<div class="row g-2 mb-3 align-items-center p-2 bg-light border border-secondary'
                        + ' rounded shadow-sm" id="existingFile_' + row.id + '_' + RND + '">'
                        + '<div class="col-md-10">'
                        +   '<label class="form-label text-success fw-bold small mb-1">'
                        +     '<i class="fas fa-check-circle me-1"></i>'
                        +     '<%=Common.getBahasaConfigJS("File Tersimpan")%></label>'
                        +   '<input type="text" class="form-control bg-white border-0"'
                        +     ' value="' + row.nama_file.replace(/"/g, '&quot;') + '" readonly>'
                        + '</div>'
                        + '<div class="col-md-2 mt-auto text-end">'
                        +   '<button type="button" class="btn btn-outline-danger w-100 shadow-sm"'
                        +     ' onclick="hapusExistingFile' + RND + '(' + row.id + ')"'
                        +     ' title="<%=Common.getBahasaConfig("Hapus Data & File Fisik")%>">'
                        +     '<i class="fas fa-trash-alt"></i></button>'
                        + '</div>'
                        + '</div>';
                });
                wadah.innerHTML = html;
            }
        } catch (e) {
            console.error(e);
            toast('<%=Common.getBahasaConfigJS("Gagal memuat rincian data repositori dari peladen.")%>',
                'bg-danger text-white');
        }
    };

    // ── Simpan ─────────────────────────────────────────────────────────────
    window['simpanRepository' + RND] = async () => {
        const btnSimpan     = document.getElementById('btnSimpan'     + RND);
        const progressArea  = document.getElementById('progressArea'  + RND);

        btnSimpan.disabled = true;
        progressArea.classList.remove('d-none');

        try {
            const repoItemId = document.getElementById('repoItemId' + RND).value;

            // TAHAP 1 – Simpan / Update RepoItem utama
            setProgress(
                '<%=Common.getBahasaConfigJS("Menyimpan Dokumen Utama...")%>',
                '<%=Common.getBahasaConfigJS("Sistem sedang mendaftarkan metadata ke Repositori.")%>'
            );
            const itemData = {
                collectionId: document.getElementById('koleksiId' + RND).value,
                aktif: true
            };
            if (repoItemId === '') {
                itemData.oaiIdentifier = generateOai();
                itemData.isWithdrawn   = false;
                itemData.syncStatus    = 'DRAFT';
            }
            const itemPayload = {
                action:     'simpanDataRinci',
                class:      'ais.database.model.repository.RepoItem',
                data:       itemData,
                tanpaLogin: 'true'
            };
            if (repoItemId !== '') itemPayload.id = repoItemId;

            const itemResult = await apiPost(itemPayload);

            if (!(itemResult.status === '00' || itemResult.status === 'success' || itemResult.id)) {
                toast('<%=Common.getBahasaConfigJS("Sistem gagal menyimpan dokumen utama.")%>',
                    'bg-danger text-white');
                return;
            }

            const finalId = repoItemId !== '' ? repoItemId : itemResult.id;

            // TAHAP 2 – Simpan Metadata EAV (dengan field place untuk ordering)
            setProgress(
                '<%=Common.getBahasaConfigJS("Menyimpan Metadata...")%>',
                '<%=Common.getBahasaConfigJS("Mendaftarkan metadata Dublin Core.")%>'
            );
            for (let i = 0; i < MD_FIELDS.length; i++) {
                const mf    = MD_FIELDS[i];
                const value = document.getElementById(mf.inputId ).value.trim();
                const idMd  = document.getElementById(mf.hiddenId).value;
                const mdPayload = {
                    action:     'simpanDataRinci',
                    class:      'ais.database.model.repository.RepoItemMetadata',
                    tanpaLogin: 'true',
                    data: {
                        itemId:        finalId,
                        metadataField: mf.field,
                        metadataValue: value,
                        language:      'id',
                        place:         mf.place,   // ← fix: wajib untuk ordering
                        aktif:         true
                    }
                };
                if (idMd !== '') mdPayload.id = idMd;
                await apiPost(mdPayload);
            }

            // TAHAP 3 – Upload Lampiran Baru
            const fileInputs = document.querySelectorAll('.file-bitstream-' + RND);
            for (let i = 0; i < fileInputs.length; i++) {
                const fInput = fileInputs[i];
                if (!fInput.files || fInput.files.length === 0) continue;

                const file = fInput.files[0];
                setProgress(
                    '<%=Common.getBahasaConfigJS("Mengunggah Lampiran...")%>',
                    '<%=Common.getBahasaConfigJS("Memproses file:")%> ' + file.name
                );

                // 3a. Buat record RepoBitstream
                const bitResult = await apiPost({
                    action:     'simpanDataRinci',
                    class:      'ais.database.model.repository.RepoBitstream',
                    tanpaLogin: 'true',
                    data: {
                        itemId:     finalId,
                        namaFile:   file.name,
                        pathSistem: file.name,
                        mimeType:   file.type || 'application/octet-stream',
                        ukuranByte: file.size,
                        bundleName: 'ORIGINAL',
                        aktif:      true
                    }
                });

                if (!(bitResult.status === '00' || bitResult.status === 'success' || bitResult.id))
                    continue;

                const newBitstreamId = bitResult.id;

                // 3b. Upload file fisik via DoUpload
                const formData = new FormData();
                formData.append('nama',        file.name);
                formData.append('clazz',       'ais.database.model.file.LampiranLain');
                formData.append('jenis',       'ais.database.model.repository.RepoBitstream');
                formData.append('id',          newBitstreamId);
                formData.append('tanpaLogin',  'true');
                formData.append('subdata',     '{}');
                formData.append('fileContent', file);

                await fetch(ROOT + '/DoUpload', { method: 'POST', body: formData });
            }

            toast('<%=Common.getBahasaConfigJS("Repositori berhasil disimpan.")%>', 'bg-success text-white');
            document.dispatchEvent(new Event('repositorySavedEvent'));
            batalSimpan();

        } catch (e) {
            console.error(e);
            toast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi jaringan saat mencoba menyimpan data.")%>',
                'bg-danger text-white');
        } finally {
            btnSimpan.disabled = false;
            progressArea.classList.add('d-none');
        }
    };

    // ── Batal / Tutup Modal ────────────────────────────────────────────────
    // Hanya reset field-field input visible; hidden ID tidak di-reset agar konsisten
    const batalSimpan = () => {
        ['dcTitle', 'dcAuthor', 'dcDate', 'dcAbstract'].forEach(id => {
            const el = document.getElementById(id + RND);
            if (el) el.value = '';
        });
        document.getElementById('koleksiId'              + RND).value = '';
        document.getElementById('wadahLampiran'          + RND).innerHTML = '';
        document.getElementById('wadahLampiranExisting'  + RND).innerHTML = '';
        try {
            const modal = document.getElementById('formRepo' + RND).closest('.modal');
            if (modal) {
                const inst = bootstrap.Modal.getInstance(modal);
                if (inst) inst.hide();
            }
        } catch (err) {}
    };
    window['batalSimpan' + RND] = batalSimpan;

    // ── Init ───────────────────────────────────────────────────────────────
    (async () => {
        await loadKoleksi();
        if (IS_EDIT) {
            await loadDataEdit();
        } else {
            const wadah = document.getElementById('wadahLampiran' + RND);
            if (wadah && wadah.innerHTML.trim() === '') {
                window['tambahBarisFile' + RND]();
            }
        }
    })();

}());
</script>
