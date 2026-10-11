package io.github.ebrahimsalem1.bookingqa.autonomous.explore;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Reduces a JSON document to the sorted set of its key paths with value types, e.g.
 * {@code results[].dest_id:string}. Values are ignored, so the fingerprint is stable across
 * runs while still catching renamed, removed, added or re-typed fields (contract drift).
 */
public final class ShapeFingerprint {

    private static final int MAX_DEPTH = 5;

    private ShapeFingerprint() {
    }

    public static List<String> of(JsonNode root) {
        TreeSet<String> paths = new TreeSet<>();
        walk(root, "$", 0, paths);
        return new ArrayList<>(paths);
    }

    private static void walk(JsonNode node, String path, int depth, TreeSet<String> out) {
        if (node == null || depth > MAX_DEPTH) {
            return;
        }
        if (node.isObject()) {
            out.add(path + ":object");
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                walk(f.getValue(), path + "." + f.getKey(), depth + 1, out);
            }
        } else if (node.isArray()) {
            out.add(path + ":array");
            // merge the shape of every element (arrays are often heterogeneous in optional fields)
            for (JsonNode element : node) {
                walk(element, path + "[]", depth + 1, out);
            }
        } else {
            out.add(path + ":" + type(node));
        }
    }

    private static String type(JsonNode n) {
        if (n.isNumber()) {
            return "number";
        }
        if (n.isBoolean()) {
            return "boolean";
        }
        if (n.isNull()) {
            return "null";
        }
        return "string";
    }

    /** Fields present in the baseline but no longer returned, and vice versa. */
    public static Observation.ShapeDrift compare(List<String> baseline, List<String> current) {
        if (baseline == null) {
            return new Observation.ShapeDrift(false, List.of(), List.of());
        }
        List<String> removed = baseline.stream().filter(p -> !current.contains(p)).filter(p -> !p.endsWith(":null")).toList();
        List<String> added = current.stream().filter(p -> !baseline.contains(p)).filter(p -> !p.endsWith(":null")).toList();
        return new Observation.ShapeDrift(true, added, removed);
    }
}
