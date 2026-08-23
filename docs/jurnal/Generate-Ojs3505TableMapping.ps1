param(
    [string]$OjsRoot = (Join-Path $env:TEMP 'codex-ojs-3505-audit'),
    [string]$OutputPath = (Join-Path $PSScriptRoot '02-OJS-3505-TABLE-MAPPING.md')
)

$ErrorActionPreference = 'Stop'

$expectedOjs = '372e3b84740344db6c2f85193c3eb3e4f539cb00'
$expectedPkp = '8b5f0fdc8d5664000b8652002781a14bd406bf21'
$actualOjs = (git -C $OjsRoot rev-parse HEAD).Trim()
$actualPkp = (git -C (Join-Path $OjsRoot 'lib/pkp') rev-parse HEAD).Trim()
if ($actualOjs -ne $expectedOjs) { throw "OJS commit mismatch: $actualOjs" }
if ($actualPkp -ne $expectedPkp) { throw "pkp-lib commit mismatch: $actualPkp" }

$tableNames = @'
announcement_settings
announcement_type_settings
announcement_types
announcements
author_affiliation_settings
author_affiliations
author_settings
authors
categories
category_settings
citation_settings
citations
completed_payments
controlled_vocab_entries
controlled_vocab_entry_settings
controlled_vocabs
custom_issue_orders
custom_section_orders
data_object_tombstone_oai_set_objects
data_object_tombstone_settings
data_object_tombstones
doi_settings
dois
edit_decisions
email_log
email_log_users
email_templates
email_templates_default_data
email_templates_settings
event_log
event_log_settings
failed_jobs
files
filter_groups
filter_settings
filters
genre_settings
genres
highlight_settings
highlights
institution_ip
institution_settings
institutional_subscription_ip
institutional_subscriptions
institutions
invitations
issue_files
issue_galley_settings
issue_galleys
issue_settings
issues
job_batches
jobs
journal_settings
journals
library_file_settings
library_files
metrics_context
metrics_counter_submission_daily
metrics_counter_submission_institution_daily
metrics_counter_submission_institution_monthly
metrics_counter_submission_monthly
metrics_issue
metrics_submission
metrics_submission_geo_daily
metrics_submission_geo_monthly
navigation_menu_item_assignment_settings
navigation_menu_item_assignments
navigation_menu_item_settings
navigation_menu_items
navigation_menus
notes
notification_settings
notification_subscription_settings
notifications
oai_resumption_tokens
plugin_settings
publication_categories
publication_galley_settings
publication_galleys
publication_settings
publications
queries
query_participants
queued_payments
review_assignment_settings
review_assignments
review_files
review_form_element_settings
review_form_elements
review_form_responses
review_form_settings
review_forms
review_round_files
review_rounds
reviewer_suggestion_settings
reviewer_suggestions
ror_settings
rors
section_settings
sections
sessions
site
site_settings
stage_assignments
static_page_settings
static_pages
subeditor_submission_group
submission_comments
submission_file_revisions
submission_file_settings
submission_files
submission_search_keyword_list
submission_search_object_keywords
submission_search_objects
submission_settings
submission_tombstones
submissions
subscription_type_settings
subscription_types
subscriptions
temporary_files
usage_stats_institution_temporary_records
usage_stats_total_temporary_records
usage_stats_unique_item_investigations_temporary_records
usage_stats_unique_item_requests_temporary_records
user_group_settings
user_group_stage
user_groups
user_interests
user_settings
user_user_groups
users
versions
'@ -split "`r?`n" | Where-Object { $_ }

if (($tableNames | Sort-Object -Unique).Count -ne 134) { throw 'Expected exactly 134 unique table names.' }

$columnMethods = @(
    'bigIncrements','increments','bigInteger','integer','mediumInteger','smallInteger','tinyInteger',
    'decimal','double','float','boolean','char','string','text','mediumText','longText','date','dateTime',
    'timestamp','binary','uuid','json','jsonb','ipAddress','unsignedInteger','unsignedTinyInteger','foreignId'
)

