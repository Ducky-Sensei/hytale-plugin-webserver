package net.nitrado.hytale.plugins.webserver.util;

import jakarta.servlet.http.HttpServletRequest;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.List;

public final class RequestUtils {
    private RequestUtils() {}

    /**
     * Negotiates the content type based on the request, with query parameter override enabled.
     *
     * @param req the HTTP request
     * @param supportedContentTypes the content types supported by the endpoint, in order of preference
     * @return the negotiated content type, or {@code null} if no match is found
     * @see #negotiateContentType(HttpServletRequest, boolean, String...)
     */
    public static String negotiateContentType(HttpServletRequest req, String ...supportedContentTypes) {
        return negotiateContentType(req, true, supportedContentTypes);
    }

    /**
     * Negotiates the content type based on the request's Accept header and optionally a query parameter.
     * <p>
     * When {@code allowOverrideFromQuery} is {@code true}, the {@code output} query parameter takes precedence
     * over the Accept header. The query parameter can be:
     * <ul>
     *   <li>A simple subtype (e.g., {@code ?output=json} matches {@code application/json})</li>
     *   <li>A full media type (e.g., {@code ?output=application/x.custom+json;version=1})</li>
     * </ul>
     * <p>
     * If the Accept header is used, content types are matched based on quality values. When multiple
     * content types have the same quality, the order in {@code supportedContentTypes} determines priority.
     *
     * @param req the HTTP request
     * @param allowOverrideFromQuery if {@code true}, the {@code output} query parameter can override Accept header negotiation
     * @param supportedContentTypes the content types supported by the endpoint, in order of preference
     * @return the negotiated content type, or {@code null} if no match is found
     */
    public static String negotiateContentType(HttpServletRequest req, boolean allowOverrideFromQuery, String ...supportedContentTypes) {
        if (supportedContentTypes == null || supportedContentTypes.length == 0) {
            return null;
        }

        if (allowOverrideFromQuery) {
            String outputParam = req.getParameter("output");
            if (outputParam != null && !outputParam.isBlank()) {
                return findContentTypeByQueryParam(outputParam, supportedContentTypes);
            }
        }

        String acceptHeader = req.getHeader("Accept");
        if (acceptHeader == null || acceptHeader.isBlank() || acceptHeader.equals("*/*")) {
            // Client accepts anything, return first supported
            return supportedContentTypes[0];
        }

        return findContentTypeByAcceptHeader(supportedContentTypes, acceptHeader);
    }

    @NullableDecl
    private static String findContentTypeByAcceptHeader(String[] supportedContentTypes, String acceptHeader) {
        List<AcceptEntry> acceptEntries = parseAcceptHeader(acceptHeader);

        String bestMatch = null;
        double bestQuality = -1;
        int bestSupportedIndex = Integer.MAX_VALUE;

        for (AcceptEntry entry : acceptEntries) {
            for (int i = 0; i < supportedContentTypes.length; i++) {
                String supported = supportedContentTypes[i];
                if (matches(entry.mediaType, supported)) {
                    // If quality is higher, or same quality but earlier in supportedContentTypes
                    if (entry.quality > bestQuality ||
                        (entry.quality == bestQuality && i < bestSupportedIndex)) {
                        bestQuality = entry.quality;
                        bestMatch = supported;
                        bestSupportedIndex = i;
                    }
                }
            }
        }
        return bestMatch;
    }

    /**
     * Finds a matching content type based on the output query parameter.
     * Matches if:
     * - The query param equals the full media type (e.g., "application/x.custom+json;version=1")
     * - The query param matches the subtype (e.g., "json" matches "application/json")
     */
    private static String findContentTypeByQueryParam(String queryParam, String[] supportedContentTypes) {
        for (String supported : supportedContentTypes) {
            // Exact match with full media type
            if (supported.equals(queryParam)) {
                return supported;
            }

            // Match against subtype (part after '/')
            String subtype = extractSubtype(supported);
            if (subtype.equals(queryParam)) {
                return supported;
            }
        }
        return null;
    }

    /**
     * Extracts the subtype from a media type (e.g., "json" from "application/json").
     * For types with parameters, extracts only the subtype portion.
     */
    private static String extractSubtype(String mediaType) {
        int slashIndex = mediaType.indexOf('/');
        if (slashIndex < 0) {
            return mediaType;
        }

        String afterSlash = mediaType.substring(slashIndex + 1);

        // Remove parameters (e.g., ";version=1")
        int semicolonIndex = afterSlash.indexOf(';');
        if (semicolonIndex >= 0) {
            afterSlash = afterSlash.substring(0, semicolonIndex);
        }

        return afterSlash;
    }

    private static List<AcceptEntry> parseAcceptHeader(String acceptHeader) {
        List<AcceptEntry> entries = new ArrayList<>();
        String[] parts = acceptHeader.split(",");

        for (String part : parts) {
            String[] tokens = part.trim().split(";");
            String mediaType = tokens[0].trim();
            double quality = 1.0;

            for (int i = 1; i < tokens.length; i++) {
                String param = tokens[i].trim();
                if (param.startsWith("q=")) {
                    try {
                        quality = Double.parseDouble(param.substring(2).trim());
                    } catch (NumberFormatException e) {
                        quality = 1.0;
                    }
                }
            }

            entries.add(new AcceptEntry(mediaType, quality));
        }

        return entries;
    }

    private static boolean matches(String acceptMediaType, String supportedMediaType) {
        if ("*/*".equals(acceptMediaType)) {
            return true;
        }

        if (acceptMediaType.endsWith("/*")) {
            String acceptType = acceptMediaType.substring(0, acceptMediaType.indexOf('/'));
            String supportedType = supportedMediaType.substring(0, supportedMediaType.indexOf('/'));
            return acceptType.equals(supportedType);
        }

        return acceptMediaType.equals(supportedMediaType);
    }

    private record AcceptEntry(String mediaType, double quality) {}
}
