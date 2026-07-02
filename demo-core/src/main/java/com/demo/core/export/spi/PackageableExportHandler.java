package com.demo.core.export.spi;

public interface PackageableExportHandler<Q> extends ExportHandler<Q> {

    long countRows(Q query);
}