function Get-PhpSchemas {
    $files = @()
    $files += Get-ChildItem -LiteralPath (Join-Path $OjsRoot 'classes/migration/install') -Filter '*.php' -File
    $files += Get-ChildItem -LiteralPath (Join-Path $OjsRoot 'lib/pkp/classes/migration/install') -Filter '*.php' -File
    $files += Get-Item -LiteralPath (Join-Path $OjsRoot 'plugins/generic/staticPages/StaticPagesSchemaMigration.php')
    $schemas = @{}
    foreach ($file in $files) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw
        $matches = [regex]::Matches($raw, "Schema::create\('(?<table>[^']+)'\s*,\s*function\s*\(Blueprint\s+\`$table\)\s*\{(?<body>.*?)\n\s*\}\);", [Text.RegularExpressions.RegexOptions]::Singleline)
        foreach ($match in $matches) {
            $name = $match.Groups['table'].Value
            $body = $match.Groups['body'].Value
            $source = $file.FullName.Substring($OjsRoot.Length + 1).Replace('\','/')
            $columns = New-Object System.Collections.ArrayList
            foreach ($line in ($body -split "`r?`n")) {
                $trim = $line.Trim()
                $columnMatch = [regex]::Match($trim, "^\`$table->(?<type>[A-Za-z]+)\('(?<name>[^']+)'(?<args>[^)]*)\)(?<mods>.*?);$")
                if ($columnMatch.Success -and $columnMethods -contains $columnMatch.Groups['type'].Value) {
                    $mods = $columnMatch.Groups['mods'].Value
                    $default = ''
                    $defaultMatch = [regex]::Match($mods, "->default\((?<value>.*?)\)")
                    if ($defaultMatch.Success) { $default = $defaultMatch.Groups['value'].Value.Replace('|','\|') }
                    [void]$columns.Add([pscustomobject]@{
                        Name = $columnMatch.Groups['name'].Value
                        Type = $columnMatch.Groups['type'].Value + $columnMatch.Groups['args'].Value
                        Nullable = $(if ($mods -match '->nullable\(') { 'YES' } else { 'NO' })
                        Default = $default
                        Modifiers = (($mods -replace '->comment\(.*','') -replace '\|','\|').Trim()
                    })
                } elseif ($trim -match '^\$table->rememberToken\(\);$') {
                    [void]$columns.Add([pscustomobject]@{ Name='remember_token'; Type='string, 100'; Nullable='YES'; Default=''; Modifiers='Laravel rememberToken' })
                } elseif ($trim -match '^\$table->timestamps\(\);$') {
                    [void]$columns.Add([pscustomobject]@{ Name='created_at'; Type='timestamp'; Nullable='YES'; Default=''; Modifiers='Laravel timestamps' })
                    [void]$columns.Add([pscustomobject]@{ Name='updated_at'; Type='timestamp'; Nullable='YES'; Default=''; Modifiers='Laravel timestamps' })
                } elseif ($trim -match "^\`$table->softDeletes\('(?<name>[^']+)'[^)]*\);$") {
                    [void]$columns.Add([pscustomobject]@{ Name=$Matches['name']; Type='timestamp'; Nullable='YES'; Default=''; Modifiers='Laravel softDeletes' })
                }
            }
            $enumMatch = [regex]::Match($body, "\`$table->enum\(\s*'(?<name>[^']+)'\s*,(?<values>.*?)\]\s*\);", [Text.RegularExpressions.RegexOptions]::Singleline)
            if ($enumMatch.Success) {
                [void]$columns.Add([pscustomobject]@{
                    Name=$enumMatch.Groups['name'].Value
                    Type=('enum(' + (($enumMatch.Groups['values'].Value -replace "`r?`n", ' ') -replace '\s+', ' ').Trim() + '])')
                    Nullable='NO'
                    Default=''
                    Modifiers=''
                })
            }
            $flat = ($body -replace "`r?`n", ' ') -replace '\s+', ' '
            $constraints = New-Object System.Collections.ArrayList
            foreach ($constraint in [regex]::Matches($flat, "\`$table->(?<kind>foreign|index|unique|primary)\((?<expr>.*?)\);")) {
                [void]$constraints.Add(($constraint.Groups['kind'].Value + '(' + $constraint.Groups['expr'].Value.Trim() + ')').Replace('|','\|'))
            }
            $schemas[$name] = [pscustomobject]@{ Name=$name; Presence='FRESH_3505'; Source=$source; Columns=$columns; Constraints=$constraints }
        }
    }
    return $schemas
}

function Add-LegacyXmlSchema([hashtable]$schemas, [string]$tableName) {
    [xml]$xml = Get-Content -LiteralPath (Join-Path $OjsRoot 'dbscripts/xml/ojs_schema.xml') -Raw
    $node = $xml.SelectSingleNode("//table[@name='$tableName']")
    if ($null -eq $node) { throw "Legacy table missing from ojs_schema.xml: $tableName" }
    $columns = New-Object System.Collections.ArrayList
    foreach ($field in $node.field) {
        [void]$columns.Add([pscustomobject]@{
            Name = [string]$field.name
            Type = ([string]$field.type) + $(if ($field.size) { ', ' + [string]$field.size } else { '' })
            Nullable = $(if ($field.NOTNULL) { 'NO' } else { 'YES' })
            Default = $(if ($field.DEFAULT) { [string]$field.DEFAULT.VALUE } else { '' })
            Modifiers = $(if ($field.KEY) { 'PRIMARY KEY' } else { '' })
        })
    }
    $constraints = New-Object System.Collections.ArrayList
    foreach ($index in $node.index) {
        $value = 'index ' + [string]$index.name + '(' + (($index.col | ForEach-Object { [string]$_ }) -join ', ') + ')'
        if ($index.UNIQUE) { $value += ' UNIQUE' }
        [void]$constraints.Add($value)
    }
    $schemas[$tableName] = [pscustomobject]@{
        Name=$tableName
        Presence='LEGACY_SOURCE_ONLY'
        Source='dbscripts/xml/ojs_schema.xml (deprecated schema retained for legacy upgrade/import evidence)'
        Columns=$columns
        Constraints=$constraints
    }
}

