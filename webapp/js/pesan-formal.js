/**
 * Dialog alert global AIS/eCampus.
 *
 * Tidak bergantung pada Bootstrap/jQuery supaya dapat dipakai oleh halaman
 * JSP mandiri, shell JSP utama, dan halaman ZKoss. Pemanggilan alert(...) lama
 * tetap didukung; konfirmasi asynchronous memakai AisAlert.confirm(...).
 */
(function (window, document) {
    "use strict";

    if (window.AisAlert && window.AisAlert.version) return;

    var nativeAlert = window.__nativeAlert || window.alert;
    var nativeConfirm = window.__nativeConfirm || window.confirm;
    var sequence = 0;
    var queue = [];
    var active = false;

    window.__nativeAlert = nativeAlert;
    window.__nativeConfirm = nativeConfirm;

    function text(value, fallback) {
        if (value === null || typeof value === "undefined" || String(value).trim() === "") {
            return fallback || "";
        }
        return String(value);
    }

    function normalizeType(value) {
        var type = text(value, "info").toLowerCase();
        if (type.indexOf("danger") >= 0 || type.indexOf("error") >= 0 || type.indexOf("stop") >= 0) return "error";
        if (type.indexOf("warn") >= 0 || type.indexOf("exclamation") >= 0) return "warning";
        if (type.indexOf("success") >= 0 || type.indexOf("sukses") >= 0) return "success";
        if (type.indexOf("question") >= 0 || type.indexOf("confirm") >= 0 || type.indexOf("pertanyaan") >= 0) return "question";
        return "info";
    }

    function defaultTitle(type) {
        if (type === "error") return "Terjadi Kesalahan";
        if (type === "warning") return "Peringatan";
        if (type === "success") return "Berhasil";
        if (type === "question") return "Konfirmasi";
        return "Pemberitahuan";
    }

    function iconFor(type) {
        if (type === "success") return "\u2713";
        if (type === "question") return "?";
        if (type === "info") return "i";
        return "!";
    }

    function ensureStyles() {
        if (document.getElementById("ais-alert-global-style")) return;
        var style = document.createElement("style");
        style.id = "ais-alert-global-style";
        style.type = "text/css";
        style.appendChild(document.createTextNode(
            ".ais-alert-overlay{position:fixed;inset:0;z-index:2147483000;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(15,23,42,.48);backdrop-filter:blur(5px);-webkit-backdrop-filter:blur(5px);font-family:Poppins,'Segoe UI',Arial,sans-serif;box-sizing:border-box}" +
            ".ais-alert-dialog{width:min(790px,100%);max-width:100%;max-height:calc(100vh - 40px);display:flex;flex-direction:column;overflow:hidden;box-sizing:border-box;background:#fff;border:1px solid rgba(255,255,255,.72);border-radius:24px;box-shadow:0 28px 80px rgba(15,23,42,.36);color:#172033;animation:aisAlertIn .2s ease-out}" +
            ".ais-alert-header{display:flex;align-items:center;gap:22px;padding:32px 34px;box-sizing:border-box;background:linear-gradient(118deg,#cf6a16 0%,#80506a 44%,#2945d6 100%);color:#fff}" +
            ".ais-alert-icon{width:64px;height:64px;flex:0 0 64px;display:flex;align-items:center;justify-content:center;border-radius:50%;background:rgba(255,255,255,.18);box-shadow:inset 0 0 0 1px rgba(255,255,255,.16);font-size:38px;font-weight:700;line-height:1}" +
            ".ais-alert-heading{min-width:0;flex:1}.ais-alert-title{margin:0 0 7px;font-size:25px;line-height:1.25;font-weight:800;letter-spacing:.01em;color:#fff}.ais-alert-subtitle{margin:0;font-size:16px;line-height:1.45;color:rgba(255,255,255,.92)}" +
            ".ais-alert-close{width:48px;height:48px;flex:0 0 48px;border:1px solid rgba(255,255,255,.55);border-radius:13px;background:rgba(15,23,42,.08);color:#fff;font-size:32px;line-height:1;cursor:pointer}.ais-alert-close:hover,.ais-alert-close:focus{background:rgba(255,255,255,.16);outline:2px solid rgba(255,255,255,.55);outline-offset:2px}" +
            ".ais-alert-body{padding:34px 40px;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere;font-size:17px;line-height:1.65;color:#1e293b;background:#fff}" +
            ".ais-alert-detail{display:none;margin:0 40px 24px;padding:18px 20px;max-height:220px;overflow:auto;border:1px solid #dbe4f0;border-radius:14px;background:#f8fafc;color:#475569;white-space:pre-wrap;overflow-wrap:anywhere;font:13px/1.55 Consolas,Monaco,monospace}.ais-alert-detail.is-open{display:block}" +
            ".ais-alert-footer{display:flex;align-items:center;justify-content:flex-end;gap:14px;padding:22px 32px;box-sizing:border-box;border-top:1px solid #e7edf5;background:#fbfcfe}.ais-alert-action{min-height:46px;padding:10px 16px;border:0;border-radius:11px;background:transparent;color:#1d4ed8;font-size:15px;font-weight:700;cursor:pointer}.ais-alert-action:hover,.ais-alert-action:focus{background:#eef4ff;outline:none}.ais-alert-divider{width:1px;height:34px;background:#dbe3ee}" +
            ".ais-alert-primary,.ais-alert-secondary{min-height:48px;padding:11px 24px;border-radius:12px;font-size:15px;font-weight:800;cursor:pointer}.ais-alert-primary{border:1px solid #db7216;background:linear-gradient(135deg,#ea841e,#c85d0c);color:#fff;box-shadow:0 8px 18px rgba(201,91,12,.25)}.ais-alert-primary:hover,.ais-alert-primary:focus{filter:brightness(1.06);outline:3px solid rgba(234,132,30,.22);outline-offset:2px}.ais-alert-secondary{border:1px solid #cbd5e1;background:#fff;color:#334155}.ais-alert-secondary:hover,.ais-alert-secondary:focus{background:#f1f5f9;outline:none}" +
            "body.ais-alert-body-locked{overflow:hidden!important}" +
            "@keyframes aisAlertIn{from{opacity:0;transform:translateY(12px) scale(.975)}to{opacity:1;transform:none}}" +
            "@media(max-width:640px){.ais-alert-overlay{padding:10px;align-items:flex-end}.ais-alert-dialog{width:calc(100vw - 20px);max-width:calc(100vw - 20px);max-height:94vh;border-radius:20px}.ais-alert-header{gap:14px;padding:22px 18px}.ais-alert-icon{width:48px;height:48px;flex-basis:48px;font-size:29px}.ais-alert-title{font-size:19px}.ais-alert-subtitle{font-size:13px}.ais-alert-close{width:40px;height:40px;flex-basis:40px;font-size:27px}.ais-alert-body{padding:24px 20px;font-size:15px}.ais-alert-detail{margin:0 20px 18px}.ais-alert-footer{padding:16px;gap:8px;flex-wrap:wrap}.ais-alert-action,.ais-alert-primary,.ais-alert-secondary{font-size:13px}.ais-alert-divider{display:none}}"
        ));
        (document.head || document.documentElement).appendChild(style);
    }

    function button(label, cssClass, handler) {
        var result = document.createElement("button");
        result.type = "button";
        result.className = cssClass;
        result.textContent = label;
        result.addEventListener("click", handler);
        return result;
    }

    function enqueue(options) {
        return new Promise(function (resolve) {
            queue.push({ options: options || {}, resolve: resolve });
            pump();
        });
    }

    function pump() {
        if (active || queue.length === 0) return;
        if (!document.body) {
            document.addEventListener("DOMContentLoaded", pump, { once: true });
            return;
        }
        active = true;
        render(queue.shift());
    }

    function render(entry) {
        ensureStyles();
        sequence++;

        var options = entry.options;
        var type = normalizeType(options.type || options.jenis);
        var isConfirm = options.confirm === true || type === "question";
        var previousFocus = document.activeElement;
        var overlay = document.createElement("div");
        var dialog = document.createElement("section");
        var header = document.createElement("header");
        var icon = document.createElement("div");
        var heading = document.createElement("div");
        var title = document.createElement("h2");
        var subtitle = document.createElement("p");
        var close = document.createElement("button");
        var body = document.createElement("div");
        var detail = document.createElement("pre");
        var footer = document.createElement("footer");
        var titleId = "ais-alert-title-" + sequence;
        var bodyId = "ais-alert-body-" + sequence;
        var done = false;

        overlay.className = "ais-alert-overlay";
        overlay.setAttribute("data-ais-alert", type);
        dialog.className = "ais-alert-dialog";
        dialog.setAttribute("role", isConfirm ? "alertdialog" : "dialog");
        dialog.setAttribute("aria-modal", "true");
        dialog.setAttribute("aria-labelledby", titleId);
        dialog.setAttribute("aria-describedby", bodyId);
        dialog.tabIndex = -1;

        header.className = "ais-alert-header";
        icon.className = "ais-alert-icon";
        icon.setAttribute("aria-hidden", "true");
        icon.textContent = iconFor(type);
        heading.className = "ais-alert-heading";
        title.className = "ais-alert-title";
        title.id = titleId;
        title.textContent = text(options.title, defaultTitle(type));
        subtitle.className = "ais-alert-subtitle";
        subtitle.textContent = text(options.subtitle,
            text(options.detail, "") ? "Klik Detail untuk melihat informasi teknis dan saran perbaikan."
                : (isConfirm ? "Periksa informasi berikut sebelum melanjutkan."
                    : "Periksa informasi berikut untuk tindak lanjut yang tepat."));
        close.className = "ais-alert-close";
        close.type = "button";
        close.setAttribute("aria-label", text(options.closeLabel, "Tutup"));
        close.innerHTML = "&times;";
        heading.appendChild(title);
        heading.appendChild(subtitle);
        header.appendChild(icon);
        header.appendChild(heading);
        header.appendChild(close);

        body.className = "ais-alert-body";
        body.id = bodyId;
        body.textContent = text(options.message, "");
        detail.className = "ais-alert-detail";
        detail.textContent = text(options.detail, "");
        detail.setAttribute("aria-hidden", "true");
        footer.className = "ais-alert-footer";

        function finish(value) {
            if (done) return;
            done = true;
            document.removeEventListener("keydown", onKeydown, true);
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
            if (!document.querySelector(".ais-alert-overlay")) document.body.classList.remove("ais-alert-body-locked");
            if (previousFocus && typeof previousFocus.focus === "function") {
                try { previousFocus.focus(); } catch (ignore) {}
            }
            entry.resolve(value);
            active = false;
            pump();
        }

        function onKeydown(event) {
            if (event.key === "Escape" || event.keyCode === 27) {
                event.preventDefault();
                finish(false);
                return;
            }
            if ((event.key === "Tab" || event.keyCode === 9) && overlay.contains(document.activeElement)) {
                var focusable = overlay.querySelectorAll("button:not([disabled]),[href],input,select,textarea,[tabindex]:not([tabindex='-1'])");
                if (focusable.length === 0) return;
                var first = focusable[0];
                var last = focusable[focusable.length - 1];
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }
        }

        close.addEventListener("click", function () { finish(false); });
        overlay.addEventListener("mousedown", function (event) {
            if (event.target === overlay && !isConfirm) finish(false);
        });

        if (text(options.detail, "")) {
            footer.appendChild(button(text(options.detailLabel, "Lihat detail \u2197"), "ais-alert-action", function () {
                var open = detail.classList.toggle("is-open");
                detail.setAttribute("aria-hidden", open ? "false" : "true");
            }));
        }
        if (typeof options.onEdit === "function") {
            if (footer.childNodes.length) {
                var divider = document.createElement("span");
                divider.className = "ais-alert-divider";
                divider.setAttribute("aria-hidden", "true");
                footer.appendChild(divider);
            }
            footer.appendChild(button(text(options.editLabel, "\u270E Ubah teks"), "ais-alert-action", function () {
                options.onEdit(options);
            }));
        }
        if (isConfirm) {
            footer.appendChild(button(text(options.cancelLabel, "Batal"), "ais-alert-secondary", function () { finish(false); }));
            footer.appendChild(button(text(options.confirmLabel, "Ya, Lanjutkan"), "ais-alert-primary", function () { finish(true); }));
        } else {
            footer.appendChild(button("\u00D7  " + text(options.closeLabel, "Tutup"), "ais-alert-primary", function () { finish(true); }));
        }

        dialog.appendChild(header);
        dialog.appendChild(body);
        if (text(options.detail, "")) dialog.appendChild(detail);
        dialog.appendChild(footer);
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
        document.body.classList.add("ais-alert-body-locked");
        document.addEventListener("keydown", onKeydown, true);
        window.setTimeout(function () {
            var primary = footer.querySelector(".ais-alert-primary");
            (primary || dialog).focus();
        }, 0);
    }

    function markZkMessageboxes(root) {
        if (!document.querySelectorAll) return;
        var scope = root && root.querySelectorAll ? root : document;
        var boxes = scope.querySelectorAll(".z-messagebox");
        for (var i = 0; i < boxes.length; i++) {
            var node = boxes[i];
            var win = node;
            while (win && win !== document.body && !(" " + win.className + " ").match(/ z-window-(highlighted|modal) /)) {
                win = win.parentNode;
            }
            if (!win || win === document.body) continue;
            if ((" " + win.className + " ").indexOf(" ais-alert-zk ") < 0) {
                win.className += " ais-alert-zk";
                win.setAttribute("role", "alertdialog");
                win.setAttribute("aria-modal", "true");
            }
            var iconNode = win.querySelector(".z-msgbox");
            var zkType = normalizeType(iconNode ? iconNode.className : "");
            if ((" " + win.className + " ").indexOf(" ais-alert-zk-" + zkType + " ") < 0) win.className += " ais-alert-zk-" + zkType;
        }
        if (document.body) document.body.classList.toggle("ais-zk-alert-open", document.querySelector(".ais-alert-zk .z-messagebox") !== null);
    }

    function installZkObserver() {
        if (!document.body || window.__aisAlertZkObserver) return;
        markZkMessageboxes(document);
        if (window.MutationObserver) {
            var scanScheduled = false;
            window.__aisAlertZkObserver = new MutationObserver(function () {
                if (scanScheduled) return;
                scanScheduled = true;
                window.setTimeout(function () {
                    scanScheduled = false;
                    markZkMessageboxes(document);
                }, 16);
            });
            window.__aisAlertZkObserver.observe(document.body, { childList: true, subtree: true });
        }
    }

    var api = {
        version: "2026.09.06",
        show: function (options) {
            if (typeof options !== "object" || options === null) options = { message: options };
            return enqueue(options);
        },
        confirm: function (options) {
            if (typeof options !== "object" || options === null) options = { message: options };
            options.confirm = true;
            options.type = options.type || "question";
            return enqueue(options);
        },
        markZkMessageboxes: markZkMessageboxes,
        nativeAlert: nativeAlert,
        nativeConfirm: nativeConfirm
    };

    window.AisAlert = api;
    window.alert = function (message) { api.show({ message: message, type: "info" }); };
    window.confirmAsync = function (message) { return api.confirm({ message: message }); };

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", installZkObserver);
    else installZkObserver();
})(window, document);

