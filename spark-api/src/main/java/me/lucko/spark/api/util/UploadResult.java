package me.lucko.spark.api.util;

import java.util.Objects;

public final class UploadResult {
    private final String viewerUrl, bytebinUrl;

    public UploadResult(String viewerUrl, String bytebinUrl) {
        this.viewerUrl = viewerUrl;
        this.bytebinUrl = bytebinUrl;
    }

    /** Gets the viewer URL */
    public String getViewerUrl() {
        return viewerUrl;
    }

    /** Gets the Bytebin URL */
    public String getBytebinUrl() {
        return bytebinUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadResult that = (UploadResult) o;
        return Objects.equals(viewerUrl, that.viewerUrl) && Objects.equals(bytebinUrl, that.bytebinUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewerUrl, bytebinUrl);
    }

    @Override
    public String toString() {
        return "UploadResult{" +
                "viewerUrl='" + viewerUrl + '\'' +
                ", bytebinUrl='" + bytebinUrl + '\'' +
                '}';
    }
}
