package com.quincaisoft.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Operator-run adapter for the reviewed synthetic fixture and local Java API. */
public final class CatalogHistoryImport {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String DATASET = "quincaisoft-synthetic-history-v1";
    private static final String TENANT = "0984ca97-010e-51fc-a6a0-7c33453e214e";
    private static final String COMPANY = "f6b40fb6-3f23-5891-a908-2de13a773d7d";
    private static final List<String> FILES = List.of("branches", "manufacturers", "categories", "units",
        "suppliers", "customers", "products", "variants", "supplierCatalog");

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final URI api;
    private final String token;
    private final BufferedWriter mapping;
    private final Map<String, Map<UUID, UUID>> ids = new HashMap<>();
    private long mapped;

    private CatalogHistoryImport(URI api, String token, BufferedWriter mapping) {
        this.api = api;
        this.token = token;
        this.mapping = mapping;
    }

    public static void main(String[] args) throws Exception {
        boolean opening = args.length == 5 && args[4].equals("--opening");
        if (args.length != 4 && !opening) throw new IllegalArgumentException(
            "Usage: <loopback-api-origin> <generated-directory> <reviewed-manifest> <new-mapping.ndjson> [--opening]");
        URI api = URI.create(args[0]);
        if (!"http".equals(api.getScheme()) || !"127.0.0.1".equals(api.getHost()) ||
            api.getPort() < 1 || api.getRawUserInfo() != null || api.getRawQuery() != null ||
            api.getRawFragment() != null || !Set.of("", "/").contains(api.getRawPath()))
            throw new IllegalArgumentException("An explicit HTTP loopback origin is required");
        String token = System.getenv("QUINCAI_HISTORY_TOKEN");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("QUINCAI_HISTORY_TOKEN is required");

        Path dir = Path.of(args[1]);
        JsonNode manifest = JSON.readTree(Files.readString(Path.of(args[2])));
        validateInputs(dir, manifest, opening ? List.of("branches", "events", "journals") : FILES);
        Path output = Path.of(args[3]).toAbsolutePath();
        if (Files.exists(output)) throw new IllegalArgumentException("Mapping output already exists");
        if (opening) {
            importOpening(api, token, dir, output);
            return;
        }
        Path staging = Files.createTempFile(output.getParent(), ".history-import-", ".ndjson");
        try {
            long mapped;
            try (var mapping = Files.newBufferedWriter(staging)) {
                var importer = new CatalogHistoryImport(api, token, mapping);
                importer.requireSyntheticScope(dir);
                importer.load(dir);
                mapped = importer.mapped;
            }
            Files.move(staging, output);
            System.out.println("Catalog and customer/supplier source mappings: " + mapped);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    private static void validateInputs(Path dir, JsonNode manifest, List<String> required) throws Exception {
        if (!DATASET.equals(manifest.path("datasetId").asText()) || !manifest.path("synthetic").asBoolean() ||
            !manifest.equals(JSON.readTree(Files.readString(dir.resolve("manifest.json")))))
            throw new IllegalArgumentException("Expected the reviewed synthetic fixture manifest");
        var verified = new HashSet<String>();
        for (JsonNode input : manifest.path("files")) {
            String name = input.path("file").asText();
            if (!required.stream().anyMatch(file -> name.equals(file + ".ndjson"))) continue;
            var digest = MessageDigest.getInstance("SHA-256");
            try (InputStream bytes = Files.newInputStream(dir.resolve(name));
                 var hashed = new DigestInputStream(bytes, digest)) {
                hashed.transferTo(java.io.OutputStream.nullOutputStream());
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(input.path("sha256").asText()) || !verified.add(name))
                throw new IllegalArgumentException("Invalid checksum or duplicate manifest entry for " + name);
        }
        if (verified.size() != required.size()) throw new IllegalArgumentException("Missing required input files");
    }

    private JsonNode requireSyntheticScope(Path dir) throws Exception {
        var response = send(HttpRequest.newBuilder(api.resolve("/api/v1/session")).GET(), "session");
        JsonNode scope = response.path("scope");
        if (!TENANT.equals(scope.path("tenantId").asText()) || !COMPANY.equals(scope.path("companyId").asText()))
            throw new IllegalStateException("Target is outside the historical synthetic tenant/company");
        boolean knownBranch;
        try (var branches = Files.lines(dir.resolve("branches.ndjson"))) {
            knownBranch = branches.map(JSON::readTree)
                .anyMatch(row -> row.path("id").asText().equals(scope.path("branchId").asText()));
        }
        if (!knownBranch) throw new IllegalStateException("Target branch is not in the historical fixture");
        return scope;
    }

    private static void importOpening(URI api, String token, Path dir, Path output) throws Exception {
        String approverToken = System.getenv("QUINCAI_HISTORY_APPROVER_TOKEN");
        if (approverToken == null || approverToken.isBlank())
            throw new IllegalArgumentException("QUINCAI_HISTORY_APPROVER_TOKEN is required for opening capital");
        var maker = new CatalogHistoryImport(api, token, null);
        var approver = new CatalogHistoryImport(api, approverToken, null);
        JsonNode makerScope = maker.requireSyntheticScope(dir);
        JsonNode approverScope = approver.requireSyntheticScope(dir);
        UUID branch = uuid(makerScope, "branchId");
        if (!branch.equals(uuid(approverScope, "branchId")) ||
            uuid(makerScope, "actorId").equals(uuid(approverScope, "actorId")))
            throw new IllegalArgumentException("Opening capital requires independent actors in the same branch");

        // Only the chronological prefix is supported. Later branches must wait for intervening events.
        JsonNode event = first(dir.resolve("events.ndjson"));
        JsonNode journal = first(dir.resolve("journals.ndjson"));
        UUID source = uuid(event, "id");
        if (event.path("sequence").asLong() != 1 || !event.path("synthetic").asBoolean() ||
            !event.path("type").asText().equals("OPENING_CAPITAL") || !source.equals(uuid(event, "operationId")) ||
            !TENANT.equals(event.path("tenantId").asText()) || !COMPANY.equals(event.path("companyId").asText()) ||
            !source.equals(uuid(journal, "eventId")) || !branch.equals(uuid(event, "branchId")) ||
            !branch.equals(uuid(journal, "branchId")) ||
            !event.required("businessDate").equals(journal.required("businessDate")))
            throw new IllegalArgumentException("Unexpected historical opening event or journal");

        var lines = new ArrayList<Map<String, Object>>();
        for (JsonNode line : journal.required("lines")) {
            if (!line.path("partyId").isNull()) throw new IllegalArgumentException("Opening journal has an unexpected party");
            lines.add(fields(line, "account", "debitXaf", "creditXaf"));
        }
        var draft = maker.post("/manual-journals", operation("opening-draft", source),
            Map.of("businessDate", event.required("businessDate"), "lines", lines,
                "reason", "Synthetic opening capital " + source));
        if (!uuid(makerScope, "actorId").equals(uuid(draft, "createdBy")))
            throw new IllegalStateException("Opening replay requires the original maker");
        UUID manualId = uuid(draft, "id");
        String path = "/manual-journals/" + manualId;
        var submitted = maker.post(path + "/submit", operation("opening-submit", source),
            Map.of("version", draft.required("version"), "reason", "Submit synthetic opening capital " + source));
        var posted = approver.post(path + "/approve", operation("opening-approve", source),
            Map.of("version", submitted.required("version"), "reason", "Approve synthetic opening capital " + source));
        if (!posted.path("status").asText().equals("POSTED") ||
            !uuid(approverScope, "actorId").equals(uuid(posted, "approvedBy")))
            throw new IllegalStateException("Opening journal approval did not post");

        var entry = new LinkedHashMap<String, Object>();
        entry.put("sourceId", source);
        entry.put("sourceJournalId", uuid(journal, "id"));
        entry.put("manualJournalId", manualId);
        entry.put("journalId", uuid(posted, "journalId"));
        entry.put("postingOperationId", operation("opening-approve", source));
        Files.writeString(output, JSON.writeValueAsString(entry) + "\n", StandardOpenOption.CREATE_NEW);
        System.out.println("Historical opening capital mappings: 1");
    }

    private static JsonNode first(Path path) throws Exception {
        try (var file = Files.newBufferedReader(path)) {
            return JSON.readTree(file.readLine());
        }
    }

    private void load(Path dir) throws Exception {
        for (String kind : FILES.subList(1, FILES.size())) {
            try (var reader = Files.newBufferedReader(dir.resolve(kind + ".ndjson"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode row = JSON.readTree(line);
                    UUID source = uuid(row, "id");
                    try {
                        accept(kind, source, row);
                    } catch (Exception error) {
                        throw new IllegalStateException("Stopped at " + kind + " source " + source +
                            " after " + mapped + " mappings. Retry with the same actor and source.", error);
                    }
                }
            }
        }
    }

    private void accept(String kind, UUID source, JsonNode row) throws Exception {
        switch (kind) {
            case "manufacturers" -> save(kind, source, "/catalog/manufacturers", fields(row, "code", "name"), "id");
            case "categories" -> save(kind, source, "/catalog/categories", fields(row, "code", "name", "active"), "id");
            case "units" -> save(kind, source, "/catalog/units", fields(row, "code", "name", "baseUnit",
                "factorNumerator", "factorDenominator", "precision", "active"), "id");
            case "suppliers" -> save(kind, source, "/suppliers/directory", fields(row, "code", "name", "phone",
                "email", "address", "taxReference", "paymentTermsDays", "bankReference", "active"), "id");
            case "customers" -> save(kind, source, "/customers", fields(row, "name", "type", "phone", "email",
                "address", "taxReference", "creditLimitXaf", "paymentTermsDays", "consent", "active"), "id");
            case "products" -> {
                var family = fields(row, "productType");
                family.put("modelNumber", row.required("modelNumber").asText());
                family.put("manufacturerId", target("manufacturers", row, "manufacturerId"));
                save(kind, source, "/catalog/families", family, "id");
            }
            case "variants" -> {
                var product = fields(row, "sku", "name", "baseUnit", "sellingPriceXaf", "taxBasisPoints", "brand",
                    "barcode", "reorderLevelMilli", "trackStock", "active");
                product.put("categoryId", target("categories", row, "categoryId"));
                UUID productId = save("stock-skus", source, "/products", product, "id");
                var variant = fields(row, "size");
                variant.put("colour", row.required("color"));
                variant.put("productId", productId);
                variant.put("familyId", target("products", row, "productId"));
                save(kind, source, "/catalog/variants", variant, "productId");
            }
            case "supplierCatalog" -> {
                var offer = fields(row, "supplierSku", "quotedCostXaf", "leadTimeDays", "minimumOrderMilli", "preferred");
                offer.put("productId", target("variants", row, "variantId"));
                offer.put("supplierId", target("suppliers", row, "supplierId"));
                save(kind, source, "/catalog/supplier-offers", offer, "productId");
            }
            default -> throw new IllegalArgumentException("Unsupported source kind " + kind);
        }
    }

    private UUID save(String kind, UUID source, String path, Map<String, Object> fields, String idField) throws Exception {
        UUID operation = operation(kind, source);
        fields.put("operationId", operation);
        fields.put("reason", "Historical synthetic fixture " + kind + "/" + source);
        var response = post(path, operation, fields);
        UUID target = uuid(response, idField);
        if (ids.computeIfAbsent(kind, ignored -> new HashMap<>()).putIfAbsent(source, target) != null)
            throw new IllegalStateException("Duplicate source mapping " + source);
        var entry = new LinkedHashMap<String, Object>();
        entry.put("kind", kind);
        entry.put("sourceId", source);
        entry.put("operationId", operation);
        entry.put("targetId", target);
        if (kind.equals("supplierCatalog")) entry.put("targetSupplierId", uuid(response, "supplierId"));
        mapping.write(JSON.writeValueAsString(entry));
        mapping.newLine();
        mapped++;
        return target;
    }

    private static UUID operation(String kind, UUID source) {
        return UUID.nameUUIDFromBytes((DATASET + ":" + kind + ":" + source).getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode post(String path, UUID operation, Map<String, Object> fields) throws Exception {
        var input = new LinkedHashMap<>(fields);
        input.put("operationId", operation);
        return send(HttpRequest.newBuilder(api.resolve("/api/v1" + path))
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", operation.toString())
            .header("X-Request-ID", UUID.randomUUID().toString())
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(input))), path);
    }

    private JsonNode send(HttpRequest.Builder request, String path) throws Exception {
        var response = client.send(request.timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("HTTP " + response.statusCode() + " at " + path);
        return JSON.readTree(response.body());
    }

    private UUID target(String kind, JsonNode row, String key) {
        UUID source = uuid(row, key);
        UUID result = ids.getOrDefault(kind, Map.of()).get(source);
        if (result == null) throw new IllegalArgumentException("Unmapped " + kind + " reference " + source);
        return result;
    }

    private static UUID uuid(JsonNode row, String key) {
        return UUID.fromString(row.required(key).asText());
    }

    private static Map<String, Object> fields(JsonNode row, String... keys) {
        var result = new LinkedHashMap<String, Object>();
        for (String key : keys) result.put(key, row.required(key));
        return result;
    }
}
