package com.dealmanager.web;

/** Maps a deal's {@code @Version} to and from an HTTP ETag / If-Match value. */
final class ETags {

    private ETags() {
    }

    static String of(long version) {
        return "\"" + version + "\"";
    }

    /** Accepts {@code "3"}, {@code W/"3"} or a bare {@code 3}. */
    static long parseVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException();
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        value = value.replace("\"", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new PreconditionRequiredException();
        }
    }

    /** No usable If-Match header (HTTP 428). */
    static class PreconditionRequiredException extends RuntimeException {
        PreconditionRequiredException() {
            super("Send If-Match with the deal's current ETag (version) to change it");
        }
    }
}