function Get-Target([string]$name) {
    if ($name -eq 'sessions') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='AIS Spring Security/session store';Candidate='webapp/WEB-INF/applicationContext-security.xml';Reason='Active OJS sessions and credentials are never imported.'} }
    if ($name -eq 'versions') { return [pscustomobject]@{Decision='DERIVED';Target='ImportSumberOjs.schemaVersion and immutable import report';Candidate='JDBC read-only source reader; target persisted by HibernateUtil in schema penelitiandanpengabdian';Reason='Schema signature is read through JDBC for dialect detection, not copied as business data. The new importer does not use OjsHibernateUtil.'} }
    if ($name -match '^(jobs|job_batches|failed_jobs)$') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='Native AIS generic job/outbox infrastructure; source row retained only in reconciliation/provenance';Candidate='src/ais/database/model/tenant/ProvisioningJob.java is a lifecycle pattern only, not a reusable table';Reason='OJS queue payloads and failures contain PHP runtime semantics and must never be executed or copied into a journal-specific job table.'} }
    if ($name -match '^(filter_groups|filters|filter_settings)$') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='Native Java metadata pipeline plus reviewed RepoCollection metadataProfileJson configuration';Candidate='src/ais/database/model/repository/RepoCollection.java';Reason='OJS PHP filter class/configuration is implementation-specific. Preserve source evidence in provenance, implement equivalent behavior in Java, and do not create three filter tables.'} }
    if ($name -eq 'oai_resumption_tokens') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='OAI runtime cursor/token service';Candidate='src/ais/action/servlet/Oai.java';Reason='Resumption tokens are ephemeral runtime state and are regenerated by AIS; importing them is invalid and unsafe.'} }
    if ($name -eq 'temporary_files') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='Bounded temporary upload service; final accepted content uses RepoBitstream plus LampiranLain';Candidate='src/ais/database/model/repository/RepoBitstream.java; src/ais/database/model/file/LampiranLain.java';Reason='Temporary uploads are not durable business records. Only accepted referenced files are imported.'} }
    if ($name -match '^usage_stats_.*_temporary_records$') { return [pscustomobject]@{Decision='NOT_APPLICABLE_WITH_RATIONALE';Target='Rebuilt RepoUsageEvent/AgregatPenggunaanJurnal pipeline';Candidate='src/ais/database/model/repository/RepoUsageEvent.java';Reason='These rows are temporary aggregation staging and must not become target domain tables.'} }
    if ($name -match '^(journals|journal_settings)$') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='JurnalPenelitian canonical master plus linked RepoCollection profiles and ImportMappingOjs';Candidate='src/ais/database/model/penelitiandanpengabdian/JurnalPenelitian.java; src/ais/database/model/repository/RepoCollection.java';Reason='Do not create a second Jurnal master. Add a safe source link/config profile and retain OJS identity only in provenance.'} }
    if ($name -match '^(site|site_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='PerguruanTinggi platform identity plus JurnalPenelitian/RepoCollection configuration profiles and provenance';Candidate='src/ais/database/model/PerguruanTinggi.java; src/ais/database/model/repository/RepoCollection.java';Reason='Site identity/locales/password policy belong to existing platform configuration; no PortalJurnal table is introduced.'} }
    if ($name -match '^(submissions|submission_settings|publications|publication_settings)$') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoItem version/workflow record plus RepoItemMetadata and Artikel published projection';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoItemMetadata.java; src/ais/database/model/penelitiandanpengabdian/Artikel.java';Reason='RepoItem already has workflow, version chain, access, DOI/OAI and publication fields. Extend it with journal/root submission/stage scope instead of creating NaskahJurnal and VersiPublikasiJurnal tables.'} }
    if ($name -match '^(authors|author_settings|author_affiliations|author_affiliation_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoAuthorAuthority plus RepoItemContributor/RepoItemMetadata and AnggotaArtikel projection';Candidate='src/ais/database/model/repository/RepoAuthorAuthority.java; src/ais/database/model/repository/RepoItemContributor.java; src/ais/database/model/penelitiandanpengabdian/AnggotaArtikel.java';Reason='Existing repository authority already supports ordered contributors, ORCID, ROR, affiliation and external identity. AnggotaArtikel remains the compatibility projection.'} }
    if ($name -match '(^files$|files|_file_|galley|galleys)') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoBitstream metadata/revision/galley plus LampiranLain streaming content and FileArtikel projection';Candidate='src/ais/database/model/repository/RepoBitstream.java; src/ais/database/model/file/LampiranLain.java; src/ais/database/model/penelitiandanpengabdian/FileArtikel.java';Reason='Reuse RepoBitstream for checksum/version/bundle/access metadata and generic LampiranLain for streaming BLOB content keyed by scalar RepoBitstream ID. No LampiranJurnal table or cross-SessionFactory ORM is needed.'} }
    if ($name -match '^metrics_') { return [pscustomobject]@{Decision='MERGED';Target='RepoUsageEvent, RepoItem counters and one dimensioned AgregatPenggunaanJurnal table';Candidate='src/ais/database/model/repository/RepoUsageEvent.java; src/ais/database/model/repository/RepoItem.java';Reason='Reuse raw events and total counters; collapse daily/monthly/context/geo/COUNTER outputs into one typed aggregate table rather than one table per OJS metric.'} }
    if ($name -match '^submission_search_') { return [pscustomobject]@{Decision='DERIVED';Target='Search projection rebuilt from RepoItem and RepoItemMetadata';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoItemMetadata.java';Reason='OJS search index rows are derived and are not imported. Preserve source counts for reconciliation and rebuild the AIS index.'} }
    if ($name -match '^(event_log|event_log_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoWorkflowEvent/RepoIntegrationEvent plus Envers and ImportMappingOjs snapshot';Candidate='src/ais/database/model/repository/RepoWorkflowEvent.java; src/ais/database/model/repository/RepoIntegrationEvent.java';Reason='Reuse append-only repository events for item workflow/integration; imported audit remains immutable provenance.'} }
    if ($name -eq 'institutional_subscription_ip') { return [pscustomobject]@{Decision='MERGED';Target='RentangIpLanggananJurnal linked to existing PerguruanTinggi/PerguruanTinggiLain identity and provenance';Candidate='src/ais/database/model/PerguruanTinggi.java; src/ais/database/model/PerguruanTinggiLain.java';Reason='Only the journal access range/link is new; do not duplicate an institution master.'} }
    if ($name -eq 'submission_tombstones' -or $name -match '^data_object_tombstone') { return [pscustomobject]@{Decision='MERGED';Target='RepoItem withdrawal/OAI state plus RepoIntegrationEvent and provenance';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoIntegrationEvent.java';Reason='Do not create a journal tombstone table when the repository already models OAI identity, withdrawal time/reason and integration events.'} }
    if ($name -match '^(dois|doi_settings)$') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoItem/RepoBitstream DOI fields and RepoIntegrationEvent deposit history';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoBitstream.java; src/ais/database/model/repository/RepoIntegrationEvent.java';Reason='Article/issue DOI uses RepoItem; galley DOI extends RepoBitstream. Deposit attempts use the existing integration event table.'} }
    if ($name -match '^announcement') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='PengumumanPenelitian plus KategoriPengumuman/LampiranPengumumanPenelitian and provenance';Candidate='src/ais/database/model/penelitiandanpengabdian/PengumumanPenelitian.java; src/ais/database/model/KategoriPengumuman.java; src/ais/database/model/file/LampiranPengumumanPenelitian.java';Reason='Add nullable JurnalPenelitian scope and localized metadata JSON to the existing research announcement path; do not create PengumumanJurnal/JenisPengumumanJurnal tables.'} }
    if ($name -match '^(categories|category_settings|controlled_vocabs|controlled_vocab_entries|controlled_vocab_entry_settings|sections|section_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='Typed RepoCollection hierarchy/profiles plus RepoItemMetadata/RepoItemRelation';Candidate='src/ais/database/model/repository/RepoCollection.java; src/ais/database/model/repository/RepoItemMetadata.java; src/ais/database/model/repository/RepoItemRelation.java';Reason='Reuse collection hierarchy, sort order and JSON profiles for sections/categories/vocabulary. Item-category links extend the existing relation instead of creating separate masters.'} }
    if ($name -eq 'publication_categories') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoItemRelation item-to-collection category link';Candidate='src/ais/database/model/repository/RepoItemRelation.java';Reason='Add relatedCollectionId/sequence semantics to the existing generic relation; do not create RelasiKategoriPublikasiJurnal.'} }
    if ($name -match '^(citations|citation_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='Repeatable RepoItemMetadata citation values plus Artikel.referensi/sitasi projection';Candidate='src/ais/database/model/repository/RepoItemMetadata.java; src/ais/database/model/penelitiandanpengabdian/Artikel.java';Reason='OJS citations are ordered raw citation values and fit metadataField/language/place; no citation table is required unless a later parser introduces structured entities.'} }
    if ($name -match '^custom_(issue|section)_orders$') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoCollection.sortOrder and RepoItemRelation.sequenceNumber';Candidate='src/ais/database/model/repository/RepoCollection.java; src/ais/database/model/repository/RepoItemRelation.java';Reason='Order is an attribute of the existing hierarchy/relation, not a standalone table.'} }
    if ($name -eq 'edit_decisions') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='Append-only RepoWorkflowEvent decision payload linked to review round/stage';Candidate='src/ais/database/model/repository/RepoWorkflowEvent.java';Reason='Extend the existing event with structured payload/stage/round references; do not introduce a second workflow-event table.'} }
    if ($name -match '^email_log') { return [pscustomobject]@{Decision='MERGED';Target='Notifikasi structured message/result history plus NotifikasiDibaca and provenance';Candidate='src/ais/database/model/Notifikasi.java; src/ais/delivery/email/sender/MailSender.java';Reason='Notifikasi already stores recipients, subject/body JSON and send results. Preserve CC/BCC/event metadata in its structured payload and source provenance.'} }
    if ($name -match '^email_templates') { return [pscustomobject]@{Decision='NEW_MODEL';Target='Single TemplateEmailJurnal table with versioned localized subject/body JSON';Candidate='TemplateSurat is not safe: it lacks journal scope, immutable key/version and email variable policy';Reason='One purpose-built template table covers all 73 keys and locales; do not create separate settings/default tables.'} }
    if ($name -match '^genre') { return [pscustomobject]@{Decision='MERGED';Target='RepoCollection.metadataProfileJson component registry plus RepoBitstream.bundleName/metadata';Candidate='src/ais/database/model/repository/RepoCollection.java; src/ais/database/model/repository/RepoBitstream.java';Reason='File components are low-churn configuration and bitstream attributes, not a separate journal master table.'} }
    if ($name -match '^highlight') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoItem.featured/featuredAt plus RepoItemMetadata URL/sequence';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoItemMetadata.java';Reason='Existing repository already implements featured content. Extend metadata only for external URL/sequence highlights.'} }
    if ($name -match '^(institutions|institution_settings|institution_ip)$') { return [pscustomobject]@{Decision='MERGED';Target='Existing PerguruanTinggi/PerguruanTinggiLain identity plus LanggananJurnal policy and RentangIpLanggananJurnal';Candidate='src/ais/database/model/PerguruanTinggi.java; src/ais/database/model/PerguruanTinggiLain.java';Reason='Link recognized institutions to existing masters, retain journal policy on LanggananJurnal and create only the queryable IP range. No duplicate institution or access-profile table is needed.'} }
    if ($name -match '^(issues|issue_settings)$') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoItem documentType JOURNAL_ISSUE plus RepoItemMetadata/RepoItemRelation';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoItemMetadata.java; src/ais/database/model/repository/RepoItemRelation.java';Reason='Issue is publishable repository content with DOI/OAI/access/version/slug. Volume/number/year/show flags are typed metadata and TOC uses existing relations; no EdisiJurnal table is required.'} }
    if ($name -eq 'notes') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoWorkflowEvent NOTE payload';Candidate='src/ais/database/model/repository/RepoWorkflowEvent.java';Reason='Journal notes are item-associated workflow events; add structured title/content payload instead of creating CatatanJurnal.'} }
    if ($name -eq 'queries') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='Existing Diskusi/DiskusiKomentar with journal reference, stage, sequence and closed state';Candidate='src/ais/database/model/Diskusi.java; src/ais/database/model/DiskusiKomentar.java';Reason='The generic discussion/thread tables are mapped by the main SessionFactory and already represent a topic plus replies. Add nullable typed reference and journal workflow fields; do not create DiskusiJurnal.'} }
    if ($name -eq 'query_participants') { return [pscustomobject]@{Decision='NEW_MODEL';Target='PesertaDiskusiJurnal linked to existing Diskusi and Tbmuser';Candidate='src/ais/database/model/Diskusi.java; src/ais/database/model/Tbmuser.java';Reason='Participant membership must remain queryable, unique and object-authorizable. No safe existing join table represents this relation, so only the participant link is new.'} }
    if ($name -match '^navigation_menu') { return [pscustomobject]@{Decision='MERGED';Target='JurnalPenelitian/RepoCollection navigation profile JSON plus AIS Menu only for management entry';Candidate='src/ais/database/model/repository/RepoCollection.java; src/ais/database/model/Menu.java';Reason='Public journal navigation is low-churn localized configuration. It must not duplicate the AIS application Menu hierarchy.'} }
    if ($name -eq 'plugin_settings') { return [pscustomobject]@{Decision='MERGED';Target='Reviewed RepoCollection metadata/workflow/access profile JSON plus RepoIntegrationEvent';Candidate='src/ais/database/model/repository/RepoCollection.java; src/ais/database/model/repository/RepoIntegrationEvent.java';Reason='Only allowlisted native AIS plugin-equivalent settings are transformed; raw OJS plugin settings remain provenance.'} }
    if ($name -match '^(rors|ror_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoAuthorAuthority.rorId plus existing institution identity and provenance';Candidate='src/ais/database/model/repository/RepoAuthorAuthority.java; src/ais/database/model/PerguruanTinggi.java';Reason='Do not create a local ROR master solely to mirror OJS cache rows; retain normalized ROR IDs and refresh descriptive data through the native integration.'} }
    if ($name -match '^(static_pages|static_page_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoItem documentType STATIC_PAGE plus RepoItemMetadata';Candidate='src/ais/database/model/repository/RepoItem.java; src/ais/database/model/repository/RepoItemMetadata.java';Reason='Static pages fit existing versioned publishable repository items with slug, access and localized metadata.'} }
    if ($name -eq 'submission_comments') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoWorkflowEvent imported COMMENT payload';Candidate='src/ais/database/model/repository/RepoWorkflowEvent.java';Reason='Legacy comments are immutable workflow history and do not need a separate KomentarWorkflowJurnal table.'} }
    if ($name -eq 'user_interests') { return [pscustomobject]@{Decision='MERGED';Target='RepoAuthorAuthority.topics and RepoUserPreference plus provenance';Candidate='src/ais/database/model/repository/RepoAuthorAuthority.java; src/ais/database/model/repository/RepoUserPreference.java';Reason='Reuse authority topics/preferences for reviewer interests; normalized vocabulary entries remain RepoCollection references.'} }
    if ($name -match '^(reviewer_suggestions|reviewer_suggestion_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='PenugasanReviewerJurnal status SUGGESTED plus RepoAuthorAuthority and settings JSON';Candidate='src/ais/database/model/repository/RepoAuthorAuthority.java';Reason='A suggestion is a pre-assignment state for an existing reviewer authority; do not create a standalone suggestion master.'} }
    if ($name -match '^(subscription_types|subscription_type_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoCollection.accessPolicyJson versioned subscription catalog plus LanggananJurnal immutable policy snapshot';Candidate='src/ais/database/model/repository/RepoCollection.java';Reason='Subscription types are low-volume journal access configuration. Store the active localized catalog in the existing collection access policy and copy price/duration/format/institutional terms into each subscription snapshot; no TipeLanggananJurnal table is needed.'} }
    if ($name -eq 'review_rounds') { return [pscustomobject]@{Decision='ALTER_EXISTING';Target='RepoWorkflowEvent ROUND_STARTED/ROUND_CLOSED payload plus PenugasanReviewerJurnal.roundNumber and RepoBitstream round metadata';Candidate='src/ais/database/model/repository/RepoWorkflowEvent.java; src/ais/database/model/repository/RepoBitstream.java';Reason='A round is a lifecycle boundary, not an independent master. Append round events and carry the round number on assignments/files; do not create PutaranReviewJurnal.'} }
    if ($name -match '^(review_forms|review_form_settings|review_form_elements|review_form_element_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='RepoCollection.workflowProfileJson immutable versioned review-form definitions plus ImportMappingOjs';Candidate='src/ais/database/model/repository/RepoCollection.java';Reason='Review form definitions are low-volume journal workflow configuration. Keep append-only versions with localized elements, sequence, type, required flag and options in the existing workflow profile; no FormReviewJurnal or ElemenFormReviewJurnal table is needed.'} }
    if ($name -eq 'review_form_responses') { return [pscustomobject]@{Decision='MERGED';Target='PenugasanReviewerJurnal formVersionKey/responseJson/responseChecksum plus ImportMappingOjs';Candidate='No safe existing assessment-result table; response is owned by the new reviewer assignment aggregate';Reason='A response belongs to one reviewer assignment and form version. Store its immutable typed answer snapshot on that assignment; do not create JawabanReviewJurnal unless cross-journal per-question SQL analytics later becomes an approved requirement.'} }
    if ($name -match '^users?$|^user_settings$') { return [pscustomobject]@{Decision='MERGED';Target='Tbmuser plus scoped external journal profile and provenance';Candidate='src/ais/database/model/Tbmuser.java';Reason='Link only by provenance or verified email; never import OJS password/session.'} }
    if ($name -match '^(user_groups|user_group_settings|user_group_stage)$') { return [pscustomobject]@{Decision='MERGED';Target='Tbmrole.jurnalAksesJson plus ImportMappingOjs';Candidate='src/ais/database/model/Tbmrole.java; src/ais/action/maintenance/TbmroleAction.java; pattern src/ais/common/EbisnisMenuKatalog.java';Reason='Reuse the active AIS role and versioned journal capability JSON; do not create a parallel global journal role/group table. OJS context/stage metadata is retained in provenance and mapped only after administrator review.'} }
    if ($name -eq 'user_user_groups') { return [pscustomobject]@{Decision='MERGED';Target='PenugasanTahapJurnal plus ImportMappingOjs';Candidate='src/ais/database/model/Tbmuser.java and active Tbmrole';Reason='Membership is object/context scope, not another global role. Resolve the user to AIS identity and materialize only reviewed journal/stage assignments with source provenance.'} }
    if ($name -match '^(queued_payments|completed_payments)$') { return [pscustomobject]@{Decision='MERGED';Target='AIS payment abstraction plus Jurnal payment state/provenance';Candidate='src/ais/database/model/JenisPembayaran.java; src/ais/database/model/LogPembayaran.java and existing payment providers';Reason='Do not duplicate gateway settlement records; keep journal order/policy linkage, idempotency and reconciliation only where existing fields cannot represent them.'} }
    if ($name -match '^(notifications|notification_settings|notification_subscription_settings)$') { return [pscustomobject]@{Decision='MERGED';Target='Notifikasi/NotifikasiDibaca plus existing RepoNotification/RepoUserPreference and provenance';Candidate='src/ais/database/model/Notifikasi.java; src/ais/database/model/NotifikasiDibaca.java; src/ais/database/model/repository/RepoNotification.java; src/ais/database/model/repository/RepoUserPreference.java';Reason='Reuse platform delivery/read state and repository event preferences. Journal-specific notification and preference tables are not needed.'} }
    $entity = Get-TargetEntity $name
    if ($entity -eq '__UNMAPPED__') { throw "No explicit target aggregate for $name" }
    return [pscustomobject]@{Decision='NEW_MODEL';Target=$entity;Candidate='No exact current AIS journal equivalent found in audited package';Reason='Preserve source semantics in the named journal-scoped, tenant-safe native aggregate and its immutable provenance.'}
}