/** Menampilkan pesan gagal formal yang konsisten pada seluruh halaman JSP/JS. */
function tampilkanPesanGagalFormal(aktivitas, penyebab, langkahSolusi) {
    var pesan = "Yang terhormat Bapak/Ibu Pengguna,\n\n";
    pesan += "Mohon maaf, telah terjadi kesalahan pada saat memproses " + (aktivitas || "permintaan Bapak/Ibu") + ".\n\n";
    pesan += "Penyebab: " + (penyebab && penyebab.length > 0 ? penyebab :
        "Sistem tidak dapat menentukan penyebab pasti secara otomatis. Kendala ini kemungkinan bersifat sementara.") + "\n\n";
    pesan += "Tindak Lanjut yang dapat Bapak/Ibu coba:\n";
    if (langkahSolusi && langkahSolusi.length > 0) {
        for (var i = 0; i < langkahSolusi.length; i++) pesan += "  " + (i + 1) + ". " + langkahSolusi[i] + "\n";
    } else {
        pesan += "  1. Silakan ulangi proses ini beberapa saat lagi.\n";
        pesan += "  2. Periksa kembali data/koneksi internet Bapak/Ibu sebelum mencoba kembali.\n";
    }
    pesan += "\nApabila Bapak/Ibu kurang memahami pesan kesalahan ini atau langkah-langkah di atas belum " +
        "berhasil mengatasi masalah, kami mohon agar segera menghubungi Administrator Sistem, atau " +
        "melaporkan kejadian ini kepada Pengembang Sistem. Mohon WAJIB melampirkan tangkapan layar " +
        "(screenshot) pada saat kesalahan ini terjadi.";
    if (window.AisAlert) return window.AisAlert.show({ title: "Terjadi Kesalahan", message: pesan, type: "error" });
    window.__nativeAlert(pesan);
}

/** Menampilkan pesan sukses formal yang konsisten. */
function tampilkanPesanSuksesFormal(aktivitas, detail) {
    var pesan = "Yang terhormat Bapak/Ibu Pengguna,\n\n";
    pesan += "Dengan ini kami sampaikan bahwa proses " + (aktivitas || "yang Bapak/Ibu jalankan") +
        " telah BERHASIL diselesaikan dengan baik.";
    if (detail && detail.length > 0) pesan += "\n\nRincian: " + detail;
    pesan += "\n\nTerima kasih atas kesabaran Bapak/Ibu.";
    if (window.AisAlert) return window.AisAlert.show({ title: "Berhasil", message: pesan, type: "success" });
    window.__nativeAlert(pesan);
}
