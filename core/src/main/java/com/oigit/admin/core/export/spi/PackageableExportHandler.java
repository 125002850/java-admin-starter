package com.oigit.admin.core.export.spi;

public interface PackageableExportHandler<Q> extends ExportHandler<Q> {

    long countRows(Q query);
}