function Get-TargetEntity([string]$name) {
    switch -Regex ($name) {
        '^announcement_type' { return 'JenisPengumumanJurnal' }
        '^announcement' { return 'PengumumanJurnal' }
        '^categories$|^category_settings$|^publication_categories$' { return 'KategoriJurnal and RelasiKategoriPublikasiJurnal' }
        '^citations$|^citation_settings$' { return 'SitasiArtikelJurnal' }
        '^controlled_vocab' { return 'KosakataJurnal and EntriKosakataJurnal' }
        '^custom_issue_orders$|^custom_section_orders$' { return 'UrutanEdisiJurnal and UrutanBagianJurnal' }
        '^data_object_tombstone' { return 'TombstoneOaiJurnal' }
        '^dois$|^doi_settings$' { return 'IdentifierJurnal and LogDepositIdentifierJurnal' }
        '^edit_decisions$' { return 'KeputusanEditorialJurnal' }
        '^email_log' { return 'LogEmailJurnal and PenerimaLogEmailJurnal' }
        '^email_templates_default_data$|^email_templates$|^email_templates_settings$' { return 'TemplateEmailJurnal and PengaturanTemplateEmailJurnal' }
        '^failed_jobs$|^jobs$|^job_batches$' { return 'JobJurnal, BatchJobJurnal and KegagalanJobJurnal' }
        '^filter_groups$|^filters$|^filter_settings$' { return 'PipelineMetadataJurnal and PengaturanPipelineMetadataJurnal' }
        '^genre' { return 'KomponenFileJurnal' }
        '^highlight' { return 'SorotanJurnal' }
        '^institution_ip$|^institutions$|^institution_settings$' { return 'InstitusiAksesJurnal and RentangIpInstitusiJurnal' }
        '^invitations$' { return 'UndanganPeranJurnal' }
        '^issues$|^issue_settings$' { return 'EdisiJurnal and PengaturanEdisiJurnal' }
        '^library_file' { return 'PustakaWorkflowJurnal' }
        '^navigation_menu_item_assignment' { return 'PenempatanItemNavigasiJurnal' }
        '^navigation_menu_items$|^navigation_menu_item_settings$' { return 'ItemNavigasiJurnal' }
        '^navigation_menus$' { return 'NavigasiJurnal' }
        '^notes$' { return 'CatatanJurnal' }
        '^oai_resumption_tokens$' { return 'TokenKelanjutanOaiJurnal' }
        '^plugin_settings$' { return 'PengaturanIntegrasiJurnal' }
        '^publication_categories$' { return 'RelasiKategoriPublikasiJurnal' }
        '^queries$' { return 'Existing Diskusi and DiskusiKomentar' }
        '^query_participants$' { return 'PesertaDiskusiJurnal linked to existing Diskusi and Tbmuser' }
        '^review_assignment_settings$|^review_assignments$' { return 'PenugasanReviewerJurnal with versioned settings JSON' }
        '^review_files$|^review_round_files$' { return 'RelasiLampiranReviewJurnal' }
        '^review_form_elements$|^review_form_element_settings$' { return 'RepoCollection.workflowProfileJson versioned review form definition' }
        '^review_form_responses$' { return 'PenugasanReviewerJurnal immutable response snapshot' }
        '^review_forms$|^review_form_settings$' { return 'RepoCollection.workflowProfileJson versioned review form definition' }
        '^review_rounds$' { return 'RepoWorkflowEvent round lifecycle plus assignment/file round number' }
        '^reviewer_suggestions$|^reviewer_suggestion_settings$' { return 'SaranReviewerJurnal' }
        '^rors$|^ror_settings$' { return 'OrganisasiRorJurnal' }
        '^sections$|^section_settings$' { return 'BagianJurnal and PengaturanBagianJurnal' }
        '^site$|^site_settings$' { return 'PortalJurnal and PengaturanPortalJurnal' }
        '^stage_assignments$|^subeditor_submission_group$' { return 'PenugasanTahapJurnal' }
        '^static_pages$|^static_page_settings$' { return 'HalamanStatisJurnal' }
        '^submission_comments$' { return 'KomentarWorkflowJurnal' }
        '^submission_tombstones$' { return 'TombstoneOaiJurnal' }
        '^subscription_types$|^subscription_type_settings$' { return 'RepoCollection.accessPolicyJson subscription catalog plus LanggananJurnal policy snapshot' }
        '^subscriptions$|^institutional_subscriptions$' { return 'LanggananJurnal linked to existing user/institution/payment records' }
        '^temporary_files$' { return 'UnggahanSementaraJurnal' }
        '^user_interests$' { return 'MinatReviewerJurnal' }
        default { return '__UNMAPPED__' }
    }
}

function To-Camel([string]$value) {
    $parts = $value -split '_'
    $result = $parts[0]
    for ($i=1; $i -lt $parts.Count; $i++) {
        if ($parts[$i].Length -gt 0) { $result += $parts[$i].Substring(0,1).ToUpperInvariant() + $parts[$i].Substring(1) }
    }
    return $result
}

function Get-FieldMapping([string]$name, $column, $target) {
    $field = To-Camel $column.Name
    $raw = 'ImportMappingOjs.rawPayload.' + $field
    if ($target.Decision -eq 'NOT_APPLICABLE_WITH_RATIONALE') {
        return $raw + '; not copied to an AIS business table, retained only for source evidence/reconciliation'
    }
    if ($target.Decision -eq 'DERIVED') {
        return $raw + '; target value/index is rebuilt by the named AIS service and reconciled against source counts'
    }
    if ($name -match '^(user_groups|user_group_settings|user_group_stage)$') {
        return $raw + '; reviewed transform contributes to versioned Tbmrole.jurnalAksesJson, never an automatic privilege grant'
    }
    if ($name -eq 'user_user_groups') {
        return $raw + '; reviewed identity/scope transform contributes to PenugasanTahapJurnal'
    }
    if ($name -match '(_settings$|_setting[s]?$|_default_data$)') {
        return $raw + '; allowlisted key is normalized into ' + $target.Target + ' configuration/metadata JSON; unrecognized key remains provenance only'
    }
    if ($name -match '^(journals)$') { return 'JurnalPenelitian/RepoCollection.' + $field + '; source identity additionally recorded by ImportMappingOjs' }
    if ($name -match '^(site)$') { return 'PerguruanTinggi/RepoCollection platform configuration.' + $field + '; source identity additionally recorded by ImportMappingOjs' }
    if ($name -match '^(submissions|publications)$') { return 'RepoItem.' + $field + ' or typed RepoItemMetadata when no native column exists; Artikel receives only the published compatibility projection' }
    if ($name -match '^(authors|author_affiliations)$') { return 'RepoAuthorAuthority/RepoItemContributor.' + $field + '; localized/repeatable value uses RepoItemMetadata and published projection uses AnggotaArtikel' }
    if ($name -match '(^files$|files|_file_|galley|galleys)') { return 'RepoBitstream.' + $field + '; BLOB bytes use LampiranLain scalar ref=RepoBitstream.id and source identity uses ImportMappingOjs' }
    if ($name -match '^metrics_') { return 'RepoUsageEvent/AgregatPenggunaanJurnal.' + $field + '; RepoItem receives only validated total counters' }
    if ($name -match '^data_object_tombstone|^submission_tombstones$') { return 'RepoItem withdrawal/OAI state or RepoIntegrationEvent payload.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^dois$') { return 'RepoItem/RepoBitstream DOI state or RepoIntegrationEvent payload.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^announcement') { return 'PengumumanPenelitian/KategoriPengumuman.' + $field + '; locale/source-only value uses localized JSON/provenance' }
    if ($name -match '^(categories|controlled_vocab|sections)') { return 'RepoCollection hierarchy/profile.' + $field + '; item association uses RepoItemRelation/RepoItemMetadata' }
    if ($name -eq 'publication_categories') { return 'RepoItemRelation item-to-collection category link.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^citations$') { return 'RepoItemMetadata(metadataField=journal.citation, place=seq).' + $field + '; Artikel.referensi/sitasi is a compatibility projection' }
    if ($name -match '^custom_(issue|section)_orders$') { return 'RepoCollection.sortOrder/RepoItemRelation.sequenceNumber derived from ' + $column.Name + '; raw source retained by ImportMappingOjs' }
    if ($name -eq 'edit_decisions' -or $name -eq 'notes' -or $name -eq 'submission_comments') { return 'RepoWorkflowEvent structured payload.' + $field + '; immutable source identity retained by ImportMappingOjs' }
    if ($name -eq 'queries') { return 'Diskusi journal typed reference/workflow state.' + $field + '; messages use DiskusiKomentar and source identity uses ImportMappingOjs' }
    if ($name -eq 'query_participants') { return 'PesertaDiskusiJurnal.' + $field + '; links existing Diskusi/Tbmuser with a unique membership constraint and provenance' }
    if ($name -match '^email_log') { return 'Notifikasi structured recipient/message/result JSON.' + $field + '; source identity retained by ImportMappingOjs' }
    if ($name -match '^email_templates') { return 'TemplateEmailJurnal localized/versioned payload.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^genre') { return 'RepoCollection.metadataProfileJson/RepoBitstream component metadata.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^highlight') { return 'RepoItem featured state/RepoItemMetadata.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^(institutions|institution_ip|institutional_subscription_ip)') { return 'PerguruanTinggi/PerguruanTinggiLain identity, LanggananJurnal policy or RentangIpLanggananJurnal.' + $field + '; unmatched source preserved by ImportMappingOjs' }
    if ($name -match '^issues$') { return 'RepoItem JOURNAL_ISSUE/RepoItemMetadata.' + $field + '; issue/article ordering uses RepoItemRelation' }
    if ($name -match '^navigation_menu') { return 'RepoCollection navigation profile JSON.' + $field + '; AIS Menu is used only for management entry' }
    if ($name -eq 'plugin_settings') { return 'RepoCollection allowlisted native integration profile/RepoIntegrationEvent.' + $field + '; raw OJS key remains provenance' }
    if ($name -match '^rors$') { return 'RepoAuthorAuthority/existing institution normalized ROR metadata.' + $field + '; descriptive cache is refreshable' }
    if ($name -match '^static_pages$') { return 'RepoItem STATIC_PAGE/RepoItemMetadata.' + $field + '; source identity retained by ImportMappingOjs' }
    if ($name -eq 'user_interests') { return 'RepoAuthorAuthority.topics/RepoUserPreference.' + $field + '; normalized vocabulary reference retained in provenance' }
    if ($name -match '^reviewer_suggestions$') { return 'PenugasanReviewerJurnal status SUGGESTED/RepoAuthorAuthority.' + $field + '; source identity retained by ImportMappingOjs' }
    if ($name -match '^(subscription_types|subscription_type_settings)$') { return 'RepoCollection.accessPolicyJson versioned subscription catalog.' + $field + '; every LanggananJurnal stores an immutable policy snapshot and raw source remains in ImportMappingOjs' }
    if ($name -eq 'review_rounds') { return 'RepoWorkflowEvent round lifecycle payload/PenugasanReviewerJurnal.roundNumber/RepoBitstream round metadata.' + $field + '; raw source retained by ImportMappingOjs' }
    if ($name -match '^(review_forms|review_form_settings|review_form_elements|review_form_element_settings)$') { return 'RepoCollection.workflowProfileJson immutable review-form version.' + $field + '; localized definition and raw source retained by ImportMappingOjs' }
    if ($name -eq 'review_form_responses') { return 'PenugasanReviewerJurnal immutable responseJson/formVersionKey/responseChecksum.' + $field + '; raw source retained by ImportMappingOjs' }
    return $target.Target + '.' + $field + '; source identity is additionally recorded by ImportMappingOjs'
}

$schemas = Get-PhpSchemas
Add-LegacyXmlSchema $schemas 'institutional_subscription_ip'
Add-LegacyXmlSchema $schemas 'submission_tombstones'

$missing = @($tableNames | Where-Object { -not $schemas.ContainsKey($_) })
if ($missing.Count -gt 0) { throw ('Missing schema definitions: ' + ($missing -join ', ')) }

$freshCount = @($schemas.Values | Where-Object Presence -eq 'FRESH_3505').Count
$legacyCount = @($schemas.Values | Where-Object Presence -eq 'LEGACY_SOURCE_ONLY').Count
if ($freshCount -ne 132 -or $legacyCount -ne 2) { throw "Unexpected presence split fresh/plugin=$freshCount legacy=$legacyCount" }

$out = New-Object System.Collections.Generic.List[string]
$out.Add('# OJS 3.5.0-5 — Field-level table mapping')
$out.Add('')
$out.Add('Generated from the official OJS tag and its exact `pkp-lib` gitlink. This is an import coverage matrix, not a claim that every legacy table exists in a fresh 3.5 database.')
$out.Add('')
$out.Add("- OJS commit: ``$actualOjs``")
$out.Add("- pkp-lib commit: ``$actualPkp``")
$out.Add('- Coverage: **134/134 source table names**; **132 fresh/plugin tables** and **2 legacy-source-only tables**.')
$out.Add('- Fresh core/PKP install migrations create 130 tables; bundled `generic/staticPages` creates 2 more.')
$out.Add('- `institutional_subscription_ip` is removed by `classes/migration/upgrade/v3_4_0/I6895_Institutions.php`; `submission_tombstones` is superseded by generic data-object tombstones. Both remain covered for version-aware legacy imports.')
$out.Add('- Target persistence decision: reuse existing entities in their current schemas through the main `HibernateUtil`/`hibernate.cfg.xml`; place only genuinely new journal-specific tables in `penelitiandanpengabdian`. Source rows use external JDBC read-only, not `OjsHibernateUtil`. BLOB content reuses `LampiranLain` on the streaming SessionFactory keyed by scalar `RepoBitstream.id`.')
$out.Add('- Existing-first RBAC decision: OJS role groups map to the active AIS `Tbmrole`, versioned `jurnalAksesJson`, scoped `PenugasanTahapJurnal`, and provenance. No parallel global journal role/group table is introduced.')
$out.Add('- Existing-first publication decision: `JurnalPenelitian`, the Repository AIS aggregate (`RepoCollection`, `RepoItem`, `RepoItemMetadata`, `RepoBitstream`, contributor/event/usage models), `Artikel`, notification/payment/institution models, and `LampiranLain` are reused before any journal table is introduced. The 134 source tables are coverage, not a target-table count.')
$out.Add('- Final table budget: 11 source tables remain `NEW_MODEL` and consolidate into six domain tables (`TemplateEmailJurnal`, `LanggananJurnal`, `UndanganPeranJurnal`, `PesertaDiskusiJurnal`, `PenugasanTahapJurnal`, `PenugasanReviewerJurnal`). Together with two support and four import-control tables, the implementation target is 12 new tables.')
$out.Add('- Review/subscription consolidation: subscription catalogs use versioned `RepoCollection.accessPolicyJson`; review-form definitions use versioned `RepoCollection.workflowProfileJson`; round lifecycle uses `RepoWorkflowEvent`; each subscription/reviewer assignment stores its immutable policy/response snapshot.')
$out.Add('- Generated by `docs/jurnal/Generate-Ojs3505TableMapping.ps1`; generation fails on commit mismatch, duplicate/missing table names, or missing schema definitions.')
$out.Add('')
$out.Add('## Decision vocabulary')
$out.Add('')
$out.Add('`EXISTING_VERIFIED`, `ALTER_EXISTING`, `NEW_MODEL`, `MERGED`, `DERIVED`, and `NOT_APPLICABLE_WITH_RATIONALE` have the meanings required by the implementation specification. No model implementation may start until this generated matrix and the candidate audit are reviewed together.')
$out.Add('')

$testNumber = 1
foreach ($name in ($tableNames | Sort-Object)) {
    $schema = $schemas[$name]
    $target = Get-Target $name
    $testId = 'MAP-' + $testNumber.ToString('000')
    $testNumber++
    $out.Add("## ``$name``")
    $out.Add('')
    $out.Add("- **Presence:** ``$($schema.Presence)``")
    $out.Add("- **Authoritative source:** ``$($schema.Source)``")
    $out.Add("- **AIS candidate:** $($target.Candidate)")
    $out.Add("- **Decision:** ``$($target.Decision)``")
    $out.Add("- **Target class/table:** $($target.Target)")
    $out.Add('- **Target persistence:** entity existing tetap pada schema existing-nya dan seluruh tabel journal-specific baru berada di `penelitiandanpengabdian`, semuanya melalui `HibernateUtil` + `hibernate.cfg.xml`. Source dibaca JDBC read-only. BLOB file/galley memakai `LampiranLain` pada SessionFactory streaming dengan scalar `RepoBitstream.id`, tanpa ORM lintas database.')
    $out.Add("- **Semantics/decision rationale:** $($target.Reason)")
    $out.Add("- **Conflict policy:** provenance key ``(sourceInstanceUuid, sourceTable, sourcePrimaryKey, sourceRevision)``; normalized DOI/URN/email/slug conflicts require explicit link, non-destructive merge, external profile, skip, or manual resolution. Never match people by name alone.")
    $out.Add("- **Transform:** preserve locale and raw source value in provenance; normalize identifiers, UTC/date semantics, booleans/enums, safe HTML, and scalar cross-database file references in a version-aware adapter.")
    $out.Add("- **Test:** ``$testId`` verifies row counts, every source column, relationship counts, collision behavior, and dry-run/write reconciliation.")
    $out.Add('')
    $out.Add('| Source column | Type / size | Nullable | Default | Source modifiers | Field-level target mapping |')
    $out.Add('|---|---|---:|---|---|---|')
    foreach ($column in $schema.Columns) {
        $mapped = Get-FieldMapping $name $column $target
        $out.Add("| ``$($column.Name)`` | ``$($column.Type.Replace('|','\|'))`` | $($column.Nullable) | ``$($column.Default)`` | ``$($column.Modifiers)`` | $mapped |")
    }
    $out.Add('')
    $constraintText = if ($schema.Constraints.Count -gt 0) { ($schema.Constraints | ForEach-Object { '``' + $_ + '``' }) -join '; ' } else { 'No explicit index/unique/FK expression in the authoritative create block.' }
    $out.Add("**Indexes, unique constraints, and foreign keys:** $constraintText")
    $out.Add('')
}

$content = ($out -join "`r`n") + "`r`n"
if ($content -match '(?i)\bTBD\b|\bUNKNOWN\b') { throw 'Forbidden unresolved marker in generated mapping.' }
[IO.File]::WriteAllText($OutputPath, $content, (New-Object Text.UTF8Encoding($false)))
Write-Output "Generated $OutputPath"
Write-Output "Coverage: $($tableNames.Count)/134; fresh/plugin=$freshCount; legacy-source-only=$legacyCount"
